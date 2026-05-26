package com.eazpire.creator.wear.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    var ownerId by remember { mutableStateOf(tokenStore.getOwnerId().orEmpty()) }
    val api = remember(tokenStore) { CreatorApi(jwt = tokenStore.getJwt()) }
    val scope = rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(tokenStore, refreshKey) {
        ownerId = tokenStore.getOwnerId().orEmpty()
    }

    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var catalog by remember { mutableStateOf<List<WearCarouselItem>>(emptyList()) }
    var mockupCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var storefrontCache by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var searchDraft by remember { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    val fetchingKeys = remember { mutableStateOf(setOf<String>()) }

    val displayItems = remember(catalog, mockupCache, storefrontCache) {
        catalog.map { it.withResolvedImage(mockupCache, storefrontCache = storefrontCache) }
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
        val need = items.filter { it.needsWearImageEnrichment(mockupCache, emptyMap(), storefrontCache) }
        if (need.isEmpty()) return@coroutineScope

        val mockKeys = need.mapNotNull { it.productKey }.distinct()
        val handles = need.mapNotNull { it.shopifyHandle }.distinct().take(6)

        val mockDeferred = async(Dispatchers.IO) {
            if (mockKeys.isEmpty()) emptyMap()
            else fetchWearProductMockups(api, ownerId, mockKeys)
        }
        val storefrontDeferred = async(Dispatchers.IO) {
            if (handles.isEmpty()) {
                emptyMap()
            } else {
                coroutineScope {
                    handles.map { handle ->
                        async {
                            val url = fetchWearStorefrontImage(handle)
                            if (url != null) handle to url else null
                        }
                    }.awaitAll().filterNotNull().toMap()
                }
            }
        }

        val mockLoaded = mockDeferred.await()
        val storefrontLoaded = storefrontDeferred.await()

        if (mockLoaded.isNotEmpty()) mockupCache = mockupCache + mockLoaded
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
        storefrontCache = emptyMap()
        fetchingKeys.value = emptySet()
        loadError = null
        try {
            val loaded = withContext(Dispatchers.IO) { loadWearProductCatalog(api, ownerId) }
            catalog = loaded
            if (loaded.isEmpty()) {
                val probe = withContext(Dispatchers.IO) {
                    api.getPublishedProducts(ownerId, limit = 1, wearFast = true)
                }
                if (!probe.optBoolean("ok", false)) {
                    loadError = probe.optString("error", "load_failed")
                }
            }
            val initial = catalog.take(WEAR_PRODUCT_MOCKUP_PREFETCH)
            loading = false
            if (initial.isNotEmpty()) {
                enrichImagesFor(initial)
            }
        } catch (e: Exception) {
            catalog = emptyList()
            loadError = e.message ?: "load_failed"
            loading = false
        }
    }

    WearCarouselScreen(
        items = displayItems,
        loading = loading,
        emptyText = loadError?.let {
            translationStore.t("wear.products_load_error", "Could not load products")
        } ?: translationStore.t("wear.no_products", "No products yet"),
        searchText = searchDraft,
        onSearchTextChange = { searchDraft = it },
        onSearchSubmit = { appliedSearch = searchDraft.trim() },
        filterQuery = appliedSearch,
        onVoiceSearch = { launchVoice() },
        searchPlaceholder = translationStore.t("wear.search_short", "Search…"),
        showSearch = true,
        productImageMode = true,
        onPageIndexChanged = { index, _ -> prefetchAround(index, displayItems) },
        modifier = modifier,
    )
}
