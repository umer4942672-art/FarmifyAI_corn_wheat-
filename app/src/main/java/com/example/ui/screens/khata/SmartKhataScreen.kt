package com.example.ui.screens.khata

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.KhataEntryEntity
import com.example.ui.components.CurrencyText
import com.example.ui.components.GlassCard
import com.example.ui.components.OfflineStatusPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocalAppLanguage
import com.example.util.str

@Composable
fun SmartKhataScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    val entries by viewModel.khataEntries.collectAsStateWithLifecycle()
    val stats by viewModel.khataSummary.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE", "FIELD_WORK"
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddFieldWorkDialog by remember { mutableStateOf(false) }

    val filteredEntries = remember(entries, selectedFilter) {
        when (selectedFilter) {
            "INCOME" -> entries.filter { it.entryType == "INCOME" }
            "EXPENSE" -> entries.filter { it.entryType == "EXPENSE" }
            "FIELD_WORK" -> entries.filter { it.entryType == "FIELD_WORK" }
            else -> entries
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("smart_khata_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // 1. Header Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = str("khata_title"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = str("khata_subtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(VeryLightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 2. Net Profit & Financial Overview Card
        item {
            GlassCard(
                backgroundColor = Color(0xFFF2F8F4),
                borderColor = BorderLight
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = str("net_profit"),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            OfflineStatusPill(isOnline = true)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        CurrencyText(
                            amount = stats.netProfit,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = if (stats.netProfit >= 0) SuccessGreen else ErrorRed
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SoftWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = if (langState.isUrdu) "سب سے زیادہ منافع" else "Top Crop Profit",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = stats.mostProfitableCrop,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Income Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = SoftWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F5E9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = str("total_income"),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyText(
                                amount = stats.totalIncome,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SuccessGreen
                            )
                        }
                    }

                    // Total Expense Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = SoftWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFEBEE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = ErrorRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = str("total_expense"),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            CurrencyText(
                                amount = stats.totalExpense,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ErrorRed
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Action Buttons Row (+ Income, + Expense, + Field Work)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddIncomeDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_income")
                ) {
                    Text(
                        text = str("add_income"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAddExpenseDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_add_expense")
                ) {
                    Text(
                        text = str("add_expense"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAddFieldWorkDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_add_field_work")
                ) {
                    Text(
                        text = str("add_field_work"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Filter Chips Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    label = { Text(str("filter_all")) }
                )
                FilterChip(
                    selected = selectedFilter == "INCOME",
                    onClick = { selectedFilter = "INCOME" },
                    label = { Text(str("filter_income")) }
                )
                FilterChip(
                    selected = selectedFilter == "EXPENSE",
                    onClick = { selectedFilter = "EXPENSE" },
                    label = { Text(str("filter_expense")) }
                )
                FilterChip(
                    selected = selectedFilter == "FIELD_WORK",
                    onClick = { selectedFilter = "FIELD_WORK" },
                    label = { Text(str("filter_field_work")) }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 5. Entries List or Empty State
        if (filteredEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = str("no_records"),
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.id }) { entry ->
                KhataEntryCard(
                    entry = entry,
                    onDelete = { viewModel.deleteKhataEntry(entry.id) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Dialogs
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onSave = { crop, qty, unit, price, buyer, field, desc ->
                viewModel.addIncome(crop, qty, unit, price, buyer, field, desc)
                showAddIncomeDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { crop, field, cat, amt, desc ->
                viewModel.addExpense(crop, field, cat, amt, desc)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddFieldWorkDialog) {
        AddFieldWorkDialog(
            onDismiss = { showAddFieldWorkDialog = false },
            onSave = { crop, field, acres, act, desc, labor, seed, fert, pest, irrig, mach, trans, other ->
                viewModel.addFieldWork(crop, field, acres, act, desc, labor, seed, fert, pest, irrig, mach, trans, other)
                showAddFieldWorkDialog = false
            }
        )
    }
}

@Composable
fun KhataEntryCard(
    entry: KhataEntryEntity,
    onDelete: () -> Unit
) {
    val langState = LocalAppLanguage.current
    var expanded by remember { mutableStateOf(false) }

    val isIncome = entry.entryType == "INCOME"
    val isFieldWork = entry.entryType == "FIELD_WORK"

    val typeColor = if (isIncome) SuccessGreen else if (isFieldWork) EmeraldGreen else ErrorRed
    val typeIcon = if (isIncome) Icons.Default.TrendingUp else if (isFieldWork) Icons.Default.Engineering else Icons.Default.TrendingDown

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("khata_entry_${entry.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (isFieldWork) "${entry.cropName} • ${entry.activityType}" else "${entry.cropName} (${entry.entryType})",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "${entry.date}${if (entry.fieldName.isNotBlank()) " • ${entry.fieldName}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    CurrencyText(
                        amount = if (isIncome) entry.effectiveIncome else entry.effectiveExpense,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = typeColor,
                        prefix = if (isIncome) "+ PKR" else "- PKR"
                    )

                    if (isIncome && entry.quantity > 0) {
                        Text(
                            text = "${entry.quantity.toInt()} ${entry.unit} @ ${entry.sellingPricePerUnit.toInt()}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            if (entry.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = if (expanded) 10 else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    HorizontalDivider(color = BorderLight)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isFieldWork) {
                        Text(
                            text = if (langState.isUrdu) "اخراجات کی تفصیل:" else "Cost Breakdown:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CostBreakdownRow("Fertilizer / کھاد", entry.fertilizerCost)
                        CostBreakdownRow("Pesticide / سپرے", entry.pesticideCost)
                        CostBreakdownRow("Labor / مزدوری", entry.laborCost)
                        CostBreakdownRow("Seed / بیج", entry.seedCost)
                        CostBreakdownRow("Irrigation / پانی", entry.irrigationCost)
                        CostBreakdownRow("Machinery / ٹریکٹر", entry.machineryCost)
                        CostBreakdownRow("Transport / کرایہ", entry.transportationCost)
                        CostBreakdownRow("Other / دیگر", entry.otherExpenses)
                    }

                    if (isIncome && entry.buyerOrMandi.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (langState.isUrdu) "خریدار / منڈی:" else "Buyer / Mandi:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = entry.buyerOrMandi,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (langState.isUrdu) "حذف کریں" else "Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CostBreakdownRow(label: String, amount: Double) {
    if (amount > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            CurrencyText(amount = amount, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

// ----------------------------------------------------
// Dialogs & Input Helpers
// ----------------------------------------------------

@Composable
fun KhataInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 13.sp) },
        textStyle = androidx.compose.ui.text.TextStyle(
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = EmeraldGreen,
            focusedBorderColor = EmeraldGreen,
            unfocusedBorderColor = BorderSlate,
            focusedContainerColor = SoftWhite,
            unfocusedContainerColor = OffWhite,
            focusedLabelColor = EmeraldGreen,
            unfocusedLabelColor = TextSecondary
        ),
        modifier = modifier
    )
}

@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onSave: (crop: String, qty: Double, unit: String, price: Double, buyer: String, field: String, desc: String) -> Unit
) {
    val langState = LocalAppLanguage.current
    var cropName by remember { mutableStateOf("Wheat") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("Mann") }
    var pricePerUnit by remember { mutableStateOf("") }
    var buyerOrMandi by remember { mutableStateOf("") }
    var fieldName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SoftWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = str("record_income"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Crop Name selection
                KhataInputField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = str("crop_name"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quantity and Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = str("quantity"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    KhataInputField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = str("unit"),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Price per Unit
                KhataInputField(
                    value = pricePerUnit,
                    onValueChange = { pricePerUnit = it },
                    label = str("selling_price_per_unit"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Total Preview
                val qtyVal = quantity.toDoubleOrNull() ?: 0.0
                val priceVal = pricePerUnit.toDoubleOrNull() ?: 0.0
                val totalCalc = qtyVal * priceVal
                if (totalCalc > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Income:", fontWeight = FontWeight.Bold, color = SuccessGreen)
                            CurrencyText(amount = totalCalc, color = SuccessGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Buyer or Mandi
                KhataInputField(
                    value = buyerOrMandi,
                    onValueChange = { buyerOrMandi = it },
                    label = str("buyer_or_mandi"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Field Name
                KhataInputField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = str("field_name"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Description
                KhataInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = str("description"),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(str("cancel"), color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(cropName, qtyVal, unit, priceVal, buyerOrMandi, fieldName, description)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text(str("save_record"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (crop: String, field: String, category: String, amount: Double, desc: String) -> Unit
) {
    var cropName by remember { mutableStateOf("Wheat") }
    var fieldName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Fertilizer") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SoftWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = str("add_expense"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                KhataInputField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = str("crop_name"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                KhataInputField(
                    value = category,
                    onValueChange = { category = it },
                    label = str("activity_type"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                KhataInputField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount (PKR)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                KhataInputField(
                    value = fieldName,
                    onValueChange = { fieldName = it },
                    label = str("field_name"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                KhataInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = str("description"),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(str("cancel"), color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            onSave(cropName, fieldName, category, amt, description)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                    ) {
                        Text(str("save_record"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddFieldWorkDialog(
    onDismiss: () -> Unit,
    onSave: (
        crop: String, field: String, acres: Double, act: String, desc: String,
        labor: Double, seed: Double, fert: Double, pest: Double,
        irrig: Double, mach: Double, trans: Double, other: Double
    ) -> Unit
) {
    var cropName by remember { mutableStateOf("Wheat") }
    var fieldName by remember { mutableStateOf("North Plot") }
    var fieldSize by remember { mutableStateOf("5.0") }
    var activityType by remember { mutableStateOf("Fertilization & Irrigation") }
    var description by remember { mutableStateOf("") }

    var laborCost by remember { mutableStateOf("") }
    var seedCost by remember { mutableStateOf("") }
    var fertCost by remember { mutableStateOf("") }
    var pestCost by remember { mutableStateOf("") }
    var irrigCost by remember { mutableStateOf("") }
    var machCost by remember { mutableStateOf("") }
    var transCost by remember { mutableStateOf("") }
    var otherCost by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SoftWhite,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .imePadding()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = str("record_field_work"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                KhataInputField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = str("crop_name"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = fieldName,
                        onValueChange = { fieldName = it },
                        label = str("field_name"),
                        modifier = Modifier.weight(1.2f)
                    )
                    KhataInputField(
                        value = fieldSize,
                        onValueChange = { fieldSize = it },
                        label = str("field_size"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                KhataInputField(
                    value = activityType,
                    onValueChange = { activityType = it },
                    label = str("activity_type"),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Expense Breakdown / اخراجات کی تفصیل (PKR)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = fertCost,
                        onValueChange = { fertCost = it },
                        label = str("fertilizer_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    KhataInputField(
                        value = pestCost,
                        onValueChange = { pestCost = it },
                        label = str("pesticide_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = laborCost,
                        onValueChange = { laborCost = it },
                        label = str("labor_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    KhataInputField(
                        value = seedCost,
                        onValueChange = { seedCost = it },
                        label = str("seed_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = irrigCost,
                        onValueChange = { irrigCost = it },
                        label = str("irrigation_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    KhataInputField(
                        value = machCost,
                        onValueChange = { machCost = it },
                        label = str("machinery_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KhataInputField(
                        value = transCost,
                        onValueChange = { transCost = it },
                        label = str("transport_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    KhataInputField(
                        value = otherCost,
                        onValueChange = { otherCost = it },
                        label = str("other_cost"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                KhataInputField(
                    value = description,
                    onValueChange = { description = it },
                    label = str("description"),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(str("cancel"), color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val acresVal = fieldSize.toDoubleOrNull() ?: 0.0
                            val l = laborCost.toDoubleOrNull() ?: 0.0
                            val s = seedCost.toDoubleOrNull() ?: 0.0
                            val f = fertCost.toDoubleOrNull() ?: 0.0
                            val p = pestCost.toDoubleOrNull() ?: 0.0
                            val i = irrigCost.toDoubleOrNull() ?: 0.0
                            val m = machCost.toDoubleOrNull() ?: 0.0
                            val t = transCost.toDoubleOrNull() ?: 0.0
                            val o = otherCost.toDoubleOrNull() ?: 0.0
                            onSave(cropName, fieldName, acresVal, activityType, description, l, s, f, p, i, m, t, o)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text(str("save_record"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
