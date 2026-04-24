package com.example.ecorotas.data

import com.google.android.gms.maps.model.LatLng

// Tudo que a gente precisa depois de calcular a rota: distância, polyline pra desenhar no mapa, melhor modal e CO2 economizado.
data class RouteResult(
    val originAddress: String,
    val destinationAddress: String,
    val distanceKm: Double,
    val distanceMeters: Long,
    val durationText: String?,
    val bestMode: Emissions.TransportMode,
    val co2SavedKg: Double,
    val polylinePoints: List<LatLng>,
    val originLatLng: LatLng,
    val destinationLatLng: LatLng
)
