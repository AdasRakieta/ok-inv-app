package com.example.inventoryapp.ui.products

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.CategoryEntity
import com.example.inventoryapp.databinding.FragmentProductDetailsBinding
import com.example.inventoryapp.databinding.BottomSheetEditSerialBinding
import com.example.inventoryapp.databinding.BottomSheetDeleteConfirmBinding
import com.example.inventoryapp.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.inventoryapp.data.local.entities.ProductEntity
import androidx.appcompat.app.AlertDialog
import android.content.Intent
import android.widget.ImageView
import android.widget.TextView
import android.net.Uri
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.inventoryapp.utils.BarcodeGenerator
import com.example.inventoryapp.utils.MediaStoreHelper
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.example.inventoryapp.utils.BrotherPrinterHelper
import com.example.inventoryapp.utils.PrinterPreferences
import com.google.android.material.snackbar.Snackbar
import com.example.inventoryapp.data.local.entities.ProductStatus
import kotlinx.coroutines.flow.firstOrNull
import com.example.inventoryapp.data.local.entities.BoxEntity

class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!
    
    private val args: ProductDetailsFragmentArgs by navArgs()
    
    private val productRepository by lazy {
        (requireActivity().application as InventoryApplication).productRepository
    }
    private val categoryRepository by lazy {
        (requireActivity().application as InventoryApplication).categoryRepository
    }
    private val employeeRepository by lazy {
        (requireActivity().application as InventoryApplication).employeeRepository
    }

    private var currentProduct: ProductEntity? = null
    private var categories: List<CategoryEntity> = emptyList()
    
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        observeCategories()
        loadProductDetails()
        setupActions()
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                categoryRepository.getAllCategories().collect { list ->
                    categories = list
                    updateCategoryLabel()
                }
            }
        }
    }
    
    private fun loadProductDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getProductById(args.productId).collect { product ->
                    currentProduct = product
                    product?.let {
                        binding.apply {
                            productNameText.text = it.name
                            productCategoryText.text = categoryNameFor(it.categoryId)
                            productIconText.text = categoryIconFor(it.categoryId)
                            productIdValue.text = it.customId ?: "-"
                            manufacturerValue.text = it.manufacturer ?: "-"
                            modelValue.text = it.model ?: "-"
                            descriptionValue.text = it.description ?: "-"
                            
                            createdAtText.text = formatDate(it.createdAt)
                            updatedAtText.text = formatDate(it.updatedAt)

                            // Assignment info (legacy section)
                            if (it.assignedToEmployeeId != null) {
                                assignedEmployeeLayout.visibility = View.VISIBLE
                                loadEmployeeInfo(it.assignedToEmployeeId)
                            } else {
                                assignedEmployeeLayout.visibility = View.GONE
                            }

                            buildMovementHistory(it)

                            updateStatusBadge(it.status)

                            // Status-level extra info (location / box / employee)
                            // Show location and box only when item is in stock; show employee when assigned
                            val status = it.status
                            if (status == ProductStatus.IN_STOCK) {
                                statusExtraContainer.visibility = View.VISIBLE
                                statusLocationContainer.visibility = View.VISIBLE
                                // compute location display
                                val locText = if (!it.shelf.isNullOrBlank()) {
                                    it.shelf + (if (!it.bin.isNullOrBlank()) " / ${it.bin}" else "")
                                } else if (it.warehouseLocationId != null) {
                                    val loc = (requireActivity().application as InventoryApplication)
                                        .warehouseLocationRepository.getLocationById(it.warehouseLocationId).firstOrNull()
                                    loc?.code ?: "Nie przypisano"
                                } else {
                                    "Nie przypisano"
                                }
                                statusLocationValue.text = locText

                                // box info
                                if (it.boxId != null) {
                                    val box = (requireActivity().application as InventoryApplication)
                                        .boxRepository.getBoxById(it.boxId).firstOrNull()
                                    statusBoxValue.text = box?.name ?: "-"
                                    statusBoxContainer.visibility = View.VISIBLE
                                } else {
                                    statusBoxContainer.visibility = View.GONE
                                }

                                statusEmployeeContainer.visibility = View.GONE
                            } else if (status == ProductStatus.ASSIGNED) {
                                statusExtraContainer.visibility = View.VISIBLE
                                statusLocationContainer.visibility = View.GONE
                                statusBoxContainer.visibility = View.GONE
                                statusEmployeeContainer.visibility = View.VISIBLE
                                if (it.assignedToEmployeeId != null) {
                                    val emp = employeeRepository.getEmployeeById(it.assignedToEmployeeId)
                                    statusEmployeeValue.text = emp?.fullName ?: "Nieznany"
                                } else {
                                    statusEmployeeValue.text = "-"
                                }
                            } else {
                                statusExtraContainer.visibility = View.GONE
                            }

                            // Serial number visibility
                            if (it.serialNumber.isNotBlank()) {
                                serialNumberAssignedLayout.visibility = View.VISIBLE
                                serialNumberNotAssignedLayout.visibility = View.GONE
                                serialNumberText.text = it.serialNumber
                                printEanButton.visibility = View.VISIBLE
                            } else {
                                serialNumberAssignedLayout.visibility = View.GONE
                                serialNumberNotAssignedLayout.visibility = View.VISIBLE
                                printEanButton.visibility = View.GONE
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryLabel() {
        currentProduct?.let {
            binding.productCategoryText.text = categoryNameFor(it.categoryId)
            binding.productIconText.text = categoryIconFor(it.categoryId)
        }
    }

    private fun setupActions() {
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.moreButton.setOnClickListener { showOptionsMenu(it) }
        binding.editSerialButton.setOnClickListener { promptEditSerial() }
        binding.printEanButton.setOnClickListener { printBarcodeLabel() }
    }

    private fun showOptionsMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_product_details, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    navigateToEditForm()
                    true
                }
                R.id.action_delete -> {
                    confirmDeleteProduct()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateStatusBadge(status: ProductStatus) {
        val color = ContextCompat.getColor(requireContext(), statusColor(status))
        binding.statusBadgeText.text = statusLabel(status)
        binding.statusBadgeText.setTextColor(color)
        binding.statusDot.backgroundTintList = ColorStateList.valueOf(color)
        binding.statusBadgeCard.setCardBackgroundColor(ColorUtils.setAlphaComponent(color, 26))
    }

    private fun statusLabel(status: ProductStatus): String = when (status) {
        ProductStatus.IN_STOCK -> "Magazyn"
        ProductStatus.ASSIGNED -> "Przypisane"
        ProductStatus.UNASSIGNED -> "Brak przypisania"
        ProductStatus.IN_REPAIR -> "Serwis"
        ProductStatus.RETIRED -> "Wycofane"
        ProductStatus.LOST -> "Zaginione"
    }

    private fun statusColor(status: ProductStatus): Int = when (status) {
        ProductStatus.IN_STOCK -> R.color.success
        ProductStatus.ASSIGNED -> R.color.info
        ProductStatus.UNASSIGNED -> R.color.warning
        ProductStatus.IN_REPAIR -> R.color.warning
        ProductStatus.RETIRED -> R.color.text_tertiary
        ProductStatus.LOST -> R.color.error
    }

    private fun promptEditSerial() {
        val product = currentProduct ?: return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetEditSerialBinding.inflate(layoutInflater)
        
        // Set current serial number
        sheetBinding.serialNumberInput.setText(product.serialNumber)
        sheetBinding.serialNumberInput.requestFocus()
        
        // Cancel button
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        // Save button
        sheetBinding.saveButton.setOnClickListener {
            val newSerial = sheetBinding.serialNumberInput.text.toString().trim()
            
            when {
                newSerial.isEmpty() -> {
                    toast("Numer seryjny nie może być pusty")
                }
                newSerial == product.serialNumber -> {
                    toast("Numer seryjny bez zmian")
                    bottomSheet.dismiss()
                }
                else -> {
                    saveProduct(product.copy(serialNumber = newSerial, updatedAt = System.currentTimeMillis()))
                    bottomSheet.dismiss()
                }
            }
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
        
        // Show keyboard
        sheetBinding.serialNumberInput.postDelayed({
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(sheetBinding.serialNumberInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun navigateToEditForm() {
        val product = currentProduct ?: return
        val action = ProductDetailsFragmentDirections.actionProductDetailsToAdd(product.id)
        findNavController().navigate(action)
    }

    private fun confirmDeleteProduct() {
        val product = currentProduct ?: return
        
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetDeleteConfirmBinding.inflate(layoutInflater)
        
        // Set product name
        sheetBinding.productNameText.text = product.name
        
        // Cancel button
        sheetBinding.cancelButton.setOnClickListener {
            bottomSheet.dismiss()
        }
        
        // Delete button with animation
        sheetBinding.deleteButton.setOnClickListener {
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    
                    lifecycleScope.launch {
                        productRepository.deleteProductById(product.id)
                        toast("Produkt usunięty")
                        bottomSheet.dismiss()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                .start()
        }
        
        bottomSheet.setContentView(sheetBinding.root)
        bottomSheet.show()
    }

    private fun saveProduct(updated: ProductEntity) {
        lifecycleScope.launch {
            try {
                productRepository.updateProduct(updated)
                toast("Zapisano zmiany")
            } catch (e: Exception) {
                toast("Błąd: ${e.message}")
            }
        }
    }

    private fun formatDate(timestamp: Long?): String {
        return if (timestamp == null) "-" else dateFormat.format(Date(timestamp))
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun categoryNameFor(id: Long?): String {
        if (id == null) return "-"
        return categories.firstOrNull { it.id == id }?.name ?: "-"
    }
    
    private fun categoryIconFor(id: Long?): String {
        if (id == null) return "📦"
        return categories.firstOrNull { it.id == id }?.icon ?: "📦"
    }

    private fun loadEmployeeInfo(employeeId: Long) {
        lifecycleScope.launch {
            val employee = employeeRepository.getEmployeeById(employeeId)
            binding.assignedEmployeeValue.text = employee?.fullName ?: "Nieznany"
        }
    }

    private fun buildMovementHistory(product: ProductEntity) {
        val storedHistory = MovementHistoryUtils.formatForDisplay(product.movementHistory)
        if (storedHistory.isNotBlank()) {
            binding.movementHistoryText.text = storedHistory
            return
        }

        val warehouseLocation = if (product.shelf != null) {
            "Magazyn (${product.shelf}${if (!product.bin.isNullOrBlank()) " / ${product.bin}" else ""})"
        } else {
            "Magazyn"
        }

        if (product.assignedToEmployeeId != null) {
            lifecycleScope.launch {
                val employee = employeeRepository.getEmployeeById(product.assignedToEmployeeId)
                val employeeName = employee?.fullName ?: "Nieznany"
                val assignmentInfo = if (product.assignmentDate != null) {
                    "$employeeName (${formatDate(product.assignmentDate)})"
                } else {
                    employeeName
                }
                binding.movementHistoryText.text = "$warehouseLocation → $assignmentInfo"
            }
        } else {
            val statusFallback = when (product.status) {
                com.example.inventoryapp.data.local.entities.ProductStatus.IN_REPAIR -> "Serwis"
                com.example.inventoryapp.data.local.entities.ProductStatus.RETIRED -> "Wycofane"
                com.example.inventoryapp.data.local.entities.ProductStatus.LOST -> "Zaginione"
                com.example.inventoryapp.data.local.entities.ProductStatus.UNASSIGNED -> "Brak przypisania"
                else -> warehouseLocation
            }
            binding.movementHistoryText.text = statusFallback
        }
    }

    private fun showEanDialog() {
        val product = currentProduct ?: return
        val serial = product.serialNumber
        if (serial.isBlank()) {
            Snackbar.make(binding.root, "Numer seryjny jest pusty", Snackbar.LENGTH_LONG).show()
            return
        }

        // Choose barcode type: prefer EAN-13 when valid, otherwise Code128
        val bitmap = when {
            BarcodeGenerator.isValidEAN13Content(serial) -> BarcodeGenerator.generateEAN13(serial, 700, 200)
            else -> BarcodeGenerator.generateCode128(serial, 700, 200)
        }

        if (bitmap == null) {
            Snackbar.make(binding.root, "Nie udało się wygenerować kodu", Snackbar.LENGTH_LONG).show()
            return
        }

        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_barcode_preview, null)
        val previewImage = view.findViewById<ImageView>(R.id.previewImage)
        val previewText = view.findViewById<TextView>(R.id.previewText)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)
        val shareButton = view.findViewById<MaterialButton>(R.id.shareButton)

        previewImage.setImageBitmap(bitmap)
        previewText.text = serial

        var savedUri: Uri? = null

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                val name = "kod_${serial}_${System.currentTimeMillis()}"
                val uri = withContext(Dispatchers.IO) { MediaStoreHelper.saveBitmap(requireContext(), bitmap, name) }
                if (uri != null) {
                    savedUri = uri
                    toast("Zapisano do galerii")
                } else {
                    toast("Błąd zapisu")
                }
            }
        }

        shareButton.setOnClickListener {
            lifecycleScope.launch {
                val uri = savedUri ?: withContext(Dispatchers.IO) {
                    MediaStoreHelper.saveBitmap(requireContext(), bitmap, "kod_${serial}_${System.currentTimeMillis()}")
                }

                if (uri == null) {
                    toast("Nie udało się przygotować pliku do udostępnienia")
                    return@launch
                }

                val share = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/png"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(share, "Udostępnij kod"))
            }
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    /**
     * Print barcode label for current product
     */
    private fun printBarcodeLabel() {
        val product = currentProduct ?: return
        
        if (product.serialNumber.isBlank()) {
            Snackbar.make(
                binding.root,
                "Cannot print label: Serial number is empty",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        // Check if printer is configured
        val printerPreferences = PrinterPreferences(requireContext())
        val config = printerPreferences.loadPrinterConfig()
        
        if (!config.isConfigured) {
            Snackbar.make(
                binding.root,
                "Printer not configured. Please configure printer first.",
                Snackbar.LENGTH_LONG
            ).setAction("Configure") {
                // Navigate to printer settings
                findNavController().navigate(
                    com.example.inventoryapp.R.id.action_global_printerSettings
                )
            }.show()
            return
        }

        // Show printing progress
        Snackbar.make(
            binding.root,
            "Printing label for SN: ${product.serialNumber}...",
            Snackbar.LENGTH_SHORT
        ).show()

        // Print in background
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = when (config.connectionMethod) {
                    com.example.inventoryapp.data.models.PrinterConfig.ConnectionMethod.WIFI -> {
                        BrotherPrinterHelper.printSerialNumberLabelWifi(
                            serialNumber = product.serialNumber,
                            ipAddress = config.ipAddress,
                            port = config.port,
                            tapeWidthMm = config.labelWidth,
                            labelLengthMm = config.labelHeight
                        )
                    }
                    com.example.inventoryapp.data.models.PrinterConfig.ConnectionMethod.BLUETOOTH -> {
                        // Get Bluetooth device by MAC - NO PAIRING REQUIRED!
                        val device = BrotherPrinterHelper.getBluetoothDeviceByAddress(
                            requireContext(),
                            config.bluetoothAddress
                        )
                        
                        if (device == null) {
                            Snackbar.make(
                                binding.root,
                                "Bluetooth not available or invalid MAC: ${config.bluetoothAddress}",
                                Snackbar.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                        
                        BrotherPrinterHelper.printSerialNumberLabelBluetooth(
                            serialNumber = product.serialNumber,
                            device = device,
                            tapeWidthMm = config.labelWidth,
                            labelLengthMm = config.labelHeight
                        )
                    }
                    com.example.inventoryapp.data.models.PrinterConfig.ConnectionMethod.WIRELESS_DIRECT -> {
                        // Wireless Direct uses WiFi with direct connection IP
                        BrotherPrinterHelper.printSerialNumberLabelWifi(
                            serialNumber = product.serialNumber,
                            ipAddress = config.ipAddress,
                            port = config.port,
                            tapeWidthMm = config.labelWidth,
                            labelLengthMm = config.labelHeight
                        )
                    }
                }

                if (result.isSuccess) {
                    Snackbar.make(
                        binding.root,
                        "Label printed successfully!",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    printerPreferences.saveLastConnectionStatus(
                        true,
                        "Label printed for ${product.serialNumber}"
                    )
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                    Snackbar.make(
                        binding.root,
                        "Print failed: $errorMessage",
                        Snackbar.LENGTH_LONG
                    ).show()
                    
                    printerPreferences.saveLastConnectionStatus(false, errorMessage)
                }

            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Print error: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
