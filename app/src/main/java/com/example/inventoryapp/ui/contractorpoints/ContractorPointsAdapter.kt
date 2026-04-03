package com.example.inventoryapp.ui.contractorpoints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import com.example.inventoryapp.databinding.ItemContractorPointBinding

data class ContractorPointListItem(
    val contractorPoint: ContractorPointEntity,
    val companyName: String
)

class ContractorPointsAdapter(
    private val onPointClick: (ContractorPointEntity) -> Unit
) : ListAdapter<ContractorPointListItem, ContractorPointsAdapter.ContractorPointViewHolder>(
    ContractorPointDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractorPointViewHolder {
        val binding = ItemContractorPointBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContractorPointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContractorPointViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ContractorPointViewHolder(
        private val binding: ItemContractorPointBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContractorPointListItem) {
            val point = item.contractorPoint
            val context = binding.root.context

            binding.pointName.text = point.name
            binding.pointCode.text = point.code
            binding.pointTypeBadge.text = point.pointType.name
            binding.pointCompany.text = item.companyName

            binding.pointLocationStatus.text = when {
                !point.city.isNullOrBlank() && !point.address.isNullOrBlank() ->
                    "${point.city} • ${point.address}"
                !point.city.isNullOrBlank() -> point.city
                !point.address.isNullOrBlank() -> point.address
                else -> context.getString(R.string.contractor_points_location_missing)
            }

            val (accentColorRes, badgeBackgroundRes) = when (point.pointType) {
                PointType.CP -> R.color.home_primary to R.drawable.bg_employee_badge_primary
                PointType.CC -> R.color.success to R.drawable.bg_employee_badge_muted
                PointType.DC -> R.color.warning to R.drawable.bg_employee_badge_muted
            }

            binding.pointAccent.setBackgroundColor(ContextCompat.getColor(context, accentColorRes))
            binding.pointTypeBadge.setBackgroundResource(badgeBackgroundRes)
            binding.pointTypeBadge.setTextColor(ContextCompat.getColor(context, accentColorRes))

            binding.root.setOnClickListener { onPointClick(point) }
        }
    }
}

private class ContractorPointDiffCallback : DiffUtil.ItemCallback<ContractorPointListItem>() {
    override fun areItemsTheSame(
        oldItem: ContractorPointListItem,
        newItem: ContractorPointListItem
    ): Boolean = oldItem.contractorPoint.id == newItem.contractorPoint.id

    override fun areContentsTheSame(
        oldItem: ContractorPointListItem,
        newItem: ContractorPointListItem
    ): Boolean = oldItem == newItem
}
