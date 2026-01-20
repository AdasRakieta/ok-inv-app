package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentWarehouseBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class WarehouseFragment : Fragment() {

    private var _binding: FragmentWarehouseBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private lateinit var locationsAdapter: WarehouseLocationsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupActions()
        loadWarehouseData()
    }

    private fun setupRecyclerView() {
        locationsAdapter = WarehouseLocationsListAdapter(
            onLocationClick = { locationName ->
                val action = WarehouseFragmentDirections.actionWarehouseToLocationDetails(locationName)
                findNavController().navigate(action)
            }
        )

        binding.locationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationsAdapter
        }
    }

    private fun setupActions() {
        binding.addLocationFab.setOnClickListener {
            showAddLocationDialog()
        }
    }

    private fun loadWarehouseData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { products ->
                    val inStockProducts = products.filter { it.status == ProductStatus.IN_STOCK }

                    // Group by location from products
                    val locationGroups = inStockProducts.groupBy { product ->
                        val shelf = product.shelf ?: "Magazyn"
                        val bin = product.bin ?: ""
                        shelf + (if (bin.isNotBlank()) " / $bin" else "")
                    }
                    val storedLocations = locationStorage.getLocations()

                    categoryRepository.getAllCategories().collect { categories ->
                        // Build location cards from product groups
                        val productCards = locationGroups.map { (locationName, productsInLocation) ->
                            val categoryIds = productsInLocation.map { it.categoryId }.distinct()
                            val categoryNames = categories.filter { it.id in categoryIds }
                                .joinToString(", ") { it.name }

                            WarehouseLocationCard(
                                name = locationName,
                                productCount = productsInLocation.size,
                                categories = categoryNames,
                                description = ""
                            )
                        }

                        // Add stored locations with zero products
                        val cardsFromStorage = storedLocations
                            .filter { stored -> productCards.none { it.name == stored } }
                            .map { storedName ->
                                WarehouseLocationCard(
                                    name = storedName,
                                    productCount = 0,
                                    categories = "",
                                    description = ""
                                )
                            }

                        val locationCards = (productCards + cardsFromStorage).sortedBy { it.name }

                        locationsAdapter.submitList(locationCards)
                        updateEmptyState(locationCards.isEmpty())

                        // Check for low stock alerts
                        checkLowStockAlerts(categories, inStockProducts)
                    }
                }
            }
        }
    }

    private fun checkLowStockAlerts(categories: List<com.example.inventoryapp.data.local.entities.CategoryEntity>, productsInLocation: List<com.example.inventoryapp.data.local.entities.ProductEntity>) {
        categories.forEach { category ->
            val productCount = productsInLocation.count { it.categoryId == category.id }
            if (productCount < 5 && productCount > 0) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("⚠️ Niski stan magazynowy")
                    .setMessage("Kategoria '${category.name}' ma tylko $productCount szt. Rekomendujemy zamówienie nowych egzemplarzy.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }
    }

    private fun showAddLocationDialog() {
        val action = WarehouseFragmentDirections.actionWarehouseToAddLocation(null)
        findNavController().navigate(action)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.locationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.locationsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
