package com.example.data.model

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.min

/**
 * Conservative visual pre-filter. This is intentionally NOT called an AI model.
 * A production-grade non-plant detector requires a separately trained Plant/NonPlant
 * TFLite model. Disease classifiers alone cannot reliably reject out-of-distribution images.
 */
object PlantImageGate {
    fun isLikelyPlant(bitmap: Bitmap): Boolean {
        if (bitmap.width < 64 || bitmap.height < 64) return false
        val size = 96
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        if (scaled !== bitmap) scaled.recycle()

        var vegetation = 0
        var naturalBrown = 0
        var textured = 0
        var valid = 0
        for (y in 1 until size - 1) for (x in 1 until size - 1) {
            val p = pixels[y * size + x]
            val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
            val mx=max(r,max(g,b)); val mn=min(r,min(g,b))
            if (mx < 25 || mx-mn < 12) continue
            valid++
            if (g > r * 1.12f && g > b * 1.05f && g > 55) vegetation++
            if (r > 65 && g > 40 && b < g * 0.9f && r > b * 1.25f) naturalBrown++
            val q=pixels[y*size+x+1]; val diff= kotlin.math.abs(r-((q shr 16) and 0xFF))+kotlin.math.abs(g-((q shr 8) and 0xFF))+kotlin.math.abs(b-(q and 0xFF))
            if (diff > 45) textured++
        }
        if (valid < pixels.size * 0.08f) return false
        val v=vegetation.toFloat()/valid; val n=naturalBrown.toFloat()/valid; val t=textured.toFloat()/valid
        return (v >= 0.10f || (v+n)>=0.28f) && t >= 0.08f
    }
}
