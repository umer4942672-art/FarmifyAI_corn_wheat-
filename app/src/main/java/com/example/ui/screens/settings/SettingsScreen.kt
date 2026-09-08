package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FarmerAvatar
import com.example.ui.components.GlassCard
import com.example.ui.components.OfflineStatusPill
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import com.example.util.str

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val langState = LocalAppLanguage.current
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    // Photo picker. The chosen image is copied into app storage by the repository;
    // the gallery Uri itself is not stored because that permission is revoked on
    // restart and the avatar would break the next day.
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) viewModel.updateProfilePhoto(uri)
    }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // 1. Header
        item {
            Text(
                text = str("settings_title"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. Farmer Profile Card
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            FarmerAvatar(
                                photoPath = profile.profilePhotoPath,
                                size = 56.dp,
                                showTickMark = false,
                                onClick = { photoPickerLauncher.launch("image/*") }
                            )
                            // Small camera badge so it is obvious the avatar is tappable.
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldGreen)
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .clickable { photoPickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = if (langState.isUrdu) "تصویر تبدیل کریں" else "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = profile.fullName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Text(
                                text = profile.phone,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${profile.farmName} • ${profile.totalAcres.toInt()} Acres",
                                fontSize = 11.sp,
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("btn_edit_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile",
                            tint = EmeraldGreen
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Language Switcher Card
        item {
            SectionHeader(title = str("language_setting"), icon = Icons.Outlined.Translate)

            GlassCard {
                // Stacked, not side by side. The old layout squeezed a two-line
                // description and both chips into one row, so on narrow phones the
                // text and the buttons ran into each other.
                Text(
                    text = str("language_title"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = str("language_sub"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LanguageChoiceButton(
                        label = "English",
                        selected = !langState.isUrdu,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.setLanguage(AppLanguage.ENGLISH)
                        langState.setLanguage(AppLanguage.ENGLISH)
                    }
                    LanguageChoiceButton(
                        label = "اردو",
                        selected = langState.isUrdu,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.setLanguage(AppLanguage.URDU)
                        langState.setLanguage(AppLanguage.URDU)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(
                title = if (langState.isUrdu) "ایپ کی شکل" else "Appearance",
                icon = Icons.Outlined.Palette
            )

            GlassCard {
                Text(
                    text = if (langState.isUrdu) "تھیم" else "Theme",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (langState.isUrdu) "دھوپ میں روشن، رات کو گہرا" else "Light for daylight, dark for night",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeChoiceButton(
                        label = if (langState.isUrdu) AppThemeMode.LIGHT.titleUr else AppThemeMode.LIGHT.titleEn,
                        icon = Icons.Outlined.LightMode,
                        selected = currentTheme == AppThemeMode.LIGHT,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setAppTheme(AppThemeMode.LIGHT) }

                    ThemeChoiceButton(
                        label = if (langState.isUrdu) AppThemeMode.DARK.titleUr else AppThemeMode.DARK.titleEn,
                        icon = Icons.Outlined.DarkMode,
                        selected = currentTheme == AppThemeMode.DARK,
                        modifier = Modifier.weight(1f)
                    ) { viewModel.setAppTheme(AppThemeMode.DARK) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Notification Preferences
        item {
            SectionHeader(title = str("notifications"), icon = Icons.Outlined.Notifications)

            GlassCard {
                NotificationToggleRow(
                    title = str("weather_alerts"),
                    subtitle = str("weather_alerts_sub"),
                    icon = Icons.Outlined.WbSunny,
                    checked = profile.weatherNotifications,
                    onCheckedChange = { viewModel.toggleNotification("weather", it) }
                )
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleRow(
                    title = str("mandi_alerts"),
                    subtitle = str("mandi_alerts_sub"),
                    icon = Icons.Outlined.Storefront,
                    checked = profile.mandiNotifications,
                    onCheckedChange = { viewModel.toggleNotification("mandi", it) }
                )
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                // This toggle existed on the profile model but had no row in the UI.
                NotificationToggleRow(
                    title = str("disease_alerts"),
                    subtitle = str("disease_alerts_sub"),
                    icon = Icons.Outlined.LocalFlorist,
                    checked = profile.diseaseAlerts,
                    onCheckedChange = { viewModel.toggleNotification("disease", it) }
                )
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 8.dp))
                NotificationToggleRow(
                    title = str("khata_reminder"),
                    subtitle = str("khata_reminder_sub"),
                    icon = Icons.Outlined.MenuBook,
                    checked = profile.khataReminders,
                    onCheckedChange = { viewModel.toggleNotification("khata", it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Official Pakistan Agri Helplines
        item {
            SectionHeader(title = str("agri_helplines"), icon = Icons.Outlined.PhoneInTalk)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HelplineCard(
                    title = "Punjab Agriculture Helpline",
                    number = "0800-15000",
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:080015000"))
                        context.startActivity(intent)
                    }
                )
                HelplineCard(
                    title = "Sindh Agriculture Helpline",
                    number = "0800-29000",
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:080029000"))
                        context.startActivity(intent)
                    }
                )
                HelplineCard(
                    title = "Emergency Rescue Services",
                    number = "1122",
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:1122"))
                        context.startActivity(intent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. About App Card
        item {
            SectionHeader(title = str("about_app"), icon = Icons.Outlined.Info)

            GlassCard {
                // Backend status, visible in the app. Diagnosing "invalid credentials"
                // previously meant reading Gradle logs to find out whether the APK had
                // a backend URL compiled in at all.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (langState.isUrdu) "سرور" else "Server",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Text(
                            text = com.example.data.remote.ApiConfig.BASE_URL.ifBlank {
                                if (langState.isUrdu) "سیٹ نہیں ہے" else "Not configured"
                            },
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (com.example.data.remote.ApiConfig.isConfigured)
                            SuccessGreen.copy(alpha = 0.15f) else ErrorRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (com.example.data.remote.ApiConfig.isConfigured) "OK" else "MISSING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (com.example.data.remote.ApiConfig.isConfigured) SuccessGreen else ErrorRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 10.dp))

                Text(
                    text = "FarmifyAI v1.0.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Empowering Pakistani farmers with AI disease detection, digital field ledgers, real-time mandi prices, and agro-weather advisories in Urdu & English.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 8. Account & Logout Card
        item {
            SectionHeader(
                title = if (langState.isUrdu) "اکاؤنٹ اور سیشن" else "Account & Session",
                icon = Icons.Outlined.AccountCircle
            )

            GlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (langState.isUrdu) "آپ لاگ ان ہیں بطور: ${profile.fullName}" else "Logged in as: ${profile.fullName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("settings_logout_btn")
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (langState.isUrdu) "لاگ آؤٹ" else "Log Out",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("settings_switch_account_btn")
                        ) {
                            Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (langState.isUrdu) "اکاؤنٹ بدلیں" else "Switch Account",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, farm, location, acres ->
                viewModel.updateProfile(name, phone, farm, location, acres)
                showEditProfileDialog = false
            }
        )
    }
}

/** Theme option: filled when active, outlined otherwise. */
@Composable
fun ThemeChoiceButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EmeraldGreen else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) EmeraldGreen else BorderLight
        ),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}

/** Full-width language option. Both halves are equal, so English and Urdu never
 *  squeeze each other regardless of label width. */
@Composable
fun LanguageChoiceButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) EmeraldGreen else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selected) EmeraldGreen else BorderLight
        ),
        modifier = modifier.height(46.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = EmeraldGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = EmeraldGreen
            )
        )
    }
}

@Composable
fun HelplineCard(
    title: String,
    number: String,
    onCall: () -> Unit
) {
    Surface(
        onClick = onCall,
        shape = RoundedCornerShape(12.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Text(text = number, fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VeryLightGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    profile: com.example.data.repository.FarmerProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, farm: String, location: String, acres: Double) -> Unit
) {
    var name by remember { mutableStateOf(profile.fullName) }
    var phone by remember { mutableStateOf(profile.phone) }
    var farmName by remember { mutableStateOf(profile.farmName) }
    var farmLocation by remember { mutableStateOf(profile.farmLocation) }
    var acres by remember { mutableStateOf(profile.totalAcres.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SoftWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Farmer Profile",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldGreen,
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = BorderSlate,
                    focusedContainerColor = SoftWhite,
                    unfocusedContainerColor = OffWhite,
                    focusedLabelColor = EmeraldGreen,
                    unfocusedLabelColor = TextSecondary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Farmer Name") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Farm / Estate Name") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = farmLocation,
                    onValueChange = { farmLocation = it },
                    label = { Text("Location / District") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = acres,
                    onValueChange = { acres = it },
                    label = { Text("Total Land Area (Acres)") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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
                            val a = acres.toDoubleOrNull() ?: 25.0
                            onSave(name, phone, farmName, farmLocation, a)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
