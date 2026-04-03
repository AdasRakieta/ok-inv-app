package com.example.inventoryapp.ui.companies

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.databinding.ItemCompanyBinding

class CompaniesAdapter(
    private val onCompanyClick: (CompanyEntity) -> Unit,
    private val onCompanyLongClick: (CompanyEntity) -> Unit
) : ListAdapter<CompanyEntity, CompaniesAdapter.CompanyViewHolder>(CompanyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompanyViewHolder {
        val binding = ItemCompanyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CompanyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CompanyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CompanyViewHolder(
        private val binding: ItemCompanyBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(company: CompanyEntity) {
            binding.companyName.text = company.name
            binding.companyNip.text = "NIP: ${company.taxId}"
            binding.companyLocation.text = when {
                !company.city.isNullOrBlank() && !company.address.isNullOrBlank() ->
                    "${company.city} • ${company.address}"
                !company.city.isNullOrBlank() -> company.city
                !company.address.isNullOrBlank() -> company.address
                else -> "Brak danych adresowych"
            }
            binding.companyContact.text = when {
                !company.contactPerson.isNullOrBlank() -> company.contactPerson
                !company.email.isNullOrBlank() -> company.email
                !company.phone.isNullOrBlank() -> company.phone
                else -> "Brak danych kontaktowych"
            }

            binding.root.setOnClickListener { onCompanyClick(company) }
            binding.root.setOnLongClickListener {
                onCompanyLongClick(company)
                true
            }
        }
    }
}

private class CompanyDiffCallback : DiffUtil.ItemCallback<CompanyEntity>() {
    override fun areItemsTheSame(oldItem: CompanyEntity, newItem: CompanyEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: CompanyEntity, newItem: CompanyEntity): Boolean {
        return oldItem == newItem
    }
}

