package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ContractorLink
import com.example.data.model.UserRole
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderDetail
import com.example.data.repository.CaptureOutcome
import com.example.data.repository.WorkRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the landowner and contractor verification screens.
 *
 * Kept apart from MainViewModel so the feature can be reasoned about on its
 * own; it shares nothing but the database and the backend client.
 */
class WorkViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = WorkRepository(
        app, AppDatabase.getDatabase(app).pendingProofDao()
    )
    private val api get() = repository.api

    private val _role = MutableStateFlow(repository.cachedRole())
    val role: StateFlow<UserRole> = _role.asStateFlow()

    private val _orders = MutableStateFlow<List<WorkOrder>>(emptyList())
    val orders: StateFlow<List<WorkOrder>> = _orders.asStateFlow()

    private val _links = MutableStateFlow<List<ContractorLink>>(emptyList())
    val links: StateFlow<List<ContractorLink>> = _links.asStateFlow()

    private val _detail = MutableStateFlow<WorkOrderDetail?>(null)
    val detail: StateFlow<WorkOrderDetail?> = _detail.asStateFlow()

    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        refreshAll()
    }

    private suspend fun say(text: String) {
        _message.emit(text)
    }

    private suspend fun report(result: Result<*>, success: String) {
        if (result.isSuccess) say(success)
        else say(result.exceptionOrNull()?.message ?: "Something went wrong")
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isLoading.value = true
            _role.value = repository.resolveRole()
            api.listOrders().onSuccess { _orders.value = it }
            api.listLinks().onSuccess { _links.value = it }
            // Anything captured while offline goes out as soon as there is a session.
            val sent = repository.flushPendingProofs()
            if (sent > 0) say("$sent pending photo(s) uploaded")
            _isLoading.value = false
        }
    }

    fun chooseRoleAtSignup(role: UserRole) = repository.rememberRoleChoice(role)

    // --- contractors ------------------------------------------------------
    fun inviteContractor(emailOrPhone: String) {
        viewModelScope.launch {
            val r = api.inviteContractor(emailOrPhone)
            report(r, "Invitation sent. The contractor must accept it before you can assign work.")
            if (r.isSuccess) api.listLinks().onSuccess { _links.value = it }
        }
    }

    fun acceptLink(linkId: String) {
        viewModelScope.launch {
            val r = api.acceptLink(linkId)
            report(r, "Invitation accepted")
            if (r.isSuccess) api.listLinks().onSuccess { _links.value = it }
        }
    }

    // --- work orders ------------------------------------------------------
    fun createOrder(
        contractorId: String,
        taskType: String,
        fieldName: String,
        cropName: String,
        notes: String,
        areaAcres: Double,
        ratePerAcre: Double,
        advance: Double,
        lat: Double?,
        lng: Double?
    ) {
        viewModelScope.launch {
            val r = api.createOrder(
                contractorId, taskType, fieldName, cropName, notes,
                areaAcres, ratePerAcre, advance, lat, lng
            )
            report(r, "Work sent to the contractor")
            if (r.isSuccess) api.listOrders().onSuccess { _orders.value = it }
        }
    }

    fun openOrder(orderId: String) {
        _selectedOrderId.value = orderId
        loadDetail(orderId)
    }

    fun closeOrder() {
        _selectedOrderId.value = null
        _detail.value = null
    }

    private fun loadDetail(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            api.getOrder(orderId)
                .onSuccess { _detail.value = it }
                .onFailure { say(it.message ?: "Could not open this job") }
            _isLoading.value = false
        }
    }

    private fun refreshCurrent() {
        _selectedOrderId.value?.let { loadDetail(it) }
        viewModelScope.launch { api.listOrders().onSuccess { _orders.value = it } }
    }

    fun acceptOrder(orderId: String) = act(orderId, "Work accepted") { api.acceptOrder(it) }
    fun declineOrder(orderId: String) = act(orderId, "Work declined") { api.declineOrder(it) }
    fun submitOrder(orderId: String) = act(orderId, "Sent to the landowner for review") { api.submitOrder(it) }

    private fun act(orderId: String, success: String, call: suspend (String) -> Result<Unit>) {
        viewModelScope.launch {
            val r = call(orderId)
            report(r, success)
            if (r.isSuccess) refreshCurrent()
        }
    }

    fun review(orderId: String, decision: String, verifiedAcres: Double?, note: String) {
        viewModelScope.launch {
            val r = api.reviewOrder(orderId, decision, verifiedAcres, note)
            report(
                r,
                when (decision) {
                    "approved" -> "Work approved in full"
                    "partial" -> "Partly approved. The balance now follows the verified area."
                    else -> "Dispute recorded. The contractor can add more evidence."
                }
            )
            if (r.isSuccess) refreshCurrent()
        }
    }

    // --- evidence ---------------------------------------------------------
    fun captureProof(orderId: String, stage: String, bitmap: Bitmap) {
        viewModelScope.launch {
            _isCapturing.value = true
            when (val outcome = repository.captureProof(orderId, stage, bitmap)) {
                is CaptureOutcome.Uploaded -> {
                    say("Photo saved with its location and time")
                    refreshCurrent()
                }
                is CaptureOutcome.QueuedOffline ->
                    say("No signal. The photo is saved with its field location and will upload later.")
                is CaptureOutcome.Failed -> say(outcome.message)
            }
            _isCapturing.value = false
        }
    }

    // --- payments ---------------------------------------------------------
    fun recordPayment(
        orderId: String,
        kind: String,
        amount: Double,
        method: String,
        transactionRef: String,
        note: String
    ) {
        viewModelScope.launch {
            val r = api.recordPayment(orderId, kind, amount, method, transactionRef, note)
            report(r, "Payment recorded. It counts once the contractor confirms it.")
            if (r.isSuccess) refreshCurrent()
        }
    }

    fun confirmPayment(paymentId: String, received: Boolean) {
        viewModelScope.launch {
            val r = api.confirmPayment(paymentId, received)
            report(r, if (received) "Payment confirmed" else "Marked as not received")
            if (r.isSuccess) refreshCurrent()
        }
    }
}
