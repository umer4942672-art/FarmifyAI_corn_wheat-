package com.example

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.model.PlantDiseaseModelEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlantDiseaseModelTest {

    @Test
    fun `test healthy leaf classification on green bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Fill with lush green
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                bitmap.setPixel(x, y, Color.rgb(30, 180, 40))
            }
        }

        val result = PlantDiseaseModelEngine.classifyImage(bitmap)
        assertNotNull(result)
        assertTrue(result.isHealthy)
        assertTrue(result.confidencePercent >= 88)
    }

    @Test
    fun `test necrotic blighted leaf classification`() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // Fill with dark brown / necrotic lesion pixels
        for (x in 0 until 100) {
            for (y in 0 until 100) {
                if (x % 3 == 0) {
                    bitmap.setPixel(x, y, Color.rgb(85, 45, 20))
                } else {
                    bitmap.setPixel(x, y, Color.rgb(100, 120, 40))
                }
            }
        }

        val result = PlantDiseaseModelEngine.classifyImage(bitmap)
        assertNotNull(result)
        assertFalse(result.isHealthy)
        assertTrue(result.confidencePercent >= 80)
        assertNotNull(result.chemicalTreatmentEn)
        assertNotNull(result.symptomsUr)
    }

    @Test
    fun `test disease dataset contains full crop coverage`() {
        val dataset = PlantDiseaseModelEngine.diseaseDataset
        assertTrue(dataset.size >= 10)

        val cropNames = dataset.map { it.cropName }
        assertTrue(cropNames.any { it.contains("Wheat") })
        assertTrue(cropNames.any { it.contains("Cotton") })
        assertTrue(cropNames.any { it.contains("Rice") })
        assertTrue(cropNames.any { it.contains("Tomato") })
        assertTrue(cropNames.any { it.contains("Sugarcane") })
        assertTrue(cropNames.any { it.contains("Potato") })
    }
}
