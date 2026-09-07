package com.example.data.repository

import android.content.Context
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import com.example.data.local.PasswordHasher
import com.example.data.remote.SupabaseAuthService
import com.example.data.remote.SupabaseDataSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

data class FarmerProfile(
    val fullName: String = "Pakistani Kisan",
    val phone: String = "0300 1234567",
    val email: String = "farmer@farmify.pk",
    val farmName: String = "Green Agri Farm",
    val district: String = "Faisalabad",
    val province: String = "Punjab",
    val farmLocation: String = "Faisalabad, Punjab",
    val totalAcres: Double = 10.0,
    val primaryCrops: List<String> = listOf("Wheat", "Cotton", "Sugarcane", "Tomato"),
    val isAuthenticated: Boolean = false,
    val weatherNotifications: Boolean = true,
    val mandiNotifications: Boolean = true,
    val diseaseAlerts: Boolean = true,
    val khataReminders: Boolean = true
)

class UserRepository(
    private val userDao: UserDao,
    context: Context,
    private val supabaseAuthService: SupabaseAuthService = SupabaseAuthService(context),
    private val supabaseDataSync: SupabaseDataSyncService = SupabaseDataSyncService(context)
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _profile = MutableStateFlow(FarmerProfile(isAuthenticated = false))
    val profile: StateFlow<FarmerProfile> = _profile.asStateFlow()

    init {
        repositoryScope.launch {
            // Restore active authenticated user session from database if exists
            val activeUser = userDao.getActiveUserDirect()
            if (activeUser != null) {
                _profile.value = activeUser.toModel(isAuthenticated = true)
                // Sync profile state to Supabase
                supabaseDataSync.syncProfile(activeUser)
            }
        }
    }

    suspend fun login(identifier: String, password: String): Result<FarmerProfile> {
        val cleanKey = identifier.trim().replace(" ", "").replace("-", "").replace("+92", "0")
        val isEmail = identifier.contains("@")
        val emailToTry = if (isEmail) identifier.trim().lowercase() else null

        // 1. Try Supabase Cloud Auth API
        val cloudAuth = supabaseAuthService.signInWithPassword(if (isEmail) emailToTry!! else normalizePhone(cleanKey), password)
        if (cloudAuth.isSuccess) {
            val meta = cloudAuth.userMetadata
            val name = (meta["name"] as? String)?.ifBlank { null }
                ?: (meta["full_name"] as? String)?.ifBlank { null }
                ?: if (isEmail) identifier.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() } else "Kisan ($cleanKey)"
            val farmName = (meta["farmName"] as? String) ?: (meta["farm_name"] as? String) ?: "Apna Agri Farm"
            val district = (meta["district"] as? String) ?: "Faisalabad"
            val province = (meta["province"] as? String) ?: "Punjab"
            val farmLocation = (meta["farmLocation"] as? String) ?: "$district, $province"
            val acres = (meta["totalAcres"] as? Number)?.toDouble() ?: 10.0
            val crops = (meta["primaryCrops"] as? String) ?: "Wheat, Cotton"

            val userEntity = UserEntity(
                phoneOrEmail = cleanKey,
                fullName = name,
                phone = if (!isEmail) cleanKey else "03001234567",
                email = emailToTry ?: "",
                passwordHash = PasswordHasher.hash(password),
                supabaseUserId = cloudAuth.userId.orEmpty(),
                farmName = farmName,
                district = district,
                province = province,
                farmLocation = farmLocation,
                totalAcres = acres,
                primaryCropsString = crops,
                isActiveSession = true
            )
            userDao.clearActiveSessions()
            userDao.insertOrUpdateUser(userEntity)
            
            // Sync to Supabase profiles
            repositoryScope.launch {
                supabaseDataSync.syncProfile(userEntity)
            }

            val profile = userEntity.toModel(isAuthenticated = true)
            _profile.value = profile
            return Result.success(profile)
        }

        // 2. Offline fallback is allowed only when the backend itself is unreachable.
        // A real Supabase "invalid credentials" response must never be bypassed locally.
        val backendUnavailable = cloudAuth.errorMessage?.startsWith(
            "Backend connection failed", ignoreCase = true
        ) == true
        if (!backendUnavailable) {
            return Result.failure(
                Exception(cloudAuth.errorMessage ?: "Authentication failed")
            )
        }

        // 3. Fallback to Local Room database if an account already exists.
        val existing = userDao.getUserByPhoneOrEmail(cleanKey)
            ?: emailToTry?.let { userDao.getUserByPhoneOrEmail(it) }

        if (existing != null) {
            val valid = PasswordHasher.verify(password, existing.passwordHash) ||
                    PasswordHasher.isLegacyPlaintextMatch(password, existing.passwordHash)
            if (!valid) {
                return Result.failure(Exception("غلط پاس ورڈ! براہ کرم درست پاس ورڈ درج کریں (Incorrect password)"))
            }

            // Migrate accounts created by older builds that stored plaintext locally.
            val migrated = if (PasswordHasher.isLegacyPlaintextMatch(password, existing.passwordHash)) {
                existing.copy(passwordHash = PasswordHasher.hash(password))
            } else {
                existing
            }
            userDao.clearActiveSessions()
            userDao.insertOrUpdateUser(migrated.copy(isActiveSession = true))
            val profile = migrated.toModel(isAuthenticated = true)
            _profile.value = profile
            // Cloud sync will work only when a valid Supabase session exists.
            repositoryScope.launch { supabaseDataSync.syncProfile(migrated) }
            return Result.success(profile)
        }

        // Account does not exist in the local database
        val errMessage = if (cloudAuth.errorMessage?.contains("Invalid login credentials", ignoreCase = true) == true) {
            "غلط فون نمبر یا پاس ورڈ! اگر نیا صارف ہیں تو سائن اپ کریں (Invalid credentials. Please Sign Up if you are a new user)"
        } else {
            cloudAuth.errorMessage ?: "اکاؤنٹ موجود نہیں ہے۔ برائے مہربانی پہلے سائن اپ کریں (Account not found. Please Sign Up to create a new account)"
        }
        return Result.failure(Exception(errMessage))
    }

    /** Password changes are completed only through Supabase's verified recovery link/OTP flow. */
    suspend fun startGuestSession(): Result<Unit> {
        val result = supabaseAuthService.signInAnonymously()
        return if (result.isSuccess && result.userId != null) Result.success(Unit)
        else Result.failure(Exception(result.errorMessage ?: "Guest sign-in failed"))
    }

    suspend fun recoverPassword(identifier: String, newPassword: String = ""): Result<String> {
        val cleanKey = identifier.trim().replace(" ", "").replace("-", "").replace("+92", "0")
        val emailToTry = if (identifier.contains("@")) identifier.trim().lowercase() else "$cleanKey@farmify.pk"
        if (newPassword.isNotBlank()) {
            // Never overwrite a local credential before remote identity verification.
            return Result.failure(Exception("For security, reset the password through the verified recovery email/link."))
        }
        val cloudRes = supabaseAuthService.recoverPassword(emailToTry)
        return if (cloudRes.isSuccess) Result.success("Password recovery instructions sent. Complete verification before choosing a new password.")
        else Result.failure(Exception(cloudRes.errorMessage ?: "Account recovery failed"))
    }

    suspend fun signup(
        fullName: String,
        phone: String,
        email: String,
        password: String,
        farmName: String,
        district: String,
        province: String,
        totalAcres: Double,
        primaryCrops: List<String>
    ): Result<FarmerProfile> {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "").replace("+92", "0")
        val cleanEmail = email.trim().lowercase().takeIf { it.isNotBlank() }
        val primaryKey = cleanPhone.ifBlank { cleanEmail.orEmpty() }

        // 1. Try Supabase Cloud Sign-Up
        val metaMap = mapOf(
            "name" to fullName.trim().ifBlank { "Pakistani Kisan" },
            "full_name" to fullName.trim().ifBlank { "Pakistani Kisan" },
            "phone" to cleanPhone.ifBlank { "03001234567" },
            "farmName" to farmName.trim().ifBlank { "Apna Agri Farm" },
            "district" to district.ifBlank { "Faisalabad" },
            "province" to province.ifBlank { "Punjab" },
            "farmLocation" to "${district.ifBlank { "Faisalabad" }}, ${province.ifBlank { "Punjab" }}",
            "totalAcres" to (if (totalAcres > 0) totalAcres else 10.0),
            "primaryCrops" to if (primaryCrops.isNotEmpty()) primaryCrops.joinToString(", ") else "Wheat, Cotton"
        )
        val cloudSignup = supabaseAuthService.signUp(email = cleanEmail.orEmpty(), phone = if (cleanEmail == null) normalizePhone(cleanPhone) else null, password = password, metadata = metaMap)
        if (!cloudSignup.isSuccess &&
            cloudSignup.errorMessage?.startsWith("Backend connection failed", ignoreCase = true) != true) {
            return Result.failure(Exception(cloudSignup.errorMessage ?: "Signup failed"))
        }

        // 2. Save locally in Room SQLite
        if (cloudSignup.accessToken == null) {
            supabaseAuthService.clearSession()
        }

        val entity = UserEntity(
            phoneOrEmail = primaryKey,
            fullName = fullName.trim().ifBlank { "Pakistani Kisan" },
            phone = cleanPhone.ifBlank { "03001234567" },
            email = cleanEmail,
            passwordHash = PasswordHasher.hash(password),
            supabaseUserId = cloudSignup.userId.orEmpty(),
            farmName = farmName.trim().ifBlank { "Apna Agri Farm" },
            district = district.ifBlank { "Faisalabad" },
            province = province.ifBlank { "Punjab" },
            farmLocation = "${district.ifBlank { "Faisalabad" }}, ${province.ifBlank { "Punjab" }}",
            totalAcres = if (totalAcres > 0) totalAcres else 10.0,
            primaryCropsString = if (primaryCrops.isNotEmpty()) primaryCrops.joinToString(",") else "Wheat,Cotton",
            isActiveSession = true
        )

        userDao.clearActiveSessions()
        userDao.insertOrUpdateUser(entity)

        // Sync to Supabase
        repositoryScope.launch {
            supabaseDataSync.syncProfile(entity)
        }

        val newProfile = entity.toModel(isAuthenticated = true)
        _profile.value = newProfile
        return Result.success(newProfile)
    }

    suspend fun updateProfile(
        name: String,
        phone: String,
        farmName: String,
        farmLocation: String,
        totalAcres: Double
    ) {
        val current = _profile.value
        val updated = current.copy(
            fullName = name,
            phone = phone,
            farmName = farmName,
            farmLocation = farmLocation,
            totalAcres = totalAcres
        )
        _profile.value = updated

        // Sync update to Room database
        val cleanKey = phone.trim().replace(" ", "").replace("-", "").replace("+92", "0").ifBlank { current.email }
        val existing = userDao.getUserByPhoneOrEmail(cleanKey)
            ?: userDao.getUserByPhoneOrEmail(current.email)
        val entity = UserEntity(
            phoneOrEmail = existing?.phoneOrEmail ?: cleanKey,
            fullName = name,
            phone = phone,
            email = current.email,
            passwordHash = existing?.passwordHash ?: throw IllegalStateException("Local user record not found; refusing to overwrite password credentials"), 
            supabaseUserId = existing?.supabaseUserId.orEmpty(),
            farmName = farmName,
            district = current.district,
            province = current.province,
            farmLocation = farmLocation,
            totalAcres = totalAcres,
            primaryCropsString = current.primaryCrops.joinToString(","),
            isActiveSession = true,
            weatherNotifications = current.weatherNotifications,
            mandiNotifications = current.mandiNotifications,
            diseaseAlerts = current.diseaseAlerts,
            khataReminders = current.khataReminders
        )
        userDao.insertOrUpdateUser(entity)

        // Sync to Supabase profiles
        repositoryScope.launch {
            supabaseDataSync.syncProfile(entity)
        }
    }

    fun close() { repositoryScope.cancel() }

    fun toggleNotification(type: String, enabled: Boolean) {
        _profile.update {
            when (type) {
                "weather" -> it.copy(weatherNotifications = enabled)
                "mandi" -> it.copy(mandiNotifications = enabled)
                "disease" -> it.copy(diseaseAlerts = enabled)
                "khata" -> it.copy(khataReminders = enabled)
                else -> it
            }
        }
    }

    suspend fun logout() {
        userDao.clearActiveSessions()
        supabaseAuthService.clearSession()
        _profile.update { it.copy(isAuthenticated = false) }
    }

    private fun UserEntity.toModel(isAuthenticated: Boolean = true): FarmerProfile {
        return FarmerProfile(
            fullName = this.fullName,
            phone = this.phone,
            email = this.email,
            farmName = this.farmName,
            district = this.district,
            province = this.province,
            farmLocation = this.farmLocation,
            totalAcres = this.totalAcres,
            primaryCrops = this.primaryCropsString.split(",").map { it.trim() }.filter { it.isNotBlank() },
            isAuthenticated = isAuthenticated,
            weatherNotifications = this.weatherNotifications,
            mandiNotifications = this.mandiNotifications,
            diseaseAlerts = this.diseaseAlerts,
            khataReminders = this.khataReminders
        )
    }
}


private fun normalizePhone(value: String): String {
    val digits = value.filter { it.isDigit() }
    return when {
        digits.startsWith("92") -> "+$digits"
        digits.startsWith("0") -> "+92${digits.drop(1)}"
        else -> "+92$digits"
    }
}
