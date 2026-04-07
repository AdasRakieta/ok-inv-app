package com.example.inventoryapp.ui.warehouse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.databinding.ItemBoxBinding

class BoxesAdapter(
    private val onBoxClick: (BoxEntity) -> Unit
) : ListAdapter<BoxEntity, BoxesAdapter.BoxViewHolder>(BoxDiffCallback()) {

    private var countsMap: Map<Long, Int> = emptyMap()
    private var fullList: List<BoxEntity> = emptyList()
    private var filteredList: List<BoxEntity> = emptyList()

    fun setCountsMap(map: Map<Long, Int>) {
        countsMap = map
        submitList(filteredList.toList())
    }

    fun setFullList(list: List<BoxEntity>) {
        fullList = list
        filteredList = list
        submitList(list.toList())
    }

    fun filterByQuery(query: String) {
        filteredList = if (query.isBlank()) {
            fullList
        } else {
            fullList.filter { box ->
                box.name.contains(query, ignoreCase = true) ||
                    (box.description ?: "").contains(query, ignoreCase = true)
            }
        }
        submitList(filteredList.toList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoxViewHolder {
        val binding = ItemBoxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BoxViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BoxViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BoxViewHolder(private val binding: ItemBoxBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(box: BoxEntity) {
            binding.boxName.text = box.name
            binding.boxCreatedDate.text = android.text.format.DateFormat.getDateFormat(binding.root.context)
                .format(java.util.Date(box.createdAt))
            val ctx = binding.root.context
            binding.boxLocation.text = box.warehouseLocationId?.toString() ?: ctx.getString(com.example.inventoryapp.R.string.no_location)
            binding.boxDescription.text = box.description ?: ""
            val count = countsMap[box.id] ?: 0
            binding.boxProductCount.text = if (count == 0) {
                ctx.getString(com.example.inventoryapp.R.string.no_products)
            } else {
                ctx.resources.getQuantityString(com.example.inventoryapp.R.plurals.products_count, count, count)
            }

            binding.root.setOnClickListener { onBoxClick(box) }
        }
    }
}

class BoxDiffCallback : DiffUtil.ItemCallback<BoxEntity>() {
    override fun areItemsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem == newItem
}
