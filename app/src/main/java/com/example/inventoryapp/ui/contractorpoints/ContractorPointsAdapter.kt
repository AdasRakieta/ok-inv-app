package com.example.inventoryapp.ui.contractorpoints

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
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
    private val onPointClick: (ContractorPointEntity) -> Unit,
    private val onPointLongClick: (ContractorPointEntity) -> Unit = {},
    private val onOptionsClick: (ContractorPointEntity, View) -> Unit = { _, _ -> }
) : ListAdapter<ContractorPointListItem, ContractorPointsAdapter.ContractorPointViewHolder>(
    ContractorPointDiffCallback()
) {

    private val selectedItems = mutableSetOf<Long>()
    var selectionMode = false
        set(value) {
            if (!value) selectedItems.clear()
            field = value
            notifyDataSetChanged()
        }

    fun getSelectedItems(): Set<Long> = selectedItems.toSet()

    fun getSelectedCount(): Int = selectedItems.size

    fun selectAll() {
        currentList.forEach { selectedItems.add(it.contractorPoint.id) }
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun toggleSelection(pointId: Long) {
        if (selectedItems.contains(pointId)) selectedItems.remove(pointId) else selectedItems.add(pointId)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractorPointViewHolder {
        val binding = ItemContractorPointBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContractorPointViewHolder(binding, onPointClick, onPointLongClick, onOptionsClick)
    }

    override fun onBindViewHolder(holder: ContractorPointViewHolder, position: Int) {
        val point = getItem(position).contractorPoint
        val isSelected = selectedItems.contains(point.id)
        holder.bind(getItem(position), selectionMode, isSelected)
    }

    inner class ContractorPointViewHolder(
        private val binding: ItemContractorPointBinding,
        private val onPointClick: (ContractorPointEntity) -> Unit,
        private val onPointLongClick: (ContractorPointEntity) -> Unit,
        private val onOptionsClick: (ContractorPointEntity, View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ContractorPointListItem, selectionMode: Boolean, isSelected: Boolean) {
            val point = item.contractorPoint
            val context = binding.root.context

            binding.pointName.text = point.name
            binding.pointMarketNumber.text = point.marketNumber ?: ""
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

            // Selection UI
            binding.selectionCheckbox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            binding.selectionCheckbox.isChecked = isSelected

            if (selectionMode && isSelected) {
                binding.pointCard.strokeColor = Color.parseColor("#3B82F6")
                binding.pointCard.strokeWidth = 4
            } else {
                binding.pointCard.strokeColor = Color.parseColor("#E5E7EB")
                binding.pointCard.strokeWidth = 2
            }

            binding.root.setOnClickListener {
                if (selectionMode) {
                    binding.selectionCheckbox.isChecked = !binding.selectionCheckbox.isChecked
                }
                onPointClick(point)
            }

            binding.selectionCheckbox.setOnClickListener {
                onPointClick(point)
            }

            binding.root.setOnLongClickListener {
                onPointLongClick(point)
                true
            }

            binding.pointOptions.setOnClickListener { v -> onOptionsClick(point, v) }
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
