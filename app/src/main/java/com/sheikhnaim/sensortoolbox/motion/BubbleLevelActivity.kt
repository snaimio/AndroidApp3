package com.sheikhnaim.sensortoolbox.motion

// ============================================================
// IMPORTS
// ============================================================
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
import androidx.core.content.ContextCompat  // ✅ ADDED: For getColor() replacement
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * BubbleLevelActivity - A digital bubble level using the accelerometer
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Accelerometer to detect device tilt
 * 2. Calculates pitch (forward/backward tilt) and roll (left/right tilt)
 * 3. Moves a "bubble" on screen to show the tilt
 * 4. Shows "LEVEL!" when the device is flat
 *
 * ============================================================
 * WHAT ARE PITCH AND ROLL?
 * ============================================================
 * - Pitch: Rotation around the X-axis (forward/backward tilt)
 * - Roll: Rotation around the Y-axis (left/right tilt)
 * - When both are near 0°, the device is level!
 *
 * ============================================================
 * HOW THE BUBBLE MOVES:
 * ============================================================
 * - The bubble moves in the OPPOSITE direction of tilt
 * - If you tilt left, the bubble moves right (like a real bubble level)
 * - The bubble position is clamped to stay within the container
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class BubbleLevelActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Size of the bubble in pixels */
        private const val BUBBLE_SIZE_DP = 48

        /** Margin from container edges in pixels */
        private const val MARGIN_DP = 30

        /** Maximum angle for full bubble movement (degrees) */
        private const val MAX_ANGLE_DEGREES = 45f

        /** Threshold angle to consider device level (degrees) */
        private const val LEVEL_THRESHOLD_DEGREES = 2.0
    }

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

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bubble_level)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        getContainerDimensions()
    }

    override fun onResume() {
        super.onResume()
        // Register sensor listener when activity is visible
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister sensor listener to save battery
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // INITIALIZATION METHODS
    // ============================================================

    /**
     * Sets up the toolbar with title and back navigation.
     */
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.bubble_level_title)
    }

    /**
     * Initializes all UI views from the layout.
     */
    private fun initializeViews() {
        bubble = findViewById(R.id.bubble)
        levelContainer = findViewById(R.id.levelContainer)
        pitchText = findViewById(R.id.pitchText)
        rollText = findViewById(R.id.rollText)
        levelStatusText = findViewById(R.id.levelStatusText)
    }

    /**
     * Initializes the sensor manager and checks for accelerometer.
     */
    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            levelStatusText.text = getString(R.string.bubble_no_sensor)
        } else {
            levelStatusText.text = getString(R.string.bubble_tilt_hint)
        }
    }

    /**
     * Gets container dimensions after layout is complete.
     */
    private fun getContainerDimensions() {
        levelContainer.post {
            containerWidth = levelContainer.width
            containerHeight = levelContainer.height
        }
    }

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

    /**
     * onSensorChanged - Called when accelerometer data changes
     *
     * HOW IT WORKS:
     * 1. Gets X, Y, Z values from the accelerometer
     * 2. Calculates pitch (X-axis tilt) and roll (Y-axis tilt)
     * 3. Moves the bubble based on the angles
     * 4. Updates the UI with current values
     *
     * @param event The sensor event containing accelerometer data
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
            pitchText.text = String.format(
                getString(R.string.bubble_pitch_format),
                pitch
            )
            rollText.text = String.format(
                getString(R.string.bubble_roll_format),
                roll
            )

            // Check if the device is level
            val isLevel = kotlin.math.abs(pitch) < LEVEL_THRESHOLD_DEGREES &&
                    kotlin.math.abs(roll) < LEVEL_THRESHOLD_DEGREES

            // Update the status text and color
            updateLevelStatus(isLevel)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for accelerometer
    }

    // ============================================================
    // UI UPDATE METHODS
    // ============================================================

    /**
     * Updates the level status text and color based on whether device is level.
     *
     * @param isLevel True if device is level, false otherwise
     */
    private fun updateLevelStatus(isLevel: Boolean) {
        levelStatusText.text = if (isLevel) {
            getString(R.string.bubble_level)
        } else {
            getString(R.string.bubble_tilt_hint)
        }

        val color = if (isLevel) {
            ContextCompat.getColor(this, android.R.color.holo_green_dark)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        levelStatusText.setTextColor(color)
    }

    /**
     * updateBubble - Moves the bubble based on pitch and roll angles
     *
     * The bubble moves in the OPPOSITE direction of tilt:
     * - If you tilt left, the bubble moves right
     * - If you tilt forward, the bubble moves backward
     * - This mimics a real bubble level
     *
     * @param pitch The pitch angle in degrees (-90 to 90)
     * @param roll The roll angle in degrees (-90 to 90)
     */
    private fun updateBubble(pitch: Float, roll: Float) {
        // Get the container dimensions
        if (containerWidth == 0 || containerHeight == 0) {
            // Try again if dimensions aren't ready
            levelContainer.post {
                containerWidth = levelContainer.width
                containerHeight = levelContainer.height
                // Recursively call with same values after dimensions are set
                updateBubble(pitch, roll)
            }
            return
        }

        // Calculate bubble size and margin in pixels
        val bubbleSize = dpToPixels(BUBBLE_SIZE_DP)
        val margin = dpToPixels(MARGIN_DP)

        // Calculate the available space for the bubble to move
        val maxX = (containerWidth - bubbleSize) / 2f - margin
        val maxY = (containerHeight - bubbleSize) / 2f - margin

        // If no space to move, return
        if (maxX <= 0 || maxY <= 0) return

        // Map pitch and roll to bubble position
        // Clamp values to -1 to 1 range
        val normalizedPitch = (pitch / MAX_ANGLE_DEGREES).coerceIn(-1f, 1f)
        val normalizedRoll = (roll / MAX_ANGLE_DEGREES).coerceIn(-1f, 1f)

        // Calculate the bubble position
        // Note: We negate the values because the bubble moves in the OPPOSITE direction of tilt
        val xOffset = -normalizedPitch * maxX
        val yOffset = -normalizedRoll * maxY

        // Move the bubble using translation
        bubble.translationX = xOffset
        bubble.translationY = yOffset
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Converts dp to pixels.
     *
     * @param dp The value in dp
     * @return The value in pixels
     */
    private fun dpToPixels(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}