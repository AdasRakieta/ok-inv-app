package com.example.inventoryapp.ui.templates

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentBulkScanBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import com.example.inventoryapp.ui.scanner.BarcodeAnalyzer
import com.example.inventoryapp.utils.CategoryHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BulkProductScanFragment : Fragment() {

    private var _binding: FragmentBulkScanBinding? = null
    private val binding get() = _binding!!

    private val args: BulkProductScanFragmentArgs by navArgs()
    private lateinit var templateRepository: ProductTemplateRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var cameraExecutor: ExecutorService

    private var templateName: String = ""
    private var templateDescription: String? = null
    private var categoryId: Long? = null
    private var requiresSerialNumber: Boolean = true // Track if category requires SN
    private var scannedCount: Int = 0
    private val scannedSerials = mutableSetOf<String>()
    private val pendingProducts = mutableListOf<PendingProduct>() // Products waiting to be saved
    
    private var currentInputField: TextInputEditText? = null

    // Data class to hold pending product info
    data class PendingProduct(
        val serialNumber: String?, // Nullable for "Other" category
        val createdAt: Long = System.currentTimeMillis()
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Camera permission not used anymore - removed camera mode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val database = AppDatabase.getDatabase(requireContext())
        templateRepository = ProductTemplateRepository(database.productTemplateDao())
        productRepository = ProductRepository(database.productDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadTemplateData()
        setupClickListeners()
        updateUI()
        
        // Start with one empty input field
        addProductInputField()
    }

    private fun loadTemplateData() {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getTemplateById(args.templateId).collect { template ->
                template?.let {
                    templateName = it.name
                    templateDescription = it.description
                    categoryId = it.categoryId
                    
                    // Check if category requires serial number
                    requiresSerialNumber = CategoryHelper.requiresSerialNumber(it.categoryId)
                    
                    // Update hint text based on category requirement
                    updateInputFieldHint()
                }
            }
        }
    }
    
    private fun updateInputFieldHint() {
        currentInputField?.let { field ->
            val layout = field.parent.parent as? TextInputLayout
            layout?.hint = if (requiresSerialNumber) {
                "Serial Number *"
            } else {
                "Item identifier (optional)"
            }
        }
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveAllProducts()
        }

        binding.cancelButton.setOnClickListener {
            cancelAllProducts()
        }
        
        // Quantity controls for "Other" category
        binding.increaseQuantityButton.setOnClickListener {
            if (!requiresSerialNumber) {
                // Add one more item to pending list
                pendingProducts.add(PendingProduct(serialNumber = null))
                updateUI()
                Toast.makeText(requireContext(), "Quantity +1", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.decreaseQuantityButton.setOnClickListener {
            if (!requiresSerialNumber && pendingProducts.isNotEmpty()) {
                // Remove last item from pending list
                pendingProducts.removeAt(pendingProducts.size - 1)
                updateUI()
                Toast.makeText(requireContext(), "Quantity -1", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun addProductInputField() {
        // For "Other" category, only create one reusable input field
        if (!requiresSerialNumber && binding.productsInputContainer.childCount > 0) {
            // Field already exists, just clear and focus it
            currentInputField?.setText("")
            currentInputField?.requestFocus()
            return
        }
        
        val context = requireContext()
        val productNumber = pendingProducts.size + 1

        // Create horizontal container for input field and delete button
        val horizontalContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Create TextInputLayout (takes most of the space)
        val inputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // width = 0 for weight
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f // weight = 1 to take remaining space
            )
            // Dynamic hint based on category requirement
            hint = if (requiresSerialNumber) {
                "$productNumber. Serial Number *"
            } else {
                "Scan/Enter product name (quantity: ${pendingProducts.size})"
            }
            setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
        }

        // Create TextInputEditText
        val editText = TextInputEditText(inputLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE

            // Handle enter key
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    val serialNumber = text.toString().trim()
                    processManualEntry(serialNumber)
                    true
                } else {
                    false
                }
            }

            // Auto-focus for barcode scanners that act as keyboard
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    // If scanner inputs entire string at once, process it
                    val text = s.toString().trim()
                    if (text.isNotEmpty() && text.length >= 5) {
                        // Check if this looks like a barcode (no manual typing in progress)
                        // We'll process it after a short delay to allow scanner to finish
                        postDelayed({
                            if (this@apply.text.toString().trim() == text) {
                                processManualEntry(text)
                            }
                        }, 100)
                    }
                }
            })
        }

        // Create delete button (X) - only for categories requiring SN
        if (requiresSerialNumber) {
            val deleteButton = androidx.appcompat.widget.AppCompatImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini),
                    resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini)
                ).apply {
                    marginStart = 8
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                contentDescription = "Delete this entry"
                background = null
                setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                setOnClickListener {
                    removeProductInputField(horizontalContainer, editText)
                }
            }
            horizontalContainer.addView(deleteButton)
        }

        // Assemble the layout
        inputLayout.addView(editText)
        horizontalContainer.addView(inputLayout)

        binding.productsInputContainer.addView(horizontalContainer)

        // Store current input field reference
        currentInputField = editText

        // Focus the new field
        editText.requestFocus()
    }

    private fun removeProductInputField(container: LinearLayout, editText: TextInputEditText) {
        // Get the serial number from the field
        val serialNumber = editText.text.toString().trim()

        // Remove from pending list
        if (serialNumber.isNotEmpty()) {
            pendingProducts.removeAll { it.serialNumber == serialNumber }
            scannedSerials.remove(serialNumber)
            showStatus("🗑️ Removed: $serialNumber")
        }

        // Remove the container from the layout
        binding.productsInputContainer.removeView(container)

        // If this was the current input field, clear the reference
        if (currentInputField == editText) {
            currentInputField = null
        }

        // Update UI
        updateUI()

        // If no more input fields, add a new empty one
        if (binding.productsInputContainer.childCount == 0) {
            addProductInputField()
        }
    }
    
    private fun processManualEntry(serialNumber: String) {
        // For "Other" category, allow multiple scans of same name (for counting quantity)
        if (!requiresSerialNumber) {
            // Each scan adds +1 to quantity count
            val itemNumber = pendingProducts.size + 1
            val pendingProduct = PendingProduct(
                serialNumber = null, // No SN for "Other" category
                createdAt = System.currentTimeMillis()
            )
            pendingProducts.add(pendingProduct)
            
            updateUI()
            
            if (serialNumber.isEmpty()) {
                showStatus("✅ Added item #$itemNumber (no SN required)")
            } else {
                // User scanned/entered product name for counting
                showStatus("✅ Added item #$itemNumber: $serialNumber")
            }
            
            // Clear field and keep focus for next scan
            currentInputField?.setText("")
            currentInputField?.requestFocus()
            return
        }
        
        // For categories requiring SN, validate as before
        if (serialNumber.isEmpty()) return

        // Validate SN format based on category
        if (!CategoryHelper.isValidSerialNumber(serialNumber, categoryId)) {
            val requiredPrefix = when (categoryId) {
                1L, 3L -> "S" // Scanner, Scanner Docking Station
                2L, 4L -> "X" // Printer, Printer Docking Station
                else -> "S" // Fallback
            }
            showStatus("❌ Invalid SN format: $serialNumber (must start with '$requiredPrefix')")
            currentInputField?.setText("")
            return
        }

        // Check if already in pending list
        if (scannedSerials.contains(serialNumber)) {
            showStatus("⚠️ Already in list: $serialNumber")
            currentInputField?.setText("")
            return
        }

        // Check if serial number exists in database
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val exists = productRepository.isSerialNumberExists(serialNumber)
                if (exists) {
                    showStatus("❌ Duplicate SN in database: $serialNumber")
                    currentInputField?.setText("")
                    return@launch
                }

                // Add to pending list (not saved yet)
                val pendingProduct = PendingProduct(serialNumber)
                pendingProducts.add(pendingProduct)
                scannedSerials.add(serialNumber)

                // Update UI with date created info
                updateUI()
                val dateStr = formatDateTime(pendingProduct.createdAt)
                showStatus("✅ Added to list: $serialNumber (${dateStr})")
                
                // Mark current field as processed and add new one
                currentInputField?.isEnabled = false
                addProductInputField()

            } catch (e: Exception) {
                showStatus("❌ Error: ${e.message}")
                currentInputField?.setText("")
            }
        }
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
    
    private fun saveAllProducts() {
        if (pendingProducts.isEmpty()) {
            Toast.makeText(requireContext(), "No products to save", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                var successCount = 0
                
                // For "Other" category without SN: aggregate all items to one product with quantity
                if (!requiresSerialNumber && pendingProducts.all { it.serialNumber == null }) {
                    // Try to find existing product with same name and category
                    val existingProduct = productRepository.findProductByNameAndCategory(templateName, categoryId)
                    
                    if (existingProduct != null) {
                        // Update existing product quantity
                        val newQuantity = existingProduct.quantity + pendingProducts.size
                        productRepository.updateQuantity(existingProduct.id, newQuantity)
                        
                        Toast.makeText(
                            requireContext(),
                            "✅ Updated ${existingProduct.name}: +${pendingProducts.size} (Total: $newQuantity)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Create new product with quantity
                        val product = ProductEntity(
                            name = templateName,
                            categoryId = categoryId,
                            serialNumber = null,
                            description = templateDescription,
                            quantity = pendingProducts.size,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        productRepository.insertProduct(product)
                        
                        Toast.makeText(
                            requireContext(),
                            "✅ Created new product: ${templateName} (Qty: ${pendingProducts.size})",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    
                    findNavController().navigateUp()
                    return@launch
                }
                
                // For categories requiring SN: save each product separately
                for (pending in pendingProducts) {
                    val product = ProductEntity(
                        name = templateName,
                        categoryId = categoryId,
                        serialNumber = pending.serialNumber,
                        description = templateDescription,
                        quantity = 1,
                        createdAt = pending.createdAt,
                        updatedAt = pending.createdAt
                    )
                    productRepository.insertProduct(product)
                    successCount++
                }
                
                Toast.makeText(
                    requireContext(),
                    "✅ Saved $successCount products successfully!",
                    Toast.LENGTH_LONG
                ).show()
                
                findNavController().navigateUp()
                
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "❌ Error saving: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun cancelAllProducts() {
        // Clear all pending products
        pendingProducts.clear()
        scannedSerials.clear()
        
        Toast.makeText(
            requireContext(),
            "Cancelled - no products saved",
            Toast.LENGTH_SHORT
        ).show()
        
        findNavController().navigateUp()
    }


    private fun showStatus(message: String) {
        activity?.runOnUiThread {
            binding.lastScannedText.text = message
        }
    }

    private fun updateUI() {
        activity?.runOnUiThread {
            val count = pendingProducts.size
            binding.scanCountText.text = "Products in list: $count"
            
            // Show/hide quantity controls based on category
            if (!requiresSerialNumber) {
                binding.quantityControlsLayout.visibility = View.VISIBLE
                binding.currentQuantityText.text = count.toString()
                
                // Update input field hint for "Other" category
                if (currentInputField != null) {
                    val inputLayout = currentInputField?.parent?.parent as? TextInputLayout
                    inputLayout?.hint = "Scan/Enter product name (quantity: $count)"
                }
            } else {
                binding.quantityControlsLayout.visibility = View.GONE
            }
            
            if (count == 0) {
                binding.lastScannedText.text = "Ready to add products..."
            } else {
                // For "Other" category, show simple count
                if (!requiresSerialNumber) {
                    binding.lastScannedText.text = "✅ $count items added - Ready for more scans"
                } else {
                    // Show list with dates for categories with SN
                    val preview = pendingProducts.takeLast(3).joinToString("\n") { 
                        "• ${it.serialNumber} - ${formatDateTime(it.createdAt)}"
                    }
                    binding.lastScannedText.text = if (count > 3) {
                        "Last 3 of $count:\n$preview"
                    } else {
                        preview
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
