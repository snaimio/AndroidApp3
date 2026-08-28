package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================
import android.Manifest                              // For location permission
import android.content.pm.PackageManager            // To check if permission is granted
import android.os.Bundle                            // For saving/restoring state
import android.view.View                            // For the dot views
import android.widget.TextView                      // To display text on screen
import androidx.appcompat.app.AppCompatActivity      // Base class for our activity
import androidx.appcompat.widget.Toolbar            // The top bar with the app name
import androidx.core.content.ContextCompat          // For checking permissions safely
import androidx.core.view.ViewCompat                // For edge-to-edge display
import androidx.core.view.WindowInsetsCompat        // For handling system bars
import androidx.recyclerview.widget.GridLayoutManager // Arranges tools in a grid
import androidx.recyclerview.widget.RecyclerView     // Efficient list view
import com.sheikhnaim.sensortoolbox.R              // Resource IDs (layouts, strings, etc.)
import com.sheikhnaim.sensortoolbox.data.ToolAdapter // Connects data to RecyclerView
import com.sheikhnaim.sensortoolbox.data.ToolItem   // Data class for each tool
import com.sheikhnaim.sensortoolbox.detection.MetalDetectorActivity  // Tool: Metal Detector
import com.sheikhnaim.sensortoolbox.detection.EMFMeterActivity       // Tool: EMF Meter
import com.sheikhnaim.sensortoolbox.navigation.CompassActivity        // Tool: Digital Compass
import com.sheikhnaim.sensortoolbox.location.LocationActivity         // Tool: GPS Location
import com.sheikhnaim.sensortoolbox.location.TrailTrackerActivity     // Tool: Trail Tracker
import com.sheikhnaim.sensortoolbox.motion.BubbleLevelActivity       // Tool: Bubble Level
import com.sheikhnaim.sensortoolbox.motion.GravityMeterActivity      // Tool: Gravity Meter
import com.sheikhnaim.sensortoolbox.motion.ShakeDetectorActivity     // Tool: Shake Detector
import com.sheikhnaim.sensortoolbox.motion.SpaceBallActivity         // Tool: Space Ball
import com.sheikhnaim.sensortoolbox.speed.SpeedometerActivity        // Tool: Speedometer
import com.sheikhnaim.sensortoolbox.speed.DistanceTrackerActivity    // Tool: Distance Tracker
import com.sheikhnaim.sensortoolbox.speed.AltimeterActivity          // Tool: Altimeter
import com.sheikhnaim.sensortoolbox.astronomy.MoonPhaseActivity      // Tool: Moon Phase
import com.sheikhnaim.sensortoolbox.astronomy.SunTrackerActivity     // Tool: Sun Tracker

/**
 * DashboardActivity - Main screen for Sensor ToolBox
 *
 * This is the HOME SCREEN of the app. It displays all available tools
 * in a grid layout using a RecyclerView for performance.
 *
 * ============================================================
 * TOOLS INCLUDED (14 total):
 * ============================================================
 * LOCATION & NAVIGATION (3 tools):
 *   1. Digital Compass - Uses Accelerometer + Magnetometer (Sensor Fusion)
 *   2. GPS Location - Uses GPS
 *   3. Trail Tracker - Uses GPS
 *
 * SPEED & DISTANCE (3 tools):
 *   4. Speedometer - Uses GPS
 *   5. Distance Tracker - Uses GPS
 *   6. Altimeter - Uses GPS
 *
 * MOTION & SENSORS (4 tools):
 *   7. Gravity Meter - Uses Accelerometer
 *   8. Bubble Level - Uses Accelerometer
 *   9. Shake Detector - Uses Accelerometer
 *   10. Space Ball - Uses Accelerometer (Game)
 *
 * DETECTION (2 tools):
 *   11. Metal Detector - Uses Magnetometer
 *   12. EMF Meter - Uses Magnetometer
 *
 * ASTRONOMY (2 tools):
 *   13. Moon Phase - Uses GPS + Math
 *   14. Sun Tracker - Uses GPS + Math
 *
 * ============================================================
 * STATUS BAR - Clean design with colored dots (NO EMOJIS!)
 * ============================================================
 * - GPS Status: Shows "GPS: Locked" with a green dot
 * - Sensors Status: Shows "Sensors: Active" with a green dot
 * - No more green checkboxes (✅) - they looked unprofessional!
 */
class DashboardActivity : AppCompatActivity() {

    // ============================================================
    // PROPERTIES - Variables that hold references to UI elements
    // ============================================================
    private lateinit var gpsStatusText: TextView      // Shows GPS status in bottom bar
    private lateinit var sensorStatusText: TextView   // Shows sensor status in bottom bar
    private lateinit var gpsDot: View                // Green/red dot for GPS status
    private lateinit var sensorDot: View             // Green/red dot for sensors status
    private lateinit var recyclerView: RecyclerView   // The grid of tools

    /**
     * onCreate - Called when the activity is first created
     *
     * This is the ENTRY POINT of the activity. It sets up everything:
     * - The layout (UI)
     * - The toolbar (top bar)
     * - The status bar (bottom bar with colored dots)
     * - The RecyclerView (grid of tools)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ============================================================
        // STEP 1: SET UP THE TOOLBAR
        // The toolbar is the top bar with the app name
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)  // Make it the action bar for this activity

        // ============================================================
        // STEP 2: FIND ALL UI VIEWS
        // Find views by their ID (defined in activity_dashboard.xml)
        // ============================================================
        gpsStatusText = findViewById(R.id.gpsStatusText)
        sensorStatusText = findViewById(R.id.sensorStatusText)
        gpsDot = findViewById(R.id.gpsDot)           // NEW: GPS status dot
        sensorDot = findViewById(R.id.sensorDot)     // NEW: Sensors status dot
        recyclerView = findViewById(R.id.toolRecyclerView)

        // ============================================================
        // STEP 3: UPDATE THE STATUS BAR
        // Check if we have GPS permission and update the status text and dot colors
        // ============================================================
        updateStatus()

        // ============================================================
        // STEP 4: SET UP THE RECYCLERVIEW
        // Create the list of tools and display them in a 3-column grid
        // ============================================================
        setupRecyclerView()

        // ============================================================
        // STEP 5: HANDLE WINDOW INSETS
        // Add padding so content doesn't overlap with system bars
        // ============================================================
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            // Get the system bars (status bar, navigation bar)
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Add padding so content doesn't overlap with system bars
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /**
     * updateStatus - Updates the status bar at the bottom of the screen
     *
     * WHAT IT DOES:
     * 1. Checks if the app has location permission
     * 2. Updates GPS status text and dot color
     * 3. Updates sensors status text and dot color
     *
     * WHY DOTS INSTEAD OF EMOJIS?
     * - Colored dots are more professional and clean
     * - Green dot = active/working
     * - Red dot = error/permission needed
     * - No more green checkboxes (✅) that look unprofessional!
     */
    private fun updateStatus() {
        // ============================================================
        // STEP 1: Check if we have GPS permission
        // ============================================================
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,                                           // The current activity
            Manifest.permission.ACCESS_FINE_LOCATION        // The permission we're checking
        ) == PackageManager.PERMISSION_GRANTED              // Compare to "granted"

        // ============================================================
        // STEP 2: UPDATE GPS STATUS
        // - Text: "GPS: Locked" or "GPS: Permission needed"
        // - Dot: Green if locked, Red if permission needed
        // ============================================================
        gpsStatusText.text = if (hasLocationPermission) {
            // If we have permission, show "Locked"
            getString(R.string.gps_locked)
        } else {
            // If we don't have permission, show "Permission needed"
            getString(R.string.gps_permission_needed)
        }

        // Update GPS dot color based on permission status
        gpsDot.setBackgroundResource(
            if (hasLocationPermission) {
                R.drawable.status_dot_green      // Green = working
            } else {
                R.drawable.status_dot_red        // Red = permission needed
            }
        )

        // ============================================================
        // STEP 3: UPDATE SENSORS STATUS
        // - Text: "Sensors: Active"
        // - Dot: Green (sensors are always active on most devices)
        // ============================================================
        sensorStatusText.text = getString(R.string.sensors_active)
        sensorDot.setBackgroundResource(R.drawable.status_dot_green)  // Always green
    }

    /**
     * setupRecyclerView - Creates the list of tools and displays them
     *
     * WHAT IT DOES:
     * 1. Creates a list of ToolItem objects (each tool is one item)
     * 2. Categories are also ToolItems with isCategory = true
     * 3. Creates an Adapter to connect the data to the RecyclerView
     * 4. Sets up a GridLayoutManager with 3 columns
     * 5. Categories span 3 columns (full width), tools span 1 column
     *
     * WHY 3 COLUMNS?
     * - We have 14 tools now, so 2 columns makes the list too long
     * - 3 columns fits all tools better on the screen
     * - Categories still span the full width for better readability
     */
    private fun setupRecyclerView() {
        // ============================================================
        // CREATE THE LIST OF TOOLS
        // Each tool is a ToolItem with:
        // - iconRes: The emoji resource ID (from strings.xml)
        // - titleRes: The tool name resource ID
        // - descRes: The tool description resource ID
        // - activityClass: The activity to open when clicked
        // - buttonTextRes: The button text (OPEN or PLAY)
        // - isCategory: True for category headers (span full width)
        // ============================================================
        val tools = listOf(
            // ============================================================
            // CATEGORY 1: LOCATION & NAVIGATION
            // This is a CATEGORY HEADER (isCategory = true)
            // It spans the full width of the grid
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_compass,           // The icon for this category
                titleRes = R.string.category_location,      // "📍 LOCATION & NAVIGATION"
                descRes = R.string.desc_compass,           // Description (not used for categories)
                activityClass = null,                      // No activity for categories
                buttonTextRes = R.string.button_open,      // Not used for categories
                isCategory = true                          // ✅ This is a category!
            ),

            // ============================================================
            // TOOL 1: Digital Compass
            // Uses: Accelerometer + Magnetometer (Sensor Fusion)
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_compass,           // 🧭
                titleRes = R.string.tool_compass,          // "Digital Compass"
                descRes = R.string.desc_compass,           // "Find Direction"
                activityClass = CompassActivity::class.java, // Opens CompassActivity
                buttonTextRes = R.string.button_open       // "OPEN"
            ),

            // ============================================================
            // TOOL 2: GPS Location
            // Uses: GPS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_gps,               // 📍
                titleRes = R.string.tool_gps,              // "GPS Location"
                descRes = R.string.desc_gps,               // "Coordinates & Address"
                activityClass = LocationActivity::class.java, // Opens LocationActivity
                buttonTextRes = R.string.button_open       // "OPEN"
            ),

            // ============================================================
            // TOOL 3: Trail Tracker
            // Uses: GPS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_trail,             // 🥾
                titleRes = R.string.tool_trail_tracker,    // "Trail Tracker"
                descRes = R.string.desc_trail_tracker,     // "Record Hikes"
                activityClass = TrailTrackerActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // CATEGORY 2: SPEED & DISTANCE
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_speedometer,
                titleRes = R.string.category_speed,
                descRes = R.string.desc_speedometer,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),

            // ============================================================
            // TOOL 4: Speedometer
            // Uses: GPS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_speedometer,       // 🏎️
                titleRes = R.string.tool_speedometer,      // "Speedometer"
                descRes = R.string.desc_speedometer,       // "Check Speed"
                activityClass = SpeedometerActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 5: Distance Tracker
            // Uses: GPS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_distance,          // 📏
                titleRes = R.string.tool_distance_tracker, // "Distance Tracker"
                descRes = R.string.desc_distance_tracker,  // "Track Distance"
                activityClass = DistanceTrackerActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 6: Altimeter
            // Uses: GPS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_altimeter,         // ⛰️
                titleRes = R.string.tool_altimeter,        // "Altimeter"
                descRes = R.string.desc_altimeter,         // "Check Altitude"
                activityClass = AltimeterActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // CATEGORY 3: MOTION & SENSORS
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_gravity,
                titleRes = R.string.category_motion,
                descRes = R.string.desc_gravity_meter,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),

            // ============================================================
            // TOOL 7: Gravity Meter
            // Uses: Accelerometer
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_gravity,           // 🌐
                titleRes = R.string.tool_gravity_meter,    // "Gravity Meter"
                descRes = R.string.desc_gravity_meter,     // "Measure Gravity"
                activityClass = GravityMeterActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 8: Bubble Level
            // Uses: Accelerometer
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_bubble,            // 🔵
                titleRes = R.string.tool_bubble_level,     // "Bubble Level"
                descRes = R.string.desc_bubble_level,      // "Check if Level"
                activityClass = BubbleLevelActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 9: Shake Detector
            // Uses: Accelerometer
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_shake,             // 🌀
                titleRes = R.string.tool_shake_detector,   // "Shake Detector"
                descRes = R.string.desc_shake_detector,    // "Detect Shakes"
                activityClass = ShakeDetectorActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 10: Space Ball
            // Uses: Accelerometer (Game)
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_space,             // 🎮
                titleRes = R.string.tool_space_ball,       // "Space Ball"
                descRes = R.string.desc_space_ball,        // "Tilt to Move"
                activityClass = SpaceBallActivity::class.java,
                buttonTextRes = R.string.button_play       // "PLAY" (not OPEN)
            ),

            // ============================================================
            // CATEGORY 4: DETECTION
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_metal,
                titleRes = R.string.category_detection,
                descRes = R.string.desc_metal_detector,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),

            // ============================================================
            // TOOL 11: Metal Detector
            // Uses: Magnetometer
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_metal,             // 🧲
                titleRes = R.string.tool_metal_detector,   // "Metal Detector"
                descRes = R.string.desc_metal_detector,    // "Detect Metals"
                activityClass = MetalDetectorActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 12: EMF Meter
            // Uses: Magnetometer
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_emf,               // 📡
                titleRes = R.string.tool_emf_meter,        // "EMF Meter"
                descRes = R.string.desc_emf_meter,         // "Electromagnetic Field"
                activityClass = EMFMeterActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // CATEGORY 5: ASTRONOMY
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_moon,
                titleRes = R.string.category_astronomy,
                descRes = R.string.desc_moon_phase,
                activityClass = null,
                buttonTextRes = R.string.button_open,
                isCategory = true
            ),

            // ============================================================
            // TOOL 13: Moon Phase
            // Uses: GPS + Math
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_moon,              // 🌙
                titleRes = R.string.tool_moon_phase,       // "Moon Phase"
                descRes = R.string.desc_moon_phase,        // "Check Moon Phase"
                activityClass = MoonPhaseActivity::class.java,
                buttonTextRes = R.string.button_open
            ),

            // ============================================================
            // TOOL 14: Sun Tracker
            // Uses: GPS + Math
            // ============================================================
            ToolItem(
                iconRes = R.string.icon_sun,               // ☀️
                titleRes = R.string.tool_sun_tracker,      // "Sun Tracker"
                descRes = R.string.desc_sun_tracker,       // "Sunrise & Sunset"
                activityClass = SunTrackerActivity::class.java,
                buttonTextRes = R.string.button_open
            )
        )

        // ============================================================
        // STEP 2: CREATE THE ADAPTER
        // The adapter connects the data (tools list) to the RecyclerView
        // ============================================================
        val adapter = ToolAdapter(this, tools)

        // ============================================================
        // STEP 3: CREATE THE LAYOUT MANAGER WITH 3 COLUMNS
        // GridLayoutManager arranges items in a grid with 3 columns
        // This fits all 14 tools better on the screen
        // ============================================================
        val layoutManager = GridLayoutManager(this, 3)  // ✅ 3 columns!

        // ============================================================
        // STEP 4: SET SPAN SIZE FOR CATEGORIES
        // Categories should span ALL 3 columns (full width)
        // Tools should span 1 column (one-third width)
        // ============================================================
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // Get the item at this position
                val item = tools[position]
                // If it's a category, span ALL 3 columns. Otherwise, span 1 column.
                return if (item.isCategory) {
                    3
                } else {
                    1
                }
            }
        }

        // ============================================================
        // STEP 5: APPLY TO RECYCLERVIEW
        // ============================================================
        recyclerView.layoutManager = layoutManager  // Set the layout with 3 columns
        recyclerView.adapter = adapter              // Set the data adapter
        recyclerView.setHasFixedSize(true)          // Performance optimization
    }
}