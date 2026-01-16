package com.example.inventoryapp.ui.packages

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentPackageListBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PackageListFragment : Fragment() {

    private var _binding: FragmentPackageListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PackagesViewModel
    private lateinit var adapter: PackagesAdapter
    private val fabOffset by lazy { resources.getDimension(R.dimen.selection_panel_fab_spacing) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        // Use shared repositories from Application (ensures DeviceMovementRepository is available)
        val repository = app.packageRepository
        val contractorRepository = com.example.inventoryapp.data.repository.ContractorRepository(
            AppDatabase.getDatabase(requireContext()).contractorDao()
        )
        val factory = PackagesViewModelFactory(repository, contractorRepository)
        val vm: PackagesViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupSearchBar()
        setupFilterButtons()
        observePackages()
        observeSortOrder()
    }

    private fun setupRecyclerView() {
        adapter = PackagesAdapter(
            onPackageClick = { packageEntity ->
                val action = PackageListFragmentDirections
                    .actionPackagesToPackageDetails(packageEntity.id)
                findNavController().navigate(action)
            },
            onPackageLongClick = { packageEntity ->
                adapter.enterSelectionMode()
                adapter.toggleSelection(packageEntity.id)
                updateSelectionUI()
                true
            }
        )
        
        binding.packagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PackageListFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addPackageFab.setOnClickListener {
            if (adapter.selectionMode) {
                exitSelectionMode()
            } else {
                showCreatePackageDialog()
            }
        }
        
        binding.emptyAddButton.setOnClickListener {
            showCreatePackageDialog()
        }

        binding.selectAllButton.setOnClickListener {
            val totalCount = adapter.itemCount
            val selectedCount = adapter.getSelectedCount()
            
            if (selectedCount == totalCount) {
                adapter.deselectAll()
            } else {
                adapter.selectAll(adapter.currentList)
            }
            updateSelectionUI()
        }

        binding.deleteSelectedButton.setOnClickListener {
            if (adapter.getSelectedCount() > 0) {
                showDeleteConfirmationDialog()
            }
        }

        binding.bulkEditButton.setOnClickListener {
            if (adapter.getSelectedCount() > 0) {
                showBulkEditDialog()
            }
        }
    }

    private fun updateSelectionUI() {
        if (adapter.selectionMode) {
            val count = adapter.getSelectedCount()
            val totalCount = adapter.itemCount
            
            // Show selection panel
            binding.selectionPanel.visibility = View.VISIBLE
            binding.selectionCountText.text = "$count selected"
            
            // Update Select All button text
            binding.selectAllButton.text = if (count == totalCount) "Deselect All" else "Select All"
            
            // Change FAB to cancel icon
            binding.addPackageFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            
            // Move FAB up to avoid overlapping with selection panel
            binding.addPackageFab.animate()
                .translationY(-binding.selectionPanel.height.toFloat() - fabOffset)
                .setDuration(200)
                .start()
        } else {
            // Hide selection panel
            binding.selectionPanel.visibility = View.GONE
            
            // Restore FAB to add icon
            binding.addPackageFab.setImageResource(android.R.drawable.ic_input_add)
            
            // Move FAB back to original position
            binding.addPackageFab.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun showArchiveConfirmationDialog() {
        val count = adapter.getSelectedCount()
        val selectedIds = adapter.getSelectedPackages()
        
        // Check if all selected packages are RETURNED
        viewLifecycleOwner.lifecycleScope.launch {
            val packages = selectedIds.mapNotNull { id ->
                viewModel.getPackageById(id)
            }
            
            val nonReturnedCount = packages.count { it.status != "RETURNED" }
            
            if (nonReturnedCount > 0) {
                Toast.makeText(
                    requireContext(),
                    "Only returned packages can be archived. $nonReturnedCount package(s) are not returned.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            
            // All selected packages are RETURNED, proceed
            AlertDialog.Builder(requireContext())
                .setTitle("Archive Packages")
                .setMessage("Archive $count selected package(s)? They will be moved to the Archive tab.")
                .setPositiveButton("Archive") { _, _ ->
                    archiveSelectedPackages()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun archiveSelectedPackages() {
        val selectedIds = adapter.getSelectedPackages().toList()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.archivePackages(selectedIds)
            Toast.makeText(
                requireContext(),
                "Archived ${selectedIds.size} package(s)",
                Toast.LENGTH_SHORT
            ).show()
            exitSelectionMode()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val count = adapter.getSelectedCount()
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Packages")
            .setMessage("Are you sure you want to delete $count selected package(s)?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedPackages()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedPackages() {
        val selectedIds = adapter.getSelectedPackages()
        viewLifecycleOwner.lifecycleScope.launch {
            selectedIds.forEach { packageId ->
                viewModel.deletePackage(packageId)
            }
            Toast.makeText(
                requireContext(),
                "Deleted ${selectedIds.size} package(s)",
                Toast.LENGTH_SHORT
            ).show()
            exitSelectionMode()
        }
    }

    private fun exitSelectionMode() {
        adapter.clearSelection()
        updateSelectionUI()
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupFilterButtons() {
        binding.statusFilterButton.setOnClickListener {
            showStatusFilterDialog()
        }
        
        binding.contractorFilterButton.setOnClickListener {
            showContractorFilterDialog()
        }
        
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showStatusFilterDialog() {
        val statuses = CategoryHelper.PackageStatus.PACKAGE_STATUSES.toList()
        val checkedItems = BooleanArray(statuses.size)
        
        // Initialize checked states based on current filters
        viewLifecycleOwner.lifecycleScope.launch {
            val currentFilters = viewModel.packagesWithCount.value
                .map { it.packageWithCount.packageEntity.status }
                .toSet()
            
            statuses.forEachIndexed { index, status ->
                checkedItems[index] = currentFilters.contains(status)
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Filter by Status")
            .setMultiChoiceItems(
                statuses.toTypedArray(),
                checkedItems
            ) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Apply") { _, _ ->
                val selectedStatuses = statuses.filterIndexed { index, _ -> 
                    checkedItems[index] 
                }.toSet()
                viewModel.setStatusFilters(selectedStatuses)
                updateStatusButtonText(selectedStatuses.size)
            }
            .setNeutralButton("Clear") { _, _ ->
                viewModel.setStatusFilters(emptySet())
                updateStatusButtonText(0)
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }

    private fun showContractorFilterDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contractors = viewModel.allContractors.first()
            val items = mutableListOf("(Unassigned)")
            items.addAll(contractors.map { it.name })
            
            val checkedItems = BooleanArray(items.size)
            
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Filter by Contractor")
                .setMultiChoiceItems(
                    items.toTypedArray(),
                    checkedItems
                ) { _, which, isChecked ->
                    checkedItems[which] = isChecked
                }
                .setPositiveButton("Apply") { _, _ ->
                    val selectedIds = mutableSetOf<Long>()
                    checkedItems.forEachIndexed { index, isChecked ->
                        if (isChecked) {
                            if (index == 0) {
                                selectedIds.add(-1L) // Unassigned
                            } else {
                                contractors[index - 1].id?.let { selectedIds.add(it) }
                            }
                        }
                    }
                    viewModel.setContractorFilters(selectedIds)
                    updateContractorButtonText(selectedIds.size)
                }
                .setNeutralButton("Clear") { _, _ ->
                    viewModel.setContractorFilters(emptySet())
                    updateContractorButtonText(0)
                }
                .setNegativeButton("Cancel", null)
                .create()
            
            dialog.show()
        }
    }

    private fun showSortDialog() {
        val sortOptions = listOf(
            "Name (A-Z)",
            "Name (Z-A)",
            "Status (A-Z)",
            "Status (Z-A)",
            "Product Count (Low-High)",
            "Product Count (High-Low)",
            "Date (Oldest First)",
            "Date (Newest First)"
        )
        
        val currentSort = viewModel.sortOrder.value
        val currentIndex = when (currentSort) {
            PackageSortOrder.NAME_ASC -> 0
            PackageSortOrder.NAME_DESC -> 1
            PackageSortOrder.STATUS_ASC -> 2
            PackageSortOrder.STATUS_DESC -> 3
            PackageSortOrder.PRODUCT_COUNT_ASC -> 4
            PackageSortOrder.PRODUCT_COUNT_DESC -> 5
            PackageSortOrder.DATE_ASC -> 6
            PackageSortOrder.DATE_DESC -> 7
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Sort Packages")
            .setSingleChoiceItems(sortOptions.toTypedArray(), currentIndex) { dialog, which ->
                val sortOrder = when (which) {
                    0 -> PackageSortOrder.NAME_ASC
                    1 -> PackageSortOrder.NAME_DESC
                    2 -> PackageSortOrder.STATUS_ASC
                    3 -> PackageSortOrder.STATUS_DESC
                    4 -> PackageSortOrder.PRODUCT_COUNT_ASC
                    5 -> PackageSortOrder.PRODUCT_COUNT_DESC
                    6 -> PackageSortOrder.DATE_ASC
                    7 -> PackageSortOrder.DATE_DESC
                    else -> PackageSortOrder.NAME_ASC
                }
                viewModel.setSortOrder(sortOrder)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateStatusButtonText(count: Int) {
        binding.statusFilterButton.text = if (count > 0) {
            "Status ($count)"
        } else {
            "Status"
        }
    }

    private fun updateContractorButtonText(count: Int) {
        binding.contractorFilterButton.text = if (count > 0) {
            "Contractor ($count)"
        } else {
            "Contractor"
        }
    }

    private fun observeSortOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sortOrder.collect { sortOrder ->
                val sortText = when (sortOrder) {
                    PackageSortOrder.NAME_ASC -> "Sort ↑A"
                    PackageSortOrder.NAME_DESC -> "Sort ↓Z"
                    PackageSortOrder.STATUS_ASC -> "Sort ↑S"
                    PackageSortOrder.STATUS_DESC -> "Sort ↓S"
                    PackageSortOrder.PRODUCT_COUNT_ASC -> "Sort ↑#"
                    PackageSortOrder.PRODUCT_COUNT_DESC -> "Sort ↓#"
                    PackageSortOrder.DATE_ASC -> "Sort ↑📅"
                    PackageSortOrder.DATE_DESC -> "Sort ↓📅"
                }
                binding.sortButton.text = sortText
            }
        }
    }

    private fun showCreatePackageDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Package name"
            setSingleLine(true)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Create Package")
            .setMessage("Enter a name for the new package")
            .setView(editText)
            .setPositiveButton("Create") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createPackage(name)
                    Toast.makeText(requireContext(), "Package created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Package name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkEditDialog() {
        val selectedCount = adapter.getSelectedCount()
        val options = arrayOf("Change Status", "Assign Contractor", "Archive Returned")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Bulk Edit ($selectedCount packages)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showBulkStatusChangeDialog()
                    1 -> showBulkContractorChangeDialog()
                    2 -> showArchiveConfirmationDialog()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkStatusChangeDialog() {
        val statuses = CategoryHelper.PackageStatus.PACKAGE_STATUSES
        
        AlertDialog.Builder(requireContext())
            .setTitle("Change Status")
            .setItems(statuses as Array<CharSequence>) { _, which ->
                val newStatus = statuses[which]
                val selectedIds = adapter.getSelectedPackages()
                
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Status Change")
                    .setMessage("Change status to $newStatus for ${selectedIds.size} package(s)?")
                    .setPositiveButton("Confirm") { _, _ ->
                        viewModel.bulkUpdateStatus(selectedIds, newStatus)
                        Toast.makeText(
                            requireContext(),
                            "Updated ${selectedIds.size} package(s) to $newStatus",
                            Toast.LENGTH_SHORT
                        ).show()
                        exitSelectionMode()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBulkContractorChangeDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            val contractors = viewModel.allContractors.first()
            val items = mutableListOf("(None)")
            items.addAll(contractors.map { it.name })
            
            AlertDialog.Builder(requireContext())
                .setTitle("Assign Contractor")
                .setItems(items.toTypedArray()) { _, which ->
                    val contractorId = if (which == 0) null else contractors[which - 1].id
                    val contractorName = items[which]
                    val selectedIds = adapter.getSelectedPackages()
                    
                    AlertDialog.Builder(requireContext())
                        .setTitle("Confirm Contractor Assignment")
                        .setMessage("Assign contractor \"$contractorName\" to ${selectedIds.size} package(s)?")
                        .setPositiveButton("Confirm") { _, _ ->
                            viewModel.bulkUpdateContractor(selectedIds, contractorId)
                            Toast.makeText(
                                requireContext(),
                                "Assigned \"$contractorName\" to ${selectedIds.size} package(s)",
                                Toast.LENGTH_SHORT
                            ).show()
                            exitSelectionMode()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun observePackages() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.packagesWithCount.collect { packages ->
                if (packages.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.packagesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.packagesRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(packages)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
