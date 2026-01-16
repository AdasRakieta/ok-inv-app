package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val args: ProductDetailsFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pl", "PL"))

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
        
        setupButtons()
        loadProductDetails()
    }
    
    private fun setupButtons() {
        binding.editButton.setOnClickListener {
            val action = ProductDetailsFragmentDirections.actionDetailsToAddEdit(args.productId)
            findNavController().navigate(action)
        }
        
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
        
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun loadProductDetails() {
        lifecycleScope.launch {
            val product = productRepository.getProductById(args.productId) ?: run {
                findNavController().navigateUp()
                return@launch
            }
            
            binding.apply {
                productNameText.text = product.name
                serialNumberText.text = product.serialNumber
                
                // Status badge with color
                statusBadge.text = product.status.name
                statusBadge.setBackgroundColor(getStatusColor(product.status))
                
                // Category
                product.categoryId?.let { categoryId ->
                    lifecycleScope.launch {
                        val category = categoryRepository.getCategoryById(categoryId)
                        categoryText.text = category?.name ?: "N/A"
                    }
                } ?: run {
                    categoryText.text = "N/A"
                }
                
                // Manufacturer & Model
                manufacturerText.text = product.manufacturer ?: "N/A"
                modelText.text = product.model ?: "N/A"
                
                // Description
                descriptionText.text = product.description ?: "Brak opisu"
                
                // Location
                val location = buildString {
                    product.shelf?.let { append("Półka: $it") }
                    if (product.shelf != null && product.bin != null) append(" | ")
                    product.bin?.let { append("Bin: $it") }
                }
                locationText.text = location.ifEmpty { "Nie przypisano" }
                
                // Purchase info
                product.purchaseDate?.let {
                    purchaseDateText.text = dateFormat.format(Date(it))
                } ?: run {
                    purchaseDateText.text = "N/A"
                }
                
                product.purchasePrice?.let {
                    purchasePriceText.text = currencyFormat.format(it)
                } ?: run {
                    purchasePriceText.text = "N/A"
                }
                
                // Warranty
                product.warrantyExpiryDate?.let {
                    warrantyText.text = dateFormat.format(Date(it))
                    val isExpired = it < System.currentTimeMillis()
                    if (isExpired) {
                        warrantyText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                } ?: run {
                    warrantyText.text = "N/A"
                }
                
                // Condition
                conditionText.text = product.condition ?: "N/A"
                
                // Assignment
                if (product.status == ProductStatus.ASSIGNED && product.assignedToEmployeeId != null) {
                    assignmentCard.visibility = View.VISIBLE
                    lifecycleScope.launch {
                        val employee = employeeRepository.getEmployeeById(product.assignedToEmployeeId!!)
                        assignedToText.text = employee?.let { "${it.firstName} ${it.lastName}" } ?: "Unknown"
                        product.assignmentDate?.let {
                            assignmentDateText.text = "Od: ${dateFormat.format(Date(it))}"
                        }
                    }
                } else {
                    assignmentCard.visibility = View.GONE
                }
                
                // Notes
                product.notes?.let {
                    notesCard.visibility = View.VISIBLE
                    notesText.text = it
                } ?: run {
                    notesCard.visibility = View.GONE
                }
                
                // Timestamps
                createdAtText.text = "Utworzono: ${dateFormat.format(Date(product.createdAt))}"
                updatedAtText.text = "Zaktualizowano: ${dateFormat.format(Date(product.updatedAt))}"
            }
        }
    }
    
    private fun getStatusColor(status: ProductStatus): Int {
        return when (status) {
            ProductStatus.IN_STOCK -> resources.getColor(android.R.color.holo_green_light, null)
            ProductStatus.ASSIGNED -> resources.getColor(android.R.color.holo_blue_light, null)
            ProductStatus.IN_REPAIR -> resources.getColor(android.R.color.holo_orange_light, null)
            ProductStatus.RETIRED -> resources.getColor(android.R.color.darker_gray, null)
            ProductStatus.LOST -> resources.getColor(android.R.color.holo_red_dark, null)
        }
    }
    
    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń produkt")
            .setMessage("Czy na pewno chcesz usunąć ten produkt? Ta operacja jest nieodwracalna.")
            .setPositiveButton("Usuń") { _, _ ->
                deleteProduct()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    
    private fun deleteProduct() {
        lifecycleScope.launch {
            val product = productRepository.getProductById(args.productId)
            product?.let {
                productRepository.deleteProduct(it)
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
