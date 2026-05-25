package com.eazpire.creator.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eazpire.creator.core.api.CreatorApi
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.i18n.WearTranslationStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

@Composable
fun WearProductsScreen(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    refreshKey: Int,
    modifier: Modifier = Modifier,
) {
    val ownerId = remember(tokenStore) { tokenStore.getOwnerId().orEmpty() }
    val api = remember(tokenStore) { CreatorApi(jwt = tokenStore.getJwt()) }
    var loading by remember { mutableStateOf(true) }
    var items by remember { mutableStateOf<List<WearCarouselItem>>(emptyList()) }

    LaunchedEffect(ownerId, refreshKey) {
        if (ownerId.isBlank()) {
            loading = false
            items = emptyList()
            return@LaunchedEffect
        }
        loading = true
        try {
            val list = withContext(Dispatchers.IO) {
                val res = api.getPublishedProducts(ownerId)
                if (!res.optBoolean("ok", false)) return@withContext emptyList()
                val arr: JSONArray = res.optJSONArray("products") ?: JSONArray()
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val img = o.optString("featured_image", "").takeIf { it.isNotBlank() }
                        val name = o.optString("product_name", "").takeIf { it.isNotBlank() }
                        add(WearCarouselItem(imageUrl = img, label = name))
                    }
                }
            }
            items = list
        } catch (_: Exception) {
            items = emptyList()
        }
        loading = false
    }

    WearCarouselScreen(
        items = items,
        loading = loading,
        emptyText = translationStore.t("wear.no_products", "No products yet"),
        modifier = modifier,
    )
}
