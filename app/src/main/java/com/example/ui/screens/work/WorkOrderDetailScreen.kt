package com.example.ui.screens.work

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.UserRole
import com.example.data.model.WorkOrderDetail
import com.example.data.model.WorkMessage
import com.example.data.model.WorkPayment
import com.example.data.model.WorkProof
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkViewModel
import com.example.util.LocalAppLanguage
import com.example.util.VoiceInputManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun WorkOrderDetailScreen(
    workViewModel: WorkViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = LocalAppLanguage.current.isUrdu
    val detail by workViewModel.detail.collectAsState()
    val messages by workViewModel.messages.collectAsState()
    val viewerId by workViewModel.viewerId.collectAsState()
    val isSending by workViewModel.isSending.collectAsState()
    val isLoading by workViewModel.isLoading.collectAsState()
    val isCapturing by workViewModel.isCapturing.collectAsState()

    var showReview by remember { mutableStateOf(false) }
    var showPayment by remember { mutableStateOf(false) }
    var captureStage by remember { mutableStateOf("after") }

    val d = detail
    // Camera only. A gallery picker would allow an old photo from elsewhere,
    // which is exactly what this evidence is meant to rule out.
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null && d != null) {
            workViewModel.captureProof(d.order.id, captureStage, bitmap)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) cameraLauncher.launch(null)
    }

    fun capture(stage: String) {
        captureStage = stage
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }

    Box(Modifier.fillMaxSize().background(PaleGreenBg)) {
        if (d == null) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = EmeraldGreen)
            }
            return@Box
        }

        val order = d.order
        val isLandowner = d.viewerRole == UserRole.LANDOWNER
        val (statusLabel, statusColour) = statusStyle(order.status, isUrdu)

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ForestGreen)
                    }
                    Text(
                        "${taskLabel(order.taskType, isUrdu)} · ${order.fieldName}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ForestGreen,
                        modifier = Modifier.weight(1f), maxLines = 1
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = statusColour.copy(alpha = 0.14f)) {
                        Text(
                            statusLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColour,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            item { MoneyCard(d, isUrdu) }

            if (order.reviewNote.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ErrorRed.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (isUrdu) "زمیندار کا تبصرہ" else "Landowner's note",
                                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ErrorRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(order.reviewNote, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
            }

            // --- contractor actions ---------------------------------------
            if (!isLandowner) {
                when (order.status) {
                    "proposed" -> item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { workViewModel.acceptOrder(order.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text(if (isUrdu) "ریٹ قبول" else "Accept rate", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                            OutlinedButton(
                                onClick = { workViewModel.declineOrder(order.id) },
                                border = BorderStroke(1.5.dp, ErrorRed),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) { Text(if (isUrdu) "انکار" else "Decline", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }
                    "accepted", "disputed" -> item {
                        Column {
                            Text(
                                if (isUrdu) "کام کا ثبوت" else "Evidence",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (isUrdu) "تصویر ایپ کے کیمرے سے لی جائے گی، جگہ اور وقت خود لگ جائے گا۔"
                                else "Photos are taken with the app camera; place and time are attached automatically.",
                                fontSize = 11.5.sp, color = TextSecondary, lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CaptureButton(if (isUrdu) "پہلے" else "Before", isCapturing) { capture("before") }
                                CaptureButton(if (isUrdu) "بعد" else "After", isCapturing) { capture("after") }
                                CaptureButton(if (isUrdu) "رسید" else "Receipt", isCapturing) { capture("receipt") }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { workViewModel.submitOrder(order.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (isUrdu) "جائزے کے لیے بھیجیں" else "Send for review",
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- landowner review -----------------------------------------
            if (isLandowner && order.status == "submitted") {
                item {
                    Button(
                        onClick = { showReview = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Icon(Icons.Default.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isUrdu) "کام کا جائزہ لیں" else "Review the work", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --- evidence list --------------------------------------------
            item {
                Text(
                    (if (isUrdu) "ثبوت" else "Evidence") + " (${d.proofs.size})",
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary
                )
            }
            if (d.proofs.isEmpty()) {
                item {
                    Text(
                        if (isUrdu) "ابھی کوئی تصویر نہیں۔" else "No photos yet.",
                        fontSize = 12.5.sp, color = TextSecondary
                    )
                }
            }
            items(d.proofs.size) { index -> ProofCard(d.proofs[index], isUrdu) }

            // --- payments --------------------------------------------------
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isUrdu) "ادائیگیاں" else "Payments",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isLandowner && order.status !in listOf("proposed", "cancelled")) {
                        TextButton(onClick = { showPayment = true }) {
                            Text(
                                if (isUrdu) "ادائیگی درج کریں" else "Record payment",
                                color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            if (d.payments.isEmpty()) {
                item {
                    Text(
                        if (isUrdu) "ابھی کوئی ادائیگی نہیں۔" else "No payments yet.",
                        fontSize = 12.5.sp, color = TextSecondary
                    )
                }
            }
            items(d.payments.size) { index ->
                PaymentCard(
                    payment = d.payments[index],
                    canConfirm = !isLandowner && d.payments[index].status == "pending",
                    isUrdu = isUrdu,
                    onConfirm = { received -> workViewModel.confirmPayment(d.payments[index].id, received) }
                )
            }

            // --- messages ---------------------------------------------------
            item {
                Column {
                    Text(
                        if (isUrdu) "گفتگو" else "Messages",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (isUrdu)
                            "یہ باتیں اسی کام کے ساتھ محفوظ رہتی ہیں، ثبوت اور حساب کے ساتھ۔"
                        else
                            "These stay attached to this job, beside its evidence and its account.",
                        fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp
                    )
                }
            }
            items(messages.size) { index ->
                MessageRow(messages[index], viewerId, isUrdu)
            }
            item {
                MessageComposer(
                    isUrdu = isUrdu,
                    isSending = isSending,
                    enabled = d.order.status != "cancelled",
                    onSend = { workViewModel.sendMessage(d.order.id, it) }
                )
            }
        }

        if (isCapturing) {
            Surface(
                color = Color.Black.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isUrdu) "جگہ کا تعین ہو رہا ہے…" else "Getting the location…",
                        color = Color.White, fontSize = 13.sp
                    )
                }
            }
        }

        if (showReview) {
            ReviewDialog(
                isUrdu = isUrdu,
                totalAcres = order.areaAcres,
                ratePerAcre = order.ratePerAcre,
                onDismiss = { showReview = false },
                onSubmit = { decision, acres, note ->
                    workViewModel.review(order.id, decision, acres, note)
                    showReview = false
                }
            )
        }

        if (showPayment) {
            PaymentDialog(
                isUrdu = isUrdu,
                suggested = d.balance?.balance?.coerceAtLeast(0.0) ?: 0.0,
                onDismiss = { showPayment = false },
                onSubmit = { kind, amount, method, ref ->
                    workViewModel.recordPayment(order.id, kind, amount, method, ref, "")
                    showPayment = false
                }
            )
        }
    }
}

@Composable
private fun CaptureButton(label: String, busy: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = !busy,
        border = BorderStroke(1.5.dp, EmeraldGreen),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 6.dp),
        modifier = Modifier.height(46.dp).widthIn(min = 92.dp)
    ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun MoneyCard(d: WorkOrderDetail, isUrdu: Boolean) {
    val order = d.order
    val b = d.balance
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            MoneyRow(
                if (isUrdu) "طے شدہ" else "Agreed",
                "${order.areaAcres} × ${rupees(order.ratePerAcre)}",
                rupees(order.totalAmount), TextPrimary
            )
            if (order.verifiedAcres != null) {
                Spacer(Modifier.height(6.dp))
                MoneyRow(
                    if (isUrdu) "تصدیق شدہ" else "Verified",
                    "${order.verifiedAcres} × ${rupees(order.ratePerAcre)}",
                    rupees((b?.payable) ?: (order.verifiedAcres * order.ratePerAcre)), ForestGreen
                )
            }
            if (b != null) {
                Spacer(Modifier.height(6.dp))
                MoneyRow(if (isUrdu) "ادا شدہ" else "Paid", "", rupees(b.paid), SuccessGreen)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = BorderLight)
                Spacer(Modifier.height(8.dp))
                MoneyRow(
                    if (isUrdu) "باقی" else "Balance", "",
                    rupees(b.balance), if (b.balance > 0) ErrorRed else SuccessGreen, bold = true
                )
            }
            if (order.verifiedAcres == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isUrdu) "باقی رقم تصدیق شدہ رقبے سے بنے گی۔"
                    else "The balance is calculated from the verified area once the work is reviewed.",
                    fontSize = 11.5.sp, color = TextSecondary, lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun MoneyRow(label: String, detail: String, amount: String, colour: Color, bold: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = if (bold) 14.sp else 12.5.sp,
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = TextPrimary)
            if (detail.isNotBlank()) Text(detail, fontSize = 11.sp, color = TextSecondary)
        }
        Text(amount, fontSize = if (bold) 16.sp else 13.5.sp, fontWeight = FontWeight.Bold, color = colour)
    }
}

@Composable
private fun ProofCard(proof: WorkProof, isUrdu: Boolean) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(7.dp), color = EmeraldGreen.copy(alpha = 0.14f)) {
                    Text(
                        when (proof.stage) {
                            "before" -> if (isUrdu) "پہلے" else "Before"
                            "after" -> if (isUrdu) "بعد" else "After"
                            else -> if (isUrdu) "رسید" else "Receipt"
                        },
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(proof.capturedAt.take(16).replace("T", "  "), fontSize = 11.sp, color = TextSecondary)
            }
            if (proof.imageUrl != null) {
                Spacer(Modifier.height(10.dp))
                AsyncImage(
                    model = proof.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(10.dp))
                )
            }
            proof.distanceFromFieldM?.let { metres ->
                Spacer(Modifier.height(8.dp))
                val far = metres > 500
                Text(
                    text = (if (isUrdu) "کھیت سے فاصلہ: " else "Distance from the field: ") +
                        if (metres < 1000) "${metres.toInt()} m" else String.format("%.1f km", metres / 1000),
                    fontSize = 11.5.sp,
                    fontWeight = if (far) FontWeight.Bold else FontWeight.Normal,
                    color = if (far) ErrorRed else TextSecondary
                )
            }
            if (proof.isMockLocation || proof.clockWarning) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (proof.isMockLocation) {
                        if (isUrdu) "انتباہ: جگہ مصنوعی ہو سکتی ہے" else "Warning: the location may be simulated"
                    } else {
                        if (isUrdu) "انتباہ: فون کا وقت سرور سے مختلف تھا"
                        else "Warning: the device clock differed from the server"
                    },
                    fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    payment: WorkPayment,
    canConfirm: Boolean,
    isUrdu: Boolean,
    onConfirm: (Boolean) -> Unit
) {
    val statusColour = when (payment.status) {
        "confirmed" -> SuccessGreen
        "rejected" -> ErrorRed
        else -> AmberOrange
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(rupees(payment.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        text = buildString {
                            append(
                                when (payment.kind) {
                                    "advance" -> if (isUrdu) "ایڈوانس" else "Advance"
                                    "balance" -> if (isUrdu) "باقی" else "Balance"
                                    else -> if (isUrdu) "خرچہ" else "Expense"
                                }
                            )
                            append(" · ")
                            append(payment.method.replaceFirstChar { it.uppercase() })
                            if (payment.transactionRef.isNotBlank()) append(" · ${payment.transactionRef}")
                        },
                        fontSize = 11.5.sp, color = TextSecondary
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColour.copy(alpha = 0.14f)) {
                    Text(
                        when (payment.status) {
                            "confirmed" -> if (isUrdu) "تصدیق شدہ" else "Confirmed"
                            "rejected" -> if (isUrdu) "نہیں ملے" else "Not received"
                            else -> if (isUrdu) "تصدیق باقی" else "Awaiting confirmation"
                        },
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusColour,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            if (canConfirm) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onConfirm(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) { Text(if (isUrdu) "مل گئے" else "Received", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { onConfirm(false) },
                        border = BorderStroke(1.5.dp, ErrorRed),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) { Text(if (isUrdu) "نہیں ملے" else "Not received", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun ReviewDialog(
    isUrdu: Boolean,
    totalAcres: Double,
    ratePerAcre: Double,
    onDismiss: () -> Unit,
    onSubmit: (String, Double?, String) -> Unit
) {
    var decision by remember { mutableStateOf("approved") }
    var acres by remember { mutableStateOf(totalAcres.toString()) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    val acresValue = acres.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (isUrdu) "کام کا جائزہ" else "Review the work", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                listOf(
                    "approved" to (if (isUrdu) "پورا کام منظور" else "Approve in full"),
                    "partial" to (if (isUrdu) "جزوی منظور" else "Approve part of it"),
                    "disputed" to (if (isUrdu) "اختلاف درج کریں" else "Raise a dispute")
                ).forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = decision == key,
                            onClick = { decision = key },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldGreen)
                        )
                        Text(label, fontSize = 13.sp, color = TextPrimary)
                    }
                }
                if (decision == "partial") {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = acres,
                        onValueChange = { acres = it },
                        label = { Text(if (isUrdu) "تصدیق شدہ رقبہ (ایکڑ)" else "Verified acres", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = SoftWhite, unfocusedContainerColor = OffWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        (if (isUrdu) "قابلِ ادائیگی: " else "Payable: ") + rupees(acresValue * ratePerAcre),
                        fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ForestGreen
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            if (decision == "disputed") {
                                if (isUrdu) "وجہ (لازمی)" else "Reason (required)"
                            } else {
                                if (isUrdu) "تبصرہ (اختیاری)" else "Note (optional)"
                            },
                            fontSize = 11.sp
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = SoftWhite, unfocusedContainerColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, fontSize = 12.sp, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    error = when {
                        decision == "partial" && (acresValue <= 0 || acresValue >= totalAcres) ->
                            if (isUrdu) "رقبہ 0 سے زیادہ اور کل سے کم ہونا چاہیے"
                            else "Verified acres must be more than 0 and less than $totalAcres"
                        decision == "disputed" && note.isBlank() ->
                            if (isUrdu) "اختلاف کی وجہ لکھیں" else "Give a reason for the dispute"
                        else -> ""
                    }
                    if (error.isBlank()) {
                        onSubmit(decision, if (decision == "partial") acresValue else null, note.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isUrdu) "محفوظ کریں" else "Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isUrdu) "بند" else "Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun PaymentDialog(
    isUrdu: Boolean,
    suggested: Double,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String, String) -> Unit
) {
    var kind by remember { mutableStateOf("advance") }
    var amount by remember { mutableStateOf(if (suggested > 0) suggested.toInt().toString() else "") }
    var method by remember { mutableStateOf("cash") }
    var reference by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (isUrdu) "ادائیگی درج کریں" else "Record a payment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    if (isUrdu) "ٹھیکیدار کے تصدیق کرنے کے بعد ہی یہ حساب میں گنی جائے گی۔"
                    else "It counts towards the balance only once the contractor confirms it.",
                    fontSize = 12.sp, color = TextSecondary, lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                ChipRow(
                    options = listOf(
                        "advance" to (if (isUrdu) "ایڈوانس" else "Advance"),
                        "balance" to (if (isUrdu) "باقی" else "Balance"),
                        "expense" to (if (isUrdu) "خرچہ" else "Expense")
                    ),
                    selected = kind, onSelect = { kind = it }
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isUrdu) "رقم" else "Amount", fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = SoftWhite, unfocusedContainerColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                ChipRow(
                    options = listOf(
                        "cash" to (if (isUrdu) "نقد" else "Cash"),
                        "jazzcash" to "JazzCash",
                        "easypaisa" to "Easypaisa",
                        "bank" to (if (isUrdu) "بینک" else "Bank")
                    ),
                    selected = method, onSelect = { method = it }
                )
                if (method != "cash") {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = { Text(if (isUrdu) "ٹرانزیکشن آئی ڈی" else "Transaction ID", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = BorderSlate,
                            focusedContainerColor = SoftWhite, unfocusedContainerColor = OffWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (error.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(error, fontSize = 12.sp, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull() ?: 0.0
                    error = when {
                        value <= 0 -> if (isUrdu) "درست رقم درج کریں" else "Enter a valid amount"
                        method != "cash" && reference.isBlank() ->
                            if (isUrdu) "ڈیجیٹل ادائیگی کے لیے ٹرانزیکشن آئی ڈی لازمی ہے"
                            else "A transaction ID is required for digital payments"
                        else -> ""
                    }
                    if (error.isBlank()) onSubmit(kind, value, method, reference.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isUrdu) "درج کریں" else "Record", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isUrdu) "بند" else "Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (key, label) ->
            val active = key == selected
            Surface(
                onClick = { onSelect(key) },
                shape = RoundedCornerShape(10.dp),
                color = if (active) EmeraldGreen else Color.Transparent,
                border = BorderStroke(1.2.dp, if (active) EmeraldGreen else BorderSlate)
            ) {
                Text(
                    label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                    color = if (active) Color.White else TextPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    maxLines = 1
                )
            }
        }
    }
}


@Composable
private fun MessageRow(message: WorkMessage, viewerId: String, isUrdu: Boolean) {
    // System entries are the record of what happened, so they sit centred and
    // plain rather than looking like something either party said.
    if (message.isSystem) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(8.dp), color = PaleGreenBg) {
                Text(
                    message.body,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        return
    }

    val mine = message.senderId == viewerId
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp, topEnd = 14.dp,
                bottomStart = if (mine) 14.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 14.dp
            ),
            color = if (mine) EmeraldGreen else SoftWhite,
            border = if (mine) null else BorderStroke(1.dp, BorderLight),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(
                    message.body,
                    fontSize = 13.sp,
                    color = if (mine) Color.White else TextPrimary,
                    lineHeight = 18.sp
                )
                Text(
                    message.createdAt.take(16).replace("T", "  "),
                    fontSize = 9.5.sp,
                    color = if (mine) Color.White.copy(alpha = 0.75f) else TextMuted
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    isUrdu: Boolean,
    isSending: Boolean,
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var listening by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val voice = remember { VoiceInputManager(context) }

    DisposableEffect(Unit) { onDispose { voice.stop() } }

    // Spoken words land in the box for review, not straight into the thread,
    // which matters for a contractor who may not read back easily.
    fun listen() {
        listening = true
        voice.start(
            isUrdu = isUrdu,
            callbacks = object : VoiceInputManager.Callbacks {
                override fun onPartial(partial: String) { text = partial }
                override fun onFinal(final: String) { text = final; listening = false }
                override fun onError(message: String) { listening = false }
                override fun onEndOfSpeech() {}
            }
        )
    }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) listen() }

    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            enabled = enabled,
            placeholder = {
                Text(
                    if (isUrdu) "پیغام لکھیں یا بولیں" else "Write or speak a message",
                    fontSize = 12.5.sp, color = TextMuted
                )
            },
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                unfocusedBorderColor = BorderSlate,
                focusedContainerColor = SoftWhite, unfocusedContainerColor = SoftWhite
            ),
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = {
                if (listening) {
                    voice.stop(); listening = false
                } else if (voice.hasPermission()) {
                    listen()
                } else {
                    micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = enabled
        ) {
            Icon(
                if (listening) Icons.Filled.Mic else Icons.Filled.MicNone,
                contentDescription = null,
                tint = if (listening) ErrorRed else EmeraldGreen
            )
        }

        IconButton(
            onClick = { onSend(text); text = "" },
            enabled = enabled && !isSending && text.isNotBlank()
        ) {
            Icon(
                Icons.Default.Send,
                contentDescription = null,
                tint = if (text.isNotBlank()) EmeraldGreen else TextMuted
            )
        }
    }
}
