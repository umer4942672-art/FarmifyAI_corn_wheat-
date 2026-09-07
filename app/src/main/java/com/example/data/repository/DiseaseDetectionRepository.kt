package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.DiseaseScanDao
import com.example.data.local.DiseaseScanEntity
import com.example.data.model.DiseaseCrop
import com.example.data.model.PlantDiseaseResult
import com.example.data.model.CornTFLiteClassifier
import com.example.data.model.TFLitePlantClassifier
import com.example.data.remote.SupabaseDataSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class DiseaseDetectionRepository(
    private val diseaseScanDao: DiseaseScanDao,
    context: Context,
    private val supabaseSync: SupabaseDataSyncService = SupabaseDataSyncService(context.applicationContext)
) {

    private val context: Context = context.applicationContext

    private val wheatClassifier: TFLitePlantClassifier? by lazy {
        try {
            TFLitePlantClassifier(context)
        } catch (_: Exception) {
            null
        }
    }

    private val cornClassifier: CornTFLiteClassifier? by lazy {
        try {
            CornTFLiteClassifier(context)
        } catch (_: Exception) {
            null
        }
    }

    val allScans: Flow<List<PlantDiseaseResult>> = diseaseScanDao.getAllScans().map { entities ->
        entities.map { it.toModel() }
    }

    fun getScansForUser(userId: String): Flow<List<PlantDiseaseResult>> =
        diseaseScanDao.getScansForUser(userId).map { entities -> entities.map { it.toModel() } }

    suspend fun saveScan(result: PlantDiseaseResult, userId: String = ""): Long = withContext(Dispatchers.IO) {
        require(result.isPlantImage) { "Rejected image results must not be saved to scan history" }
        val entity = DiseaseScanEntity(
            id = result.id,
            userId = userId,
            cropName = result.cropName,
            diseaseNameEn = result.diseaseNameEn,
            diseaseNameUr = result.diseaseNameUr,
            confidencePercent = result.confidencePercent,
            isHealthy = result.isHealthy,
            severityLevel = result.severityLevel,
            symptoms = result.symptomsEn,
            symptomsUr = result.symptomsUr,
            chemicalTreatment = result.chemicalTreatmentEn,
            chemicalTreatmentUr = result.chemicalTreatmentUr,
            organicPrevention = result.organicPreventionEn,
            organicPreventionUr = result.organicPreventionUr,
            advisoryNote = result.advisoryNoteEn,
            advisoryNoteUr = result.advisoryNoteUr,
            imageUriOrPath = result.imagePathOrUri,
            timestamp = result.timestamp
        )
        val id = diseaseScanDao.insertScan(entity)

        try {
            supabaseSync.syncDiseaseDetection(entity.copy(id = id))
        } catch (_: Exception) {
            // Local Room record remains safe if cloud sync is unavailable.
        }

        id
    }

    suspend fun deleteScan(id: Long) = withContext(Dispatchers.IO) {
        diseaseScanDao.deleteById(id)
    }

    /**
     * Main disease detection pipeline.
     * Every captured or selected plant image is classified by the bundled
     * TensorFlow Lite model first. the chatbot is intentionally NOT used for
     * disease classification, so the prediction shown to the user comes from
     * the project's own trained model and also works offline.
     */
    suspend fun analyzePlantImage(
        bitmap: Bitmap,
        selectedCrop: DiseaseCrop = DiseaseCrop.WHEAT
    ): PlantDiseaseResult = withContext(Dispatchers.IO) {
        when (selectedCrop) {
            DiseaseCrop.WHEAT -> {
                val classifier = wheatClassifier
                    ?: throw IllegalStateException("Wheat disease model is not available")
                if (!classifier.isReady()) {
                    throw IllegalStateException("Wheat disease model could not be loaded")
                }
                classifier.classify(bitmap)
                    ?: createUncertainResult(selectedCrop)
            }
            DiseaseCrop.CORN -> {
                val classifier = cornClassifier
                    ?: throw IllegalStateException("Corn disease model is not available")
                if (!classifier.isReady()) {
                    throw IllegalStateException("Corn disease model could not be loaded. Add model/corn_disease.tflite and model/corn_labels.txt to app/src/main/assets.")
                }
                classifier.classify(bitmap)
                    ?: createUncertainResult(selectedCrop)
            }
        }
    }

    fun close() {
        wheatClassifier?.close()
        cornClassifier?.close()
    }

    private fun createUncertainResult(selectedCrop: DiseaseCrop): PlantDiseaseResult =
        PlantDiseaseResult(
            cropName = if (selectedCrop == DiseaseCrop.CORN) "Corn / مکئی" else "Wheat / گندم",
            diseaseNameEn = "Image not recognized with sufficient confidence",
            diseaseNameUr = "تصویر قابلِ اعتماد اعتماد کے ساتھ شناخت نہیں ہو سکی",
            confidencePercent = 0,
            isHealthy = false,
            severityLevel = "Unknown",
            symptomsEn = "The model could not confidently classify this image as one of its trained crop disease classes.",
            symptomsUr = "ماڈل اس تصویر کو اپنی تربیت یافتہ فصل یا بیماری کی کلاسز میں قابلِ اعتماد طور پر شناخت نہیں کر سکا۔",
            chemicalTreatmentEn = "Do not apply treatment based on this scan. Capture a clear close-up of a single leaf.",
            chemicalTreatmentUr = "اس سکین کی بنیاد پر علاج نہ کریں۔ ایک واضح پتے کی قریبی تصویر دوبارہ لیں۔",
            organicPreventionEn = "Use good natural light and keep the leaf centered and in focus.",
            organicPreventionUr = "اچھی قدرتی روشنی استعمال کریں اور پتے کو درمیان میں واضح فوکس کے ساتھ رکھیں۔",
            advisoryNoteEn = "This result was rejected by confidence and prediction-margin validation, not replaced with a predefined disease.",
            advisoryNoteUr = "یہ نتیجہ اعتماد اور پیش گوئی کے فرق کی جانچ کی وجہ سے مسترد ہوا ہے، اسے کسی پہلے سے طے شدہ بیماری سے تبدیل نہیں کیا گیا۔",
            isPlantImage = false
        )

    private fun DiseaseScanEntity.toModel(): PlantDiseaseResult {
        return PlantDiseaseResult(
            id = id,
            cropName = cropName,
            diseaseNameEn = diseaseNameEn,
            diseaseNameUr = diseaseNameUr,
            confidencePercent = confidencePercent,
            isHealthy = isHealthy,
            severityLevel = severityLevel,
            symptomsEn = symptoms,
            symptomsUr = symptomsUr,
            chemicalTreatmentEn = chemicalTreatment,
            chemicalTreatmentUr = chemicalTreatmentUr,
            organicPreventionEn = organicPrevention,
            organicPreventionUr = organicPreventionUr,
            advisoryNoteEn = advisoryNote,
            advisoryNoteUr = advisoryNoteUr,
            imagePathOrUri = imageUriOrPath,
            timestamp = timestamp
        )
    }
}
