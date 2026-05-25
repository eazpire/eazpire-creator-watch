package com.eazpire.creator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.creator.wear.EazColors
import kotlin.math.abs

/**
 * Local debug gallery: job loading animations inside a round watch viewport.
 * Enable via [com.eazpire.creator.wear.dev.WearDevFlags.LAUNCH_JOB_ANIM_GALLERY].
 */
@Composable
fun WearJobAnimationTestScreen(
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val styles = remember { WearJobLoaderStyle.entries.toList() }
    var index by remember { mutableIntStateOf(0) }
    val safe = index.coerceIn(0, styles.lastIndex)
    if (safe != index) index = safe
    val style = styles[safe]
    val numberText = "%02d".format(safe + 1)
    val totalText = styles.size.toString()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EazColors.CreatorBg),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.96f)
                .clip(CircleShape)
                .background(EazColors.CreatorBg)
                .pointerInput(styles.size) {
                    detectHorizontalDragGestures { _, drag ->
                        if (abs(drag) < 24f) return@detectHorizontalDragGestures
                        if (drag < 0 && index < styles.lastIndex) index++
                        if (drag > 0 && index > 0) index--
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wearRoundSafePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Loader test",
                    style = MaterialTheme.typography.caption1,
                    color = EazColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                )
                WearGalleryPageProgress(
                    pageCount = styles.size,
                    currentPage = safe,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                    ) {
                        Text(
                            text = numberText,
                            color = EazColors.Orange,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            text = " / $totalText",
                            color = EazColors.TextPrimary.copy(alpha = 0.65f),
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                        )
                    }

                    WearJobLoadingAnimation(
                        style = style,
                        modifier = Modifier
                            .size(80.dp)
                            .padding(vertical = 6.dp),
                    )

                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.caption2,
                        color = EazColors.Orange,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = "swipe",
                        style = MaterialTheme.typography.caption2,
                        color = EazColors.TextPrimary.copy(alpha = 0.45f),
                        fontSize = 9.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "‹",
                        color = EazColors.Orange,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable(enabled = safe > 0) {
                                if (safe > 0) index--
                            }
                            .padding(8.dp),
                    )
                    Text(
                        text = "App",
                        style = MaterialTheme.typography.caption2,
                        color = EazColors.TextPrimary,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EazColors.Orange)
                            .clickable(onClick = onOpenApp)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                    Text(
                        text = "›",
                        color = EazColors.Orange,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .clickable(enabled = safe < styles.lastIndex) {
                                if (safe < styles.lastIndex) index++
                            }
                            .padding(8.dp),
                    )
                }
            }
        }
    }
}
