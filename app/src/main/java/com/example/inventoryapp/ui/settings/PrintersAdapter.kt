package com.example.inventoryapp.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.PrinterEntity
import com.example.inventoryapp.data.models.PrinterModel
import com.example.inventoryapp.databinding.ItemPrinterBinding

/**
 * Adapter for displaying list of printers
 */
class PrintersAdapter(
    private val onPrinterClick: (PrinterEntity) -> Unit,
    private val onSetDefaultClick: (PrinterEntity) -> Unit,
    private val onDeleteClick: (PrinterEntity) -> Unit
) : ListAdapter<PrinterEntity, PrintersAdapter.PrinterViewHolder>(PrinterDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrinterViewHolder {
        val binding = ItemPrinterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrinterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PrinterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PrinterViewHolder(
        private val binding: ItemPrinterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(printer: PrinterEntity) {
            val printerModel = PrinterModel.fromString(printer.model)
            
            binding.printerNameText.text = printer.name
            binding.printerMacText.text = "${printer.macAddress} • ${printerModel.displayName}"
            
            // Show default badge if this is the default printer
            binding.defaultBadge.visibility = if (printer.isDefault) View.VISIBLE else View.GONE
            
            // Click on item opens options (handled by onPrinterClick)
            binding.root.setOnClickListener {
                onPrinterClick(printer)
            }
        }
    }

    private class PrinterDiffCallback : DiffUtil.ItemCallback<PrinterEntity>() {
        override fun areItemsTheSame(oldItem: PrinterEntity, newItem: PrinterEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PrinterEntity, newItem: PrinterEntity): Boolean {
            return oldItem == newItem
        }
    }
}
