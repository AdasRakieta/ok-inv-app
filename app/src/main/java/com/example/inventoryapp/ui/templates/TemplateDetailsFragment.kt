package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.databinding.FragmentTemplateDetailsBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class TemplateDetailsFragment : Fragment() {

    private var _binding: FragmentTemplateDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TemplateDetailsFragmentArgs by navArgs()

    private val templateRepository by lazy {
        (requireActivity().application as InventoryApplication).productTemplateRepository
    }

    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private var categories: List<CategoryEntity> = emptyList()
    private var leafCategories: List<CategoryEntity> = emptyList()
    private var selectedCategoryId: Long? = null
    private var editingTemplate: ProductTemplateEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplateDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeCategories()
        setupCategoryDropdown()
        setupActions()

        if (args.templateId != 0L) {
            loadTemplate(args.templateId)
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                categories = list
                leafCategories = list.filter { candidate -> list.none { it.parentId == candidate.id } }
                updateCategoryDropdown()
            }
        }
    }

    private fun setupCategoryDropdown() {
        updateCategoryDropdown()
    }

    private fun updateCategoryDropdown() {
        val categoryNames = leafCategories.map { it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categoryNames
        )
        binding.categoryInput.setAdapter(adapter)
        binding.categoryInput.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = leafCategories.getOrNull(position)?.id
        }
    }

    private fun loadTemplate(templateId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getTemplateById(templateId).collect { template ->
                if (template != null) {
                    editingTemplate = template
                    populateFields(template)
                }
            }
        }
    }

    private fun populateFields(template: ProductTemplateEntity) {
        binding.nameInput.setText(template.name)
        binding.manufacturerInput.setText(template.defaultManufacturer)
        binding.modelInput.setText(template.defaultModel)
        binding.descriptionInput.setText(template.defaultDescription)

        selectedCategoryId = template.categoryId
        // If the template's category is a leaf, show it. If it's a parent, try to pick a child under it.
        val leafMatch = leafCategories.firstOrNull { it.id == template.categoryId }
        val categoryName = leafMatch?.name
            ?: categories.firstOrNull { it.parentId == template.categoryId }?.name
            ?: categories.firstOrNull { it.id == template.categoryId }?.name
        binding.categoryInput.setText(categoryName, false)
    }

    private fun setupActions() {
        binding.saveButton.setOnClickListener {
            saveTemplate()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun saveTemplate() {
        val name = binding.nameInput.text.toString().trim()
        val manufacturer = binding.manufacturerInput.text.toString().trim()
        val model = binding.modelInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()

        // Validation
        if (name.isEmpty()) {
            Snackbar.make(binding.root, "Podaj nazwę szablonu", Snackbar.LENGTH_SHORT).show()
            return
        }

        if (selectedCategoryId == null) {
            Snackbar.make(binding.root, "Wybierz kategorię", Snackbar.LENGTH_SHORT).show()
            return
        }

        val categoryId = selectedCategoryId ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            val template = ProductTemplateEntity(
                id = editingTemplate?.id ?: 0L,
                name = name,
                categoryId = categoryId,
                defaultManufacturer = manufacturer.takeIf { it.isNotEmpty() },
                defaultModel = model.takeIf { it.isNotEmpty() },
                defaultDescription = description.takeIf { it.isNotEmpty() }
            )

            if (editingTemplate != null) {
                templateRepository.updateTemplate(template)
                Snackbar.make(binding.root, "Szablon zaktualizowany", Snackbar.LENGTH_SHORT).show()
            } else {
                templateRepository.insertTemplate(template)
                Snackbar.make(binding.root, "Szablon utworzony", Snackbar.LENGTH_SHORT).show()
            }

            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
