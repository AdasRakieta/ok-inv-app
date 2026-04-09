package com.example.inventoryapp.ui.products

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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
    private val onOptionsClick: (android.view.View, ProductEntity) -> Unit = { _, _ -> },
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

    // Auxiliary mappings to display box / location names in the list
    private var boxesMap: Map<Long, String> = emptyMap()
    private var boxLocationMap: Map<Long, String> = emptyMap()
    private var locationsMap: Map<Long, String> = emptyMap()

    fun setBoxesMap(map: Map<Long, String>) {
        boxesMap = map
        notifyDataSetChanged()
    }

    fun setBoxLocationMap(map: Map<Long, String>) {
        boxLocationMap = map
        notifyDataSetChanged()
    }

    fun setLocationsMap(map: Map<Long, String>) {
        locationsMap = map
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
        return ViewHolder(binding, onItemClick, onItemLongClick, onOptionsClick, getCategoryName, getCategoryIcon)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = getItem(position)
        val isSelected = selectedItems.contains(product.id)
        holder.bind(product, selectionMode, isSelected)
    }

    inner class ViewHolder(
        private val binding: ItemProductBinding,
        private val onItemClick: (ProductEntity) -> Unit,
        private val onItemLongClick: (ProductEntity) -> Unit,
        private val onOptionsClick: (android.view.View, ProductEntity) -> Unit,
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
                    ProductStatus.CONTRACTOR -> "Wydane do kontrahenta" to "#8B5CF6"
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

                // Show location / box info only when item is in stock
                if (product.status == ProductStatus.IN_STOCK) {
                    var infoText = ""
                    if (product.boxId != null) {
                        val boxName = boxesMap[product.boxId] ?: ""
                        val boxLocation = boxLocationMap[product.boxId] ?: ""
                        if (boxLocation.isNotBlank()) {
                            infoText = "Magazyn - $boxLocation → $boxName"
                        } else {
                            val shelfLoc = (product.shelf ?: "").trim()
                            val binLoc = (product.bin ?: "").trim()
                            val shelfText = if (shelfLoc.isNotBlank()) shelfLoc + if (binLoc.isNotBlank()) " / $binLoc" else "" else ""
                            infoText = if (shelfText.isNotBlank()) "Magazyn - $shelfText → $boxName" else "Karton: $boxName"
                        }
                    } else {
                        val shelfLoc = (product.shelf ?: "").trim()
                        val binLoc = (product.bin ?: "").trim()
                        val shelfText = if (shelfLoc.isNotBlank()) shelfLoc + if (binLoc.isNotBlank()) " / $binLoc" else "" else ""
                        val locFromMap = product.warehouseLocationId?.let { locationsMap[it] } ?: ""
                        val finalLoc = if (shelfText.isNotBlank()) shelfText else locFromMap
                        if (finalLoc.isNotBlank()) infoText = "Magazyn - $finalLoc" else infoText = ""
                    }
                    if (infoText.isNotBlank()) {
                        packageInfo.visibility = android.view.View.VISIBLE
                        packageInfo.text = infoText
                    } else {
                        packageInfo.visibility = android.view.View.GONE
                    }
                } else {
                    packageInfo.visibility = android.view.View.GONE
                }
                
                // Handle clicks
                root.setOnClickListener {
                    if (selectionMode) {
                        selectionCheckbox.isChecked = !selectionCheckbox.isChecked
                    }
                    onItemClick(product)
                }
                // Ensure direct clicks on the checkbox trigger the same selection logic
                selectionCheckbox.setOnClickListener {
                    onItemClick(product)
                }
                
                root.setOnLongClickListener {
                    onItemLongClick(product)
                    true
                }
                productOptions?.isVisible = !selectionMode
                productOptions?.setOnClickListener {
                    onOptionsClick(it, product)
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
