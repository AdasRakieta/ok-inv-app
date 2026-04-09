package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.widget.addTextChangedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentBoxListBinding
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BoxListFragment : Fragment() {

    private var _binding: FragmentBoxListBinding? = null
    private val binding get() = _binding!!

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }
    private val productRepository by lazy { (requireActivity().application as InventoryApplication).productRepository }
    private val warehouseLocationRepository by lazy { (requireActivity().application as InventoryApplication).warehouseLocationRepository }

    private lateinit var boxesAdapter: BoxesAdapter
    private var actionMode: ActionMode? = null
    private val selectionActionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            (requireActivity() as AppCompatActivity).menuInflater.inflate(com.example.inventoryapp.R.menu.selection_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            return when (item.itemId) {
                com.example.inventoryapp.R.id.action_delete -> {
                    confirmBulkDelete()
                    mode.finish()
                    true
                }
                com.example.inventoryapp.R.id.action_select_all -> {
                    boxesAdapter.selectAll()
                    updateSelectionPanel()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            hideSelectionPanel()
        }
    }
    private var allBoxes: List<com.example.inventoryapp.data.local.entities.BoxEntity> = emptyList()
    private var currentQuery: String = ""
    private var searchJob: Job? = null
    private var countsJob: Job? = null

    private enum class SortOption { NEWEST, OLDEST, NAME_ASC, NAME_DESC }
    private var sortOption: SortOption = SortOption.NEWEST

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBoxListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        boxesAdapter = BoxesAdapter(
            onBoxClick = { box ->
                if (boxesAdapter.selectionMode) {
                    boxesAdapter.toggleSelection(box.id)
                    updateSelectionPanel()
                } else {
                    val bundle = android.os.Bundle().apply {
                        putLong("boxId", box.id)
                        putString("qrUid", box.qrUid)
                    }
                    findNavController().navigate(com.example.inventoryapp.R.id.boxDetailsFragment, bundle)
                }
            },
            onBoxLongClick = { box ->
                if (!boxesAdapter.selectionMode) {
                    boxesAdapter.selectionMode = true
                    boxesAdapter.toggleSelection(box.id)
                    showSelectionPanel()
                    if (actionMode == null) {
                        actionMode = (requireActivity() as AppCompatActivity).startSupportActionMode(selectionActionModeCallback)
                    }
                    actionMode?.title = "Zaznaczono: ${boxesAdapter.getSelectedCount()}"
                }
            },
            onOptionsClick = { box, anchor ->
                val popup = PopupMenu(requireContext(), anchor)
                popup.menu.add("Edytuj")
                popup.menu.add("Usuń")
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Edytuj" -> {
                            val bundle = android.os.Bundle().apply {
                                putLong("boxId", box.id)
                                putLong("warehouseLocationId", box.warehouseLocationId ?: 0L)
                            }
                            findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, bundle)
                        }
                        "Usuń" -> showDeleteConfirm(box)
                    }
                    true
                }
                popup.show()
            }
        )

        binding.boxesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = boxesAdapter
            setHasFixedSize(false)
        }

        // Load warehouse locations so adapter can display location codes (e.g., P1)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                warehouseLocationRepository.getAllLocations().collect { locations ->
                    val locMap = locations.associate { it.id to (it.code ?: "") }
                    boxesAdapter.setLocationsMap(locMap)
                }
            }
        }

        // FAB opens Add/Edit Box screen
        binding.addBoxFab.setOnClickListener {
            findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, android.os.Bundle().apply {
                putLong("warehouseLocationId", 0L)
            })
        }

        // Empty-state Create button should behave the same
        binding.emptyAddButton.setOnClickListener {
            findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, android.os.Bundle().apply {
                putLong("warehouseLocationId", 0L)
            })
        }

        // Debounced search text -> filter list (300ms)
        binding.searchEditText.addTextChangedListener { editable ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(300)
                currentQuery = editable?.toString() ?: ""
                applyFilterAndSort()
            }
        }

        // Sort button -> use app's FilterBottomSheet for parity with other lists
        binding.sortButton.setOnClickListener {
            val options = listOf(
                FilterOption("NEWEST", "Najnowsze", "🆕", sortOption == SortOption.NEWEST),
                FilterOption("OLDEST", "Najstarsze", "📅", sortOption == SortOption.OLDEST),
                FilterOption("NAME_ASC", "Nazwa A-Z", "🔤", sortOption == SortOption.NAME_ASC),
                FilterOption("NAME_DESC", "Nazwa Z-A", "🔡", sortOption == SortOption.NAME_DESC)
            )
            FilterBottomSheet.show(this, "Sortuj", options) { option ->
                sortOption = when (option.id) {
                    "NEWEST" -> SortOption.NEWEST
                    "OLDEST" -> SortOption.OLDEST
                    "NAME_ASC" -> SortOption.NAME_ASC
                    "NAME_DESC" -> SortOption.NAME_DESC
                    else -> sortOption
                }
                applyFilterAndSort()
            }
        }

        // Selection panel actions
        binding.selectAllButton.setOnClickListener {
            boxesAdapter.selectAll()
            updateSelectionPanel()
        }

        binding.deleteSelectedButton.setOnClickListener {
            confirmBulkDelete()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                boxRepository.getAllBoxes().collectLatest { boxes ->
                    allBoxes = boxes
                    if (boxes.isEmpty()) {
                        binding.emptyStateLayout.visibility = View.VISIBLE
                        binding.boxesRecyclerView.visibility = View.GONE
                    } else {
                        binding.emptyStateLayout.visibility = View.GONE
                        binding.boxesRecyclerView.visibility = View.VISIBLE
                        applyFilterAndSort()
                    }

                    countsJob?.cancel()
                    countsJob = viewLifecycleOwner.lifecycleScope.launch {
                        val counts = mutableMapOf<Long, Int>()
                        for (box in boxes) {
                            val products = productRepository.getProductsByBoxId(box.id).firstOrNull().orEmpty()
                            counts[box.id] = products.size
                        }
                        boxesAdapter.setCountsMap(counts)
                    }
                }
            }
        }
    }

    private fun applyFilterAndSort() {
        var list = allBoxes
        if (currentQuery.isNotBlank()) {
            val q = currentQuery.trim()
            list = list.filter { it.name.contains(q, ignoreCase = true) || (it.description?.contains(q, ignoreCase = true) ?: false) }
        }

        list = when (sortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.OLDEST -> list.sortedBy { it.createdAt }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase() }
        }

        // Keep adapter's internal full/filtered lists in sync so counts updates
        // (which call setCountsMap) won't accidentally submit an empty list.
        boxesAdapter.setFullList(list)
        boxesAdapter.filterByQuery(currentQuery)
    }

    private fun showSelectionPanel() {
        val panel = _binding?.selectionPanel ?: return
        panel.visibility = View.VISIBLE
        panel.alpha = 0f
        panel.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        updateSelectionPanel()
    }

    private fun hideSelectionPanel() {
        val panel = _binding?.selectionPanel ?: return
        panel.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                panel.visibility = View.GONE
            }
            .start()
        boxesAdapter.selectionMode = false
        boxesAdapter.clearSelection()
    }

    private fun updateSelectionPanel() {
        val selectedCount = boxesAdapter.getSelectedCount()
        binding.selectionCountText.text = "Zaznaczono: $selectedCount"

        actionMode?.title = "Zaznaczono: $selectedCount"

        if (selectedCount == 0) {
            hideSelectionPanel()
            actionMode?.finish()
        }
    }

    private fun confirmBulkDelete() {
        val selectedIds = boxesAdapter.getSelectedItems()
        val count = selectedIds.size

        if (count == 0) return

        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding.inflate(layoutInflater)

        sheetBinding.titleText.text = "Usuń kartony"
        sheetBinding.productNameText.text = "$count ${pluralForm(count, "karton", "kartony", "kartonów")}"
        sheetBinding.warningTitleText.text = "Czy na pewno chcesz usunąć wybrane kartony?"
        sheetBinding.warningDetailsText.text = buildString {
            append("• Usuniętych kartonów: $count\n")
            append("• Produkty zostaną odpięte od kartonów\n")
            append("• Tej operacji nie można cofnąć")
        }

        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }

        sheetBinding.deleteButton.text = "Usuń kartony"
        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteBulkBoxes(selectedIds)
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

    private fun deleteBulkBoxes(boxIds: Set<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                boxIds.forEach { id ->
                    val box = allBoxes.find { it.id == id } ?: boxRepository.getBoxByIdOnce(id)
                    if (box != null) {
                        val products = productRepository.getProductsByBoxId(box.id).firstOrNull().orEmpty()
                        products.forEach { product ->
                            val updated = product.copy(boxId = null)
                            productRepository.updateWithHistory(updated, "Karton ${box.name} został usunięty")
                        }
                        boxRepository.deleteBox(box)
                    }
                }
                hideSelectionPanel()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun showDeleteConfirm(box: com.example.inventoryapp.data.local.entities.BoxEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            val productsInBox = productRepository.getProductsByBoxId(box.id).firstOrNull().orEmpty()

            val message = if (productsInBox.isEmpty()) {
                "Czy na pewno chcesz usunąć karton ${box.name}?"
            } else {
                "Karton ${box.name} zawiera ${productsInBox.size} produktów. Produkty zostaną odpięte od kartonu. Kontynuować?"
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Usuń karton")
                .setMessage(message)
                .setNegativeButton(getString(com.example.inventoryapp.R.string.cancel_pl), null)
                .setPositiveButton("Usuń") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            productsInBox.forEach { product ->
                                val updated = product.copy(boxId = null)
                                productRepository.updateWithHistory(updated, "Karton ${box.name} został usunięty")
                            }
                            boxRepository.deleteBox(box)
                        } catch (e: Exception) {
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
        countsJob?.cancel()
        _binding = null
    }
}
