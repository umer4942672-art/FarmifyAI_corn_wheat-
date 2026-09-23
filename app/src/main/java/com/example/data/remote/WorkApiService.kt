package com.example.data.remote

import android.content.Context
import com.example.data.model.ContractorLink
import com.example.data.model.UserRole
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderDetail
import com.example.data.model.parseBalance
import com.example.data.model.parseLink
import com.example.data.model.parsePayment
import com.example.data.model.parseProof
import com.example.data.model.parseWorkOrder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Calls the /api/work, /api/contractors and /api/payments endpoints.
 *
 * Every call goes through [AuthorizedApiClient], so an expired session is
 * refreshed transparently. Failures come back as [Result.failure] carrying the
 * server's own message, which the screens show as-is.
 */
class WorkApiService(context: Context) {

    private val api = AuthorizedApiClient(context.applicationContext)
    private val json = AuthorizedApiClient.JSON

    private suspend fun get(path: String): Result<JSONObject> = send { token ->
        Request.Builder().url(ApiConfig.endpoint(path))
            .header("Authorization", "Bearer $token").get().build()
    }

    private suspend fun post(path: String, body: JSONObject = JSONObject()): Result<JSONObject> = send { token ->
        Request.Builder().url(ApiConfig.endpoint(path))
            .header("Authorization", "Bearer $token")
            .post(body.toString().toRequestBody(json)).build()
    }

    private suspend fun send(build: (String) -> Request): Result<JSONObject> {
        if (!ApiConfig.isConfigured) {
            return Result.failure(Exception("This build has no backend URL configured"))
        }
        val response = api.call(build)
            ?: return Result.failure(Exception("Could not reach the server. Check your connection."))
        return if (response.isSuccessful) {
            Result.success(response.json())
        } else {
            Result.failure(Exception(response.errorDetail("Request failed (${response.code})")))
        }
    }

    // --- role -------------------------------------------------------------
    suspend fun getRole(): Result<UserRole> =
        get("/api/work/role").map { UserRole.from(it.optString("role")) }

    suspend fun setRole(role: UserRole): Result<UserRole> =
        post("/api/work/role", JSONObject().put("role", role.name.lowercase())).map { role }

    // --- contractor links ------------------------------------------------
    suspend fun listLinks(): Result<List<ContractorLink>> = get("/api/contractors").map { o ->
        val arr = o.optJSONArray("links")
        (0 until (arr?.length() ?: 0)).map { parseLink(arr!!.getJSONObject(it)) }
    }

    suspend fun inviteContractor(emailOrPhone: String): Result<Unit> {
        val v = emailOrPhone.trim()
        val body = if (v.contains("@")) JSONObject().put("email", v) else JSONObject().put("phone", v)
        return post("/api/contractors/invite", body).map { }
    }

    suspend fun acceptLink(linkId: String): Result<Unit> =
        post("/api/contractors/$linkId/accept").map { }

    // --- work orders -----------------------------------------------------
    suspend fun listOrders(): Result<List<WorkOrder>> = get("/api/work-orders").map { o ->
        val arr = o.optJSONArray("orders")
        (0 until (arr?.length() ?: 0)).map { parseWorkOrder(arr!!.getJSONObject(it)) }
    }

    suspend fun getOrder(orderId: String): Result<WorkOrderDetail> = get("/api/work-orders/$orderId").map { o ->
        val proofs = o.optJSONArray("proofs")
        val payments = o.optJSONArray("payments")
        WorkOrderDetail(
            viewerRole = UserRole.from(o.optString("viewer_role")),
            order = parseWorkOrder(o.getJSONObject("order")),
            proofs = (0 until (proofs?.length() ?: 0)).map { parseProof(proofs!!.getJSONObject(it)) },
            payments = (0 until (payments?.length() ?: 0)).map { parsePayment(payments!!.getJSONObject(it)) },
            balance = parseBalance(o.optJSONObject("balance"))
        )
    }

    suspend fun createOrder(
        contractorId: String,
        taskType: String,
        fieldName: String,
        cropName: String,
        notes: String,
        areaAcres: Double,
        ratePerAcre: Double,
        advance: Double,
        fieldLat: Double?,
        fieldLng: Double?
    ): Result<Unit> {
        val body = JSONObject()
            .put("contractor_id", contractorId)
            .put("task_type", taskType)
            .put("field_name", fieldName)
            .put("crop_name", cropName.ifBlank { JSONObject.NULL })
            .put("notes", notes.ifBlank { JSONObject.NULL })
            .put("area_acres", areaAcres)
            .put("rate_per_acre", ratePerAcre)
            .put("advance_amount", advance)
        if (fieldLat != null && fieldLng != null) {
            body.put("field_lat", fieldLat).put("field_lng", fieldLng)
        }
        return post("/api/work-orders", body).map { }
    }

    suspend fun acceptOrder(orderId: String) = post("/api/work-orders/$orderId/accept").map { }
    suspend fun declineOrder(orderId: String) = post("/api/work-orders/$orderId/decline").map { }
    suspend fun submitOrder(orderId: String) = post("/api/work-orders/$orderId/submit").map { }

    suspend fun reviewOrder(orderId: String, decision: String, verifiedAcres: Double?, note: String): Result<Unit> {
        val body = JSONObject().put("decision", decision).put("note", note)
        if (verifiedAcres != null) body.put("verified_acres", verifiedAcres)
        return post("/api/work-orders/$orderId/review", body).map { }
    }

    // --- evidence --------------------------------------------------------
    suspend fun uploadProof(
        orderId: String,
        stage: String,
        imageFile: File,
        latitude: Double,
        longitude: Double,
        accuracyM: Float?,
        capturedAtIso: String,
        isMock: Boolean
    ): Result<Unit> = send { token ->
        // Rebuilt on each attempt so a retry after a token refresh sends a fresh body.
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("stage", stage)
            .addFormDataPart("latitude", latitude.toString())
            .addFormDataPart("longitude", longitude.toString())
            .addFormDataPart("captured_at", capturedAtIso)
            .addFormDataPart("is_mock_location", isMock.toString())
            .apply { if (accuracyM != null) addFormDataPart("accuracy_m", accuracyM.toString()) }
            .addFormDataPart("file", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaType()))
            .build()
        Request.Builder().url(ApiConfig.endpoint("/api/work-orders/$orderId/proofs"))
            .header("Authorization", "Bearer $token").post(form).build()
    }.map { }

    // --- payments --------------------------------------------------------
    suspend fun recordPayment(
        orderId: String,
        kind: String,
        amount: Double,
        method: String,
        transactionRef: String,
        note: String
    ): Result<Unit> = post(
        "/api/work-orders/$orderId/payments",
        JSONObject()
            .put("kind", kind)
            .put("amount", amount)
            .put("method", method)
            .put("transaction_ref", transactionRef)
            .put("note", note)
    ).map { }

    suspend fun confirmPayment(paymentId: String, received: Boolean): Result<Unit> =
        post("/api/payments/$paymentId/confirm", JSONObject().put("received", received)).map { }
}
