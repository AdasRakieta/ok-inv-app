package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.EmployeeEntity
import com.example.inventoryapp.databinding.FragmentAddEmployeeBinding
import kotlinx.coroutines.launch

class AddEmployeeFragment : Fragment() {

    private var _binding: FragmentAddEmployeeBinding? = null
    private val binding get() = _binding!!

    private lateinit var employeeRepository: com.example.inventoryapp.data.repository.EmployeeRepository
    private val args: AddEmployeeFragmentArgs by navArgs()
    private var existingEmployee: EmployeeEntity? = null

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

        setupDepartmentDropdown()
        loadEmployeeIfEditing()
        setupButtons()
    }

    private fun setupDepartmentDropdown() {
        lifecycleScope.launch {
            val departments = employeeRepository.getAllDepartments().ifEmpty {
                listOf("IT / Helpdesk", "Marketing", "Sprzedaż", "HR", "Zarząd")
            }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                departments
            )
            (binding.departmentInput as? AutoCompleteTextView)?.setAdapter(adapter)
        }
    }

    private fun loadEmployeeIfEditing() {
        if (args.employeeId > 0) {
            binding.headerText.text = "Edytuj pracownika"
            
            lifecycleScope.launch {
                existingEmployee = employeeRepository.getEmployeeById(args.employeeId)
                existingEmployee?.let { employee ->
                    binding.apply {
                        firstNameInput.setText(employee.firstName)
                        lastNameInput.setText(employee.lastName)
                        val department = employee.department ?: ""
                        (departmentInput as? AutoCompleteTextView)?.setText(department, false)
                            ?: departmentInput.setText(department)
                        positionInput.setText(employee.position ?: "")
                        emailInput.setText(employee.email ?: "")
                        phoneInput.setText(employee.phone ?: "")
                        notesInput.setText(employee.notes ?: "")
                    }
                }
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
            val department = departmentInput.text.toString().trim().ifBlank { null }
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
