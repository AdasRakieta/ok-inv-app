package com.example.inventoryapp.ui.warehouse

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import kotlinx.coroutines.withContext
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.utils.MovementHistoryUtils
import com.example.inventoryapp.databinding.FragmentBoxDetailsBinding
import com.example.inventoryapp.ui.employees.AssignedProductsAdapter
import com.example.inventoryapp.utils.MediaStoreHelper
import com.example.inventoryapp.utils.QRCodeGenerator
import com.example.inventoryapp.utils.FileHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BoxDetailsFragment : Fragment() {

    private var _binding: FragmentBoxDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: BoxDetailsFragmentArgs by navArgs()

    private val boxRepository by lazy { (requireActivity().application as InventoryApplication).boxRepository }
    private val productRepository by lazy { (requireActivity().application as InventoryApplication).productRepository }
    private val categoryRepository by lazy { (requireActivity().application as InventoryApplication).categoryRepository }
    private val warehouseLocationRepository by lazy { (requireActivity().application as InventoryApplication).warehouseLocationRepository }

    private lateinit var assignedProductsAdapter: AssignedProductsAdapter

    private var currentBox: BoxEntity? = null
    private var currentProductsInBox: List<ProductEntity> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBoxDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        assignedProductsAdapter = AssignedProductsAdapter(onProductClick = {}, onUnassignClick = { product ->
            // Unassign product from box
            lifecycleScope.launch {
                val updated = product.copy(boxId = null)
                productRepository.updateWithHistory(updated, "Usunięto z kartonu ${currentBox?.name.orEmpty()}")
                Toast.makeText(requireContext(), getString(R.string.product_removed_from_box), Toast.LENGTH_SHORT).show()
            }
        })

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedProductsAdapter
            setHasFixedSize(false)
        }

        binding.printLabelButton.setOnClickListener { generateAndShowQr() }
        binding.editBoxButton.setOnClickListener { openEditScreen() }
        binding.addProductsButton.setOnClickListener { showAssignProductsDialog() }
        binding.addBulkButton.setOnClickListener { showAssignProductsDialog() }
        binding.modifyProductsButton.setOnClickListener { showRemoveProductsDialog() }
        binding.deleteBoxButton.setOnClickListener { showDeleteBoxConfirmation() }

        loadBox()
    }

    private fun loadBox() {
        lifecycleScope.launch {
            val box = try {
                if (args.boxId != 0L) {
                    boxRepository.getBoxById(args.boxId).firstOrNull()
                } else if (!args.qrUid.isNullOrBlank()) {
                    boxRepository.getBoxByQrUid(args.qrUid!!)
                } else null
            } catch (e: Exception) { null }

            if (box == null) {
                Toast.makeText(requireContext(), "Karton nie znaleziony", Toast.LENGTH_SHORT).show()
                return@launch
            }

            currentBox = box
            binding.boxName.text = box.name
            binding.boxDescription.text = box.description ?: "Brak opisu"
            binding.boxDescription.visibility = if (box.description.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.boxLocation.text = resolveLocationLabel(box.warehouseLocationId)
            binding.boxCreatedDate.text = android.text.format.DateFormat.getDateFormat(requireContext()).format(java.util.Date(box.createdAt))

            observeProducts(box.id)
        }
    }

    private fun observeProducts(boxId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productRepository.getProductsByBoxId(boxId).collect { products ->
                    currentProductsInBox = products
                    assignedProductsAdapter.setFullList(products)
                    // Provide category map to adapter so items show category labels
                    val categories = categoryRepository.getAllCategories().firstOrNull() ?: emptyList()
                    val categoryMapForAdapter = categories.associate { it.id to (it.icon ?: it.name) }
                    assignedProductsAdapter.setCategoriesMap(categoryMapForAdapter)
                    val isEmpty = products.isEmpty()
                    binding.noProductsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.productsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    val countLabel = resources.getQuantityString(R.plurals.products_count, products.size, products.size)
                    binding.productsHeader.text = "$countLabel w kartonie"
                }
            }
        }
    }

    private suspend fun resolveLocationLabel(locationId: Long?): String {
        if (locationId == null) return getString(R.string.no_location)
        val location = warehouseLocationRepository.getLocationById(locationId).firstOrNull()
        return location?.code ?: "Lokalizacja #$locationId"
    }

    private fun openEditScreen() {
        val box = currentBox ?: return
        val bundle = Bundle().apply {
            putLong("boxId", box.id)
            putLong("warehouseLocationId", box.warehouseLocationId ?: 0L)
        }
        findNavController().navigate(R.id.addEditBoxFragment, bundle)
    }

    private fun showAssignProductsDialog() {
        val box = currentBox ?: return
        val dialog = AssignProductsToBoxDialogFragment(box.id) { selectedProducts ->
            assignProductsToBox(selectedProducts, box)
        }
        dialog.show(childFragmentManager, "AssignProductsToBoxDialog")
    }

    private fun assignProductsToBox(products: List<ProductEntity>, box: BoxEntity) {
        lifecycleScope.launch {
            try {
                // Resolve location label for the box (if any) to store shelf/bin and for history
                val locationLabel = resolveLocationLabel(box.warehouseLocationId)
                val shelf = locationLabel.substringBefore("/").trim()
                val bin = locationLabel.substringAfter("/", "").trim().takeIf { it.isNotEmpty() }
                productRepository.assignProductsToBox(products, box.id, box.warehouseLocationId, shelf, bin, box.name)
                Toast.makeText(requireContext(), "Dodano ${products.size} produktów", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveProductsDialog() {
        val box = currentBox ?: return
        if (currentProductsInBox.isEmpty()) {
            Toast.makeText(requireContext(), "Karton nie zawiera produktów", Toast.LENGTH_SHORT).show()
            return
        }

        val labels = currentProductsInBox.map { product ->
            "${product.name} • ${product.serialNumber}"
        }.toTypedArray()
        val checked = BooleanArray(labels.size)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń produkty z kartonu")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setNegativeButton(getString(R.string.cancel_pl), null)
            .setPositiveButton("Usuń") { _, _ ->
                val selected = currentProductsInBox.filterIndexed { index, _ -> checked[index] }
                if (selected.isEmpty()) return@setPositiveButton

                lifecycleScope.launch {
                    selected.forEach { product ->
                        val updated = product.copy(boxId = null)
                        productRepository.updateWithHistory(updated, "Usunięto z kartonu ${box.name}")
                    }
                    Toast.makeText(requireContext(), "Usunięto ${selected.size} produktów", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showDeleteBoxConfirmation() {
        val box = currentBox ?: return

        val message = if (currentProductsInBox.isEmpty()) {
            "Czy na pewno chcesz usunąć karton ${box.name}?"
        } else {
            "Karton ${box.name} zawiera ${currentProductsInBox.size} produktów. Produkty zostaną odpięte od kartonu. Kontynuować?"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń karton")
            .setMessage(message)
            .setNegativeButton(getString(R.string.cancel_pl), null)
            .setPositiveButton("Usuń") { _, _ ->
                lifecycleScope.launch {
                    try {
                        currentProductsInBox.forEach { product ->
                            val updated = product.copy(boxId = null)
                            productRepository.updateWithHistory(updated, "Karton ${box.name} został usunięty")
                        }
                        boxRepository.deleteBox(box)
                        Toast.makeText(requireContext(), "Karton usunięty", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Nie udało się usunąć kartonu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    private fun generateAndShowQr() {
        lifecycleScope.launch {
            try {
                var qrUid = currentBox?.qrUid

                if (qrUid.isNullOrBlank()) {
                    val newQr = java.util.UUID.randomUUID().toString()
                    val updated = currentBox?.copy(qrUid = newQr)
                    if (updated != null) {
                        boxRepository.updateBox(updated)
                        currentBox = updated
                        qrUid = newQr
                    }
                }

                qrUid?.let { uid ->
                    val payload = "invapp://box/$uid"
                    val bitmap = QRCodeGenerator.generateFromString(payload, 1024, 1024)
                    if (bitmap == null) {
                        Toast.makeText(requireContext(), "Nie udało się wygenerować QR", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                        val sanitized = FileHelper.sanitizeFileName(currentBox?.name ?: uid.take(8))
                        val displayName = "Box_${sanitized}"
                        // Show preview bottom sheet with Save / Share / Cancel. Saving will write to
                        // Documents/ok_inv_app/Eksport QR Lokalizacja
                        showQrDialog(bitmap, displayName)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Błąd: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showQrDialog(bitmap: Bitmap, displayName: String) {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_barcode_preview, null)
        val previewImage = view.findViewById<ImageView>(R.id.previewImage)
        val previewText = view.findViewById<TextView>(R.id.previewText)
        val saveButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.saveButton)
        val shareButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.shareButton)
        val cancelButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancelButton)

        previewImage.setImageBitmap(bitmap)
        previewText.text = displayName

        var savedUri: Uri? = null

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                val uri = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    MediaStoreHelper.saveBitmap(requireContext(), bitmap, displayName, "Eksport QR Lokalizacja")
                }

                if (uri != null) {
                    savedUri = uri
                    Toast.makeText(requireContext(), "Zapisano do Documents/ok_inv_app/Eksport QR Lokalizacja", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Błąd zapisu", Toast.LENGTH_SHORT).show()
                }
            }
        }

        shareButton.setOnClickListener {
            lifecycleScope.launch {
                val localUri: Uri? = savedUri ?: withContext(kotlinx.coroutines.Dispatchers.IO) {
                    MediaStoreHelper.saveBitmap(requireContext(), bitmap, displayName, "Eksport QR Lokalizacja")
                }

                if (localUri == null) {
                    Toast.makeText(requireContext(), "Nie udało się przygotować pliku do udostępnienia", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val share = Intent(Intent.ACTION_SEND).apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    putExtra(Intent.EXTRA_STREAM, localUri as android.os.Parcelable)
                    type = "image/png"
                }
                startActivity(Intent.createChooser(share, "Udostępnij QR"))
            }
        }

        cancelButton?.setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
