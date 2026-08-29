package com.sheikhnaim.sensortoolbox.location

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.Manifest                              // For location permission
import android.content.pm.PackageManager            // To check if permission is granted
import android.location.Address                     // For address data from Geocoder
import android.location.Geocoder                   // For converting coordinates to address
import android.location.Location                   // For GPS location data
import android.os.Build                            // For checking Android version
import android.os.Bundle                           // For saving/restoring state
import android.widget.Button                       // For button views
import android.widget.TextView                     // For text views
import android.widget.Toast                        // For showing toast messages
import androidx.activity.result.contract.ActivityResultContracts // For permission handling
import androidx.appcompat.app.AppCompatActivity     // Base class for our activity
import androidx.appcompat.widget.Toolbar           // The top bar with back button
import androidx.core.content.ContextCompat          // For checking permissions safely
import com.google.android.gms.location.FusedLocationProviderClient // For GPS
import com.google.android.gms.location.LocationServices // For getting location services
import com.google.android.gms.location.Priority    // For location accuracy priority
import com.sheikhnaim.sensortoolbox.R              // Resource IDs (layouts, strings, etc.)
import java.util.Locale                             // For locale-specific formatting

/**
 * LocationActivity - GPS Location Tool
 *
 * ============================================================
 * HOW THIS WORKS:
 * ============================================================
 * 1. Uses FusedLocationProviderClient to get high-accuracy GPS location
 * 2. Displays Latitude, Longitude, and Altitude
 * 3. Uses Geocoder to convert coordinates to a human-readable address
 * 4. Handles location permissions properly
 *
 * ============================================================
 * WHY WE NEED PERMISSIONS:
 * ============================================================
 * - Android requires runtime permission for location access
 * - Users must grant permission before the app can access GPS
 * - We handle this with ActivityResultContracts for modern Android
 *
 * ============================================================
 * WHAT IS REVERSE GEOCODING?
 * ============================================================
 * - Converting latitude/longitude coordinates to a human-readable address
 * - Example: (43.7740, -79.3450) → "Toronto, ON"
 * - The Geocoder class handles this for us
 *
 * This is the SAME technique used in the LocationFinder tutorial!
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class LocationActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT - Gets location from GPS
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS - References to the layout elements
    // ============================================================
    private lateinit var latitudeText: TextView      // Shows latitude value
    private lateinit var longitudeText: TextView     // Shows longitude value
    private lateinit var altitudeText: TextView      // Shows altitude value
    private lateinit var addressText: TextView       // Shows address from reverse geocoding
    private lateinit var statusText: TextView        // Shows status messages
    private lateinit var getLocationButton: Button   // Button to trigger location fetch

    // ============================================================
    // PERMISSION HANDLING - Requests location permission from user
    // ============================================================
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            // Check if the user granted fine location permission
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (fineLocationGranted) {
                // Permission granted - get location
                getCurrentLocation()
            } else {
                // Permission denied - show message using string resource
                statusText.text = getString(R.string.permission_denied)
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied_toast),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // ============================================================
    // LIFECYCLE METHODS - When the activity starts/stops
    // ============================================================

    /**
     * onCreate - Called when the activity is first created
     *
     * This sets up:
     * - The layout (UI)
     * - The toolbar (top bar with back button)
     * - All UI views
     * - The FusedLocationProviderClient for GPS
     * - The Get Location button click listener
     *
     * @param savedInstanceState Previously saved state (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        setupToolbar()
        initializeViews()
        initializeLocationClient()
        setupClickListeners()
        setInitialStatus()
    }

    // ============================================================
    // INITIALIZATION METHODS
    // ============================================================

    /**
     * Sets up the toolbar with title and back navigation.
     */
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Show back button
        supportActionBar?.title = getString(R.string.gps_location_title)
    }

    /**
     * Finds and initializes all UI views from the layout.
     */
    private fun initializeViews() {
        latitudeText = findViewById(R.id.latitudeText)
        longitudeText = findViewById(R.id.longitudeText)
        altitudeText = findViewById(R.id.altitudeText)
        addressText = findViewById(R.id.addressText)
        statusText = findViewById(R.id.statusText)
        getLocationButton = findViewById(R.id.getLocationButton)
    }

    /**
     * Initializes the FusedLocationProviderClient for GPS access.
     */
    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Sets up click listeners for buttons.
     */
    private fun setupClickListeners() {
        getLocationButton.setOnClickListener {
            checkLocationPermission()
        }
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

        // Format and display coordinates (6 decimal places for accuracy)
        latitudeText.text = String.format(Locale.US, "%.6f", latitude)
        longitudeText.text = String.format(Locale.US, "%.6f", longitude)

        // Display altitude (handles negative values - below sea level)
        altitudeText.text = String.format(Locale.US, "%.1f m", altitude)

        // Update status
        statusText.text = getString(R.string.location_found)

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
                                addressText.text = address.getAddressLine(0)
                                    ?: getString(R.string.location_no_address)
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
                            addressText.text = address.getAddressLine(0)
                                ?: getString(R.string.location_no_address)
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
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}