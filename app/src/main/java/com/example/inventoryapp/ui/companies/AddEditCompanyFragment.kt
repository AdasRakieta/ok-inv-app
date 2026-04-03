package com.example.inventoryapp.ui.companies

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.databinding.FragmentAddEditCompanyBinding
import kotlinx.coroutines.launch

class AddEditCompanyFragment : Fragment() {

    private var _binding: FragmentAddEditCompanyBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditCompanyFragmentArgs by navArgs()

    private val companyRepository by lazy {
        (requireActivity().application as InventoryApplication).companyRepository
    }

    private val departmentRepository by lazy {
        (requireActivity().application as InventoryApplication).departmentRepository
    }

    private var existingCompany: CompanyEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCompanyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { saveCompany() }
        binding.manageDepartmentsButton.setOnClickListener { openDepartmentsManager() }
        binding.usesDepartmentsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.manageDepartmentsButton.isVisible = isChecked
        }
        binding.manageDepartmentsButton.isVisible = false

        loadCompanyIfEditing()
    }

    private fun openDepartmentsManager() {
        if (!binding.usesDepartmentsCheckbox.isChecked) {
            Toast.makeText(requireContext(), getString(R.string.company_enable_departments_first), Toast.LENGTH_SHORT).show()
            return
        }

        val company = existingCompany
        if (company == null || company.id <= 0L) {
            Toast.makeText(requireContext(), getString(R.string.company_save_first_for_departments), Toast.LENGTH_SHORT).show()
            return
        }

        val sheet = com.example.inventoryapp.ui.employees.DepartmentsBottomSheetFragment
            .newInstance(company.id, company.name)
        sheet.show(parentFragmentManager, "company_departments_sheet")
    }

    private fun loadCompanyIfEditing() {
        if (args.companyId <= 0L) {
            binding.headerText.text = getString(R.string.company_add_title)
            binding.usesDepartmentsCheckbox.isChecked = false
            binding.manageDepartmentsButton.isVisible = false
            return
        }

        binding.headerText.text = getString(R.string.company_edit_title)
        binding.saveButton.text = getString(R.string.company_save_changes)

        lifecycleScope.launch {
            val company = companyRepository.getCompanyById(args.companyId)
            if (company == null) {
                Toast.makeText(requireContext(), getString(R.string.company_not_found), Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return@launch
            }

            existingCompany = company
            binding.nameInput.setText(company.name)
            binding.taxIdInput.setText(company.taxId ?: "")
            binding.usesDepartmentsCheckbox.isChecked = company.usesDepartments
            binding.manageDepartmentsButton.isVisible = company.usesDepartments
            binding.addressInput.setText(company.address ?: "")
            binding.cityInput.setText(company.city ?: "")
            binding.postalCodeInput.setText(company.postalCode ?: "")
            binding.countryInput.setText(company.country ?: "")
            binding.contactPersonInput.setText(company.contactPerson ?: "")
            binding.emailInput.setText(company.email ?: "")
            binding.phoneInput.setText(company.phone ?: "")
            binding.notesInput.setText(company.notes ?: "")
        }
    }

    private fun saveCompany() {
        clearErrors()

        val name = binding.nameInput.text.toString().trim()
        val taxIdRaw = binding.taxIdInput.text.toString().trim()
        val taxId = taxIdRaw.filter { it.isDigit() }.ifBlank { null }
        val usesDepartments = binding.usesDepartmentsCheckbox.isChecked
        val address = binding.addressInput.text.toString().trim().ifBlank { null }
        val city = binding.cityInput.text.toString().trim().ifBlank { null }
        val postalCode = binding.postalCodeInput.text.toString().trim().ifBlank { null }
        val country = binding.countryInput.text.toString().trim().ifBlank { null }
        val contactPerson = binding.contactPersonInput.text.toString().trim().ifBlank { null }
        val email = binding.emailInput.text.toString().trim().ifBlank { null }
        val phone = binding.phoneInput.text.toString().trim().ifBlank { null }
        val notes = binding.notesInput.text.toString().trim().ifBlank { null }

        var hasError = false

        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_field_required)
            hasError = true
        }

        if (taxId != null && taxId.length != 10) {
            binding.taxIdLayout.error = getString(R.string.error_invalid_tax_id)
            hasError = true
        }

        if (email != null && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = getString(R.string.error_invalid_email)
            hasError = true
        }

        if (hasError) return

        val now = System.currentTimeMillis()
        val current = existingCompany
        val company = if (current == null) {
            CompanyEntity(
                name = name,
                taxId = taxId,
                usesDepartments = usesDepartments,
                address = address,
                city = city,
                postalCode = postalCode,
                country = country,
                contactPerson = contactPerson,
                email = email,
                phone = phone,
                notes = notes,
                createdAt = now,
                updatedAt = now
            )
        } else {
            current.copy(
                name = name,
                taxId = taxId,
                usesDepartments = usesDepartments,
                address = address,
                city = city,
                postalCode = postalCode,
                country = country,
                contactPerson = contactPerson,
                email = email,
                phone = phone,
                notes = notes,
                updatedAt = now
            )
        }

        lifecycleScope.launch {
            try {
                if (current == null) {
                    companyRepository.insertCompany(company)
                    Toast.makeText(requireContext(), getString(R.string.company_created), Toast.LENGTH_SHORT).show()
                } else {
                    companyRepository.updateCompany(company)
                    if (!usesDepartments) {
                        departmentRepository.deleteByCompany(current.id)
                    }
                    Toast.makeText(requireContext(), getString(R.string.company_updated), Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            } catch (e: SQLiteConstraintException) {
                if (
                    e.message?.contains("taxId", ignoreCase = true) == true ||
                    e.message?.contains("index_companies_taxId", ignoreCase = true) == true
                ) {
                    binding.taxIdLayout.error = getString(R.string.error_tax_id_exists)
                } else {
                    Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearErrors() {
        binding.nameLayout.error = null
        binding.taxIdLayout.error = null
        binding.emailLayout.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

