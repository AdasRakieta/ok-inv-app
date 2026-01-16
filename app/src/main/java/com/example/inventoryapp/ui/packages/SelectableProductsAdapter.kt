package com.example.inventoryapp.ui.packages

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemSelectableProductBinding
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.utils.CategoryHelper

class SelectableProductsAdapter(
    private val onSelectionChanged: (Set<Long>) -> Unit
) : ListAdapter<ProductEntity, SelectableProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private val selectedProducts = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemSelectableProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position), selectedProducts.contains(getItem(position).id))
    }

    fun getSelectedProductIds(): Set<Long> = selectedProducts.toSet()

    fun selectAll() {
        selectedProducts.clear()
        currentList.forEach { product ->
            selectedProducts.add(product.id)
        }
        onSelectionChanged(selectedProducts)
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedProducts.clear()
        onSelectionChanged(selectedProducts)
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(
        private val binding: ItemSelectableProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity, isSelected: Boolean) {
            binding.categoryIcon.text = CategoryHelper.getCategoryIcon(product.categoryId)
            binding.productName.text = product.name
            binding.productCategory.text = CategoryHelper.getCategoryName(product.categoryId)
            
            if (product.serialNumber != null) {
                binding.productSerialNumber.visibility = View.VISIBLE
                binding.productSerialNumber.text = "SN: ${product.serialNumber}"
            } else {
                binding.productSerialNumber.visibility = View.GONE
            }

            binding.productCheckbox.isChecked = isSelected
            
            binding.productCheckbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedProducts.add(product.id)
                } else {
                    selectedProducts.remove(product.id)
                }
                onSelectionChanged(selectedProducts)
            }

            binding.root.setOnClickListener {
                binding.productCheckbox.isChecked = !binding.productCheckbox.isChecked
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
