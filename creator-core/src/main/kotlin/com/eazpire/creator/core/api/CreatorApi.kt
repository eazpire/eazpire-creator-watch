package com.eazpire.creator.core.api

import com.eazpire.creator.core.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Slim creator-engine client for Wear MVP (stats, jobs).
 */
class CreatorApi(
    private val baseUrl: String = AuthConfig.CREATOR_ENGINE_URL,
    private val jwt: String? = null
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun call(
        op: String,
        params: Map<String, String> = emptyMap(),
        method: String = "GET"
    ): JSONObject = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$baseUrl/apps/creator-dispatch?op=$op")
            if (method == "GET") append("&_t=${System.currentTimeMillis()}")
            params.forEach { (k, v) ->
                if (v.isNotBlank()) append("&${k}=${java.net.URLEncoder.encode(v, "UTF-8")}")
            }
        }
        val request = Request.Builder()
            .url(url)
            .apply { jwt?.let { addHeader("Authorization", "Bearer $it") } }
            .method(method, if (method == "POST") okhttp3.RequestBody.create(null, byteArrayOf()) else null)
            .build()
        val response = client.newCall(request).execute()
        JSONObject(response.body?.string() ?: "{}")
    }

    suspend fun getDesignSourceCounts(ownerId: String): JSONObject =
        call("get-design-source-counts", mapOf("owner_id" to ownerId))

    suspend fun getPublishStats(ownerId: String): JSONObject =
        call("get-publish-stats", mapOf("owner_id" to ownerId))

    suspend fun getCreatorSales(ownerId: String): JSONObject =
        call("get-creator-sales", mapOf("owner_id" to ownerId))

    suspend fun getCreatorPayoutOverview(ownerId: String, days: Int = 90): JSONObject =
        call("get-creator-payout-overview", mapOf("owner_id" to ownerId, "days" to days.toString()))

    suspend fun listJobs(ownerId: String, limit: Int = 10): JSONObject =
        call("list-jobs", mapOf("owner_id" to ownerId, "limit" to limit.toString()))
}
