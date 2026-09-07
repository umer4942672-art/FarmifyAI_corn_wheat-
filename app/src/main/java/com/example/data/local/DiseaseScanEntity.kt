package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "disease_scans")
data class DiseaseScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "",
    val cropName: String,
    val diseaseNameEn: String,
    val diseaseNameUr: String,
    val confidencePercent: Int,
    val isHealthy: Boolean,
    val severityLevel: String, // "Low", "Moderate", "High", "Critical"
    val symptoms: String,
    val symptomsUr: String,
    val chemicalTreatment: String,
    val chemicalTreatmentUr: String,
    val organicPrevention: String,
    val organicPreventionUr: String,
    val advisoryNote: String,
    val advisoryNoteUr: String,
    val imageUriOrPath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSample: Boolean = false
)
