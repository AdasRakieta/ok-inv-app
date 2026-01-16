package com.example.inventoryapp.ui.boxes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentAddBoxBinding
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fragment for creating a new box and selecting products.
 */
class AddBoxFragment : Fragment() {

    private var _binding: FragmentAddBoxBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddBoxViewModel by viewModels {
        AddBoxViewModelFactory(
            (requireActivity().application as InventoryApplication).boxRepository,
            (requireActivity().application as InventoryApplication).productRepository
        )
    }

    private lateinit var adapter: SelectableProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBoxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputListeners()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupInputListeners() {
        binding.boxNameInput.doAfterTextChanged { text ->
            viewModel.setBoxName(text?.toString() ?: "")
        }

        binding.boxDescriptionInput.doAfterTextChanged { text ->
            viewModel.setBoxDescription(text?.toString() ?: "")
        }

        binding.warehouseLocationInput.doAfterTextChanged { text ->
            viewModel.setWarehouseLocation(text?.toString() ?: "")
        }

        // Search input listener
        binding.searchInput.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectableProductsAdapter(
            onProductToggle = { productId ->
                viewModel.toggleProductSelection(productId)
            },
            selectedProductIds = emptySet()
        )

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AddBoxFragment.adapter
        }
    }

    private fun setupClickListeners() {
        // Save FAB
        binding.saveFab.setOnClickListener {
            viewModel.createBox()
        }

        // Filter button - show category filter dialog
        binding.filterButton.setOnClickListener {
            showCategoryFilterDialog()
        }

        // Select All Products button
        binding.selectAllProductsButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val selectedCount = viewModel.selectedProductIds.value.size
                val filteredCount = viewModel.filteredProducts.value.size
                
                if (selectedCount >= filteredCount && filteredCount > 0) {
                    viewModel.deselectAll()
                } else {
                    viewModel.selectAll()
                }
            }
        }
    }

    private fun showCategoryFilterDialog() {
        val categories = CategoryHelper.getAllCategories()
        val categoryNames = mutableListOf("All Categories")
        categoryNames.addAll(categories.map { it.name })

        val currentCategoryId = viewModel.selectedCategoryId.value
        val selectedIndex = if (currentCategoryId == null) {
            0
        } else {
            categories.indexOfFirst { it.id == currentCategoryId } + 1
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Category")
            .setSingleChoiceItems(categoryNames.toTypedArray(), selectedIndex) { dialog, which ->
                val categoryId = if (which == 0) null else categories[which - 1].id
                viewModel.setCategoryFilter(categoryId)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        // Observe filtered products instead of allProducts
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredProducts.collect { products ->
                adapter.submitList(products)
                updateEmptyState(products.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedProductIds.collect { selectedIds ->
                adapter.updateSelectedIds(selectedIds)
                updateSelectedCountText(selectedIds.size)
                updateSelectAllButtonText(selectedIds.size, viewModel.filteredProducts.value.size)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.boxCreated.collect { created ->
                if (created) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.box_created_successfully),
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.productsRecyclerView.visibility = View.GONE
            binding.noProductsText.visibility = View.VISIBLE
        } else {
            binding.productsRecyclerView.visibility = View.VISIBLE
            binding.noProductsText.visibility = View.GONE
        }
    }

    private fun updateSelectedCountText(count: Int) {
        binding.selectedCountText.text = "$count products selected"
    }

    private fun updateSelectAllButtonText(selectedCount: Int, totalCount: Int) {
        val allSelected = selectedCount == totalCount && selectedCount > 0
        binding.selectAllProductsButton.text = if (allSelected) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
