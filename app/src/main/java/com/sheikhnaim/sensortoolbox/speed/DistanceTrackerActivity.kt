package com.sheikhnaim.sensortoolbox.speed

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
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
 * DistanceTrackerActivity - Tracks distance traveled using GPS
 *
 * HOW IT WORKS:
 * 1. Uses FusedLocationProviderClient to get GPS location updates
 * 2. Calculates distance between consecutive GPS points
 * 3. Accumulates total distance
 * 4. Tracks elapsed time and average speed
 * 5. Start/Stop/Reset controls
 *
 * HOW DISTANCE IS CALCULATED:
 * - Each time GPS updates, we get a new Location
 * - We calculate distance between previous and current location
 * - Add that distance to the total
 * - This gives us the total distance traveled!
 */
class DistanceTrackerActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var avgSpeedText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button
    private lateinit var viewMapButton: Button

    // ============================================================
    // TRACKING DATA
    // ============================================================
    private var totalDistance = 0f           // Total distance in meters
    private var elapsedTime = 0L            // Elapsed time in milliseconds
    private var startTime = 0L              // When tracking started
    private var isTracking = false          // Whether tracking is active
    private var lastLocation: Location? = null // Previous GPS location
    private val routePoints = mutableListOf<Location>()

    private var speedSum = 0f               // Sum of speeds for average
    private var speedCount = 0              // Number of speed readings

    // ============================================================
    // LOCATION CALLBACK - Receives location updates
    // ============================================================
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                if (lastLocation != null && isTracking) {
                    // Calculate distance between last and current location
                    val distance = lastLocation!!.distanceTo(location)
                    totalDistance += distance
                    routePoints.add(location)

                    // Calculate speed from this location
                    val speed = location.speed * 3.6f  // m/s → km/h
                    if (speed > 0) {
                        speedSum += speed
                        speedCount++
                    }
                } else if (isTracking) {
                    routePoints.add(location)
                }
                // Save this location for the next update
                lastLocation = location
                updateUI()
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
        setContentView(R.layout.activity_distance_tracker)

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
        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        avgSpeedText = findViewById(R.id.avgSpeedText)
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
            if (routePoints.isNotEmpty()) {
                val lats = DoubleArray(routePoints.size) { routePoints[it].latitude }
                val lons = DoubleArray(routePoints.size) { routePoints[it].longitude }
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TRAIL_LATS, lats)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TRAIL_LONS, lons)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TITLE, "📏 Distance Route (${String.format(Locale.US, "%.2f km", totalDistance / 1000f)})")
            } else if (lastLocation != null) {
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LATITUDE, lastLocation!!.latitude)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LONGITUDE, lastLocation!!.longitude)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TITLE, "📏 Distance Tracker Position")
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
     * startTracking - Begins tracking distance
     */
    private fun startTracking() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        // Reset tracking data
        totalDistance = 0f
        elapsedTime = 0L
        startTime = SystemClock.elapsedRealtime()
        lastLocation = null
        speedSum = 0f
        speedCount = 0
        isTracking = true

        // Update button states
        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false
        statusText.text = "📡 Tracking... (GPS: Locking)"

        // Start receiving location updates
        startLocationUpdates()

        Toast.makeText(this, "✅ Tracking started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    /**
     * stopTracking - Stops tracking distance
     */
    private fun stopTracking() {
        isTracking = false
        stopLocationUpdates()

        // Update button states
        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = true
        statusText.text = "⏹️ Tracking stopped"

        // Save final elapsed time
        elapsedTime = SystemClock.elapsedRealtime() - startTime

        Toast.makeText(this, "⏹️ Tracking stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    /**
     * resetTracking - Resets all tracking data
     */
    private fun resetTracking() {
        totalDistance = 0f
        elapsedTime = 0L
        startTime = 0L
        lastLocation = null
        speedSum = 0f
        speedCount = 0
        isTracking = false
        routePoints.clear()

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
        // Display total distance in kilometers
        val distanceKm = totalDistance / 1000
        distanceText.text = String.format(Locale.US, "%.2f km", distanceKm)

        // Display elapsed time
        if (isTracking) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
        }

        val seconds = elapsedTime / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        val hours = minutes / 60
        val mins = minutes % 60
        timeText.text = String.format("%02d:%02d:%02d", hours, mins, secs)

        // Calculate and display average speed
        val avgSpeed = if (speedCount > 0) {
            speedSum / speedCount
        } else {
            0f
        }
        avgSpeedText.text = String.format(Locale.US, "%.1f km/h", avgSpeed)

        // Update status
        if (isTracking) {
            statusText.text = "📡 Tracking... Distance: %.2f km".format(distanceKm)
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
        // Ensure cleanup
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