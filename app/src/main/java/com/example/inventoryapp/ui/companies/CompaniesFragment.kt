package com.example.inventoryapp.ui.companies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentCompaniesBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CompaniesFragment : Fragment() {

    private var _binding: FragmentCompaniesBinding? = null
    private val binding get() = _binding!!

    private val companyRepository by lazy {
        (requireActivity().application as InventoryApplication).companyRepository
    }

    private lateinit var adapter: CompaniesAdapter
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompaniesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupActions()
        loadCompanies(null)
    }

    private fun setupRecyclerView() {
        adapter = CompaniesAdapter { company ->
            val action = CompaniesFragmentDirections.actionCompaniesToAddEditCompany(company.id)
            findNavController().navigate(action)
        }
        binding.companiesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.companiesRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { text ->
            val query = text?.toString()?.trim().orEmpty()
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(250)
                loadCompanies(query.ifBlank { null })
            }
        }
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.addCompanyFab.setOnClickListener {
            val action = CompaniesFragmentDirections.actionCompaniesToAddEditCompany(0L)
            findNavController().navigate(action)
        }
        binding.emptyAddButton.setOnClickListener {
            val action = CompaniesFragmentDirections.actionCompaniesToAddEditCompany(0L)
            findNavController().navigate(action)
        }
    }

    private fun loadCompanies(query: String?) {
        setLoadingState()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val companies = companyRepository.searchCompanies(query)
                adapter.submitList(companies)
                binding.companiesCountBadge.text = resources.getQuantityString(
                    R.plurals.companies_count,
                    companies.size,
                    companies.size
                )
                binding.errorState.isVisible = false
                binding.emptyState.isVisible = companies.isEmpty()
                binding.companiesRecyclerView.isVisible = companies.isNotEmpty()
            } catch (e: Exception) {
                binding.companiesRecyclerView.isVisible = false
                binding.emptyState.isVisible = false
                binding.errorState.isVisible = true
            } finally {
                binding.loadingState.isVisible = false
            }
        }
    }

    private fun setLoadingState() {
        binding.loadingState.isVisible = true
        binding.errorState.isVisible = false
        binding.emptyState.isVisible = false
        binding.companiesRecyclerView.isVisible = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}

