package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.example.inventoryapp.databinding.FragmentEmployeeDetailsBinding
import com.example.inventoryapp.databinding.BottomSheetDeleteEmployeeConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmployeeDetailsFragment : Fragment() {

    private var _binding: FragmentEmployeeDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository
    private lateinit var companyRepository: com.example.inventoryapp.data.repository.CompanyRepository
    private lateinit var categoryRepository: com.example.inventoryapp.data.repository.CategoryRepository
    private val args: EmployeeDetailsFragmentArgs by navArgs()
    
    private var currentEmployee: EmployeeEntity? = null
    private lateinit var assignedProductsAdapter: AssignedProductsAdapter
    private val assignmentValidator = AssignmentValidator()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        employeeRepository = app.employeeRepository
        productRepository = app.productRepository
        companyRepository = app.companyRepository
        categoryRepository = app.categoryRepository

        setupRecyclerView()
        setupButtons()
        loadEmployeeDetails()
    }

    private fun setupRecyclerView() {
        assignedProductsAdapter = AssignedProductsAdapter(
            onProductClick = { product ->
                val action = EmployeeDetailsFragmentDirections
                    .actionEmployeeDetailsToProductDetails(product.id)
                findNavController().navigate(action)
            },
            onUnassignClick = { product ->
                showUnassignConfirmation(product)
            }
        )

        binding.assignedEquipmentRecyclerView.apply {
            adapter = assignedProductsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupButtons() {
        binding.editButton.setOnClickListener {
            val action = EmployeeDetailsFragmentDirections
                .actionEmployeeDetailsToAddEmployee(args.employeeId)
            findNavController().navigate(action)
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.assignEquipmentButton.setOnClickListener {
            showAssignEquipmentDialog()
        }

        binding.assignEquipmentScanButton.setOnClickListener {
            val action = EmployeeDetailsFragmentDirections
                .actionEmployeeDetailsToAssignByScan(
                    employeeId = args.employeeId,
                    locationName = null,
                    contractorPointId = -1L
                )
            findNavController().navigate(action)
        }
    }

    private fun loadEmployeeDetails() {
        lifecycleScope.launch {
            currentEmployee = employeeRepository.getEmployeeById(args.employeeId)
            currentEmployee?.let { employee ->
                displayEmployeeInfo(employee)
                loadAssignedProducts(employee.id)
            } ?: run {
                Toast.makeText(requireContext(), "Nie znaleziono pracownika", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun displayEmployeeInfo(employee: EmployeeEntity) {
        binding.apply {
            val initials = "${employee.firstName.firstOrNull() ?: ""}${employee.lastName.firstOrNull() ?: ""}".uppercase()
            employeeInitials.text = initials
            employeeName.text = employee.fullName
            
            val positionText = buildString {
                if (!employee.position.isNullOrBlank()) append(employee.position)
                if (!employee.department.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append(employee.department)
                }
            }
            employeePosition.text = positionText.ifBlank { "Brak stanowiska" }

            lifecycleScope.launch {
                val companyName = employee.companyId?.let { companyRepository.getCompanyById(it)?.name }
                employeeCompany.text = companyName?.let { "🏢 $it" } ?: "🏢 Brak przypisanej firmy"
            }
            
            employeeEmail.text = if (!employee.email.isNullOrBlank()) "📧 ${employee.email}" else "Brak emaila"
            employeeEmail.isVisible = !employee.email.isNullOrBlank()
            
            employeePhone.text = if (!employee.phone.isNullOrBlank()) "📱 ${employee.phone}" else "Brak telefonu"
            employeePhone.isVisible = !employee.phone.isNullOrBlank()
            
            contactCard.isVisible = !employee.email.isNullOrBlank() || !employee.phone.isNullOrBlank()
            
            if (!employee.notes.isNullOrBlank()) {
                employeeNotes.text = employee.notes
                notesCard.isVisible = true
            } else {
                notesCard.isVisible = false
            }
        }
    }

    private fun loadAssignedProducts(employeeId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getProductsAssignedToEmployee(employeeId).collect { products ->
                    val safeBinding = _binding ?: return@collect
                    assignedProductsAdapter.submitList(products)

                    safeBinding.assignedCountText.text = when (products.size) {
                        0 -> "Brak przypisanych urządzeń"
                        1 -> "1 urządzenie przypisane"
                        in 2..4 -> "${products.size} urządzenia przypisane"
                        else -> "${products.size} urządzeń przypisanych"
                    }

                    safeBinding.assignedEquipmentRecyclerView.isVisible = products.isNotEmpty()
                    safeBinding.noEquipmentText.isVisible = products.isEmpty()
                }
            }
        }
    }

    private fun showAssignEquipmentDialog() {
        currentEmployee?.let { employee ->
            val dialog = AssignEquipmentDialogFragment(employee.id) { selectedProducts ->
                assignProducts(selectedProducts)
            }
            dialog.show(childFragmentManager, "AssignEquipmentDialog")
        }
    }

    private fun assignProducts(products: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                currentEmployee?.let { employee ->
                    val categoriesById = categoryRepository.getAllCategories().first().associateBy { it.id }
                    for (product in products) {
                        val category = categoriesById[product.categoryId]
                        when (val result = assignmentValidator.canAssignToEmployee(product, employee, category)) {
                            is AssignmentValidator.ValidationResult.Success -> Unit
                            is AssignmentValidator.ValidationResult.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                        }
                    }

                    products.forEach { product ->
                        val base = product.copy(
                            assignedToEmployeeId = employee.id,
                            assignmentDate = System.currentTimeMillis(),
                            status = ProductStatus.ASSIGNED,
                            shelf = null,
                            bin = null
                        )
                        productRepository.updateWithHistory(
                            base,
                            MovementHistoryUtils.entryForEmployee(employee.fullName)
                        )
                    }
                    Toast.makeText(
                        requireContext(),
                        "Przypisano ${products.size} urządzeń",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUnassignConfirmation(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń przypisanie sprzętu")
            .setMessage("Czy na pewno chcesz usunąć przypisanie?\n\n${product.name}\nS/N: ${product.serialNumber}")
            .setPositiveButton("Usuń przypisanie") { _, _ ->
                unassignProduct(product)
            }
            .setNegativeButton("Anuluj", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun unassignProduct(product: ProductEntity) {
        lifecycleScope.launch {
            try {
                productRepository.unassignFromEmployee(product.id)
                Toast.makeText(requireContext(), "Usunięto przypisanie", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val employee = currentEmployee ?: return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDeleteEmployeeConfirmBinding.inflate(layoutInflater)
        
        // Set employee name
        sheetBinding.employeeNameText.text = employee.fullName
        
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
                    
                    deleteEmployee()
                    bottomSheet.dismiss()
                }
                .start()
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun deleteEmployee() {
        lifecycleScope.launch {
            try {
                currentEmployee?.let { employee ->
                    // First unassign all products
                    val assignedProducts = productRepository.getProductsAssignedToEmployee(employee.id).first()
                    assignedProducts.forEach { product ->
                        productRepository.unassignFromEmployee(product.id)
                    }
                    
                    // Then delete employee
                    employeeRepository.deleteEmployee(employee)
                    Toast.makeText(requireContext(), "Usunięto pracownika", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
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
