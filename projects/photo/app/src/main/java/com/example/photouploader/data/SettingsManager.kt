package com.example.photouploader.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var authUrl: String
        get() = prefs.getString("auth_url", "https://api.test.abc8888.ru/auth/say_bay") ?: ""
        set(v) = prefs.edit().putString("auth_url", v).apply()

    var searchUrl: String
        get() = prefs.getString("search_url", "https://api.test.abc8888.ru/items/search_display_name_all") ?: ""
        set(v) = prefs.edit().putString("search_url", v).apply()

    var uploadUrl: String
        get() = prefs.getString("upload_url", "https://api.test.abc8888.ru/items/upload_image") ?: ""
        set(v) = prefs.edit().putString("upload_url", v).apply()

    var saveUrl: String
        get() = prefs.getString("save_url", "https://api.test.abc8888.ru/items/save_image") ?: ""
        set(v) = prefs.edit().putString("save_url", v).apply()

    var username: String
        get() = prefs.getString("username", "photo") ?: ""
        set(v) = prefs.edit().putString("username", v).apply()

    var password: String
        get() = prefs.getString("password", "") ?: ""
        set(v) = prefs.edit().putString("password", v).apply()

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(v) = prefs.edit().putString("token", v).apply()

    var limit: Int
        get() = prefs.getInt("limit", 5)
        set(v) = prefs.edit().putInt("limit", v).apply()

}