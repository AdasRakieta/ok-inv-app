package com.example.inventoryapp.ui.boxes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentEditBoxBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Fragment for editing box details.
 */
class EditBoxFragment : Fragment() {

    private var _binding: FragmentEditBoxBinding? = null
    private val binding get() = _binding!!

    private val args: EditBoxFragmentArgs by navArgs()

    private val viewModel: EditBoxViewModel by viewModels {
        EditBoxViewModelFactory(
            (requireActivity().application as InventoryApplication).boxRepository,
            (requireActivity().application as InventoryApplication).productRepository,
            args.boxId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBoxBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputListeners()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupInputListeners() {
        binding.boxNameInput.doAfterTextChanged { text ->
            viewModel.setBoxName(text?.toString() ?: "")
        }

        binding.boxDescriptionInput.doAfterTextChanged { text ->
            viewModel.setBoxDescription(text?.toString() ?: "")
        }

        binding.warehouseLocationInput.doAfterTextChanged { text ->
            viewModel.setWarehouseLocation(text?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.saveFab.setOnClickListener {
            viewModel.saveBox()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.box.collect { box ->
                box?.let {
                    // Set initial values
                    if (binding.boxNameInput.text.isNullOrEmpty()) {
                        binding.boxNameInput.setText(it.name)
                    }
                    if (binding.boxDescriptionInput.text.isNullOrEmpty()) {
                        binding.boxDescriptionInput.setText(it.description ?: "")
                    }
                    if (binding.warehouseLocationInput.text.isNullOrEmpty()) {
                        binding.warehouseLocationInput.setText(it.warehouseLocation ?: "")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.boxSaved.collect { saved ->
                if (saved) {
                    Toast.makeText(
                        requireContext(),
                        "Box updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
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
