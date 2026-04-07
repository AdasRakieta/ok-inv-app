package com.example.inventoryapp.ui.employees

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.ItemAssignedProductBinding

class AssignedProductsAdapter(
    private val onProductClick: (ProductEntity) -> Unit,
    private val onUnassignClick: (ProductEntity) -> Unit
) : ListAdapter<ProductEntity, AssignedProductsAdapter.ProductViewHolder>(ProductDiffCallback()) {

    private var fullList: List<ProductEntity> = emptyList()
    private var filteredList: List<ProductEntity> = emptyList()
    private var boxesMap: Map<Long, String> = emptyMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemAssignedProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setFullList(list: List<ProductEntity>) {
        fullList = list
        filteredList = list
        submitList(list.toList()) {
            // Callback when list update is complete
        }
    }

    fun setBoxesMap(map: Map<Long, String>) {
        boxesMap = map
        // Refresh visible list to show updated box names
        submitList(filteredList.toList())
    }

    fun filterByQuery(query: String) {
        filteredList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { product ->
                product.name.contains(query, ignoreCase = true) ||
                product.serialNumber.contains(query, ignoreCase = true)
            }
        }
        submitList(filteredList.toList())
    }

    inner class ProductViewHolder(
        private val binding: ItemAssignedProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: ProductEntity) {
            binding.apply {
                productName.text = product.name
                // Append box name when available
                val boxSuffix = product.boxId?.let { id ->
                    boxesMap[id]?.let { name -> " • ${name}" } ?: " • Box#${id}"
                } ?: ""
                productSerial.text = "S/N: ${product.serialNumber}${boxSuffix}"
                
                root.setOnClickListener {
                    onProductClick(product)
                }
                
                unassignButton.setOnClickListener {
                    onUnassignClick(product)
                }
            }
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
