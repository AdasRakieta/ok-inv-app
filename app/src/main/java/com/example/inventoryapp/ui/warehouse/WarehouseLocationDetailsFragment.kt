package com.example.inventoryapp.ui.warehouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.ui.warehouse.LocationStorage
import com.example.inventoryapp.databinding.BottomSheetDeleteLocationConfirmBinding
import com.example.inventoryapp.databinding.FragmentWarehouseLocationDetailsBinding
import com.example.inventoryapp.ui.employees.AssignedProductsAdapter
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Intent
import android.graphics.Bitmap
import android.widget.ImageView
import com.example.inventoryapp.utils.QRCodeGenerator
import com.example.inventoryapp.utils.MediaStoreHelper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class WarehouseLocationDetailsFragment : Fragment() {

    private var _binding: FragmentWarehouseLocationDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: WarehouseLocationDetailsFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val warehouseLocationRepository by lazy { (requireActivity().application as InventoryApplication).warehouseLocationRepository }
    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }
    private val locationStorage by lazy { LocationStorage(requireContext()) }

    private lateinit var assignedProductsAdapter: AssignedProductsAdapter
    private lateinit var boxesAdapter: BoxesAdapter
    private var showingPackedOnly: Boolean = false
    private var currentSearchQuery: String = ""
    private var latestProductsInLocation: List<ProductEntity> = emptyList()
    private var latestBoxesInLocation: List<BoxEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseLocationDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupActions()
        applyFiltersAndRender()
        loadLocationDetails()
    }

    private fun setupRecyclerView() {
        assignedProductsAdapter = AssignedProductsAdapter(
            onProductClick = { product ->
                // Navigate to product details if needed
            },
            onUnassignClick = { product ->
                showUnassignConfirmation(product)
            }
        )

        boxesAdapter = BoxesAdapter(onBoxClick = { box ->
            val bundle = android.os.Bundle().apply { putLong("boxId", box.id) }
            findNavController().navigate(com.example.inventoryapp.R.id.boxDetailsFragment, bundle)
        })

        binding.assignedProductsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedProductsAdapter
            setHasFixedSize(false)
        }
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.assignProductButton.setOnClickListener {
            showAssignProductDialog()
        }

        binding.assignProductScanButton.setOnClickListener {
            val action = WarehouseLocationDetailsFragmentDirections
                .actionLocationDetailsToAssignByScan(
                    employeeId = -1L,
                    locationName = args.locationName,
                    contractorPointId = -1L
                )
            findNavController().navigate(action)
        }

        binding.editButton.setOnClickListener {
            showEditLocationDialog()
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.qrButton.setOnClickListener {
            generateAndShowQr()
        }

        binding.searchProductsEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString().orEmpty()
                applyFiltersAndRender()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.viewToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            showingPackedOnly = checkedId == com.example.inventoryapp.R.id.boxesModeButton
            applyFiltersAndRender()
        }
    }

    private fun loadLocationDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            var locationName = args.locationName

            // If invoked via deep link with qrUid, resolve to location code/name
            if (locationName.isNullOrBlank() && !args.qrUid.isNullOrBlank()) {
                try {
                    val loc = warehouseLocationRepository.getLocationByQrUid(args.qrUid!!)
                    locationName = loc?.code ?: args.qrUid
                } catch (e: Exception) {
                    locationName = args.qrUid
                }
            }

            val displayName = locationName ?: ""
            val normalizedLocationName = normalizeLocationName(displayName)

            // Update header
            binding.locationName.text = displayName
            binding.locationShelf.text = displayName.substringBefore("/").trim().ifBlank { "-" }
            binding.locationBin.text = displayName.substringAfter("/", "").trim().ifBlank { "-" }
            val locationDescription = locationStorage.getLocationDescription(displayName)
            binding.locationDescriptionText.text = locationDescription.takeIf { it.isNotEmpty() } ?: "Brak opisu"

            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Try to resolve a real location entity to use DB-level aggregation (including boxes)
                val locationEntity = try {
                    // Try by qrUid if available
                    if (!args.qrUid.isNullOrBlank()) {
                        warehouseLocationRepository.getLocationByQrUid(args.qrUid!!)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                } ?: try {
                    // Try by code/name
                    warehouseLocationRepository.getLocationByCode(displayName)
                } catch (e: Exception) {
                    null
                }

                if (locationEntity != null) {
                    // When we have a persisted location entity, allow creating a box tied to it
                    binding.addBoxButton.setOnClickListener {
                        val bundle = android.os.Bundle().apply {
                            putLong("warehouseLocationId", locationEntity.id)
                            putString("locationName", displayName)
                        }
                        findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, bundle)
                    }
                    // Collect boxes for this location to support packed view and box name mapping
                    launch {
                        boxRepository.getBoxesByLocation(locationEntity.id).collect { boxes ->
                            latestBoxesInLocation = boxes

                            // Also prepare a map of boxId -> name for product annotations
                            val boxesMap = boxes.associate { it.id to it.name }
                            assignedProductsAdapter.setBoxesMap(boxesMap)
                            // Compute product counts per box and update adapter
                            val counts = mutableMapOf<Long, Int>()
                            for (box in boxes) {
                                try {
                                    val products = productRepository.getProductsByBoxId(box.id).firstOrNull() ?: emptyList()
                                    counts[box.id] = products.size
                                } catch (e: Exception) {
                                    counts[box.id] = 0
                                }
                            }
                            boxesAdapter.setCountsMap(counts)
                            updateOverviewCounters()
                            applyFiltersAndRender()
                        }
                    }

                    // Use optimized DAO query that includes products inside boxes placed in this location
                    productRepository.getProductsByLocationIncludingBoxes(locationEntity.id).collect { productsInLocation ->
                        latestProductsInLocation = productsInLocation
                        val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                        val categoriesInLocation = productsInLocation.mapNotNull { it.categoryId }.distinct()
                        val categoryEmojis = categories.filter { it.id in categoriesInLocation }
                            .mapNotNull { it.icon ?: getCategoryEmoji(it.name) }
                            .distinct()
                            .joinToString(" ")
                        binding.locationCategories.text = categoryEmojis.takeIf { it.isNotEmpty() } ?: "-"

                        // Provide category map (icon or name) to the adapter for visual label
                        val categoryMapForAdapter = categories.associate { it.id to (it.icon ?: it.name) }
                        assignedProductsAdapter.setCategoriesMap(categoryMapForAdapter)
                        updateOverviewCounters()
                        applyFiltersAndRender()
                    }
                } else {
                    // Non-persisted location: create box but pass the display name to prefill
                    binding.addBoxButton.setOnClickListener {
                        val bundle = android.os.Bundle().apply {
                            putLong("warehouseLocationId", 0L)
                            putString("locationName", displayName)
                        }
                        findNavController().navigate(com.example.inventoryapp.R.id.addEditBoxFragment, bundle)
                    }
                    latestBoxesInLocation = emptyList()
                    updateOverviewCounters()
                    // Fallback: existing string-based matching (for non-persisted locations)
                    productRepository.getAllProducts().collect { allProducts ->
                        val productsInLocation = allProducts.filter { product ->
                            if (product.status != ProductStatus.IN_STOCK) return@filter false
                            val shelf = product.shelf ?: "Magazyn"
                            val bin = product.bin ?: ""
                            val productLocation = normalizeLocationName(
                                shelf + (if (bin.isNotBlank()) " / $bin" else "")
                            )
                            productLocation == normalizedLocationName
                        }
                        latestProductsInLocation = productsInLocation

                        val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                        val categoriesInLocation = productsInLocation.mapNotNull { it.categoryId }.distinct()
                        val categoryEmojis = categories.filter { it.id in categoriesInLocation }
                            .mapNotNull { it.icon ?: getCategoryEmoji(it.name) }
                            .distinct()
                            .joinToString(" ")
                        binding.locationCategories.text = categoryEmojis.takeIf { it.isNotEmpty() } ?: "-"
                        updateOverviewCounters()
                        applyFiltersAndRender()
                    }
                }
            }
        }
    }

    private fun applyFiltersAndRender() {
        if (!::assignedProductsAdapter.isInitialized || !::boxesAdapter.isInitialized) return

        assignedProductsAdapter.setFullList(latestProductsInLocation)
        assignedProductsAdapter.filterByQuery(currentSearchQuery)

        boxesAdapter.setFullList(latestBoxesInLocation)
        boxesAdapter.filterByQuery(currentSearchQuery)

        val isBoxesMode = showingPackedOnly
        binding.assignedProductsRecyclerView.adapter = if (isBoxesMode) boxesAdapter else assignedProductsAdapter

        val visibleItemsCount = if (isBoxesMode) boxesAdapter.itemCount else assignedProductsAdapter.itemCount
        binding.assignedCountText.text = if (isBoxesMode) {
            "$visibleItemsCount ${pluralForm(visibleItemsCount, "karton", "kartony", "kartonów")} w tej lokalizacji"
        } else {
            "$visibleItemsCount ${pluralForm(visibleItemsCount, "produkt", "produkty", "produktów")} w tej lokalizacji"
        }

        binding.searchProductsEditText.hint = if (isBoxesMode) {
            "Szukaj kartonu (nazwa, opis)..."
        } else {
            "Szukaj produktu (nazwa, S/N)..."
        }

        binding.noProductsText.text = if (isBoxesMode) {
            "Brak kartonów w tej lokalizacji"
        } else {
            "Brak produktów w tej lokalizacji"
        }

        val isEmpty = visibleItemsCount == 0
        binding.noProductsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.assignedProductsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateOverviewCounters() {
        binding.totalProductsValue.text = latestProductsInLocation.size.toString()
        binding.totalBoxesValue.text = latestBoxesInLocation.size.toString()
    }

    private fun resolvedLocationName(): String {
        val fromHeader = binding.locationName.text?.toString()?.trim().orEmpty()
        if (fromHeader.isNotEmpty()) return fromHeader
        return args.locationName.orEmpty()
    }

    private fun generateAndShowQr() {
        lifecycleScope.launch {
            try {
                var qrUid = args.qrUid

                if (qrUid.isNullOrBlank()) {
                    val code = args.locationName ?: binding.locationName.text?.toString() ?: ""
                    if (code.isBlank()) {
                        Toast.makeText(requireContext(), "Brak nazwy lokalizacji i qrUid", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val loc = warehouseLocationRepository.getLocationByCode(code)
                    if (loc == null) {
                        val newQr = java.util.UUID.randomUUID().toString()
                        val entity = com.example.inventoryapp.data.local.entities.WarehouseLocationEntity(
                            code = code,
                            qrUid = newQr,
                            name = code
                        )
                        warehouseLocationRepository.insertLocation(entity)
                        qrUid = newQr
                    } else {
                        qrUid = loc.qrUid
                        if (qrUid.isNullOrBlank()) {
                            val newQr = java.util.UUID.randomUUID().toString()
                            val updated = loc.copy(qrUid = newQr)
                            warehouseLocationRepository.updateLocation(updated)
                            qrUid = newQr
                        }
                    }
                }

                qrUid?.let { uid ->
                    val payload = "invapp://location/$uid"
                    val bitmap = QRCodeGenerator.generateFromString(payload, 1024, 1024)
                    if (bitmap == null) {
                        Toast.makeText(requireContext(), "Nie udało się wygenerować QR", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    showQrDialog(bitmap)

                    val displayName = "location_$uid"
                    val uri = MediaStoreHelper.saveBitmap(requireContext(), bitmap, displayName)
                    if (uri != null) {
                        Snackbar.make(binding.root, "QR zapisano", Snackbar.LENGTH_LONG)
                            .setAction("Udostępnij") {
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    type = "image/png"
                                }
                                startActivity(Intent.createChooser(share, "Udostępnij QR"))
                            }
                            .show()
                    } else {
                        Toast.makeText(requireContext(), "Nie udało się zapisać obrazu", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showQrDialog(bitmap: Bitmap) {
        val imageView = ImageView(requireContext()).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("QR lokalizacji")
            .setView(imageView)
            .setPositiveButton("Zamknij", null)
            .setNeutralButton("Udostępnij") { _, _ ->
                val uri = MediaStoreHelper.saveBitmap(requireContext(), bitmap, "location_${System.currentTimeMillis()}")
                if (uri != null) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                    }
                    startActivity(Intent.createChooser(share, "Udostępnij QR"))
                } else {
                    Toast.makeText(requireContext(), "Nie udało się zapisać obrazu", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showAssignProductDialog() {
        val dialog = AssignProductsToLocationDialogFragment(resolvedLocationName()) { selectedProducts ->
            assignProducts(selectedProducts)
        }
        dialog.show(childFragmentManager, "AssignProductsDialog")
    }
    
    private fun assignProducts(products: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                val locationName = resolvedLocationName()
                val shelf = locationName.substringBefore("/").trim()
                val bin = locationName.substringAfter("/", "").trim().takeIf { it.isNotEmpty() }
                
                products.forEach { product ->
                    val updatedProduct = product.copy(
                        shelf = shelf,
                        bin = bin,
                        status = ProductStatus.IN_STOCK,
                        updatedAt = System.currentTimeMillis()
                    )
                    productRepository.updateWithHistory(
                        updatedProduct,
                        MovementHistoryUtils.entryForLocation(locationName)
                    )
                }
                
                Toast.makeText(
                    requireContext(),
                    "Przypisano ${products.size} produktów do ${locationName}",
                    Toast.LENGTH_SHORT
                ).show()

                locationStorage.addLocation(locationName)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditLocationDialog() {
        val action = WarehouseLocationDetailsFragmentDirections.actionLocationDetailsToEdit(resolvedLocationName())
        findNavController().navigate(action)
    }

    private fun showDeleteConfirmation() {
        val locationName = resolvedLocationName()
        
        lifecycleScope.launch {
            val allProducts = productRepository.getAllProducts().firstOrNull() ?: return@launch
            val productsInLocation = allProducts.filter { product ->
                val shelf = product.shelf ?: "Magazyn"
                val bin = product.bin ?: ""
                val productLocation = shelf + (if (bin.isNotBlank()) " / $bin" else "")
                productLocation == locationName
            }
            
            val bottomSheet = BottomSheetDialog(requireContext())
            val sheetBinding = BottomSheetDeleteLocationConfirmBinding.inflate(layoutInflater)

            sheetBinding.titleText.text = "Usuń lokalizację"
            sheetBinding.locationNameText.text = locationName

            sheetBinding.warningTitleText.text = "Czy na pewno chcesz usunąć tę lokalizację?"
            sheetBinding.warningDetailsText.text = if (productsInLocation.isEmpty()) {
                "• Lokalizacja zostanie usunięta\n• Tej operacji nie można cofnąć"
            } else {
                "• Przypisanie ${productsInLocation.size} produktów zostanie usunięte\n• Produkty zostaną odpięte od lokalizacji\n• Tej operacji nie można cofnąć"
            }

            sheetBinding.cancelButton.setOnClickListener {
                bottomSheet.dismiss()
            }

            sheetBinding.deleteButton.setOnClickListener {
                bottomSheet.dismiss()
                deleteLocation(locationName, productsInLocation)
            }

            bottomSheet.setContentView(sheetBinding.root)
            bottomSheet.show()
        }
    }
    
    private fun deleteLocation(locationName: String, productsInLocation: List<ProductEntity>) {
        lifecycleScope.launch {
            try {
                // Unassign all products from this location
                productsInLocation.forEach { product ->
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
                
                Toast.makeText(
                    requireContext(),
                    "Usunięto lokalizację ${locationName}",
                    Toast.LENGTH_SHORT
                ).show()
                locationStorage.removeLocation(locationName)
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showUnassignConfirmation(product: ProductEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń przypisanie produktu")
            .setMessage("Czy na pewno chcesz usunąć przypisanie?\n\n${product.name}\nS/N: ${product.serialNumber}")
            .setPositiveButton("Usuń przypisanie") { _, _ ->
                unassignProduct(product)
            }
            .setNegativeButton("Anuluj", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }
    
    private fun unassignProduct(product: ProductEntity) {
        lifecycleScope.launch {
            try {
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
                Toast.makeText(requireContext(), "Usunięto przypisanie", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun normalizeLocationName(raw: String): String {
        return raw.split("/")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" / ")
    }

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

    private fun pluralForm(count: Int, singular: String, plural2_4: String, plural5: String): String {
        return when {
            count == 1 -> singular
            count in 2..4 -> plural2_4
            else -> plural5
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
