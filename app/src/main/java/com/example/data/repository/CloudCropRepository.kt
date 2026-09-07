package com.example.data.repository

import android.content.Context
import com.example.data.remote.ApiConfig
import com.example.data.remote.AuthSessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class CloudCrop(
    val id: String = "",
    val cropName: String,
    val variety: String = "",
    val area: Double? = null,
    val areaUnit: String = "Acres",
    val sowingDate: String? = null,
    val expectedHarvestDate: String? = null,
    val status: String = "Active",
    val notes: String = ""
)

/** All crop CRUD goes through Vercel; the APK never talks to Supabase directly. */
class CloudCropRepository(context: Context) {
    private val client = OkHttpClient()
    private val session = AuthSessionStore(context.applicationContext)
    private val media = "application/json; charset=utf-8".toMediaType()

    private fun request(path: String, method: String, body: JSONObject? = null): Request? {
        val token = session.accessToken() ?: return null
        val b = Request.Builder().url(ApiConfig.endpoint(path)).header("Authorization", "Bearer $token")
        return when (method) {
            "GET" -> b.get().build()
            "DELETE" -> b.delete().build()
            else -> b.method(method, (body ?: JSONObject()).toString().toRequestBody(media)).build()
        }
    }

    suspend fun list(): Result<List<CloudCrop>> = withContext(Dispatchers.IO) {
        try {
            val req = request("/api/crops", "GET") ?: return@withContext Result.failure(Exception("No active session"))
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext Result.failure(Exception("Failed to load crops (${r.code})"))
                val arr = JSONObject(r.body?.string().orEmpty()).optJSONArray("crops")
                val out = buildList { for (i in 0 until (arr?.length() ?: 0)) { val x=arr!!.getJSONObject(i); add(CloudCrop(x.optString("id"),x.optString("crop_name"),x.optString("variety"),if(x.isNull("area")) null else x.optDouble("area"),x.optString("area_unit","Acres"),x.optString("sowing_date").ifBlank{null},x.optString("expected_harvest_date").ifBlank{null},x.optString("status","Active"),x.optString("notes"))) } }
                Result.success(out)
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun save(crop: CloudCrop, localId: String = crop.id.ifBlank { "android:${crop.cropName}:${System.currentTimeMillis()}" }): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val json=JSONObject().apply { put("local_id",localId); put("crop_name",crop.cropName); put("variety",crop.variety); put("area",crop.area); put("area_unit",crop.areaUnit); put("sowing_date",crop.sowingDate); put("expected_harvest_date",crop.expectedHarvestDate); put("status",crop.status); put("notes",crop.notes) }
            val path=if(crop.id.isBlank()) "/api/crops" else "/api/crops/${crop.id}"
            val method=if(crop.id.isBlank()) "POST" else "PUT"
            val req=request(path,method,json) ?: return@withContext Result.failure(Exception("No active session"))
            client.newCall(req).execute().use { if(it.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to save crop (${it.code})")) }
        } catch(e:Exception){Result.failure(e)}
    }

    suspend fun delete(id:String): Result<Unit> = withContext(Dispatchers.IO) {
        try { val req=request("/api/crops/$id","DELETE") ?: return@withContext Result.failure(Exception("No active session")); client.newCall(req).execute().use { if(it.isSuccessful) Result.success(Unit) else Result.failure(Exception("Failed to delete crop (${it.code})")) } } catch(e:Exception){Result.failure(e)}
    }
}
