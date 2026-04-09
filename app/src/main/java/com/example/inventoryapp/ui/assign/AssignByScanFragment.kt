package com.example.inventoryapp.ui.assign

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
import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.databinding.FragmentAssignByScanBinding
import com.example.inventoryapp.ui.warehouse.LocationStorage
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssignByScanFragment : Fragment() {

    private var _binding: FragmentAssignByScanBinding? = null
    private val binding get() = _binding!!

    private val args: AssignByScanFragmentArgs by navArgs()

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }
    private val contractorPointRepository by lazy {
        (requireActivity().application as InventoryApplication).contractorPointRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val locationStorage by lazy { LocationStorage(requireContext()) }
    private val assignmentValidator = AssignmentValidator()

    private val scannedProducts = mutableListOf<ProductEntity>()
    private val scannedSerials = mutableSetOf<String>()
    private var currentInputField: TextInputEditText? = null

    private enum class TargetType { EMPLOYEE, LOCATION, CONTRACTOR_POINT }

    private val targetType: TargetType
        get() = when {
            args.contractorPointId > 0L -> TargetType.CONTRACTOR_POINT
            !args.locationName.isNullOrBlank() -> TargetType.LOCATION
            else -> TargetType.EMPLOYEE
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAssignByScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupHeader()
        setupActions()
        addProductInputField()
        updateStats()
    }

    private fun setupHeader() {
        when (targetType) {
            TargetType.EMPLOYEE -> {
                binding.targetIconText.text = "👤"
                binding.targetTitleText.text = "Pracownik"
                val employeeId = args.employeeId
                lifecycleScope.launch {
                    val employee = employeeRepository.getEmployeeById(employeeId)
                    binding.targetNameText.text = employee?.fullName ?: "Nieznany"
                }
            }
            TargetType.LOCATION -> {
                binding.targetIconText.text = "📦"
                binding.targetTitleText.text = "Lokalizacja"
                binding.targetNameText.text = args.locationName
            }
            TargetType.CONTRACTOR_POINT -> {
                binding.targetIconText.text = "🏢"
                binding.targetTitleText.text = "Punkt kontrahenta"
                val contractorPointId = args.contractorPointId
                lifecycleScope.launch {
                    val contractorPoint = contractorPointRepository.getContractorPointById(contractorPointId)
                    binding.targetNameText.text = contractorPoint?.name ?: "Nieznany"
                }
            }
        }
    }

    private fun setupActions() {
        binding.saveAllButton.setOnClickListener {
            saveAssignments()
        }
    }

    private fun addProductInputField() {
        if (binding.productsInputContainer.childCount > 0 &&
            currentInputField != null &&
            currentInputField?.isEnabled == true
        ) {
            currentInputField?.setText("")
            currentInputField?.requestFocus()
            return
        }

        val context = requireContext()
        val productNumber = scannedProducts.size + 1

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

        val inputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "$productNumber. Skanuj numer seryjny"
            setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
        }

        val editText = TextInputEditText(inputLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE

            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    val serialNumber = text.toString().trim()
                    handleScan(serialNumber)
                    true
                } else {
                    false
                }
            }

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

        inputLayout.addView(editText)
        horizontalContainer.addView(inputLayout)
        horizontalContainer.addView(deleteButton)

        binding.productsInputContainer.addView(horizontalContainer)
        currentInputField = editText
        editText.requestFocus()
    }

    private fun removeProductInputField(container: LinearLayout, editText: TextInputEditText) {
        val serialNumber = editText.text.toString().trim()
        if (serialNumber.isNotEmpty()) {
            scannedSerials.remove(serialNumber)
            val product = scannedProducts.find { it.serialNumber == serialNumber }
            if (product != null) {
                scannedProducts.remove(product)
            }
            showStatus("❌ Usunięto: $serialNumber")
            updateStats()
        }

        binding.productsInputContainer.removeView(container)

        if (currentInputField == editText) {
            currentInputField = null
        }

        if (binding.productsInputContainer.childCount == 0) {
            addProductInputField()
        }
    }

    private fun handleScan(scannedValue: String) {
        if (scannedValue.isEmpty()) return

        if (targetType == TargetType.EMPLOYEE && args.employeeId <= 0L) {
            Toast.makeText(requireContext(), "Brak wybranego pracownika", Toast.LENGTH_SHORT).show()
            currentInputField?.setText("")
            return
        }

        if (targetType == TargetType.LOCATION && args.locationName.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Brak wybranej lokalizacji", Toast.LENGTH_SHORT).show()
            currentInputField?.setText("")
            return
        }

        if (targetType == TargetType.CONTRACTOR_POINT && args.contractorPointId <= 0L) {
            Toast.makeText(requireContext(), "Brak wybranego punktu kontrahenta", Toast.LENGTH_SHORT).show()
            currentInputField?.setText("")
            return
        }

        lifecycleScope.launch {
            try {
                if (scannedSerials.contains(scannedValue)) {
                    showStatus("⚠️ Już zeskanowano: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }

                val existing = productRepository.getProductBySerialNumber(scannedValue)
                if (existing == null) {
                    showStatus("❌ Brak w bazie: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }

                if (targetType == TargetType.EMPLOYEE && existing.assignedToEmployeeId == args.employeeId && existing.status == ProductStatus.ASSIGNED) {
                    showStatus("ℹ️ Już przypisany: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }

                if (targetType == TargetType.LOCATION) {
                    val shelf = existing.shelf ?: ""
                    val bin = existing.bin ?: ""
                    val currentLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                    if (currentLocation == args.locationName && existing.status == ProductStatus.IN_STOCK) {
                        showStatus("ℹ️ Już w lokalizacji: $scannedValue")
                        currentInputField?.setText("")
                        return@launch
                    }
                }

                if (targetType == TargetType.CONTRACTOR_POINT &&
                    existing.assignedToContractorPointId == args.contractorPointId &&
                    existing.status == ProductStatus.ASSIGNED
                ) {
                    showStatus("ℹ️ Już przypisany do punktu: $scannedValue")
                    currentInputField?.setText("")
                    return@launch
                }

                scannedProducts.add(0, existing)
                scannedSerials.add(scannedValue)
                updateStats()

                showStatus("✅ Dodano: $scannedValue")
                currentInputField?.isEnabled = false
                addProductInputField()

            } catch (e: Exception) {
                showStatus("❌ Błąd: ${e.message}")
                currentInputField?.setText("")
            }
        }
    }

    private fun saveAssignments() {
        if (scannedProducts.isEmpty()) {
            Toast.makeText(requireContext(), "Brak produktów do przypisania", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val now = System.currentTimeMillis()
                if (targetType == TargetType.EMPLOYEE) {
                    val employee = employeeRepository.getEmployeeById(args.employeeId)
                    if (employee == null) {
                        Toast.makeText(requireContext(), "Nie znaleziono pracownika", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val categoriesById = categoryRepository.getAllCategories().first().associateBy { it.id }

                    for (product in scannedProducts) {
                        val category = categoriesById[product.categoryId]
                        when (val result = assignmentValidator.canAssignToEmployee(product, employee, category)) {
                            is AssignmentValidator.ValidationResult.Success -> Unit
                            is AssignmentValidator.ValidationResult.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                                return@launch
                            }
                        }
                    }

                    val employeeName = employee?.fullName
                    scannedProducts.forEach { product ->
                        val updated = product.copy(
                            assignedToEmployeeId = args.employeeId,
                            assignmentDate = now,
                            status = ProductStatus.ASSIGNED,
                            shelf = null,
                            bin = null,
                            boxId = null,
                            updatedAt = now
                        )
                        productRepository.updateWithHistory(
                            updated,
                            MovementHistoryUtils.entryForEmployee(employeeName)
                        )
                    }
                } else if (targetType == TargetType.LOCATION) {
                    val locationName = args.locationName ?: ""
                    val shelf = locationName.substringBefore("/").trim()
                    val bin = locationName.substringAfter("/", "").trim().takeIf { it.isNotEmpty() }

                    scannedProducts.forEach { product ->
                        val updated = product.copy(
                            shelf = shelf,
                            bin = bin,
                            boxId = null,
                            assignedToEmployeeId = null,
                            assignmentDate = null,
                            status = ProductStatus.IN_STOCK,
                            updatedAt = now
                        )
                        productRepository.updateWithHistory(
                            updated,
                            MovementHistoryUtils.entryForLocation(locationName)
                        )
                    }

                    locationStorage.addLocation(locationName)
                } else {
                    val contractorPoint = contractorPointRepository.getContractorPointById(args.contractorPointId)
                    if (contractorPoint == null) {
                        Toast.makeText(requireContext(), "Nie znaleziono punktu kontrahenta", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val categoriesById = categoryRepository.getAllCategories().first().associateBy { it.id }
                    for (product in scannedProducts) {
                        val category = categoriesById[product.categoryId]
                        when (val result = assignmentValidator.canAssignToContractorPoint(product, category)) {
                            is AssignmentValidator.ValidationResult.Success -> Unit
                            is AssignmentValidator.ValidationResult.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                                return@launch
                            }
                        }
                    }

                    scannedProducts.forEach { product ->
                        productRepository.assignToContractorPoint(
                            productId = product.id,
                            contractorPointId = contractorPoint.id,
                            contractorPointName = contractorPoint.name
                        )
                    }
                }

                Toast.makeText(requireContext(), "✓ Przypisano ${scannedProducts.size} produktów", Toast.LENGTH_LONG).show()

                scannedProducts.clear()
                scannedSerials.clear()
                binding.productsInputContainer.removeAllViews()
                addProductInputField()
                updateStats()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
