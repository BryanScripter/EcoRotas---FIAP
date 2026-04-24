package com.example.ecorotas.data

data class RouteRecord(
    val id: String,
    val origin: String,
    val destination: String,
    val distance: Double,
    val bestMode: String,
    val co2Saved: Double,
    val date: String
)
