package com.sheikhnaim.sensortoolbox.data

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sheikhnaim.sensortoolbox.R

class ToolAdapter(
    private val context: Context,
    private val items: List<ToolItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_TOOL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isCategory) VIEW_TYPE_CATEGORY else VIEW_TYPE_TOOL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category, parent, false)
                CategoryViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_tool, parent, false)
                ToolViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                val item = items[position]
                holder.categoryTitle.setText(item.titleRes)
            }
            is ToolViewHolder -> {
                val item = items[position]
                holder.toolIcon.setText(item.iconRes)
                holder.toolTitle.setText(item.titleRes)
                holder.toolDesc.setText(item.descRes)
                holder.toolButton.setText(item.buttonTextRes)

                holder.toolButton.setOnClickListener { item.openActivity(context) }
                holder.itemView.setOnClickListener { item.openActivity(context) }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
    }

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val toolIcon: TextView = itemView.findViewById(R.id.toolIcon)
        val toolTitle: TextView = itemView.findViewById(R.id.toolTitle)
        val toolDesc: TextView = itemView.findViewById(R.id.toolDesc)
        val toolButton: Button = itemView.findViewById(R.id.toolButton)
    }
}