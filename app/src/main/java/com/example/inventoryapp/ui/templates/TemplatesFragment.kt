package com.example.inventoryapp.ui.templates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentTemplatesBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TemplatesFragment : Fragment() {

    private var _binding: FragmentTemplatesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: TemplatesViewModel
    private lateinit var adapter: TemplatesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTemplatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupFab()
        observeTemplates()
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ProductTemplateRepository(database.productTemplateDao())
        viewModel = TemplatesViewModel(repository)
    }

    private fun setupRecyclerView() {
        adapter = TemplatesAdapter(
            onTemplateClick = { template ->
                navigateToTemplateDetails(template)
            },
            onTemplateLongClick = { template ->
                showDeleteConfirmation(template)
            }
        )
        
        binding.templatesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.templatesRecycler.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTemplate.setOnClickListener {
            showAddTemplateDialog()
        }
    }

    private fun observeTemplates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.templates.collect { templates ->
                adapter.submitList(templates)
            }
        }
    }

    private fun showAddTemplateDialog() {
        val dialog = TemplateDialogFragment.newInstance()
        dialog.setOnSaveListener { name, categoryId, description ->
            viewModel.addTemplate(name, categoryId, description)
        }
        dialog.show(childFragmentManager, "AddTemplateDialog")
    }

    private fun navigateToTemplateDetails(template: com.example.inventoryapp.data.local.entities.ProductTemplateEntity) {
        val action = TemplatesFragmentDirections
            .actionTemplatesToTemplateDetails(template.id)
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmation(template: com.example.inventoryapp.data.local.entities.ProductTemplateEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.template_delete_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteTemplate(template)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
