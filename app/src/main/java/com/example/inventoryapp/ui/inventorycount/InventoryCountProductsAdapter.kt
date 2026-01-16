package com.example.inventoryapp.ui.inventorycount

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemInventoryCountProductBinding
import com.example.inventoryapp.data.local.entities.ProductWithPackageInfo
import com.example.inventoryapp.utils.CategoryHelper

class InventoryCountProductsAdapter : ListAdapter<ProductWithPackageInfo, InventoryCountProductsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryCountProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemInventoryCountProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(productWithPackage: ProductWithPackageInfo) {
            val product = productWithPackage.product
            val packageInfo = productWithPackage.packageInfo

            val category = CategoryHelper.getCategoryById(product.categoryId)

            binding.productIconText.text = category?.icon ?: "📦"
            binding.productNameText.text = product.name
            binding.productSerialText.text = product.serialNumber ?: "No SN"
            binding.productCategoryText.text = category?.name ?: "Unknown"

            // Package information
            if (packageInfo != null) {
                binding.packageInfoText.text = "📦 ${packageInfo.name}"
                binding.packageInfoText.setTextColor(binding.root.context.getColor(com.example.inventoryapp.R.color.primary))
            } else {
                binding.packageInfoText.text = "📦 Not assigned"
                binding.packageInfoText.setTextColor(binding.root.context.getColor(com.example.inventoryapp.R.color.text_secondary))
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProductWithPackageInfo>() {
        override fun areItemsTheSame(oldItem: ProductWithPackageInfo, newItem: ProductWithPackageInfo): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: ProductWithPackageInfo, newItem: ProductWithPackageInfo): Boolean {
            return oldItem == newItem
        }
    }
}