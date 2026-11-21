package com.example.photouploader

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.photouploader.data.RetrofitClient
import com.example.photouploader.data.SettingsManager
import com.example.photouploader.data.model.LoginRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settings = SettingsManager(this)
        val etAuthUrl = findViewById<EditText>(R.id.etAuthUrl)
        val etSearchUrl = findViewById<EditText>(R.id.etSearchUrl)
        val etUploadUrl = findViewById<EditText>(R.id.etUploadUrl)
        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPass)
        val etLimit = findViewById<EditText>(R.id.etLimit)

        etAuthUrl.setText(settings.authUrl)
        etSearchUrl.setText(settings.searchUrl)
        etUploadUrl.setText(settings.uploadUrl)
        etUser.setText(settings.username)
        etPass.setText(settings.password)
        etLimit.setText(settings.limit.toString())

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            settings.authUrl = etAuthUrl.text.toString()
            settings.searchUrl = etSearchUrl.text.toString()
            settings.uploadUrl = etUploadUrl.text.toString()
            settings.username = etUser.text.toString()
            settings.password = etPass.text.toString()
            settings.limit = etLimit.text.toString().toIntOrNull() ?: 5

            // Attempt login
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val api = RetrofitClient.create(settings)
                    val resp = api.login(settings.authUrl,
                        LoginRequest(settings.username, settings.password))
                    settings.token = resp.access_token
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Login OK", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity,
                            "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}