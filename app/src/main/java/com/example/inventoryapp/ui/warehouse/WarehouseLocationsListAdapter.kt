package com.example.inventoryapp.ui.warehouse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemWarehouseLocationCardBinding

data class WarehouseLocationCard(
    val name: String,
    val productCount: Int,
    val categories: String,
    val description: String = ""
)

class WarehouseLocationsListAdapter(
    private val onLocationClick: (String) -> Unit
) : ListAdapter<WarehouseLocationCard, WarehouseLocationsListAdapter.LocationViewHolder>(
    object : DiffUtil.ItemCallback<WarehouseLocationCard>() {
        override fun areItemsTheSame(oldItem: WarehouseLocationCard, newItem: WarehouseLocationCard) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: WarehouseLocationCard, newItem: WarehouseLocationCard) =
            oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemWarehouseLocationCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LocationViewHolder(private val binding: ItemWarehouseLocationCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: WarehouseLocationCard) {
            binding.locationName.text = location.name
            binding.productCount.text = "${location.productCount} szt."
            binding.locationCategories.text = location.categories
            binding.locationDescription.text = location.description.takeIf { it.isNotEmpty() } ?: "Brak opisu"

            // Set product count color and background
            val color = when {
                location.productCount < 5 -> android.graphics.Color.parseColor("#EF4444") // Red
                location.productCount < 10 -> android.graphics.Color.parseColor("#F59E0B") // Orange
                else -> android.graphics.Color.parseColor("#10B981") // Green
            }
            binding.productCount.setTextColor(android.graphics.Color.WHITE)
            binding.productCount.setBackgroundColor(color)

            // Click listener
            binding.root.setOnClickListener {
                onLocationClick(location.name)
            }
        }
    }
}
