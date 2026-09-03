package com.sheikhnaim.sensortoolbox

// ============================================================
// IMPORTS - These bring in the classes we need
// ============================================================

// Android permissions and location
import android.Manifest
import android.content.pm.PackageManager

// AndroidX support
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

// UI components
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import android.widget.TextView

// App data and adapters
import com.sheikhnaim.sensortoolbox.R
import com.sheikhnaim.sensortoolbox.data.DashboardDataBuilder
import com.sheikhnaim.sensortoolbox.data.ToolAdapter
import com.sheikhnaim.sensortoolbox.data.ToolItem

// Import all tools (existing from Assignment 5)
import com.sheikhnaim.sensortoolbox.astronomy.MoonPhaseActivity
import com.sheikhnaim.sensortoolbox.astronomy.SunTrackerActivity
import com.sheikhnaim.sensortoolbox.detection.MetalDetectorActivity
import com.sheikhnaim.sensortoolbox.detection.EMFMeterActivity
import com.sheikhnaim.sensortoolbox.location.LocationActivity
import com.sheikhnaim.sensortoolbox.motion.BubbleLevelActivity
import com.sheikhnaim.sensortoolbox.motion.GravityMeterActivity
import com.sheikhnaim.sensortoolbox.motion.ShakeDetectorActivity
import com.sheikhnaim.sensortoolbox.motion.SpaceBallActivity
import com.sheikhnaim.sensortoolbox.navigation.CompassActivity
import com.sheikhnaim.sensortoolbox.speed.AltimeterActivity
import com.sheikhnaim.sensortoolbox.speed.DistanceTrackerActivity
import com.sheikhnaim.sensortoolbox.speed.SpeedometerActivity

/**
 * DashboardActivity - Main screen of Sensor ToolBox app
 *
 * ============================================================
 * WHAT THIS ACTIVITY DOES:
 * ============================================================
 * 1. Displays all tools in a RecyclerView grid
 * 2. Shows GPS and Sensor status
 * 3. Provides toolbar menu navigation to Live Location and Map
 *
 * ============================================================
 * ASSIGNMENT 6 UPDATES:
 * ============================================================
 * - Added Toolbar Menu with 2 items:
 *   • Live Location (NEW - continuous GPS updates)
 *   • Map Location (NEW - OpenStreetMap)
 *   (Compass is already available as a tool in the dashboard)
 *
 * @author Sheikh Naim
 * @since 1.0
 */
class DashboardActivity : AppCompatActivity() {

    // ============================================================
    // UI VIEWS
    // ============================================================

    /** Shows GPS status (Locked / Permission needed) */
    private lateinit var gpsStatusText: TextView

    /** Shows sensor status (Active) */
    private lateinit var sensorStatusText: TextView

    /** RecyclerView that displays all tools in a grid */
    private lateinit var recyclerView: RecyclerView

    // ============================================================
    // LIFECYCLE METHODS
    // ============================================================

    /**
     * onCreate - Called when the activity is created
     *
     * Sets up:
     * - Layout
     * - Toolbar with support action bar
     * - GPS and sensor status
     * - RecyclerView with all tools
     * - Window insets for edge-to-edge display
     *
     * @param savedInstanceState Previously saved state (if any)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // ============================================================
        // STEP 1: Set up the Toolbar
        // The toolbar has the title and menu (three dots)
        // ============================================================
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // ============================================================
        // STEP 2: Initialize UI views
        // Find all views by their IDs from activity_dashboard.xml
        // ============================================================
        gpsStatusText = findViewById(R.id.gpsStatusText)
        sensorStatusText = findViewById(R.id.sensorStatusText)
        recyclerView = findViewById(R.id.toolRecyclerView)

        // STEP 3: Setup UI
        // ============================================================
        updateStatus()
        setupRecyclerView()
    }

    // ============================================================
    // MENU METHODS (ASSIGNMENT 6)
    // ============================================================

    /**
     * onCreateOptionsMenu - Creates the toolbar menu
     *
     * ============================================================
     * WHAT THIS DOES:
     * ============================================================
     * 1. Inflates the menu resource file (dashboard_menu.xml)
     * 2. Displays menu items in the toolbar overflow menu (⋮)
     *
     * ============================================================
     * MENU ITEMS:
     * ============================================================
     * - Live Location (NEW for Assignment 6)
     * - Map Location (NEW for Assignment 6)
     *
     * NOTE: Compass is already available as a tool in the dashboard
     * so we don't add it to the menu to avoid duplication.
     *
     * @param menu The menu object to inflate into
     * @return true if menu was created successfully
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu resource file
        menuInflater.inflate(R.menu.dashboard_menu, menu)
        return true
    }

    /**
     * onOptionsItemSelected - Handles menu item clicks
     *
     * ============================================================
     * WHAT THIS DOES:
     * ============================================================
     * 1. Determines which menu item was clicked
     * 2. Navigates to the corresponding activity using Intent
     * 3. Returns true if the click was handled
     *
     * @param item The menu item that was clicked
     * @return true if the click was handled, false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // ============================================================
            // OPTION 1: Live Location (NEW for Assignment 6)
            // ============================================================
            R.id.action_live_location -> {
                val intent = Intent(this, LiveLocationActivity::class.java)
                startActivity(intent)
                true
            }

            // ============================================================
            // OPTION 2: Map Location (NEW for Assignment 6)
            // ============================================================
            R.id.action_map -> {
                val intent = Intent(this, MapActivity::class.java)
                startActivity(intent)
                true
            }

            // ============================================================
            // DEFAULT: Handle any other menu item
            // ============================================================
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ============================================================
    // STATUS METHODS (FROM ASSIGNMENT 5)
    // ============================================================

    /**
     * updateStatus - Updates GPS and sensor status
     *
     * ============================================================
     * WHAT THIS DOES:
     * ============================================================
     * 1. Checks if location permission is granted
     * 2. Updates GPS status text (Locked / Permission needed)
     * 3. Shows sensor status as active
     */
    private fun updateStatus() {
        // Check if we have location permission
        val hasLocationPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Update GPS status based on permission
        gpsStatusText.text = if (hasLocationPermission) {
            getString(R.string.gps_locked)  // "GPS: Locked"
        } else {
            getString(R.string.gps_permission_needed)  // "GPS: Permission needed"
        }

        // Sensors are always active (they don't need permission)
        sensorStatusText.text = getString(R.string.sensors_active)  // "Sensors: Active"
    }

    // ============================================================
    // RECYCLERVIEW SETUP (FROM ASSIGNMENT 5)
    // ============================================================

    /**
     * setupRecyclerView - Sets up the RecyclerView with all tools
     *
     * ============================================================
     * WHAT THIS DOES:
     * ============================================================
     * 1. Creates a list of ToolItem objects
     * 2. Each category is a header (isCategory = true)
     * 3. Each tool is a clickable card
     * 4. GridLayoutManager with 1 column
     * 5. ToolAdapter handles the display
     *
     * ============================================================
     * TOOLS INCLUDED:
     * ============================================================
     * - Compass (navigation)
     * - GPS Location
     * - Speedometer
     * - Distance Tracker
     * - Altimeter
     * - Gravity Meter
     * - Bubble Level
     * - Shake Detector
     * - Space Ball (game)
     * - Metal Detector
     * - EMF Meter
     * - Moon Phase
     * - Sun Tracker
     */
    private fun setupRecyclerView() {
        val tools = DashboardDataBuilder.buildDashboardItems()
        val adapter = ToolAdapter(this, tools)
        val layoutManager = GridLayoutManager(this, 1)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
    }
}