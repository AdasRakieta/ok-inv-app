package com.example.inventoryapp.ui.products.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.ui.products.models.DisplayMovement
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceMovementAdapter : ListAdapter<DisplayMovement, DeviceMovementAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device_movement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val actionText: TextView = itemView.findViewById(R.id.actionText)
        private val containerText: TextView = itemView.findViewById(R.id.containerText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(entity: DisplayMovement) {
            val statusPart = entity.packageStatus?.let { " ($it)" } ?: ""
            actionText.text = "${entity.action}$statusPart"

            val from = entity.fromDisplay ?: ""
            val to = entity.toDisplay ?: ""
            val connector = if (from.isNotEmpty() && to.isNotEmpty()) " → " else ""
            containerText.text = "$from$connector$to"

            val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            timestampText.text = dateFormat.format(Date(entity.timestamp))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DisplayMovement>() {
        override fun areItemsTheSame(oldItem: DisplayMovement, newItem: DisplayMovement): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DisplayMovement, newItem: DisplayMovement): Boolean = oldItem == newItem
    }
}
