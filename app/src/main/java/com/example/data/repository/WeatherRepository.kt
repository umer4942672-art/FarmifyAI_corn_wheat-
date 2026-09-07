package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository {

    val districts = listOf(
        FarmDistrict("faisalabad", "Faisalabad", "فیصل آباد", "Punjab", 31.4504, 73.1350),
        FarmDistrict("multan", "Multan", "ملتان", "Punjab", 30.1575, 71.5249),
        FarmDistrict("lahore", "Lahore", "لاہور", "Punjab", 31.5204, 74.3587),
        FarmDistrict("sahiwal", "Sahiwal", "ساہیوال", "Punjab", 30.6682, 73.1114),
        FarmDistrict("bahawalpur", "Bahawalpur", "بہاولپور", "Punjab", 29.3956, 71.6836),
        FarmDistrict("sargodha", "Sargodha", "سرگودھا", "Punjab", 32.0836, 72.6711),
        FarmDistrict("ryk", "Rahim Yar Khan", "رحیم یار خان", "Punjab", 28.4212, 70.2989),
        FarmDistrict("gujranwala", "Gujranwala", "گوجرانوالہ", "Punjab", 32.1877, 74.1945),
        FarmDistrict("hyderabad", "Hyderabad", "حیدرآباد", "Sindh", 25.3960, 68.3578),
        FarmDistrict("sukkur", "Sukkur", "سکھر", "Sindh", 27.7052, 68.8574),
        FarmDistrict("peshawar", "Peshawar", "پشاور", "KPK", 34.0151, 71.5249),
        FarmDistrict("quetta", "Quetta", "کوئٹہ", "Balochistan", 30.1798, 66.9750)
    )

    private val _weatherState = MutableStateFlow(
        WeatherDashboardState(
            selectedDistrict = districts[0],
            current = getDefaultCurrent(districts[0]),
            forecast7Days = getDefaultForecast(),
            advisories = generateAdvisories(34.0, 48, 11.5, 10),
            isLoading = false,
            isOnline = false
        )
    )
    val weatherState: StateFlow<WeatherDashboardState> = _weatherState.asStateFlow()

    suspend fun selectDistrict(district: FarmDistrict) {
        _weatherState.update { it.copy(selectedDistrict = district, isLoading = true) }
        fetchWeather(district)
    }

    suspend fun refreshWeather() {
        val currentDistrict = _weatherState.value.selectedDistrict
        fetchWeather(currentDistrict)
    }

    /**
     * Uses the phone's current location for weather instead of a fixed district.
     * Falls back to the previously selected district when location is unavailable.
     */
    suspend fun refreshWeatherForCurrentLocation(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            refreshWeather()
            return@withContext false
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext false

        val candidates = mutableListOf<Location>()
        try {
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let(candidates::add)
        } catch (_: SecurityException) {
        }
        try {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)?.let(candidates::add)
        } catch (_: SecurityException) {
        }

        val best = candidates.maxByOrNull { it.time }
        if (best == null) {
            refreshWeather()
            return@withContext false
        }

        val district = reverseGeocode(context, best.latitude, best.longitude)
        _weatherState.update { it.copy(selectedDistrict = district, isLoading = true) }
        fetchWeather(district)
        true
    }

    private fun reverseGeocode(context: Context, latitude: Double, longitude: Double): FarmDistrict {
        var city = "Current Location"
        var province = "Pakistan"
        var cityUr = "موجودہ مقام"

        try {
            if (Geocoder.isPresent()) {
                val addresses = Geocoder(context, Locale.ENGLISH).getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                city = address?.locality
                    ?: address?.subAdminArea
                    ?: address?.adminArea
                    ?: city
                province = address?.adminArea ?: province
                cityUr = city
            }
        } catch (_: Exception) {
            // Coordinates remain usable even when reverse geocoding is unavailable.
        }

        return FarmDistrict(
            id = "current_location",
            nameEn = city,
            nameUr = cityUr,
            province = province,
            latitude = latitude,
            longitude = longitude
        )
    }

    private suspend fun fetchWeather(district: FarmDistrict) = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=${district.latitude}&longitude=${district.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max,uv_index_max,sunrise,sunset&timezone=auto"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.requestMethod = "GET"

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                
                val currentObj = json.getJSONObject("current")
                val temp = currentObj.optDouble("temperature_2m", 32.0)
                val feelsLike = currentObj.optDouble("apparent_temperature", 34.0)
                val humidity = currentObj.optInt("relative_humidity_2m", 50)
                val windSpeed = currentObj.optDouble("wind_speed_10m", 12.0)
                val windDirDeg = currentObj.optInt("wind_direction_10m", 180)
                val weatherCode = currentObj.optInt("weather_code", 0)

                val (condEn, condUr) = mapWeatherCode(weatherCode)
                val windDirStr = mapWindDirection(windDirDeg)

                val dailyObj = json.getJSONObject("daily")
                val times = dailyObj.getJSONArray("time")
                val maxTemps = dailyObj.getJSONArray("temperature_2m_max")
                val minTemps = dailyObj.getJSONArray("temperature_2m_min")
                val dailyRainProbs = dailyObj.getJSONArray("precipitation_probability_max")
                val dailyCodes = dailyObj.getJSONArray("weather_code")
                val dailyWinds = dailyObj.getJSONArray("wind_speed_10m_max")
                val dailyUv = dailyObj.getJSONArray("uv_index_max")
                val dailySunrise = dailyObj.getJSONArray("sunrise")
                val dailySunset = dailyObj.getJSONArray("sunset")
                val rainProb = dailyRainProbs.optInt(0, 10)
                val uvIndex = dailyUv.optDouble(0, 7.0).toInt()
                val sunrise = dailySunrise.optString(0, "--")
                    .substringAfter('T', dailySunrise.optString(0, "--"))
                val sunset = dailySunset.optString(0, "--")
                    .substringAfter('T', dailySunset.optString(0, "--"))

                val forecastList = mutableListOf<DailyForecast>()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val dayFormatEn = SimpleDateFormat("EEEE", Locale.ENGLISH)
                val shortDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)

                for (i in 0 until minOf(7, times.length())) {
                    val dateStr = times.getString(i)
                    val date = try { sdf.parse(dateStr) ?: Date() } catch (e: Exception) { Date() }
                    val dayEn = dayFormatEn.format(date)
                    val dayUr = mapDayToUrdu(dayEn)
                    val formattedDate = shortDateFormat.format(date)
                    val maxT = maxTemps.optDouble(i, 35.0)
                    val minT = minTemps.optDouble(i, 24.0)
                    val rProb = dailyRainProbs.optInt(i, 10)
                    val code = dailyCodes.optInt(i, 0)
                    val (wEn, wUr) = mapWeatherCode(code)
                    val wSpeed = dailyWinds.optDouble(i, 14.0)
                    val iconType = mapWeatherCodeToIcon(code)

                    forecastList.add(
                        DailyForecast(
                            dayNameEn = if (i == 0) "Today" else dayEn,
                            dayNameUr = if (i == 0) "آج" else dayUr,
                            dateFormatted = formattedDate,
                            maxTempC = maxT,
                            minTempC = minT,
                            conditionEn = wEn,
                            conditionUr = wUr,
                            rainProbability = rProb,
                            humidityPercent = (humidity - i * 2).coerceIn(30, 85),
                            windSpeedKmh = wSpeed,
                            iconType = iconType
                        )
                    )
                }

                val advisories = generateAdvisories(temp, humidity, windSpeed, rainProb)

                val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val updatedTime = sdfTime.format(Date())

                _weatherState.update {
                    it.copy(
                        current = CurrentWeather(
                            temperatureC = temp,
                            feelsLikeC = feelsLike,
                            conditionEn = condEn,
                            conditionUr = condUr,
                            humidityPercent = humidity,
                            windSpeedKmh = windSpeed,
                            windDirection = windDirStr,
                            rainProbability = rainProb,
                            uvIndex = uvIndex,
                            sunrise = sunrise,
                            sunset = sunset,
                            locationName = "${district.nameEn}, ${district.province}",
                            locationNameUr = "${district.nameUr}، ${district.province}",
                            lastUpdated = updatedTime
                        ),
                        forecast7Days = forecastList,
                        advisories = advisories,
                        isLoading = false,
                        isOnline = true
                    )
                }
            } else {
                setFallback(district)
            }
        } catch (e: Exception) {
            setFallback(district)
        }
    }

    private fun setFallback(district: FarmDistrict) {
        val current = getDefaultCurrent(district)
        val forecast = getDefaultForecast()
        val advisories = generateAdvisories(current.temperatureC, current.humidityPercent, current.windSpeedKmh, current.rainProbability)
        _weatherState.update {
            it.copy(
                current = current,
                forecast7Days = forecast,
                advisories = advisories,
                isLoading = false,
                isOnline = false
            )
        }
    }

    private fun generateAdvisories(temp: Double, humidity: Int, windSpeed: Double, rainProb: Int): List<FarmAdvisory> {
        val list = mutableListOf<FarmAdvisory>()

        // 1. Irrigation
        if (rainProb >= 50) {
            list.add(
                FarmAdvisory(
                    titleEn = "Delay Irrigation",
                    titleUr = "آبپاشی موخر کریں",
                    adviceEn = "High probability of rain ($rainProb%). Save tube-well fuel & electricity by pausing irrigation for 48 hours.",
                    adviceUr = "بارش کا امکان $rainProb% ہے۔ ٹیوب ویل اور نہری پانی 48 گھنٹے کے لیے روک کر اخراجات بچائیں۔",
                    category = AdvisoryCategory.IRRIGATION,
                    severity = AdvisorySeverity.WARNING
                )
            )
        } else {
            list.add(
                FarmAdvisory(
                    titleEn = "Suitable for Irrigation",
                    titleUr = "آبپاشی کے لیے موزوں دن",
                    adviceEn = "Dry weather ahead. Ideal time to irrigate wheat, cotton, and vegetable plots during early morning or evening.",
                    adviceUr = "خشک موسم متوقع ہے۔ فصلوں کو صبح کے وقت یا شام کے وقت پانی لگانے کا بہترین وقت ہے۔",
                    category = AdvisoryCategory.IRRIGATION,
                    severity = AdvisorySeverity.SUCCESS
                )
            )
        }

        // 2. Pesticide Spraying
        if (windSpeed > 18.0) {
            list.add(
                FarmAdvisory(
                    titleEn = "Unsafe for Spraying (High Wind)",
                    titleUr = "سپرے کے لیے ناموزوں (تیز ہوا)",
                    adviceEn = "Wind speed is ${windSpeed.toInt()} km/h. Avoid pesticide spraying to prevent chemical drift and uneven application.",
                    adviceUr = "ہوا کی رفتار ${windSpeed.toInt()} کلومیٹر فی گھنٹہ ہے۔ کیڑے مار ادویات کا سپرے ضائع ہونے سے بچانے کے لیے روک دیں۔",
                    category = AdvisoryCategory.SPRAYING,
                    severity = AdvisorySeverity.ALERT
                )
            )
        } else {
            list.add(
                FarmAdvisory(
                    titleEn = "Optimal Spraying Window",
                    titleUr = "سپرے کا بہترین وقت",
                    adviceEn = "Calm winds (${windSpeed.toInt()} km/h). Best window for fungicide, pesticide, or foliar fertilizer spraying before 10 AM.",
                    adviceUr = "ہوا پرسکون ہے۔ کیڑے مار اور فنگس کش ادویات کے سپرے کے لیے صبح 10 بجے سے پہلے کا وقت نہایت موزوں ہے۔",
                    category = AdvisoryCategory.SPRAYING,
                    severity = AdvisorySeverity.SUCCESS
                )
            )
        }

        // 3. Heat or Field Work
        if (temp >= 38.0) {
            list.add(
                FarmAdvisory(
                    titleEn = "High Heat Alert",
                    titleUr = "شدید گرمی کا الرٹ",
                    adviceEn = "Max temperature reaching ${temp.toInt()}°C. Keep farm laborers hydrated and maintain light moisture in nursery/vegetable beds.",
                    adviceUr = "درجہ حرارت ${temp.toInt()} ڈگری سینٹی گریڈ تک پہنچنے کا امکان ہے۔ سبزیوں اور بیج والی کیاریوں میں نمی برقرار رکھیں۔",
                    category = AdvisoryCategory.HEAT_COLD,
                    severity = AdvisorySeverity.WARNING
                )
            )
        } else {
            list.add(
                FarmAdvisory(
                    titleEn = "Wheat / Crop Harvesting",
                    titleUr = "فصل کی کٹائی کا مشورہ",
                    adviceEn = "Clear sunny conditions provide excellent grain drying and harvester combine operations.",
                    adviceUr = "صاف دھوپ والی فضا فصل کی کٹائی اور گہائی (تھریشنگ) کے لیے انتہائی موزوں ہے۔",
                    category = AdvisoryCategory.HARVESTING,
                    severity = AdvisorySeverity.INFO
                )
            )
        }

        return list
    }

    private fun mapWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Clear Sky", "صاف آسمان")
            1, 2 -> Pair("Mainly Clear", "جزوی ابر آلود")
            3 -> Pair("Overcast", "ابر آلود")
            45, 48 -> Pair("Foggy", "دھند")
            51, 53, 55 -> Pair("Light Drizzle", "ہلکی بونداباندی")
            61, 63 -> Pair("Rain Showers", "بارش")
            65 -> Pair("Heavy Rain", "تیز بارش")
            80, 81, 82 -> Pair("Scattered Showers", "وقفے وقفے سے بارش")
            95, 96, 99 -> Pair("Thunderstorm", "گرج چمک کے ساتھ بارش")
            else -> Pair("Sunny & Warm", "دھوپ اور گرم")
        }
    }

    private fun mapWeatherCodeToIcon(code: Int): String {
        return when (code) {
            0 -> "sunny"
            1, 2, 3 -> "partly_cloudy"
            45, 48 -> "fog"
            51, 53, 55, 61, 63, 80, 81, 82 -> "rainy"
            65, 95, 96, 99 -> "thunderstorm"
            else -> "sunny"
        }
    }

    private fun mapWindDirection(deg: Int): String {
        return when (deg) {
            in 0..45, in 315..360 -> "North (شمال)"
            in 46..135 -> "East (مشرق)"
            in 136..225 -> "South (جنوب)"
            else -> "West (مغرب)"
        }
    }

    private fun mapDayToUrdu(day: String): String {
        return when (day.lowercase(Locale.ENGLISH)) {
            "monday" -> "پیر"
            "tuesday" -> "منگل"
            "wednesday" -> "بدھ"
            "thursday" -> "جمعرات"
            "friday" -> "جمعہ"
            "saturday" -> "ہفتہ"
            "sunday" -> "اتوار"
            else -> day
        }
    }

    private fun getDefaultCurrent(district: FarmDistrict) = CurrentWeather(
        temperatureC = 33.0,
        feelsLikeC = 36.0,
        conditionEn = "Sunny & Warm",
        conditionUr = "صاف دھوپ اور گرم",
        humidityPercent = 48,
        windSpeedKmh = 11.2,
        windDirection = "East (مشرق)",
        rainProbability = 10,
        uvIndex = 8,
        sunrise = "05:40 AM",
        sunset = "06:55 PM",
        locationName = "${district.nameEn}, ${district.province}",
        locationNameUr = "${district.nameUr}، ${district.province}",
        lastUpdated = "Today, 08:30 AM"
    )

    private fun getDefaultForecast() = listOf(
        DailyForecast("Today", "آج", "20 Aug", 34.0, 24.0, "Sunny", "صاف دھوپ", 10, 48, 11.0, "sunny"),
        DailyForecast("Thursday", "جمعرات", "21 Aug", 35.0, 25.0, "Partly Cloudy", "جزوی ابر آلود", 15, 52, 13.0, "partly_cloudy"),
        DailyForecast("Friday", "جمعہ", "22 Aug", 33.0, 23.0, "Chance of Rain", "بارش کا امکان", 45, 65, 16.0, "rainy"),
        DailyForecast("Saturday", "ہفتہ", "23 Aug", 31.0, 22.0, "Scattered Showers", "ہلکی بارش", 60, 72, 18.0, "thunderstorm"),
        DailyForecast("Sunday", "اتوار", "24 Aug", 33.0, 23.0, "Clear Sky", "صاف آسمان", 10, 50, 10.0, "sunny"),
        DailyForecast("Monday", "پیر", "25 Aug", 34.0, 24.0, "Sunny", "صاف دھوپ", 5, 45, 9.0, "sunny"),
        DailyForecast("Tuesday", "منگل", "26 Aug", 36.0, 26.0, "Hot & Sunny", "تیز دھوپ اور گرم", 0, 40, 12.0, "sunny")
    )
}
