package com.eazpire.creator.wear.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.eazpire.creator.wear.EazColors
import kotlin.math.abs

data class WearCarouselItem(
    val imageUrl: String?,
    val label: String? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearCarouselScreen(
    items: List<WearCarouselItem>,
    loading: Boolean,
    emptyText: String,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onVoiceSearch: () -> Unit = {},
    searchPlaceholder: String = "Search…",
    showSearch: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var index by remember(items, searchQuery) { mutableIntStateOf(0) }
    val filtered = remember(items, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) items
        else items.filter {
            it.label?.contains(q, ignoreCase = true) == true ||
                it.imageUrl?.contains(q, ignoreCase = true) == true
        }
    }
    val total = filtered.size
    val safeIndex = index.coerceIn(0, (total - 1).coerceAtLeast(0))
    if (safeIndex != index) index = safeIndex
    val current = filtered.getOrNull(safeIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .wearRoundSafePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showSearch) {
            WearSearchBar(
                query = searchQuery,
                onQueryChange = {
                    onSearchQueryChange(it)
                    index = 0
                },
                onVoiceClick = onVoiceSearch,
                placeholder = searchPlaceholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )
        }

        when {
            loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            total == 0 -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(12.dp),
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(total, safeIndex) {
                            detectHorizontalDragGestures { _, drag ->
                                if (abs(drag) < 28f) return@detectHorizontalDragGestures
                                if (drag < 0 && safeIndex < total - 1) index++
                                if (drag > 0 && safeIndex > 0) index--
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    val url = current?.imageUrl
                    if (!url.isNullOrBlank()) {
                        AsyncImage(
                            model = url,
                            contentDescription = current.label,
                            modifier = Modifier.size(88.dp),
                            contentScale = ContentScale.Fit,
                        )
                    } else {
                        Text(
                            text = current?.label ?: "—",
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                WearPageDots(pageCount = total, currentPage = safeIndex)
                Text(
                    text = "${safeIndex + 1}/$total",
                    style = MaterialTheme.typography.caption2,
                    color = EazColors.TextPrimary.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
            }
        }
    }
}
