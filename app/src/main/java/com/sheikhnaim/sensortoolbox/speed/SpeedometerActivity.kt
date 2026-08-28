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
 * SpeedometerActivity - Shows current, max, and average speed using GPS
 *
 * HOW IT WORKS:
 * 1. Uses FusedLocationProviderClient to get GPS location updates
 * 2. Calculates speed from location data (m/s → km/h)
 * 3. Tracks max speed and average speed
 * 4. Start/Stop buttons control tracking
 *
 * WHAT IS SPEED?
 * - Speed is calculated from GPS location changes over time
 * - The Location object already provides speed in m/s
 * - We convert to km/h for display (m/s * 3.6 = km/h)
 */
class SpeedometerActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var speedText: TextView
    private lateinit var maxSpeedText: TextView
    private lateinit var avgSpeedText: TextView
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    // ============================================================
    // TRACKING DATA
    // ============================================================
    private var maxSpeed = 0f          // Highest speed reached (km/h)
    private var totalSpeed = 0f        // Sum of all speed readings (for average)
    private var speedCount = 0         // Number of readings taken
    private var currentSpeed = 0f      // Current speed (km/h)
    private var isTracking = false     // Whether tracking is active

    // ============================================================
    // LOCATION CALLBACK - Receives location updates
    // ============================================================
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                // Speed from GPS in m/s, convert to km/h
                val speedMps = location.speed
                currentSpeed = speedMps * 3.6f  // m/s → km/h
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
        setContentView(R.layout.activity_speedometer)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        speedText = findViewById(R.id.speedText)
        maxSpeedText = findViewById(R.id.maxSpeedText)
        avgSpeedText = findViewById(R.id.avgSpeedText)
        statusText = findViewById(R.id.statusText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

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

        // ============================================================
        // STEP 5: Initialize UI
        // ============================================================
        updateUI()
    }

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================

    /**
     * checkPermission - Checks if we have location permission
     */
    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * requestPermission - Requests location permission from the user
     */
    private fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
    }

    // ============================================================
    // TRACKING CONTROLS
    // ============================================================

    /**
     * startTracking - Begins tracking speed
     *
     * WHAT IT DOES:
     * 1. Checks permission
     * 2. Resets tracking data
     * 3. Requests location updates from GPS
     * 4. Updates button states
     */
    private fun startTracking() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        // Reset tracking data
        maxSpeed = 0f
        totalSpeed = 0f
        speedCount = 0
        currentSpeed = 0f
        isTracking = true

        // Update button states
        startButton.isEnabled = false
        stopButton.isEnabled = true
        statusText.text = "📡 Tracking... (GPS: Locking)"

        // Start receiving location updates
        startLocationUpdates()

        Toast.makeText(this, "✅ Tracking started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    /**
     * stopTracking - Stops tracking speed
     */
    private fun stopTracking() {
        isTracking = false
        stopLocationUpdates()

        // Update button states
        startButton.isEnabled = true
        stopButton.isEnabled = false
        statusText.text = "⏹️ Tracking stopped"

        Toast.makeText(this, "⏹️ Tracking stopped", Toast.LENGTH_SHORT).show()
    }

    // ============================================================
    // LOCATION UPDATES
    // ============================================================

    /**
     * startLocationUpdates - Registers for GPS location updates
     */
    private fun startLocationUpdates() {
        if (!checkPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000  // Update every 1 second
        ).apply {
            setMinUpdateIntervalMillis(1000)
            setMaxUpdateDelayMillis(2000)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
    }

    /**
     * stopLocationUpdates - Unregisters from GPS location updates
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    /**
     * updateUI - Updates all UI elements with current speed data
     */
    private fun updateUI() {
        // Display current speed
        speedText.text = String.format(Locale.US, "%.0f km/h", currentSpeed)

        // Update max speed if current is higher
        if (currentSpeed > maxSpeed) {
            maxSpeed = currentSpeed
        }

        // Accumulate for average speed
        if (currentSpeed > 0 && isTracking) {
            totalSpeed += currentSpeed
            speedCount++
        }

        // Display max speed
        maxSpeedText.text = String.format(Locale.US, "%.0f km/h", maxSpeed)

        // Calculate and display average speed
        val avgSpeed = if (speedCount > 0) {
            totalSpeed / speedCount
        } else {
            0f
        }
        avgSpeedText.text = String.format(Locale.US, "%.0f km/h", avgSpeed)

        // Update status
        if (isTracking) {
            statusText.text = "📡 Tracking... Speed: ${currentSpeed.toInt()} km/h"
        }

        // Color code the speed
        val color = when {
            currentSpeed > 100 -> android.R.color.holo_red_dark
            currentSpeed > 60 -> android.R.color.holo_orange_dark
            currentSpeed > 20 -> android.R.color.holo_green_dark
            else -> android.R.color.holo_blue_dark
        }
        speedText.setTextColor(getColor(color))
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

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}