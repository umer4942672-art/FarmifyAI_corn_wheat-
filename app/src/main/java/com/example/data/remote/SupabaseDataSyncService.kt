package com.example.data.remote

import android.content.Context
import com.example.data.local.DiseaseScanEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sends and restores cloud data through the authenticated FarmifyAI backend.
 * Every call goes through [AuthorizedApiClient], so an expired access token is
 * refreshed and the request retried instead of failing silently.
 */
class SupabaseDataSyncService(context: Context) {
    private val appContext = context.applicationContext
    private val media = AuthorizedApiClient.JSON
    private val session = AuthSessionStore(appContext)
    private val api = AuthorizedApiClient(appContext)

    private suspend fun send(path: String, data: JSONObject): Boolean {
        val payload = JSONObject().put("data", data).toString()
        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint(path))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .post(payload.toRequestBody(media))
                .build()
        }
        return response?.isSuccessful == true
    }

    private suspend fun delete(path: String): Boolean {
        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint(path))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .delete()
                .build()
        }
        // 404 means the row was never synced in the first place; nothing left to remove.
        return response != null && (response.isSuccessful || response.code == 404)
    }

    fun hasSession(): Boolean = session.hasSession()

    suspend fun syncProfile(u: UserEntity): Boolean {
        return send("/api/sync/profile", JSONObject().apply {
            // Backend takes the authenticated Supabase user id from the access token.
            put("email", u.email)
            put("name", u.fullName)
            put("phone", u.phone)
            put("farm_name", u.farmName)
            put("district", u.district)
            put("province", u.province)
            put("farm_location", u.farmLocation)
            put("total_acres", u.totalAcres)
            put("primary_crops", u.primaryCropsString)
        })
    }

    private fun khata(e: KhataEntryEntity, localId: String) = JSONObject().apply {
        put("local_id", localId)
        put("type", e.entryType.lowercase())
        put("category", e.activityType.ifBlank { e.entryType }.lowercase())
        put("crop_name", e.cropName.ifBlank { "General" })
        put("amount", e.totalAmount)
        put("quantity", e.quantity)
        put("unit", e.unit.ifBlank { "Mann" })
        put("field_name", e.fieldName.ifBlank { "Main Field" })
        put("buyer_or_mandi", e.buyerOrMandi.ifBlank { JSONObject.NULL })
        put("description", e.description.ifBlank { JSONObject.NULL })
        put("transaction_date", e.date)
    }

    private fun khataStableId(e: KhataEntryEntity): String = if (e.id > 0) "db:${e.id}" else "legacy:${e.date}:${e.entryType}:${e.activityType}:${e.cropName}:${e.totalAmount}:${e.quantity}:${e.fieldName}:${e.buyerOrMandi}"

    suspend fun syncKhataTransaction(e: KhataEntryEntity): Boolean =
        send("/api/sync/khata", khata(e, khataStableId(e)))

    suspend fun syncMultipleKhataEntries(entries: List<KhataEntryEntity>): Boolean {
        var ok = true
        entries.forEach { entry -> ok = syncKhataTransaction(entry) && ok }
        return ok
    }

    /** Removes a khata row from Supabase after it was deleted on the device. */
    suspend fun deleteKhataTransaction(localDbId: Long): Boolean =
        delete("/api/sync/khata/db:$localDbId")

    private fun diseaseStableId(s: DiseaseScanEntity): String =
        if (s.id > 0) "db:${s.id}" else "legacy:${s.timestamp}:${s.cropName}:${s.diseaseNameEn}"

    suspend fun syncDiseaseDetection(s: DiseaseScanEntity): Boolean = send(
        "/api/sync/disease",
        JSONObject().apply {
            put("local_id", diseaseStableId(s))
            put("crop_name", s.cropName)
            put("disease_name", s.diseaseNameEn)
            put("disease_name_ur", s.diseaseNameUr)
            put("confidence", s.confidencePercent)
            put("severity", s.severityLevel)
            put("symptoms", s.symptoms)
            put("treatment_chemical", s.chemicalTreatment)
            put("treatment_organic", s.organicPrevention)
            put("recommendation", s.advisoryNote)
        }
    )

    /** Removes a disease scan and its stored image from Supabase. */
    suspend fun deleteDiseaseDetection(localDbId: Long): Boolean =
        delete("/api/sync/disease/db:$localDbId")

    /** Uploads the original disease image through Vercel to private Supabase Storage. */
    suspend fun uploadDiseaseImage(filePath: String, localId: String, cropName: String): Result<String> = withContext(Dispatchers.IO) {
        if (!session.hasSession()) return@withContext Result.failure(Exception("No active session"))
        val file = File(filePath)
        if (!file.exists()) return@withContext Result.failure(Exception("Image file not found"))

        val response = api.call { token ->
            // Rebuilt per attempt so the retry after a token refresh sends a fresh body.
            val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("local_id", localId)
                .addFormDataPart("crop_name", cropName)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .build()
            Request.Builder()
                .url(ApiConfig.endpoint("/api/disease-images/upload"))
                .header("Authorization", "Bearer $token")
                .post(multipart)
                .build()
        } ?: return@withContext Result.failure(Exception("Image upload failed: no active session or network error"))

        if (!response.isSuccessful) {
            return@withContext Result.failure(Exception(response.errorDetail("Image upload failed")))
        }
        Result.success(response.json().optString("path"))
    }

    /**
     * Restores the signed-in farmer's cloud state (profile, crops, khata and
     * disease history) after a login on a new device or a fresh install.
     */
    suspend fun fetchBootstrap(): JSONObject? {
        val response = api.call { token ->
            Request.Builder()
                .url(ApiConfig.endpoint("/api/sync/bootstrap"))
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .get()
                .build()
        } ?: return null
        if (!response.isSuccessful) return null
        return runCatching { response.json() }.getOrNull()
    }

    companion object {
        fun jsonArrayOrEmpty(root: JSONObject?, key: String): JSONArray =
            root?.optJSONArray(key) ?: JSONArray()
    }
}
