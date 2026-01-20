package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentWarehouseLocationDetailsBinding
import com.example.inventoryapp.ui.products.ProductsAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WarehouseLocationDetailsFragment : Fragment() {

    private var _binding: FragmentWarehouseLocationDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: WarehouseLocationDetailsFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private lateinit var productsAdapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseLocationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupActions()
        loadLocationDetails()
    }

    private fun setupRecyclerView() {
        productsAdapter = ProductsAdapter(
            onItemClick = { product ->
                // Navigate to product details
            },
            onItemLongClick = { product ->
                false
            },
            getCategoryName = { categoryId ->
                ""
            },
            getCategoryIcon = { categoryId ->
                "📦"
            }
        )

        binding.assignedProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
        }
    }

    private fun setupActions() {
        binding.assignProductButton.setOnClickListener {
            showAssignProductDialog()
        }

        binding.editButton.setOnClickListener {
            showEditLocationDialog()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadLocationDetails() {
        val locationName = args.locationName

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { allProducts ->
                    // Get products in this location
                    val productsInLocation = allProducts.filter { product ->
                        val shelf = product.shelf ?: "Magazyn"
                        val bin = product.bin ?: ""
                        val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                        productLocation == locationName
                    }

                    // Update header
                    binding.locationName.text = locationName
                    binding.locationShelf.text = locationName.substringBefore("/").trim()
                    binding.locationBin.text = locationName.substringAfter("/").trim().takeIf { it.isNotEmpty() } ?: "-"

                    // Get unique categories
                    categoryRepository.getAllCategories().collect { categories ->
                        val categoriesInLocation = productsInLocation.map { it.categoryId }.distinct()
                        val categoryNames = categories.filter { it.id in categoriesInLocation }.joinToString(", ") { it.name }
                        binding.locationCategories.text = "${categoriesInLocation.size}"
                        binding.locationDescription.text = categoryNames.takeIf { it.isNotEmpty() }?.let { "Kategorie: $it" } ?: ""
                        if (binding.locationDescription.text.isNotEmpty()) {
                            binding.locationDescription.visibility = View.VISIBLE
                        }
                    }

                    // Update products list
                    productsAdapter.submitList(productsInLocation)

                    // Update empty state
                    val isEmpty = productsInLocation.isEmpty()
                    binding.noProductsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.assignedProductsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

                    binding.assignedCountText.text = "${productsInLocation.size} ${pluralForm(productsInLocation.size, "produkt", "produkty", "produktów")} w tej lokalizacji"
                }
            }
        }
    }

    private fun showAssignProductDialog() {
        val dialog = AssignProductsToLocationDialogFragment(args.locationName) { selectedProducts ->
            assignProducts(selectedProducts)
        }
        dialog.show(childFragmentManager, "AssignProductsDialog")
    }
    
    private fun assignProducts(products: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                val locationName = args.locationName
                val shelf = locationName.substringBefore("/").trim()
                val bin = locationName.substringAfter("/", "").trim().takeIf { it.isNotEmpty() }
                
                products.forEach { product ->
                    val updatedProduct = product.copy(
                        shelf = shelf,
                        bin = bin,
                        status = ProductStatus.IN_STOCK,
                        updatedAt = System.currentTimeMillis()
                    )
                    productRepository.updateProduct(updatedProduct)
                }
                
                Toast.makeText(
                    requireContext(),
                    "Przypisano ${products.size} produktów do ${locationName}",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditLocationDialog() {
        val locationName = args.locationName
        val currentShelf = locationName.substringBefore("/").trim()
        val currentBin = locationName.substringAfter("/", "").trim()
        
        val shelfInput = android.widget.EditText(requireContext()).apply {
            setText(currentShelf)
            hint = "Półka (np. A1)"
            setPadding(32, 16, 32, 16)
        }
        
        val binInput = android.widget.EditText(requireContext()).apply {
            setText(currentBin)
            hint = "Bin (opcjonalnie)"
            setPadding(32, 16, 32, 16)
        }
        
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
            addView(shelfInput)
            addView(binInput)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edytuj lokalizację")
            .setMessage("Zmiana lokalizacji automatycznie zaktualizuje wszystkie produkty w tej lokalizacji.")
            .setView(layout)
            .setPositiveButton("Zapisz") { _, _ ->
                val newShelf = shelfInput.text.toString().trim()
                val newBin = binInput.text.toString().trim()
                
                if (newShelf.isNotEmpty()) {
                    updateLocation(locationName, newShelf, newBin)
                } else {
                    Toast.makeText(requireContext(), "Nazwa półki jest wymagana", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    
    private fun updateLocation(oldLocationName: String, newShelf: String, newBin: String) {
        lifecycleScope.launch {
            try {
                val allProducts = productRepository.getAllProducts().firstOrNull() ?: return@launch
                
                // Find products in old location
                val productsToUpdate = allProducts.filter { product ->
                    val shelf = product.shelf ?: "Magazyn"
                    val bin = product.bin ?: ""
                    val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                    productLocation == oldLocationName
                }
                
                // Update all products to new location
                productsToUpdate.forEach { product ->
                    val updatedProduct = product.copy(
                        shelf = newShelf,
                        bin = newBin.takeIf { it.isNotEmpty() },
                        updatedAt = System.currentTimeMillis()
                    )
                    productRepository.updateProduct(updatedProduct)
                }
                
                Toast.makeText(
                    requireContext(),
                    "Zaktualizowano lokalizację dla ${productsToUpdate.size} produktów",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Navigate back to warehouse
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        val locationName = args.locationName
        
        lifecycleScope.launch {
            val allProducts = productRepository.getAllProducts().firstOrNull() ?: return@launch
            val productsInLocation = allProducts.filter { product ->
                val shelf = product.shelf ?: "Magazyn"
                val bin = product.bin ?: ""
                val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                productLocation == locationName
            }
            
            val message = if (productsInLocation.isEmpty()) {
                "Czy na pewno chcesz usunąć lokalizację: ${locationName}?"
            } else {
                "Lokalizacja zawiera ${productsInLocation.size} produktów. Usunięcie lokalizacji usunie przypisanie tych produktów.\n\nCzy na pewno kontynuować?"
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Usuń lokalizację")
                .setMessage(message)
                .setPositiveButton("Usuń") { _, _ ->
                    deleteLocation(locationName, productsInLocation)
                }
                .setNegativeButton("Anuluj", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }
    
    private fun deleteLocation(locationName: String, productsInLocation: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                // Unassign all products from this location
                productsInLocation.forEach { product ->
                    val updatedProduct = product.copy(
                        shelf = null,
                        bin = null,
                        updatedAt = System.currentTimeMillis()
                    )
                    productRepository.updateProduct(updatedProduct)
                }
                
                Toast.makeText(
                    requireContext(),
                    "Usunięto lokalizację ${locationName}",
                    Toast.LENGTH_SHORT
                ).show()
                
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pluralForm(count: Int, singular: String, plural2_4: String, plural5: String): String {
        return when {
            count == 1 -> singular
            count in 2..4 -> plural2_4
            else -> plural5
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
