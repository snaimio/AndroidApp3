package com.sheikhnaim.sensortoolbox.motion

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * ShakeDetectorActivity - Detects when the phone is shaken
 *
 * HOW IT WORKS:
 * 1. Uses the Accelerometer to detect sudden movements
 * 2. Calculates the total acceleration (like Gravity Meter)
 * 3. When acceleration exceeds a threshold, counts it as a shake
 * 4. Prevents multiple detections in quick succession (debounce)
 * 5. Tracks intensity of the shake
 *
 * WHAT IS A SHAKE?
 * - A sudden change in acceleration (like you're shaking the phone)
 * - The phone moves quickly in one direction, then another
 * - The total acceleration exceeds a certain threshold
 *
 * WHY DEBOUNCE?
 * - One shake can trigger multiple sensor readings
 * - We only count one shake per 500ms to avoid duplicates
 */
class ShakeDetectorActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var shakeCountText: TextView
    private lateinit var lastShakeText: TextView
    private lateinit var intensityText: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button

    // ============================================================
    // SHAKE DETECTION DATA
    // ============================================================
    private var shakeCount = 0          // Total shakes detected
    private var lastShakeTime = 0L      // When the last shake occurred
    private val DEBOUNCE_TIME = 500L    // Minimum time between shakes (ms)
    private val SHAKE_THRESHOLD = 15f   // Minimum acceleration to count as shake

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_detector)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        shakeCountText = findViewById(R.id.shakeCountText)
        lastShakeText = findViewById(R.id.lastShakeText)
        intensityText = findViewById(R.id.intensityText)
        statusText = findViewById(R.id.statusText)
        resetButton = findViewById(R.id.resetButton)

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            statusText.text = "❌ Accelerometer not available"
            Toast.makeText(this, "Accelerometer not found", Toast.LENGTH_LONG).show()
        } else {
            statusText.text = "📱 Shake your phone to detect shakes!"
        }

        // ============================================================
        // STEP 4: Set up the Reset button
        // ============================================================
        resetButton.setOnClickListener {
            shakeCount = 0
            lastShakeTime = 0L
            lastShakeText.text = "--"
            intensityText.text = "--"
            shakeCountText.text = "0"
            statusText.text = "🔄 Reset - Shake your phone!"
            Toast.makeText(this, "🔄 Reset", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * onResume - Register the sensor listener
     */
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
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
     * onSensorChanged - Called when accelerometer data changes
     *
     * HOW SHAKE DETECTION WORKS:
     * 1. Get X, Y, Z values from the accelerometer
     * 2. Calculate total acceleration (like Gravity Meter)
     * 3. If total acceleration > threshold, it's a shake!
     * 4. Check if enough time has passed since last shake (debounce)
     * 5. If it's a new shake, increment count and update UI
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            // Calculate total acceleration
            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Check if acceleration exceeds threshold (shake detected!)
            if (total > SHAKE_THRESHOLD) {
                val currentTime = SystemClock.elapsedRealtime()

                // Check if enough time has passed since last shake (debounce)
                if (currentTime - lastShakeTime > DEBOUNCE_TIME) {
                    // It's a new shake!
                    shakeCount++
                    lastShakeTime = currentTime

                    // Calculate intensity based on acceleration magnitude
                    val intensity = when {
                        total > 30 -> "💥 Very High"
                        total > 25 -> "🔥 High"
                        total > 20 -> "📶 Medium"
                        else -> "📶 Low"
                    }

                    // Update UI
                    shakeCountText.text = shakeCount.toString()
                    lastShakeText.text = "Now"
                    intensityText.text = intensity
                    statusText.text = "🌀 Shake #$shakeCount detected!"

                    // Visual feedback - flash the count briefly
                    shakeCountText.setTextColor(getColor(android.R.color.holo_orange_dark))
                    shakeCountText.postDelayed({
                        shakeCountText.setTextColor(getColor(android.R.color.holo_blue_dark))
                    }, 300)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}