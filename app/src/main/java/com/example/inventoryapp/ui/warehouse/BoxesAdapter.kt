package com.example.inventoryapp.ui.warehouse

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.databinding.ItemBoxBinding

class BoxesAdapter(
    private val onBoxClick: (BoxEntity) -> Unit,
    private val onBoxLongClick: (BoxEntity) -> Unit = {},
    private val onOptionsClick: (BoxEntity, View) -> Unit = { _, _ -> }
) : ListAdapter<BoxEntity, BoxesAdapter.BoxViewHolder>(BoxDiffCallback()) {

    private var countsMap: Map<Long, Int> = emptyMap()
    private var locationMap: Map<Long, String> = emptyMap()
    private var fullList: List<BoxEntity> = emptyList()
    private var filteredList: List<BoxEntity> = emptyList()

    private val selectedItems = mutableSetOf<Long>()
    var selectionMode = false
        set(value) {
            if (!value) {
                selectedItems.clear()
            }
            field = value
            notifyDataSetChanged()
        }

    fun getSelectedItems(): Set<Long> = selectedItems.toSet()

    fun getSelectedCount(): Int = selectedItems.size

    fun selectAll() {
        currentList.forEach { selectedItems.add(it.id) }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun toggleSelection(boxId: Long) {
        if (selectedItems.contains(boxId)) {
            selectedItems.remove(boxId)
        } else {
            selectedItems.add(boxId)
        }
        notifyDataSetChanged()
    }

    fun setCountsMap(map: Map<Long, Int>) {
        countsMap = map
        submitList(filteredList.toList())
    }

    fun setFullList(list: List<BoxEntity>) {
        fullList = list
        filteredList = list
        submitList(list.toList())
    }

    fun setLocationsMap(map: Map<Long, String>) {
        locationMap = map
        submitList(filteredList.toList())
    }

    fun filterByQuery(query: String) {
        filteredList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { box ->
                box.name.contains(query, ignoreCase = true) ||
                    (box.description ?: "").contains(query, ignoreCase = true)
            }
        }
        submitList(filteredList.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val binding = ItemBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoxViewHolder(binding, onBoxClick, onBoxLongClick, onOptionsClick)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        val box = getItem(position)
        val isSelected = selectedItems.contains(box.id)
        holder.bind(box, selectionMode, isSelected)
    }

    inner class BoxViewHolder(
        private val binding: ItemBoxBinding,
        private val onBoxClick: (BoxEntity) -> Unit,
        private val onBoxLongClick: (BoxEntity) -> Unit,
        private val onOptionsClick: (BoxEntity, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(box: BoxEntity, selectionMode: Boolean, isSelected: Boolean) {
            binding.boxName.text = box.name
            binding.boxCreatedDate.text = android.text.format.DateFormat.getDateFormat(binding.root.context)
                .format(java.util.Date(box.createdAt))
            val ctx = binding.root.context
            if (box.warehouseLocationId != null) {
                val locLabel = locationMap[box.warehouseLocationId] ?: "Lokalizacja #${box.warehouseLocationId}"
                binding.boxLocation.text = locLabel
            } else {
                binding.boxLocation.text = ctx.getString(com.example.inventoryapp.R.string.no_location)
            }
            binding.boxDescription.text = box.description ?: ""
            val count = countsMap[box.id] ?: 0
            binding.boxProductCount.text = if (count == 0) {
                ctx.getString(com.example.inventoryapp.R.string.no_products)
            } else {
                ctx.resources.getQuantityString(com.example.inventoryapp.R.plurals.products_count, count, count)
            }

            // Selection UI
            binding.selectionCheckbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.selectionCheckbox.isChecked = isSelected

            if (selectionMode && isSelected) {
                binding.boxCard.strokeColor = Color.parseColor("#3B82F6")
                binding.boxCard.strokeWidth = 4
            } else {
                binding.boxCard.strokeColor = Color.parseColor("#E5E7EB")
                binding.boxCard.strokeWidth = 2
            }

            binding.root.setOnClickListener {
                if (selectionMode) {
                    binding.selectionCheckbox.isChecked = !binding.selectionCheckbox.isChecked
                }
                onBoxClick(box)
            }

            binding.selectionCheckbox.setOnClickListener {
                onBoxClick(box)
            }

            binding.root.setOnLongClickListener {
                onBoxLongClick(box)
                true
            }

            binding.boxOptions.setOnClickListener { v -> onOptionsClick(box, v) }
        }
    }
}

class BoxDiffCallback : DiffUtil.ItemCallback<BoxEntity>() {
    override fun areItemsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem == newItem
}
