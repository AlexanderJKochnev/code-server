package com.example.photouploader.data

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val settings: SettingsManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = settings.token
        val request = if (token.isNotBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}