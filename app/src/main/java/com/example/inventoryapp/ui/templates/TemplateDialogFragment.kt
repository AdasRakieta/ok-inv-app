package com.example.inventoryapp.ui.templates

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.DialogTemplateBinding
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.utils.CategoryHelper

class TemplateDialogFragment : DialogFragment() {

    private var _binding: DialogTemplateBinding? = null
    private val binding get() = _binding!!

    private var template: ProductTemplateEntity? = null
    private var onSaveCallback: ((String, Long?, String?) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogTemplateBinding.inflate(LayoutInflater.from(requireContext()))

        template = arguments?.getParcelable(ARG_TEMPLATE)

        setupCategorySpinner()
        populateFields()

        return AlertDialog.Builder(requireContext())
            .setTitle(if (template == null) R.string.template_add else R.string.template_edit)
            .setView(binding.root)
            .setPositiveButton(R.string.action_save) { _, _ ->
                saveTemplate()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .create()
    }

    private fun setupCategorySpinner() {
        val categories = CategoryHelper.getAllCategories()
        val categoryNames = listOf("None") + categories.map { it.name }
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = adapter
    }

    private fun populateFields() {
        template?.let {
            binding.templateNameInput.setText(it.name)
            binding.templateDescriptionInput.setText(it.description ?: "")

            // Set category spinner selection
            it.categoryId?.let { categoryId ->
                val categories = CategoryHelper.getAllCategories()
                val index = categories.indexOfFirst { cat -> cat.id == categoryId }
                if (index >= 0) {
                    binding.categorySpinner.setSelection(index + 1) // +1 because of "None" at index 0
                }
            }
        }
    }

    private fun saveTemplate() {
        val name = binding.templateNameInput.text.toString().trim()
        if (name.isEmpty()) {
            return
        }

        val selectedPosition = binding.categorySpinner.selectedItemPosition
        val categoryId = if (selectedPosition > 0) {
            CategoryHelper.getAllCategories()[selectedPosition - 1].id
        } else {
            null
        }

        val description = binding.templateDescriptionInput.text.toString().trim()
            .takeIf { it.isNotEmpty() }

        onSaveCallback?.invoke(name, categoryId, description)
    }

    fun setOnSaveListener(callback: (String, Long?, String?) -> Unit) {
        onSaveCallback = callback
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TEMPLATE = "template"

        fun newInstance(template: ProductTemplateEntity? = null): TemplateDialogFragment {
            return TemplateDialogFragment().apply {
                arguments = Bundle().apply {
                    template?.let { putParcelable(ARG_TEMPLATE, it) }
                }
            }
        }
    }
}
