package com.eazpire.creator.wear.ui

import com.eazpire.creator.core.api.CreatorApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal const val WEAR_PRODUCT_MOCKUP_PREFETCH = 10

internal fun normalizeWearImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return if (url.startsWith("//")) "https:$url" else url
}

internal fun resolvePublishedProductImage(obj: JSONObject): String? {
    fun fromAny(v: Any?): String? = when (v) {
        is String -> v.takeIf { it.isNotBlank() }
        is JSONObject -> v.optString("src", "").takeIf { it.isNotBlank() }
            ?: v.optString("url", "").takeIf { it.isNotBlank() }
        else -> null
    }
    val featured = fromAny(obj.opt("featured_image"))
    return normalizeWearImageUrl(
        fromAny(obj.opt("image_url"))
            ?: featured
            ?: fromAny(obj.opt("preview_url"))
            ?: fromAny(obj.opt("thumbnail_url"))
            ?: obj.optJSONArray("images")?.let { arr ->
                if (arr.length() > 0) fromAny(arr.opt(0)) else null
            },
    )
}

/** Catalog only (names + keys + inline Shopify image if present) — no bulk mockup fetch. */
internal suspend fun loadWearProductCatalog(
    api: CreatorApi,
    ownerId: String,
): List<WearCarouselItem> = withContext(Dispatchers.IO) {
    val res = api.getPublishedProducts(ownerId)
    if (!res.optBoolean("ok", false)) return@withContext emptyList()
    val arr: JSONArray = res.optJSONArray("products") ?: JSONArray()
    buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val key = o.optString("product_key", "").trim()
            val name = o.optString("product_name", "").trim()
            if (key.isBlank() && name.isBlank()) continue
            val img = resolvePublishedProductImage(o)
            add(
                WearCarouselItem(
                    imageUrl = img,
                    label = name.takeIf { it.isNotBlank() },
                    productKey = key.takeIf { it.isNotBlank() },
                ),
            )
        }
    }
}

internal suspend fun fetchWearProductMockups(
    api: CreatorApi,
    ownerId: String,
    productKeys: List<String>,
): Map<String, String> = withContext(Dispatchers.IO) {
    val keys = productKeys.filter { it.isNotBlank() }.distinct()
    if (keys.isEmpty()) return@withContext emptyMap()
    val map = mutableMapOf<String, String>()
    for (chunk in keys.chunked(40)) {
        val mockRes = api.getProductsByKeys(ownerId, chunk.joinToString(","))
        if (!mockRes.optBoolean("ok", false)) continue
        val mockArr = mockRes.optJSONArray("products") ?: JSONArray()
        for (i in 0 until mockArr.length()) {
            val m = mockArr.optJSONObject(i) ?: continue
            val pk = m.optString("product_key", "").trim()
            val url = normalizeWearImageUrl(m.optString("image_url", ""))
            if (pk.isNotBlank() && !url.isNullOrBlank()) map[pk] = url
        }
    }
    map
}

internal fun WearCarouselItem.withMockupCache(cache: Map<String, String>): WearCarouselItem {
    if (!imageUrl.isNullOrBlank() || productKey.isNullOrBlank()) return this
    val url = cache[productKey] ?: return this
    return copy(imageUrl = url)
}

/** @deprecated Use catalog + lazy fetchWearProductMockups */
internal suspend fun loadWearProductCarouselItems(
    api: CreatorApi,
    ownerId: String,
): List<WearCarouselItem> {
    val catalog = loadWearProductCatalog(api, ownerId)
    val needKeys = catalog.filter { it.imageUrl.isNullOrBlank() && !it.productKey.isNullOrBlank() }
        .mapNotNull { it.productKey }
        .take(WEAR_PRODUCT_MOCKUP_PREFETCH)
    val mockups = fetchWearProductMockups(api, ownerId, needKeys)
    return catalog.map { it.withMockupCache(mockups) }
}
