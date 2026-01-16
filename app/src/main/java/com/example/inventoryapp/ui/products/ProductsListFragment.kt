package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProductsListFragment : Fragment() {

    private var _binding: FragmentProductsListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProductsViewModel
    private lateinit var adapter: ProductsAdapter
    private val fabOffset by lazy { resources.getDimension(R.dimen.selection_panel_fab_spacing) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        val productRepository = app.productRepository
        val packageRepository = app.packageRepository
        val factory = ProductsViewModelFactory(productRepository, packageRepository)
        val vm: ProductsViewModel by viewModels { factory }
        viewModel = vm
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
        setupClickListeners()
        setupSearchBar()
        setupFilterAndSort()
        observeProducts()
    }

    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onProductClick = { product ->
                val action = ProductsListFragmentDirections
                    .actionProductsToProductDetails(product.id)
                findNavController().navigate(action)
            },
            onProductLongClick = { product ->
                adapter.enterSelectionMode()
                adapter.toggleSelection(product.id)
                updateSelectionUI()
                true
            }
        )
        
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProductsListFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addProductFab.setOnClickListener {
            if (adapter.selectionMode) {
                exitSelectionMode()
            } else {
                findNavController().navigate(R.id.action_products_to_add_product)
            }
        }
        
        binding.emptyAddButton.setOnClickListener {
            findNavController().navigate(R.id.action_products_to_add_product)
        }

        binding.statsButton.setOnClickListener {
            showCategoryStatisticsDialog()
        }

        binding.selectAllButton.setOnClickListener {
            val totalCount = adapter.itemCount
            val selectedCount = adapter.getSelectedCount()
            
            if (selectedCount == totalCount) {
                adapter.deselectAll()
            } else {
                adapter.selectAll(adapter.currentList)
            }
            updateSelectionUI()
        }

        binding.deleteSelectedButton.setOnClickListener {
            if (adapter.getSelectedCount() > 0) {
                showDeleteConfirmationDialog()
            }
        }
    }

    private fun updateSelectionUI() {
        if (adapter.selectionMode) {
            val count = adapter.getSelectedCount()
            val totalCount = adapter.itemCount
            
            // Show selection panel
            binding.selectionPanel.visibility = View.VISIBLE
            binding.selectionCountText.text = "$count selected"
            
            // Update Select All button text
            binding.selectAllButton.text = if (count == totalCount) "Deselect All" else "Select All"
            
            // Change FAB to cancel icon
            binding.addProductFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            
            // Move FAB up to avoid overlapping with selection panel
            binding.addProductFab.animate()
                .translationY(-binding.selectionPanel.height.toFloat() - fabOffset)
                .setDuration(200)
                .start()
        } else {
            // Hide selection panel
            binding.selectionPanel.visibility = View.GONE
            
            // Restore FAB to add icon
            binding.addProductFab.setImageResource(android.R.drawable.ic_input_add)
            
            // Move FAB back to original position
            binding.addProductFab.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val count = adapter.getSelectedCount()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Products")
            .setMessage("Are you sure you want to delete $count selected product(s)?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedProducts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedProducts() {
        val selectedIds = adapter.getSelectedProducts()
        viewLifecycleOwner.lifecycleScope.launch {
            selectedIds.forEach { productId ->
                viewModel.deleteProduct(productId)
            }
            Toast.makeText(
                requireContext(),
                "Deleted ${selectedIds.size} product(s)",
                Toast.LENGTH_SHORT
            ).show()
            exitSelectionMode()
        }
    }

    private fun exitSelectionMode() {
        adapter.clearSelection()
        updateSelectionUI()
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupFilterAndSort() {
        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }
        
        binding.statusFilterButton.setOnClickListener {
            showPackageStatusFilterDialog()
        }
        
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }
    
    private fun showCategoryFilterDialog() {
        val categories = CategoryHelper.getAllCategories()
        val categoryNames = categories.map { "${it.icon} ${it.name}" }.toTypedArray()
        val selectedCategoryIds = viewModel.selectedCategoryIds.value.toMutableSet()
        val checkedItems = BooleanArray(categories.size) { index ->
            categories[index].id in selectedCategoryIds
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Categories")
            .setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
                val categoryId = categories[which].id
                if (isChecked) {
                    selectedCategoryIds.add(categoryId)
                } else {
                    selectedCategoryIds.remove(categoryId)
                }
            }
            .setPositiveButton("Apply") { _, _ ->
                viewModel.setCategoryFilters(selectedCategoryIds)
            }
            .setNeutralButton("Clear") { _, _ ->
                viewModel.setCategoryFilters(emptySet())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showPackageStatusFilterDialog() {
        val statuses = CategoryHelper.PackageStatus.FILTER_STATUSES.toList()
        val statusNames = statuses.map { status ->
            CategoryHelper.PackageStatus.getDisplayName(status)
        }.toTypedArray()
        
        val selectedStatuses = viewModel.selectedPackageStatuses.value.toMutableSet()
        val checkedItems = BooleanArray(statuses.size) { index ->
            statuses[index] in selectedStatuses
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Package Status")
            .setMultiChoiceItems(statusNames, checkedItems) { _, which, isChecked ->
                val status = statuses[which]
                if (isChecked) {
                    selectedStatuses.add(status)
                } else {
                    selectedStatuses.remove(status)
                }
            }
            .setPositiveButton("Apply") { _, _ ->
                viewModel.setPackageStatusFilters(selectedStatuses)
            }
            .setNeutralButton("Clear") { _, _ ->
                viewModel.setPackageStatusFilters(emptySet())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Name (A-Z)",
            "Name (Z-A)",
            "Newest First",
            "Oldest First",
            "By Category"
        )
        
        val currentSortOrder = viewModel.sortOrder.value
        val currentSelection = when (currentSortOrder) {
            ProductSortOrder.NAME_ASC -> 0
            ProductSortOrder.NAME_DESC -> 1
            ProductSortOrder.DATE_NEWEST -> 2
            ProductSortOrder.DATE_OLDEST -> 3
            ProductSortOrder.CATEGORY -> 4
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Products")
            .setSingleChoiceItems(sortOptions, currentSelection) { dialog, which ->
                val selectedSortOrder = when (which) {
                    0 -> ProductSortOrder.NAME_ASC
                    1 -> ProductSortOrder.NAME_DESC
                    2 -> ProductSortOrder.DATE_NEWEST
                    3 -> ProductSortOrder.DATE_OLDEST
                    4 -> ProductSortOrder.CATEGORY
                    else -> ProductSortOrder.NAME_ASC
                }
                viewModel.setSortOrder(selectedSortOrder)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.products.collect { products ->
                if (products.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.productsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.productsRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(products)
                }
            }
        }
    }

    private fun showCategoryStatisticsDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val statistics = viewModel.getCategoryStatistics()
                
                val dialogView = layoutInflater.inflate(R.layout.dialog_category_statistics, null)
                val dialog = AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .create()
                
                val recyclerView = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.categoryStatsRecyclerView)
                val totalCountText = dialogView.findViewById<android.widget.TextView>(R.id.totalCountText)
                val closeButton = dialogView.findViewById<android.widget.Button>(R.id.closeButton)
                
                val statsAdapter = CategoryStatisticsAdapter()
                recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
                recyclerView.adapter = statsAdapter
                statsAdapter.submitList(statistics)
                
                val totalCount = statistics.sumOf { it.count }
                totalCountText.text = totalCount.toString()
                
                closeButton.setOnClickListener {
                    dialog.dismiss()
                }
                
                dialog.show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading statistics: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
