package com.example.inventoryapp.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemProductPreviewBinding
import com.example.inventoryapp.data.local.entities.ProductEntity

class ProductPreviewAdapter : ListAdapter<ProductEntity, ProductPreviewAdapter.ProductPreviewViewHolder>(ProductPreviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductPreviewViewHolder {
        val binding = ItemProductPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductPreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductPreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductPreviewViewHolder(
        private val binding: ItemProductPreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.productName.text = product.name
            binding.productSerial.text = product.serialNumber ?: "No Serial Number"
        }
    }

    private class ProductPreviewDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
