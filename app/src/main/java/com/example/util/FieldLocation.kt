package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * One GPS fix for evidence capture.
 *
 * Evidence needs a fresh position, so a cached fix is only accepted if it is
 * recent. Otherwise an active request is made, preferring GPS over network,
 * since the point of the photo is to show where the work happened.
 */
data class FieldFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Float?,
    val isMock: Boolean
)

object FieldLocation {

    private const val FRESH_MS = 2 * 60 * 1000L
    private const val TIMEOUT_MS = 20_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentFix(context: Context): FieldFix? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val cutoff = System.currentTimeMillis() - FRESH_MS
        val cached = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .filter { it.time >= cutoff }
            .maxByOrNull { it.time }
        if (cached != null) return cached.toFix()

        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine<FieldFix?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(location.toFix())
                    }
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        runCatching { lm.removeUpdates(this) }
                        if (cont.isActive) cont.resume(null)
                    }
                    @Deprecated("Required below API 30")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                }
                runCatching {
                    lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                }.onFailure { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
    }

    private fun Location.toFix(): FieldFix = FieldFix(
        latitude = latitude,
        longitude = longitude,
        accuracyM = if (hasAccuracy()) accuracy else null,
        isMock = isMockCompat()
    )

    /** Fake-GPS apps set this flag. It is reported to the landowner, not used to block. */
    @Suppress("DEPRECATION")
    private fun Location.isMockCompat(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) isMock else isFromMockProvider
}
