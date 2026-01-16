package com.example.inventoryapp.ui.packages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.databinding.FragmentProductSelectionBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProductSelectionFragment : Fragment() {

    private var _binding: FragmentProductSelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProductSelectionViewModel
    private lateinit var adapter: SelectableProductsAdapter
    private val args: ProductSelectionFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        val productRepository = app.productRepository
        val packageRepository = app.packageRepository
        val factory = ProductSelectionViewModelFactory(
            productRepository,
            packageRepository,
            args.packageId
        )
        val vm: ProductSelectionViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupSearch()
        observeAvailableProducts()
    }

    private fun setupSearch() {
        // SearchView listener
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.setSearchQuery(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // Filter button listener
        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }
    }

    private fun showCategoryFilterDialog() {
        val categories = listOf(
            com.example.inventoryapp.utils.CategoryHelper.Category(0, "All", "🗂️", requiresSerialNumber = false)
        ) + com.example.inventoryapp.utils.CategoryHelper.getAllCategories()
        
        val categoryNames = categories.map { it.name }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Filter by Category")
            .setItems(categoryNames) { _, which: Int ->
                val selectedCategory = categories[which]
                if (selectedCategory.name == "All") {
                    viewModel.setCategoryFilter(null)
                } else {
                    viewModel.setCategoryFilter(selectedCategory.name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupRecyclerView() {
        adapter = SelectableProductsAdapter { selectedIds ->
            updateSelectedCount(selectedIds.size)
        }
        
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ProductSelectionFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addNewButton.setOnClickListener {
            // Navigate to Add Product screen
            val action = ProductSelectionFragmentDirections.actionProductSelectionToAddProduct(args.packageId)
            findNavController().navigate(action)
        }
        
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.addButton.setOnClickListener {
            val selectedIds = adapter.getSelectedProductIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one product",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            viewLifecycleOwner.lifecycleScope.launch {
                val results = viewModel.addProductsToPackage(selectedIds)
                
                // Count successful additions
                val successCount = results.count { it is AddProductResult.Success || it is AddProductResult.TransferredFromBox }
                val errorMessages = results.filterIsInstance<AddProductResult.Error>().map { it.message }
                
                // Show success message
                if (successCount > 0) {
                    Toast.makeText(
                        requireContext(),
                        "$successCount product(s) added to package",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Show transfer messages
                results.filterIsInstance<AddProductResult.TransferredFromBox>().forEach { result ->
                    Toast.makeText(
                        requireContext(),
                        "Product transferred from box: ${result.boxName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Show error messages
                errorMessages.forEach { message ->
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                
                findNavController().navigateUp()
            }
        }
    }

    private fun observeAvailableProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableProducts.collect { products ->
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

    private fun updateSelectedCount(count: Int) {
        binding.selectedCountText.text = if (count == 1) {
            "1 product selected"
        } else {
            "$count products selected"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
