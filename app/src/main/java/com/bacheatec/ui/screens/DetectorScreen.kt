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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.bacheatec.sensor.PotholeDetector

@Composable
fun DetectorScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val detector = remember { PotholeDetector(appContext) }

    var listening by remember { mutableStateOf(false) }
    var count by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    var pendingStart by remember { mutableStateOf(false) }

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
                    hint = "No se pudo iniciar (¿acelerómetro?)."
                }
            } else {
                hint = "Se necesita ubicación para guardar coordenadas."
            }
        }
    }

    DisposableEffect(detector) {
        detector.onPotholeDetected = { _, _, _ ->
            count++
        }
        onDispose {
            detector.onPotholeDetected = null
            detector.stop()
        }
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
            text = "Baches detectados: $count",
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Umbral magnitud: ${"%.1f".format(detector.accelerationThresholdMs2)} m/s² (ajústalo en código si hace falta).",
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
                        hint = "No hay acelerómetro o no se pudo iniciar."
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

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mueve el teléfono con cuidado en pruebas; revisa Logcat con el tag PotholeDetector.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
