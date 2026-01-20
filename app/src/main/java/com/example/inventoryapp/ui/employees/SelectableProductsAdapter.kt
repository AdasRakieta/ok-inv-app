package com.example.inventoryapp.ui.employees

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.ItemSelectableProductBinding

class SelectableProductsAdapter(
    private val onProductClick: (ProductEntity) -> Unit
) : ListAdapter<SelectableProductItem, SelectableProductsAdapter.ViewHolder>(DiffCallback()) {

    private val selectedProducts = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSelectableProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), selectedProducts.contains(getItem(position).product.id))
    }

    fun toggleSelection(productId: Long) {
        if (selectedProducts.contains(productId)) {
            selectedProducts.remove(productId)
        } else {
            selectedProducts.add(productId)
        }
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedProducts.clear()
        selectedProducts.addAll(currentList.map { it.product.id })
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedProducts.clear()
        notifyDataSetChanged()
    }

    fun getSelectedProducts(): List<ProductEntity> {
        return currentList.filter { selectedProducts.contains(it.product.id) }
            .map { it.product }
    }

    fun getSelectedCount(): Int = selectedProducts.size

    class ViewHolder(
        private val binding: ItemSelectableProductBinding,
        private val onProductClick: (ProductEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelectableProductItem, isSelected: Boolean) {
            binding.apply {
                productCheckbox.isChecked = isSelected
                categoryIcon.text = item.categoryIcon
                productName.text = item.product.name
                productCategory.text = item.product.categoryName
                
                productSerialNumber.isVisible = item.product.serialNumber?.isNotEmpty() == true
                productSerialNumber.text = "S/N: ${item.product.serialNumber}"

                root.setOnClickListener {
                    onProductClick(item.product)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SelectableProductItem>() {
        override fun areItemsTheSame(
            oldItem: SelectableProductItem,
            newItem: SelectableProductItem
        ): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(
            oldItem: SelectableProductItem,
            newItem: SelectableProductItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}

data class SelectableProductItem(
    val product: ProductEntity,
    val categoryIcon: String
)
