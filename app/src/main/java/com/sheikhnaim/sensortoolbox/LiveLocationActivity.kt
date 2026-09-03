package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS
// ============================================================

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * LiveLocationActivity - Live GPS Navigation & Geodetic Telemetry Hub
 *
 * ============================================================
 * FEATURES:
 * ============================================================
 * 1. Google Maps-style Point-to-Point Navigation (OSRM OpenStreetMap routing)
 * 2. Real-time Turn Maneuvers, Instructions & ETA Banner
 * 3. Search Bar + Tap Map Destination Selection
 * 4. Raw GPS Geodetic Telemetry (Decimal & DMS Coordinates, Accuracy, Heading)
 * 5. Reverse Geocoded Full Street Address & Quick Coordinates Copy
 * 6. Follow-Me Camera Mode & Layer Switcher
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class LiveLocationActivity : AppCompatActivity(), MapEventsReceiver {

    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // UI VIEWS
    private lateinit var miniMapView: MapView
    private lateinit var gpsStatusBadge: TextView
    private lateinit var etDestinationSearch: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var navBannerCard: CardView
    private lateinit var tvManeuverIcon: TextView
    private lateinit var tvNextTurnDistance: TextView
    private lateinit var tvManeuverInstruction: TextView
    private lateinit var tvNavEta: TextView

    private lateinit var tvDecimalCoords: TextView
    private lateinit var tvDmsCoords: TextView
    private lateinit var tvGpsAccuracy: TextView
    private lateinit var tvGpsBearing: TextView
    private lateinit var tvGpsProvider: TextView
    private lateinit var addressText: TextView

    private lateinit var btnCopyCoords: MaterialButton
    private lateinit var btnNavigateAction: MaterialButton
    private lateinit var btnShareGps: MaterialButton
    private lateinit var fabLayer: FloatingActionButton
    private lateinit var fabFollowMe: FloatingActionButton
    private lateinit var fabMyLocation: FloatingActionButton

    // MAP OVERLAYS
    private var routePolyline: Polyline? = null
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null

    // STATE
    private var currentLocation: Location? = null
    private var destinationPoint: GeoPoint? = null
    private var destinationAddress: String? = null
    private var isNavigating = false
    private var followMeEnabled = true
    private var currentLayerIndex = 0

    // NAVIGATION STEPS & ROUTE ANIMATION
    private data class NavStep(
        val instruction: String,
        val distance: Double,
        val icon: String,
        val location: GeoPoint
    )
    private val navigationSteps = mutableListOf<NavStep>()
    private var currentStepIndex = 0
    private val navRoutePoints = ArrayList<GeoPoint>()
    private var navPulseMarker: Marker? = null
    private var navPulseIndex = 0

    /**
     * routeAnimRunnable - Animates a bright glowing pulse along the road polyline
     * toward the destination in real time.
     */
    private val routeAnimRunnable = object : Runnable {
        override fun run() {
            if (isNavigating && destinationPoint != null && navRoutePoints.isNotEmpty()) {
                navPulseIndex = (navPulseIndex + 1) % navRoutePoints.size
                val currentPt = navRoutePoints[navPulseIndex]

                if (navPulseMarker == null) {
                    navPulseMarker = Marker(miniMapView).apply {
                        icon = createNavigationPulseIcon()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "⚡ Guidance Active"
                    }
                    miniMapView.overlays.add(navPulseMarker)
                }
                navPulseMarker?.position = currentPt
                miniMapView.invalidate()

                mainHandler.postDelayed(this, 50L) // 20 FPS smooth travelling pulse
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startLocationTracking()
            } else {
                Toast.makeText(this, "Location permission required for GPS", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(this, getSharedPreferences("osmdroid_gps", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_live_location)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "📡 Live GPS & Navigation"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        initializeViews()
        setupMap()
        setupListeners()
        setupLocationServices()
    }

    override fun onResume() {
        super.onResume()
        miniMapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        stopRoutePulseAnimation()
        miniMapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun initializeViews() {
        miniMapView = findViewById(R.id.miniMapView)
        gpsStatusBadge = findViewById(R.id.gpsStatusBadge)
        etDestinationSearch = findViewById(R.id.etDestinationSearch)
        btnSearch = findViewById(R.id.btnSearch)
        navBannerCard = findViewById(R.id.navBannerCard)
        tvManeuverIcon = findViewById(R.id.tvManeuverIcon)
        tvNextTurnDistance = findViewById(R.id.tvNextTurnDistance)
        tvManeuverInstruction = findViewById(R.id.tvManeuverInstruction)
        tvNavEta = findViewById(R.id.tvNavEta)

        tvDecimalCoords = findViewById(R.id.tvDecimalCoords)
        tvDmsCoords = findViewById(R.id.tvDmsCoords)
        tvGpsAccuracy = findViewById(R.id.tvGpsAccuracy)
        tvGpsBearing = findViewById(R.id.tvGpsBearing)
        tvGpsProvider = findViewById(R.id.tvGpsProvider)
        addressText = findViewById(R.id.addressText)

        btnCopyCoords = findViewById(R.id.btnCopyCoords)
        btnNavigateAction = findViewById(R.id.btnNavigateAction)
        btnShareGps = findViewById(R.id.btnShareGps)
        fabLayer = findViewById(R.id.fabLayer)
        fabFollowMe = findViewById(R.id.fabFollowMe)
        fabMyLocation = findViewById(R.id.fabMyLocation)
    }

    private fun setupMap() {
        miniMapView.setTileSource(TileSourceFactory.MAPNIK)
        miniMapView.setMultiTouchControls(true)
        miniMapView.controller.setZoom(17.0)

        // Add map click listener to tap and select destination
        val mapEventsOverlay = MapEventsOverlay(this)
        miniMapView.overlays.add(0, mapEventsOverlay)

        routePolyline = Polyline(miniMapView).apply {
            outlinePaint.color = Color.parseColor("#2196F3")
            outlinePaint.strokeWidth = 12f
            title = "Navigation Route"
        }
        miniMapView.overlays.add(routePolyline)
    }

    private fun setupListeners() {
        btnSearch.setOnClickListener {
            performDestinationSearch()
        }

        etDestinationSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performDestinationSearch()
                true
            } else false
        }

        btnNavigateAction.setOnClickListener {
            if (isNavigating) {
                stopNavigation()
            } else {
                if (destinationPoint != null) {
                    startNavigation()
                } else {
                    Toast.makeText(this, "Tap map or search a destination first", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCopyCoords.setOnClickListener {
            currentLocation?.let { loc ->
                val coords = "${loc.latitude}, ${loc.longitude}"
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("GPS Coordinates", coords))
                Toast.makeText(this, "📋 Coordinates copied: $coords", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "No GPS fix yet", Toast.LENGTH_SHORT).show()
        }

        btnShareGps.setOnClickListener {
            shareGpsLocation()
        }

        fabLayer.setOnClickListener {
            val layers = arrayOf(
                Pair("🗺️ Street View (Mapnik)", TileSourceFactory.MAPNIK),
                Pair("⛰️ Elevation Topo (OpenTopoMap)", TileSourceFactory.OpenTopo)
            )
            currentLayerIndex = (currentLayerIndex + 1) % layers.size
            val (name, tileSource) = layers[currentLayerIndex]
            miniMapView.setTileSource(tileSource)
            Toast.makeText(this, "Layer: $name", Toast.LENGTH_SHORT).show()
            miniMapView.invalidate()
        }

        fabFollowMe.setOnClickListener {
            followMeEnabled = !followMeEnabled
            val msg = if (followMeEnabled) "🟢 Follow-Me Camera: Enabled" else "⚪ Follow-Me Camera: Free Pan"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            fabFollowMe.backgroundTintList = ContextCompat.getColorStateList(
                this,
                if (followMeEnabled) R.color.colorPrimary else android.R.color.darker_gray
            )
        }

        fabMyLocation.setOnClickListener {
            currentLocation?.let { loc ->
                val pt = GeoPoint(loc.latitude, loc.longitude)
                miniMapView.controller.animateTo(pt)
                miniMapView.controller.setZoom(18.0)
            } ?: Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1500L
        ).apply {
            setMinUpdateIntervalMillis(1000L)
            setMaxUpdateDelayMillis(3000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    onLocationUpdated(location)
                }
            }
        }

        if (checkLocationPermission()) {
            startLocationTracking()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationTracking() {
        if (!checkLocationPermission()) return
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationUpdated(location)
                    miniMapView.controller.setCenter(GeoPoint(location.latitude, location.longitude))
                }
            }
    }

    private fun onLocationUpdated(location: Location) {
        currentLocation = location
        val userPt = GeoPoint(location.latitude, location.longitude)

        // 1. Update User Marker
        if (userMarker == null) {
            userMarker = Marker(miniMapView).apply {
                position = userPt
                title = "📍 You are here"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            miniMapView.overlays.add(userMarker)
        } else {
            userMarker?.position = userPt
        }

        // 2. Camera tracking in Follow-Me Mode
        if (followMeEnabled) {
            miniMapView.controller.animateTo(userPt)
        }

        // 3. Update Raw GPS Geodesy Telemetry
        updateGpsTelemetry(location)

        // 4. Update Navigation Guidance if active
        if (isNavigating && destinationPoint != null) {
            updateLiveNavigation(location)
        }

        miniMapView.invalidate()
    }

    private fun updateGpsTelemetry(location: Location) {
        val lat = location.latitude
        val lon = location.longitude
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"

        // Decimal Format
        tvDecimalCoords.text = String.format(Locale.US, "Decimal: %.6f° %s, %.6f° %s", abs(lat), latDir, abs(lon), lonDir)

        // DMS Format (Degrees, Minutes, Seconds)
        tvDmsCoords.text = String.format(Locale.US, "DMS: %s, %s", convertToDms(lat, latDir), convertToDms(lon, lonDir))

        // Accuracy
        val acc = if (location.hasAccuracy()) location.accuracy else 0f
        val accColor = if (acc <= 5f) "#00E676" else if (acc <= 15f) "#FFEB3B" else "#FF9800"
        tvGpsAccuracy.text = String.format(Locale.US, "±%.1f m (%s)", acc, if (acc <= 5f) "High" else "Medium")
        tvGpsAccuracy.setTextColor(Color.parseColor(accColor))
        gpsStatusBadge.text = String.format(Locale.US, "🟢 GPS Active • ±%.1fm", acc)

        // Course / Bearing
        val bearing = if (location.hasBearing()) location.bearing else 0f
        tvGpsBearing.text = String.format(Locale.US, "%.0f° %s", bearing, getCompassDirection(bearing))

        // Provider
        val providerName = location.provider?.uppercase(Locale.US) ?: "GPS"
        tvGpsProvider.text = "3D Fix ($providerName)"

        // Geocoding Address
        reverseGeocode(location)
    }

    private fun convertToDms(coordinate: Double, direction: String): String {
        val absCoord = abs(coordinate)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60
        return String.format(Locale.US, "%d° %02d' %04.1f\" %s", degrees, minutes, seconds, direction)
    }

    private fun getCompassDirection(bearing: Float): String {
        return when {
            bearing >= 337.5 || bearing < 22.5 -> "N"
            bearing >= 22.5 && bearing < 67.5 -> "NE"
            bearing >= 67.5 && bearing < 112.5 -> "E"
            bearing >= 112.5 && bearing < 157.5 -> "SE"
            bearing >= 157.5 && bearing < 202.5 -> "S"
            bearing >= 202.5 && bearing < 247.5 -> "SW"
            bearing >= 247.5 && bearing < 292.5 -> "W"
            else -> "NW"
        }
    }

    private fun reverseGeocode(location: Location) {
        backgroundExecutor.execute {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            val addr = formatAddress(addresses[0])
                            mainHandler.post { addressText.text = addr }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(this, Locale.getDefault()).getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = formatAddress(addresses[0])
                        mainHandler.post { addressText.text = addr }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun formatAddress(address: Address): String {
        val parts = mutableListOf<String>()
        address.featureName?.let { parts.add(it) }
        address.thoroughfare?.let { if (!parts.contains(it)) parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else "Unknown Location"
    }

    // ============================================================
    // MAP EVENTS (TAP TO SET DESTINATION)
    // ============================================================

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        p?.let { point ->
            setDestination(point, "Dropped Destination Pin")
        }
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        return false
    }

    private fun performDestinationSearch() {
        val query = etDestinationSearch.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "Enter a destination name or address", Toast.LENGTH_SHORT).show()
            return
        }

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etDestinationSearch.windowToken, 0)
        Toast.makeText(this, "🔍 Searching '$query' on OpenStreetMap...", Toast.LENGTH_SHORT).show()

        backgroundExecutor.execute {
            var foundPoint: GeoPoint? = null
            var foundLabel: String = ""

            // STEP 1: Query OpenStreetMap Nominatim Search API
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlStr = "https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&addressdetails=1&limit=5"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.setRequestProperty("User-Agent", "SensorToolBox-AndroidApp (com.sheikhnaim.sensortoolbox)")
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

                if (conn.responseCode == 200) {
                    val streamReader = BufferedReader(InputStreamReader(conn.inputStream))
                    val responseText = streamReader.readText()
                    streamReader.close()

                    val jsonArray = JSONArray(responseText)
                    if (jsonArray.length() > 0) {
                        val firstObj = jsonArray.getJSONObject(0)
                        val lat = firstObj.getDouble("lat")
                        val lon = firstObj.getDouble("lon")
                        val name = firstObj.optString("name", query)
                        val displayName = firstObj.optString("display_name", "$lat, $lon")

                        foundPoint = GeoPoint(lat, lon)
                        foundLabel = if (name.isNotEmpty() && name != displayName) "$name ($displayName)" else displayName
                    }
                }
            } catch (_: Exception) {
                // Fallback to Android Geocoder if offline
            }

            // STEP 2: Fallback to Android Geocoder if Nominatim had no network
            if (foundPoint == null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val results = geocoder.getFromLocationName(query, 5)
                    if (!results.isNullOrEmpty()) {
                        val dest = results[0]
                        foundPoint = GeoPoint(dest.latitude, dest.longitude)
                        foundLabel = formatAddress(dest)
                    }
                } catch (_: Exception) {}
            }

            mainHandler.post {
                if (foundPoint != null) {
                    setDestination(foundPoint, foundLabel)
                    miniMapView.controller.animateTo(foundPoint)
                    miniMapView.controller.setZoom(17.0)
                    Toast.makeText(this, "🎯 Found: $foundLabel", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No location found for '$query'", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setDestination(point: GeoPoint, label: String) {
        destinationPoint = point
        destinationAddress = label
        etDestinationSearch.setText(label)

        if (destinationMarker == null) {
            destinationMarker = Marker(miniMapView).apply {
                position = point
                title = "🏁 Destination: $label"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            miniMapView.overlays.add(destinationMarker)
        } else {
            destinationMarker?.position = point
            destinationMarker?.title = "🏁 Destination: $label"
        }

        btnNavigateAction.text = "🧭 Start Navigation"
        btnNavigateAction.setBackgroundColor(Color.parseColor("#00897B"))
        fetchRoute(point)
    }

    private fun fetchRoute(dest: GeoPoint) {
        val loc = currentLocation ?: return
        val startLat = loc.latitude
        val startLon = loc.longitude
        val endLat = dest.latitude
        val endLon = dest.longitude

        backgroundExecutor.execute {
            val routePoints = ArrayList<GeoPoint>()
            val steps = mutableListOf<NavStep>()

            try {
                // Fetch route from OpenStreetMap OSRM Routing Machine
                val urlStr = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson&steps=true"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.requestMethod = "GET"

                if (conn.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(conn.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    val routes = json.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        val geometry = route.getJSONObject("geometry")
                        val coordinates = geometry.getJSONArray("coordinates")

                        for (i in 0 until coordinates.length()) {
                            val coord = coordinates.getJSONArray(i)
                            val lon = coord.getDouble(0)
                            val lat = coord.getDouble(1)
                            routePoints.add(GeoPoint(lat, lon))
                        }

                        // Parse turn maneuvers
                        val legs = route.getJSONArray("legs")
                        if (legs.length() > 0) {
                            val leg = legs.getJSONObject(0)
                            val rawSteps = leg.getJSONArray("steps")
                            for (s in 0 until rawSteps.length()) {
                                val stepObj = rawSteps.getJSONObject(s)
                                val maneuver = stepObj.getJSONObject("maneuver")
                                val stepType = maneuver.optString("type", "turn")
                                val stepModifier = maneuver.optString("modifier", "")
                                val stepName = stepObj.optString("name", "road")
                                val dist = stepObj.optDouble("distance", 0.0)

                                val icon = when {
                                    stepType == "arrive" -> "🏁"
                                    stepModifier.contains("right") -> "↗️"
                                    stepModifier.contains("left") -> "↖️"
                                    stepModifier.contains("sharp right") -> "➡️"
                                    stepModifier.contains("sharp left") -> "⬅️"
                                    else -> "⬆️"
                                }

                                val instruction = if (stepType == "arrive") {
                                    "Arrive at destination"
                                } else {
                                    val action = if (stepModifier.isNotEmpty()) "$stepType $stepModifier" else "Continue"
                                    "$action onto $stepName"
                                }

                                val stepCoord = maneuver.getJSONArray("location")
                                steps.add(
                                    NavStep(
                                        instruction = instruction,
                                        distance = dist,
                                        icon = icon,
                                        location = GeoPoint(stepCoord.getDouble(1), stepCoord.getDouble(0))
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Geodesic straight road fallback if offline
                routePoints.clear()
                routePoints.add(GeoPoint(startLat, startLon))
                routePoints.add(dest)
                steps.clear()
                steps.add(NavStep("Head directly towards destination", loc.distanceTo(Location("").apply { latitude = dest.latitude; longitude = dest.longitude }).toDouble(), "⬆️", dest))
            }

            mainHandler.post {
                navRoutePoints.clear()
                navRoutePoints.addAll(routePoints)
                routePolyline?.setPoints(routePoints)
                navigationSteps.clear()
                navigationSteps.addAll(steps)
                currentStepIndex = 0
                if (isNavigating) {
                    startRoutePulseAnimation()
                }
                miniMapView.invalidate()

                val totalDistMeters = loc.distanceTo(Location("").apply { latitude = dest.latitude; longitude = dest.longitude })
                val estMinutes = (totalDistMeters / 1000f * 2.5f).toInt().coerceAtLeast(1)
                Toast.makeText(this, String.format(Locale.US, "Route ready: %.2f km (~%d min)", totalDistMeters / 1000f, estMinutes), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNavigationPulseIcon(): Drawable {
        val size = 42
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer glowing aura
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6600E5FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, auraPaint)

        // Electric cyan core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, corePaint)

        // Center white point
        val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 6f, centerPaint)

        return BitmapDrawable(resources, bitmap)
    }

    private fun startRoutePulseAnimation() {
        stopRoutePulseAnimation()
        if (navRoutePoints.isNotEmpty()) {
            navPulseIndex = 0
            mainHandler.post(routeAnimRunnable)
        }
    }

    private fun stopRoutePulseAnimation() {
        mainHandler.removeCallbacks(routeAnimRunnable)
        navPulseMarker?.let {
            miniMapView.overlays.remove(it)
            navPulseMarker = null
        }
        miniMapView.invalidate()
    }

    private fun startNavigation() {
        isNavigating = true
        followMeEnabled = true
        navBannerCard.visibility = android.view.View.VISIBLE
        btnNavigateAction.text = "⏹️ Stop Navigation"
        btnNavigateAction.setBackgroundColor(Color.parseColor("#F44336"))

        startRoutePulseAnimation()
        currentLocation?.let { updateLiveNavigation(it) }
        Toast.makeText(this, "🧭 Turn-by-Turn Navigation Started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopNavigation() {
        isNavigating = false
        stopRoutePulseAnimation()
        navBannerCard.visibility = android.view.View.GONE
        btnNavigateAction.text = "🧭 Start Navigation"
        btnNavigateAction.setBackgroundColor(Color.parseColor("#00897B"))
        Toast.makeText(this, "⏹️ Navigation stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateLiveNavigation(location: Location) {
        val dest = destinationPoint ?: return
        val destLoc = Location("").apply {
            latitude = dest.latitude
            longitude = dest.longitude
        }

        val remainingDistMeters = location.distanceTo(destLoc)
        val remainingKm = remainingDistMeters / 1000f
        val remainingMin = (remainingKm * 2.2f).toInt().coerceAtLeast(1)

        tvNavEta.text = String.format(Locale.US, "Remaining: %.2f km • ~%d min", remainingKm, remainingMin)

        if (remainingDistMeters <= 20f) {
            tvManeuverIcon.text = "🏁"
            tvNextTurnDistance.text = "Arrived!"
            tvManeuverInstruction.text = "You have arrived at your destination"
            return
        }

        if (navigationSteps.isNotEmpty()) {
            val step = navigationSteps[currentStepIndex.coerceIn(0, navigationSteps.size - 1)]
            val stepLoc = Location("").apply {
                latitude = step.location.latitude
                longitude = step.location.longitude
            }
            val distToStep = location.distanceTo(stepLoc)

            if (distToStep < 25f && currentStepIndex < navigationSteps.size - 1) {
                currentStepIndex++
            }

            val currentStep = navigationSteps[currentStepIndex]
            tvManeuverIcon.text = currentStep.icon
            tvNextTurnDistance.text = String.format(Locale.US, "In %.0f m", distToStep)
            tvManeuverInstruction.text = currentStep.instruction
        } else {
            tvManeuverIcon.text = "⬆️"
            tvNextTurnDistance.text = String.format(Locale.US, "In %.0f m", remainingDistMeters)
            tvManeuverInstruction.text = "Continue straight to destination"
        }
    }

    private fun shareGpsLocation() {
        val loc = currentLocation
        if (loc == null) {
            Toast.makeText(this, "Acquiring GPS fix...", Toast.LENGTH_SHORT).show()
            return
        }

        val mapUrl = "https://www.openstreetmap.org/?mlat=${loc.latitude}&mlon=${loc.longitude}#map=17/${loc.latitude}/${loc.longitude}"
        val text = "📍 LIVE GPS LOCATION\n" +
                "Coordinates: ${loc.latitude}, ${loc.longitude}\n" +
                "Accuracy: ±${String.format(Locale.US, "%.1f", loc.accuracy)} m\n" +
                "Address: ${addressText.text}\n" +
                "🗺️ OpenStreetMap Link: $mapUrl"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "My Live GPS Location")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(sendIntent, "Share GPS Location via"))
    }
}