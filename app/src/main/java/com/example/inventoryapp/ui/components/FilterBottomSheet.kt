package com.example.inventoryapp.ui.components

import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.databinding.BottomSheetFilterBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

object FilterBottomSheet {
    fun show(
        fragment: Fragment,
        title: String,
        options: List<FilterOption>,
        onOptionSelected: (FilterOption) -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(fragment.requireContext())
        val binding = BottomSheetFilterBinding.inflate(fragment.layoutInflater)
        binding.sheetTitle.text = title

        val listAdapter = FilterOptionsAdapter(options) { option ->
            bottomSheet.dismiss()
            onOptionSelected(option)
        }

        binding.optionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(fragment.requireContext())
            adapter = listAdapter
        }

        bottomSheet.setContentView(binding.root)
        bottomSheet.show()
    }
}
