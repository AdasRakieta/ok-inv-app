package com.example.inventoryapp.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.FragmentScannerBinding
import com.example.inventoryapp.data.local.database.AppDatabase
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ScanRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ScannerViewModel
    private lateinit var cameraExecutor: ExecutorService
    private var productId: Long = -1L

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.scanner_permission_required),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get productId from arguments
        arguments?.let {
            productId = it.getLong("productId", -1L)
        }
        
        // Initialize ViewModel with repositories
        val database = AppDatabase.getDatabase(requireContext())
        val productRepository = ProductRepository(database.productDao())
        val scanRepository = ScanRepository(database.scanHistoryDao())
        
        viewModel = ScannerViewModel(productRepository, scanRepository)
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()

        // Check camera permission
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupObservers() {
        viewModel.scanResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScannerViewModel.ScanState.Success -> {
                    binding.errorText.visibility = View.GONE
                    // Assign the scanned code to the product
                    if (productId != -1L) {
                        viewModel.assignSerialNumberToProduct(productId, state.code)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Scanned: ${state.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is ScannerViewModel.ScanState.Error -> {
                    binding.errorText.text = state.message
                    binding.errorText.visibility = View.VISIBLE
                    viewModel.resetScanState()
                }
                is ScannerViewModel.ScanState.Idle -> {
                    binding.errorText.visibility = View.GONE
                }
            }
        }

        viewModel.assignmentResult.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ScannerViewModel.AssignmentState.Success -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.scanner_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate back or close scanner
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                is ScannerViewModel.AssignmentState.Error -> {
                    Toast.makeText(
                        requireContext(),
                        state.message,
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetAssignmentState()
                }
                is ScannerViewModel.AssignmentState.Idle -> {
                    // Do nothing
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.cancelButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(
                        cameraExecutor,
                        BarcodeAnalyzer(
                            onBarcodeDetected = { code, format ->
                                requireActivity().runOnUiThread {
                                    viewModel.onBarcodeScanned(code, format)
                                }
                            },
                            onError = { exception ->
                                requireActivity().runOnUiThread {
                                    Toast.makeText(
                                        requireContext(),
                                        "Scan error: ${exception.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    )
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Failed to start camera: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        fun newInstance(productId: Long): ScannerFragment {
            return ScannerFragment().apply {
                arguments = Bundle().apply {
                    putLong("productId", productId)
                }
            }
        }
    }
}
