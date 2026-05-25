package com.eazpire.creator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.eazpire.creator.wear.EazColors

@Composable
fun WearPageDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .size(if (active) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) EazColors.Orange else EazColors.TextPrimary.copy(alpha = 0.35f),
                    ),
            )
        }
    }
}

/** Slim bar for galleries with many pages (e.g. 20 loader variants). */
@Composable
fun WearGalleryPageProgress(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val fraction =
        if (pageCount <= 0) 0f
        else ((currentPage + 1).coerceIn(1, pageCount)).toFloat() / pageCount.toFloat()
    Box(
        modifier = modifier
            .fillMaxWidth(0.72f)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(EazColors.TextPrimary.copy(alpha = 0.2f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(RoundedCornerShape(2.dp))
                .background(EazColors.Orange),
        )
    }
}
