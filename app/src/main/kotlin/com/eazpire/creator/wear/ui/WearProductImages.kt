package com.eazpire.creator.wear.ui

import com.eazpire.creator.core.api.CreatorApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal const val WEAR_PRODUCT_MOCKUP_PREFETCH = 10
internal const val WEAR_PRODUCT_MOCKUP_CHUNK = 30
internal const val WEAR_PRODUCT_PREVIEW_CHUNK = 40

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
    if (lower.contains("/file/http") || lower.contains("/file/https%3a")) return false
    if (!lower.startsWith("https://")) return false
    return true
}

internal fun convertWearShopifyImageUrl(shopifyUrl: String?): String? {
    val raw = normalizeWearImageUrl(shopifyUrl) ?: return null
    val lower = raw.lowercase()
    if (lower.contains("www.eazpire.com") || lower.contains("eazpire.com/cdn")) return raw
    if (!lower.contains("cdn.shopify.com")) return raw
    return try {
        val path = URL(raw).path.split("/").filter { it.isNotEmpty() }
        val filesIdx = path.indexOfLast { it == "files" }
        if (filesIdx < 0 || filesIdx >= path.lastIndex) return raw
        val filename = path[filesIdx + 1]
        val version = URL(raw).query?.split("&")?.firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
        val base = "https://www.eazpire.com/cdn/shop/files/$filename"
        if (!version.isNullOrBlank()) "$base?v=$version&width=512" else "$base?width=512"
    } catch (_: Exception) {
        raw
    }
}

internal fun resolvePublishedProductImage(obj: JSONObject): String? {
    fun fromAny(v: Any?): String? = when (v) {
        is String -> v.takeIf { it.isNotBlank() }
        is JSONObject -> v.optString("src", "").takeIf { it.isNotBlank() }
            ?: v.optString("url", "").takeIf { it.isNotBlank() }
        else -> null
    }
    val mockup = fromAny(obj.opt("mockup_image"))
    val preview = fromAny(obj.opt("preview_image"))
    val featured = fromAny(obj.opt("featured_image"))
    val resolved = normalizeWearImageUrl(
        mockup
            ?: preview
            ?: convertWearShopifyImageUrl(featured)
            ?: fromAny(obj.opt("image_url"))
            ?: fromAny(obj.opt("preview_url"))
            ?: fromAny(obj.opt("thumbnail_url"))
            ?: obj.optJSONArray("images")?.let { arr ->
                if (arr.length() > 0) convertWearShopifyImageUrl(fromAny(arr.opt(0))) else null
            },
    )
    return resolved?.takeIf { isWearLoadableImageUrl(it) }
}

internal fun firstDesignIdFromProduct(obj: JSONObject): String? {
    val arr = obj.optJSONArray("design_ids") ?: return null
    for (i in 0 until arr.length()) {
        val id = arr.opt(i)?.toString()?.trim().orEmpty()
        if (id.isNotBlank() && id != "null") return id
    }
    return null
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
                    designId = firstDesignIdFromProduct(o),
                    shopifyHandle = o.optString("shopify_handle", "").trim().takeIf { it.isNotBlank() },
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
        val keysParam = chunk.joinToString(",")
        val mockRes = api.getProductsByKeys(ownerId, keysParam)
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

internal suspend fun fetchWearDesignPreviews(
    api: CreatorApi,
    ownerId: String,
    designIds: List<String>,
): Map<String, String> = withContext(Dispatchers.IO) {
    val ids = designIds.filter { it.isNotBlank() }.distinct()
    if (ids.isEmpty()) return@withContext emptyMap()
    val map = mutableMapOf<String, String>()
    for (chunk in ids.chunked(WEAR_PRODUCT_PREVIEW_CHUNK)) {
        val res = api.getDesignPreviews(ownerId, chunk.joinToString(","))
        if (!res.optBoolean("ok", false)) continue
        val previews = res.optJSONObject("previews") ?: continue
        for (id in chunk) {
            val url = normalizeWearImageUrl(previews.optString(id, ""))
            if (isWearLoadableImageUrl(url)) map[id] = url!!
        }
    }
    map
}

internal suspend fun fetchWearStorefrontImage(handle: String): String? = withContext(Dispatchers.IO) {
    val h = handle.trim()
    if (h.isBlank()) return@withContext null
    try {
        val conn = URL("https://www.eazpire.com/products/$h.json").openConnection() as HttpURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 12_000
        conn.requestMethod = "GET"
        if (conn.responseCode !in 200..299) return@withContext null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        val product = JSONObject(body).optJSONObject("product") ?: return@withContext null
        val raw = product.optString("featured_image", "").takeIf { it.isNotBlank() }
            ?: product.optJSONArray("images")?.let { arr ->
                if (arr.length() > 0) arr.optJSONObject(0)?.optString("src", "") else null
            }
        val converted = convertWearShopifyImageUrl(raw)
        converted?.takeIf { isWearLoadableImageUrl(it) }
    } catch (_: Exception) {
        null
    }
}

internal fun WearCarouselItem.resolvedProductImage(
    mockupCache: Map<String, String>,
    previewCache: Map<String, String> = emptyMap(),
    storefrontCache: Map<String, String> = emptyMap(),
): String? {
    val pk = productKey?.trim().orEmpty()
    if (pk.isNotBlank()) {
        val mock = mockupCache[pk]
        if (isWearLoadableImageUrl(mock)) return normalizeWearImageUrl(mock)
    }
    val did = designId?.trim().orEmpty()
    if (did.isNotBlank()) {
        val preview = previewCache[did]
        if (isWearLoadableImageUrl(preview)) return normalizeWearImageUrl(preview)
    }
    val handle = shopifyHandle?.trim().orEmpty()
    if (handle.isNotBlank()) {
        val sf = storefrontCache[handle]
        if (isWearLoadableImageUrl(sf)) return normalizeWearImageUrl(sf)
    }
    return imageUrl?.takeIf { isWearLoadableImageUrl(it) }
}

internal fun WearCarouselItem.withResolvedImage(
    mockupCache: Map<String, String>,
    previewCache: Map<String, String> = emptyMap(),
    storefrontCache: Map<String, String> = emptyMap(),
): WearCarouselItem {
    val url = resolvedProductImage(mockupCache, previewCache, storefrontCache)
    return if (url == imageUrl) this else copy(imageUrl = url)
}

internal fun WearCarouselItem.needsWearImageEnrichment(
    mockupCache: Map<String, String>,
    previewCache: Map<String, String>,
    storefrontCache: Map<String, String>,
): Boolean = !isWearLoadableImageUrl(resolvedProductImage(mockupCache, previewCache, storefrontCache))
