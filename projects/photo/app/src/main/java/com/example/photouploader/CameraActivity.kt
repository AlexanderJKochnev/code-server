package com.example.photouploader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
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
import com.example.photouploader.util.BitmapUtils
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
    private var lastWebp: ByteArray? = null
    private var lastImageHash: Int? = null

    private val tagId: Int? by lazy { intent.getIntExtra("id", -1).takeIf { it >= 0 } }
    private val tagName: String by lazy { intent.getStringExtra("display_name") ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.previewView)
        settings = SettingsManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<View>(R.id.ivResult).setOnClickListener {
            it.visibility = View.GONE
            findViewById<View>(R.id.llActions).visibility = View.GONE
            findViewById<View>(R.id.btnCapture).visibility = View.VISIBLE
            startCamera()
        }

        findViewById<Button>(R.id.btnNo).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveApprovedImage() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        } else {
            startCamera()
        }

        findViewById<View>(R.id.btnCapture).setOnClickListener { takePhoto() }
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
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(this, "Camera bind failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val file = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(output, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    runOnUiThread {
                        cameraProvider?.unbindAll()
                        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
                        findViewById<View>(R.id.btnCapture).visibility = View.GONE
                        uploadPhoto(file)
                    }
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

                val response = api.uploadImage(
                    settings.uploadUrl, tagName, tagId, filePart
                )

                val imageBytes = response.body()?.bytes()
                    ?: throw IllegalStateException("Empty body")
                val imageHash = response.headers()["X-Image-Hash"]?.toIntOrNull()
                response.body()?.close()

                lastWebp = webp
                lastImageHash = imageHash

                val bitmapRaw = android.graphics.BitmapFactory
                    .decodeByteArray(imageBytes, 0, imageBytes.size)
                val bitmap = bitmapRaw?.let { BitmapUtils.applyExifRotation(it, imageBytes) }

                withContext(Dispatchers.Main) {
                    findViewById<View>(R.id.progressBar).visibility = View.GONE
                    if (bitmap != null) {
                        findViewById<ImageView>(R.id.ivResult).setImageBitmap(bitmap)
                        findViewById<View>(R.id.ivResult).visibility = View.VISIBLE
                        findViewById<View>(R.id.llActions).visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this@CameraActivity,
                            "Не удалось декодировать ответ сервера",
                            Toast.LENGTH_LONG
                        ).show()
                        findViewById<View>(R.id.btnCapture).visibility = View.VISIBLE
                        startCamera()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    findViewById<View>(R.id.progressBar).visibility = View.GONE
                    findViewById<View>(R.id.btnCapture).visibility = View.VISIBLE
                    Toast.makeText(
                        this@CameraActivity, "Upload error: ${e.message}", Toast.LENGTH_LONG
                    ).show()
                    startCamera()
                }
            }
        }
    }

    private fun saveApprovedImage() {
        val hash = lastImageHash
        if (hash == null) {
            Toast.makeText(this, "Нет хэша изображения", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.create(settings)
                val response = api.saveImage(settings.saveUrl, hash)
                withContext(Dispatchers.Main) {
                    showResultDialog(response.result, response.detail)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showResultDialog(false, "Ошибка сети: ${e.message}")
                }
            }
        }
    }

    private fun showResultDialog(result: Boolean, detail: String) {
        AlertDialog.Builder(this)
            .setTitle(if (result) "Успешно" else "Ошибка")
            .setMessage(detail)
            .setPositiveButton("Ok") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}