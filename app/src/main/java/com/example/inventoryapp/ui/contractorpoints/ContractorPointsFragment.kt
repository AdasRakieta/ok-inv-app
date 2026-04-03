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
import com.example.inventoryapp.databinding.BottomSheetFilterBinding
import com.example.inventoryapp.databinding.FragmentContractorPointsBinding
import com.example.inventoryapp.ui.products.FilterOption
import com.example.inventoryapp.ui.products.FilterOptionsAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ContractorPointsFragment : Fragment() {

    private var _binding: FragmentContractorPointsBinding? = null
    private val binding get() = _binding!!

    private val args: ContractorPointsFragmentArgs by navArgs()

    private lateinit var contractorPointRepository: ContractorPointRepository
    private lateinit var companyRepository: CompanyRepository

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
        updateFilterLabels()
        loadCompaniesAndPoints()
    }

    private fun setupRecyclerView() {
        adapter = ContractorPointsAdapter { point ->
            val action = ContractorPointsFragmentDirections.actionContractorPointsToDetails(point.id)
            findNavController().navigate(action)
        }

        binding.contractorPointsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.contractorPointsRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
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

        showFilterBottomSheet(getString(R.string.contractor_points_filter_type_title), options) { option ->
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

        showFilterBottomSheet(getString(R.string.contractor_points_filter_company_title), options) { option ->
            selectedCompanyId = if (option.id == "all") null else option.id.toLong()
            updateFilterLabels()
            loadContractorPoints(binding.searchEditText.text?.toString()?.trim().orEmpty().ifBlank { null })
        }
    }

    private fun showFilterBottomSheet(
        title: String,
        options: List<FilterOption>,
        onOptionSelected: (FilterOption) -> Unit
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetFilterBinding.inflate(layoutInflater)

        sheetBinding.sheetTitle.text = title
        val sheetAdapter = FilterOptionsAdapter(options) { selected ->
            bottomSheet.dismiss()
            onOptionSelected(selected)
        }
        sheetBinding.optionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.optionsRecyclerView.adapter = sheetAdapter

        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
