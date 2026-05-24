package com.eazpire.creator.wear

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

private val WearEazColors = Colors(
    primary = EazColors.Orange,
    primaryVariant = EazColors.Orange,
    secondary = EazColors.Orange,
    secondaryVariant = EazColors.Orange,
    background = EazColors.CreatorBg,
    surface = EazColors.CreatorSurface,
    error = androidx.compose.ui.graphics.Color(0xFFB91C1C),
    onPrimary = EazColors.TextPrimary,
    onSecondary = EazColors.TextPrimary,
    onBackground = EazColors.TextPrimary,
    onSurface = EazColors.TextPrimary,
    onError = EazColors.TextPrimary,
)

@Composable
fun WearEazTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = WearEazColors, content = content)
}
