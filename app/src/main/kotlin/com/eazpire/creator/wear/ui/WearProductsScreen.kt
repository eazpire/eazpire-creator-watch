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
    var searchDraft by remember { mutableStateOf("") }
    var appliedSearch by remember { mutableStateOf("") }
    val fetchingKeys = remember { mutableStateOf(setOf<String>()) }

    val displayItems = remember(catalog, mockupCache) {
        catalog.map { it.withMockupCache(mockupCache) }
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

    fun prefetchMockupsAround(index: Int, items: List<WearCarouselItem>) {
        if (ownerId.isBlank() || items.isEmpty()) return
        val q = appliedSearch.trim()
        val filtered = if (q.isBlank()) items else items.filter {
            it.label?.contains(q, ignoreCase = true) == true ||
                it.productKey?.contains(q, ignoreCase = true) == true
        }
        if (filtered.isEmpty()) return
        val safe = index.coerceIn(0, filtered.lastIndex)
        val keys = filtered.drop(safe).take(WEAR_PRODUCT_MOCKUP_PREFETCH)
            .mapNotNull { it.productKey }
            .filter { pk ->
                val row = filtered.find { it.productKey == pk }
                row?.imageUrl.isNullOrBlank() && !mockupCache.containsKey(pk) && !fetchingKeys.value.contains(pk)
            }
        if (keys.isEmpty()) return
        fetchingKeys.value = fetchingKeys.value + keys
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) {
                    fetchWearProductMockups(api, ownerId, keys)
                }
                if (loaded.isNotEmpty()) {
                    mockupCache = mockupCache + loaded
                }
            } catch (_: Exception) { /* ignore */ }
            finally {
                fetchingKeys.value = fetchingKeys.value - keys.toSet()
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
        try {
            catalog = withContext(Dispatchers.IO) { loadWearProductCatalog(api, ownerId) }
            mockupCache = emptyMap()
            fetchingKeys.value = emptySet()
        } catch (_: Exception) {
            catalog = emptyList()
        }
        loading = false
    }

    LaunchedEffect(catalog.size, appliedSearch) {
        if (catalog.isNotEmpty()) prefetchMockupsAround(0, displayItems)
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
        onPageIndexChanged = { index, _ -> prefetchMockupsAround(index, displayItems) },
        modifier = modifier,
    )
}
