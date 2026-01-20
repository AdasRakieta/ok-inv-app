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
import com.example.inventoryapp.ui.warehouse.LocationStorage
import com.example.inventoryapp.databinding.FragmentWarehouseLocationDetailsBinding
import com.example.inventoryapp.ui.employees.AssignedProductsAdapter
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
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private lateinit var assignedProductsAdapter: AssignedProductsAdapter

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
        assignedProductsAdapter = AssignedProductsAdapter(
            onProductClick = { product ->
                // Navigate to product details if needed
            },
            onUnassignClick = { product ->
                showUnassignConfirmation(product)
            }
        )

        binding.assignedProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedProductsAdapter
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
        val normalizedLocationName = normalizeLocationName(locationName)

        // Update header immediately
        binding.locationName.text = locationName
        binding.locationShelf.text = locationName.substringBefore("/").trim()
        binding.locationBin.text = locationName.substringAfter("/", "").trim().takeIf { it.isNotEmpty() } ?: "-"

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { allProducts ->
                    // Get products in this location - MUST match the format used in WarehouseFragment
                    val productsInLocation = allProducts.filter { product ->
                        if (product.status != ProductStatus.IN_STOCK) return@filter false
                        val shelf = product.shelf ?: "Magazyn"
                        val bin = product.bin ?: ""
                        val productLocation = normalizeLocationName(
                            shelf + (if (bin.isNotBlank()) " / $bin" else "")
                        )
                        productLocation == normalizedLocationName
                    }

                    // Get categories
                    val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val categoriesInLocation = productsInLocation.mapNotNull { it.categoryId }.distinct()
                    val categoryNames = categories.filter { it.id in categoriesInLocation }.joinToString(", ") { it.name }
                    binding.locationCategories.text = "${categoriesInLocation.size}"
                    binding.locationDescription.text = categoryNames.takeIf { it.isNotEmpty() }?.let { "Kategorie: $it" } ?: ""
                    if (binding.locationDescription.text.isNotEmpty()) {
                        binding.locationDescription.visibility = View.VISIBLE
                    }

                    // Update products list
                    assignedProductsAdapter.submitList(productsInLocation)

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

                locationStorage.addLocation(locationName)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditLocationDialog() {
        val action = WarehouseLocationDetailsFragmentDirections.actionLocationDetailsToEdit(args.locationName)
        findNavController().navigate(action)
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
                "Czy na pewno chcesz usunąć tę lokalizację?"
            } else {
                "Czy na pewno chcesz usunąć ten produkt?\n\n• Produkt zostanie trwale usunięty\n• Historii ruchu nie można cofnąć\n• Tej operacji nie można cofnąć"
            }
            
            // Create custom view for warning box
            val warningBox = android.widget.LinearLayout(requireContext()).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(48, 24, 48, 24)
                setBackgroundColor(android.graphics.Color.parseColor("#FEF2F2"))
                
                addView(android.widget.TextView(requireContext()).apply {
                    text = "⚠️"
                    textSize = 48f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 16, 0, 16)
                })
                
                addView(android.widget.TextView(requireContext()).apply {
                    text = "Usuń lokalizację"
                    textSize = 20f
                    setTextColor(android.graphics.Color.parseColor("#991B1B"))
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 16)
                })
                
                addView(android.widget.TextView(requireContext()).apply {
                    text = locationName
                    textSize = 16f
                    setTextColor(android.graphics.Color.parseColor("#374151"))
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 0, 0, 24)
                })
                
                if (productsInLocation.isNotEmpty()) {
                    addView(android.widget.TextView(requireContext()).apply {
                        text = "Czy na pewno chcesz usunąć tę lokalizację?\n\n• Przypisanie ${productsInLocation.size} produktów zostanie usunięte\n• Produkty zostaną odpięte od lokalizacji\n• Tej operacji nie można cofnąć"
                        textSize = 14f
                        setTextColor(android.graphics.Color.parseColor("#991B1B"))
                        setPadding(0, 0, 0, 0)
                    })
                }
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setView(warningBox)
                .setPositiveButton("Usuń lokalizację") { _, _ ->
                    deleteLocation(locationName, productsInLocation)
                }
                .setNegativeButton("Anuluj", null)
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
                locationStorage.removeLocation(locationName)
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showUnassignConfirmation(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń przypisanie produktu")
            .setMessage("Czy na pewno chcesz usunąć przypisanie?\n\n${product.name}\nS/N: ${product.serialNumber}")
            .setPositiveButton("Usuń przypisanie") { _, _ ->
                unassignProduct(product)
            }
            .setNegativeButton("Anuluj", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun unassignProduct(product: ProductEntity) {
        lifecycleScope.launch {
            try {
                val updatedProduct = product.copy(
                    shelf = null,
                    bin = null,
                    status = ProductStatus.ASSIGNED,
                    updatedAt = System.currentTimeMillis()
                )
                productRepository.updateProduct(updatedProduct)
                Toast.makeText(requireContext(), "Usunięto przypisanie", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizeLocationName(raw: String): String {
        return raw.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")
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
