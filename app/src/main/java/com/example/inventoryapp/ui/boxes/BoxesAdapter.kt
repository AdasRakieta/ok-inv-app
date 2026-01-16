package com.example.inventoryapp.ui.boxes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.dao.BoxDao
import com.example.inventoryapp.data.local.dao.BoxWithCount
import com.example.inventoryapp.databinding.ItemBoxBinding
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * RecyclerView adapter for displaying boxes with product counts
 */
class BoxesAdapter(
    private val onBoxClick: (Long) -> Unit,
    private val onBoxLongClick: (Long) -> Boolean
) : ListAdapter<BoxWithCount, BoxesAdapter.BoxViewHolder>(BoxDiffCallback()) {

    private val selectedBoxes = mutableSetOf<Long>()
    var selectionMode = false
        private set

    fun toggleSelection(boxId: Long) {
        if (selectedBoxes.contains(boxId)) {
            selectedBoxes.remove(boxId)
        } else {
            selectedBoxes.add(boxId)
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedBoxes.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun enterSelectionMode() {
        selectionMode = true
        notifyDataSetChanged()
    }

    fun getSelectedBoxes(): Set<Long> = selectedBoxes.toSet()

    fun getSelectedCount(): Int = selectedBoxes.size

    fun selectAll(boxes: List<BoxWithCount>) {
        boxes.forEach { selectedBoxes.add(it.box.id) }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedBoxes.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val binding = ItemBoxBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BoxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BoxViewHolder(
        private val binding: ItemBoxBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(boxWithCount: BoxWithCount) {
            val box = boxWithCount.box
            val productCount = boxWithCount.productCount

            binding.boxName.text = box.name
            
            // Show description as badge only if exists and not empty
            if (!box.description.isNullOrBlank()) {
                binding.boxDescription.visibility = android.view.View.VISIBLE
                binding.boxDescription.text = box.description
            } else {
                binding.boxDescription.visibility = android.view.View.GONE
            }
            
            binding.boxLocation.text = box.warehouseLocation ?: "No location"
            binding.boxProductCount.text = "$productCount products"

            // Format creation date - "Created on MMM d, yyyy" style
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            binding.boxCreatedDate.text = "Created on ${dateFormat.format(box.createdAt)}"

            // Selection mode visual feedback
            if (selectionMode) {
                binding.root.isChecked = selectedBoxes.contains(box.id)
                binding.root.strokeWidth = if (selectedBoxes.contains(box.id)) 4 else 2
                binding.root.strokeColor = if (selectedBoxes.contains(box.id)) {
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                } else {
                    ContextCompat.getColor(binding.root.context, R.color.border)
                }
            } else {
                binding.root.isChecked = false
                binding.root.strokeWidth = 2
                binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.border)
            }

            // Click listeners
            binding.root.setOnClickListener {
                if (selectionMode) {
                    toggleSelection(box.id)
                } else {
                    onBoxClick(box.id)
                }
            }

            binding.root.setOnLongClickListener {
                if (!selectionMode) {
                    enterSelectionMode()
                    toggleSelection(box.id)
                }
                onBoxLongClick(box.id)
            }
        }
    }
}

/**
 * DiffUtil callback for efficient list updates
 */
class BoxDiffCallback : DiffUtil.ItemCallback<BoxWithCount>() {
    override fun areItemsTheSame(oldItem: BoxWithCount, newItem: BoxWithCount): Boolean {
        return oldItem.box.id == newItem.box.id
    }

    override fun areContentsTheSame(oldItem: BoxWithCount, newItem: BoxWithCount): Boolean {
        return oldItem == newItem
    }
}
