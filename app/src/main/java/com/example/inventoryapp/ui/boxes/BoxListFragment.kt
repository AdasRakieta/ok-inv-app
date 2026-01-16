package com.example.inventoryapp.ui.boxes

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
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentBoxListBinding
import com.example.inventoryapp.data.local.dao.BoxWithCount
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fragment for displaying the list of boxes.
 * Supports search, selection mode, and deletion.
 */
class BoxListFragment : Fragment() {

    private var _binding: FragmentBoxListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BoxListViewModel by viewModels {
        BoxListViewModelFactory(
            (requireActivity().application as InventoryApplication).boxRepository
        )
    }

    private lateinit var adapter: BoxesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoxListBinding.inflate(inflater, container, false)
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
            // Log the error and show a user-friendly message
            android.util.Log.e("BoxListFragment", "Error initializing fragment", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Error loading boxes: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = BoxesAdapter(
            onBoxClick = { boxId ->
                // Navigate to box details
                val action = BoxListFragmentDirections.actionBoxListToBoxDetails(boxId)
                findNavController().navigate(action)
            },
            onBoxLongClick = { boxId ->
                // Enter selection mode
                adapter.enterSelectionMode()
                adapter.toggleSelection(boxId)
                updateSelectionUI()
                true
            }
        )

        binding.boxesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BoxListFragment.adapter
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        // Add box FAB
        binding.addBoxFab.setOnClickListener {
            if (adapter.selectionMode) {
                // Exit selection mode
                adapter.clearSelection()
                updateSelectionUI()
            } else {
                // Navigate to add box screen
                findNavController().navigate(R.id.action_boxList_to_addBox)
            }
        }

        // Empty state add button
        binding.emptyAddButton.setOnClickListener {
            findNavController().navigate(R.id.action_boxList_to_addBox)
        }

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

        // Delete Selected button
        binding.deleteSelectedButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.filteredBoxes.collect { boxes: List<BoxWithCount> ->
                adapter.submitList(boxes)
                updateEmptyState(boxes.isEmpty())
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
            binding.addBoxFab.setImageResource(android.R.drawable.ic_delete)
            
            // Move FAB up to avoid overlapping with selection panel
            binding.addBoxFab.animate()
                .translationY(-binding.selectionPanel.height.toFloat() - 75f)
                .setDuration(200)
                .start()
        } else {
            binding.addBoxFab.setImageResource(android.R.drawable.ic_input_add)
            
            // Move FAB back to original position
            binding.addBoxFab.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val selectedCount = adapter.getSelectedCount()
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_boxes))
            .setMessage(getString(R.string.confirm_delete_boxes, selectedCount))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteSelectedBoxes()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteSelectedBoxes() {
        val selectedBoxIds = adapter.getSelectedBoxes()
        viewModel.deleteBoxes(selectedBoxIds)
        adapter.clearSelection()
        updateSelectionUI()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.boxesRecyclerView.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
        } else {
            binding.boxesRecyclerView.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
