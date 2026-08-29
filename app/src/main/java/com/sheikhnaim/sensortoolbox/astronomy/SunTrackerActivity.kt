package com.sheikhnaim.sensortoolbox.astronomy

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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sheikhnaim.sensortoolbox.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

/**
 * SunTrackerActivity displays sunrise/sunset times and current sun position
 * based on the device's current location.
 *
 * This activity uses simplified astronomical calculations to estimate:
 * - Sunrise and sunset times
 * - Day length
 * - Current sun altitude and azimuth
 *
 * Note: Uses approximate solar calculations suitable for most latitudes but may
 * have inaccuracies near polar regions or during equinoxes.
 *
 * @author Sheikh Naim
 * @since 1.0
 */
@Suppress("SpellCheckingInspection") // Suppress false positive "atan" typo warnings
class SunTrackerActivity : AppCompatActivity() {

    // ========== Constants ==========

    companion object {
        /** Maximum solar declination angle (degrees) - occurs at summer solstice */
        private const val SOLAR_DECLINATION_MAX = 23.44

        /** Solar disk angle (degrees) - accounts for the sun's apparent size and atmospheric refraction */
        private const val SOLAR_DISK_ANGLE = -0.83

        /** Minutes per degree of longitude (for time zone correction) */
        private const val MINUTES_PER_DEGREE = 4.0

        /** Total minutes in a day (24 hours × 60 minutes) */
        private const val MINUTES_PER_DAY = 1440

        /** Solar noon in minutes since midnight (12:00 PM) */
        private const val SOLAR_NOON_MINUTES = 720
    }

    // ========== UI Components ==========

    /** Displays the current location coordinates */
    private lateinit var locationText: TextView

    /** Displays the calculated sunrise time */
    private lateinit var sunriseText: TextView

    /** Displays the calculated sunset time */
    private lateinit var sunsetText: TextView

    /** Displays the total day length */
    private lateinit var dayLengthText: TextView

    /** Displays the current sun altitude and azimuth */
    private lateinit var sunPositionText: TextView

    /** Button to manually refresh the data */
    private lateinit var refreshButton: Button

    /** Button to reset the display and refresh */
    private lateinit var resetButton: Button

    // ========== Location Services ==========

    /** Google Play Services location client for getting device location */
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ========== Permission Handling ==========

    /**
     * Permission launcher for requesting location permissions.
     * Handles the result and triggers location update if granted.
     * Shows appropriate messages if permission is denied.
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                    // Permission granted - proceed with location update
                    getLocationAndUpdate()
                }
                else -> {
                    // Permission denied - show error message and update UI
                    Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
                    locationText.text = getString(R.string.permission_denied)
                }
            }
        }

    // ========== Lifecycle Methods ==========

    /**
     * Called when the activity is created.
     * Initializes all UI components, sets up listeners, and starts the location process.
     *
     * @param savedInstanceState Previously saved state, if any
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sun_tracker)

        setupToolbar()
        initializeViews()
        setupClickListeners()
        initializeLocationClient()

        // Start the main process
        checkPermissionAndUpdate()
    }

    // ========== Initialization Methods ==========

    /**
     * Sets up the toolbar with back navigation support
     */
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.sun_tracker_title)
    }

    /**
     * Initializes all view references
     */
    private fun initializeViews() {
        locationText = findViewById(R.id.locationText)
        sunriseText = findViewById(R.id.sunriseText)
        sunsetText = findViewById(R.id.sunsetText)
        dayLengthText = findViewById(R.id.dayLengthText)
        sunPositionText = findViewById(R.id.sunPositionText)
        refreshButton = findViewById(R.id.refreshButton)
        resetButton = findViewById(R.id.resetButton)
    }

    /**
     * Sets up button click listeners
     */
    private fun setupClickListeners() {
        refreshButton.setOnClickListener {
            // Manual refresh - check permission and update
            checkPermissionAndUpdate()
        }

        resetButton.setOnClickListener {
            // Reset display to default state and refresh
            resetDisplay()
        }
    }

    /**
     * Initializes the FusedLocationProviderClient for location services
     */
    private fun initializeLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // ========== Permission and Location Methods ==========

    /**
     * Checks for location permission and updates if granted.
     * If permission is not granted, requests it from the user.
     */
    private fun checkPermissionAndUpdate() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted - get location
            getLocationAndUpdate()
        } else {
            // Request permission
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    /**
     * Gets the current device location and updates sun data.
     * Handles success, failure, and null location cases.
     */
    private fun getLocationAndUpdate() {
        // Double-check permission (required for location API calls)
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Update UI to show loading state
        locationText.text = getString(R.string.location_getting)

        // Request current location with high accuracy
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null  // No cancellation token needed
        )
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    // Location obtained successfully - update all data
                    updateSunData(location)
                } else {
                    // Location is null (shouldn't happen with success)
                    locationText.text = getString(R.string.location_unavailable_short)
                    Toast.makeText(this, R.string.location_unavailable_toast, Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { _ ->
                // Location request failed - unused parameter replaced with underscore
                locationText.text = getString(R.string.location_error)
                Toast.makeText(
                    this,
                    R.string.location_error_toast,
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // ========== UI Update Methods ==========

    /**
     * Resets the display to default values and triggers a refresh.
     * Removes old data and shows loading state.
     */
    private fun resetDisplay() {
        // Reset all text views to default values
        locationText.text = getString(R.string.location_getting)
        sunriseText.text = getString(R.string.sun_tracker_sunrise_default)
        sunsetText.text = getString(R.string.sun_tracker_sunset_default)
        dayLengthText.text = getString(R.string.sun_tracker_day_length_default)
        sunPositionText.text = getString(R.string.sun_tracker_sun_position_default)

        // Show feedback to user
        Toast.makeText(this, R.string.button_refresh, Toast.LENGTH_SHORT).show()

        // Trigger a fresh data update
        // Note: The permission check is handled inside checkPermissionAndUpdate()
        checkPermissionAndUpdate()
    }

    /**
     * Updates all sun-related data based on the given location.
     * This is the main orchestration method for data updates.
     *
     * @param location Current device location with latitude and longitude
     */
    private fun updateSunData(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        // Display location coordinates with 4 decimal places
        locationText.text = String.format(Locale.US, "📍 %.4f, %.4f", lat, lon)

        // Get current date and time
        val calendar = Calendar.getInstance()
        val date = calendar.time

        try {
            // Calculate all sun-related data
            val (sunrise, sunset, dayLength) = calculateSunTimes(lat, lon, date)
            val (altitude, azimuth) = calculateSunPosition(lat, lon, date)

            // Update UI with calculated values
            updateTimeDisplay(sunrise, sunset, dayLength)
            updatePositionDisplay(altitude, azimuth)
        } catch (_: Exception) {
            // Handle calculation errors gracefully - unused exception replaced with underscore
            Toast.makeText(this, R.string.location_error_toast, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Updates the time-related UI elements with sunrise, sunset, and day length.
     *
     * @param sunrise The calculated sunrise time
     * @param sunset The calculated sunset time
     * @param dayLength The total day length in minutes
     */
    private fun updateTimeDisplay(sunrise: Date, sunset: Date, dayLength: Int) {
        // Format times in 12-hour format with AM/PM
        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        sunriseText.text = timeFormat.format(sunrise)
        sunsetText.text = timeFormat.format(sunset)

        // Convert day length to hours and minutes
        val hours = dayLength / 60
        val minutes = dayLength % 60
        dayLengthText.text = String.format(Locale.US, "%dh %dm", hours, minutes)
    }

    /**
     * Updates the sun position UI elements with altitude and azimuth.
     *
     * @param altitude The sun's altitude in degrees
     * @param azimuth The sun's azimuth in degrees (measured from North clockwise)
     */
    private fun updatePositionDisplay(altitude: Double, azimuth: Double) {
        val direction = getCardinalDirection(azimuth)
        sunPositionText.text = String.format(
            Locale.US,
            "Altitude: %.0f° | Azimuth: %.0f° (%s)",
            altitude, azimuth, direction
        )
    }

    // ========== Astronomical Calculation Methods ==========

    /**
     * Calculates sunrise, sunset times and day length using an approximate algorithm.
     *
     * This is a simplified calculation that:
     * - Uses a constant solar declination formula
     * - Accounts for the solar disk angle (-0.83°)
     * - Applies longitude correction for time zones
     * - Does NOT account for atmospheric refraction or equation of time
     *
     * Note: This algorithm may produce inaccuracies near polar regions, during equinoxes,
     * or at extreme latitudes. For production use, consider using a more accurate library.
     *
     * @param lat Latitude in degrees (positive for North, negative for South)
     * @param lon Longitude in degrees (positive for East, negative for West)
     * @param date Current date (used for solar declination calculation)
     * @return Triple containing:
     *         - sunrise Date object
     *         - sunset Date object
     *         - day length in minutes
     */
    private fun calculateSunTimes(lat: Double, lon: Double, date: Date): Triple<Date, Date, Int> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        // Calculate approximate solar declination
        // Formula: 23.44° * sin(360/365 * (dayOfYear - 81))
        // The -81 offset aligns with the spring equinox (March 21)
        val declination = SOLAR_DECLINATION_MAX * sin(
            Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))
        )

        // Convert to radians for trigonometric calculations
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)

        // Calculate the hour angle at sunrise/sunset
        // Formula: cos(HA) = (sin(-0.83°) - sin(lat)*sin(dec)) / (cos(lat)*cos(dec))
        val cosHA = (sin(Math.toRadians(SOLAR_DISK_ANGLE)) - sin(latRad) * sin(decRad)) /
                (cos(latRad) * cos(decRad))

        // Handle polar region cases
        when {
            cosHA < -1.0 -> {
                // Sun never rises (polar night)
                Toast.makeText(this, "Sun does not rise today", Toast.LENGTH_LONG).show()
                return Triple(date, date, 0)
            }
            cosHA > 1.0 -> {
                // Sun never sets (midnight sun)
                Toast.makeText(this, "Sun does not set today", Toast.LENGTH_LONG).show()
                return Triple(date, date, MINUTES_PER_DAY)
            }
        }

        // Calculate the hour angle in degrees
        val ha = Math.toDegrees(acos(cosHA.coerceIn(-1.0, 1.0)))

        // Calculate sunrise and sunset times in minutes since midnight
        // Sunrise = 12:00 - HA/15 (in hours) converted to minutes
        // Sunset = 12:00 + HA/15 (in hours) converted to minutes
        // Longitude correction: 4 minutes per degree East/West
        val sunriseMinutes = SOLAR_NOON_MINUTES - ha * MINUTES_PER_DEGREE - lon * MINUTES_PER_DEGREE
        val sunsetMinutes = SOLAR_NOON_MINUTES + ha * MINUTES_PER_DEGREE - lon * MINUTES_PER_DEGREE

        // Create Date objects for sunrise and sunset
        val sunriseCalendar = Calendar.getInstance().apply {
            set(year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, sunriseMinutes.toInt())
        }

        val sunsetCalendar = Calendar.getInstance().apply {
            set(year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, sunsetMinutes.toInt())
        }

        // Calculate day length in minutes
        val dayLengthMinutes = (sunsetMinutes - sunriseMinutes).toInt()

        return Triple(sunriseCalendar.time, sunsetCalendar.time, dayLengthMinutes)
    }

    /**
     * Calculates the current sun altitude and azimuth for the given location and time.
     *
     * The altitude is the angle of the sun above the horizon (0° at horizon, 90° at zenith).
     * The azimuth is the compass direction of the sun (0° = North, 90° = East, etc.)
     *
     * Formula references:
     * - Altitude: sin(alt) = sin(lat)*sin(dec) + cos(lat)*cos(dec)*cos(HA)
     * - Azimuth: atan2(-sin(HA), tan(dec)*cos(lat) - sin(lat)*cos(HA))
     *
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param date Current date and time
     * @return Pair containing:
     *         - altitude in degrees (ranges from -90° to 90°)
     *         - azimuth in degrees (ranges from 0° to 360°)
     */
    private fun calculateSunPosition(lat: Double, lon: Double, date: Date): Pair<Double, Double> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        // Calculate time in minutes since midnight (with fractional seconds)
        val timeInMinutes = hour * 60.0 + minute + second / 60.0

        // Calculate approximate solar declination (same as above)
        val declination = SOLAR_DECLINATION_MAX * sin(
            Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))
        )

        // Calculate hour angle: (time since solar noon) * 15° per hour
        // Solar noon is at 12:00 (720 minutes)
        // Longitude correction: subtract longitude (4 minutes per degree)
        val ha = (timeInMinutes - SOLAR_NOON_MINUTES) / MINUTES_PER_DEGREE - lon

        // Convert to radians
        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)
        val haRad = Math.toRadians(ha)

        // Calculate solar altitude (angle above horizon)
        // Formula: sin(alt) = sin(lat)*sin(dec) + cos(lat)*cos(dec)*cos(HA)
        val altitudeRad = asin(
            sin(latRad) * sin(decRad) +
                    cos(latRad) * cos(decRad) * cos(haRad)
        )
        val altitude = Math.toDegrees(altitudeRad)

        // Calculate solar azimuth (compass direction)
        // Formula: azimuth = atan2(-sin(HA), tan(dec)*cos(lat) - sin(lat)*cos(HA))
        val azimuthRad = atan2(
            -sin(haRad),
            tan(decRad) * cos(latRad) - sin(latRad) * cos(haRad)
        )

        // Convert to degrees and normalize to 0-360°
        // Add 360 and modulo to ensure positive values
        val azimuth = (Math.toDegrees(azimuthRad) + 360) % 360

        return Pair(altitude, azimuth)
    }

    /**
     * Converts an azimuth angle to a cardinal direction string.
     * Divides the compass into 8 primary directions (N, NE, E, SE, S, SW, W, NW).
     *
     * @param azimuth Angle in degrees measured from North clockwise (0° = North)
     * @return String representing the cardinal direction (e.g., "N", "NE", "E")
     */
    private fun getCardinalDirection(azimuth: Double): String {
        return when (azimuth) {
            // North (337.5° - 360° and 0° - 22.5°)
            in 337.5..360.0, in 0.0..22.5 -> "N"
            // Northeast (22.5° - 67.5°)
            in 22.5..67.5 -> "NE"
            // East (67.5° - 112.5°)
            in 67.5..112.5 -> "E"
            // Southeast (112.5° - 157.5°)
            in 112.5..157.5 -> "SE"
            // South (157.5° - 202.5°)
            in 157.5..202.5 -> "S"
            // Southwest (202.5° - 247.5°)
            in 202.5..247.5 -> "SW"
            // West (247.5° - 292.5°)
            in 247.5..292.5 -> "W"
            // Northwest (292.5° - 337.5°)
            in 292.5..337.5 -> "NW"
            // Fallback for any edge cases
            else -> "N/A"
        }
    }

    // ========== Navigation Methods ==========

    /**
     * Handles the up navigation button in the toolbar.
     * Navigates back to the previous activity.
     *
     * @return true if navigation was handled
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}