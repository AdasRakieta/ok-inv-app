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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.BoxEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
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

    private lateinit var assignedProductsAdapter: AssignedProductsAdapter

    private var currentBox: BoxEntity? = null

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
                productRepository.updateWithHistory(updated, "Removed from box")
                Toast.makeText(requireContext(), "Produkt usunięty z pudełka", Toast.LENGTH_SHORT).show()
            }
        })

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = assignedProductsAdapter
            setHasFixedSize(false)
        }

        binding.printLabelButton.setOnClickListener { generateAndShowQr() }

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
                Toast.makeText(requireContext(), "Pudełko nie znalezione", Toast.LENGTH_SHORT).show()
                return@launch
            }

            currentBox = box
            binding.boxName.text = box.name
            binding.boxDescription.text = box.description ?: ""
            binding.boxLocation.text = box.warehouseLocationId?.toString() ?: "Brak lokalizacji"
            binding.boxCreatedDate.text = android.text.format.DateFormat.getDateFormat(requireContext()).format(java.util.Date(box.createdAt))

            // Collect products in this box
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                productRepository.getProductsByBoxId(box.id).collect { products ->
                    assignedProductsAdapter.setFullList(products)
                    val isEmpty = products.isEmpty()
                    binding.noProductsText.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    binding.productsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    binding.productsHeader.text = "${products.size} ${if (products.size==1) "product" else "products"} assigned"
                }
            }
        }
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
            .setTitle("QR pudełka")
            .setView(imageView)
            .setPositiveButton("Zamknij", null)
            .setNeutralButton("Udostępnij") { _, _ ->
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
