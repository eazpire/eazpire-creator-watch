package com.eazpire.creator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.eazpire.creator.core.api.CreatorApi
import com.eazpire.creator.core.i18n.WearTranslationStore
import com.eazpire.creator.wear.EazColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private enum class WearDesignActionPhase {
    MENU,
    CONFIRM_DELETE,
    CONFIRM_DEACTIVATE,
    CONFIRM_ACTIVATE,
    PICK_CREATOR,
}

@Composable
fun WearDesignLibraryActionsHost(
    item: WearCarouselItem?,
    activityFilter: String,
    publishedCountByDesignId: Map<String, Int>,
    api: CreatorApi,
    ownerId: String,
    translationStore: WearTranslationStore,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val design = item ?: return
    val designId = design.designId?.trim().orEmpty()
    if (designId.isBlank()) return

    val scope = rememberCoroutineScope()
    var phase by remember(designId) { mutableStateOf(WearDesignActionPhase.MENU) }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var creatorNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedCreator by remember { mutableStateOf<String?>(null) }

    val isActive = activityFilter == "active" || design.libraryStatus == "active"
    val publishedCount = publishedCountByDesignId[designId] ?: 0

    suspend fun loadCreators(): List<String> {
        val res = withContext(Dispatchers.IO) { api.getSettings(ownerId) }
        if (!res.optBoolean("ok", false)) return emptyList()
        val settings = res.optJSONObject("settings") ?: res
        val arr = settings.optJSONArray("creator_names") ?: JSONArray()
        return buildList {
            for (i in 0 until arr.length()) {
                val n = arr.optString(i, "").trim()
                if (n.isNotBlank()) add(n)
            }
        }.distinct()
    }

    suspend fun runDelete() {
        busy = true
        errorText = null
        try {
            val res = withContext(Dispatchers.IO) { api.deleteDesign(ownerId, designId) }
            if (!res.optBoolean("ok", false)) {
                errorText = formatWearApiError(
                    translationStore,
                    res.optString("error", null),
                    res.optString("message", null),
                )
                return
            }
            onCompleted()
            onDismiss()
        } catch (e: Exception) {
            errorText = e.message
        } finally {
            busy = false
        }
    }

    suspend fun runDeactivate() {
        busy = true
        errorText = null
        try {
            val rowsRes = withContext(Dispatchers.IO) {
                api.getDesignPublishedRows(ownerId, designId)
            }
            val ids = mutableListOf<Int>()
            if (rowsRes.optBoolean("ok", false)) {
                val arr = rowsRes.optJSONArray("rows") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val row = arr.optJSONObject(i) ?: continue
                    val id = row.optInt("id", -1)
                    if (id > 0) ids.add(id)
                }
            }
            if (ids.isNotEmpty()) {
                val batch = withContext(Dispatchers.IO) {
                    api.batchUnpublishPublished(ownerId, ids)
                }
                if (!batch.optBoolean("ok", false)) {
                    val enq = batch.optJSONArray("enqueued_ids")
                    if (enq == null || enq.length() == 0) {
                        errorText = formatWearApiError(
                            translationStore,
                            batch.optString("error", null),
                            batch.optString("message", null),
                        )
                        return
                    }
                }
            }
            val body = JSONObject()
                .put("design_id", designId)
                .put("library_status", "inactive")
            val upd = withContext(Dispatchers.IO) { api.updateDesign(ownerId, body) }
            if (!upd.optBoolean("ok", false)) {
                errorText = formatWearApiError(
                    translationStore,
                    upd.optString("error", null),
                    upd.optString("message", null),
                )
                return
            }
            onCompleted()
            onDismiss()
        } catch (e: Exception) {
            errorText = e.message
        } finally {
            busy = false
        }
    }

    suspend fun runActivate(creatorName: String) {
        busy = true
        errorText = null
        try {
            val body = JSONObject()
                .put("design_id", designId)
                .put("library_status", "active")
                .put("visibility", "public")
                .put("creator_name", creatorName)
            val upd = withContext(Dispatchers.IO) { api.updateDesign(ownerId, body) }
            if (!upd.optBoolean("ok", false)) {
                errorText = formatWearApiError(
                    translationStore,
                    upd.optString("error", null),
                    upd.optString("message", null),
                )
                return
            }
            onCompleted()
            onDismiss()
        } catch (e: Exception) {
            errorText = e.message
        } finally {
            busy = false
        }
    }

    fun startActivateFlow() {
        scope.launch {
            busy = true
            errorText = null
            try {
                val names = loadCreators()
                creatorNames = names
                when {
                    names.isEmpty() -> {
                        errorText = translationStore.t(
                            "wear.design_no_creator",
                            "No creator name configured.",
                        )
                    }
                    names.size == 1 -> {
                        selectedCreator = names[0]
                        phase = WearDesignActionPhase.CONFIRM_ACTIVATE
                    }
                    else -> phase = WearDesignActionPhase.PICK_CREATOR
                }
            } catch (e: Exception) {
                errorText = e.message
            } finally {
                busy = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(EazColors.CreatorBg.copy(alpha = 0.94f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (busy) {
                CircularProgressIndicator()
            }

            when (phase) {
                WearDesignActionPhase.MENU -> {
                    Text(
                        text = design.label ?: translationStore.t("wear.design", "Design"),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    if (isActive) {
                        Button(
                            onClick = { phase = WearDesignActionPhase.CONFIRM_DELETE },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.action_delete", "Delete"))
                        }
                        Button(
                            onClick = { phase = WearDesignActionPhase.CONFIRM_DEACTIVATE },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.design_deactivate", "Deactivate"))
                        }
                    } else {
                        Button(
                            onClick = { phase = WearDesignActionPhase.CONFIRM_DELETE },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.action_delete", "Delete"))
                        }
                        Button(
                            onClick = { startActivateFlow() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.design_activate", "Activate"))
                        }
                    }
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(translationStore.t("wear.back", "Back"))
                    }
                }

                WearDesignActionPhase.CONFIRM_DELETE -> {
                    val warn = if (isActive && publishedCount > 0) {
                        translationStore.t(
                            "wear.design_delete_products_warn",
                            "Deleting will remove {{count}} products.",
                        ).replace("{{count}}", publishedCount.toString())
                    } else {
                        translationStore.t(
                            "wear.design_delete_confirm",
                            "Delete this design?",
                        )
                    }
                    Text(text = warn, style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Button(
                            onClick = { scope.launch { runDelete() } },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.confirm", "Confirm"))
                        }
                        Button(
                            onClick = { phase = WearDesignActionPhase.MENU },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.cancel", "Cancel"))
                        }
                    }
                }

                WearDesignActionPhase.CONFIRM_DEACTIVATE -> {
                    Text(
                        text = translationStore.t(
                            "wear.design_deactivate_confirm",
                            "Deactivate this design?",
                        ),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Button(
                            onClick = { scope.launch { runDeactivate() } },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.confirm", "Confirm"))
                        }
                        Button(
                            onClick = { phase = WearDesignActionPhase.MENU },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.cancel", "Cancel"))
                        }
                    }
                }

                WearDesignActionPhase.CONFIRM_ACTIVATE -> {
                    val name = selectedCreator.orEmpty()
                    Text(
                        text = translationStore.t(
                            "wear.design_activate_creator_confirm",
                            "Activate as {{creator}}?",
                        ).replace("{{creator}}", name),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Button(
                            onClick = { scope.launch { runActivate(name) } },
                            modifier = Modifier.weight(1f),
                            enabled = !busy && name.isNotBlank(),
                        ) {
                            Text(translationStore.t("wear.confirm", "Confirm"))
                        }
                        Button(
                            onClick = { phase = WearDesignActionPhase.MENU },
                            modifier = Modifier.weight(1f),
                            enabled = !busy,
                        ) {
                            Text(translationStore.t("wear.cancel", "Cancel"))
                        }
                    }
                }

                WearDesignActionPhase.PICK_CREATOR -> {
                    Text(
                        text = translationStore.t(
                            "wear.design_pick_creator",
                            "Choose creator",
                        ),
                        style = MaterialTheme.typography.caption2,
                        textAlign = TextAlign.Center,
                    )
                    for (name in creatorNames) {
                        Button(
                            onClick = {
                                selectedCreator = name
                                phase = WearDesignActionPhase.CONFIRM_ACTIVATE
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) {
                            Text(name, maxLines = 1)
                        }
                    }
                    Button(
                        onClick = { phase = WearDesignActionPhase.MENU },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Text(translationStore.t("wear.cancel", "Cancel"))
                    }
                }
            }

            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText!!,
                    style = MaterialTheme.typography.caption2,
                    color = EazColors.Orange,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
