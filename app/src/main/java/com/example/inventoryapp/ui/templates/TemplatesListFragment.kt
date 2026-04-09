package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.example.inventoryapp.ui.components.FilterOptionsAdapter
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
    private var allTemplates: List<ProductTemplateEntity> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategoryId: Long? = null
    private var isFabMenuOpen = false

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
                openBulkAddWithTemplate(template.id)
            },
            onEditTemplate = { template ->
                openTemplateDetails(template.id)
            },
            onDeleteTemplate = { template ->
                confirmDeleteTemplate(template)
            },
            getCategoryName = { categoryId -> categoryNameFor(categoryId) },
            getCategoryIcon = { categoryId -> categoryIconFor(categoryId) },
            getCategoryColor = { categoryId -> categoryColorFor(categoryId) }
        )

        binding.templatesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.templatesRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.addTemplateFab.setOnClickListener {
            toggleFabMenu()
        }
        binding.emptyAddButton.setOnClickListener {
            openTemplateDetails(0L)
        }
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.searchInput.doAfterTextChanged { text ->
            searchQuery = text?.toString()?.trim().orEmpty()
            applyFilters()
        }
        binding.filterButton.setOnClickListener {
            openCategoryFilter()
        }
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        // Capture views locally to avoid referencing `binding` from animation callbacks
        val overlay = _binding?.templatesFabMenuOverlay ?: return
        val singleCard = _binding?.templatesSingleAddCard ?: return
        val bulkCard = _binding?.templatesBulkAddCard ?: return

        isFabMenuOpen = true

        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        showCard(singleCard, 0)
        showCard(bulkCard, 50)

        overlay.setOnClickListener { closeFabMenu() }

        singleCard.setOnClickListener {
            closeFabMenu()
            openTemplateDetails(0L)
        }

        bulkCard.setOnClickListener {
            closeFabMenu()
            Toast.makeText(requireContext(), "Import szablonów: funkcja w przygotowaniu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closeFabMenu() {
        // Capture views locally to avoid dereferencing cleared binding inside animation callbacks
        val overlay = _binding?.templatesFabMenuOverlay ?: return
        val singleCard = _binding?.templatesSingleAddCard ?: return
        val bulkCard = _binding?.templatesBulkAddCard ?: return

        isFabMenuOpen = false

        overlay.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction { overlay.visibility = View.GONE }
            .start()

        hideCard(singleCard)
        hideCard(bulkCard)

        // Show global FAB again — none in this fragment
    }

    private fun showCard(card: View, delay: Long) {
        card.visibility = View.VISIBLE
        card.alpha = 0f
        card.translationY = 20f
        card.scaleX = 0.8f
        card.scaleY = 0.8f

        card.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setStartDelay(delay)
            .start()
    }

    private fun hideCard(card: View) {
        card.animate()
            .alpha(0f)
            .translationY(20f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction { card.visibility = View.GONE }
            .start()
    }

    private fun loadTemplates() {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
                allTemplates = templates
                applyFilters()
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                categories = list
                applyFilters()
            }
        }
    }

    private fun openCategoryFilter() {
        val options = mutableListOf(
            FilterOption("all", "Wszystkie", "📦", selectedCategoryId == null)
        )

        categories.forEach { category ->
            options.add(
                FilterOption(
                    category.id.toString(),
                    category.name,
                    category.icon ?: "📦",
                    selectedCategoryId == category.id
                )
            )
        }

        FilterBottomSheet.show(this, "Filtruj po kategorii", options) { selectedOption ->
            selectedCategoryId = if (selectedOption.id == "all") {
                null
            } else {
                selectedOption.id.toLongOrNull()
            }
            applyFilters()
        }
    }

    private fun applyFilters() {
        var filtered = allTemplates

        selectedCategoryId?.let { id ->
            val isParent = categories.any { it.parentId == id }
            filtered = if (isParent) {
                val childIds = categories.filter { it.parentId == id }.map { it.id }
                filtered.filter { it.categoryId == id || it.categoryId in childIds }
            } else {
                filtered.filter { it.categoryId == id }
            }
        }

        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase()
            filtered = filtered.filter { template ->
                template.name.lowercase().contains(query) ||
                    (template.defaultManufacturer?.lowercase()?.contains(query) == true) ||
                    (template.defaultModel?.lowercase()?.contains(query) == true)
            }
        }

        adapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun categoryNameFor(categoryId: Long?): String {
        if (categoryId == null) return "-"
        return categories.firstOrNull { it.id == categoryId }?.name ?: "-"
    }

    private fun categoryIconFor(categoryId: Long?): String {
        if (categoryId == null) return "📦"
        return categories.firstOrNull { it.id == categoryId }?.icon ?: "📦"
    }

    private fun categoryColorFor(categoryId: Long?): String? {
        if (categoryId == null) return null
        return categories.firstOrNull { it.id == categoryId }?.color
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
