package com.bacheatec.data.model

/**
 * Modelo mínimo de un reporte de bache (se ampliará con Firebase).
 */
data class PotholeReport(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val description: String = "",
)
