package com.sheikhnaim.sensortoolbox.motion

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.hardware.Sensor                      // Represents a physical sensor
import android.hardware.SensorEvent                // Contains sensor data when it changes
import android.hardware.SensorEventListener        // Interface to listen for sensor changes
import android.hardware.SensorManager              // Manages sensors on the device
import android.os.Bundle                           // For saving/restoring state
import android.widget.Button                       // For button views
import android.widget.TextView                     // For text views
import android.widget.Toast                        // For showing toast messages
import androidx.appcompat.app.AppCompatActivity     // Base class for our activity
import androidx.appcompat.widget.Toolbar           // The top bar with back button
import androidx.core.content.ContextCompat          // ✅ ADDED: For getColor() replacement
import com.sheikhnaim.sensortoolbox.R             // Resource IDs
import kotlin.math.sqrt                            // For square root calculation

/**
 * GravityMeterActivity - Measures gravitational force using the accelerometer
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses the Accelerometer to measure acceleration forces
 * 2. Calculates total force: sqrt(x² + y² + z²)
 * 3. On Earth, this should be approximately 9.81 m/s² (when stationary)
 * 4. Detects device orientation based on which axis has the highest value
 * 5. Reset button saves the current reading as a baseline for comparison
 *
 * ============================================================
 * WHAT DO THE VALUES MEAN?
 * ============================================================
 * - X-Axis: Left/Right acceleration
 * - Y-Axis: Forward/Backward acceleration
 * - Z-Axis: Up/Down acceleration (gravity is usually here when face up)
 *
 * ============================================================
 * EARTH'S GRAVITY = 9.81 m/s²
 * ============================================================
 * - If total ≈ 9.81: Device is stationary on Earth
 * - If total < 9.81: Device is in free-fall
 * - If total > 9.81: Device is accelerating (moving)
 *
 * ============================================================
 * COLOR CODING (Based on Baseline):
 * ============================================================
 * - GREEN: Close to baseline (normal)
 * - ORANGE: Slight deviation
 * - RED: Significant deviation (moving or falling)
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class GravityMeterActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Earth's standard gravity (m/s²) */
        private const val EARTH_GRAVITY = 9.81f

        /** Threshold for Face Up/Down detection (m/s²) */
        private const val FACE_THRESHOLD = 7.0f

        /** Threshold for Standing orientation detection (m/s²) */
        private const val STANDING_THRESHOLD = 5.0f

        /** Threshold for Green color (close to baseline) */
        private const val GREEN_THRESHOLD = 0.3f

        /** Threshold for Orange color (slight deviation) */
        private const val ORANGE_THRESHOLD = 1.0f
    }

    // ============================================================
    // SENSOR MANAGER - Gets sensor data from the device
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // ============================================================
    // UI VIEWS - References to the layout elements
    // ============================================================
    private lateinit var totalForceText: TextView       // Shows total gravitational force
    private lateinit var xAxisText: TextView            // Shows X-axis acceleration
    private lateinit var yAxisText: TextView            // Shows Y-axis acceleration
    private lateinit var zAxisText: TextView            // Shows Z-axis acceleration
    private lateinit var orientationText: TextView      // Shows device orientation
    private lateinit var baselineText: TextView         // Shows the saved baseline value
    private lateinit var resetButton: Button            // Button to save baseline

    // ============================================================
    // BASELINE - The "normal" gravity reading to compare against
    // ============================================================
    private var baseline = EARTH_GRAVITY  // Default: Earth's gravity
    private var hasBaseline = false       // Tracks if user has set a baseline
    private var currentTotal = 0f         // Stores the current total for baseline

    /**
     * onCreate - Called when the activity is first created
     *
     * This sets up:
     * - The layout (UI)
     * - The toolbar (top bar with back button)
     * - All UI views
     * - The sensor manager and sensors
     * - The reset button functionality
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gravity_meter)

        setupToolbar()
        initializeViews()
        setupSensorManager()
        setupResetButton()
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
        supportActionBar?.title = getString(R.string.gravity_meter_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    /**
     * Initializes all UI views from the layout.
     */
    private fun initializeViews() {
        totalForceText = findViewById(R.id.totalForceText)
        xAxisText = findViewById(R.id.xAxisText)
        yAxisText = findViewById(R.id.yAxisText)
        zAxisText = findViewById(R.id.zAxisText)
        orientationText = findViewById(R.id.orientationText)
        baselineText = findViewById(R.id.baselineText)
        resetButton = findViewById(R.id.resetButton)

        // Show initial baseline
        baselineText.text = String.format(
            getString(R.string.gravity_baseline_format),
            baseline
        )
    }

    /**
     * Initializes the sensor manager and checks for accelerometer.
     */
    private fun setupSensorManager() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            totalForceText.text = getString(R.string.gravity_no_sensor)
            Toast.makeText(
                this,
                R.string.gravity_sensor_not_found,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Sets up the reset button to save current reading as baseline.
     */
    private fun setupResetButton() {
        resetButton.setOnClickListener {
            if (currentTotal > 0) {
                // Save the current reading as the new baseline
                baseline = currentTotal
                hasBaseline = true
                baselineText.text = String.format(
                    getString(R.string.gravity_baseline_format),
                    baseline
                )
                Toast.makeText(
                    this,
                    getString(R.string.gravity_baseline_set, baseline),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // If no reading is available yet
                Toast.makeText(
                    this,
                    R.string.gravity_wait_for_reading,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    /**
     * onResume - Register the sensor listener when the app is in the foreground
     * This is important to save battery - we only listen when the user can see the app
     */
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * onPause - Unregister sensor listeners when the app goes to the background
     * This saves battery and prevents sensor conflicts with other apps
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // SENSOR EVENT LISTENER METHODS
    // ============================================================

    /**
     * onSensorChanged - Called every time a sensor value changes
     *
     * HOW IT WORKS:
     * 1. Gets X, Y, Z values from the accelerometer
     * 2. Calculates total force using Pythagorean theorem
     * 3. Updates the UI with all values
     * 4. Detects device orientation based on Z-axis value
     * 5. Colors the total force based on how close it is to baseline
     *
     * @param event The sensor event containing accelerometer data
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Get acceleration values in m/s²
            val x = event.values[0]  // Left/Right
            val y = event.values[1]  // Forward/Backward
            val z = event.values[2]  // Up/Down (gravity is here when face up)

            // Calculate total gravitational force using Pythagorean theorem
            // total = sqrt(x² + y² + z²)
            currentTotal = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // Update UI
            updateAxisValues(x, y, z)
            updateTotalForce(currentTotal)
            updateOrientation(x, y, z)
            updateColor(currentTotal)
        }
    }

    /**
     * onAccuracyChanged - Called when sensor accuracy changes
     * (Not needed for this app, but required by the interface)
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for accelerometer
    }

    // ============================================================
    // UI UPDATE METHODS
    // ============================================================

    /**
     * Updates the axis value displays.
     *
     * @param x X-axis acceleration (m/s²)
     * @param y Y-axis acceleration (m/s²)
     * @param z Z-axis acceleration (m/s²)
     */
    private fun updateAxisValues(x: Float, y: Float, z: Float) {
        xAxisText.text = String.format(
            getString(R.string.gravity_axis_format),
            x
        )
        yAxisText.text = String.format(
            getString(R.string.gravity_axis_format),
            y
        )
        zAxisText.text = String.format(
            getString(R.string.gravity_axis_format),
            z
        )
    }

    /**
     * Updates the total force display.
     *
     * @param total The total gravitational force (m/s²)
     */
    private fun updateTotalForce(total: Float) {
        totalForceText.text = String.format(
            getString(R.string.gravity_total_format),
            total
        )
    }

    /**
     * Detects and updates the device orientation.
     *
     * @param x X-axis acceleration (m/s²)
     * @param y Y-axis acceleration (m/s²)
     * @param z Z-axis acceleration (m/s²)
     */
    private fun updateOrientation(x: Float, y: Float, z: Float) {
        orientationText.text = when {
            z > FACE_THRESHOLD -> getString(R.string.gravity_face_up)
            z < -FACE_THRESHOLD -> getString(R.string.gravity_face_down)
            kotlin.math.abs(x) > STANDING_THRESHOLD -> getString(R.string.gravity_portrait)
            kotlin.math.abs(y) > STANDING_THRESHOLD -> getString(R.string.gravity_landscape)
            else -> getString(R.string.gravity_tilted)
        }
    }

    /**
     * Updates the color of the total force text based on baseline deviation.
     *
     * @param total The current total gravitational force (m/s²)
     */
    private fun updateColor(total: Float) {
        val deviation = kotlin.math.abs(total - baseline)
        val colorResId = when {
            deviation < GREEN_THRESHOLD -> android.R.color.holo_green_dark
            deviation < ORANGE_THRESHOLD -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        totalForceText.setTextColor(ContextCompat.getColor(this, colorResId))
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    /**
     * onSupportNavigateUp - Handles the back button in the toolbar
     * Returns to the DashboardActivity
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}