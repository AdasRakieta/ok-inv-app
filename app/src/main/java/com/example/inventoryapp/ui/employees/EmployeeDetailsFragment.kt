package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.databinding.FragmentEmployeeDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EmployeeDetailsFragment : Fragment() {

    private var _binding: FragmentEmployeeDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository
    private val args: EmployeeDetailsFragmentArgs by navArgs()
    
    private var currentEmployee: EmployeeEntity? = null
    private lateinit var assignedProductsAdapter: AssignedProductsAdapter

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
        lifecycleScope.launch {
            productRepository.getProductsAssignedToEmployee(employeeId).collect { products ->
                assignedProductsAdapter.submitList(products)
                
                binding.assignedCountText.text = when (products.size) {
                    0 -> "Brak przypisanych urządzeń"
                    1 -> "1 urządzenie przypisane"
                    in 2..4 -> "${products.size} urządzenia przypisane"
                    else -> "${products.size} urządzeń przypisanych"
                }
                
                binding.assignedEquipmentRecyclerView.isVisible = products.isNotEmpty()
                binding.noEquipmentText.isVisible = products.isEmpty()
            }
        }
    }

    private fun showAssignEquipmentDialog() {
        lifecycleScope.launch {
            // Get all products that are not assigned
            val allProducts = productRepository.getAllProducts().first()
            val availableProducts = allProducts.filter { it.assignedToEmployeeId == null }
            
            if (availableProducts.isEmpty()) {
                Toast.makeText(requireContext(), "Brak dostępnego sprzętu do przypisania", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            val productNames = availableProducts.map { "${it.name} (S/N: ${it.serialNumber})" }.toTypedArray()
            val selectedProducts = mutableListOf<ProductEntity>()
            val checkedItems = BooleanArray(productNames.size) { false }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Przypisz sprzęt")
                .setMultiChoiceItems(productNames, checkedItems) { _, which, isChecked ->
                    if (isChecked) {
                        selectedProducts.add(availableProducts[which])
                    } else {
                        selectedProducts.remove(availableProducts[which])
                    }
                }
                .setPositiveButton("Przypisz (${selectedProducts.size})") { _, _ ->
                    if (selectedProducts.isNotEmpty()) {
                        assignProducts(selectedProducts)
                    }
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    private fun assignProducts(products: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                currentEmployee?.let { employee ->
                    products.forEach { product ->
                        productRepository.assignToEmployee(product.id, employee.id)
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
            .setTitle("Usuń przypisanie")
            .setMessage("Czy chcesz usunąć przypisanie: ${product.name}?")
            .setPositiveButton("Usuń") { _, _ ->
                unassignProduct(product)
            }
            .setNegativeButton("Anuluj", null)
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
        lifecycleScope.launch {
            val assignedCount = productRepository.getAssignedProductsCount(args.employeeId)
            
            val message = if (assignedCount > 0) {
                "Ten pracownik ma przypisanych $assignedCount urządzeń. Usunięcie pracownika spowoduje odłączenie całego sprzętu. Kontynuować?"
            } else {
                "Czy na pewno chcesz usunąć tego pracownika?"
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Usuń pracownika")
                .setMessage(message)
                .setPositiveButton("Usuń") { _, _ ->
                    deleteEmployee()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
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
