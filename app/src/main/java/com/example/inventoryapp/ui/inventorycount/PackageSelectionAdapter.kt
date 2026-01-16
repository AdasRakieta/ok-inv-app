package com.example.inventoryapp.ui.inventorycount

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemSelectablePackageBinding
import com.example.inventoryapp.data.local.entities.PackageEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackageSelectionAdapter(
    private val onPackageSelected: (PackageEntity) -> Unit
) : ListAdapter<PackageEntity, PackageSelectionAdapter.PackageViewHolder>(PackageDiffCallback()) {

    private var selectedPackageId: Long? = null

    fun setSelectedPackage(packageId: Long?) {
        selectedPackageId = packageId
        notifyDataSetChanged()
    }

    fun getSelectedPackage(): PackageEntity? {
        return currentList.find { it.id == selectedPackageId }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemSelectablePackageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackageViewHolder(binding, ::onPackageClick)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position), selectedPackageId == getItem(position).id)
    }

    private fun onPackageClick(packageEntity: PackageEntity) {
        selectedPackageId = packageEntity.id
        notifyDataSetChanged()
        onPackageSelected(packageEntity)
    }

    class PackageViewHolder(
        private val binding: ItemSelectablePackageBinding,
        private val onPackageClick: (PackageEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(packageEntity: PackageEntity, isSelected: Boolean) {
            binding.packageNameText.text = packageEntity.name
            binding.packageInfoText.text = buildString {
                append(packageEntity.status)
                if (packageEntity.packageCode != null) {
                    append(" • ${packageEntity.packageCode}")
                }
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                append(" • ${dateFormat.format(Date(packageEntity.createdAt))}")
            }

            // Update selection indicator
            binding.selectionIndicator.setImageResource(
                if (isSelected) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
            )

            binding.root.setOnClickListener {
                onPackageClick(packageEntity)
            }
        }
    }

    private class PackageDiffCallback : DiffUtil.ItemCallback<PackageEntity>() {
        override fun areItemsTheSame(oldItem: PackageEntity, newItem: PackageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PackageEntity, newItem: PackageEntity): Boolean {
            return oldItem == newItem
        }
    }
}