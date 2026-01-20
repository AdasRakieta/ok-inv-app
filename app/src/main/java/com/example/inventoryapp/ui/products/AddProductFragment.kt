package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.databinding.FragmentAddProductBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddProductFragment : Fragment() {

    private var _binding: FragmentAddProductBinding? = null
    private val binding get() = _binding!!

    private val args: AddProductFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private var categories: List<CategoryEntity> = emptyList()
    private var currentProduct: ProductEntity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveProduct()
        }

        setupCategories()
        setupStatusDropdown()
        loadProductIfEditing()
    }

    private fun setupCategories() {
        lifecycleScope.launch {
            categoryRepository.getAllCategories().collect { list ->
                categories = list
                val names = list.map { it.name }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
                binding.categoryInput.setAdapter(adapter)
                val targetCategoryId = currentProduct?.categoryId
                val prefill = names.firstOrNull { name ->
                    val match = categories.firstOrNull { it.name == name }
                    match?.id == targetCategoryId
                }
                when {
                    prefill != null -> binding.categoryInput.setText(prefill, false)
                    binding.categoryInput.text.isNullOrEmpty() && names.isNotEmpty() -> binding.categoryInput.setText(names.first(), false)
                }
            }
        }
    }
    
    private fun setupStatusDropdown() {
        val statusLabels = mapOf(
            ProductStatus.IN_STOCK to "Magazyn",
            ProductStatus.ASSIGNED to "Przypisane",
            ProductStatus.IN_REPAIR to "Serwis",
            ProductStatus.RETIRED to "Wycofane",
            ProductStatus.LOST to "Zaginione"
        )
        val statusNames = statusLabels.values.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusNames)
        binding.statusInput.setAdapter(adapter)
        
        // Set default to "Magazyn" for new products
        val currentStatus = currentProduct?.status ?: ProductStatus.IN_STOCK
        val currentLabel = statusLabels[currentStatus] ?: "Magazyn"
        binding.statusInput.setText(currentLabel, false)
    }

    private fun loadProductIfEditing() {
        val productId = args.productId
        if (productId <= 0) return
        lifecycleScope.launch {
            val product = productRepository.getProductById(productId).firstOrNull()
            if (product != null) {
                currentProduct = product
                binding.titleText.text = "Edytuj produkt"
                binding.saveButton.text = "Zapisz zmiany"
                binding.productNameInput.setText(product.name)
                binding.productIdInput.setText(product.customId ?: "")
                binding.serialNumberInput.setText(product.serialNumber)
                binding.manufacturerInput.setText(product.manufacturer ?: "")
                binding.modelInput.setText(product.model ?: "")
                binding.descriptionInput.setText(product.description ?: "")
                binding.locationInput.setText(product.shelf ?: "")

                // Set category text when categories already loaded
                val targetCategory = categories.firstOrNull { it.id == product.categoryId }
                targetCategory?.name?.let { binding.categoryInput.setText(it, false) }
                
                // Set status
                setupStatusDropdown()
            } else {
                Toast.makeText(requireContext(), "Nie znaleziono produktu", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }
    
    private fun saveProduct() {
        val name = binding.productNameInput.text.toString().trim()
        val customId = binding.productIdInput.text.toString().trim().ifEmpty { null }
        val serialNumber = binding.serialNumberInput.text.toString().trim()
        val categoryName = binding.categoryInput.text.toString().trim()
        val categoryId = categories.firstOrNull { it.name == categoryName }?.id
        val manufacturer = binding.manufacturerInput.text.toString().trim().ifEmpty { null }
        val model = binding.modelInput.text.toString().trim().ifEmpty { null }
        val description = binding.descriptionInput.text.toString().trim().ifEmpty { null }
        val location = binding.locationInput.text.toString().trim().ifEmpty { null }
        
        // Get selected status
        val statusLabel = binding.statusInput.text.toString().trim()
        val selectedStatus = when (statusLabel) {
            "Magazyn" -> ProductStatus.IN_STOCK
            "Przypisane" -> ProductStatus.ASSIGNED
            "Serwis" -> ProductStatus.IN_REPAIR
            "Wycofane" -> ProductStatus.RETIRED
            "Zaginione" -> ProductStatus.LOST
            else -> ProductStatus.IN_STOCK
        }
        
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Nazwa produktu jest wymagana", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (serialNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Numer seryjny jest wymagany", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryId == null) {
            Toast.makeText(requireContext(), "Wybierz kategorię", Toast.LENGTH_SHORT).show()
            return
        }
        
        val existing = currentProduct
        val now = System.currentTimeMillis()
        val product = if (existing == null) {
            ProductEntity(
                name = name,
                customId = customId,
                serialNumber = serialNumber,
                categoryId = categoryId,
                status = selectedStatus,
                manufacturer = manufacturer,
                model = model,
                description = description,
                shelf = location,
                createdAt = now,
                updatedAt = now
            )
        } else {
            existing.copy(
                name = name,
                customId = customId,
                serialNumber = serialNumber,
                categoryId = categoryId,
                status = selectedStatus,
                manufacturer = manufacturer,
                model = model,
                description = description,
                shelf = location,
                updatedAt = now
            )
        }
        
        lifecycleScope.launch {
            try {
                if (existing == null) {
                    productRepository.insertProduct(product)
                    Toast.makeText(requireContext(), "Produkt dodany", Toast.LENGTH_SHORT).show()
                } else {
                    productRepository.updateProduct(product)
                    Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                }
                findNavController().navigateUp()
            } catch (e: android.database.sqlite.SQLiteConstraintException) {
                // Check if it's a customId conflict
                if (e.message?.contains("customId") == true) {
                    binding.productIdInput.error = "To ID jest już w użyciu"
                } else if (e.message?.contains("serialNumber") == true) {
                    binding.serialNumberInput.error = "Ten numer seryjny już istnieje"
                } else {
                    Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
