package com.example.data.model

data class PlantDiseaseGuide(
    val id: String,
    val diseaseNameEn: String,
    val diseaseNameUr: String,
    val scientificName: String,
    val affectedCrops: List<String>,
    val pathogenType: String, // Fungus, Virus, Bacteria, Insect Pest
    val severityLevel: String, // Critical, High, Moderate, Low
    val favorableWeatherEn: String,
    val favorableWeatherUr: String,
    val symptomsSummaryEn: String,
    val symptomsSummaryUr: String,
    val detailedSymptomsEn: List<String>,
    val detailedSymptomsUr: List<String>,
    val chemicalTreatments: List<ChemicalTreatmentItem>,
    val organicRemediesEn: List<String>,
    val organicRemediesUr: List<String>,
    val preventiveMeasuresEn: List<String>,
    val preventiveMeasuresUr: List<String>,
    val audioExplanationEn: String,
    val audioExplanationUr: String
)

data class ChemicalTreatmentItem(
    val chemicalName: String,
    val tradeBrandPakistan: String,
    val manufacturer: String,
    val dosagePerAcreOr100L: String,
    val applicationMethodEn: String,
    val applicationMethodUr: String,
    val safetyWaitingPeriodDays: Int
)
