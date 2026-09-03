package com.sheikhnaim.sensortoolbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

/**
 * FitnessTrackerActivity - Real-time Fitness & Workout Tracking Hub
 *
 * HOW IT WORKS (Workout Telemetry & Algorithms):
 * 1. Tracks running, walking, or cycling sessions using continuous high-accuracy GPS.
 * 2. Real-time Metrics Calculated:
 *    - Speed: Instantaneous velocity from GPS (m/s converted to km/h).
 *    - Distance: Cumulative haversine geodesic distance between GPS fixes (in km).
 *    - Duration: Active workout clock ticking every 1 second (HH:MM:SS).
 *    - Current Pace: Minutes required to cover 1 kilometer (min/km), calculated as
 *      (60 / speedKmh).
 *    - Estimated Calories Burned: Energy expenditure estimated via Metabolic Equivalent
 *      of Task (MET) formula: Calories = MET * Weight(kg) * Time(hours).
 *    - Altitude: Live elevation above sea level in meters.
 * 3. Draws a live green breadcrumb route polyline on the embedded OpenStreetMap.
 * 4. Supports cycling map tile layers (Street / Topo Elevation) and sharing workout summaries.
 */
class FitnessTrackerActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // UI Views
    private lateinit var mapView: MapView
    private lateinit var speedText: TextView
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var paceText: TextView
    private lateinit var caloriesText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var gpsStatusBadge: TextView

    private lateinit var startButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var shareButton: MaterialButton
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabLayer: FloatingActionButton
    private var currentLayerIndex = 0

    // Map components
    private var locationMarker: Marker? = null
    private val pathPoints = mutableListOf<GeoPoint>()
    private var pathPolyline: Polyline? = null

    // Tracking data
    private var totalDistance = 0.0f // in km
    private var startTime = 0L
    private var isTracking = false
    private var lastLocation: Location? = null
    private var currentLocation: Location? = null
    private var locationCount = 0

    // Timer Handler
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTracking && startTime > 0) {
                updateTimerDisplay()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_fitness_tracker)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        initializeViews()
        setupMap()
        setupLocation()
        setupButtons()
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        speedText = findViewById(R.id.speedText)
        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        paceText = findViewById(R.id.paceText)
        caloriesText = findViewById(R.id.caloriesText)
        altitudeText = findViewById(R.id.altitudeText)
        gpsStatusBadge = findViewById(R.id.gpsStatusBadge)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        shareButton = findViewById(R.id.shareButton)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        fabLayer = findViewById(R.id.fabLayer)
    }

    private fun cycleMapLayer() {
        currentLayerIndex = (currentLayerIndex + 1) % 2
        if (currentLayerIndex == 0) {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            Toast.makeText(this, "🗺️ Map: Standard Street", Toast.LENGTH_SHORT).show()
        } else {
            mapView.setTileSource(TileSourceFactory.OpenTopo)
            Toast.makeText(this, "🏔️ Map: Topographic Elevation Contours", Toast.LENGTH_SHORT).show()
        }
        mapView.invalidate()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.5)

        pathPolyline = Polyline(mapView).apply {
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 10f
            title = "Fitness Trail"
        }
        mapView.overlays.add(pathPolyline)
    }

    private fun setupLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                    if (isTracking) {
                        updateLocation(location)
                    } else {
                        // Center preview if not started
                        val pt = GeoPoint(location.latitude, location.longitude)
                        if (locationMarker == null) {
                            locationMarker = Marker(mapView).apply {
                                position = pt
                                title = "Start Point"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(locationMarker)
                            mapView.controller.setCenter(pt)
                            mapView.invalidate()
                        }
                    }
                }
            }
        }

        checkPermissionAndStart()
    }

    private fun setupButtons() {
        startButton.setOnClickListener { startTracking() }
        stopButton.setOnClickListener { stopTracking() }
        resetButton.setOnClickListener { resetTracking() }

        fabMyLocation.setOnClickListener {
            currentLocation?.let { loc ->
                val pt = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.animateTo(pt)
                mapView.controller.setZoom(18.0)
            } ?: run {
                Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
            }
        }

        fabLayer.setOnClickListener {
            cycleMapLayer()
        }

        shareButton.setOnClickListener {
            shareWorkout()
        }
    }

    private fun startTracking() {
        totalDistance = 0.0f
        startTime = SystemClock.elapsedRealtime()
        lastLocation = null
        pathPoints.clear()
        pathPolyline?.setPoints(pathPoints)
        mapView.invalidate()
        isTracking = true

        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false
        gpsStatusBadge.text = "🟢 Workout Active"

        timerHandler.removeCallbacks(timerRunnable)
        timerHandler.post(timerRunnable)

        Toast.makeText(this, "🏃 Workout tracking started!", Toast.LENGTH_SHORT).show()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            )
                .setMinUpdateIntervalMillis(1000L)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }

    private fun stopTracking() {
        isTracking = false
        timerHandler.removeCallbacks(timerRunnable)
        fusedLocationClient.removeLocationUpdates(locationCallback)

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = true
        gpsStatusBadge.text = "⏹️ Workout Paused"

        Toast.makeText(this, "⏹️ Workout completed!", Toast.LENGTH_SHORT).show()
    }

    private fun resetTracking() {
        isTracking = false
        timerHandler.removeCallbacks(timerRunnable)
        totalDistance = 0.0f
        startTime = 0L
        lastLocation = null
        locationCount = 0
        pathPoints.clear()
        pathPolyline?.setPoints(pathPoints)
        mapView.invalidate()

        speedText.text = "0.0 km/h"
        distanceText.text = "0.00 km"
        timeText.text = "00:00:00"
        paceText.text = "--:-- /km"
        caloriesText.text = "0 kcal"
        altitudeText.text = "-- m"
        gpsStatusBadge.text = "🟢 GPS Ready"

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = false

        Toast.makeText(this, "🔄 Session reset", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            requestInitialLocationFix()
        }
    }

    private fun requestInitialLocationFix() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        currentLocation = loc
                        val pt = GeoPoint(loc.latitude, loc.longitude)
                        if (locationMarker == null) {
                            locationMarker = Marker(mapView).apply {
                                position = pt
                                title = "Start Point"
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(locationMarker)
                        } else {
                            locationMarker?.position = pt
                        }
                        mapView.controller.animateTo(pt)
                        mapView.invalidate()
                        gpsStatusBadge.text = "🟢 GPS Locked"
                    }
                }
        }
    }

    private fun updateLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        locationCount++

        val currentPosition = GeoPoint(latitude, longitude)

        if (locationMarker == null) {
            locationMarker = Marker(mapView).apply {
                position = currentPosition
                title = "Live Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(locationMarker)
        } else {
            locationMarker?.position = currentPosition
        }

        pathPoints.add(currentPosition)
        pathPolyline?.setPoints(pathPoints)

        if (pathPoints.size == 1) {
            mapView.controller.setCenter(currentPosition)
        } else {
            mapView.controller.animateTo(currentPosition)
        }
        mapView.invalidate()

        calculateStats(location)
    }

    private fun calculateStats(location: Location) {
        // Live Speed
        val speedKmh = if (location.hasSpeed()) location.speed * 3.6f else 0.0f
        speedText.text = String.format(Locale.US, "%.1f km/h", speedKmh)

        // Distance
        if (lastLocation != null) {
            val distanceMeters = lastLocation!!.distanceTo(location)
            totalDistance += (distanceMeters / 1000.0f)
        }
        lastLocation = location
        distanceText.text = String.format(Locale.US, "%.2f km", totalDistance)

        // Pace: min per km
        if (speedKmh > 0.5f) {
            val minutesPerKm = 60.0f / speedKmh
            val paceMin = minutesPerKm.toInt()
            val paceSec = ((minutesPerKm - paceMin) * 60).toInt()
            paceText.text = String.format(Locale.US, "%d'%02d\" /km", paceMin, paceSec)
        } else {
            paceText.text = "--:-- /km"
        }

        // Calories estimate: ~65 kcal per km
        val calories = (totalDistance * 65.0f).toInt()
        caloriesText.text = String.format(Locale.US, "%d kcal", calories)

        // Altitude
        if (location.hasAltitude()) {
            altitudeText.text = String.format(Locale.US, "%.0f m", location.altitude)
        }
    }

    private fun updateTimerDisplay() {
        val elapsedMillis = SystemClock.elapsedRealtime() - startTime
        val totalSeconds = elapsedMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        timeText.text = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun shareWorkout() {
        val lat = currentLocation?.latitude ?: 0.0
        val lon = currentLocation?.longitude ?: 0.0
        val url = if (lat != 0.0 && lon != 0.0) {
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"
        } else ""

        val summary = "🏃 FITNESS WORKOUT REPORT\n" +
                "📏 Total Distance: ${distanceText.text}\n" +
                "⏱️ Duration: ${timeText.text}\n" +
                "🏎️ Current Speed: ${speedText.text}\n" +
                "🔥 Calories Burned: ${caloriesText.text}\n" +
                (if (url.isNotEmpty()) "🗺️ OpenStreetMap Trail: $url" else "")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Fitness Workout")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        startActivity(Intent.createChooser(intent, "Share Workout Report via"))
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "✅ GPS permission granted!", Toast.LENGTH_SHORT).show()
            requestInitialLocationFix()
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}