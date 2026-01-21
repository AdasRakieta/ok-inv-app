package com.example.inventoryapp.ui.warehouse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
    private val onLocationClick: (WarehouseLocationCard) -> Unit,
    private val onLocationLongClick: (WarehouseLocationCard) -> Boolean = { false },
    private val onSelectionChanged: () -> Unit = {}
) : ListAdapter<WarehouseLocationCard, WarehouseLocationsListAdapter.LocationViewHolder>(
    object : DiffUtil.ItemCallback<WarehouseLocationCard>() {
        override fun areItemsTheSame(oldItem: WarehouseLocationCard, newItem: WarehouseLocationCard) =
            oldItem.name == newItem.name

        override fun areContentsTheSame(oldItem: WarehouseLocationCard, newItem: WarehouseLocationCard) =
            oldItem == newItem
    }
) {

    var selectionMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    private val selectedItems = mutableSetOf<String>()

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

    fun toggleSelection(locationName: String) {
        if (selectedItems.contains(locationName)) {
            selectedItems.remove(locationName)
        } else {
            selectedItems.add(locationName)
        }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(currentList.map { it.name })
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun getSelectedNames(): List<String> = selectedItems.toList()

    fun getSelectedCount(): Int = selectedItems.size

    inner class LocationViewHolder(private val binding: ItemWarehouseLocationCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: WarehouseLocationCard) {
            binding.locationName.text = location.name
            binding.productCount.text = "${location.productCount} szt."
            binding.locationCategories.text = location.categories
            binding.locationDescription.text = location.description.takeIf { it.isNotEmpty() } ?: "Brak opisu"

            binding.locationCheckbox.isVisible = selectionMode
            binding.locationCheckbox.isChecked = selectedItems.contains(location.name)

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
                if (selectionMode) {
                    toggleSelection(location.name)
                } else {
                    onLocationClick(location)
                }
            }

            binding.root.setOnLongClickListener {
                if (!selectionMode) {
                    onLocationLongClick(location)
                } else {
                    false
                }
            }
        }
    }
}
