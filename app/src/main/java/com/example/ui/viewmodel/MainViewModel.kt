package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.KhataEntryEntity
import com.example.data.model.*
import com.example.data.repository.*
import com.example.ui.theme.AppThemeMode
import com.example.util.AppLanguage
import com.example.util.VoiceAssistantHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KhataSummaryStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netProfit: Double = 0.0,
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val totalEntriesCount: Int = 0,
    val mostProfitableCrop: String = "Wheat",
    val cropProfits: Map<String, Double> = emptyMap(),
    val expenseCategories: Map<String, Double> = emptyMap()
)

data class MandiUiFilter(
    val searchQuery: String = "",
    val selectedProvince: String = "All", // "All", "Punjab", "Sindh", "KPK"
    val selectedCategory: String = "All", // "All", "Grain", "Cash Crop", "Vegetable", "Fruit", "Oilseed"
    val sortBy: MandiSort = MandiSort.PRICE_HIGH_TO_LOW,
    val showFavoritesOnly: Boolean = false,
    val isPerMann: Boolean = false // false = PKR/KG, true = PKR/Mann
)

enum class MandiSort {
    PRICE_HIGH_TO_LOW,
    PRICE_LOW_TO_HIGH,
    TOP_GAINERS,
    NAME
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val khataRepository = KhataRepository(db.khataDao(), application)
    val mandiRepository = MandiRepository()
    val weatherRepository = WeatherRepository()
    val diseaseRepository = DiseaseDetectionRepository(db.diseaseScanDao(), application)
    val userRepository = UserRepository(db.userDao(), application)
    val cloudSyncRepository = CloudSyncRepository(db.khataDao(), db.diseaseScanDao(), application)

    // Language state
    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // App Theme State
    private val _currentTheme = MutableStateFlow(AppThemeMode.EMERALD)
    val currentTheme: StateFlow<AppThemeMode> = _currentTheme.asStateFlow()

    fun setAppTheme(theme: AppThemeMode) {
        _currentTheme.value = theme
    }

    // Voice & Chat Repositories
    val kisanChatRepository = KisanChatRepository(application, mandiRepository)
    val cropGuideRepository = CropGuideRepository()
    val voiceHelper = VoiceAssistantHelper(application)

    // Kisan Dost AI Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI_ASSISTANT,
                textEn = "Assalam-o-Alaikum! I am Kisan Dost AI, your smart farming companion. How can I help you with your crops, fertilizer, mandi rates, or pest sprays today?",
                textUr = "السلام علیکم! میں ہوں آپ کا کسان دوست AI۔ آج میں آپ کی فصلوں، کھاد کے حساب، منڈی ریٹس یا سپرے کے بارے میں کیا مدد کر سکتا ہوں؟",
                suggestedActions = listOf(
                    "گندم کی کھاد کا شیڈول",
                    "کپاس میں سفید مکھی کا علاج",
                    "آج کے منڈی کے ریٹ",
                    "آلو میں جھلساؤ کا سپرے"
                )
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _currentChatInput = MutableStateFlow("")
    val currentChatInput: StateFlow<String> = _currentChatInput.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    fun onChatInputChange(input: String) {
        _currentChatInput.value = input
    }

    fun sendChatMessage(queryText: String? = null) {
        val query = (queryText ?: _currentChatInput.value).trim()
        if (query.isBlank()) return

        _currentChatInput.value = ""
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            textEn = query,
            textUr = query
        )

        _chatMessages.update { it + userMsg }
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val isUrdu = _currentLanguage.value == AppLanguage.URDU ||
                        query.any { Character.UnicodeBlock.of(it) == Character.UnicodeBlock.ARABIC }
                val aiResponse = kisanChatRepository.getAgriAiResponse(
                    userQuery = query,
                    chatHistory = _chatMessages.value,
                    isUrdu = isUrdu
                )
                _chatMessages.update { it + aiResponse }
            } catch (e: Exception) {
                _chatMessages.update {
                    it + ChatMessage(
                        sender = MessageSender.AI_ASSISTANT,
                        textEn = "I encountered an issue connecting to the AI agri server. Please try again or check local guides.",
                        textUr = "سرور سے رابطہ کرنے میں مسئلہ پیش آیا ہے۔ براہ کرم دوبارہ کوشش کریں۔",
                        isError = true
                    )
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI_ASSISTANT,
                textEn = "Chat reset. How can I assist your farm today?",
                textUr = "چیٹ ری سیٹ ہو گئی ہے۔ آج آپ کی کیا زرعی رہنمائی کروں؟",
                suggestedActions = listOf(
                    "گندم میں زرد کنگی کا سپرے",
                    "کپاس کے لیے کھاد کا فارمولا",
                    "دھان میں تنے کی سنڈی کا علاج",
                    "تازہ ترین منڈی ریٹس"
                )
            )
        )
    }

    fun speakAudio(text: String, isUrdu: Boolean = false, utteranceId: String = "ai_audio") {
        voiceHelper.speak(text, isUrdu, utteranceId)
    }

    fun stopAudio() {
        voiceHelper.stop()
    }

    fun setVoiceListening(listening: Boolean) {
        _isVoiceListening.value = listening
    }

    // Crops & Diseases Directory State
    val allCrops: StateFlow<List<CropGuide>> = cropGuideRepository.cropsList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiseaseGuides: StateFlow<List<PlantDiseaseGuide>> = cropGuideRepository.diseaseGuides
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCrop = MutableStateFlow<CropGuide?>(null)
    val selectedCrop: StateFlow<CropGuide?> = _selectedCrop.asStateFlow()

    private val _selectedDiseaseGuide = MutableStateFlow<PlantDiseaseGuide?>(null)
    val selectedDiseaseGuide: StateFlow<PlantDiseaseGuide?> = _selectedDiseaseGuide.asStateFlow()

    private val _guideSearchQuery = MutableStateFlow("")
    val guideSearchQuery: StateFlow<String> = _guideSearchQuery.asStateFlow()

    private val _guideSelectedTab = MutableStateFlow(0) // 0 = Crops, 1 = Plant Diseases
    val guideSelectedTab: StateFlow<Int> = _guideSelectedTab.asStateFlow()

    fun setGuideTab(tabIndex: Int) {
        _guideSelectedTab.value = tabIndex
    }

    fun setGuideSearchQuery(query: String) {
        _guideSearchQuery.value = query
    }

    fun selectCrop(crop: CropGuide?) {
        _selectedCrop.value = crop
    }

    fun selectDiseaseGuide(disease: PlantDiseaseGuide?) {
        _selectedDiseaseGuide.value = disease
    }

    override fun onCleared() {
        diseaseRepository.close()
        userRepository.close()
        voiceHelper.shutdown()
        super.onCleared()
    }

    fun toggleLanguage() {
        _currentLanguage.update {
            if (it == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
        }
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    // User Profile
    val userProfile: StateFlow<FarmerProfile> = userRepository.profile

    // Khata State - Scoped strictly to the active user (new users start with clean empty khata)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val khataEntries: StateFlow<List<KhataEntryEntity>> = userProfile.flatMapLatest { profile ->
        val userKey = profile.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0").ifBlank { profile.email.trim() }
        if (profile.isAuthenticated && userKey.isNotBlank()) {
            khataRepository.getEntriesForUser(userKey)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val khataSummary: StateFlow<KhataSummaryStats> = khataEntries.map { entries ->
        calculateKhataStats(entries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KhataSummaryStats())

    // Mandi Filter & List
    private val _mandiFilter = MutableStateFlow(MandiUiFilter())
    val mandiFilter: StateFlow<MandiUiFilter> = _mandiFilter.asStateFlow()

    val filteredMandiRates: StateFlow<List<MandiRate>> = combine(
        mandiRepository.rates,
        _mandiFilter
    ) { rates, filter ->
        var list = rates

        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase(Locale.ENGLISH)
            list = list.filter {
                it.cropNameEn.lowercase(Locale.ENGLISH).contains(q) ||
                it.cropNameUr.contains(q) ||
                it.city.lowercase(Locale.ENGLISH).contains(q) ||
                it.mandiName.lowercase(Locale.ENGLISH).contains(q)
            }
        }

        if (filter.selectedProvince != "All") {
            list = list.filter { it.province.equals(filter.selectedProvince, ignoreCase = true) }
        }

        if (filter.selectedCategory != "All") {
            list = list.filter { it.category.equals(filter.selectedCategory, ignoreCase = true) }
        }

        if (filter.showFavoritesOnly) {
            list = list.filter { it.isFavorite }
        }

        when (filter.sortBy) {
            MandiSort.PRICE_HIGH_TO_LOW -> list.sortedByDescending { it.pricePerKg }
            MandiSort.PRICE_LOW_TO_HIGH -> list.sortedBy { it.pricePerKg }
            MandiSort.TOP_GAINERS -> list.sortedByDescending { it.changePercent }
            MandiSort.NAME -> list.sortedBy { it.cropNameEn }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weather State
    val weatherState: StateFlow<WeatherDashboardState> = weatherRepository.weatherState

    // Disease Scan State
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val scanHistory: StateFlow<List<PlantDiseaseResult>> = userProfile.flatMapLatest { profile ->
        if (profile.isAuthenticated) {
            val userKey = profile.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0")
                .ifBlank { profile.email.trim() }
            diseaseRepository.getScansForUser(userKey)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScanResult = MutableStateFlow<PlantDiseaseResult?>(null)

    private val _selectedDiseaseCrop = MutableStateFlow(DiseaseCrop.WHEAT)
    val selectedDiseaseCrop: StateFlow<DiseaseCrop> = _selectedDiseaseCrop.asStateFlow()
    val currentScanResult: StateFlow<PlantDiseaseResult?> = _currentScanResult.asStateFlow()

    private val _isAnalyzingPlant = MutableStateFlow(false)
    val isAnalyzingPlant: StateFlow<Boolean> = _isAnalyzingPlant.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    // Toast/Snackbar Message
    private val _userFeedback = MutableSharedFlow<String>()
    val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

    init {
        viewModelScope.launch {
            khataRepository.initializeSampleDataIfEmpty()
            weatherRepository.refreshWeather()
            mandiRepository.refreshRates()
            syncWithSupabaseNow()
            // Push anything that failed to reach Supabase in an earlier session.
            retryPendingCloudSync()
        }
    }

    /**
     * Pulls the farmer's cloud state back into Room. Called after a successful
     * login/signup so a fresh install or a new phone shows the existing ledger
     * and scan history instead of an empty screen.
     */
    fun restoreCloudData(showFeedback: Boolean = true) {
        viewModelScope.launch {
            val userKey = currentUserKey()
            if (userKey.isBlank()) return@launch
            val summary = cloudSyncRepository.restoreFromCloud(userKey)
            val restored = summary.khataRestored + summary.diseaseRestored
            if (showFeedback && summary.ok && restored > 0) {
                _userFeedback.emit("کلاؤڈ سے $restored ریکارڈ بحال ہوئے (Restored $restored records from cloud)")
            }
        }
    }

    /** Re-sends locally stored rows that never reached Supabase. */
    fun retryPendingCloudSync() {
        viewModelScope.launch {
            if (!userProfile.value.isAuthenticated) return@launch
            runCatching { cloudSyncRepository.retryPendingSync(currentUserKey()) }
        }
    }

    fun syncWithSupabaseNow() {
        viewModelScope.launch {
            try {
                val profile = userProfile.value
                if (!profile.isAuthenticated) return@launch
                val userKey = profile.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0")
                    .ifBlank { profile.email.trim() }
                val currentEntries = khataRepository.getEntriesForUser(userKey).first()
                val syncService = com.example.data.remote.SupabaseDataSyncService(getApplication())
                val userEntity = com.example.data.local.UserEntity(
                    phoneOrEmail = profile.phone.ifBlank { profile.email },
                    fullName = profile.fullName,
                    phone = profile.phone,
                    email = profile.email,
                    passwordHash = "saved",
                    farmName = profile.farmName,
                    district = profile.district,
                    province = profile.province,
                    farmLocation = profile.farmLocation,
                    totalAcres = profile.totalAcres,
                    primaryCropsString = profile.primaryCrops.joinToString(",")
                )
                syncService.syncProfile(userEntity)
                val khataOk = syncService.syncMultipleKhataEntries(currentEntries)
                if (khataOk) {
                    _userFeedback.emit("کلاؤڈ سنک کامیاب! Data synced to Supabase")
                }
            } catch (e: Exception) {
                // Keep local safe
            }
        }
    }

    // Khata Actions
    fun addIncome(
        cropName: String,
        quantity: Double,
        unit: String,
        pricePerUnit: Double,
        buyerOrMandi: String,
        fieldName: String,
        description: String
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val total = quantity * pricePerUnit
            val currentUserId = userProfile.value.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0").ifBlank { userProfile.value.email.trim() }
            val entry = KhataEntryEntity(
                userId = currentUserId,
                entryType = "INCOME",
                date = sdf.format(Date()),
                cropName = cropName.ifBlank { "Wheat" },
                fieldName = fieldName,
                quantity = quantity,
                unit = unit,
                sellingPricePerUnit = pricePerUnit,
                totalAmount = total,
                buyerOrMandi = buyerOrMandi,
                description = description
            )
            khataRepository.insertEntry(entry)
            _userFeedback.emit("Income of PKR ${total.toInt()} saved to Smart Khata!")
        }
    }

    fun addExpense(
        cropName: String,
        fieldName: String,
        category: String,
        amount: Double,
        description: String
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val currentUserId = userProfile.value.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0").ifBlank { userProfile.value.email.trim() }
            val entry = KhataEntryEntity(
                userId = currentUserId,
                entryType = "EXPENSE",
                date = sdf.format(Date()),
                cropName = cropName.ifBlank { "General Farm" },
                fieldName = fieldName,
                activityType = category,
                totalAmount = amount,
                otherExpenses = amount,
                description = description
            )
            khataRepository.insertEntry(entry)
            _userFeedback.emit("Expense of PKR ${amount.toInt()} saved!")
        }
    }

    fun addFieldWork(
        cropName: String,
        fieldName: String,
        fieldSizeAcres: Double,
        activityType: String,
        description: String,
        laborCost: Double,
        seedCost: Double,
        fertilizerCost: Double,
        pesticideCost: Double,
        irrigationCost: Double,
        machineryCost: Double,
        transportCost: Double,
        otherCost: Double
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val totalExpenses = laborCost + seedCost + fertilizerCost + pesticideCost +
                    irrigationCost + machineryCost + transportCost + otherCost
            val currentUserId = userProfile.value.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0").ifBlank { userProfile.value.email.trim() }

            val entry = KhataEntryEntity(
                userId = currentUserId,
                entryType = "FIELD_WORK",
                date = sdf.format(Date()),
                cropName = cropName.ifBlank { "Wheat" },
                fieldName = fieldName,
                fieldSizeAcres = fieldSizeAcres,
                activityType = activityType,
                description = description,
                laborCost = laborCost,
                seedCost = seedCost,
                fertilizerCost = fertilizerCost,
                pesticideCost = pesticideCost,
                irrigationCost = irrigationCost,
                machineryCost = machineryCost,
                transportationCost = transportCost,
                otherExpenses = otherCost,
                totalAmount = totalExpenses
            )
            khataRepository.insertEntry(entry)
            _userFeedback.emit("Field work recorded successfully!")
        }
    }

    fun deleteKhataEntry(id: Long) {
        viewModelScope.launch {
            khataRepository.deleteEntry(id)
            _userFeedback.emit("Entry deleted")
        }
    }

    // Mandi Actions
    fun updateMandiSearch(query: String) {
        _mandiFilter.update { it.copy(searchQuery = query) }
    }

    fun updateMandiProvince(province: String) {
        _mandiFilter.update { it.copy(selectedProvince = province) }
    }

    fun updateMandiCategory(category: String) {
        _mandiFilter.update { it.copy(selectedCategory = category) }
    }

    fun updateMandiSort(sort: MandiSort) {
        _mandiFilter.update { it.copy(sortBy = sort) }
    }

    fun toggleMandiFavoriteFilter() {
        _mandiFilter.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun toggleMandiUnit() {
        _mandiFilter.update { it.copy(isPerMann = !it.isPerMann) }
    }

    fun toggleMandiFavorite(id: String) {
        mandiRepository.toggleFavorite(id)
    }

    fun refreshMandiRates() {
        viewModelScope.launch {
            val live = mandiRepository.refreshRates()
            _userFeedback.emit(if (live) "Live Mandi rates refreshed" else "Live Mandi rates are currently unavailable")
        }
    }

    // Weather Actions
    fun selectWeatherDistrict(district: FarmDistrict) {
        viewModelScope.launch {
            weatherRepository.selectDistrict(district)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            weatherRepository.refreshWeather()
            _userFeedback.emit("Weather forecast refreshed")
        }
    }

    fun refreshWeatherForCurrentLocation() {
        viewModelScope.launch {
            val success = weatherRepository.refreshWeatherForCurrentLocation(getApplication<Application>())
            if (!success) {
                _userFeedback.emit("Current location unavailable; showing the saved weather location")
            }
        }
    }

    // AI Disease Actions
    fun selectDiseaseCrop(crop: DiseaseCrop) {
        if (_selectedDiseaseCrop.value != crop) {
            _selectedDiseaseCrop.value = crop
            resetScan()
        }
    }

    fun analyzePlantBitmap(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
        _isAnalyzingPlant.value = true
        val crop = _selectedDiseaseCrop.value
        viewModelScope.launch {
            try {
                val result = diseaseRepository.analyzePlantImage(bitmap, crop)
                val imagePath = if (result.isPlantImage) saveScanImage(bitmap) else ""
                val resultWithImage = result.copy(imagePathOrUri = imagePath)
                _currentScanResult.value = resultWithImage
                // Audible confirmation: a double beep for a recognised plant,
                // a distinct error tone when the image was rejected.
                com.example.util.ScanSound.playScanComplete(isSuccess = result.isPlantImage)
            } catch (e: Exception) {
                _userFeedback.emit("Scan error: ${e.message}")
            } finally {
                // Keep the inspection animation visible long enough to be perceived
                // even when the on-device TFLite inference finishes very quickly.
                delay(1200)
                _isAnalyzingPlant.value = false
            }
        }
    }

    fun analyzePlantUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val bitmap = decodeDownsampledBitmap(uri)
                if (bitmap != null) {
                    analyzePlantBitmap(bitmap)
                } else {
                    _userFeedback.emit("Could not decode the selected image")
                }
            } catch (e: Exception) {
                _userFeedback.emit("Could not load image: ${e.message}")
            }
        }
    }

    private fun decodeDownsampledBitmap(uri: Uri, maxDimension: Int = 1600): Bitmap? {
        val resolver = getApplication<Application>().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxDimension || bounds.outHeight / sample > maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun saveScanImage(bitmap: Bitmap): String = try {
        val dir = java.io.File(getApplication<Application>().filesDir, "scan_images").apply { mkdirs() }
        val file = java.io.File(dir, "scan_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        file.absolutePath
    } catch (_: Exception) { "" }

    fun selectHistoryScan(scan: PlantDiseaseResult) {
        _currentScanResult.value = scan
        _selectedImageBitmap.value = scan.imagePathOrUri.takeIf { it.isNotBlank() }?.let { path ->
            BitmapFactory.decodeFile(path)
        }
    }

    fun saveCurrentScan() {
        val result = _currentScanResult.value ?: return
        viewModelScope.launch {
            diseaseRepository.saveScan(result, currentUserKey())
            _userFeedback.emit("Scan saved to history!")
        }
    }

    private fun currentUserKey(): String {
        val profile = userProfile.value
        return profile.phone.trim().replace(" ", "").replace("-", "").replace("+92", "0")
            .ifBlank { profile.email.trim() }
    }

    fun deleteScanHistory(id: Long) {
        viewModelScope.launch {
            diseaseRepository.deleteScan(id)
            _userFeedback.emit("Scan history deleted")
        }
    }

    /**
     * Opens the advisory chat with a question already built from the diagnosis,
     * so the farmer does not have to retype a disease name they just saw.
     */
    fun askAdvisoryAboutDiagnosis(result: PlantDiseaseResult, isUrdu: Boolean) {
        val question = if (isUrdu) {
            "میری ${result.cropName} کی فصل میں ${result.diseaseNameUr} کی تشخیص ہوئی ہے، شدت ${result.severityLevel} ہے۔ " +
                "اس بیماری کے بارے میں تفصیل سے بتائیں: یہ کیوں پھیلتی ہے، آگے کیا احتیاط کروں، اور کتنے دن میں فرق نظر آنا چاہیے؟"
        } else {
            "My ${result.cropName} crop was diagnosed with ${result.diseaseNameEn} at ${result.severityLevel} severity. " +
                "Explain this disease in detail: why it spreads, what I should do next, and how long before I see improvement."
        }
        sendChatMessage(question)
    }

    fun resetScan() {
        _currentScanResult.value = null
        _selectedImageBitmap.value = null
    }

    // User Profile & Authentication

    /**
     * Exact reason the last auth attempt failed.
     *
     * The auth screen is full screen, so the snackbar carrying this text was never
     * visible there and the user only saw a generic "verify credentials" line.
     * Surfacing the real Supabase message ("Email not confirmed", "Invalid login
     * credentials", a network error, and so on) is the difference between a
     * fixable problem and a dead end.
     */
    private val _lastAuthError = MutableStateFlow<String?>(null)
    val lastAuthError: StateFlow<String?> = _lastAuthError.asStateFlow()

    fun clearAuthError() { _lastAuthError.value = null }

    suspend fun login(identifier: String, pass: String): Boolean {
        val result = userRepository.login(identifier, pass)
        return if (result.isSuccess) {
            _lastAuthError.value = null
            _userFeedback.emit("خوش آمدید! Welcome ${result.getOrNull()?.fullName ?: ""}")
            // Bring this farmer's Supabase records down to the device, then push
            // anything still pending from a previous offline session.
            restoreCloudData()
            retryPendingCloudSync()
            true
        } else {
            val reason = result.exceptionOrNull()?.message ?: "Login failed"
            _lastAuthError.value = reason
            _userFeedback.emit(reason)
            false
        }
    }

    suspend fun signup(
        fullName: String,
        phone: String,
        email: String,
        pass: String,
        farmName: String,
        district: String,
        province: String,
        acres: Double,
        crops: List<String>
    ): Boolean {
        val result = userRepository.signup(
            fullName, phone, email, pass, farmName, district, province, acres, crops
        )
        return if (result.isSuccess) {
            _userFeedback.emit("اکاؤنٹ بن گیا! Account created for ${fullName}")
            // A re-registration on a new device may already have cloud records.
            restoreCloudData(showFeedback = false)
            retryPendingCloudSync()
            true
        } else {
            val reason = result.exceptionOrNull()?.message ?: "Signup failed. Please check inputs."
            _lastAuthError.value = reason
            _userFeedback.emit(reason)
            false
        }
    }

    suspend fun recoverPassword(identifier: String, newPassword: String = ""): Boolean {
        val result = userRepository.recoverPassword(identifier, newPassword)
        return if (result.isSuccess) {
            _userFeedback.emit("پاس ورڈ بحالی کی ہدایات بھیج دی گئیں (Recovery instructions sent)")
            true
        } else {
            _userFeedback.emit(result.exceptionOrNull()?.message ?: "Password recovery failed")
            false
        }
    }

    suspend fun quickDemoLogin(): Boolean {
        val res = userRepository.startGuestSession()
        if (res.isSuccess) {
            // startGuestSession now also marks the profile authenticated and
            // creates the local row, so history and khata scope correctly.
            _userFeedback.emit("Guest session started")
            restoreCloudData(showFeedback = false)
        } else {
            _userFeedback.emit(res.exceptionOrNull()?.message ?: "Guest sign-in failed")
        }
        return res.isSuccess
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _userFeedback.emit("لاگ آؤٹ ہو گئے۔ Logged out successfully.")
        }
    }

    fun updateProfile(
        name: String,
        phone: String,
        farmName: String,
        farmLocation: String,
        acres: Double
    ) {
        viewModelScope.launch {
            userRepository.updateProfile(name, phone, farmName, farmLocation, acres)
            _userFeedback.emit("پروفائل اپ ڈیٹ ہو گئی! Profile updated successfully")
        }
    }

    fun toggleNotification(type: String, enabled: Boolean) {
        userRepository.toggleNotification(type, enabled)
    }

    private fun calculateKhataStats(entries: List<KhataEntryEntity>): KhataSummaryStats {
        var totalInc = 0.0
        var totalExp = 0.0
        var todayInc = 0.0
        var todayExp = 0.0
        val cropProfitMap = mutableMapOf<String, Double>()
        val expenseCategoryMap = mutableMapOf<String, Double>()

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val todayStr = sdf.format(Date())

        for (entry in entries) {
            val isToday = entry.date == todayStr
            val inc = entry.effectiveIncome
            val exp = entry.effectiveExpense

            totalInc += inc
            totalExp += exp

            if (isToday) {
                todayInc += inc
                todayExp += exp
            }

            // Crop wise
            val crop = entry.cropName.ifBlank { "General" }
            val currentCropProfit = cropProfitMap.getOrDefault(crop, 0.0)
            cropProfitMap[crop] = currentCropProfit + inc - exp

            // Expense categories
            if (entry.entryType == "FIELD_WORK" || entry.entryType == "EXPENSE") {
                if (entry.fertilizerCost > 0) expenseCategoryMap["Fertilizer"] = expenseCategoryMap.getOrDefault("Fertilizer", 0.0) + entry.fertilizerCost
                if (entry.pesticideCost > 0) expenseCategoryMap["Pesticide/Spray"] = expenseCategoryMap.getOrDefault("Pesticide/Spray", 0.0) + entry.pesticideCost
                if (entry.laborCost > 0) expenseCategoryMap["Labor"] = expenseCategoryMap.getOrDefault("Labor", 0.0) + entry.laborCost
                if (entry.seedCost > 0) expenseCategoryMap["Seed"] = expenseCategoryMap.getOrDefault("Seed", 0.0) + entry.seedCost
                if (entry.irrigationCost > 0) expenseCategoryMap["Irrigation/Fuel"] = expenseCategoryMap.getOrDefault("Irrigation/Fuel", 0.0) + entry.irrigationCost
                if (entry.machineryCost > 0) expenseCategoryMap["Machinery"] = expenseCategoryMap.getOrDefault("Machinery", 0.0) + entry.machineryCost
                if (entry.transportationCost > 0) expenseCategoryMap["Transport"] = expenseCategoryMap.getOrDefault("Transport", 0.0) + entry.transportationCost
                if (entry.otherExpenses > 0) expenseCategoryMap["Other"] = expenseCategoryMap.getOrDefault("Other", 0.0) + entry.otherExpenses
            }
        }

        val bestCrop = cropProfitMap.maxByOrNull { it.value }?.key ?: "Wheat"

        return KhataSummaryStats(
            totalIncome = totalInc,
            totalExpense = totalExp,
            netProfit = totalInc - totalExp,
            todayIncome = todayInc,
            todayExpense = todayExp,
            totalEntriesCount = entries.size,
            mostProfitableCrop = bestCrop,
            cropProfits = cropProfitMap,
            expenseCategories = expenseCategoryMap
        )
    }
}
