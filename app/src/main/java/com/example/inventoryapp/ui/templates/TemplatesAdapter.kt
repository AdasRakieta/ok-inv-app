package com.example.inventoryapp.ui.templates

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.databinding.ItemTemplateBinding

class TemplatesAdapter(
    private val onItemClick: (ProductTemplateEntity) -> Unit,
    private val onUseTemplate: (ProductTemplateEntity) -> Unit,
    private val onEditTemplate: (ProductTemplateEntity) -> Unit,
    private val onDeleteTemplate: (ProductTemplateEntity) -> Unit,
    private val getCategoryName: (Long?) -> String,
    private val getCategoryIcon: (Long?) -> String
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
            binding.apply {
                templateName.text = template.name
                categoryName.text = getCategoryName(template.categoryId)
                categoryIcon.text = getCategoryIcon(template.categoryId)
                manufacturer.text = template.defaultManufacturer ?: "-"

                root.setOnClickListener {
                    onItemClick(template)
                }

                useTemplateButton.setOnClickListener {
                    onUseTemplate(template)
                }

                moreButton.setOnClickListener {
                    showPopupMenu(template)
                }
            }
        }

        private fun showPopupMenu(template: ProductTemplateEntity) {
            val popup = PopupMenu(binding.root.context, binding.moreButton)
            popup.menuInflater.inflate(R.menu.template_item_menu, popup.menu)
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_edit -> {
                        onEditTemplate(template)
                        true
                    }
                    R.id.action_delete -> {
                        onDeleteTemplate(template)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private class TemplateDiffCallback : DiffUtil.ItemCallback<ProductTemplateEntity>() {
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
