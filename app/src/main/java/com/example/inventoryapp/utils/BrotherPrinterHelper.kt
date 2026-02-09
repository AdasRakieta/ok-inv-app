package com.example.inventoryapp.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

/**
 * Helper class for printing labels to Brother PT-P950NW printer
 * Supports WiFi and Bluetooth connectivity with ESC/POS command protocol
 */
object BrotherPrinterHelper {

    private const val TAG = "BrotherPrinterHelper"
    private const val WIFI_TIMEOUT_MS = 5000
    private const val BLUETOOTH_TIMEOUT_MS = 10000
    private const val RETRY_COUNT = 2
    private const val RETRY_DELAY_MS = 1000L

    // Standard SPP UUID for Bluetooth Serial Port Profile
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /**
     * Print label data over WiFi connection
     * @param ipAddress Printer IP address
     * @param port Printer port (default 9100)
     * @param labelData ESC/POS formatted label data
     * @return Result indicating success or error
     */
    suspend fun printLabelOverWifi(
        ipAddress: String,
        port: Int = 9100,
        labelData: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting WiFi print to $ipAddress:$port")
        
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < RETRY_COUNT) {
            attempt++
            Log.d(TAG, "WiFi print attempt $attempt/$RETRY_COUNT")

            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ipAddress, port), WIFI_TIMEOUT_MS)
                
                if (!socket.isConnected) {
                    throw IOException("Failed to connect to printer")
                }

                Log.d(TAG, "WiFi socket connected successfully")
                
                // Log data details for debugging
                val dataPreview = labelData.take(20).joinToString(" ") { 
                    String.format("%02X", it) 
                }
                Log.d(TAG, "Sending ${labelData.size} bytes to printer")
                Log.d(TAG, "Data header (first 20 bytes): $dataPreview")
                
                val outputStream = socket.getOutputStream()
                outputStream.write(labelData)
                outputStream.flush()
                
                Log.d(TAG, "Label data sent and flushed successfully")
                
                // Wait for printer to process
                Thread.sleep(1500)  // Increased wait time
                
                socket.close()
                Log.i(TAG, "WiFi print completed")
                
                return@withContext Result.success(true)

            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "WiFi print attempt $attempt failed: ${e.message}", e)
                
                if (attempt < RETRY_COUNT) {
                    Thread.sleep(RETRY_DELAY_MS)
                }
            }
        }

        val errorMessage = "WiFi print failed after $RETRY_COUNT attempts: ${lastException?.message}"
        Log.e(TAG, errorMessage)
        Result.failure(lastException ?: IOException(errorMessage))
    }

    /**
     * Print label data over Bluetooth connection (NO PAIRING REQUIRED)
     * Uses insecure RFCOMM connection for direct printing without authentication
     * @param device Bluetooth device (can be unpaired)
     * @param labelData Brother P-touch formatted label data
     * @return Result indicating success or error
     */
    suspend fun printLabelOverBluetooth(
        device: BluetoothDevice,
        labelData: ByteArray
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Attempting Bluetooth print to ${device.name ?: "Unknown"} (${device.address})")
        
        var bluetoothSocket: BluetoothSocket? = null
        var attempt = 0
        var lastException: Exception? = null

        while (attempt < RETRY_COUNT) {
            attempt++
            Log.d(TAG, "Bluetooth print attempt $attempt/$RETRY_COUNT")

            try {
                // Create INSECURE RFCOMM socket - NO PAIRING REQUIRED!
                bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                
                if (bluetoothSocket == null) {
                    throw IOException("Failed to create Bluetooth socket")
                }

                Log.d(TAG, "Connecting to Bluetooth device (insecure mode - no pairing)...")
                bluetoothSocket.connect()
                
                if (!bluetoothSocket.isConnected) {
                    throw IOException("Bluetooth socket not connected")
                }

                Log.d(TAG, "Bluetooth connected successfully (no pairing required)")
                
                // Log data details for debugging
                val dataPreview = labelData.take(20).joinToString(" ") { 
                    String.format("%02X", it) 
                }
                Log.d(TAG, "Sending ${labelData.size} bytes to printer")
                Log.d(TAG, "Data header (first 20 bytes): $dataPreview")
                
                val outputStream: OutputStream = bluetoothSocket.outputStream
                outputStream.write(labelData)
                outputStream.flush()
                
                Log.d(TAG, "Label data sent and flushed successfully")
                
                // Wait for printer to process
                Thread.sleep(2000)  // Increased wait time for processing
                
                bluetoothSocket.close()
                Log.i(TAG, "Bluetooth print completed (insecure connection)")
                
                return@withContext Result.success(true)

            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Bluetooth print attempt $attempt failed: ${e.message}", e)
                
                try {
                    bluetoothSocket?.close()
                } catch (closeException: Exception) {
                    Log.w(TAG, "Error closing Bluetooth socket: ${closeException.message}")
                }
                
                if (attempt < RETRY_COUNT) {
                    Thread.sleep(RETRY_DELAY_MS * 2)  // Longer delay for Bluetooth
                }
            }
        }

        val errorMessage = "Bluetooth print failed after $RETRY_COUNT attempts: ${lastException?.message}"
        Log.e(TAG, errorMessage)
        Result.failure(lastException ?: IOException(errorMessage))
    }

    /**
     * Print label with serial number using WiFi
     * Generates barcode and formats label automatically
     * Default: 5cm (50mm) label length for SN codes
     */
    suspend fun printSerialNumberLabelWifi(
        serialNumber: String,
        ipAddress: String,
        port: Int = 9100,
        tapeWidthMm: Int = 29,
        labelLengthMm: Int = 50
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Printing SN label via WiFi: $serialNumber")
        
        try {
            // Generate barcode
            val barcodeSize = BrotherLabelFormatter.calculateBarcodeSize(
                tapeWidthMm,
                labelLengthMm
            )
            
            val barcodeBitmap = BarcodeGenerator.generateCode128(
                serialNumber,
                barcodeSize.first,
                barcodeSize.second
            ) ?: return@withContext Result.failure(
                IllegalStateException("Failed to generate barcode for: $serialNumber")
            )

            Log.d(TAG, "Barcode generated: ${barcodeSize.first}x${barcodeSize.second}px")
            
            // Format label data
            val labelData = BrotherLabelFormatter.formatLabelData(
                serialNumber,
                barcodeBitmap,
                tapeWidthMm,
                labelLengthMm
            )

            Log.d(TAG, "Label data formatted (${labelData.size} bytes)")
            
            // Print
            printLabelOverWifi(ipAddress, port, labelData)

        } catch (e: Exception) {
            Log.e(TAG, "Error printing SN label via WiFi: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Print label with serial number using Bluetooth
     * Generates barcode and formats label automatically
     * Default: 5cm (50mm) label length for SN codes
     */
    suspend fun printSerialNumberLabelBluetooth(
        serialNumber: String,
        device: BluetoothDevice,
        tapeWidthMm: Int = 29,
        labelLengthMm: Int = 50
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Printing SN label via Bluetooth: $serialNumber")
        
        try {
            // Generate barcode
            val barcodeSize = BrotherLabelFormatter.calculateBarcodeSize(
                tapeWidthMm,
                labelLengthMm
            )
            
            val barcodeBitmap = BarcodeGenerator.generateCode128(
                serialNumber,
                barcodeSize.first,
                barcodeSize.second
            ) ?: return@withContext Result.failure(
                IllegalStateException("Failed to generate barcode for: $serialNumber")
            )

            Log.d(TAG, "Barcode generated: ${barcodeSize.first}x${barcodeSize.second}px")
            
            // Format label data
            val labelData = BrotherLabelFormatter.formatLabelData(
                serialNumber,
                barcodeBitmap,
                tapeWidthMm,
                labelLengthMm
            )

            Log.d(TAG, "Label data formatted (${labelData.size} bytes)")
            
            // Print
            printLabelOverBluetooth(device, labelData)

        } catch (e: Exception) {
            Log.e(TAG, "Error printing SN label via Bluetooth: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send test print to verify printer connectivity via WiFi
     */
    suspend fun sendTestPrint(
        ipAddress: String,
        port: Int = 9100
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sending test print to $ipAddress:$port")
        
        try {
            val testData = BrotherLabelFormatter.createTestLabel("Brother PT-P950NW Test")
            printLabelOverWifi(ipAddress, port, testData)
        } catch (e: Exception) {
            Log.e(TAG, "Test print failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Send test print to verify printer connectivity via Bluetooth
     */
    suspend fun sendTestPrintBluetooth(
        device: BluetoothDevice
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Sending test print via Bluetooth to ${device.name} (${device.address})")
        
        try {
            val testData = BrotherLabelFormatter.createTestLabel("Brother BT Test")
            printLabelOverBluetooth(device, testData)
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth test print failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if Bluetooth device is reachable (NO PAIRING REQUIRED)
     * Uses insecure connection for quick connectivity check
     */
    suspend fun checkBluetoothConnection(
        device: BluetoothDevice
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking Bluetooth connection to ${device.name ?: "Unknown"} (${device.address})")
        
        var bluetoothSocket: BluetoothSocket? = null
        
        try {
            // Use INSECURE socket - no pairing needed
            bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
            
            if (bluetoothSocket == null) {
                return@withContext Result.failure(IOException("Failed to create Bluetooth socket"))
            }

            Log.d(TAG, "Connecting to Bluetooth device (insecure mode - no pairing)...")
            bluetoothSocket.connect()
            
            val isConnected = bluetoothSocket.isConnected
            
            if (isConnected) {
                Log.i(TAG, "Bluetooth connection successful (no pairing required)")
                Result.success(true)
            } else {
                Log.w(TAG, "Bluetooth connection failed")
                Result.failure(IOException("Unable to connect to Bluetooth device"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth connection check failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                bluetoothSocket?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing Bluetooth socket: ${e.message}")
            }
        }
    }

    /**
     * Get Bluetooth device by MAC address (NO PAIRING REQUIRED)
     * Creates BluetoothDevice object directly from MAC address
     * @param macAddress Device MAC address (e.g. "A4:34:F1:7D:4A:B8")
     * @return BluetoothDevice or null if Bluetooth not available
     */
    fun getBluetoothDeviceByAddress(macAddress: String): BluetoothDevice? {
        try {
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                Log.e(TAG, "Bluetooth adapter not available or disabled")
                return null
            }

            // Validate MAC address format
            val macPattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
            if (!macPattern.matches(macAddress)) {
                Log.e(TAG, "Invalid MAC address format: $macAddress")
                return null
            }

            // Convert to uppercase with colons (standard format)
            val normalizedMac = macAddress.uppercase().replace("-", ":")
            
            // Get remote device - NO PAIRING REQUIRED!
            val device = bluetoothAdapter.getRemoteDevice(normalizedMac)
            Log.d(TAG, "Created BluetoothDevice for MAC: $normalizedMac (no pairing check)")
            
            return device
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bluetooth device by address: ${e.message}", e)
            return null
        }
    }

    /**
     * Check if printer is reachable over WiFi
     */
    suspend fun checkWifiConnection(
        ipAddress: String,
        port: Int = 9100,
        timeoutMs: Int = 3000
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking WiFi connection to $ipAddress:$port")
        
        try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
            
            val isConnected = socket.isConnected
            socket.close()
            
            if (isConnected) {
                Log.i(TAG, "WiFi connection successful")
                Result.success(true)
            } else {
                Log.w(TAG, "WiFi connection failed")
                Result.failure(IOException("Unable to connect"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "WiFi connection check failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
