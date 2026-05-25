package com.eazpire.creator.wear.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    data class Qr(val qrUrl: String) : PairUiState()
    data class Error(val message: String) : PairUiState()
}

/**
 * Login gate: QR only (no demo / retry chips). Polls until phone claims session or Data Layer sync logs in.
 */
@Composable
fun WearPairingScreen(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    onPaired: () -> Unit,
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
    var sessionGeneration by remember { mutableIntStateOf(0) }

    LaunchedEffect(deviceId, sessionGeneration) {
        state = PairUiState.Loading
        try {
            val session = withContext(Dispatchers.IO) {
                pairApi.createSession(deviceId, deviceName)
            }
            if (!session.optBoolean("ok", false)) {
                state = PairUiState.Error(session.optString("error", "session_failed"))
                delay(4000)
                sessionGeneration++
                return@LaunchedEffect
            }
            val token = session.optString("token", "").trim()
            if (token.isBlank()) {
                state = PairUiState.Error("missing_token")
                delay(4000)
                sessionGeneration++
                return@LaunchedEffect
            }
            val qrUrl = pairApi.qrImageUrl(token)
            state = PairUiState.Qr(qrUrl)

            while (true) {
                delay(2500)
                if (tokenStore.isLoggedIn()) {
                    onPaired()
                    break
                }
                val poll = withContext(Dispatchers.IO) { pairApi.pollSession(token) }
                when (poll.optString("status", "")) {
                    "claimed" -> {
                        val jwt = poll.optString("jwt", "").trim()
                        val ownerId = poll.optString("owner_id", "").trim()
                        if (jwt.isNotBlank() && ownerId.isNotBlank()) {
                            tokenStore.saveJwt(jwt, ownerId)
                            onPaired()
                        }
                        break
                    }
                    "expired" -> {
                        sessionGeneration++
                        break
                    }
                }
            }
        } catch (e: Exception) {
            state = PairUiState.Error(e.message ?: "error")
            delay(4000)
            sessionGeneration++
        }
    }

    ScalingLazyColumn(
        modifier = modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = translationStore.t(
                    "wear.pair_qr_hint",
                    "Log in with the Eazpire app: Creator Settings → Creator Wear → Connect",
                ),
                style = MaterialTheme.typography.caption2,
                color = EazColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val s = state) {
            PairUiState.Loading -> item { CircularProgressIndicator() }
            is PairUiState.Qr -> item {
                AsyncImage(
                    model = s.qrUrl,
                    contentDescription = "Pairing QR",
                    modifier = Modifier.size(150.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            is PairUiState.Error -> item {
                CircularProgressIndicator()
                Text(
                    text = translationStore.t("wear.pair_qr_error", "Could not start pairing"),
                    style = MaterialTheme.typography.caption2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
