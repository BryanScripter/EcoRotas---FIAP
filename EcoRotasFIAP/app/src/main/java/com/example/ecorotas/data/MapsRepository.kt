package com.example.ecorotas.data

import com.example.ecorotas.api.GoogleMapsModule
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// Converte a string encoded que o Google devolve na rota em lista de pontos (lat/lng).
// Ver: https://developers.google.com/maps/documentation/utilities/polylinealgorithm
fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        poly.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return poly
}

// Chama a Directions API, pega a distância real da rota, escolhe o modal que emite menos CO2 e monta o RouteResult.
suspend fun fetchRouteAndBestMode(
    originQuery: String,
    destinationQuery: String,
    originLabel: String,
    destinationLabel: String
): Result<RouteResult> = withContext(Dispatchers.IO) {
    val api = GoogleMapsModule.api
    val key = GoogleMapsModule.apiKey()
    if (key.isEmpty()) return@withContext Result.failure(Exception("Coloque MAPS_API_KEY no local.properties"))

    // Se já veio "lat,lng" não encoda; senão encoda o endereço
    val originStr = if (originQuery.contains(',')) originQuery else URLEncoder.encode(originQuery, StandardCharsets.UTF_8.toString())
    val destStr = if (destinationQuery.contains(',')) destinationQuery else URLEncoder.encode(destinationQuery, StandardCharsets.UTF_8.toString())

    val response = withTimeoutOrNull(20_000) {
        api.getDirections(origin = originStr, destination = destStr, apiKey = key)
    } ?: return@withContext Result.failure(Exception("Demorou demais, tenta de novo"))

    if (!response.isSuccessful) {
        return@withContext Result.failure(Exception("Erro da API: ${response.code()}"))
    }
    val body = response.body() ?: return@withContext Result.failure(Exception("Resposta vazia"))
    if (body.status != "OK") {
        return@withContext Result.failure(Exception(body.error_message ?: body.status ?: "Direções não encontradas"))
    }
    val route = body.routes?.firstOrNull() ?: return@withContext Result.failure(Exception("Nenhuma rota retornada"))
    val leg = route.legs?.firstOrNull() ?: return@withContext Result.failure(Exception("Nenhum trecho na rota"))
    val distanceMeters = leg.distance?.value ?: 0L
    val distanceKm = distanceMeters / 1000.0
    val durationText = leg.duration?.text
    val encodedPoints = route.overviewPolyline?.points ?: ""
    val polylinePoints = if (encodedPoints.isNotEmpty()) decodePolyline(encodedPoints) else emptyList()

    val originLatLng = when {
        originQuery.contains(',') -> {
            val p = originQuery.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
            if (p.size >= 2) LatLng(p[0], p[1]) else polylinePoints.firstOrNull() ?: LatLng(0.0, 0.0)
        }
        else -> polylinePoints.firstOrNull() ?: LatLng(0.0, 0.0)
    }
    val destinationLatLng = when {
        destinationQuery.contains(',') -> {
            val p = destinationQuery.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
            if (p.size >= 2) LatLng(p[0], p[1]) else polylinePoints.lastOrNull() ?: LatLng(0.0, 0.0)
        }
        else -> polylinePoints.lastOrNull() ?: LatLng(0.0, 0.0)
    }

    val bestMode = Emissions.getBestOption(distanceKm)
    val co2SavedKg = Emissions.calculateSavings(distanceKm, bestMode)

    Result.success(
        RouteResult(
            originAddress = originLabel.ifEmpty { leg.start_address ?: originQuery },
            destinationAddress = destinationLabel.ifEmpty { leg.end_address ?: destinationQuery },
            distanceKm = distanceKm,
            distanceMeters = distanceMeters,
            durationText = durationText,
            bestMode = bestMode,
            co2SavedKg = co2SavedKg,
            polylinePoints = polylinePoints,
            originLatLng = originLatLng,
            destinationLatLng = destinationLatLng
        )
    )
}
