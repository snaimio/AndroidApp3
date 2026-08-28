package com.sheikhnaim.sensortoolbox.detection

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * MetalDetectorActivity - Detects metals using the magnetic field sensor
 *
 * HOW IT WORKS:
 * 1. Uses the Magnetometer (magnetic field sensor)
 * 2. Reads the X, Y, Z values of the magnetic field
 * 3. Calculates the total magnetic field strength (µT)
 * 4. The higher the value, the more metal is nearby!
 *
 * Earth's magnetic field is typically around 25-65 µT.
 * When you get near metal, the value increases significantly!
 */
class MetalDetectorActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var strengthText: TextView
    private lateinit var strengthBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var hintText: TextView
    private lateinit var calibrateButton: Button

    // ============================================================
    // DATA
    // ============================================================
    private var baseline: Float = 0f  // Used for calibration
    private var isCalibrated = false

    // Maximum expected magnetic field (µT) for the progress bar
    private val MAX_EXPECTED = 100.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metal_detector)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        strengthText = findViewById(R.id.strengthText)
        strengthBar = findViewById(R.id.strengthBar)
        statusText = findViewById(R.id.statusText)
        hintText = findViewById(R.id.hintText)
        calibrateButton = findViewById(R.id.calibrateButton)

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        // ============================================================
        // STEP 4: Check if sensor is available
        // ============================================================
        if (magneticSensor == null) {
            statusText.text = "❌ Magnetic sensor not available"
            hintText.text = "This device doesn't have a magnetometer"
            Toast.makeText(this, "Magnetometer not found", Toast.LENGTH_LONG).show()
        } else {
            statusText.text = "📡 Scanning..."
            hintText.text = "Move phone near metal objects to detect them!"
        }

        // ============================================================
        // STEP 5: Set up the calibrate button
        // ============================================================
        calibrateButton.setOnClickListener {
            calibrate()
        }
    }

    /**
     * onResume - Register the sensor listener
     */
    override fun onResume() {
        super.onResume()
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * onPause - Unregister the sensor listener
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    /**
     * onSensorChanged - Called when magnetic field data changes
     *
     * HOW IT WORKS:
     * 1. Gets X, Y, Z values from the sensor
     * 2. Calculates total strength using Pythagorean theorem
     * 3. Converts from microtesla (µT)
     * 4. Updates the UI
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]  // X-axis magnetic field
            val y = event.values[1]  // Y-axis magnetic field
            val z = event.values[2]  // Z-axis magnetic field

            // Calculate total magnetic field strength using Pythagorean theorem
            // total = sqrt(x² + y² + z²)
            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Apply calibration offset
            val calibratedValue = if (isCalibrated) {
                total - baseline
            } else {
                total
            }

            // Calculate percentage for progress bar
            val percentage = (calibratedValue / MAX_EXPECTED * 100).coerceIn(0.0, 100.0)

            // Update UI
            updateUI(calibratedValue, percentage.toInt())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    /**
     * updateUI - Updates all UI elements with the current reading
     */
    private fun updateUI(value: Float, percentage: Int) {
        // Update the text display
        strengthText.text = String.format("%.1f µT", value)

        // Update the progress bar
        strengthBar.progress = percentage

        // Update the status message based on the reading
        statusText.text = when {
            percentage > 80 -> "⚠️ STRONG SIGNAL - Metal detected! 🧲"
            percentage > 50 -> "📶 Medium signal - Something is near"
            percentage > 20 -> "📶 Weak signal - Move closer"
            else -> "✅ No metal detected"
        }

        // Change color based on strength
        val color = when {
            percentage > 80 -> android.R.color.holo_red_dark
            percentage > 50 -> android.R.color.holo_orange_dark
            percentage > 20 -> android.R.color.holo_green_dark
            else -> android.R.color.holo_blue_dark
        }
        strengthText.setTextColor(getColor(color))
    }

    /**
     * calibrate - Resets the baseline reading
     *
     * This helps remove background magnetic field noise
     * The phone should be away from metal when calibrating
     */
    private fun calibrate() {
        // We need to get the current magnetic field reading
        // Since we can't read the sensor directly, we'll use a temporary listener
        // and get the latest reading from the sensor
        Toast.makeText(this, "Calibrating... Keep away from metal!", Toast.LENGTH_SHORT).show()
        statusText.text = "🔄 Calibrating..."

        // Get the current magnetic field reading
        magneticSensor?.let { sensor ->
            sensorManager.registerListener(
                object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                            val x = event.values[0]
                            val y = event.values[1]
                            val z = event.values[2]
                            baseline = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                            isCalibrated = true

                            // Unregister this temporary listener
                            sensorManager.unregisterListener(this)

                            runOnUiThread {
                                statusText.text = "✅ Calibrated! Baseline: ${String.format("%.1f", baseline)} µT"
                                Toast.makeText(this@MetalDetectorActivity, "Calibrated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                },
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}