package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentEmployeesListBinding
import com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmployeesListFragment : Fragment() {

    private var _binding: FragmentEmployeesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository
    private lateinit var departmentRepository: com.example.inventoryapp.data.repository.DepartmentRepository
    private lateinit var adapter: EmployeesAdapter

    private val searchQueryFlow = MutableStateFlow("")
    private val departmentFilterFlow = MutableStateFlow<String?>(null)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        employeeRepository = app.employeeRepository
        departmentRepository = app.departmentRepository
        productRepository = app.productRepository

        setupRecyclerView()
        setupSearchBar()
        setupFilterButton()
        setupSortButton()
        setupBackButton()
        setupFab()
        setupSelectionPanel()
        observeEmployees()
    }

    private fun setupRecyclerView() {
        adapter = EmployeesAdapter(
            onEmployeeClick = { employee ->
                val action = EmployeesListFragmentDirections
                    .actionEmployeesListToEmployeeDetails(employee.id)
                findNavController().navigate(action)
            },
            onEmployeeLongClick = { employee ->
                adapter.selectionMode = true
                adapter.toggleSelection(employee.id)
                updateSelectionPanel()
                true
            }
        )

        binding.employeesRecyclerView.apply {
            this.adapter = this@EmployeesListFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { text ->
            searchQueryFlow.value = text?.toString() ?: ""
        }
    }

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showDepartmentFilterDialog()
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showDepartmentFilterDialog() {
        lifecycleScope.launch {
            val departments = departmentRepository.getAllNames()
            val options = listOf("Wszystkie") + departments
            val selectedIndex = if (departmentFilterFlow.value == null) 0 
                else departments.indexOf(departmentFilterFlow.value) + 1

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Filtruj po dziale")
                .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                    departmentFilterFlow.value = if (which == 0) null else departments[which - 1]
                    binding.filterButton.text = if (which == 0) "Dział" else departments[which - 1]
                    dialog.dismiss()
                }
                .setNegativeButton("Anuluj", null)
                .show()
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Alfabetycznie (A-Z)",
            "Alfabetycznie (Z-A)",
            "Najnowsi",
            "Najstarsi"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sortuj")
            .setItems(options) { dialog, which ->
                // TODO: Implement sorting
                Toast.makeText(requireContext(), "Sortowanie: ${options[which]}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFab() {
        binding.addEmployeeFab.setOnClickListener {
            findNavController().navigate(R.id.action_employeesList_to_addEmployee)
        }
    }

    private fun setupSelectionPanel() {
        binding.selectAllButton.setOnClickListener {
            adapter.selectAll()
            updateSelectionPanel()
        }

        binding.deleteSelectedButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun updateSelectionPanel() {
        val selectedCount = adapter.getSelectedCount()
        binding.selectionPanel.isVisible = adapter.selectionMode
        binding.selectionCountText.text = "Zaznaczono: $selectedCount"
        
        if (selectedCount == 0 && adapter.selectionMode) {
            adapter.clearSelection()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = adapter.getSelectedCount()
        
        if (selectedCount == 0) return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDeleteConfirmBinding.inflate(layoutInflater)
        
        sheetBinding.productNameText.text = "$selectedCount ${pluralForm(selectedCount, "pracownik", "pracowników", "pracowników")}"
        
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        sheetBinding.deleteButton.text = "Usuń pracowników"
        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteSelectedEmployees()
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }
    
    private fun pluralForm(count: Int, singular: String, few: String, many: String): String {
        return when {
            count == 1 -> singular
            count % 10 in 2..4 && count % 100 !in 12..14 -> few
            else -> many
        }
    }

    private fun deleteSelectedEmployees() {
        lifecycleScope.launch {
            try {
                val selectedIds = adapter.getSelectedIds()
                selectedIds.forEach { employeeId ->
                    val assignedProducts = productRepository.getProductsAssignedToEmployee(employeeId).first()
                    assignedProducts.forEach { product ->
                        productRepository.unassignFromEmployee(product.id)
                    }
                }
                employeeRepository.deleteEmployees(selectedIds)
                adapter.clearSelection()
                Toast.makeText(requireContext(), "Usunięto pracowników", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeEmployees() {
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                searchQueryFlow.debounce(300),
                departmentFilterFlow
            ) { search, department ->
                Pair(search, department)
            }.flatMapLatest { (search, department) ->
                employeeRepository.searchEmployees(
                    if (search.isBlank()) null else search,
                    department
                )
            }.collect { employees ->
                // Get assigned counts for each employee
                val employeesWithStats = employees.map { employee ->
                    val count = productRepository.getAssignedProductsCount(employee.id)
                    EmployeeWithStats(employee, count)
                }
                
                adapter.submitList(employeesWithStats)
                binding.emptyState.isVisible = employeesWithStats.isEmpty()
                binding.employeesRecyclerView.isVisible = employeesWithStats.isNotEmpty()
                binding.employeesCountBadge.text = "${employeesWithStats.size} osób"
                
                // Update selection panel if in selection mode
                if (adapter.selectionMode) {
                    updateSelectionPanel()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
