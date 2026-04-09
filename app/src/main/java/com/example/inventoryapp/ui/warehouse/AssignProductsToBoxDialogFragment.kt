package com.example.inventoryapp.ui.warehouse

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
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.DialogAssignEquipmentBinding
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.example.inventoryapp.ui.employees.SelectableProductItem
import com.example.inventoryapp.ui.employees.SelectableProductsAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssignProductsToBoxDialogFragment(
    private val boxId: Long,
    private val onProductsAssigned: (List<ProductEntity>) -> Unit
) : DialogFragment() {

    private var _binding: DialogAssignEquipmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SelectableProductsAdapter
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository

    private var allProducts = listOf<SelectableProductItem>()
    private var filteredProducts = listOf<SelectableProductItem>()
    private var currentCategoryFilter: String? = null
    private var currentStatusFilter: ProductStatus? = null
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
        binding.toolbar.title = "Przypisz do kartonu"
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
            adapter = this@AssignProductsToBoxDialogFragment.adapter
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
            showCategoryFilterBottomSheet()
        }

        binding.statusFilterButton.setOnClickListener {
            showStatusFilterBottomSheet()
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

        binding.assignButton.text = "Dodaj (0)"
        binding.assignButton.isEnabled = false
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
            val availableProducts = products.filter { product ->
                product.boxId == null &&
                    product.assignedToEmployeeId == null &&
                    product.assignedToContractorPointId == null
            }

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
            updateFilterLabels()
            updateEmptyState()
            updateSelectedCount()
        }
    }

    private fun filterProducts() {
        filteredProducts = allProducts.filter { item ->
            val matchesSearch = if (currentSearchQuery.isBlank()) {
                true
            } else {
                item.product.name.contains(currentSearchQuery, ignoreCase = true) ||
                    item.product.serialNumber.orEmpty().contains(currentSearchQuery, ignoreCase = true) ||
                    item.categoryLabel.contains(currentSearchQuery, ignoreCase = true)
            }

            val matchesCategory = currentCategoryFilter?.let { category ->
                item.categoryLabel == category
            } ?: true

            val matchesStatus = currentStatusFilter?.let { status ->
                item.product.status == status
            } ?: true

            matchesSearch && matchesCategory && matchesStatus
        }

        adapter.submitList(filteredProducts)
        updateEmptyState()
        updateSelectAllButton()
        updateFilterLabels()
        updateSelectedCount()
    }

    private fun showCategoryFilterBottomSheet() {
        val categories = allProducts.map { it.categoryLabel }.distinct().sorted()
        val options = mutableListOf(
            FilterOption(
                id = "all",
                label = "Wszystkie kategorie",
                icon = "📦",
                isSelected = currentCategoryFilter == null
            )
        )

        categories.forEach { categoryName ->
            options.add(
                FilterOption(
                    id = categoryName,
                    label = categoryName,
                    icon = getCategoryIcon(categoryName),
                    isSelected = currentCategoryFilter == categoryName
                )
            )
        }

        FilterBottomSheet.show(this, "Filtruj po kategorii", options) { option ->
            currentCategoryFilter = if (option.id == "all") null else option.id
            updateFilterLabels()
            filterProducts()
        }
    }

    private fun showStatusFilterBottomSheet() {
        val options = listOf(
            FilterOption(
                id = "all",
                label = "Wszystkie statusy",
                icon = "🔄",
                isSelected = currentStatusFilter == null
            ),
            FilterOption(
                id = ProductStatus.IN_STOCK.name,
                label = "Magazyn",
                icon = "✅",
                isSelected = currentStatusFilter == ProductStatus.IN_STOCK
            ),
            FilterOption(
                id = ProductStatus.UNASSIGNED.name,
                label = "Brak przypisania",
                icon = "📋",
                isSelected = currentStatusFilter == ProductStatus.UNASSIGNED
            ),
            FilterOption(
                id = ProductStatus.IN_REPAIR.name,
                label = "Serwis",
                icon = "🔧",
                isSelected = currentStatusFilter == ProductStatus.IN_REPAIR
            ),
            FilterOption(
                id = ProductStatus.RETIRED.name,
                label = "Wycofane",
                icon = "📁",
                isSelected = currentStatusFilter == ProductStatus.RETIRED
            ),
            FilterOption(
                id = ProductStatus.LOST.name,
                label = "Zaginione",
                icon = "❓",
                isSelected = currentStatusFilter == ProductStatus.LOST
            )
        )

        FilterBottomSheet.show(this, "Filtruj po statusie", options) { option ->
            currentStatusFilter = if (option.id == "all") null else ProductStatus.valueOf(option.id)
            updateFilterLabels()
            filterProducts()
        }
    }

    private fun updateFilterLabels() {
        binding.filterButton.text = currentCategoryFilter ?: "Kategoria"
        binding.statusFilterButton.text = currentStatusFilter?.let { statusLabel(it) } ?: "Status"
    }

    private fun statusLabel(status: ProductStatus): String {
        return when (status) {
            ProductStatus.IN_STOCK -> "Magazyn"
            ProductStatus.ASSIGNED -> "Przypisane"
            ProductStatus.UNASSIGNED -> "Brak przypisania"
            ProductStatus.IN_REPAIR -> "Serwis"
            ProductStatus.RETIRED -> "Wycofane"
            ProductStatus.LOST -> "Zaginione"
        }
    }

    private fun updateSelectedCount() {
        val count = adapter.getSelectedCount()
        binding.selectedCountText.text = "Zaznaczono: $count"
        binding.assignButton.text = "Dodaj ($count)"
        binding.assignButton.isEnabled = count > 0
        updateSelectAllButton()
    }

    private fun updateSelectAllButton() {
        binding.selectAllButton.text = if (
            adapter.getSelectedCount() == filteredProducts.size && filteredProducts.isNotEmpty()
        ) {
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
