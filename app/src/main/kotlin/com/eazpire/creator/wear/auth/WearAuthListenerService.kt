package com.eazpire.creator.wear.auth

import android.content.Intent
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.auth.WearAuthPaths
import com.eazpire.creator.core.auth.WearSessionGate
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/**
 * Receives JWT + owner_id from phone app via Data Layer.
 */
class WearAuthListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val tokenStore = SecureTokenStore(applicationContext)
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if (!item.uri.path.orEmpty().startsWith(WearAuthPaths.DATA_PATH)) continue
            val map = DataMapItem.fromDataItem(item).dataMap
            val json = map.getString("payload") ?: continue
            applyPayload(applicationContext, tokenStore, json)
            sendBroadcast(Intent(ACTION_AUTH_CHANGED).setPackage(packageName))
        }
    }

    companion object {
        const val ACTION_AUTH_CHANGED = "com.eazpire.creator.wear.AUTH_CHANGED"

        fun applyPayload(context: android.content.Context, tokenStore: SecureTokenStore, json: String) {
            val jo = try {
                JSONObject(json)
            } catch (_: Exception) {
                return
            }
            val jwt = jo.optString("jwt", "").trim()
            val ownerId = jo.optString("owner_id", "").trim()
            if (jwt.isBlank() || ownerId.isBlank()) {
                WearSessionGate.clearSession(context, tokenStore)
            } else {
                tokenStore.saveJwt(jwt, ownerId)
                WearSessionGate.markSessionReady(context)
            }
        }
    }
}
