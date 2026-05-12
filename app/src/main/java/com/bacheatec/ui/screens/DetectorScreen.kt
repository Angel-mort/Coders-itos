package com.bacheatec.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.bacheatec.data.PotholeSession
import com.bacheatec.data.model.PotholeCandidate
import com.bacheatec.navigation.NavRoutes
import com.bacheatec.sensor.PotholeDetector

@Composable
fun DetectorScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val detector = remember { PotholeDetector(appContext) }

    var listening by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf(false) }
    var dialogCandidate by remember { mutableStateOf<PotholeCandidate?>(null) }

    val candidates = PotholeSession.candidates

    val permissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val granted = results.values.any { it }
        if (pendingStart) {
            pendingStart = false
            if (granted) {
                hint = null
                if (detector.start()) {
                    listening = true
                } else {
                    hint = "No se pudo iniciar (¿sensor de movimiento?)."
                }
            } else {
                hint = "Se necesita ubicación para guardar coordenadas."
            }
        }
    }

    DisposableEffect(detector) {
        detector.onPotholeDetected = { lat, lon, mag ->
            val c = PotholeSession.recordDetection(lat, lon, mag)
            dialogCandidate = c
        }
        onDispose {
            detector.onPotholeDetected = null
            detector.stop()
        }
    }

    dialogCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { dialogCandidate = null },
            title = { Text("¿Reportar un bache?") },
            text = {
                Text(
                    "Se detectó un posible bache (${"%.1f".format(candidate.magnitudeMs2)} m/s²). " +
                        "¿Ir al formulario con la ubicación?",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PotholeSession.setReportDraft(candidate)
                        dialogCandidate = null
                        navController.navigate(NavRoutes.REPORT) {
                            launchSingleTop = true
                        }
                    },
                ) { Text("Sí, reportar") }
            },
            dismissButton = {
                TextButton(onClick = { dialogCandidate = null }) { Text("No") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Detector automático",
            style = MaterialTheme.typography.titleLarge,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (listening) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ),
        ) {
            Text(
                text = if (listening) {
                    "Escuchando sensores"
                } else {
                    "En pausa"
                },
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            text = "Baches detectados (sesión): ${candidates.size}",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Umbral magnitud: ${"%.1f".format(detector.accelerationThresholdMs2)} m/s² ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        hint?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    hint = null
                    if (!hasLocationPermission()) {
                        pendingStart = true
                        permissionLauncher.launch(permissions)
                        return@Button
                    }
                    if (detector.start()) {
                        listening = true
                    } else {
                        hint = "No hay sensor de movimiento o no se pudo iniciar."
                    }
                },
                enabled = !listening,
            ) {
                Text("Iniciar")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    detector.stop()
                    listening = false
                },
                enabled = listening,
            ) {
                Text("Detener")
            }
        }

        Text(
            text = "Últimos en esta sesión",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(candidates.take(8), key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${"%.1f".format(item.magnitudeMs2)} m/s²",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                when {
                                    item.latitude != null && item.longitude != null ->
                                        "${"%.5f".format(item.latitude)}, ${"%.5f".format(item.longitude)}"
                                    else -> "Sin ubicación aún"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = {
                                PotholeSession.setReportDraft(item)
                                navController.navigate(NavRoutes.REPORT) {
                                    launchSingleTop = true
                                }
                            },
                        ) { Text("Reportar") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Los datos se guardan solo en esta app hasta que conectes Firebase.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
