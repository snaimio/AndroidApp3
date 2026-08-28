package com.sheikhnaim.sensortoolbox.location

// ============================================================
// IMPORTS
// ============================================================
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.view.View
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
 * TrailTrackerActivity - Records and displays hiking trails
 *
 * HOW IT WORKS:
 * 1. Uses GPS to record location points
 * 2. Draws the trail path on a view
 * 3. Tracks distance, time, and elevation
 * 4. Start/Stop/Reset controls
 */
class TrailTrackerActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var trailView: View
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var elevationText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button

    // ============================================================
    // TRAIL DATA
    // ============================================================
    private val locations = mutableListOf<Location>()  // All GPS points
    private var totalDistance = 0f                      // Total distance in meters
    private var elapsedTime = 0L                       // Elapsed time in milliseconds
    private var startTime = 0L                         // When tracking started
    private var isTracking = false                     // Whether tracking is active
    private var startElevation = 0f                    // Starting elevation
    private var elevationGain = 0f                     // Total elevation gain

    // Drawing tools for the trail view
    private var pathPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 8f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true                             // ✅ FIXED: isAntiAlias
    }
    private var trailPath = Path()

    // ============================================================
    // LOCATION CALLBACK
    // ============================================================
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                if (isTracking) {
                    addLocation(location)
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
            }
        }

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_tracker)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        trailView = findViewById(R.id.trailView)
        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        elevationText = findViewById(R.id.elevationText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)

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
            resetTrail()
        }

        // ============================================================
        // STEP 5: Initialize UI
        // ============================================================
        updateUI()
        trailView.invalidate()
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

    private fun startTracking() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        locations.clear()
        totalDistance = 0f
        elapsedTime = 0L
        startTime = SystemClock.elapsedRealtime()
        elevationGain = 0f
        startElevation = 0f
        trailPath.reset()
        isTracking = true

        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false

        startLocationUpdates()

        Toast.makeText(this, "✅ Trail recording started", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopTracking() {
        isTracking = false
        stopLocationUpdates()
        elapsedTime = SystemClock.elapsedRealtime() - startTime

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = true

        Toast.makeText(this, "⏹️ Trail recording stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun resetTrail() {
        locations.clear()
        totalDistance = 0f
        elapsedTime = 0L
        elevationGain = 0f
        startElevation = 0f
        trailPath.reset()
        isTracking = false

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = false

        Toast.makeText(this, "🔄 Trail reset", Toast.LENGTH_SHORT).show()
        updateUI()
        trailView.invalidate()
    }

    // ============================================================
    // LOCATION UPDATES
    // ============================================================

    private fun startLocationUpdates() {
        if (!checkPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            2000
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
    // TRAIL DATA MANAGEMENT
    // ============================================================

    /**
     * addLocation - Adds a location point to the trail
     */
    private fun addLocation(location: Location) {
        if (locations.isEmpty()) {
            // First location - set starting elevation
            startElevation = location.altitude.toFloat()   // ✅ FIXED: toFloat()
            locations.add(location)
            updateUI()
            return
        }

        val lastLocation = locations.last()

        // Calculate distance from last point
        val distance = lastLocation.distanceTo(location)
        if (distance > 1.0) {  // Only add if more than 1 meter away
            totalDistance += distance
            locations.add(location)

            // Calculate elevation gain
            val elevationDiff = location.altitude - lastLocation.altitude
            if (elevationDiff > 0) {
                elevationGain += elevationDiff.toFloat()   // ✅ FIXED: toFloat()
            }

            // Update the trail path for drawing
            updateTrailPath()
            updateUI()
        }
    }

    /**
     * updateTrailPath - Updates the path to draw on the trail view
     */
    private fun updateTrailPath() {
        if (locations.size < 2) return

        trailPath.reset()
        val first = locations.first()
        val last = locations.last()

        // Simple path drawing
        trailPath.moveTo(100f, 100f)

        for (i in 1 until locations.size) {
            val prev = locations[i - 1]
            val curr = locations[i]
            val x = 100 + (i * 10).toFloat()
            val y = 100 + ((prev.latitude - curr.latitude) * 1000).toFloat()
            trailPath.lineTo(x, y)
        }

        trailView.invalidate()
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    private fun updateUI() {
        val distanceKm = totalDistance / 1000
        distanceText.text = String.format(Locale.US, "%.1f km", distanceKm)

        if (isTracking) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
        }
        val seconds = elapsedTime / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        val hours = minutes / 60
        val mins = minutes % 60
        timeText.text = String.format("%02d:%02d:%02d", hours, mins, secs)

        elevationText.text = String.format(Locale.US, "%.0f m", elevationGain)
    }

    // ============================================================
    // LIFECYCLE - Clean up
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