package com.example.photouploader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.photouploader.data.RetrofitClient
import com.example.photouploader.data.SettingsManager
import com.example.photouploader.util.ExifHelper
import com.example.photouploader.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var settings: SettingsManager

    private var cameraProvider: ProcessCameraProvider? = null
    private var photoFile: File? = null

    private var lastWebp: ByteArray? = null
    private var lastMetadata: String? = null

    private val tagId: Int? by lazy { intent.getIntExtra("id", -1).takeIf { it >= 0 } }
    private val tagName: String by lazy { intent.getStringExtra("display_name") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        settings = SettingsManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Клик по результату — скрыть результат, вернуть камеру
        findViewById<View>(R.id.ivResult).setOnClickListener {
            it.visibility = View.GONE
            findViewById<View>(R.id.llActions).visibility = View.GONE
            startCamera()
        }

        // No — ничего не отправляем, возврат на главное
        findViewById<Button>(R.id.btnNo).setOnClickListener {
            finish()
        }

        // Save — отправляем изображение на save_image
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            val webp = lastWebp ?: return@setOnClickListener
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val api = RetrofitClient.create(settings)
                    val filePart = MultipartBody.Part.createFormData(
                        "file", "image.webp",
                        webp.toRequestBody("image/webp".toMediaTypeOrNull())
                    )
                    // Новая сигнатура: url, tag, id, file
                    api.saveImage(settings.saveUrl, tagName, tagId, filePart)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CameraActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CameraActivity, "Save error: ${e.message}", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        } else {
            startCamera()
        }

        findViewById<View>(R.id.btnCapture).setOnClickListener {
            takePhoto()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, selector, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "Camera bind failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        photoFile = file
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(output, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    runOnUiThread { uploadPhoto(file) }
                }
                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@CameraActivity,
                            "Capture failed: ${exception.message}", Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
    }

    private fun uploadPhoto(file: File) {
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            val raw = file.readBytes()
            val webp = ImageCompressor.compressToWebp(raw)

            val api = RetrofitClient.create(settings)
            val filePart = MultipartBody.Part.createFormData(
                "file", "image.webp",
                webp.toRequestBody("image/webp".toMediaTypeOrNull())
            )

            val responseBody = api.uploadImage(
                settings.uploadUrl, tagName, tagId, filePart
            )
            val imageBytes = responseBody.bytes()
            responseBody.close()

            lastWebp = webp

            val bitmap = android.graphics.BitmapFactory
                .decodeByteArray(imageBytes, 0, imageBytes.size)

            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    cameraProvider?.unbindAll()
                    findViewById<ImageView>(R.id.ivResult).setImageBitmap(bitmap)
                    findViewById<View>(R.id.ivResult).visibility = View.VISIBLE
                    findViewById<View>(R.id.llActions).visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        this@CameraActivity,
                        "Сервер вернул ${imageBytes.size} байт, но это не изображение",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@CameraActivity,
                    "Upload error: ${e.message}", Toast.LENGTH_LONG
                ).show()
                }
            }
        }
    }

    private fun saveApprovedImage() {
        val webp = lastWebp ?: return
        val meta = lastMetadata ?: "{}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.create(settings)
                val filePart = MultipartBody.Part.createFormData(
                    "file", "image.webp",
                    webp.toRequestBody("image/webp".toMediaTypeOrNull())
                )
                val idPart = tagId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val namePart = tagName.toRequestBody("text/plain".toMediaTypeOrNull())
                val metaPart = meta.toRequestBody("application/json".toMediaTypeOrNull())

                val responseBody = api.saveImage(
                    settings.saveUrl, tagName, filePart, idPart, namePart, metaPart
                )
                responseBody.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CameraActivity, "Сохранено", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Save error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}