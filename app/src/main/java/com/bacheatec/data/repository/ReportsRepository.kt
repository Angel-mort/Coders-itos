package com.bacheatec.data.repository

import com.bacheatec.data.model.PotholeReport

/**
 * Capa de datos local mínima; más adelante leerá Firestore.
 */
class ReportsRepository {
    fun getReports(): List<PotholeReport> = emptyList()
}
