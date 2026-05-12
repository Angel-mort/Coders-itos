package com.bacheatec.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bacheatec.data.PotholeSession

@Composable
fun ReportScreen(modifier: Modifier = Modifier) {
    val draft = PotholeSession.reportDraft

    var description by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf("") }
    var lonText by remember { mutableStateOf("") }
    var magText by remember { mutableStateOf("") }
    var sentHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draft?.id) {
        val d = draft
        if (d != null) {
            latText = d.latitude?.let { "%.6f".format(it) } ?: ""
            lonText = d.longitude?.let { "%.6f".format(it) } ?: ""
            magText = "%.2f".format(d.magnitudeMs2)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nuevo reporte",
            style = MaterialTheme.typography.titleLarge,
        )

        if (draft != null) {
            Text(
                text = "Datos traídos del detector (puedes editar la descripción).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Completa el formulario. Si vienes del detector, pulsa \"Reportar\" allí.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        OutlinedTextField(
            value = latText,
            onValueChange = { latText = it },
            label = { Text("Latitud") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = lonText,
            onValueChange = { lonText = it },
            label = { Text("Longitud") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = magText,
            onValueChange = { magText = it },
            label = { Text("Magnitud (m/s²)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        sentHint?.let {
            Text(it, color = MaterialTheme.colorScheme.primary)
        }

        Button(
            onClick = {
                // Aquí irá Firebase: guardar lat/lon/mag/descripción
                sentHint = "Listo (local). Firebase: pendiente de conectar."
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enviar reporte")
        }

        TextButton(
            onClick = {
                PotholeSession.clearReportDraft()
                description = ""
                latText = ""
                lonText = ""
                magText = ""
                sentHint = null
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Quitar datos del detector")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Coordenadas y magnitud se pueden ajustar a mano antes de enviar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
