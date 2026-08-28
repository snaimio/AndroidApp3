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
import com.sheikhnaim.sensortoolbox.R             // Resource IDs
import kotlin.math.sqrt                            // For square root calculation

/**
 * GravityMeterActivity - Measures gravitational force using the accelerometer
 *
 * HOW IT WORKS:
 * 1. Uses the Accelerometer to measure acceleration forces
 * 2. Calculates total force: sqrt(x² + y² + z²)
 * 3. On Earth, this should be approximately 9.81 m/s² (when stationary)
 * 4. Detects device orientation based on which axis has the highest value
 * 5. Reset button saves the current reading as a baseline for comparison
 *
 * WHAT DO THE VALUES MEAN?
 * - X-Axis: Left/Right acceleration
 * - Y-Axis: Forward/Backward acceleration
 * - Z-Axis: Up/Down acceleration (gravity is usually here when face up)
 *
 * EARTH'S GRAVITY = 9.81 m/s²
 * - If total ≈ 9.81: Device is stationary on Earth
 * - If total < 9.81: Device is in free-fall
 * - If total > 9.81: Device is accelerating (moving)
 */
class GravityMeterActivity : AppCompatActivity(), SensorEventListener {

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
    private var baseline = 9.81f    // Default: Earth's gravity
    private var hasBaseline = false // Tracks if user has set a baseline

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

        // ============================================================
        // STEP 1: Set up the Toolbar
        // The toolbar has the title and back button
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // Each view is found by its ID from activity_gravity_meter.xml
        // ============================================================
        totalForceText = findViewById(R.id.totalForceText)
        xAxisText = findViewById(R.id.xAxisText)
        yAxisText = findViewById(R.id.yAxisText)
        zAxisText = findViewById(R.id.zAxisText)
        orientationText = findViewById(R.id.orientationText)
        baselineText = findViewById(R.id.baselineText)      // NEW: Baseline display
        resetButton = findViewById(R.id.resetButton)        // NEW: Reset button

        // ============================================================
        // STEP 3: Initialize the Sensor Manager
        // ============================================================
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // ============================================================
        // STEP 4: Check if sensor is available
        // ============================================================
        if (accelerometer == null) {
            totalForceText.text = "❌"
            Toast.makeText(this, "Accelerometer not found", Toast.LENGTH_LONG).show()
        }

        // ============================================================
        // STEP 5: Set up the Reset Button
        // When clicked, it saves the current reading as the baseline
        // ============================================================
        resetButton.setOnClickListener {
            // Get the current total force from the text display
            val currentTotal = totalForceText.text.toString()
                .replace(" m/s²", "")  // Remove the unit
                .toFloatOrNull()       // Convert to Float

            if (currentTotal != null && currentTotal > 0) {
                // Save the current reading as the new baseline
                baseline = currentTotal
                hasBaseline = true
                baselineText.text = String.format("Baseline: %.2f m/s²", baseline)
                Toast.makeText(
                    this,
                    "✅ Baseline set to %.2f m/s²".format(baseline),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // If no reading is available yet
                Toast.makeText(
                    this,
                    "⏳ Wait for a reading first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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

    /**
     * onSensorChanged - Called every time a sensor value changes
     *
     * HOW IT WORKS:
     * 1. Gets X, Y, Z values from the accelerometer
     * 2. Calculates total force using Pythagorean theorem
     * 3. Updates the UI with all values
     * 4. Detects device orientation based on Z-axis value
     * 5. Colors the total force based on how close it is to baseline
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Get acceleration values in m/s²
            val x = event.values[0]  // Left/Right
            val y = event.values[1]  // Forward/Backward
            val z = event.values[2]  // Up/Down (gravity is here when face up)

            // Calculate total gravitational force using Pythagorean theorem
            // total = sqrt(x² + y² + z²)
            val total = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

            // ============================================================
            // UPDATE UI WITH VALUES
            // ============================================================
            // Display total force (2 decimal places)
            totalForceText.text = String.format("%.2f m/s²", total)

            // Display individual axis values (2 decimal places)
            xAxisText.text = String.format("%.2f m/s²", x)
            yAxisText.text = String.format("%.2f m/s²", y)
            zAxisText.text = String.format("%.2f m/s²", z)

            // ============================================================
            // DETECT ORIENTATION
            // The Z-axis tells us if the device is face up or face down
            // - Z ≈ +9.81: Face Up (lying flat on a table)
            // - Z ≈ -9.81: Face Down
            // - X or Y ≈ 9.81: Standing up (portrait or landscape)
            // ============================================================
            orientationText.text = when {
                z > 7 -> "📱 Face Up"
                z < -7 -> "📱 Face Down"
                kotlin.math.abs(x) > 5 -> "📱 Portrait (Standing)"
                kotlin.math.abs(y) > 5 -> "📱 Landscape (Standing)"
                else -> "📱 Tilted"
            }

            // ============================================================
            // COLOR CODE THE TOTAL FORCE
            // Compares the current reading to the BASELINE (not fixed 9.81)
            // - Green: Close to baseline (normal)
            // - Orange: Slight deviation
            // - Red: Significant deviation (moving or falling)
            // ============================================================
            val color = when {
                kotlin.math.abs(total - baseline) < 0.3f -> android.R.color.holo_green_dark
                kotlin.math.abs(total - baseline) < 1.0f -> android.R.color.holo_orange_dark
                else -> android.R.color.holo_red_dark
            }
            totalForceText.setTextColor(getColor(color))
        }
    }

    /**
     * onAccuracyChanged - Called when sensor accuracy changes
     * (Not needed for this app, but required by the interface)
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
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