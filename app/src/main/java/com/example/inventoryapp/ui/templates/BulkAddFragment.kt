package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.data.local.entities.ScanType
import com.example.inventoryapp.databinding.FragmentBulkAddBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Fragment for bulk adding products using templates and scanner input
 * Scanner device (e.g., Zebra) inputs serial numbers into text field
 */
class BulkAddFragment : Fragment() {

    private var _binding: FragmentBulkAddBinding? = null
    private val binding get() = _binding!!
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    
    private val templateRepository by lazy {
        (requireActivity().application as InventoryApplication).productTemplateRepository
    }
    
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    
    private val scanHistoryRepository by lazy {
        (requireActivity().application as InventoryApplication).scanHistoryRepository
    }
    
    private var selectedTemplate: ProductTemplateEntity? = null
    private val scannedProducts = mutableListOf<ProductEntity>()
    private lateinit var adapter: ScannedProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupScanInput()
        loadTemplates()
        updateUI()
    }
    
    private fun setupRecyclerView() {
        adapter = ScannedProductsAdapter(
            onDeleteClick = { product ->
                scannedProducts.remove(product)
                adapter.submitList(scannedProducts.toList())
                updateStats()
            }
        )
        
        binding.scannedProductsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.scannedProductsRecycler.adapter = adapter
    }
    
    private fun setupScanInput() {
        // Scanner input field - scanner acts as keyboard
        binding.scanInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                handleScan(binding.scanInput.text.toString().trim())
                true
            } else {
                false
            }
        }
        
        // Also handle enter key directly (some scanners send this)
        binding.scanInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                handleScan(binding.scanInput.text.toString().trim())
                true
            } else {
                false
            }
        }
        
        // Save all button
        binding.saveAllButton.setOnClickListener {
            saveAllProducts()
        }
        
        // Clear all button
        binding.clearAllButton.setOnClickListener {
            clearAll()
        }
    }
    
    private fun loadTemplates() {
        lifecycleScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
                // For now, just show template selection in spinner/dropdown
                // You can expand this with a proper template picker dialog
                if (templates.isNotEmpty() && selectedTemplate == null) {
                    selectedTemplate = templates.first()
                    updateUI()
                }
            }
        }
    }
    
    private fun handleScan(scannedValue: String) {
        if (scannedValue.isEmpty()) {
            return
        }
        
        if (selectedTemplate == null) {
            Toast.makeText(requireContext(), "Wybierz wzór produktu", Toast.LENGTH_SHORT).show()
            binding.scanInput.text?.clear()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Check if serial number already exists
                val existing = productRepository.getProductBySerialNumber(scannedValue)
                if (existing != null) {
                    Toast.makeText(requireContext(), "SN już istnieje: $scannedValue", Toast.LENGTH_SHORT).show()
                    binding.scanInput.text?.clear()
                    binding.scanInput.error = "Duplikat SN"
                    
                    // Record failed scan
                    scanHistoryRepository.recordScan(
                        scannedValue = scannedValue,
                        scanType = ScanType.SERIAL_NUMBER,
                        context = "bulk_add",
                        success = false,
                        errorMessage = "Duplicate serial number"
                    )
                    return@launch
                }
                
                // Check if already in current session
                if (scannedProducts.any { it.serialNumber == scannedValue }) {
                    Toast.makeText(requireContext(), "SN już zeskanowany w sesji", Toast.LENGTH_SHORT).show()
                    binding.scanInput.text?.clear()
                    return@launch
                }
                
                // Create product from template
                val template = selectedTemplate!!
                val category = categoryRepository.getCategoryById(template.categoryId).firstOrNull()
                
                val newProduct = ProductEntity(
                    name = "${template.name} - $scannedValue",
                    serialNumber = scannedValue,
                    categoryId = template.categoryId,
                    manufacturer = template.defaultManufacturer,
                    model = template.defaultModel,
                    description = template.defaultDescription,
                    status = ProductStatus.IN_STOCK
                )
                
                // Add to list
                scannedProducts.add(0, newProduct)
                adapter.submitList(scannedProducts.toList())
                updateStats()
                
                // Clear input
                binding.scanInput.text?.clear()
                
                // Record successful scan
                scanHistoryRepository.recordScan(
                    scannedValue = scannedValue,
                    scanType = ScanType.SERIAL_NUMBER,
                    context = "bulk_add",
                    success = true
                )
                
                // Visual feedback
                Toast.makeText(requireContext(), "✓ Dodano: $scannedValue", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Record error
                scanHistoryRepository.recordScan(
                    scannedValue = scannedValue,
                    scanType = ScanType.SERIAL_NUMBER,
                    context = "bulk_add",
                    success = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    private fun saveAllProducts() {
        if (scannedProducts.isEmpty()) {
            Toast.makeText(requireContext(), "Brak produktów do zapisania", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            try {
                // Save all products to database
                val ids = productRepository.insertProducts(scannedProducts)
                
                Toast.makeText(
                    requireContext(),
                    "✓ Zapisano ${ids.size} produktów",
                    Toast.LENGTH_LONG
                ).show()
                
                // Clear session
                scannedProducts.clear()
                adapter.submitList(emptyList())
                updateStats()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun clearAll() {
        scannedProducts.clear()
        adapter.submitList(emptyList())
        updateStats()
        Toast.makeText(requireContext(), "Wyczyszczono sesję", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateStats() {
        binding.scannedCountText.text = "Zeskanowano: ${scannedProducts.size}"
    }
    
    private fun updateUI() {
        selectedTemplate?.let { template ->
            binding.selectedTemplateText.text = "Wzór: ${template.name}"
        } ?: run {
            binding.selectedTemplateText.text = "Brak wzoru - wybierz wzór"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
