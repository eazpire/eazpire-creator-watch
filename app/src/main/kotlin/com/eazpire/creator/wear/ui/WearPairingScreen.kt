package com.eazpire.creator.wear.ui

import android.os.Build
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
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.eazpire.creator.core.api.WearPairApi
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.auth.WearSessionGate
import com.eazpire.creator.core.device.WearDeviceId
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import com.eazpire.creator.wear.auth.bootstrapAuthFromPhone
import com.eazpire.creator.wear.companion.WearCompanionState
import com.eazpire.creator.wear.companion.resolveWearCompanionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private sealed class PairUiState {
    data object Loading : PairUiState()
    data class Qr(val qrUrl: String) : PairUiState()
    data object Error : PairUiState()
}

/**
 * QR pairing — [ScalingLazyColumn] keeps content in the round safe zone (center band).
 * Long hints belong in short lines below the QR, not in a full-width row at the top.
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
    var companionState by remember { mutableStateOf(WearCompanionState.NoPhoneConnected) }

    val hintLine1 = translationStore.t("wear.pair_hint_line1", "Scan with Eazpire app")
    val hintLine2 = translationStore.t("wear.pair_hint_line2", "Creator Wear → Connect")

    LaunchedEffect(Unit) {
        while (true) {
            if (!WearSessionGate.isSessionReady(context, tokenStore)) {
                bootstrapAuthFromPhone(context, tokenStore)
                if (WearSessionGate.isSessionReady(context, tokenStore)) {
                    onPaired()
                    break
                }
            }
            companionState = resolveWearCompanionState(
                context,
                WearSessionGate.isSessionReady(context, tokenStore),
            )
            delay(3000)
        }
    }

    LaunchedEffect(deviceId, sessionGeneration) {
        state = PairUiState.Loading
        try {
            val session = withContext(Dispatchers.IO) {
                pairApi.createSession(deviceId, deviceName)
            }
            if (!session.optBoolean("ok", false)) {
                state = PairUiState.Error
                delay(4000)
                sessionGeneration++
                return@LaunchedEffect
            }
            val token = session.optString("token", "").trim()
            if (token.isBlank()) {
                state = PairUiState.Error
                delay(4000)
                sessionGeneration++
                return@LaunchedEffect
            }
            val qrUrl = pairApi.qrImageUrl(token)
            state = PairUiState.Qr(qrUrl)

            while (true) {
                delay(2500)
                if (WearSessionGate.isSessionReady(context, tokenStore)) {
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
                            WearSessionGate.markSessionReady(context)
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
        } catch (_: Exception) {
            state = PairUiState.Error
            delay(4000)
            sessionGeneration++
        }
    }

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = WearRoundInsets.contentPadding,
        autoCentering = AutoCenteringParams(itemIndex = 1),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            when (val s = state) {
                PairUiState.Loading -> CircularProgressIndicator()
                PairUiState.Error -> {
                    CircularProgressIndicator()
                    Text(
                        text = translationStore.t("wear.pair_qr_error", "Loading QR…"),
                        style = MaterialTheme.typography.caption3,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                    )
                }
                is PairUiState.Qr -> AsyncImage(
                    model = s.qrUrl,
                    contentDescription = "Pairing QR",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        item {
            Text(
                text = hintLine1,
                style = MaterialTheme.typography.caption2,
                color = EazColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
        item {
            Text(
                text = hintLine2,
                style = MaterialTheme.typography.caption3,
                color = EazColors.TextSecondary.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            val companionHint = when (companionState) {
                WearCompanionState.NoPhoneConnected -> translationStore.t(
                    "wear.pair_need_phone",
                    "Pair an Android phone, then install Eazpire Creator.",
                )
                WearCompanionState.PhoneConnectedAppMissing -> translationStore.t(
                    "wear.pair_need_phone_app",
                    "Install Eazpire Creator on your phone from Play Store.",
                )
                WearCompanionState.PhoneAppInstalledNotLoggedIn -> translationStore.t(
                    "wear.pair_need_phone_login",
                    "Open the phone app and sign in.",
                )
                WearCompanionState.Ready -> translationStore.t(
                    "wear.pair_sync_hint",
                    "Phone app connected — session may sync automatically.",
                )
            }
            Text(
                text = companionHint,
                style = MaterialTheme.typography.caption3,
                color = EazColors.Orange.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
    }
}
