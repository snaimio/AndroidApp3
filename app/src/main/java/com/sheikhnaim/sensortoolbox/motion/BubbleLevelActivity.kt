package com.sheikhnaim.sensortoolbox.motion

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * BubbleLevelActivity - A digital bubble level using the accelerometer
 *
 * HOW IT WORKS:
 * 1. Uses the Accelerometer to detect device tilt
 * 2. Calculates pitch (forward/backward tilt) and roll (left/right tilt)
 * 3. Moves a "bubble" on screen to show the tilt
 * 4. Shows "LEVEL!" when the device is flat
 *
 * WHAT ARE PITCH AND ROLL?
 * - Pitch: Rotation around the X-axis (forward/backward tilt)
 * - Roll: Rotation around the Y-axis (left/right tilt)
 * - When both are near 0°, the device is level!
 */
class BubbleLevelActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var bubble: View
    private lateinit var levelContainer: CardView
    private lateinit var pitchText: TextView
    private lateinit var rollText: TextView
    private lateinit var levelStatusText: TextView

    // ============================================================
    // BUBBLE POSITION
    // ============================================================
    private var containerWidth = 0
    private var containerHeight = 0
    private val BUBBLE_SIZE = 48
    private val MARGIN = 30

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble_level)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        bubble = findViewById(R.id.bubble)
        levelContainer = findViewById(R.id.levelContainer)
        pitchText = findViewById(R.id.pitchText)
        rollText = findViewById(R.id.rollText)
        levelStatusText = findViewById(R.id.levelStatusText)

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            levelStatusText.text = "❌ Accelerometer not available"
        }

        // ============================================================
        // STEP 4: Get container dimensions after layout
        // ============================================================
        levelContainer.post {
            containerWidth = levelContainer.width
            containerHeight = levelContainer.height
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
     * HOW IT WORKS:
     * 1. Gets X, Y, Z values from the accelerometer
     * 2. Calculates pitch (X-axis tilt) and roll (Y-axis tilt)
     * 3. Moves the bubble based on the angles
     * 4. Updates the UI with current values
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]  // Acceleration on X-axis (left/right)
            val y = event.values[1]  // Acceleration on Y-axis (forward/backward)
            val z = event.values[2]  // Acceleration on Z-axis (up/down)

            // Calculate pitch (rotation around X-axis)
            // Formula: pitch = atan2(x, sqrt(y² + z²))
            val pitch = Math.toDegrees(atan2(x.toDouble(), sqrt(y * y + z * z).toDouble()))

            // Calculate roll (rotation around Y-axis)
            // Formula: roll = atan2(y, z)
            val roll = Math.toDegrees(atan2(y.toDouble(), z.toDouble()))

            // Update the bubble position based on pitch and roll
            updateBubble(pitch.toFloat(), roll.toFloat())

            // Update the text displays
            pitchText.text = String.format("Pitch: %.1f°", pitch)
            rollText.text = String.format("Roll: %.1f°", roll)

            // Check if the device is level
            val isLevel = kotlin.math.abs(pitch) < 2.0 && kotlin.math.abs(roll) < 2.0

            // Update the status text
            levelStatusText.text = if (isLevel) {
                "✅ LEVEL! 🎉"
            } else {
                "📱 Tilt your phone to level it"
            }

            // Change color based on level status
            levelStatusText.setTextColor(
                if (isLevel) {
                    getColor(android.R.color.holo_green_dark)
                } else {
                    getColor(android.R.color.darker_gray)
                }
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    /**
     * updateBubble - Moves the bubble based on pitch and roll angles
     *
     * @param pitch The pitch angle in degrees (-90 to 90)
     * @param roll The roll angle in degrees (-90 to 90)
     */
    private fun updateBubble(pitch: Float, roll: Float) {
        // Get the container dimensions
        if (containerWidth == 0 || containerHeight == 0) {
            levelContainer.post {
                containerWidth = levelContainer.width
                containerHeight = levelContainer.height
            }
            return
        }

        // Calculate the available space for the bubble to move
        val maxX = (containerWidth - BUBBLE_SIZE) / 2 - MARGIN
        val maxY = (containerHeight - BUBBLE_SIZE) / 2 - MARGIN

        // Map pitch and roll to bubble position
        // Clamp values to -1 to 1 range
        val normalizedPitch = (pitch / 45f).coerceIn(-1f, 1f)
        val normalizedRoll = (roll / 45f).coerceIn(-1f, 1f)

        // Calculate the bubble position
        // Note: We negate the values because the bubble moves in the OPPOSITE direction of tilt
        val xOffset = -normalizedPitch * maxX
        val yOffset = -normalizedRoll * maxY

        // Move the bubble using translation
        bubble.translationX = xOffset
        bubble.translationY = yOffset
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}