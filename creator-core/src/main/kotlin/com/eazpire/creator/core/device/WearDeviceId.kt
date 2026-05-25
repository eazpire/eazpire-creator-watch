package com.eazpire.creator.core.device

import android.content.Context
import java.util.UUID

/** Stable id for this watch in wear-pair sessions. */
object WearDeviceId {
    private const val PREFS = "eaz_wear_device"
    private const val KEY = "device_id"

    fun get(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)?.trim().orEmpty()
        if (existing.isNotBlank()) return existing
        val id = "wear-${UUID.randomUUID()}"
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}
