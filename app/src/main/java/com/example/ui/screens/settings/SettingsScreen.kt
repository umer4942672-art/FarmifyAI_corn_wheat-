package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
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
import com.example.ui.components.FarmerUserVectorAvatar
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
                        FarmerUserVectorAvatar(
                            size = 52.dp,
                            showTickMark = true,
                            onClick = { showEditProfileDialog = true }
                        )

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (langState.isUrdu) "ایپ کی زبان تبدیل کریں" else "App Interface Language",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (langState.isUrdu) "انگریزی یا اردو میں مکمل ایپ استعمال کریں" else "Select English or Urdu language",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = !langState.isUrdu,
                            onClick = {
                                viewModel.setLanguage(AppLanguage.ENGLISH)
                                langState.setLanguage(AppLanguage.ENGLISH)
                            },
                            label = { Text("English") }
                        )
                        FilterChip(
                            selected = langState.isUrdu,
                            onClick = {
                                viewModel.setLanguage(AppLanguage.URDU)
                                langState.setLanguage(AppLanguage.URDU)
                            },
                            label = { Text("اردو") }
                        )
                    }
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
                    checked = profile.weatherNotifications,
                    onCheckedChange = { viewModel.toggleNotification("weather", it) }
                )
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 6.dp))
                NotificationToggleRow(
                    title = str("mandi_alerts"),
                    checked = profile.mandiNotifications,
                    onCheckedChange = { viewModel.toggleNotification("mandi", it) }
                )
                HorizontalDivider(color = BorderLight, modifier = Modifier.padding(vertical = 6.dp))
                NotificationToggleRow(
                    title = str("khata_reminder"),
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

@Composable
fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
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
