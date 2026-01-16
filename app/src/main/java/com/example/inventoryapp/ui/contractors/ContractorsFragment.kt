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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ContractorRepository
import com.example.inventoryapp.databinding.FragmentContractorsBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ContractorsFragment : Fragment() {

    private var _binding: FragmentContractorsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ContractorsViewModel
    private lateinit var adapter: ContractorsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(requireContext())
        val contractorRepository = ContractorRepository(database.contractorDao())
        val factory = ContractorsViewModelFactory(contractorRepository)
        val vm: ContractorsViewModel by viewModels { factory }
        viewModel = vm
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContractorsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeContractors()
    }

    private fun setupRecyclerView() {
        adapter = ContractorsAdapter(
            onContractorClick = { contractor ->
                val action = ContractorsFragmentDirections.actionContractorsToContractorDetails(contractor.id)
                findNavController().navigate(action)
            }
        )

        binding.contractorsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ContractorsFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.addContractorButton.setOnClickListener {
            showAddContractorDialog()
        }
        
        // Setup empty state button
        binding.emptyAddButton.setOnClickListener {
            showAddContractorDialog()
        }
    }

    private fun observeContractors() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allContractors.collect { contractors ->
                adapter.submitList(contractors)
                updateEmptyState(contractors.isEmpty())
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.contractorsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.contractorsRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showAddContractorDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Contractor Name"
            setSingleLine(true)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Contractor")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val name = editText.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Contractor name is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                viewModel.addContractor(
                    name = name,
                    phone = null,
                    email = null,
                    description = null
                )

                Toast.makeText(requireContext(), "Contractor added", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}