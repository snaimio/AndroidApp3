package com.sheikhnaim.sensortoolbox.detection

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
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
 * MetalDetectorActivity - Detects ferromagnetic metals and magnetic anomalies
 * using the hardware Magnetometer sensor and geotags detection locations on OpenStreetMap.
 *
 * HOW IT WORKS (Physics & Sensor Math):
 * 1. Ferromagnetic metals (iron, steel, cobalt, nickel) distort and concentrate
 *    local magnetic flux lines.
 * 2. When a metal object approaches the phone's magnetometer chip, the total field strength:
 *    B_total = sqrt(Bx² + By² + Bz²) spikes significantly above the natural ambient baseline (~45µT).
 * 3. The app calibrates the ambient background baseline and converts the positive delta
 *    into a relative percentage gauge (0% to 100%).
 * 4. Found detection spots can be saved with GPS coordinates and viewed on OpenStreetMap.
 */
class MetalDetectorActivity : AppCompatActivity(), SensorEventListener {

    /**
     * Data class to store a single geotagged metal detection spot.
     */
    data class DetectionSpot(
        val lat: Double,
        val lon: Double,
        val strength: Float,
        val timestamp: Long
    )

    // ============================================================
    // CONSTANTS & CALIBRATION THRESHOLDS
    // ============================================================
    companion object {
        /** Maximum expected magnetic field deviation for progress gauge (µT) */
        private const val MAX_EXPECTED_VALUE = 100.0

        /** Signal percentage thresholds for classifying proximity */
        private const val THRESHOLD_STRONG = 80
        private const val THRESHOLD_MEDIUM = 50
        private const val THRESHOLD_WEAK = 20

        /** Calibration delay (ms) to settle sensor readings */
        private const val CALIBRATION_DELAY_MS = 500L
    }

    // ============================================================
    // SENSOR MANAGER & LOCATION
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private val detectionSpots = mutableListOf<DetectionSpot>()
    private var currentStrength: Float = 0f

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var strengthText: TextView
    private lateinit var strengthBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var calibrateButton: Button
    private lateinit var saveSpotButton: Button
    private lateinit var viewMapButton: Button

    // ============================================================
    // DATA
    // ============================================================
    private var baseline = 0.0f
    private var isCalibrated = false
    private val handler = Handler(Looper.getMainLooper())
    private var calibrationRunnable: Runnable? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                fetchLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metal_detector)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupButtons()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fetchLocation()
    }

    override fun onResume() {
        super.onResume()
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        fetchLocation()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        calibrationRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.metal_detector_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        strengthText = findViewById(R.id.strengthText)
        strengthBar = findViewById(R.id.strengthBar)
        statusText = findViewById(R.id.statusText)
        hintText = findViewById(R.id.hintText)
        calibrateButton = findViewById(R.id.calibrateButton)
        saveSpotButton = findViewById(R.id.saveSpotButton)
        viewMapButton = findViewById(R.id.viewMapButton)
    }

    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticSensor == null) {
            statusText.text = getString(R.string.metal_no_sensor)
            hintText.text = getString(R.string.metal_no_sensor_hint)
            Toast.makeText(this, R.string.metal_sensor_not_found, Toast.LENGTH_LONG).show()
        } else {
            statusText.text = getString(R.string.metal_scanning)
            hintText.text = getString(R.string.metal_hint)
        }
    }

    private fun setupButtons() {
        calibrateButton.setOnClickListener {
            performCalibration()
        }

        saveSpotButton.setOnClickListener {
            if (lastLocation != null) {
                val spot = DetectionSpot(
                    lastLocation!!.latitude,
                    lastLocation!!.longitude,
                    currentStrength,
                    System.currentTimeMillis()
                )
                detectionSpots.add(spot)
                Toast.makeText(this, "📍 Detection spot marked! (${String.format(Locale.US, "%.1f µT", currentStrength)})", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Acquiring GPS location...", Toast.LENGTH_SHORT).show()
                fetchLocation()
            }
        }

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, MapActivity::class.java)
            if (detectionSpots.isNotEmpty()) {
                val lats = DoubleArray(detectionSpots.size) { detectionSpots[it].lat }
                val lons = DoubleArray(detectionSpots.size) { detectionSpots[it].lon }
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val titles = Array(detectionSpots.size) { "🧲 Metal Spot #${it + 1} (${String.format(Locale.US, "%.1f µT", detectionSpots[it].strength)})" }
                val snippets = Array(detectionSpots.size) { "Time: ${sdf.format(Date(detectionSpots[it].timestamp))} | Lat: ${String.format(Locale.US, "%.5f", detectionSpots[it].lat)}, Lon: ${String.format(Locale.US, "%.5f", detectionSpots[it].lon)}" }

                intent.putExtra(MapActivity.EXTRA_SPOT_LATS, lats)
                intent.putExtra(MapActivity.EXTRA_SPOT_LONS, lons)
                intent.putExtra(MapActivity.EXTRA_SPOT_TITLES, titles)
                intent.putExtra(MapActivity.EXTRA_SPOT_SNIPPETS, snippets)
                intent.putExtra(MapActivity.EXTRA_TITLE, "🧲 Metal Detection Spots")
            } else if (lastLocation != null) {
                intent.putExtra(MapActivity.EXTRA_LATITUDE, lastLocation!!.latitude)
                intent.putExtra(MapActivity.EXTRA_LONGITUDE, lastLocation!!.longitude)
                intent.putExtra(MapActivity.EXTRA_TITLE, "🧲 Metal Detector (${String.format(Locale.US, "%.1f µT", currentStrength)})")
                intent.putExtra(MapActivity.EXTRA_SNIPPET, statusText.text.toString())
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

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

    /**
     * Called when sensor data changes.
     * Calculates magnetic field strength and updates the UI.
     *
     * @param event The sensor event containing magnetic field data
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Get magnetic field values in X, Y, Z axes
            val x = event.values[0]  // X-axis magnetic field
            val y = event.values[1]  // Y-axis magnetic field
            val z = event.values[2]  // Z-axis magnetic field

            // Calculate total magnetic field strength using Pythagorean theorem
            // total = sqrt(x² + y² + z²)
            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Apply calibration offset
            val calibratedValue = if (isCalibrated) {
                (total - baseline).coerceAtLeast(0.0f)
            } else {
                total
            }
            currentStrength = calibratedValue

            // Calculate percentage for progress bar
            val percentage = (calibratedValue / MAX_EXPECTED_VALUE * 100).coerceIn(0.0, 100.0)

            // Update UI
            updateUI(calibratedValue, percentage.toInt())
        }
    }

    /**
     * Called when sensor accuracy changes.
     * Not used for this sensor type.
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for magnetic field sensor
    }

    // ============================================================
    // UI UPDATE METHODS
    // ============================================================

    /**
     * Updates all UI elements with the current reading.
     * Updates the value display, progress bar, status, and colors.
     *
     * @param value The current magnetic field strength (µT)
     * @param percentage The percentage for the progress bar (0-100)
     */
    private fun updateUI(value: Float, percentage: Int) {
        // Update the text display
        strengthText.text = String.format(
            getString(R.string.metal_value_format),
            value
        )

        // Update the progress bar
        strengthBar.progress = percentage

        // Update the status message based on the reading
        val status = when {
            percentage > THRESHOLD_STRONG -> getString(R.string.metal_status_strong)
            percentage > THRESHOLD_MEDIUM -> getString(R.string.metal_status_medium)
            percentage > THRESHOLD_WEAK -> getString(R.string.metal_status_weak)
            else -> getString(R.string.metal_status_none)
        }
        statusText.text = status

        // Change color based on strength
        val colorResId = when {
            percentage > THRESHOLD_STRONG -> android.R.color.holo_red_dark
            percentage > THRESHOLD_MEDIUM -> android.R.color.holo_orange_dark
            percentage > THRESHOLD_WEAK -> android.R.color.holo_green_dark
            else -> android.R.color.holo_blue_dark
        }
        strengthText.setTextColor(ContextCompat.getColor(this, colorResId))
    }

    // ============================================================
    // CALIBRATION METHODS
    // ============================================================

    /**
     * Performs calibration to remove background magnetic field.
     *
     * Calibration process:
     * 1. Shows "Calibrating..." status
     * 2. Waits for a moment to get a stable reading
     * 3. Sets the current reading as the baseline
     * 4. Updates UI to show calibration complete
     *
     * IMPORTANT: Keep phone away from metal during calibration!
     */
    private fun performCalibration() {
        // Show calibration status
        Toast.makeText(this, R.string.metal_calibrating_toast, Toast.LENGTH_SHORT).show()
        statusText.text = getString(R.string.metal_calibrating)

        // Remove any pending calibration
        calibrationRunnable?.let { handler.removeCallbacks(it) }

        // Create a new calibration runnable
        calibrationRunnable = Runnable {
            // Get the latest magnetic field reading
            magneticSensor?.let { sensor ->
                // Create a temporary listener to get one reading
                val tempListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]

                            // Set baseline to current reading
                            baseline = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                            isCalibrated = true

                            // Unregister this temporary listener
                            sensorManager.unregisterListener(this)

                            // Update UI on main thread
                            runOnUiThread {
                                statusText.text = String.format(
                                    getString(R.string.metal_calibrated_format),
                                    baseline
                                )
                                Toast.makeText(
                                    this@MetalDetectorActivity,
                                    R.string.metal_calibrated_toast,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        // Not needed
                    }
                }

                // Register the temporary listener
                sensorManager.registerListener(
                    tempListener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI
                )
            }
        }

        // Execute calibration after a short delay
        handler.postDelayed(calibrationRunnable!!, CALIBRATION_DELAY_MS)
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    /**
     * Handles the up navigation button in the toolbar.
     * Navigates back to the previous activity.
     *
     * @return true if navigation was handled
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}