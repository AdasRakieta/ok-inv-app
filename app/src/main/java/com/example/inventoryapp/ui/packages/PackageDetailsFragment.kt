package com.example.inventoryapp.ui.packages

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.databinding.FragmentPackageDetailsBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.models.AddProductResult
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.R
import com.example.inventoryapp.utils.CategoryHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackageDetailsFragment : Fragment() {

    private var _binding: FragmentPackageDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: PackageDetailsViewModel
    private lateinit var adapter: PackageProductsAdapter
    private val args: PackageDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = requireActivity().application as com.example.inventoryapp.InventoryApplication
        val packageRepository = app.packageRepository
        val productRepository = app.productRepository
        val contractorRepository = com.example.inventoryapp.data.repository.ContractorRepository(
            AppDatabase.getDatabase(requireContext()).contractorDao()
        )
        val factory = PackageDetailsViewModelFactory(packageRepository, productRepository, contractorRepository, args.packageId)
        val vm: PackageDetailsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPackageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observePackage()
        observeProducts()
        observeContractor()
        observeAddProductResult()
    }

    private fun setupRecyclerView() {
        adapter = PackageProductsAdapter { product ->
            showRemoveProductDialog(product)
        }
        
        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PackageDetailsFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.modifyProductsButton.setOnClickListener {
            // Navigate to modify products (mass delete)
            val action = PackageDetailsFragmentDirections
                .actionPackageDetailsToModifyPackageProducts(args.packageId)
            findNavController().navigate(action)
        }
        
        binding.addProductsButton.setOnClickListener {
            // Navigate to product selection
            val action = PackageDetailsFragmentDirections
                .actionPackageDetailsToProductSelection(args.packageId)
            findNavController().navigate(action)
        }
        
        binding.addBulkButton.setOnClickListener {
            // Navigate to bulk scan for packages
            val action = PackageDetailsFragmentDirections.actionPackageDetailsToBulkPackageScan(args.packageId)
            findNavController().navigate(action)
        }
        
        binding.editPackageButton.setOnClickListener {
            showEditPackageDialog()
        }
        
        binding.changeStatusButton.setOnClickListener {
            showChangeStatusDialog()
        }
        
        binding.archivePackageButton.setOnClickListener {
            showArchiveConfirmationDialog()
        }
        
        binding.deletePackageButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun observePackage() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.packageEntity.collect { pkg ->
                pkg?.let {
                    binding.packageNameText.text = it.name
                    binding.packageStatusText.text = it.status
                    
                    // Format dates
                    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    binding.createdAtText.text = dateFormat.format(Date(it.createdAt))
                    
                    if (it.shippedAt != null) {
                        binding.shippedAtLayout.visibility = View.VISIBLE
                        binding.shippedAtText.text = dateFormat.format(Date(it.shippedAt))
                    } else {
                        binding.shippedAtLayout.visibility = View.GONE
                    }
                    
                    if (it.returnedAt != null) {
                        binding.returnedAtLayout.visibility = View.VISIBLE
                        binding.returnedAtText.text = dateFormat.format(Date(it.returnedAt))
                    } else {
                        binding.returnedAtLayout.visibility = View.GONE
                    }

                    // If package is archived, make it read-only in the UI and show Unarchive action
                    if (it.archived) {
                        binding.modifyProductsButton.visibility = View.GONE
                        binding.addProductsButton.visibility = View.GONE
                        binding.addBulkButton.visibility = View.GONE
                        binding.editPackageButton.visibility = View.GONE
                        binding.changeStatusButton.visibility = View.GONE
                        binding.archivePackageButton.text = "Unarchive Package"
                        binding.archivePackageButton.setOnClickListener {
                            viewLifecycleOwner.lifecycleScope.launch {
                                try {
                                    viewModel.unarchivePackageBlocking()
                                    Toast.makeText(requireContext(), "Package unarchived", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(requireContext(), "Failed to unarchive: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        // Restore normal actions for non-archived package
                        binding.modifyProductsButton.visibility = View.VISIBLE
                        binding.addProductsButton.visibility = View.VISIBLE
                        binding.addBulkButton.visibility = View.VISIBLE
                        binding.editPackageButton.visibility = View.VISIBLE
                        binding.changeStatusButton.visibility = View.VISIBLE
                        binding.archivePackageButton.text = "Archive Package"
                        binding.archivePackageButton.setOnClickListener { showArchiveConfirmationDialog() }
                    }
                }
            }
        }
    }

    private fun observeProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productsInPackage.collect { products ->
                val count = products.size
                binding.productCountText.text = if (count == 1) {
                    "1 product assigned"
                } else {
                    "$count products assigned"
                }
                
                if (products.isEmpty()) {
                    binding.productsRecyclerView.visibility = View.GONE
                    binding.emptyProductsText.visibility = View.VISIBLE
                } else {
                    binding.productsRecyclerView.visibility = View.VISIBLE
                    binding.emptyProductsText.visibility = View.GONE
                    adapter.submitList(products)
                }
            }
        }
    }

    private fun observeContractor() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contractor.collect { contractor ->
                binding.packageContractorText.text = contractor?.name ?: "No contractor assigned"
            }
        }
    }

    private fun observeAddProductResult() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addProductResult.collect { result ->
                result?.let {
                    when (it) {
                        is AddProductResult.Success -> {
                            Toast.makeText(requireContext(), "Product added to package", Toast.LENGTH_SHORT).show()
                        }
                        is AddProductResult.TransferredFromBox -> {
                            Toast.makeText(requireContext(), "Product transferred from box: ${it.boxName}", Toast.LENGTH_SHORT).show()
                        }
                        is AddProductResult.TransferredFromPackage -> {
                            Toast.makeText(requireContext(), "Product transferred from package: ${it.packageName}", Toast.LENGTH_SHORT).show()
                        }
                        is AddProductResult.AlreadyInActivePackage -> {
                            // English error message as requested
                            val msg = "Device is in package: ${it.packageName} with status: ${it.status}. Package must be archived before reassignment."
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                        is AddProductResult.Error -> {
                            Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    // Reset result
                    viewModel.resetAddProductResult()
                }
            }
        }
    }

    private fun showEditPackageDialog() {
        val currentPackage = viewModel.packageEntity.value ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_package, null)
        val packageNameEdit = dialogView.findViewById<EditText>(R.id.packageNameEdit)
        val contractorSpinner = dialogView.findViewById<Spinner>(R.id.contractorSpinner)
        val shippedDateEdit = dialogView.findViewById<EditText>(R.id.shippedDateEdit)
        val returnDateEdit = dialogView.findViewById<EditText>(R.id.returnDateEdit)

        // Set current package name
        packageNameEdit.setText(currentPackage.name)
        
        // Variables to store selected dates
        var selectedShippedDate: Long? = currentPackage.shippedAt
        var selectedReturnDate: Long? = currentPackage.returnedAt
        
        // Format and display current dates
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        if (selectedShippedDate != null) {
            shippedDateEdit.setText(dateFormat.format(Date(selectedShippedDate!!)))
        }
        if (selectedReturnDate != null) {
            returnDateEdit.setText(dateFormat.format(Date(selectedReturnDate!!)))
        }
        
        // Setup date picker for shipped date
        shippedDateEdit.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            if (selectedShippedDate != null) {
                calendar.timeInMillis = selectedShippedDate!!
            }
            
            val picker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedShippedDate = calendar.timeInMillis
                    shippedDateEdit.setText(dateFormat.format(Date(selectedShippedDate!!)))
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            picker.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                selectedShippedDate = null
                shippedDateEdit.setText("")
            }
            picker.show()
        }
        
        // Setup date picker for return date
        returnDateEdit.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            if (selectedReturnDate != null) {
                calendar.timeInMillis = selectedReturnDate!!
            }
            
            val picker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    selectedReturnDate = calendar.timeInMillis
                    returnDateEdit.setText(dateFormat.format(Date(selectedReturnDate!!)))
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            picker.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Clear") { _, _ ->
                selectedReturnDate = null
                returnDateEdit.setText("")
            }
            picker.show()
        }

        // Load contractors for spinner
        val database = AppDatabase.getDatabase(requireContext())
        val contractorDao = database.contractorDao()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val contractors = contractorDao.getAllContractors().first()
                val contractorList = mutableListOf<com.example.inventoryapp.data.local.entities.ContractorEntity?>()
                contractorList.add(null) // "No contractor" option
                contractorList.addAll(contractors.map { it as? com.example.inventoryapp.data.local.entities.ContractorEntity })

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    contractorList.map { it?.name ?: "No contractor assigned" }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                contractorSpinner.adapter = adapter

                // Set current contractor selection
                val currentContractorIndex = contractorList.indexOfFirst { it?.id == currentPackage.contractorId }
                if (currentContractorIndex >= 0) {
                    contractorSpinner.setSelection(currentContractorIndex)
                }

                // Show dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Edit Package")
                    .setView(dialogView)
                    .setPositiveButton("Save") { _, _ ->
                        val newName = packageNameEdit.text.toString().trim()
                        val selectedContractorIndex = contractorSpinner.selectedItemPosition
                        val selectedContractor = if (selectedContractorIndex > 0) contractorList[selectedContractorIndex] else null

                        if (newName.isNotEmpty()) {
                            viewModel.updatePackageName(newName)
                            viewModel.updatePackageContractor(selectedContractor?.id)
                            viewModel.updatePackageDates(selectedShippedDate, selectedReturnDate)
                            Toast.makeText(requireContext(), "Package updated", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "Package name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading contractors: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveProductDialog(product: com.example.inventoryapp.data.local.entities.ProductEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Product")
            .setMessage("Remove \"${product.name}\" from this package?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeProductFromPackage(product.id)
                Toast.makeText(requireContext(), "Product removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showArchiveConfirmationDialog() {
        val currentPackage = viewModel.packageEntity.value ?: return
        
        // Only allow archiving RETURNED packages
        if (currentPackage.status != "RETURNED") {
            Toast.makeText(
                requireContext(),
                "Only returned packages can be archived",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Archive Package")
            .setMessage("Archive \"${currentPackage.name}\"? Archived packages will be moved to the Archive tab.")
            .setPositiveButton("Archive") { _, _ ->
                // Perform the archive and wait for completion before navigating back
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        viewModel.archivePackageBlocking()
                        Toast.makeText(requireContext(), "Package archived", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to archive package: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        val currentPackage = viewModel.packageEntity.value ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Package")
            .setMessage("Are you sure you want to delete \"${currentPackage.name}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deletePackage()
                Toast.makeText(requireContext(), "Package deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddNewProductDialog() {
        // Navigate to AddProductFragment with packageId to automatically assign product to this package
        val action = PackageDetailsFragmentDirections.actionPackageDetailsFragmentToAddProductFragment(
            packageId = args.packageId
        )
        findNavController().navigate(action)
    }

    private fun showChangeStatusDialog() {
        val currentPackage = viewModel.packageEntity.value ?: return
        
        val statuses = CategoryHelper.PackageStatus.PACKAGE_STATUSES
        val currentStatusIndex = statuses.indexOf(currentPackage.status)
        
        AlertDialog.Builder(requireContext())
            .setTitle("Change Package Status")
            .setSingleChoiceItems(statuses, currentStatusIndex) { dialog, which ->
                val selectedStatus = statuses[which]
                
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        viewModel.updatePackageStatus(selectedStatus)
                        Toast.makeText(requireContext(), "Status updated to $selectedStatus", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
