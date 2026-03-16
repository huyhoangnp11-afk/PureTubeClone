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
        // List of public Invidious instances (fallback order)
        // If one is down, the app will try the next one automatically
        private val INSTANCES = listOf(
            "https://invidious.fdn.fr/",
            "https://yewtu.be/",
            "https://invidious.nerdvpn.de/",
            "https://inv.nadeko.net/",
            "https://invidious.privacyredirect.com/",
            "https://invidious.protokoll-11.de/",
            "https://iv.datura.network/"
        )

        private fun createForInstance(baseUrl: String): InvidiousApi {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(InvidiousApi::class.java)
        }

        fun create(): InvidiousApiWithFallback {
            return InvidiousApiWithFallback(INSTANCES.map { createForInstance(it) })
        }
    }
}

class InvidiousApiWithFallback(private val apis: List<InvidiousApi>) {
    suspend fun getVideoDetails(videoId: String): VideoStatus {
        var lastException: Exception? = null
        for (api in apis) {
            try {
                return api.getVideoDetails(videoId)
            } catch (e: Exception) {
                lastException = e
                // This instance failed, try next one
            }
        }
        throw lastException ?: Exception("Tất cả máy chủ Invidious đều không phản hồi. Vui lòng thử lại sau.")
    }
}
