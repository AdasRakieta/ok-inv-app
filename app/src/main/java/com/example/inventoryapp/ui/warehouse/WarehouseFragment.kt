package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Typeface
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentWarehouseBinding
import com.example.inventoryapp.databinding.BottomSheetDeleteLocationConfirmBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.inventoryapp.utils.MovementHistoryUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WarehouseFragment : Fragment() {

    private var _binding: FragmentWarehouseBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private lateinit var locationsAdapter: WarehouseLocationsListAdapter

    private data class LowStockItem(val category: CategoryEntity, val count: Int)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupActions()
        loadWarehouseData()
    }

    private fun setupRecyclerView() {
        locationsAdapter = WarehouseLocationsListAdapter(
            onLocationClick = { location ->
                val action = WarehouseFragmentDirections.actionWarehouseToLocationDetails(location.name)
                findNavController().navigate(action)
            },
            onLocationLongClick = { location ->
                if (!locationsAdapter.selectionMode) {
                    locationsAdapter.selectionMode = true
                    locationsAdapter.toggleSelection(location.name)
                    updateSelectionPanel()
                    true
                } else {
                    false
                }
            },
            onSelectionChanged = {
                updateSelectionPanel()
            }
        )

        binding.locationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = locationsAdapter
        }
    }

    private fun setupActions() {
        binding.addLocationFab.setOnClickListener {
            showAddLocationDialog()
        }

        binding.selectAllButton.setOnClickListener {
            locationsAdapter.selectAll()
            updateSelectionPanel()
        }

        binding.deleteSelectedButton.setOnClickListener {
            showDeleteSelectedConfirmation()
        }
    }

    private fun loadWarehouseData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { products ->
                    val inStockProducts = products.filter { it.status == ProductStatus.IN_STOCK }

                    // Group by location from products
                    val locationGroups = inStockProducts.groupBy { product ->
                        val shelf = product.shelf ?: "Magazyn"
                        val bin = product.bin ?: ""
                        shelf + (if (bin.isNotBlank()) " / $bin" else "")
                    }
                    val storedLocations = locationStorage.getLocations()

                    categoryRepository.getAllCategories().collect { categories ->
                        // Build location cards from product groups
                        val productCards = locationGroups.map { (locationName, productsInLocation) ->
                            val categoryIds = productsInLocation.map { it.categoryId }.distinct()
                            val categoryEmojis = categories.filter { it.id in categoryIds }
                                .mapNotNull { it.icon ?: getCategoryEmoji(it.name) }
                                .distinct()
                                .joinToString(" ")

                            WarehouseLocationCard(
                                name = locationName,
                                productCount = productsInLocation.size,
                                   categories = categoryEmojis,
                                   description = locationStorage.getLocationDescription(locationName)
                            )
                        }

                        // Add stored locations with zero products
                        val cardsFromStorage = storedLocations
                            .filter { stored -> productCards.none { it.name == stored } }
                            .map { storedName ->
                                WarehouseLocationCard(
                                    name = storedName,
                                    productCount = 0,
                                    categories = "",
                                       description = locationStorage.getLocationDescription(storedName)
                                )
                            }

                        val locationCards = (productCards + cardsFromStorage).sortedBy { it.name }

                        locationsAdapter.submitList(locationCards)
                        updateEmptyState(locationCards.isEmpty())

                        if (locationsAdapter.selectionMode) {
                            updateSelectionPanel()
                        }

                        // Check for low stock alerts
                        checkLowStockAlerts(categories, inStockProducts)
                    }
                }
            }
        }
    }

    private fun checkLowStockAlerts(categories: List<CategoryEntity>, productsInLocation: List<ProductEntity>) {
        val lowStockItems = categories
            .map { category ->
                val productCount = productsInLocation.count { it.categoryId == category.id }
                LowStockItem(category, productCount)
            }
            .filter { it.count in 1..4 }
            .sortedBy { it.count }

        if (lowStockItems.isEmpty()) return

        val dialogView = layoutInflater.inflate(R.layout.bottom_sheet_low_stock_alerts, null)
        val totalText = dialogView.findViewById<TextView>(R.id.lowStockTotalText)
        val container = dialogView.findViewById<LinearLayout>(R.id.lowStockContainer)

        totalText.text = lowStockItems.size.toString()
        container.removeAllViews()

        lowStockItems.forEach { item ->
            container.addView(createLowStockCard(requireContext(), item))
        }

        BottomSheetDialog(requireContext()).apply {
            setContentView(dialogView)
            show()
        }
    }

    private fun updateSelectionPanel() {
        val selectedCount = locationsAdapter.getSelectedCount()
        binding.selectionPanel.isVisible = locationsAdapter.selectionMode
        binding.selectionCountText.text = "Zaznaczono: $selectedCount"

        if (selectedCount == 0 && locationsAdapter.selectionMode) {
            locationsAdapter.clearSelection()
            binding.selectionPanel.isVisible = false
        }
    }

    private fun showDeleteSelectedConfirmation() {
        val selectedNames = locationsAdapter.getSelectedNames()
        val selectedCount = selectedNames.size
        if (selectedCount == 0) return

        lifecycleScope.launch {
            val allProducts = productRepository.getAllProducts().firstOrNull() ?: emptyList()
            val selectedSet = selectedNames.toSet()
            val productsToUnassign = allProducts.filter { product ->
                val shelf = product.shelf ?: "Magazyn"
                val bin = product.bin ?: ""
                val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                productLocation in selectedSet
            }

            val bottomSheet = BottomSheetDialog(requireContext())
            val sheetBinding = BottomSheetDeleteLocationConfirmBinding.inflate(layoutInflater)

            sheetBinding.titleText.text = "Usuń lokalizacje"
            sheetBinding.locationNameText.text = "$selectedCount ${pluralForm(selectedCount, "lokalizację", "lokalizacje", "lokalizacji")}" 

            sheetBinding.warningTitleText.text = "Czy na pewno chcesz usunąć wybrane lokalizacje?"
            sheetBinding.warningDetailsText.text = buildString {
                append("• Usuniętych lokalizacji: $selectedCount\n")
                if (productsToUnassign.isNotEmpty()) {
                    append("• Odpiętych produktów: ${productsToUnassign.size}\n")
                }
                append("• Tej operacji nie można cofnąć")
            }

            sheetBinding.deleteButton.text = "Usuń lokalizacje"
            sheetBinding.cancelButton.setOnClickListener { bottomSheet.dismiss() }
            sheetBinding.deleteButton.setOnClickListener {
                bottomSheet.dismiss()
                deleteSelectedLocations(selectedNames)
            }

            bottomSheet.setContentView(sheetBinding.root)
            bottomSheet.show()
        }
    }

    private fun deleteSelectedLocations(locationNames: List<String>) {
        lifecycleScope.launch {
            try {
                val selectedSet = locationNames.toSet()
                val allProducts = productRepository.getAllProducts().firstOrNull() ?: emptyList()

                allProducts.forEach { product ->
                    val shelf = product.shelf ?: "Magazyn"
                    val bin = product.bin ?: ""
                    val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")

                    if (productLocation in selectedSet) {
                        val updatedProduct = product.copy(
                            shelf = null,
                            bin = null,
                            status = ProductStatus.UNASSIGNED,
                            updatedAt = System.currentTimeMillis()
                        )
                        productRepository.updateWithHistory(
                            updatedProduct,
                            MovementHistoryUtils.entryUnassigned()
                        )
                    }
                }

                locationNames.forEach { locationStorage.removeLocation(it) }

                val updatedList = locationsAdapter.currentList.filter { it.name !in selectedSet }
                locationsAdapter.submitList(updatedList)

                locationsAdapter.clearSelection()
                Toast.makeText(requireContext(), "Usunięto lokalizacje", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun pluralForm(count: Int, singular: String, few: String, many: String): String {
        return when {
            count == 1 -> singular
            count % 10 in 2..4 && count % 100 !in 12..14 -> few
            else -> many
        }
    }

    private fun createLowStockCard(context: android.content.Context, item: LowStockItem): View {
        val card = com.google.android.material.card.MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
            radius = 12f * context.resources.displayMetrics.density
            cardElevation = 0f
            setContentPadding(16, 16, 16, 16)
        }

        val (bgColor, strokeColor, textColor, warningIcon) = when {
            item.count <= 2 -> Quadruple("#FEE2E2", "#FCA5A5", "#991B1B", "❗")
            else -> Quadruple("#FEF3C7", "#FCD34D", "#92400E", "⚠️")
        }

        card.setCardBackgroundColor(android.graphics.Color.parseColor(bgColor))
        card.strokeColor = android.graphics.Color.parseColor(strokeColor)
        card.strokeWidth = 1

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val emojiText = TextView(context).apply {
            text = getCategoryEmoji(item.category.name)
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
        }

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameText = TextView(context).apply {
            text = item.category.name
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor(textColor))
        }

        val countText = TextView(context).apply {
            text = "${item.count} szt."
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textColor))
        }

        val warningText = TextView(context).apply {
            text = warningIcon
            textSize = 20f
        }

        textContainer.addView(nameText)
        textContainer.addView(countText)

        container.addView(emojiText)
        container.addView(textContainer)
        container.addView(warningText)

        card.addView(container)
        return card
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun getCategoryEmoji(categoryName: String): String {
        return when (categoryName.lowercase()) {
            "laptop" -> "💻"
            "drukarka" -> "🖨️"
            "monitor" -> "🖥️"
            "klawiatura" -> "⌨️"
            "mysz" -> "🖱️"
            "myszka" -> "🖱️"
            "słuchawki" -> "🎧"
            "headphones" -> "🎧"
            "router" -> "📡"
            "modem" -> "📡"
            "hub" -> "🔌"
            "kabel" -> "🔌"
            "zasilacz" -> "🔌"
            "dysk" -> "💾"
            "pamięć" -> "💾"
            "ram" -> "💾"
            "procesor" -> "🖲️"
            "cpu" -> "🖲️"
            "telefon" -> "☎️"
            "smartphone" -> "📱"
            "tablet" -> "📱"
            "inne" -> "📦"
            else -> "📦"
        }
    }

    private fun showAddLocationDialog() {
        val action = WarehouseFragmentDirections.actionWarehouseToAddLocation(null)
        findNavController().navigate(action)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.locationsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.locationsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
