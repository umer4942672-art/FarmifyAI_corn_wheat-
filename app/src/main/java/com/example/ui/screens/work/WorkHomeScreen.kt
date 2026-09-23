package com.example.ui.screens.work

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContractorLink
import com.example.data.model.UserRole
import com.example.data.model.WorkOrder
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkViewModel
import com.example.util.LocalAppLanguage

/** Colour and wording for each stage of a job. */
@Composable
fun statusStyle(status: String, isUrdu: Boolean): Pair<String, androidx.compose.ui.graphics.Color> =
    when (status) {
        "proposed" -> (if (isUrdu) "منظوری کا انتظار" else "Awaiting acceptance") to AmberOrange
        "accepted" -> (if (isUrdu) "کام جاری" else "In progress") to EmeraldGreen
        "submitted" -> (if (isUrdu) "جائزے کے لیے" else "Awaiting review") to GoldenYellow
        "approved" -> (if (isUrdu) "منظور شدہ" else "Approved") to SuccessGreen
        "partial" -> (if (isUrdu) "جزوی منظور" else "Partly approved") to AmberOrange
        "disputed" -> (if (isUrdu) "اختلاف" else "Disputed") to ErrorRed
        "closed" -> (if (isUrdu) "مکمل" else "Settled") to ForestGreen
        else -> (if (isUrdu) "منسوخ" else "Cancelled") to TextMuted
    }

fun taskLabel(task: String, isUrdu: Boolean): String = when (task) {
    "spray" -> if (isUrdu) "سپرے" else "Spray"
    "plough" -> if (isUrdu) "ہل چلانا" else "Ploughing"
    "fertilizer" -> if (isUrdu) "کھاد" else "Fertilizer"
    "sowing" -> if (isUrdu) "بوائی" else "Sowing"
    "harvest" -> if (isUrdu) "کٹائی" else "Harvest"
    "irrigation" -> if (isUrdu) "آبپاشی" else "Irrigation"
    else -> if (isUrdu) "دیگر" else "Other"
}

fun rupees(value: Double): String = "Rs " + String.format("%,.0f", value)

@Composable
fun WorkHomeScreen(
    workViewModel: WorkViewModel,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val isUrdu = lang.isUrdu
    val role by workViewModel.role.collectAsState()
    val orders by workViewModel.orders.collectAsState()
    val links by workViewModel.links.collectAsState()
    val isLoading by workViewModel.isLoading.collectAsState()

    var showInvite by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { workViewModel.refreshAll() }

    Box(modifier = modifier.fillMaxSize().background(PaleGreenBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (role == UserRole.LANDOWNER) {
                        if (isUrdu) "ٹھیکیدار کے کام" else "Contractor work"
                    } else {
                        if (isUrdu) "میرے کام" else "My jobs"
                    },
                    fontFamily = null,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (isUrdu)
                        "ہر کام کی تصویر، جگہ اور وقت محفوظ رہتا ہے، اور ادائیگی دونوں کی تصدیق سے گنی جاتی ہے۔"
                    else
                        "Every job keeps its photo, place and time, and a payment counts once both sides confirm it.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }

            if (isLoading) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = EmeraldGreen) }
            }

            // Contractors a landowner works with, or invitations a contractor received.
            item {
                ContractorsCard(
                    role = role,
                    links = links,
                    isUrdu = isUrdu,
                    onInvite = { showInvite = true },
                    onAccept = workViewModel::acceptLink
                )
            }

            item {
                Text(
                    text = if (isUrdu) "کام" else "Jobs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            if (orders.isEmpty() && !isLoading) {
                item {
                    EmptyCard(
                        text = if (role == UserRole.LANDOWNER) {
                            if (isUrdu) "ابھی کوئی کام نہیں۔ نیچے سے نیا کام بنائیں۔"
                            else "No jobs yet. Create one with the button below."
                        } else {
                            if (isUrdu) "ابھی آپ کو کوئی کام نہیں ملا۔"
                            else "No jobs have been assigned to you yet."
                        }
                    )
                }
            }

            items(orders, key = { it.id }) { order ->
                WorkOrderCard(order, isUrdu) { onOpenOrder(order.id) }
            }
        }

        if (role == UserRole.LANDOWNER) {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                containerColor = EmeraldGreen,
                contentColor = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isUrdu) "نیا کام" else "New job", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showInvite) {
        InviteContractorDialog(
            isUrdu = isUrdu,
            onDismiss = { showInvite = false },
            onInvite = {
                workViewModel.inviteContractor(it)
                showInvite = false
            }
        )
    }

    if (showCreate) {
        CreateWorkOrderDialog(
            isUrdu = isUrdu,
            contractors = links.filter { it.isActive },
            onDismiss = { showCreate = false },
            onCreate = { contractorId, task, field, crop, notes, acres, rate, advance ->
                workViewModel.createOrder(contractorId, task, field, crop, notes, acres, rate, advance, null, null)
                showCreate = false
            }
        )
    }
}

@Composable
private fun EmptyCard(text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(18.dp)
        )
    }
}

@Composable
private fun ContractorsCard(
    role: UserRole,
    links: List<ContractorLink>,
    isUrdu: Boolean,
    onInvite: () -> Unit,
    onAccept: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (role == UserRole.LANDOWNER) {
                        if (isUrdu) "میرے ٹھیکیدار" else "My contractors"
                    } else {
                        if (isUrdu) "زمیندار" else "Landowners"
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestGreen,
                    modifier = Modifier.weight(1f)
                )
                if (role == UserRole.LANDOWNER) {
                    TextButton(onClick = onInvite) {
                        Text(if (isUrdu) "شامل کریں" else "Add", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            if (links.isEmpty()) {
                Text(
                    text = if (role == UserRole.LANDOWNER) {
                        if (isUrdu) "ابھی کوئی ٹھیکیدار شامل نہیں۔ کام دینے سے پہلے ٹھیکیدار کو شامل کریں۔"
                        else "No contractors yet. Add one before assigning work."
                    } else {
                        if (isUrdu) "ابھی کسی زمیندار نے آپ کو شامل نہیں کیا۔"
                        else "No landowner has added you yet."
                    },
                    fontSize = 12.sp, color = TextSecondary, lineHeight = 17.sp
                )
            } else {
                links.forEach { link ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(link.otherParty.display, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1)
                            Text(
                                text = if (link.isActive) (if (isUrdu) "فعال" else "Active")
                                else (if (isUrdu) "دعوت بھیجی گئی" else "Invitation sent"),
                                fontSize = 11.sp,
                                color = if (link.isActive) SuccessGreen else AmberOrange
                            )
                        }
                        if (role == UserRole.CONTRACTOR && link.isPending) {
                            Button(
                                onClick = { onAccept(link.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) { Text(if (isUrdu) "قبول" else "Accept", fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrderCard(order: WorkOrder, isUrdu: Boolean, onClick: () -> Unit) {
    val (label, colour) = statusStyle(order.status, isUrdu)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${taskLabel(order.taskType, isUrdu)} · ${order.fieldName}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    modifier = Modifier.weight(1f), maxLines = 1
                )
                Surface(shape = RoundedCornerShape(8.dp), color = colour.copy(alpha = 0.14f)) {
                    Text(
                        label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colour,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${order.areaAcres} " + (if (isUrdu) "ایکڑ" else "acre") +
                    " × ${rupees(order.ratePerAcre)}  =  ${rupees(order.totalAmount)}",
                fontSize = 12.5.sp, color = TextSecondary
            )
            order.balance?.let { b ->
                if (b.payable > 0 || b.paid > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = (if (isUrdu) "باقی: " else "Balance: ") + rupees(b.balance),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (b.balance > 0) ErrorRed else SuccessGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun InviteContractorDialog(isUrdu: Boolean, onDismiss: () -> Unit, onInvite: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (isUrdu) "ٹھیکیدار شامل کریں" else "Add a contractor", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = if (isUrdu)
                        "ٹھیکیدار کا ای میل یا فون درج کریں۔ اس کا اکاؤنٹ ٹھیکیدار کے طور پر بنا ہونا چاہیے۔"
                    else
                        "Enter the contractor's email or phone. They must already have a contractor account.",
                    fontSize = 12.5.sp, color = TextSecondary, lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    placeholder = { Text("contractor@farmify.pk", fontSize = 13.sp, color = TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        cursorColor = EmeraldGreen, focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = BorderSlate,
                        focusedContainerColor = SoftWhite, unfocusedContainerColor = OffWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (value.isNotBlank()) onInvite(value) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isUrdu) "دعوت بھیجیں" else "Send invitation", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isUrdu) "بند" else "Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun CreateWorkOrderDialog(
    isUrdu: Boolean,
    contractors: List<ContractorLink>,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, Double, Double, Double) -> Unit
) {
    var contractorId by remember { mutableStateOf(contractors.firstOrNull()?.contractorId ?: "") }
    var task by remember { mutableStateOf("spray") }
    var field by remember { mutableStateOf("") }
    var crop by remember { mutableStateOf("") }
    var acres by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var advance by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf("") }

    val acresValue = acres.toDoubleOrNull() ?: 0.0
    val rateValue = rate.toDoubleOrNull() ?: 0.0
    val total = acresValue * rateValue

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        shape = RoundedCornerShape(18.dp),
        title = { Text(if (isUrdu) "نیا کام" else "New job", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (contractors.isEmpty()) {
                    Text(
                        text = if (isUrdu)
                            "پہلے کوئی ٹھیکیدار شامل کریں اور اس کے قبول کرنے کا انتظار کریں۔"
                        else
                            "Add a contractor first and wait for them to accept.",
                        fontSize = 13.sp, color = ErrorRed
                    )
                } else {
                    LabelledDropdown(
                        label = if (isUrdu) "ٹھیکیدار" else "Contractor",
                        options = contractors.map { it.contractorId to it.otherParty.display },
                        selected = contractorId,
                        onSelect = { contractorId = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    LabelledDropdown(
                        label = if (isUrdu) "کام کی قسم" else "Task",
                        options = listOf("spray", "plough", "fertilizer", "sowing", "harvest", "irrigation", "other")
                            .map { it to taskLabel(it, isUrdu) },
                        selected = task,
                        onSelect = { task = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    SmallField(if (isUrdu) "کھیت کا نام" else "Field name", field, { field = it })
                    Spacer(Modifier.height(8.dp))
                    SmallField(if (isUrdu) "فصل (اختیاری)" else "Crop (optional)", crop, { crop = it })
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            SmallField(if (isUrdu) "رقبہ (ایکڑ)" else "Area (acre)", acres, { acres = it }, numeric = true)
                        }
                        Box(Modifier.weight(1f)) {
                            SmallField(if (isUrdu) "فی ایکڑ ریٹ" else "Rate per acre", rate, { rate = it }, numeric = true)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SmallField(if (isUrdu) "ایڈوانس" else "Advance", advance, { advance = it }, numeric = true)

                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(12.dp), color = PaleGreenBg, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = (if (isUrdu) "طے شدہ کل: " else "Agreed total: ") + rupees(total),
                            fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ForestGreen,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    if (error.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, fontSize = 12.sp, color = ErrorRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = contractors.isNotEmpty(),
                onClick = {
                    val adv = advance.toDoubleOrNull() ?: 0.0
                    error = when {
                        field.isBlank() -> if (isUrdu) "کھیت کا نام درج کریں" else "Enter the field name"
                        acresValue <= 0 -> if (isUrdu) "رقبہ درج کریں" else "Enter the area"
                        rateValue < 0 -> if (isUrdu) "ریٹ درست نہیں" else "Rate is not valid"
                        adv > total -> if (isUrdu) "ایڈوانس کل رقم سے زیادہ نہیں ہو سکتا"
                                       else "Advance cannot exceed the agreed total"
                        else -> ""
                    }
                    if (error.isBlank()) {
                        onCreate(contractorId, task, field.trim(), crop.trim(), "", acresValue, rateValue, adv)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isUrdu) "بھیجیں" else "Send", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isUrdu) "بند" else "Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun SmallField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    numeric: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = TextSecondary) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
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

@Composable
private fun LabelledDropdown(
    label: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: ""
    Column {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = OffWhite,
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(current, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (key, text) ->
                DropdownMenuItem(
                    text = { Text(text, fontSize = 13.sp) },
                    onClick = { onSelect(key); expanded = false }
                )
            }
        }
    }
}
