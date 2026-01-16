package com.example.inventoryapp.ui.boxes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.ItemProductSelectableBinding
import com.example.inventoryapp.utils.CategoryHelper

/**
 * RecyclerView adapter for selectable products with checkboxes
 */
class SelectableProductsAdapter(
    private val onProductToggle: (Long) -> Unit,
    private val selectedProductIds: Set<Long>
) : ListAdapter<ProductEntity, SelectableProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private var currentSelectedIds = selectedProductIds.toSet()

    fun updateSelectedIds(selectedIds: Set<Long>) {
        currentSelectedIds = selectedIds
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductSelectableBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(
        private val binding: ItemProductSelectableBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.productName.text = product.name
            binding.productSerialNumber.text = "SN: ${product.serialNumber ?: "N/A"}"

            // Display category
            val category = CategoryHelper.getCategoryById(product.categoryId)
            binding.categoryIconText.text = category?.icon ?: "📦"
            binding.categoryNameText.text = "Category: ${category?.name ?: "Unknown"}"

            val isSelected = currentSelectedIds.contains(product.id)
            binding.productCheckbox.isChecked = isSelected

            // Visual feedback for selection
            binding.root.strokeWidth = if (isSelected) 3 else 0
            binding.root.strokeColor = if (isSelected) {
                ContextCompat.getColor(binding.root.context, R.color.primary)
            } else {
                ContextCompat.getColor(binding.root.context, android.R.color.transparent)
            }

            // Click listeners
            binding.root.setOnClickListener {
                onProductToggle(product.id)
            }

            binding.productCheckbox.setOnClickListener {
                onProductToggle(product.id)
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
