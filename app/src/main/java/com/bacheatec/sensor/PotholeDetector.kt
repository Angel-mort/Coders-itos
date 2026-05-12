package com.bacheatec.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlin.math.sqrt

/**
 * MVP: aceleración lineal (sin gravedad) si el dispositivo la expone; si no, acelerómetro bruto con umbral más alto.
 * Magnitud + umbral + cooldown + última ubicación Fused.
 */
class PotholeDetector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val motionSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val usesLinearAcceleration: Boolean =
        motionSensor?.type == Sensor.TYPE_LINEAR_ACCELERATION

    /** Umbral en m/s² sobre la magnitud del vector (interpretación según el sensor activo). */
    var accelerationThresholdMs2: Float =
        motionSensor?.let { defaultThresholdForSensorType(it.type) } ?: DEFAULT_THRESHOLD_RAW_MS2
        set(value) {
            field = value.coerceIn(MIN_THRESHOLD_MS2, MAX_THRESHOLD_MS2)
        }

    var onPotholeDetected: ((latitude: Double?, longitude: Double?, magnitudeMs2: Float) -> Unit)? =
        null

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            lastLocation = result.lastLocation ?: lastLocation
        }
    }

    private var running = false
    private var lastDetectionElapsedMs: Long = 0L

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Activa sensores y actualizaciones de ubicación. Devuelve false si falta hardware o permisos.
     */
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        if (motionSensor == null) {
            Log.w(TAG, "Sin sensor de movimiento")
            return false
        }
        if (!hasLocationPermission()) {
            Log.w(TAG, "Sin permiso de ubicación")
            return false
        }

        lastLocation = null
        fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
            if (loc != null) {
                lastLocation = loc
            }
        }

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LOCATION_UPDATE_INTERVAL_MS,
        )
            .setMinUpdateIntervalMillis(LOCATION_MIN_INTERVAL_MS)
            .build()

        fusedClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper(),
        )

        sensorManager.registerListener(
            this,
            motionSensor,
            SensorManager.SENSOR_DELAY_UI,
        )

        running = true
        Log.d(
            TAG,
            "Detector iniciado (sensor=${if (usesLinearAcceleration) "LINEAR" else "RAW"}, umbral=${accelerationThresholdMs2} m/s²)",
        )
        return true
    }

    fun stop() {
        if (!running) return
        sensorManager.unregisterListener(this)
        fusedClient.removeLocationUpdates(locationCallback)
        running = false
        Log.d(TAG, "Detector detenido")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != motionSensor?.type) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude <= accelerationThresholdMs2) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastDetectionElapsedMs < COOLDOWN_MS) return
        lastDetectionElapsedMs = now

        val loc = lastLocation
        Log.d(
            TAG,
            "Posible bache | magnitud=${"%.2f".format(magnitude)} m/s² | lat=${loc?.latitude} lon=${loc?.longitude}",
        )

        onPotholeDetected?.invoke(loc?.latitude, loc?.longitude, magnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "PotholeDetector"
        private const val COOLDOWN_MS = 2_000L
        private const val LOCATION_UPDATE_INTERVAL_MS = 2_000L
        private const val LOCATION_MIN_INTERVAL_MS = 1_000L

        private const val MIN_THRESHOLD_MS2 = 6f
        private const val MAX_THRESHOLD_MS2 = 45f

        /**
         * LINEAR: sin gravedad; ~12 m/s² filtra mucho ruido de mano.
         * RAW: incluye gravedad; hace falta umbral más alto (~26) para no disparar al mover el teléfono.
         */
        const val DEFAULT_THRESHOLD_LINEAR_MS2 = 12f
        const val DEFAULT_THRESHOLD_RAW_MS2 = 26f

        fun defaultThresholdForSensorType(sensorType: Int): Float =
            if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION) {
                DEFAULT_THRESHOLD_LINEAR_MS2
            } else {
                DEFAULT_THRESHOLD_RAW_MS2
            }
    }
}
