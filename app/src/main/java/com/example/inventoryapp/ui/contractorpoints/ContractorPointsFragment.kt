package com.example.inventoryapp.ui.contractorpoints

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CompanyEntity
import com.example.inventoryapp.data.local.entities.ContractorPointEntity
import com.example.inventoryapp.data.local.entities.PointType
import com.example.inventoryapp.data.repository.CompanyRepository
import com.example.inventoryapp.data.repository.ContractorPointRepository
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.example.inventoryapp.databinding.FragmentContractorPointsBinding
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ContractorPointsFragment : Fragment() {

    private var _binding: FragmentContractorPointsBinding? = null
    private val binding get() = _binding!!

    private val args: ContractorPointsFragmentArgs by navArgs()

    private lateinit var contractorPointRepository: ContractorPointRepository
    private lateinit var companyRepository: CompanyRepository

    private val productRepository by lazy { (requireActivity().application as InventoryApplication).productRepository }

    private lateinit var adapter: ContractorPointsAdapter
    private var searchJob: Job? = null

    private var selectedPointType: PointType? = null
    private var selectedCompanyId: Long? = null
    private var companies: List<CompanyEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractorPointsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        contractorPointRepository = app.contractorPointRepository
        companyRepository = app.companyRepository
        selectedCompanyId = args.companyId.takeIf { it > 0L }

        setupRecyclerView()
        setupActions()
        setupSearch()
        setupSelectionActions()
        updateFilterLabels()
        loadCompaniesAndPoints()
    }

    private fun setupRecyclerView() {
        adapter = ContractorPointsAdapter(
            onPointClick = { point ->
                if (adapter.selectionMode) {
                    adapter.toggleSelection(point.id)
                    updateSelectionPanel()
                } else {
                    val action = ContractorPointsFragmentDirections.actionContractorPointsToDetails(point.id)
                    findNavController().navigate(action)
                }
            },
            onPointLongClick = { point ->
                if (!adapter.selectionMode) {
                    adapter.selectionMode = true
                    adapter.toggleSelection(point.id)
                    showSelectionPanel()
                }
            },
            onOptionsClick = { point, anchor ->
                val popup = android.widget.PopupMenu(requireContext(), anchor)
                popup.menu.add("Edytuj")
                popup.menu.add("Usuń")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edytuj" -> {
                            val action = ContractorPointsFragmentDirections.actionContractorPointsToAddEditContractorPoint(point.id)
                            findNavController().navigate(action)
                        }
                        "Usuń" -> showDeleteConfirm(point)
                    }
                    true
                }
                popup.show()
            }
        )

        binding.contractorPointsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.contractorPointsRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.filterTypeButton.setOnClickListener { openTypeFilter() }
        binding.filterCompanyButton.setOnClickListener { openCompanyFilter() }
        binding.addContractorPointFab.setOnClickListener {
            findNavController().navigate(R.id.action_contractorPoints_to_addEditContractorPoint)
        }
        binding.emptyAddButton.setOnClickListener {
            findNavController().navigate(R.id.action_contractorPoints_to_addEditContractorPoint)
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text?.toString()?.trim().orEmpty()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(250)
                loadContractorPoints(query.ifBlank { null })
            }
        }
    }

    private fun setupSelectionActions() {
        binding.selectAllButton.setOnClickListener {
            adapter.selectAll()
            updateSelectionPanel()
        }

        binding.deleteSelectedButton.setOnClickListener {
            confirmBulkDelete()
        }
    }

    private fun loadCompaniesAndPoints() {
        setLoadingState()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                companies = companyRepository.getAllCompanies()
                updateFilterLabels()
                loadContractorPoints(binding.searchEditText.text?.toString()?.trim().orEmpty().ifBlank { null })
            } catch (_: Exception) {
                showErrorState()
            }
        }
    }

    private fun loadContractorPoints(searchQuery: String?) {
        setLoadingState()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val points = when {
                    searchQuery.isNullOrBlank() && selectedPointType == null && selectedCompanyId == null ->
                        contractorPointRepository.getAllContractorPoints()

                    searchQuery.isNullOrBlank() && selectedPointType != null && selectedCompanyId == null ->
                        contractorPointRepository.getContractorPointsByType(selectedPointType!!)

                    searchQuery.isNullOrBlank() && selectedPointType == null && selectedCompanyId != null ->
                        contractorPointRepository.getContractorPointsByCompany(selectedCompanyId!!)

                    else ->
                        contractorPointRepository.searchContractorPoints(searchQuery)
                }

                val filtered = applyPostFilters(points)
                val companiesById = companies.associateBy { it.id }
                val items = filtered.map { point ->
                    ContractorPointListItem(
                        contractorPoint = point,
                        companyName = companiesById[point.companyId]?.name
                            ?: getString(R.string.contractor_points_company_missing)
                    )
                }

                adapter.submitList(items)
                binding.contractorPointsCountBadge.text = resources.getQuantityString(
                    R.plurals.contractor_points_count,
                    items.size,
                    items.size
                )
                binding.errorState.isVisible = false
                binding.emptyState.isVisible = items.isEmpty()
                binding.contractorPointsRecyclerView.isVisible = items.isNotEmpty()
            } catch (_: Exception) {
                showErrorState()
            } finally {
                binding.loadingState.isVisible = false
            }
        }
    }

    private fun applyPostFilters(points: List<ContractorPointEntity>): List<ContractorPointEntity> {
        return points
            .asSequence()
            .filter { selectedPointType == null || it.pointType == selectedPointType }
            .filter { selectedCompanyId == null || it.companyId == selectedCompanyId }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    private fun openTypeFilter() {
        val options = listOf(
            FilterOption("all", getString(R.string.contractor_points_type_all), "🏷️", selectedPointType == null),
            FilterOption(PointType.CP.name, "CP", "📍", selectedPointType == PointType.CP),
            FilterOption(PointType.CC.name, "CC", "🏬", selectedPointType == PointType.CC),
            FilterOption(PointType.DC.name, "DC", "🚚", selectedPointType == PointType.DC)
        )

        FilterBottomSheet.show(getParentFragment() ?: this, getString(R.string.contractor_points_filter_type_title), options) { option ->
            selectedPointType = if (option.id == "all") null else PointType.valueOf(option.id)
            updateFilterLabels()
            loadContractorPoints(binding.searchEditText.text?.toString()?.trim().orEmpty().ifBlank { null })
        }
    }

    private fun openCompanyFilter() {
        val options = mutableListOf(
            FilterOption("all", getString(R.string.contractor_points_company_all), "🏢", selectedCompanyId == null)
        )

        companies.forEach { company ->
            options.add(
                FilterOption(
                    company.id.toString(),
                    company.name,
                    "🏢",
                    selectedCompanyId == company.id
                )
            )
        }

        FilterBottomSheet.show(getParentFragment() ?: this, getString(R.string.contractor_points_filter_company_title), options) { option ->
            selectedCompanyId = if (option.id == "all") null else option.id.toLong()
            updateFilterLabels()
            loadContractorPoints(binding.searchEditText.text?.toString()?.trim().orEmpty().ifBlank { null })
        }
    }

    

    private fun updateFilterLabels() {
        binding.filterTypeButton.text =
            selectedPointType?.name ?: getString(R.string.contractor_points_filter_type_button)
        binding.filterCompanyButton.text = selectedCompanyId?.let { id ->
            companies.firstOrNull { it.id == id }?.name
        } ?: getString(R.string.contractor_points_filter_company_button)
    }

    private fun setLoadingState() {
        binding.loadingState.isVisible = true
        binding.errorState.isVisible = false
        binding.emptyState.isVisible = false
        binding.contractorPointsRecyclerView.isVisible = false
    }

    private fun showErrorState() {
        binding.loadingState.isVisible = false
        binding.contractorPointsRecyclerView.isVisible = false
        binding.emptyState.isVisible = false
        binding.errorState.isVisible = true
    }

    private fun showSelectionPanel() {
        binding.selectionPanel.visibility = View.VISIBLE
        binding.selectionPanel.alpha = 0f
        binding.selectionPanel.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        updateSelectionPanel()
    }

    private fun hideSelectionPanel() {
        binding.selectionPanel.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.selectionPanel.visibility = View.GONE
            }
            .start()
        adapter.selectionMode = false
        adapter.clearSelection()
    }

    private fun updateSelectionPanel() {
        val selectedCount = adapter.getSelectedCount()
        binding.selectionCountText.text = "Zaznaczono: $selectedCount"

        if (selectedCount == 0) {
            hideSelectionPanel()
        }
    }

    private fun confirmBulkDelete() {
        val selectedIds = adapter.getSelectedItems()
        val count = selectedIds.size

        if (count == 0) return

        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding.inflate(layoutInflater)

        sheetBinding.productNameText.text = "$count ${pluralForm(count, "punkt", "punkty", "punktów") }"

        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }

        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteBulkContractorPoints(selectedIds)
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

    private fun deleteBulkContractorPoints(pointIds: Set<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                pointIds.forEach { id ->
                    val point = contractorPointRepository.getContractorPointById(id) ?: return@forEach
                    val products = productRepository.getProductsAssignedToContractorPoint(point.id).firstOrNull().orEmpty()
                    products.forEach { product ->
                        val updated = product.copy(
                            assignedToContractorPointId = null,
                            assignedToEmployeeId = null,
                            assignmentDate = null,
                            status = ProductStatus.UNASSIGNED,
                            updatedAt = System.currentTimeMillis()
                        )
                        productRepository.updateWithHistory(updated, MovementHistoryUtils.entryUnassigned())
                    }
                    contractorPointRepository.deleteContractorPoint(point)
                }
                hideSelectionPanel()
            } catch (_: Exception) {
                // ignore for now
            }
        }
    }

    private fun showDeleteConfirm(point: ContractorPointEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val productsInPoint = productRepository.getProductsAssignedToContractorPoint(point.id).firstOrNull().orEmpty()

            val message = if (productsInPoint.isEmpty()) {
                "Czy na pewno chcesz usunąć punkt ${point.name}?"
            } else {
                "Punkt ${point.name} zawiera ${productsInPoint.size} urządzeń. Urządzenia zostaną odpięte. Kontynuować?"
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Usuń punkt kontrahenta")
                .setMessage(message)
                .setNegativeButton(getString(R.string.cancel_pl), null)
                .setPositiveButton("Usuń") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            productsInPoint.forEach { product ->
                                val updated = product.copy(
                                    assignedToContractorPointId = null,
                                    assignedToEmployeeId = null,
                                    assignmentDate = null,
                                    status = ProductStatus.UNASSIGNED,
                                    updatedAt = System.currentTimeMillis()
                                )
                                productRepository.updateWithHistory(updated, MovementHistoryUtils.entryUnassigned())
                            }
                            contractorPointRepository.deleteContractorPoint(point)
                        } catch (_: Exception) {
                            // ignore
                        }
                    }
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
