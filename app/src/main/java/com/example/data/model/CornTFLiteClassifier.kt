package com.example.data.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * TensorFlow Lite inference engine for the separately trained Corn MobileNetV2 model.
 * IMPORTANT: the exported Keras model already contains MobileNetV2 preprocess_input.
 * Therefore this Android wrapper must feed raw float pixels in the model's declared
 * input range [0, 255] and must NOT apply the [-1, 1] transform a second time.
 */
class CornTFLiteClassifier(private val context: Context) {

    companion object {
        private const val TAG = "CornTFLiteClassifier"
        private const val INPUT_SIZE = 224
        private const val CHANNELS = 3
        private const val CLASS_COUNT = 5
    }

    private var interpreter: Interpreter? = null
    private var isModelLoaded = false

    private val labels: List<String> by lazy {
        try {
            context.assets.open("model/corn_labels.txt").bufferedReader().useLines { lines ->
                lines.map { it.trim() }.filter { it.isNotEmpty() }.toList()
            }.also { loaded ->
                require(loaded.size == CLASS_COUNT) {
                    "Corn label file must contain exactly $CLASS_COUNT labels; found ${loaded.size}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not load model/corn_labels.txt", e)
            emptyList()
        }
    }

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetFileDescriptor = context.assets.openFd("model/corn_disease.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val buffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )

            val options = Interpreter.Options().apply { setNumThreads(4) }
            interpreter = Interpreter(buffer, options)

            val inputShape = interpreter!!.getInputTensor(0).shape()
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            val inputType = interpreter!!.getInputTensor(0).dataType()
            val outputType = interpreter!!.getOutputTensor(0).dataType()

            require(inputShape.contentEquals(intArrayOf(1, INPUT_SIZE, INPUT_SIZE, CHANNELS))) {
                "Unexpected Corn input shape: ${inputShape.contentToString()}"
            }
            require(outputShape.contentEquals(intArrayOf(1, CLASS_COUNT))) {
                "Unexpected Corn output shape: ${outputShape.contentToString()}"
            }
            require(inputType == org.tensorflow.lite.DataType.FLOAT32) {
                "Unexpected Corn input type: $inputType"
            }
            require(outputType == org.tensorflow.lite.DataType.FLOAT32) {
                "Unexpected Corn output type: $outputType"
            }
            require(labels.size == CLASS_COUNT) { "Corn labels are unavailable or invalid" }

            isModelLoaded = true
            Log.d(TAG, "Corn TFLite model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Could not initialize Corn TFLite model: ${e.message}", e)
            interpreter?.close()
            interpreter = null
            isModelLoaded = false
        }
    }

    fun isReady(): Boolean = isModelLoaded && interpreter != null

    fun classify(bitmap: Bitmap): PlantDiseaseResult? {
        val tflite = interpreter ?: return null
        if (labels.size != CLASS_COUNT) return null
        return try {
            val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
            val byteBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * CHANNELS * 4).order(ByteOrder.nativeOrder())
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            for (pixel in pixels) {
                byteBuffer.putFloat(((pixel shr 16) and 0xFF).toFloat())
                byteBuffer.putFloat(((pixel shr 8) and 0xFF).toFloat())
                byteBuffer.putFloat((pixel and 0xFF).toFloat())
            }
            if (resized !== bitmap) resized.recycle()
            val output = Array(1) { FloatArray(CLASS_COUNT) }
            tflite.run(byteBuffer, output)
            val probabilities = normalizeToProbabilities(output[0])
            val order=probabilities.indices.sortedByDescending { probabilities[it] }
            val best=order[0]; val confidence=probabilities[best]; val second=probabilities[order.getOrElse(1){best}]
            val margin=confidence-second
            Log.d(TAG, "Corn probabilities=${probabilities.joinToString()} confidence=$confidence margin=$margin")
            if (confidence < 0.60f || margin < 0.12f) return null
            mapClassToResult(best, (confidence*100f).toInt().coerceIn(0,100))
        } catch (e: Exception) { Log.e(TAG, "Corn inference error: ${e.message}", e); null }
    }


    private fun normalizeToProbabilities(values: FloatArray): FloatArray {
        val sum = values.sum()
        val looksLikeProbabilities = values.all { it in 0f..1f } && sum in 0.99f..1.01f
        if (looksLikeProbabilities) return values

        val max = values.maxOrNull() ?: 0f
        val exps = FloatArray(values.size) { i -> exp((values[i] - max).toDouble()).toFloat() }
        val expSum = exps.sum().coerceAtLeast(Float.MIN_VALUE)
        return FloatArray(values.size) { i -> exps[i] / expSum }
    }

    private fun mapClassToResult(classIndex: Int, confidence: Int): PlantDiseaseResult {
        return when (classIndex) {
            0 -> PlantDiseaseResult(
                cropName = "Corn / مکئی",
                diseaseNameEn = "Corn Leaf Blight",
                diseaseNameUr = "مکئی کا لیف بلائٹ",
                confidencePercent = confidence,
                isHealthy = false,
                severityLevel = if (confidence >= 85) "High" else "Moderate",
                symptomsEn = "Brown or tan leaf lesions that can enlarge and merge, causing affected leaf tissue to dry and die.",
                symptomsUr = "پتوں پر بھورے یا خاکستری دھبے بنتے ہیں جو پھیل کر آپس میں مل سکتے ہیں اور متاثرہ حصہ خشک ہو جاتا ہے۔",
                chemicalTreatmentEn = "Remove heavily infected plant debris where practical and use a locally registered fungicide only according to its label and local agricultural guidance.",
                chemicalTreatmentUr = "زیادہ متاثرہ پودوں کی باقیات مناسب طریقے سے ہٹا دیں اور صرف مقامی طور پر منظور شدہ فنجی سائیڈ لیبل اور زرعی ماہر کی ہدایت کے مطابق استعمال کریں۔",
                organicPreventionEn = "Use clean seed, maintain good field sanitation, avoid unnecessary leaf wetness, and prefer resistant or tolerant varieties when available.",
                organicPreventionUr = "صاف بیج استعمال کریں، کھیت کی صفائی رکھیں، پتوں کو غیر ضروری طور پر گیلا رکھنے سے بچیں اور دستیاب مزاحم اقسام کو ترجیح دیں۔",
                advisoryNoteEn = "Monitor lower leaves regularly and seek local agronomic advice if lesions are spreading quickly or reaching upper leaves.",
                advisoryNoteUr = "نچلے پتوں کا باقاعدگی سے معائنہ کریں۔ اگر دھبے تیزی سے پھیلیں یا اوپر کے پتوں تک پہنچیں تو مقامی زرعی ماہر سے مشورہ کریں۔"
            )
            1 -> PlantDiseaseResult(
                cropName = "Corn / مکئی",
                diseaseNameEn = "Common Rust",
                diseaseNameUr = "کامن رسٹ",
                confidencePercent = confidence,
                isHealthy = false,
                severityLevel = if (confidence >= 85) "High" else "Moderate",
                symptomsEn = "Small reddish-brown to cinnamon-colored rust pustules appear on leaf surfaces and may release powdery spores.",
                symptomsUr = "پتوں کی سطح پر سرخی مائل بھورے یا دارچینی رنگ کے چھوٹے ابھرے ہوئے رسٹ کے دانے بنتے ہیں جن سے پاؤڈر نما بیج نما ذرات نکل سکتے ہیں۔",
                chemicalTreatmentEn = "Maintain crop monitoring and use a locally registered fungicide only when needed and strictly according to the product label and local agricultural advice.",
                chemicalTreatmentUr = "فصل کی نگرانی جاری رکھیں اور ضرورت کے وقت صرف مقامی طور پر منظور شدہ فنجی سائیڈ کو پروڈکٹ لیبل اور مقامی زرعی مشورے کے مطابق استعمال کریں۔",
                organicPreventionEn = "Prefer resistant varieties, maintain balanced crop nutrition, and remove volunteer or heavily infected plants where practical.",
                organicPreventionUr = "مزاحم اقسام کو ترجیح دیں، متوازن غذائیت فراہم کریں اور جہاں ممکن ہو خود رو یا بہت زیادہ متاثرہ پودوں کو ختم کریں۔",
                advisoryNoteEn = "Rust develops readily under favorable moist conditions. Recheck the crop after humid or rainy weather.",
                advisoryNoteUr = "رسٹ موزوں مرطوب حالات میں تیزی سے بڑھ سکتا ہے۔ زیادہ نمی یا بارش کے بعد فصل کا دوبارہ معائنہ کریں۔"
            )
            2 -> PlantDiseaseResult(
                cropName = "Corn / مکئی",
                diseaseNameEn = "Gray Leaf Spot",
                diseaseNameUr = "گرے لیف اسپاٹ",
                confidencePercent = confidence,
                isHealthy = false,
                severityLevel = if (confidence >= 85) "High" else "Moderate",
                symptomsEn = "Gray to tan rectangular lesions develop on leaves, often expanding along the leaf surface and reducing healthy green tissue.",
                symptomsUr = "پتوں پر سرمئی سے خاکستری مستطیل دھبے بنتے ہیں جو پھیل کر سبز اور صحت مند پتوں کے حصے کو کم کر سکتے ہیں۔",
                chemicalTreatmentEn = "Improve field monitoring and use a locally registered fungicide when justified, following its label and local agricultural recommendations.",
                chemicalTreatmentUr = "فصل کی نگرانی بہتر کریں اور ضرورت ثابت ہونے پر مقامی طور پر منظور شدہ فنجی سائیڈ لیبل اور زرعی سفارشات کے مطابق استعمال کریں۔",
                organicPreventionEn = "Use resistant or tolerant hybrids when available, rotate crops where suitable, and manage infected crop residue to reduce disease carryover.",
                organicPreventionUr = "دستیاب مزاحم اقسام استعمال کریں، مناسب جگہ پر فصلوں کی تبدیلی کریں اور متاثرہ باقیات کو سنبھالیں تاکہ بیماری اگلی فصل میں کم منتقل ہو۔",
                advisoryNoteEn = "Early detection is useful because expanding lesions can reduce photosynthetic leaf area. Inspect the crop regularly.",
                advisoryNoteUr = "ابتدائی شناخت مفید ہے کیونکہ پھیلتے ہوئے دھبے پتوں کے فعال سبز حصے کو کم کر سکتے ہیں۔ فصل کا باقاعدگی سے معائنہ کریں۔"
            )
            3 -> PlantDiseaseResult(
                cropName = "Corn / مکئی",
                diseaseNameEn = "Healthy Corn",
                diseaseNameUr = "صحت مند مکئی",
                confidencePercent = confidence,
                isHealthy = true,
                severityLevel = "None",
                symptomsEn = "Healthy green leaves with no clear disease lesions, rust pustules, or major insect feeding damage detected by the model.",
                symptomsUr = "پتے صحت مند سبز ہیں اور ماڈل کے مطابق بیماری کے واضح دھبے، رسٹ کے دانے یا کیڑوں کے نمایاں نقصان کی علامات نہیں ملیں۔",
                chemicalTreatmentEn = "No disease-control chemical is indicated from this scan. Continue normal crop monitoring and follow local crop-management recommendations.",
                chemicalTreatmentUr = "اس سکین کی بنیاد پر بیماری کے خلاف کسی کیمیکل کی ضرورت ظاہر نہیں ہوتی۔ فصل کی معمول کے مطابق نگرانی اور مقامی زرعی سفارشات پر عمل جاری رکھیں۔",
                organicPreventionEn = "Maintain balanced irrigation and nutrition, keep the field clean, and scout regularly for early signs of disease or pests.",
                organicPreventionUr = "متوازن آبپاشی اور غذائیت برقرار رکھیں، کھیت صاف رکھیں اور بیماری یا کیڑوں کی ابتدائی علامات کے لیے باقاعدگی سے نگرانی کریں۔",
                advisoryNoteEn = "A healthy prediction does not guarantee that every part of the field is disease-free. Continue routine scouting.",
                advisoryNoteUr = "صحت مند پیش گوئی کا مطلب یہ نہیں کہ کھیت کا ہر حصہ بیماری سے پاک ہے۔ معمول کے مطابق نگرانی جاری رکھیں۔"
            )
            4 -> PlantDiseaseResult(
                cropName = "Corn / مکئی",
                diseaseNameEn = "Insect Damage",
                diseaseNameUr = "کیڑوں سے نقصان",
                confidencePercent = confidence,
                isHealthy = false,
                severityLevel = if (confidence >= 85) "High" else "Moderate",
                symptomsEn = "Visible feeding injury such as holes, chewing marks, scraped tissue, or other leaf damage consistent with insect activity.",
                symptomsUr = "پتوں پر سوراخ، کترنے کے نشان، رگڑا ہوا ٹشو یا دیگر نقصان نظر آ سکتا ہے جو کیڑوں کی سرگرمی سے مطابقت رکھتا ہے۔",
                chemicalTreatmentEn = "Inspect plants and identify the pest before treatment. If control is needed, use only a locally registered product according to its label and local agricultural guidance.",
                chemicalTreatmentUr = "سپرے سے پہلے پودوں کا معائنہ کر کے کیڑے کی شناخت کریں۔ اگر کنٹرول ضروری ہو تو صرف مقامی طور پر منظور شدہ دوا لیبل اور زرعی ماہر کی ہدایت کے مطابق استعمال کریں۔",
                organicPreventionEn = "Scout regularly, remove heavily damaged leaves where practical, maintain field sanitation, and encourage beneficial insects.",
                organicPreventionUr = "باقاعدگی سے نگرانی کریں، جہاں ممکن ہو زیادہ متاثرہ پتے ہٹا دیں، کھیت کی صفائی رکھیں اور مفید کیڑوں کی افزائش کی حوصلہ افزائی کریں۔",
                advisoryNoteEn = "Do not spray based on image classification alone. Confirm the pest and assess damage level before choosing a control method.",
                advisoryNoteUr = "صرف تصویر کی شناخت کی بنیاد پر سپرے نہ کریں۔ کنٹرول کا طریقہ منتخب کرنے سے پہلے کیڑے کی شناخت اور نقصان کی شدت کی تصدیق کریں۔"
            )
            else -> error("Unsupported Corn class index: $classIndex")
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
    }
}
