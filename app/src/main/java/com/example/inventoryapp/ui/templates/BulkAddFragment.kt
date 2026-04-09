package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
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
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.data.local.entities.ScanType
import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.databinding.FragmentBulkAddBinding
import com.example.inventoryapp.ui.warehouse.LocationStorage
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
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

    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }
    
    private val locationStorage by lazy { LocationStorage(requireContext()) }
    
    private var selectedTemplate: ProductTemplateEntity? = null
    private var selectedStatus: ProductStatus = ProductStatus.IN_STOCK
    private var selectedEmployeeId: Long? = null
    private var selectedLocation: String? = null
    private var employees: List<EmployeeEntity> = emptyList()
    private val scannedProducts = mutableListOf<ProductEntity>()
    private val scannedSerials = mutableSetOf<String>()
    private var currentInputField: TextInputEditText? = null
    private val assignmentValidator = AssignmentValidator()

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
        setupStatusDropdown()
        setupLocationsDropdown()
        observeEmployees()
        
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

    private fun setupStatusDropdown() {
        val statusLabels = mapOf(
            ProductStatus.IN_STOCK to "Magazyn",
            ProductStatus.ASSIGNED to "Przypisane",
            ProductStatus.UNASSIGNED to "Brak przypisania",
            ProductStatus.IN_REPAIR to "Serwis",
            ProductStatus.RETIRED to "Wycofane",
            ProductStatus.LOST to "Zaginione"
        )
        val selectableStatusLabels = statusLabels.filterKeys { it != ProductStatus.UNASSIGNED }
        val statusNames = selectableStatusLabels.values.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusNames)
        binding.bulkStatusInput.setAdapter(adapter)
        binding.bulkStatusInput.setText(statusLabels[selectedStatus] ?: "Magazyn", false)

        binding.bulkStatusInput.setOnItemClickListener { _, _, position, _ ->
            val label = statusNames[position]
            selectedStatus = selectableStatusLabels.entries.firstOrNull { it.value == label }?.key ?: ProductStatus.IN_STOCK
            toggleEmployeeVisibility()
        }

        toggleEmployeeVisibility()
    }

    private fun observeEmployees() {
        viewLifecycleOwner.lifecycleScope.launch {
            employeeRepository.getAllEmployeesFlow().collect { list ->
                employees = list
                val names = list.map { it.fullName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.bulkEmployeeInput.setAdapter(adapter)

                // Preselect if we already have an id
                selectedEmployeeId?.let { id ->
                    val index = employees.indexOfFirst { it.id == id }
                    if (index >= 0) {
                        binding.bulkEmployeeInput.setText(employees[index].fullName, false)
                    }
                }
            }
        }
    }

    private fun setupLocationsDropdown() {
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                val fromProducts = products
                    .mapNotNull { it.shelf }
                    .filter { it.isNotBlank() }
                    .distinct()
                val fromStorage = locationStorage.getLocations()
                val locations = (fromProducts + fromStorage.toList()).distinct().sorted()

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locations.toList())
                binding.bulkLocationInput.setAdapter(adapter)

                binding.bulkLocationInput.setOnItemClickListener { _, _, position, _ ->
                    selectedLocation = locations.toList().getOrNull(position)
                }
            }
        }
    }

    private fun toggleEmployeeVisibility() {
        val showEmployee = selectedStatus == ProductStatus.ASSIGNED
        val showLocation = selectedStatus == ProductStatus.IN_STOCK
        
        binding.bulkEmployeeLayout.visibility = if (showEmployee) View.VISIBLE else View.GONE
        binding.bulkEmployeeHint.visibility = if (showEmployee) View.VISIBLE else View.GONE
        binding.bulkLocationLayout.visibility = if (showLocation) View.VISIBLE else View.GONE
        binding.bulkLocationHint.visibility = if (showLocation) View.VISIBLE else View.GONE
        
        if (!showEmployee) {
            selectedEmployeeId = null
            binding.bulkEmployeeInput.setText("")
        }
        if (!showLocation) {
            selectedLocation = null
            binding.bulkLocationInput.setText("")
        }

        if (showEmployee) {
            binding.bulkEmployeeInput.setOnItemClickListener { _, _, position, _ ->
                selectedEmployeeId = employees.getOrNull(position)?.id
            }
        } else {
            binding.bulkEmployeeInput.onItemClickListener = null
        }
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
                override fun beforeTextChanged(_s: CharSequence?, _start: Int, _count: Int, _after: Int) {}
                override fun onTextChanged(_s: CharSequence?, _start: Int, _before: Int, _count: Int) {}
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

        if (selectedStatus == ProductStatus.ASSIGNED && selectedEmployeeId == null) {
            Toast.makeText(requireContext(), "Wybierz pracownika dla statusu Przypisane", Toast.LENGTH_SHORT).show()
            binding.bulkEmployeeLayout.error = "Wymagany pracownik"
            return
        } else {
            binding.bulkEmployeeLayout.error = null
        }

        if (selectedStatus == ProductStatus.IN_STOCK && selectedLocation.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Wybierz lokalizację dla statusu Magazyn", Toast.LENGTH_SHORT).show()
            binding.bulkLocationLayout.error = "Wymagana lokalizacja"
            return
        } else {
            binding.bulkLocationLayout.error = null
        }
        
        lifecycleScope.launch {
            try {
                if (selectedStatus == ProductStatus.ASSIGNED) {
                    val employee = employees.firstOrNull { it.id == selectedEmployeeId }
                    if (employee == null) {
                        Toast.makeText(requireContext(), "Nie znaleziono pracownika", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val categoryId = selectedTemplate?.categoryId
                    val category = categoryRepository.getAllCategories().first().firstOrNull { it.id == categoryId }
                    val productForValidation = ProductEntity(
                        name = selectedTemplate?.name ?: "Produkt",
                        serialNumber = scannedValue,
                        categoryId = categoryId
                    )

                    when (val result = assignmentValidator.canAssignToEmployee(productForValidation, employee, category)) {
                        is AssignmentValidator.ValidationResult.Success -> Unit
                        is AssignmentValidator.ValidationResult.Error -> {
                            showStatus("❌ ${result.message}")
                            currentInputField?.setText("")
                            return@launch
                        }
                    }
                }

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
                val now = System.currentTimeMillis()
                val shelf = if (selectedStatus == ProductStatus.IN_STOCK) selectedLocation?.substringBefore("/")?.trim() else null
                val bin = if (selectedStatus == ProductStatus.IN_STOCK) selectedLocation?.substringAfter("/", "")?.trim()?.takeIf { it.isNotEmpty() } else null
                val locationName = if (selectedStatus == ProductStatus.IN_STOCK) {
                    selectedLocation
                } else {
                    null
                }
                val employeeName = employees.firstOrNull { it.id == selectedEmployeeId }?.fullName
                val historyEntry = when (selectedStatus) {
                    ProductStatus.ASSIGNED -> MovementHistoryUtils.entryForEmployee(employeeName)
                    ProductStatus.IN_STOCK -> MovementHistoryUtils.entryForLocation(locationName)
                    ProductStatus.UNASSIGNED -> MovementHistoryUtils.entryUnassigned()
                    ProductStatus.IN_REPAIR -> MovementHistoryUtils.entryForStatus("Serwis")
                    ProductStatus.RETIRED -> MovementHistoryUtils.entryForStatus("Wycofane")
                    ProductStatus.LOST -> MovementHistoryUtils.entryForStatus("Zaginione")
                }
                val newProduct = ProductEntity(
                    name = "${template.name} - $scannedValue",
                    serialNumber = scannedValue,
                    categoryId = template.categoryId,
                    manufacturer = template.defaultManufacturer,
                    model = template.defaultModel,
                    description = template.defaultDescription,
                    status = selectedStatus,
                    shelf = shelf,
                    bin = bin,
                    assignedToEmployeeId = if (selectedStatus == ProductStatus.ASSIGNED) selectedEmployeeId else null,
                    assignmentDate = if (selectedStatus == ProductStatus.ASSIGNED && selectedEmployeeId != null) now else null,
                    movementHistory = historyEntry?.let { MovementHistoryUtils.append(null, it) }
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
