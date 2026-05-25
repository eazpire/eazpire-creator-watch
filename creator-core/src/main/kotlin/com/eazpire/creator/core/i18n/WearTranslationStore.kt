package com.eazpire.creator.core.i18n

import java.util.Locale

/**
 * Wear MVP: English defaults + fixed Active Jobs / No active jobs labels.
 */
class WearTranslationStore(private var lang: String = "en") {

    fun setLanguage(code: String) {
        lang = code.trim().lowercase(Locale.ROOT)
    }

    fun t(key: String, default: String): String {
        if (isFixedActiveJobsKey(key)) return fixedActiveJobsLabel(lang)
        if (isFixedNoActiveJobsKey(key)) return "No active jobs"
        return strings[key] ?: default
    }

    private fun isFixedActiveJobsKey(key: String): Boolean = when (key) {
        "creator.notifications.active_jobs",
        "eazy_fn.active_jobs" -> true
        else -> false
    }

    private fun isFixedNoActiveJobsKey(key: String): Boolean =
        key == "creator.notifications.empty_jobs"

    private fun fixedActiveJobsLabel(lang: String): String {
        val base = lang.trim().lowercase(Locale.ROOT).take(2)
        return if (base == "de") "Aktive Jobs" else "Active Jobs"
    }

    companion object {
        private val strings = mapOf(
            "wear.pair_title" to "Pair with phone",
            "wear.pair_body" to "Log in with the Eazpire app on your phone.",
            "wear.pair_qr_hint" to "Creator Settings → Creator Wear → Connect, then scan this QR.",
            "wear.pair_qr_loading" to "Preparing QR…",
            "wear.pair_qr_error" to "Could not start pairing",
            "wear.pair_sync_hint" to "Or log in on the phone app — session may sync automatically.",
            "wear.dashboard" to "Dashboard",
            "wear.jobs" to "Active Jobs",
            "wear.upload" to "Phone upload",
            "wear.products_online" to "Products online",
            "wear.products_offline" to "Products offline",
            "wear.sales" to "Sales",
            "wear.designs_gen" to "Designs generated",
            "wear.designs_up" to "Designs uploaded",
            "wear.payout" to "Available payout",
            "wear.upload_scan" to "Scan with your phone",
            "wear.upload_done" to "Upload complete",
            "wear.upload_error" to "Upload not available",
            "wear.loading" to "Loading…",
            "wear.refresh" to "Refresh",
        )
    }
}
