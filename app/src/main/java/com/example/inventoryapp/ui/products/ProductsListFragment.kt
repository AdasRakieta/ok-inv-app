package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import java.util.Locale
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

    private var allProducts: List<ProductEntity> = emptyList()
    private var categories: List<CategoryEntity> = emptyList()
    private var selectedCategoryId: Long? = null
    private var selectedStatus: ProductStatus? = null
    private var sortOption: SortOption = SortOption.NEWEST
    private var searchQuery: String = ""

    private enum class SortOption {
        NEWEST,
        NAME_ASC,
        NAME_DESC,
        STATUS,
        SERIAL
    }

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
        
        setupRecyclerView()
        setupSearch()
        setupActions()
        setupFilters()
        observeCategories()
        loadProducts()
    }
    
    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product ->
                openDetails(product.id)
            },
            getCategoryName = { categoryId -> categoryNameFor(categoryId) },
            getCategoryIcon = { categoryId -> categoryIconFor(categoryId) }
        )
        
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productsRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.addProductFab.setOnClickListener { openAddProduct() }
        binding.emptyAddButton.setOnClickListener { openAddProduct() }
    }

    private fun setupFilters() {
        binding.statsButton.setOnClickListener {
            openStatsDialog()
        }
        binding.filterButton.setOnClickListener {
            openCategoryFilter()
        }
        binding.statusFilterButton.setOnClickListener {
            openStatusFilter()
        }
        binding.sortButton.setOnClickListener {
            openSortDialog()
        }
    }
    
    private fun setupSearch() {
        // Scanner or manual search
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
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
        searchQuery = query
        applyFilters()
    }
    
    private fun loadProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { products ->
                    allProducts = products
                    applyFilters()
                }
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                categoryRepository.getAllCategories().collect { list ->
                    categories = list
                    updateFilterLabels()
                }
            }
        }
    }

    private fun applyFilters() {
        var filtered = allProducts

        selectedCategoryId?.let { id ->
            filtered = filtered.filter { it.categoryId == id }
        }

        selectedStatus?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase(Locale.getDefault())
            filtered = filtered.filter { product ->
                product.name.lowercase(Locale.getDefault()).contains(query) ||
                        product.serialNumber.lowercase(Locale.getDefault()).contains(query)
            }
        }

        val sorted = sortProducts(filtered)
        adapter.submitList(sorted)
        updateEmptyState(sorted.isEmpty())
        updateFilterLabels()
    }

    private fun sortProducts(list: List<ProductEntity>): List<ProductEntity> {
        return when (sortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortOption.STATUS -> list.sortedWith(
                compareBy<ProductEntity> { it.status.ordinal }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
            SortOption.SERIAL -> list.sortedBy { it.serialNumber.lowercase(Locale.getDefault()) }
        }
    }

    private fun openStatsDialog() {
        val total = allProducts.size
        val inStock = allProducts.count { it.status == ProductStatus.IN_STOCK }
        val assigned = allProducts.count { it.status == ProductStatus.ASSIGNED }
        val inRepair = allProducts.count { it.status == ProductStatus.IN_REPAIR }
        val retired = allProducts.count { it.status == ProductStatus.RETIRED }
        val lost = allProducts.count { it.status == ProductStatus.LOST }

        val message = """
            Łącznie produktów: $total
            W magazynie: $inStock
            Przypisane: $assigned
            W serwisie: $inRepair
            Wycofane: $retired
            Zaginione: $lost
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Statystyki produktów")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openCategoryFilter() {
        if (categories.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setMessage("Brak kategorii do filtrowania")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val labels = mutableListOf("Wszystkie kategorie")
        val ids = mutableListOf<Long?>(null)
        categories.forEach { category ->
            labels.add(category.name)
            ids.add(category.id)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filtruj po kategorii")
            .setItems(labels.toTypedArray()) { _, index ->
                selectedCategoryId = ids[index]
                applyFilters()
            }
            .show()
    }

    private fun openStatusFilter() {
        val statuses = listOf<ProductStatus?>(null) + ProductStatus.values().toList()
        val labels = statuses.map { status ->
            status?.let { statusLabel(it) } ?: "Wszystkie statusy"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Filtruj po statusie")
            .setItems(labels) { _, index ->
                selectedStatus = statuses[index]
                applyFilters()
            }
            .show()
    }

    private fun openSortDialog() {
        val options = SortOption.values()
        val labels = arrayOf(
            "Najnowsze",
            "Nazwa A-Z",
            "Nazwa Z-A",
            "Status",
            "Numer seryjny"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Sortuj")
            .setSingleChoiceItems(labels, options.indexOf(sortOption)) { dialog, which ->
                sortOption = options[which]
                applyFilters()
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun statusLabel(status: ProductStatus): String {
        return when (status) {
            ProductStatus.IN_STOCK -> "Magazyn"
            ProductStatus.ASSIGNED -> "Przypisane"
            ProductStatus.IN_REPAIR -> "Serwis"
            ProductStatus.RETIRED -> "Wycofane"
            ProductStatus.LOST -> "Zaginione"
        }
    }

    private fun updateFilterLabels() {
        val categoryText = selectedCategoryId?.let { id ->
            categories.firstOrNull { it.id == id }?.name
        } ?: "Kategoria"
        binding.filterButton.text = categoryText

        val statusText = selectedStatus?.let { statusLabel(it) } ?: "Status"
        binding.statusFilterButton.text = statusText

        val sortDescription = when (sortOption) {
            SortOption.NEWEST -> "Najnowsze"
            SortOption.NAME_ASC -> "Nazwa A-Z"
            SortOption.NAME_DESC -> "Nazwa Z-A"
            SortOption.STATUS -> "Status"
            SortOption.SERIAL -> "Nr seryjny"
        }
        binding.sortButton.contentDescription = "Sortowanie: $sortDescription"
        TooltipCompat.setTooltipText(binding.sortButton, sortDescription)
    }
    
    private fun categoryNameFor(categoryId: Long?): String {
        if (categoryId == null) return "-"
        return categories.firstOrNull { it.id == categoryId }?.name ?: "-"
    }
    
    private fun categoryIconFor(categoryId: Long?): String {
        if (categoryId == null) return "📦"
        return categories.firstOrNull { it.id == categoryId }?.icon ?: "📦"
    }
    
    private fun openDetails(productId: Long) {
        findNavController().navigate(
            ProductsListFragmentDirections.actionProductsToDetails(productId)
        )
    }

    private fun openAddProduct() {
        findNavController().navigate(
            ProductsListFragmentDirections.actionProductsToAdd()
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
