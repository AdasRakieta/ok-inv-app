package com.example.inventoryapp.ui.contractors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.ContractorEntity
import com.example.inventoryapp.databinding.ItemContractorBinding

class ContractorsAdapter(
    private val onContractorClick: (ContractorEntity) -> Unit
) : ListAdapter<ContractorEntity, ContractorsAdapter.ContractorViewHolder>(ContractorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractorViewHolder {
        val binding = ItemContractorBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ContractorViewHolder(binding, onContractorClick)
    }

    override fun onBindViewHolder(holder: ContractorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContractorViewHolder(
        private val binding: ItemContractorBinding,
        private val onContractorClick: (ContractorEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contractor: ContractorEntity) {
            binding.nameTextView.text = contractor.name

            binding.root.setOnClickListener { onContractorClick(contractor) }
        }
    }

    class ContractorDiffCallback : DiffUtil.ItemCallback<ContractorEntity>() {
        override fun areItemsTheSame(oldItem: ContractorEntity, newItem: ContractorEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ContractorEntity, newItem: ContractorEntity): Boolean {
            return oldItem == newItem
        }
    }
}