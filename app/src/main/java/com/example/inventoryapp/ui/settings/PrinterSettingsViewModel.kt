package com.example.inventoryapp.ui.settings

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.models.PrinterConfig
import com.example.inventoryapp.utils.BrotherPrinterHelper
import com.example.inventoryapp.utils.NearbyDeviceManager
import com.example.inventoryapp.utils.PrinterPreferences
import kotlinx.coroutines.launch

/**
 * ViewModel for Printer Settings screen
 * Manages printer configuration and connection testing
 */
class PrinterSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val printerPreferences = PrinterPreferences(application)
    private val nearbyDeviceManager = NearbyDeviceManager(application)

    private val _printerConfig = MutableLiveData<PrinterConfig>()
    val printerConfig: LiveData<PrinterConfig> = _printerConfig

    private val _connectionStatus = MutableLiveData<ConnectionStatus>()
    val connectionStatus: LiveData<ConnectionStatus> = _connectionStatus

    private val _nearbyPrinters = MutableLiveData<List<String>>()
    val nearbyPrinters: LiveData<List<String>> = _nearbyPrinters

    private val _testPrintResult = MutableLiveData<TestPrintResult>()
    val testPrintResult: LiveData<TestPrintResult> = _testPrintResult

    init {
        loadConfiguration()
    }

    /**
     * Load saved printer configuration
     */
    fun loadConfiguration() {
        val config = printerPreferences.loadPrinterConfig()
        _printerConfig.value = config
        
        if (config.isConfigured) {
            _connectionStatus.value = ConnectionStatus.Configured
        } else {
            _connectionStatus.value = ConnectionStatus.NotConfigured
        }
    }

    /**
     * Save printer configuration
     */
    fun saveConfiguration(config: PrinterConfig) {
        val updatedConfig = config.copy(isConfigured = config.isValid())
        printerPreferences.savePrinterConfig(updatedConfig)
        _printerConfig.value = updatedConfig
        
        if (updatedConfig.isConfigured) {
            _connectionStatus.value = ConnectionStatus.Configured
        }
    }

    /**
     * Update IP address
     */
    fun updateIpAddress(ipAddress: String) {
        _printerConfig.value = _printerConfig.value?.copy(ipAddress = ipAddress)
    }

    /**
     * Update port
     */
    fun updatePort(port: Int) {
        _printerConfig.value = _printerConfig.value?.copy(port = port)
    }

    /**
     * Update connection method
     */
    fun updateConnectionMethod(method: PrinterConfig.ConnectionMethod) {
        _printerConfig.value = _printerConfig.value?.copy(connectionMethod = method)
    }

    /**
     * Update label size
     */
    fun updateLabelSize(widthMm: Int, heightMm: Int) {
        _printerConfig.value = _printerConfig.value?.copy(
            labelWidth = widthMm,
            labelHeight = heightMm
        )
    }

    /**
     * Scan network for nearby printers
     */
    fun scanForPrinters() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Scanning

            try {
                nearbyDeviceManager.startDeviceDiscovery(
                    onDeviceFound = { device ->
                        val currentList = _nearbyPrinters.value.orEmpty().toMutableList()
                        val deviceInfo = when (device.type) {
                            com.example.inventoryapp.utils.DeviceType.NETWORK_PRINTER -> 
                                "${device.address} - ${device.name}"
                            com.example.inventoryapp.utils.DeviceType.BLUETOOTH_PRINTER -> 
                                "BT: ${device.name} (${device.address})"
                            else -> "${device.name}"
                        }
                        
                        if (!currentList.contains(deviceInfo)) {
                            currentList.add(deviceInfo)
                            _nearbyPrinters.postValue(currentList)
                        }
                    },
                    onDiscoveryComplete = { devices ->
                        _connectionStatus.postValue(
                            if (devices.isEmpty()) {
                                ConnectionStatus.ScanFailed("No printers found")
                            } else {
                                ConnectionStatus.ScanComplete
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                _connectionStatus.value = ConnectionStatus.ScanFailed(e.message ?: "Scan failed")
            }
        }
    }

    /**
     * Test printer connection
     */
    fun testConnection() {
        val config = _printerConfig.value ?: return

        if (!config.isValid()) {
            _testPrintResult.value = TestPrintResult.Error("Invalid configuration")
            return
        }

        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.Testing
            _testPrintResult.value = TestPrintResult.InProgress

            when (config.connectionMethod) {
                PrinterConfig.ConnectionMethod.WIFI -> {
                    testWifiConnection(config.ipAddress, config.port)
                }
                PrinterConfig.ConnectionMethod.BLUETOOTH -> {
                    testBluetoothConnection(config.bluetoothAddress)
                }
                PrinterConfig.ConnectionMethod.WIRELESS_DIRECT -> {
                    testWirelessDirectConnection(config.ipAddress, config.port)
                }
            }
        }
    }

    /**
     * Test WiFi connection and send test print
     */
    private suspend fun testWifiConnection(ipAddress: String, port: Int) {
        // First check if printer is reachable
        val connectionResult = BrotherPrinterHelper.checkWifiConnection(ipAddress, port)
        
        if (connectionResult.isFailure) {
            _testPrintResult.value = TestPrintResult.Error(
                "Cannot reach printer at $ipAddress:$port\n${connectionResult.exceptionOrNull()?.message}"
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
            
            printerPreferences.saveLastConnectionStatus(
                false,
                "Connection failed: ${connectionResult.exceptionOrNull()?.message}"
            )
            return
        }

        // If reachable, send test print
        val printResult = BrotherPrinterHelper.sendTestPrint(ipAddress, port)
        
        if (printResult.isSuccess) {
            _testPrintResult.value = TestPrintResult.Success("Test print sent successfully!")
            _connectionStatus.value = ConnectionStatus.TestSuccess
            
            printerPreferences.saveLastConnectionStatus(true, "Test print successful")
        } else {
            _testPrintResult.value = TestPrintResult.Error(
                "Test print failed\n${printResult.exceptionOrNull()?.message}"
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
            
            printerPreferences.saveLastConnectionStatus(
                false,
                "Test print failed: ${printResult.exceptionOrNull()?.message}"
            )
        }
    }

    /**
     * Test Bluetooth connection and send test print (NO PAIRING REQUIRED)
     */
    private suspend fun testBluetoothConnection(macAddress: String) {
        try {
            // Get Bluetooth adapter
            val bluetoothAdapter = com.example.inventoryapp.utils.getBluetoothAdapter(getApplication())
            if (bluetoothAdapter == null) {
                _testPrintResult.value = TestPrintResult.Error(
                    "Bluetooth adapter not found on this device"
                )
                _connectionStatus.value = ConnectionStatus.TestFailed
                return
            }

            if (!bluetoothAdapter.isEnabled) {
                _testPrintResult.value = TestPrintResult.Error(
                    "Bluetooth is disabled. Please enable Bluetooth and try again."
                )
                _connectionStatus.value = ConnectionStatus.TestFailed
                return
            }

            // Get device by MAC address - NO PAIRING CHECK!
            val device = BrotherPrinterHelper.getBluetoothDeviceByAddress(getApplication(), macAddress)

            if (device == null) {
                _testPrintResult.value = TestPrintResult.Error(
                    "Invalid MAC address: $macAddress\nPlease check the format (e.g. A4:34:F1:7D:4A:B8)"
                )
                _connectionStatus.value = ConnectionStatus.TestFailed
                
                printerPreferences.saveLastConnectionStatus(
                    false,
                    "Invalid MAC address"
                )
                return
            }

            // Check connection (NO PAIRING REQUIRED - uses insecure connection)
            val connectionResult = BrotherPrinterHelper.checkBluetoothConnection(device)
            
            if (connectionResult.isFailure) {
                _testPrintResult.value = TestPrintResult.Error(
                    "Cannot connect to ${device.name} ($macAddress)\n${connectionResult.exceptionOrNull()?.message}"
                )
                _connectionStatus.value = ConnectionStatus.TestFailed
                
                printerPreferences.saveLastConnectionStatus(
                    false,
                    "BT connection failed: ${connectionResult.exceptionOrNull()?.message}"
                )
                return
            }

            // Send test print
            val printResult = BrotherPrinterHelper.sendTestPrintBluetooth(device)
            
            if (printResult.isSuccess) {
                _testPrintResult.value = TestPrintResult.Success(
                    "Bluetooth test print sent to ${device.name}!"
                )
                _connectionStatus.value = ConnectionStatus.TestSuccess
                
                printerPreferences.saveLastConnectionStatus(true, "BT test print successful")
            } else {
                _testPrintResult.value = TestPrintResult.Error(
                    "Bluetooth test print failed\n${printResult.exceptionOrNull()?.message}"
                )
                _connectionStatus.value = ConnectionStatus.TestFailed
                
                printerPreferences.saveLastConnectionStatus(
                    false,
                    "BT test print failed: ${printResult.exceptionOrNull()?.message}"
                )
            }
        } catch (e: Exception) {
            _testPrintResult.value = TestPrintResult.Error(
                "Bluetooth error: ${e.message}"
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
        }
    }

    /**
     * Test Wireless Direct connection and send test print
     * Wireless Direct uses WiFi Direct protocol with a specific IP
     */
    private suspend fun testWirelessDirectConnection(ipAddress: String, port: Int) {
        // Wireless Direct is essentially WiFi with a different IP range (usually 192.168.50.x)
        if (ipAddress.isBlank()) {
            _testPrintResult.value = TestPrintResult.Error(
                "Wireless Direct IP address not configured.\nConnect to printer's WiFi Direct network first."
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
            return
        }

        // First check if printer is reachable
        val connectionResult = BrotherPrinterHelper.checkWifiConnection(ipAddress, port)
        
        if (connectionResult.isFailure) {
            _testPrintResult.value = TestPrintResult.Error(
                "Cannot reach printer via Wireless Direct at $ipAddress:$port\n" +
                "Make sure you are connected to the printer's WiFi Direct network.\n" +
                connectionResult.exceptionOrNull()?.message
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
            
            printerPreferences.saveLastConnectionStatus(
                false,
                "Wireless Direct connection failed: ${connectionResult.exceptionOrNull()?.message}"
            )
            return
        }

        // If reachable, send test print
        val printResult = BrotherPrinterHelper.sendTestPrint(ipAddress, port)
        
        if (printResult.isSuccess) {
            _testPrintResult.value = TestPrintResult.Success(
                "Wireless Direct test print sent successfully!"
            )
            _connectionStatus.value = ConnectionStatus.TestSuccess
            
            printerPreferences.saveLastConnectionStatus(true, "Wireless Direct test print successful")
        } else {
            _testPrintResult.value = TestPrintResult.Error(
                "Wireless Direct test print failed\n${printResult.exceptionOrNull()?.message}"
            )
            _connectionStatus.value = ConnectionStatus.TestFailed
            
            printerPreferences.saveLastConnectionStatus(
                false,
                "Wireless Direct test print failed: ${printResult.exceptionOrNull()?.message}"
            )
        }
    }

    /**
     * Clear configuration
     */
    fun clearConfiguration() {
        printerPreferences.clearPrinterConfig()
        loadConfiguration()
        _nearbyPrinters.value = emptyList()
    }

    /**
     * Connection status states
     */
    sealed class ConnectionStatus {
        object NotConfigured : ConnectionStatus()
        object Configured : ConnectionStatus()
        object Scanning : ConnectionStatus()
        object ScanComplete : ConnectionStatus()
        data class ScanFailed(val message: String) : ConnectionStatus()
        object Testing : ConnectionStatus()
        object TestSuccess : ConnectionStatus()
        object TestFailed : ConnectionStatus()
    }

    /**
     * Test print result states
     */
    sealed class TestPrintResult {
        object InProgress : TestPrintResult()
        data class Success(val message: String) : TestPrintResult()
        data class Error(val message: String) : TestPrintResult()
    }
}
