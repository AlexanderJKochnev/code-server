package com.example.photosender // Замените на ваш package

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var ivPreview: ImageView
    private lateinit var etDescription: TextInputEditText
    private lateinit var btnSend: Button

    private var rawPhotoFile: File? = null
    private var currentPhotoUri: Uri? = null
    private val client = OkHttpClient()

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && currentPhotoUri != null) {
            ivPreview.setImageURI(null)
            ivPreview.setImageURI(currentPhotoUri)
            btnSend.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPreview = findViewById(R.id.ivPreview)
        etDescription = findViewById(R.id.etDescription)
        btnSend = findViewById(R.id.btnSend)

        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            showSettingsDialog()
        }

        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            dispatchTakePictureIntent()
        }

        btnSend.setOnClickListener {
            processAndUpload()
        }
    }

    // Окно настроек: URL, API-ключ и лимит размера в МБ
    private fun showSettingsDialog() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)

        val etBaseUrl = view.findViewById<TextInputEditText>(R.id.etBaseUrl)
        val etApiKey = view.findViewById<TextInputEditText>(R.id.etApiKey)
        val etMaxSizeMb = view.findViewById<TextInputEditText>(R.id.etMaxSizeMb)

        etBaseUrl.setText(prefs.getString("base_url", "https://api.test.abc8888.ru/api/upload_image"))
        etApiKey.setText(prefs.getString("api_key", ""))
        etMaxSizeMb.setText(prefs.getFloat("max_size_mb", 1.0f).toString())

        AlertDialog.Builder(this)
            .setTitle("Настройки сервера и сжатия")
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val url = etBaseUrl.text.toString().trim()
                val key = etApiKey.text.toString().trim()
                val maxMb = etMaxSizeMb.text.toString().toFloatOrNull() ?: 1.0f

                prefs.edit()
                    .putString("base_url", url)
                    .putString("api_key", key)
                    .putFloat("max_size_mb", maxMb.coerceAtLeast(0.1f))
                    .apply()

                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = File(cacheDir, "raw_photo_${System.currentTimeMillis()}.jpg")
        rawPhotoFile = photoFile
        currentPhotoUri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(currentPhotoUri)
    }

    // Сжатие в фоне и отправка
    private fun processAndUpload() {
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val baseUrlStr = prefs.getString("base_url", "") ?: ""
        val apiKey = prefs.getString("api_key", "") ?: ""
        val maxSizeMb = prefs.getFloat("max_size_mb", 1.0f)

        val parsedUrl = baseUrlStr.toHttpUrlOrNull()
        if (parsedUrl == null) {
            Toast.makeText(this, "Некорректный URL в настройках", Toast.LENGTH_SHORT).show()
            return
        }

        val sourceFile = rawPhotoFile ?: return
        val tagText = etDescription.text.toString().trim()

        btnSend.isEnabled = false
        btnSend.text = "Сжатие и отправка..."

        lifecycleScope.launch {
            try {
                // 1. Сжимаем в WebP в фоновом потоке (макс. сторона 1600px под планшеты)
                val webpFile = withContext(Dispatchers.Default) {
                    compressToWebp(sourceFile, maxSidePx = 1600, maxBytes = (maxSizeMb * 1024 * 1024).toLong())
                }

                // 2. Собираем URL с query-параметром ?tag=...
                val finalUrl = parsedUrl.newBuilder()
                    .apply {
                        if (tagText.isNotEmpty()) {
                            addQueryParameter("tag", tagText)
                        }
                    }
                    .build()

                // 3. Формируем multipart-запрос как в curl (-F 'file=@...;type=image/webp')
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file",
                        webpFile.name,
                        webpFile.asRequestBody("image/webp".toMediaType())
                    )
                    .build()

                val requestBuilder = Request.Builder()
                    .url(finalUrl)
                    .addHeader("accept", "*/*")
                    .post(requestBody)

                if (apiKey.isNotEmpty()) {
                    requestBuilder.addHeader("X-API-Key", apiKey)
                }

                // 4. Отправляем
                sendRequest(requestBuilder.build(), webpFile)

            } catch (e: Exception) {
                btnSend.isEnabled = true
                btnSend.text = "Отправить на сервер"
                Toast.makeText(this@MainActivity, "Ошибка обработки: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendRequest(request: Request, tempWebpFile: File) {
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                tempWebpFile.delete()
                runOnUiThread {
                    btnSend.isEnabled = true
                    btnSend.text = "Отправить на сервер"
                    Toast.makeText(this@MainActivity, "Ошибка сети: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val code = response.code
                val isSuccessful = response.isSuccessful
                response.close()
                tempWebpFile.delete() // Удаляем временный webp после отправки

                runOnUiThread {
                    btnSend.isEnabled = true
                    btnSend.text = "Отправить на сервер"
                    if (isSuccessful) {
                        rawPhotoFile?.delete()
                        Toast.makeText(this@MainActivity, "Успешно загружено!", Toast.LENGTH_SHORT).show()
                        etDescription.text?.clear()
                        ivPreview.setImageResource(android.R.drawable.ic_menu_camera)
                        btnSend.isEnabled = false
                    } else {
                        Toast.makeText(this@MainActivity, "Ошибка сервера: HTTP $code", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    // Функция ресайза (под экран планшета), коррекции поворота и сжатия в WebP под заданный вес
    private fun compressToWebp(sourceFile: File, maxSidePx: Int, maxBytes: Long): File {
        // Определяем размеры без загрузки в память
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, options)

        var inSampleSize = 1
        val longestSide = maxOf(options.outWidth, options.outHeight)
        while (longestSide / inSampleSize > maxSidePx * 2) {
            inSampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
        var bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions)
            ?: throw IOException("Не удалось прочитать изображение")

        // Исправляем поворот EXIF (чтобы фото не легло на бок)
        bitmap = rotateBitmapIfRequired(bitmap, sourceFile.absolutePath)

        // Точное масштабирование до maxSidePx (1600px)
        val currentLongest = maxOf(bitmap.width, bitmap.height)
        if (currentLongest > maxSidePx) {
            val scale = maxSidePx.toFloat() / currentLongest
            val newW = (bitmap.width * scale).toInt()
            val newH = (bitmap.height * scale).toInt()
            val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            if (scaled != bitmap) bitmap.recycle()
            bitmap = scaled
        }

        // Выбираем формат сжатия WebP в зависимости от версии Android
        val webpFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        // Подбираем качество, чтобы уложиться в maxBytes (по умолчанию 1 МБ)
        val outputStream = ByteArrayOutputStream()
        var quality = 88
        do {
        outputStream.reset()
        bitmap.compress(webpFormat, quality, outputStream)
        quality -= 8
        } while (outputStream.size() > maxBytes && quality >= 30)
        bitmap.recycle()
        val outFile = File(cacheDir, "upload_${System.currentTimeMillis()}.webp")
        FileOutputStream(outFile).use { fos ->
        fos.write(outputStream.toByteArray())
        }
        return outFile
        }
        private fun rotateBitmapIfRequired(bitmap: Bitmap, path: String): Bitmap {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
        }
        }