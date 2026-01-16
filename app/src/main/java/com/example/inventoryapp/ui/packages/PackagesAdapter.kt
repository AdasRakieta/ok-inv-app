package com.example.inventoryapp.ui.packages

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemPackageBinding
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.data.local.dao.PackageWithCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// UI wrapper for PackageWithCount that includes contractor info
data class PackageWithCountAndContractor(
    val packageWithCount: PackageWithCount,
    val contractor: ContractorEntity? = null
)

class PackagesAdapter(
    private val onPackageClick: (PackageEntity) -> Unit,
    private val onPackageLongClick: (PackageEntity) -> Boolean
) : ListAdapter<PackageWithCountAndContractor, PackagesAdapter.PackageViewHolder>(PackageDiffCallback()) {

    private val selectedPackages = mutableSetOf<Long>()
    var selectionMode = false
        private set

    fun toggleSelection(packageId: Long) {
        if (selectedPackages.contains(packageId)) {
            selectedPackages.remove(packageId)
        } else {
            selectedPackages.add(packageId)
        }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPackages.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun enterSelectionMode() {
        selectionMode = true
        notifyDataSetChanged()
    }

    fun getSelectedPackages(): Set<Long> = selectedPackages.toSet()

    fun getSelectedCount(): Int = selectedPackages.size

    fun selectAll(packages: List<PackageWithCountAndContractor>) {
        packages.forEach { selectedPackages.add(it.packageWithCount.packageEntity.id) }
        notifyDataSetChanged()
    }

    fun deselectAll() {
        selectedPackages.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val binding = ItemPackageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackageViewHolder(binding, onPackageClick, onPackageLongClick, ::isSelected)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position), selectionMode)
    }

    private fun isSelected(packageId: Long): Boolean = selectedPackages.contains(packageId)

    class PackageViewHolder(
        private val binding: ItemPackageBinding,
        private val onPackageClick: (PackageEntity) -> Unit,
        private val onPackageLongClick: (PackageEntity) -> Boolean,
        private val isSelected: (Long) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PackageWithCountAndContractor, selectionMode: Boolean) {
            val pkg = item.packageWithCount.packageEntity
            
            binding.packageName.text = pkg.name
            binding.packageStatus.text = pkg.status
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            binding.packageDate.text = "Created on ${dateFormat.format(Date(pkg.createdAt))}"
            
            // Product count
            val count = item.packageWithCount.productCount
            binding.productCount.text = if (count == 1) "1 product" else "$count products"
            
            // Contractor
            binding.packageContractor.text = item.contractor?.name ?: "No contractor"

            // Handle selection state
            val isItemSelected = isSelected(pkg.id)
            if (isItemSelected) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.selection_highlight)
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.card_background)
                )
            }

            binding.root.setOnClickListener {
                if (selectionMode) {
                    onPackageLongClick(pkg)
                } else {
                    onPackageClick(pkg)
                }
            }

            binding.root.setOnLongClickListener {
                onPackageLongClick(pkg)
            }
        }
    }

    private class PackageDiffCallback : DiffUtil.ItemCallback<PackageWithCountAndContractor>() {
        override fun areItemsTheSame(oldItem: PackageWithCountAndContractor, newItem: PackageWithCountAndContractor): Boolean {
            return oldItem.packageWithCount.packageEntity.id == newItem.packageWithCount.packageEntity.id
        }

        override fun areContentsTheSame(oldItem: PackageWithCountAndContractor, newItem: PackageWithCountAndContractor): Boolean {
            return oldItem == newItem
        }
    }
}
