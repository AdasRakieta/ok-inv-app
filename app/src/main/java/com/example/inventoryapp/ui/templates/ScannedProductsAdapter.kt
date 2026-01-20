package com.example.inventoryapp.ui.templates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.ItemScannedProductBinding

class ScannedProductsAdapter(
    private val onDeleteClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, ScannedProductsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemScannedProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemScannedProductBinding,
        private val onDeleteClick: (ProductEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.apply {
                productNameText.text = product.name
                serialNumberText.text = "SN: ${product.serialNumber}"
                manufacturerText.text = product.manufacturer ?: "—"
                
                deleteButton.setOnClickListener {
                    onDeleteClick(product)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.serialNumber == newItem.serialNumber
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
