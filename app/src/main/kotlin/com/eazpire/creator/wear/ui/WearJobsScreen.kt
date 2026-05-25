package com.eazpire.creator.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import com.eazpire.creator.core.api.CreatorApi
import com.eazpire.creator.core.auth.SecureTokenStore
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class WearJobRow(val title: String, val status: String)

@Composable
fun WearJobsScreen(
    tokenStore: SecureTokenStore,
    translationStore: WearTranslationStore,
    refreshKey: Int,
    showTitle: Boolean = true,
    activeOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val ownerId = remember(tokenStore) { tokenStore.getOwnerId().orEmpty() }
    val api = remember(tokenStore) { CreatorApi(jwt = tokenStore.getJwt()) }
    var loading by remember { mutableStateOf(true) }
    var jobs by remember { mutableStateOf<List<WearJobRow>>(emptyList()) }

    suspend fun loadJobs() {
        if (ownerId.isBlank()) {
            jobs = emptyList()
            return
        }
        val rows = withContext(Dispatchers.IO) {
            val res = api.listJobs(ownerId, limit = 15)
            if (!res.optBoolean("ok", false)) return@withContext emptyList()
            val arr: JSONArray = res.optJSONArray("items") ?: JSONArray()
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val done = o.optBoolean("done", false)
                    val message = o.optString("message", "").trim()
                    val progress = o.optInt("progress", -1)
                    val saving = o.optBoolean("saving", false)
                    if (activeOnly) {
                        if (done) continue
                        val msgLower = message.lowercase()
                        if (msgLower.contains("failed") || msgLower.contains("error")) continue
                    }
                    val prompt = o.optString("prompt", o.optString("final_prompt", "")).take(36)
                    val type = o.optString("type", o.optString("action", "job"))
                    val title = if (prompt.isNotBlank()) prompt else type
                    val statusLine = when {
                        saving && !done -> "Saving…"
                        progress in 0..100 && message.isNotBlank() -> "$message · $progress%"
                        progress in 0..100 -> "$progress%"
                        message.isNotBlank() -> message
                        done -> "Done"
                        else -> "Processing…"
                    }
                    add(WearJobRow(title, statusLine))
                }
            }
        }
        jobs = rows
    }

    LaunchedEffect(ownerId, refreshKey, activeOnly) {
        loading = true
        try {
            loadJobs()
        } catch (_: Exception) {
            jobs = emptyList()
        }
        loading = false
        if (activeOnly) {
            while (isActive) {
                delay(4000)
                try {
                    loadJobs()
                } catch (_: Exception) { /* ignore */ }
            }
        }
    }

    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = WearRoundInsets.contentPadding,
        autoCentering = AutoCenteringParams(itemIndex = 0),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        if (showTitle) {
            item {
                Text(
                    text = translationStore.t("creator.notifications.active_jobs", "Active Jobs"),
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (loading && jobs.isEmpty()) {
            item { CircularProgressIndicator() }
        } else if (jobs.isEmpty()) {
            item {
                Text(
                    text = "No active jobs",
                    style = MaterialTheme.typography.body2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            items(jobs.size) { i ->
                val job = jobs[i]
                Text(
                    text = "${job.title}\n${job.status}",
                    style = MaterialTheme.typography.body2,
                    color = EazColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
