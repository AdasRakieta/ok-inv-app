package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentBoxListBinding
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BoxListFragment : Fragment() {

    private var _binding: FragmentBoxListBinding? = null
    private val binding get() = _binding!!

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }

    private lateinit var boxesAdapter: BoxesAdapter
    private var allBoxes: List<com.example.inventoryapp.data.local.entities.BoxEntity> = emptyList()
    private var currentQuery: String = ""
    private var searchJob: Job? = null

    private enum class SortOption { NEWEST, OLDEST, NAME_ASC, NAME_DESC }
    private var sortOption: SortOption = SortOption.NEWEST

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBoxListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        boxesAdapter = BoxesAdapter(onBoxClick = { box ->
            val bundle = android.os.Bundle().apply {
                putLong("boxId", box.id)
                putString("qrUid", box.qrUid)
            }
            findNavController().navigate(com.example.inventoryapp.R.id.boxDetailsFragment, bundle)
        })

        binding.boxesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = boxesAdapter
            setHasFixedSize(false)
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
            searchJob = lifecycleScope.launch {
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
            FilterBottomSheet.show(this, "↕️ Sortuj", options) { option ->
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

        lifecycleScope.launch {
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

        boxesAdapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
