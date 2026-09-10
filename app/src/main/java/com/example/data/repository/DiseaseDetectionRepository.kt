package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.DiseaseScanDao
import com.example.data.local.DiseaseScanEntity
import com.example.data.model.DiseaseCrop
import com.example.data.model.PlantDiseaseResult
import com.example.data.model.CornTFLiteClassifier
import com.example.data.model.PlantImageGate
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
            val cloudEntity = entity.copy(id = id)
            // Mark it synced so the retry pass skips it on the next launch.
            if (supabaseSync.syncDiseaseDetection(cloudEntity)) {
                diseaseScanDao.setScanSynced(id, true)
            }
            if (cloudEntity.imageUriOrPath.isNotBlank() && !cloudEntity.imageUriOrPath.startsWith("content://")) {
                supabaseSync.uploadDiseaseImage(cloudEntity.imageUriOrPath, "db:$id", cloudEntity.cropName)
            }
        } catch (_: Exception) {
            // Local Room record remains safe if cloud sync is unavailable.
        }

        id
    }

    suspend fun deleteScan(id: Long) = withContext(Dispatchers.IO) {
        val existing = diseaseScanDao.getScanById(id)
        diseaseScanDao.deleteById(id)

        // Remove the local image file too, otherwise deleted scans keep filling storage.
        existing?.imageUriOrPath
            ?.takeIf { it.isNotBlank() && !it.startsWith("content://") }
            ?.let { path -> runCatching { java.io.File(path).delete() } }

        // Delete the cloud copy as well; previously the row stayed in Supabase
        // forever and reappeared on the next restore.
        runCatching { supabaseSync.deleteDiseaseDetection(id) }
        Unit
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
        // A disease classifier only knows its own trained classes; shown any
        // out-of-distribution photo it will still return one of them with a
        // usable-looking confidence. The visual pre-filter rejects obvious
        // non-plant images before they ever reach the model.
        if (!PlantImageGate.isLikelyPlant(bitmap)) {
            return@withContext createNonPlantResult(selectedCrop)
        }

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

    private fun createNonPlantResult(selectedCrop: DiseaseCrop): PlantDiseaseResult =
        PlantDiseaseResult(
            cropName = if (selectedCrop == DiseaseCrop.CORN) "Corn / مکئی" else "Wheat / گندم",
            diseaseNameEn = "No plant leaf detected in this photo",
            diseaseNameUr = "اس تصویر میں پودے کا پتہ نظر نہیں آیا",
            confidencePercent = 0,
            isHealthy = false,
            severityLevel = "Unknown",
            symptomsEn = "The image does not show the colour and texture pattern of plant foliage, so it was not sent to the disease model.",
            symptomsUr = "تصویر میں پودے کے پتوں جیسا رنگ اور بناوٹ موجود نہیں، اس لیے اسے بیماری کے ماڈل تک نہیں بھیجا گیا۔",
            chemicalTreatmentEn = "No diagnosis was produced, so no treatment should be applied.",
            chemicalTreatmentUr = "کوئی تشخیص نہیں ہوئی، اس لیے کوئی علاج نہ کریں۔",
            organicPreventionEn = "Photograph a single leaf close up, filling most of the frame, in good natural light.",
            organicPreventionUr = "ایک پتے کی قریبی تصویر لیں جو زیادہ تر فریم بھرے، اچھی قدرتی روشنی میں۔",
            advisoryNoteEn = "Rejected by the plant pre-filter before model inference, so no disease class was guessed for a non-plant image.",
            advisoryNoteUr = "ماڈل چلنے سے پہلے ہی پری فلٹر نے اس تصویر کو مسترد کر دیا، اس لیے کوئی بیماری کا اندازہ نہیں لگایا گیا۔",
            isPlantImage = false
        )

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
