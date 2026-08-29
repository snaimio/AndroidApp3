package com.sheikhnaim.sensortoolbox.detection

// ============================================================
// IMPORTS
// ============================================================
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * EMFMeterActivity - Measures Electromagnetic Field strength
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Magnetometer to detect magnetic fields
 * 2. Calculates total EMF strength: sqrt(x² + y² + z²)
 * 3. Displays as µT (microtesla)
 * 4. Shows color-coded safety status
 *
 * ============================================================
 * SAFETY LEVELS:
 * ============================================================
 * - < 0.3 µT: ✅ Safe (typical background)
 * - 0.3 - 1.0 µT: ⚠️ Moderate (near electronics)
 * - 1.0 - 5.0 µT: 🔶 High (near power lines)
 * - > 5.0 µT: 🔴 Very High (dangerous levels)
 *
 * ============================================================
 * CALIBRATION:
 * ============================================================
 * The app calibrates on first reading to remove Earth's
 * magnetic field baseline. Press "Reset" to recalibrate.
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class EMFMeterActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Maximum EMF value for full bar display (µT) */
        private const val MAX_EMF_VALUE = 3.0f

        /** Safe EMF threshold (µT) - below this is considered safe */
        private const val THRESHOLD_SAFE = 0.3f

        /** Moderate EMF threshold (µT) - between safe and moderate */
        private const val THRESHOLD_MODERATE = 1.0f

        /** High EMF threshold (µT) - between moderate and high */
        private const val THRESHOLD_HIGH = 5.0f
    }

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    /** Manages sensor registration and updates */
    private lateinit var sensorManager: SensorManager

    /** The magnetic field sensor (magnetometer) */
    private var magneticSensor: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    /** Displays the current EMF value in µT */
    private lateinit var emfValueText: TextView

    /** Visual progress bar showing EMF strength */
    private lateinit var emfBar: View

    /** Shows safety status (Safe/Moderate/High/Very High) */
    private lateinit var statusText: TextView

    /** Shows additional safety information */
    private lateinit var safetyInfoText: TextView

    /** Button to recalibrate the sensor */
    private lateinit var resetButton: Button

    // ============================================================
    // DATA
    // ============================================================
    /** Baseline magnetic field value (removes Earth's field) */
    private var baseline = 0f

    /** True if calibration has been performed */
    private var isCalibrated = false

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    /**
     * Called when the activity is created.
     * Sets up the UI, sensors, and button listeners.
     *
     * @param savedInstanceState Previously saved state (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emf_meter)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupResetButton()
    }

    /**
     * Called when the activity becomes visible.
     * Registers the sensor listener to start receiving updates.
     */
    override fun onResume() {
        super.onResume()
        // Register sensor listener when activity is visible
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * Called when the activity is no longer visible.
     * Unregisters the sensor listener to save battery.
     */
    override fun onPause() {
        super.onPause()
        // Unregister sensor listener to save battery
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // INITIALIZATION METHODS
    // ============================================================

    /**
     * Sets up the toolbar with back navigation and title.
     */
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.emf_meter_title)
    }

    /**
     * Initializes all UI view references from the layout.
     */
    private fun initializeViews() {
        emfValueText = findViewById(R.id.emfValueText)
        emfBar = findViewById(R.id.emfBar)
        statusText = findViewById(R.id.statusText)
        safetyInfoText = findViewById(R.id.safetyInfoText)
        resetButton = findViewById(R.id.resetButton)
    }

    /**
     * Initializes the sensor manager and checks for magnetometer availability.
     * Shows appropriate error messages if the sensor is not available.
     */
    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticSensor == null) {
            // Magnetometer not available - show error
            emfValueText.text = getString(R.string.emf_no_sensor)
            statusText.text = getString(R.string.emf_sensor_unavailable)
            Toast.makeText(this, R.string.emf_sensor_not_found, Toast.LENGTH_LONG).show()
        } else {
            // Sensor available - start scanning
            statusText.text = getString(R.string.emf_scanning)
        }
    }

    /**
     * Sets up the reset/recalibrate button.
     * Resets the baseline calibration to current readings.
     */
    private fun setupResetButton() {
        resetButton.setOnClickListener {
            // Reset calibration to current values
            baseline = 0f
            isCalibrated = false
            Toast.makeText(this, R.string.emf_calibrated, Toast.LENGTH_SHORT).show()
            statusText.text = getString(R.string.emf_calibrated_scanning)
            safetyInfoText.text = getString(R.string.emf_resetting_baseline)
        }
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