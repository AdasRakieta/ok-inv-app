package com.example.inventoryapp.ui.tools

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
import com.example.inventoryapp.databinding.FragmentImportPreviewBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.PackageEntity
import com.example.inventoryapp.utils.AppLogger
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch

class ImportPreviewFragment : Fragment() {

    private var _binding: FragmentImportPreviewBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var productRepository: ProductRepository
    private lateinit var packageRepository: PackageRepository
    private lateinit var templateRepository: ProductTemplateRepository
    
    private val gson = Gson()
    
    private val productPreviewAdapter = ProductPreviewAdapter()
    private val packagePreviewAdapter = PackagePreviewAdapter()
    
    private var currentExportData: ExportData? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRepositories()
        setupRecyclerViews()
        setupInputHandling()
        setupButtons()
        
        // Auto-focus on QR input field for hardware scanner
        binding.qrInput.requestFocus()
    }
    
    private fun setupRepositories() {
        val database = AppDatabase.getDatabase(requireContext())
        productRepository = ProductRepository(database.productDao())
        packageRepository = PackageRepository(database.packageDao(), database.productDao())
        templateRepository = ProductTemplateRepository(database.productTemplateDao())
    }
    
    private fun setupRecyclerViews() {
        binding.recyclerProducts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productPreviewAdapter
        }
        
        binding.recyclerPackages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = packagePreviewAdapter
        }
    }
    
    private fun setupInputHandling() {
        // Handle Enter key from keyboard or hardware scanner
        binding.qrInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE || 
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                parseJson()
                true
            } else {
                false
            }
        }
        
        // Also handle Enter key directly
        binding.qrInput.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                parseJson()
                true
            } else {
                false
            }
        }
    }
    
    private fun setupButtons() {
        binding.parseButton.setOnClickListener {
            parseJson()
        }
        
        binding.importButton.setOnClickListener {
            importToDatabase()
        }
    }
    
    private fun parseJson() {
        val rawJson = binding.qrInput.text.toString()
        
        if (rawJson.isBlank()) {
            Toast.makeText(requireContext(), "Please scan QR code or paste JSON", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            AppLogger.logAction("Import Preview", "Parsing JSON (${rawJson.length} chars)")
            
            // Step 1: Basic cleaning
            var cleanJson = rawJson
                .trim()
                .replace("\r\n", "")     // Remove Windows newlines
                .replace("\n", "")       // Remove Unix newlines
                .replace("\r", "")       // Remove carriage returns

            // Step 2: Handle escaped characters from QR scanner
            // QR scanners often return JSON with escaped newlines and quotes
            cleanJson = cleanJson
                .replace("\\n", "")      // Remove literal \n (two characters: backslash and 'n')
                .replace("\\t", "")      // Remove literal \t
                .replace("\\r", "")      // Remove literal \r

            // Step 3: Fix escaped quotes
            cleanJson = cleanJson.replace("\\\"", "\"")

            // Step 4: Remove outer quotes if entire JSON is wrapped as a string
            // This happens when QR code contains JSON as escaped string
            while (cleanJson.startsWith("\"") && cleanJson.endsWith("\"") && cleanJson.length > 2) {
                cleanJson = cleanJson.substring(1, cleanJson.length - 1)
                cleanJson = cleanJson.replace("\\\"", "\"")  // Unescape quotes again
            }

            // Step 5: Remove any remaining extra whitespace
            cleanJson = cleanJson.trim()
            
            // Step 6: Check for GZIP compression and decompress (legacy support)
            // New QR codes use plain JSON, old ones may still use GZIP compression
            if (cleanJson.startsWith("GZIP:")) {
                cleanJson = com.example.inventoryapp.utils.QRCodeGenerator.decodeAndDecompress(cleanJson)
            }

            // Debug: Log first 300 chars of cleaned JSON
            val preview = if (cleanJson.length > 300) cleanJson.take(300) + "..." else cleanJson
            AppLogger.d("Import Preview", "Cleaned JSON: $preview")

            // Parse JSON
            val exportData = gson.fromJson(cleanJson, ExportData::class.java)
            
            // Validate data
            val validationErrors = validateExportData(exportData)
            if (validationErrors.isNotEmpty()) {
                val errorMessage = "Validation errors:\n${validationErrors.joinToString("\n")}"
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                AppLogger.w("Import Preview", "Validation failed: $errorMessage")
                return
            }
            
            // Store parsed data
            currentExportData = exportData
            
            // Display preview
            displayPreview(exportData)
            
            Toast.makeText(
                requireContext(), 
                "Parsed: ${exportData.products.size} products, ${exportData.packages.size} packages",
                Toast.LENGTH_SHORT
            ).show()
            
            AppLogger.logAction("Import Preview", "Parsed successfully: ${exportData.products.size}P, ${exportData.packages.size}Pk")
            
        } catch (e: JsonSyntaxException) {
            val errorMsg = e.message ?: "Unknown syntax error"
            val jsonPreview = rawJson.take(100) + if (rawJson.length > 100) "..." else ""
            val fullError = "JSON Syntax Error:\n$errorMsg\n\nJSON start: $jsonPreview"
            Toast.makeText(requireContext(), fullError, Toast.LENGTH_LONG).show()
            AppLogger.logError("Import Preview - Parse JSON", e)
            AppLogger.d("Import Preview", "Failed raw JSON: $rawJson")
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.javaClass.simpleName}: ${e.message}"
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
            AppLogger.logError("Import Preview - Parse", e)
            AppLogger.d("Import Preview", "Failed raw JSON: $rawJson")
        }
    }
    
    private fun validateExportData(exportData: ExportData): List<String> {
        val errors = mutableListOf<String>()
        
        // Check for empty serial numbers
        val invalidSerialNumbers = exportData.products.filter { 
            it.serialNumber.isNullOrBlank() 
        }
        if (invalidSerialNumbers.isNotEmpty()) {
            errors.add("${invalidSerialNumbers.size} products have empty serial numbers")
        }
        
        // Check for duplicate serial numbers in imported data
        val serialNumberGroups = exportData.products
            .filter { !it.serialNumber.isNullOrBlank() }
            .groupBy { it.serialNumber }
            .filter { it.value.size > 1 }
        
        if (serialNumberGroups.isNotEmpty()) {
            errors.add("Duplicate serial numbers found: ${serialNumberGroups.keys.joinToString(", ")}")
        }
        
        return errors
    }
    
    private fun displayPreview(exportData: ExportData) {
        // Build summary text
        val summaryBuilder = StringBuilder()
        summaryBuilder.appendLine("📊 Data to be imported:")
        summaryBuilder.appendLine("")
        
        if (exportData.contractors.isNotEmpty()) {
            summaryBuilder.appendLine("👥 Contractors: ${exportData.contractors.size}")
            exportData.contractors.take(3).forEach { contractor ->
                summaryBuilder.appendLine("   • ${contractor.name}")
            }
            if (exportData.contractors.size > 3) {
                summaryBuilder.appendLine("   ... and ${exportData.contractors.size - 3} more")
            }
            summaryBuilder.appendLine("")
        }
        
        if (exportData.packages.isNotEmpty()) {
            summaryBuilder.appendLine("📦 Packages: ${exportData.packages.size}")
            exportData.packages.take(3).forEach { pkg ->
                val contractorInfo = pkg.contractorId?.let { id ->
                    exportData.contractors.find { it.id == id }?.name?.let { " (${it})" } ?: ""
                } ?: ""
                summaryBuilder.appendLine("   • ${pkg.name}${contractorInfo}")
            }
            if (exportData.packages.size > 3) {
                summaryBuilder.appendLine("   ... and ${exportData.packages.size - 3} more")
            }
            summaryBuilder.appendLine("")
        }
        
        if (exportData.boxes.isNotEmpty()) {
            summaryBuilder.appendLine("📦 Boxes: ${exportData.boxes.size}")
            exportData.boxes.take(3).forEach { box ->
                val location = box.location?.let { " @ $it" } ?: ""
                summaryBuilder.appendLine("   • ${box.name}${location}")
            }
            if (exportData.boxes.size > 3) {
                summaryBuilder.appendLine("   ... and ${exportData.boxes.size - 3} more")
            }
            summaryBuilder.appendLine("")
        }
        
        if (exportData.products.isNotEmpty()) {
            summaryBuilder.appendLine("📱 Products: ${exportData.products.size}")
            summaryBuilder.appendLine("")
        }
        
        // Show summary
        binding.summarySection.visibility = View.VISIBLE
        binding.summaryText.text = summaryBuilder.toString()
        
        // Show products
        if (exportData.products.isNotEmpty()) {
            binding.productsSection.visibility = View.VISIBLE
            productPreviewAdapter.submitList(exportData.products)
        } else {
            binding.productsSection.visibility = View.GONE
        }
        
        // Show packages (detailed view)
        if (exportData.packages.isNotEmpty()) {
            binding.packagesSection.visibility = View.VISIBLE
            packagePreviewAdapter.submitList(exportData.packages)
        } else {
            binding.packagesSection.visibility = View.GONE
        }
        
        // Enable import button
        binding.importButton.isEnabled = true
    }
    
    private fun importToDatabase() {
        val exportData = currentExportData
        if (exportData == null) {
            Toast.makeText(requireContext(), "No data to import. Please parse JSON first.", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                AppLogger.logAction("Import Preview", "Starting import to database")
                
                var productsAdded = 0
                var productsUpdated = 0
                var packagesAdded = 0
                var templatesAdded = 0
                var relationsAdded = 0
                
                // Step 1: Import templates first
                for (template in exportData.templates) {
                    try {
                        val newTemplate = template.copy(
                            id = 0,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        templateRepository.insertTemplate(newTemplate)
                        templatesAdded++
                    } catch (e: Exception) {
                        AppLogger.w("Import Preview", "Skipped template: ${template.name}", e)
                    }
                }
                
                // Step 2: Import products with duplicate handling and track ID mapping
                val productIdMap = mutableMapOf<Long, Long>() // old ID -> new ID
                for (product in exportData.products) {
                    try {
                        val oldId = product.id
                        // Skip products with null/blank serial numbers
                        val serialNumber = product.serialNumber
                        if (serialNumber.isNullOrBlank()) {
                            AppLogger.w("Import Preview", "Skipping product with empty SN: ${product.name}")
                            continue
                        }
                        
                        // Check if product with this serial number exists
                        val existingProduct = productRepository.getProductBySerialNumber(serialNumber)
                        
                        if (existingProduct != null) {
                            // Update existing product
                            val updatedProduct = product.copy(
                                id = existingProduct.id,
                                updatedAt = System.currentTimeMillis()
                            )
                            productRepository.updateProduct(updatedProduct)
                            productIdMap[oldId] = existingProduct.id
                            productsUpdated++
                            AppLogger.d("Import Preview", "Updated product: ${product.name} (SN: $serialNumber)")
                        } else {
                            // Insert new product
                            val newProduct = product.copy(
                                id = 0,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            val newId = productRepository.insertProduct(newProduct)
                            productIdMap[oldId] = newId
                            productsAdded++
                            AppLogger.d("Import Preview", "Added product: ${product.name} (SN: $serialNumber)")
                        }
                    } catch (e: Exception) {
                        AppLogger.w("Import Preview", "Failed to import product: ${product.name}", e)
                    }
                }
                
                // Step 3: Import packages and track ID mapping
                val packageIdMap = mutableMapOf<Long, Long>() // old ID -> new ID
                for (pkg in exportData.packages) {
                    try {
                        val oldId = pkg.id
                        val newPackage = pkg.copy(
                            id = 0,
                            createdAt = System.currentTimeMillis()
                        )
                        val newId = packageRepository.insertPackage(newPackage)
                        packageIdMap[oldId] = newId
                        packagesAdded++
                    } catch (e: Exception) {
                        AppLogger.w("Import Preview", "Failed to import package: ${pkg.name}", e)
                    }
                }
                
                // Step 4: Import package-product relationships using mapped IDs
                for (relation in exportData.packageProductRelations) {
                    try {
                        val newPackageId = packageIdMap[relation.packageId]
                        val newProductId = productIdMap[relation.productId]
                        
                        if (newPackageId != null && newProductId != null) {
                            packageRepository.addProductToPackage(newPackageId, newProductId)
                            relationsAdded++
                            AppLogger.d("Import Preview", "Added relation: Package $newPackageId -> Product $newProductId")
                        } else {
                            AppLogger.w("Import Preview", "Skipped relation - package ${relation.packageId} or product ${relation.productId} not mapped")
                        }
                    } catch (e: Exception) {
                        AppLogger.w("Import Preview", "Failed to import relation: ${relation.packageId} -> ${relation.productId}", e)
                    }
                }
                
                val message = "Imported: $productsAdded new, $productsUpdated updated products\n" +
                             "$packagesAdded packages, $templatesAdded templates, $relationsAdded relations"
                
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                AppLogger.logAction("Import Preview", "Import completed: $message")
                
                // Clear the form
                binding.qrInput.text?.clear()
                binding.importButton.isEnabled = false
                currentExportData = null
                productPreviewAdapter.submitList(emptyList())
                packagePreviewAdapter.submitList(emptyList())
                binding.productsSection.visibility = View.GONE
                binding.packagesSection.visibility = View.GONE
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                AppLogger.logError("Import Preview - Import", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Data class for parsing exported JSON from QR codes
     */
    data class ExportData(
        val version: Int = 3,
        val exportedAt: Long = System.currentTimeMillis(),
        val products: List<com.example.inventoryapp.data.local.entities.ProductEntity> = emptyList(),
        val packages: List<com.example.inventoryapp.data.local.entities.PackageEntity> = emptyList(),
        val templates: List<com.example.inventoryapp.data.local.entities.ProductTemplateEntity> = emptyList(),
        val boxes: List<com.example.inventoryapp.data.local.entities.BoxEntity> = emptyList(),
        val contractors: List<com.example.inventoryapp.data.local.entities.ContractorEntity> = emptyList(),
        val packageProductRelations: List<com.example.inventoryapp.data.local.entities.PackageProductCrossRef> = emptyList(),
        val boxProductRelations: List<com.example.inventoryapp.data.local.entities.BoxProductCrossRef> = emptyList()
    )
}
