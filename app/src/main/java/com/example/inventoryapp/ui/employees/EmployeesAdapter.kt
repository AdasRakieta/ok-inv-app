package com.example.inventoryapp.ui.employees

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.databinding.ItemEmployeeBinding
import com.example.inventoryapp.R

class EmployeesAdapter(
    private val onEmployeeClick: (EmployeeEntity) -> Unit,
    private val onEmployeeLongClick: (EmployeeEntity) -> Boolean = { false }
) : ListAdapter<EmployeeWithStats, EmployeesAdapter.EmployeeViewHolder>(EmployeeDiffCallback()) {

    var selectionMode = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    private val selectedItems = mutableSetOf<Long>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmployeeViewHolder {
        val binding = ItemEmployeeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EmployeeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EmployeeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun toggleSelection(employeeId: Long) {
        if (selectedItems.contains(employeeId)) {
            selectedItems.remove(employeeId)
        } else {
            selectedItems.add(employeeId)
        }
        notifyDataSetChanged()
    }

    fun selectAll() {
        selectedItems.clear()
        selectedItems.addAll(currentList.map { it.employee.id })
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        selectionMode = false
        notifyDataSetChanged()
    }

    fun getSelectedIds(): List<Long> = selectedItems.toList()
    
    fun getSelectedCount(): Int = selectedItems.size

    inner class EmployeeViewHolder(
        private val binding: ItemEmployeeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(employeeWithStats: EmployeeWithStats) {
            val employee = employeeWithStats.employee
            val equipmentCount = employeeWithStats.assignedProductsCount
            
            binding.apply {
                // Show/hide checkbox based on selection mode
                employeeCheckbox.isVisible = selectionMode
                employeeCheckbox.isChecked = selectedItems.contains(employee.id)
                
                // Employee name
                employeeName.text = employee.fullName
                
                // Company + Department
                val subtitleParts = listOfNotNull(
                    employeeWithStats.companyName?.takeIf { it.isNotBlank() },
                    employee.department?.takeIf { it.isNotBlank() }
                )
                employeeDepartment.text = if (subtitleParts.isNotEmpty()) {
                    subtitleParts.joinToString(" • ")
                } else {
                    "Brak przypisanej firmy"
                }
                
                // Assigned equipment count
                assignedCount.text = when (equipmentCount) {
                    0 -> "Brak sprzętu"
                    1 -> "1 urządzenie"
                    in 2..4 -> "$equipmentCount urządzenia"
                    else -> "$equipmentCount urządzeń"
                }
                
                if (equipmentCount == 0) {
                    assignedCountContainer.setBackgroundResource(R.drawable.bg_employee_badge_muted)
                    assignedCountIcon.setImageResource(R.drawable.ic_devices_off)
                    assignedCountIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                        itemView.context.getColor(R.color.text_secondary)
                    )
                    assignedCount.setTextColor(itemView.context.getColor(R.color.text_secondary))
                } else {
                    assignedCountContainer.setBackgroundResource(R.drawable.bg_employee_badge_primary)
                    assignedCountIcon.setImageResource(R.drawable.ic_devices)
                    assignedCountIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                        itemView.context.getColor(R.color.home_primary)
                    )
                    assignedCount.setTextColor(itemView.context.getColor(R.color.home_primary))
                }
                
                // Click listeners
                root.setOnClickListener {
                    if (selectionMode) {
                        toggleSelection(employee.id)
                    } else {
                        onEmployeeClick(employee)
                    }
                }
                
                root.setOnLongClickListener {
                    if (!selectionMode) {
                        onEmployeeLongClick(employee)
                    } else {
                        false
                    }
                }
            }
        }
    }
}

class EmployeeDiffCallback : DiffUtil.ItemCallback<EmployeeWithStats>() {
    override fun areItemsTheSame(oldItem: EmployeeWithStats, newItem: EmployeeWithStats): Boolean {
        return oldItem.employee.id == newItem.employee.id
    }

    override fun areContentsTheSame(oldItem: EmployeeWithStats, newItem: EmployeeWithStats): Boolean {
        return oldItem == newItem
    }
}

// Data class to hold employee with their assigned products count
data class EmployeeWithStats(
    val employee: EmployeeEntity,
    val assignedProductsCount: Int,
    val companyName: String? = null
)
