package com.sheikhnaim.sensortoolbox.location

// ============================================================
// IMPORTS
// ============================================================
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sheikhnaim.sensortoolbox.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.Locale

/**
 * TrailTrackerActivity - Records and displays hiking trails with embedded OpenStreetMap & OpenTopoMap
 *
 * ============================================================
 * FEATURES:
 * ============================================================
 * 1. Embedded OpenStreetMap / OpenTopoMap MapView
 * 2. Live hiking trail polyline with start/end markers
 * 3. Distance, elapsed duration, and elevation gain tracking
 * 4. Layer switcher (Street / Elevation Topo contours)
 * 5. Instant GPS acquisition and shareable workout summary
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class TrailTrackerActivity : AppCompatActivity() {

    companion object {
        private const val MIN_DISTANCE_METERS = 1.5f
        private const val UPDATE_INTERVAL_MS = 2000L
        private const val LOCATION_PERMISSION_REQUEST = 101
    }

    // GPS & OSM MAP
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: MapView
    private var pathPolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var currentMarker: Marker? = null
    private val pathPoints = ArrayList<GeoPoint>()
    private var currentLayerIndex = 0

    // UI VIEWS
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var elevationText: TextView
    private lateinit var pointsCountText: TextView
    private lateinit var gpsStatusBadge: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var stopButton: MaterialButton
    private lateinit var resetButton: MaterialButton
    private lateinit var shareTrailButton: MaterialButton
    private lateinit var fabLayer: FloatingActionButton
    private lateinit var fabMyLocation: FloatingActionButton

    // TRAIL DATA & TIMER
    private val locations = mutableListOf<Location>()
    private var currentLocation: Location? = null
    private var totalDistance = 0f
    private var elapsedTime = 0L
    private var startTime = 0L
    private var isTracking = false
    private var startElevation = 0f
    private var elevationGain = 0f

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTracking && startTime > 0) {
                updateUI()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            for (location in result.locations) {
                currentLocation = location
                if (isTracking) {
                    addLocation(location)
                } else {
                    updatePreviewMarker(location)
                }
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                requestInitialLocationFix()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied_toast),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_trail", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_trail_tracker)

        setupToolbar()
        initializeViews()
        setupMap()
        initializeLocationClient()
        setupClickListeners()
        updateUI()
        checkPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (isTracking) {
            startLocationUpdates()
            handler.post(timerRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        if (isTracking) {
            handler.removeCallbacks(timerRunnable)
            stopLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        if (isTracking) {
            stopLocationUpdates()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.trail_tracker_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        distanceText = findViewById(R.id.distanceText)
        timeText = findViewById(R.id.timeText)
        elevationText = findViewById(R.id.elevationText)
        pointsCountText = findViewById(R.id.pointsCountText)
        gpsStatusBadge = findViewById(R.id.gpsStatusBadge)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resetButton = findViewById(R.id.resetButton)
        shareTrailButton = findViewById(R.id.shareTrailButton)
        fabLayer = findViewById(R.id.fabLayer)
        fabMyLocation = findViewById(R.id.fabMyLocation)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(17.5)

        pathPolyline = Polyline(mapView).apply {
            outlinePaint.color = Color.parseColor("#FF9800")
            outlinePaint.strokeWidth = 10f
            title = "Hiking Trail"
        }
        mapView.overlays.add(pathPolyline)
    }

    private fun cycleMapLayer() {
        val layers = arrayOf(
            Pair("🗺️ Street View (Mapnik)", TileSourceFactory.MAPNIK),
            Pair("⛰️ Elevation Topo (OpenTopoMap)", TileSourceFactory.OpenTopo)
        )
        currentLayerIndex = (currentLayerIndex + 1) % layers.size
        val (name, tileSource) = layers[currentLayerIndex]
        mapView.setTileSource(tileSource)
        Toast.makeText(this, "Layer: $name", Toast.LENGTH_SHORT).show()
        mapView.invalidate()
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

        shareTrailButton.setOnClickListener {
            shareTrailReport()
        }

        fabLayer.setOnClickListener {
            cycleMapLayer()
        }

        fabMyLocation.setOnClickListener {
            currentLocation?.let { loc ->
                val pt = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.animateTo(pt)
                mapView.controller.setZoom(18.0)
            } ?: run {
                Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
                requestInitialLocationFix()
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun checkPermissionAndStart() {
        if (checkPermission()) {
            requestInitialLocationFix()
        } else {
            requestPermission()
        }
    }

    private fun requestInitialLocationFix() {
        if (!checkPermission()) return
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    currentLocation = loc
                    updatePreviewMarker(loc)
                    gpsStatusBadge.text = "🟢 GPS Locked"
                }
            }
    }

    private fun updatePreviewMarker(location: Location) {
        val pt = GeoPoint(location.latitude, location.longitude)
        if (currentMarker == null) {
            currentMarker = Marker(mapView).apply {
                position = pt
                title = "Start Location"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(currentMarker)
            mapView.controller.setCenter(pt)
        } else {
            currentMarker?.position = pt
        }
        mapView.invalidate()
    }

    private fun startTracking() {
        if (!checkPermission()) {
            requestPermission()
            return
        }

        locations.clear()
        pathPoints.clear()
        pathPolyline?.setPoints(pathPoints)
        totalDistance = 0f
        elapsedTime = 0L
        startTime = SystemClock.elapsedRealtime()
        elevationGain = 0f
        startElevation = 0f
        isTracking = true

        startButton.isEnabled = false
        stopButton.isEnabled = true
        resetButton.isEnabled = false
        gpsStatusBadge.text = "🟢 Hike Active"

        startLocationUpdates()
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)

        // Capture initial location fix immediately
        if (checkPermission()) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null && isTracking) {
                        addLocation(loc)
                    }
                }
        }

        Toast.makeText(this, "🥾 Hiking trail recording started!", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun stopTracking() {
        if (!isTracking) return

        isTracking = false
        handler.removeCallbacks(timerRunnable)
        stopLocationUpdates()
        elapsedTime = SystemClock.elapsedRealtime() - startTime

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = locations.isNotEmpty()
        gpsStatusBadge.text = "⏹️ Hike Paused"

        Toast.makeText(this, "⏹️ Trail recording stopped", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun resetTrail() {
        isTracking = false
        handler.removeCallbacks(timerRunnable)
        locations.clear()
        pathPoints.clear()
        pathPolyline?.setPoints(pathPoints)
        totalDistance = 0f
        elapsedTime = 0L
        elevationGain = 0f
        startElevation = 0f
        mapView.invalidate()

        startButton.isEnabled = true
        stopButton.isEnabled = false
        resetButton.isEnabled = false
        gpsStatusBadge.text = "🟢 GPS Ready"

        Toast.makeText(this, "🔄 Trail reset", Toast.LENGTH_SHORT).show()
        updateUI()
    }

    private fun startLocationUpdates() {
        if (!checkPermission()) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            UPDATE_INTERVAL_MS
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setMaxUpdateDelayMillis(UPDATE_INTERVAL_MS * 2)
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun addLocation(location: Location) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)

        if (locations.isEmpty()) {
            startElevation = location.altitude.toFloat()
            locations.add(location)
            pathPoints.add(geoPoint)
            pathPolyline?.setPoints(pathPoints)

            startMarker = Marker(mapView).apply {
                position = geoPoint
                title = "🟢 Hike Start"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(startMarker)
            mapView.controller.setCenter(geoPoint)
            mapView.invalidate()
            updateUI()
            return
        }

        val lastLocation = locations.last()
        val distance = lastLocation.distanceTo(location)

        if (distance >= MIN_DISTANCE_METERS) {
            totalDistance += distance
            locations.add(location)
            pathPoints.add(geoPoint)
            pathPolyline?.setPoints(pathPoints)

            val elevationDiff = location.altitude - lastLocation.altitude
            if (elevationDiff > 0) {
                elevationGain += elevationDiff.toFloat()
            }

            if (currentMarker == null) {
                currentMarker = Marker(mapView).apply {
                    position = geoPoint
                    title = "📍 Current Position"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(currentMarker)
            } else {
                currentMarker?.position = geoPoint
            }

            mapView.controller.animateTo(geoPoint)
            mapView.invalidate()
            updateUI()
        }
    }

    private fun updateUI() {
        // Update distance
        val distanceKm = totalDistance / 1000f
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
        pointsCountText.text = String.format(Locale.US, "📍 %d points", locations.size)
    }

    private fun shareTrailReport() {
        val lat = currentLocation?.latitude ?: 0.0
        val lon = currentLocation?.longitude ?: 0.0
        val mapUrl = if (lat != 0.0 && lon != 0.0) {
            "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=16/$lat/$lon"
        } else ""

        val report = "🥾 HIKING TRAIL REPORT\n" +
                "📏 Total Trail Distance: ${distanceText.text}\n" +
                "⏱️ Active Duration: ${timeText.text}\n" +
                "⛰️ Total Elevation Gain: ${elevationText.text}\n" +
                "📍 GPS Breadcrumbs: ${locations.size} points\n" +
                (if (mapUrl.isNotEmpty()) "🗺️ OpenStreetMap Trail Link: $mapUrl" else "")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Hiking Trail Report")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(intent, "Share Hiking Trail via"))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}