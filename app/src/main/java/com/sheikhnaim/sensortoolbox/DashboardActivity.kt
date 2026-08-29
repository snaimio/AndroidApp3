package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS - Only the ones needed
// ============================================================
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sheikhnaim.sensortoolbox.R
import com.sheikhnaim.sensortoolbox.data.ToolAdapter
import com.sheikhnaim.sensortoolbox.data.ToolItem
import com.sheikhnaim.sensortoolbox.detection.MetalDetectorActivity
import com.sheikhnaim.sensortoolbox.navigation.CompassActivity
import com.sheikhnaim.sensortoolbox.location.LocationActivity
import com.sheikhnaim.sensortoolbox.motion.BubbleLevelActivity
import com.sheikhnaim.sensortoolbox.motion.GravityMeterActivity
import com.sheikhnaim.sensortoolbox.motion.ShakeDetectorActivity
import com.sheikhnaim.sensortoolbox.motion.SpaceBallActivity
import com.sheikhnaim.sensortoolbox.speed.SpeedometerActivity
import com.sheikhnaim.sensortoolbox.speed.DistanceTrackerActivity
import com.sheikhnaim.sensortoolbox.speed.AltimeterActivity
import com.sheikhnaim.sensortoolbox.astronomy.MoonPhaseActivity
import com.sheikhnaim.sensortoolbox.astronomy.SunTrackerActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var gpsStatusText: TextView
    private lateinit var sensorStatusText: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        gpsStatusText = findViewById(R.id.gpsStatusText)
        sensorStatusText = findViewById(R.id.sensorStatusText)
        recyclerView = findViewById(R.id.toolRecyclerView)

        updateStatus()
        setupRecyclerView()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateStatus() {
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        gpsStatusText.text = if (hasLocationPermission) {
            getString(R.string.gps_locked)
        } else {
            getString(R.string.gps_permission_needed)
        }

        sensorStatusText.text = getString(R.string.sensors_active)
    }

    private fun setupRecyclerView() {
        val tools = listOf(
            // Category 1: Location & Navigation
            ToolItem(
                iconRes = R.string.icon_compass,
                titleRes = R.string.category_location,
                descRes = R.string.desc_compass,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),
            ToolItem(
                iconRes = R.string.icon_compass,
                titleRes = R.string.tool_compass,
                descRes = R.string.desc_compass,
                activityClass = CompassActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_gps,
                titleRes = R.string.tool_gps,
                descRes = R.string.desc_gps,
                activityClass = LocationActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // Category 2: Speed & Distance
            ToolItem(
                iconRes = R.string.icon_speedometer,
                titleRes = R.string.category_speed,
                descRes = R.string.desc_speedometer,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),
            ToolItem(
                iconRes = R.string.icon_speedometer,
                titleRes = R.string.tool_speedometer,
                descRes = R.string.desc_speedometer,
                activityClass = SpeedometerActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_distance,
                titleRes = R.string.tool_distance_tracker,
                descRes = R.string.desc_distance_tracker,
                activityClass = DistanceTrackerActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_altimeter,
                titleRes = R.string.tool_altimeter,
                descRes = R.string.desc_altimeter,
                activityClass = AltimeterActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // Category 3: Motion & Sensors
            ToolItem(
                iconRes = R.string.icon_gravity,
                titleRes = R.string.category_motion,
                descRes = R.string.desc_gravity_meter,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),
            ToolItem(
                iconRes = R.string.icon_gravity,
                titleRes = R.string.tool_gravity_meter,
                descRes = R.string.desc_gravity_meter,
                activityClass = GravityMeterActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_bubble,
                titleRes = R.string.tool_bubble_level,
                descRes = R.string.desc_bubble_level,
                activityClass = BubbleLevelActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_shake,
                titleRes = R.string.tool_shake_detector,
                descRes = R.string.desc_shake_detector,
                activityClass = ShakeDetectorActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_space,
                titleRes = R.string.tool_space_ball,
                descRes = R.string.desc_space_ball,
                activityClass = SpaceBallActivity::class.java,
                buttonTextRes = R.string.button_play
            ),

            // Category 4: Detection
            ToolItem(
                iconRes = R.string.icon_metal,
                titleRes = R.string.category_detection,
                descRes = R.string.desc_metal_detector,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),
            ToolItem(
                iconRes = R.string.icon_metal,
                titleRes = R.string.tool_metal_detector,
                descRes = R.string.desc_metal_detector,
                activityClass = MetalDetectorActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // Category 5: Astronomy
            ToolItem(
                iconRes = R.string.icon_moon,
                titleRes = R.string.category_astronomy,
                descRes = R.string.desc_moon_phase,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),
            ToolItem(
                iconRes = R.string.icon_moon,
                titleRes = R.string.tool_moon_phase,
                descRes = R.string.desc_moon_phase,
                activityClass = MoonPhaseActivity::class.java,
                buttonTextRes = R.string.button_open
            ),
            ToolItem(
                iconRes = R.string.icon_sun,
                titleRes = R.string.tool_sun_tracker,
                descRes = R.string.desc_sun_tracker,
                activityClass = SunTrackerActivity::class.java,
                buttonTextRes = R.string.button_open
            )
        )

        val adapter = ToolAdapter(this, tools)
        val layoutManager = GridLayoutManager(this, 1)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
    }
}