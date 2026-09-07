package com.example.data.remote

import com.example.BuildConfig

/** Central backend configuration. The release APK must be built with a real HTTPS backend URL. */
object ApiConfig {
    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')

    fun endpoint(path: String): String {
        require(BASE_URL.startsWith("https://")) {
            "Backend URL is not configured. Set backendBaseUrl in app/.env (debug) or pass -PbackendBaseUrl=https://your-backend"
        }
        return BASE_URL + if (path.startsWith('/')) path else "/$path"
    }

    val isConfigured: Boolean
        get() = BASE_URL.startsWith("https://")
}
