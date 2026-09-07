package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocalAppLanguage
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    SIGNUP,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: MainViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }
    val focusManager = LocalFocusManager.current
    var isSubmitting by remember { mutableStateOf(false) }

    // Login Fields
    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    // Forgot Password Fields
    var forgotIdentifier by remember { mutableStateOf("") }
    var forgotNewPassword by remember { mutableStateOf("") }
    var forgotError by remember { mutableStateOf<String?>(null) }
    var forgotSuccess by remember { mutableStateOf<String?>(null) }

    // Signup Fields
    var signupFullName by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupFarmName by remember { mutableStateOf("") }
    var signupDistrict by remember { mutableStateOf("Faisalabad") }
    var signupProvince by remember { mutableStateOf("Punjab") }
    var signupAcres by remember { mutableStateOf("20") }
    val selectedCrops = remember { mutableStateListOf("Wheat", "Cotton") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupError by remember { mutableStateOf<String?>(null) }
    var districtDropdownExpanded by remember { mutableStateOf(false) }

    val pakistaniDistricts = listOf(
        "Faisalabad" to "فیصل آباد (پنجاب)",
        "Multan" to "ملتان (پنجاب)",
        "Sahiwal" to "ساہیوال (پنجاب)",
        "Lahore" to "لاہور (پنجاب)",
        "Rahim Yar Khan" to "رحیم یار خان (پنجاب)",
        "Sargodha" to "سرگودھا (پنجاب)",
        "Bahawalpur" to "بہاولپور (پنجاب)",
        "Gujranwala" to "گوجرانوالہ (پنجاب)",
        "Sheikhupura" to "شیخوپورہ (پنجاب)",
        "Jhang" to "جھنگ (پنجاب)",
        "Sukkur" to "سکھر (سندھ)",
        "Hyderabad" to "حیدرآباد (سندھ)",
        "Larkana" to "لاڑکانہ (سندھ)",
        "Peshawar" to "پشاور (خیبر پختونخوا)",
        "Mardan" to "مردان (خیبر پختونخوا)",
        "Swat" to "سوات (خیبر پختونخوا)",
        "Quetta" to "کوئٹہ (بلوچستان)"
    )

    val cropOptions = listOf(
        "Wheat" to "🌾 گندم",
        "Cotton" to "🌱 کپاس",
        "Rice" to "🌾 دھان/چاول",
        "Sugarcane" to "🎋 کماد",
        "Maize" to "🌽 مکئی",
        "Potato" to "🥔 آلو",
        "Vegetables" to "🥦 سبزیاں",
        "Mango" to "🥭 آم",
        "Citrus" to "🍊 کینو/سٹرس"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .navigationBarsPadding()
            .imePadding()
            .testTag("auth_screen"),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // 1. Top Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF134E1B),
                                Color(0xFF1B5E20),
                                Color(0xFF2E7D32)
                            )
                        )
                    )
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Bar: Lang Switcher & Guest Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon & Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Spa,
                                    contentDescription = null,
                                    tint = GoldenYellow,
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "FarmifyAI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Language Toggle
                        Surface(
                            onClick = { langState.toggleLanguage() },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                            modifier = Modifier.testTag("auth_lang_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Lang",
                                    tint = GoldenYellow,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (langState.isUrdu) "English" else "اردو",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title & Subtitle
                    Text(
                        text = if (authMode == AuthMode.LOGIN) {
                            if (langState.isUrdu) "اپنے فارم اکاؤنٹ میں لاگ ان کریں" else "Welcome Back to FarmifyAI"
                        } else {
                            if (langState.isUrdu) "نیا کسان اکاؤنٹ بنائیں" else "Create Farmer Account"
                        },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 30.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (langState.isUrdu) "سمارٹ ڈیجیٹل زرعی معاون اور فارم مینجمنٹ" else "Smart Precision Agriculture & Farm Management",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 2. Mode Selector (Login vs SignUp vs Forgot Password)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp)
                    .shadow(2.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = SoftWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabButton(
                        title = if (langState.isUrdu) "لاگ ان" else "Log In",
                        isSelected = authMode == AuthMode.LOGIN,
                        onClick = {
                            authMode = AuthMode.LOGIN
                            loginError = null
                            forgotError = null
                            forgotSuccess = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = if (langState.isUrdu) "نیا اکاؤنٹ" else "Sign Up",
                        isSelected = authMode == AuthMode.SIGNUP,
                        onClick = {
                            authMode = AuthMode.SIGNUP
                            signupError = null
                            forgotError = null
                            forgotSuccess = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = if (langState.isUrdu) "پاس ورڈ بحالی" else "Forgot?",
                        isSelected = authMode == AuthMode.FORGOT_PASSWORD,
                        onClick = {
                            authMode = AuthMode.FORGOT_PASSWORD
                            forgotIdentifier = loginIdentifier
                            forgotError = null
                            forgotSuccess = null
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Form Body
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = SoftWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Crossfade(targetState = authMode, label = "AuthFormTransition") { mode ->
                        when (mode) {
                            AuthMode.LOGIN -> {
                                // LOGIN FORM
                                Column(modifier = Modifier.fillMaxWidth()) {
                                // Error Banner
                                AnimatedVisibility(visible = loginError != null) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = loginError ?: "",
                                                color = ErrorRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Identifier (Phone / Email)
                                Text(
                                    text = if (langState.isUrdu) "موبائل نمبر یا ای میل" else "Mobile Number or Email",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = loginIdentifier,
                                    onValueChange = { loginIdentifier = it },
                                    placeholder = { Text("0300 7654321 / farmer@farmify.pk", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen)
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_login_identifier")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Password
                                Text(
                                    text = if (langState.isUrdu) "پاس ورڈ" else "Password",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = loginPassword,
                                    onValueChange = { loginPassword = it },
                                    placeholder = { Text("••••••••", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen)
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = TextSecondary
                                            )
                                        }
                                    },
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        if (loginIdentifier.isNotBlank() && loginPassword.isNotBlank()) {
                                            coroutineScope.launch {
                                                isSubmitting = true
                                                try {
                                                    val ok = viewModel.login(loginIdentifier, loginPassword)
                                                    if (ok) onAuthSuccess() else loginError = "Invalid credentials. Try Demo Login."
                                                } finally {
                                                    isSubmitting = false
                                                }
                                            }
                                        }
                                    }),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_login_password")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Forgot Password Link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            forgotIdentifier = loginIdentifier
                                            authMode = AuthMode.FORGOT_PASSWORD
                                            forgotError = null
                                            forgotSuccess = null
                                        }
                                    ) {
                                        Text(
                                            text = if (langState.isUrdu) "پاس ورڈ بھول گئے؟" else "Forgot Password?",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Main Login Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (loginIdentifier.isBlank() || loginPassword.isBlank()) {
                                            loginError = if (langState.isUrdu) "برائے مہربانی فون نمبر اور پاس ورڈ درج کریں" else "Please enter phone and password"
                                        } else {
                                            coroutineScope.launch {
                                                isSubmitting = true
                                                loginError = null
                                                try {
                                                    val ok = viewModel.login(loginIdentifier, loginPassword)
                                                    if (ok) {
                                                        onAuthSuccess()
                                                    } else {
                                                        loginError = if (langState.isUrdu) "لاگ ان میں مسئلہ پیش آیا۔ دوبارہ کوشش کریں۔" else "Login failed. Please verify credentials."
                                                    }
                                                } catch (e: Exception) {
                                                    loginError = e.localizedMessage ?: "Connection error"
                                                } finally {
                                                    isSubmitting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(2.dp, RoundedCornerShape(14.dp))
                                        .testTag("auth_submit_login")
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "تصدیق ہو رہی ہے..." else "Authenticating...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "لاگ ان کریں" else "Log In to Farmify",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        AuthMode.SIGNUP -> {
                            // SIGNUP FORM
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Error Banner
                                AnimatedVisibility(visible = signupError != null) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = signupError ?: "",
                                                color = ErrorRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Full Name
                                Text(
                                    text = if (langState.isUrdu) "کسان کا پورا نام *" else "Farmer Full Name *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupFullName,
                                    onValueChange = { signupFullName = it },
                                    placeholder = { Text("مثال: ملک طارق محمود / Tariq Mahmood", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldGreen) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_name")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Phone Number
                                Text(
                                    text = if (langState.isUrdu) "موبائل نمبر *" else "Mobile Number *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupPhone,
                                    onValueChange = { signupPhone = it },
                                    placeholder = { Text("0300 1234567", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = EmeraldGreen) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_phone")
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // District Dropdown Selector
                                Text(
                                    text = if (langState.isUrdu) "ضلع و شہر *" else "District / City *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                ExposedDropdownMenuBox(
                                    expanded = districtDropdownExpanded,
                                    onExpandedChange = { districtDropdownExpanded = !districtDropdownExpanded },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = signupDistrict,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = districtDropdownExpanded) },
                                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = EmeraldGreen) },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary,
                                            cursorColor = EmeraldGreen,
                                            focusedBorderColor = EmeraldGreen,
                                            unfocusedBorderColor = BorderSlate,
                                            focusedContainerColor = SoftWhite,
                                            unfocusedContainerColor = OffWhite
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )

                                    ExposedDropdownMenu(
                                        expanded = districtDropdownExpanded,
                                        onDismissRequest = { districtDropdownExpanded = false }
                                    ) {
                                        pakistaniDistricts.forEach { (distEn, distLabel) ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = if (langState.isUrdu) distLabel else "$distEn ($distLabel)",
                                                        fontWeight = if (signupDistrict == distEn) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                onClick = {
                                                    signupDistrict = distEn
                                                    signupProvince = if (distLabel.contains("سندھ")) "Sindh" else if (distLabel.contains("خیبر")) "KPK" else if (distLabel.contains("بلوچستان")) "Balochistan" else "Punjab"
                                                    districtDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Farm / Dera Name & Land Size Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Farm Name
                                    Column(modifier = Modifier.weight(1.2f)) {
                                        Text(
                                            text = if (langState.isUrdu) "فارم / ڈیرہ" else "Farm / Dera Name",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = signupFarmName,
                                            onValueChange = { signupFarmName = it },
                                            placeholder = { Text("ال رحمان فارم", fontSize = 12.sp, color = TextMuted) },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldGreen,
                                                focusedBorderColor = EmeraldGreen,
                                                unfocusedBorderColor = BorderSlate,
                                                focusedContainerColor = SoftWhite,
                                                unfocusedContainerColor = OffWhite
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("auth_signup_farm_name")
                                        )
                                    }

                                    // Acres
                                    Column(modifier = Modifier.weight(0.8f)) {
                                        Text(
                                            text = if (langState.isUrdu) "رقبہ (ایکڑ)" else "Acres",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = signupAcres,
                                            onValueChange = { signupAcres = it },
                                            placeholder = { Text("15", fontSize = 12.sp, color = TextMuted) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = TextPrimary,
                                                unfocusedTextColor = TextPrimary,
                                                cursorColor = EmeraldGreen,
                                                focusedBorderColor = EmeraldGreen,
                                                unfocusedBorderColor = BorderSlate,
                                                focusedContainerColor = SoftWhite,
                                                unfocusedContainerColor = OffWhite
                                            ),
                                            modifier = Modifier.fillMaxWidth().testTag("auth_signup_acres")
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Primary Crops Chips
                                Text(
                                    text = if (langState.isUrdu) "اہم فصلیں چنیں (Primary Crops):" else "Select Primary Crops:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    cropOptions.forEach { (cropKey, cropLabel) ->
                                        val isSelected = selectedCrops.contains(cropKey)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) {
                                                    if (selectedCrops.size > 1) selectedCrops.remove(cropKey)
                                                } else {
                                                    selectedCrops.add(cropKey)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    text = cropLabel,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = EmeraldGreen,
                                                selectedLabelColor = Color.White,
                                                containerColor = PaleGreenBg
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Password
                                Text(
                                    text = if (langState.isUrdu) "پاس ورڈ بنائیں *" else "Create Password *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = signupPassword,
                                    onValueChange = { signupPassword = it },
                                    placeholder = { Text("کم از کم 6 ہندسے / Min 6 chars", fontSize = 12.sp, color = TextMuted) },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldGreen) },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("auth_signup_password")
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Submit Signup Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (signupFullName.isBlank() || signupPhone.isBlank() || signupPassword.isBlank()) {
                                            signupError = if (langState.isUrdu) "برائے مہربانی تمام ضروری خانے (*) پر کریں" else "Please fill all required fields (*)"
                                        } else {
                                            coroutineScope.launch {
                                                isSubmitting = true
                                                signupError = null
                                                try {
                                                    val acresDouble = signupAcres.toDoubleOrNull() ?: 10.0
                                                    val ok = viewModel.signup(
                                                        fullName = signupFullName,
                                                        phone = signupPhone,
                                                        email = signupEmail,
                                                        pass = signupPassword,
                                                        farmName = signupFarmName,
                                                        district = signupDistrict,
                                                        province = signupProvince,
                                                        acres = acresDouble,
                                                        crops = selectedCrops.toList()
                                                    )
                                                    if (ok) {
                                                        onAuthSuccess()
                                                    } else {
                                                        signupError = "Registration error. Please check values."
                                                    }
                                                } catch (e: Exception) {
                                                    signupError = e.localizedMessage ?: "Registration error"
                                                } finally {
                                                    isSubmitting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(2.dp, RoundedCornerShape(14.dp))
                                        .testTag("auth_submit_signup")
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "اکاؤنٹ بنایا جا رہا ہے..." else "Creating account...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "رجسٹریشن مکمل کریں اور شروع کریں" else "Complete Registration & Start",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        AuthMode.FORGOT_PASSWORD -> {
                            // FORGOT PASSWORD FORM
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Error Banner
                                AnimatedVisibility(visible = forgotError != null) {
                                    Surface(
                                        color = Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ErrorOutline,
                                                contentDescription = null,
                                                tint = ErrorRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = forgotError ?: "",
                                                color = ErrorRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Success Banner
                                AnimatedVisibility(visible = forgotSuccess != null) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA5D6A7)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = EmeraldGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = forgotSuccess ?: "",
                                                color = Color(0xFF1B5E20),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Header info
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = EmeraldGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = if (langState.isUrdu)
                                                "اپنا رجسٹرڈ موبائل نمبر یا ای میل درج کریں تاکہ آپ اپنا پاس ورڈ بحال یا تبدیل کر سکیں"
                                            else
                                                "Enter your registered mobile number or email to recover or set a new password",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Registered Identifier
                                Text(
                                    text = if (langState.isUrdu) "رجسٹرڈ فون نمبر یا ای میل *" else "Registered Mobile or Email *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = forgotIdentifier,
                                    onValueChange = { forgotIdentifier = it },
                                    placeholder = { Text("0300 1234567 / farmer@farmify.pk", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContactPhone, contentDescription = null, tint = EmeraldGreen)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Optional New Password
                                Text(
                                    text = if (langState.isUrdu) "نیا پاس ورڈ (اختیاری)" else "New Password (Optional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = forgotNewPassword,
                                    onValueChange = { forgotNewPassword = it },
                                    placeholder = { Text(if (langState.isUrdu) "نیا پاس ورڈ درج کریں" else "Set new password (optional)", fontSize = 13.sp, color = TextMuted) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LockReset, contentDescription = null, tint = EmeraldGreen)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        cursorColor = EmeraldGreen,
                                        focusedBorderColor = EmeraldGreen,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedContainerColor = SoftWhite,
                                        unfocusedContainerColor = OffWhite
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Recover Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (forgotIdentifier.isBlank()) {
                                            forgotError = if (langState.isUrdu) "برائے مہربانی فون نمبر یا ای میل درج کریں" else "Please enter phone number or email"
                                        } else {
                                            coroutineScope.launch {
                                                isSubmitting = true
                                                forgotError = null
                                                forgotSuccess = null
                                                try {
                                                    val ok = viewModel.recoverPassword(forgotIdentifier, forgotNewPassword)
                                                    if (ok) {
                                                        forgotSuccess = if (langState.isUrdu)
                                                            "پاس ورڈ کی بحالی کی ہدایات / نیا پاس ورڈ کامیابی سے محفوظ ہو گیا ہے!"
                                                        else
                                                            "Password reset / recovery instructions sent successfully!"
                                                    } else {
                                                        forgotError = if (langState.isUrdu)
                                                            "اکاؤنٹ تلاش کرنے میں مسئلہ ہوا۔ براہ کرم چیک کریں۔"
                                                        else
                                                            "Could not find account. Please verify input."
                                                    }
                                                } catch (e: Exception) {
                                                    forgotError = e.localizedMessage ?: "Recovery error"
                                                } finally {
                                                    isSubmitting = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isSubmitting,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(2.dp, RoundedCornerShape(14.dp))
                                ) {
                                    if (isSubmitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "درخواست بھیجی جا رہی ہے..." else "Sending request...",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (langState.isUrdu) "پاس ورڈ بحالی کی درخواست بھیجیں" else "Submit Password Recovery",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = {
                                        authMode = AuthMode.LOGIN
                                        loginError = null
                                        forgotError = null
                                        forgotSuccess = null
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = if (langState.isUrdu) "← لاگ ان پر واپس جائیں" else "← Back to Login",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // 4. Guest / Instant Skip Button
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (viewModel.quickDemoLogin()) onAuthSuccess()
                        }
                    }
                ) {
                    Text(
                        text = if (langState.isUrdu) "مہمان کے طور پر دیکھیں (Skip as Guest) →" else "Continue as Guest / Skip →",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) EmeraldGreen else Color.Transparent,
        modifier = modifier.height(44.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else TextSecondary
            )
        }
    }
}
