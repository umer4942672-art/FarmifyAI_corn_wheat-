package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.DiseaseScanDao
import com.example.data.local.DiseaseScanEntity
import com.example.data.local.KhataDao
import com.example.data.local.KhataEntryEntity
import com.example.data.remote.SupabaseDataSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudRestoreSummary(
    val ok: Boolean = false,
    val khataRestored: Int = 0,
    val diseaseRestored: Int = 0,
    val message: String? = null
)

/**
 * Pulls the farmer's cloud state back down after login and retries anything
 * that failed to reach Supabase earlier.
 *
 * Without the restore pass, signing in on a new phone showed an empty ledger
 * even though the records existed in Supabase. Without the retry pass, a row
 * that failed to sync once stayed local forever, because nothing ever looked
 * at the `isSynced` flag again.
 */
class CloudSyncRepository(
    private val khataDao: KhataDao,
    private val diseaseScanDao: DiseaseScanDao,
    context: Context,
    private val sync: SupabaseDataSyncService = SupabaseDataSyncService(context.applicationContext)
) {

    private val restoreMutex = Mutex()
    // App start and a login both trigger a retry. Without this guard the two
    // passes overlap and push the same rows twice.
    private val retryMutex = Mutex()

    /** Calls GET /api/sync/bootstrap and merges the result into Room. */
    suspend fun restoreFromCloud(userKey: String): CloudRestoreSummary = restoreMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!sync.hasSession()) {
                return@withContext CloudRestoreSummary(message = "No active cloud session")
            }
            val payload = sync.fetchBootstrap()
                ?: return@withContext CloudRestoreSummary(message = "Cloud restore unavailable")

            var khataCount = 0
            var diseaseCount = 0

            runCatching {
                val rows = SupabaseDataSyncService.jsonArrayOrEmpty(payload, "khata")
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    if (insertKhataIfMissing(row, userKey)) khataCount++
                }
            }.onFailure { Log.w(TAG, "Khata restore failed: ${it.localizedMessage}") }

            runCatching {
                val rows = SupabaseDataSyncService.jsonArrayOrEmpty(payload, "disease_history")
                for (i in 0 until rows.length()) {
                    val row = rows.optJSONObject(i) ?: continue
                    if (insertScanIfMissing(row, userKey)) diseaseCount++
                }
            }.onFailure { Log.w(TAG, "Disease restore failed: ${it.localizedMessage}") }

            Log.d(TAG, "Cloud restore: khata=$khataCount disease=$diseaseCount")
            CloudRestoreSummary(
                ok = true,
                khataRestored = khataCount,
                diseaseRestored = diseaseCount
            )
        }
    }

    /** Re-sends everything that is still marked unsynced locally. */
    suspend fun retryPendingSync(userKey: String): Int = retryMutex.withLock {
        withContext(Dispatchers.IO) {
            retryPendingSyncInternal(userKey)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun retryPendingSyncInternal(userKey: String): Int {
        if (!sync.hasSession()) return 0
        var pushed = 0

        runCatching {
            khataDao.getUnsyncedEntries(200).forEach { entry ->
                if (sync.syncKhataTransaction(entry)) {
                    khataDao.setSynced(entry.id, true)
                    pushed++
                }
            }
        }.onFailure { Log.w(TAG, "Khata retry failed: ${it.localizedMessage}") }

        // Only rows that never reached the cloud. This used to re-push the fifty
        // most recent scans on every launch, which meant fifty sequential network
        // calls each time the app opened.
        runCatching {
            diseaseScanDao.getUnsyncedScans(100).forEach { scan ->
                if (sync.syncDiseaseDetection(scan)) {
                    diseaseScanDao.setScanSynced(scan.id, true)
                    pushed++
                }
            }
        }.onFailure { Log.w(TAG, "Disease retry failed: ${it.localizedMessage}") }

        return pushed
    }

    suspend fun pendingCount(): Int = withContext(Dispatchers.IO) {
        runCatching { khataDao.getUnsyncedCount() }.getOrDefault(0)
    }

    private suspend fun insertKhataIfMissing(row: JSONObject, userKey: String): Boolean {
        val type = row.optString("type", "expense").uppercase(Locale.ENGLISH)
        val entryType = if (type == "INCOME") "INCOME" else "EXPENSE"
        val cropName = row.optString("crop_name").ifBlank { "General" }
        val amount = row.optDouble("amount", 0.0).takeIf { !it.isNaN() } ?: 0.0
        val date = displayDate(row.optString("transaction_date"))

        if (khataDao.countMatching(userKey, date, entryType, cropName, amount) > 0) return false

        val quantity = row.optDouble("quantity", 0.0).takeIf { !it.isNaN() } ?: 0.0
        val entity = KhataEntryEntity(
            userId = userKey,
            entryType = entryType,
            date = date,
            timestamp = parseTimestamp(row.optString("created_at")),
            cropName = cropName,
            fieldName = row.optString("field_name").ifBlank { "Main Field" },
            activityType = row.optString("category"),
            description = row.optString("description").takeIf { it != "null" }.orEmpty(),
            quantity = quantity,
            unit = row.optString("unit").ifBlank { "Mann" },
            sellingPricePerUnit = if (quantity > 0) amount / quantity else 0.0,
            totalAmount = amount,
            buyerOrMandi = row.optString("buyer_or_mandi").takeIf { it != "null" }.orEmpty(),
            // It came from the cloud, so it is already in sync.
            isSynced = true
        )
        khataDao.insertEntry(entity)
        return true
    }

    private suspend fun insertScanIfMissing(row: JSONObject, userKey: String): Boolean {
        val cropName = row.optString("crop_name").ifBlank { "Unknown" }
        val diseaseEn = row.optString("disease_name").ifBlank { "Unknown" }
        val confidence = row.optDouble("confidence", 0.0).let { if (it.isNaN()) 0 else it.toInt() }

        if (diseaseScanDao.countMatching(userKey, cropName, diseaseEn, confidence) > 0) return false

        val entity = DiseaseScanEntity(
            userId = userKey,
            cropName = cropName,
            diseaseNameEn = diseaseEn,
            diseaseNameUr = row.optString("disease_name_ur"),
            confidencePercent = confidence,
            isHealthy = diseaseEn.contains("healthy", ignoreCase = true),
            severityLevel = row.optString("severity").ifBlank { "Unknown" },
            symptoms = row.optString("symptoms"),
            symptomsUr = "",
            chemicalTreatment = row.optString("treatment_chemical"),
            chemicalTreatmentUr = "",
            organicPrevention = row.optString("treatment_organic"),
            organicPreventionUr = "",
            advisoryNote = row.optString("recommendation"),
            advisoryNoteUr = "",
            // The cloud stores a private storage path, not a local file, so history
            // rows restored from the cloud show without a thumbnail.
            imageUriOrPath = "",
            timestamp = parseTimestamp(row.optString("created_at")),
            isSyncedCloud = true
        )
        diseaseScanDao.insertScan(entity)
        return true
    }

    /** Supabase returns ISO dates; the Khata UI stores "dd MMM yyyy". */
    private fun displayDate(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return uiDateFormat.format(Date())
        return runCatching {
            uiDateFormat.format(isoDateFormat.parse(isoDate.take(10))!!)
        }.getOrDefault(uiDateFormat.format(Date()))
    }

    private fun parseTimestamp(createdAt: String?): Long {
        if (createdAt.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching {
            isoDateFormat.parse(createdAt.take(10))!!.time
        }.getOrDefault(System.currentTimeMillis())
    }

    companion object {
        private const val TAG = "CloudSyncRepository"
        private val uiDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    }
}
