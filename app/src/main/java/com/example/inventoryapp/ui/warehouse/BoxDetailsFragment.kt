package com.example.inventoryapp.ui.warehouse

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
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
import com.example.inventoryapp.data.local.entities.ProductStatus
import com.example.inventoryapp.databinding.FragmentBoxDetailsBinding
import com.example.inventoryapp.ui.employees.AssignedProductsAdapter
import com.example.inventoryapp.utils.MediaStoreHelper
import com.example.inventoryapp.utils.QRCodeGenerator
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
        binding.addProductsButton.setOnClickListener { showAddProductsDialog() }
        binding.addBulkButton.setOnClickListener { showAddProductsDialog() }
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

    private fun showAddProductsDialog() {
        val box = currentBox ?: return
        lifecycleScope.launch {
            val availableProducts = productRepository.getAllProducts().firstOrNull().orEmpty()
                .filter { it.boxId == null && it.status == ProductStatus.IN_STOCK }
                .sortedBy { it.name.lowercase() }

            if (availableProducts.isEmpty()) {
                Toast.makeText(requireContext(), "Brak dostępnych produktów do dodania", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val labels = availableProducts.map { product ->
                "${product.name} • ${product.serialNumber}"
            }.toTypedArray()
            val checked = BooleanArray(labels.size)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Dodaj produkty do kartonu")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                    checked[which] = isChecked
                }
                .setNegativeButton(getString(R.string.cancel_pl), null)
                .setPositiveButton(getString(R.string.add)) { _, _ ->
                    val selected = availableProducts.filterIndexed { index, _ -> checked[index] }
                    if (selected.isEmpty()) return@setPositiveButton

                    lifecycleScope.launch {
                        selected.forEach { product ->
                            val updated = product.copy(boxId = box.id)
                            productRepository.updateWithHistory(updated, "Dodano do kartonu ${box.name}")
                        }
                        Toast.makeText(requireContext(), "Dodano ${selected.size} produktów", Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
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

                    showQrDialog(bitmap)

                    val displayName = "box_$uid"
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
            .setTitle(getString(R.string.box_qr_title))
            .setView(imageView)
            .setPositiveButton(getString(R.string.box_qr_close), null)
            .setNeutralButton(getString(R.string.qr_share)) { _, _ ->
                val uri = MediaStoreHelper.saveBitmap(requireContext(), bitmap, "box_${System.currentTimeMillis()}")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
