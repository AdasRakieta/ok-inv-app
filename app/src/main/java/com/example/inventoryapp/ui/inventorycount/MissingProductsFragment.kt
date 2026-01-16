package com.example.inventoryapp.ui.inventorycount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.InventoryCountRepository
import com.example.inventoryapp.databinding.FragmentMissingProductsBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Full-screen fragment for viewing and searching missing products in an inventory count session.
 */
class MissingProductsFragment : Fragment() {

    private var _binding: FragmentMissingProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: InventoryCountSessionViewModel

    private val args: MissingProductsFragmentArgs by navArgs()

    private lateinit var adapter: MissingProductsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val repository = InventoryCountRepository(
            database.inventoryCountDao(),
            database.productDao()
        )
        val packageRepository = (requireActivity().application as com.example.inventoryapp.InventoryApplication).packageRepository
        val productRepository = (requireActivity().application as com.example.inventoryapp.InventoryApplication).productRepository
        val factory = InventoryCountSessionViewModelFactory(repository, packageRepository, productRepository, args.sessionId)
        val vm: InventoryCountSessionViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMissingProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchAndFilters()
        setupToolbar()
        loadMissingProducts()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = MissingProductsAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MissingProductsFragment.adapter
        }
    }

    private fun setupSearchAndFilters() {
        // Search input listener
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateDisplayedProducts()
            }
        })

        // Filter checkboxes listeners
        binding.showAssignedCheckbox.setOnCheckedChangeListener { _, _ -> updateDisplayedProducts() }
        binding.showUnassignedCheckbox.setOnCheckedChangeListener { _, _ -> updateDisplayedProducts() }
    }

    private fun loadMissingProducts() {
        // Show loading indicator
        binding.loadingLayout.visibility = View.VISIBLE
        binding.searchCard.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val missingProducts = viewModel.getMissingProducts()

                if (missingProducts.isEmpty()) {
                    Toast.makeText(requireContext(), "All products have been scanned", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                    return@launch
                }

                binding.toolbar.title = "Missing Products (${missingProducts.size})"
                adapter.submitList(missingProducts)
                updateDisplayedProducts()

                // Hide loading and show content
                binding.loadingLayout.visibility = View.GONE
                binding.searchCard.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE

            } catch (e: Exception) {
                // Hide loading on error
                binding.loadingLayout.visibility = View.GONE
                binding.searchCard.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE

                Toast.makeText(requireContext(), "Error loading missing products: ${e.message}", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun updateDisplayedProducts() {
        val searchQuery = binding.searchInput.text.toString().trim().lowercase()
        val showAssigned = binding.showAssignedCheckbox.isChecked
        val showUnassigned = binding.showUnassignedCheckbox.isChecked

        val allProducts = adapter.currentList

        val filteredProducts = allProducts.filter { productWithPackage ->
            // Search filter
            val matchesSearch = searchQuery.isEmpty() ||
                    productWithPackage.product.name.lowercase().contains(searchQuery) ||
                    (productWithPackage.product.serialNumber?.lowercase()?.contains(searchQuery) == true) ||
                    (productWithPackage.packageInfo?.name?.lowercase()?.contains(searchQuery) == true)

            // Package assignment filter
            val isAssigned = productWithPackage.packageInfo != null
            val matchesFilter = (showAssigned && isAssigned) || (showUnassigned && !isAssigned)

            matchesSearch && matchesFilter
        }

        adapter.submitList(filteredProducts)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}