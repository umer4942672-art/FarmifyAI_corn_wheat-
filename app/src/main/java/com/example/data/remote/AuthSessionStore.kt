package com.example.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Keystore-backed storage for the short-lived Supabase access token and user id. */
class AuthSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        SECURE_PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init { migrateLegacyPlaintextSession() }

    fun save(accessToken: String?, userId: String?) = prefs.edit()
        .putString(KEY_ACCESS_TOKEN, accessToken.orEmpty())
        .putString(KEY_USER_ID, userId.orEmpty())
        .apply()
    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun userId(): String? = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
    fun clear() = prefs.edit().clear().apply()

    private fun migrateLegacyPlaintextSession() {
        if (prefs.contains(KEY_ACCESS_TOKEN) || prefs.contains(KEY_USER_ID)) return
        val legacy = appContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val token = legacy.getString(KEY_ACCESS_TOKEN, null)
        val userId = legacy.getString(KEY_USER_ID, null)
        if (!token.isNullOrBlank() || !userId.isNullOrBlank()) {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, token.orEmpty())
                .putString(KEY_USER_ID, userId.orEmpty())
                .apply()
            legacy.edit().clear().apply()
        }
    }

    companion object {
        private const val SECURE_PREFS_NAME = "farmify_auth_session_secure"
        private const val LEGACY_PREFS_NAME = "farmify_auth_session"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
    }
}
