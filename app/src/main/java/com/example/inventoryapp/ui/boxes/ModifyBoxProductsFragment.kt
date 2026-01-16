package com.example.inventoryapp.ui.boxes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.databinding.FragmentModifyBoxProductsBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.ui.packages.SelectableProductsAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ModifyBoxProductsFragment : Fragment() {

    private var _binding: FragmentModifyBoxProductsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ModifyBoxProductsViewModel
    private lateinit var adapter: SelectableProductsAdapter
    private val args: ModifyBoxProductsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val productRepository = ProductRepository(database.productDao())
        val boxRepository = BoxRepository(database.boxDao(), database.productDao(), database.packageDao())
        val factory = ModifyBoxProductsViewModelFactory(
            productRepository,
            boxRepository,
            args.boxId
        )
        val vm: ModifyBoxProductsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModifyBoxProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeProducts()
    }

    private fun setupRecyclerView() {
        adapter = SelectableProductsAdapter { selectedIds ->
            updateSelectedCount(selectedIds.size)
        }
        
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ModifyBoxProductsFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.selectAllButton.setOnClickListener {
            adapter.selectAll()
        }

        binding.deselectAllButton.setOnClickListener {
            adapter.deselectAll()
        }
        
        binding.removeButton.setOnClickListener {
            val selectedIds = adapter.getSelectedProductIds()
            if (selectedIds.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one product to remove",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.removeProductsFromBox(selectedIds)
                Toast.makeText(
                    requireContext(),
                    "${selectedIds.size} product(s) removed from box",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productsInBox.collect { products ->
                if (products.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.productsRecyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.productsRecyclerView.visibility = View.VISIBLE
                    adapter.submitList(products)
                }
            }
        }
    }

    private fun updateSelectedCount(count: Int) {
        binding.selectedCountText.text = if (count == 1) {
            "1 product selected for removal"
        } else {
            "$count products selected for removal"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
