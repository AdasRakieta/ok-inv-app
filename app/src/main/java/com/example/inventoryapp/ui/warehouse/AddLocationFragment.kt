package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentAddLocationBinding
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AddLocationFragment : Fragment() {

    private var _binding: FragmentAddLocationBinding? = null
    private val binding get() = _binding!!

    private val args: AddLocationFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private var isEditMode = false
    private var currentLocationName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.saveButton.setOnClickListener {
            saveLocation()
        }

        loadLocationIfEditing()
    }

    private fun loadLocationIfEditing() {
        val locationName = args.locationName
        if (!locationName.isNullOrEmpty()) {
            isEditMode = true
            currentLocationName = locationName
            
            binding.titleText.text = "Edytuj lokalizację"
            binding.saveButton.text = "Zapisz zmiany"
            
            val shelf = locationName.substringBefore("/").trim()
            val bin = locationName.substringAfter("/", "").trim()
            
            binding.shelfInput.setText(shelf)
            binding.binInput.setText(bin)
        }
    }
    
    private fun saveLocation() {
        val shelf = binding.shelfInput.text.toString().trim()
        val bin = binding.binInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        
        if (shelf.isEmpty()) {
            binding.shelfLayout.error = "Półka jest wymagana"
            return
        }
        
        val newLocationName = shelf + (if (bin.isNotEmpty()) " / $bin" else "")
        
        if (isEditMode) {
            updateLocation(currentLocationName!!, shelf, bin, description)
        } else {
            createLocation(newLocationName)
        }
    }
    
    private fun createLocation(locationName: String) {
        locationStorage.addLocation(locationName)
        // Navigate to location details - lokalizacja zostanie "utworzona" gdy przypiszemy do niej pierwszy produkt
        val action = AddLocationFragmentDirections.actionAddLocationToLocationDetails(locationName)
        findNavController().navigate(action)
        
        Toast.makeText(
            requireContext(),
            "Lokalizacja $locationName utworzona. Przypisz produkty aby była widoczna na liście.",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun updateLocation(oldLocationName: String, newShelf: String, newBin: String, description: String) {
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
                val newLocationName = newShelf + (if (newBin.isNotEmpty()) " / $newBin" else "")
                locationStorage.renameLocation(oldLocationName, newLocationName)
                
                Toast.makeText(
                    requireContext(),
                    "Zaktualizowano lokalizację dla ${productsToUpdate.size} produktów",
                    Toast.LENGTH_SHORT
                ).show()
                
                findNavController().navigateUp()
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
