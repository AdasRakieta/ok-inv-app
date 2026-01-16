package com.example.inventoryapp.ui.packages

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
import com.example.inventoryapp.data.repository.PackageRepository
import com.example.inventoryapp.data.repository.ProductRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BulkPackageScanFragment : Fragment() {

    private var _binding: FragmentBulkScanBinding? = null
    private val binding get() = _binding!!

    private val args: BulkPackageScanFragmentArgs by navArgs()
    private lateinit var packageRepository: PackageRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var cameraExecutor: ExecutorService

    private val scannedSerials = mutableSetOf<String>()
    private val pendingProductIds = mutableListOf<Long>()
    
    private var currentInputField: TextInputEditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val database = AppDatabase.getDatabase(requireContext())
        packageRepository = PackageRepository(database.packageDao(), database.productDao(), database.boxDao())
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
        
        // Hide quantity controls
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

        val inputLayout = TextInputLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            hint = "$productNumber. Scan Serial Number"
            setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE)
        }

        val editText = TextInputEditText(inputLayout.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            maxLines = 1
            imeOptions = EditorInfo.IME_ACTION_DONE

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

        inputLayout.addView(editText)
        horizontalContainer.addView(inputLayout)
        horizontalContainer.addView(deleteButton)

        binding.productsInputContainer.addView(horizontalContainer)

        currentInputField = editText
        editText.requestFocus()
    }

    private fun removeProductInputField(container: LinearLayout, editText: TextInputEditText) {
        val serialNumber = editText.text.toString().trim()

        if (serialNumber.isNotEmpty()) {
            scannedSerials.remove(serialNumber)
            viewLifecycleOwner.lifecycleScope.launch {
                val product = productRepository.getProductBySerialNumber(serialNumber)
                product?.let { pendingProductIds.remove(it.id) }
                showStatus("🗑️ Removed: $serialNumber")
                updateUI()
            }
        }

        binding.productsInputContainer.removeView(container)

        if (currentInputField == editText) {
            currentInputField = null
        }

        if (binding.productsInputContainer.childCount == 0) {
            addProductInputField()
        }
    }
    
    private fun processSerialNumber(serialNumber: String) {
        if (serialNumber.isEmpty()) return

        if (scannedSerials.contains(serialNumber)) {
            showStatus("⚠️ Already in list: $serialNumber")
            currentInputField?.setText("")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val product = productRepository.getProductBySerialNumber(serialNumber)
                if (product == null) {
                    showStatus("❌ Product not found: $serialNumber")
                    currentInputField?.setText("")
                    return@launch
                }

                val isAlreadyInPackage = packageRepository.isProductInPackage(args.packageId, product.id)
                if (isAlreadyInPackage) {
                    showStatus("⚠️ Already in package: ${product.name} (${serialNumber})")
                    currentInputField?.setText("")
                    return@launch
                }

                pendingProductIds.add(product.id)
                scannedSerials.add(serialNumber)

                updateUI()
                showStatus("✅ Added: ${product.name} (${serialNumber})")
                
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
                    packageRepository.addProductToPackage(args.packageId, productId)
                }
                
                Toast.makeText(
                    requireContext(),
                    "✅ Added ${pendingProductIds.size} products to package!",
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
