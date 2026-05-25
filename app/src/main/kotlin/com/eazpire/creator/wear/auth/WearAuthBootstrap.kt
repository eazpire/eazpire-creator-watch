package com.eazpire.creator.wear.auth

import android.content.Context
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.auth.WearAuthPaths
import com.eazpire.creator.core.auth.WearSessionGate
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

data class WearBootstrapResult(
    val connectedNodes: Int,
    val loggedInAfter: Boolean,
)

/**
 * On launch / retry, read latest auth DataItem from a connected phone if present.
 */
suspend fun bootstrapAuthFromPhone(context: Context, tokenStore: SecureTokenStore): WearBootstrapResult {
    WearSessionGate.ensureSchema(context, tokenStore)
    if (WearSessionGate.isSessionReady(context, tokenStore)) {
        return WearBootstrapResult(connectedNodes = -1, loggedInAfter = true)
    }
    return try {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) {
            return WearBootstrapResult(connectedNodes = 0, loggedInAfter = false)
        }
        val uri = android.net.Uri.Builder()
            .scheme("wear")
            .path(WearAuthPaths.DATA_PATH)
            .build()
        Wearable.getDataClient(context).getDataItems(uri).await().use { buffer ->
            for (i in 0 until buffer.count) {
                val item = buffer[i]
                val map = DataMapItem.fromDataItem(item).dataMap
                val json = map.getString("payload") ?: continue
                WearAuthListenerService.applyPayload(context, tokenStore, json)
                if (tokenStore.isLoggedIn()) break
            }
        }
        if (tokenStore.isLoggedIn()) {
            WearSessionGate.markSessionReady(context)
        }
        WearBootstrapResult(
            connectedNodes = nodes.size,
            loggedInAfter = WearSessionGate.isSessionReady(context, tokenStore),
        )
    } catch (_: Exception) {
        WearBootstrapResult(connectedNodes = 0, loggedInAfter = false)
    }
}
