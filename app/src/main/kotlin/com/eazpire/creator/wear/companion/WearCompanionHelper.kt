package com.eazpire.creator.wear.companion

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** Must match [android/app/src/main/res/xml/wearable_app_desc.xml] capability name. */
const val PHONE_APP_PACKAGE = "com.eazpire.creator"
const val PHONE_CAPABILITY = "eaz_creator_account_sync"

enum class WearCompanionState {
    NoPhoneConnected,
    PhoneConnectedAppMissing,
    PhoneAppInstalledNotLoggedIn,
    Ready,
}

suspend fun resolveWearCompanionState(context: Context, loggedInOnWatch: Boolean): WearCompanionState {
    val nodes = try {
        Wearable.getNodeClient(context).connectedNodes.await()
    } catch (_: Exception) {
        emptyList()
    }
    if (nodes.isEmpty()) return WearCompanionState.NoPhoneConnected

    val hasCapability = try {
        val info = Wearable.getCapabilityClient(context)
            .getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
        info.nodes.isNotEmpty()
    } catch (_: Exception) {
        false
    }
    if (!hasCapability) return WearCompanionState.PhoneConnectedAppMissing
    if (loggedInOnWatch) return WearCompanionState.Ready
    return WearCompanionState.PhoneAppInstalledNotLoggedIn
}
