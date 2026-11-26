package com.example.photouploader.data.model

data class SearchResult(val id: String?, val display_name: String)
data class LoginRequest(val username: String, val password: String)
data class LoginResponse(val access_token: String)