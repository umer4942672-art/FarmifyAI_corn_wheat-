package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EntryType {
    INCOME,
    EXPENSE,
    FIELD_WORK
}

@Entity(tableName = "khata_entries")
data class KhataEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "", // Associated farmer identifier (phone/email)
    val entryType: String, // "INCOME", "EXPENSE", "FIELD_WORK"
    val date: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cropName: String,
    val fieldName: String = "",
    val fieldSizeAcres: Double = 0.0,
    val activityType: String = "", // Sowing, Spraying, Fertilizing, Irrigation, Harvesting, Labor, Machinery, Transportation, Other
    val description: String = "",
    
    // Expense breakdown
    val laborCost: Double = 0.0,
    val seedCost: Double = 0.0,
    val fertilizerCost: Double = 0.0,
    val pesticideCost: Double = 0.0,
    val irrigationCost: Double = 0.0,
    val machineryCost: Double = 0.0,
    val transportationCost: Double = 0.0,
    val otherExpenses: Double = 0.0,
    
    // Income breakdown
    val quantity: Double = 0.0,
    val unit: String = "Mann", // "KG", "Mann", "Ton", "Bori"
    val sellingPricePerUnit: Double = 0.0,
    val totalAmount: Double = 0.0,
    val buyerOrMandi: String = "",
    
    val isSynced: Boolean = false
) {
    val effectiveExpense: Double
        get() {
            if (entryType == "INCOME") return 0.0
            val sum = laborCost + seedCost + fertilizerCost + pesticideCost + irrigationCost + machineryCost + transportationCost + otherExpenses
            return if (sum > 0) sum else totalAmount
        }

    val effectiveIncome: Double
        get() {
            if (entryType != "INCOME") return 0.0
            return if (totalAmount > 0) totalAmount else (quantity * sellingPricePerUnit)
        }
}
