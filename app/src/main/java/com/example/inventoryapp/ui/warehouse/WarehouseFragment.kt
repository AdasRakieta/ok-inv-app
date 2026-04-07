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
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.widget.PopupMenu
import androidx.core.graphics.ColorUtils
import androidx.core.widget.doAfterTextChanged
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
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import androidx.appcompat.widget.TooltipCompat
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
    private var searchQuery: String = ""
    private var allLocationCards: List<WarehouseLocationCard> = emptyList()

    private var zonesList: List<String> = emptyList()

    private enum class SortOption { NAME_ASC, COUNT_DESC, FREE_DESC }
    private var selectedZone: String? = null
    private var sortOption: SortOption = SortOption.NAME_ASC

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

        binding.searchInput.doAfterTextChanged { text ->
            searchQuery = text?.toString()?.trim().orEmpty()
            applySearchFilter()
        }

        binding.zoneFilterButton.setOnClickListener {
            openZoneFilter()
        }

        binding.sortButton.setOnClickListener {
            openSortDialog()
        }

        binding.boxesButton.setOnClickListener {
            findNavController().navigate(com.example.inventoryapp.R.id.boxListFragment)
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
                    val repairProducts = products.filter { it.status == ProductStatus.IN_REPAIR }
                    val retiredProducts = products.filter { it.status == ProductStatus.RETIRED }
                    val lostProducts = products.filter { it.status == ProductStatus.LOST }

                    // Group by location from products
                    val locationGroups = inStockProducts.groupBy { product ->
                        val shelf = product.shelf ?: "Magazyn"
                        val bin = product.bin ?: ""
                        shelf + (if (bin.isNotBlank()) " / $bin" else "")
                    }
                    val storedLocations = locationStorage.getLocations()

                    categoryRepository.getAllCategories().collect { categories ->
                        bindStatusCards(categories, repairProducts, retiredProducts, lostProducts)
                        // Build location cards from product groups
                        val productCards = locationGroups.map { (locationName, productsInLocation) ->
                            val categoryIds = productsInLocation.map { it.categoryId }.distinct()
                            val categoryEmojis = categories.filter { it.id in categoryIds }
                                .mapNotNull { it.icon ?: getCategoryEmoji(it.name) }
                                .distinct()
                                .joinToString(" ")

                            val dominantCategory = productsInLocation
                                .mapNotNull { it.categoryId }
                                .groupingBy { it }
                                .eachCount()
                                .maxByOrNull { it.value }
                                ?.key
                            val dominantColor = categories.firstOrNull { it.id == dominantCategory }?.color
                            val fallbackColor = locationStorage.getOrAssignLocationColor(locationName)
                            val colorHex = dominantColor ?: fallbackColor

                            WarehouseLocationCard(
                                name = locationName,
                                productCount = productsInLocation.size,
                                   categories = categoryEmojis,
                                   description = locationStorage.getLocationDescription(locationName),
                                   colorHex = colorHex,
                                   progressPercent = 0
                            )
                        }

                        // Add stored locations with zero products
                        val cardsFromStorage = storedLocations
                            .filter { stored -> productCards.none { it.name == stored } }
                            .map { storedName ->
                                val colorHex = locationStorage.getOrAssignLocationColor(storedName)
                                WarehouseLocationCard(
                                    name = storedName,
                                    productCount = 0,
                                    categories = "",
                                       description = locationStorage.getLocationDescription(storedName),
                                       colorHex = colorHex,
                                       progressPercent = 0
                                )
                            }

                        val allCards = (productCards + cardsFromStorage)
                        val maxCount = (allCards.maxOfOrNull { it.productCount } ?: 0).coerceAtLeast(1)
                        val locationCards = allCards
                            .map { card ->
                                card.copy(progressPercent = ((card.productCount * 100) / maxCount))
                            }
                            .sortedBy { it.name }

                        allLocationCards = locationCards

                        // populate zones spinner from location names (prefix before ' / ')
                        val zones = locationCards.map { it.name.substringBefore(" /") }.distinct().sorted()
                        val spinnerItems = listOf("Wszystkie") + zones

                        zonesList = zones
                        binding.zoneFilterButton.text = "Wszystkie"
                        selectedZone = null

                        // apply combined filters (search + zone + sort)
                        applyFilters()
                        updateFilterLabels()

                        if (locationsAdapter.selectionMode) {
                            updateSelectionPanel()
                        }

                        // Check for low stock alerts
                        checkLowStockAlerts(categories, inStockProducts)

                        updateSummary(inStockProducts.size, repairProducts.size, retiredProducts.size, lostProducts.size)
                    }
                }
            }
        }
    }

    private fun applySearchFilter() {
        applyFilters()
    }

    private fun applyFilters() {
        var filtered = filterLocations(allLocationCards, searchQuery)

        val zone = selectedZone
        if (!zone.isNullOrBlank()) {
            filtered = filtered.filter { it.name.split(" /")[0] == zone }
        }

        filtered = when (sortOption) {
            SortOption.NAME_ASC -> filtered.sortedBy { it.name }
            SortOption.COUNT_DESC -> filtered.sortedByDescending { it.productCount }
            SortOption.FREE_DESC -> filtered.sortedByDescending { 100 - it.progressPercent }
        }

        locationsAdapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun openZoneFilter() {
        val options = mutableListOf<FilterOption>()
        options.add(FilterOption("all", "Wszystkie", "", selectedZone == null))
        zonesList.forEach { z ->
            options.add(FilterOption(z, z, "", selectedZone == z))
        }

        FilterBottomSheet.show(this, "🏷️ Filtruj po strefie", options) { option ->
            selectedZone = if (option.id == "all") null else option.id
            applyFilters()
            updateFilterLabels()
        }
    }

    private fun openSortDialog() {
        val options = listOf(
            FilterOption("NAME_ASC", "Nazwa", "🔤", sortOption == SortOption.NAME_ASC),
            FilterOption("COUNT_DESC", "Ilość produktów", "📦", sortOption == SortOption.COUNT_DESC),
            FilterOption("FREE_DESC", "Wolne miejsce", "📤", sortOption == SortOption.FREE_DESC)
        )

        FilterBottomSheet.show(this, "↕️ Sortuj", options) { option ->
            sortOption = SortOption.valueOf(option.id)
            applyFilters()
            updateFilterLabels()
        }
    }

    private fun updateFilterLabels() {
        val zoneText = selectedZone ?: "Strefa"
        binding.zoneFilterButton.text = zoneText

        val sortDescription = when (sortOption) {
            SortOption.NAME_ASC -> "Nazwa"
            SortOption.COUNT_DESC -> "Ilość produktów"
            SortOption.FREE_DESC -> "Wolne miejsce"
        }

        binding.sortButton.contentDescription = "Sortowanie: $sortDescription"
        TooltipCompat.setTooltipText(binding.sortButton, sortDescription)
    }

    private fun filterLocations(cards: List<WarehouseLocationCard>, queryRaw: String): List<WarehouseLocationCard> {
        if (queryRaw.isBlank()) return cards
        val query = queryRaw.lowercase()
        return cards.filter { card ->
            card.name.lowercase().contains(query) ||
                card.description.lowercase().contains(query)
        }
    }

    private fun bindStatusCards(
        categories: List<CategoryEntity>,
        repairProducts: List<ProductEntity>,
        retiredProducts: List<ProductEntity>,
        lostProducts: List<ProductEntity>
    ) {
        val container = binding.statusContainer
        container.removeAllViews()

        val total = (repairProducts.size + retiredProducts.size + lostProducts.size).coerceAtLeast(1)

        val items = listOf(
            StatusCard("Serwis", "Sprzet w naprawie", "🛠️", "#F59E0B", repairProducts.size, total),
            StatusCard("Wycofane", "Sprzet wycofany", "🗂️", "#6B7280", retiredProducts.size, total),
            StatusCard("Zaginione", "Brak w lokalizacji", "⚠️", "#EF4444", lostProducts.size, total)
        )

        items.forEach { item ->
            val card = layoutInflater.inflate(R.layout.item_warehouse_status_card, container, false)
            val accent = card.findViewById<View>(R.id.statusAccent)
            val iconContainer = card.findViewById<View>(R.id.statusIconContainer)
            val iconText = card.findViewById<TextView>(R.id.statusIcon)
            val title = card.findViewById<TextView>(R.id.statusTitle)
            val subtitle = card.findViewById<TextView>(R.id.statusSubtitle)
            val value = card.findViewById<TextView>(R.id.statusValue)
            val progress = card.findViewById<android.widget.ProgressBar>(R.id.statusProgress)

            val color = android.graphics.Color.parseColor(item.colorHex)
            accent.setBackgroundColor(color)
            iconText.text = item.icon
            iconContainer.background?.setTint(ColorUtils.setAlphaComponent(color, 32))
            title.text = item.title
            subtitle.text = item.subtitle
            value.text = item.count.toString()
            progress.progress = if (item.total == 0) 0 else (item.count * 100 / item.total)
            progress.progressTintList = android.content.res.ColorStateList.valueOf(color)

            container.addView(card)
        }
    }

    private data class StatusCard(
        val title: String,
        val subtitle: String,
        val icon: String,
        val colorHex: String,
        val count: Int,
        val total: Int
    )

    private fun updateSummary(inStock: Int, repair: Int, retired: Int, lost: Int) {
        val total = (inStock + repair + retired + lost).coerceAtLeast(1)
        val usedPercent = ((inStock * 100) / total).coerceIn(0, 100)
        val freePercent = (100 - usedPercent).coerceIn(0, 100)

        binding.summaryCapacityValue.text = "$usedPercent%"
        binding.summaryFreeValue.text = "$freePercent%"
        binding.summaryCapacityBar.progress = usedPercent
        binding.summaryFreeBar.progress = freePercent
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
