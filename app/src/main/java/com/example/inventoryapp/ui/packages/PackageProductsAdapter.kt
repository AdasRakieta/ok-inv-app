package com.example.inventoryapp.ui.packages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemPackageProductBinding
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.utils.CategoryHelper

class PackageProductsAdapter(
    private val onRemoveClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, PackageProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemPackageProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onRemoveClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemPackageProductBinding,
        private val onRemoveClick: (ProductEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.categoryIcon.text = CategoryHelper.getCategoryIcon(product.categoryId)
            binding.productName.text = product.name
            
            if (product.serialNumber != null) {
                binding.productSerialNumber.visibility = View.VISIBLE
                binding.productSerialNumber.text = "SN: ${product.serialNumber}"
            } else {
                binding.productSerialNumber.visibility = View.GONE
            }

            binding.removeButton.setOnClickListener {
                onRemoveClick(product)
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<ProductEntity>() {
        override fun areItemsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProductEntity, newItem: ProductEntity): Boolean {
            return oldItem == newItem
        }
    }
}
