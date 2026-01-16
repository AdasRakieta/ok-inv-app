package com.example.inventoryapp.ui.products

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.ui.products.adapters.DeviceMovementAdapter
import com.example.inventoryapp.utils.CategoryHelper
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ProductDetailsViewModel
    private val args: ProductDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Prefer using application-level repositories (InventoryApplication)
        val app = requireActivity().application as InventoryApplication
        val productRepo = app.productRepository
        val deviceMovementRepo = app.deviceMovementRepository
        val boxRepo = app.boxRepository
        val packageRepo = app.packageRepository
        val factory = ProductDetailsViewModelFactory(productRepo, deviceMovementRepo, boxRepo, packageRepo, args.productId)
        val vm: ProductDetailsViewModel by viewModels { factory }
        viewModel = vm
    }

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

        // Setup movements list
        setupMovementsList()
        observeProduct()
        observeSnUpdateError()
        setupClickListeners()
    }

    private lateinit var movementAdapter: DeviceMovementAdapter

    private fun setupMovementsList() {
        movementAdapter = DeviceMovementAdapter()
        binding.movementsRecyclerView.apply {
            adapter = movementAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Allow nested scrolling inside ScrollView
            isNestedScrollingEnabled = false
        }

        // Click once: scroll to movement card on summary tap when data exists
        binding.movementSummaryText.setOnClickListener {
            if (movementAdapter.itemCount > 0) {
                try {
                    binding.root.smoothScrollTo(0, binding.movementsRecyclerView.top)
                } catch (e: Exception) {
                    binding.movementsRecyclerView.post { binding.movementsRecyclerView.smoothScrollToPosition(0) }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movements.collect { list ->
                movementAdapter.submitList(list)
                // Show empty state when there are no movements to display
                binding.movementsEmptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE

                // Update summary row in Information card: show count or 'See below'
                if (list.isEmpty()) {
                    binding.movementSummaryText.text = "No movements"
                    binding.movementSummaryText.isClickable = false
                } else {
                    binding.movementSummaryText.text = "See below"
                    binding.movementSummaryText.isClickable = true
                }
            }
        }
    }

    private fun observeProduct() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.product.collect { product ->
                product?.let {
                    // Update product name, icon and category
                    binding.productIconText.text = CategoryHelper.getCategoryIcon(it.categoryId)
                    binding.productNameText.text = it.name
                    binding.productCategoryText.text = getCategoryName(it.categoryId)
                    
                    // Update serial number section (always present now)
                    binding.serialNumberAssignedLayout.visibility = View.VISIBLE
                    binding.serialNumberNotAssignedLayout.visibility = View.GONE
                    binding.serialNumberText.text = it.serialNumber ?: "N/A"
                    binding.editSerialButton.text = "Edit"
                    
                    // Show quantity section only for "Other" category (SN is empty or default)
                    val isOtherCategory = it.serialNumber.isNullOrEmpty() || it.serialNumber == "N/A"
                    if (isOtherCategory) {
                        binding.quantitySectionLabel.visibility = View.VISIBLE
                        binding.quantityCard.visibility = View.VISIBLE
                        binding.quantityText.text = it.quantity.toString()
                        
                        // Hide serial number section for Other
                        binding.serialNumberAssignedLayout.visibility = View.GONE
                        binding.serialNumberNotAssignedLayout.visibility = View.GONE
                    } else {
                        binding.quantitySectionLabel.visibility = View.GONE
                        binding.quantityCard.visibility = View.GONE
                    }
                    
                    // Update timestamps
                    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    binding.createdAtText.text = dateFormat.format(Date(it.createdAt))
                    binding.updatedAtText.text = dateFormat.format(Date(it.updatedAt))
                }
            }
        }
    }

    private fun observeSnUpdateError() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.snUpdateError.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                    viewModel.clearSnError()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.scanSerialButton.setOnClickListener {
            // TODO: Navigate to scanner with product ID
            Toast.makeText(requireContext(), "Scanner integration coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.editSerialButton.setOnClickListener {
            showEditSerialNumberDialog()
        }
        
        binding.editProductButton.setOnClickListener {
            val currentProduct = viewModel.product.value ?: return@setOnClickListener
            val action = ProductDetailsFragmentDirections.actionProductDetailsToEditProduct(currentProduct.id)
            findNavController().navigate(action)
        }
        
        binding.deleteProductButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        // Quantity controls
        binding.increaseQuantityButton.setOnClickListener {
            val currentProduct = viewModel.product.value ?: return@setOnClickListener
            val newQuantity = currentProduct.quantity + 1
            viewModel.updateQuantity(newQuantity)
            Toast.makeText(requireContext(), "Quantity increased to $newQuantity", Toast.LENGTH_SHORT).show()
        }
        
        binding.decreaseQuantityButton.setOnClickListener {
            val currentProduct = viewModel.product.value ?: return@setOnClickListener
            if (currentProduct.quantity > 1) {
                val newQuantity = currentProduct.quantity - 1
                viewModel.updateQuantity(newQuantity)
                Toast.makeText(requireContext(), "Quantity decreased to $newQuantity", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Quantity cannot be less than 1", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditSerialNumberDialog() {
        val currentProduct = viewModel.product.value ?: return
        
        val editText = EditText(requireContext()).apply {
            setText(currentProduct.serialNumber)
            hint = "Enter serial number"
            setSingleLine(true)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Serial Number")
            .setMessage("Enter or edit the serial number for this product")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newSerialNumber = editText.text.toString().trim()
                if (newSerialNumber.isNotEmpty()) {
                    viewModel.updateSerialNumber(newSerialNumber)
                } else {
                    Toast.makeText(requireContext(), "Serial number cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        val currentProduct = viewModel.product.value ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete \"${currentProduct.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteProduct()
                Toast.makeText(requireContext(), "Product deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCategoryName(categoryId: Long?): String {
        return CategoryHelper.getCategoryName(categoryId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
