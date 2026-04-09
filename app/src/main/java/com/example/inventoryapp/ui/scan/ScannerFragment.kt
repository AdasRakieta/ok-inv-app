package com.example.inventoryapp.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.databinding.FragmentBulkScanBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * ScannerFragment
 * - Uses CameraX Preview + ImageAnalysis and ML Kit Barcode Scanning
 * - Parses payloads and navigates to details or add location screens
 */
class ScannerFragment : Fragment() {

    private var _binding: FragmentBulkScanBinding? = null
    private val binding get() = _binding!!

    private val warehouseLocationRepository by lazy {
        (requireActivity().application as InventoryApplication).warehouseLocationRepository
    }
    private val boxRepository by lazy {
        (requireActivity().application as InventoryApplication).boxRepository
    }

    private var cameraExecutor: ExecutorService? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null

    // simple lock to debounce navigation after a detected QR
    private var isLocked = false
    private val unlockHandler = Handler(Looper.getMainLooper())

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showPermissionUi()
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

        binding.previewView.post {
            // Kick off permission check
            checkCameraPermissionAndStart()
        }

        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = "Point camera at barcode"
    }

    private fun checkCameraPermissionAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            startCamera()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showPermissionUi() {
        binding.previewView.visibility = View.GONE
        binding.statusText.visibility = View.VISIBLE
        binding.statusText.text = "Camera permission required. Grant to scan."
        Snackbar.make(binding.root, "Camera permission required", Snackbar.LENGTH_INDEFINITE)
            .setAction("Grant") {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            .show()
    }

    private fun startCamera() {
        binding.previewView.visibility = View.VISIBLE
        binding.statusText.visibility = View.GONE

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture?.addListener({
            val cameraProvider = cameraProviderFuture?.get()
            bindCameraUseCases(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider?) {
        try {
            cameraProvider?.unbindAll()

            val preview = Preview.Builder().build()
            val previewView: PreviewView = binding.previewView
            preview.setSurfaceProvider(previewView.surfaceProvider)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy: ImageProxy ->
                processImageProxy(scanner, imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.bindToLifecycle(
                viewLifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        } catch (e: Exception) {
            Log.e("ScannerFragment", "Camera bind failed", e)
            binding.statusText.visibility = View.VISIBLE
            binding.statusText.text = "Camera error: ${e.message}"
        }
    }

    private fun processImageProxy(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    val barcode = barcodes[0]
                    val raw = barcode.rawValue
                    if (!raw.isNullOrBlank()) {
                        handleRawValue(raw)
                    }
                }
            }
            .addOnFailureListener { /* ignore */ }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun handleRawValue(raw: String) {
        if (isLocked) return

        // parse raw payload into qrUid
        val qrUid = parsePayload(raw) ?: return

        // debounce immediately to avoid double navigation
        isLocked = true

        lifecycleScope.launch {
            try {
                // first try to resolve as Box
                val box = withContext(Dispatchers.IO) {
                    try { boxRepository.getBoxByQrUid(qrUid) } catch (_: Exception) { null }
                }

                if (box != null) {
                    val bundle = bundleOf(
                        "boxId" to box.id,
                        "qrUid" to qrUid
                    )
                    findNavController().navigate(com.example.inventoryapp.R.id.boxDetailsFragment, bundle)
                } else {
                    val loc = withContext(Dispatchers.IO) {
                        warehouseLocationRepository.getLocationByQrUid(qrUid)
                    }

                    if (loc != null) {
                        // navigate to details (pass both code and qrUid if available)
                        val bundle = bundleOf(
                            "locationName" to loc.code,
                            "qrUid" to qrUid
                        )
                        findNavController().navigate(
                            com.example.inventoryapp.R.id.warehouseLocationDetailsFragment,
                            bundle
                        )
                    } else {
                        // navigate to add location and prefill with qrUid
                        val bundle = bundleOf("locationName" to qrUid)
                        findNavController().navigate(
                            com.example.inventoryapp.R.id.addLocationFragment,
                            bundle
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ScannerFragment", "Error resolving location", e)
                Snackbar.make(binding.root, "Błąd: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                // unlock after short delay to avoid multiple navigations
                unlockHandler.postDelayed({ isLocked = false }, 1000)
            }
        }
    }

    /**
     * Parse payload: supports full deep link `invapp://location/{qrUid}`, plain qrUid,
     * or JSON object with `qrUid` or `code` fields.
     */
    private fun parsePayload(raw: String): String? {
        try {
            // deep link
            if (raw.startsWith("invapp://location/")) {
                return raw.substringAfterLast('/')
            }

            // attempt JSON parse
            if (raw.startsWith("{") && raw.endsWith("}")) {
                try {
                    val map = com.google.gson.Gson().fromJson(raw, Map::class.java)
                    val candidate = (map["qrUid"] ?: map["code"] ?: map["qruid"]) as? String
                    if (!candidate.isNullOrBlank()) return candidate
                } catch (e: Exception) {
                    // ignore JSON parse errors
                }
            }

            // fallback: raw string as uid
            return raw.trim()
        } catch (e: Exception) {
            Log.e("ScannerFragment", "parsePayload error", e)
            return null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        cameraProviderFuture?.cancel(true)
        _binding = null
    }
}
