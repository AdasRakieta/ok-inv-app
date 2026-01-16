package com.example.inventoryapp.ui.boxes

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.data.local.entities.PrinterEntity
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.models.PrinterModel
import com.example.inventoryapp.databinding.FragmentBoxDetailsBinding
import com.example.inventoryapp.utils.BluetoothPrinterHelper
import com.example.inventoryapp.utils.PrinterSelectionHelper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment for displaying box details and printing labels.
 * Uses the same simple list layout as PackageDetailsFragment.
 */
class BoxDetailsFragment : Fragment() {

    private var _binding: FragmentBoxDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: BoxDetailsFragmentArgs by navArgs()

    private val viewModel: BoxDetailsViewModel by viewModels {
        BoxDetailsViewModelFactory(
            (requireActivity().application as InventoryApplication).boxRepository,
            args.boxId
        )
    }

    private lateinit var productsAdapter: BoxProductsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBoxDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productsAdapter = BoxProductsAdapter { product ->
            showRemoveProductDialog(product)
        }

        binding.productsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = productsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.modifyProductsButton.setOnClickListener {
            // Navigate to modify products (mass delete)
            val action = BoxDetailsFragmentDirections.actionBoxDetailsToModifyBoxProducts(args.boxId)
            findNavController().navigate(action)
        }
        
        binding.addProductsButton.setOnClickListener {
            // Navigate to product selection for boxes
            val action = BoxDetailsFragmentDirections.actionBoxDetailsToBoxProductSelection(args.boxId)
            findNavController().navigate(action)
        }
        
        binding.addBulkButton.setOnClickListener {
            // Navigate to bulk scan for boxes
            val action = BoxDetailsFragmentDirections.actionBoxDetailsToBulkBoxScan(args.boxId)
            findNavController().navigate(action)
        }
        
        binding.printLabelButton.setOnClickListener {
            printBoxLabel()
        }

        binding.testPrintButton.setOnClickListener {
            testPrinterConnection()
        }

        binding.editBoxButton.setOnClickListener {
            editBox()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.box.collect { box ->
                box?.let {
                    binding.boxName.text = it.name
                    binding.boxDescription.text = it.description ?: "No description"
                    binding.boxLocation.text = it.warehouseLocation ?: "No location"

                    val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
                    binding.boxCreatedDate.text = dateFormat.format(it.createdAt)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.productsInBox.collect { products ->
                val count = products.size
                binding.productsHeader.text = if (count == 1) {
                    "1 product assigned"
                } else {
                    "$count products assigned"
                }

                if (products.isEmpty()) {
                    binding.productsRecyclerView.visibility = View.GONE
                    binding.noProductsText.visibility = View.VISIBLE
                } else {
                    binding.productsRecyclerView.visibility = View.VISIBLE
                    binding.noProductsText.visibility = View.GONE
                    productsAdapter.submitList(products)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun showRemoveProductDialog(product: ProductEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove Product")
            .setMessage("Remove ${product.name} from this box?")
            .setPositiveButton("Remove") { _, _ ->
                viewModel.removeProductFromBox(product.id)
                Toast.makeText(requireContext(), "Product removed from box", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun printBoxLabel() {
        // Show printer selection dialog
        PrinterSelectionHelper.getDefaultOrSelectPrinter(this) { selectedPrinter ->
            printBoxLabelWithPrinter(selectedPrinter)
        }
    }

    private fun printBoxLabelWithPrinter(printer: PrinterEntity) {
        val startTime = System.currentTimeMillis()
        android.util.Log.i("BoxDetails", "═══════════════════════════════════════════════════════")
        android.util.Log.i("BoxDetails", "🎯 PRINT LABEL REQUEST")
        android.util.Log.i("BoxDetails", "Printer: ${printer.name}")
        android.util.Log.i("BoxDetails", "MAC: ${printer.macAddress}")
        android.util.Log.i("BoxDetails", "DPI: ${printer.dpi}, Size: ${printer.labelWidthMm}x${printer.labelHeightMm ?: "auto"}mm")
        android.util.Log.i("BoxDetails", "Font: ${printer.fontSize}")
        
        viewLifecycleOwner.lifecycleScope.launch {
            val box = viewModel.box.value
            val productsWithCategories = viewModel.productsWithCategories.value

            if (box == null) {
                android.util.Log.e("BoxDetails", "❌ Box data is NULL")
                Toast.makeText(requireContext(), "Box data not available", Toast.LENGTH_SHORT).show()
                return@launch
            }

            android.util.Log.d("BoxDetails", "Box: ${box.name}, Products: ${productsWithCategories.size}")

            try {
                // Generate ZPL label using printer dimensions and smart wrapping
                val genStart = System.currentTimeMillis()
                android.util.Log.d("BoxDetails", "")
                android.util.Log.d("BoxDetails", "📝 Generating ZPL content...")
                val zplContent = com.example.inventoryapp.printer.ZplContentGenerator.generateBoxLabel(
                    box = box,
                    products = productsWithCategories,
                    printer = printer
                )
                val genElapsed = System.currentTimeMillis() - genStart
                android.util.Log.d("BoxDetails", "✓ ZPL generated in ${genElapsed}ms (${zplContent.length} chars)")
                
                // Connect to printer and send ZPL
                android.util.Log.d("BoxDetails", "")
                android.util.Log.d("BoxDetails", "🔌 Attempting connection to printer...")
                val connStart = System.currentTimeMillis()
                
                // Use model-specific connection strategy
                val printerModel = PrinterModel.fromString(printer.model)
                android.util.Log.d("BoxDetails", "Using ${printerModel.displayName} connection strategy")
                val socket = BluetoothPrinterHelper.connectToPrinterWithModel(
                    requireContext(), 
                    printer.macAddress,
                    printerModel
                )
                val connElapsed = System.currentTimeMillis() - connStart
                
                if (socket == null) {
                    android.util.Log.e("BoxDetails", "❌ Connection failed after ${connElapsed}ms")
                    android.util.Log.e("BoxDetails", "═══════════════════════════════════════════════════════")
                    Toast.makeText(
                        requireContext(),
                        "❌ Failed to connect to ${printer.name}\nMAC: ${printer.macAddress}\n\nCheck:\n1. Printer is ON\n2. Bluetooth enabled\n3. In range",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                android.util.Log.d("BoxDetails", "✓ Connected in ${connElapsed}ms")

                // Send ZPL to printer
                android.util.Log.d("BoxDetails", "")
                android.util.Log.d("BoxDetails", "📤 Sending ZPL to printer...")
                val printStart = System.currentTimeMillis()
                val success = BluetoothPrinterHelper.printZpl(requireContext(), socket, zplContent)
                val printElapsed = System.currentTimeMillis() - printStart
                
                socket.close()
                socket.close()
                android.util.Log.d("BoxDetails", "Socket closed")
                
                val totalTime = System.currentTimeMillis() - startTime
                
                if (success) {
                    android.util.Log.i("BoxDetails", "")
                    android.util.Log.i("BoxDetails", "✅ PRINT COMPLETED SUCCESSFULLY")
                    android.util.Log.i("BoxDetails", "Total time: ${totalTime}ms")
                    android.util.Log.i("BoxDetails", "  - ZPL generation: ${genElapsed}ms")
                    android.util.Log.i("BoxDetails", "  - Connection: ${connElapsed}ms")
                    android.util.Log.i("BoxDetails", "  - Print: ${printElapsed}ms")
                    android.util.Log.i("BoxDetails", "═══════════════════════════════════════════════════════")
                    
                    Toast.makeText(
                        requireContext(), 
                        "✅ Label printed on ${printer.name}\n${productsWithCategories.size} products", 
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.util.Log.e("BoxDetails", "")
                    android.util.Log.e("BoxDetails", "❌ PRINT FAILED (ZPL send error)")
                    android.util.Log.e("BoxDetails", "Total time: ${totalTime}ms")
                    android.util.Log.e("BoxDetails", "═══════════════════════════════════════════════════════")
                    
                    Toast.makeText(requireContext(), "❌ Failed to print label", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                val totalTime = System.currentTimeMillis() - startTime
                android.util.Log.e("BoxDetails", "")
                android.util.Log.e("BoxDetails", "💥 EXCEPTION DURING PRINT")
                android.util.Log.e("BoxDetails", "Error: ${e.javaClass.simpleName}: ${e.message}")
                android.util.Log.e("BoxDetails", "Time before error: ${totalTime}ms")
                android.util.Log.e("BoxDetails", "Stack trace:", e)
                android.util.Log.e("BoxDetails", "═══════════════════════════════════════════════════════")
                
                Toast.makeText(requireContext(), "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun testPrinterConnection() {
        PrinterSelectionHelper.getDefaultOrSelectPrinter(this) { selectedPrinter ->
            testPrinterWithSelected(selectedPrinter)
        }
    }

    private fun testPrinterWithSelected(printer: PrinterEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Toast.makeText(requireContext(), "Testing printer: ${printer.name}", Toast.LENGTH_SHORT).show()

                // Use model-specific connection strategy
                val printerModel = PrinterModel.fromString(printer.model)
                val socket = BluetoothPrinterHelper.connectToPrinterWithModel(
                    requireContext(), 
                    printer.macAddress,
                    printerModel
                )
                if (socket != null) {
                    val success = BluetoothPrinterHelper.sendTestLabel(socket)
                    BluetoothPrinterHelper.disconnect(socket)

                    if (success) {
                        Toast.makeText(requireContext(), "✅ Test label sent! Check printer.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "❌ Test failed - check logs", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "❌ Failed to connect to ${printer.name}\nMAC: ${printer.macAddress}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Test error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun editBox() {
        val box = viewModel.box.value ?: return
        
        val action = BoxDetailsFragmentDirections.actionBoxDetailsToEditBox(box.id)
        findNavController().navigate(action)
    }
    
    private fun showAddNewProductDialog() {
        // Navigate to AddProductFragment with boxId to automatically assign product to this box
        val action = BoxDetailsFragmentDirections.actionBoxDetailsFragmentToAddProductFragment(
            boxId = args.boxId
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
