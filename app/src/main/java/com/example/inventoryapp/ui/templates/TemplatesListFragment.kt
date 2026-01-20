package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.databinding.FragmentTemplatesListBinding
import kotlinx.coroutines.launch

class TemplatesListFragment : Fragment() {

    private var _binding: FragmentTemplatesListBinding? = null
    private val binding get() = _binding!!

    private val templateRepository by lazy {
        (requireActivity().application as InventoryApplication).productTemplateRepository
    }

    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private lateinit var adapter: TemplatesAdapter
    private var categories: List<CategoryEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplatesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupActions()
        observeCategories()
        loadTemplates()
    }

    private fun setupRecyclerView() {
        adapter = TemplatesAdapter(
            onItemClick = { template ->
                openTemplateDetails(template.id)
            },
            onUseTemplate = { template ->
                openBulkAddWithTemplate(template.id)
            },
            onEditTemplate = { template ->
                openTemplateDetails(template.id)
            },
            onDeleteTemplate = { template ->
                confirmDeleteTemplate(template)
            },
            getCategoryName = { categoryId -> categoryNameFor(categoryId) },
            getCategoryIcon = { categoryId -> categoryIconFor(categoryId) }
        )

        binding.templatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.templatesRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.addTemplateFab.setOnClickListener {
            openTemplateDetails(0L)
        }
        binding.emptyAddButton.setOnClickListener {
            openTemplateDetails(0L)
        }
    }

    private fun loadTemplates() {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
                adapter.submitList(templates)
                updateEmptyState(templates.isEmpty())
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                categories = list
            }
        }
    }

    private fun categoryNameFor(categoryId: Long?): String {
        if (categoryId == null) return "-"
        return categories.firstOrNull { it.id == categoryId }?.name ?: "-"
    }

    private fun categoryIconFor(categoryId: Long?): String {
        if (categoryId == null) return "📦"
        return categories.firstOrNull { it.id == categoryId }?.icon ?: "📦"
    }

    private fun openTemplateDetails(templateId: Long) {
        findNavController().navigate(
            TemplatesListFragmentDirections.actionTemplatesToDetails(templateId)
        )
    }

    private fun openBulkAddWithTemplate(templateId: Long) {
        findNavController().navigate(
            TemplatesListFragmentDirections.actionTemplatesToBulkAdd(templateId)
        )
    }

    private fun confirmDeleteTemplate(template: ProductTemplateEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Usuń szablon")
            .setMessage("Czy na pewno chcesz usunąć szablon \"${template.name}\"?")
            .setPositiveButton("Usuń") { _, _ ->
                deleteTemplate(template)
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteTemplate(template: ProductTemplateEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.deleteTemplate(template)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.templatesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.templatesRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
