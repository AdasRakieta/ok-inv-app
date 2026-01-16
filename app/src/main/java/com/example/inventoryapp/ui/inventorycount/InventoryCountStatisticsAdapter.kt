package com.example.inventoryapp.ui.inventorycount

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemInventoryCountStatBinding

data class InventoryCountStatistic(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val count: Int
)

class InventoryCountStatisticsAdapter : ListAdapter<InventoryCountStatistic, InventoryCountStatisticsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemInventoryCountStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemInventoryCountStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: InventoryCountStatistic) {
            binding.categoryIconText.text = stat.categoryIcon
            binding.categoryNameText.text = stat.categoryName
            binding.categoryCountText.text = stat.count.toString()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<InventoryCountStatistic>() {
        override fun areItemsTheSame(oldItem: InventoryCountStatistic, newItem: InventoryCountStatistic): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: InventoryCountStatistic, newItem: InventoryCountStatistic): Boolean {
            return oldItem == newItem
        }
    }
}
