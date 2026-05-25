package com.eazpire.creator.wear.ui

import com.eazpire.creator.core.api.CreatorApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal const val WEAR_PRODUCT_MOCKUP_PREFETCH = 10
internal const val WEAR_PRODUCT_MOCKUP_CHUNK = 40

internal fun normalizeWearImageUrl(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (trimmed.equals("null", ignoreCase = true)) return null
    return when {
        trimmed.startsWith("//") -> "https:$trimmed"
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        else -> null
    }
}

/** Reject broken /file/https%3A… double-encoded URLs from legacy API responses. */
internal fun isWearLoadableImageUrl(url: String?): Boolean {
    val u = normalizeWearImageUrl(url) ?: return false
    val lower = u.lowercase()
    if (lower.contains("%3a%2f%2f") || lower.contains("%2f%2fhttps")) return false
    if (lower.contains("/file/http")) return false
    return lower.startsWith("https://") && (
        lower.contains("cdn.shopify.com") ||
        lower.contains("eazpire.com") ||
        lower.contains("workers.dev") ||
        lower.contains("r2.dev") ||
        lower.contains("/file/")
    )
}

internal fun resolvePublishedProductImage(obj: JSONObject): String? {
    fun fromAny(v: Any?): String? = when (v) {
        is String -> v.takeIf { it.isNotBlank() }
        is JSONObject -> v.optString("src", "").takeIf { it.isNotBlank() }
            ?: v.optString("url", "").takeIf { it.isNotBlank() }
        else -> null
    }
    val mockup = fromAny(obj.opt("mockup_image"))
    val featured = fromAny(obj.opt("featured_image"))
    val resolved = normalizeWearImageUrl(
        mockup
            ?: featured
            ?: fromAny(obj.opt("image_url"))
            ?: fromAny(obj.opt("preview_url"))
            ?: fromAny(obj.opt("thumbnail_url"))
            ?: obj.optJSONArray("images")?.let { arr ->
                if (arr.length() > 0) fromAny(arr.opt(0)) else null
            },
    )
    return resolved?.takeIf { isWearLoadableImageUrl(it) }
}

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
                    label = name.takeIf { it.isNotBlank() } ?: key.takeIf { it.isNotBlank() },
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
    for (chunk in keys.chunked(WEAR_PRODUCT_MOCKUP_CHUNK)) {
        val mockRes = api.getProductsByKeysPost(ownerId, chunk)
        if (!mockRes.optBoolean("ok", false)) continue
        val mockArr = mockRes.optJSONArray("products") ?: JSONArray()
        for (i in 0 until mockArr.length()) {
            val m = mockArr.optJSONObject(i) ?: continue
            val pk = m.optString("product_key", "").trim()
            val url = normalizeWearImageUrl(m.optString("image_url", ""))
            if (pk.isNotBlank() && isWearLoadableImageUrl(url)) map[pk] = url!!
        }
    }
    map
}

internal fun WearCarouselItem.resolvedProductImage(mockupCache: Map<String, String>): String? {
    val pk = productKey?.trim().orEmpty()
    if (pk.isNotBlank()) {
        val mock = mockupCache[pk]
        if (isWearLoadableImageUrl(mock)) return normalizeWearImageUrl(mock)
    }
    return imageUrl?.takeIf { isWearLoadableImageUrl(it) }
}

internal fun WearCarouselItem.withResolvedImage(mockupCache: Map<String, String>): WearCarouselItem {
    val url = resolvedProductImage(mockupCache)
    return if (url == imageUrl) this else copy(imageUrl = url)
}
