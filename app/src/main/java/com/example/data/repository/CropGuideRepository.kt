package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CropGuideRepository {

    private val _cropsList = MutableStateFlow<List<CropGuide>>(getPakistaniCropsData())
    val cropsList: Flow<List<CropGuide>> = _cropsList.asStateFlow()

    private val _diseaseGuides = MutableStateFlow<List<PlantDiseaseGuide>>(getPakistaniDiseaseData())
    val diseaseGuides: Flow<List<PlantDiseaseGuide>> = _diseaseGuides.asStateFlow()

    fun getCropById(id: String): CropGuide? {
        return _cropsList.value.find { it.id.equals(id, ignoreCase = true) }
    }

    fun getDiseaseById(id: String): PlantDiseaseGuide? {
        return _diseaseGuides.value.find { it.id.equals(id, ignoreCase = true) }
    }

    private fun getPakistaniCropsData(): List<CropGuide> {
        return listOf(
            CropGuide(
                id = "wheat",
                nameEn = "Wheat",
                nameUr = "گندم",
                scientificName = "Triticum aestivum",
                category = "Grain",
                sowingSeasonEn = "Nov 01 - Nov 30 (Ideal)",
                sowingSeasonUr = "1 تا 30 نومبر (بہترین وقت)",
                harvestingSeasonEn = "April 15 - May 10",
                harvestingSeasonUr = "15 اپریل تا 10 مئی",
                optimalTemperature = "15°C - 25°C",
                waterRequirement = "Medium (4 - 5 irrigations)",
                soilTypeEn = "Well-drained Loam to Clay Loam",
                soilTypeUr = "زرخیز مٹی اور چکنی میرا زمین",
                seedRatePerAcre = "50 - 55 kg / acre",
                expectedYieldPerAcre = "45 - 60 Mann / acre",
                estimatedCostPerAcre = 65000.0,
                estimatedRevenuePerAcre = 180000.0,
                recommendedVarieties = listOf(
                    "Akbar-19 (اکبر-19)",
                    "Dilkash-20 (دلکش-20)",
                    "Ghazi-19 (غازی-19)",
                    "Subhani-21 (سبحانی-21)",
                    "Fakhar-e-Bhakkar (فخر بھکر)",
                    "Urooj-22 (عروج-22)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "At Sowing (Rauni)",
                        stageNameUr = "بجائی کے وقت",
                        timingEn = "During land preparation",
                        timingUr = "زمین کی آخری تیاری میں",
                        recommendationEn = "Apply 1.5 - 2 bags DAP and 1 bag SOP (Sulphate of Potash).",
                        recommendationUr = "ڈیڑھ سے دو بوری ڈی اے پی اور ایک بوری پوٹاش ڈالیں۔",
                        dapBags = 2.0,
                        potashBags = 1.0
                    ),
                    FertilizerStage(
                        stageNameEn = "1st Irrigation (Crown Root / کور کا پانی)",
                        stageNameUr = "پہلے پانی پر (20 تا 25 دن)",
                        timingEn = "20-25 days after sowing",
                        timingUr = "بجائی کے 20 سے 25 دن بعد",
                        recommendationEn = "Apply 1 bag Urea + 5 kg Zinc Sulphate (33%).",
                        recommendationUr = "ایک بوری یوریا اور 5 کلو زنک سلفیٹ ڈالیں۔",
                        ureaBags = 1.0,
                        zincKg = 5.0
                    ),
                    FertilizerStage(
                        stageNameEn = "2nd Irrigation (Tillering / شگوفے)",
                        stageNameUr = "دوسرے پانی پر (شگوفے بنتے وقت)",
                        timingEn = "40-45 days after sowing",
                        timingUr = "بجائی کے 40 سے 45 دن بعد",
                        recommendationEn = "Apply 1 bag Urea with irrigation water.",
                        recommendationUr = "ایک بوری یوریا پانی کے ساتھ فلڈ کریں۔",
                        ureaBags = 1.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Booting / Flag Leaf (گوبھ کی حالت)",
                        stageNameUr = "گوبھ کی حالت پر",
                        timingEn = "75-80 days after sowing",
                        timingUr = "بجائی کے 75 سے 80 دن بعد",
                        recommendationEn = "Foliar spray of Boron + Potash (0.5%) for solid grain filling.",
                        recommendationUr = "بوران اور پوٹاش کا سپرے کریں تاکہ دانہ موٹا اور وزنی بنے۔"
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Germination & Crown Root (CRI)",
                        titleUr = "اگاؤ اور کور جڑیں",
                        daysAfterSowing = "0 - 25 Days",
                        descriptionEn = "Seed emerges and crown root initiates. Most critical for early root development.",
                        descriptionUr = "بیج اگتا ہے اور بنیادی جڑیں بنتی ہیں۔ یہ پہلا نازک مرحلہ ہے۔",
                        keyActionsEn = listOf("Maintain light moisture", "Apply 1st irrigation at 21 days", "Apply 1 bag Urea + Zinc"),
                        keyActionsUr = listOf("ہلکی نمی برقرار رکھیں", "21 دن پر پہلا پانی لگائیں", "یوریا اور زنک ڈالیں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Tillering & Jointing",
                        titleUr = "شگوفے نکلنا اور نالی بننا",
                        daysAfterSowing = "25 - 65 Days",
                        descriptionEn = "Maximum tillers are formed. Weed competition must be suppressed.",
                        descriptionUr = "پودا شگوفے نکالتا ہے۔ جڑی بوٹیوں کا تدارک اس مرحلے پر انتہائی اہم ہے۔",
                        keyActionsEn = listOf("Spray broadleaf & narrowleaf weedicides", "Apply 2nd irrigation with Nitrogen"),
                        keyActionsUr = listOf("چوڑے اور نوکیلے پتوں والی جڑی بوٹی مار سپرے کریں", "دوسرا پانی لگائیں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 3,
                        titleEn = "Booting & Heading (Ear emergence)",
                        titleUr = "گوبھ اور سٹہ نکلنا",
                        daysAfterSowing = "65 - 95 Days",
                        descriptionEn = "Wheat ears emerge from the flag leaf sheath.",
                        descriptionUr = "سٹے نکلتے ہیں اور پولینیشن کا عمل شروع ہوتا ہے۔",
                        keyActionsEn = listOf("Scout for yellow rust stripes", "Apply 3rd irrigation carefully (avoid lodging)"),
                        keyActionsUr = listOf("زرد کنگی کا معائنہ کریں", "تیز ہوا میں پانی نہ لگائیں تاکہ فصل گرے نہ")
                    ),
                    GrowthStageGuide(
                        stageNumber = 4,
                        titleEn = "Grain Filling (Milk & Dough stage)",
                        titleUr = "دودھیا حالت اور دانہ پکنا",
                        daysAfterSowing = "95 - 125 Days",
                        descriptionEn = "Grains accumulate starch. Stop watering 15 days before harvest.",
                        descriptionUr = "دانے میں نشاستہ بھرتا ہے اور دانہ سخت ہوتا ہے۔",
                        keyActionsEn = listOf("Protect against aphids with spray if threshold exceeds", "Cut water before harvest"),
                        keyActionsUr = listOf("سست تیلے کا معائنہ کریں", "کٹائی سے 15 دن پہلے پانی بند کر دیں")
                    )
                ),
                commonPestsAndDiseases = listOf("Yellow Rust (زرد کنگی)", "Brown Rust", "Wheat Aphid (تیلا)", "Loose Smut (کانگیاری)"),
                expertTipsEn = listOf(
                    "Sowing after November 25 decreases yield by 1% per day of delay.",
                    "Use certified treated seeds with fungicide to prevent bunt & smut.",
                    "Avoid applying water during high wind speeds to prevent crop lodging."
                ),
                expertTipsUr = listOf(
                    "25 نومبر کے بعد ہر دن تاخیر سے پیداوار میں روزانہ 1 فیصد کمی ہوتی ہے۔",
                    "بیج کو زہر لگا کر کاشت کریں تاکہ پھپھوندی اور کانگیاری سے بچاؤ ممکن ہو۔",
                    "تیز ہوا کے دوران پانی لگانے سے پرہیز کریں تاکہ فصل زمین پر نہ گرے۔"
                )
            ),
            CropGuide(
                id = "cotton",
                nameEn = "Cotton (Silver Fiber)",
                nameUr = "کپاس (چاندی کا ریشہ)",
                scientificName = "Gossypium hirsutum",
                category = "Cash Crop",
                sowingSeasonEn = "April 01 - May 31",
                sowingSeasonUr = "یکم اپریل تا 31 مئی",
                harvestingSeasonEn = "September - December",
                harvestingSeasonUr = "ستمبر تا دسمبر",
                optimalTemperature = "25°C - 42°C",
                waterRequirement = "High (6 - 8 irrigations / Bed planting)",
                soilTypeEn = "Deep Sandy Loam to Clay Loam",
                soilTypeUr = "گہری میرا زمین جس میں پانی کا نکاس اچھا ہو",
                seedRatePerAcre = "6 - 8 kg Delinted Seed",
                expectedYieldPerAcre = "30 - 45 Mann / acre",
                estimatedCostPerAcre = 90000.0,
                estimatedRevenuePerAcre = 260000.0,
                recommendedVarieties = listOf(
                    "CKC-3 (سی کے سی-3)",
                    "FH-333 (ایف ایچ-333)",
                    "BS-15 (بی ایس-15)",
                    "IUB-13 (آئی یو بی-13)",
                    "MNH-886 (ایم این ایچ-886)",
                    "CIM-602 (سی آئی ایم-602)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "Basal (Sowing)",
                        stageNameUr = "بجائی کے وقت",
                        timingEn = "During ridge formation",
                        timingUr = "کھیلیاں بناتے وقت",
                        recommendationEn = "1.5 bags DAP + 1 bag SOP + 5kg Sulfur per acre.",
                        recommendationUr = "ڈیڑھ بوری ڈی اے پی + ایک بوری ایس او پی + 5 کلو سلفر فی ایکڑ۔",
                        dapBags = 1.5,
                        potashBags = 1.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Squaring / Flowering",
                        stageNameUr = "ڈوڈی اور پھول آنے پر",
                        timingEn = "45 - 60 days after sowing",
                        timingUr = "بجائی کے 45 سے 60 دن بعد",
                        recommendationEn = "Split application: 0.75 bag Urea with each irrigation (total 2.5-3 bags Urea).",
                        recommendationUr = "ہر پانی پر پونی بوری یوریا ڈالیں (کل ڈھائی سے تین بوری یوریا)۔",
                        ureaBags = 2.5
                    ),
                    FertilizerStage(
                        stageNameEn = "Boll Development & Peak",
                        stageNameUr = "ٹینڈے بننے کے دوران",
                        timingEn = "75 - 110 days after sowing",
                        timingUr = "بجائی کے 75 سے 110 دن بعد",
                        recommendationEn = "Foliar spray of Potassium Nitrate (13-0-45) + Zinc + Boron every 12 days.",
                        recommendationUr = "پوٹاشیم نائٹریٹ، زنک اور بوران کا سپرے کریں تاکہ ٹینڈے زیادہ اور وزنی بنیں۔"
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Emergence & Thinning",
                        titleUr = "اگاؤ اور چھدرائی",
                        daysAfterSowing = "0 - 25 Days",
                        descriptionEn = "Keep plant distance 9-12 inches on ridges. Thinning at 20 days is compulsory.",
                        descriptionUr = "پودے سے پودے کا فاصلہ 9 سے 12 انچ رکھیں۔ چھدرائی 20 سے 25 دن کے اندر مکمل کریں۔",
                        keyActionsEn = listOf("First irrigation after 3-4 days of planting", "Complete thinning", "Scout for thrips & jassid"),
                        keyActionsUr = listOf("کاشت کے 3 سے 4 دن بعد ہلکا پانی دیں", "چھدرائی مکمل کریں", "تھرپس اور چست تیلے کا معائنہ کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Vegetative & Squaring (Buddin)",
                        titleUr = "شاخیں اور ڈوڈیاں بننا",
                        daysAfterSowing = "25 - 60 Days",
                        descriptionEn = "Sympodial fruiting branches initiate. Critical pest window.",
                        descriptionUr = "پھل والی شاخیں اور ڈوڈیاں نکلتی ہیں۔",
                        keyActionsEn = listOf("Monitor Whitefly ETL (5 nymphs/leaf)", "Apply Bio-pesticides or selective chemistry"),
                        keyActionsUr = listOf("سفید مکھی کا باقاعدہ معائنہ کریں", "محفوظ زرعی ادویات کا سپرے کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 3,
                        titleEn = "Boll Formation & Picking",
                        titleUr = "ٹینڈے بننا اور چنائی",
                        daysAfterSowing = "60 - 150 Days",
                        descriptionEn = "Bolls mature and open fluffy white cotton. Pick when dew dries (after 10 AM).",
                        descriptionUr = "ٹینڈے کھلتے ہیں۔ چنائی شبنم خشک ہونے کے بعد (صبح 10 بجے) شروع کریں۔",
                        keyActionsEn = listOf("Install pink bollworm pheromone traps", "Store raw cotton in dry airy space"),
                        keyActionsUr = listOf("گلابی سنڈی کے لیے جنسی پھندے لگائیں", "کپاس کو خشک اور ہوادار جگہ پر رکھیں")
                    )
                ),
                commonPestsAndDiseases = listOf("Whitefly (سفید مکھی)", "Pink Bollworm (گلابی سنڈی)", "Cotton Leaf Curl Virus (سی ایل سی یو وی)", "Jassid (سبز تیلا)"),
                expertTipsEn = listOf(
                    "Bed/Ridge planting saves 30% water and prevents root suffocation.",
                    "Spray under leaf surfaces in early morning or late afternoon.",
                    "Alternate insecticide groups to avoid pest resistance."
                ),
                expertTipsUr = listOf(
                    "کھیلیوں پر کاشت سے 30 فیصد پانی کی بچت ہوتی ہے اور پودا تندرست رہتا ہے۔",
                    "سپرے ہمیشہ صبح یا شام کے وقت پتوں کے نیچے کی طرف کریں۔",
                    "کیڑے مار ادویات کا گروپ بدل بدل کر سپرے کریں تاکہ کیڑوں میں قوت مدافعت نہ بنے۔"
                )
            ),
            CropGuide(
                id = "rice",
                nameEn = "Rice / Paddy (Basmati)",
                nameUr = "چاول / دھان (باسمتی)",
                scientificName = "Oryza sativa",
                category = "Grain",
                sowingSeasonEn = "June 15 - July 15 (Nursery Transplantation)",
                sowingSeasonUr = "15 جون تا 15 جولائی (پنیری منتقلی)",
                harvestingSeasonEn = "October 15 - November 20",
                harvestingSeasonUr = "15 اکتوبر تا 20 نومبر",
                optimalTemperature = "22°C - 35°C",
                waterRequirement = "Very High (Standing water initially)",
                soilTypeEn = "Heavy Clay with Impermeable Hardpan",
                soilTypeUr = "چکنی اور بھاری زمین جس میں پانی کھڑا رہ سکے",
                seedRatePerAcre = "5 - 6 kg (Basmati) / acre",
                expectedYieldPerAcre = "40 - 55 Mann / acre",
                estimatedCostPerAcre = 75000.0,
                estimatedRevenuePerAcre = 220000.0,
                recommendedVarieties = listOf(
                    "Super Basmati (سپر باسمتی)",
                    "Kainat 1121 (کائنات 1121)",
                    "Chenab Basmati (چناب باسمتی)",
                    "Punjab Basmati (پنجاب باسمتی)",
                    "Kissan Basmati (کسان باسمتی)",
                    "Super Gold (سپر گولڈ)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "At Puddling (Kaddu)",
                        stageNameUr = "کدو کرتے وقت",
                        timingEn = "Final puddling before transplantation",
                        timingUr = "پنیری لگانے سے پہلے کدو میں",
                        recommendationEn = "1.5 bags DAP + 1 bag SOP (Potash) incorporated into mud.",
                        recommendationUr = "ڈیڑھ بوری ڈی اے پی اور ایک بوری پوٹاش کدو کے اندر ملائیں۔",
                        dapBags = 1.5,
                        potashBags = 1.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Tillering Stage (15-20 Days)",
                        stageNameUr = "شگوفے نکلتے وقت (15 تا 20 دن)",
                        timingEn = "15-20 days after transplantation",
                        timingUr = "منتقلی کے 15 سے 20 دن بعد",
                        recommendationEn = "1 bag Urea + 5 kg Zinc Sulphate (33% Powder).",
                        recommendationUr = "ایک بوری یوریا اور 5 کلو زنک سلفیٹ (33 فیصد) ڈالیں۔",
                        ureaBags = 1.0,
                        zincKg = 5.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Panicle Initiation (35-40 Days)",
                        stageNameUr = "سٹہ بننے کے آغاز پر (35 تا 40 دن)",
                        timingEn = "35-40 days after transplantation",
                        timingUr = "منتقلی کے 35 سے 40 دن بعد",
                        recommendationEn = "1 bag Urea or Calcium Ammonium Nitrate.",
                        recommendationUr = "ایک بوری یوریا یا گوارا کھاد ڈالیں۔",
                        ureaBags = 1.0
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Transplantation & Stand Establishment",
                        titleUr = "پنیری کی منتقلی اور جڑ پکڑنا",
                        daysAfterSowing = "0 - 15 Days",
                        descriptionEn = "Transplant 25-30 days old nursery seedlings. Keep 2 seedlings per hill at 9x9 inch spacing.",
                        descriptionUr = "25 سے 30 دن کی پنیری منتقل کریں۔ ہر جگہ 2 پودے 9 انچ کے فاصلے پر لگائیں۔",
                        keyActionsEn = listOf("Maintain 2 inches standing water for 7 days", "Apply pre-emergence weedicide"),
                        keyActionsUr = listOf("پہلے 7 دن 2 انچ پانی کھڑا رکھیں", "جڑی بوٹی مار زہر استعمال کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Active Tillering & Stem Elongation",
                        titleUr = "شگوفے بننا اور قد بڑھنا",
                        daysAfterSowing = "15 - 45 Days",
                        descriptionEn = "Plant produces maximum productive tillers.",
                        descriptionUr = "پودا تیزی سے شگوفے بناتا ہے جس سے پیداوار طے ہوتی ہے۔",
                        keyActionsEn = listOf("Apply Cartap or Padan granules against stem borer", "Zinc application"),
                        keyActionsUr = listOf("تنے کی سنڈی کے لیے کارٹاپ دانے دار زہر ڈالیں", "زنک سلفیٹ کا استعمال کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 3,
                        titleEn = "Flowering & Grain Ripening",
                        titleUr = "پھول آنا اور دانہ پکنا",
                        daysAfterSowing = "45 - 90 Days",
                        descriptionEn = "Panicle emergence and ripening. Drain field 10 days prior to combine harvesting.",
                        descriptionUr = "سٹے نکلتے ہیں اور چاول کا دانہ بنتا ہے۔ کٹائی سے 10 دن پہلے پانی خشک کریں۔",
                        keyActionsEn = listOf("Spray for blast and leaf blight", "Drain field 10 days before harvest"),
                        keyActionsUr = listOf("بلاسٹ اور بیکٹیریل بلائٹ کا سپرے کریں", "کٹائی سے 10 دن قبل پانی بند کریں")
                    )
                ),
                commonPestsAndDiseases = listOf("Rice Stem Borer (تنے کی سنڈی)", "Rice Blast (بلاسٹ)", "Bacterial Leaf Blight (بیکٹیریل بلائٹ)", "Leaf Folder (پتہ لپیٹ سنڈی)"),
                expertTipsEn = listOf(
                    "Zinc deficiency causes 'Khaira' disease (rusty brown spots on young leaves).",
                    "Do not allow standing water at ripening stage to prevent grain staining.",
                    "Use 80,000 hills per acre for maximum yield potential."
                ),
                expertTipsUr = listOf(
                    "زنک کی کمی سے پتوں پر زنگ نما بھورے دھبے بنتے ہیں، زنک کا بروقت استعمال لازمی ہے۔",
                    "پکنے کے آخری دنوں میں پانی کھڑا نہ رکھیں تاکہ چاول کا دانہ چمکدار رہے۔",
                    "ایک ایکڑ میں کم از کم 80 ہزار پودوں کے سوراخ (ہلز) ہونے چاہئیں۔"
                )
            ),
            CropGuide(
                id = "sugarcane",
                nameEn = "Sugarcane",
                nameUr = "کماد / گنا",
                scientificName = "Saccharum officinarum",
                category = "Cash Crop",
                sowingSeasonEn = "Autumn (Sep-Oct) & Spring (Feb-March)",
                sowingSeasonUr = "ستمبر تا اکتوبر (خزاں) اور فروری تا مارچ (بہار)",
                harvestingSeasonEn = "November - March",
                harvestingSeasonUr = "نومبر تا مارچ",
                optimalTemperature = "26°C - 38°C",
                waterRequirement = "Very High (16 - 20 irrigations)",
                soilTypeEn = "Deep Rich Loamy Soil",
                soilTypeUr = "گہری اور زرخیز میرا زمین",
                seedRatePerAcre = "70 - 80 Mann Sets (دو یا تین آنکھوں والی پوریاں)",
                expectedYieldPerAcre = "800 - 1200 Mann / acre",
                estimatedCostPerAcre = 110000.0,
                estimatedRevenuePerAcre = 420000.0,
                recommendedVarieties = listOf(
                    "CPF-249 (سی پی ایف-249)",
                    "CPF-253 (سی پی ایف-253)",
                    "HSF-240 (ایچ ایس ایف-240)",
                    "CPF-248 (سی پی ایف-248)",
                    "CP-77/400 (سی پی-77/400)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "At Planting",
                        stageNameUr = "کاشت کے وقت",
                        timingEn = "In furrows with sets",
                        timingUr = "کھالیوں میں بیج کے ساتھ",
                        recommendationEn = "2.5 bags DAP + 1.5 bags SOP (Potash).",
                        recommendationUr = "ڈھائی بوری ڈی اے پی اور ڈیڑھ بوری پوٹاش کھالیوں میں ڈالیں۔",
                        dapBags = 2.5,
                        potashBags = 1.5
                    ),
                    FertilizerStage(
                        stageNameEn = "Tillering (90-120 Days)",
                        stageNameUr = "شگوفے نکلنے کے دوران",
                        timingEn = "Spring growth surge",
                        timingUr = "بہار کے موسم میں بڑھوتری پر",
                        recommendationEn = "Apply 3.5 bags Urea in 3 split doses before earthing up.",
                        recommendationUr = "مٹی چڑھانے سے پہلے ساڑھے تین بوری یوریا تین اقساط میں ڈالیں۔",
                        ureaBags = 3.5
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Germination & Tillering",
                        titleUr = "اگاؤ اور شگوفے",
                        daysAfterSowing = "0 - 90 Days",
                        descriptionEn = "Buds germinate from nodes. Interculturing and weed control essential.",
                        descriptionUr = "آنکھوں سے کونپلیں پھوٹتی ہیں اور شگوفے بنتے ہیں۔",
                        keyActionsEn = listOf("Apply termite & borer treatment with 1st water", "Weed eradication"),
                        keyActionsUr = listOf("پہلے پانی کے ساتھ دیمک اور گڑوؤں کی دوا فلڈ کریں", "گوڈی کر کے جڑی بوٹیاں ختم کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Grand Growth & Earthing Up",
                        titleUr = "تیز بڑھوتری اور مٹی چڑھانا",
                        daysAfterSowing = "90 - 240 Days",
                        descriptionEn = "Cane develops nodes rapidly. Earthing up prevents lodging during monsoon.",
                        descriptionUr = "گنے کے پور بنتے ہیں۔ مٹی چڑھانا گنے کو گرنے سے بچاتا ہے۔",
                        keyActionsEn = listOf("Complete earthing up in May-June", "Regular 10-day irrigation cycle"),
                        keyActionsUr = listOf("مئی جون میں مٹی چڑھائیں", "10 دن کے وقفے سے پانی دیں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 3,
                        titleEn = "Maturation & Sugar Accumulation",
                        titleUr = "پکنا اور مٹھاس بننا",
                        daysAfterSowing = "240 - 330 Days",
                        descriptionEn = "Cool dry weather concentrates sucrose in stalks.",
                        descriptionUr = "ٹھنڈا موسم گنے میں رس اور چینی کی مقدار بڑھاتا ہے۔",
                        keyActionsEn = listOf("Stop irrigation 20 days prior to harvest", "Cut close to ground level"),
                        keyActionsUr = listOf("کٹائی سے 20 دن قبل پانی روکیں", "گنا بالکل زمین کی سطح سے کاٹیں")
                    )
                ),
                commonPestsAndDiseases = listOf("Top Borer (چوٹی کا گڑواں)", "Stem Borer (تنے کا گڑواں)", "Red Rot (سرخ سڑاؤ)", "Sugarcane Pyrilla (پائرلا)"),
                expertTipsEn = listOf(
                    "Autumn sugarcane produces 25% higher yield compared to spring plantation.",
                    "Always treat sets with fungicide before planting to prevent red rot.",
                    "Cutting flush to the soil increases tonnage and gives strong ratoon crop."
                ),
                expertTipsUr = listOf(
                    "ستمبر کاشتہ کماد بہاریہ کماد سے 25 فیصد زیادہ پیداوار دیتا ہے۔",
                    "بیج کی پوریوں کو پھپھوندی کش دوا میں ڈبو کر کاشت کریں۔",
                    "کٹائی زمین کی سطح سے کرنے پر وزن زیادہ نکلتا ہے اور موڈھی فصل اچھی ہوتی ہے۔"
                )
            ),
            CropGuide(
                id = "maize",
                nameEn = "Maize / Corn (Spring & Autumn)",
                nameUr = "مکئی (بہاریہ و خریف)",
                scientificName = "Zea mays",
                category = "Grain",
                sowingSeasonEn = "Spring: Feb 1 - Mar 15 | Autumn: Jul 15 - Aug 15",
                sowingSeasonUr = "بہاریہ: یکم فروری تا 15 مارچ | خریف: 15 جولائی تا 15 اگست",
                harvestingSeasonEn = "May - June (Spring) | Nov - Dec (Autumn)",
                harvestingSeasonUr = "مئی تا جون (بہاریہ) | نومبر تا دسمبر (خریف)",
                optimalTemperature = "20°C - 35°C",
                waterRequirement = "High (6 - 8 irrigations on ridges)",
                soilTypeEn = "Deep Fertile Well-Drained Silt Loam",
                soilTypeUr = "زرخیز میرا اور اچھے نکاس والی زمین",
                seedRatePerAcre = "8 - 10 kg Hybrid Seed / acre",
                expectedYieldPerAcre = "80 - 110 Mann / acre",
                estimatedCostPerAcre = 70000.0,
                estimatedRevenuePerAcre = 240000.0,
                recommendedVarieties = listOf(
                    "Pioneer 30Y87 / 31P41",
                    "Dekalb DK-6789 / DK-9108",
                    "Syngenta NK-8441 / CS-200",
                    "Monsanto DK-6142",
                    "Yousafwala Hybrid (مقامی ہائبرڈ)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "Basal (Sowing)",
                        stageNameUr = "بجائی پر",
                        timingEn = "Ridge planting",
                        timingUr = "کھیلیاں بناتے وقت",
                        recommendationEn = "2 bags DAP + 1 bag SOP + 5 kg Zinc (33%).",
                        recommendationUr = "دو بوری ڈی اے پی + ایک بوری پوٹاش + 5 کلو زنک۔",
                        dapBags = 2.0,
                        potashBags = 1.0,
                        zincKg = 5.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Knee-High Stage (گھٹنے کے برابر)",
                        stageNameUr = "پودا گھٹنے جتنا ہونے پر (30 دن)",
                        timingEn = "30 days after germination",
                        timingUr = "اگاؤ کے 30 دن بعد",
                        recommendationEn = "1.5 bags Urea.",
                        recommendationUr = "ڈیڑھ بوری یوریا پانی کے ساتھ دیں۔",
                        ureaBags = 1.5
                    ),
                    FertilizerStage(
                        stageNameEn = "Tasseling & Silking (چھلی بنتے وقت)",
                        stageNameUr = "چھلی اور بور نکلنے پر (50 دن)",
                        timingEn = "50-55 days after germination",
                        timingUr = "اگاؤ کے 50 سے 55 دن بعد",
                        recommendationEn = "1.5 bags Urea + Boron foliar spray.",
                        recommendationUr = "ڈیڑھ بوری یوریا اور بوران کا سپرے کریں۔",
                        ureaBags = 1.5
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Germination & V4 Stage",
                        titleUr = "اگاؤ اور ابتدائی بڑھوتری",
                        daysAfterSowing = "0 - 25 Days",
                        descriptionEn = "Establishment on ridges. Fall armyworm scouting in whorl is vital.",
                        descriptionUr = "کھیلیوں پر اگاؤ۔ سنڈی کا معائنہ لازمی کریں۔",
                        keyActionsEn = listOf("Scout for Fall Armyworm", "Spray Coragen or Emamectin if pinholes seen"),
                        keyActionsUr = listOf("فال آرمی ورم کی سنڈی دیکھیں", "کورا جن یا ایما مMeanیکٹن کا سپرے کریں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Silking & Cob Development",
                        titleUr = "بور نکلنا اور چھلی بننا",
                        daysAfterSowing = "45 - 75 Days",
                        descriptionEn = "Silks pollinate kernels. Moisture stress at this stage causes blank tips.",
                        descriptionUr = "پولینیشن کا مرحلہ۔ اس وقت پانی کی کمی سے چھلی خالی رہ جاتی ہے۔",
                        keyActionsEn = listOf("Do not allow moisture stress during flowering", "Check grain filling"),
                        keyActionsUr = listOf("بور اور سلکنگ کے دوران پانی کا سوکھا نہ لگنے دیں", "چھلی کے دانے چیک کریں")
                    )
                ),
                commonPestsAndDiseases = listOf("Fall Armyworm (فال آرمی ورم)", "Maize Stem Borer (مکئی کا گڑواں)", "Banded Leaf Blight (جھلساؤ)"),
                expertTipsEn = listOf(
                    "Never allow water deficit during tasseling and silking stages.",
                    "Plant on ridges with plant spacing of 6-8 inches and row spacing of 2.5 feet.",
                    "Apply granular insecticide in the plant whorl against shoot fly and armyworm."
                ),
                expertTipsUr = listOf(
                    "بور اور سلکنگ نکلنے کے دوران پانی کا سوکھا ہرگز نہ لگنے دیں۔",
                    "کھیلیوں پر لائن سے لائن کا فاصلہ ڈھائی فٹ اور پودے سے پودا 6 سے 8 انچ رکھیں۔",
                    "پودے کی گوبھ کے اندر دانے دار زہر ڈالیں تاکہ سنڈی ختم ہو سکے۔"
                )
            ),
            CropGuide(
                id = "potato",
                nameEn = "Potato (Autumn Crop)",
                nameUr = "آلو (خریف کاشتہ)",
                scientificName = "Solanum tuberosum",
                category = "Vegetable",
                sowingSeasonEn = "September 20 - October 20",
                sowingSeasonUr = "20 ستمبر تا 20 اکتوبر",
                harvestingSeasonEn = "January - February",
                harvestingSeasonUr = "جنوری تا فروری",
                optimalTemperature = "15°C - 22°C",
                waterRequirement = "Medium (6 - 8 light irrigations on ridges)",
                soilTypeEn = "Loose Sandy Loam with High Organic Matter",
                soilTypeUr = "ہلکی ریتلی میرا زمین جس میں نامیاتی مادہ زیادہ ہو",
                seedRatePerAcre = "1000 - 1200 kg Tubers (20-25 Bags) / acre",
                expectedYieldPerAcre = "200 - 280 Bags (50kg) / acre",
                estimatedCostPerAcre = 140000.0,
                estimatedRevenuePerAcre = 380000.0,
                recommendedVarieties = listOf(
                    "Mozart (موزارٹ - سرخ چھلکا)",
                    "Asterix (ایسٹرکس)",
                    "Kennebec (کینی بیک)",
                    "Lady Rosetta (لیڈی روزیٹا - چپس والی)",
                    "Santé (سانتے)",
                    "Cardinal (کارڈینل)"
                ),
                fertilizerSchedule = listOf(
                    FertilizerStage(
                        stageNameEn = "Basal (Bed Making)",
                        stageNameUr = "زمین تیاری پر",
                        timingEn = "Before planting on ridges",
                        timingUr = "کھیلیاں بنانے سے قبل",
                        recommendationEn = "3 bags DAP + 2 bags SOP (Sulphate of Potash) + 1 bag Magnesium Sulphate.",
                        recommendationUr = "3 بوری ڈی اے پی + 2 بوری پوٹاش + 1 بوری میگنیشیم سلفیٹ۔",
                        dapBags = 3.0,
                        potashBags = 2.0
                    ),
                    FertilizerStage(
                        stageNameEn = "Earthing Up (30-35 Days)",
                        stageNameUr = "مٹی چڑھاتے وقت (30 تا 35 دن)",
                        timingEn = "When plants are 6-8 inches tall",
                        timingUr = "جب پودے 6 سے 8 انچ کے ہوں",
                        recommendationEn = "1.5 bags Urea or Ammonium Nitrate + 1 bag SOP dissolved in water.",
                        recommendationUr = "ڈیڑھ بوری یوریا اور ایک بوری پوٹاش پانی کے ساتھ فلڈ کریں۔",
                        ureaBags = 1.5,
                        potashBags = 1.0
                    )
                ),
                growthStages = listOf(
                    GrowthStageGuide(
                        stageNumber = 1,
                        titleEn = "Sprouting & Vegetative Growth",
                        titleUr = "کونپلیں اور شاخیں نکلنا",
                        daysAfterSowing = "0 - 30 Days",
                        descriptionEn = "Eyes sprout through soil. Complete weed removal and earthing up.",
                        descriptionUr = "آلو کی آنکھوں سے پودے نکلتے ہیں۔ مٹی چڑھانا اور جڑی بوٹی کنٹرول لازمی ہے۔",
                        keyActionsEn = listOf("Keep soil moist but not waterlogged", "Complete earthing up to cover tubers"),
                        keyActionsUr = listOf("زمین میں نمی رکھیں لیکن زیادہ پانی نہ بھریں", "مٹی چڑھا کر آلوؤں کو ڈھانپ دیں")
                    ),
                    GrowthStageGuide(
                        stageNumber = 2,
                        titleEn = "Tuber Initiation & Bulking",
                        titleUr = "آلو بننا اور سائز بڑھنا",
                        daysAfterSowing = "30 - 80 Days",
                        descriptionEn = "Stolons swell into tubers. High risk of late blight under cloudy/foggy weather.",
                        descriptionUr = "آلو کا سائز بنتا ہے۔ کہرے اور دھند کے دنوں میں جھلساؤ کا شدید خطرہ ہوتا ہے۔",
                        keyActionsEn = listOf("Preventive fungicide spray for Late Blight (Mancozeb/Ridomil)", "Frost protection irrigation"),
                        keyActionsUr = listOf("جھلساؤ سے بچاؤ کے لیے مینکوزیب یا ریڈومل کا سپرے کریں", "کہرے سے بچاؤ کے لیے ہلکا پانی دیں")
                    )
                ),
                commonPestsAndDiseases = listOf("Late Blight (پچھیتا جھلساؤ)", "Early Blight (ارلی بلائیٹ)", "Aphids (سست تیلا)", "Black Scurf (کالے دھبے)"),
                expertTipsEn = listOf(
                    "Cover tubers thoroughly with soil during earthing up to prevent greening from sunlight.",
                    "Give light night irrigation when frost warning is forecast in December/January.",
                    "Spray preventive fungicide before persistent foggy/cloudy conditions arrive."
                ),
                expertTipsUr = listOf(
                    "مٹی چڑھاتے وقت تمام آلوؤں کو اچھی طرح ڈھانپیں تاکہ دھوپ سے وہ ہرے نہ ہوں۔",
                    "دسمبر اور جنوری میں شدید کہرے کی پیشگوئی پر رات کو ہلکا پانی لگائیں۔",
                    "دھند اور ابر آلود موسم آنے سے پہلے احتیاطی پھپھوندی کش سپرے کریں۔"
                )
            )
        )
    }

    private fun getPakistaniDiseaseData(): List<PlantDiseaseGuide> {
        return listOf(
            PlantDiseaseGuide(
                id = "yellow_rust",
                diseaseNameEn = "Yellow / Stripe Rust of Wheat",
                diseaseNameUr = "گندم کی زرد کنگی (پیلے دھبے)",
                scientificName = "Puccinia striiformis f. sp. tritici",
                affectedCrops = listOf("Wheat (گندم)", "Barley (جو)"),
                pathogenType = "Fungus",
                severityLevel = "Critical",
                favorableWeatherEn = "Cool, humid and cloudy weather with temperatures between 10°C - 20°C and frequent dew.",
                favorableWeatherUr = "ٹھنڈا، ابر آلود اور مرطوب موسم (10 سے 20 ڈگری سینٹی گریڈ) اور مسلسل شبنم۔",
                symptomsSummaryEn = "Distinct yellow-orange powdery pustules formed in parallel narrow stripes on the leaf blades.",
                symptomsSummaryUr = "پتوں پر قطار کی شکل میں پیلے اور نارنجی رنگ کی لکیریں اور پاؤڈر نمودار ہوتا ہے۔",
                detailedSymptomsEn = listOf(
                    "Small yellow-orange spots align linearly along the leaf veins.",
                    "Yellow dust adheres to hands and clothes when touching the foliage.",
                    "Severely affected leaves dry up, turn brown, and shrivel rapidly.",
                    "Ears are infected directly causing shriveled grain with up to 50% yield reduction."
                ),
                detailedSymptomsUr = listOf(
                    "پتوں کی رگوں کے ساتھ ساتھ پیلے اور نارنجی رنگ کے دھبے لکیروں کی صورت میں بنتے ہیں۔",
                    "پتے چھونے پر ہاتھ اور کپڑوں پر پیلا پاؤڈر لگ جاتا ہے۔",
                    "متاثرہ پتے خشک ہو کر بھورے ہو جاتے ہیں اور گرنے لگتے ہیں۔",
                    "سٹوں پر حملہ ہونے سے دانہ سوکھ جاتا ہے اور پیداوار میں 50 فیصد تک کمی ہو جاتی ہے۔"
                ),
                chemicalTreatments = listOf(
                    ChemicalTreatmentItem(
                        chemicalName = "Propiconazole 25% EC",
                        tradeBrandPakistan = "Tilt 250 EC (ٹلٹ)",
                        manufacturer = "Syngenta Pakistan",
                        dosagePerAcreOr100L = "200 ml per acre in 100-120 Liters water",
                        applicationMethodEn = "Foliar spray across entire field immediately upon seeing initial foci spots.",
                        applicationMethodUr = "کھیت میں ابتدائی دھبے نظر آتے ہی پورے کھیت میں فوری سپرے کریں۔",
                        safetyWaitingPeriodDays = 21
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Tebuconazole + Trifloxystrobin",
                        tradeBrandPakistan = "Nativo 75 WG (نیٹیوو)",
                        manufacturer = "Bayer CropScience",
                        dosagePerAcreOr100L = "65 grams per acre in 100 Liters water",
                        applicationMethodEn = "Systemic dual-action spray protecting new emerging leaves for up to 21 days.",
                        applicationMethodUr = "نیا نکلنے والا پتہ بھی محفوظ رہتا ہے، 21 دن تک تحفظ فراہم کرتا ہے۔",
                        safetyWaitingPeriodDays = 28
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Azoxystrobin + Difenoconazole",
                        tradeBrandPakistan = "Amistar Top (ایمسٹار ٹاپ)",
                        manufacturer = "Syngenta",
                        dosagePerAcreOr100L = "200 ml per acre in 100 Liters water",
                        applicationMethodEn = "Curative & preventive curative spray with rapid greening effect.",
                        applicationMethodUr = "علاجی اور حفاظتی سپرے جس سے فصل میں ہریالی بھی واپس آتی ہے۔",
                        safetyWaitingPeriodDays = 21
                    )
                ),
                organicRemediesEn = listOf(
                    "Plant resistant varieties: Akbar-19, Dilkash-20, Subhani-21, Fakhar-e-Bhakkar.",
                    "Spray fermented sour butter-milk (Lassi) mixed with copper vessel extract (2 liters per 100L water).",
                    "Avoid over-dosing of Nitrogen (Urea) and balance with Potash fertilizers."
                ),
                organicRemediesUr = listOf(
                    "بیماری کے خلاف قوت مدافعت رکھنے والی منظور شدہ اقسام کاشت کریں۔",
                    "کھٹی لسی تانبے کے برتن میں رکھ کر اس کا محلول (2 لیٹر فی 100 لیٹر پانی) سپرے کریں۔",
                    "نائٹروجن (یوریا) کا بے جا استعمال نہ کریں بلکہ پوٹاش کے ساتھ توازن رکھیں۔"
                ),
                preventiveMeasuresEn = listOf(
                    "Weekly crop scouting in January and February, especially in shaded field corners.",
                    "Destroy volunteer self-sown wheat and grass hosts near water channels.",
                    "Spray promptly within 24-48 hours of detecting rust hotspot."
                ),
                preventiveMeasuresUr = listOf(
                    "جنوری اور فروری میں ہفتہ وار فصل کا معائنہ کریں، خاص طور پر سایہ دار جگہوں پر۔",
                    "کھالوں اور وٹوں کے پاس اگی جنگلی گھاس اور خود رو پودے تلف کریں۔",
                    "بیماری کا پہلا دھبہ نظر آتے ہی 48 گھنٹے کے اندر سپرے مکمل کریں۔"
                ),
                audioExplanationEn = "Yellow rust is a dangerous airborne fungal disease that destroys wheat leaves in cool weather. Spray Tilt or Nativo immediately.",
                audioExplanationUr = "زرد کنگی گندم کی ایک خطرناک پھپھوندی بیماری ہے جو ٹھنڈے موسم میں پتوں کو نقصان پہنچاتی ہے۔ بروقت ٹلٹ یا نیٹیوو کا سپرے کریں۔"
            ),
            PlantDiseaseGuide(
                id = "cotton_whitefly_clcuv",
                diseaseNameEn = "Cotton Leaf Curl Virus (CLCuV) & Whitefly",
                diseaseNameUr = "کپاس کا مروڑ وائرس (سی ایل سی یو وی) اور سفید مکھی",
                scientificName = "Begomovirus (transmitted by Bemisia tabaci)",
                affectedCrops = listOf("Cotton (کپاس)", "Okra / Bhindi (بھنڈی)", "Tomato (ٹماٹر)"),
                pathogenType = "Virus (Insect Vector)",
                severityLevel = "Critical",
                favorableWeatherEn = "Hot, humid weather with temperatures between 32°C - 42°C accelerating whitefly breeding.",
                favorableWeatherUr = "گرم اور مرطوب موسم (32 تا 42 ڈگری) جس میں سفید مکھی کی نسل تیزی سے بڑھتی ہے۔",
                symptomsSummaryEn = "Upward and downward leaf curling, swollen dark veins, enations under leaves, stunted growth.",
                symptomsSummaryUr = "پتوں کا اوپر یا نیچے کی طرف مڑنا، رگوں کا موٹا ہونا اور پودے کا قد چھوٹا رہ جانا۔",
                detailedSymptomsEn = listOf(
                    "Leaf edges curl either upwards or downwards with thickened dark green veins.",
                    "Small cup-shaped leaf-like enations develop on the underside of main leaf veins.",
                    "Flowering is severely delayed, squares drop, and bolls fail to open fully.",
                    "Sticky honeydew secreted by whiteflies leads to black sooty mold on foliage."
                ),
                detailedSymptomsUr = listOf(
                    "پتوں کے کنارے اوپر یا نیچے کی طرف مڑ جاتے ہیں اور رگیں موٹی ہو جاتی ہیں۔",
                    "پتے کی نچلی سطح پر پیالہ نما ابھار بن جاتے ہیں۔",
                    "پھول اور ٹینڈے گر جاتے ہیں اور جو ٹینڈے بنتے ہیں وہ پورے کھل نہیں پاتے۔",
                    "سفید مکھی کے فضلے سے پتوں پر کالا سیاہ مواد جم جاتا ہے جس سے پتے سانس نہیں لے پاتے۔"
                ),
                chemicalTreatments = listOf(
                    ChemicalTreatmentItem(
                        chemicalName = "Pyriproxyfen 10.8% EC",
                        tradeBrandPakistan = "Pyriproxyfen (پائری پروکسی فن)",
                        manufacturer = "FMC / Kanzo",
                        dosagePerAcreOr100L = "500 ml per acre in 100 Liters water",
                        applicationMethodEn = "Insect growth regulator targeting whitefly eggs and nymphs.",
                        applicationMethodUr = "سفید مکھی کے انڈوں اور بچوں کا مکمل خاتمہ کرتا ہے۔",
                        safetyWaitingPeriodDays = 14
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Diafenthiuron 50% SC",
                        tradeBrandPakistan = "Polo (پولو)",
                        manufacturer = "Syngenta Pakistan",
                        dosagePerAcreOr100L = "250 ml per acre in 100-120 Liters water",
                        applicationMethodEn = "Adult knockdown and systemic action under leaves.",
                        applicationMethodUr = "بڑی سفید مکھی اور بچوں کے فوری خاتمے کے لیے بہترین ہے۔",
                        safetyWaitingPeriodDays = 21
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Spiromesifen 240 SC",
                        tradeBrandPakistan = "Oberon (اوبران)",
                        manufacturer = "Bayer",
                        dosagePerAcreOr100L = "100 ml per acre in 100 Liters water",
                        applicationMethodEn = "Foliar spray with translaminar penetration under leaf surface.",
                        applicationMethodUr = "پتے کی دوسری طرف موجود کیڑوں کو بھی ہلاک کرتا ہے۔",
                        safetyWaitingPeriodDays = 14
                    )
                ),
                organicRemediesEn = listOf(
                    "Install yellow sticky traps (15 - 20 traps per acre) to catch adult whiteflies.",
                    "Spray Neem Seed Kernel Extract (50g per Liter) + soft agricultural soap.",
                    "Eradicate host weeds (Peeli Booti, Itsit, Lehli) along borders."
                ),
                organicRemediesUr = listOf(
                    "کھیت میں پیلے رنگ کے گوند والے تختے (15 سے 20 ٹریپس فی ایکڑ) لگائیں۔",
                    "نیم کے بیجوں کا عرق (50 گرام فی لیٹر) اور صابن کا محلول سپرے کریں۔",
                    "کھیت کے کناروں پر اگی پیلی بوٹی اور دیگر جڑی بوٹیاں تلف کریں۔"
                ),
                preventiveMeasuresEn = listOf(
                    "Use certified tolerant cotton varieties (CKC-3, FH-333, BS-15).",
                    "Avoid excessive late nitrogen applications which attract sucking pests.",
                    "Use hollow cone nozzles directed under the plant canopy."
                ),
                preventiveMeasuresUr = listOf(
                    "مروڑ وائرس کے خلاف قوت رکھنے والی اقسام (سی کے سی-3، ایف ایچ-333) لگائیں۔",
                    "یوریا کی زیادہ مقدار سے پرہیز کریں جس سے پتے زیادہ نرم ہو کر مکھی کو کھینچتے ہیں۔",
                    "سپرے کا رخ پتوں کے نیچے کی طرف رکھیں جہاں کیڑے چھپے ہوتے ہیں۔"
                ),
                audioExplanationEn = "Cotton leaf curl virus is spread by whiteflies. Controlling the vector early with Pyriproxyfen or Polo saves your yield.",
                audioExplanationUr = "کپاس کا مروڑ وائرس سفید مکھی کے ذریعے پھیلتا ہے۔ سفید مکھی کا بروقت تدارک کریں تاکہ وائرس سے بچا جا سکے۔"
            ),
            PlantDiseaseGuide(
                id = "potato_late_blight",
                diseaseNameEn = "Late Blight of Potato & Tomato",
                diseaseNameUr = "آلو اور ٹماٹر کا پچھیتا جھلساؤ",
                scientificName = "Phytophthora infestans",
                affectedCrops = listOf("Potato (آلو)", "Tomato (ٹماٹر)", "Chilli (مرچ)"),
                pathogenType = "Oomycete / Water Mold",
                severityLevel = "Critical",
                favorableWeatherEn = "Dense fog, cloudy days, high humidity (>90%) with cool temperatures (12°C - 18°C).",
                favorableWeatherUr = "گہری دھند، ابر آلود موسم، ہوا میں نمی 90 فیصد سے زیادہ اور درجہ حرارت 12 سے 18 ڈگری۔",
                symptomsSummaryEn = "Water-soaked dark lesions on leaf tips that rapidly turn black, with white fungal mold underneath.",
                symptomsSummaryUr = "پتوں کے کناروں پر گیلے سیاہ دھبے، جو تیزی سے پھیلتے ہیں اور پتے کے نیچے سفید پھپھوندی نظر آتی ہے۔",
                detailedSymptomsEn = listOf(
                    "Irregular dark brown water-soaked lesions appear on leaf tips and margins.",
                    "White delicate fungal growth visible on the underside of leaves during morning dew.",
                    "Stems turn black and become brittle, emitting a characteristic rotting smell.",
                    "Tubers develop brown-to-purple sunken skin rot extending into the flesh."
                ),
                detailedSymptomsUr = listOf(
                    "پتوں کے سروں اور کناروں پر گہرے بھورے پانی بھرے دھبے بنتے ہیں۔",
                    "صبح کی شبنم کے وقت پتے کی نچلی سطح پر سفید رنگ کا روئی جیسا جال دکھائی دیتا ہے۔",
                    "تنے کالے ہو کر ٹوٹنے لگتے ہیں اور کھیت سے سڑاند کی بو آتی ہے۔",
                    "آلو کی جلد اندر کو دھنس جاتی ہے اور اندر سے گودا بھورا ہو کر گل جاتا ہے۔"
                ),
                chemicalTreatments = listOf(
                    ChemicalTreatmentItem(
                        chemicalName = "Mancozeb + Metalaxyl",
                        tradeBrandPakistan = "Ridomil Gold MZ (ریڈومل گولڈ)",
                        manufacturer = "Syngenta Pakistan",
                        dosagePerAcreOr100L = "250 - 300 grams per acre in 100 Liters water",
                        applicationMethodEn = "Preventive & early curative spray applied before or right at disease onset.",
                        applicationMethodUr = "بیماری آنے سے پہلے یا پہلے نشان پر فوری سپرے کریں۔",
                        safetyWaitingPeriodDays = 14
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Dimethomorph + Mancozeb",
                        tradeBrandPakistan = "Acrobat MZ (ایکروبیٹ)",
                        manufacturer = "BASF",
                        dosagePerAcreOr100L = "250 grams per acre in 100 Liters water",
                        applicationMethodEn = "Strong antisporulant action stopping further spread within 24 hours.",
                        applicationMethodUr = "بیماری کے بیضوں کو 24 گھنٹے کے اندر روک دیتا ہے۔",
                        safetyWaitingPeriodDays = 14
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Cymoxanil + Famoxadone",
                        tradeBrandPakistan = "Equation Pro (ایکویژن پرو)",
                        manufacturer = "Corteva Agriscience",
                        dosagePerAcreOr100L = "160 grams per acre in 100 Liters water",
                        applicationMethodEn = "Rapid rain-fast kickback chemistry.",
                        applicationMethodUr = "بارش اور کہرے کے بعد فوری کام کرنے والی دوا۔",
                        safetyWaitingPeriodDays = 10
                    )
                ),
                organicRemediesEn = listOf(
                    "Avoid sprinkler/overhead irrigation that prolongs leaf wetness.",
                    "Ensure high earthing up so spores cannot wash down into tubers.",
                    "Spray copper-based organic formulations or Bordeaux mixture (1%)."
                ),
                organicRemediesUr = listOf(
                    "پتوں پر براہ راست پانی گرانے سے پرہیز کریں تاکہ پتے گیلے نہ رہیں۔",
                    "آلوؤں پر مٹی اچھی طرح چڑھائیں تاکہ بیماری کے جراثیم زمین کے اندر آلو تک نہ پہنچیں۔",
                    "کاپر بیسڈ قدرتی محلول یا بورڈو مکسچر کا سپرے کریں۔"
                ),
                preventiveMeasuresEn = listOf(
                    "Spray preventive Mancozeb 75 WP before dense winter fog begins in December.",
                    "Inspect field every morning during foggy spells.",
                    "Kill foliage (haulm killing) 10-12 days before harvesting infected crop."
                ),
                preventiveMeasuresUr = listOf(
                    "دسمبر میں شدید دھند شروع ہونے سے قبل مینکوزیب کا حفاظتی سپرے کریں۔",
                    "دھند کے دوران روزانہ صبح کھیت کے اندر جا کر پتوں کا معائنہ کریں۔",
                    "کٹائی سے 10 دن قبل بیلیں کاٹ دیں تاکہ آلو محفوظ رہیں۔"
                ),
                audioExplanationEn = "Late blight can destroy an entire potato field in 4 days during foggy weather. Spray Ridomil Gold or Acrobat immediately.",
                audioExplanationUr = "پچھیتا جھلساؤ دھند میں چند دنوں کے اندر پوری فصل تباہ کر سکتا ہے۔ ریڈومل گولڈ کا فوری سپرے کریں۔"
            ),
            PlantDiseaseGuide(
                id = "rice_blast",
                diseaseNameEn = "Rice Blast & Neck Rot",
                diseaseNameUr = "دھان کا بلاسٹ اور گردن توڑ",
                scientificName = "Magnaporthe oryzae",
                affectedCrops = listOf("Rice / Paddy (چاول / دھان)"),
                pathogenType = "Fungus",
                severityLevel = "Critical",
                favorableWeatherEn = "Cool nights (20°C), warm days with high humidity (>90%) and cloudy conditions.",
                favorableWeatherUr = "ٹھنڈی راتیں (20 ڈگری)، دن گرم اور ہوا میں نمی زیادہ (90 فیصد)۔",
                symptomsSummaryEn = "Spindle-shaped or eye-shaped lesions on leaves with gray centers; neck rot turns whole panicle white and empty.",
                symptomsSummaryUr = "پتوں پر آنکھ نما بیضوی دھبے اور گردن سڑنے سے پورا سٹہ سفید اور خالی رہ جاتا ہے۔",
                detailedSymptomsEn = listOf(
                    "Eye-shaped lesions with gray/white center and dark reddish-brown margins on leaf blades.",
                    "Neck node turns dark brown and shrivels, preventing grain filling (Neck Blast).",
                    "Infected panicles turn completely white and stand erect without grain weight ('White Heads').",
                    "Lodging of crop occurs when nodes are infected."
                ),
                detailedSymptomsUr = listOf(
                    "پتوں پر بیضوی آنکھ کی شکل کے دھبے جن کے بیچ کا حصہ سرمئی اور کنارے لال بھورے ہوتے ہیں۔",
                    "سٹے کی گردن کالی ہو کر گل جاتی ہے جس سے سٹے میں خوراک جانا بند ہو جاتی ہے۔",
                    "سٹے بالکل سفید ہو جاتے ہیں اور ان میں چاول کا دانہ نہیں بنتا۔",
                    "پودے کے جوڑ گلنے سے فصل زمین پر گر جاتی ہے۔"
                ),
                chemicalTreatments = listOf(
                    ChemicalTreatmentItem(
                        chemicalName = "Tricyclazole 75% WP",
                        tradeBrandPakistan = "Beam 75 WP (بیم)",
                        manufacturer = "Corteva Agriscience",
                        dosagePerAcreOr100L = "120 grams per acre in 100 Liters water",
                        applicationMethodEn = "Specialized blast fungicide applied at booting and panicle emergence.",
                        applicationMethodUr = "بلاسٹ کی خاص دوا ہے، سٹہ نکلنے کے وقت سپرے کریں۔",
                        safetyWaitingPeriodDays = 28
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Kasugamycin 2% SL",
                        tradeBrandPakistan = "Kasumin (کاسومین)",
                        manufacturer = "Arysta LifeScience",
                        dosagePerAcreOr100L = "250 - 300 ml per acre in 100 Liters water",
                        applicationMethodEn = "Antibiotic-fungicide controlling blast and bacterial blight simultaneously.",
                        applicationMethodUr = "بلاسٹ اور بیکٹیریل بلائٹ دونوں کے خلاف بیک وقت موثر ہے۔",
                        safetyWaitingPeriodDays = 14
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Isoprothiolane 40% EC",
                        tradeBrandPakistan = "Fuji-One (فیوجی ون)",
                        manufacturer = "Nihon Nohyaku",
                        dosagePerAcreOr100L = "300 ml per acre in 100 Liters water",
                        applicationMethodEn = "Systemic blast cure translocating to panicles.",
                        applicationMethodUr = "سٹے کے اندر تک پہنچ کر بیماری کا خاتمہ کرتی ہے۔",
                        safetyWaitingPeriodDays = 21
                    )
                ),
                organicRemediesEn = listOf(
                    "Seed treatment before nursery sowing with bio-fungicides.",
                    "Maintain 3-5 cm water depth in paddy during critical growth periods.",
                    "Avoid excessive single applications of Urea (split into 3 equal doses)."
                ),
                organicRemediesUr = listOf(
                    "پنیری لگانے سے پہلے بیج کو دوائی لگا کر کاشت کریں۔",
                    "کھیت میں 3 سے 5 سینٹی میٹر پانی کی سطح برقرار رکھیں۔",
                    "یوریا کی ایک دم زیادہ مقدار ڈالنے سے پرہیز کریں۔"
                ),
                preventiveMeasuresEn = listOf(
                    "Mandatory preventive spray at 50% boot emergence stage.",
                    "Collect and burn stubble after harvesting infected fields.",
                    "Plant tolerant Basmati varieties like Super Gold and Kissan Basmati."
                ),
                preventiveMeasuresUr = listOf(
                    "سٹہ نکلتے ہی حفاظتی سپرے لازمی کریں۔",
                    "متاثرہ کھیت کے مڈھے جلا کر زمین کو اچھی طرح ہل چلائیں۔",
                    "قوت مدافعت رکھنے والی اقسام کاشت کریں۔"
                ),
                audioExplanationEn = "Rice blast attacks the panicle neck and turns grains empty. Spray Beam or Kasumin right at booting stage.",
                audioExplanationUr = "دھان کا بلاسٹ سٹے کی گردن توڑ دیتا ہے جس سے چاول نہیں بنتا۔ سٹہ نکلنے کے وقت بیم کا سپرے کریں۔"
            ),
            PlantDiseaseGuide(
                id = "pink_bollworm",
                diseaseNameEn = "Pink Bollworm of Cotton",
                diseaseNameUr = "کپاس کی گلابی سنڈی",
                scientificName = "Pectinophora gossypiella",
                affectedCrops = listOf("Cotton (کپاس)", "Okra (بھنڈی)"),
                pathogenType = "Insect Pest",
                severityLevel = "Critical",
                favorableWeatherEn = "Warm humid conditions during boll development (July - October).",
                favorableWeatherUr = "ٹینڈے بننے کے دوران گرم اور مرطوب موسم (جولائی تا اکتوبر)۔",
                symptomsSummaryEn = "Rosetted flower petals (گلابی پھول), punctured bolls with stained lint, premature opening.",
                symptomsSummaryUr = "پھول آپس میں جڑ کر گلاب نما ہو جاتے ہیں، ٹینڈوں میں سوراخ اور روئی داغدار ہو جاتی ہے۔",
                detailedSymptomsEn = listOf(
                    "Flowers twist together like a rose bud and fail to open naturally ('Rosetted Flowers').",
                    "Young larvae enter bolls and seal the entry hole, feeding silently inside on seeds.",
                    "Bolls develop double seeds eaten together and fiber quality is degraded with yellow staining.",
                    "Bolls rot internally or open poorly ('Parrot Beaking')."
                ),
                detailedSymptomsUr = listOf(
                    "پھول کے پتے آپس میں مڑ کر گلاب کے پھول جیسے بن جاتے ہیں اور کھلتے نہیں۔",
                    "سنڈی چھوٹے ٹینڈے میں گھس کر راستہ بند کر لیتی ہے اور اندر بیج کھاتی ہے۔",
                    "روئی کا ریشہ کالا اور پیلا ہو جاتا ہے جس کی مارکیٹ میں قیمت گر جاتی ہے۔",
                    "ٹینڈا پورا نہیں کھلتا اور طوطے کی چونچ جیسا رہ جاتا ہے۔"
                ),
                chemicalTreatments = listOf(
                    ChemicalTreatmentItem(
                        chemicalName = "Chlorantraniliprole 18.5% SC",
                        tradeBrandPakistan = "Coragen (کورا جن)",
                        manufacturer = "FMC Pakistan",
                        dosagePerAcreOr100L = "50 ml per acre in 100 Liters water",
                        applicationMethodEn = "Ovi-larvicidal chemistry preventing caterpillar emergence.",
                        applicationMethodUr = "سنڈی اور انڈے دونوں کو ختم کرتا ہے۔",
                        safetyWaitingPeriodDays = 21
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Emamectin Benzoate 1.9% EC",
                        tradeBrandPakistan = "Proclaim (پروکلیم)",
                        manufacturer = "Syngenta Pakistan",
                        dosagePerAcreOr100L = "200 ml per acre in 100 Liters water",
                        applicationMethodEn = "Stomach poison that paralyzes caterpillars inside squares.",
                        applicationMethodUr = "سنڈی کا معدہ مفلوج کر کے اسے ہلاک کرتا ہے۔",
                        safetyWaitingPeriodDays = 14
                    ),
                    ChemicalTreatmentItem(
                        chemicalName = "Spinetoram 11.7% SC",
                        tradeBrandPakistan = "Radiant 120 SC (ریڈیئنٹ)",
                        manufacturer = "Corteva Agriscience",
                        dosagePerAcreOr100L = "80 - 100 ml per acre in 100 Liters water",
                        applicationMethodEn = "Fast penetrating formulation against hidden bollworms.",
                        applicationMethodUr = "ٹینڈے کے اندر چھپی سنڈی کے خاتمے کے لیے تیز ترین دوا۔",
                        safetyWaitingPeriodDays = 14
                    )
                ),
                organicRemediesEn = listOf(
                    "Install PB-Rope pheromone dispensers or 8 delta pheromone traps per acre.",
                    "Release Trichogramma egg-parasitoid cards (2 cards per acre every 10 days).",
                    "Shred cotton sticks with a rotary slasher after final picking."
                ),
                organicRemediesUr = listOf(
                    "کھیت میں جنسی پھندے (8 ٹریپس فی ایکڑ) لگائیں۔",
                    "ٹرائیکو گراما دوست کیڑوں کے کارڈز کھیت میں لگائیں۔",
                    "آخری چنائی کے بعد چھڑیاں روٹا ویٹر سے زمین میں ملا دیں۔"
                ),
                preventiveMeasuresEn = listOf(
                    "Do not store un-shredded cotton sticks with intact bolls near fields.",
                    "Check 20 bolls per acre weekly; treat if 1-2 larvae are found inside bolls.",
                    "Adopt non-BT refuge rows or use latest double-gene BT seeds."
                ),
                preventiveMeasuresUr = listOf(
                    "کپاس کی چھڑیاں جن پر ٹینڈے لگے ہوں کھیت کے قریب جمع نہ کریں۔",
                    "ہفتہ وار 20 ٹینڈے توڑ کر معائنہ کریں، ایک سنڈی نظر آنے پر بھی سپرے کریں۔",
                    "مصدقہ بی ٹی بیج استعمال کریں۔"
                ),
                audioExplanationEn = "Pink bollworm damages cotton bolls internally. Install pheromone traps and spray Coragen or Radiant.",
                audioExplanationUr = "گلابی سنڈی ٹینڈے کے اندر چھپ کر روئی کو کالا کرتی ہے۔ جنسی پھندے لگائیں اور کورا جن کا سپرے کریں۔"
            )
        )
    }
}
