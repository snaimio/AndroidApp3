package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
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
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
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
import org.json.JSONArray
import org.json.JSONObject

/**
 * MapActivity - Interactive OpenStreetMap Hub & Navigation Tool
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class MapActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private var isCompassModeActive = false

    // UI VIEWS
    private lateinit var mapView: MapView
    private lateinit var statusText: TextView
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var fabLayer: FloatingActionButton

    // Search Bar & Live Navigation HUD UI
    private lateinit var searchEditText: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnClearSearch: ImageView
    private lateinit var navGuidanceCard: CardView
    private lateinit var navDistanceText: TextView
    private lateinit var navBearingText: TextView
    private lateinit var navEtaText: TextView
    private lateinit var navTargetAddressText: TextView
    private lateinit var btnStopNav: MaterialButton

    // Live Point A to Point B Navigation State
    private var destinationPoint: GeoPoint? = null
    private var destinationName: String = ""
    private var navGuidancePolyline: Polyline? = null

    // Bottom Info Sheet UI
    private lateinit var cardTitleText: TextView
    private lateinit var cardAddressText: TextView
    private lateinit var accuracyBadge: TextView
    private lateinit var cardSpeedText: TextView
    private lateinit var cardAltitudeText: TextView
    private lateinit var cardCoordinatesText: TextView
    private lateinit var btnShareLocation: MaterialButton
    private lateinit var btnCopyCoords: MaterialButton
    private lateinit var btnNavigate: MaterialButton

    private lateinit var locationCallback: LocationCallback
    private var locationMarker: Marker? = null
    private var tappedMarker: Marker? = null
    private var routePolyline: Polyline? = null
    private var startMarker: Marker? = null
    private var endMarker: Marker? = null

    private var firstLocation = true
    private var userLocation: Location? = null
    private var currentActiveLat: Double = 0.0
    private var currentActiveLon: Double = 0.0
    private var currentActiveAddress: String = "Resolving street address..."
    private var currentLayerIndex = 0
    private val bgExecutor = Executors.newSingleThreadExecutor()
    private val routeAnimHandler = Handler(Looper.getMainLooper())
    private val navRoutePoints = ArrayList<GeoPoint>()
    private var navPulseMarker: Marker? = null
    private var navPulseIndex = 0

    /**
     * routeAnimRunnable - Smoothly advances an animated glowing beacon dot along the route
     * polyline towards the destination, providing real-time visual proof of active navigation.
     */
    private val routeAnimRunnable = object : Runnable {
        override fun run() {
            if (destinationPoint != null && navRoutePoints.isNotEmpty()) {
                navPulseIndex = (navPulseIndex + 1) % navRoutePoints.size
                val currentPt = navRoutePoints[navPulseIndex]

                if (navPulseMarker == null) {
                    navPulseMarker = Marker(mapView).apply {
                        icon = createNavigationPulseIcon()
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "⚡ Live Navigation Route"
                    }
                    mapView.overlays.add(navPulseMarker)
                }
                navPulseMarker?.position = currentPt
                mapView.invalidate()

                routeAnimHandler.postDelayed(this, 50L) // 20 FPS smooth travelling pulse
            }
        }
    }

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SNIPPET = "extra_snippet"
        const val EXTRA_TRAIL_LATS = "extra_trail_lats"
        const val EXTRA_TRAIL_LONS = "extra_trail_lons"
        const val EXTRA_BEARING = "extra_bearing"
        const val EXTRA_ALTITUDE = "extra_altitude"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_SPOT_LATS = "extra_spot_lats"
        const val EXTRA_SPOT_LONS = "extra_spot_lons"
        const val EXTRA_SPOT_TITLES = "extra_spot_titles"
        const val EXTRA_SPOT_SNIPPETS = "extra_spot_snippets"
        const val EXTRA_SUN_AZIMUTH = "extra_sun_azimuth"
        const val EXTRA_SUNRISE_AZIMUTH = "extra_sunrise_azimuth"
        const val EXTRA_SUNSET_AZIMUTH = "extra_sunset_azimuth"
        const val EXTRA_HEADING_LOCK = "extra_heading_lock"

        private const val LOCATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup osmdroid configuration
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        initializeViews()
        setupSensors()
        setupMap()
        setupListeners()
        processIntentExtras()
        setupLocation()
    }

    private fun setupSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        mapView = findViewById(R.id.mapView)
        fabMyLocation = findViewById(R.id.fabMyLocation)
        fabLayer = findViewById(R.id.fabLayer)

        searchEditText = findViewById(R.id.searchEditText)
        btnSearch = findViewById(R.id.btnSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        navGuidanceCard = findViewById(R.id.navGuidanceCard)
        navDistanceText = findViewById(R.id.navDistanceText)
        navBearingText = findViewById(R.id.navBearingText)
        navEtaText = findViewById(R.id.navEtaText)
        navTargetAddressText = findViewById(R.id.navTargetAddressText)
        btnStopNav = findViewById(R.id.btnStopNav)

        cardTitleText = findViewById(R.id.cardTitleText)
        cardAddressText = findViewById(R.id.cardAddressText)
        accuracyBadge = findViewById(R.id.accuracyBadge)
        cardSpeedText = findViewById(R.id.cardSpeedText)
        cardAltitudeText = findViewById(R.id.cardAltitudeText)
        cardCoordinatesText = findViewById(R.id.cardCoordinatesText)
        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnCopyCoords = findViewById(R.id.btnCopyCoords)
        btnNavigate = findViewById(R.id.btnNavigate)
    }

    private fun setupListeners() {
        fabMyLocation.setOnClickListener {
            userLocation?.let { loc ->
                val pt = GeoPoint(loc.latitude, loc.longitude)
                mapView.controller.animateTo(pt)
                mapView.controller.setZoom(18.0)
                updateBottomCardWithLocation(loc, "📍 Live GPS Position")
            } ?: run {
                Toast.makeText(this, "Acquiring high-accuracy GPS...", Toast.LENGTH_SHORT).show()
                checkPermissionAndStart()
            }
        }

        fabLayer.setOnClickListener {
            cycleMapLayer()
        }

        btnShareLocation.setOnClickListener {
            shareLocation()
        }

        btnCopyCoords.setOnClickListener {
            copyCoordinates()
        }

        btnNavigate.setOnClickListener {
            launchNavigation()
        }

        // Search Bar listeners
        btnSearch.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchDestination(query)
            }
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchEditText.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchDestination(query)
                }
                true
            } else false
        }

        btnClearSearch.setOnClickListener {
            searchEditText.setText("")
            btnClearSearch.visibility = View.GONE
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnStopNav.setOnClickListener {
            stopNavigation()
        }

        // Tap to drop pin & measure distance / navigate
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                onMapTapped(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                onMapTapped(p)
                return true
            }
        })
        mapView.overlays.add(0, mapEventsOverlay)
    }

    /**
     * searchDestination - Uses OpenStreetMap's official Nominatim Geocoding engine
     * (with fallback to Android Geocoder) to accurately resolve landmarks (like CN Tower in Toronto),
     * cities, street addresses, and POIs worldwide without capital bias or incorrect city matches.
     */
    private fun searchDestination(query: String) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(searchEditText.windowToken, 0)
        Toast.makeText(this, "🔍 Searching '$query' on OpenStreetMap...", Toast.LENGTH_SHORT).show()

        bgExecutor.execute {
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
                // Ignore network error and fall back to Geocoder
            }

            // STEP 2: Fallback to Android Geocoder if Nominatim had no network
            if (foundPoint == null) {
                try {
                    val geocoder = Geocoder(this@MapActivity, Locale.getDefault())
                    val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        var result: List<Address>? = null
                        val latch = java.util.concurrent.CountDownLatch(1)
                        geocoder.getFromLocationName(query, 5) { list ->
                            result = list
                            latch.countDown()
                        }
                        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
                        result
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocationName(query, 5)
                    }

                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        foundPoint = GeoPoint(addr.latitude, addr.longitude)
                        val fullAddress = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
                        foundLabel = if (!addr.featureName.isNullOrEmpty() && addr.featureName != query) "${addr.featureName} ($fullAddress)" else fullAddress
                    }
                } catch (_: Exception) {}
            }

            // STEP 3: Update Map on Main Thread
            runOnUiThread {
                if (foundPoint != null) {
                    startNavigationTo(foundPoint, foundLabel)
                    mapView.controller.animateTo(foundPoint)
                    mapView.controller.setZoom(17.0)
                    Toast.makeText(this@MapActivity, "🎯 Found: $foundLabel", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MapActivity, "Location '$query' not found. Try another search.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * startNavigationTo - Sets the active destination, places a marker, computes road route,
     * and shows the live Navigation HUD.
     */
    fun startNavigationTo(point: GeoPoint, title: String) {
        destinationPoint = point
        destinationName = title
        currentActiveLat = point.latitude
        currentActiveLon = point.longitude

        if (tappedMarker == null) {
            tappedMarker = Marker(mapView).apply {
                position = point
                this.title = "🎯 Destination: $title"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(tappedMarker)
        } else {
            tappedMarker?.position = point
            tappedMarker?.title = "🎯 Destination: $title"
        }
        tappedMarker?.showInfoWindow()

        if (navGuidancePolyline == null) {
            navGuidancePolyline = Polyline().apply {
                outlinePaint.color = Color.parseColor("#00E5FF") // High-contrast Neon Cyan
                outlinePaint.strokeWidth = 10f
                this.title = "Navigation Route"
            }
            mapView.overlays.add(navGuidancePolyline)
        }

        navGuidanceCard.visibility = View.VISIBLE
        statusText.visibility = View.GONE
        updateNavigationHUD()
        fetchRoadRoute(point)
        mapView.invalidate()
    }

    /**
     * fetchRoadRoute - Uses OpenStreetMap's OSRM routing engine to fetch actual street geometries
     * between the user's current GPS location and the destination point.
     */
    private fun fetchRoadRoute(dest: GeoPoint) {
        val userLoc = userLocation ?: return
        val startLat = userLoc.latitude
        val startLon = userLoc.longitude
        val endLat = dest.latitude
        val endLon = dest.longitude

        bgExecutor.execute {
            val routePoints = ArrayList<GeoPoint>()
            try {
                val urlStr = "https://router.project-osrm.org/route/v1/driving/$startLon,$startLat;$endLon,$endLat?overview=full&geometries=geojson"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 4000
                conn.readTimeout = 4000

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
                            routePoints.add(GeoPoint(coord.getDouble(1), coord.getDouble(0)))
                        }
                    }
                }
            } catch (_: Exception) {
                // Fallback to straight line
                routePoints.clear()
                routePoints.add(GeoPoint(startLat, startLon))
                routePoints.add(dest)
            }

            if (routePoints.isNotEmpty()) {
                runOnUiThread {
                    navRoutePoints.clear()
                    navRoutePoints.addAll(routePoints)
                    navGuidancePolyline?.setPoints(routePoints)
                    startRoutePulseAnimation()
                    mapView.invalidate()
                }
            }
        }
    }

    /**
     * createNavigationPulseIcon - Generates a glowing, electric neon cyan traveling beacon
     * that slides along the street path toward the destination.
     */
    private fun createNavigationPulseIcon(): Drawable {
        val size = 42
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Outer translucent aura
        val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6600E5FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, auraPaint)

        // Vibrant neon cyan core
        val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 3.2f, corePaint)

        // High-contrast white center spark
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
            routeAnimHandler.post(routeAnimRunnable)
        }
    }

    private fun stopRoutePulseAnimation() {
        routeAnimHandler.removeCallbacksAndMessages(null)
        navPulseMarker?.let {
            mapView.overlays.remove(it)
            navPulseMarker = null
        }
        mapView.invalidate()
    }

    fun stopNavigation() {
        stopRoutePulseAnimation()
        destinationPoint = null
        destinationName = ""
        navRoutePoints.clear()
        navGuidancePolyline?.let {
            mapView.overlays.remove(it)
            navGuidancePolyline = null
        }
        tappedMarker?.let {
            mapView.overlays.remove(it)
            tappedMarker = null
        }
        navGuidanceCard.visibility = View.GONE
        statusText.visibility = View.VISIBLE
        statusText.text = "📍 Navigation Stopped"
        mapView.invalidate()
        Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateNavigationHUD() {
        val dest = destinationPoint ?: return
        val userLoc = userLocation ?: run {
            navDistanceText.text = "Waiting for GPS fix..."
            return
        }

        val userPt = GeoPoint(userLoc.latitude, userLoc.longitude)
        if (navGuidancePolyline?.actualPoints.isNullOrEmpty()) {
            navGuidancePolyline?.setPoints(listOf(userPt, dest))
        }

        val results = FloatArray(2)
        Location.distanceBetween(userLoc.latitude, userLoc.longitude, dest.latitude, dest.longitude, results)
        val distanceMeters = results[0]
        val bearingDegrees = (results[1] + 360f) % 360f

        val distString = if (distanceMeters >= 1000) {
            String.format(Locale.US, "%.2f km remaining", distanceMeters / 1000f)
        } else {
            String.format(Locale.US, "%.0f m remaining", distanceMeters)
        }
        navDistanceText.text = distString

        val cardinal = getCardinalDirection(bearingDegrees)
        navBearingText.text = String.format(Locale.US, "🧭 Head %03d° %s", bearingDegrees.toInt(), cardinal)

        val speed = if (userLoc.hasSpeed() && userLoc.speed > 0.5f) userLoc.speed else 1.4f // walking speed fallback (~5 km/h)
        val etaSeconds = (distanceMeters / speed).toLong()
        val etaMinutes = etaSeconds / 60
        navEtaText.text = if (etaMinutes < 1) "⏱️ ETA: < 1 min" else "⏱️ ETA: ~$etaMinutes mins"

        navTargetAddressText.text = "🎯 Target: $destinationName"

        if (distanceMeters <= 25) {
            navDistanceText.text = "🎉 You have arrived!"
            navBearingText.text = "🎯 Destination reached"
        }
    }

    private fun getCardinalDirection(azimuth: Float): String {
        val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val index = (((azimuth + 11.25) / 22.5).toInt()) % 16
        return directions[index]
    }

    private fun toggleCompassHeadingMode(forceEnable: Boolean? = null) {
        isCompassModeActive = forceEnable ?: !isCompassModeActive
        if (isCompassModeActive) {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        } else {
            sensorManager.unregisterListener(this)
            mapView.mapOrientation = 0f
        }
        mapView.invalidate()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isCompassModeActive || event == null) return
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
            event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR
        ) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
            val normalizedAzimuth = (azimuthDegrees + 360) % 360
            mapView.mapOrientation = -normalizedAzimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun cycleMapLayer() {
        currentLayerIndex = (currentLayerIndex + 1) % 3
        when (currentLayerIndex) {
            0 -> {
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                Toast.makeText(this, "🗺️ Map Style: Standard Street", Toast.LENGTH_SHORT).show()
            }
            1 -> {
                mapView.setTileSource(TileSourceFactory.OpenTopo)
                Toast.makeText(this, "🏔️ Map Style: Topographic & Elevation", Toast.LENGTH_SHORT).show()
            }
            2 -> {
                mapView.setTileSource(TileSourceFactory.HIKEBIKEMAP)
                Toast.makeText(this, "🚲 Map Style: Outdoors & Hiking Trails", Toast.LENGTH_SHORT).show()
            }
        }
        mapView.invalidate()
    }

    private fun onMapTapped(point: GeoPoint) {
        currentActiveLat = point.latitude
        currentActiveLon = point.longitude

        var distanceInfo = ""
        userLocation?.let { userLoc ->
            val results = FloatArray(1)
            Location.distanceBetween(userLoc.latitude, userLoc.longitude, point.latitude, point.longitude, results)
            val dist = results[0]
            distanceInfo = if (dist >= 1000) {
                String.format(Locale.US, " • %.2f km away", dist / 1000f)
            } else {
                String.format(Locale.US, " • %.0f m away", dist)
            }
        }

        cardTitleText.text = "🎯 Selected Destination$distanceInfo"
        accuracyBadge.text = "NAV TARGET"
        cardCoordinatesText.text = String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)
        cardAddressText.text = "Resolving destination address..."
        cardSpeedText.text = "--"
        cardAltitudeText.text = String.format(Locale.US, "%.0f m", point.altitude)

        startNavigationTo(point, "Pinned Destination")
        reverseGeocode(point.latitude, point.longitude)
    }

    private fun shareLocation() {
        if (currentActiveLat == 0.0 && currentActiveLon == 0.0) {
            Toast.makeText(this, "No location selected to share", Toast.LENGTH_SHORT).show()
            return
        }
        val url = "https://www.openstreetmap.org/?mlat=$currentActiveLat&mlon=$currentActiveLon#map=17/$currentActiveLat/$currentActiveLon"
        val shareText = "📍 Location: $currentActiveAddress\n🌐 Coordinates: $currentActiveLat, $currentActiveLon\n🗺️ OpenStreetMap: $url"

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, "Share Location via"))
    }

    private fun copyCoordinates() {
        if (currentActiveLat == 0.0 && currentActiveLon == 0.0) {
            Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show()
            return
        }
        val coords = String.format(Locale.US, "%.6f, %.6f", currentActiveLat, currentActiveLon)
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("GPS Coordinates", coords)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "📋 Copied: $coords", Toast.LENGTH_SHORT).show()
    }

    private fun launchNavigation() {
        if (currentActiveLat == 0.0 && currentActiveLon == 0.0) {
            Toast.makeText(this, "Tap on the map or search an address to navigate", Toast.LENGTH_SHORT).show()
            return
        }
        val targetPoint = GeoPoint(currentActiveLat, currentActiveLon)
        startNavigationTo(targetPoint, currentActiveAddress)
        mapView.controller.animateTo(targetPoint)
        mapView.controller.setZoom(18.0)
        Toast.makeText(this, "🎯 Navigating to: $currentActiveAddress", Toast.LENGTH_SHORT).show()
    }

    private fun reverseGeocode(lat: Double, lon: Double) {
        bgExecutor.execute {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(lat, lon, 1) { addresses ->
                        val addr = formatAddress(addresses)
                        runOnUiThread {
                            currentActiveAddress = addr
                            cardAddressText.text = addr
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    val addr = formatAddress(addresses)
                    runOnUiThread {
                        currentActiveAddress = addr
                        cardAddressText.text = addr
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    currentActiveAddress = String.format(Locale.US, "Lat: %.5f, Lon: %.5f", lat, lon)
                    cardAddressText.text = currentActiveAddress
                }
            }
        }
    }

    private fun formatAddress(addresses: List<Address>?): String {
        if (addresses.isNullOrEmpty()) return "Address not found"
        val address = addresses[0]
        val parts = mutableListOf<String>()
        address.thoroughfare?.let { parts.add(it) } ?: address.featureName?.let { parts.add(it) }
        address.locality?.let { parts.add(it) } ?: address.subAdminArea?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else address.getAddressLine(0) ?: "Unknown Location"
    }

    private fun updateBottomCardWithLocation(location: Location, title: String) {
        currentActiveLat = location.latitude
        currentActiveLon = location.longitude

        cardTitleText.text = title
        accuracyBadge.text = if (location.hasAccuracy()) String.format(Locale.US, "±%.1fm", location.accuracy) else "GPS OK"
        cardCoordinatesText.text = String.format(Locale.US, "%.5f, %.5f", location.latitude, location.longitude)
        cardSpeedText.text = if (location.hasSpeed()) String.format(Locale.US, "%.1f km/h", location.speed * 3.6f) else "0.0 km/h"
        cardAltitudeText.text = if (location.hasAltitude()) String.format(Locale.US, "%.0f m", location.altitude) else "-- m"

        reverseGeocode(location.latitude, location.longitude)
    }

    /**
     * processIntentExtras - Reads any coordinates, routes, spots, or headings passed by other tools
     */
    private fun processIntentExtras() {
        val spotLats = intent.getDoubleArrayExtra(EXTRA_SPOT_LATS)
        val spotLons = intent.getDoubleArrayExtra(EXTRA_SPOT_LONS)
        val spotTitles = intent.getStringArrayExtra(EXTRA_SPOT_TITLES)
        val spotSnippets = intent.getStringArrayExtra(EXTRA_SPOT_SNIPPETS)

        if (spotLats != null && spotLons != null && spotLats.isNotEmpty() && spotLats.size == spotLons.size) {
            val geoPoints = ArrayList<GeoPoint>()
            for (i in spotLats.indices) {
                val point = GeoPoint(spotLats[i], spotLons[i])
                geoPoints.add(point)

                val spotMarker = Marker(mapView).apply {
                    position = point
                    title = if (spotTitles != null && i < spotTitles.size) spotTitles[i] else "Detection Spot #${i + 1}"
                    snippet = if (spotSnippets != null && i < spotSnippets.size) spotSnippets[i] else String.format(Locale.US, "%.5f, %.5f", spotLats[i], spotLons[i])
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(spotMarker)
                if (i == spotLats.size - 1) {
                    spotMarker.showInfoWindow()
                }
            }

            firstLocation = false
            val mainTitle = intent.getStringExtra(EXTRA_TITLE) ?: "Detection Spots"
            statusText.text = "$mainTitle (${geoPoints.size} spots mapped)"

            mapView.post {
                try {
                    if (geoPoints.size > 1) {
                        val box = BoundingBox.fromGeoPoints(geoPoints)
                        mapView.zoomToBoundingBox(box, true, 130)
                    } else {
                        mapView.controller.setCenter(geoPoints.first())
                        mapView.controller.setZoom(17.5)
                    }
                } catch (e: Exception) {
                    mapView.controller.setCenter(geoPoints.first())
                    mapView.controller.setZoom(17.0)
                }
            }
            return
        }

        val trailLats = intent.getDoubleArrayExtra(EXTRA_TRAIL_LATS)
        val trailLons = intent.getDoubleArrayExtra(EXTRA_TRAIL_LONS)

        if (trailLats != null && trailLons != null && trailLats.isNotEmpty() && trailLats.size == trailLons.size) {
            // Draw Polyline route from TrailTracker / DistanceTracker
            val geoPoints = ArrayList<GeoPoint>()
            for (i in trailLats.indices) {
                geoPoints.add(GeoPoint(trailLats[i], trailLons[i]))
            }

            routePolyline = Polyline().apply {
                outlinePaint.color = Color.parseColor("#FF5722") // Vibrant orange
                outlinePaint.strokeWidth = 10f
                setPoints(geoPoints)
                title = intent.getStringExtra(EXTRA_TITLE) ?: "Recorded Route"
            }
            mapView.overlays.add(routePolyline)

            // Add Start Marker
            startMarker = Marker(mapView).apply {
                position = geoPoints.first()
                title = "🟢 Start Point"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(startMarker)

            // Add End Marker
            if (geoPoints.size > 1) {
                endMarker = Marker(mapView).apply {
                    position = geoPoints.last()
                    title = "🔴 End Point"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                mapView.overlays.add(endMarker)
            }

            firstLocation = false
            mapView.post {
                try {
                    val box = BoundingBox.fromGeoPoints(geoPoints)
                    mapView.zoomToBoundingBox(box, true, 120)
                } catch (e: Exception) {
                    mapView.controller.setCenter(geoPoints.first())
                    mapView.controller.setZoom(16.0)
                }
            }
            statusText.text = "${geoPoints.size} GPS trail points mapped"
            return
        }

        // Single Coordinate Pinned Location (Location, Sun, Moon, Altimeter, Compass, etc.)
        if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
            val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
            val lon = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "Pinned Location"
            val snippet = intent.getStringExtra(EXTRA_SNIPPET) ?: String.format("Lat: %.6f, Lon: %.6f", lat, lon)
            val bearing = intent.getFloatExtra(EXTRA_BEARING, 0f)
            val sunAzimuth = intent.getDoubleExtra(EXTRA_SUN_AZIMUTH, -1.0)

            val point = GeoPoint(lat, lon)
            locationMarker = Marker(mapView).apply {
                position = point
                this.title = title
                this.snippet = snippet
                if (bearing != 0f) {
                    rotation = bearing
                }
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(locationMarker)
            locationMarker?.showInfoWindow()

            // Solar Rays: Current Sun, Sunrise, and Sunset vectors
            val sunriseAzimuth = intent.getDoubleExtra(EXTRA_SUNRISE_AZIMUTH, -1.0)
            val sunsetAzimuth = intent.getDoubleExtra(EXTRA_SUNSET_AZIMUTH, -1.0)
            val rayLength = 0.005 // ~500 meters on map

            if (sunriseAzimuth >= 0) {
                val rad = Math.toRadians(sunriseAzimuth)
                val destLat = lat + rayLength * kotlin.math.cos(rad)
                val destLon = lon + (rayLength * kotlin.math.sin(rad) / kotlin.math.cos(Math.toRadians(lat)))
                val sunriseRay = Polyline().apply {
                    outlinePaint.color = Color.parseColor("#FF9800") // Vibrant Sunrise Orange
                    outlinePaint.strokeWidth = 6f
                    setPoints(listOf(point, GeoPoint(destLat, destLon)))
                    this.title = "🌅 Sunrise Azimuth: ${sunriseAzimuth.toInt()}°"
                }
                mapView.overlays.add(sunriseRay)
            }

            if (sunAzimuth >= 0) {
                val rad = Math.toRadians(sunAzimuth)
                val destLat = lat + rayLength * kotlin.math.cos(rad)
                val destLon = lon + (rayLength * kotlin.math.sin(rad) / kotlin.math.cos(Math.toRadians(lat)))
                val sunRay = Polyline().apply {
                    outlinePaint.color = Color.parseColor("#FFD700") // Gold
                    outlinePaint.strokeWidth = 8f
                    setPoints(listOf(point, GeoPoint(destLat, destLon)))
                    this.title = "☀️ Live Sun Direction: ${sunAzimuth.toInt()}°"
                }
                mapView.overlays.add(sunRay)
            }

            if (sunsetAzimuth >= 0) {
                val rad = Math.toRadians(sunsetAzimuth)
                val destLat = lat + rayLength * kotlin.math.cos(rad)
                val destLon = lon + (rayLength * kotlin.math.sin(rad) / kotlin.math.cos(Math.toRadians(lat)))
                val sunsetRay = Polyline().apply {
                    outlinePaint.color = Color.parseColor("#E91E63") // Deep Sunset Pink/Red
                    outlinePaint.strokeWidth = 6f
                    setPoints(listOf(point, GeoPoint(destLat, destLon)))
                    this.title = "🌇 Sunset Azimuth: ${sunsetAzimuth.toInt()}°"
                }
                mapView.overlays.add(sunsetRay)
            }

            val lockHeading = intent.getBooleanExtra(EXTRA_HEADING_LOCK, false)
            if (lockHeading) {
                toggleCompassHeadingMode(true)
            }

            currentActiveLat = lat
            currentActiveLon = lon
            currentActiveAddress = snippet
            cardTitleText.text = title
            cardAddressText.text = snippet
            cardCoordinatesText.text = String.format(Locale.US, "%.5f, %.5f", lat, lon)

            firstLocation = false
            mapView.controller.setCenter(point)
            mapView.controller.setZoom(17.5)
            statusText.text = "$title\n$snippet"
            reverseGeocode(lat, lon)
        }
    }

    /**
     * setupMap - Configures the OpenStreetMap
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Sets the tile source to MAPNIK (standard OpenStreetMap style)
     * 2. Enables multi-touch controls (pinch to zoom)
     * 3. Enables built-in zoom controls (+ and - buttons)
     * 4. Sets initial zoom level to 17.5 (street level)
     *
     * ============================================================
     * TILE SOURCE OPTIONS:
     * ============================================================
     * - TileSourceFactory.MAPNIK : Standard OpenStreetMap (recommended)
     * - TileSourceFactory.HIKEBIKEMAP : Topographic map
     * - TileSourceFactory.CYCLEMAP : Cycle routes
     */
    private fun setupMap() {
        // Use standard OpenStreetMap tiles (MAPNIK style)
        mapView.setTileSource(TileSourceFactory.MAPNIK)

        // Enable pinch-to-zoom and pan
        mapView.setMultiTouchControls(true)

        // Enable + and - zoom buttons on the map
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)

        // Set initial zoom level to 17.5 (street level)
        mapView.controller.setZoom(17.5)

        // Update status text
        statusText.text = "Waiting for location..."
    }

    /**
     * setupLocation - Initializes location services
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Initializes FusedLocationProviderClient
     * 2. Creates LocationCallback
     * 3. Checks location permission
     * 4. Starts updates if permission granted
     */
    private fun setupLocation() {
        // ============================================================
        // STEP 1: Initialize FusedLocationProviderClient
        // ============================================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ============================================================
        // STEP 2: Create LocationCallback
        // ============================================================
        locationCallback = object : LocationCallback() {

            /**
             * onLocationResult - Called when location updates arrive
             *
             * This is the same pattern as LiveLocationActivity!
             * Each time location changes, we update the map.
             *
             * @param locationResult Contains the new location(s)
             */
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateMapLocation(location)
                }
            }
        }

        // ============================================================
        // STEP 3: Check permission and start updates
        // ============================================================
        checkPermissionAndStart()
    }

    /**
     * requestInitialLocationFix - Immediately retrieves the latest known cached GPS fix
     * or active high-accuracy single fix so the user does not have to wait 3-5 seconds
     * for periodic location updates to kick in.
     */
    private fun requestInitialLocationFix() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null && (userLocation == null || firstLocation)) {
                updateMapLocation(loc)
            }
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
            if (loc != null) {
                updateMapLocation(loc)
            }
        }
    }

    // ============================================================
    // PERMISSION METHODS
    // ============================================================

    /**
     * checkPermissionAndStart - Checks permission and starts updates
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Check if location permission is granted
     * 2. If YES: Start location updates & request immediate initial fix
     * 3. If NO: Request permission from the user
     */
    private fun checkPermissionAndStart() {
        // Check if we have location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted - start updates & fetch immediate initial fix
            requestInitialLocationFix()
            startLocationUpdates()
        } else {
            // Permission not granted - request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    /**
     * onRequestPermissionsResult - Handles permission request result
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Called after user responds to permission dialog
     * 2. If user granted permission: Start location updates
     * 3. If user denied permission: Show error message
     *
     * @param requestCode The request code we used (LOCATION_PERMISSION_REQUEST)
     * @param permissions The permissions that were requested
     * @param grantResults The results (granted or denied)
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Check if this is our permission request
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // User granted permission - start updates
            requestInitialLocationFix()
            startLocationUpdates()
        } else {
            // User denied permission - show message
            statusText.text = "Location permission was denied."
            Toast.makeText(
                this,
                "Location permission is required for maps.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ============================================================
    // LOCATION UPDATE METHODS
    // ============================================================

    /**
     * startLocationUpdates - Begins receiving location updates
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Creates LocationRequest with 3-second interval
     * 2. Requests updates from FusedLocationProviderClient
     * 3. Updates status text
     *
     * ============================================================
     * WHY 3 SECONDS?
     * ============================================================
     * - Fast enough for smooth map updates
     * - Saves battery compared to 1-second updates
     * - Good balance between accuracy and battery life
     */
    private fun startLocationUpdates() {
        // ============================================================
        // STEP 1: Create LocationRequest
        // ============================================================
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,  // Use GPS for best accuracy
            3000  // Update every 3 seconds
        ).apply {
            // Minimum time between updates: 1 second
            setMinUpdateIntervalMillis(1000)
        }.build()

        // ============================================================
        // STEP 2: Check permission again (defensive check)
        // ============================================================
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // ============================================================
            // STEP 3: Request location updates
            // ============================================================
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )

            // ============================================================
            // STEP 4: Update status
            // ============================================================
            statusText.text = "Receiving location updates..."
        }
    }

    /**
     * updateMapLocation - Updates the map with new location
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Extract latitude and longitude from Location object
     * 2. Convert to GeoPoint (osmdroid's coordinate class)
     * 3. Update status text with coordinates
     * 4. Create or move the location marker
     * 5. Center the map on the new location
     * 6. Refresh the map
     *
     * @param location The new Location object from GPS
     *
     * ============================================================
     * GeoPoint vs Location:
     * ============================================================
     * - Location: From Google Play Services (standard Android)
     * - GeoPoint: From osmdroid (used for OpenStreetMap)
     * - We convert between them using the constructor
     */
    private fun updateMapLocation(location: Location) {
        userLocation = location
        // ============================================================
        // STEP 1: Extract coordinates
        // ============================================================
        val latitude = location.latitude
        val longitude = location.longitude

        // ============================================================
        // STEP 2: Update status text with coordinates
        // ============================================================
        statusText.text = String.format(
            "Lat: %.6f\nLon: %.6f",
            latitude,
            longitude
        )

        // ============================================================
        // STEP 3: Convert to GeoPoint (osmdroid format)
        // ============================================================
        val currentPosition = GeoPoint(latitude, longitude)

        // ============================================================
        // STEP 4: Update or create the marker
        // ============================================================
        if (locationMarker == null) {
            // ============================================================
            // FIRST TIME - Create a new marker
            // ============================================================
            locationMarker = Marker(mapView).apply {
                position = currentPosition
                title = "Current Location"

                // Anchor the marker at the bottom-center
                // This makes the marker point to the exact location
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }

            // Add the marker to the map
            mapView.overlays.add(locationMarker)

        } else {
            // ============================================================
            // EXISTING MARKER - Just move it to the new position
            // ============================================================
            locationMarker?.position = currentPosition
        }

        // ============================================================
        // STEP 5: Center the map on the new location
        // ============================================================
        if (firstLocation) {
            // ============================================================
            // FIRST LOCATION FIX - Zoom and center
            // ============================================================
            // This only happens once!
            // We zoom to street level (17.5) and center the map.
            // ============================================================
            mapView.controller.setCenter(currentPosition)
            firstLocation = false

        } else {
            // ============================================================
            // SUBSEQUENT UPDATES - Animate smoothly
            // ============================================================
            // This happens every time location changes.
            // The map moves smoothly to follow the user.
            // ============================================================
            mapView.controller.animateTo(currentPosition)
        }

        updateBottomCardWithLocation(location, "📍 Live GPS Position")

        // Update Point A to Point B live route & HUD if navigating
        if (destinationPoint != null) {
            updateNavigationHUD()
        }

        // ============================================================
        // STEP 6: Refresh the map
        // ============================================================
        mapView.invalidate()
    }

    // ============================================================
    // LIFECYCLE METHODS - Manage map lifecycle
    // ============================================================

    /**
     * onResume - Called when the activity becomes visible
     *
     * ============================================================
     * WHAT HAPPENS HERE:
     * ============================================================
     * 1. Resume the map (important for osmdroid)
     * 2. Check if we need to restart location updates
     *
     * ============================================================
     * WHY WE RESUME THE MAP:
     * ============================================================
     * - osmdroid requires onResume() to be called
     * - This reconnects to the tile cache and network
     * - Ensures map tiles load properly
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        if (isCompassModeActive) {
            rotationSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
        checkPermissionAndStart()
    }

    override fun onPause() {
        super.onPause()
        stopRoutePulseAnimation()
        if (isCompassModeActive) {
            sensorManager.unregisterListener(this)
        }
        fusedLocationClient.removeLocationUpdates(locationCallback)
        mapView.onPause()
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    /**
     * onSupportNavigateUp - Handles the back button in the toolbar
     *
     * Returns to the DashboardActivity when user clicks back arrow.
     *
     * @return true if navigation was handled
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}