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
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.example.inventoryapp.ui.components.FilterOptionsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.PopupMenu
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmployeesListFragment : Fragment() {

    private var _binding: FragmentEmployeesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private lateinit var productRepository: com.example.inventoryapp.data.repository.ProductRepository
    private lateinit var departmentRepository: com.example.inventoryapp.data.repository.DepartmentRepository
    private lateinit var companyRepository: com.example.inventoryapp.data.repository.CompanyRepository
    private lateinit var adapter: EmployeesAdapter

    private val searchQueryFlow = MutableStateFlow("")
    private val departmentFilterFlow = MutableStateFlow<String?>(null)
    private val companyFilterFlow = MutableStateFlow<Long?>(null)

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
        companyRepository = app.companyRepository
        productRepository = app.productRepository

        setupRecyclerView()
        setupSearchBar()
        setupFilterButtons()
        setupSortButton()
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
            },
            onOptionsClick = { anchor, employee ->
                showEmployeeOptions(anchor, employee)
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

    private fun setupFilterButtons() {
        binding.departmentFilterButton.setOnClickListener {
            showDepartmentFilterDialog()
        }

        binding.companyFilterButton.setOnClickListener {
            showCompanyFilterDialog()
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showFilterDialog() {
        val options = listOf(
            FilterOption("department", "Dział", "🏢", false),
            FilterOption("company", "Firma", "🏬", false)
        )

        FilterBottomSheet.show(this, "Filtruj po", options) { option ->
            when (option.id) {
                "department" -> showDepartmentFilterDialog()
                "company" -> showCompanyFilterDialog()
            }
        }
    }

    private fun showDepartmentFilterDialog() {
        lifecycleScope.launch {
            val departments = employeeRepository.getAllDepartments()
            val options = mutableListOf<FilterOption>()
            options.add(FilterOption("all", "Wszystkie", "", departmentFilterFlow.value == null))
            departments.forEach { dep ->
                options.add(FilterOption(dep, dep, "", departmentFilterFlow.value == dep))
            }

            FilterBottomSheet.show(this@EmployeesListFragment, "Filtruj po dziale", options) { option ->
                if (option.id == "all") {
                    departmentFilterFlow.value = null
                    binding.departmentFilterButton.text = "Dział"
                } else {
                    departmentFilterFlow.value = option.id
                    binding.departmentFilterButton.text = option.label
                }
            }
        }
    }

    private fun showCompanyFilterDialog() {
        lifecycleScope.launch {
            val companies = companyRepository.getAllCompanies()
            val options = mutableListOf<FilterOption>()
            options.add(FilterOption("all", "Wszystkie", "", companyFilterFlow.value == null))
            options.add(FilterOption("no_company", "Bez firmy", "", companyFilterFlow.value == -1L))
            companies.forEach { c ->
                options.add(FilterOption(c.id.toString(), c.name, "", companyFilterFlow.value == c.id))
            }

            FilterBottomSheet.show(this@EmployeesListFragment, "Filtruj po firmie", options) { option ->
                companyFilterFlow.value = when (option.id) {
                    "all" -> null
                    "no_company" -> -1L
                    else -> option.id.toLong()
                }
                if (option.id == "all") {
                    binding.companyFilterButton.text = "Firma"
                } else {
                    binding.companyFilterButton.text = option.label
                }
            }
        }
    }

    private fun showSortDialog() {
        val options = listOf(
            FilterOption("az", "Alfabetycznie (A-Z)", "", false),
            FilterOption("za", "Alfabetycznie (Z-A)", "", false),
            FilterOption("newest", "Najnowsi", "", false),
            FilterOption("oldest", "Najstarsi", "", false)
        )

        FilterBottomSheet.show(this, "Sortuj", options) { option ->
            // TODO: Implement sorting behavior
            Toast.makeText(requireContext(), "Sortowanie: ${option.label}", Toast.LENGTH_SHORT).show()
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

    private fun showEmployeeOptions(anchorView: View, employee: com.example.inventoryapp.data.local.entities.EmployeeEntity) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add("Edytuj")
        popup.menu.add("Usuń")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Edytuj" -> {
                    val action = EmployeesListFragmentDirections.actionEmployeesListToAddEmployee(employee.id)
                    findNavController().navigate(action)
                    true
                }
                "Usuń" -> {
                    showDeleteEmployeeConfirmation(employee)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteEmployeeConfirmation(employee: com.example.inventoryapp.data.local.entities.EmployeeEntity) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDeleteConfirmBinding.inflate(layoutInflater)

        val name = "${employee.firstName} ${employee.lastName}"
        sheetBinding.productNameText.text = name
        sheetBinding.cancelButton.setOnClickListener { bottomSheet.dismiss() }
        sheetBinding.deleteButton.text = "Usuń pracownika"
        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteSingleEmployee(employee.id)
        }

        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun deleteSingleEmployee(employeeId: Long) {
        lifecycleScope.launch {
            try {
                val assignedProducts = productRepository.getProductsAssignedToEmployee(employeeId).first()
                assignedProducts.forEach { product ->
                    productRepository.unassignFromEmployee(product.id)
                }
                employeeRepository.deleteEmployees(listOf(employeeId))
                Toast.makeText(requireContext(), "Usunięto pracownika", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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
                departmentFilterFlow,
                companyFilterFlow
            ) { search, department, companyFilter ->
                Triple(search, department, companyFilter)
            }.flatMapLatest { (search, department, companyFilter) ->
                when {
                    companyFilter == null || companyFilter == -1L -> {
                        employeeRepository.searchEmployees(
                            if (search.isBlank()) null else search,
                            department
                        ).map { employees ->
                            if (companyFilter == -1L) {
                                employees.filter { it.companyId == null }
                            } else {
                                employees
                            }
                        }
                    }
                    else -> {
                        employeeRepository.searchEmployeesByCompany(
                            companyFilter,
                            if (search.isBlank()) null else search,
                            department
                        )
                    }
                }
            }.collect { employees ->
                val companiesById = companyRepository.getAllCompanies().associateBy { it.id }
                // Get assigned counts for each employee
                val employeesWithStats = employees.map { employee ->
                    val count = productRepository.getAssignedProductsCount(employee.id)
                    val companyName = employee.companyId?.let { companiesById[it]?.name }
                    EmployeeWithStats(employee, count, companyName)
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
