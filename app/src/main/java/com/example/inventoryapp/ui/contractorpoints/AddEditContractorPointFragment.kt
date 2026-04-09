package com.example.inventoryapp.ui.contractorpoints

import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.util.Patterns
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import com.example.inventoryapp.data.repository.CompanyRepository
import com.example.inventoryapp.data.repository.ContractorPointRepository
import com.example.inventoryapp.databinding.FragmentAddEditContractorPointBinding
import kotlinx.coroutines.launch

class AddEditContractorPointFragment : Fragment() {

    companion object {
        const val RESULT_KEY = "contractor_point_form_result"
        const val RESULT_ID = "contractorPointId"
        private val POSTAL_CODE_REGEX = Regex("^\\d{2}-\\d{3}$")
    }

    private var _binding: FragmentAddEditContractorPointBinding? = null
    private val binding get() = _binding!!

    private val args: AddEditContractorPointFragmentArgs by navArgs()

    private lateinit var contractorPointRepository: ContractorPointRepository
    private lateinit var companyRepository: CompanyRepository

    private var existingContractorPoint: ContractorPointEntity? = null
    private var companyOptions: List<CompanyEntity> = emptyList()
    private var selectedCompanyId: Long? = null
    private var selectedPointType: PointType? = null

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentAddEditContractorPointBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        contractorPointRepository = app.contractorPointRepository
        companyRepository = app.companyRepository

        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { saveContractorPoint() }

        setupPointTypeDropdown()
        setupCompanyDropdownAndLoad()
    }

    private fun setupPointTypeDropdown() {
        val pointTypes = PointType.values().map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, pointTypes)
        val input = binding.pointTypeInput as AutoCompleteTextView
        input.setAdapter(adapter)
        input.setOnItemClickListener { _, _, position, _ ->
            selectedPointType = PointType.valueOf(pointTypes[position])
            binding.marketNumberLayout.isVisible = (selectedPointType == PointType.CP)
        }
    }

    private fun setupCompanyDropdownAndLoad() {
        lifecycleScope.launch {
            companyOptions = companyRepository.getAllCompanies()
            val companyNames = companyOptions.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, companyNames)
            val input = binding.companyInput as AutoCompleteTextView
            input.setAdapter(adapter)
            input.setOnItemClickListener { _, _, position, _ ->
                selectedCompanyId = companyOptions.getOrNull(position)?.id
                binding.companyLayout.error = null
            }

            if (args.contractorPointId > 0L) {
                loadContractorPointForEdit()
            } else {
                binding.headerText.text = getString(R.string.contractor_point_add_title)
            }
        }
    }

    private suspend fun loadContractorPointForEdit() {
        binding.headerText.text = getString(R.string.contractor_point_edit_title)
        binding.saveButton.text = getString(R.string.contractor_point_save_changes)

        val contractorPoint = contractorPointRepository.getContractorPointById(args.contractorPointId)
        if (contractorPoint == null) {
            Toast.makeText(requireContext(), getString(R.string.contractor_point_not_found), Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        existingContractorPoint = contractorPoint
        selectedCompanyId = contractorPoint.companyId
        selectedPointType = contractorPoint.pointType

        binding.nameInput.setText(contractorPoint.name)
        binding.marketNumberInput.setText(contractorPoint.marketNumber ?: "")
        binding.addressInput.setText(contractorPoint.address ?: "")
        binding.cityInput.setText(contractorPoint.city ?: "")
        binding.postalCodeInput.setText(contractorPoint.postalCode ?: "")
        binding.phoneInput.setText(contractorPoint.phone ?: "")

        (binding.pointTypeInput as AutoCompleteTextView).setText(contractorPoint.pointType.name, false)
        binding.marketNumberLayout.isVisible = (contractorPoint.pointType == PointType.CP)
        val companyName = companyOptions.firstOrNull { it.id == contractorPoint.companyId }?.name.orEmpty()
        (binding.companyInput as AutoCompleteTextView).setText(companyName, false)
    }

    private fun saveContractorPoint() {
        clearErrors()

        val name = binding.nameInput.text.toString().trim()
        val marketNumber = binding.marketNumberInput.text.toString().trim().ifBlank { null }
        val address = binding.addressInput.text.toString().trim().ifBlank { null }
        val city = binding.cityInput.text.toString().trim().ifBlank { null }
        val postalCode = binding.postalCodeInput.text.toString().trim().ifBlank { null }
        val phone = binding.phoneInput.text.toString().trim().ifBlank { null }

        var hasError = false

        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_field_required)
            hasError = true
        }

        if (selectedPointType == null) {
            binding.pointTypeLayout.error = getString(R.string.error_field_required)
            hasError = true
        }

        if (selectedCompanyId == null) {
            binding.companyLayout.error = getString(R.string.error_field_required)
            hasError = true
        }

        if (postalCode != null && !POSTAL_CODE_REGEX.matches(postalCode)) {
            binding.postalCodeLayout.error = getString(R.string.error_invalid_postal_code)
            hasError = true
        }

        if (hasError) return

        // Capture non-null values to avoid repeated '!!' assertions
        val pointTypeNonNull = selectedPointType!!
        val companyIdNonNull = selectedCompanyId!!

        val now = System.currentTimeMillis()
        val current = existingContractorPoint
        val contractorPoint = if (current == null) {
            ContractorPointEntity(
                name = name,
                pointType = pointTypeNonNull,
                companyId = companyIdNonNull,
                marketNumber = marketNumber,
                address = address,
                city = city,
                postalCode = postalCode,
                phone = phone,
                createdAt = now,
                updatedAt = now
            )
        } else {
            current.copy(
                name = name,
                pointType = pointTypeNonNull,
                companyId = companyIdNonNull,
                marketNumber = marketNumber,
                address = address,
                city = city,
                postalCode = postalCode,
                phone = phone,
                updatedAt = now
            )
        }

        lifecycleScope.launch {
                try {
                val savedId = if (current == null) {
                    val id = contractorPointRepository.insertContractorPoint(contractorPoint)
                    Toast.makeText(requireContext(), getString(R.string.contractor_point_created), Toast.LENGTH_SHORT).show()
                    id
                } else {
                    contractorPointRepository.updateContractorPoint(contractorPoint)
                    Toast.makeText(requireContext(), getString(R.string.contractor_point_updated), Toast.LENGTH_SHORT).show()
                    contractorPoint.id
                }

                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    bundleOf(RESULT_ID to savedId)
                )
                findNavController().navigateUp()
            } catch (e: SQLiteConstraintException) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearErrors() {
        binding.nameLayout.error = null
        binding.pointTypeLayout.error = null
        binding.companyLayout.error = null
        binding.postalCodeLayout.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

