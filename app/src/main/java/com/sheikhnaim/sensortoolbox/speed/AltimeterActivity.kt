package com.sheikhnaim.sensortoolbox.speed

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sheikhnaim.sensortoolbox.R
import java.util.Locale

/**
 * AltimeterActivity - Tracks altitude (elevation) using GPS
 *
 * HOW IT WORKS:
 * 1. Uses FusedLocationProviderClient to get GPS location updates
 * 2. Reads altitude from the Location object (location.altitude)
 * 3. Tracks elevation gain (climbing up) and loss (going down)
 * 4. Tracks min and max altitude reached
 * 5. Start/Stop/Reset controls
 *
 * WHAT IS ALTITUDE?
 * - Altitude is the height above sea level (in meters)
 * - GPS provides altitude as part of the location data
 * - Accuracy is typically ±10-30 meters with GPS
 */
class AltimeterActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var altitudeText: TextView
    private lateinit var gainText: TextView
    private lateinit var lossText: TextView
    private lateinit var minText: TextView
    private lateinit var maxText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var viewMapButton: Button

    // ============================================================
    // TRACKING DATA - Using Double for altitude values
    // ============================================================
    private var currentAltitude = 0.0          // Current altitude in meters (Double)
    private var minAltitude = Double.MAX_VALUE // Lowest altitude reached (Double)
    private var maxAltitude = Double.MIN_VALUE // Highest altitude reached (Double)
    private var elevationGain = 0.0            // Total gain (climbing up) (Double)
    private var elevationLoss = 0.0            // Total loss (going down) (Double)
    private var previousAltitude = 0.0         // Previous altitude reading (Double)
    private var isTracking = false             // Whether tracking is active
    private var firstReading = true            // First reading of the session
    private var lastLocation: Location? = null

    // ============================================================
    // LOCATION CALLBACK - Receives location updates
    // ============================================================
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                lastLocation = location
                // Get altitude from GPS (meters above sea level)
                // location.altitude returns a Double
                val altitude = location.altitude

                if (altitude > 0.0) { // Valid altitude reading
                    currentAltitude = altitude

                    if (firstReading) {
                        // First reading - set baseline
                        previousAltitude = altitude
                        minAltitude = altitude
                        maxAltitude = altitude
                        firstReading = false
                    } else {
                        // Calculate gain/loss since last reading
                        val difference = altitude - previousAltitude

                        if (difference > 0.0) {
                            // We went UP - gain
                            elevationGain += difference
                        } else if (difference < 0.0) {
                            // We went DOWN - loss
                            elevationLoss += kotlin.math.abs(difference)
                        }

                        // Update min and max
                        if (altitude < minAltitude) minAltitude = altitude
                        if (altitude > maxAltitude) maxAltitude = altitude

                        // Save for next comparison
                        previousAltitude = altitude
                    }

                    updateUI()
                }
            }
        }
    }

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startTracking()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
                statusText.text = "⚠️ Permission denied"
            }
        }

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_altimeter)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        altitudeText = findViewById(R.id.altitudeText)
        gainText = findViewById(R.id.gainText)
        lossText = findViewById(R.id.lossText)
        minText = findViewById(R.id.minText)
        maxText = findViewById(R.id.maxText)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        viewMapButton = findViewById(R.id.viewMapButton)

        // ============================================================
        // STEP 3: Initialize the Fused Location Client
        // ============================================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ============================================================
        // STEP 4: Set up button click listeners
        // ============================================================
        startButton.setOnClickListener {
            if (checkPermission()) {
                startTracking()
            } else {
                requestPermission()
            }
        }

        stopButton.setOnClickListener {
            stopTracking()
        }

        resetButton.setOnClickListener {
            resetTracking()
        }

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, com.sheikhnaim.sensortoolbox.MapActivity::class.java)
            if (lastLocation != null) {
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LATITUDE, lastLocation!!.latitude)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LONGITUDE, lastLocation!!.longitude)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TITLE, "🏔️ Altitude: ${String.format(Locale.US, "%.1f m", currentAltitude)}")
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_SNIPPET, "Gain: +${String.format(Locale.US, "%.1f m", elevationGain)} | Loss: -${String.format(Locale.US, "%.1f m", elevationLoss)}")
            }
            startActivity(intent)
        }

        // ============================================================
        // STEP 5: Initialize UI
        // ============================================================
        updateUI()
    }

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // ============================================================
    // TRACKING CONTROLS
    // ============================================================

    /**
     * startTracking - Begins tracking altitude
     */
    private fun startTracking() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        // Reset tracking data
        currentAltitude = 0.0
        minAltitude = Double.MAX_VALUE
        maxAltitude = Double.MIN_VALUE
        elevationGain = 0.0
        elevationLoss = 0.0
        previousAltitude = 0.0
        firstReading = true
        isTracking = true

        // Update button states
        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false
        statusText.text = "📡 Tracking altitude..."

        // Start receiving location updates
        startLocationUpdates()

        Toast.makeText(this, "✅ Altitude tracking started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    /**
     * stopTracking - Stops tracking altitude
     */
    private fun stopTracking() {
        isTracking = false
        stopLocationUpdates()

        // Update button states
        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = true
        statusText.text = "⏹️ Tracking stopped"

        Toast.makeText(this, "⏹️ Tracking stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    /**
     * resetTracking - Resets all tracking data
     */
    private fun resetTracking() {
        currentAltitude = 0.0
        minAltitude = Double.MAX_VALUE
        maxAltitude = Double.MIN_VALUE
        elevationGain = 0.0
        elevationLoss = 0.0
        previousAltitude = 0.0
        firstReading = true
        isTracking = false

        // Update button states
        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = false
        statusText.text = "🔄 Reset - Press Start to begin"

        Toast.makeText(this, "🔄 Reset", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    // ============================================================
    // LOCATION UPDATES
    // ============================================================

    private fun startLocationUpdates() {
        if (!checkPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000  // Update every 2 seconds
        ).apply {
            setMinUpdateIntervalMillis(2000)
            setMaxUpdateDelayMillis(5000)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    /**
     * updateUI - Updates all UI elements with current data
     */
    private fun updateUI() {
        // Display current altitude
        val altitudeDisplay = if (currentAltitude > 0.0) {
            String.format(Locale.US, "%.0f m", currentAltitude)
        } else {
            "-- m"
        }
        altitudeText.text = altitudeDisplay

        // Display elevation gain and loss
        gainText.text = String.format(Locale.US, "%.0f m", elevationGain)
        lossText.text = String.format(Locale.US, "%.0f m", elevationLoss)

        // Display min and max altitude
        val minDisplay = if (minAltitude != Double.MAX_VALUE) {
            String.format(Locale.US, "%.0f m", minAltitude)
        } else {
            "-- m"
        }
        minText.text = minDisplay

        val maxDisplay = if (maxAltitude != Double.MIN_VALUE) {
            String.format(Locale.US, "%.0f m", maxAltitude)
        } else {
            "-- m"
        }
        maxText.text = maxDisplay

        // Update status
        if (isTracking) {
            statusText.text = "📡 Tracking... Altitude: ${altitudeText.text}"
        }
    }

    // ============================================================
    // LIFECYCLE - Clean up when activity is paused
    // ============================================================

    override fun onPause() {
        super.onPause()
        if (isTracking) {
            stopTracking()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isTracking) {
            stopTracking()
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