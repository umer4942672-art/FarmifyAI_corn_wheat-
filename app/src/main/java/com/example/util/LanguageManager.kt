package com.example.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    ENGLISH("en", "English", "English"),
    URDU("ur", "Urdu", "اردو")
}

class LanguageState {
    var currentLanguage by mutableStateOf(AppLanguage.ENGLISH)
        private set

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
    }

    val isUrdu: Boolean
        get() = currentLanguage == AppLanguage.URDU
}

val LocalAppLanguage = compositionLocalOf { LanguageState() }

object AppStrings {
    fun get(key: String, language: AppLanguage): String {
        val entry = stringMap[key] ?: return key
        return if (language == AppLanguage.URDU) entry.second else entry.first
    }

    private val stringMap = mapOf(
        // App & Navigation
        "app_title" to Pair("FarmifyAI", "فارمائی فائی اے آئی"),
        "tagline" to Pair("Smart Farming Assistant for Pakistani Farmers", "پاکستانی کسانوں کا سمارٹ زرعی معاون"),
        "nav_dashboard" to Pair("Home", "ہوم"),
        "nav_khata" to Pair("Smart Khata", "سمارٹ کھاتہ"),
        "nav_mandi" to Pair("Mandi Rates", "منڈی کے بھاؤ"),
        "nav_weather" to Pair("Weather", "موسم"),
        "nav_scan" to Pair("AI Plant Scan", "پودا سکین"),
        "nav_settings" to Pair("Settings", "ترتیبات"),

        // Greetings & Headers
        "greeting_prefix" to Pair("Assalam-o-Alaikum,", "السلام علیکم،"),
        "offline_mode" to Pair("Offline Mode", "آف لائن موڈ"),
        "synced" to Pair("All Synced", "تمام ریکارڈ محفوظ ہیں"),
        "syncing" to Pair("Syncing data...", "ڈیٹا ہم وقت سازی ہو رہی ہے..."),

        // Weather Card & Screen
        "weather_title" to Pair("Farm Weather", "کھیت کا موسم"),
        "weather_humidity" to Pair("Humidity", "نمی"),
        "weather_wind" to Pair("Wind", "ہوا کی رفتار"),
        "weather_rain_prob" to Pair("Rain Chance", "بارش کا امکان"),
        "weather_feels_like" to Pair("Feels Like", "محسوس درجہ حرارت"),
        "weather_view_7day" to Pair("View 7-Day Forecast", "7 دن کی پیشگوئی دیکھیں"),
        "weather_advisory" to Pair("Farming Advisory", "زرعی مشورہ"),
        "irrigation_advice" to Pair("Irrigation Advice", "آبپاشی کا مشورہ"),
        "spray_advice" to Pair("Spray Advice", "سپرے کا مشورہ"),
        "harvest_advice" to Pair("Harvesting Recommendation", "کٹائی کی سفارش"),

        // Smart Khata
        "khata_title" to Pair("Smart Khata Ledger", "سمارٹ کھاتہ لیجر"),
        "khata_subtitle" to Pair("Digital ledger for daily farm expenses & crop income", "روزمرہ اخراجات اور فصلوں کی آمدن کا ڈیجیٹل کھاتہ"),
        "total_income" to Pair("Total Income", "کل آمدن"),
        "total_expense" to Pair("Total Expenses", "کل اخراجات"),
        "net_profit" to Pair("Net Profit", "خالص منافع"),
        "today_income" to Pair("Today's Income", "آج کی آمدن"),
        "today_expense" to Pair("Today's Expense", "آج کا خرچہ"),
        "add_income" to Pair("+ Add Income", "+ آمدن درج کریں"),
        "add_expense" to Pair("+ Add Expense", "+ خرچہ درج کریں"),
        "add_field_work" to Pair("+ Add Field Work", "+ فیلڈ کام درج کریں"),
        "filter_all" to Pair("All", "تمام"),
        "filter_income" to Pair("Income", "آمدن"),
        "filter_expense" to Pair("Expenses", "اخراجات"),
        "filter_field_work" to Pair("Field Work", "کھیت کا کام"),
        "no_records" to Pair("No transactions recorded yet. Tap + to add!", "ابھی تک کوئی اندراج موجود نہیں۔ نیا درج کریں!"),
        "crop_wise_profit" to Pair("Crop-wise Profit & Loss", "فصل کے لحاظ سے نفع و نقصان"),
        "expense_breakdown" to Pair("Expense Categories", "اخراجات کی تفصیل"),
        "recent_transactions" to Pair("Recent Farm Activity", "حالیہ زرعی سرگرمیاں"),

        // Field Work Form
        "record_field_work" to Pair("Record Daily Field Work", "روزانہ فیلڈ ورک درج کریں"),
        "crop_name" to Pair("Crop Name", "فصل کا نام"),
        "field_name" to Pair("Field / Plot Name", "کھیت / رقبہ کا نام"),
        "field_size" to Pair("Field Size (Acres)", "رقبہ (ایکڑ)"),
        "activity_type" to Pair("Activity Type", "کام کی قسم"),
        "description" to Pair("Description / Notes", "تفصیل / نوٹس"),
        "labor_cost" to Pair("Labor Cost (PKR)", "مزدوری خرچہ (روپے)"),
        "seed_cost" to Pair("Seed Cost (PKR)", "بیج کا خرچہ (روپے)"),
        "fertilizer_cost" to Pair("Fertilizer Cost (PKR)", "کھاد کا خرچہ (روپے)"),
        "pesticide_cost" to Pair("Pesticide Cost (PKR)", "سپرے کا خرچہ (روپے)"),
        "irrigation_cost" to Pair("Irrigation Cost (PKR)", "ٹیوب ویل / نہری پانی (روپے)"),
        "machinery_cost" to Pair("Tractor / Machinery (PKR)", "ٹریکٹر / مشینری (روپے)"),
        "transport_cost" to Pair("Transport (PKR)", "کرایہ / ٹرانسپورٹ (روپے)"),
        "other_cost" to Pair("Other Expenses (PKR)", "دیگر متفرق اخراجات (روپے)"),
        "save_record" to Pair("Save to Smart Khata", "کھاتہ میں محفوظ کریں"),
        "cancel" to Pair("Cancel", "منسوخ کریں"),

        // Income Form
        "record_income" to Pair("Record Crop Sale / Income", "فصل کی فروخت / آمدن درج کریں"),
        "quantity" to Pair("Quantity", "مقدار / وزن"),
        "unit" to Pair("Unit", "اکائی"),
        "unit_mann" to Pair("Mann (40 KG)", "من (40 کلو)"),
        "unit_kg" to Pair("KG", "کلو گرام"),
        "unit_ton" to Pair("Ton", "ٹن"),
        "unit_bori" to Pair("Bags / Bori", "بوری"),
        "selling_price_per_unit" to Pair("Price per Unit (PKR)", "فی اکائی قیمت (روپے)"),
        "buyer_or_mandi" to Pair("Buyer / Mandi / Arthi", "خریدار / منڈی / آڑھتی"),

        // Mandi Rates
        "mandi_title" to Pair("Live Pakistani Mandi Rates", "پاکستانی غلہ و سبزی منڈی کے بھاؤ"),
        "mandi_subtitle" to Pair("Official market rates updated daily from key hubs", "پنجاب، سندھ اور کے پی کے کی اہم منڈیوں کے روزانہ ریٹس"),
        "search_crop" to Pair("Search crop or mandi...", "فصل یا منڈی تلاش کریں..."),
        "all_mandis" to Pair("All Mandis", "تمام منڈیاں"),
        "filter_punjab" to Pair("Punjab", "پنجاب"),
        "filter_sindh" to Pair("Sindh", "سندھ"),
        "filter_kpk" to Pair("KPK", "کے پی کے"),
        "price_per_kg" to Pair("PKR / KG", "روپے / کلو"),
        "price_per_mann" to Pair("PKR / 40 KG (Mann)", "روپے / من"),
        "mandi_updated" to Pair("Updated Today", "آج کا تازہ ترین ریٹ"),
        "favorite_crops" to Pair("Favorite Watchlist", "پسندیدہ فصلیں"),
        "trend_up" to Pair("Rising", "بڑھ رہا ہے"),
        "trend_down" to Pair("Falling", "کم ہو رہا ہے"),
        "trend_stable" to Pair("Stable", "مستحکم"),

        // AI Disease Detection
        "ai_scan_title" to Pair("AI Plant Health & Disease Scanner", "مصنوعی ذہانت پودا بیماری سکینر"),
        "ai_scan_subtitle" to Pair("Identify crop diseases instantly & get Urdu/English treatments", "پودے کی تصویر لے کر بیماری کی تشخیص اور فوری علاج حاصل کریں"),
        "take_photo" to Pair("Take Photo", "کیمرے سے تصویر لیں"),
        "choose_gallery" to Pair("Choose from Gallery", "گیلری سے منتخب کریں"),
        "scan_sample" to Pair("Quick Sample Test", "نمونہ پودے سے ٹیسٹ کریں"),
        "analyzing" to Pair("Analyzing plant leaf with AI...", "مصنوعی ذہانت پودے کا معائنہ کر رہی ہے..."),
        "scan_result" to Pair("Diagnosis Report", "تشخیصی رپورٹ"),
        "status_healthy" to Pair("Healthy Crop", "صحت مند فصل"),
        "status_diseased" to Pair("Disease Detected", "بیماری کی تشخیص"),
        "confidence" to Pair("AI Confidence", "درستگی کا تناسب"),
        "symptoms" to Pair("Key Symptoms", "علامات"),
        "chemical_cure" to Pair("Recommended Chemical Treatment", "تجویز کردہ زرعی ادویات / سپرے"),
        "organic_prevention" to Pair("Organic & Cultural Prevention", "احتیاطی تدابیر اور قدرتی طریقے"),
        "advisory_disclaimer" to Pair("Disclaimer: AI advice is for guidance. Consult your local agriculture officer or extension center for severe outbreaks.", "نوٹ: یہ مشورہ رہنمائی کے لیے ہے۔ شدید نقصان کی صورت میں محکمہ زراعت کے ماہر سے رابطہ کریں۔"),
        "save_to_history" to Pair("Save to History", "ہسٹری میں محفوظ کریں"),
        "scan_another" to Pair("Scan Another Plant", "ایک اور پودا سکین کریں"),
        "history_title" to Pair("Past Disease Scans", "پچھلے سکینز کا ریکارڈ"),

        // Settings
        "settings_title" to Pair("App Settings & Profile", "ترتیبات اور پروفائل"),
        "farmer_profile" to Pair("Farmer Profile", "کسان کا پروفائل"),
        "farm_location" to Pair("Farm Location", "کھیت کا مقام"),
        "farm_size" to Pair("Total Farm Area", "کل زرعی رقبہ"),
        "language_setting" to Pair("Language / زبان", "زبان / Language"),
        "notifications" to Pair("Notifications & Alerts", "نوٹیفیکیشنز اور الرٹس"),
        "weather_alerts" to Pair("Weather & Rain Alerts", "موسم اور بارش الرٹ"),
        "mandi_alerts" to Pair("Daily Mandi Price Updates", "روزانہ منڈی ریٹ الرٹ"),
        "khata_reminder" to Pair("Daily Khata Entry Reminder", "کھاتہ درج کرنے کی یاد دہانی"),
        "agri_helplines" to Pair("Pakistan Agri Helplines", "پاکستان ایگریکلچر ہیلپ لائنز"),
        "punjab_helpline" to Pair("Punjab Agri Helpline: 0800-15000", "پنجاب زراعت ہیلپ لائن: 0800-15000"),
        "sindh_helpline" to Pair("Sindh Agri Helpline: 0800-29000", "سندھ زراعت ہیلپ لائن: 0800-29000"),
        "backup_sync" to Pair("Backup & Cloud Sync", "بیک اپ اور کلاؤڈ سنک"),
        "about_app" to Pair("About FarmifyAI", "فارمائی فائی کے بارے میں")
    )
}

@Composable
fun str(key: String): String {
    val langState = LocalAppLanguage.current
    return AppStrings.get(key, langState.currentLanguage)
}
