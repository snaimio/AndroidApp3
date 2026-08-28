package com.sheikhnaim.sensortoolbox.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
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
import com.sheikhnaim.sensortoolbox.R
import java.util.Locale

/**
 * LocationActivity - GPS Location Tool
 *
 * HOW THIS WORKS:
 * 1. Uses FusedLocationProviderClient to get high-accuracy GPS location
 * 2. Displays Latitude, Longitude, and Altitude
 * 3. Uses Geocoder to convert coordinates to a human-readable address
 * 4. Handles location permissions properly
 *
 * This is the SAME technique used in the LocationFinder tutorial!
 */
class LocationActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT - Gets location from GPS
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS
    // ============================================================
    private lateinit var latitudeText: TextView
    private lateinit var longitudeText: TextView
    private lateinit var altitudeText: TextView
    private lateinit var addressText: TextView
    private lateinit var statusText: TextView
    private lateinit var getLocationButton: Button

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (fineLocationGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
                statusText.text = "⚠️ Permission denied"
            }
        }

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        latitudeText = findViewById(R.id.latitudeText)
        longitudeText = findViewById(R.id.longitudeText)
        altitudeText = findViewById(R.id.altitudeText)
        addressText = findViewById(R.id.addressText)
        statusText = findViewById(R.id.statusText)
        getLocationButton = findViewById(R.id.getLocationButton)

        // ============================================================
        // STEP 3: Initialize the Fused Location Client
        // ============================================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ============================================================
        // STEP 4: Set up the Get Location button
        // ============================================================
        getLocationButton.setOnClickListener {
            checkLocationPermission()
        }
    }

    // ============================================================
    // PERMISSION HANDLING
    // ============================================================

    /**
     * checkLocationPermission - Checks if we have permission, requests if not
     */
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
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
     * 1. Checks if we have permission
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

        statusText.text = "📍 Getting location..."
        addressText.text = "Searching..."

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    displayLocation(location)
                } else {
                    latitudeText.text = "--.------"
                    longitudeText.text = "--.------"
                    altitudeText.text = "--.- m"
                    addressText.text = "Unable to determine location"
                    statusText.text = "⚠️ Location unavailable"
                    Toast.makeText(this, "Could not get location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                latitudeText.text = "--.------"
                longitudeText.text = "--.------"
                altitudeText.text = "--.- m"
                addressText.text = "Error: ${exception.message}"
                statusText.text = "⚠️ Error getting location"
                Toast.makeText(this, "Location error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // ============================================================
    // DISPLAY LOCATION
    // ============================================================

    /**
     * displayLocation - Updates the UI with the current location
     *
     * @param location The Location object from GPS
     */
    private fun displayLocation(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        val altitude = location.altitude

        // Format and display coordinates
        latitudeText.text = String.format(Locale.US, "%.6f", latitude)
        longitudeText.text = String.format(Locale.US, "%.6f", longitude)

        // Display altitude (if available)
        altitudeText.text = if (altitude > 0) {
            String.format(Locale.US, "%.1f m", altitude)
        } else {
            "N/A"
        }

        statusText.text = "✅ Location found!"

        // Get the address from coordinates
        getAddress(latitude, longitude)
    }

    // ============================================================
    // REVERSE GEOCODING - Convert coordinates to address
    // ============================================================

    /**
     * getAddress - Converts latitude/longitude to a human-readable address
     *
     * This uses the Geocoder API, which is the SAME technique used
     * in the LocationFinder tutorial!
     *
     * @param latitude The latitude
     * @param longitude The longitude
     */
    private fun getAddress(latitude: Double, longitude: Double) {
        // Check if Geocoder is available
        if (!Geocoder.isPresent()) {
            addressText.text = "Address lookup not available"
            return
        }

        val geocoder = Geocoder(this, Locale.getDefault())

        // Android 13+ (API 33+) - Use asynchronous API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        runOnUiThread {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                addressText.text = address.getAddressLine(0) ?: "Address not found"
                            } else {
                                addressText.text = "No address found"
                            }
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        runOnUiThread {
                            addressText.text = "Unable to determine address"
                        }
                    }
                }
            )
        } else {
            // Android 12 and below - Use synchronous API in background thread
            Thread {
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    runOnUiThread {
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            addressText.text = address.getAddressLine(0) ?: "Address not found"
                        } else {
                            addressText.text = "No address found"
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        addressText.text = "Unable to determine address"
                    }
                }
            }.start()
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