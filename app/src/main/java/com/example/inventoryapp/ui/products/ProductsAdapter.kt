package com.example.inventoryapp.ui.products

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.ItemProductBinding
import java.text.SimpleDateFormat
import java.util.*

class ProductsAdapter(
    private val onItemClick: (ProductEntity) -> Unit,
    private val onItemLongClick: (ProductEntity) -> Unit = {},
    private val getCategoryName: (Long?) -> String = { _ -> "-" },
    private val getCategoryIcon: (Long?) -> String = { _ -> "📦" }
) : ListAdapter<ProductEntity, ProductsAdapter.ViewHolder>(DiffCallback()) {

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
    
    fun toggleSelection(productId: Long) {
        if (selectedItems.contains(productId)) {
            selectedItems.remove(productId)
        } else {
            selectedItems.add(productId)
        }
        notifyDataSetChanged()
    }

    companion object {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick, getCategoryName, getCategoryIcon)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        val isSelected = selectedItems.contains(product.id)
        holder.bind(product, selectionMode, isSelected)
    }

    class ViewHolder(
        private val binding: ItemProductBinding,
        private val onItemClick: (ProductEntity) -> Unit,
        private val onItemLongClick: (ProductEntity) -> Unit,
        private val getCategoryName: (Long?) -> String,
        private val getCategoryIcon: (Long?) -> String
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity, selectionMode: Boolean, isSelected: Boolean) {
            binding.apply {
                productName.text = product.name
                productSerialNumber.text = product.serialNumber
                productCategory.text = getCategoryName(product.categoryId)
                categoryIcon.text = getCategoryIcon(product.categoryId)
                
                // Selection mode UI
                selectionCheckbox.visibility = if (selectionMode) android.view.View.VISIBLE else android.view.View.GONE
                selectionCheckbox.isChecked = isSelected
                
                // Highlight selected items
                if (selectionMode && isSelected) {
                    productCard.strokeColor = Color.parseColor("#3B82F6")
                    productCard.strokeWidth = 4
                } else {
                    productCard.strokeColor = Color.parseColor("#E5E7EB")
                    productCard.strokeWidth = 2
                }
                
                // Set status with Polish label and color
                val (statusLabel, statusColor) = when (product.status) {
                    ProductStatus.IN_STOCK -> "Magazyn" to "#10B981"
                    ProductStatus.ASSIGNED -> "Przypisane" to "#3B82F6"
                    ProductStatus.UNASSIGNED -> "Brak przypisania" to "#6B7280"
                    ProductStatus.IN_REPAIR -> "Serwis" to "#F59E0B"
                    ProductStatus.RETIRED -> "Wycofane" to "#6B7280"
                    ProductStatus.LOST -> "Zaginione" to "#EF4444"
                }
                productStatus.text = statusLabel
                
                // Create rounded background with status color
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.setColor(Color.parseColor(statusColor))
                drawable.cornerRadius = 12f
                productStatus.background = drawable
                
                if (product.createdAt > 0) {
                    productCreatedDate.text = "Dodano: ${dateFormat.format(Date(product.createdAt))}"
                    productCreatedDate.visibility = android.view.View.VISIBLE
                } else {
                    productCreatedDate.visibility = android.view.View.GONE
                }
                
                // Handle clicks
                root.setOnClickListener {
                    if (selectionMode) {
                        selectionCheckbox.isChecked = !selectionCheckbox.isChecked
                    }
                    onItemClick(product)
                }
                
                root.setOnLongClickListener {
                    onItemLongClick(product)
                    true
                }
            }
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
