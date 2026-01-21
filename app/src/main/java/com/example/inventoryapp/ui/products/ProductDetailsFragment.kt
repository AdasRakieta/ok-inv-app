package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import com.example.inventoryapp.databinding.BottomSheetEditSerialBinding
import com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.inventoryapp.data.local.entities.ProductEntity
import androidx.appcompat.app.AlertDialog

class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val args: ProductDetailsFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }

    private var currentProduct: ProductEntity? = null
    private var categories: List<CategoryEntity> = emptyList()
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        observeCategories()
        loadProductDetails()
        setupActions()
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                categoryRepository.getAllCategories().collect { list ->
                    categories = list
                    updateCategoryLabel()
                }
            }
        }
    }
    
    private fun loadProductDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getProductById(args.productId).collect { product ->
                    currentProduct = product
                    product?.let {
                        binding.apply {
                            productNameText.text = it.name
                            productCategoryText.text = categoryNameFor(it.categoryId)
                            productIconText.text = categoryIconFor(it.categoryId)
                            productIdValue.text = it.customId ?: "-"
                            manufacturerValue.text = it.manufacturer ?: "-"
                            modelValue.text = it.model ?: "-"
                            descriptionValue.text = it.description ?: "-"
                            
                            // Warehouse location
                            val warehouseLocation = if (it.shelf != null) {
                                it.shelf + (if (!it.bin.isNullOrBlank()) " / ${it.bin}" else "")
                            } else {
                                "Nie przypisano"
                            }
                            warehouseLocationValue.text = warehouseLocation

                            createdAtText.text = formatDate(it.createdAt)
                            updatedAtText.text = formatDate(it.updatedAt)

                            // Assignment info
                            if (it.assignedToEmployeeId != null) {
                                assignedEmployeeLayout.visibility = View.VISIBLE
                                loadEmployeeInfo(it.assignedToEmployeeId)
                                buildMovementHistory(it)
                            } else {
                                assignedEmployeeLayout.visibility = View.GONE
                                movementHistoryText.text = "Magazyn"
                            }

                            // Serial number visibility
                            if (it.serialNumber.isNotBlank()) {
                                serialNumberAssignedLayout.visibility = View.VISIBLE
                                serialNumberNotAssignedLayout.visibility = View.GONE
                                serialNumberText.text = it.serialNumber
                            } else {
                                serialNumberAssignedLayout.visibility = View.GONE
                                serialNumberNotAssignedLayout.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryLabel() {
        currentProduct?.let {
            binding.productCategoryText.text = categoryNameFor(it.categoryId)
            binding.productIconText.text = categoryIconFor(it.categoryId)
        }
    }

    private fun setupActions() {
        binding.editSerialButton.setOnClickListener { promptEditSerial() }
        binding.editProductButton.setOnClickListener { navigateToEditForm() }
        binding.deleteProductButton.setOnClickListener { confirmDeleteProduct() }
    }

    private fun promptEditSerial() {
        val product = currentProduct ?: return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetEditSerialBinding.inflate(layoutInflater)
        
        // Set current serial number
        sheetBinding.serialNumberInput.setText(product.serialNumber)
        sheetBinding.serialNumberInput.requestFocus()
        
        // Cancel button
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        // Save button
        sheetBinding.saveButton.setOnClickListener {
            val newSerial = sheetBinding.serialNumberInput.text.toString().trim()
            
            when {
                newSerial.isEmpty() -> {
                    toast("Numer seryjny nie może być pusty")
                }
                newSerial == product.serialNumber -> {
                    toast("Numer seryjny bez zmian")
                    bottomSheet.dismiss()
                }
                else -> {
                    saveProduct(product.copy(serialNumber = newSerial, updatedAt = System.currentTimeMillis()))
                    bottomSheet.dismiss()
                }
            }
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
        
        // Show keyboard
        sheetBinding.serialNumberInput.postDelayed({
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(sheetBinding.serialNumberInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun navigateToEditForm() {
        val product = currentProduct ?: return
        val action = ProductDetailsFragmentDirections.actionProductDetailsToAdd(product.id)
        findNavController().navigate(action)
    }

    private fun confirmDeleteProduct() {
        val product = currentProduct ?: return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDeleteConfirmBinding.inflate(layoutInflater)
        
        // Set product name
        sheetBinding.productNameText.text = product.name
        
        // Cancel button
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        // Delete button with animation
        sheetBinding.deleteButton.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    
                    lifecycleScope.launch {
                        productRepository.deleteProductById(product.id)
                        toast("Produkt usunięty")
                        bottomSheet.dismiss()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                .start()
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun saveProduct(updated: ProductEntity) {
        lifecycleScope.launch {
            try {
                productRepository.updateProduct(updated)
                toast("Zapisano zmiany")
            } catch (e: Exception) {
                toast("Błąd: ${e.message}")
            }
        }
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp == null) "-" else dateFormat.format(Date(timestamp))
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun categoryNameFor(id: Long?): String {
        if (id == null) return "-"
        return categories.firstOrNull { it.id == id }?.name ?: "-"
    }
    
    private fun categoryIconFor(id: Long?): String {
        if (id == null) return "📦"
        return categories.firstOrNull { it.id == id }?.icon ?: "📦"
    }

    private fun loadEmployeeInfo(employeeId: Long) {
        lifecycleScope.launch {
            val employee = employeeRepository.getEmployeeById(employeeId)
            binding.assignedEmployeeValue.text = employee?.fullName ?: "Nieznany"
        }
    }

    private fun buildMovementHistory(product: ProductEntity) {
        val history = mutableListOf<String>()
        
        // Start with warehouse
        // Build warehouse location string
        val warehouseLocation = if (product.shelf != null) {
            "Magazyn (${product.shelf}${if (!product.bin.isNullOrBlank()) " / ${product.bin}" else ""})"
        } else {
            "Magazyn"
        }
        history.add(warehouseLocation)
        
        // Add assignment if exists
        if (product.assignedToEmployeeId != null) {
            lifecycleScope.launch {
                val employee = employeeRepository.getEmployeeById(product.assignedToEmployeeId)
                val employeeName = employee?.fullName ?: "Nieznany"
                val assignmentInfo = if (product.assignmentDate != null) {
                    "$employeeName (${formatDate(product.assignmentDate)})"
                } else {
                    employeeName
                }
                
                val fullHistory = "$warehouseLocation → $assignmentInfo"
                binding.movementHistoryText.text = fullHistory
            }
        } else {
            binding.movementHistoryText.text = warehouseLocation
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
