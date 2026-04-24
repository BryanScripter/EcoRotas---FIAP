package com.example.ecorotas.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ecorotas.data.Emissions
import com.example.ecorotas.data.RouteResult
import com.example.ecorotas.data.fetchRouteAndBestMode
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Tela do mapa: origem/destino, "minha localização", calcula rota no Google e mostra polyline + quanto economizou de CO2.
@Composable
fun MapaRotaScreen(
    onBack: () -> Unit,
    onVerResultado: (origin: String, destination: String, distance: Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var routeResult by remember { mutableStateOf<RouteResult?>(null) }
    // Mapa começa em SP; depois a câmera vai pra rota ou pro usuário
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-23.5505, -46.6333), 11f)
    }
    var hasLocationPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { map ->
        hasLocationPermission = map[Manifest.permission.ACCESS_FINE_LOCATION] == true
            || map[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Pedir localização logo ao abrir a tela
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun useMyLocation() {
        scope.launch {
            if (!hasLocationPermission) {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return@launch
            }
            try {
                val client = LocationServices.getFusedLocationProviderClient(context)
                val loc = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                if (loc != null) {
                    // Directions aceita "lat,lng" direto
                    origin = "${loc.latitude},${loc.longitude}"
                    cameraPositionState.move(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(loc.latitude, loc.longitude),
                            14f
                        )
                    )
                } else {
                    error = "Não foi possível obter sua localização."
                }
            } catch (e: SecurityException) {
                error = "Permita o acesso à localização."
            } catch (e: Exception) {
                error = "Erro ao obter localização."
            }
        }
    }

    fun calculateRoute() {
        error = ""
        if (origin.isBlank() || destination.isBlank()) {
            error = "Informe origem e destino."
            return
        }
        loading = true
        routeResult = null
        scope.launch {
            val r = fetchRouteAndBestMode(
                originQuery = origin.trim(),
                destinationQuery = destination.trim(),
                originLabel = origin.trim(),
                destinationLabel = destination.trim()
            )
            loading = false
            r.fold(
                onSuccess = { result ->
                    routeResult = result
                    if (result.polylinePoints.isNotEmpty()) {
                        val first = result.polylinePoints.first()
                        val last = result.polylinePoints.last()
                        val center = LatLng(
                            (first.latitude + last.latitude) / 2,
                            (first.longitude + last.longitude) / 2
                        )
                        cameraPositionState.move(
                            CameraUpdateFactory.newLatLngZoom(center, 11f)
                        )
                    }
                },
                onFailure = { e ->
                    error = e.message ?: "Não deu pra buscar a rota. Confere se a MAPS_API_KEY está no local.properties."
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = MaterialTheme.colorScheme.onPrimary)
            }
            Text(
                "Nova Rota",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "Use o mapa e calcule a melhor rota para economizar CO2",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }

        // Campos e mapa
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = origin,
                onValueChange = { origin = it; error = "" },
                label = { Text("Origem") },
                placeholder = { Text("Endereço ou use \"Minha localização\"") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
            OutlinedButton(
                onClick = { useMyLocation() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Usar minha localização")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it; error = "" },
                label = { Text("Destino") },
                placeholder = { Text("Para onde você vai?") },
                leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.primary
                )
            )
            if (error.isNotEmpty()) {
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            Button(
                onClick = { calculateRoute() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp).padding(end = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
                Text(if (loading) "Calculando rota..." else "Calcular melhor rota")
            }

                Spacer(modifier = Modifier.height(8.dp))

            // Área do mapa (polyline aparece quando tem rota)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = remember(hasLocationPermission) {
                        MapProperties(isMyLocationEnabled = hasLocationPermission)
                    }
                ) {
                    routeResult?.polylinePoints?.let { points ->
                        if (points.isNotEmpty()) {
                            Polyline(
                                points = points,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Card com melhor modal e CO2 economizado (só aparece depois de calcular)
            routeResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val icon = when (result.bestMode) {
                            Emissions.TransportMode.CAR -> Icons.Default.DirectionsCar
                            Emissions.TransportMode.BUS -> Icons.Default.DirectionsBus
                            Emissions.TransportMode.BIKE -> Icons.Default.ElectricBike
                        }
                        Text("Melhor opção: ${result.bestMode.label}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Você economiza %.2f kg de CO2 nesta rota (em relação ao carro).".format(result.co2SavedKg),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                        Text("Distância: %.1f km".format(result.distanceKm), fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        result.durationText?.let { Text("Tempo estimado: $it", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)) }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onVerResultado(
                                    result.originAddress,
                                    result.destinationAddress,
                                    result.distanceKm
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Ver detalhes e salvar no histórico")
                        }
                    }
                }
            }
        }
    }
}
