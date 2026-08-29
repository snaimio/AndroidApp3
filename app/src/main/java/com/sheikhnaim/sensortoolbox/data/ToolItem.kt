package com.sheikhnaim.sensortoolbox.data

import android.content.Context
import android.content.Intent
import com.sheikhnaim.sensortoolbox.R

/**
 * ToolItem - Data class representing a tool or category in the dashboard.
 *
 * This class holds all the information needed to display a tool card
 * in the RecyclerView dashboard, including its icon, title, description,
 * and the activity to launch when clicked.
 *
 * ============================================================
 * HOW IT WORKS:
 * ============================================================
 * 1. Tools are displayed as cards with icon, title, description, and OPEN button
 * 2. Categories are displayed as section headers (no click action)
 * 3. Clicking a tool opens its corresponding activity
 * 4. The same data structure is used for both tools and categories
 *
 * ============================================================
 * PROPERTIES:
 * ============================================================
 * @property iconRes Resource ID for the tool icon (emoji string resource)
 *                   Categories should use 0 (no icon)
 * @property titleRes Resource ID for the tool title (displayed in bold)
 *                    Categories show this as the section header
 * @property descRes Resource ID for the tool description (displayed in smaller text)
 *                   Categories should use 0 (no description)
 * @property activityClass The activity class to launch when this tool is clicked
 *                         Must be null for categories
 * @property buttonTextRes Resource ID for the button text (usually "OPEN" or "PLAY")
 *                         Categories should use 0 (no button)
 * @property isCategory True if this item is a category header (section divider)
 *                      When true, the item is NOT clickable
 *
 * ============================================================
 * USAGE EXAMPLES:
 * ============================================================
 *
 * // 1. Create a Tool (clickable)
 * ToolItem(
 *     iconRes = R.string.icon_compass,
 *     titleRes = R.string.tool_compass,
 *     descRes = R.string.desc_compass,
 *     activityClass = CompassActivity::class.java,
 *     buttonTextRes = R.string.button_open,
 *     isCategory = false
 * )
 *
 * // 2. Create a Category (non-clickable)
 * ToolItem(
 *     iconRes = 0,                    // Categories don't use icons
 *     titleRes = R.string.category_location,
 *     descRes = 0,                    // Categories don't use descriptions
 *     activityClass = null,           // Categories are not clickable
 *     buttonTextRes = 0,              // Categories don't have buttons
 *     isCategory = true
 * )
 *
 * // 3. Open the tool's activity
 * toolItem.openActivity(context)
 *
 * // 4. Check if the item is clickable
 * if (toolItem.isClickable()) {
 *     // Handle click
 * }
 *
 * ============================================================
 * BEST PRACTICES:
 * ============================================================
 * 1. Always use string resources (R.string.xxx) for text
 * 2. Categories should have isCategory = true
 * 3. Tools should have isCategory = false (default)
 * 4. Use the companion object helpers for cleaner code
 *
 * @author Sheikh Naim
 * @since 1.0
 */
data class ToolItem(
    /** Resource ID for the tool icon (emoji string from strings.xml) */
    val iconRes: Int,

    /** Resource ID for the tool title or category name */
    val titleRes: Int,

    /** Resource ID for the tool description (0 for categories) */
    val descRes: Int,

    /** The activity class to launch (null for categories) */
    val activityClass: Class<*>?,

    /** Resource ID for the button text (0 for categories) */
    val buttonTextRes: Int,

    /** True if this item is a category header (section divider) */
    val isCategory: Boolean = false
) {

    /**
     * Opens the activity associated with this tool.
     *
     * Creates an Intent for the activity class and starts it.
     * Does nothing if activityClass is null (for categories).
     *
     * @param context The Android context used to start the activity
     *
     * USAGE:
     * ```
     * toolItem.openActivity(context)
     * ```
     */
    fun openActivity(context: Context) {
        activityClass?.let {
            context.startActivity(Intent(context, it))
        }
    }

    /**
     * Checks if this item represents a clickable tool.
     *
     * An item is clickable if:
     * - It has an associated activity (activityClass != null)
     * - It is NOT a category (isCategory == false)
     *
     * @return True if this item can be clicked, False if it's a category
     *
     * USAGE:
     * ```
     * if (toolItem.isClickable()) {
     *     // Handle click
     * }
     * ```
     */
    fun isClickable(): Boolean {
        return activityClass != null && !isCategory
    }

    /**
     * Gets the appropriate content description for accessibility.
     *
     * Screen readers use this to describe the item to visually impaired users.
     *
     * @param context The Android context for string resources
     * @return A descriptive string for screen readers
     *
     * USAGE:
     * ```
     * val description = toolItem.getContentDescription(context)
     * view.contentDescription = description
     * ```
     */
    fun getContentDescription(context: Context): String {
        return if (isCategory) {
            // Category: just the title
            context.getString(titleRes)
        } else {
            // Tool: icon + title + description + button
            val icon = context.getString(iconRes)
            val title = context.getString(titleRes)
            val desc = context.getString(descRes)
            val button = context.getString(buttonTextRes)
            "$icon $title. $desc. $button"
        }
    }

    /**
     * Provides a string representation of this ToolItem for debugging.
     *
     * Useful for logging and debugging purposes.
     *
     * @return A string describing this ToolItem
     *
     * USAGE:
     * ```
     * Log.d("Dashboard", toolItem.toString())
     * ```
     */
    override fun toString(): String {
        return "ToolItem(" +
                "iconRes=$iconRes, " +
                "titleRes=$titleRes, " +
                "descRes=$descRes, " +
                "activityClass=${activityClass?.simpleName ?: "null"}, " +
                "buttonTextRes=$buttonTextRes, " +
                "isCategory=$isCategory)"
    }

    // ============================================================
    // COMPANION OBJECT - Helper functions
    // ============================================================

    companion object {
        /**
         * Creates a ToolItem for a tool (clickable item).
         *
         * This is a convenience method that sets isCategory = false
         * and validates that all required fields are provided.
         *
         * @param iconRes Resource ID for the tool icon
         * @param titleRes Resource ID for the tool title
         * @param descRes Resource ID for the tool description
         * @param activityClass The activity class to launch
         * @param buttonTextRes Resource ID for the button text (default: R.string.button_open)
         * @return A ToolItem configured as a clickable tool
         *
         * USAGE:
         * ```
         * ToolItem.createTool(
         *     iconRes = R.string.icon_compass,
         *     titleRes = R.string.tool_compass,
         *     descRes = R.string.desc_compass,
         *     activityClass = CompassActivity::class.java
         * )
         * ```
         */
        fun createTool(
            iconRes: Int,
            titleRes: Int,
            descRes: Int,
            activityClass: Class<*>,
            buttonTextRes: Int = R.string.button_open
        ): ToolItem {
            return ToolItem(
                iconRes = iconRes,
                titleRes = titleRes,
                descRes = descRes,
                activityClass = activityClass,
                buttonTextRes = buttonTextRes,
                isCategory = false
            )
        }

        /**
         * Creates a ToolItem for a category (non-clickable header).
         *
         * This is a convenience method that sets isCategory = true
         * and automatically sets iconRes, descRes, activityClass, and buttonTextRes to 0/null.
         *
         * @param titleRes Resource ID for the category title
         * @return A ToolItem configured as a category
         *
         * USAGE:
         * ```
         * ToolItem.createCategory(R.string.category_location)
         * ```
         */
        fun createCategory(
            titleRes: Int
        ): ToolItem {
            return ToolItem(
                iconRes = 0,           // Categories don't have icons
                titleRes = titleRes,
                descRes = 0,           // Categories don't have descriptions
                activityClass = null,  // Categories are not clickable
                buttonTextRes = 0,     // Categories don't have buttons
                isCategory = true
            )
        }

        /**
         * Creates a ToolItem for a game tool with "PLAY" button.
         *
         * This is a convenience method for game-type tools that use
         * "PLAY" instead of "OPEN" for the button text.
         *
         * @param iconRes Resource ID for the tool icon
         * @param titleRes Resource ID for the tool title
         * @param descRes Resource ID for the tool description
         * @param activityClass The activity class to launch
         * @return A ToolItem configured as a clickable game tool
         *
         * USAGE:
         * ```
         * ToolItem.createGameTool(
         *     iconRes = R.string.icon_space,
         *     titleRes = R.string.tool_space_ball,
         *     descRes = R.string.desc_space_ball,
         *     activityClass = SpaceBallActivity::class.java
         * )
         * ```
         */
        fun createGameTool(
            iconRes: Int,
            titleRes: Int,
            descRes: Int,
            activityClass: Class<*>
        ): ToolItem {
            return ToolItem(
                iconRes = iconRes,
                titleRes = titleRes,
                descRes = descRes,
                activityClass = activityClass,
                buttonTextRes = R.string.button_play,
                isCategory = false
            )
        }
    }
}