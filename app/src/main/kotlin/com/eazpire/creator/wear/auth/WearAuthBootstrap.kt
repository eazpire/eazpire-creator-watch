package com.eazpire.creator.wear.auth

import android.content.Context
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.auth.WearAuthPaths
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * On launch, read latest auth DataItem from phone if present.
 */
suspend fun bootstrapAuthFromPhone(context: Context, tokenStore: SecureTokenStore) {
    if (tokenStore.isLoggedIn()) return
    try {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        if (nodes.isEmpty()) return
        val uri = android.net.Uri.Builder()
            .scheme("wear")
            .path(WearAuthPaths.DATA_PATH)
            .build()
        Wearable.getDataClient(context).getDataItems(uri).await().use { buffer ->
            for (i in 0 until buffer.count) {
                val item = buffer[i]
                val map = com.google.android.gms.wearable.DataMapItem.fromDataItem(item).dataMap
                val json = map.getString("payload") ?: continue
                WearAuthListenerService.applyPayload(tokenStore, json)
                if (tokenStore.isLoggedIn()) break
            }
        }
    } catch (_: Exception) {
    }
}
