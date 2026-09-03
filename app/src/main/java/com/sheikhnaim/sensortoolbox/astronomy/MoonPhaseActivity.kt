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

/**
 * MoonPhaseActivity - Shows the current moon phase
 *
 * HOW IT WORKS:
 * 1. Gets current location from GPS (for display only)
 * 2. Calculates the moon phase based on the DATE (not location!)
 * 3. Displays the phase with emoji, name, and illumination
 *
 * IMPORTANT: Moon phase is based on DATE, not location!
 * The moon phase is the same everywhere on Earth on the same day.
 * Location only affects the moon's rising/setting time, not the phase.
 *
 * MOON PHASE CALCULATION:
 * - Based on the lunar cycle (29.53 days)
 * - New Moon = 0, Full Moon = 0.5
 * - Uses a known new moon date as reference
 * - Result is consistent no matter how many times you refresh!
 */
class MoonPhaseActivity : AppCompatActivity() {

    // ============================================================
    // GPS LOCATION CLIENT - Gets location from GPS
    // ============================================================
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // ============================================================
    // UI VIEWS - References to the layout elements
    // ============================================================
    private lateinit var locationText: TextView        // Shows the current location
    private lateinit var moonEmojiText: TextView       // Shows the moon emoji
    private lateinit var moonPhaseNameText: TextView   // Shows the phase name
    private lateinit var illuminationText: TextView    // Shows illumination percentage
    private lateinit var nextFullMoonText: TextView    // Shows next full moon date
    private lateinit var refreshButton: Button         // Refresh button
    private lateinit var viewMapButton: Button        // View on map button
    private lateinit var dateText: TextView            // Shows current date

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    // ============================================================
    // PERMISSION HANDLING - Requests location permission from user
    // ============================================================
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getLocationAndUpdate()
            } else {
                Toast.makeText(this, R.string.permission_denied_toast, Toast.LENGTH_LONG).show()
                locationText.text = getString(R.string.permission_denied)
                // Still show moon phase without location
                updateMoonPhase(null)
            }
        }

    /**
     * onCreate - Called when the activity is first created
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_moon_phase)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.moon_phase_title)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // ============================================================
        // STEP 2: Find all UI views
        // ============================================================
        locationText = findViewById(R.id.locationText)
        moonEmojiText = findViewById(R.id.moonEmojiText)
        moonPhaseNameText = findViewById(R.id.moonPhaseNameText)
        illuminationText = findViewById(R.id.illuminationText)
        nextFullMoonText = findViewById(R.id.nextFullMoonText)
        refreshButton = findViewById(R.id.refreshButton)
        viewMapButton = findViewById(R.id.viewMapButton)
        dateText = findViewById(R.id.dateText)

        // ============================================================
        // STEP 3: Initialize the Fused Location Client
        // ============================================================
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ============================================================
        // STEP 4: Set up the Buttons
        // ============================================================
        refreshButton.setOnClickListener {
            checkPermissionAndUpdate()
        }

        viewMapButton.setOnClickListener {
            val intent = android.content.Intent(this, com.sheikhnaim.sensortoolbox.MapActivity::class.java)
            if (currentLatitude != null && currentLongitude != null) {
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LATITUDE, currentLatitude!!)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_LONGITUDE, currentLongitude!!)
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_TITLE, "🌙 Moon Phase: ${moonPhaseNameText.text}")
                intent.putExtra(com.sheikhnaim.sensortoolbox.MapActivity.EXTRA_SNIPPET, "${illuminationText.text} | ${nextFullMoonText.text}")
            }
            startActivity(intent)
        }

        // ============================================================
        // STEP 5: Initial load
        // ============================================================
        checkPermissionAndUpdate()
    }

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
            getLocationAndUpdate()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    /**
     * Gets the current device location and updates moon data.
     * Handles success, failure, and null location cases.
     * Always shows moon phase even if location is unavailable.
     */
    private fun getLocationAndUpdate() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Still show moon phase without location
            updateMoonPhase(null)
            return
        }

        locationText.text = getString(R.string.location_getting)

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    updateMoonPhase(location)
                } else {
                    locationText.text = getString(R.string.location_unavailable_short)
                    // Still show moon phase without location
                    updateMoonPhase(null)
                }
            }
            .addOnFailureListener { _ ->
                locationText.text = getString(R.string.location_error)
                // Still show moon phase without location
                updateMoonPhase(null)
            }
    }

    /**
     * updateMoonPhase - Updates the UI with moon phase data
     *
     * IMPORTANT: The moon phase is based on DATE, not location!
     * Location is only used for display purposes.
     * This means refreshing will ALWAYS give the same result on the same day.
     *
     * @param location Current device location (optional - used only for display)
     */
    private fun updateMoonPhase(location: Location?) {
        // ============================================================
        // STEP 1: Get the current date
        // The moon phase is based on DATE, not location!
        // ============================================================
        val calendar = Calendar.getInstance()
        val date = calendar.time

        // Display the current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.US)
        dateText.text = String.format(Locale.US, "📅 %s", dateFormat.format(date))

        // ============================================================
        // STEP 2: Calculate moon phase using the DATE
        // This calculation uses ONLY the date, not location!
        // Result is consistent every time you refresh on the same day.
        // ============================================================
        // Reference: New Moon on January 6, 2020 (known reference point)
        val knownNewMoon = Calendar.getInstance()
        knownNewMoon.set(2020, Calendar.JANUARY, 6, 0, 0, 0)
        val diff = date.time - knownNewMoon.timeInMillis
        val daysSinceNewMoon = diff / (1000.0 * 60 * 60 * 24)
        val lunarCycle = 29.53058867 // days
        val phase = (daysSinceNewMoon % lunarCycle) / lunarCycle

        // ============================================================
        // STEP 3: Display the location (if available)
        // Location doesn't affect the moon phase, but we show it anyway
        // ============================================================
        if (location != null) {
            val lat = location.latitude
            val lon = location.longitude
            locationText.text = String.format(Locale.US, "📍 %.4f, %.4f", lat, lon)
        } else {
            locationText.text = getString(R.string.location_unknown)
        }

        // ============================================================
        // STEP 4: Get and display moon phase info
        // The phase value is ALWAYS the same for the same date!
        // ============================================================
        val (emoji, name, illumination) = getMoonPhaseInfo(phase)
        moonEmojiText.text = emoji
        moonPhaseNameText.text = name
        illuminationText.text = String.format(
            Locale.US,
            getString(R.string.moon_phase_illumination_format),
            illumination
        )

        // ============================================================
        // STEP 5: Calculate and display next full moon
        // ============================================================
        val nextFullMoon = calculateNextFullMoon(date)
        val fullMoonFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        nextFullMoonText.text = String.format(
            Locale.US,
            getString(R.string.moon_phase_next_full_moon_format),
            fullMoonFormat.format(nextFullMoon)
        )
    }

    /**
     * getMoonPhaseInfo - Returns the moon phase based on the phase value
     *
     * @param phase Value from 0 to 1 (0 = New Moon, 0.5 = Full Moon)
     * @return Triple of (Emoji, Name, Illumination %)
     *
     * PHASE MAPPING:
     * 0.00 - 0.06: New Moon (🌑)
     * 0.06 - 0.19: Waxing Crescent (🌒)
     * 0.19 - 0.31: First Quarter (🌓)
     * 0.31 - 0.44: Waxing Gibbous (🌔)
     * 0.44 - 0.56: Full Moon (🌕)
     * 0.56 - 0.69: Waning Gibbous (🌖)
     * 0.69 - 0.81: Third Quarter (🌗)
     * 0.81 - 0.94: Waning Crescent (🌘)
     * 0.94 - 1.00: New Moon (🌑)
     */
    private fun getMoonPhaseInfo(phase: Double): Triple<String, String, Double> {
        val illumination = when {
            phase < 0.5 -> phase * 2 * 100
            else -> (1 - phase) * 2 * 100
        }

        return when (phase) {
            in 0.0..0.0625 -> Triple("🌑", "New Moon", illumination)
            in 0.0625..0.1875 -> Triple("🌒", "Waxing Crescent", illumination)
            in 0.1875..0.3125 -> Triple("🌓", "First Quarter", illumination)
            in 0.3125..0.4375 -> Triple("🌔", "Waxing Gibbous", illumination)
            in 0.4375..0.5625 -> Triple("🌕", "Full Moon", illumination)
            in 0.5625..0.6875 -> Triple("🌖", "Waning Gibbous", illumination)
            in 0.6875..0.8125 -> Triple("🌗", "Third Quarter", illumination)
            in 0.8125..0.9375 -> Triple("🌘", "Waning Crescent", illumination)
            else -> Triple("🌑", "New Moon", illumination)
        }
    }

    /**
     * calculateNextFullMoon - Calculates the date of the next full moon
     *
     * Uses a known full moon date (January 10, 2020) as reference.
     * The lunar cycle is 29.53058867 days.
     *
     * @param currentDate The current date
     * @return Date of the next full moon
     */
    private fun calculateNextFullMoon(currentDate: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = currentDate

        // Known Full Moon reference: January 10, 2020
        val knownFullMoon = Calendar.getInstance()
        knownFullMoon.set(2020, Calendar.JANUARY, 10, 0, 0, 0)

        val diff = currentDate.time - knownFullMoon.timeInMillis
        val daysSinceKnownFull = diff / (1000.0 * 60 * 60 * 24)
        val lunarCycle = 29.53058867

        val nextFullMoonDays = daysSinceKnownFull - (daysSinceKnownFull % lunarCycle) + lunarCycle
        val nextFullMoonMillis = knownFullMoon.timeInMillis + (nextFullMoonDays * 24 * 60 * 60 * 1000).toLong()

        return Date(nextFullMoonMillis)
    }

    // ============================================================
    // BACK BUTTON NAVIGATION
    // ============================================================

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}