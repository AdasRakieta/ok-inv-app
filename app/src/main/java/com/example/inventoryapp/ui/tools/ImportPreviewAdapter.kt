package com.example.inventoryapp.ui.tools

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.*

sealed class ImportPreviewItem {
    abstract val id: String
    abstract var isSelected: Boolean
    
    data class ProductItem(
        val product: ProductEntity,
        val isNew: Boolean,
        override var isSelected: Boolean = true
    ) : ImportPreviewItem() {
        override val id: String get() = "product_${product.serialNumber}"
    }
    
    data class PackageItem(
        val packageEntity: PackageEntity,
        val isNew: Boolean,
        override var isSelected: Boolean = true
    ) : ImportPreviewItem() {
        override val id: String get() = "package_${packageEntity.id}"
    }
    
    data class TemplateItem(
        val template: ProductTemplateEntity,
        val isNew: Boolean,
        override var isSelected: Boolean = true
    ) : ImportPreviewItem() {
        override val id: String get() = "template_${template.name}"
    }
    
    data class ContractorItem(
        val contractor: ContractorEntity,
        val isNew: Boolean,
        override var isSelected: Boolean = true
    ) : ImportPreviewItem() {
        override val id: String get() = "contractor_${contractor.id}"
    }
    
    data class BoxItem(
        val box: BoxEntity,
        val isNew: Boolean,
        override var isSelected: Boolean = true
    ) : ImportPreviewItem() {
        override val id: String get() = "box_${box.id}"
    }
}

class ImportPreviewAdapter : ListAdapter<ImportPreviewItem, ImportPreviewAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.checkbox)
        val typeBadge: TextView = view.findViewById(R.id.typeBadge)
        val title: TextView = view.findViewById(R.id.title)
        val serialNumber: TextView = view.findViewById(R.id.serialNumber)
        val description: TextView = view.findViewById(R.id.description)
        val relations: TextView = view.findViewById(R.id.relations)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_import_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        
        holder.checkbox.isChecked = item.isSelected
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
        }
        
        when (item) {
            is ImportPreviewItem.ProductItem -> {
                val badge = if (item.isNew) "NEW Product" else "UPDATE Product"
                holder.typeBadge.text = badge
                holder.typeBadge.setBackgroundColor(
                    holder.itemView.context.getColor(
                        if (item.isNew) android.R.color.holo_green_dark 
                        else android.R.color.holo_orange_dark
                    )
                )
                
                holder.title.text = item.product.name
                
                if (!item.product.serialNumber.isNullOrBlank()) {
                    holder.serialNumber.visibility = View.VISIBLE
                    holder.serialNumber.text = "SN: ${item.product.serialNumber}"
                } else {
                    holder.serialNumber.visibility = View.GONE
                }
                
                if (!item.product.description.isNullOrBlank()) {
                    holder.description.visibility = View.VISIBLE
                    holder.description.text = item.product.description
                } else {
                    holder.description.visibility = View.GONE
                }
                
                holder.relations.visibility = View.GONE
            }
            
            is ImportPreviewItem.PackageItem -> {
                val badge = if (item.isNew) "NEW Package" else "UPDATE Package"
                holder.typeBadge.text = badge
                holder.typeBadge.setBackgroundColor(
                    holder.itemView.context.getColor(
                        if (item.isNew) android.R.color.holo_blue_dark 
                        else android.R.color.holo_purple
                    )
                )
                
                holder.title.text = item.packageEntity.name
                holder.serialNumber.visibility = View.GONE
                holder.description.visibility = View.GONE
                holder.relations.visibility = View.GONE
            }
            
            is ImportPreviewItem.TemplateItem -> {
                holder.typeBadge.text = "NEW Template"
                holder.typeBadge.setBackgroundColor(
                    holder.itemView.context.getColor(android.R.color.holo_blue_light)
                )
                
                holder.title.text = item.template.name
                holder.serialNumber.visibility = View.GONE
                
                if (!item.template.description.isNullOrBlank()) {
                    holder.description.visibility = View.VISIBLE
                    holder.description.text = item.template.description
                } else {
                    holder.description.visibility = View.GONE
                }
                
                holder.relations.visibility = View.GONE
            }
            
            is ImportPreviewItem.ContractorItem -> {
                val badge = if (item.isNew) "NEW Contractor" else "UPDATE Contractor"
                holder.typeBadge.text = badge
                holder.typeBadge.setBackgroundColor(
                    holder.itemView.context.getColor(
                        if (item.isNew) android.R.color.holo_red_dark 
                        else android.R.color.holo_red_light
                    )
                )
                
                holder.title.text = item.contractor.name
                holder.serialNumber.visibility = View.GONE
                holder.description.visibility = View.GONE
                holder.relations.visibility = View.GONE
            }
            
            is ImportPreviewItem.BoxItem -> {
                val badge = if (item.isNew) "NEW Box" else "UPDATE Box"
                holder.typeBadge.text = badge
                holder.typeBadge.setBackgroundColor(
                    holder.itemView.context.getColor(
                        if (item.isNew) android.R.color.holo_purple 
                        else android.R.color.darker_gray
                    )
                )
                
                holder.title.text = item.box.name
                holder.serialNumber.visibility = View.GONE
                
                if (!item.box.location.isNullOrBlank()) {
                    holder.description.visibility = View.VISIBLE
                    holder.description.text = "Location: ${item.box.location}"
                } else {
                    holder.description.visibility = View.GONE
                }
                
                holder.relations.visibility = View.GONE
            }
        }
        
        holder.itemView.setOnClickListener {
            holder.checkbox.toggle()
        }
    }

    fun getSelectedItems(): List<ImportPreviewItem> {
        return currentList.filter { it.isSelected }
    }

    fun selectAll() {
        currentList.forEach { it.isSelected = true }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        currentList.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    class DiffCallback : DiffUtil.ItemCallback<ImportPreviewItem>() {
        override fun areItemsTheSame(oldItem: ImportPreviewItem, newItem: ImportPreviewItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ImportPreviewItem, newItem: ImportPreviewItem): Boolean {
            return oldItem == newItem
        }
    }
}