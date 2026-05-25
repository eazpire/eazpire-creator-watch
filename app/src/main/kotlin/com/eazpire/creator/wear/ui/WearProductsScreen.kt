package com.eazpire.creator.wear.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eazpire.creator.core.api.CreatorApi
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.i18n.WearTranslationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WearProductsScreen(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    refreshKey: Int,
    modifier: Modifier = Modifier,
) {
    val ownerId = remember(tokenStore) { tokenStore.getOwnerId().orEmpty() }
    val api = remember(tokenStore) { CreatorApi(jwt = tokenStore.getJwt()) }
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var catalog by remember { mutableStateOf<List<WearCarouselItem>>(emptyList()) }
    var mockupCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var previewCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var storefrontCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var searchDraft by remember { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    var carouselIndex by remember { mutableIntStateOf(0) }
    val fetchingKeys = remember { mutableStateOf(setOf<String>()) }

    val displayItems = remember(catalog, mockupCache, previewCache, storefrontCache) {
        catalog.map { it.withResolvedImage(mockupCache, previewCache, storefrontCache) }
    }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
            if (!text.isNullOrBlank()) {
                searchDraft = text
                appliedSearch = text
            }
        }
    }

    fun launchVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) { /* ignore */ }
    }

    fun filteredItems(items: List<WearCarouselItem>): List<WearCarouselItem> {
        val q = appliedSearch.trim()
        if (q.isBlank()) return items
        return items.filter {
            it.label?.contains(q, ignoreCase = true) == true ||
                it.productKey?.contains(q, ignoreCase = true) == true
        }
    }

    suspend fun enrichImagesFor(items: List<WearCarouselItem>) = coroutineScope {
        if (ownerId.isBlank() || items.isEmpty()) return@coroutineScope
        val need = items.filter { it.needsWearImageEnrichment(mockupCache, previewCache, storefrontCache) }
        if (need.isEmpty()) return@coroutineScope

        val mockKeys = need.mapNotNull { it.productKey }.distinct()
        val designIds = need.mapNotNull { it.designId }.distinct()
        val handles = need.mapNotNull { it.shopifyHandle }.distinct()

        val mockDeferred = async(Dispatchers.IO) {
            if (mockKeys.isEmpty()) emptyMap()
            else fetchWearProductMockups(api, ownerId, mockKeys)
        }
        val previewDeferred = async(Dispatchers.IO) {
            if (designIds.isEmpty()) emptyMap()
            else fetchWearDesignPreviews(api, ownerId, designIds)
        }
        val storefrontDeferred = async(Dispatchers.IO) {
            if (handles.isEmpty()) emptyMap()
            else withContext(Dispatchers.IO) {
                handles.map { handle ->
                    async {
                        val url = fetchWearStorefrontImage(handle)
                        if (url != null) handle to url else null
                    }
                }.awaitAll().filterNotNull().toMap()
            }
        }

        val mockLoaded = mockDeferred.await()
        val previewLoaded = previewDeferred.await()
        val storefrontLoaded = storefrontDeferred.await()

        if (mockLoaded.isNotEmpty()) mockupCache = mockupCache + mockLoaded
        if (previewLoaded.isNotEmpty()) previewCache = previewCache + previewLoaded
        if (storefrontLoaded.isNotEmpty()) storefrontCache = storefrontCache + storefrontLoaded
    }

    fun prefetchAround(index: Int, items: List<WearCarouselItem>) {
        if (ownerId.isBlank()) return
        val filtered = filteredItems(items)
        if (filtered.isEmpty()) return
        val safe = index.coerceIn(0, filtered.lastIndex)
        val window = filtered.drop(safe).take(WEAR_PRODUCT_MOCKUP_PREFETCH)
        val token = window.joinToString("|") { "${it.productKey}:${it.designId}:${it.shopifyHandle}" }
        if (fetchingKeys.value.contains(token)) return
        fetchingKeys.value = fetchingKeys.value + token
        scope.launch {
            try {
                enrichImagesFor(window)
            } catch (_: Exception) { /* ignore */ }
            finally {
                fetchingKeys.value = fetchingKeys.value - token
            }
        }
    }

    LaunchedEffect(ownerId, refreshKey) {
        if (ownerId.isBlank()) {
            loading = false
            catalog = emptyList()
            return@LaunchedEffect
        }
        loading = true
        mockupCache = emptyMap()
        previewCache = emptyMap()
        storefrontCache = emptyMap()
        fetchingKeys.value = emptySet()
        carouselIndex = 0
        try {
            catalog = withContext(Dispatchers.IO) { loadWearProductCatalog(api, ownerId) }
            enrichImagesFor(catalog.take(WEAR_PRODUCT_MOCKUP_PREFETCH))
        } catch (_: Exception) {
            catalog = emptyList()
        }
        loading = false
    }

    LaunchedEffect(carouselIndex, appliedSearch, catalog.size, loading) {
        if (!loading && catalog.isNotEmpty()) {
            prefetchAround(carouselIndex, displayItems)
        }
    }

    WearCarouselScreen(
        items = displayItems,
        loading = loading,
        emptyText = translationStore.t("wear.no_products", "No products yet"),
        searchText = searchDraft,
        onSearchTextChange = { searchDraft = it },
        onSearchSubmit = { appliedSearch = searchDraft.trim() },
        filterQuery = appliedSearch,
        onVoiceSearch = { launchVoice() },
        searchPlaceholder = translationStore.t("wear.search_short", "Search…"),
        showSearch = true,
        productImageMode = true,
        initialCarouselIndex = carouselIndex,
        onPageIndexChanged = { index, _ ->
            carouselIndex = index
            prefetchAround(index, displayItems)
        },
        modifier = modifier,
    )
}
