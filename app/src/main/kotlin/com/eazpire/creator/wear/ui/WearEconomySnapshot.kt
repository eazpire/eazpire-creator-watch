package com.eazpire.creator.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.eazpire.creator.core.api.CreatorApi
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale
import kotlin.math.round

internal const val EAZ_COIN_LOGO_URL =
    "https://pub-2ffb11d4a361463498b9a842a87a870c.r2.dev/brand/coin/eaz-coin-logo.png"

data class WearEconomySnapshot(
    val walletActive: Boolean = false,
    val eazBalance: Double = 0.0,
    val eazGenerateCost: Double = 10.0,
    val isGenerateFree: Boolean = false,
    val trialGenerateRemaining: Int = 0,
    val trialGenerateCap: Int = 0,
    val trialUploadRemaining: Int = 0,
    val trialUploadCap: Int = 0,
)

suspend fun loadWearEconomySnapshot(
    api: CreatorApi,
    ownerId: String,
): WearEconomySnapshot = withContext(Dispatchers.IO) {
    if (ownerId.isBlank()) return@withContext WearEconomySnapshot()
    val bal = try {
        api.getBalance(ownerId)
    } catch (_: Exception) {
        JSONObject()
    }
    val walletActive = bal.optBoolean("eaz_wallet_active", false)
    val costs = bal.optJSONObject("eaz_costs")
    val features = bal.optJSONObject("eaz_feature_active")
    val costRaw = costs?.opt("design_generate")
    val costNum = when (costRaw) {
        is Number -> costRaw.toDouble()
        is String -> costRaw.toDoubleOrNull()
        else -> null
    }
    val featureOff = features?.optBoolean("design_generate") == false
    val isFree = featureOff || (costNum != null && costNum <= 0.0)
    val eazCost = if (costNum != null && costNum > 0) costNum else 10.0
    WearEconomySnapshot(
        walletActive = walletActive,
        eazBalance = bal.optDouble("balance_total", bal.optDouble("balance_eaz", 0.0)),
        eazGenerateCost = if (isFree) 0.0 else eazCost,
        isGenerateFree = isFree,
        trialGenerateRemaining = bal.optInt("trial_generate_remaining", 0).coerceAtLeast(0),
        trialGenerateCap = bal.optInt("trial_generate_cap", 0).coerceAtLeast(0),
        trialUploadRemaining = bal.optInt("trial_upload_remaining", 0).coerceAtLeast(0),
        trialUploadCap = bal.optInt("trial_upload_cap", 0).coerceAtLeast(0),
    )
}

@Composable
fun rememberWearEconomySnapshot(
    api: CreatorApi,
    ownerId: String,
    refreshKey: Int = 0,
): WearEconomySnapshot {
    var snapshot by remember(ownerId) { mutableStateOf(WearEconomySnapshot()) }
    LaunchedEffect(ownerId, refreshKey) {
        if (ownerId.isBlank()) {
            snapshot = WearEconomySnapshot()
            return@LaunchedEffect
        }
        snapshot = loadWearEconomySnapshot(api, ownerId)
    }
    return snapshot
}

private fun formatEazAmount(value: Double): String {
    val rounded = (round(value * 10.0) / 10.0)
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

@Composable
fun WearEazCoinIcon(modifier: Modifier = Modifier.size(14.dp)) {
    AsyncImage(
        model = EAZ_COIN_LOGO_URL,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

enum class WearEconomyFooterMode { Generate, Upload }

@Composable
fun WearEconomyFooterLine(
    snapshot: WearEconomySnapshot,
    translationStore: WearTranslationStore,
    mode: WearEconomyFooterMode = WearEconomyFooterMode.Generate,
    modifier: Modifier = Modifier,
) {
    val text = if (snapshot.walletActive) {
        "EAZ ${formatEazAmount(snapshot.eazBalance)}"
    } else when (mode) {
        WearEconomyFooterMode.Upload -> {
            val cap = snapshot.trialUploadCap.coerceAtLeast(0)
            val rem = snapshot.trialUploadRemaining.coerceAtLeast(0)
            "Uploads $rem/$cap"
        }
        WearEconomyFooterMode.Generate -> {
            val cap = snapshot.trialGenerateCap.coerceAtLeast(0)
            val rem = snapshot.trialGenerateRemaining.coerceAtLeast(0)
            "Free gens $rem/$cap"
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (snapshot.walletActive) {
            WearEazCoinIcon(modifier = Modifier.size(12.dp))
            Text(
                text = " ",
                style = MaterialTheme.typography.caption2,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.caption2,
            color = EazColors.TextPrimary.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            maxLines = 1,
            fontSize = 10.sp,
        )
    }
}
