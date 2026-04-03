package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.databinding.FragmentAddEmployeeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AddEmployeeFragment : Fragment() {

    private var _binding: FragmentAddEmployeeBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private lateinit var departmentRepository: com.example.inventoryapp.data.repository.DepartmentRepository
    private lateinit var companyRepository: com.example.inventoryapp.data.repository.CompanyRepository
    private val args: AddEmployeeFragmentArgs by navArgs()
    private var existingEmployee: EmployeeEntity? = null
    private var companyOptions: List<CompanyEntity> = emptyList()
    private var selectedCompanyId: Long? = null
    private var initialDepartmentValue: String? = null
    private var loadDepartmentsJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEmployeeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        employeeRepository = app.employeeRepository
        departmentRepository = app.departmentRepository
        companyRepository = app.companyRepository

        setupButtons()
        if (args.employeeId > 0) {
            loadEmployeeIfEditing()
        } else {
            binding.headerText.text = "Dodaj pracownika"
            setupCompanyDropdown()
        }
    }

    private fun setupCompanyDropdown() {
        lifecycleScope.launch {
            companyOptions = companyRepository.getAllCompanies()
            val labels = mutableListOf("Brak firmy").apply { addAll(companyOptions.map { it.name }) }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                labels
            )

            val companyInput = binding.companyInput as? AutoCompleteTextView ?: return@launch
            companyInput.setAdapter(adapter)
            companyInput.setOnItemClickListener { _, _, position, _ ->
                selectedCompanyId = if (position == 0) null else companyOptions[position - 1].id
                updateDepartmentVisibilityAndOptions(restoreInitial = false)
            }
            companyInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    val selectedName = companyOptions.firstOrNull { it.id == selectedCompanyId }?.name ?: "Brak firmy"
                    companyInput.setText(selectedName, false)
                }
            }
            companyInput.doAfterTextChanged { text ->
                if (text.isNullOrBlank()) {
                    selectedCompanyId = null
                    updateDepartmentVisibilityAndOptions(restoreInitial = false)
                } else {
                    val typedName = text.toString().trim()
                    val matchedCompany = companyOptions.firstOrNull { it.name == typedName }
                    val newSelectedId = matchedCompany?.id
                    if (newSelectedId != selectedCompanyId) {
                        selectedCompanyId = newSelectedId
                        updateDepartmentVisibilityAndOptions(restoreInitial = false)
                    }
                }
            }

            val selectedCompanyName = companyOptions.firstOrNull { it.id == selectedCompanyId }?.name
                ?: "Brak firmy"
            companyInput.setText(selectedCompanyName, false)
            updateDepartmentVisibilityAndOptions(restoreInitial = true)
        }
    }

    private fun updateDepartmentVisibilityAndOptions(restoreInitial: Boolean) {
        val companyId = selectedCompanyId
        val selectedCompany = companyOptions.firstOrNull { it.id == companyId }
        if (companyId == null || selectedCompany?.usesDepartments != true) {
            binding.departmentLabel.isVisible = false
            binding.departmentLayout.isVisible = false
            binding.departmentLayout.error = null
            (binding.departmentInput as? AutoCompleteTextView)?.setText("", false)
            return
        }

        loadDepartmentsJob?.cancel()
        loadDepartmentsJob = viewLifecycleOwner.lifecycleScope.launch {
            val departments = departmentRepository.getNamesByCompany(companyId)
            val hasDepartments = departments.isNotEmpty()

            binding.departmentLabel.isVisible = hasDepartments
            binding.departmentLayout.isVisible = hasDepartments
            binding.departmentLayout.error = null

            val departmentInput = binding.departmentInput as? AutoCompleteTextView ?: return@launch
            if (!hasDepartments) {
                departmentInput.setText("", false)
                return@launch
            }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                departments
            )
            departmentInput.setAdapter(adapter)

            val current = departmentInput.text?.toString()?.trim().orEmpty()
            val target = when {
                restoreInitial && !initialDepartmentValue.isNullOrBlank() && departments.contains(initialDepartmentValue) ->
                    initialDepartmentValue.orEmpty()
                current.isNotBlank() && departments.contains(current) -> current
                else -> ""
            }
            departmentInput.setText(target, false)
            if (restoreInitial) {
                initialDepartmentValue = null
            }
        }
    }

    private fun loadEmployeeIfEditing() {
        if (args.employeeId > 0) {
            binding.headerText.text = "Edytuj pracownika"

            lifecycleScope.launch {
                existingEmployee = employeeRepository.getEmployeeById(args.employeeId)
                existingEmployee?.let { employee ->
                    selectedCompanyId = employee.companyId
                    initialDepartmentValue = employee.department
                    binding.apply {
                        firstNameInput.setText(employee.firstName)
                        lastNameInput.setText(employee.lastName)
                        positionInput.setText(employee.position ?: "")
                        emailInput.setText(employee.email ?: "")
                        phoneInput.setText(employee.phone ?: "")
                        notesInput.setText(employee.notes ?: "")
                    }
                }
                setupCompanyDropdown()
            }
        }
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveEmployee()
        }
    }

    private fun saveEmployee() {
        binding.apply {
            // Clear previous errors
            firstNameLayout.error = null
            lastNameLayout.error = null
            emailLayout.error = null

            // Get values
            val firstName = firstNameInput.text.toString().trim()
            val lastName = lastNameInput.text.toString().trim()
            val department = if (departmentLayout.isVisible) {
                departmentInput.text.toString().trim().ifBlank { null }
            } else {
                null
            }
            val position = positionInput.text.toString().trim().ifBlank { null }
            val email = emailInput.text.toString().trim().ifBlank { null }
            val phone = phoneInput.text.toString().trim().ifBlank { null }
            val notes = notesInput.text.toString().trim().ifBlank { null }

            // Validate
            var hasError = false

            if (firstName.isBlank()) {
                firstNameLayout.error = "Pole wymagane"
                hasError = true
            }

            if (lastName.isBlank()) {
                lastNameLayout.error = "Pole wymagane"
                hasError = true
            }

            if (email != null && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Nieprawidłowy email"
                hasError = true
            }

            if (hasError) return

            // Create or update employee
            val employee = if (existingEmployee == null) {
                EmployeeEntity(
                    firstName = firstName,
                    lastName = lastName,
                    companyId = selectedCompanyId,
                    email = email,
                    phone = phone,
                    department = department,
                    position = position,
                    notes = notes
                )
            } else {
                existingEmployee!!.copy(
                    firstName = firstName,
                    lastName = lastName,
                    companyId = selectedCompanyId,
                    email = email,
                    phone = phone,
                    department = department,
                    position = position,
                    notes = notes
                )
            }

            lifecycleScope.launch {
                try {
                    if (existingEmployee == null) {
                        employeeRepository.insertEmployee(employee)
                        Toast.makeText(requireContext(), "Dodano pracownika", Toast.LENGTH_SHORT).show()
                    } else {
                        employeeRepository.updateEmployee(employee)
                        Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                    }
                    findNavController().navigateUp()
                } catch (e: android.database.sqlite.SQLiteConstraintException) {
                    if (e.message?.contains("email") == true) {
                        emailLayout.error = "Ten email jest już w użyciu"
                    } else {
                        Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
