package com.sheikhnaim.sensortoolbox.data

import android.content.Context
import android.content.Intent

/**
 * ToolItem - Data class for each tool in the dashboard
 * Holds all information for a tool: icon, name, description, and activity to open
 */
data class ToolItem(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val activityClass: Class<*>?,
    val buttonTextRes: Int,
    val isCategory: Boolean = false
) {
    fun openActivity(context: Context) {
        activityClass?.let {
            context.startActivity(Intent(context, it))
        }
    }
}