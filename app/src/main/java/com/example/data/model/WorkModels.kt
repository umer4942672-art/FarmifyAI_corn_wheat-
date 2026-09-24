package com.example.data.model

import org.json.JSONObject

/**
 * Contractor verification models, parsed from the backend's JSON.
 * Numeric columns arrive from PostgreSQL as numbers or strings, so every
 * numeric read goes through [num] to accept either.
 */

enum class UserRole { LANDOWNER, CONTRACTOR;
    companion object {
        fun from(value: String?) = if (value == "contractor") CONTRACTOR else LANDOWNER
    }
}

data class PartyInfo(val name: String, val phone: String, val email: String) {
    val display: String get() = name.ifBlank { email.ifBlank { phone } }
}

data class ContractorLink(
    val id: String,
    val landownerId: String,
    val contractorId: String,
    val status: String,
    val otherParty: PartyInfo
) {
    val isActive get() = status == "active"
    val isPending get() = status == "pending"
}

data class WorkBalance(
    val agreedAmount: Double,
    val payable: Double,
    val paid: Double,
    val expensesPaid: Double,
    val balance: Double
)

data class WorkOrder(
    val id: String,
    val landownerId: String,
    val contractorId: String,
    val taskType: String,
    val fieldName: String,
    val cropName: String,
    val notes: String,
    val areaAcres: Double,
    val ratePerAcre: Double,
    val totalAmount: Double,
    val advanceAmount: Double,
    val fieldLat: Double?,
    val fieldLng: Double?,
    val verifiedAcres: Double?,
    val unreadMessages: Int,
    val reviewNote: String,
    val status: String,
    val createdAt: String,
    val balance: WorkBalance?
)

data class WorkProof(
    val id: String,
    val stage: String,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val accuracyM: Double?,
    val capturedAt: String,
    val distanceFromFieldM: Double?,
    val isMockLocation: Boolean,
    val clockWarning: Boolean
)

data class WorkPayment(
    val id: String,
    val payerId: String,
    val payeeId: String,
    val kind: String,
    val amount: Double,
    val method: String,
    val transactionRef: String,
    val note: String,
    val status: String,
    val createdAt: String
)

data class WorkMessage(
    val id: String,
    val senderId: String,
    val kind: String,
    val body: String,
    val createdAt: String
) {
    val isSystem get() = kind == "system"
}

data class WorkOrderDetail(
    val viewerRole: UserRole,
    val order: WorkOrder,
    val proofs: List<WorkProof>,
    val payments: List<WorkPayment>,
    val balance: WorkBalance?
)

// ---------------------------------------------------------------------------
// JSON parsing
// ---------------------------------------------------------------------------
private fun JSONObject.num(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return when (val v = get(key)) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
}

private fun JSONObject.str(key: String): String =
    if (!has(key) || isNull(key)) "" else optString(key, "")

fun parseBalance(o: JSONObject?): WorkBalance? {
    if (o == null) return null
    return WorkBalance(
        agreedAmount = o.num("agreed_amount") ?: 0.0,
        payable = o.num("payable") ?: 0.0,
        paid = o.num("paid") ?: 0.0,
        expensesPaid = o.num("expenses_paid") ?: 0.0,
        balance = o.num("balance") ?: 0.0
    )
}

fun parseWorkOrder(o: JSONObject): WorkOrder = WorkOrder(
    id = o.str("id"),
    landownerId = o.str("landowner_id"),
    contractorId = o.str("contractor_id"),
    taskType = o.str("task_type"),
    fieldName = o.str("field_name"),
    cropName = o.str("crop_name"),
    notes = o.str("notes"),
    areaAcres = o.num("area_acres") ?: 0.0,
    ratePerAcre = o.num("rate_per_acre") ?: 0.0,
    totalAmount = o.num("total_amount") ?: 0.0,
    advanceAmount = o.num("advance_amount") ?: 0.0,
    fieldLat = o.num("field_lat"),
    fieldLng = o.num("field_lng"),
    verifiedAcres = o.num("verified_acres"),
    unreadMessages = (o.num("unread_messages") ?: 0.0).toInt(),
    reviewNote = o.str("review_note"),
    status = o.str("status"),
    createdAt = o.str("created_at"),
    balance = parseBalance(o.optJSONObject("balance"))
)

fun parseProof(o: JSONObject): WorkProof = WorkProof(
    id = o.str("id"),
    stage = o.str("stage"),
    imageUrl = o.str("image_url").ifBlank { null },
    latitude = o.num("latitude") ?: 0.0,
    longitude = o.num("longitude") ?: 0.0,
    accuracyM = o.num("accuracy_m"),
    capturedAt = o.str("captured_at"),
    distanceFromFieldM = o.num("distance_from_field_m"),
    isMockLocation = o.optBoolean("is_mock_location", false),
    clockWarning = o.optBoolean("clock_warning", false)
)

fun parsePayment(o: JSONObject): WorkPayment = WorkPayment(
    id = o.str("id"),
    payerId = o.str("payer_id"),
    payeeId = o.str("payee_id"),
    kind = o.str("kind"),
    amount = o.num("amount") ?: 0.0,
    method = o.str("method"),
    transactionRef = o.str("transaction_ref"),
    note = o.str("note"),
    status = o.str("status"),
    createdAt = o.str("created_at")
)

fun parseLink(o: JSONObject): ContractorLink {
    val p = o.optJSONObject("other_party") ?: JSONObject()
    return ContractorLink(
        id = o.str("id"),
        landownerId = o.str("landowner_id"),
        contractorId = o.str("contractor_id"),
        status = o.str("status"),
        otherParty = PartyInfo(p.str("name"), p.str("phone"), p.str("email"))
    )
}

fun parseMessage(o: JSONObject): WorkMessage = WorkMessage(
    id = o.str("id"),
    senderId = o.str("sender_id"),
    kind = o.str("kind"),
    body = o.str("body"),
    createdAt = o.str("created_at")
)
