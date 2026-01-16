package com.example.inventoryapp.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemCategoryStatBinding

data class CategoryStatistic(
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val count: Int
)

class CategoryStatisticsAdapter : ListAdapter<CategoryStatistic, CategoryStatisticsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemCategoryStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: CategoryStatistic) {
            binding.categoryIconText.text = stat.categoryIcon
            binding.categoryNameText.text = stat.categoryName
            binding.categoryCountText.text = stat.count.toString()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CategoryStatistic>() {
        override fun areItemsTheSame(oldItem: CategoryStatistic, newItem: CategoryStatistic): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: CategoryStatistic, newItem: CategoryStatistic): Boolean {
            return oldItem == newItem
        }
    }
}
