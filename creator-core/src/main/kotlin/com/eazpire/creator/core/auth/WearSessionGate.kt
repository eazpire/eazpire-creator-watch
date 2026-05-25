package com.eazpire.creator.core.auth

import android.content.Context

/**
 * Wear UI only enters the main shell after an explicit auth handoff (QR claim or phone sync).
 * Bumping [AUTH_SCHEMA_VERSION] clears legacy demo/emulator sessions.
 */
object WearSessionGate {
    private const val PREFS = "eaz_wear_session_gate"
    private const val KEY_READY = "session_ready"
    private const val KEY_SCHEMA = "auth_schema_version"

    /** Increment to force QR pairing again (e.g. after removing demo bypass). */
    const val AUTH_SCHEMA_VERSION = 2

    fun ensureSchema(context: Context, tokenStore: SecureTokenStore) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_SCHEMA, 0)
        if (current >= AUTH_SCHEMA_VERSION) return
        tokenStore.clear()
        prefs.edit()
            .putInt(KEY_SCHEMA, AUTH_SCHEMA_VERSION)
            .remove(KEY_READY)
            .apply()
    }

    fun isSessionReady(context: Context, tokenStore: SecureTokenStore): Boolean {
        ensureSchema(context, tokenStore)
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return tokenStore.isLoggedIn() && prefs.getBoolean(KEY_READY, false)
    }

    fun markSessionReady(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_READY, true)
            .putInt(KEY_SCHEMA, AUTH_SCHEMA_VERSION)
            .apply()
    }

    fun clearSession(context: Context, tokenStore: SecureTokenStore) {
        tokenStore.clear()
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_READY)
            .apply()
    }
}
