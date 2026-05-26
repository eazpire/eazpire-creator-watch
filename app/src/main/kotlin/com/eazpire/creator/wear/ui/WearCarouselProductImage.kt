package com.eazpire.creator.wear.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.SubcomposeAsyncImage

/**
 * Product carousel image: mockup with label fallback when URL missing or Coil fails.
 */
@Composable
fun WearCarouselProductImage(
    imageUrl: String?,
    label: String?,
    modifier: Modifier = Modifier,
) {
    val fallbackLabel = label?.takeIf { it.isNotBlank() } ?: "—"

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (!isWearLoadableImageUrl(imageUrl)) {
            Text(
                text = fallbackLabel,
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            return
        }

        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = fallbackLabel,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            contentScale = ContentScale.Fit,
            loading = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            error = {
                Text(
                    text = fallbackLabel,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            },
            success = { state ->
                Image(
                    painter = state.painter,
                    contentDescription = fallbackLabel,
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.88f),
                    contentScale = ContentScale.Fit,
                )
            },
        )
    }
}
