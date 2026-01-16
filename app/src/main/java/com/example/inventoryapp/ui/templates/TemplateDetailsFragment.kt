package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentTemplateDetailsBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import com.example.inventoryapp.ui.products.ProductWithPackage
import com.example.inventoryapp.ui.products.ProductsAdapter
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TemplateDetailsFragment : Fragment() {

    private var _binding: FragmentTemplateDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: TemplateDetailsFragmentArgs by navArgs()
    private lateinit var templateRepository: ProductTemplateRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplateDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRepositories()
        setupRecyclerView()
        setupClickListeners()
        loadTemplateDetails()
        observeProducts()
    }

    private fun setupRepositories() {
        val database = AppDatabase.getDatabase(requireContext())
        templateRepository = ProductTemplateRepository(database.productTemplateDao())
        productRepository = ProductRepository(database.productDao())
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(
            onProductClick = { product ->
                // Navigate to product details if needed
            }
        )
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.addProductsBulkButton.setOnClickListener {
            navigateToBulkScan()
        }

        binding.editTemplateButton.setOnClickListener {
            // Edit template - will load template and show dialog
            viewLifecycleOwner.lifecycleScope.launch {
                templateRepository.getTemplateById(args.templateId).collect { template ->
                    template?.let {
                        val dialog = TemplateDialogFragment.newInstance(it)
                        dialog.setOnSaveListener { name, categoryId, description ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                val updatedTemplate = it.copy(
                                    name = name,
                                    categoryId = categoryId,
                                    description = description,
                                    updatedAt = System.currentTimeMillis()
                                )
                                templateRepository.updateTemplate(updatedTemplate)
                                loadTemplateDetails() // Refresh
                            }
                        }
                        dialog.show(childFragmentManager, "EditTemplateDialog")
                    }
                }
            }
        }

        binding.deleteTemplateButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadTemplateDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getTemplateById(args.templateId).collect { template ->
                template?.let {
                    binding.templateNameText.text = it.name
                    
                    // Category
                    val categoryText = if (it.categoryId != null) {
                        val category = CategoryHelper.getAllCategories()
                            .find { cat -> cat.id == it.categoryId }
                        "Category: ${category?.name ?: "Unknown"}"
                    } else {
                        "Category: None"
                    }
                    binding.templateCategoryText.text = categoryText

                    // Description
                    binding.templateDescriptionText.text = it.description ?: "No description"

                    // Date
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    binding.templateDateText.text = "Created: ${dateFormat.format(Date(it.createdAt))}"
                }
            }
        }
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Get products that match this template (by name)
            // Since we don't have templateId in ProductEntity, we'll filter by name
            templateRepository.getTemplateById(args.templateId).collect { template ->
                template?.let {
                    productRepository.getAllProducts().collect { products ->
                        val filteredProducts = products.filter { product ->
                            product.name == it.name && product.categoryId == it.categoryId
                        }.map { product ->
                            ProductWithPackage(product, null) // Template products don't have packages
                        }
                        
                        if (filteredProducts.isEmpty()) {
                            binding.emptyStateLayout.visibility = View.VISIBLE
                            binding.productsRecyclerView.visibility = View.GONE
                        } else {
                            binding.emptyStateLayout.visibility = View.GONE
                            binding.productsRecyclerView.visibility = View.VISIBLE
                            productsAdapter.submitList(filteredProducts)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToBulkScan() {
        val action = TemplateDetailsFragmentDirections
            .actionTemplateDetailsToBulkScan(args.templateId)
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.template_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                deleteTemplate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteTemplate() {
        viewLifecycleOwner.lifecycleScope.launch {
            templateRepository.getTemplateById(args.templateId).collect { template ->
                template?.let {
                    templateRepository.deleteTemplate(it)
                    // Navigate back
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
