package com.example.inventoryapp.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemProductBinding
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.utils.CategoryHelper

data class ProductWithPackage(
    val productEntity: ProductEntity,
    val packageEntity: PackageEntity? = null
)

class ProductsAdapter(
    private val onProductClick: (ProductEntity) -> Unit,
    private val onProductLongClick: (ProductEntity) -> Boolean = { false }
) : ListAdapter<ProductWithPackage, ProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val selectedProducts = mutableSetOf<Long>()
    var selectionMode = false
        private set

    fun toggleSelection(productId: Long) {
        if (selectedProducts.contains(productId)) {
            selectedProducts.remove(productId)
        } else {
            selectedProducts.add(productId)
        }
        notifyDataSetChanged()
    }

    fun getSelectedProducts(): Set<Long> = selectedProducts.toSet()

    fun clearSelection() {
        selectedProducts.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun enterSelectionMode() {
        selectionMode = true
        notifyDataSetChanged()
    }

    fun getSelectedCount(): Int = selectedProducts.size

    fun selectAll(products: List<ProductWithPackage>) {
        products.forEach { selectedProducts.add(it.productEntity.id) }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedProducts.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onProductClick, onProductLongClick, ::toggleSelection, ::isSelected, ::isInSelectionMode)
    }

    private fun isSelected(productId: Long): Boolean = selectedProducts.contains(productId)
    
    private fun isInSelectionMode(): Boolean = selectionMode

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemProductBinding,
        private val onProductClick: (ProductEntity) -> Unit,
        private val onProductLongClick: (ProductEntity) -> Boolean,
        private val onToggleSelection: (Long) -> Unit,
        private val isSelected: (Long) -> Boolean,
        private val isInSelectionMode: () -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(productWithPackage: ProductWithPackage) {
            val product = productWithPackage.productEntity
            val pkg = productWithPackage.packageEntity
            
            // Display product name with quantity if > 1
            binding.productName.text = if (product.quantity > 1) {
                "${product.name} (x${product.quantity})"
            } else {
                product.name
            }
            
            binding.productCategory.text = CategoryHelper.getCategoryName(product.categoryId)
            binding.categoryIcon.text = CategoryHelper.getCategoryIcon(product.categoryId)
            
            // Display package information with status
            if (pkg != null) {
                val statusIcon = when (pkg.status) {
                    "PREPARATION" -> "📦"
                    "READY" -> "✅"
                    "SHIPPED" -> "🚚"
                    "DELIVERED" -> "📬"
                    "RETURNED" -> "↩️"
                    "ISSUED" -> "🔖"
                    "WAREHOUSE" -> "🏬"
                    else -> "❓"
                }
                val statusText = when (pkg.status) {
                    "PREPARATION" -> "Preparation"
                    "READY" -> "Ready"
                    "SHIPPED" -> "Shipped"
                    "DELIVERED" -> "Delivered"
                    "RETURNED" -> "Returned"
                    "ISSUED" -> "Issued"
                    "WAREHOUSE" -> "Warehouse"
                    else -> pkg.status
                }
                binding.packageInfo.text = "$statusIcon ${pkg.name} - $statusText"
                binding.packageInfo.visibility = View.VISIBLE
            } else {
                binding.packageInfo.text = "❓ Unassigned"
                binding.packageInfo.visibility = View.VISIBLE
            }
            
            if (product.serialNumber != null) {
                binding.serialNumberContainer.visibility = View.VISIBLE
                binding.noSerialNumber.visibility = View.GONE
                binding.productSerialNumber.text = product.serialNumber
            } else {
                binding.serialNumberContainer.visibility = View.GONE
                binding.noSerialNumber.visibility = View.VISIBLE
            }

            // Display created date
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val createdDate = dateFormat.format(java.util.Date(product.createdAt))
            binding.productCreatedDate.text = "Created: $createdDate"

            // Selection mode styling
            if (isInSelectionMode()) {
                if (isSelected(product.id)) {
                    binding.root.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, R.color.selection_highlight)
                    )
                } else {
                    binding.root.setBackgroundColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                    )
                }
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                )
            }

            binding.root.setOnClickListener {
                if (isInSelectionMode()) {
                    onToggleSelection(product.id)
                } else {
                    onProductClick(product)
                }
            }

            binding.root.setOnLongClickListener {
                onProductLongClick(product)
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<ProductWithPackage>() {
        override fun areItemsTheSame(oldItem: ProductWithPackage, newItem: ProductWithPackage): Boolean {
            return oldItem.productEntity.id == newItem.productEntity.id
        }

        override fun areContentsTheSame(oldItem: ProductWithPackage, newItem: ProductWithPackage): Boolean {
            return oldItem == newItem
        }
    }
}
