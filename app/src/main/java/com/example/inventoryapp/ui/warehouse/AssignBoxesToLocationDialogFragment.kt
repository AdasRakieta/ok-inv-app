package com.example.inventoryapp.ui.warehouse

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.databinding.DialogAssignProductsToLocationBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AssignBoxesToLocationDialogFragment(
    private val currentLocationEntityId: Long?,
    private val onBoxesAssigned: (List<BoxEntity>) -> Unit
) : DialogFragment() {

    private var _binding: DialogAssignProductsToLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: BoxesAdapter
    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.example.inventoryapp.R.style.FullScreenDialogStyle)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogAssignProductsToLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupSearchBar()
        setupActionButtons()
        loadBoxes()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Przypisz kartony"
        binding.toolbar.setNavigationOnClickListener { dismiss() }
    }

    private fun setupRecyclerView() {
        adapter = BoxesAdapter(
            onBoxClick = { box ->
                adapter.toggleSelection(box.id)
                updateSelectedCount()
            },
            onBoxLongClick = { /* no-op */ },
            onOptionsClick = { _, _ -> /* no-op */ }
        )

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AssignBoxesToLocationDialogFragment.adapter
        }
    }

    private fun setupSearchBar() {
        binding.searchEditText.addTextChangedListener { text ->
            adapter.filterByQuery(text?.toString().orEmpty())
            updateEmptyState()
        }

        binding.selectAllButton.setOnClickListener {
            if (adapter.getSelectedCount() == adapter.currentList.size && adapter.currentList.isNotEmpty()) {
                adapter.clearSelection()
            } else {
                adapter.selectAll()
            }
            updateSelectedCount()
        }
    }

    private fun setupActionButtons() {
        binding.cancelButton.setOnClickListener { dismiss() }

        binding.assignButton.text = "Przypisz (0)"
        binding.assignButton.isEnabled = false
        binding.assignButton.setOnClickListener {
            val selectedIds = adapter.getSelectedItems()
            val selectedBoxes = adapter.currentList.filter { it.id in selectedIds }
            if (selectedBoxes.isNotEmpty()) {
                onBoxesAssigned(selectedBoxes)
                dismiss()
            }
        }
    }

    private fun loadBoxes() {
        lifecycleScope.launch {
            val allBoxes = boxRepository.getAllBoxes().first()
            // exclude boxes already assigned to this location id (if provided)
            val available = if (currentLocationEntityId != null && currentLocationEntityId > 0L) {
                allBoxes.filter { it.warehouseLocationId != currentLocationEntityId }
            } else {
                allBoxes
            }
            adapter.setFullList(available)
            updateEmptyState()
            updateSelectedCount()
        }
    }

    private fun updateSelectedCount() {
        val count = adapter.getSelectedCount()
        binding.selectedCountText.text = "Zaznaczono: $count"
        binding.assignButton.text = "Przypisz ($count)"
        binding.assignButton.isEnabled = count > 0
    }

    private fun updateEmptyState() {
        binding.emptyStateLayout.isVisible = adapter.currentList.isEmpty()
        binding.productsRecyclerView.isVisible = adapter.currentList.isNotEmpty()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
