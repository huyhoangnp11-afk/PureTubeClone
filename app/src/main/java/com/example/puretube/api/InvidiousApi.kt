package com.example.puretube.api

import com.example.puretube.model.VideoStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface InvidiousApi {
    @GET("api/v1/videos/{videoId}")
    suspend fun getVideoDetails(@Path("videoId") videoId: String): VideoStatus

    companion object {
        // We use a public, reliable instance. You can switch to others if this one is down.
        // List of instances: https://api.invidious.io/
        private const val BASE_URL = "https://vid.puffyan.us/" 

        fun create(): InvidiousApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(InvidiousApi::class.java)
        }
    }
}
