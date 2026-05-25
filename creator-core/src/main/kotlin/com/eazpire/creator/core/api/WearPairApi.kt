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
 * Wear login via QR: watch creates session and polls; phone claims with JWT.
 */
class WearPairApi(
    private val baseUrl: String = AuthConfig.CREATOR_ENGINE_URL,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun createSession(deviceId: String, deviceName: String?): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("device_id", deviceId)
        if (!deviceName.isNullOrBlank()) body.put("device_name", deviceName)
        postJson("$baseUrl/api/wear-pair/session", body.toString())
    }

    suspend fun pollSession(token: String): JSONObject = withContext(Dispatchers.IO) {
        getJson("$baseUrl/api/wear-pair/session?token=${enc(token)}")
    }

    fun qrImageUrl(token: String): String =
        "$baseUrl/api/wear-pair/qr-image?token=${enc(token)}"

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
