package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentBoxListBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BoxListFragment : Fragment() {

    private var _binding: FragmentBoxListBinding? = null
    private val binding get() = _binding!!

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }

    private lateinit var boxesAdapter: BoxesAdapter

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

        binding.addBoxFab.setOnClickListener {
            // Open full Add/Edit Box screen
            findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, android.os.Bundle().apply {
                putLong("warehouseLocationId", 0L)
            })
        }

        lifecycleScope.launch {
            boxRepository.getAllBoxes().collectLatest { boxes ->
                if (boxes.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.boxesRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.boxesRecyclerView.visibility = View.VISIBLE
                    boxesAdapter.submitList(boxes)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
