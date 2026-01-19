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
    private val getCategoryName: (Long?) -> String = { _ -> "-" },
    private val getCategoryIcon: (Long?) -> String = { _ -> "📦" }
) : ListAdapter<ProductEntity, ProductsAdapter.ViewHolder>(DiffCallback()) {

    companion object {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, getCategoryName, getCategoryIcon)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemProductBinding,
        private val onItemClick: (ProductEntity) -> Unit,
        private val getCategoryName: (Long?) -> String,
        private val getCategoryIcon: (Long?) -> String
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.apply {
                productName.text = product.name
                productSerialNumber.text = product.serialNumber
                productCategory.text = getCategoryName(product.categoryId)
                categoryIcon.text = getCategoryIcon(product.categoryId)
                
                // Set status with Polish label and color
                val (statusLabel, statusColor) = when (product.status) {
                    ProductStatus.IN_STOCK -> "Magazyn" to "#10B981"
                    ProductStatus.ASSIGNED -> "Przypisane" to "#3B82F6"
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
                
                root.setOnClickListener {
                    onItemClick(product)
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
