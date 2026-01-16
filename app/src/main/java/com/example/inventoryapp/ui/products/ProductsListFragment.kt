package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProductsListFragment : Fragment() {

    private var _binding: FragmentProductsListBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }

    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private lateinit var adapter: ProductsAdapter

    private var currentCategoryFilter: Long? = null
    private var currentStatusFilter: ProductStatus? = null
    private val selectedIds = mutableSetOf<Long>()
    private var selectionMode = false
    private var categoryNames: Map<Long, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupButtons()
        observeCategories()
        loadProducts()
    }

    private fun setupToolbar() {
        binding.productsToolbar.subtitle = "Przegl¹d sprzêtu"
        binding.productsToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_add_product -> {
                    openAddEdit(-1L)
                    true
                }

                R.id.menu_stats -> {
                    showStatsDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product ->
                if (selectionMode) {
                    toggleSelection(product.id, !selectedIds.contains(product.id))
                } else {
                    openDetails(product.id)
                }
            },
            onEditClick = { product -> openAddEdit(product.id) },
            onDeleteClick = { product -> confirmDeleteSingle(product) },
            onToggleSelection = { product, shouldSelect ->
                if (shouldSelect && !selectionMode) {
                    enableSelectionMode()
                }
                toggleSelection(product.id, shouldSelect)
            }
        )

        binding.productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productsRecyclerView.adapter = adapter
    }

    private fun setupButtons() {
        // FAB - Add product
        binding.addProductFab.setOnClickListener {
            openAddEdit(-1L)
        }

        // Empty state add button
        binding.emptyAddButton.setOnClickListener {
            openAddEdit(-1L)
        }

        // Stats button
        binding.statsButton.setOnClickListener {
            showStatsDialog()
        }

        // Category filter
        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }

        // Status filter
        binding.statusFilterButton.setOnClickListener {
            showStatusFilterDialog()
        }

        // Sort button (placeholder for now)
        binding.sortButton.setOnClickListener {
            // TODO: Implement sorting
        }

        binding.selectAllButton.setOnClickListener {
            selectAllVisible()
        }

        binding.deleteSelectedButton.setOnClickListener {
            confirmDeleteSelected()
        }
    }

    private fun setupSearch() {
        // Scanner or manual search
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                performSearch(binding.searchEditText.text.toString().trim())
                true
            } else {
                false
            }
        }

        // Enter key handling for scanner
        binding.searchEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                performSearch(binding.searchEditText.text.toString().trim())
                true
            } else {
                false
            }
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            loadProducts()
            return
        }

        lifecycleScope.launch {
            productRepository.searchProducts(query).collect { products ->
                updateUI(products)
            }
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            categoryRepository.getAllCategories().collectLatest { categories ->
                categoryNames = categories.associate { it.id to it.name }
                adapter.updateCategoryNames(categoryNames)

                // Reset filter button if selected category disappears
                currentCategoryFilter?.let { selected ->
                    if (!categoryNames.containsKey(selected)) {
                        currentCategoryFilter = null
                        binding.filterButton.text = "Category"
                    }
                }
            }
        }
    }

    private fun loadProducts() {
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { allProducts ->
                var filteredProducts = allProducts

                // Apply category filter
                currentCategoryFilter?.let { categoryId ->
                    filteredProducts = filteredProducts.filter { it.categoryId == categoryId }
                }

                // Apply status filter
                currentStatusFilter?.let { status ->
                    filteredProducts = filteredProducts.filter { it.status == status }
                }

                updateUI(filteredProducts)
            }
        }
    }

    private fun updateUI(products: List<ProductEntity>) {
        if (products.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
        }

        val visibleIds = products.map { it.id }.toSet()
        selectedIds.retainAll(visibleIds)
        selectionMode = selectedIds.isNotEmpty()
        binding.productsToolbar.subtitle = "${products.size} produktów"

        adapter.submitList(products) {
            applySelectionState()
        }
    }

    private fun applySelectionState() {
        binding.selectionPanel.visibility = if (selectionMode) View.VISIBLE else View.GONE
        binding.selectionCountText.text = "${selectedIds.size} zaznaczono"
        adapter.updateSelectionState(selectionMode, selectedIds)
    }

    private fun enableSelectionMode() {
        selectionMode = true
        applySelectionState()
    }

    private fun toggleSelection(productId: Long, shouldSelect: Boolean) {
        if (shouldSelect) {
            selectedIds.add(productId)
        } else {
            selectedIds.remove(productId)
        }

        selectionMode = selectedIds.isNotEmpty()
        applySelectionState()
    }

    private fun clearSelection() {
        selectedIds.clear()
        selectionMode = false
        applySelectionState()
    }

    private fun selectAllVisible() {
        val ids = adapter.currentList.map { it.id }
        selectedIds.clear()
        selectedIds.addAll(ids)
        selectionMode = selectedIds.isNotEmpty()
        applySelectionState()
    }

    private fun confirmDeleteSingle(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuñ produkt")
            .setMessage("Na pewno chcesz usun¹æ ${product.name}?")
            .setPositiveButton("Usuñ") { _, _ ->
                lifecycleScope.launch {
                    productRepository.deleteProductById(product.id)
                    Toast.makeText(requireContext(), "Produkt usuniêty", Toast.LENGTH_SHORT).show()
                    clearSelection()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun confirmDeleteSelected() {
        if (selectedIds.isEmpty()) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuñ zaznaczone")
            .setMessage("Usun¹æ ${selectedIds.size} wybranych produktów?")
            .setPositiveButton("Usuñ") { _, _ ->
                lifecycleScope.launch {
                    selectedIds.forEach { productRepository.deleteProductById(it) }
                    Toast.makeText(requireContext(), "Usuniêto zaznaczone", Toast.LENGTH_SHORT).show()
                    clearSelection()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun openAddEdit(productId: Long) {
        val action = ProductsListFragmentDirections.actionProductsToAddEdit(productId)
        findNavController().navigate(action)
    }

    private fun openDetails(productId: Long) {
        val action = ProductsListFragmentDirections.actionProductsToDetails(productId)
        findNavController().navigate(action)
    }

    private fun showStatsDialog() {
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                val total = products.size
                val inStock = products.count { it.status == ProductStatus.IN_STOCK }
                val assigned = products.count { it.status == ProductStatus.ASSIGNED }
                val inRepair = products.count { it.status == ProductStatus.IN_REPAIR }
                val retired = products.count { it.status == ProductStatus.RETIRED }

                val message = """
                    Wszystkie produkty: $total

                    Na stanie: $inStock
                    Przypisane: $assigned
                    W naprawie: $inRepair
                    Wycofane: $retired
                """.trimIndent()

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Statystyki produktów")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showCategoryFilterDialog() {
        lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                val items = mutableListOf("Wszystkie kategorie")
                items.addAll(categories.map { it.name })

                val selectedIndex = if (currentCategoryFilter == null) {
                    0
                } else {
                    categories.indexOfFirst { it.id == currentCategoryFilter } + 1
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Filtruj wed³ug kategorii")
                    .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
                        if (which == 0) {
                            currentCategoryFilter = null
                            binding.filterButton.text = "Category"
                        } else {
                            currentCategoryFilter = categories[which - 1].id
                            binding.filterButton.text = categories[which - 1].name
                        }
                        loadProducts()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Anuluj", null)
                    .show()
            }
        }
    }

    private fun showStatusFilterDialog() {
        val statuses = ProductStatus.values()
        val items = mutableListOf("Wszystkie statusy")
        items.addAll(statuses.map { it.name })

        val selectedIndex = if (currentStatusFilter == null) {
            0
        } else {
            statuses.indexOf(currentStatusFilter) + 1
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtruj wed³ug statusu")
            .setSingleChoiceItems(items.toTypedArray(), selectedIndex) { dialog, which ->
                if (which == 0) {
                    currentStatusFilter = null
                    binding.statusFilterButton.text = "Status"
                } else {
                    currentStatusFilter = statuses[which - 1]
                    binding.statusFilterButton.text = statuses[which - 1].name
                }
                loadProducts()
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
