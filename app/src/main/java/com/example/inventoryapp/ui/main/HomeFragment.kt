package com.example.inventoryapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.BuildConfig
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentHomeBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.PackageRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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

        setupClickListeners()
        loadStatistics()
        setupAppVersion()
    }

    private fun setupAppVersion() {
        binding.appVersionText.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun setupClickListeners() {
        // Products card - navigate to products list
        binding.productsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_products)
        }

        // Packages card - navigate to packages list
        binding.packagesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_packages)
        }

        // Templates card - navigate to product templates (catalog)
        binding.templatesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_templates)
        }

        // Export/Import card - navigate to tools screen
        binding.exportImportCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_export_import)
        }

        // Printer Settings card - navigate to printer configuration
        binding.printerSettingsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_printer_settings)
        }

        // Contractors card - navigate to contractors list
        binding.contractorsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_contractors)
        }

        // Boxes card - navigate to boxes list
        binding.boxesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_boxes)
        }
        
        // Archive card - navigate to archived packages
        binding.archiveCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_archive)
        }
        
        // Inventory Count card - navigate to inventory count sessions
        binding.inventoryCountCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_inventory_count)
        }
    }

    private fun loadStatistics() {
        val database = AppDatabase.getDatabase(requireContext())
        val productRepository = ProductRepository(database.productDao())
        val packageRepository = PackageRepository(database.packageDao(), database.productDao(), database.boxDao())
        val categoryDao = database.categoryDao()

        // Initialize default categories if none exist
        viewLifecycleOwner.lifecycleScope.launch {
            categoryDao.getAllCategories().collect { categories ->
                if (categories.isEmpty()) {
                    // Add default categories
                    val defaultCategories = listOf(
                        CategoryEntity(name = "Skaner"),
                        CategoryEntity(name = "Drukarka"),
                        CategoryEntity(name = "Stacja dokująca skanera"),
                        CategoryEntity(name = "Stacja dokująca drukarki")
                    )
                    defaultCategories.forEach { category ->
                        categoryDao.insertCategory(category)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Load product count
            productRepository.getAllProducts().collect { products ->
                binding.productsCountText.text = products.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            // Load package count
            packageRepository.getAllPackages().collect { packages ->
                binding.packagesCountText.text = packages.size.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
