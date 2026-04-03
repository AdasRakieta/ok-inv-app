package com.example.inventoryapp.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.BuildConfig
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import com.example.inventoryapp.data.local.entities.ScanType
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentHomeBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }
    private val scanHistoryRepository by lazy {
        (requireActivity().application as InventoryApplication).scanHistoryRepository
    }
    private val companyRepository by lazy {
        (requireActivity().application as InventoryApplication).companyRepository
    }
    private val contractorPointRepository by lazy {
        (requireActivity().application as InventoryApplication).contractorPointRepository
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
        loadSummaryCounts()
        loadWarehouseStats()
        loadRecentActivity()
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

    private fun loadSummaryCounts() {
        viewLifecycleOwner.lifecycleScope.launch {
            productRepository.getProductCount().collect { count ->
                binding.productsCountText.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            employeeRepository.getAllEmployeesFlow().collect { employees ->
                binding.employeesCountText.text = employees.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            companyRepository.getAllCompaniesFlow().collect { companies ->
                binding.companiesCountText.text = companies.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            contractorPointRepository.getAllContractorPointsFlow().collect { points ->
                binding.contractorPointsCountText.text = points.size.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            productRepository.getProductCountByStatus(ProductStatus.IN_STOCK).collect { count ->
                binding.warehouseCountText.text = count.toString()
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

        binding.companiesCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_companies)
        }

        binding.contractorPointsCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_contractorPoints)
        }
        
        // Printer module - navigate to printer settings
        binding.printerCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_printer_settings)
        }

        binding.warehouseInfoButton.setOnClickListener {
            showCategoriesBottomSheet()
        }
    }

    private fun loadRecentActivity() {
        viewLifecycleOwner.lifecycleScope.launch {
            scanHistoryRepository.getRecentScans().collect { scans ->
                val recentItems = scans.take(5)
                val container = binding.recentActivityContainer
                container.removeAllViews()

                if (recentItems.isEmpty()) {
                    binding.recentActivityEmpty.visibility = View.VISIBLE
                    container.addView(binding.recentActivityEmpty)
                    return@collect
                }

                binding.recentActivityEmpty.visibility = View.GONE
                recentItems.forEach { scan ->
                    val itemView = layoutInflater.inflate(
                        R.layout.item_recent_activity,
                        container,
                        false
                    )
                    bindRecentActivityItem(itemView, scan)
                    container.addView(itemView)
                }
            }
        }
    }

    private fun bindRecentActivityItem(view: View, scan: ScanHistoryEntity) {
        val iconContainer = view.findViewById<MaterialCardView>(R.id.activityIconContainer)
        val iconText = view.findViewById<TextView>(R.id.activityIcon)
        val titleText = view.findViewById<TextView>(R.id.activityTitle)
        val subtitleText = view.findViewById<TextView>(R.id.activitySubtitle)

        val (icon, colorRes) = getActivityIcon(scan)
        val baseColor = ContextCompat.getColor(requireContext(), colorRes)
        val backgroundColor = ColorUtils.setAlphaComponent(baseColor, 32)

        iconText.text = icon
        iconText.setTextColor(baseColor)
        iconContainer.setCardBackgroundColor(backgroundColor)

        val titlePrefix = if (scan.success) "Zeskanowano" else "Nieudany skan"
        titleText.text = "$titlePrefix: ${formatScanValue(scan.scannedValue)}"

        val contextLabel = getContextLabel(scan.context)
        subtitleText.text = "$contextLabel • ${formatTimeAgo(scan.timestamp)}"
    }

    private fun getActivityIcon(scan: ScanHistoryEntity): Pair<String, Int> {
        if (!scan.success) {
            return "⚠️" to R.color.warning
        }

        return when (scan.scanType) {
            ScanType.QR_CODE -> "🔳" to R.color.info
            ScanType.SERIAL_NUMBER -> "🔢" to R.color.accent
            ScanType.BARCODE -> "➕" to R.color.home_primary
        }
    }

    private fun getContextLabel(context: String?): String {
        return when (context?.lowercase()) {
            "bulk_add" -> "Dodawanie zbiorcze"
            "product_details" -> "Szczegoly produktu"
            "inventory_count" -> "Inwentaryzacja"
            "assign_by_scan" -> "Przypisanie"
            "warehouse" -> "Magazyn"
            else -> "Skanowanie"
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "przed chwila"
            minutes < 60 -> "$minutes min temu"
            hours < 24 -> "$hours godz. temu"
            days < 7 -> "$days dni temu"
            else -> "ponad tydzien temu"
        }
    }

    private fun formatScanValue(value: String): String {
        val trimmed = value.trim()
        return if (trimmed.length <= 24) trimmed else trimmed.take(21) + "..."
    }

    private fun showCategoriesBottomSheet() {
        val context = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_warehouse_categories, null)
        
        val totalProductsText = dialogView.findViewById<TextView>(R.id.totalProductsText)
        val categoriesContainer = dialogView.findViewById<LinearLayout>(R.id.categoriesContainer)

        // Calculate total
        val totalCount = cachedCategoryCounts.sumOf { it.count }
        totalProductsText.text = totalCount.toString()

        // Add category cards
        if (cachedCategoryCounts.isEmpty()) {
            val placeholder = TextView(context).apply {
                text = "Brak danych magazynowych"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setPadding(0, 16, 0, 16)
            }
            categoriesContainer.addView(placeholder)
        } else {
            cachedCategoryCounts.forEach { item ->
                val card = createCategoryCard(context, item)
                categoriesContainer.addView(card)
            }
        }

        BottomSheetDialog(context).apply {
            setContentView(dialogView)
            show()
        }
    }

    private fun createCategoryCard(context: android.content.Context, item: CategoryCount): View {
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

        // Determine color based on count
        val (bgColor, strokeColor, textColor) = when {
            item.count == 0 -> Triple("#F3F4F6", "#D1D5DB", "#374151")
            item.count <= 2 -> Triple("#FEE2E2", "#FCA5A5", "#991B1B")
            item.count <= 5 -> Triple("#FEF3C7", "#FCD34D", "#92400E")
            item.count <= 10 -> Triple("#DBEAFE", "#93C5FD", "#1E40AF")
            else -> Triple("#F0FDF4", "#86EFAC", "#166534")
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
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor(textColor))
        }

        textContainer.addView(nameText)
        textContainer.addView(countText)

        // Warning icon
        val warningIcon = TextView(context).apply {
            val icon = when {
                item.count <= 2 -> "❗"
                item.count <= 5 -> "⚠️"
                else -> ""
            }
            text = icon
            textSize = 20f
        }

        container.addView(emojiText)
        container.addView(textContainer)
        if (item.count <= 5) {
            container.addView(warningIcon)
        }

        card.addView(container)
        return card
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
