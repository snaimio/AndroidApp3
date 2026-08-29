package com.sheikhnaim.sensortoolbox.location

// ============================================================
// IMPORTS
// ============================================================
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
 * TrailTrackerActivity - Records and displays hiking trails
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Uses GPS to record location points
 * 2. Draws the trail path on a custom TrailView
 * 3. Tracks distance, time, and elevation gain
 * 4. Start/Stop/Reset controls
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class TrailTrackerActivity : AppCompatActivity() {

    // ============================================================
    // CONSTANTS
    // ============================================================
    companion object {
        /** Minimum distance between points to record (meters) */
        private const val MIN_DISTANCE_METERS = 1.0f

        /** Update interval for location updates (milliseconds) */
        private const val UPDATE_INTERVAL_MS = 2000L
    }

    // ============================================================
    // GPS LOCATION CLIENT
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var trailView: TrailView
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var elevationText: TextView
    private lateinit var pointsCountText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resetButton: Button

    // ============================================================
    // TRAIL DATA
    // ============================================================
    private val locations = mutableListOf<Location>()
    private var totalDistance = 0f
    private var elapsedTime = 0L
    private var startTime = 0L
    private var isTracking = false
    private var startElevation = 0f
    private var elevationGain = 0f

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
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied_toast),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trail_tracker)

        setupToolbar()
        initializeViews()
        initializeLocationClient()
        setupClickListeners()
        updateUI()
    }

    override fun onResume() {
        super.onResume()
        if (isTracking) {
            startLocationUpdates()
        }
    }

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
    // INITIALIZATION METHODS
    // ============================================================

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.trail_tracker_title)
    }

    private fun initializeViews() {
        trailView = findViewById(R.id.trailView)
        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        elevationText = findViewById(R.id.elevationText)
        pointsCountText = findViewById(R.id.pointsCountText)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
    }

    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupClickListeners() {
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
        trailView.clearTrail()
        isTracking = true

        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false

        startLocationUpdates()

        Toast.makeText(
            this,
            getString(R.string.trail_started),
            Toast.LENGTH_SHORT
        ).show()
        updateUI()
    }

    private fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        stopLocationUpdates()
        elapsedTime = SystemClock.elapsedRealtime() - startTime

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = locations.isNotEmpty()

        Toast.makeText(
            this,
            getString(R.string.trail_stopped),
            Toast.LENGTH_SHORT
        ).show()
        updateUI()
    }

    private fun resetTrail() {
        locations.clear()
        totalDistance = 0f
        elapsedTime = 0L
        elevationGain = 0f
        startElevation = 0f
        trailView.clearTrail()
        isTracking = false

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = false

        Toast.makeText(
            this,
            getString(R.string.trail_reset),
            Toast.LENGTH_SHORT
        ).show()
        updateUI()
    }

    // ============================================================
    // LOCATION UPDATES
    // ============================================================

    private fun startLocationUpdates() {
        if (!checkPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
            setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS * 2)
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

    private fun addLocation(location: Location) {
        if (locations.isEmpty()) {
            startElevation = location.altitude.toFloat()
            locations.add(location)
            updateUI()
            return
        }

        val lastLocation = locations.last()
        val distance = lastLocation.distanceTo(location)

        if (distance > MIN_DISTANCE_METERS) {
            totalDistance += distance
            locations.add(location)

            val elevationDiff = location.altitude - lastLocation.altitude
            if (elevationDiff > 0) {
                elevationGain += elevationDiff.toFloat()
            }

            updateTrailView()
            updateUI()
        }
    }

    private fun updateTrailView() {
        if (locations.size < 2) return

        val points = locations.map { location ->
            Pair(location.latitude, location.longitude)
        }
        trailView.updateTrail(points)
    }

    // ============================================================
    // UI UPDATE
    // ============================================================

    private fun updateUI() {
        // Update distance
        val distanceKm = totalDistance / 1000
        distanceText.text = String.format(Locale.US, "%.2f km", distanceKm)

        // Update elapsed time
        if (isTracking) {
            elapsedTime = SystemClock.elapsedRealtime() - startTime
        }
        val seconds = elapsedTime / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        timeText.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)

        // Update elevation gain
        elevationText.text = String.format(Locale.US, "%.0f m", elevationGain)

        // Update points count
        pointsCountText.text = String.format(
            Locale.US,
            getString(R.string.trail_points_format),
            locations.size
        )
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}