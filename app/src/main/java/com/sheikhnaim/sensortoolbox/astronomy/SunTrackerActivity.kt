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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.asin
import kotlin.math.acos

class SunTrackerActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var locationText: TextView
    private lateinit var sunriseText: TextView
    private lateinit var sunsetText: TextView
    private lateinit var dayLengthText: TextView
    private lateinit var sunPositionText: TextView
    private lateinit var refreshButton: Button
    private lateinit var resetButton: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getLocationAndUpdate()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
                locationText.text = "⚠️ Permission denied"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sun_tracker)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        locationText = findViewById(R.id.locationText)
        sunriseText = findViewById(R.id.sunriseText)
        sunsetText = findViewById(R.id.sunsetText)
        dayLengthText = findViewById(R.id.dayLengthText)
        sunPositionText = findViewById(R.id.sunPositionText)
        refreshButton = findViewById(R.id.refreshButton)
        resetButton = findViewById(R.id.resetButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        refreshButton.setOnClickListener {
            checkPermissionAndUpdate()
        }

        resetButton.setOnClickListener {
            locationText.text = "📍 Refreshing..."
            sunriseText.text = "--:-- --"
            sunsetText.text = "--:-- --"
            dayLengthText.text = "--h --m"
            sunPositionText.text = "Altitude: --° | Direction: --"
            checkPermissionAndUpdate()
            Toast.makeText(this, "🔄 Reset", Toast.LENGTH_SHORT).show()
        }

        checkPermissionAndUpdate()
    }

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

    private fun getLocationAndUpdate() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        locationText.text = "📍 Getting location..."

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    updateSunTimes(location)
                } else {
                    locationText.text = "📍 Location unavailable"
                }
            }
            .addOnFailureListener {
                locationText.text = "📍 Error getting location"
            }
    }

    private fun updateSunTimes(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        locationText.text = String.format("📍 %.4f, %.4f", lat, lon)

        val calendar = Calendar.getInstance()
        val date = calendar.time

        val (sunrise, sunset, dayLength) = calculateSunTimes(lat, lon, date)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.US)
        sunriseText.text = timeFormat.format(sunrise)
        sunsetText.text = timeFormat.format(sunset)

        val hours = dayLength / 60
        val minutes = dayLength % 60
        dayLengthText.text = String.format("%dh %dm", hours, minutes)

        val (altitude, azimuth) = calculateSunPosition(lat, lon, date)
        val direction = getDirection(azimuth)
        sunPositionText.text = String.format("Altitude: %.0f° | Direction: %s", altitude, direction)
    }

    private fun calculateSunTimes(lat: Double, lon: Double, date: Date): Triple<Date, Date, Int> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        val declination = 23.44 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))

        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)
        val cosHA = (sin(Math.toRadians(-0.83)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))

        val ha = Math.toDegrees(acos(cosHA))

        val sunriseMinutes = 720 - ha * 4 - lon + 60
        val sunsetMinutes = 720 + ha * 4 - lon + 60

        val sunriseCalendar = Calendar.getInstance()
        sunriseCalendar.set(year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        sunriseCalendar.set(Calendar.HOUR_OF_DAY, 0)
        sunriseCalendar.set(Calendar.MINUTE, 0)
        sunriseCalendar.add(Calendar.MINUTE, sunriseMinutes.toInt())

        val sunsetCalendar = Calendar.getInstance()
        sunsetCalendar.set(year, calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
        sunsetCalendar.set(Calendar.HOUR_OF_DAY, 0)
        sunsetCalendar.set(Calendar.MINUTE, 0)
        sunsetCalendar.add(Calendar.MINUTE, sunsetMinutes.toInt())

        val dayLengthMinutes = (sunsetMinutes - sunriseMinutes).toInt()

        return Triple(sunriseCalendar.time, sunsetCalendar.time, dayLengthMinutes)
    }

    private fun calculateSunPosition(lat: Double, lon: Double, date: Date): Pair<Double, Double> {
        val calendar = Calendar.getInstance()
        calendar.time = date

        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeInMinutes = hour * 60 + minute

        val declination = 23.44 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81)))
        val ha = (timeInMinutes - 720) / 4.0 + lon

        val latRad = Math.toRadians(lat)
        val decRad = Math.toRadians(declination)
        val haRad = Math.toRadians(ha)

        val altitudeRad = asin(
            sin(latRad) * sin(decRad) +
                    cos(latRad) * cos(decRad) * cos(haRad)
        )
        val altitude = Math.toDegrees(altitudeRad)

        val azimuthRad = atan2(
            -sin(haRad),
            tan(decRad) * cos(latRad) - sin(latRad) * cos(haRad)
        )
        val azimuth = (Math.toDegrees(azimuthRad) + 360) % 360

        return Pair(altitude, azimuth)
    }

    private fun getDirection(azimuth: Double): String {
        return when (azimuth) {
            in 337.5..360.0, in 0.0..22.5 -> "North"
            in 22.5..67.5 -> "Northeast"
            in 67.5..112.5 -> "East"
            in 112.5..157.5 -> "Southeast"
            in 157.5..202.5 -> "South"
            in 202.5..247.5 -> "Southwest"
            in 247.5..292.5 -> "West"
            in 292.5..337.5 -> "Northwest"
            else -> "North"
        }
    }

    private fun acos(value: Double): Double {
        return Math.acos(value.coerceIn(-1.0, 1.0))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}