package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.inventoryapp.data.local.entities.ProductEntity
import androidx.appcompat.app.AlertDialog

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

    private var currentProduct: ProductEntity? = null
    private var categories: List<CategoryEntity> = emptyList()
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

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
        
        observeCategories()
        loadProductDetails()
        setupActions()
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                categoryRepository.getAllCategories().collect { list ->
                    categories = list
                    updateCategoryLabel()
                }
            }
        }
    }
    
    private fun loadProductDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getProductById(args.productId).collect { product ->
                    currentProduct = product
                    product?.let {
                        binding.apply {
                            productNameText.text = it.name
                            productCategoryText.text = categoryNameFor(it.categoryId)
                            productIconText.text = categoryIconFor(it.categoryId)
                            manufacturerValue.text = it.manufacturer ?: "-"
                            modelValue.text = it.model ?: "-"
                            descriptionValue.text = it.description ?: "-"
                            locationValue.text = it.shelf ?: "-"

                            createdAtText.text = formatDate(it.createdAt)
                            updatedAtText.text = formatDate(it.updatedAt)

                            // Serial number visibility
                            if (it.serialNumber.isNotBlank()) {
                                serialNumberAssignedLayout.visibility = View.VISIBLE
                                serialNumberNotAssignedLayout.visibility = View.GONE
                                serialNumberText.text = it.serialNumber
                            } else {
                                serialNumberAssignedLayout.visibility = View.GONE
                                serialNumberNotAssignedLayout.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryLabel() {
        currentProduct?.let {
            binding.productCategoryText.text = categoryNameFor(it.categoryId)
            binding.productIconText.text = categoryIconFor(it.categoryId)
        }
    }

    private fun setupActions() {
        binding.editSerialButton.setOnClickListener { promptEditSerial() }
        binding.editProductButton.setOnClickListener { navigateToEditForm() }
        binding.deleteProductButton.setOnClickListener { confirmDeleteProduct() }
    }

    private fun promptEditSerial() {
        val product = currentProduct ?: return
        val input = EditText(requireContext()).apply {
            setText(product.serialNumber)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edytuj numer seryjny")
            .setView(input)
            .setPositiveButton("Zapisz") { _, _ ->
                val newSerial = input.text.toString().trim()
                if (newSerial.isEmpty()) {
                    toast("Numer seryjny nie może być pusty")
                } else if (newSerial == product.serialNumber) {
                    toast("Numer seryjny bez zmian")
                } else {
                    saveProduct(product.copy(serialNumber = newSerial, updatedAt = System.currentTimeMillis()))
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun navigateToEditForm() {
        val product = currentProduct ?: return
        val action = ProductDetailsFragmentDirections.actionProductDetailsToAdd(product.id)
        findNavController().navigate(action)
    }

    private fun confirmDeleteProduct() {
        val product = currentProduct ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Usuń produkt")
            .setMessage("Czy na pewno chcesz usunąć ten produkt?")
            .setPositiveButton("Usuń") { _, _ ->
                lifecycleScope.launch {
                    productRepository.deleteProductById(product.id)
                    toast("Produkt usunięty")
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun saveProduct(updated: ProductEntity) {
        lifecycleScope.launch {
            try {
                productRepository.updateProduct(updated)
                toast("Zapisano zmiany")
            } catch (e: Exception) {
                toast("Błąd: ${e.message}")
            }
        }
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp == null) "-" else dateFormat.format(Date(timestamp))
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun categoryNameFor(id: Long?): String {
        if (id == null) return "-"
        return categories.firstOrNull { it.id == id }?.name ?: "-"
    }
    
    private fun categoryIconFor(id: Long?): String {
        if (id == null) return "📦"
        return categories.firstOrNull { it.id == id }?.icon ?: "📦"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
