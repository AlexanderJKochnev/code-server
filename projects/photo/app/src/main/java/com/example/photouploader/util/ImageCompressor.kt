package com.example.photouploader.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageCompressor {

    private const val MAX_LONG_EDGE = 2048
    private const val TARGET_MAX_BYTES = 2 * 1024 * 1024 // 2 MB

    fun compressToWebp(inputBytes: ByteArray): ByteArray {
        // 1. Decode with downsampling if needed
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size, options)
        var sampleSize = 1
        while (max(options.outWidth, options.outHeight) / sampleSize > MAX_LONG_EDGE * 2) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size, decodeOptions)
            ?: throw IllegalArgumentException("Cannot decode image")

        // 2. Resize to target long edge
        val longEdge = max(bitmap.width, bitmap.height)
        if (longEdge > MAX_LONG_EDGE) {
            val scale = MAX_LONG_EDGE.toFloat() / longEdge
            val newW = (bitmap.width * scale).toInt()
            val newH = (bitmap.height * scale).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        }

        // 3. Binary search quality for WEBP_LOSSY
        var low = 10
        var high = 100
        var best: ByteArray? = null

        while (low <= high) {
            val mid = (low + high) / 2
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, mid, out)
            val bytes = out.toByteArray()
            if (bytes.size <= TARGET_MAX_BYTES) {
                best = bytes
                low = mid + 1 // try higher quality
            } else {
                high = mid - 1
            }
        }

        bitmap.recycle()
        return best ?: run {
            // Fallback: minimum quality
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 10, out)
            out.toByteArray()
        }
    }
}