package com.example.photouploader.data

import com.example.photouploader.data.model.LoginRequest
import com.example.photouploader.data.model.LoginResponse
import com.example.photouploader.data.model.SaveResponse
import com.example.photouploader.data.model.SearchResult
import okhttp3.MultipartBody
import okhttp3.ResponseBody
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
    ): List<SearchResult>

    @Multipart
    @POST
    suspend fun uploadImage(
        @Url url: String,
        @Query("tag") tag: String,
        @Query("id") id: String?,
        @Part file: MultipartBody.Part
    ): retrofit2.Response<ResponseBody>

    @POST
    suspend fun saveImage(
        @Url url: String,
        @Query("image_hash") imageHash: String
    ): SaveResponse
}