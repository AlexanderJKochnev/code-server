package com.example.photouploader

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.photouploader.data.RetrofitClient
import com.example.photouploader.data.SettingsManager
import com.example.photouploader.data.model.SearchResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.collectLatest

class SearchActivity : AppCompatActivity() {

    private lateinit var autoComplete: AutoCompleteTextView
    private lateinit var settings: SettingsManager
    private var searchJob: Job? = null
    private val queryFlow = MutableStateFlow("")
    private var results: List<SearchResult> = emptyList()
    private var selected: SearchResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        settings = SettingsManager(this)
        autoComplete = findViewById(R.id.autoComplete)

        autoComplete.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                selected = null
                queryFlow.value = text
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        autoComplete.setOnItemClickListener { _, _, position, _ ->
            selected = results.getOrNull(position)
            autoComplete.setText(selected?.display_name ?: "")
            autoComplete.dismissDropDown()
        }

        // Debounce 400ms
        lifecycleScope.launch {
            queryFlow
                .debounce(400)
                .filter { it.length > 3 }
                .collectLatest { query ->
                    performSearch(query)
                }
        }

        findViewById<android.view.View>(R.id.btnNext).setOnClickListener {
            val displayName = selected?.display_name
                ?: autoComplete.text.toString().trim()
            if (displayName.isBlank()) return@setOnClickListener

            val intent = Intent(this, CameraActivity::class.java).apply {
                putExtra("id", selected?.id)
                putExtra("display_name", displayName)
            }
            startActivity(intent)
        }
    }

    private suspend fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            try {
                val api = RetrofitClient.create(settings)
                val response = withContext(Dispatchers.IO) {
                    api.search(settings.searchUrl, query, settings.limit)
                }
                results = response.results
                val names = results.map { it.display_name }
                val adapter = ArrayAdapter(
                    this@SearchActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    names
                )
                autoComplete.setAdapter(adapter)
                if (names.isNotEmpty()) autoComplete.showDropDown()
            } catch (_: Exception) { /* silently ignore, user continues typing */ }
        }
    }
}