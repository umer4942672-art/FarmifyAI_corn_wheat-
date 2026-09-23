package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.PendingProofDao
import com.example.data.local.PendingProofEntity
import com.example.data.model.UserRole
import com.example.data.remote.WorkApiService
import com.example.util.FieldLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** What happened to a captured photo. */
sealed interface CaptureOutcome {
    data object Uploaded : CaptureOutcome
    data object QueuedOffline : CaptureOutcome
    data class Failed(val message: String) : CaptureOutcome
}

/**
 * Contractor verification data layer.
 *
 * Evidence follows the same offline-first rule as the rest of the app: the
 * photo, its GPS fix and its timestamp are fixed at the moment of capture and
 * stored on the device. If the upload fails for lack of signal the record waits
 * in Room and is sent by [flushPendingProofs] once a connection returns.
 */
class WorkRepository(
    context: Context,
    private val pendingDao: PendingProofDao,
    val api: WorkApiService = WorkApiService(context.applicationContext)
) {
    private val appContext = context.applicationContext
    private val flushMutex = Mutex()

    private val prefs = appContext.getSharedPreferences("farmify_work", Context.MODE_PRIVATE)

    // --- role chosen at sign-up, sent once the session exists ---------------
    fun rememberRoleChoice(role: UserRole) {
        prefs.edit().putString(KEY_PENDING_ROLE, role.name).apply()
    }

    suspend fun resolveRole(): UserRole {
        prefs.getString(KEY_PENDING_ROLE, null)?.let { pending ->
            val role = runCatching { UserRole.valueOf(pending) }.getOrDefault(UserRole.LANDOWNER)
            if (api.setRole(role).isSuccess) {
                prefs.edit().remove(KEY_PENDING_ROLE).apply()
                cacheRole(role)
                return role
            }
        }
        val remote = api.getRole().getOrNull()
        if (remote != null) {
            cacheRole(remote)
            return remote
        }
        return cachedRole()
    }

    private fun cacheRole(role: UserRole) = prefs.edit().putString(KEY_CACHED_ROLE, role.name).apply()

    fun cachedRole(): UserRole = runCatching {
        UserRole.valueOf(prefs.getString(KEY_CACHED_ROLE, UserRole.LANDOWNER.name)!!)
    }.getOrDefault(UserRole.LANDOWNER)

    // --- evidence ---------------------------------------------------------
    suspend fun captureProof(orderId: String, stage: String, bitmap: Bitmap): CaptureOutcome =
        withContext(Dispatchers.IO) {
            if (!FieldLocation.hasPermission(appContext)) {
                return@withContext CaptureOutcome.Failed(
                    "Location permission is needed so the photo can show where the work was done."
                )
            }
            val fix = FieldLocation.currentFix(appContext)
                ?: return@withContext CaptureOutcome.Failed(
                    "Could not get a GPS fix. Turn on location and try again in the open."
                )

            val file = saveImage(bitmap)
            val capturedAt = isoNow()

            val result = api.uploadProof(
                orderId, stage, file, fix.latitude, fix.longitude,
                fix.accuracyM, capturedAt, fix.isMock
            )
            if (result.isSuccess) {
                runCatching { file.delete() }
                return@withContext CaptureOutcome.Uploaded
            }

            val message = result.exceptionOrNull()?.message.orEmpty()
            if (message.startsWith("Could not reach the server")) {
                pendingDao.insert(
                    PendingProofEntity(
                        workOrderId = orderId,
                        stage = stage,
                        imagePath = file.absolutePath,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        accuracyM = fix.accuracyM,
                        capturedAtIso = capturedAt,
                        isMockLocation = fix.isMock
                    )
                )
                CaptureOutcome.QueuedOffline
            } else {
                // A rule rejection (wrong status, not your job) will not succeed on
                // retry, so it is reported now instead of being queued forever.
                runCatching { file.delete() }
                CaptureOutcome.Failed(message.ifBlank { "Upload failed" })
            }
        }

    /** Sends queued evidence. Returns how many were delivered. */
    suspend fun flushPendingProofs(): Int = flushMutex.withLock {
        withContext(Dispatchers.IO) {
            var sent = 0
            for (p in pendingDao.getAll()) {
                val file = File(p.imagePath)
                if (!file.exists()) {
                    pendingDao.delete(p.id)
                    continue
                }
                val r = api.uploadProof(
                    p.workOrderId, p.stage, file, p.latitude, p.longitude,
                    p.accuracyM, p.capturedAtIso, p.isMockLocation
                )
                if (r.isSuccess) {
                    pendingDao.delete(p.id)
                    runCatching { file.delete() }
                    sent++
                } else {
                    pendingDao.markAttempt(p.id)
                    val msg = r.exceptionOrNull()?.message.orEmpty()
                    if (msg.startsWith("Could not reach the server")) break   // still offline
                    if (p.attempts >= MAX_ATTEMPTS) {
                        Log.w(TAG, "Dropping proof ${p.id} after repeated rejection: $msg")
                        pendingDao.delete(p.id)
                        runCatching { file.delete() }
                    }
                }
            }
            sent
        }
    }

    fun pendingCountFor(orderId: String) = pendingDao.countForOrder(orderId)

    private fun saveImage(bitmap: Bitmap): File {
        val dir = File(appContext.filesDir, "work_proofs").apply { mkdirs() }
        val file = File(dir, "proof_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        return file
    }

    private fun isoNow(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date())

    companion object {
        private const val TAG = "WorkRepository"
        private const val KEY_PENDING_ROLE = "pending_role"
        private const val KEY_CACHED_ROLE = "cached_role"
        private const val MAX_ATTEMPTS = 5
    }
}
