package com.eazpire.creator.core.api

import com.eazpire.creator.core.auth.AuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Phone reference upload via QR — mirrors web [creator-phone-upload-modal.js].
 */
class CreatorPhoneUploadApi(
    private val baseUrl: String = AuthConfig.CREATOR_ENGINE_URL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun getConfig(): JSONObject = withContext(Dispatchers.IO) {
        getJson("$baseUrl/api/creator-phone-upload/config")
    }

    suspend fun createSession(ownerId: String): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject().put("owner_id", ownerId).toString()
        postJson("$baseUrl/api/creator-phone-upload/session", body)
    }

    suspend fun pollSession(sessionId: String, ownerId: String): JSONObject = withContext(Dispatchers.IO) {
        val url =
            "$baseUrl/api/creator-phone-upload/session?id=${enc(sessionId)}&owner_id=${enc(ownerId)}"
        getJson(url)
    }

    fun qrImageUrl(sessionId: String): String =
        "$baseUrl/api/creator-phone-upload/qr-image?session=${enc(sessionId)}"

    /** Plain QR fallback — same as web [creator-phone-upload-modal.js] qrCodeUrlForScan. */
    fun qrFallbackUrl(scanUrl: String): String =
        "https://api.qrserver.com/v1/create-qr-code/?size=220x220&margin=1&qzone=1&data=${enc(scanUrl)}"

    private fun enc(v: String): String = java.net.URLEncoder.encode(v, "UTF-8")

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "{}")
    }

    private fun postJson(url: String, jsonBody: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toRequestBody(jsonType))
            .build()
        val response = client.newCall(request).execute()
        return JSONObject(response.body?.string() ?: "{}")
    }
}
