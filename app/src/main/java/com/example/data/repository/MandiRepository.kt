package com.example.data.repository

import com.example.data.model.MandiRate
import com.example.data.model.MandiTrend
import com.example.data.remote.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MandiRepository {

    private val initialRates = listOf(
        MandiRate(
            id = "wheat_lahore",
            cropNameEn = "Wheat (Gandum)",
            cropNameUr = "گندم",
            category = "Grain",
            mandiName = "Badami Bagh Grain Market",
            city = "Lahore",
            province = "Punjab",
            pricePerKg = 97.5,
            pricePerMann = 3900.0,
            minPricePerKg = 95.0,
            maxPricePerKg = 100.0,
            trend = MandiTrend.UP,
            changePercent = 2.4,
            lastUpdated = "Today, 08:30 AM",
            drawableResName = "crop_wheat",
            isFavorite = true
        ),
        MandiRate(
            id = "wheat_faisalabad",
            cropNameEn = "Wheat (Gandum)",
            cropNameUr = "گندم",
            category = "Grain",
            mandiName = "Ghalla Mandi",
            city = "Faisalabad",
            province = "Punjab",
            pricePerKg = 98.0,
            pricePerMann = 3920.0,
            minPricePerKg = 96.0,
            maxPricePerKg = 101.0,
            trend = MandiTrend.UP,
            changePercent = 1.8,
            lastUpdated = "Today, 09:00 AM",
            drawableResName = "crop_wheat",
            isFavorite = false
        ),
        MandiRate(
            id = "rice_basmati_gujranwala",
            cropNameEn = "Super Basmati Rice",
            cropNameUr = "سپر باسمتی چاول",
            category = "Grain",
            mandiName = "Kamoke Rice Market",
            city = "Gujranwala",
            province = "Punjab",
            pricePerKg = 285.0,
            pricePerMann = 11400.0,
            minPricePerKg = 275.0,
            maxPricePerKg = 295.0,
            trend = MandiTrend.UP,
            changePercent = 3.2,
            lastUpdated = "Today, 09:15 AM",
            drawableResName = "crop_rice",
            isFavorite = true
        ),
        MandiRate(
            id = "cotton_multan",
            cropNameEn = "Cotton / Phutti",
            cropNameUr = "کپاس / پھٹی",
            category = "Cash Crop",
            mandiName = "Multan Cotton Exchange",
            city = "Multan",
            province = "Punjab",
            pricePerKg = 215.0,
            pricePerMann = 8600.0,
            minPricePerKg = 205.0,
            maxPricePerKg = 225.0,
            trend = MandiTrend.DOWN,
            changePercent = -1.5,
            lastUpdated = "Today, 10:00 AM",
            drawableResName = "crop_cotton",
            isFavorite = true
        ),
        MandiRate(
            id = "sugarcane_ryk",
            cropNameEn = "Sugarcane (Ganna)",
            cropNameUr = "کماد / گنا",
            category = "Cash Crop",
            mandiName = "Rahim Yar Khan Mandi",
            city = "Rahim Yar Khan",
            province = "Punjab",
            pricePerKg = 11.25,
            pricePerMann = 450.0,
            minPricePerKg = 10.5,
            maxPricePerKg = 12.0,
            trend = MandiTrend.STABLE,
            changePercent = 0.0,
            lastUpdated = "Today, 08:45 AM",
            drawableResName = "crop_sugarcane",
            isFavorite = false
        ),
        MandiRate(
            id = "maize_sahiwal",
            cropNameEn = "Maize / Corn (Makai)",
            cropNameUr = "مکئی",
            category = "Grain",
            mandiName = "Sahiwal Grain Mandi",
            city = "Sahiwal",
            province = "Punjab",
            pricePerKg = 62.5,
            pricePerMann = 2500.0,
            minPricePerKg = 60.0,
            maxPricePerKg = 65.0,
            trend = MandiTrend.UP,
            changePercent = 4.1,
            lastUpdated = "Today, 09:30 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "tomato_lahore",
            cropNameEn = "Tomato (Tamatar)",
            cropNameUr = "ٹماٹر",
            category = "Vegetable",
            mandiName = "Kot Lakhpat Sabzi Mandi",
            city = "Lahore",
            province = "Punjab",
            pricePerKg = 75.0,
            pricePerMann = 3000.0,
            minPricePerKg = 70.0,
            maxPricePerKg = 85.0,
            trend = MandiTrend.DOWN,
            changePercent = -5.2,
            lastUpdated = "Today, 07:45 AM",
            drawableResName = "crop_tomato",
            isFavorite = true
        ),
        MandiRate(
            id = "onion_hyderabad",
            cropNameEn = "Onion (Piyaz)",
            cropNameUr = "پیاز",
            category = "Vegetable",
            mandiName = "Hyderabad Sabzi Mandi",
            city = "Hyderabad",
            province = "Sindh",
            pricePerKg = 92.0,
            pricePerMann = 3680.0,
            minPricePerKg = 85.0,
            maxPricePerKg = 98.0,
            trend = MandiTrend.UP,
            changePercent = 6.8,
            lastUpdated = "Today, 08:00 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "potato_okara",
            cropNameEn = "Potato (Aloo)",
            cropNameUr = "آلو",
            category = "Vegetable",
            mandiName = "Okara Potato Hub",
            city = "Okara",
            province = "Punjab",
            pricePerKg = 48.0,
            pricePerMann = 1920.0,
            minPricePerKg = 44.0,
            maxPricePerKg = 52.0,
            trend = MandiTrend.STABLE,
            changePercent = 0.5,
            lastUpdated = "Today, 08:15 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "chilies_kunri",
            cropNameEn = "Red Chilies (Surkh Mirch)",
            cropNameUr = "سرخ مرچ (کنری)",
            category = "Cash Crop",
            mandiName = "Kunri Red Chili Mandi",
            city = "Kunri",
            province = "Sindh",
            pricePerKg = 480.0,
            pricePerMann = 19200.0,
            minPricePerKg = 450.0,
            maxPricePerKg = 510.0,
            trend = MandiTrend.UP,
            changePercent = 5.0,
            lastUpdated = "Today, 09:40 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "mango_multan",
            cropNameEn = "Mango Chaunsa / Sindhri",
            cropNameUr = "آم چونسا / سندھڑی",
            category = "Fruit",
            mandiName = "Multan Fruit Market",
            city = "Multan",
            province = "Punjab",
            pricePerKg = 165.0,
            pricePerMann = 6600.0,
            minPricePerKg = 150.0,
            maxPricePerKg = 180.0,
            trend = MandiTrend.DOWN,
            changePercent = -2.0,
            lastUpdated = "Today, 10:15 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "citrus_sargodha",
            cropNameEn = "Kinnow / Citrus",
            cropNameUr = "کنو / مالٹا",
            category = "Fruit",
            mandiName = "Bhalwal Citrus Market",
            city = "Sargodha",
            province = "Punjab",
            pricePerKg = 85.0,
            pricePerMann = 3400.0,
            minPricePerKg = 75.0,
            maxPricePerKg = 95.0,
            trend = MandiTrend.UP,
            changePercent = 3.5,
            lastUpdated = "Today, 08:30 AM",
            drawableResName = null,
            isFavorite = false
        ),
        MandiRate(
            id = "mustard_bahawalpur",
            cropNameEn = "Mustard / Raya (Sarson)",
            cropNameUr = "سرسوں / رایہ",
            category = "Oilseed",
            mandiName = "Bahawalpur Mandi",
            city = "Bahawalpur",
            province = "Punjab",
            pricePerKg = 145.0,
            pricePerMann = 5800.0,
            minPricePerKg = 140.0,
            maxPricePerKg = 152.0,
            trend = MandiTrend.UP,
            changePercent = 1.2,
            lastUpdated = "Today, 09:10 AM",
            drawableResName = null,
            isFavorite = false
        )
    )

    private val _rates = MutableStateFlow(initialRates)
    val rates: StateFlow<List<MandiRate>> = _rates.asStateFlow()
    private val _isLive = MutableStateFlow(false)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    suspend fun refreshRates(): Boolean = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(ApiConfig.endpoint("/api/mandi/rates")).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode == 200) {
                val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val array = json.optJSONArray("rates") ?: JSONArray()
                val remote = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        add(
                            MandiRate(
                                id = item.optString("id"),
                                cropNameEn = item.optString("crop_name_en"),
                                cropNameUr = item.optString("crop_name_ur"),
                                category = item.optString("category"),
                                mandiName = item.optString("mandi_name"),
                                city = item.optString("city"),
                                province = item.optString("province"),
                                pricePerKg = item.optDouble("price_per_kg"),
                                pricePerMann = item.optDouble("price_per_mann"),
                                minPricePerKg = item.optDouble("min_price_per_kg"),
                                maxPricePerKg = item.optDouble("max_price_per_kg"),
                                trend = runCatching { MandiTrend.valueOf(item.optString("trend", "STABLE")) }.getOrDefault(MandiTrend.STABLE),
                                changePercent = item.optDouble("change_percent"),
                                lastUpdated = item.optString("last_updated", "Recently"),
                                drawableResName = null,
                                isFavorite = _rates.value.firstOrNull { it.id == item.optString("id") }?.isFavorite ?: false
                            )
                        )
                    }
                }
                if (remote.isNotEmpty()) {
                    _rates.value = remote
                    return@withContext true
                }
            }
            connection.disconnect()
        } catch (_: Exception) {
            // Do not present synthetic/stale values as live rates.
        }
        false
    }

    fun toggleFavorite(id: String) {
        _rates.update { list ->
            list.map { if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it }
        }
    }
}
