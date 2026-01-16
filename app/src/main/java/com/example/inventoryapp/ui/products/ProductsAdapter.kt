package com.example.inventoryapp.ui.products

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.ItemProductBinding
import java.text.SimpleDateFormat
import java.util.*

class ProductsAdapter(
    private val onItemClick: (ProductEntity) -> Unit,
    private val onEditClick: (ProductEntity) -> Unit,
    private val onDeleteClick: (ProductEntity) -> Unit,
    private val onToggleSelection: (ProductEntity, Boolean) -> Unit
) : ListAdapter<ProductEntity, ProductsAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private var categoryNames: Map<Long, String> = emptyMap()
    private var selectionMode: Boolean = false
    private var selectedIds: Set<Long> = emptySet()

    fun updateCategoryNames(names: Map<Long, String>) {
        categoryNames = names
        notifyDataSetChanged()
    }

    fun updateSelectionState(enabled: Boolean, selected: Set<Long>) {
        selectionMode = enabled
        selectedIds = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, ::handleItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun handleItemClick(product: ProductEntity) {
        if (selectionMode) {
            val currentlySelected = selectedIds.contains(product.id)
            onToggleSelection(product, !currentlySelected)
        } else {
            onItemClick(product)
        }
    }

    inner class ViewHolder(
        private val binding: ItemProductBinding,
        private val defaultClick: (ProductEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            val categoryName = product.categoryId?.let { categoryNames[it] } ?: "Brak kategorii"
            val hasSerial = product.serialNumber.isNotEmpty()
            val isSelected = selectedIds.contains(product.id)

            binding.productName.text = product.name
            binding.productSerialNumber.text = product.serialNumber
            binding.productCategory.text = categoryName

            binding.serialNumberContainer.isVisible = hasSerial
            binding.noSerialNumber.isVisible = !hasSerial

            binding.productCreatedDate.isVisible = product.createdAt > 0
            if (product.createdAt > 0) {
                binding.productCreatedDate.text = "Dodano: ${dateFormat.format(Date(product.createdAt))}"
            }

            // Selection visuals
            binding.selectionCheckBox.setOnCheckedChangeListener(null)
            binding.selectionCheckBox.isVisible = selectionMode
            binding.selectionCheckBox.isChecked = isSelected
            binding.root.strokeColor = if (isSelected) Color.parseColor("#6366F1") else Color.parseColor("#E5E7EB")

            binding.selectionCheckBox.setOnCheckedChangeListener { _, checked ->
                onToggleSelection(product, checked)
            }

            binding.root.setOnClickListener { defaultClick(product) }
            binding.root.setOnLongClickListener {
                onToggleSelection(product, !isSelected)
                true
            }

            binding.editButton.setOnClickListener { onEditClick(product) }
            binding.deleteButton.setOnClickListener { onDeleteClick(product) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
