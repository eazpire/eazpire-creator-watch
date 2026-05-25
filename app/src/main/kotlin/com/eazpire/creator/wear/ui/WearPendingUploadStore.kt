package com.eazpire.creator.wear.ui

import android.content.Context

/** Persists in-progress design uploads across tab switches and app restarts. */
class WearPendingUploadStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class Pending(
        val jobId: String,
        val previewUrl: String?,
        val label: String?,
    )

    fun save(pending: Pending) {
        prefs.edit()
            .putString(KEY_JOB, pending.jobId)
            .putString(KEY_PREVIEW, pending.previewUrl)
            .putString(KEY_LABEL, pending.label)
            .apply()
    }

    fun load(): Pending? {
        val jobId = prefs.getString(KEY_JOB, null)?.trim().orEmpty()
        if (jobId.isBlank()) return null
        return Pending(
            jobId = jobId,
            previewUrl = prefs.getString(KEY_PREVIEW, null)?.takeIf { it.isNotBlank() },
            label = prefs.getString(KEY_LABEL, null)?.takeIf { it.isNotBlank() },
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS = "wear_pending_design_upload"
        private const val KEY_JOB = "job_id"
        private const val KEY_PREVIEW = "preview_url"
        private const val KEY_LABEL = "label"
    }
}
