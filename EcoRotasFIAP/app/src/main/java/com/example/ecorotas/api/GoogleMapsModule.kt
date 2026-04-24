package com.example.ecorotas.api

import com.example.ecorotas.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Retrofit apontando pro Google Maps; a key vem do BuildConfig (preenchido pelo local.properties).
object GoogleMapsModule {

    private const val BASE_URL = "https://maps.googleapis.com/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GoogleMapsApi get() = retrofit.create(GoogleMapsApi::class.java)

    fun apiKey(): String = BuildConfig.MAPS_API_KEY
}
