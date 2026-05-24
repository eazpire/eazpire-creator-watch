package com.eazpire.creator.wear

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.auth.WearAuthListenerService
import com.eazpire.creator.wear.ui.WearDashboardScreen
import com.eazpire.creator.wear.ui.WearJobsScreen
import com.eazpire.creator.wear.ui.WearPairingScreen
import com.eazpire.creator.wear.ui.WearUploadScreen

private enum class WearTab { Dashboard, Jobs, Upload }

@Composable
fun WearApp(tokenStore: SecureTokenStore) {
    val context = LocalContext.current
    val translationStore = remember { WearTranslationStore() }
    var loggedIn by remember { mutableStateOf(tokenStore.isLoggedIn()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var tab by remember { mutableStateOf(WearTab.Dashboard) }

    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
                loggedIn = tokenStore.isLoggedIn()
                refreshKey++
            }
        }
        val filter = android.content.IntentFilter(WearAuthListenerService.ACTION_AUTH_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    if (!loggedIn) {
        WearPairingScreen(translationStore = translationStore, modifier = Modifier.fillMaxSize())
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            WearNavChip(
                label = translationStore.t("wear.dashboard", "Dashboard"),
                selected = tab == WearTab.Dashboard,
                onClick = { tab = WearTab.Dashboard }
            )
            WearNavChip(
                label = translationStore.t("creator.notifications.active_jobs", "Active Jobs"),
                selected = tab == WearTab.Jobs,
                onClick = { tab = WearTab.Jobs }
            )
            WearNavChip(
                label = translationStore.t("wear.upload", "Phone upload"),
                selected = tab == WearTab.Upload,
                onClick = { tab = WearTab.Upload }
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                WearTab.Dashboard -> WearDashboardScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize()
                )
                WearTab.Jobs -> WearJobsScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize()
                )
                WearTab.Upload -> WearUploadScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun WearNavChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        colors = ChipDefaults.chipColors(
            backgroundColor = if (selected) EazColors.Orange else MaterialTheme.colors.surface,
            contentColor = EazColors.TextPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    )
}
