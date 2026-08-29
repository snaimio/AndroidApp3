package com.sheikhnaim.sensortoolbox.detection

// ============================================================
// IMPORTS
// ============================================================
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * MetalDetectorActivity - Detects metals using the magnetic field sensor
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Magnetometer (magnetic field sensor)
 * 2. Reads the X, Y, Z values of the magnetic field
 * 3. Calculates the total magnetic field strength using:
 *    total = sqrt(x² + y² + z²)
 * 4. Values are displayed in microtesla (µT)
 * 5. The higher the value, the more metal is nearby!
 *
 * ============================================================
 * SCIENCE BEHIND IT:
 * ============================================================
 * Earth's magnetic field is typically around 25-65 µT.
 * When you bring a ferromagnetic metal (iron, steel) near
 * the phone, it distorts the magnetic field, causing a
 * significant increase in the measured value.
 *
 * ============================================================
 * CALIBRATION:
 * ============================================================
 * Calibration removes the background magnetic field (Earth's
 * field + nearby electronics) so only the metal's effect is
 * measured. Press "Calibrate" away from metal objects.
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class MetalDetectorActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Maximum expected magnetic field for progress bar (µT) */
        private const val MAX_EXPECTED_VALUE = 100.0

        /** Threshold for strong signal (percentage) */
        private const val THRESHOLD_STRONG = 80

        /** Threshold for medium signal (percentage) */
        private const val THRESHOLD_MEDIUM = 50

        /** Threshold for weak signal (percentage) */
        private const val THRESHOLD_WEAK = 20

        /** Delay before calibration starts (milliseconds) */
        private const val CALIBRATION_DELAY_MS = 500L
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
    /** Displays the current magnetic field strength in µT */
    private lateinit var strengthText: TextView

    /** Visual progress bar showing signal strength */
    private lateinit var strengthBar: ProgressBar

    /** Shows the current detection status */
    private lateinit var statusText: TextView

    /** Shows helpful hints to the user */
    private lateinit var hintText: TextView

    /** Button to recalibrate the sensor */
    private lateinit var calibrateButton: Button

    // ============================================================
    // DATA
    // ============================================================
    /** Baseline magnetic field value (removes background) */
    private var baseline = 0.0f

    /** True if calibration has been performed */
    private var isCalibrated = false

    /** Handler for delayed calibration */
    private val handler = Handler(Looper.getMainLooper())

    /** Runnable for calibration */
    private var calibrationRunnable: Runnable? = null

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
        setContentView(R.layout.activity_metal_detector)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupCalibrateButton()
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
        // Remove any pending calibration
        calibrationRunnable?.let { handler.removeCallbacks(it) }
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
        supportActionBar?.title = getString(R.string.metal_detector_title)
    }

    /**
     * Initializes all UI view references from the layout.
     */
    private fun initializeViews() {
        strengthText = findViewById(R.id.strengthText)
        strengthBar = findViewById(R.id.strengthBar)
        statusText = findViewById(R.id.statusText)
        hintText = findViewById(R.id.hintText)
        calibrateButton = findViewById(R.id.calibrateButton)
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
            statusText.text = getString(R.string.metal_no_sensor)
            hintText.text = getString(R.string.metal_no_sensor_hint)
            Toast.makeText(this, R.string.metal_sensor_not_found, Toast.LENGTH_LONG).show()
        } else {
            // Sensor available - start scanning
            statusText.text = getString(R.string.metal_scanning)
            hintText.text = getString(R.string.metal_hint)
        }
    }

    /**
     * Sets up the calibrate button.
     * Resets the baseline calibration to current readings.
     */
    private fun setupCalibrateButton() {
        calibrateButton.setOnClickListener {
            performCalibration()
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