package com.example.inventoryapp.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemFilterOptionBinding

class FilterOptionsAdapter(
    private val options: List<FilterOption>,
    private val onOptionClick: (FilterOption) -> Unit
) : RecyclerView.Adapter<FilterOptionsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFilterOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount() = options.size

    inner class ViewHolder(
        private val binding: ItemFilterOptionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(option: FilterOption) {
            binding.optionLabel.text = option.label
            binding.optionIcon.text = option.icon
            binding.selectedIcon.visibility = if (option.isSelected) View.VISIBLE else View.GONE

            if (option.isSelected) {
                binding.optionCard.strokeColor = ContextCompat.getColor(
                    binding.root.context,
                    R.color.primary
                )
                binding.optionCard.setCardBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.primary_light)
                )
            } else {
                binding.optionCard.strokeColor = android.graphics.Color.TRANSPARENT
                binding.optionCard.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#F9FAFB")
                )
            }

            binding.root.setOnClickListener {
                it.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        it.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start()
                        onOptionClick(option)
                    }
                    .start()
            }
        }
    }
}
