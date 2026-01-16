package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.databinding.FragmentAddProductBinding
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.runBlocking

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProductsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        val repository = app.productRepository
        val packageRepository = app.packageRepository
        val factory = ProductsViewModelFactory(repository, packageRepository)
        val vm: ProductsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryDropdown()
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

    private fun setupClickListeners() {
        binding.scanSerialButton.setOnClickListener {
            // TODO: Navigate to scanner with result callback
            Toast.makeText(requireContext(), "Scanner integration coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.saveButton.setOnClickListener {
            saveProduct()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun saveProduct() {
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

                // Use the already calculated categoryId

                // Use null for serial number if category doesn't require it
                val finalSerialNumber = if (serialNumber.isEmpty()) null else serialNumber

                // Check for duplicate SN if provided
                if (finalSerialNumber != null) {
                    val repository = ProductRepository(AppDatabase.getDatabase(requireContext()).productDao())
                    val existingProduct = runBlocking { repository.getProductBySerialNumber(finalSerialNumber) }
                    if (existingProduct != null) {
                        binding.serialNumberLayout.error = "This Serial Number is already in use"
                        return
                    }
                }

                viewModel.addProduct(
                    name = name,
                    categoryId = categoryId,
                    serialNumber = finalSerialNumber,
                    description = description
                )

                Toast.makeText(
                    requireContext(),
                    "Product added successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
