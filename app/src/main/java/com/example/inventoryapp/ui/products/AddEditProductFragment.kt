package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentAddEditProductBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class AddEditProductFragment : Fragment() {
    
    private var _binding: FragmentAddEditProductBinding? = null
    private val binding get() = _binding!!
    
    private val args: AddEditProductFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    
    private var selectedCategoryId: Long? = null
    private var isEditMode = false
    private var existingProduct: ProductEntity? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditProductBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        isEditMode = args.productId != -1L
        
        setupSpinners()
        setupButtons()
        
        if (isEditMode) {
            loadProduct()
        }
    }
    
    private fun setupSpinners() {
        // Status spinner
        val statusAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            ProductStatus.values().map { it.name }
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.statusSpinner.adapter = statusAdapter
        
        // Category spinner
        lifecycleScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            val categoryNames = listOf("Select category") + categories.map { it.name }
            val categoryAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                categoryNames
            )
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.categorySpinner.adapter = categoryAdapter
            
            binding.categorySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedCategoryId = if (position > 0) {
                        categories[position - 1].id
                    } else {
                        null
                    }
                }
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                    selectedCategoryId = null
                }
            }
        }
    }
    
    private fun setupButtons() {
        binding.saveButton.setOnClickListener {
            saveProduct()
        }
        
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun loadProduct() {
        lifecycleScope.launch {
            val product = productRepository.getProductById(args.productId).first()
            if (product != null) {
                existingProduct = product
                populateFields(product)
            } else {
                Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
    
    private suspend fun populateFields(product: ProductEntity) {
        binding.nameInput.setText(product.name)
        binding.serialNumberInput.setText(product.serialNumber)
        binding.manufacturerInput.setText(product.manufacturer)
        binding.modelInput.setText(product.model)
        binding.descriptionInput.setText(product.description)
        
        // Status
        val statusPosition = ProductStatus.values().indexOf(product.status)
        binding.statusSpinner.setSelection(statusPosition)
        
        // Category
        if (product.categoryId != null) {
            val categories = categoryRepository.getAllCategories().first()
            val categoryPosition = categories.indexOfFirst { it.id == product.categoryId }
            if (categoryPosition >= 0) {
                binding.categorySpinner.setSelection(categoryPosition + 1)
                selectedCategoryId = product.categoryId
            }
        }
    }
    
    private fun saveProduct() {
        // Validation
        val name = binding.nameInput.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameInputLayout.error = "Name is required"
            return
        } else {
            binding.nameInputLayout.error = null
        }
        
        val serialNumber = binding.serialNumberInput.text.toString().trim()
        if (serialNumber.isEmpty()) {
            binding.serialNumberInputLayout.error = "Serial number is required"
            return
        } else {
            binding.serialNumberInputLayout.error = null
        }
        
        val status = ProductStatus.values()[binding.statusSpinner.selectedItemPosition]
        
        val now = System.currentTimeMillis()
        val product = ProductEntity(
            id = if (isEditMode) args.productId else 0,
            name = name,
            serialNumber = serialNumber,
            categoryId = selectedCategoryId,
            manufacturer = binding.manufacturerInput.text.toString().trim().takeIf { it.isNotEmpty() },
            model = binding.modelInput.text.toString().trim().takeIf { it.isNotEmpty() },
            description = binding.descriptionInput.text.toString().trim().takeIf { it.isNotEmpty() },
            status = status,
            purchaseDate = null,
            purchasePrice = null,
            warrantyExpiryDate = null,
            condition = null,
            assignedToEmployeeId = null,
            assignmentDate = null,
            shelf = null,
            bin = null,
            notes = null,
            createdAt = existingProduct?.createdAt ?: now,
            updatedAt = now
        )
        
        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    productRepository.updateProduct(product)
                    Toast.makeText(requireContext(), "Product updated", Toast.LENGTH_SHORT).show()
                } else {
                    productRepository.insertProduct(product)
                    Toast.makeText(requireContext(), "Product added", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            } catch (e: Exception) {
                if (e.message?.contains("UNIQUE constraint failed") == true) {
                    binding.serialNumberInputLayout.error = "This serial number already exists"
                } else {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
