package com.example.ui.screens.disease

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DiseaseCrop
import com.example.data.model.PlantDiseaseResult
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocalAppLanguage
import com.example.util.str

@Composable
fun AIDiseaseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAdvisory: () -> Unit = {}
) {
    val langState = LocalAppLanguage.current
    val currentResult by viewModel.currentScanResult.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingPlant.collectAsStateWithLifecycle()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.voiceHelper.isSpeaking.collectAsStateWithLifecycle()
    val selectedDiseaseCrop by viewModel.selectedDiseaseCrop.collectAsStateWithLifecycle()
    var showPathologistSheet by remember { mutableStateOf(false) }

    if (showPathologistSheet) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showPathologistSheet = false },
            icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = EmeraldGreen) },
            title = {
                Text(
                    text = if (langState.isUrdu) "ماہرِ نباتات سے رابطہ" else "Contact a plant pathologist",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (langState.isUrdu)
                            "ایپ کی تشخیص صرف رہنمائی کے لیے ہے۔ نقصان زیادہ ہو یا بیماری پھیل رہی ہو تو کھیت کا معائنہ ضروری ہے۔"
                        else
                            "The app's diagnosis is guidance only. If the damage is heavy or spreading, the field needs to be inspected in person.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    PathologistContactRow(
                        label = if (langState.isUrdu) "محکمہ زراعت پنجاب ہیلپ لائن" else "Punjab Agriculture Helpline",
                        value = "0800-15000"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PathologistContactRow(
                        label = if (langState.isUrdu) "کسان دوست ہیلپ لائن" else "Kisan Dost Helpline",
                        value = "0800-17000"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (langState.isUrdu)
                            "اپنے قریبی زرعی توسیعی افسر سے بھی رابطہ کیا جا سکتا ہے۔"
                        else
                            "Your local agriculture extension officer can also be reached at the tehsil office.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPathologistSheet = false
                        try {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:080015000")
                                )
                            )
                        } catch (_: Exception) {
                            // No dialer on this device; the numbers stay visible above.
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (langState.isUrdu) "کال کریں" else "Call now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPathologistSheet = false }) {
                    Text(if (langState.isUrdu) "بند کریں" else "Close", color = TextSecondary)
                }
            },
            containerColor = SoftWhite,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
            iconContentColor = EmeraldGreen,
            shape = RoundedCornerShape(20.dp)
        )
    }

    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("SCAN") } // "SCAN", "ENCYCLOPEDIA", "HISTORY"
    var selectedCropFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.analyzePlantBitmap(bitmap)
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (langState.isUrdu) "کیمرہ شروع کرنے میں خرابی ہوئی" else "Could not open camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                if (langState.isUrdu) "پودوں کی بیماری کی تشخیص کے لیے کیمرے کی اجازت درکار ہے" else "Camera permission is required to photograph plant leaves",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun launchCameraSafely() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    if (langState.isUrdu) "کیمرہ شروع کرنے میں خرابی ہوئی" else "Could not open camera: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.analyzePlantUri(uri)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("ai_disease_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // 1. Header & Tab Row
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = str("ai_scan_title"),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = if (langState.isUrdu)
                                "مصنوعی ذہانت سے لیس جدید پودوں کا ڈاکٹر اور سپرے گائیڈ"
                            else
                                "On-Device Plant Vision Diagnostic Model & Cure Advisory",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ForestGreen.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Trained AI Model",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs with generous touch targets & equal weights
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { activeTab = "SCAN" }
                            .testTag("ai_disease_tab_scanner"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeTab == "SCAN") ForestGreen else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (activeTab == "SCAN") ForestGreen else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = if (activeTab == "SCAN") Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (langState.isUrdu) {
                                    if (selectedDiseaseCrop == DiseaseCrop.WHEAT) "گندم سکینر" else "مکئی سکینر"
                                } else {
                                    if (selectedDiseaseCrop == DiseaseCrop.WHEAT) "Wheat Scanner" else "Corn Scanner"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "SCAN") Color.White else TextPrimary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable { activeTab = "HISTORY" }
                            .testTag("ai_disease_tab_history"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (activeTab == "HISTORY") ForestGreen else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (activeTab == "HISTORY") ForestGreen else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = if (activeTab == "HISTORY") Color.White else TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${if (langState.isUrdu) "ہسٹری" else "History"} (${scanHistory.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == "HISTORY") Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (activeTab == "SCAN") {
            item {
                CropSelectionCard(
                    selectedCrop = selectedDiseaseCrop,
                    isUrdu = langState.isUrdu,
                    onCropSelected = { viewModel.selectDiseaseCrop(it) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // If analyzing, show pulse state
            if (isAnalyzing) {
                item {
                    AnalyzingIndicator(selectedBitmap)
                }
            } else if (currentResult != null) {
                // Show Diagnosis Result Card
                item {
                    DiagnosisResultView(
                        result = currentResult!!,
                        capturedBitmap = selectedBitmap,
                        isSpeaking = isSpeaking,
                        onSpeak = { text ->
                            if (isSpeaking) {
                                viewModel.voiceHelper.stop()
                            } else {
                                viewModel.voiceHelper.speak(text, langState.isUrdu, "disease_report")
                            }
                        },
                        onSave = { viewModel.saveCurrentScan() },
                        onReset = {
                            viewModel.voiceHelper.stop()
                            viewModel.resetScan()
                        },
                        onAskAdvisory = { diagnosis ->
                            viewModel.voiceHelper.stop()
                            // Seed the chat with this diagnosis so the farmer does not
                            // have to retype the disease name.
                            viewModel.askAdvisoryAboutDiagnosis(diagnosis, langState.isUrdu)
                            onNavigateToAdvisory()
                        },
                        onContactPathologist = { showPathologistSheet = true }
                    )
                }
            } else {
                // Camera & Gallery Upload Card
                item {
                    UploadAndCaptureCard(
                        onLaunchCamera = { launchCameraSafely() },
                        onLaunchGallery = {
                            try {
                                galleryLauncher.launch("image/*")
                            } catch (e: Exception) {
                                Toast.makeText(
                                    context,
                                    if (langState.isUrdu) "گیلری کھولنے میں خرابی ہوئی" else "Could not open gallery: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

            }
        } else {
            // HISTORY TAB
            if (scanHistory.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.HistoryEdu,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (langState.isUrdu) "کوئی پچھلا سکین ریکارڈ موجود نہیں۔" else "No scan history recorded yet.",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                items(scanHistory, key = { it.id }) { scan ->
                    HistoryScanCard(
                        scan = scan,
                        onDelete = { viewModel.deleteScanHistory(scan.id) },
                        onSelect = {
                            viewModel.selectHistoryScan(scan)
                            activeTab = "SCAN"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun CropSelectionCard(
    selectedCrop: DiseaseCrop,
    isUrdu: Boolean,
    onCropSelected: (DiseaseCrop) -> Unit
) {
    Column {
        Text(
            text = if (isUrdu) "فصل منتخب کریں" else "Select Crop",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CropChoiceButton(
                modifier = Modifier.weight(1f), selected = selectedCrop == DiseaseCrop.WHEAT,
                emoji = "🌾", title = if (isUrdu) "گندم" else "Wheat",
                onClick = { onCropSelected(DiseaseCrop.WHEAT) }
            )
            CropChoiceButton(
                modifier = Modifier.weight(1f), selected = selectedCrop == DiseaseCrop.CORN,
                emoji = "🌽", title = if (isUrdu) "مکئی" else "Corn",
                onClick = { onCropSelected(DiseaseCrop.CORN) }
            )
        }
    }
}

@Composable
private fun CropChoiceButton(
    modifier: Modifier,
    selected: Boolean,
    emoji: String,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(52.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ForestGreen else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) ForestGreen else BorderLight)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 19.sp)
            Spacer(modifier = Modifier.width(7.dp))
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = if (selected) Color.White else TextPrimary)
        }
    }
}

@Composable
fun UploadAndCaptureCard(
    onLaunchCamera: () -> Unit,
    onLaunchGallery: () -> Unit
) {
    val langState = LocalAppLanguage.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                )
            )
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (langState.isUrdu) "پودے یا پتے کی تصویر لیں" else "Capture or Upload Plant Leaf",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (langState.isUrdu)
                    "فصل کے پتوں کے دھبے، پیلا پن یا کیڑوں کی تصویر واضح روشنی میں کھینچیں، ماڈل فوری تشخیص کرے گا۔"
                else
                    "Take a close-up photo of infected leaves or stems for on-device AI pathology diagnosis.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Both buttons get a fixed height and single-line text so they stay
                // the same size regardless of label length in English or Urdu.
                Button(
                    onClick = onLaunchCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ai_take_photo_btn")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = str("take_photo"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Gallery is the secondary action, so it is outlined rather than a
                // second filled block competing with the camera button.
                OutlinedButton(
                    onClick = onLaunchGallery,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("ai_gallery_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = str("choose_gallery"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun AnalyzingIndicator(selectedBitmap: Bitmap?) {
    val langState = LocalAppLanguage.current
    val infiniteTransition = rememberInfiniteTransition(label = "inspection_scan")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inspection_line"
    )

    GlassCard(
        backgroundColor = Color(0xFFF1F8F4),
        borderColor = EmeraldGreen
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedBitmap != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    Image(
                        bitmap = selectedBitmap.asImageBitmap(),
                        contentDescription = if (langState.isUrdu) "معائنہ کے لیے منتخب تصویر" else "Image being inspected",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .offset(y = (maxHeight - 5.dp) * scanProgress)
                            .background(Color(0xFF00E676))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.58f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (langState.isUrdu) "AI معائنہ جاری ہے" else "AI INSPECTION",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                CircularProgressIndicator(
                    color = EmeraldGreen,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = if (langState.isUrdu) "AI ماڈل پودے کا معائنہ کر رہا ہے..." else "AI is inspecting the plant...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (langState.isUrdu)
                    "تصویر کے رنگ، دھبوں اور بیماری کی علامات کا تجزیہ کیا جا رہا ہے۔"
                else
                    "Analyzing leaf patterns, colors, and visible disease symptoms.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun DiagnosisResultView(
    result: PlantDiseaseResult,
    capturedBitmap: Bitmap?,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onAskAdvisory: (PlantDiseaseResult) -> Unit,
    onContactPathologist: () -> Unit
) {
    val langState = LocalAppLanguage.current

    var acresInput by remember { mutableStateOf("1") }
    var showDosageCalculator by remember { mutableStateOf(false) }

    if (!result.isPlantImage) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = if (langState.isUrdu) "⚠️ پودے کی تصویر نہیں ملی" else "⚠️ This is not a plant image",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WarningAmber
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (langState.isUrdu) result.advisoryNoteUr else result.advisoryNoteEn,
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text(if (langState.isUrdu) "دوبارہ تصویر لیں" else "Capture / Select Another Photo")
                }
            }
        }
        return
    }

    val severityColor = when (result.severityLevel) {
        "Critical" -> ErrorRed
        "High" -> Color(0xFFE65100)
        "Moderate" -> WarningAmber
        else -> SuccessGreen
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Result Header Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (result.isHealthy) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (result.isHealthy) SuccessGreen else severityColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (result.isHealthy) SuccessGreen else severityColor
                        ) {
                            Text(
                                text = if (result.isHealthy) str("status_healthy") else "${result.severityLevel} Severity",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SoftWhite
                        ) {
                            Text(
                                text = "${result.confidencePercent}% Match",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Audio Readout Button
                    IconButton(
                        onClick = {
                            val speechText = if (langState.isUrdu) {
                                "${result.cropName} میں ${result.diseaseNameUr} کی تشخیص ہوئی ہے۔ شدت ${result.severityLevel} ہے۔ تجویز کردہ سپرے: ${result.chemicalTreatmentUr}۔ احتیاطی تدابیر: ${result.organicPreventionUr}"
                            } else {
                                "Diagnosis for ${result.cropName}: ${result.diseaseNameEn}. Severity level is ${result.severityLevel}. Recommended treatment: ${result.chemicalTreatmentEn}"
                            }
                            onSpeak(speechText)
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) ErrorRed else ForestGreen)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Read out",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${result.cropName}: ${if (langState.isUrdu) result.diseaseNameUr else result.diseaseNameEn}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )

                if (langState.isUrdu) {
                    Text(
                        text = result.diseaseNameEn,
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                // Confidence Bar
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { result.confidencePercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (result.isHealthy) SuccessGreen else severityColor,
                    trackColor = Color.Black.copy(alpha = 0.08f)
                )
            }
        }

        // Captured Preview Image if any
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured leaf",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // 1. Symptoms Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Visibility, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = str("symptoms"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (langState.isUrdu) result.symptomsUr else result.symptomsEn,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }

        // 2. Recommended Chemical Treatment Card
        GlassCard(
            backgroundColor = Color(0xFFF2F8F4),
            borderColor = EmeraldGreen
        ) {
            // Title and the calculator toggle used to share one SpaceBetween row.
            // The Urdu calculator label is long, so it squeezed the heading and the
            // whole card ended up looking cramped. The toggle now sits on its own
            // line under the treatment text, at full width.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Medication, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = str("chemical_cure"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ForestGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (langState.isUrdu) result.chemicalTreatmentUr else result.chemicalTreatmentEn,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = { showDosageCalculator = !showDosageCalculator },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showDosageCalculator) (if (langState.isUrdu) "کیلکولیٹر چھپائیں" else "Hide dosage calculator")
                           else (if (langState.isUrdu) "سپرے کیلکولیٹر کھولیں" else "Open dosage calculator"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            // Spray Dosage Calculator Section
            AnimatedVisibility(visible = showDosageCalculator) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (langState.isUrdu) "اپنے رقبے کے مطابق سپرے کا حساب:" else "Calculate Spray Requirements for Field:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ForestGreen
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = acresInput,
                            onValueChange = { acresInput = it },
                            label = {
                                Text(
                                    text = if (langState.isUrdu) "رقبہ (ایکڑ)" else "Field acres",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            // This field sits on a hardcoded white box. Without explicit
                            // colours it inherits the theme's onSurface, which is white in
                            // dark mode, so the typed number was invisible.
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                disabledTextColor = TextSecondary,
                                cursorColor = ForestGreen,
                                focusedBorderColor = ForestGreen,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = ForestGreen,
                                unfocusedLabelColor = TextSecondary,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        val acres = acresInput.toDoubleOrNull() ?: 1.0
                        val waterLiters = (acres * 100).toInt()
                        val backpackTanks = (waterLiters / 20).coerceAtLeast(1)

                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "💧 ${if (langState.isUrdu) "پانی:" else "Water:"} $waterLiters Liters",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "🎒 ${if (langState.isUrdu) "ٹینکیوں کی تعداد:" else "20L Tanks:"} $backpackTanks tanks",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 3. Organic & Cultural Prevention Card
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Eco, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = str("organic_prevention"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (langState.isUrdu) result.organicPreventionUr else result.organicPreventionEn,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }

        // 4. Agri Advisory & Weather Safety Note
        GlassCard(
            backgroundColor = Color(0xFFFFF9E6),
            borderColor = Color(0xFFFFD54F)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (langState.isUrdu) "زرعی ماہر کا مشورہ" else "Agronomist Advisory Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFF57F17)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (langState.isUrdu) result.advisoryNoteUr else result.advisoryNoteEn,
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp
            )
        }

        // Ask the advisory chatbot about this exact diagnosis.
        Button(
            onClick = { onAskAdvisory(result) },
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ai_ask_advisory_btn")
        ) {
            Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = if (langState.isUrdu) "AI مشورہ لیں" else "Ask AI advisory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = if (langState.isUrdu) "اسی بیماری پر مزید معلومات" else "More about this disease",
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // A model prediction is not a diagnosis. Anything severe should reach a
        // real plant pathologist, so the route to one is on the result screen.
        OutlinedButton(
            onClick = onContactPathologist,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, EmeraldGreen),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ai_contact_pathologist_btn")
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = if (langState.isUrdu) "ماہرِ نباتات سے رابطہ" else "Contact a pathologist",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Text(
                    text = if (langState.isUrdu) "محکمہ زراعت کی ہیلپ لائن" else "Agriculture department helpline",
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Both actions get the same fixed height so neither looks like the odd one out.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ai_save_result_btn")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = str("save_to_history"), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SuccessGreen),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("ai_scan_another_btn")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = str("scan_another"), fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}




@Composable
fun HistoryScanCard(
    scan: PlantDiseaseResult,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val langState = LocalAppLanguage.current

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (scan.imagePathOrUri.isNotBlank()) {
                coil.compose.AsyncImage(
                    model = scan.imagePathOrUri,
                    contentDescription = if (langState.isUrdu) "اسکین تصویر" else "Scan image",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${scan.cropName}: ${if (langState.isUrdu) scan.diseaseNameUr else scan.diseaseNameEn}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = VeryLightGreen
                    ) {
                        Text(
                            text = "${scan.confidencePercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreen,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (langState.isUrdu) scan.chemicalTreatmentUr else scan.chemicalTreatmentEn,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
            }
        }
    }
}


@Composable
private fun PathologistContactRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F8F4))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 2)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ForestGreen)
    }
}
