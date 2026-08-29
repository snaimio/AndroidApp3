package com.sheikhnaim.sensortoolbox.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sheikhnaim.sensortoolbox.R

/**
 * ToolAdapter - RecyclerView Adapter for displaying tools and categories
 * on the main dashboard.
 *
 * HOW IT WORKS:
 * 1. Receives a list of ToolItem objects (mix of categories and tools)
 * 2. Uses two different view types: CATEGORY and TOOL
 * 3. Categories are headers (e.g., "📍 LOCATION & NAVIGATION")
 * 4. Tools are clickable items with icon, title, description, and OPEN button
 * 5. Clicking a tool opens its corresponding activity
 *
 * VIEW TYPES:
 * - VIEW_TYPE_CATEGORY: Shows a section header (no click action)
 * - VIEW_TYPE_TOOL: Shows a tool with icon, title, description, and OPEN button
 *
 * @param context The Android context (used for starting activities)
 * @param items The list of ToolItem objects to display
 */
class ToolAdapter(
    private val context: Context,
    private val items: List<ToolItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // ============================================================
    // COMPANION OBJECT - Constants for view types
    // ============================================================

    companion object {
        /** View type for category headers (section dividers) */
        private const val VIEW_TYPE_CATEGORY = 0

        /** View type for individual tool items (clickable) */
        private const val VIEW_TYPE_TOOL = 1
    }

    // ============================================================
    // OVERRIDE METHODS - RecyclerView.Adapter implementation
    // ============================================================

    /**
     * Determines the view type for a given position.
     *
     * @param position The position in the data set
     * @return VIEW_TYPE_CATEGORY for headers, VIEW_TYPE_TOOL for tools
     */
    override fun getItemViewType(position: Int): Int {
        return if (items[position].isCategory) VIEW_TYPE_CATEGORY else VIEW_TYPE_TOOL
    }

    /**
     * Creates a new ViewHolder for the given view type.
     * Inflates the appropriate layout (category or tool).
     *
     * @param parent The parent ViewGroup
     * @param viewType The type of view to create (CATEGORY or TOOL)
     * @return A ViewHolder for the specified view type
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                // Inflate category header layout
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }
            else -> {
                // Inflate tool item layout
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tool, parent, false)
                ToolViewHolder(view)
            }
        }
    }

    /**
     * Binds data to the ViewHolder at the given position.
     * Sets text, icons, and click listeners.
     *
     * @param holder The ViewHolder to bind data to
     * @param position The position in the data set
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Get the item at this position
        val item = items[position]

        when (holder) {
            is CategoryViewHolder -> {
                // Category ViewHolder - show section header
                holder.categoryTitle.setText(item.titleRes)
            }
            is ToolViewHolder -> {
                // Tool ViewHolder - show tool details with click handling

                // Set icon, title, description, and button text
                holder.toolIcon.setText(item.iconRes)
                holder.toolTitle.setText(item.titleRes)
                holder.toolDesc.setText(item.descRes)
                holder.toolButton.setText(item.buttonTextRes)

                // Set click listeners
                // The OPEN button opens the activity
                holder.toolButton.setOnClickListener {
                    item.openActivity(context)
                }

                // The entire card view also opens the activity (better UX)
                holder.itemView.setOnClickListener {
                    item.openActivity(context)
                }
            }
        }
    }

    /**
     * Returns the total number of items in the data set.
     *
     * @return The size of the items list
     */
    override fun getItemCount(): Int = items.size

    /**
     * Optional: Enable stable IDs for better performance.
     * Uncomment if you need stable IDs for animations.
     */
    // override fun getItemId(position: Int): Long = position.toLong()
    // override fun setHasStableIds(true) // Call in constructor

    // ============================================================
    // VIEWHOLDER CLASSES - Hold references to views
    // ============================================================

    /**
     * CategoryViewHolder - Holds views for category headers.
     *
     * Categories are section headers that divide the dashboard
     * into logical groups (e.g., "📍 LOCATION & NAVIGATION").
     * These are NOT clickable.
     *
     * @param itemView The inflated view for this ViewHolder
     */
    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** TextView displaying the category title */
        val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
    }

    /**
     * ToolViewHolder - Holds views for individual tool items.
     *
     * Tools are clickable items that display an icon, title,
     * description, and an OPEN button. Clicking either the
     * button or the entire card opens the corresponding activity.
     *
     * @param itemView The inflated view for this ViewHolder
     */
    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** TextView displaying the tool icon (emoji) */
        val toolIcon: TextView = itemView.findViewById(R.id.toolIcon)

        /** TextView displaying the tool title */
        val toolTitle: TextView = itemView.findViewById(R.id.toolTitle)

        /** TextView displaying the tool description */
        val toolDesc: TextView = itemView.findViewById(R.id.toolDesc)

        /** Button that opens the tool's activity */
        val toolButton: Button = itemView.findViewById(R.id.toolButton)
    }
}