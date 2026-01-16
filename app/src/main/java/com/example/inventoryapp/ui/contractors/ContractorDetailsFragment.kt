package com.example.inventoryapp.ui.contractors

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
import com.example.inventoryapp.databinding.FragmentContractorDetailsBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ContractorRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ContractorDetailsFragment : Fragment() {

    private var _binding: FragmentContractorDetailsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: ContractorDetailsViewModel
    private val args: ContractorDetailsFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(requireContext())
        val contractorRepository = ContractorRepository(database.contractorDao())
        val factory = ContractorDetailsViewModelFactory(contractorRepository, args.contractorId)
        val vm: ContractorDetailsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractorDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeContractor()
    }

    private fun setupClickListeners() {
        binding.editButton.setOnClickListener {
            showEditContractorDialog()
        }
        
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeContractor() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.contractor.collect { contractor ->
                contractor?.let {
                    binding.nameText.text = it.name
                    
                    // Phone
                    if (!it.phone.isNullOrBlank()) {
                        binding.phoneLayout.visibility = View.VISIBLE
                        binding.phoneText.text = it.phone
                    } else {
                        binding.phoneLayout.visibility = View.GONE
                    }
                    
                    // Email
                    if (!it.email.isNullOrBlank()) {
                        binding.emailLayout.visibility = View.VISIBLE
                        binding.emailText.text = it.email
                    } else {
                        binding.emailLayout.visibility = View.GONE
                    }
                    
                    // Description
                    if (!it.description.isNullOrBlank()) {
                        binding.descriptionLayout.visibility = View.VISIBLE
                        binding.descriptionText.text = it.description
                    } else {
                        binding.descriptionLayout.visibility = View.GONE
                    }
                    
                    // Dates
                    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    binding.createdAtText.text = "Created: ${dateFormat.format(Date(it.createdAt))}"
                    binding.updatedAtText.text = "Updated: ${dateFormat.format(Date(it.updatedAt))}"
                }
            }
        }
    }

    private fun showEditContractorDialog() {
        val contractor = viewModel.contractor.value ?: return
        
        val dialogView = layoutInflater.inflate(com.example.inventoryapp.R.layout.dialog_edit_contractor, null)
        val nameEdit = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.nameEdit)
        val phoneEdit = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.phoneEdit)
        val emailEdit = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.emailEdit)
        val descriptionEdit = dialogView.findViewById<EditText>(com.example.inventoryapp.R.id.descriptionEdit)
        
        // Pre-fill
        nameEdit.setText(contractor.name)
        phoneEdit.setText(contractor.phone ?: "")
        emailEdit.setText(contractor.email ?: "")
        descriptionEdit.setText(contractor.description ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Contractor")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameEdit.text.toString().trim()
                val phone = phoneEdit.text.toString().trim().ifBlank { null }
                val email = emailEdit.text.toString().trim().ifBlank { null }
                val description = descriptionEdit.text.toString().trim().ifBlank { null }

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedContractor = contractor.copy(
                    name = name,
                    phone = phone,
                    email = email,
                    description = description,
                    updatedAt = System.currentTimeMillis()
                )

                viewModel.updateContractor(updatedContractor)
                Toast.makeText(requireContext(), "Contractor updated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        val contractor = viewModel.contractor.value ?: return
        
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Contractor")
            .setMessage("Are you sure you want to delete '${contractor.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteContractor(contractor)
                Toast.makeText(requireContext(), "Contractor deleted", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
