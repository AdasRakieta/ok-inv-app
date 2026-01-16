package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        setupButtons()
        loadProducts()
    }
    
    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product ->
                // Navigate to product details
                val action = ProductsListFragmentDirections.actionProductsToDetails(product.id)
                findNavController().navigate(action)
            }
        )
        
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productsRecyclerView.adapter = adapter
    }
    
    private fun setupButtons() {
        // FAB - Add product
        binding.addProductFab.setOnClickListener {
            navigateToAddProduct()
        }
        
        // Empty state add button
        binding.emptyAddButton.setOnClickListener {
            navigateToAddProduct()
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
    }
    
    private fun navigateToAddProduct() {
        val action = ProductsListFragmentDirections.actionProductsToAddEdit(productId = -1L)
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
                    .setTitle("Filtruj według kategorii")
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
            .setTitle("Filtruj według statusu")
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
    
    private fun updateUI(products: List<com.example.inventoryapp.data.local.entities.ProductEntity>) {
        if (products.isEmpty()) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
        }
        
        adapter.submitList(products)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
