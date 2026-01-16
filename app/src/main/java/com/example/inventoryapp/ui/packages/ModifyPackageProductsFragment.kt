package com.example.inventoryapp.ui.packages

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
import com.example.inventoryapp.databinding.FragmentModifyPackageProductsBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ModifyPackageProductsFragment : Fragment() {

    private var _binding: FragmentModifyPackageProductsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ModifyPackageProductsViewModel
    private lateinit var adapter: SelectableProductsAdapter
    private val args: ModifyPackageProductsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val productRepository = ProductRepository(database.productDao())
        val packageRepository = PackageRepository(database.packageDao(), database.productDao(), database.boxDao())
        val factory = ModifyPackageProductsViewModelFactory(
            productRepository,
            packageRepository,
            args.packageId
        )
        val vm: ModifyPackageProductsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModifyPackageProductsBinding.inflate(inflater, container, false)
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
            adapter = this@ModifyPackageProductsFragment.adapter
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
                viewModel.removeProductsFromPackage(selectedIds)
                Toast.makeText(
                    requireContext(),
                    "${selectedIds.size} product(s) removed from package",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productsInPackage.collect { products ->
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
