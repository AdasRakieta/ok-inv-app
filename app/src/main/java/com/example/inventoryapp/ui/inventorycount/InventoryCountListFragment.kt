package com.example.inventoryapp.ui.inventorycount

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
import com.example.inventoryapp.databinding.FragmentInventoryCountListBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.InventoryCountRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fragment displaying list of inventory count sessions.
 * Allows creating, viewing, and deleting sessions.
 */
class InventoryCountListFragment : Fragment() {

    private var _binding: FragmentInventoryCountListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: InventoryCountListViewModel
    private lateinit var adapter: InventoryCountSessionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val repository = InventoryCountRepository(
            database.inventoryCountDao(),
            database.productDao()
        )
        val factory = InventoryCountListViewModelFactory(repository)
        val vm: InventoryCountListViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryCountListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupSearchBar()
        observeSessions()
    }

    private fun setupRecyclerView() {
        adapter = InventoryCountSessionsAdapter(
            onSessionClick = { sessionWithCount ->
                val action = InventoryCountListFragmentDirections
                    .actionInventoryCountListToInventoryCountSession(sessionWithCount.session.id)
                findNavController().navigate(action)
            },
            onSessionLongClick = { sessionWithCount ->
                adapter.enterSelectionMode()
                adapter.toggleSelection(sessionWithCount.session.id)
                updateSelectionUI()
                true
            }
        )
        
        binding.sessionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@InventoryCountListFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addSessionFab.setOnClickListener {
            if (adapter.selectionMode) {
                exitSelectionMode()
            } else {
                showCreateSessionDialog()
            }
        }
        
        binding.emptyAddButton.setOnClickListener {
            showCreateSessionDialog()
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
    }

    private fun updateSelectionUI() {
        if (adapter.selectionMode) {
            val count = adapter.getSelectedCount()
            binding.selectionPanel.visibility = View.VISIBLE
            binding.selectionCountText.text = "$count selected"
            binding.addSessionFab.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            
            // Move FAB up to avoid overlapping with selection panel
            binding.addSessionFab.animate()
                .translationY(-binding.selectionPanel.height.toFloat() - 75f)
                .setDuration(200)
                .start()
        } else {
            binding.selectionPanel.visibility = View.GONE
            binding.addSessionFab.setImageResource(android.R.drawable.ic_input_add)
            
            // Move FAB back to original position
            binding.addSessionFab.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val count = adapter.getSelectedCount()
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Sessions")
            .setMessage("Are you sure you want to delete $count selected session(s)?")
            .setPositiveButton("Delete") { _, _ ->
                deleteSelectedSessions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteSelectedSessions() {
        val selectedIds = adapter.getSelectedSessions()
        viewLifecycleOwner.lifecycleScope.launch {
            selectedIds.forEach { sessionId ->
                viewModel.deleteSessionById(sessionId)
            }
            Toast.makeText(
                requireContext(),
                "Deleted ${selectedIds.size} session(s)",
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

    private fun showCreateSessionDialog() {
        val dialogView = layoutInflater.inflate(
            com.example.inventoryapp.R.layout.dialog_create_session,
            null
        )
        val nameInput = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.sessionNameInput)
        val notesInput = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.sessionNotesInput)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Create Inventory Count Session")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                val notes = notesInput.text.toString().trim().takeIf { it.isNotEmpty() }
                
                if (name.isNotEmpty()) {
                    viewModel.createSession(name, notes)
                    Toast.makeText(requireContext(), "Session created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Session name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessions.collect { sessions ->
                if (sessions.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.sessionsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.sessionsRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(sessions)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
