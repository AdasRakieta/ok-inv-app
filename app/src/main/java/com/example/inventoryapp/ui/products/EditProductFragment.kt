package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.databinding.FragmentEditProductBinding
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class EditProductFragment : Fragment() {

    private var _binding: FragmentEditProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductDetailsViewModel
    private val args: EditProductFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        val productRepo = app.productRepository
        val deviceMovementRepo = app.deviceMovementRepository
        val boxRepo = app.boxRepository
        val packageRepo = app.packageRepository
        val factory = ProductDetailsViewModelFactory(productRepo, deviceMovementRepo, boxRepo, packageRepo, args.productId)
        val vm: ProductDetailsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
        observeProduct()
        observeSnUpdateError()
        setupClickListeners()
    }

    private fun setupCategoryDropdown() {
        val categories = CategoryHelper.getCategoryNames()

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            categories
        )

        binding.categoryInput.setAdapter(adapter)
    }

    private fun observeProduct() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.product.collect { product ->
                product?.let {
                    // Populate fields
                    binding.productNameInput.setText(it.name)
                    binding.serialNumberInput.setText(it.serialNumber)
                    binding.descriptionInput.setText(it.description)
                    
                    // Set category
                    val categoryName = CategoryHelper.getCategoryName(it.categoryId)
                    binding.categoryInput.setText(categoryName, false)
                }
            }
        }
    }

    private fun observeSnUpdateError() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.snUpdateError.collect { error ->
                error?.let {
                    binding.serialNumberLayout.error = it
                    viewModel.clearSnError()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveProduct()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun saveProduct() {
        val currentProduct = viewModel.product.value ?: return
        
        val name = binding.productNameInput.text.toString().trim()
        val serialNumber = binding.serialNumberInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim().takeIf { it.isNotEmpty() }
        val categoryName = binding.categoryInput.text.toString().trim()

        // Map category name to ID early for validation
        val categoryId = if (categoryName.isNotEmpty()) {
            CategoryHelper.getCategoryIdByName(categoryName)
        } else {
            null
        }

        when {
            name.isEmpty() -> {
                binding.productNameLayout.error = "Product name is required"
                return
            }
            categoryName.isNotEmpty() && CategoryHelper.requiresSerialNumber(categoryName) && serialNumber.isEmpty() -> {
                binding.serialNumberLayout.error = "Serial number is required for this category"
                return
            }
            !CategoryHelper.isValidSerialNumber(serialNumber, categoryId) -> {
                binding.serialNumberLayout.error = CategoryHelper.getSerialNumberValidationError(serialNumber, categoryId)
                return
            }
            else -> {
                binding.productNameLayout.error = null
                binding.serialNumberLayout.error = null

                // Use null for serial number if category doesn't require it
                val finalSerialNumber = if (serialNumber.isEmpty()) null else serialNumber

                // Update product
                viewLifecycleOwner.lifecycleScope.launch {
                    val updatedProduct = currentProduct.copy(
                        name = name,
                        categoryId = categoryId,
                        serialNumber = finalSerialNumber,
                        description = description,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Check for duplicate SN only if SN is provided
                    val repository = (viewModel as ProductDetailsViewModel).let {
                        val database = AppDatabase.getDatabase(requireContext())
                        ProductRepository(database.productDao())
                    }
                    
                    val existingProduct = repository.getProductBySerialNumber(serialNumber)
                    if (existingProduct != null && existingProduct.id != currentProduct.id) {
                        binding.serialNumberLayout.error = "This Serial Number is already in use"
                        return@launch
                    }
                    
                    repository.updateProduct(updatedProduct)

                    Toast.makeText(
                        requireContext(),
                        "Product updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
