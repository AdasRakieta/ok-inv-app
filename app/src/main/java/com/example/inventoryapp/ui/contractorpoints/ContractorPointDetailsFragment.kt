package com.example.inventoryapp.ui.contractorpoints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentContractorDetailsBinding
import com.example.inventoryapp.domain.validators.AssignmentValidator
import com.example.inventoryapp.ui.employees.AssignEquipmentDialogFragment
import com.example.inventoryapp.ui.employees.AssignedProductsAdapter
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContractorPointDetailsFragment : Fragment() {

    private var _binding: FragmentContractorDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: ContractorPointDetailsFragmentArgs by navArgs()

    private val contractorPointRepository by lazy {
        (requireActivity().application as InventoryApplication).contractorPointRepository
    }
    private val companyRepository by lazy {
        (requireActivity().application as InventoryApplication).companyRepository
    }
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val assignmentValidator = AssignmentValidator()

    private lateinit var assignedProductsAdapter: AssignedProductsAdapter
    private var currentContractorPoint: ContractorPointEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractorDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActions()
        loadContractorPointDetails()
    }

    private fun setupRecyclerView() {
        assignedProductsAdapter = AssignedProductsAdapter(
            onProductClick = { product ->
                val action = ContractorPointDetailsFragmentDirections
                    .actionContractorPointDetailsToProductDetails(product.id)
                findNavController().navigate(action)
            },
            onUnassignClick = { product ->
                showUnassignConfirmation(product)
            }
        )

        binding.assignedEquipmentRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedProductsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.retryButton.setOnClickListener { loadContractorPointDetails() }

        binding.editButton.setOnClickListener {
            val contractorPoint = currentContractorPoint ?: return@setOnClickListener
            val action = ContractorPointDetailsFragmentDirections
                .actionContractorPointDetailsToAddEditContractorPoint(contractorPoint.id)
            findNavController().navigate(action)
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.assignEquipmentButton.setOnClickListener {
            showAssignEquipmentDialog()
        }

        binding.assignEquipmentScanButton.setOnClickListener {
            val contractorPoint = currentContractorPoint ?: return@setOnClickListener
            val action = ContractorPointDetailsFragmentDirections
                .actionContractorPointDetailsToAssignByScan(
                    employeeId = -1L,
                    locationName = null,
                    contractorPointId = contractorPoint.id
                )
            findNavController().navigate(action)
        }
    }

    private fun loadContractorPointDetails() {
        showLoadingState()
        lifecycleScope.launch {
            runCatching { contractorPointRepository.getContractorPointById(args.contractorPointId) }
                .onSuccess { contractorPoint ->
                    if (contractorPoint == null) {
                        showErrorState(getString(R.string.contractor_point_not_found))
                        return@onSuccess
                    }

                    currentContractorPoint = contractorPoint
                    displayContractorPoint(contractorPoint)
                    showContentState()
                    observeAssignedProducts(contractorPoint.id)
                }
                .onFailure {
                    showErrorState(getString(R.string.error_prefix, it.message ?: ""))
                }
        }
    }

    private fun displayContractorPoint(contractorPoint: ContractorPointEntity) {
        binding.nameText.text = contractorPoint.name
        binding.codeTypeText.text = "${contractorPoint.code} • ${contractorPoint.pointType.name}"

        lifecycleScope.launch {
            val companyName = companyRepository.getCompanyById(contractorPoint.companyId)?.name ?: "Nieznana firma"
            binding.companyText.text = "🏢 Firma: $companyName"
        }

        val addressParts = listOfNotNull(
            contractorPoint.address?.takeIf { it.isNotBlank() },
            listOfNotNull(
                contractorPoint.postalCode?.takeIf { it.isNotBlank() },
                contractorPoint.city?.takeIf { it.isNotBlank() }
            ).takeIf { it.isNotEmpty() }?.joinToString(" ")
        )
        binding.addressText.text = if (addressParts.isEmpty()) {
            "📍 Brak adresu"
        } else {
            "📍 ${addressParts.joinToString(", ")}"
        }

        val contactParts = listOfNotNull(
            contractorPoint.contactPerson?.takeIf { it.isNotBlank() },
            contractorPoint.phone?.takeIf { it.isNotBlank() },
            contractorPoint.email?.takeIf { it.isNotBlank() }
        )
        binding.contactText.text = if (contactParts.isEmpty()) {
            "📞 Brak danych kontaktowych"
        } else {
            "📞 ${contactParts.joinToString(" • ")}"
        }

        binding.notesCard.isVisible = !contractorPoint.notes.isNullOrBlank()
        binding.notesText.text = contractorPoint.notes.orEmpty()

        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        binding.updatedAtText.text = "Zaktualizowano: ${formatter.format(Date(contractorPoint.updatedAt))}"
    }

    private fun observeAssignedProducts(contractorPointId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productRepository.getProductsAssignedToContractorPoint(contractorPointId).collect { products ->
                    val safeBinding = _binding ?: return@collect
                    // Provide category labels to adapter
                    val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val categoryMapForAdapter = categories.associate { it.id to (it.icon ?: it.name) }
                    assignedProductsAdapter.setCategoriesMap(categoryMapForAdapter)
                    assignedProductsAdapter.setFullList(products)

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
        val contractorPoint = currentContractorPoint ?: return
        val dialog = AssignEquipmentDialogFragment(contractorPoint.id) { selectedProducts ->
            assignProductsToContractorPoint(selectedProducts, contractorPoint)
        }
        dialog.show(childFragmentManager, "AssignEquipmentDialog")
    }

    private fun assignProductsToContractorPoint(
        products: List<ProductEntity>,
        contractorPoint: ContractorPointEntity
    ) {
        lifecycleScope.launch {
            try {
                val categoriesById = categoryRepository.getAllCategories().first().associateBy { it.id }
                for (product in products) {
                    val category = categoriesById[product.categoryId]
                    when (val result = assignmentValidator.canAssignToContractorPoint(product, category)) {
                        is AssignmentValidator.ValidationResult.Success -> Unit
                        is AssignmentValidator.ValidationResult.Error -> {
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                    }
                }

                products.forEach { product ->
                    productRepository.assignToContractorPoint(
                        productId = product.id,
                        contractorPointId = contractorPoint.id,
                        contractorPointName = contractorPoint.name
                    )
                }
                Toast.makeText(requireContext(), "Przypisano ${products.size} urządzeń", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUnassignConfirmation(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń przypisanie sprzętu")
            .setMessage("Czy na pewno chcesz usunąć przypisanie?\n\n${product.name}\nS/N: ${product.serialNumber}")
            .setPositiveButton("Usuń przypisanie") { _, _ -> unassignProduct(product) }
            .setNegativeButton("Anuluj", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun unassignProduct(product: ProductEntity) {
        lifecycleScope.launch {
            try {
                productRepository.unassignFromContractorPoint(product.id)
                Toast.makeText(requireContext(), "Usunięto przypisanie", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val contractorPoint = currentContractorPoint ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń punkt kontrahenta")
            .setMessage("Czy na pewno chcesz usunąć punkt:\n\n${contractorPoint.name}\n(${contractorPoint.code})")
            .setPositiveButton("Usuń") { _, _ -> deleteContractorPoint(contractorPoint) }
            .setNegativeButton("Anuluj", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteContractorPoint(contractorPoint: ContractorPointEntity) {
        lifecycleScope.launch {
            try {
                val assignedProducts = productRepository
                    .getProductsAssignedToContractorPoint(contractorPoint.id)
                    .first()

                assignedProducts.forEach { product ->
                    val updated = product.copy(
                        assignedToContractorPointId = null,
                        assignedToEmployeeId = null,
                        assignmentDate = null,
                        status = ProductStatus.UNASSIGNED,
                        updatedAt = System.currentTimeMillis()
                    )
                    productRepository.updateWithHistory(
                        updated,
                        MovementHistoryUtils.entryUnassigned()
                    )
                }

                contractorPointRepository.deleteContractorPoint(contractorPoint)
                Toast.makeText(requireContext(), "Usunięto punkt kontrahenta", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoadingState() {
        binding.loadingState.isVisible = true
        binding.errorState.isVisible = false
        binding.contentScroll.isVisible = false
    }

    private fun showErrorState(message: String) {
        binding.loadingState.isVisible = false
        binding.errorState.isVisible = true
        binding.contentScroll.isVisible = false
        binding.errorMessageText.text = message
    }

    private fun showContentState() {
        binding.loadingState.isVisible = false
        binding.errorState.isVisible = false
        binding.contentScroll.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

