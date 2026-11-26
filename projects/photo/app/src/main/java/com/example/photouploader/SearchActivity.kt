package com.example.photouploader

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.photouploader.data.RetrofitClient
import com.example.photouploader.data.SettingsManager
import com.example.photouploader.data.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var etQuery: EditText
    private lateinit var rvResults: RecyclerView
    private lateinit var adapter: ResultsAdapter
    private lateinit var settings: SettingsManager

    private val queryFlow = MutableStateFlow("")
    private var selected: SearchResult? = null
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        settings = SettingsManager(this)
        etQuery = findViewById(R.id.etQuery)
        rvResults = findViewById(R.id.rvResults)

        adapter = ResultsAdapter { item ->
            selected = item
            etQuery.setText(item.display_name)
            etQuery.setSelection(etQuery.text.length)
        }
        rvResults.layoutManager = LinearLayoutManager(this)
        rvResults.adapter = adapter

        etQuery.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                selected = null
                queryFlow.value = text
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Debounce 400 мс, запрос только при длине > 3
        lifecycleScope.launch {
            queryFlow
                .debounce(400)
                .filter { it.length > 3 }
                .collectLatest { query -> performSearch(query) }
        }

        findViewById<Button>(R.id.btnNext).setOnClickListener {
            val displayName = selected?.display_name
                ?: etQuery.text.toString().trim()
            if (displayName.isBlank()) return@setOnClickListener

            val intent = Intent(this, CameraActivity::class.java).apply {
                putExtra("id", selected?.id?.toString())
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
                val results = withContext(Dispatchers.IO) {
                    api.search(settings.searchUrl, query, settings.limit)
                }
                adapter.submit(results)
            } catch (_: Exception) {
                // молча игнорируем — пользователь продолжает печатать
            }
        }
    }

    // --- Адаптер ---
    private class ResultsAdapter(
        val onClick: (SearchResult) -> Unit
    ) : RecyclerView.Adapter<ResultsAdapter.VH>() {

        private val items = mutableListOf<SearchResult>()

        fun submit(newItems: List<SearchResult>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_search_result, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvName.text = item.display_name
            holder.tvId.text = "id: ${item.id ?: "—"}"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size

        class VH(view: android.view.View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvId: TextView = view.findViewById(R.id.tvId)
        }
    }
}