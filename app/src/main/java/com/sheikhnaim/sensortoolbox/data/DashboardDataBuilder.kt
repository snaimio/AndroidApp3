package com.sheikhnaim.sensortoolbox.data

import com.sheikhnaim.sensortoolbox.LiveLocationActivity
import com.sheikhnaim.sensortoolbox.MapActivity
import com.sheikhnaim.sensortoolbox.R
import com.sheikhnaim.sensortoolbox.astronomy.MoonPhaseActivity
import com.sheikhnaim.sensortoolbox.astronomy.SunTrackerActivity
import com.sheikhnaim.sensortoolbox.detection.EMFMeterActivity
import com.sheikhnaim.sensortoolbox.detection.MetalDetectorActivity
import com.sheikhnaim.sensortoolbox.location.LocationActivity
import com.sheikhnaim.sensortoolbox.location.TrailTrackerActivity
import com.sheikhnaim.sensortoolbox.motion.BubbleLevelActivity
import com.sheikhnaim.sensortoolbox.motion.GravityMeterActivity
import com.sheikhnaim.sensortoolbox.motion.ShakeDetectorActivity
import com.sheikhnaim.sensortoolbox.motion.SpaceBallActivity
import com.sheikhnaim.sensortoolbox.navigation.CompassActivity
import com.sheikhnaim.sensortoolbox.speed.AltimeterActivity
import com.sheikhnaim.sensortoolbox.speed.DistanceTrackerActivity
import com.sheikhnaim.sensortoolbox.speed.SpeedometerActivity

/**
 * DashboardDataBuilder - Creates the list of ToolItems for the dashboard.
 *
 * This is a central place to define all tools and categories
 * that appear on the main dashboard screen.
 *
 * @author Sheikh Naim
 * @since 1.0
 */
object DashboardDataBuilder {

    /**
     * Builds the complete list of tools and categories for the dashboard.
     *
     * @return List of ToolItem objects ready for the RecyclerView
     */
    fun buildDashboardItems(): List<ToolItem> {
        return listOf(
            // ============================================================
            // LOCATION & NAVIGATION CATEGORY
            // ============================================================
            ToolItem.createCategory(R.string.category_location),

            ToolItem.createTool(
                iconRes = R.string.icon_map,
                titleRes = R.string.tool_map,
                descRes = R.string.desc_map,
                activityClass = MapActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_live_location,
                titleRes = R.string.tool_live_location,
                descRes = R.string.desc_live_location,
                activityClass = LiveLocationActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_trail_tracker,
                titleRes = R.string.tool_trail_tracker,
                descRes = R.string.desc_trail_tracker,
                activityClass = TrailTrackerActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_compass,
                titleRes = R.string.tool_compass,
                descRes = R.string.desc_compass,
                activityClass = CompassActivity::class.java
            ),

            // ============================================================
            // SPEED & DISTANCE CATEGORY
            // ============================================================
            ToolItem.createCategory(R.string.category_speed),

            ToolItem.createTool(
                iconRes = R.string.icon_speedometer,
                titleRes = R.string.tool_speedometer,
                descRes = R.string.desc_speedometer,
                activityClass = SpeedometerActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_distance,
                titleRes = R.string.tool_distance_tracker,
                descRes = R.string.desc_distance_tracker,
                activityClass = DistanceTrackerActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_altimeter,
                titleRes = R.string.tool_altimeter,
                descRes = R.string.desc_altimeter,
                activityClass = AltimeterActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_fitness_tracker,
                titleRes = R.string.tool_fitness_tracker,
                descRes = R.string.desc_fitness_tracker,
                activityClass = com.sheikhnaim.sensortoolbox.FitnessTrackerActivity::class.java
            ),

            // ============================================================
            // MOTION & SENSORS CATEGORY
            // ============================================================
            ToolItem.createCategory(R.string.category_motion),

            ToolItem.createTool(
                iconRes = R.string.icon_gravity,
                titleRes = R.string.tool_gravity_meter,
                descRes = R.string.desc_gravity_meter,
                activityClass = GravityMeterActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_bubble,
                titleRes = R.string.tool_bubble_level,
                descRes = R.string.desc_bubble_level,
                activityClass = BubbleLevelActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_shake,
                titleRes = R.string.tool_shake_detector,
                descRes = R.string.desc_shake_detector,
                activityClass = ShakeDetectorActivity::class.java
            ),

            // Space Ball uses "PLAY" button
            ToolItem.createGameTool(
                iconRes = R.string.icon_space,
                titleRes = R.string.tool_space_ball,
                descRes = R.string.desc_space_ball,
                activityClass = SpaceBallActivity::class.java
            ),

            // ============================================================
            // DETECTION CATEGORY
            // ============================================================
            ToolItem.createCategory(R.string.category_detection),

            ToolItem.createTool(
                iconRes = R.string.icon_metal,
                titleRes = R.string.tool_metal_detector,
                descRes = R.string.desc_metal_detector,
                activityClass = MetalDetectorActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_emf,
                titleRes = R.string.tool_emf_meter,
                descRes = R.string.desc_emf_meter,
                activityClass = EMFMeterActivity::class.java
            ),

            // ============================================================
            // ASTRONOMY CATEGORY
            // ============================================================
            ToolItem.createCategory(R.string.category_astronomy),

            ToolItem.createTool(
                iconRes = R.string.icon_moon,
                titleRes = R.string.tool_moon_phase,
                descRes = R.string.desc_moon_phase,
                activityClass = MoonPhaseActivity::class.java
            ),

            ToolItem.createTool(
                iconRes = R.string.icon_sun,
                titleRes = R.string.tool_sun_tracker,
                descRes = R.string.desc_sun_tracker,
                activityClass = SunTrackerActivity::class.java
            )
        )
    }
}