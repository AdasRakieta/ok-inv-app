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
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.domain.validators.StorageTargetValidator
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.example.inventoryapp.databinding.FragmentAddProductBinding
import com.example.inventoryapp.ui.warehouse.LocationStorage
import kotlinx.coroutines.flow.firstOrNull
import androidx.core.widget.addTextChangedListener
import kotlinx.coroutines.launch

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val args: AddProductFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private var categories: List<CategoryEntity> = emptyList()
    private var leafCategories: List<CategoryEntity> = emptyList()
    private var employees: List<EmployeeEntity> = emptyList()
    private var contractorPoints: List<ContractorPointEntity> = emptyList()
    private var currentProduct: ProductEntity? = null
    private var selectedEmployeeId: Long? = null
    private var selectedContractorPointId: Long? = null
    private var selectedStatus: ProductStatus = ProductStatus.IN_STOCK
    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }
    private var boxes: List<BoxEntity> = emptyList()
    private val contractorPointRepository by lazy { (requireActivity().application as InventoryApplication).contractorPointRepository }
    private var storageTypeIsBox: Boolean = false
    private var selectedBoxId: Long? = null
    private val assignmentValidator = AssignmentValidator()
    private val storageTargetValidator = StorageTargetValidator()

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

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveProduct()
        }

        setupCategories()
        setupStatusDropdown()
        setupEmployeesDropdown()
        setupContractorPointsDropdown()
        setupBoxesDropdown()
        setupWarehouseLocationsDropdown()
        loadProductIfEditing()

        binding.storageTypeGroup.setOnCheckedChangeListener { _, checkedId ->
            storageTypeIsBox = checkedId == com.example.inventoryapp.R.id.storageTypeBox
            binding.warehouseLocationLayout.error = null
            binding.boxLayout.error = null
            toggleEmployeeField()
        }

        // Clear layout errors when user edits inputs so error icon/message is removed
        binding.serialNumberInput.addTextChangedListener { _ -> binding.serialNumberLayout.error = null }
        binding.productIdInput.addTextChangedListener { _ -> binding.productIdLayout.error = null }
        binding.warehouseLocationInput.addTextChangedListener { _ -> binding.warehouseLocationLayout.error = null }
        binding.boxInput.addTextChangedListener { _ -> binding.boxLayout.error = null }
    }

    private fun setupCategories() {
        lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                categories = list
                // Leaf categories are those that are not parents of any other category
                leafCategories = list.filter { candidate -> list.none { it.parentId == candidate.id } }
                val names = leafCategories.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.categoryInput.setAdapter(adapter)

                val targetCategoryId = currentProduct?.categoryId
                // Try to prefill with the exact leaf category; if product has a parent category selected,
                // fallback to first child of that parent so the input contains a selectable leaf value.
                val prefill = when {
                    targetCategoryId == null -> null
                    else -> leafCategories.firstOrNull { it.id == targetCategoryId }?.name
                } ?: categories.firstOrNull { it.parentId == targetCategoryId }?.name

                when {
                    prefill != null -> binding.categoryInput.setText(prefill, false)
                    binding.categoryInput.text.isNullOrEmpty() && names.isNotEmpty() -> binding.categoryInput.setText(names.first(), false)
                }
                binding.categoryInput.setOnItemClickListener { _, _, _, _ ->
                    toggleFixedIdField()
                }

                // Ensure fixedId visibility matches current category value
                toggleFixedIdField()
            }
        }
        // category dropdown and fixed-id toggling handled above; contractor-specific
        // assignment validation is performed in saveProduct() where user inputs exist.
    }
    
    private fun setupStatusDropdown() {
        val statusLabels = mapOf(
            ProductStatus.IN_STOCK to "Magazyn",
            ProductStatus.ASSIGNED to "Przypisane",
            ProductStatus.CONTRACTOR to "Wydane do kontrahenta",
            ProductStatus.UNASSIGNED to "Brak przypisania",
            ProductStatus.IN_REPAIR to "Serwis",
            ProductStatus.RETIRED to "Wycofane",
            ProductStatus.LOST to "Zaginione"
        )
        val selectableStatusLabels = statusLabels.filterKeys { it != ProductStatus.UNASSIGNED }
        val statusNames = selectableStatusLabels.values.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusNames)
        binding.statusInput.setAdapter(adapter)

        val currentLabel = statusLabels[selectedStatus] ?: "Magazyn"
        binding.statusInput.setText(currentLabel, false)

        binding.statusInput.setOnItemClickListener { _, _, position, _ ->
            val label = statusNames[position]
            selectedStatus = selectableStatusLabels.entries.firstOrNull { it.value == label }?.key ?: ProductStatus.IN_STOCK
            toggleEmployeeField()
        }

        toggleEmployeeField()
    }

    private fun setupEmployeesDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            employeeRepository.getAllEmployeesFlow().collect { list ->
                employees = list
                val names = list.map { it.fullName }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.assignedEmployeeInput.setAdapter(adapter)

                selectedEmployeeId?.let { id ->
                    val idx = employees.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        binding.assignedEmployeeInput.setText(employees[idx].fullName, false)
                    }
                }

                binding.assignedEmployeeInput.setOnItemClickListener { _, _, position, _ ->
                    selectedEmployeeId = employees.getOrNull(position)?.id
                }
            }
        }
    }

    private fun setupContractorPointsDropdown() {
        viewLifecycleOwner.lifecycleScope.launch {
            contractorPointRepository.getAllContractorPointsFlow().collect { list ->
                contractorPoints = list
                val names = list.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.assignedContractorInput.setAdapter(adapter)

                selectedContractorPointId?.let { id ->
                    val idx = contractorPoints.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        binding.assignedContractorInput.setText(contractorPoints[idx].name, false)
                    }
                }

                binding.assignedContractorInput.setOnItemClickListener { _, _, position, _ ->
                    selectedContractorPointId = contractorPoints.getOrNull(position)?.id
                }
            }
        }
    }

    private fun toggleEmployeeField() {
        val showEmployee = selectedStatus == ProductStatus.ASSIGNED
        binding.assignedEmployeeContainer.visibility = if (showEmployee) View.VISIBLE else View.GONE
        if (!showEmployee) {
            selectedEmployeeId = null
            binding.assignedEmployeeInput.setText("")
        }
        val showContractor = selectedStatus == ProductStatus.CONTRACTOR
        binding.assignedContractorContainer.visibility = if (showContractor) View.VISIBLE else View.GONE
        if (!showContractor) {
            selectedContractorPointId = null
            binding.assignedContractorInput.setText("")
        }
        
        val showLocation = selectedStatus == ProductStatus.IN_STOCK
        // Show/hide storage type selector (Lokalizacja/Karton) only for IN_STOCK
        binding.storageTypeGroup.visibility = if (showLocation) View.VISIBLE else View.GONE

        // Show warehouse location when storageType is set to Location
        binding.warehouseLocationContainer.visibility = if (showLocation && !storageTypeIsBox) View.VISIBLE else View.GONE
        // Show box selector when storageType is set to Box
        binding.boxContainer.visibility = if (showLocation && storageTypeIsBox) View.VISIBLE else View.GONE

        if (!showLocation) {
            // reset storage selection & clear inputs when not in warehouse status
            storageTypeIsBox = false
            // ensure default selection is 'Lokalizacja'
            binding.storageTypeLocation.isChecked = true
            binding.warehouseLocationInput.setText("")
            binding.boxInput.setText("")
            selectedBoxId = null
        }
    }

    private fun toggleFixedIdField() {
        val categoryName = binding.categoryInput.text.toString().trim()
        val showFixed = categoryName.equals("Skaner", ignoreCase = true)
        binding.fixedIdContainer.visibility = if (showFixed) View.VISIBLE else View.GONE
        if (!showFixed) {
            binding.fixedIdInput.setText("")
        }
    }

    private fun setupBoxesDropdown() {
        lifecycleScope.launch {
            boxRepository.getAllBoxes().collect { list ->
                boxes = list
                val names = boxes.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.boxInput.setAdapter(adapter)

                val product = currentProduct
                if (product != null && product.boxId != null) {
                    val box = boxes.firstOrNull { it.id == product.boxId }
                    if (box != null) {
                        binding.boxInput.setText(box.name, false)
                        selectedBoxId = box.id
                        storageTypeIsBox = true
                        binding.storageTypeBox.isChecked = true
                        toggleEmployeeField()
                    }
                }

                binding.boxInput.setOnItemClickListener { _, _, position, _ ->
                    selectedBoxId = boxes.getOrNull(position)?.id
                }
            }
        }
    }

    private fun setupWarehouseLocationsDropdown() {
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                // Get locations from products (format: "Shelf / Bin")
                val fromProducts = products
                    .filter { !it.shelf.isNullOrBlank() && !it.bin.isNullOrBlank() }
                    .map { "${it.shelf} / ${it.bin}" }
                    .distinct()
                val fromStorage = locationStorage.getLocations()
                val locations = (fromProducts + fromStorage.toList()).distinct().sorted()

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, locations.toList())
                binding.warehouseLocationInput.setAdapter(adapter)
                
                // Prefill current product location if editing
                val product = currentProduct
                if (product != null && !product.shelf.isNullOrBlank() && !product.bin.isNullOrBlank()) {
                    val currentLocation = "${product.shelf} / ${product.bin}"
                    if (locations.contains(currentLocation)) {
                        binding.warehouseLocationInput.setText(currentLocation, false)
                    }
                }
            }
        }
    }

    private fun loadProductIfEditing() {
        val productId = args.productId
        if (productId <= 0) return
        lifecycleScope.launch {
            val product = productRepository.getProductById(productId).firstOrNull()
            if (product != null) {
                currentProduct = product
                binding.titleText.text = "Edytuj produkt"
                binding.saveButton.text = "Zapisz zmiany"
                binding.productNameInput.setText(product.name)
                binding.productIdInput.setText(product.customId ?: "")
                binding.serialNumberInput.setText(product.serialNumber)
                binding.manufacturerInput.setText(product.manufacturer ?: "")
                binding.modelInput.setText(product.model ?: "")
                binding.descriptionInput.setText(product.description ?: "")
                binding.fixedIdInput.setText(product.fixedId ?: "")

                selectedStatus = product.status
                selectedEmployeeId = product.assignedToEmployeeId
                selectedContractorPointId = product.assignedToContractorPointId

                // Set category text when categories already loaded
                val targetCategory = categories.firstOrNull { it.id == product.categoryId }
                targetCategory?.name?.let { binding.categoryInput.setText(it, false) }

                setupStatusDropdown()
            } else {
                Toast.makeText(requireContext(), "Nie znaleziono produktu", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
    
    private fun saveProduct() {
        val name = binding.productNameInput.text.toString().trim()
        val customId = binding.productIdInput.text.toString().trim().ifEmpty { null }
        val serialNumber = binding.serialNumberInput.text.toString().trim()
        val categoryName = binding.categoryInput.text.toString().trim()
        val categoryId = leafCategories.firstOrNull { it.name == categoryName }?.id
        val manufacturer = binding.manufacturerInput.text.toString().trim().ifEmpty { null }
        val model = binding.modelInput.text.toString().trim().ifEmpty { null }
        val description = binding.descriptionInput.text.toString().trim().ifEmpty { null }
        val fixedId = binding.fixedIdInput.text.toString().trim().ifEmpty { null }
        val warehouseLocation = binding.warehouseLocationInput.text.toString().trim().ifEmpty { null }
        // Parse shelf and bin from warehouse location (format: "Shelf / Bin")
        val shelf = warehouseLocation?.substringBefore("/")?.trim()
        val bin = warehouseLocation?.substringAfter("/", "")?.trim()?.takeIf { it.isNotEmpty() }
        
        // Get selected status
        val statusLabel = binding.statusInput.text.toString().trim()
        selectedStatus = when (statusLabel) {
            "Magazyn" -> ProductStatus.IN_STOCK
            "Przypisane" -> ProductStatus.ASSIGNED
            "Wydane do kontrahenta" -> ProductStatus.CONTRACTOR
            "Brak przypisania" -> ProductStatus.UNASSIGNED
            "Serwis" -> ProductStatus.IN_REPAIR
            "Wycofane" -> ProductStatus.RETIRED
            "Zaginione" -> ProductStatus.LOST
            else -> ProductStatus.IN_STOCK
        }

        if (selectedStatus == ProductStatus.ASSIGNED && selectedEmployeeId == null) {
            Toast.makeText(requireContext(), "Wybierz pracownika dla statusu Przypisane", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStatus == ProductStatus.ASSIGNED) {
            val employee = employees.firstOrNull { it.id == selectedEmployeeId }
            if (employee == null) {
                Toast.makeText(requireContext(), "Nie znaleziono pracownika", Toast.LENGTH_SHORT).show()
                return
            }

            val category = categories.firstOrNull { it.id == categoryId }
            val productForValidation = ProductEntity(
                id = currentProduct?.id ?: 0L,
                name = name,
                serialNumber = serialNumber,
                categoryId = categoryId
            )

            when (val result = assignmentValidator.canAssignToEmployee(productForValidation, employee, category)) {
                is AssignmentValidator.ValidationResult.Success -> Unit
                is AssignmentValidator.ValidationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        // Contractor assignment validation: ensure a contractor point is selected
        // and the product can be assigned to that contractor point.
        if (selectedStatus == ProductStatus.CONTRACTOR && selectedContractorPointId == null) {
            Toast.makeText(requireContext(), "Wybierz punkt kontrahenta dla statusu Wydane do kontrahenta", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStatus == ProductStatus.CONTRACTOR) {
            val category = categories.firstOrNull { it.id == categoryId }
            val productForValidation = ProductEntity(
                id = currentProduct?.id ?: 0L,
                name = name,
                serialNumber = serialNumber,
                categoryId = categoryId
            )

            when (val result = assignmentValidator.canAssignToContractorPoint(productForValidation, category)) {
                is AssignmentValidator.ValidationResult.Success -> Unit
                is AssignmentValidator.ValidationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    return
                }
            }
        }

        when (val storageValidation = storageTargetValidator.validate(
            status = selectedStatus,
            storageTypeIsBox = storageTypeIsBox,
            warehouseLocation = warehouseLocation,
            selectedBoxId = selectedBoxId
        )) {
            is StorageTargetValidator.ValidationResult.Success -> {
                binding.warehouseLocationLayout.error = null
                binding.boxLayout.error = null
            }
            is StorageTargetValidator.ValidationResult.Error -> {
                if (selectedStatus == ProductStatus.IN_STOCK) {
                    if (storageTypeIsBox) {
                        binding.boxLayout.error = "Wymagany karton"
                    } else {
                        binding.warehouseLocationLayout.error = "Wymagana lokalizacja"
                    }
                }
                Toast.makeText(requireContext(), storageValidation.message, Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Nazwa produktu jest wymagana", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (serialNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Numer seryjny jest wymagany", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryId == null) {
            Toast.makeText(requireContext(), "Wybierz kategorię", Toast.LENGTH_SHORT).show()
            return
        }
        
        val existing = currentProduct
        val now = System.currentTimeMillis()
        val assignmentDate = if (selectedStatus == ProductStatus.ASSIGNED) {
            if (existing?.assignedToEmployeeId == selectedEmployeeId && existing?.assignmentDate != null) {
                existing.assignmentDate
            } else {
                now
            }
        } else {
            null
        }
        val product = if (existing == null) {
            ProductEntity(
                name = name,
                customId = customId,
                fixedId = fixedId,
                serialNumber = serialNumber,
                categoryId = categoryId,
                status = selectedStatus,
                manufacturer = manufacturer,
                model = model,
                description = description,
                // Only set shelf/bin when storing directly in location
                shelf = if (selectedStatus == ProductStatus.IN_STOCK && !storageTypeIsBox) shelf else null,
                bin = if (selectedStatus == ProductStatus.IN_STOCK && !storageTypeIsBox) bin else null,
                // If storing in box, set boxId; otherwise clear
                boxId = if (selectedStatus == ProductStatus.IN_STOCK && storageTypeIsBox) selectedBoxId else null,
                assignedToEmployeeId = if (selectedStatus == ProductStatus.ASSIGNED) selectedEmployeeId else null,
                assignedToContractorPointId = if (selectedStatus == ProductStatus.CONTRACTOR) selectedContractorPointId else null,
                assignmentDate = assignmentDate,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                name = name,
                customId = customId,
                fixedId = fixedId,
                serialNumber = serialNumber,
                categoryId = categoryId,
                status = selectedStatus,
                manufacturer = manufacturer,
                model = model,
                description = description,
                shelf = if (selectedStatus == ProductStatus.IN_STOCK && !storageTypeIsBox) shelf else null,
                bin = if (selectedStatus == ProductStatus.IN_STOCK && !storageTypeIsBox) bin else null,
                boxId = if (selectedStatus == ProductStatus.IN_STOCK && storageTypeIsBox) selectedBoxId else null,
                assignedToEmployeeId = if (selectedStatus == ProductStatus.ASSIGNED) selectedEmployeeId else null,
                assignedToContractorPointId = if (selectedStatus == ProductStatus.CONTRACTOR) selectedContractorPointId else null,
                assignmentDate = assignmentDate,
                updatedAt = now
            )
        }

        val locationName = if (selectedStatus == ProductStatus.IN_STOCK && !shelf.isNullOrBlank()) {
            shelf + (if (!bin.isNullOrBlank()) " / $bin" else "")
        } else {
            null
        }

        val employeeName = employees.firstOrNull { it.id == selectedEmployeeId }?.fullName
        val contractorName = contractorPoints.firstOrNull { it.id == selectedContractorPointId }?.name

        val statusEntry = when (selectedStatus) {
            ProductStatus.IN_STOCK -> MovementHistoryUtils.entryForLocation(locationName)
            ProductStatus.ASSIGNED -> MovementHistoryUtils.entryForEmployee(employeeName)
            ProductStatus.CONTRACTOR -> MovementHistoryUtils.entryForContractorPoint(contractorName)
            ProductStatus.UNASSIGNED -> MovementHistoryUtils.entryUnassigned()
            ProductStatus.IN_REPAIR -> MovementHistoryUtils.entryForStatus("Serwis")
            ProductStatus.RETIRED -> MovementHistoryUtils.entryForStatus("Wycofane")
            ProductStatus.LOST -> MovementHistoryUtils.entryForStatus("Zaginione")
        }

        val historyEntry = when {
            existing == null -> statusEntry
            selectedStatus != existing.status -> statusEntry
            selectedStatus == ProductStatus.ASSIGNED && selectedEmployeeId != existing.assignedToEmployeeId -> statusEntry
            selectedStatus == ProductStatus.CONTRACTOR && selectedContractorPointId != existing.assignedToContractorPointId -> statusEntry
            selectedStatus == ProductStatus.IN_STOCK && (existing.shelf != shelf || existing.bin != bin) -> statusEntry
            else -> null
        }

        val productWithHistory = if (historyEntry != null) {
            val baseHistory = existing?.movementHistory
            product.copy(movementHistory = MovementHistoryUtils.append(baseHistory, historyEntry))
        } else {
            product
        }
        
        lifecycleScope.launch {
            try {
                if (existing == null) {
                    productRepository.insertProduct(productWithHistory)
                    Toast.makeText(requireContext(), "Produkt dodany", Toast.LENGTH_SHORT).show()
                } else {
                    productRepository.updateProduct(productWithHistory)
                    Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                // Check if it's a customId conflict
                    if (e.message?.contains("customId") == true) {
                        binding.productIdLayout.error = "To ID jest już w użyciu"
                    } else if (e.message?.contains("serialNumber") == true) {
                        binding.serialNumberLayout.error = "Ten numer seryjny już istnieje"
                    } else {
                        Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
