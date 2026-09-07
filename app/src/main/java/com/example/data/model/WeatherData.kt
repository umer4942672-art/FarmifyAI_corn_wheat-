package com.example.data.model

data class FarmDistrict(
    val id: String,
    val nameEn: String,
    val nameUr: String,
    val province: String,
    val latitude: Double,
    val longitude: Double
)

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val conditionEn: String,
    val conditionUr: String,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val windDirection: String,
    val rainProbability: Int,
    val uvIndex: Int,
    val sunrise: String,
    val sunset: String,
    val locationName: String,
    val locationNameUr: String,
    val lastUpdated: String
)

data class DailyForecast(
    val dayNameEn: String,
    val dayNameUr: String,
    val dateFormatted: String,
    val maxTempC: Double,
    val minTempC: Double,
    val conditionEn: String,
    val conditionUr: String,
    val rainProbability: Int,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val iconType: String // "sunny", "partly_cloudy", "rainy", "thunderstorm", "windy"
)

data class FarmAdvisory(
    val titleEn: String,
    val titleUr: String,
    val adviceEn: String,
    val adviceUr: String,
    val category: AdvisoryCategory,
    val severity: AdvisorySeverity
)

enum class AdvisoryCategory {
    IRRIGATION,
    SPRAYING,
    HARVESTING,
    HEAT_COLD,
    FERTILIZATION
}

enum class AdvisorySeverity {
    INFO,
    SUCCESS,
    WARNING,
    ALERT
}

data class WeatherDashboardState(
    val selectedDistrict: FarmDistrict,
    val current: CurrentWeather,
    val forecast7Days: List<DailyForecast>,
    val advisories: List<FarmAdvisory>,
    val isLoading: Boolean = false,
    val isOnline: Boolean = true
)
