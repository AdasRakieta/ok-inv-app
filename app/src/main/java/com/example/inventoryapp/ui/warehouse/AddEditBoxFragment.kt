package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.R
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.databinding.FragmentAddEditBoxBinding
import kotlinx.coroutines.launch

class AddEditBoxFragment : Fragment() {

    private var _binding: FragmentAddEditBoxBinding? = null
    private val binding get() = _binding!!

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddEditBoxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val boxId = arguments?.getLong("boxId") ?: 0L
        val warehouseLocationId = arguments?.getLong("warehouseLocationId") ?: 0L
        val locationName = arguments?.getString("locationName")

        if (!locationName.isNullOrBlank()) {
            binding.locationHint.visibility = View.VISIBLE
            binding.locationHint.text = "Lokalizacja: $locationName"
        }

        binding.cancelBoxButton.text = getString(R.string.cancel_pl)
        binding.saveBoxButton.text = getString(R.string.save_pl)

        if (boxId > 0L) {
            binding.saveBoxButton.text = getString(R.string.edit_box)
            // Load existing
            lifecycleScope.launch {
                val existing = boxRepository.getBoxByIdOnce(boxId)
                existing?.let { box ->
                    binding.nameEdit.setText(box.name)
                    binding.descriptionEdit.setText(box.description)
                }
            }
        }

        binding.saveBoxButton.setOnClickListener {
            val name = binding.nameEdit.text?.toString()?.trim() ?: ""
            val desc = binding.descriptionEdit.text?.toString()?.trim()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Nazwa kartonu jest wymagana", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    if (boxId > 0L) {
                        val updated = BoxEntity(id = boxId, name = name, description = desc, warehouseLocationId = if (warehouseLocationId > 0L) warehouseLocationId else null)
                        boxRepository.updateBox(updated)
                    } else {
                        val newBox = BoxEntity(name = name, description = desc, warehouseLocationId = if (warehouseLocationId > 0L) warehouseLocationId else null)
                        boxRepository.insertBox(newBox)
                    }

                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Błąd zapisu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.cancelBoxButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
