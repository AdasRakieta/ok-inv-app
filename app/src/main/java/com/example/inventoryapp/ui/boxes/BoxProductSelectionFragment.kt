package com.example.inventoryapp.ui.boxes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.databinding.FragmentBoxProductSelectionBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.ui.packages.SelectableProductsAdapter
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class BoxProductSelectionFragment : Fragment() {

    private var _binding: FragmentBoxProductSelectionBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: BoxProductSelectionViewModel
    private lateinit var adapter: SelectableProductsAdapter
    private val args: BoxProductSelectionFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val productRepository = ProductRepository(database.productDao())
        val boxRepository = BoxRepository(database.boxDao(), database.productDao(), database.packageDao())
        val factory = BoxProductSelectionViewModelFactory(
            productRepository,
            boxRepository,
            args.boxId
        )
        val vm: BoxProductSelectionViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoxProductSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupClickListeners()
        observeAvailableProducts()
    }

    private fun setupRecyclerView() {
        adapter = SelectableProductsAdapter { selectedIds ->
            // No need for selected count in this view
        }
        
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BoxProductSelectionFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.setSearchQuery(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }
    }

    private fun showCategoryFilterDialog() {
        val categories = listOf(
            CategoryHelper.Category(0, "All", "🗂️", requiresSerialNumber = false)
        ) + CategoryHelper.getAllCategories()
        
        val categoryNames = categories.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
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

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.addNewButton.setOnClickListener {
            val action = BoxProductSelectionFragmentDirections
                .actionBoxProductSelectionToAddProduct(boxId = args.boxId)
            findNavController().navigate(action)
        }
        
        binding.addSelectedButton.setOnClickListener {
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
                val results = viewModel.addProductsToBox(selectedIds)
                
                // Count successful additions
                val successCount = results.count { it is AddProductResult.Success || it is AddProductResult.TransferredFromPackage }
                val errorMessages = results.filterIsInstance<AddProductResult.Error>().map { it.message }
                val blockedMessages = results.filterIsInstance<AddProductResult.AlreadyInActivePackage>()
                    .map { "This device is in package: ${it.packageName} with status: ${it.status}" }
                
                // Show success message
                if (successCount > 0) {
                    Toast.makeText(
                        requireContext(),
                        "$successCount product(s) added to box",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Show transfer messages
                results.filterIsInstance<AddProductResult.TransferredFromPackage>().forEach { result ->
                    Toast.makeText(
                        requireContext(),
                        "Product transferred from package: ${result.packageName}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Show blocking messages
                blockedMessages.forEach { message ->
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
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
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.productsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateText.visibility = View.GONE
                    binding.productsRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(products)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
