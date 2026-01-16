package com.example.inventoryapp.ui.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.databinding.FragmentArchiveBinding
import com.example.inventoryapp.ui.packages.PackagesAdapter
import com.example.inventoryapp.ui.packages.PackageWithCountAndContractor
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fragment for displaying archived packages.
 * Supports search, filtering, sorting, selection mode, and unarchiving.
 * UI and functionality matches BoxListFragment exactly.
 */
class ArchiveFragment : Fragment() {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArchiveViewModel by viewModels {
        val database = AppDatabase.getDatabase(requireContext())
        val packageRepository = PackageRepository(database.packageDao(), database.productDao(), database.boxDao())
        val contractorRepository = ContractorRepository(database.contractorDao())
        ArchiveViewModelFactory(
            packageRepository,
            contractorRepository
        )
    }

    private lateinit var adapter: PackagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            setupRecyclerView()
            setupSearchBar()
            setupClickListeners()
            observeViewModel()
        } catch (e: Exception) {
            android.util.Log.e("ArchiveFragment", "Error initializing fragment", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error loading archived packages: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = PackagesAdapter(
            onPackageClick = { packageEntity ->
                // Navigate to package details
                val action = ArchiveFragmentDirections
                    .actionArchiveToPackageDetails(packageEntity.id)
                findNavController().navigate(action)
            },
            onPackageLongClick = { packageEntity ->
                // Enter selection mode
                adapter.enterSelectionMode()
                adapter.toggleSelection(packageEntity.id)
                updateSelectionUI()
                true
            }
        )

        binding.archiveRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ArchiveFragment.adapter
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        // Unarchive FAB (replaces add button in boxes)
        binding.unarchiveFab.setOnClickListener {
            if (adapter.selectionMode) {
                // Exit selection mode
                adapter.clearSelection()
                updateSelectionUI()
            } else {
                // Show info message
                android.widget.Toast.makeText(
                    requireContext(),
                    "Long press to select packages to restore",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Empty state - no action button needed
        
        // Select All button
        binding.selectAllButton.setOnClickListener {
            val currentList = adapter.currentList
            if (adapter.getSelectedCount() == currentList.size) {
                adapter.deselectAll()
            } else {
                adapter.selectAll(currentList)
            }
            updateSelectionUI()
        }

        // Restore Selected button
        binding.restoreSelectedButton.setOnClickListener {
            showUnarchiveConfirmationDialog()
        }

        // Delete Selected button
        binding.deleteSelectedButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredPackages.collect { packages: List<PackageWithCountAndContractor> ->
                adapter.submitList(packages)
                updateEmptyState(packages.isEmpty())
            }
        }
    }

    private fun updateSelectionUI() {
        // Safety check to ensure adapter is initialized
        if (!::adapter.isInitialized) return
        
        val selectionMode = adapter.selectionMode
        val selectedCount = adapter.getSelectedCount()

        // Show/hide selection panel
        binding.selectionPanel.visibility = if (selectionMode) View.VISIBLE else View.GONE

        // Update selection count text
        binding.selectionCountText.text = "$selectedCount selected"

        // Update Select All button text
        val allSelected = selectedCount == adapter.currentList.size && selectedCount > 0
        binding.selectAllButton.text = if (allSelected) {
            getString(R.string.deselect_all)
        } else {
            getString(R.string.select_all)
        }

        // Update FAB icon and position
        if (selectionMode) {
            binding.unarchiveFab.setImageResource(android.R.drawable.ic_delete)
            
            // Move FAB up to avoid overlapping with selection panel
            binding.unarchiveFab.animate()
                .translationY(-binding.selectionPanel.height.toFloat() - 75f)
                .setDuration(200)
                .start()
        } else {
            binding.unarchiveFab.setImageResource(android.R.drawable.ic_input_add)
            
            // Move FAB back to original position
            binding.unarchiveFab.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun showUnarchiveConfirmationDialog() {
        val selectedCount = adapter.getSelectedCount()
        AlertDialog.Builder(requireContext())
            .setTitle("Restore Packages")
            .setMessage("Restore $selectedCount package(s) to active packages?")
            .setPositiveButton("Restore") { _, _ ->
                unarchiveSelectedPackages()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = adapter.getSelectedCount()
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Packages")
            .setMessage("Permanently delete $selectedCount package(s)? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedPackages()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun unarchiveSelectedPackages() {
        val selectedPackageIds = adapter.getSelectedPackages()
        viewModel.unarchivePackages(selectedPackageIds)
        adapter.clearSelection()
        updateSelectionUI()
        
        android.widget.Toast.makeText(
            requireContext(),
            "Restored ${selectedPackageIds.size} package(s)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun deleteSelectedPackages() {
        val selectedPackageIds = adapter.getSelectedPackages()
        viewModel.deletePackages(selectedPackageIds)
        adapter.clearSelection()
        updateSelectionUI()
        
        android.widget.Toast.makeText(
            requireContext(),
            "Deleted ${selectedPackageIds.size} package(s)",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.archiveRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.archiveRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
