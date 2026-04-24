package com.example.ecorotas.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Endpoints do Google que usamos: Directions (rota) e Geocoding (endereço -> lat/lng). Precisa ativar no Cloud e colocar a key no local.properties.
interface GoogleMapsApi {

    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("key") apiKey: String
    ): Response<DirectionsResponse>

    @GET("maps/api/geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("key") apiKey: String
    ): Response<GeocodingResponse>
}
