package com.example.inventoryapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.BuildConfig
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentHomeBinding
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppVersion()
        setupClickListeners()
        loadWarehouseStats()
    }

    private fun setupAppVersion() {
        binding.appVersionText.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun loadWarehouseStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                val inStockProducts = products.filter { it.status == ProductStatus.IN_STOCK }
                val locations = inStockProducts
                    .groupBy { (it.shelf ?: "Magazyn") + (if (it.bin?.isNotBlank() == true) " / ${it.bin}" else "") }
                    .size
                
                // Get unique categories in stock
                val categories = inStockProducts.map { it.categoryId }.distinct().size
                
                // Update UI
                binding.warehouseCountText.text = "$locations"
                binding.warehouseCategoriesText.text = "$categories kategor" + if (categories == 1) "ii" else if (categories in 2..4) "ie" else "ii"
            }
        }
    }

    private fun setupClickListeners() {
        // Products module - navigate to products list
        binding.productsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_products)
        }
        
        // Employees module - navigate to employees list
        binding.employeesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_employees)
        }

        // Warehouse module - navigate to warehouse
        binding.warehouseCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_warehouse)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
