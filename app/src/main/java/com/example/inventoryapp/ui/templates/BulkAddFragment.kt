package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.data.local.entities.ScanType
import com.example.inventoryapp.databinding.FragmentBulkAddBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Fragment for bulk adding products using templates and scanner input
 * Scanner device (e.g., Zebra) inputs serial numbers into text field
 */
class BulkAddFragment : Fragment() {

    private var _binding: FragmentBulkAddBinding? = null
    private val binding get() = _binding!!
    
    private val args: BulkAddFragmentArgs by navArgs()
    
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
    private val scannedSerials = mutableSetOf<String>()
    private var currentInputField: TextInputEditText? = null

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
        
        setupScanInput()
        
        // Load template from arguments if provided
        if (args.templateId != 0L) {
            loadTemplate(args.templateId)
        } else {
            loadDefaultTemplate()
        }
        
        // Start with one empty input field
        addProductInputField()
        
        updateUI()
    }
    
    private fun setupScanInput() {
        // Save all button
        binding.saveAllButton.setOnClickListener {
            saveAllProducts()
        }
    }
    
    private fun addProductInputField() {
        // Only reuse field if it's still enabled (not processed yet)
        if (binding.productsInputContainer.childCount > 0 &&
            currentInputField != null &&
            currentInputField?.isEnabled == true) {
            // Field already exists and is active, just clear and focus it
            currentInputField?.setText("")
            currentInputField?.requestFocus()
            return
        }

        val context = requireContext()
        val productNumber = scannedProducts.size + 1

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

        // Create TextInputLayout
        val inputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "$productNumber. Skanuj numer seryjny"
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
                    handleScan(serialNumber)
                    true
                } else {
                    false
                }
            }

            // Auto-detect barcode scanner input
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString().trim()
                    if (text.isNotEmpty() && text.length >= 5) {
                        postDelayed({
                            if (this@apply.text.toString().trim() == text) {
                                handleScan(text)
                            }
                        }, 100)
                    }
                }
            })
        }

        // Create delete button
        val deleteButton = AppCompatImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini),
                resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini)
            ).apply {
                marginStart = 8
            }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = "Usuń wpis"
            background = null
            setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            setOnClickListener {
                removeProductInputField(horizontalContainer, editText)
            }
        }

        // Assemble the layout
        inputLayout.addView(editText)
        horizontalContainer.addView(inputLayout)
        horizontalContainer.addView(deleteButton)

        binding.productsInputContainer.addView(horizontalContainer)

        // Store current input field reference
        currentInputField = editText

        // Focus the new field
        editText.requestFocus()
    }

    private fun removeProductInputField(container: LinearLayout, editText: TextInputEditText) {
        val serialNumber = editText.text.toString().trim()

        // Remove from pending list
        if (serialNumber.isNotEmpty()) {
            scannedSerials.remove(serialNumber)
            // Find and remove product matching this SN
            val product = scannedProducts.find { it.serialNumber == serialNumber }
            if (product != null) {
                scannedProducts.remove(product)
            }
            showStatus("❌ Usunięto: $serialNumber")
            updateStats()
        }

        // Remove the container from the layout
        binding.productsInputContainer.removeView(container)

        if (currentInputField == editText) {
            currentInputField = null
        }

        // If no more input fields, add a new empty one
        if (binding.productsInputContainer.childCount == 0) {
            addProductInputField()
        }
    }
    
    private fun loadTemplate(templateId: Long) {
        lifecycleScope.launch {
            templateRepository.getTemplateById(templateId).collect { template ->
                if (template != null) {
                    selectedTemplate = template
                    updateUI()
                }
            }
        }
    }
    
    private fun loadDefaultTemplate() {
        lifecycleScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
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
            currentInputField?.setText("")
            return
        }
        
        lifecycleScope.launch {
            try {
                // Check if already in current session
                if (scannedSerials.contains(scannedValue)) {
                    showStatus("⚠️ Już zeskanowano: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }
                
                // Check if serial number already exists in database
                val existing = productRepository.getProductBySerialNumber(scannedValue)
                if (existing != null) {
                    showStatus("⚠️ SN już istnieje: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }
                
                // Create product from template
                val template = selectedTemplate!!
                
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
                scannedSerials.add(scannedValue)
                updateStats()
                
                // Record successful scan
                scanHistoryRepository.recordScan(
                    scannedValue = scannedValue,
                    scanType = ScanType.SERIAL_NUMBER,
                    context = "bulk_add",
                    success = true
                )
                
                // Visual feedback
                showStatus("✅ Dodano: $scannedValue")
                
                // Mark current field as processed and add new one
                currentInputField?.isEnabled = false
                addProductInputField()
                
            } catch (e: Exception) {
                showStatus("❌ Błąd: ${e.message}")
                currentInputField?.setText("")
                
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
                scannedSerials.clear()
                binding.productsInputContainer.removeAllViews()
                addProductInputField()
                updateStats()
                
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd zapisu: ${e.message}", Toast.LENGTH_LONG).show()
                
                // Clear on error too
                scannedProducts.clear()
                scannedSerials.clear()
                binding.productsInputContainer.removeAllViews()
                addProductInputField()
                updateStats()
            }
        }
    }
    
    private fun updateStats() {
        binding.scannedCountText.text = "Zeskanowano: ${scannedProducts.size}"
    }
    
    private fun showStatus(message: String) {
        activity?.runOnUiThread {
            binding.lastScannedText.text = message
        }
    }
    
    private fun updateUI() {
        selectedTemplate?.let { template ->
            lifecycleScope.launch {
                val category = categoryRepository.getCategoryById(template.categoryId).firstOrNull()
                val categoryIcon = category?.icon ?: "📦"
                binding.templateEmojiText.text = categoryIcon
                binding.selectedTemplateText.text = template.name
            }
        } ?: run {
            binding.templateEmojiText.text = "❓"
            binding.selectedTemplateText.text = "Brak wybranego wzoru"
        }
    }
    
    private fun openTemplateSelection() {
        lifecycleScope.launch {
            val templates = templateRepository.getAllTemplates().firstOrNull() ?: emptyList()
            
            if (templates.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Brak szablonów. Najpierw utwórz szablon produktu.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            
            // Create dialog with template selection
            val templateNames = templates.map { it.name }.toTypedArray()
            val currentIndex = templates.indexOfFirst { it.id == selectedTemplate?.id }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Wybierz szablon produktu")
                .setSingleChoiceItems(templateNames, currentIndex) { dialog, which ->
                    selectedTemplate = templates[which]
                    updateUI()
                    dialog.dismiss()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
