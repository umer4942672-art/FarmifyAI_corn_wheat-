package com.example.data.model

data class MandiRate(
    val id: String,
    val cropNameEn: String,
    val cropNameUr: String,
    val category: String, // "Grain", "Cash Crop", "Vegetable", "Fruit", "Oilseed"
    val mandiName: String,
    val city: String,
    val province: String,
    val pricePerKg: Double,
    val pricePerMann: Double, // 40 kg rate
    val minPricePerKg: Double,
    val maxPricePerKg: Double,
    val trend: MandiTrend,
    val changePercent: Double,
    val lastUpdated: String,
    val drawableResName: String? = null,
    val isFavorite: Boolean = false
)

enum class MandiTrend {
    UP,
    DOWN,
    STABLE
}
