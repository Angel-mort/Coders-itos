package com.bacheatec.data.model

/**
 * Un hit del detector (aún no es un reporte enviado a Firebase).
 */
data class PotholeCandidate(
    val id: Long,
    val latitude: Double?,
    val longitude: Double?,
    val magnitudeMs2: Float,
    val detectedAtEpochMs: Long,
)
