package com.example.inventoryapp.ui.templates

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.databinding.ItemTemplateBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TemplatesAdapter(
    private val onItemClick: (ProductTemplateEntity) -> Unit,
    private val onEditTemplate: (ProductTemplateEntity) -> Unit,
    private val onDeleteTemplate: (ProductTemplateEntity) -> Unit,
    private val getCategoryName: (Long?) -> String,
    private val getCategoryIcon: (Long?) -> String,
    private val getCategoryColor: (Long?) -> String?
) : ListAdapter<ProductTemplateEntity, TemplatesAdapter.TemplateViewHolder>(TemplateDiffCallback()) {

    companion object {
        private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }

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
                manufacturer.text = template.defaultManufacturer ?: "Producent nieustawiony"

                val colorHex = getCategoryColor(template.categoryId)
                if (!colorHex.isNullOrBlank()) {
                    val baseColor = Color.parseColor(colorHex)
                    val bgColor = ColorUtils.setAlphaComponent(baseColor, 36)
                    categoryIconContainer.background?.setTint(bgColor)
                }

                val filledCount = countFilledFields(template)
                templateMeta.text = "$filledCount wypełnionych pól"
                templateUpdated.text = "Zaktualizowano: ${dateFormat.format(Date(template.updatedAt))}"

                root.setOnClickListener {
                    onItemClick(template)
                }

                editButton.setOnClickListener {
                    onEditTemplate(template)
                }

                deleteButton.setOnClickListener {
                    onDeleteTemplate(template)
                }
            }
        }
    }

    private fun countFilledFields(template: ProductTemplateEntity): Int {
        var count = 0
        if (!template.defaultManufacturer.isNullOrBlank()) count += 1
        if (!template.defaultModel.isNullOrBlank()) count += 1
        if (!template.defaultDescription.isNullOrBlank()) count += 1
        if (!template.serialNumberPattern.isNullOrBlank()) count += 1
        if (!template.serialNumberPrefix.isNullOrBlank()) count += 1
        return count
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
