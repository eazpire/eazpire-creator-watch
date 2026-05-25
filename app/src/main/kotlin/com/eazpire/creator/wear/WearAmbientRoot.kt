package com.eazpire.creator.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.wear.ui.WearSplashScreen

/**
 * Wear OS ambient mode draws a tiny UI strip unless we take over the full display.
 * Interactive mode shows the full app (no widget / complication).
 */
@Composable
fun WearAmbientRoot(
    tokenStore: SecureTokenStore,
    isAmbient: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EazColors.CreatorBg),
    ) {
        if (isAmbient) {
            WearSplashScreen(modifier = Modifier.fillMaxSize())
        } else {
            WearApp(tokenStore = tokenStore)
        }
    }
}
