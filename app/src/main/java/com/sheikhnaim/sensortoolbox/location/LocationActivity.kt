package com.sheikhnaim.sensortoolbox.location

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sheikhnaim.sensortoolbox.MapActivity
import com.sheikhnaim.sensortoolbox.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

/**
 * LocationActivity - High-Accuracy GPS Location Tool & Coordinate Inspector
 *
 * HOW IT WORKS (GPS & Geodesy):
 * 1. Uses Google Play Services FusedLocationProviderClient with PRIORITY_HIGH_ACCURACY.
 * 2. Fetches exact Geodetic Coordinates (Latitude, Longitude) in both:
 *    - Standard Decimal Degrees (e.g. 40.7128° N, 74.0060° W)
 *    - Degrees, Minutes, Seconds (DMS) (e.g. 40° 42' 46" N, 74° 0' 21" W)
 * 3. Measures horizontal GPS fix precision (Accuracy in meters).
 * 4. Performs asynchronous Reverse Geocoding using Android's Geocoder API to resolve
 *    street address, postal code, administrative area, and country.
 * 5. Embeds a live interactive OpenStreetMap view that centers on the user's position
 *    and provides quick-share & clipboard copying actions.
 */
class LocationActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // UI VIEWS
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var addressText: TextView
    private lateinit var statusText: TextView
    private lateinit var dmsText: TextView
    private lateinit var accuracyText: TextView
    private lateinit var mapView: MapView

    private lateinit var getLocationButton: Button
    private lateinit var viewMapButton: Button
    private lateinit var btnShareLocation: Button
    private lateinit var btnCopyCoords: Button
    private lateinit var btnNavigate: Button

    private var locationMarker: Marker? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAddress: String? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (fineLocationGranted) {
                getCurrentLocation()
            } else {
                statusText.text = getString(R.string.permission_denied)
                Toast.makeText(this, getString(R.string.permission_denied_toast), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_location)

        setupToolbar()
        initializeViews()
        setupMap()
        initializeLocationClient()
        setupClickListeners()
        setInitialStatus()

        // Auto-fetch if permission already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.gps_location_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun initializeViews() {
        latitudeText = findViewById(R.id.latitudeText)
        longitudeText = findViewById(R.id.longitudeText)
        altitudeText = findViewById(R.id.altitudeText)
        addressText = findViewById(R.id.addressText)
        statusText = findViewById(R.id.statusText)
        dmsText = findViewById(R.id.dmsText)
        accuracyText = findViewById(R.id.accuracyText)
        mapView = findViewById(R.id.mapView)

        getLocationButton = findViewById(R.id.getLocationButton)
        viewMapButton = findViewById(R.id.viewMapButton)
        btnShareLocation = findViewById(R.id.btnShareLocation)
        btnCopyCoords = findViewById(R.id.btnCopyCoords)
        btnNavigate = findViewById(R.id.btnNavigate)
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(16.5)
    }

    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupClickListeners() {
        getLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        viewMapButton.setOnClickListener {
            val lat = currentLatitude
            val lon = currentLongitude
            val intent = Intent(this, MapActivity::class.java)
            if (lat != null && lon != null) {
                intent.putExtra(MapActivity.EXTRA_LATITUDE, lat)
                intent.putExtra(MapActivity.EXTRA_LONGITUDE, lon)
                intent.putExtra(MapActivity.EXTRA_TITLE, "📍 GPS Location")
                intent.putExtra(MapActivity.EXTRA_SNIPPET, currentAddress ?: "Lat: $lat, Lon: $lon")
            }
            startActivity(intent)
        }

        btnShareLocation.setOnClickListener {
            val lat = currentLatitude
            val lon = currentLongitude
            if (lat == null || lon == null) {
                Toast.makeText(this, "Acquire GPS position first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val url = "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"
            val text = "📍 My GPS Location:\nAddress: ${currentAddress ?: addressText.text}\nCoordinates: $lat, $lon\nDMS: ${dmsText.text}\nAltitude: ${altitudeText.text}\n🗺️ OpenStreetMap: $url"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "My GPS Location")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(sendIntent, "Share Location via"))
        }

        btnCopyCoords.setOnClickListener {
            val lat = currentLatitude
            val lon = currentLongitude
            if (lat == null || lon == null) {
                Toast.makeText(this, "Acquire GPS position first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val coords = String.format(Locale.US, "%.6f, %.6f", lat, lon)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("GPS Coordinates", coords)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "📋 Coordinates copied: $coords", Toast.LENGTH_SHORT).show()
        }

        btnNavigate.setOnClickListener {
            val lat = currentLatitude
            val lon = currentLongitude
            if (lat == null || lon == null) {
                Toast.makeText(this, "Acquire GPS position first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, MapActivity::class.java).apply {
                putExtra(MapActivity.EXTRA_LATITUDE, lat)
                putExtra(MapActivity.EXTRA_LONGITUDE, lon)
                putExtra(MapActivity.EXTRA_TITLE, "📍 GPS Location")
                putExtra(MapActivity.EXTRA_SNIPPET, currentAddress ?: "Lat: $lat, Lon: $lon")
            }
            startActivity(intent)
        }
    }

    private fun toDms(coordinate: Double, isLatitude: Boolean): String {
        val absolute = Math.abs(coordinate)
        val degrees = absolute.toInt()
        val minutesNotTruncated = (absolute - degrees) * 60.0
        val minutes = minutesNotTruncated.toInt()
        val seconds = ((minutesNotTruncated - minutes) * 60.0).toInt()
        val direction = if (isLatitude) {
            if (coordinate >= 0) "N" else "S"
        } else {
            if (coordinate >= 0) "E" else "W"
        }
        return "$degrees° $minutes' $seconds\" $direction"
    }

    /**
     * Sets the initial status message.
     */
    private fun setInitialStatus() {
        statusText.text = getString(R.string.location_ready)
    }

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================

    /**
     * checkLocationPermission - Checks if we have permission, requests if not
     *
     * HOW IT WORKS:
     * 1. Check if we have fine location permission
     * 2. If YES: get the location
     * 3. If NO: request permission from the user
     */
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // We have permission - get location
            getCurrentLocation()
        } else {
            // We don't have permission - request it
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ============================================================
    // GET CURRENT LOCATION
    // ============================================================

    /**
     * getCurrentLocation - Gets the device's current location using GPS
     *
     * HOW IT WORKS:
     * 1. Checks if we have permission (just in case)
     * 2. Asks FusedLocationProviderClient for the current location
     * 3. On success: displays the location
     * 4. On failure: shows an error message
     */
    private fun getCurrentLocation() {
        // Check permission again (just in case)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Update status to show we're working
        statusText.text = getString(R.string.location_getting)
        addressText.text = getString(R.string.location_searching)

        // Request the current location from GPS
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,  // Use high accuracy GPS
            null
        )
            .addOnSuccessListener { location: Location? ->
                // Location received successfully
                if (location != null) {
                    displayLocation(location)
                } else {
                    // Location is null (device couldn't get GPS signal)
                    showLocationUnavailable()
                }
            }
            .addOnFailureListener { _ ->
                // Location request failed (error)
                showLocationError()
            }
    }

    // ============================================================
    // DISPLAY LOCATION
    // ============================================================

    /**
     * displayLocation - Updates the UI with the current location
     *
     * @param location The Location object from GPS
     *
     * WHAT IT DOES:
     * 1. Extracts latitude, longitude, and altitude from the Location object
     * 2. Formats and displays them in the TextViews
     * 3. Calls reverse geocoding to get the address
     */
    private fun displayLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val altitude = location.altitude

        currentLatitude = latitude
        currentLongitude = longitude

        // Format and display coordinates (6 decimal places for accuracy)
        latitudeText.text = String.format(Locale.US, "%.6f", latitude)
        longitudeText.text = String.format(Locale.US, "%.6f", longitude)

        // DMS formatting
        dmsText.text = "${toDms(latitude, true)}, ${toDms(longitude, false)}"

        // Accuracy badge
        if (location.hasAccuracy()) {
            accuracyText.text = String.format(Locale.US, "GPS: ±%.1fm", location.accuracy)
        } else {
            accuracyText.text = "GPS: Active"
        }

        // Display altitude
        altitudeText.text = String.format(Locale.US, "%.1f m", altitude)

        // Update status
        statusText.text = getString(R.string.location_found)

        // Update map position and marker
        val point = GeoPoint(latitude, longitude)
        if (locationMarker == null) {
            locationMarker = Marker(mapView).apply {
                position = point
                title = "My GPS Position"
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            mapView.overlays.add(locationMarker)
        } else {
            locationMarker?.position = point
        }
        mapView.controller.animateTo(point)
        mapView.invalidate()

        // Get the address from coordinates
        getAddress(latitude, longitude)
    }

    /**
     * Shows location unavailable message.
     */
    private fun showLocationUnavailable() {
        latitudeText.text = getString(R.string.gps_default_coordinate)
        longitudeText.text = getString(R.string.gps_default_coordinate)
        altitudeText.text = getString(R.string.gps_default_altitude)
        addressText.text = getString(R.string.gps_location_unavailable)
        statusText.text = getString(R.string.location_unavailable_short)
        Toast.makeText(
            this,
            getString(R.string.location_unavailable_toast),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Shows location error message.
     */
    private fun showLocationError() {
        latitudeText.text = getString(R.string.gps_default_coordinate)
        longitudeText.text = getString(R.string.gps_default_coordinate)
        altitudeText.text = getString(R.string.gps_default_altitude)
        addressText.text = getString(R.string.location_error)
        statusText.text = getString(R.string.location_error)
        Toast.makeText(
            this,
            getString(R.string.location_error_toast),
            Toast.LENGTH_LONG
        ).show()
    }

    // ============================================================
    // REVERSE GEOCODING - Convert coordinates to address
    // ============================================================

    /**
     * getAddress - Converts latitude/longitude to a human-readable address
     *
     * ============================================================
     * WHAT IS REVERSE GEOCODING?
     * ============================================================
     * - Forward geocoding: Address → Coordinates
     * - Reverse geocoding: Coordinates → Address (what we're doing here)
     *
     * ============================================================
     * HOW IT WORKS:
     * ============================================================
     * 1. Creates a Geocoder instance
     * 2. Calls getFromLocation() with latitude and longitude
     * 3. Gets a list of Address objects (usually 1-2 results)
     * 4. Displays the first address
     *
     * ============================================================
     * ANDROID VERSION HANDLING:
     * ============================================================
     * - Android 13+ (API 33+): Uses async GeocodeListener API
     * - Android 12 and below: Uses synchronous API in background thread
     *
     * This is the SAME technique used in the LocationFinder tutorial!
     *
     * @param latitude The latitude to convert
     * @param longitude The longitude to convert
     */
    private fun getAddress(latitude: Double, longitude: Double) {
        // Check if Geocoder is available on the device
        if (!Geocoder.isPresent()) {
            addressText.text = getString(R.string.location_geocoder_unavailable)
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        // ============================================================
        // ANDROID 13+ (API 33+) - Use asynchronous API
        // ============================================================
        // Modern Android requires asynchronous geocoding to avoid blocking the UI
        // The GeocodeListener callback runs on a background thread automatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    // Called when addresses are found
                    override fun onGeocode(addresses: MutableList<Address>) {
                        runOnUiThread {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val addrStr = address.getAddressLine(0) ?: getString(R.string.location_no_address)
                                currentAddress = addrStr
                                addressText.text = addrStr
                            } else {
                                addressText.text = getString(R.string.location_no_address)
                            }
                        }
                    }

                    // Called when there's an error
                    override fun onError(errorMessage: String?) {
                        runOnUiThread {
                            addressText.text = getString(R.string.location_no_address)
                        }
                    }
                }
            )
        } else {
            // ============================================================
            // ANDROID 12 AND BELOW - Use synchronous API in background thread
            // ============================================================
            // Older Android versions use synchronous geocoding
            // We run it in a background thread to avoid blocking the UI
            // @Suppress is used to hide the deprecation warning since this
            // is still needed for older devices
            @Suppress("DEPRECATION")
            Thread {
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    runOnUiThread {
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val addrStr = address.getAddressLine(0) ?: getString(R.string.location_no_address)
                            currentAddress = addrStr
                            addressText.text = addrStr
                        } else {
                            addressText.text = getString(R.string.location_no_address)
                        }
                    }
                } catch (_: Exception) {
                    // Exception caught but not used - that's okay
                    // We just show a user-friendly error message
                    runOnUiThread {
                        addressText.text = getString(R.string.location_no_address)
                    }
                }
            }.start()
        }
    }

    // ============================================================
    // NAVIGATION METHODS
    // ============================================================

    /**
     * onSupportNavigateUp - Handles the back button in the toolbar
     * Returns to the DashboardActivity
     *
     * @return true if navigation was handled
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}