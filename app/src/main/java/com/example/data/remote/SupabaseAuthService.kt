package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SupabaseAuthResult(
    val isSuccess: Boolean,
    val userId: String? = null,
    val email: String? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userMetadata: Map<String, Any?> = emptyMap(),
    val errorMessage: String? = null
)

/**
 * Calls the FarmifyAI backend. Supabase credentials never live in the APK.
 *
 * Both the access token and the refresh token returned by the backend are
 * persisted, so [AuthorizedApiClient] can rotate an expired session instead of
 * dropping the user's cloud sync after an hour.
 */
class SupabaseAuthService(context: Context) {
    private val appContext = context.applicationContext
    private val client = AuthorizedApiClient.httpClient
    private val media = AuthorizedApiClient.JSON
    private val session = AuthSessionStore(appContext)
    private val api = AuthorizedApiClient(appContext)

    private suspend fun post(path: String, body: JSONObject): SupabaseAuthResult = withContext(Dispatchers.IO) {
        // Checked before the try block. A missing backend URL used to throw inside
        // it and be caught as "Backend connection failed", which UserRepository
        // reads as "the server is down" and falls back to offline login. The user
        // then sees "invalid credentials" for what is really a build problem.
        if (!ApiConfig.isConfigured) {
            return@withContext SupabaseAuthResult(
                false,
                errorMessage = "This build has no backend URL, so it cannot sign in to the cloud. " +
                    "Rebuild with backendBaseUrl set in app/.env."
            )
        }
        try {
            val request = Request.Builder()
                .url(ApiConfig.endpoint(path))
                .post(body.toString().toRequestBody(media))
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val json = if (text.isNotBlank()) JSONObject(text) else JSONObject()
                if (!response.isSuccessful) {
                    return@withContext SupabaseAuthResult(
                        false,
                        errorMessage = json.optString("detail", "Request failed (${response.code})")
                    )
                }
                val user = json.optJSONObject("user")
                val metadata = mutableMapOf<String, Any?>()
                user?.optJSONObject("user_metadata")?.let { meta ->
                    meta.keys().forEach { key -> metadata[key] = meta.opt(key) }
                }
                SupabaseAuthResult(
                    isSuccess = true,
                    userId = user?.optString("id")?.takeIf { it.isNotBlank() },
                    email = user?.optString("email")?.takeIf { it.isNotBlank() },
                    accessToken = json.optString("access_token").takeIf { it.isNotBlank() },
                    refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() },
                    userMetadata = metadata
                )
            }
        } catch (e: Exception) {
            SupabaseAuthResult(false, errorMessage = "Backend connection failed: ${e.localizedMessage ?: "network error"}")
        }
    }

    private fun persist(result: SupabaseAuthResult) {
        session.save(result.accessToken, result.userId, result.refreshToken)
    }

    suspend fun signUp(email: String? = null, phone: String? = null, password: String, metadata: Map<String, Any> = emptyMap()): SupabaseAuthResult {
        val meta = JSONObject()
        metadata.forEach { (key, value) -> meta.put(key, value) }
        val result = post(
            "/api/auth/signup",
            JSONObject().apply { if (!email.isNullOrBlank()) put("email", email.trim()); if (!phone.isNullOrBlank()) put("phone", phone.trim()); put("password", password); put("metadata", meta) }
        )
        if (result.isSuccess && result.accessToken != null) {
            persist(result)
        } else if (result.isSuccess) {
            // Email-confirmation flows may return no session; do not retain a previous user's token.
            session.clear()
        }
        return result
    }

    suspend fun signInAnonymously(): SupabaseAuthResult {
        val result = post("/api/auth/guest", JSONObject())
        if (result.isSuccess && result.accessToken != null) persist(result)
        return result
    }

    suspend fun signInWithPassword(identifier: String, password: String): SupabaseAuthResult {
        val isEmail = identifier.contains("@")
        val phone = if (isEmail) null else identifier.trim()
        val email = if (isEmail) identifier.trim() else null
        val result = post(
            "/api/auth/login",
            JSONObject().apply { if (email != null) put("email", email); if (phone != null) put("phone", phone); put("password", password) }
        )
        if (result.isSuccess && result.accessToken != null) {
            persist(result)
        }
        return result
    }

    suspend fun recoverPassword(email: String) =
        post("/api/auth/forgot-password", JSONObject().put("email", email.trim()))

    /**
     * Validates the stored session against the backend, transparently refreshing
     * an expired access token. Call this on app start before trusting a restored
     * local "logged in" flag.
     */
    suspend fun getCurrentUser(): SupabaseAuthResult {
        if (session.accessToken() == null) {
            return SupabaseAuthResult(false, errorMessage = "No active session")
        }
        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint("/api/auth/me"))
                .header("Authorization", "Bearer $token")
                .get()
                .build()
        } ?: return SupabaseAuthResult(false, errorMessage = "Backend connection failed: network error")

        if (!response.isSuccessful) {
            return SupabaseAuthResult(false, errorMessage = response.errorDetail("Session validation failed"))
        }
        val user = response.json().optJSONObject("user")
        return SupabaseAuthResult(
            isSuccess = true,
            userId = user?.optString("id")?.takeIf { it.isNotBlank() },
            email = user?.optString("email")?.takeIf { it.isNotBlank() },
            accessToken = session.accessToken()
        )
    }

    /** True when a valid (possibly just-refreshed) Supabase session exists. */
    suspend fun hasValidSession(): Boolean = getCurrentUser().isSuccess

    suspend fun logout(): Boolean {
        if (session.accessToken() == null) {
            session.clear()
            return true
        }
        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint("/api/auth/logout"))
                .header("Authorization", "Bearer $token")
                .post("".toRequestBody(media))
                .build()
        }
        // Always clear the local encrypted session so the app cannot reuse a stale token,
        // even when the network call could not be completed.
        session.clear()
        return response?.isSuccessful == true
    }

    fun clearSession() = session.clear()
}
