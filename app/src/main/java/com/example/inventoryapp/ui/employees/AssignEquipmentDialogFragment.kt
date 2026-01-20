package com.example.inventoryapp.ui.employees

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.DialogAssignEquipmentBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssignEquipmentDialogFragment(
    private val employeeId: Long,
    private val onProductsAssigned: (List<ProductEntity>) -> Unit
) : DialogFragment() {

    private var _binding: DialogAssignEquipmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SelectableProductsAdapter
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository
    
    private var allProducts = listOf<SelectableProductItem>()
    private var filteredProducts = listOf<SelectableProductItem>()
    private var currentCategoryFilter: String? = null
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAssignEquipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        productRepository = app.productRepository

        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        setupFilterButtons()
        setupActionButtons()
        loadProducts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectableProductsAdapter { product ->
            adapter.toggleSelection(product.id)
            updateSelectedCount()
        }

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AssignEquipmentDialogFragment.adapter
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { text ->
            currentSearchQuery = text.toString()
            filterProducts()
        }
    }

    private fun setupFilterButtons() {
        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }

        binding.statusFilterButton.setOnClickListener {
            // Status filter can be added if needed
        }

        binding.selectAllButton.setOnClickListener {
            if (adapter.getSelectedCount() == filteredProducts.size && filteredProducts.isNotEmpty()) {
                adapter.clearSelection()
                binding.selectAllButton.text = "Zaznacz wszystkie"
            } else {
                adapter.selectAll()
                binding.selectAllButton.text = "Odznacz wszystkie"
            }
            updateSelectedCount()
        }
    }

    private fun setupActionButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.assignButton.setOnClickListener {
            val selectedProducts = adapter.getSelectedProducts()
            if (selectedProducts.isNotEmpty()) {
                onProductsAssigned(selectedProducts)
                dismiss()
            }
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            val products = productRepository.getAllProducts().first()
            val availableProducts = products.filter { it.assignedToEmployeeId == null }
            val categoryRepository = (requireActivity().application as InventoryApplication).categoryRepository
            val categories = categoryRepository.getAllCategories().first()
            val categoryMap = categories.associateBy { it.id }

            allProducts = availableProducts.map { product ->
                val categoryName = categoryMap[product.categoryId]?.name ?: "Inne"
                SelectableProductItem(
                    product = product,
                    categoryIcon = getCategoryIcon(categoryName),
                    categoryLabel = categoryName
                )
            }

            filteredProducts = allProducts
            adapter.submitList(filteredProducts)
            updateEmptyState()
        }
    }

    private fun filterProducts() {
        filteredProducts = allProducts.filter { item ->
            val matchesSearch = if (currentSearchQuery.isBlank()) {
                true
            } else {
                item.product.name.contains(currentSearchQuery, ignoreCase = true) ||
                        item.product.serialNumber?.contains(currentSearchQuery, ignoreCase = true) == true ||
                        item.categoryLabel.contains(currentSearchQuery, ignoreCase = true)
            }

            val matchesCategory = if (currentCategoryFilter == null || currentCategoryFilter == "Wszystkie") {
                true
            } else {
                item.categoryLabel == currentCategoryFilter
            }

            matchesSearch && matchesCategory
        }

        adapter.submitList(filteredProducts)
        updateEmptyState()
        updateSelectAllButton()
    }

    private fun showCategoryFilterDialog() {
        lifecycleScope.launch {
            val categories = allProducts.map { it.categoryLabel }.distinct().sorted()
            val items = (listOf("Wszystkie") + categories).toTypedArray()
            val currentIndex = items.indexOf(currentCategoryFilter ?: "Wszystkie")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Wybierz kategorię")
                .setSingleChoiceItems(items, currentIndex) { dialog, which ->
                    currentCategoryFilter = if (which == 0) null else items[which]
                    binding.filterButton.text = items[which]
                    filterProducts()
                    dialog.dismiss()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    private fun updateSelectedCount() {
        val count = adapter.getSelectedCount()
        binding.selectedCountText.text = "Zaznaczono: $count"
        binding.assignButton.text = "Przypisz ($count)"
        binding.assignButton.isEnabled = count > 0
        updateSelectAllButton()
    }

    private fun updateSelectAllButton() {
        binding.selectAllButton.text = if (adapter.getSelectedCount() == filteredProducts.size && filteredProducts.isNotEmpty()) {
            "Odznacz wszystkie"
        } else {
            "Zaznacz wszystkie"
        }
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.isVisible = filteredProducts.isEmpty()
        binding.productsRecyclerView.isVisible = filteredProducts.isNotEmpty()
    }

    private fun getCategoryIcon(categoryName: String): String {
        return when (categoryName.lowercase()) {
            "laptop" -> "💻"
            "monitor" -> "🖥️"
            "klawiatura" -> "⌨️"
            "mysz" -> "🖱️"
            "drukarka" -> "🖨️"
            "skaner" -> "📠"
            "telefon" -> "📱"
            "tablet" -> "📱"
            "router" -> "📡"
            "switch" -> "🔀"
            "ups" -> "🔋"
            "projektor" -> "📽️"
            "kamera" -> "📷"
            "słuchawki" -> "🎧"
            "głośnik" -> "🔊"
            "dysk" -> "💾"
            "stacja dokująca" -> "🔌"
            else -> "📦"
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
