package com.example.data.model

data class CropGuide(
    val id: String,
    val nameEn: String,
    val nameUr: String,
    val scientificName: String,
    val category: String, // Grain, Cash Crop, Vegetable, Fruit
    val sowingSeasonEn: String,
    val sowingSeasonUr: String,
    val harvestingSeasonEn: String,
    val harvestingSeasonUr: String,
    val optimalTemperature: String,
    val waterRequirement: String, // High, Medium, Low
    val soilTypeEn: String,
    val soilTypeUr: String,
    val seedRatePerAcre: String,
    val expectedYieldPerAcre: String,
    val estimatedCostPerAcre: Double,
    val estimatedRevenuePerAcre: Double,
    val recommendedVarieties: List<String>,
    val fertilizerSchedule: List<FertilizerStage>,
    val growthStages: List<GrowthStageGuide>,
    val commonPestsAndDiseases: List<String>,
    val expertTipsEn: List<String>,
    val expertTipsUr: List<String>
)

data class FertilizerStage(
    val stageNameEn: String,
    val stageNameUr: String,
    val timingEn: String,
    val timingUr: String,
    val recommendationEn: String,
    val recommendationUr: String,
    val dapBags: Double = 0.0,
    val ureaBags: Double = 0.0,
    val potashBags: Double = 0.0,
    val zincKg: Double = 0.0
)

data class GrowthStageGuide(
    val stageNumber: Int,
    val titleEn: String,
    val titleUr: String,
    val daysAfterSowing: String,
    val descriptionEn: String,
    val descriptionUr: String,
    val keyActionsEn: List<String>,
    val keyActionsUr: List<String>
)
