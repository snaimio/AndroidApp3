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
 * HOW IT WORKS:
 * 1. Uses the Magnetometer to detect magnetic fields
 * 2. Calculates total EMF strength: sqrt(x² + y² + z²)
 * 3. Displays as µT (microtesla)
 * 4. Shows color-coded safety status
 *
 * SAFETY LEVELS:
 * - < 0.3 µT: ✅ Safe (typical background)
 * - 0.3 - 1.0 µT: ⚠️ Moderate (near electronics)
 * - 1.0 - 5.0 µT: 🔶 High (near power lines)
 * - > 5.0 µT: 🔴 Very High (dangerous levels)
 */
class EMFMeterActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var emfValueText: TextView
    private lateinit var emfBar: View
    private lateinit var statusText: TextView
    private lateinit var safetyInfoText: TextView
    private lateinit var resetButton: Button

    // ============================================================
    // DATA
    // ============================================================
    private var baseline = 0f
    private var isCalibrated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emf_meter)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        emfValueText = findViewById(R.id.emfValueText)
        emfBar = findViewById(R.id.emfBar)
        statusText = findViewById(R.id.statusText)
        safetyInfoText = findViewById(R.id.safetyInfoText)
        resetButton = findViewById(R.id.resetButton)

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticSensor == null) {
            emfValueText.text = "❌ No sensor"
            statusText.text = "⚠️ Magnetometer not available"
            Toast.makeText(this, "Magnetometer not found", Toast.LENGTH_LONG).show()
        } else {
            statusText.text = "📡 Scanning EMF..."
        }

        // ============================================================
        // STEP 4: Set up Reset button
        // ============================================================
        resetButton.setOnClickListener {
            baseline = 0f
            isCalibrated = false
            Toast.makeText(this, "🔄 Calibrated!", Toast.LENGTH_SHORT).show()
            statusText.text = "📡 Calibrated - Scanning EMF..."
            safetyInfoText.text = "📱 Resetting baseline"
        }
    }

    override fun onResume() {
        super.onResume()
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Calibration: set baseline on first reading
            if (!isCalibrated) {
                baseline = total
                isCalibrated = true
                safetyInfoText.text = "📱 Baseline: ${String.format("%.2f", baseline)} µT"
            }

            // Calculate relative value (removes Earth's magnetic field)
            val relativeValue = (total - baseline).coerceAtLeast(0f)

            updateUI(relativeValue, total)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateUI(value: Float, rawValue: Float) {
        // Display the EMF value
        emfValueText.text = String.format("%.2f µT", value)

        // Update the bar - max width is proportional
        val maxValue = 3.0f  // 3.0 µT = full bar
        val percentage = (value / maxValue * 100).coerceIn(0f, 100f)
        val maxWidth = 300 // dp
        val barWidth = (percentage / 100 * maxWidth).toInt()
        emfBar.layoutParams.width = barWidth
        emfBar.requestLayout()

        // Determine safety level and colors
        val (color, status, info) = when {
            value < 0.3 -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_green_dark),
                "✅ Safe - Low EMF",
                "📱 Typical background level"
            )
            value < 1.0 -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_orange_dark),
                "⚠️ Moderate EMF",
                "📱 Near electronics or appliances"
            )
            value < 5.0 -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_orange_light),
                "🔶 High EMF",
                "⚠️ Possible power lines nearby"
            )
            else -> Triple(
                ContextCompat.getColor(this, android.R.color.holo_red_dark),
                "🔴 Very High EMF",
                "🚨 Dangerous levels detected!"
            )
        }

        // Update UI with colors
        emfValueText.setTextColor(color)
        emfBar.setBackgroundColor(color)
        statusText.setTextColor(color)
        statusText.text = status
        safetyInfoText.text = info
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}