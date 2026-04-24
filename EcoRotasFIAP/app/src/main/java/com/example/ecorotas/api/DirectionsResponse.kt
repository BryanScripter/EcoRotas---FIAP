package com.example.ecorotas.api

import com.google.gson.annotations.SerializedName

// DTO da resposta do Directions (JSON). Estrutura: routes -> legs -> distance/duration e overview_polyline.
data class DirectionsResponse(
    val routes: List<Route>? = null,
    val status: String? = null,
    val error_message: String? = null
)

data class Route(
    val legs: List<Leg>? = null,
    @SerializedName("overview_polyline") val overviewPolyline: OverviewPolyline? = null
)

data class Leg(
    val distance: TextValue? = null,
    val duration: TextValue? = null,
    val start_address: String? = null,
    val end_address: String? = null,
    val steps: List<Step>? = null
)

data class TextValue(
    val value: Long = 0,   // em metros (distância) ou segundos (duração)
    val text: String? = null
)

data class OverviewPolyline(
    val points: String? = null  // polyline encoded (decode com decodePolyline no MapsRepository)
)

data class Step(
    val distance: TextValue? = null,
    val duration: TextValue? = null,
    val polyline: OverviewPolyline? = null
)
