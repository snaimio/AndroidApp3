package com.sheikhnaim.sensortoolbox.detection

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
import android.view.View
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
 * EMFMeterActivity - Measures Electromagnetic Field (EMF) strength in microTeslas (µT)
 * using the hardware Magnetometer / Geomagnetic sensor and geotags hotspots on OpenStreetMap.
 *
 * HOW IT WORKS (Physics & Sensor Math):
 * 1. The hardware magnetic field sensor measures magnetic flux density along the X, Y, and Z axes.
 * 2. Total ambient field magnitude is computed via:
 *    B = sqrt(Bx² + By² + Bz²)
 * 3. Earth's natural geomagnetic background field ranges from ~30µT to ~60µT.
 * 4. This activity allows establishing a baseline calibration to zero out ambient
 *    geomagnetic background and isolate active EMF radiating from electrical appliances,
 *    wiring, motors, and electronic devices.
 * 5. Users can save geotagged EMF hotspots to view on the OpenStreetMap interactive canvas.
 */
class EMFMeterActivity : AppCompatActivity(), SensorEventListener {

    /**
     * Data class to store an EMF Hotspot reading with timestamp and GPS coordinates.
     */
    data class EMFHotspot(
        val lat: Double,
        val lon: Double,
        val emfValue: Float,
        val status: String,
        val timestamp: Long
    )

    // ============================================================
    // CONSTANTS & EMF RADIATION THRESHOLDS
    // ============================================================
    companion object {
        /** Maximum relative EMF reading mapped to full scale gauge bar (µT) */
        private const val MAX_EMF_VALUE = 3.0f

        /** Safe exposure baseline threshold (µT) */
        private const val THRESHOLD_SAFE = 0.3f

        /** Moderate exposure warning threshold (µT) */
        private const val THRESHOLD_MODERATE = 1.0f

        /** High radiation alert threshold (µT) */
        private const val THRESHOLD_HIGH = 5.0f
    }

    // ============================================================
    // SENSOR MANAGER & LOCATION
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private val emfHotspots = mutableListOf<EMFHotspot>()
    private var currentEmfValue: Float = 0f

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var emfValueText: TextView
    private lateinit var emfBar: View
    private lateinit var statusText: TextView
    private lateinit var safetyInfoText: TextView
    private lateinit var resetButton: Button
    private lateinit var saveHotspotButton: Button
    private lateinit var viewMapButton: Button

    // ============================================================
    // DATA
    // ============================================================
    private var baseline = 0f
    private var isCalibrated = false

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                fetchLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emf_meter)

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
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.emf_meter_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        emfValueText = findViewById(R.id.emfValueText)
        emfBar = findViewById(R.id.emfBar)
        statusText = findViewById(R.id.statusText)
        safetyInfoText = findViewById(R.id.safetyInfoText)
        resetButton = findViewById(R.id.resetButton)
        saveHotspotButton = findViewById(R.id.saveHotspotButton)
        viewMapButton = findViewById(R.id.viewMapButton)
    }

    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticSensor == null) {
            statusText.text = getString(R.string.emf_no_sensor)
            safetyInfoText.text = getString(R.string.emf_sensor_unavailable)
            Toast.makeText(this, R.string.emf_sensor_not_found, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupButtons() {
        resetButton.setOnClickListener {
            resetCalibration()
        }

        saveHotspotButton.setOnClickListener {
            if (lastLocation != null) {
                val spot = EMFHotspot(
                    lastLocation!!.latitude,
                    lastLocation!!.longitude,
                    currentEmfValue,
                    statusText.text.toString(),
                    System.currentTimeMillis()
                )
                emfHotspots.add(spot)
                Toast.makeText(this, "📍 EMF Hotspot logged! (${String.format(Locale.US, "%.2f µT", currentEmfValue)})", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Acquiring GPS location...", Toast.LENGTH_SHORT).show()
                fetchLocation()
            }
        }

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, MapActivity::class.java)
            if (emfHotspots.isNotEmpty()) {
                val lats = DoubleArray(emfHotspots.size) { emfHotspots[it].lat }
                val lons = DoubleArray(emfHotspots.size) { emfHotspots[it].lon }
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val titles = Array(emfHotspots.size) { "📡 EMF Spot #${it + 1} (${String.format(Locale.US, "%.2f µT", emfHotspots[it].emfValue)})" }
                val snippets = Array(emfHotspots.size) { "${emfHotspots[it].status} | Time: ${sdf.format(Date(emfHotspots[it].timestamp))}" }

                intent.putExtra(MapActivity.EXTRA_SPOT_LATS, lats)
                intent.putExtra(MapActivity.EXTRA_SPOT_LONS, lons)
                intent.putExtra(MapActivity.EXTRA_SPOT_TITLES, titles)
                intent.putExtra(MapActivity.EXTRA_SPOT_SNIPPETS, snippets)
                intent.putExtra(MapActivity.EXTRA_TITLE, "📡 EMF Hotspots Map")
            } else if (lastLocation != null) {
                intent.putExtra(MapActivity.EXTRA_LATITUDE, lastLocation!!.latitude)
                intent.putExtra(MapActivity.EXTRA_LONGITUDE, lastLocation!!.longitude)
                intent.putExtra(MapActivity.EXTRA_TITLE, "📡 EMF Meter (${String.format(Locale.US, "%.2f µT", currentEmfValue)})")
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

    private fun resetCalibration() {
        baseline = 0f
        isCalibrated = false
        Toast.makeText(this, R.string.emf_calibrated, Toast.LENGTH_SHORT).show()
        statusText.text = getString(R.string.emf_calibrated_scanning)
        safetyInfoText.text = getString(R.string.emf_resetting_baseline)
    }

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

    /**
     * Called when sensor data changes.
     * Calculates EMF strength and updates the UI.
     *
     * @param event The sensor event containing magnetic field data
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            // Get magnetic field values in X, Y, Z axes
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate total EMF strength using Pythagorean theorem
            // Total = sqrt(x² + y² + z²)
            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Calibration: set baseline on first reading
            if (!isCalibrated) {
                baseline = total
                isCalibrated = true
                safetyInfoText.text = String.format(
                    getString(R.string.emf_baseline_format),
                    baseline
                )
            }

            // Calculate relative value (removes Earth's magnetic field)
            val relativeValue = (total - baseline).coerceAtLeast(0f)
            currentEmfValue = relativeValue

            updateUI(relativeValue, total)
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
     * Updates the UI with the current EMF values.
     * Updates the value display, progress bar, and safety status.
     *
     * @param value The relative EMF value (µT) - baseline subtracted
     * @param rawValue The raw EMF value (µT) - includes Earth's field
     */
    private fun updateUI(value: Float, rawValue: Float) {
        // Update the EMF value display
        emfValueText.text = String.format(
            getString(R.string.emf_value_format),
            value
        )

        // Update the progress bar
        updateProgressBar(value)

        // Update safety status and colors
        updateSafetyStatus(value)
    }

    /**
     * Updates the progress bar based on EMF value.
     * Bar width scales proportionally up to MAX_EMF_VALUE.
     *
     * @param value The current EMF value (µT)
     */
    private fun updateProgressBar(value: Float) {
        // Calculate percentage of max value (capped at 100%)
        val percentage = (value / MAX_EMF_VALUE * 100).coerceIn(0f, 100f)

        // Use half of screen width for the bar
        val maxWidth = resources.displayMetrics.widthPixels / 2
        val barWidth = (percentage / 100 * maxWidth).toInt()

        // Update bar width
        emfBar.layoutParams.width = barWidth
        emfBar.requestLayout()
    }

    /**
     * Updates safety status with appropriate colors and messages.
     * Uses color-coded safety levels with corresponding text.
     *
     * @param value The current EMF value (µT)
     */
    private fun updateSafetyStatus(value: Float) {
        // Determine safety level based on value
        val (color, status, info) = when {
            // Safe: Below 0.3 µT - Green
            value < THRESHOLD_SAFE -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_green_dark),
                getString(R.string.emf_status_safe),
                getString(R.string.emf_info_safe)
            )
            // Moderate: 0.3 - 1.0 µT - Orange
            value < THRESHOLD_MODERATE -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                getString(R.string.emf_status_moderate),
                getString(R.string.emf_info_moderate)
            )
            // High: 1.0 - 5.0 µT - Light Orange
            value < THRESHOLD_HIGH -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_orange_light),
                getString(R.string.emf_status_high),
                getString(R.string.emf_info_high)
            )
            // Very High: > 5.0 µT - Red
            else -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_red_dark),
                getString(R.string.emf_status_very_high),
                getString(R.string.emf_info_very_high)
            )
        }

        // Update UI with appropriate colors and text
        emfValueText.setTextColor(color)
        emfBar.setBackgroundColor(color)
        statusText.setTextColor(color)
        statusText.text = status
        safetyInfoText.text = info
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