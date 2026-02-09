package com.example.inventoryapp.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.inventoryapp.R
import com.example.inventoryapp.data.models.PrinterConfig
import com.example.inventoryapp.databinding.FragmentPrinterSettingsBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Fragment for configuring Brother PT-P950NW printer settings
 * Allows users to set up WiFi, Bluetooth, or Wireless Direct connection
 */
class PrinterSettingsFragment : Fragment() {

    private var _binding: FragmentPrinterSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrinterSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrinterSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupConnectionMethodRadioGroup()
        setupLabelSizeSpinner()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupConnectionMethodRadioGroup() {
        binding.radioGroupConnectionMethod.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioWifi -> {
                    viewModel.updateConnectionMethod(PrinterConfig.ConnectionMethod.WIFI)
                    showWifiFields()
                }
                R.id.radioBluetooth -> {
                    viewModel.updateConnectionMethod(PrinterConfig.ConnectionMethod.BLUETOOTH)
                    showBluetoothFields()
                }
                R.id.radioWirelessDirect -> {
                    viewModel.updateConnectionMethod(PrinterConfig.ConnectionMethod.WIRELESS_DIRECT)
                    showWirelessDirectFields()
                }
            }
        }
    }

    private fun setupLabelSizeSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            PrinterConfig.LABEL_SIZE_NAMES
        ).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        binding.spinnerLabelSize.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.buttonScanNetwork.setOnClickListener {
            viewModel.scanForPrinters()
        }

        binding.buttonTestPrint.setOnClickListener {
            viewModel.testConnection()
        }

        binding.buttonSave.setOnClickListener {
            saveConfiguration()
        }

        binding.buttonClear.setOnClickListener {
            viewModel.clearConfiguration()
            Toast.makeText(context, "Configuration cleared", Toast.LENGTH_SHORT).show()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.printerConfig.observe(viewLifecycleOwner) { config ->
            updateUIFromConfig(config)
        }

        viewModel.connectionStatus.observe(viewLifecycleOwner) { status ->
            handleConnectionStatus(status)
        }

        viewModel.nearbyPrinters.observe(viewLifecycleOwner) { printers ->
            displayNearbyPrinters(printers)
        }

        viewModel.testPrintResult.observe(viewLifecycleOwner) { result ->
            handleTestPrintResult(result)
        }
    }

    private fun updateUIFromConfig(config: PrinterConfig) {
        // Set connection method
        when (config.connectionMethod) {
            PrinterConfig.ConnectionMethod.WIFI -> {
                binding.radioWifi.isChecked = true
                showWifiFields()
            }
            PrinterConfig.ConnectionMethod.BLUETOOTH -> {
                binding.radioBluetooth.isChecked = true
                showBluetoothFields()
            }
            PrinterConfig.ConnectionMethod.WIRELESS_DIRECT -> {
                binding.radioWirelessDirect.isChecked = true
                showWirelessDirectFields()
            }
        }

        // Set values
        binding.editTextIpAddress.setText(config.ipAddress)
        binding.editTextPort.setText(config.port.toString())
        binding.bluetoothMacInput.setText(config.bluetoothAddress)

        // Set label size
        val sizeIndex = PrinterConfig.LABEL_SIZES.indexOfFirst {
            it.first == config.labelWidth && it.second == config.labelHeight
        }
        if (sizeIndex >= 0) {
            binding.spinnerLabelSize.setSelection(sizeIndex)
        }

        // Update status
        if (config.isConfigured) {
            binding.textViewStatus.text = "Printer configured"
            binding.textViewStatus.setTextColor(
                resources.getColor(android.R.color.holo_green_dark, null)
            )
        } else {
            binding.textViewStatus.text = "Not configured"
            binding.textViewStatus.setTextColor(
                resources.getColor(android.R.color.holo_orange_dark, null)
            )
        }
    }

    private fun showWifiFields() {
        binding.layoutWifiSettings.isVisible = true
        binding.layoutBluetoothSettings.isVisible = false
        binding.buttonScanNetwork.isVisible = true
    }

    private fun showBluetoothFields() {
        binding.layoutWifiSettings.isVisible = false
        binding.layoutBluetoothSettings.isVisible = true
        binding.buttonScanNetwork.isVisible = false
    }

    private fun showWirelessDirectFields() {
        binding.layoutWifiSettings.isVisible = true
        binding.layoutBluetoothSettings.isVisible = false
        binding.buttonScanNetwork.isVisible = false
    }

    private fun handleConnectionStatus(status: PrinterSettingsViewModel.ConnectionStatus) {
        when (status) {
            is PrinterSettingsViewModel.ConnectionStatus.Scanning -> {
                binding.progressBar.isVisible = true
                binding.textViewScanStatus.text = "Scanning network..."
                binding.textViewScanStatus.isVisible = true
                binding.buttonScanNetwork.isEnabled = false
            }
            is PrinterSettingsViewModel.ConnectionStatus.ScanComplete -> {
                binding.progressBar.isVisible = false
                binding.textViewScanStatus.text = "Scan complete"
                binding.buttonScanNetwork.isEnabled = true
            }
            is PrinterSettingsViewModel.ConnectionStatus.ScanFailed -> {
                binding.progressBar.isVisible = false
                binding.textViewScanStatus.text = "Scan failed: ${status.message}"
                binding.buttonScanNetwork.isEnabled = true
            }
            is PrinterSettingsViewModel.ConnectionStatus.Testing -> {
                binding.progressBar.isVisible = true
                binding.buttonTestPrint.isEnabled = false
            }
            is PrinterSettingsViewModel.ConnectionStatus.TestSuccess -> {
                binding.progressBar.isVisible = false
                binding.buttonTestPrint.isEnabled = true
                Snackbar.make(binding.root, "Test print successful!", Snackbar.LENGTH_LONG).show()
            }
            is PrinterSettingsViewModel.ConnectionStatus.TestFailed -> {
                binding.progressBar.isVisible = false
                binding.buttonTestPrint.isEnabled = true
            }
            else -> {
                binding.progressBar.isVisible = false
                binding.textViewScanStatus.isVisible = false
                binding.buttonScanNetwork.isEnabled = true
                binding.buttonTestPrint.isEnabled = true
            }
        }
    }

    private fun displayNearbyPrinters(printers: List<String>) {
        if (printers.isEmpty()) {
            binding.textViewNearbyPrinters.isVisible = false
            return
        }

        binding.textViewNearbyPrinters.isVisible = true
        binding.textViewNearbyPrinters.text = "Found printers:\n${printers.joinToString("\n")}"
    }

    private fun handleTestPrintResult(result: PrinterSettingsViewModel.TestPrintResult) {
        when (result) {
            is PrinterSettingsViewModel.TestPrintResult.InProgress -> {
                // Already handled by connection status
            }
            is PrinterSettingsViewModel.TestPrintResult.Success -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
            is PrinterSettingsViewModel.TestPrintResult.Error -> {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveConfiguration() {
        val ipAddress = binding.editTextIpAddress.text.toString().trim()
        val portText = binding.editTextPort.text.toString().trim()
        val port = portText.toIntOrNull() ?: 9100
        val bluetoothMac = binding.bluetoothMacInput.text.toString().trim().uppercase()

        val selectedLabelSize = binding.spinnerLabelSize.selectedItemPosition
        val labelSize = if (selectedLabelSize >= 0 && selectedLabelSize < PrinterConfig.LABEL_SIZES.size) {
            PrinterConfig.LABEL_SIZES[selectedLabelSize]
        } else {
            PrinterConfig.LABEL_SIZES[0]
        }

        val currentConfig = viewModel.printerConfig.value ?: return

        val newConfig = currentConfig.copy(
            ipAddress = ipAddress,
            port = port,
            bluetoothAddress = bluetoothMac,
            labelWidth = labelSize.first,
            labelHeight = labelSize.second
        )

        if (!newConfig.isValid()) {
            Toast.makeText(
                context,
                "Invalid configuration. Please check your settings.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        viewModel.saveConfiguration(newConfig)
        Toast.makeText(context, "Configuration saved", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
