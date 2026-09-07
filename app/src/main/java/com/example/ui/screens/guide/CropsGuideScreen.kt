package com.example.ui.screens.guide

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppLanguage
import java.util.Locale

@Composable
fun CropsGuideScreen(
    viewModel: MainViewModel,
    onNavigateToScan: () -> Unit = {},
    onAskAiQuestion: (String) -> Unit = {}
) {
    val language by viewModel.currentLanguage.collectAsState()
    val isUrdu = language == AppLanguage.URDU

    val crops by viewModel.allCrops.collectAsState()
    val diseases by viewModel.allDiseaseGuides.collectAsState()
    val selectedTab by viewModel.guideSelectedTab.collectAsState()
    val searchQuery by viewModel.guideSearchQuery.collectAsState()
    val isSpeaking by viewModel.voiceHelper.isSpeaking.collectAsState()
    val speakingId by viewModel.voiceHelper.currentSpeakingId.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) "زرعی انسائیکلوپیڈیا" else "Agri Encyclopedia",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isUrdu) "فصلوں کی نگہداشت اور بیماریوں کی شناخت" else "Crop Management & Plant Disease Guide",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // AI Scanner Shortcut Button
                    FilledTonalButton(
                        onClick = onNavigateToScan,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DocumentScanner,
                                contentDescription = "Scan",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isUrdu) "پودا سکین" else "AI Scan",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Primary Tab Selector (Crops vs Diseases)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setGuideTab(0) },
                        text = {
                            Text(
                                text = if (isUrdu) "🌾 تمام فصلیں (${crops.size})" else "🌾 Crops Guide (${crops.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setGuideTab(1) },
                        text = {
                            Text(
                                text = if (isUrdu) "🔬 پودوں کی بیماریاں (${diseases.size})" else "🔬 Plant Diseases (${diseases.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setGuideSearchQuery(it) },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) {
                                if (isUrdu) "فصل کا نام تلاش کریں (گندم، کپاس، چاول)..." else "Search crop (Wheat, Cotton, Rice)..."
                            } else {
                                if (isUrdu) "بیماری یا سپرے تلاش کریں (زرد کنگی، سفید مکھی)..." else "Search disease or spray (Rust, Whitefly, Blight)..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setGuideSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("guide_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
            }
        }

        // Category Filter Chips for Crops
        if (selectedTab == 0) {
            val categories = if (isUrdu) {
                listOf("تمام" to "All", "اناج" to "Grain", "نقد آور" to "Cash Crop", "سبزیاں" to "Vegetable")
            } else {
                listOf("All" to "All", "Grain" to "Grain", "Cash Crop" to "Cash Crop", "Vegetables" to "Vegetable")
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { (label, value) ->
                    FilterChip(
                        selected = selectedCategoryFilter == value,
                        onClick = { selectedCategoryFilter = value },
                        label = { Text(text = label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // Content List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (selectedTab == 0) {
                // Filtered Crops
                val filteredCrops = crops.filter { crop ->
                    val matchesQuery = searchQuery.isBlank() ||
                            crop.nameEn.contains(searchQuery, ignoreCase = true) ||
                            crop.nameUr.contains(searchQuery) ||
                            crop.scientificName.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategoryFilter == "All" || crop.category.equals(selectedCategoryFilter, ignoreCase = true)
                    matchesQuery && matchesCategory
                }

                if (filteredCrops.isEmpty()) {
                    item {
                        EmptyGuideState(isUrdu = isUrdu, title = if (isUrdu) "کوئی فصل نہیں ملی" else "No matching crops found")
                    }
                } else {
                    items(filteredCrops, key = { it.id }) { crop ->
                        CropCardItem(
                            crop = crop,
                            isUrdu = isUrdu,
                            isSpeaking = isSpeaking && speakingId == crop.id,
                            onSpeakClick = {
                                val text = if (isUrdu) {
                                    "${crop.nameUr}۔ بجائی کا وقت: ${crop.sowingSeasonUr}۔ متوقع پیداوار: ${crop.expectedYieldPerAcre}۔ کھاد: ${crop.fertilizerSchedule.firstOrNull()?.recommendationUr ?: ""}"
                                } else {
                                    "${crop.nameEn}. Sowing season: ${crop.sowingSeasonEn}. Expected yield: ${crop.expectedYieldPerAcre}."
                                }
                                if (isSpeaking && speakingId == crop.id) {
                                    viewModel.stopAudio()
                                } else {
                                    viewModel.speakAudio(text, isUrdu = isUrdu, utteranceId = crop.id)
                                }
                            },
                            onAskAi = { question ->
                                onAskAiQuestion(question)
                            }
                        )
                    }
                }
            } else {
                // Filtered Plant Diseases
                val filteredDiseases = diseases.filter { disease ->
                    searchQuery.isBlank() ||
                            disease.diseaseNameEn.contains(searchQuery, ignoreCase = true) ||
                            disease.diseaseNameUr.contains(searchQuery) ||
                            disease.chemicalTreatments.any { it.tradeBrandPakistan.contains(searchQuery, ignoreCase = true) || it.chemicalName.contains(searchQuery, ignoreCase = true) } ||
                            disease.affectedCrops.any { it.contains(searchQuery, ignoreCase = true) }
                }

                if (filteredDiseases.isEmpty()) {
                    item {
                        EmptyGuideState(isUrdu = isUrdu, title = if (isUrdu) "کوئی بیماری نہیں ملی" else "No matching diseases found")
                    }
                } else {
                    items(filteredDiseases, key = { it.id }) { disease ->
                        PlantDiseaseCardItem(
                            disease = disease,
                            isUrdu = isUrdu,
                            isSpeaking = isSpeaking && speakingId == disease.id,
                            onSpeakClick = {
                                val text = if (isUrdu) disease.audioExplanationUr else disease.audioExplanationEn
                                if (isSpeaking && speakingId == disease.id) {
                                    viewModel.stopAudio()
                                } else {
                                    viewModel.speakAudio(text, isUrdu = isUrdu, utteranceId = disease.id)
                                }
                            },
                            onAskAi = { question ->
                                onAskAiQuestion(question)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CropCardItem(
    crop: CropGuide,
    isUrdu: Boolean,
    isSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onAskAi: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (crop.id) {
                                "wheat" -> "🌾"
                                "cotton" -> "🌱"
                                "rice" -> "🌾"
                                "sugarcane" -> "🎋"
                                "maize" -> "🌽"
                                "potato" -> "🥔"
                                else -> "🌿"
                            },
                            fontSize = 22.sp
                        )
                    }

                    Column {
                        Text(
                            text = if (isUrdu) crop.nameUr else crop.nameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = crop.scientificName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                // Audio Speaker & Expand Indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onSpeakClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                            contentDescription = "Speak",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Key Metrics Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricChip(
                    label = if (isUrdu) "بجائی" else "Sowing",
                    value = if (isUrdu) crop.sowingSeasonUr else crop.sowingSeasonEn,
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    label = if (isUrdu) "پیداوار" else "Yield",
                    value = crop.expectedYieldPerAcre,
                    modifier = Modifier.weight(1f)
                )
            }

            // High-yield recommended varieties
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isUrdu) "منظور شدہ بہترین اقسام:" else "Approved High-Yield Varieties:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = crop.recommendedVarieties.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            // Expanded Detailed Agronomy Section
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Fertilizer Schedule Section
                    Text(
                        text = if (isUrdu) "🧪 کھادوں کا مکمل شیڈول (فی ایکڑ)" else "🧪 Fertilizer Schedule (Per Acre)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    crop.fertilizerSchedule.forEach { stage ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isUrdu) stage.stageNameUr else stage.stageNameEn,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isUrdu) stage.timingUr else stage.timingEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isUrdu) stage.recommendationUr else stage.recommendationEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Growth Stages Timeline
                    Text(
                        text = if (isUrdu) "📈 نشوونما کے اہم مراحل" else "📈 Growth Stages & Timeline",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    crop.growthStages.forEach { stage ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stage.stageNumber.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isUrdu) stage.titleUr else stage.titleEn,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stage.daysAfterSowing,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (isUrdu) stage.descriptionUr else stage.descriptionEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Expert Tips
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "💡 ماہر زرعی مشورہ:" else "💡 Agronomist Expert Tips:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val tips = if (isUrdu) crop.expertTipsUr else crop.expertTipsEn
                            tips.forEach { tip ->
                                Text(
                                    text = "• $tip",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Ask Kisan AI Button
                    Button(
                        onClick = {
                            val question = if (isUrdu) {
                                "${crop.nameUr} کے لیے کھاد کا بہترین شیڈول اور بیماریوں کا علاج بتائیں"
                            } else {
                                "What is the best fertilizer and disease management plan for ${crop.nameEn}?"
                            }
                            onAskAi(question)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isUrdu) "کسان دوست AI سے مزید پوچھیں" else "Ask Kisan AI about this crop"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlantDiseaseCardItem(
    disease: PlantDiseaseGuide,
    isUrdu: Boolean,
    isSpeaking: Boolean,
    onSpeakClick: () -> Unit,
    onAskAi: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row with Disease Name & Severity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUrdu) disease.diseaseNameUr else disease.diseaseNameEn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${disease.scientificName} • ${disease.pathogenType}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Severity Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (disease.severityLevel == "Critical") BadgeRedBg else BadgeOrangeBg
                    ) {
                        Text(
                            text = disease.severityLevel,
                            color = if (disease.severityLevel == "Critical") ErrorRed else WarningAmber,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onSpeakClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Filled.VolumeUp else Icons.Outlined.VolumeUp,
                            contentDescription = "Speak",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Affected Crops Pill Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isUrdu) "متاثرہ فصلیں:" else "Affected Crops:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = disease.affectedCrops.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Summary of symptoms
            Text(
                text = if (isUrdu) disease.symptomsSummaryUr else disease.symptomsSummaryEn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            // Expanded Chemical Treatment & Organic Remedies
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    // Chemical Sprays in Pakistan
                    Text(
                        text = if (isUrdu) "💊 پاکستان میں دستیاب مصدقہ زرعی ادویات اور سپرے" else "💊 Recommended Chemical Sprays & Dosage in Pakistan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    disease.chemicalTreatments.forEach { chem ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = chem.tradeBrandPakistan,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = chem.manufacturer,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${if (isUrdu) "مقدار:" else "Dosage:"} ${chem.dosagePerAcreOr100L}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = if (isUrdu) chem.applicationMethodUr else chem.applicationMethodEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Organic / Cultural Prevention
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isUrdu) "🌿 قدرتی و گھریلو تدابیر:" else "🌿 Organic & Cultural Remedies:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val remedies = if (isUrdu) disease.organicRemediesUr else disease.organicRemediesEn
                            remedies.forEach { remedy ->
                                Text(
                                    text = "• $remedy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Ask Kisan AI
                    Button(
                        onClick = {
                            val q = if (isUrdu) {
                                "${disease.diseaseNameUr} کا فوری اور سستا سپرے فارمولا بتائیں"
                            } else {
                                "What is the emergency spray formula for ${disease.diseaseNameEn}?"
                            }
                            onAskAi(q)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isUrdu) "کسان دوست AI سے سپرے فارمولا پوچھیں" else "Ask Kisan AI for Spray Formula"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun EmptyGuideState(
    isUrdu: Boolean,
    title: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isUrdu) "براہ کرم کوئی اور لفظ تلاش کریں" else "Try searching with a different keyword",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
