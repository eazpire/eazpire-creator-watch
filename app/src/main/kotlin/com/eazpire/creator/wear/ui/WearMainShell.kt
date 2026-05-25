package com.eazpire.creator.wear.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import kotlin.math.abs
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 5
private const val PAGE_JOBS = 4

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearMainShell(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    refreshKey: Int,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    var jobsRefreshNonce by remember { mutableIntStateOf(0) }

    val pageLabels = remember(translationStore) {
        listOf(
            translationStore.t("wear.dashboard", "Dashboard"),
            translationStore.t("wear.generator", "Generator"),
            translationStore.t("wear.designs", "Designs"),
            translationStore.t("wear.products", "Products"),
            translationStore.t("creator.notifications.active_jobs", "Active Jobs"),
        )
    }
    val currentLabel = pageLabels.getOrElse(pagerState.currentPage) { "" }

    fun goToJobs() {
        scope.launch {
            pagerState.animateScrollToPage(PAGE_JOBS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .wearRoundSafePadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(pagerState.currentPage) {
                    detectHorizontalDragGestures { _, drag ->
                        if (abs(drag) < 36f) return@detectHorizontalDragGestures
                        scope.launch {
                            when {
                                drag < 0 && pagerState.currentPage < PAGE_COUNT - 1 ->
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                drag > 0 && pagerState.currentPage > 0 ->
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = currentLabel,
                style = MaterialTheme.typography.caption1,
                color = EazColors.TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp),
            )
            WearPageDots(
                pageCount = PAGE_COUNT,
                currentPage = pagerState.currentPage,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Only mount the visible tab — avoids Designs polling + Products enrichment running together.
            when (pagerState.currentPage) {
                0 -> WearDashboardScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize(),
                )
                1 -> WearGeneratorScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    onGenerationStarted = {
                        jobsRefreshNonce++
                        goToJobs()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                2 -> WearDesignsScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize(),
                )
                3 -> WearProductsScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> WearJobsScreen(
                    tokenStore = tokenStore,
                    translationStore = translationStore,
                    refreshKey = refreshKey + jobsRefreshNonce,
                    activeOnly = true,
                    showTitle = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
