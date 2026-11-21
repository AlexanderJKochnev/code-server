package com.example.photouploader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
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
    private var photoFile: File? = null

    private val tagId: Int? by lazy { intent.getIntExtra("id", -1).takeIf { it >= 0 } }
    private val tagName: String by lazy { intent.getStringExtra("display_name") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        settings = SettingsManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // === Клик по результату — скрыть превью и вернуться к камере ===
        findViewById<View>(R.id.ivResult).setOnClickListener {
            it.visibility = View.GONE
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
                val metadata = ExifHelper.extractMetadata(file)

                val api = RetrofitClient.create(settings)
                val filePart = MultipartBody.Part.createFormData(
                    "file", "image.webp",
                    webp.toRequestBody("image/webp".toMediaTypeOrNull())
                )
                val idPart = tagId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val namePart = tagName.toRequestBody("text/plain".toMediaTypeOrNull())
                val metaPart = metadata.toRequestBody("application/json".toMediaTypeOrNull())

                val responseBody = api.uploadImage(
                    settings.uploadUrl, tagName, filePart, idPart, namePart, metaPart
                )
                val imageBytes = responseBody.bytes()
                responseBody.close()

                val bitmap = android.graphics.BitmapFactory
                    .decodeByteArray(imageBytes, 0, imageBytes.size)

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                        findViewById<ImageView>(R.id.ivResult).setImageBitmap(bitmap)
                        findViewById<View>(R.id.ivResult).visibility = View.VISIBLE
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
                        "Upload error: ${e.message}",
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