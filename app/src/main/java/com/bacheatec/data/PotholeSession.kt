package com.bacheatec.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.bacheatec.data.model.PotholeCandidate

/**
 * Estado en memoria para el hackathon (sin Firebase). Sobrevive al cambiar de pestaña.
 */
object PotholeSession {

    private val _candidates = mutableStateListOf<PotholeCandidate>()
    val candidates: List<PotholeCandidate> get() = _candidates

    private val reportDraftState = mutableStateOf<PotholeCandidate?>(null)
    val reportDraft: PotholeCandidate? get() = reportDraftState.value

    fun recordDetection(latitude: Double?, longitude: Double?, magnitudeMs2: Float): PotholeCandidate {
        val c = PotholeCandidate(
            id = System.nanoTime(),
            latitude = latitude,
            longitude = longitude,
            magnitudeMs2 = magnitudeMs2,
            detectedAtEpochMs = System.currentTimeMillis(),
        )
        _candidates.add(0, c)
        while (_candidates.size > MAX_STORED) {
            _candidates.removeAt(_candidates.lastIndex)
        }
        return c
    }

    fun setReportDraft(candidate: PotholeCandidate) {
        reportDraftState.value = candidate
    }

    fun clearReportDraft() {
        reportDraftState.value = null
    }

    private const val MAX_STORED = 30
}
