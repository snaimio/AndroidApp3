package com.sheikhnaim.sensortoolbox.motion

// ============================================================
// IMPORTS
// ============================================================
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
import androidx.core.content.ContextCompat
import com.sheikhnaim.sensortoolbox.R
import kotlin.math.sqrt

/**
 * ShakeDetectorActivity - Detects when the phone is shaken
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Accelerometer to detect sudden movements
 * 2. Calculates the total acceleration (like Gravity Meter)
 * 3. When acceleration exceeds a threshold, counts it as a shake
 * 4. Prevents multiple detections in quick succession (debounce)
 * 5. Tracks intensity of the shake
 *
 * ============================================================
 * WHAT IS A SHAKE?
 * ============================================================
 * - A sudden change in acceleration (like you're shaking the phone)
 * - The phone moves quickly in one direction, then another
 * - The total acceleration exceeds a certain threshold
 *
 * ============================================================
 * WHY DEBOUNCE?
 * ============================================================
 * - One shake can trigger multiple sensor readings
 * - We only count one shake per 500ms to avoid duplicates
 *
 * ============================================================
 * INTENSITY LEVELS:
 * ============================================================
 * - 💥 Very High: total > 30 m/s²
 * - 🔥 High: total > 25 m/s²
 * - 📶 Medium: total > 20 m/s²
 * - 📶 Low: total <= 20 m/s²
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class ShakeDetectorActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Minimum time between shakes to avoid duplicates (milliseconds) */
        private const val DEBOUNCE_TIME_MS = 500L

        /** Minimum acceleration to count as a shake (m/s²) */
        private const val SHAKE_THRESHOLD = 15f

        /** Threshold for "Very High" intensity (m/s²) */
        private const val INTENSITY_VERY_HIGH = 30f

        /** Threshold for "High" intensity (m/s²) */
        private const val INTENSITY_HIGH = 25f

        /** Threshold for "Medium" intensity (m/s²) */
        private const val INTENSITY_MEDIUM = 20f
    }

    // ============================================================
    // SENSOR MANAGER
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ============================================================
    // UI VIEWS - All are TextViews
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

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shake_detector)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupResetButton()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // INITIALIZATION METHODS
    // ============================================================

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.shake_detector_title)
    }

    private fun initializeViews() {
        shakeCountText = findViewById(R.id.shakeCountText)
        lastShakeText = findViewById(R.id.lastShakeText)
        intensityText = findViewById(R.id.intensityText)
        statusText = findViewById(R.id.statusText)
        resetButton = findViewById(R.id.resetButton)
    }

    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            // ✅ Using getString() and assigning to TextView.text
            statusText.text = getString(R.string.shake_no_sensor)
            Toast.makeText(
                this,
                getString(R.string.shake_sensor_not_found),
                Toast.LENGTH_LONG
            ).show()
        } else {
            // ✅ Using getString() and assigning to TextView.text
            statusText.text = getString(R.string.shake_hint)
        }
    }

    private fun setupResetButton() {
        resetButton.setOnClickListener {
            // Reset all shake data
            shakeCount = 0
            lastShakeTime = 0L

            // ✅ Reset all TextViews using getString()
            shakeCountText.text = getString(R.string.shake_zero)
            lastShakeText.text = getString(R.string.shake_default)
            intensityText.text = getString(R.string.shake_default)
            statusText.text = getString(R.string.shake_reset_message)

            Toast.makeText(
                this,
                getString(R.string.shake_reset_toast),
                Toast.LENGTH_SHORT
            ).show()
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

        // ✅ Update all TextViews using getString()
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