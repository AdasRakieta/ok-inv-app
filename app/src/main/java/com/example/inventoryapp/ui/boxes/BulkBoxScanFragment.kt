package com.example.inventoryapp.ui.boxes

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentBulkScanBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.BoxRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BulkBoxScanFragment : Fragment() {

    private var _binding: FragmentBulkScanBinding? = null
    private val binding get() = _binding!!

    private val args: BulkBoxScanFragmentArgs by navArgs()
    private lateinit var boxRepository: BoxRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var cameraExecutor: ExecutorService

    private val scannedSerials = mutableSetOf<String>()
    private val pendingProductIds = mutableListOf<Long>() // IDs of products to add to box
    
    private var currentInputField: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val database = AppDatabase.getDatabase(requireContext())
        boxRepository = BoxRepository(database.boxDao(), database.productDao(), database.packageDao())
        productRepository = ProductRepository(database.productDao())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBulkScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        updateUI()
        
        // Hide quantity controls (not needed for adding existing products)
        binding.quantityControlsLayout.visibility = View.GONE
        
        // Start with one empty input field
        addProductInputField()
    }

    private fun setupClickListeners() {
        binding.saveButton.setOnClickListener {
            saveAllProducts()
        }

        binding.cancelButton.setOnClickListener {
            cancelAll()
        }
    }
    
    private fun addProductInputField() {
        // Only reuse field if it's still enabled (not processed yet)
        if (binding.productsInputContainer.childCount > 0 && 
            currentInputField != null && 
            currentInputField?.isEnabled == true) {
            // Field already exists and is active, just clear and focus it
            currentInputField?.setText("")
            currentInputField?.requestFocus()
            return
        }
        
        val context = requireContext()
        val productNumber = pendingProductIds.size + 1

        // Create horizontal container for input field and delete button
        val horizontalContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Create TextInputLayout
        val inputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "$productNumber. Scan Serial Number"
            setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
        }

        // Create TextInputEditText
        val editText = TextInputEditText(inputLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE

            // Handle enter key
            setOnEditorActionListener { _, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                    val serialNumber = text.toString().trim()
                    processSerialNumber(serialNumber)
                    true
                } else {
                    false
                }
            }

            // Auto-detect barcode scanner input
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s.toString().trim()
                    if (text.isNotEmpty() && text.length >= 5) {
                        postDelayed({
                            if (this@apply.text.toString().trim() == text) {
                                processSerialNumber(text)
                            }
                        }, 100)
                    }
                }
            })
        }

        // Create delete button
        val deleteButton = androidx.appcompat.widget.AppCompatImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini),
                resources.getDimensionPixelSize(com.google.android.material.R.dimen.design_fab_size_mini)
            ).apply {
                marginStart = 8
            }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            contentDescription = "Delete this entry"
            background = null
            setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            setOnClickListener {
                removeProductInputField(horizontalContainer, editText)
            }
        }

        // Assemble the layout
        inputLayout.addView(editText)
        horizontalContainer.addView(inputLayout)
        horizontalContainer.addView(deleteButton)

        binding.productsInputContainer.addView(horizontalContainer)

        // Store current input field reference
        currentInputField = editText

        // Focus the new field
        editText.requestFocus()
    }

    private fun removeProductInputField(container: LinearLayout, editText: TextInputEditText) {
        val serialNumber = editText.text.toString().trim()

        // Remove from pending list
        if (serialNumber.isNotEmpty()) {
            scannedSerials.remove(serialNumber)
            // Find and remove product ID matching this SN
            viewLifecycleOwner.lifecycleScope.launch {
                val product = productRepository.getProductBySerialNumber(serialNumber)
                product?.let { pendingProductIds.remove(it.id) }
                showStatus("🗑️ Removed: $serialNumber")
                updateUI()
            }
        }

        // Remove the container from the layout
        binding.productsInputContainer.removeView(container)

        if (currentInputField == editText) {
            currentInputField = null
        }

        // If no more input fields, add a new empty one
        if (binding.productsInputContainer.childCount == 0) {
            addProductInputField()
        }
    }
    
    private fun processSerialNumber(serialNumber: String) {
        if (serialNumber.isEmpty()) return

        // Check if already in pending list
        if (scannedSerials.contains(serialNumber)) {
            showStatus("⚠️ Already in list: $serialNumber")
            currentInputField?.setText("")
            return
        }

        // Check if product exists in database
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val product = productRepository.getProductBySerialNumber(serialNumber)
                if (product == null) {
                    showStatus("❌ Product not found: $serialNumber")
                    currentInputField?.setText("")
                    return@launch
                }

                // Check if product is already in this box
                val isAlreadyInBox = boxRepository.isProductInBox(args.boxId, product.id)
                if (isAlreadyInBox) {
                    showStatus("⚠️ Already in box: ${product.name} (${serialNumber})")
                    currentInputField?.setText("")
                    return@launch
                }

                // Add to pending list
                pendingProductIds.add(product.id)
                scannedSerials.add(serialNumber)

                updateUI()
                showStatus("✅ Added: ${product.name} (${serialNumber})")
                
                // Mark current field as processed and add new one
                currentInputField?.isEnabled = false
                addProductInputField()

            } catch (e: Exception) {
                showStatus("❌ Error: ${e.message}")
                currentInputField?.setText("")
            }
        }
    }
    
    private fun saveAllProducts() {
        if (pendingProductIds.isEmpty()) {
            Toast.makeText(requireContext(), "No products to add", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                for (productId in pendingProductIds) {
                    boxRepository.addProductToBox(args.boxId, productId)
                }
                
                Toast.makeText(
                    requireContext(),
                    "✅ Added ${pendingProductIds.size} products to box!",
                    Toast.LENGTH_LONG
                ).show()
                
                findNavController().navigateUp()
                
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "❌ Error adding products: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun cancelAll() {
        pendingProductIds.clear()
        scannedSerials.clear()
        
        Toast.makeText(
            requireContext(),
            "Cancelled",
            Toast.LENGTH_SHORT
        ).show()
        
        findNavController().navigateUp()
    }

    private fun showStatus(message: String) {
        activity?.runOnUiThread {
            binding.lastScannedText.text = message
        }
    }

    private fun updateUI() {
        activity?.runOnUiThread {
            val count = pendingProductIds.size
            binding.scanCountText.text = "Products ready to add: $count"
            
            if (count == 0) {
                binding.lastScannedText.text = "Ready to scan products..."
            } else {
                // Show last 3 scanned items
                val preview = scannedSerials.toList().takeLast(3).joinToString("\n") { "• $it" }
                binding.lastScannedText.text = if (count > 3) {
                    "Last 3 of $count:\n$preview"
                } else {
                    preview
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
