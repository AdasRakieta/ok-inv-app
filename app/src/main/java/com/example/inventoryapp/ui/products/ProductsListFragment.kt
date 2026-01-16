package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import kotlinx.coroutines.launch

class ProductsListFragment : Fragment() {

    private var _binding: FragmentProductsListBinding? = null
    private val binding get() = _binding!!
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    
    private lateinit var adapter: ProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        loadProducts()
    }
    
    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product ->
                // TODO: Navigate to product details
            }
        )
        
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productsRecyclerView.adapter = adapter
    }
    
    private fun setupSearch() {
        // Scanner or manual search
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                performSearch(binding.searchEditText.text.toString().trim())
                true
            } else {
                false
            }
        }
        
        // Enter key handling for scanner
        binding.searchEditText.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                performSearch(binding.searchEditText.text.toString().trim())
                true
            } else {
                false
            }
        }
    }
    
    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            loadProducts()
            return
        }
        
        lifecycleScope.launch {
            productRepository.searchProducts(query).collect { products ->
                adapter.submitList(products)
                // Update stats if available
            }
        }
    }
    
    private fun loadProducts() {
        lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                adapter.submitList(products)
                // Update stats if available
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
