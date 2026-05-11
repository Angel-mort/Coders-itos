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
 * MVP: acelerómetro + magnitud del vector + umbral + cooldown 1s + última ubicación Fused.
 */
class PotholeDetector(private val context: Context) : SensorEventListener {

    /** Magnitud en m/s² (incluye gravedad; en reposo ~9.8). */
    var accelerationThresholdMs2: Float = DEFAULT_THRESHOLD_MS2
        set(value) {
            field = value.coerceIn(10f, 50f)
        }

    var onPotholeDetected: ((latitude: Double?, longitude: Double?, magnitudeMs2: Float) -> Unit)? =
        null

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

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
        if (accelerometer == null) {
            Log.w(TAG, "Sin acelerómetro")
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
            accelerometer,
            SensorManager.SENSOR_DELAY_UI,
        )

        running = true
        Log.d(TAG, "Detector iniciado (umbral=${accelerationThresholdMs2} m/s²)")
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
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

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
        private const val COOLDOWN_MS = 1_000L
        private const val LOCATION_UPDATE_INTERVAL_MS = 2_000L
        private const val LOCATION_MIN_INTERVAL_MS = 1_000L

        /** Por encima de ~9.8 m/s² en reposo; ajusta en prototipo si hay muchos falsos positivos. */
        const val DEFAULT_THRESHOLD_MS2 = 18f
    }
}
