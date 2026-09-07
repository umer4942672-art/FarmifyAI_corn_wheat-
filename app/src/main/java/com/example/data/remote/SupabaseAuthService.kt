package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
 */
class SupabaseAuthService(context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    private val media = "application/json; charset=utf-8".toMediaType()
    private val session = AuthSessionStore(context)

    private suspend fun post(path: String, body: JSONObject): SupabaseAuthResult = withContext(Dispatchers.IO) {
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

    suspend fun signUp(email: String, password: String, metadata: Map<String, Any> = emptyMap()): SupabaseAuthResult {
        val meta = JSONObject()
        metadata.forEach { (key, value) -> meta.put(key, value) }
        val result = post(
            "/api/auth/signup",
            JSONObject().put("email", email.trim()).put("password", password).put("metadata", meta)
        )
        if (result.isSuccess && result.accessToken != null) {
            session.save(result.accessToken, result.userId)
        } else if (result.isSuccess) {
            // Email-confirmation flows may return no session; do not retain a previous user's token.
            session.clear()
        }
        return result
    }

    suspend fun signInAnonymously(): SupabaseAuthResult {
        val result = post("/api/auth/guest", JSONObject())
        if (result.isSuccess && result.accessToken != null) session.save(result.accessToken, result.userId)
        return result
    }

    suspend fun signInWithPassword(email: String, password: String): SupabaseAuthResult {
        val result = post(
            "/api/auth/login",
            JSONObject().put("email", email.trim()).put("password", password)
        )
        if (result.isSuccess && result.accessToken != null) {
            session.save(result.accessToken, result.userId)
        }
        return result
    }

    suspend fun recoverPassword(email: String) =
        post("/api/auth/forgot-password", JSONObject().put("email", email.trim()))

    fun clearSession() = session.clear()
}
