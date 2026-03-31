package com.example.inventoryapp.ui.employees

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.DepartmentEntity

class DepartmentsAdapter(
    private var items: List<DepartmentEntity> = emptyList(),
    private val onOptionsClick: (DepartmentEntity, View) -> Unit
) : RecyclerView.Adapter<DepartmentsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.departmentName)
        val options: ImageView = view.findViewById(R.id.departmentOptions)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_department, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val dept = items[position]
        holder.name.text = dept.name
        holder.options.setOnClickListener { v -> onOptionsClick(dept, v) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<DepartmentEntity>) {
        items = list
        notifyDataSetChanged()
    }
}
