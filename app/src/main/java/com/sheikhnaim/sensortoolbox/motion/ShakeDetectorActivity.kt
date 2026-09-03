package com.sheikhnaim.sensortoolbox.motion

// ============================================================
// IMPORTS
// ============================================================
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sheikhnaim.sensortoolbox.MapActivity
import com.sheikhnaim.sensortoolbox.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

/**
 * ShakeDetectorActivity - Detects device shakes using the 3-axis Accelerometer
 * and geotags shake events on OpenStreetMap.
 *
 * HOW IT WORKS (Physics & Algorithm):
 * 1. Reads raw accelerometer values: x, y, z (in m/s²).
 * 2. Calculates overall 3D acceleration vector magnitude:
 *    magnitude = sqrt(x² + y² + z²)
 * 3. Compares magnitude against SHAKE_THRESHOLD (15.0 m/s²).
 *    Normal Earth gravity when resting is ~9.81 m/s², so anything above 15 m/s²
 *    indicates active physical shaking.
 * 4. Applies a DEBOUNCE_TIME_MS (500ms) lock to prevent a single physical shake
 *    from triggering multiple count events.
 * 5. Tags current GPS latitude & longitude and saves to a historical list so users
 *    can inspect where shakes occurred on the OpenStreetMap interactive view.
 */
class ShakeDetectorActivity : AppCompatActivity(), SensorEventListener {

    /**
     * Data class to store a single geotagged shake event.
     */
    data class ShakeEvent(
        val lat: Double,
        val lon: Double,
        val count: Int,
        val intensity: String,
        val timestamp: Long
    )

    // ============================================================
    // CONSTANTS & THRESHOLDS
    // ============================================================
    companion object {
        /** Minimum delay (ms) between recorded shakes to debounce oscillations */
        private const val DEBOUNCE_TIME_MS = 500L

        /** Acceleration threshold (m/s²) required to qualify as a shake */
        private const val SHAKE_THRESHOLD = 15f

        /** Thresholds for classifying shake severity */
        private const val INTENSITY_VERY_HIGH = 30f
        private const val INTENSITY_HIGH = 25f
        private const val INTENSITY_MEDIUM = 20f
    }

    // ============================================================
    // SENSOR MANAGER & LOCATION SERVICES
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private val shakeLocations = mutableListOf<ShakeEvent>()

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var shakeCountText: TextView
    private lateinit var lastShakeText: TextView
    private lateinit var intensityText: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button
    private lateinit var viewMapButton: Button

    // ============================================================
    // SHAKE DETECTION DATA
    // ============================================================
    private var shakeCount = 0          // Total shakes detected
    private var lastShakeTime = 0L      // When the last shake occurred

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                fetchLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_detector)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupButtons()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        fetchLocation()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.shake_detector_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        shakeCountText = findViewById(R.id.shakeCountText)
        lastShakeText = findViewById(R.id.lastShakeText)
        intensityText = findViewById(R.id.intensityText)
        statusText = findViewById(R.id.statusText)
        resetButton = findViewById(R.id.resetButton)
        viewMapButton = findViewById(R.id.viewMapButton)
    }

    private fun setupButtons() {
        resetButton.setOnClickListener {
            shakeCount = 0
            shakeLocations.clear()
            shakeCountText.text = getString(R.string.shake_zero)
            lastShakeText.text = getString(R.string.shake_default)
            intensityText.text = getString(R.string.shake_default)
            statusText.text = getString(R.string.shake_reset_message)
            Toast.makeText(this, R.string.shake_reset_toast, Toast.LENGTH_SHORT).show()
        }

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, MapActivity::class.java)
            if (shakeLocations.isNotEmpty()) {
                val lats = DoubleArray(shakeLocations.size) { shakeLocations[it].lat }
                val lons = DoubleArray(shakeLocations.size) { shakeLocations[it].lon }
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val titles = Array(shakeLocations.size) { "🌀 Shake #${shakeLocations[it].count} (${shakeLocations[it].intensity})" }
                val snippets = Array(shakeLocations.size) { "Time: ${sdf.format(Date(shakeLocations[it].timestamp))} | Lat: ${String.format(Locale.US, "%.5f", shakeLocations[it].lat)}, Lon: ${String.format(Locale.US, "%.5f", shakeLocations[it].lon)}" }

                intent.putExtra(MapActivity.EXTRA_SPOT_LATS, lats)
                intent.putExtra(MapActivity.EXTRA_SPOT_LONS, lons)
                intent.putExtra(MapActivity.EXTRA_SPOT_TITLES, titles)
                intent.putExtra(MapActivity.EXTRA_SPOT_SNIPPETS, snippets)
                intent.putExtra(MapActivity.EXTRA_TITLE, "🌀 Shake Locations Map")
            } else if (lastLocation != null) {
                intent.putExtra(MapActivity.EXTRA_LATITUDE, lastLocation!!.latitude)
                intent.putExtra(MapActivity.EXTRA_LONGITUDE, lastLocation!!.longitude)
                intent.putExtra(MapActivity.EXTRA_TITLE, "🌀 Shake Detector ($shakeCount shakes)")
                intent.putExtra(MapActivity.EXTRA_SNIPPET, "Intensity: ${intensityText.text}")
            }
            startActivity(intent)
        }
    }

    private fun fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        lastLocation = loc
                    }
                }
        } else {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            statusText.text = getString(R.string.shake_no_sensor)
            Toast.makeText(
                this,
                getString(R.string.shake_sensor_not_found),
                Toast.LENGTH_LONG
            ).show()
        } else {
            statusText.text = getString(R.string.shake_hint)
        }
    }

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            if (total > SHAKE_THRESHOLD) {
                val currentTime = SystemClock.elapsedRealtime()

                if (currentTime - lastShakeTime > DEBOUNCE_TIME_MS) {
                    handleShake(total, currentTime)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    // ============================================================
    // SHAKE HANDLING METHODS
    // ============================================================

    private fun handleShake(total: Float, currentTime: Long) {
        shakeCount++
        lastShakeTime = currentTime

        val intensity = getIntensity(total)
        val intensityTextValue = when (intensity) {
            "VERY_HIGH" -> getString(R.string.shake_intensity_very_high)
            "HIGH" -> getString(R.string.shake_intensity_high)
            "MEDIUM" -> getString(R.string.shake_intensity_medium)
            else -> getString(R.string.shake_intensity_low)
        }

        lastLocation?.let { loc ->
            shakeLocations.add(
                ShakeEvent(
                    loc.latitude,
                    loc.longitude,
                    shakeCount,
                    intensityTextValue,
                    System.currentTimeMillis()
                )
            )
        }

        shakeCountText.text = shakeCount.toString()
        lastShakeText.text = getString(R.string.shake_now)
        intensityText.text = intensityTextValue
        statusText.text = getString(R.string.shake_detected_format, shakeCount)

        flashShakeCount()
    }

    private fun getIntensity(total: Float): String {
        return when {
            total > INTENSITY_VERY_HIGH -> "VERY_HIGH"
            total > INTENSITY_HIGH -> "HIGH"
            total > INTENSITY_MEDIUM -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun flashShakeCount() {
        shakeCountText.setTextColor(
            ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        )
        shakeCountText.postDelayed({
            shakeCountText.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_blue_dark)
            )
        }, 300)
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}