package com.example.photouploader.data

import com.example.photouploader.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {

    @POST
    suspend fun login(
        @Url url: String,
        @Body body: LoginRequest
    ): LoginResponse

    @GET
    suspend fun search(
        @Url url: String,
        @Query("search") query: String,
        @Query("limit") limit: Int
    ): SearchResponse

    @Multipart
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Query("tag") tag: String,
        @Part file: MultipartBody.Part,
        @Part("id") id: RequestBody?,
        @Part("display_name") displayName: RequestBody,
        @Part("metadata") metadata: RequestBody
    ): UploadResponse
}