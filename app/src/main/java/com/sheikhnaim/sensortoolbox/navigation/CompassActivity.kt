package com.sheikhnaim.sensortoolbox.navigation

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.hardware.Sensor                      // Represents a physical sensor (accelerometer, magnetometer)
import android.hardware.SensorEvent                // Contains sensor data when it changes
import android.hardware.SensorEventListener        // Interface to listen for sensor changes
import android.hardware.SensorManager              // Manages sensors on the device
import android.os.Bundle                           // For saving/restoring state
import android.widget.Button                       // For interactive buttons
import android.widget.ImageView                    // To display the compass image
import android.widget.TextView                     // To display text on screen
import android.widget.Toast                        // For showing toast messages
import androidx.appcompat.app.AppCompatActivity     // Base class for our activity
import androidx.appcompat.widget.Toolbar           // The top bar with back button
import com.sheikhnaim.sensortoolbox.R             // Resource IDs (layouts, strings, etc.)
import kotlin.math.roundToInt                      // For rounding numbers

/**
 * CompassActivity - Digital Compass Tool
 *
 * HOW THE COMPASS WORKS:
 * 1. Uses TWO sensors: Accelerometer and Magnetometer
 * 2. Combines them using sensor fusion (getRotationMatrix + getOrientation)
 * 3. Calculates the device's bearing (0-360°)
 * 4. Rotates the compass image to match the bearing
 * 5. Displays the bearing and cardinal direction
 *
 * WHY SENSOR FUSION?
 * - Accelerometer alone: Only detects tilt, not direction
 * - Magnetometer alone: Affected by phone tilt
 * - BOTH together: Tilt-compensated compass (accurate!)
 *
 * This is the SAME technique used in the LocationFinder tutorial!
 */
class CompassActivity : AppCompatActivity(), SensorEventListener {

    // ============================================================
    // SENSOR MANAGER - Gets sensor data from the device
    // ============================================================
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null      // Detects tilt/gravity
    private var magnetometer: Sensor? = null       // Detects magnetic field (direction)

    // ============================================================
    // UI VIEWS - References to the layout elements
    // ============================================================
    private lateinit var compassImage: ImageView      // The rotating compass image
    private lateinit var bearingText: TextView        // Shows the bearing in degrees
    private lateinit var directionText: TextView      // Shows cardinal direction (N, NE, etc.)
    private lateinit var statusText: TextView         // Shows status messages
    private lateinit var viewMapButton: Button

    // ============================================================
    // SENSOR DATA - Arrays to store sensor readings
    // ============================================================
    private val gravity = FloatArray(3)      // Accelerometer readings (X, Y, Z)
    private val magnetic = FloatArray(3)     // Magnetometer readings (X, Y, Z)
    private val rotationMatrix = FloatArray(9)  // Rotation matrix from sensor fusion
    private val orientation = FloatArray(3)     // Orientation angles (azimuth, pitch, roll)

    // Flags to track if we have received data from each sensor
    private var haveGravity = false          // True when we have accelerometer data
    private var haveMagnetic = false         // True when we have magnetometer data
    private var currentAzimuth = 0f

    // ============================================================
    // LIFECYCLE METHODS - When the activity starts/stops
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        compassImage = findViewById(R.id.compassImage)
        bearingText = findViewById(R.id.bearingText)
        directionText = findViewById(R.id.directionText)
        statusText = findViewById(R.id.statusText)
        viewMapButton = findViewById(R.id.viewMapButton)

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, com.sheikhnaim.sensortoolbox.MapActivity::class.java)
            intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_BEARING, currentAzimuth)
            intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_HEADING_LOCK, true)
            intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TITLE, "🧭 Compass Heading: ${currentAzimuth.toInt()}° (${directionText.text})")
            startActivity(intent)
        }

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer == null) {
            statusText.text = "⚠️ Accelerometer not available"
            Toast.makeText(this, "Accelerometer not found", Toast.LENGTH_LONG).show()
        }

        if (magnetometer == null) {
            statusText.text = "⚠️ Magnetometer not available"
            Toast.makeText(this, "Magnetometer not found", Toast.LENGTH_LONG).show()
        }

        if (accelerometer != null && magnetometer != null) {
            statusText.text = "✅ Sensors ready! Move device to calibrate."
        }
    }

    /**
     * onResume - Register sensor listeners when the app is in the foreground
     *
     * This is important to save battery - we only listen when the user can see the app.
     * When the app goes to the background, we unregister in onPause().
     */
    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    /**
     * onPause - Unregister sensor listeners when the app goes to the background
     *
     * This saves battery and prevents sensor conflicts with other apps.
     */
    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    // ============================================================
    // SENSOR EVENT HANDLING - Called when sensor data changes
    // ============================================================

    /**
     * onSensorChanged - Called every time a sensor value changes
     *
     * HOW IT WORKS:
     * 1. When accelerometer data arrives -> store it in gravity[]
     * 2. When magnetometer data arrives -> store it in magnetic[]
     * 3. Once we have BOTH sensor readings -> calculate the bearing
     */
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Copy accelerometer values into gravity array
                System.arraycopy(event.values, 0, gravity, 0, 3)
                haveGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                // Copy magnetometer values into magnetic array
                System.arraycopy(event.values, 0, magnetic, 0, 3)
                haveMagnetic = true
            }
        }

        // Only calculate bearing when we have data from BOTH sensors
        if (haveGravity && haveMagnetic) {
            calculateBearing()
        }
    }

    /**
     * onAccuracyChanged - Called when sensor accuracy changes
     * (Not needed for this app, but required by the interface)
     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed - we don't need to track accuracy changes
    }

    // ============================================================
    // BEARING CALCULATION - The heart of the compass
    // ============================================================

    /**
     * calculateBearing - Computes the device's bearing using sensor fusion
     *
     * STEP BY STEP:
     * 1. getRotationMatrix() - Combines accelerometer + magnetometer data
     *    to create a rotation matrix (tells us how the device is oriented)
     * 2. getOrientation() - Converts the rotation matrix to angles
     * 3. The first angle (orientation[0]) is the AZIMUTH (bearing)
     * 4. Convert from radians to degrees
     * 5. Normalize to 0-360°
     * 6. Update the UI
     *
     * This is EXACTLY the technique used in the professor's LocationFinder!
     */
    private fun calculateBearing() {
        // Step 1: Get the rotation matrix
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,  // Output: rotation matrix
            null,           // Output: inclination matrix (not needed)
            gravity,        // Input: accelerometer data
            magnetic        // Input: magnetometer data
        )

        // If we can't calculate the rotation matrix, exit
        if (!success) {
            statusText.text = "⚠️ Rotation matrix failed"
            return
        }

        // Step 2: Get the orientation angles from the rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientation)

        // Step 3: Get the azimuth (bearing) from orientation[0]
        // This value is in radians, so we convert to degrees
        var bearing = Math.toDegrees(orientation[0].toDouble())

        // Step 4: Normalize to 0-360° (not -180 to 180)
        if (bearing < 0) {
            bearing += 360.0
        }

        // Step 5: Round to nearest integer for display
        val roundedBearing = bearing.roundToInt()

        // Step 6: Update the UI
        updateUI(roundedBearing, bearing.toFloat())
    }

    // ============================================================
    // UI UPDATES - Display the bearing and rotate the compass
    // ============================================================

    /**
     * updateUI - Updates all UI elements with the current bearing
     *
     * @param bearing The bearing in degrees (0-360)
     * @param bearingFloat The bearing as a float for rotation
     */
    private fun updateUI(bearing: Int, bearingFloat: Float) {
        currentAzimuth = bearingFloat
        // Display the bearing in degrees
        bearingText.text = "$bearing°"

        // Get the cardinal direction (N, NE, E, etc.)
        val direction = getDirection(bearing)
        directionText.text = direction

        // Rotate the compass image
        // Note: Negative rotation because the image points NORTH (0°),
        // but the device bearing is measured from North.
        compassImage.rotation = -bearingFloat

        // Update status
        statusText.text = "📡 Bearing: $bearing° $direction"
    }

    /**
     * getDirection - Converts a bearing to a cardinal direction
     *
     * @param bearing The bearing in degrees (0-360)
     * @return The cardinal direction (N, NE, E, SE, S, SW, W, NW)
     */
    private fun getDirection(bearing: Int): String {
        return when (bearing) {
            // North (0° ± 22.5°)
            in 0..22 -> "North"
            // Northeast (22.5° - 67.5°)
            in 23..67 -> "Northeast"
            // East (67.5° - 112.5°)
            in 68..112 -> "East"
            // Southeast (112.5° - 157.5°)
            in 113..157 -> "Southeast"
            // South (157.5° - 202.5°)
            in 158..202 -> "South"
            // Southwest (202.5° - 247.5°)
            in 203..247 -> "Southwest"
            // West (247.5° - 292.5°)
            in 248..292 -> "West"
            // Northwest (292.5° - 337.5°)
            in 293..337 -> "Northwest"
            // North (337.5° - 360°)
            else -> "North"
        }
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