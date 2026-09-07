package com.example.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Plain result holder so callers never have to manage an OkHttp Response body. */
data class ApiResponse(val code: Int, val body: String) {
    val isSuccessful: Boolean get() = code in 200..299
    fun json(): JSONObject = if (body.isBlank()) JSONObject() else JSONObject(body)
    fun errorDetail(fallback: String): String =
        runCatching { json().optString("detail", fallback).ifBlank { fallback } }.getOrDefault(fallback)
}

/**
 * Single entry point for every authenticated backend call.
 *
 * Supabase access tokens expire after about an hour. Any call that comes back
 * 401 is retried exactly once with a freshly rotated token, so a long-running
 * session keeps syncing instead of failing silently.
 */
class AuthorizedApiClient(context: Context) {

    private val session = AuthSessionStore(context.applicationContext)

    suspend fun call(buildRequest: (token: String) -> Request): ApiResponse? = withContext(Dispatchers.IO) {
        val token = session.accessToken() ?: return@withContext null
        try {
            val first = execute(buildRequest(token))
            if (first.code != 401) return@withContext first

            val rotated = refreshAccessToken() ?: return@withContext first
            execute(buildRequest(rotated))
        } catch (e: Exception) {
            Log.w(TAG, "Authorized call failed: ${e.localizedMessage}")
            null
        }
    }

    /**
     * Exchanges the stored refresh token for a new access token.
     * Returns null when there is no refresh token or the session is truly gone,
     * in which case the local session is cleared so the UI can ask for a re-login.
     */
    suspend fun refreshAccessToken(): String? = refreshMutex.withLock {
        withContext(Dispatchers.IO) {
            val refreshToken = session.refreshToken() ?: return@withContext null
            try {
                val body = JSONObject().put("refresh_token", refreshToken).toString().toRequestBody(JSON)
                val response = execute(
                    Request.Builder()
                        .url(ApiConfig.endpoint("/api/auth/refresh"))
                        .post(body)
                        .build()
                )
                if (!response.isSuccessful) {
                    // A rejected refresh token means the session cannot be recovered.
                    if (response.code in 400..403) session.clear()
                    return@withContext null
                }
                val json = response.json()
                val newAccess = json.optString("access_token").takeIf { it.isNotBlank() }
                    ?: return@withContext null
                val newRefresh = json.optString("refresh_token").takeIf { it.isNotBlank() }
                session.updateTokens(newAccess, newRefresh)
                Log.d(TAG, "Supabase access token refreshed")
                newAccess
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh failed: ${e.localizedMessage}")
                null
            }
        }
    }

    private fun execute(request: Request): ApiResponse =
        httpClient.newCall(request).execute().use { response ->
            ApiResponse(response.code, response.body?.string().orEmpty())
        }

    companion object {
        private const val TAG = "AuthorizedApiClient"
        val JSON = "application/json; charset=utf-8".toMediaType()

        // One refresh at a time, otherwise several parallel 401s would each burn
        // a refresh token and invalidate one another under rotation.
        private val refreshMutex = Mutex()

        val httpClient: OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
