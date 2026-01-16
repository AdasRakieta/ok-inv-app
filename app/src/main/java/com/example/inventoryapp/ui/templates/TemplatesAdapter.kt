package com.example.inventoryapp.ui.templates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemTemplateBinding
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TemplatesAdapter(
    private val onTemplateClick: (ProductTemplateEntity) -> Unit,
    private val onTemplateLongClick: (ProductTemplateEntity) -> Unit
) : ListAdapter<ProductTemplateEntity, TemplatesAdapter.TemplateViewHolder>(TemplateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TemplateViewHolder(
        private val binding: ItemTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(template: ProductTemplateEntity) {
            binding.templateNameText.text = template.name
            binding.templateDescriptionText.text = template.description ?: "No description"
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.templateDateText.text = "Created: ${dateFormat.format(Date(template.createdAt))}"

            binding.root.setOnClickListener {
                onTemplateClick(template)
            }

            binding.root.setOnLongClickListener {
                onTemplateLongClick(template)
                true
            }
        }
    }

    class TemplateDiffCallback : DiffUtil.ItemCallback<ProductTemplateEntity>() {
        override fun areItemsTheSame(
            oldItem: ProductTemplateEntity,
            newItem: ProductTemplateEntity
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ProductTemplateEntity,
            newItem: ProductTemplateEntity
        ): Boolean = oldItem == newItem
    }
}
