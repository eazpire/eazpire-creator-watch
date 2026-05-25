package com.eazpire.creator.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.eazpire.creator.wear.EazColors
import com.eazpire.creator.wear.R

@Composable
fun WearSplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EazColors.CreatorBg),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.eazpire_creator_logo),
            contentDescription = "Eazpire Creator",
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(horizontal = 20.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
