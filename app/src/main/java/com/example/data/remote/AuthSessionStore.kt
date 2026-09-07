package com.example.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keystore-backed storage for the Supabase session.
 *
 * Supabase access tokens are short lived (one hour by default), so the refresh
 * token is persisted alongside them. Without it every cloud call would start
 * failing silently roughly an hour after login.
 */
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

    /**
     * Stores a session. [refreshToken] is optional because some Supabase flows
     * (for example email-confirmation signup) return a user without a session;
     * in that case any previously stored refresh token is preserved only when a
     * new one is not supplied.
     */
    fun save(accessToken: String?, userId: String?, refreshToken: String? = null) {
        val editor = prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken.orEmpty())
            .putString(KEY_USER_ID, userId.orEmpty())
        if (!refreshToken.isNullOrBlank()) {
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        editor.apply()
    }

    /** Replaces only the rotated token pair, keeping the stored user id. */
    fun updateTokens(accessToken: String?, refreshToken: String?) {
        val editor = prefs.edit()
        if (!accessToken.isNullOrBlank()) editor.putString(KEY_ACCESS_TOKEN, accessToken)
        if (!refreshToken.isNullOrBlank()) editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        editor.apply()
    }

    fun accessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun refreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }
    fun userId(): String? = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }
    fun hasSession(): Boolean = accessToken() != null
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
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
    }
}
