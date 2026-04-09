package com.example.inventoryapp.ui.products

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.TooltipCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.databinding.FragmentProductsListBinding
import com.example.inventoryapp.databinding.BottomSheetStatsBinding
import com.example.inventoryapp.ui.components.FilterBottomSheet
import com.example.inventoryapp.ui.components.FilterOption
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.PopupMenu
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Locale
import kotlinx.coroutines.launch

class ProductsListFragment : Fragment() {

    private var _binding: FragmentProductsListBinding? = null
    private val binding get() = _binding!!
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    
    private lateinit var adapter: ProductsAdapter

    private var allProducts: List<ProductEntity> = emptyList()
    private var categories: List<CategoryEntity> = emptyList()
    private var boxesList: List<BoxEntity> = emptyList()
    private var locationMap: Map<Long, String> = emptyMap()
    private var selectedCategoryId: Long? = null
    private var selectedStatus: ProductStatus? = null
    private var sortOption: SortOption = SortOption.NEWEST
    private var searchQuery: String = ""
    private var isFabMenuOpen = false

    private enum class SortOption {
        NEWEST,
        NAME_ASC,
        NAME_DESC,
        STATUS,
        SERIAL
    }

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

        // Improve status/navigation bar readability
        val window = requireActivity().window
        window.statusBarColor = ContextCompat.getColor(requireContext(), com.example.inventoryapp.R.color.background)
        window.navigationBarColor = ContextCompat.getColor(requireContext(), com.example.inventoryapp.R.color.background)
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        
        setupRecyclerView()
        loadBoxesAndLocations()
        setupSearch()
        setupActions()
        setupFilters()
        observeCategories()
        loadProducts()
    }
    
    private fun setupRecyclerView() {
        adapter = ProductsAdapter(
            onItemClick = { product ->
                if (adapter.selectionMode) {
                    // Toggle selection in selection mode
                    adapter.toggleSelection(product.id)
                    updateSelectionPanel()
                } else {
                    // Normal click opens details
                    openDetails(product.id)
                }
            },
            onItemLongClick = { product ->
                if (!adapter.selectionMode) {
                    // Enter selection mode on long click
                    adapter.selectionMode = true
                    adapter.toggleSelection(product.id)
                    showSelectionPanel()
                }
            },
            onOptionsClick = { anchor, product ->
                showProductOptions(anchor, product)
            },
            getCategoryName = { categoryId -> categoryNameFor(categoryId) },
            getCategoryIcon = { categoryId -> categoryIconFor(categoryId) }
        )
        
        binding.productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.productsRecyclerView.adapter = adapter
    }

    private fun setupActions() {
        binding.addProductFab.setOnClickListener { 
            toggleFabMenu()
        }
        binding.emptyAddButton.setOnClickListener { 
            openAddProduct()
        }
        // Fragment-level FAB handles toggle; no global FAB override needed
        
        // Overlay closes menu
        binding.fabMenuOverlay.setOnClickListener {
            closeFabMenu()
        }
        
        // Card actions
        binding.singleAddCard.setOnClickListener {
            closeFabMenu()
            openAddProduct()
        }
        
        binding.bulkAddCard.setOnClickListener {
            closeFabMenu()
            openTemplatesList()
        }
        
        // Selection panel actions
        binding.selectAllButton.setOnClickListener {
            adapter.selectAll()
            updateSelectionPanel()
        }
        
        binding.deleteSelectedButton.setOnClickListener {
            confirmBulkDelete()
        }
    }
    
    private fun toggleFabMenu() {
        if (isFabMenuOpen) {
            closeFabMenu()
        } else {
            openFabMenu()
        }
    }
    
    private fun openFabMenu() {
        isFabMenuOpen = true

        // No global FAB to hide — using fragment-local FAB only

        // Show overlay
        binding.fabMenuOverlay.visibility = View.VISIBLE
        binding.fabMenuOverlay.alpha = 0f
        binding.fabMenuOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Rotate main FAB
        binding.addProductFab.animate()
            .rotation(45f)
            .setDuration(200)
            .start()

        // Show cards with animation
        showCard(binding.singleAddCard, 0)
        showCard(binding.bulkAddCard, 50)
    }
    
    private fun closeFabMenu() {
        isFabMenuOpen = false
        
        // Hide overlay
        _binding?.fabMenuOverlay?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.withEndAction {
                _binding?.fabMenuOverlay?.visibility = View.GONE
            }
            ?.start()
        
        // Rotate main FAB back
        _binding?.addProductFab?.animate()
            ?.rotation(0f)
            ?.setDuration(200)
            ?.start()
        
        // Hide cards
        _binding?.let {
            hideCard(it.singleAddCard)
            hideCard(it.bulkAddCard)
        }

        // No global FAB to show
    }
    
    private fun showCard(card: View, delay: Long) {
        card.visibility = View.VISIBLE
        card.alpha = 0f
        card.translationY = 20f
        card.scaleX = 0.8f
        card.scaleY = 0.8f
        
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setStartDelay(delay)
            .start()
    }
    
    private fun hideCard(card: View) {
        card.animate()
            .alpha(0f)
            .translationY(20f)
            .scaleX(0.8f)
            .scaleY(0.8f)
            .setDuration(150)
            .withEndAction {
                card.visibility = View.GONE
            }
            .start()
    }
    
    private fun showAddOptionsDialog() {
        val options = arrayOf("Dodaj jeden produkt", "Masowe dodawanie według wzoru")
        AlertDialog.Builder(requireContext())
            .setTitle("Wybierz opcję")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openAddProduct()
                    1 -> openTemplatesList()
                }
            }
            .show()
    }

    private fun setupFilters() {
        binding.statsButton.setOnClickListener {
            openStatsDialog()
        }
        binding.filterButton.setOnClickListener {
            openCategoryFilter()
        }
        binding.statusFilterButton.setOnClickListener {
            openStatusFilter()
        }
        binding.sortButton.setOnClickListener {
            openSortDialog()
        }
    }

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }
    private val warehouseLocationRepository by lazy { (requireActivity().application as InventoryApplication).warehouseLocationRepository }

    private fun loadBoxesAndLocations() {
        // collect locations and boxes and pass maps to adapter for display
        viewLifecycleOwner.lifecycleScope.launch {
            // locations
            warehouseLocationRepository.getAllLocations().collect { locations ->
                locationMap = locations.associate { it.id to (it.code ?: "") }
                adapter.setLocationsMap(locationMap)

                // recompute box-location map using latest boxesList
                val boxLoc = boxesList.associate { box -> box.id to (box.warehouseLocationId?.let { locationMap[it] } ?: "") }
                adapter.setBoxLocationMap(boxLoc)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            boxRepository.getAllBoxes().collect { boxes ->
                boxesList = boxes
                val boxesMap = boxes.associate { it.id to it.name }
                adapter.setBoxesMap(boxesMap)

                val boxLoc = boxes.associate { box -> box.id to (box.warehouseLocationId?.let { locationMap[it] } ?: "") }
                adapter.setBoxLocationMap(boxLoc)
            }
        }
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
        searchQuery = query
        applyFilters()
    }
    
    private fun loadProducts() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getAllProducts().collect { products ->
                    allProducts = products
                    applyFilters()
                }
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                categoryRepository.getAllCategories().collect { list ->
                    categories = list
                    updateFilterLabels()
                }
            }
        }
    }

    private fun applyFilters() {
        var filtered = allProducts

        selectedCategoryId?.let { id ->
            // If selected category is a parent (has children), include products in any child category.
            val isParent = categories.any { it.parentId == id }
            filtered = if (isParent) {
                val childIds = categories.filter { it.parentId == id }.map { it.id }
                // Include products that are directly assigned to the parent as well as those in any child category
                filtered.filter { it.categoryId == id || (it.categoryId != null && it.categoryId in childIds) }
            } else {
                filtered.filter { it.categoryId == id }
            }
        }

        selectedStatus?.let { status ->
            filtered = filtered.filter { it.status == status }
        }

        if (searchQuery.isNotEmpty()) {
            val query = searchQuery.lowercase(Locale.getDefault())
            filtered = filtered.filter { product ->
                product.name.lowercase(Locale.getDefault()).contains(query) ||
                        product.serialNumber.lowercase(Locale.getDefault()).contains(query)
            }
        }

        val sorted = sortProducts(filtered)
        adapter.submitList(sorted)
        updateEmptyState(sorted.isEmpty())
        updateFilterLabels()
    }

    private fun sortProducts(list: List<ProductEntity>): List<ProductEntity> {
        return when (sortOption) {
            SortOption.NEWEST -> list.sortedByDescending { it.createdAt }
            SortOption.NAME_ASC -> list.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SortOption.NAME_DESC -> list.sortedByDescending { it.name.lowercase(Locale.getDefault()) }
            SortOption.STATUS -> list.sortedWith(
                compareBy<ProductEntity> { it.status.ordinal }
                    .thenBy { it.name.lowercase(Locale.getDefault()) }
            )
            SortOption.SERIAL -> list.sortedBy { it.serialNumber.lowercase(Locale.getDefault()) }
        }
    }

    private fun openStatsDialog() {
        val total = allProducts.size
        val inStock = allProducts.count { it.status == ProductStatus.IN_STOCK }
        val assigned = allProducts.count { it.status == ProductStatus.ASSIGNED }
        val inRepair = allProducts.count { it.status == ProductStatus.IN_REPAIR }
        val retired = allProducts.count { it.status == ProductStatus.RETIRED }
        val lost = allProducts.count { it.status == ProductStatus.LOST }

        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetStatsBinding.inflate(layoutInflater)
        
        sheetBinding.totalProductsText.text = total.toString()
        sheetBinding.inStockText.text = inStock.toString()
        sheetBinding.assignedText.text = assigned.toString()
        sheetBinding.inRepairText.text = inRepair.toString()
        sheetBinding.retiredText.text = retired.toString()
        sheetBinding.lostText.text = lost.toString()
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun openCategoryFilter() {
        if (categories.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setMessage("Brak kategorii do filtrowania")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val options = mutableListOf(
            FilterOption("all", "Wszystkie kategorie", "📦", selectedCategoryId == null)
        )

        // Group categories by parent. Show parents (parentId == null) and their children indented below.
        val parents = categories.filter { it.parentId == null }
        parents.forEach { parent ->
            options.add(
                FilterOption(
                    parent.id.toString(),
                    parent.name,
                    parent.icon ?: "📦",
                    selectedCategoryId == parent.id
                )
            )
            val children = categories.filter { it.parentId == parent.id }
            children.forEach { child ->
                options.add(
                    FilterOption(
                        child.id.toString(),
                        "  ${child.name}",
                        child.icon ?: "📦",
                        selectedCategoryId == child.id
                    )
                )
            }
        }

        FilterBottomSheet.show(this, "🗂️ Filtruj po kategorii", options) { option ->
            selectedCategoryId = if (option.id == "all") null else option.id.toLong()
            applyFilters()
        }
    }

    private fun openStatusFilter() {
        val options = listOf(
            FilterOption("all", "Wszystkie statusy", "🔄", selectedStatus == null),
            FilterOption(
                ProductStatus.IN_STOCK.name,
                "Magazyn",
                "✅",
                selectedStatus == ProductStatus.IN_STOCK
            ),
            FilterOption(
                ProductStatus.ASSIGNED.name,
                "Przypisane",
                "👤",
                selectedStatus == ProductStatus.ASSIGNED
            ),
            FilterOption(
                ProductStatus.UNASSIGNED.name,
                "Brak przypisania",
                "❓",
                selectedStatus == ProductStatus.UNASSIGNED
            ),
            FilterOption(
                ProductStatus.IN_REPAIR.name,
                "Serwis",
                "🔧",
                selectedStatus == ProductStatus.IN_REPAIR
            ),
            FilterOption(
                ProductStatus.RETIRED.name,
                "Wycofane",
                "📁",
                selectedStatus == ProductStatus.RETIRED
            ),
            FilterOption(
                ProductStatus.LOST.name,
                "Zaginione",
                "❓",
                selectedStatus == ProductStatus.LOST
            )
        )

        FilterBottomSheet.show(this, "🏷️ Filtruj po statusie", options) { option ->
            selectedStatus = if (option.id == "all") {
                null
            } else {
                ProductStatus.valueOf(option.id)
            }
            applyFilters()
        }
    }

    private fun openSortDialog() {
        val options = listOf(
            FilterOption("NEWEST", "Najnowsze", "🆕", sortOption == SortOption.NEWEST),
            FilterOption("NAME_ASC", "Nazwa A-Z", "🔤", sortOption == SortOption.NAME_ASC),
            FilterOption("NAME_DESC", "Nazwa Z-A", "🔡", sortOption == SortOption.NAME_DESC),
            FilterOption("STATUS", "Status", "🏷️", sortOption == SortOption.STATUS),
            FilterOption("SERIAL", "Numer seryjny", "🔢", sortOption == SortOption.SERIAL)
        )

        FilterBottomSheet.show(this, "↕️ Sortuj", options) { option ->
            sortOption = SortOption.valueOf(option.id)
            applyFilters()
        }
    }
    
    

    private fun statusLabel(status: ProductStatus): String {
        return when (status) {
            ProductStatus.IN_STOCK -> "Magazyn"
            ProductStatus.ASSIGNED -> "Przypisane"
            ProductStatus.UNASSIGNED -> "Brak przypisania"
            ProductStatus.IN_REPAIR -> "Serwis"
            ProductStatus.RETIRED -> "Wycofane"
            ProductStatus.LOST -> "Zaginione"
        }
    }

    private fun updateFilterLabels() {
        val categoryText = selectedCategoryId?.let { id ->
            categories.firstOrNull { it.id == id }?.name
        } ?: "Kategoria"
        binding.filterButton.text = categoryText

        val statusText = selectedStatus?.let { statusLabel(it) } ?: "Status"
        binding.statusFilterButton.text = statusText

        val sortDescription = when (sortOption) {
            SortOption.NEWEST -> "Najnowsze"
            SortOption.NAME_ASC -> "Nazwa A-Z"
            SortOption.NAME_DESC -> "Nazwa Z-A"
            SortOption.STATUS -> "Status"
            SortOption.SERIAL -> "Nr seryjny"
        }
        binding.sortButton.contentDescription = "Sortowanie: $sortDescription"
        TooltipCompat.setTooltipText(binding.sortButton, sortDescription)
    }
    
    private fun categoryNameFor(categoryId: Long?): String {
        if (categoryId == null) return "-"
        return categories.firstOrNull { it.id == categoryId }?.name ?: "-"
    }
    
    private fun categoryIconFor(categoryId: Long?): String {
        if (categoryId == null) return "📦"
        return categories.firstOrNull { it.id == categoryId }?.icon ?: "📦"
    }
    
    private fun openDetails(productId: Long) {
        findNavController().navigate(
            ProductsListFragmentDirections.actionProductsToDetails(productId)
        )
    }

    private fun openAddProduct() {
        findNavController().navigate(
            ProductsListFragmentDirections.actionProductsToAdd()
        )
    }
    
    private fun openTemplatesList() {
        findNavController().navigate(
            ProductsListFragmentDirections.actionProductsToTemplates()
        )
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.productsRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.productsRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Anuluj wszystkie animacje
        _binding?.apply {
            fabMenuOverlay.animate().cancel()
            addProductFab.animate().cancel()
            singleAddCard.animate().cancel()
            bulkAddCard.animate().cancel()
        }
        _binding = null
    }
    
    private fun showSelectionPanel() {
        binding.selectionPanel.visibility = View.VISIBLE
        binding.selectionPanel.alpha = 0f
        binding.selectionPanel.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
        updateSelectionPanel()
    }
    
    private fun hideSelectionPanel() {
        binding.selectionPanel.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                binding.selectionPanel.visibility = View.GONE
            }
            .start()
        adapter.selectionMode = false
        adapter.clearSelection()
    }
    
    private fun updateSelectionPanel() {
        val selectedCount = adapter.getSelectedCount()
        binding.selectionCountText.text = "Zaznaczono: $selectedCount"
        
        if (selectedCount == 0) {
            hideSelectionPanel()
        }
    }
    
    private fun confirmBulkDelete() {
        val selectedIds = adapter.getSelectedItems()
        val count = selectedIds.size
        
        if (count == 0) return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding.inflate(layoutInflater)
        
        sheetBinding.productNameText.text = "$count ${pluralForm(count, "produkt", "produkty", "produktów")}"
        
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteBulkProducts(selectedIds)
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun showProductOptions(anchorView: View, product: ProductEntity) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menu.add("Edytuj")
        popup.menu.add("Usuń")
        popup.setOnMenuItemClickListener { item ->
            when (item.title.toString()) {
                "Edytuj" -> {
                    val action = ProductsListFragmentDirections.actionProductsToAdd(product.id)
                    findNavController().navigate(action)
                    true
                }
                "Usuń" -> {
                    showDeleteProductConfirmation(product)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteProductConfirmation(product: ProductEntity) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding.inflate(layoutInflater)

        sheetBinding.productNameText.text = product.name
        sheetBinding.cancelButton.setOnClickListener { bottomSheet.dismiss() }
        sheetBinding.deleteButton.text = "Usuń produkt"
        sheetBinding.deleteButton.setOnClickListener {
            bottomSheet.dismiss()
            deleteSingleProduct(product.id)
        }

        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun deleteSingleProduct(productId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                productRepository.deleteProductById(productId)
            } catch (e: Exception) {
                // ignore
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
    
    private fun deleteBulkProducts(productIds: Set<Long>) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                productIds.forEach { id ->
                    productRepository.deleteProductById(id)
                }
                hideSelectionPanel()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
