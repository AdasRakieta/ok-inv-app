package com.example.inventoryapp.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemPackagePreviewBinding
import com.example.inventoryapp.data.local.entities.PackageEntity

class PackagePreviewAdapter : ListAdapter<PackageEntity, PackagePreviewAdapter.PackagePreviewViewHolder>(PackagePreviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagePreviewViewHolder {
        val binding = ItemPackagePreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PackagePreviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackagePreviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PackagePreviewViewHolder(
        private val binding: ItemPackagePreviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(pkg: PackageEntity) {
            binding.packageName.text = pkg.name
            binding.packageStatus.text = pkg.status
        }
    }

    private class PackagePreviewDiffCallback : DiffUtil.ItemCallback<PackageEntity>() {
        override fun areItemsTheSame(oldItem: PackageEntity, newItem: PackageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PackageEntity, newItem: PackageEntity): Boolean {
            return oldItem == newItem
        }
    }
}
