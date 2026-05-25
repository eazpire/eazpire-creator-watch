package com.eazpire.creator.wear.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import coil.compose.AsyncImage
import com.eazpire.creator.core.api.WearPairApi
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.device.WearDeviceId
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed class PairUiState {
    data object Loading : PairUiState()
    data class Qr(val token: String, val qrUrl: String) : PairUiState()
    data class Error(val message: String) : PairUiState()
}

@Composable
fun WearPairingScreen(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    connectionStatus: String,
    onRetrySync: () -> Unit,
    onPaired: () -> Unit,
    onDemoPreview: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pairApi = remember { WearPairApi() }
    val deviceId = remember { WearDeviceId.get(context) }
    val deviceName = remember {
        listOfNotNull(Build.MANUFACTURER, Build.MODEL)
            .joinToString(" ")
            .trim()
            .ifBlank { "Wear OS" }
    }
    var state by remember { mutableStateOf<PairUiState>(PairUiState.Loading) }

    LaunchedEffect(deviceId) {
        state = PairUiState.Loading
        try {
            val session = withContext(Dispatchers.IO) {
                pairApi.createSession(deviceId, deviceName)
            }
            if (!session.optBoolean("ok", false)) {
                state = PairUiState.Error(session.optString("error", "session_failed"))
                return@LaunchedEffect
            }
            val token = session.optString("token", "").trim()
            if (token.isBlank()) {
                state = PairUiState.Error("missing_token")
                return@LaunchedEffect
            }
            val qrUrl = pairApi.qrImageUrl(token)
            state = PairUiState.Qr(token, qrUrl)

            while (true) {
                delay(2500)
                val poll = withContext(Dispatchers.IO) { pairApi.pollSession(token) }
                val status = poll.optString("status", "")
                if (status == "claimed") {
                    val jwt = poll.optString("jwt", "").trim()
                    val ownerId = poll.optString("owner_id", "").trim()
                    if (jwt.isNotBlank() && ownerId.isNotBlank()) {
                        tokenStore.saveJwt(jwt, ownerId)
                        onPaired()
                    }
                    break
                }
                if (status == "expired") {
                    state = PairUiState.Error("expired")
                    break
                }
            }
        } catch (e: Exception) {
            state = PairUiState.Error(e.message ?: "error")
        }
    }

    ScalingLazyColumn(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = translationStore.t("wear.pair_title", "Pair with phone"),
                style = MaterialTheme.typography.title2,
                color = EazColors.Orange,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val s = state) {
            PairUiState.Loading -> item { CircularProgressIndicator() }
            is PairUiState.Qr -> {
                item {
                    Text(
                        text = translationStore.t(
                            "wear.pair_qr_hint",
                            "Creator Settings → Creator Wear → Connect, then scan this QR.",
                        ),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    AsyncImage(
                        model = s.qrUrl,
                        contentDescription = "Pairing QR",
                        modifier = Modifier.size(130.dp),
                        contentScale = ContentScale.Fit,
                    )
                }
            }
            is PairUiState.Error -> item {
                Text(
                    text = translationStore.t("wear.pair_qr_error", "Could not start pairing") +
                        "\n" + s.message,
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(
                text = translationStore.t(
                    "wear.pair_sync_hint",
                    "Or log in on the phone app — session may sync automatically.",
                ),
                style = MaterialTheme.typography.caption2,
                color = EazColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (connectionStatus.isNotBlank()) {
            item {
                Text(
                    text = connectionStatus,
                    style = MaterialTheme.typography.caption1,
                    color = EazColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Chip(
                onClick = onRetrySync,
                label = {
                    Text(
                        translationStore.t("wear.retry_sync", "Retry sync"),
                        maxLines = 1,
                    )
                },
                colors = ChipDefaults.primaryChipColors(
                    backgroundColor = EazColors.Orange,
                    contentColor = EazColors.TextPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onDemoPreview != null) {
            item {
                Chip(
                    onClick = onDemoPreview,
                    label = {
                        Text(
                            translationStore.t("wear.demo_preview", "Preview app (emulator)"),
                            maxLines = 2,
                        )
                    },
                    colors = ChipDefaults.secondaryChipColors(
                        backgroundColor = EazColors.CreatorSurface,
                        contentColor = EazColors.TextPrimary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
