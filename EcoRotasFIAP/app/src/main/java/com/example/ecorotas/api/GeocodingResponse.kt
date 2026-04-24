package com.example.ecorotas.api

import com.google.gson.annotations.SerializedName

// DTO da resposta do Geocoding (endereço -> lat/lng). Usado se a gente for converter texto em coordenada.
data class GeocodingResponse(
    val results: List<GeocodingResult>? = null,
    val status: String? = null,
    val error_message: String? = null
)

data class GeocodingResult(
    val geometry: Geometry? = null,
    @SerializedName("formatted_address") val formattedAddress: String? = null,
    @SerializedName("place_id") val placeId: String? = null
)

data class Geometry(
    val location: LatLngResult? = null
)

data class LatLngResult(
    val lat: Double = 0.0,
    val lng: Double = 0.0
)
