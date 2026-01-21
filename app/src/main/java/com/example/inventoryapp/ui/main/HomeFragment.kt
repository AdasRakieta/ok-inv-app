package com.example.inventoryapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.BuildConfig
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }

    private var cachedCategoryCounts: List<CategoryCount> = emptyList()

    private data class CategoryCount(val category: CategoryEntity, val count: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAppVersion()
        setupClickListeners()
        loadWarehouseStats()
    }

    private fun setupAppVersion() {
        binding.appVersionText.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
    }

    private fun loadWarehouseStats() {
        viewLifecycleOwner.lifecycleScope.launch {
            productRepository.getAllProducts().collect { products ->
                val inStockProducts = products.filter { it.status == ProductStatus.IN_STOCK }
                val categoryCountsMap = inStockProducts.groupingBy { it.categoryId }.eachCount()
                val categories = categoryRepository.getAllCategories().first()

                cachedCategoryCounts = categories
                    .map { cat -> CategoryCount(cat, categoryCountsMap[cat.id] ?: 0) }
                    .sortedBy { it.count }
            }
        }
    }

    private fun setupClickListeners() {
        // Products module - navigate to products list
        binding.productsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_products)
        }
        
        // Employees module - navigate to employees list
        binding.employeesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_employees)
        }

        // Warehouse module - navigate to warehouse
        binding.warehouseCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_warehouse)
        }

        binding.warehouseInfoButton.setOnClickListener {
            showCategoriesDialog()
        }
    }

    private fun showCategoriesDialog() {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }

        if (cachedCategoryCounts.isEmpty()) {
            val placeholder = TextView(context).apply {
                text = "Brak danych magazynowych"
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
            container.addView(placeholder)
        } else {
            cachedCategoryCounts.forEach { item ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 6, 0, 6)
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val nameView = TextView(context).apply {
                    text = "${getCategoryEmoji(item.category.name)} ${item.category.name}"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val countView = TextView(context).apply {
                    text = "${item.count} szt."
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                }

                val warningView = TextView(context).apply {
                    val (icon, colorRes) = when {
                        item.count <= 2 -> "❗" to android.R.color.holo_red_dark
                        item.count <= 5 -> "❗" to android.R.color.holo_orange_dark
                        else -> "" to android.R.color.transparent
                    }
                    text = icon
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, colorRes))
                    setPadding(12, 0, 0, 0)
                }

                row.addView(nameView)
                row.addView(countView)
                row.addView(warningView)

                container.addView(row)
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Kategorie w magazynie")
            .setView(container)
            .setPositiveButton("Zamknij", null)
            .show()
    }

    private fun getCategoryEmoji(name: String?): String {
        return when (name?.lowercase()?.trim()) {
            "laptop", "laptopy" -> "💻"
            "tablet", "tablety" -> "📱"
            "telefon", "telefony" -> "📱"
            "drukarka", "drukarki" -> "🖨️"
            "monitor", "monitory" -> "🖥️"
            "akcesoria", "accessories" -> "🎒"
            else -> "📦"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
