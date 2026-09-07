package com.example.data.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * TensorFlow Lite On-Device Inference Engine for Wheat & Plant Disease Detection.
 * Loaded from assets/model/plant_disease.tflite with labels:
 * 0: Healthy
 * 1: septoria
 * 2: stripe_rust
 */
class TFLitePlantClassifier(private val context: Context) {

    private var interpreter: Interpreter? = null
    private var isModelLoaded: Boolean = false
    private val labels: List<String> by lazy {
        try {
            context.assets.open("model/labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }.ifEmpty { listOf("Healthy", "septoria", "stripe_rust") }
        } catch (e: Exception) {
            listOf("Healthy", "septoria", "stripe_rust")
        }
    }
    private val inputImageSize = 224 // standard vision classifier input (e.g. MobileNet / EfficientNet)

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val possiblePaths = listOf(
                "model/plant_disease.tflite",
                "plant_disease.tflite"
            )
            for (path in possiblePaths) {
                try {
                    val buffer = context.assets.openFd(path).use { assetFileDescriptor ->
                        FileInputStream(assetFileDescriptor.fileDescriptor).channel.use { fileChannel ->
                            fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
                        }
                    }
                    val options = Interpreter.Options().apply { setNumThreads(4) }
                    interpreter = Interpreter(buffer, options)
                    isModelLoaded = true
                    Log.d("TFLitePlantClassifier", "Successfully loaded TFLite model from asset: $path")
                    break
                } catch (e: Exception) {
                    // Try next path
                }
            }
        } catch (e: Exception) {
            Log.w("TFLitePlantClassifier", "Could not initialize TFLite model: ${e.message}")
            isModelLoaded = false
        }
    }

    fun isReady(): Boolean = isModelLoaded && interpreter != null

    /**
     * Runs inference on the given plant leaf bitmap
     */
    fun classify(bitmap: Bitmap): PlantDiseaseResult? {
        val tflite = interpreter ?: return null
        return try {
            val inputTensor = tflite.getInputTensor(0)
            val shape = inputTensor.shape()
            if (!shape.contentEquals(intArrayOf(1, 224, 224, 3)) || inputTensor.dataType() != org.tensorflow.lite.DataType.FLOAT32) {
                Log.e("TFLitePlantClassifier", "Unsupported Wheat input tensor: ${shape.contentToString()} ${inputTensor.dataType()}")
                return null
            }
            val resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val buffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())
            val pixels = IntArray(224 * 224)
            resized.getPixels(pixels, 0, 224, 0, 0, 224, 224)
            for (p in pixels) {
                // Model export contains preprocessing; feed raw RGB [0,255].
                buffer.putFloat(((p shr 16) and 0xFF).toFloat())
                buffer.putFloat(((p shr 8) and 0xFF).toFloat())
                buffer.putFloat((p and 0xFF).toFloat())
            }
            if (resized !== bitmap) resized.recycle()
            val outShape=tflite.getOutputTensor(0).shape()
            if (outShape.size != 2 || outShape[0] != 1 || outShape[1] != labels.size) return null
            val output=Array(1){FloatArray(labels.size)}
            tflite.run(buffer, output)
            val probs=normalizeToProbabilities(output[0])
            val order=probs.indices.sortedByDescending { probs[it] }
            val best=order[0]; val second=probs[order.getOrElse(1){best}]
            val confidence=probs[best]; val margin=confidence-second
            Log.d("TFLitePlantClassifier", "Wheat probabilities=${probs.joinToString()} confidence=$confidence margin=$margin")
            if (confidence < 0.60f || margin < 0.12f) return null
            mapClassToResult(labels[best], (confidence*100f).toInt().coerceIn(0,100))
        } catch (e: Exception) {
            Log.e("TFLitePlantClassifier", "Inference error: ${e.message}", e); null
        }
    }

    private fun normalizeToProbabilities(values: FloatArray): FloatArray {
        val sum=values.sum()
        if (values.all { it in 0f..1f } && sum in 0.98f..1.02f) return values
        val max=values.maxOrNull() ?: 0f
        val exps=FloatArray(values.size){ kotlin.math.exp((values[it]-max).toDouble()).toFloat() }
        val denom=exps.sum().coerceAtLeast(Float.MIN_VALUE)
        return FloatArray(values.size){ exps[it]/denom }
    }


    /**
     * Maps model labels (Healthy, septoria, stripe_rust) to detailed agronomic diagnosis and treatment.
     */
    fun mapClassToResult(label: String, confidence: Int): PlantDiseaseResult {
        return when (label.lowercase()) {
            "septoria" -> {
                PlantDiseaseResult(
                    cropName = "Wheat / گندم",
                    diseaseNameEn = "Septoria Leaf Blotch (Septoria tritici)",
                    diseaseNameUr = "سیپٹوریا پتوں کا جھلساؤ (سیپٹوریا بلاچ)",
                    confidencePercent = confidence,
                    isHealthy = false,
                    severityLevel = if (confidence > 88) "High" else "Moderate",
                    symptomsEn = "Irregular oval to rectangular light brown necrotic lesions bounded by leaf veins with characteristic tiny black fungal specks (pycnidia) inside spots.",
                    symptomsUr = "پتوں پر بیضوی اور مستطیل بھورے سوکھے دھبے جن کے اندر باریک سیاہ تل نما دانے بنتے ہیں اور نچلے پتے سوکھ جاتے ہیں۔",
                    chemicalTreatmentEn = "Spray Tilt (Propiconazole 25% EC) @ 200ml/acre or Nativo (Tebuconazole + Trifloxystrobin) @ 65g/acre or Amistar Top @ 200ml/acre in 100L water.",
                    chemicalTreatmentUr = "ٹلٹ (سنجینٹا) 200 ملی لیٹر فی ایکڑ یا نیٹیوو 65 گرام یا ایمسٹار ٹاپ 200 ملی لیٹر 100 لیٹر پانی میں ملا کر سپرے کریں۔",
                    organicPreventionEn = "Ensure 2-year crop rotation with non-cereal crops, destroy infected straw debris, avoid excessive dense planting.",
                    organicPreventionUr = "فصلوں کا ہیر پھیر کریں، پرانی گندم کی باقیات زمین میں دبا دیں اور بیج کو فنجی سائیڈ لگا کر کاشت کریں۔",
                    advisoryNoteEn = "Spreads upward via rain splashes. Spray early before infection reaches the vital flag leaf (which builds 70% of grain yield).",
                    advisoryNoteUr = "بارش کے قطروں سے بیماری اوپر چڑھتی ہے، جھنڈا پتا نکلنے سے پہلے سپرے مکمل کریں تاکہ پیداوار محفوظ رہے۔"
                )
            }
            "stripe_rust" -> {
                PlantDiseaseResult(
                    cropName = "Wheat / گندم",
                    diseaseNameEn = "Stripe Rust / Yellow Rust (Puccinia striiformis)",
                    diseaseNameUr = "گندم کی زرد کنگی (سٹرائپ رسٹ)",
                    confidencePercent = confidence,
                    isHealthy = false,
                    severityLevel = "Critical",
                    symptomsEn = "Bright yellow to orange powdery pustules aligned in distinctive linear stripes parallel to leaf veins. Yellow fungal spores rub off on fingertips.",
                    symptomsUr = "پتوں کی رگوں کے ساتھ متوازی قطاروں میں پیلی اور نارنجی رنگ کی لکیریں بنتی ہیں جن پر پاؤڈر لگا ہوتا ہے جو انگلی پر لگ جاتا ہے۔",
                    chemicalTreatmentEn = "Immediate foliar spray: Nativo (Bayer) @ 65g/acre or Tilt 250 EC (Propiconazole) @ 200ml/acre or Folicur (Tebuconazole) @ 200ml/acre.",
                    chemicalTreatmentUr = "فوری سپرے: نیٹیوو (بائر) 65 گرام فی ایکڑ یا ٹلٹ 200 ملی لیٹر یا فولیکر 200 ملی لیٹر 100 لیٹر پانی میں سپرے کریں۔",
                    organicPreventionEn = "Sow certified resistant varieties (Akbar-19, Dilkash-20, Subhani-21, Urooj-22). Apply balanced Potash (SOP/MOP) to boost leaf immunity.",
                    organicPreventionUr = "ہمیشہ منظور شدہ اقسام (اکبر-19، دلکش-20، عروج-22) کاشت کریں اور پوٹاش کھاد کا استعمال کریں۔",
                    advisoryNoteEn = "Airborne fungal spores spread rapidly in cool moist weather (10-20°C). Can cause 50%+ grain shriveling if left untreated.",
                    advisoryNoteUr = "ٹھنڈی ہواؤں میں یہ بیماری بہت تیزی سے پھیلتی ہے، 50 فیصد سے زائد پیداوار کم ہو سکتی ہے، بلا تاخیر سپرے کریں۔"
                )
            }
            else -> { // Healthy
                PlantDiseaseResult(
                    cropName = "Wheat / General Crop",
                    diseaseNameEn = "Healthy & Disease-Free Wheat Leaf",
                    diseaseNameUr = "صحت مند پودا (بیماری سے پاک)",
                    confidencePercent = confidence,
                    isHealthy = true,
                    severityLevel = "None",
                    symptomsEn = "Vibrant deep green foliage, crisp intact leaf margins, active photosynthesis, no rust stripes, spots, or fungal lesions detected.",
                    symptomsUr = "پتے گہرے سبز، تروتازہ اور زرد کنگی یا دھبوں سے بالکل پاک ہیں۔ پودے کی صحت اور بڑھوتری بہترین ہے۔",
                    chemicalTreatmentEn = "No chemical pesticide or fungicide needed. Maintain regular NPK fertigation and watering schedule.",
                    chemicalTreatmentUr = "کسی زہر یا فنجی سائیڈ سپرے کی ضرورت نہیں ہے۔ شیڈول کے مطابق کھاد اور پانی دیں۔",
                    organicPreventionEn = "Apply micronutrients (Zinc, Boron, Potassium) and bio-stimulant foliar spray to maximize grain weight.",
                    organicPreventionUr = "پودے کی مضبوطی اور دانے کا وزن بڑھانے کے لیے زنک، بوران اور پوٹاش کا استعمال جاری رکھیں۔",
                    advisoryNoteEn = "Crop is in optimal health. Re-inspect after high humidity or heavy rainfall.",
                    advisoryNoteUr = "فصل بہترین حالت میں ہے۔ بارش یا زیادہ نمی کے بعد دوبارہ معائنہ کریں۔"
                )
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
