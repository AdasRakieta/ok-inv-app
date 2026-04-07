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

    fun setCountsMap(map: Map<Long, Int>) {
        countsMap = map
        notifyDataSetChanged()
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
            binding.boxLocation.text = box.warehouseLocationId?.toString() ?: "No location"
            binding.boxDescription.text = box.description ?: ""
            val count = countsMap[box.id] ?: 0
            binding.boxProductCount.text = if (count == 0) "Brak produktów" else "${count} ${if (count==1) "produkt" else "produkty"}"

            binding.root.setOnClickListener { onBoxClick(box) }
        }
    }
}

class BoxDiffCallback : DiffUtil.ItemCallback<BoxEntity>() {
    override fun areItemsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: BoxEntity, newItem: BoxEntity): Boolean = oldItem == newItem
}
