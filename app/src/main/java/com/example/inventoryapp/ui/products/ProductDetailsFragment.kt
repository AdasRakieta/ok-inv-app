package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.CategoryRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailsFragment : Fragment() {
    
    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val args: ProductDetailsFragmentArgs by navArgs()
    
    private lateinit var productRepository: ProductRepository
    private lateinit var categoryRepository: CategoryRepository
    
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        productRepository = ProductRepository(database.productDao())
        categoryRepository = CategoryRepository(database.categoryDao())
        
        setupButtons()
        loadProductDetails()
    }
    
    private fun setupButtons() {
        binding.editProductButton.setOnClickListener {
            val action = ProductDetailsFragmentDirections.actionDetailsToAddEdit(args.productId)
            findNavController().navigate(action)
        }
        
        binding.deleteProductButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }
    
    private fun loadProductDetails() {
        lifecycleScope.launch {
            val product = productRepository.getProductById(args.productId).first()
            
            if (product == null) {
                findNavController().navigateUp()
                return@launch
            }
            
            // Basic info
            binding.productNameText.text = product.name
            
            // Category
            product.categoryId?.let { categoryId ->
                lifecycleScope.launch {
                    val category = categoryRepository.getCategoryById(categoryId).first()
                    binding.productCategoryText.text = category?.name ?: "Unknown"
                }
            } ?: run {
                binding.productCategoryText.text = "No category"
            }

            // Details
            binding.manufacturerValue.text = product.manufacturer ?: "-"
            binding.modelValue.text = product.model ?: "-"
            binding.descriptionValue.text = product.description ?: "-"
            val locationText = listOfNotNull(product.shelf, product.bin).joinToString(" / ")
            binding.locationValue.text = if (locationText.isNotEmpty()) locationText else "-"
            
            // Serial number
            if (!product.serialNumber.isNullOrEmpty()) {
                binding.serialNumberText.text = product.serialNumber
                binding.serialNumberAssignedLayout.visibility = View.VISIBLE
                binding.serialNumberNotAssignedLayout.visibility = View.GONE
            } else {
                binding.serialNumberAssignedLayout.visibility = View.GONE
                binding.serialNumberNotAssignedLayout.visibility = View.VISIBLE
            }
            
            // Timestamps
            binding.createdAtText.text = dateFormat.format(Date(product.createdAt))
            binding.updatedAtText.text = dateFormat.format(Date(product.updatedAt))
            
            // Movement summary (placeholder)
            binding.movementSummaryText.text = "No movements"
            binding.movementsEmptyText.visibility = View.VISIBLE
        }
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteProduct()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteProduct() {
        lifecycleScope.launch {
            val product = productRepository.getProductById(args.productId).first()
            product?.let {
                productRepository.deleteProduct(it)
                Toast.makeText(requireContext(), "Product deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
