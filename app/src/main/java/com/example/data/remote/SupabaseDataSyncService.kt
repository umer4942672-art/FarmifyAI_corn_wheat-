package com.example.data.remote

import android.content.Context
import com.example.data.local.DiseaseScanEntity
import com.example.data.local.KhataEntryEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import org.json.JSONObject

/** Sends cloud data through the authenticated FarmifyAI backend. */
class SupabaseDataSyncService(context: Context) {
    private val client = OkHttpClient()
    private val media = "application/json; charset=utf-8".toMediaType()
    private val session = AuthSessionStore(context)

    private suspend fun send(path: String, data: JSONObject): Boolean = withContext(Dispatchers.IO) {
        val token = session.accessToken() ?: return@withContext false
        try {
            val body = JSONObject().put("data", data).toString().toRequestBody(media)
            client.newCall(
                Request.Builder()
                    .url(ApiConfig.endpoint(path))
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .post(body)
                    .build()
            ).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }

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

    suspend fun syncDiseaseDetection(s: DiseaseScanEntity): Boolean = send(
        "/api/sync/disease",
        JSONObject().apply {
            put("local_id", if (s.id > 0) "db:${s.id}" else "legacy:${s.timestamp}:${s.cropName}:${s.diseaseNameEn}")
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
    /** Uploads the original disease image through Vercel to private Supabase Storage. */
    suspend fun uploadDiseaseImage(filePath: String, localId: String, cropName: String): Result<String> = withContext(Dispatchers.IO) {
        val token = session.accessToken() ?: return@withContext Result.failure(Exception("No active session"))
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext Result.failure(Exception("Image file not found"))
            val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("local_id", localId)
                .addFormDataPart("crop_name", cropName)
                .addFormDataPart("file", file.name, file.asRequestBody("image/jpeg".toMediaType()))
                .build()
            val request = Request.Builder().url(ApiConfig.endpoint("/api/disease-images/upload"))
                .header("Authorization", "Bearer $token").post(multipart).build()
            client.newCall(request).execute().use { response ->
                val json = JSONObject(response.body?.string().orEmpty())
                if (!response.isSuccessful) return@withContext Result.failure(Exception(json.optString("detail", "Image upload failed")))
                Result.success(json.optString("path"))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

}
