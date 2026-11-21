package com.example.photouploader.util

import androidx.exifinterface.media.ExifInterface
import org.json.JSONObject
import java.io.File

object ExifHelper {
    fun extractMetadata(file: File): String {
        val exif = ExifInterface(file.absolutePath)
        val json = JSONObject()
        json.put("datetime", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL) ?: "")
        json.put("make", exif.getAttribute(ExifInterface.TAG_MAKE) ?: "")
        json.put("model", exif.getAttribute(ExifInterface.TAG_MODEL) ?: "")
        json.put("width", exif.getAttribute(ExifInterface.TAG_PIXEL_X_DIMENSION) ?: "")
        json.put("height", exif.getAttribute(ExifInterface.TAG_PIXEL_Y_DIMENSION) ?: "")
        val latLong = exif.latLong
        if (latLong != null) {
            json.put("latitude", latLong[0])
            json.put("longitude", latLong[1])
        }
        return json.toString()
    }
}