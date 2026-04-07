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
import com.example.inventoryapp.data.local.entities.WarehouseLocationEntity
import java.util.UUID
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
    private val warehouseLocationRepository by lazy { (requireActivity().application as InventoryApplication).warehouseLocationRepository }

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
            // Show full location string in single input
            binding.locationInput.setText(locationName)
        }
    }
    
    private fun saveLocation() {
        val locationText = binding.locationInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()

        if (locationText.isEmpty()) {
            binding.locationLayout.error = "Lokalizacja jest wymagana"
            return
        }

        val shelf = locationText.substringBefore("/").trim()
        val bin = locationText.substringAfter("/", "").trim()
        val newLocationName = shelf + (if (bin.isNotEmpty()) " / $bin" else "")

        if (isEditMode) {
            updateLocation(currentLocationName!!, shelf, bin, description)
        } else {
            createLocation(newLocationName, description)
        }
    }
    
    private fun createLocation(locationName: String, description: String = "") {
        // Add to local prefs storage
        locationStorage.addLocation(locationName, description)

        // Also persist to Room DB with generated qrUid so QR generation works immediately
        lifecycleScope.launch {
            try {
                val entity = WarehouseLocationEntity(
                    code = locationName,
                    qrUid = UUID.randomUUID().toString(),
                    name = locationName
                )
                warehouseLocationRepository.insertLocation(entity)
            } catch (e: Exception) {
                // ignore DB errors - we still proceed with UI flow
            }
        }

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
                locationStorage.updateLocationDescription(newLocationName, description)
                
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
