package com.example.inventoryapp.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.inventoryapp.data.models.PrinterModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.OutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Helper class for printing QR codes via Bluetooth thermal printers
 * Supports ESC/POS command protocol
 * Logs Bluetooth operations to /Documents/inventory/logs/bluetooth_YYYY-MM-DD.txt
 * Supports multiple printer models with optimized connection strategies
 */
class BluetoothPrinterHelper {

    companion object {
        private const val TAG = "BluetoothPrinter"
        
        // Standard UUID for Bluetooth Serial Port Profile (SPP)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        
        // ESC/POS commands
        private val ESC_INIT = byteArrayOf(0x1B, 0x40) // Initialize printer
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Center alignment
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00) // Left alignment
        private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00) // Cut paper
        private val LINE_FEED = byteArrayOf(0x0A) // New line
        
        // File logging
        private var logFile: File? = null
        private var logWriter: PrintWriter? = null
        private val logDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        
        /**
         * Initialize file logging for Bluetooth operations
         * Creates logs directory in Documents/inventory/logs and daily log file
         */
        private fun initFileLogging(context: Context) {
            try {
                // Use same directory as AppLogger: /Documents/inventory/logs
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val logsDir = File(documentsDir, "inventory/logs")
                if (!logsDir.exists()) {
                    logsDir.mkdirs()
                }
                
                // Create daily Bluetooth log file
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val dateStr = dateFormat.format(Date())
                logFile = File(logsDir, "bluetooth_$dateStr.txt")
                
                // Open writer in append mode
                logWriter = PrintWriter(FileWriter(logFile, true), true)
                
                Log.d(TAG, "Bluetooth file logging initialized: ${logFile?.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Bluetooth file logging", e)
            }
        }
        
        /**
         * Write log entry to both Logcat and file
         */
        private fun logToFile(context: Context?, level: String, message: String) {
            try {
                context?.let {
                    if (logWriter == null || logFile == null) {
                        initFileLogging(it)
                    }
                }
                
                val timestamp = logDateFormat.format(Date())
                val logEntry = "[$timestamp] [$level] $message"
                
                logWriter?.println(logEntry)
                logWriter?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to log file", e)
            }
        }
        
        /**
         * Close file logging
         */
        private fun closeFileLogging() {
            try {
                logWriter?.flush()
                logWriter?.close()
                logWriter = null
                logFile = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to close file logging", e)
            }
        }
        
        /**
         * Scan for paired Bluetooth devices
         * Returns list of printer-like devices (based on name patterns)
         * Note: BLUETOOTH and BLUETOOTH_ADMIN are normal permissions on API ≤30 (auto-granted at install)
         */
        suspend fun scanPrinters(context: Context): List<BluetoothDevice> = withContext(Dispatchers.IO) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    Log.w(TAG, "Bluetooth not available or not enabled")
                    return@withContext emptyList()
                }
                
                @Suppress("MissingPermission")
                val pairedDevices = bluetoothAdapter.bondedDevices
                // Filter devices that might be printers
                pairedDevices.filter { device ->
                    @Suppress("MissingPermission")
                    val name = device.name?.toLowerCase() ?: ""
                    name.contains("printer") || 
                    name.contains("print") || 
                    name.contains("pos") ||
                    name.contains("thermal")
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception - missing Bluetooth permissions", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning for printers", e)
                emptyList()
            }
        }
        
        /**
         * Connect to printer by MAC address with model-specific strategy
         * @param context Application context
         * @param macAddress MAC address of the printer
         * @param printerModel Printer model enum for connection strategy
         * @return BluetoothSocket or null if connection fails
         */
        suspend fun connectToPrinterWithModel(
            context: Context, 
            macAddress: String,
            printerModel: PrinterModel
        ): BluetoothSocket? = withContext(Dispatchers.IO) {
            initFileLogging(context)
            
            logToFile(context, "INFO", "Attempting connection to ${printerModel.displayName} at $macAddress")
            Log.d(TAG, "Connecting to ${printerModel.displayName} at $macAddress")
            
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                    logToFile(context, "WARN", "Bluetooth not available or not enabled")
                    Log.w(TAG, "Bluetooth not available or not enabled")
                    return@withContext null
                }
                
                val device = bluetoothAdapter.getRemoteDevice(macAddress)
                
                // Cancel discovery to improve connection speed
                @Suppress("MissingPermission")
                bluetoothAdapter.cancelDiscovery()
                
                logToFile(context, "INFO", "Trying model-specific connection for ${printerModel.name}")
                
                // Use connection strategy based on printer model
                val socket = when (printerModel) {
                    PrinterModel.ZD421, PrinterModel.ZD621 -> {
                        // ZD421/ZD621: Try secure connection first (supports pairing with PIN/Numeric Comparison)
                        connectWithZD421Strategy(context, device)
                    }
                    PrinterModel.ZQ310_PLUS, PrinterModel.OTHER_ZEBRA -> {
                        // ZQ310 Plus: Works with insecure connection
                        connectWithZQ310Strategy(context, device)
                    }
                    PrinterModel.GENERIC_ESC_POS -> {
                        // Generic: Try standard SPP first, then fallbacks
                        connectWithGenericStrategy(context, device)
                    }
                }
                
                if (socket != null) {
                    @Suppress("MissingPermission")
                    logToFile(context, "INFO", "✅ Connected to ${device.name} using ${printerModel.name} strategy")
                    Log.d(TAG, "✅ Connected successfully")
                } else {
                    logToFile(context, "ERROR", "❌ All connection attempts failed for ${printerModel.name}")
                    Log.e(TAG, "❌ Connection failed")
                }
                
                socket
            } catch (e: SecurityException) {
                logToFile(context, "ERROR", "Security exception - missing Bluetooth permissions: ${e.message}")
                Log.e(TAG, "Security exception - missing Bluetooth permissions", e)
                null
            } catch (e: Exception) {
                logToFile(context, "ERROR", "Error connecting to printer: ${e.message}")
                Log.e(TAG, "Error connecting to printer", e)
                null
            }
        }
        
        /**
         * Connection strategy for ZD421/ZD621 printers
         * These printers may require secure pairing with PIN or Numeric Comparison
         */
        private fun connectWithZD421Strategy(context: Context, device: BluetoothDevice): BluetoothSocket? {
            var socket: BluetoothSocket? = null
            
            // Method 1: Try secure (encrypted) RFCOMM connection first
            try {
                @Suppress("MissingPermission")
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "ZD421: Connected via secure RFCOMM (${device.name})")
                Log.d(TAG, "ZD421: Connected via secure RFCOMM")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "WARN", "ZD421: Secure RFCOMM failed: ${e.message}")
                Log.w(TAG, "ZD421: Secure RFCOMM failed, trying insecure", e)
            }
            
            // Method 2: Try insecure RFCOMM connection (fallback)
            try {
                @Suppress("MissingPermission")
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "ZD421: Connected via insecure RFCOMM (${device.name})")
                Log.d(TAG, "ZD421: Connected via insecure RFCOMM")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "WARN", "ZD421: Insecure RFCOMM failed: ${e.message}")
                Log.w(TAG, "ZD421: Insecure RFCOMM failed, trying reflection", e)
            }
            
            // Method 3: Reflection-based connection (last resort)
            try {
                @Suppress("MissingPermission")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = method.invoke(device, 1) as BluetoothSocket
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "ZD421: Connected via reflection (${device.name})")
                Log.d(TAG, "ZD421: Connected via reflection")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "ERROR", "ZD421: All connection methods failed: ${e.message}")
                Log.e(TAG, "ZD421: All connection methods failed", e)
            }
            
            return null
        }
        
        /**
         * Connection strategy for ZQ310 Plus
         * Works well with insecure connection
         */
        private fun connectWithZQ310Strategy(context: Context, device: BluetoothDevice): BluetoothSocket? {
            var socket: BluetoothSocket? = null
            
            // Method 1: Try insecure RFCOMM connection (preferred for ZQ310)
            try {
                @Suppress("MissingPermission")
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "ZQ310: Connected via insecure RFCOMM (${device.name})")
                Log.d(TAG, "ZQ310: Connected via insecure RFCOMM")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "WARN", "ZQ310: Insecure RFCOMM failed: ${e.message}")
                Log.w(TAG, "ZQ310: Insecure RFCOMM failed, trying reflection", e)
            }
            
            // Method 2: Reflection-based connection (fallback)
            try {
                @Suppress("MissingPermission")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = method.invoke(device, 1) as BluetoothSocket
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "ZQ310: Connected via reflection (${device.name})")
                Log.d(TAG, "ZQ310: Connected via reflection")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "ERROR", "ZQ310: All connection methods failed: ${e.message}")
                Log.e(TAG, "ZQ310: All connection methods failed", e)
            }
            
            return null
        }
        
        /**
         * Generic connection strategy for ESC/POS printers
         * Tries all methods in sequence
         */
        private fun connectWithGenericStrategy(context: Context, device: BluetoothDevice): BluetoothSocket? {
            var socket: BluetoothSocket? = null
            
            // Method 1: Standard SPP connection
            try {
                @Suppress("MissingPermission")
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "Generic: Connected via SPP (${device.name})")
                Log.d(TAG, "Generic: Connected via SPP")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "WARN", "Generic: SPP failed: ${e.message}")
                Log.w(TAG, "Generic: SPP failed, trying insecure", e)
            }
            
            // Method 2: Insecure RFCOMM connection
            try {
                @Suppress("MissingPermission")
                socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "Generic: Connected via insecure RFCOMM (${device.name})")
                Log.d(TAG, "Generic: Connected via insecure RFCOMM")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "WARN", "Generic: Insecure RFCOMM failed: ${e.message}")
                Log.w(TAG, "Generic: Insecure RFCOMM failed, trying reflection", e)
            }
            
            // Method 3: Reflection-based connection
            try {
                @Suppress("MissingPermission")
                val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                socket = method.invoke(device, 1) as BluetoothSocket
                socket.connect()
                @Suppress("MissingPermission")
                logToFile(context, "INFO", "Generic: Connected via reflection (${device.name})")
                Log.d(TAG, "Generic: Connected via reflection")
                return socket
            } catch (e: Exception) {
                socket?.close()
                logToFile(context, "ERROR", "Generic: All connection methods failed: ${e.message}")
                Log.e(TAG, "Generic: All connection methods failed", e)
            }
            
            return null
        }
        
        /**
         * Connect to printer by MAC address scanned from QR code
         * Enhanced with fallback mechanisms for Zebra printers (e.g., ZQ310 Plus)
         * Note: BLUETOOTH and BLUETOOTH_ADMIN are normal permissions on API ≤30 (auto-granted at install)
         * @deprecated Use connectToPrinterWithModel for better compatibility
         */
    suspend fun connectToPrinter(context: Context, macAddress: String): BluetoothSocket? = withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            // Initialize file logging
            initFileLogging(context)
            
            val logMsg1 = "═══════════════════════════════════════════════════════"
            val logMsg2 = "🔌 CONNECTION ATTEMPT STARTED"
            val logMsg3 = "Target MAC: $macAddress"
            val logMsg4 = "Timestamp: ${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(Date())}"
            
            Log.i(TAG, logMsg1)
            Log.i(TAG, logMsg2)
            Log.i(TAG, logMsg3)
            Log.i(TAG, logMsg4)
            
            logToFile(context, "INFO", logMsg1)
            logToFile(context, "INFO", logMsg2)
            logToFile(context, "INFO", logMsg3)
            logToFile(context, "INFO", logMsg4)
            
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    val msg = "❌ FATAL: Bluetooth adapter is NULL - device doesn't support Bluetooth"
                    Log.e(TAG, msg)
                    logToFile(context, "ERROR", msg)
                    closeFileLogging()
                    return@withContext null
                }
                
                if (!bluetoothAdapter.isEnabled) {
                    val msg = "❌ FATAL: Bluetooth is DISABLED - please enable Bluetooth"
                    Log.e(TAG, msg)
                    logToFile(context, "ERROR", msg)
                    closeFileLogging()
                    return@withContext null
                }
                
                val msg = "✓ Bluetooth adapter OK, enabled"
                Log.d(TAG, msg)
                logToFile(context, "DEBUG", msg)
                
                val device = bluetoothAdapter.getRemoteDevice(macAddress)
                @Suppress("MissingPermission")
                val deviceName = device.name ?: "Unknown"
                val msg1 = "📱 Device found: $deviceName (MAC: $macAddress)"
                Log.i(TAG, msg1)
                logToFile(context, "INFO", msg1)
                
                // Cancel discovery to improve connection speed
                @Suppress("MissingPermission")
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                    val msg2 = "✓ Cancelled ongoing discovery"
                    Log.d(TAG, msg2)
                    logToFile(context, "DEBUG", msg2)
                }
                
                // Try multiple connection methods for better compatibility
                var socket: BluetoothSocket? = null
                var methodNumber = 0

                // Method 1: Reflection-based INSECURE connection on channel 1 (commonly works on Zebra)
                methodNumber++
                val method1Start = System.currentTimeMillis()
                val method1Title = "🔧 METHOD $methodNumber: Insecure RFCOMM (channel 1, reflection)"
                Log.i(TAG, "")
                Log.i(TAG, method1Title)
                logToFile(context, "INFO", "")
                logToFile(context, "INFO", method1Title)
                try {
                    @Suppress("MissingPermission")
                    val insecureMethod = runCatching {
                        device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    }.getOrNull()
                    
                    if (insecureMethod != null) {
                        val msg1 = "  ↳ Reflection method found, creating socket..."
                        Log.d(TAG, msg1)
                        logToFile(context, "DEBUG", msg1)
                        socket = insecureMethod.invoke(device, 1) as BluetoothSocket
                        val msg2 = "  ↳ Socket created, attempting connect..."
                        Log.d(TAG, msg2)
                        logToFile(context, "DEBUG", msg2)
                        socket.connect()
                        val elapsed = System.currentTimeMillis() - method1Start
                        val msg3 = "  ✅ SUCCESS in ${elapsed}ms"
                        val msg4 = "  Device: $deviceName"
                        val msg5 = "  Total connection time: ${System.currentTimeMillis() - startTime}ms"
                        val msg6 = "═══════════════════════════════════════════════════════"
                        Log.i(TAG, msg3)
                        Log.i(TAG, msg4)
                        Log.i(TAG, msg5)
                        Log.i(TAG, msg6)
                        logToFile(context, "INFO", msg3)
                        logToFile(context, "INFO", msg4)
                        logToFile(context, "INFO", msg5)
                        logToFile(context, "INFO", msg6)
                        closeFileLogging()
                        return@withContext socket
                    } else {
                        val msg = "  ⚠ Reflection method not available"
                        Log.w(TAG, msg)
                        logToFile(context, "WARN", msg)
                    }
                } catch (e: Exception) {
                    socket?.close()
                    val elapsed = System.currentTimeMillis() - method1Start
                    val msg1 = "  ❌ FAILED in ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}"
                    Log.w(TAG, msg1)
                    logToFile(context, "WARN", msg1)
                    if (e.stackTrace.isNotEmpty()) {
                        val msg2 = "  ↳ at ${e.stackTrace[0]}"
                        Log.w(TAG, msg2)
                        logToFile(context, "WARN", msg2)
                    }
                }

                // Method 2: Insecure RFCOMM over SPP UUID (no pairing/bonding)
                methodNumber++
                val method2Start = System.currentTimeMillis()
                val method2Title = "🔧 METHOD $methodNumber: Insecure SPP UUID"
                Log.i(TAG, "")
                Log.i(TAG, method2Title)
                logToFile(context, "INFO", "")
                logToFile(context, "INFO", method2Title)
                try {
                    @Suppress("MissingPermission")
                    val msg1 = "  ↳ Creating insecure RFCOMM socket with SPP UUID..."
                    Log.d(TAG, msg1)
                    logToFile(context, "DEBUG", msg1)
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    val msg2 = "  ↳ Socket created, attempting connect..."
                    Log.d(TAG, msg2)
                    logToFile(context, "DEBUG", msg2)
                    socket.connect()
                    val elapsed = System.currentTimeMillis() - method2Start
                    val msg3 = "  ✅ SUCCESS in ${elapsed}ms"
                    val msg4 = "  Device: $deviceName"
                    val msg5 = "  Total connection time: ${System.currentTimeMillis() - startTime}ms"
                    val msg6 = "═══════════════════════════════════════════════════════"
                    Log.i(TAG, msg3)
                    Log.i(TAG, msg4)
                    Log.i(TAG, msg5)
                    Log.i(TAG, msg6)
                    logToFile(context, "INFO", msg3)
                    logToFile(context, "INFO", msg4)
                    logToFile(context, "INFO", msg5)
                    logToFile(context, "INFO", msg6)
                    closeFileLogging()
                    return@withContext socket
                } catch (e: Exception) {
                    socket?.close()
                    val elapsed = System.currentTimeMillis() - method2Start
                    val msg1 = "  ❌ FAILED in ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}"
                    Log.w(TAG, msg1)
                    logToFile(context, "WARN", msg1)
                    if (e.stackTrace.isNotEmpty()) {
                        val msg2 = "  ↳ at ${e.stackTrace[0]}"
                        Log.w(TAG, msg2)
                        logToFile(context, "WARN", msg2)
                    }
                }

                // Method 3: Reflection-based SECURE connection (channel 1)
                methodNumber++
                val method3Start = System.currentTimeMillis()
                val method3Title = "🔧 METHOD $methodNumber: Secure RFCOMM (channel 1, reflection)"
                Log.i(TAG, "")
                Log.i(TAG, method3Title)
                logToFile(context, "INFO", "")
                logToFile(context, "INFO", method3Title)
                try {
                    @Suppress("MissingPermission")
                    val msg1 = "  ↳ Getting reflection method for secure socket..."
                    Log.d(TAG, msg1)
                    logToFile(context, "DEBUG", msg1)
                    val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    socket = method.invoke(device, 1) as BluetoothSocket
                    val msg2 = "  ↳ Socket created, attempting connect..."
                    Log.d(TAG, msg2)
                    logToFile(context, "DEBUG", msg2)
                    socket.connect()
                    val elapsed = System.currentTimeMillis() - method3Start
                    val msg3 = "  ✅ SUCCESS in ${elapsed}ms"
                    val msg4 = "  Device: $deviceName"
                    val msg5 = "  Total connection time: ${System.currentTimeMillis() - startTime}ms"
                    val msg6 = "═══════════════════════════════════════════════════════"
                    Log.i(TAG, msg3)
                    Log.i(TAG, msg4)
                    Log.i(TAG, msg5)
                    Log.i(TAG, msg6)
                    logToFile(context, "INFO", msg3)
                    logToFile(context, "INFO", msg4)
                    logToFile(context, "INFO", msg5)
                    logToFile(context, "INFO", msg6)
                    closeFileLogging()
                    return@withContext socket
                } catch (e: Exception) {
                    socket?.close()
                    val elapsed = System.currentTimeMillis() - method3Start
                    val msg1 = "  ❌ FAILED in ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}"
                    Log.w(TAG, msg1)
                    logToFile(context, "WARN", msg1)
                    if (e.stackTrace.isNotEmpty()) {
                        val msg2 = "  ↳ at ${e.stackTrace[0]}"
                        Log.w(TAG, msg2)
                        logToFile(context, "WARN", msg2)
                    }
                }

                // Method 4: Standard SPP (SECURE) as the last resort
                methodNumber++
                val method4Start = System.currentTimeMillis()
                val method4Title = "🔧 METHOD $methodNumber: Secure SPP UUID (may prompt pairing)"
                Log.i(TAG, "")
                Log.i(TAG, method4Title)
                logToFile(context, "INFO", "")
                logToFile(context, "INFO", method4Title)
                try {
                    @Suppress("MissingPermission")
                    val msg1 = "  ↳ Creating secure RFCOMM socket with SPP UUID..."
                    Log.d(TAG, msg1)
                    logToFile(context, "DEBUG", msg1)
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    val msg2 = "  ↳ Socket created, attempting connect..."
                    Log.d(TAG, msg2)
                    logToFile(context, "DEBUG", msg2)
                    socket.connect()
                    val elapsed = System.currentTimeMillis() - method4Start
                    val msg3 = "  ✅ SUCCESS in ${elapsed}ms"
                    val msg4 = "  Device: $deviceName"
                    val msg5 = "  Total connection time: ${System.currentTimeMillis() - startTime}ms"
                    val msg6 = "═══════════════════════════════════════════════════════"
                    Log.i(TAG, msg3)
                    Log.i(TAG, msg4)
                    Log.i(TAG, msg5)
                    Log.i(TAG, msg6)
                    logToFile(context, "INFO", msg3)
                    logToFile(context, "INFO", msg4)
                    logToFile(context, "INFO", msg5)
                    logToFile(context, "INFO", msg6)
                    closeFileLogging()
                    return@withContext socket
                } catch (e: Exception) {
                    socket?.close()
                    val elapsed = System.currentTimeMillis() - method4Start
                    val msg1 = "  ❌ FAILED in ${elapsed}ms: ${e.javaClass.simpleName}: ${e.message}"
                    Log.e(TAG, msg1)
                    logToFile(context, "ERROR", msg1)
                    if (e.stackTrace.isNotEmpty()) {
                        val msg2 = "  ↳ at ${e.stackTrace[0]}"
                        Log.e(TAG, msg2)
                        logToFile(context, "ERROR", msg2)
                    }
                }
                
                val totalTime = System.currentTimeMillis() - startTime
                val msgErr1 = ""
                val msgErr2 = "💥 ALL $methodNumber CONNECTION METHODS FAILED"
                val msgErr3 = "Device: $deviceName (MAC: $macAddress)"
                val msgErr4 = "Total time: ${totalTime}ms"
                val msgErr5 = ""
                val msgErr6 = "Troubleshooting suggestions:"
                val msgErr7 = "  1. Check if printer is ON and in range"
                val msgErr8 = "  2. Verify printer is paired in Bluetooth settings"
                val msgErr9 = "  3. Try removing and re-pairing the device"
                val msgErr10 = "  4. Restart printer and try again"
                val msgErr11 = "  5. Check if other apps can connect to printer"
                val msgErr12 = "═══════════════════════════════════════════════════════"
                
                Log.e(TAG, msgErr1)
                Log.e(TAG, msgErr2)
                Log.e(TAG, msgErr3)
                Log.e(TAG, msgErr4)
                Log.e(TAG, msgErr5)
                Log.e(TAG, msgErr6)
                Log.e(TAG, msgErr7)
                Log.e(TAG, msgErr8)
                Log.e(TAG, msgErr9)
                Log.e(TAG, msgErr10)
                Log.e(TAG, msgErr11)
                Log.e(TAG, msgErr12)
                
                logToFile(context, "ERROR", msgErr1)
                logToFile(context, "ERROR", msgErr2)
                logToFile(context, "ERROR", msgErr3)
                logToFile(context, "ERROR", msgErr4)
                logToFile(context, "ERROR", msgErr5)
                logToFile(context, "ERROR", msgErr6)
                logToFile(context, "ERROR", msgErr7)
                logToFile(context, "ERROR", msgErr8)
                logToFile(context, "ERROR", msgErr9)
                logToFile(context, "ERROR", msgErr10)
                logToFile(context, "ERROR", msgErr11)
                logToFile(context, "ERROR", msgErr12)
                
                closeFileLogging()
                null
            } catch (e: SecurityException) {
                val msgSec1 = "❌ SECURITY EXCEPTION - Missing Bluetooth permissions"
                val msgSec2 = "═══════════════════════════════════════════════════════"
                Log.e(TAG, msgSec1, e)
                Log.e(TAG, msgSec2)
                logToFile(context, "ERROR", msgSec1 + ": " + e.message)
                logToFile(context, "ERROR", msgSec2)
                closeFileLogging()
                null
            } catch (e: Exception) {
                val msgUnex1 = "❌ UNEXPECTED ERROR during connection"
                val msgUnex2 = "═══════════════════════════════════════════════════════"
                Log.e(TAG, msgUnex1, e)
                Log.e(TAG, msgUnex2)
                logToFile(context, "ERROR", msgUnex1 + ": " + e.message)
                logToFile(context, "ERROR", msgUnex2)
                closeFileLogging()
                null
            }
        }
        
        /**
         * Send raw ZPL content to Zebra printer
         * @param context Application context for file logging
         * @param socket Active Bluetooth socket connection
         * @param zplContent Complete ZPL program (including ^XA and ^XZ)
         * @return true if sent successfully, false otherwise
         */
        suspend fun printZpl(
            context: Context?,
            socket: BluetoothSocket,
            zplContent: String
        ): Boolean = withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            
            // Initialize file logging
            if (context != null) {
                initFileLogging(context)
            }
            
            val msg1 = ""
            val msg2 = "───────────────────────────────────────────────────────"
            val msg3 = "🖨️ PRINT JOB STARTED"
            val msg4 = "Timestamp: ${java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(Date())}"
            val msg5 = "Socket connected: ${socket.isConnected}"
            
            Log.i(TAG, msg1)
            Log.i(TAG, msg2)
            Log.i(TAG, msg3)
            Log.i(TAG, msg4)
            Log.d(TAG, msg5)
            
            logToFile(context, "INFO", msg1)
            logToFile(context, "INFO", msg2)
            logToFile(context, "INFO", msg3)
            logToFile(context, "INFO", msg4)
            logToFile(context, "DEBUG", msg5)
            
            var outputStream: OutputStream? = null
            try {
                outputStream = socket.outputStream
                val msg6 = "✓ Output stream obtained"
                Log.d(TAG, msg6)
                logToFile(context, "DEBUG", msg6)
                
                // Try to switch the printer to ZPL language first (Zebra SGD command)
                val sgdStart = System.currentTimeMillis()
                runCatching {
                    val sgd = "! U1 setvar \"device.languages\" \"zpl\"\r\n".toByteArray(Charsets.UTF_8)
                    val msg7 = ""
                    val msg8 = "📤 Sending SGD language switch command"
                    val msg9 = "  Command: ${String(sgd, Charsets.UTF_8).trim()}"
                    Log.d(TAG, msg7)
                    Log.d(TAG, msg8)
                    Log.d(TAG, msg9)
                    logToFile(context, "DEBUG", msg7)
                    logToFile(context, "DEBUG", msg8)
                    logToFile(context, "DEBUG", msg9)
                    outputStream.write(sgd)
                    outputStream.flush()
                    Thread.sleep(100) // Wait for language switch
                    val sgdElapsed = System.currentTimeMillis() - sgdStart
                    val msg10 = "  ✓ SGD sent in ${sgdElapsed}ms"
                    Log.d(TAG, msg10)
                    logToFile(context, "DEBUG", msg10)
                }.onFailure { e ->
                    val sgdElapsed = System.currentTimeMillis() - sgdStart
                    val msgSgdFail = "  ⚠ SGD failed in ${sgdElapsed}ms: ${e.message}"
                    Log.w(TAG, msgSgdFail)
                    logToFile(context, "WARN", msgSgdFail)
                }

                // Send ZPL program
                val zplBytes = zplContent.toByteArray(Charsets.UTF_8)
                val zplStart = System.currentTimeMillis()
                val msgZpl1 = ""
                val msgZpl2 = "📤 Sending ZPL content"
                val msgZpl3 = "  Size: ${zplBytes.size} bytes (${String.format("%.2f", zplBytes.size / 1024.0)} KB)"
                val msgZpl4 = "  Preview (first 100 chars):"
                val msgZpl5 = "  ${zplContent.take(100).replace("\n", "\\n")}"
                
                Log.d(TAG, msgZpl1)
                Log.d(TAG, msgZpl2)
                Log.d(TAG, msgZpl3)
                Log.d(TAG, msgZpl4)
                Log.d(TAG, msgZpl5)
                logToFile(context, "DEBUG", msgZpl1)
                logToFile(context, "DEBUG", msgZpl2)
                logToFile(context, "DEBUG", msgZpl3)
                logToFile(context, "DEBUG", msgZpl4)
                logToFile(context, "DEBUG", msgZpl5)
                
                outputStream.write(zplBytes)
                outputStream.flush()
                val zplElapsed = System.currentTimeMillis() - zplStart
                val msgZpl6 = "  ✓ ZPL written to stream in ${zplElapsed}ms"
                Log.d(TAG, msgZpl6)
                logToFile(context, "DEBUG", msgZpl6)
                
                Thread.sleep(200) // Wait for printing
                val totalTime = System.currentTimeMillis() - startTime
                
                val msgSuccess1 = ""
                val msgSuccess2 = "✅ PRINT JOB COMPLETED SUCCESSFULLY"
                val msgSuccess3 = "Total time: ${totalTime}ms"
                val msgSuccess4 = "  - SGD command: ~100ms"
                val msgSuccess5 = "  - ZPL transfer: ${zplElapsed}ms"
                val msgSuccess6 = "  - Wait for print: 200ms"
                val msgSuccess7 = "───────────────────────────────────────────────────────"
                
                Log.i(TAG, msgSuccess1)
                Log.i(TAG, msgSuccess2)
                Log.i(TAG, msgSuccess3)
                Log.i(TAG, msgSuccess4)
                Log.i(TAG, msgSuccess5)
                Log.i(TAG, msgSuccess6)
                Log.i(TAG, msgSuccess7)
                
                logToFile(context, "INFO", msgSuccess1)
                logToFile(context, "INFO", msgSuccess2)
                logToFile(context, "INFO", msgSuccess3)
                logToFile(context, "INFO", msgSuccess4)
                logToFile(context, "INFO", msgSuccess5)
                logToFile(context, "INFO", msgSuccess6)
                logToFile(context, "INFO", msgSuccess7)
                
                closeFileLogging()
                true
            } catch (e: Exception) {
                val totalTime = System.currentTimeMillis() - startTime
                val msgFail1 = ""
                val msgFail2 = "❌ PRINT JOB FAILED"
                val msgFail3 = "Error: ${e.javaClass.simpleName}: ${e.message}"
                val msgFail4 = "Time before failure: ${totalTime}ms"
                
                Log.e(TAG, msgFail1)
                Log.e(TAG, msgFail2)
                Log.e(TAG, msgFail3)
                Log.e(TAG, msgFail4)
                logToFile(context, "ERROR", msgFail1)
                logToFile(context, "ERROR", msgFail2)
                logToFile(context, "ERROR", msgFail3)
                logToFile(context, "ERROR", msgFail4)
                
                if (e.stackTrace.isNotEmpty()) {
                    val msgStack = "Stack trace:"
                    Log.e(TAG, msgStack)
                    logToFile(context, "ERROR", msgStack)
                    e.stackTrace.take(3).forEach { 
                        val msgStackLine = "  at $it"
                        Log.e(TAG, msgStackLine)
                        logToFile(context, "ERROR", msgStackLine)
                    }
                }
                val msgFail5 = "───────────────────────────────────────────────────────"
                Log.e(TAG, msgFail5)
                logToFile(context, "ERROR", msgFail5)
                closeFileLogging()
                false
            } finally {
                try {
                    outputStream?.flush()
                } catch (e: Exception) {
                    val msgFlush = "Error flushing output stream: ${e.message}"
                    Log.w(TAG, msgFlush)
                    logToFile(context, "WARN", msgFlush)
                }
            }
        }
        
        /**
         * Print QR code bitmap via Bluetooth
         */
        suspend fun printQRCode(
            socket: BluetoothSocket,
            qrBitmap: Bitmap,
            headerText: String? = null,
            footerText: String? = null,
            qrData: String? = null
        ): Boolean = withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                outputStream = socket.outputStream
                
                // If qrData is provided, prefer sending ZPL (Zebra-compatible, no bitmap threshold issues)
                if (!qrData.isNullOrEmpty()) {
                    // Try to switch the printer to ZPL language first (Zebra SGD command)
                    runCatching {
                        val sgd = "! U1 setvar \"device.languages\" \"zpl\"\r\n".toByteArray(Charsets.UTF_8)
                        Log.d(TAG, "Sending SGD language switch command: ${String(sgd, Charsets.UTF_8).trim()}")
                        outputStream.write(sgd)
                        outputStream.flush()
                        Thread.sleep(100) // Increased delay for language switch
                        Log.d(TAG, "SGD language switch sent successfully")
                    }.onFailure { e ->
                        Log.w(TAG, "Failed to send SGD language switch", e)
                    }

                    // Send ZPL program
                    val zpl = buildZplForQr(qrData, headerText, footerText)
                    Log.d(TAG, "Sending ZPL QR data (${zpl.size} bytes): ${String(zpl, Charsets.UTF_8).take(200)}...")
                    outputStream.write(zpl)
                    outputStream.flush()
                    Thread.sleep(200) // Wait for processing
                    Log.d(TAG, "Sent ZPL QR successfully")

                    // NOTE: Removed CPCL fallback to prevent duplicate printing
                    // If ZPL doesn't work, the ESC/POS fallback below will be used instead
                    return@withContext true
                }

                // ESC/POS fallback (for generic thermal printers)
                // Initialize printer
                outputStream.write(ESC_INIT)
                Thread.sleep(50)

                // Print header if provided
                if (!headerText.isNullOrEmpty()) {
                    outputStream.write(ESC_ALIGN_CENTER)
                    outputStream.write(headerText.toByteArray(Charsets.UTF_8))
                    outputStream.write(LINE_FEED)
                    outputStream.write(LINE_FEED)
                }

                // Convert bitmap to ESC/POS bitmap command
                val bitmapData = convertBitmapToESCPOS(qrBitmap)
                outputStream.write(ESC_ALIGN_CENTER)
                outputStream.write(bitmapData)
                outputStream.write(LINE_FEED)

                // Print footer if provided
                if (!footerText.isNullOrEmpty()) {
                    outputStream.write(LINE_FEED)
                    outputStream.write(ESC_ALIGN_CENTER)
                    outputStream.write(footerText.toByteArray(Charsets.UTF_8))
                    outputStream.write(LINE_FEED)
                }

                // Feed paper and cut
                outputStream.write(LINE_FEED)
                outputStream.write(LINE_FEED)
                outputStream.write(LINE_FEED)
                outputStream.write(ESC_CUT)

                outputStream.flush()
                Log.d(TAG, "QR code printed successfully (ESC/POS)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error printing QR code", e)
                false
            } finally {
                try {
                    outputStream?.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing output stream", e)
                }
            }
        }

        // Build a minimal ZPL program for QR with optional header/footer
        private fun buildZplForQr(data: String, header: String?, footer: String?): ByteArray {
            // Calculate dynamic label height based on content
            var totalHeight = 50 // Top margin

            // Calculate header height
            if (!header.isNullOrEmpty()) {
                val headerLines = header.lines()
                totalHeight += headerLines.size * 28 // 28 points per line
                totalHeight += 10 // spacing after header
            }

            // Calculate QR magnification and height
            val qrMagnification = when {
                data.length <= 100 -> 6    // Small data - larger QR for better readability
                data.length <= 500 -> 5    // Medium data
                data.length <= 1000 -> 4   // Large data
                data.length <= 2000 -> 3   // Very large data
                else -> 2                  // Extremely large data
            }
            val qrHeight = qrMagnification * 110 // QR code height
            totalHeight += qrHeight + 30 // QR + spacing after

            // Calculate footer height
            if (!footer.isNullOrEmpty()) {
                val footerLines = footer.lines()
                totalHeight += footerLines.size * 26 // 26 points per line
            }

            // Add bottom margin
            totalHeight += 50

            // Ensure minimum height
            totalHeight = maxOf(totalHeight, 400)

            // 384 dots width (~48mm at 203dpi)
            val sb = StringBuilder()
            sb.append("^XA\r\n")
            sb.append("^PW384\r\n") // print width (dots)
            sb.append("^LL$totalHeight\r\n") // dynamic label length (dots)
            sb.append("^LH0,0\r\n")  // label home
            sb.append("^CI28\r\n") // UTF-8
            sb.append("^XZ\r\n") // close any partial format (defensive)
            sb.append("^XA\r\n")
            sb.append("^PW384\r\n^LL$totalHeight\r\n^LH0,0\r\n^CI28\r\n")

            // Header (optional), print as multiline text at top
            var y = 50  // Top margin
            if (!header.isNullOrEmpty()) {
                header.lines().forEach { line ->
                    sb.append("^FO20,$y^A0N,24,24^FB344,1,0,C,0^FD").append(line).append("^FS\r\n")
                    y += 28
                }
                y += 10
            }

            // QR code block - positioned on the left
            sb.append("^FO20,$y\r\n") // Left-aligned QR code
            sb.append("^BQN,2,$qrMagnification\r\n")  // Model 2, calculated magnification
            sb.append("^FDLA,").append(data).append("^FS\r\n")

            // Move y position past QR code
            y += qrHeight + 30

            // Footer (optional) - positioned below QR
            if (!footer.isNullOrEmpty()) {
                footer.lines().forEach { line ->
                    sb.append("^FO20,$y^A0N,22,22^FB344,1,0,L,0^FD").append(line).append("^FS\r\n")
                    y += 26
                }
            }

            sb.append("^PQ1\r\n^XZ\r\n")
            return sb.toString().toByteArray(Charsets.UTF_8)
        }

        // CPCL fallback program for QR (mobile Zebra often supports CPCL)
        private fun buildCpclForQr(data: String, header: String?, footer: String?): ByteArray {
            val sb = StringBuilder()
            //! 0 <hres> <vres> <height> <qty>
            sb.append("! 0 200 200 400 1\r\n")
            sb.append("PW 384\r\n") // print width
            sb.append("SETMAG 0 0\r\n")
            var y = 20
            if (!header.isNullOrEmpty()) {
                header.lines().forEach { line ->
                    sb.append("CENTER\r\n")
                    sb.append("T 0 24 0 $y ").append(line).append("\r\n")
                    y += 28
                }
                y += 10
            }
            sb.append("CENTER\r\n")
            sb.append("B QR 0 $y M 2 U 6\r\n") // model 2, unit size 6
            sb.append("MA,").append(data).append("\r\n")
            sb.append("ENDQR\r\n")
            y += 220
            if (!footer.isNullOrEmpty()) {
                footer.lines().forEach { line ->
                    sb.append("CENTER\r\n")
                    sb.append("T 0 22 0 $y ").append(line).append("\r\n")
                    y += 24
                }
            }
            sb.append("PRINT\r\n")
            return sb.toString().toByteArray(Charsets.UTF_8)
        }
        
        /**
         * Print plain text content via Bluetooth
         */
        suspend fun printText(
            socket: BluetoothSocket,
            textContent: String
        ): Boolean = withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                outputStream = socket.outputStream

                // Try ZPL mode first (for Zebra printers)
                runCatching {
                    val sgd = "! U1 setvar \"device.languages\" \"zpl\"\r\n".toByteArray(Charsets.UTF_8)
                    Log.d(TAG, "Sending SGD language switch command: ${String(sgd, Charsets.UTF_8).trim()}")
                    outputStream.write(sgd)
                    outputStream.flush()
                    Thread.sleep(100)
                    Log.d(TAG, "SGD language switch sent successfully")
                }.onFailure { e ->
                    Log.w(TAG, "Failed to send SGD language switch", e)
                }

                // Send ZPL text content
                val zplContent = buildZplForText(textContent)
                Log.d(TAG, "Sending ZPL text (${zplContent.size} bytes)")
                outputStream.write(zplContent)
                outputStream.flush()
                Thread.sleep(200)
                Log.d(TAG, "Sent ZPL text successfully")

                true
            } catch (e: Exception) {
                Log.e(TAG, "Error printing text", e)
                false
            } finally {
                try {
                    outputStream?.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing output stream", e)
                }
            }
        }
        
        // Build ZPL program for plain text content
        private fun buildZplForText(textContent: String): ByteArray {
            val lines = textContent.lines()
            
            // Calculate dynamic height based on number of lines
            val totalHeight = 50 + (lines.size * 28) + 50 // top margin + lines + bottom margin
            
            val sb = StringBuilder()
            sb.append("^XA\r\n")
            sb.append("^PW384\r\n") // print width (dots)
            sb.append("^LL$totalHeight\r\n") // dynamic label length (dots)
            sb.append("^LH0,0\r\n")  // label home
            sb.append("^CI28\r\n") // UTF-8
            
            // Print each line of text
            var y = 50 // Start position
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    sb.append("^FO20,$y^A0N,24,24^FB344,1,0,L,0^FD").append(line).append("^FS\r\n")
                }
                y += 28
            }
            
            sb.append("^PQ1\r\n^XZ\r\n")
            return sb.toString().toByteArray(Charsets.UTF_8)
        }
        
        /**
         * Convert bitmap to ESC/POS format
         * Uses raster bit image mode
         */
        private fun convertBitmapToESCPOS(bitmap: Bitmap): ByteArray {
            // Scale bitmap to printer width if needed (384 pixels for 58mm, 576 for 80mm)
            val maxWidth = 384
            val scaledBitmap = if (bitmap.width > maxWidth) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    maxWidth,
                    (bitmap.height * maxWidth / bitmap.width),
                    false
                )
            } else {
                bitmap
            }
            
            val width = scaledBitmap.width
            val height = scaledBitmap.height
            
            // ESC/POS raster bit image command: GS v 0
            val result = mutableListOf<Byte>()
            result.add(0x1D) // GS
            result.add(0x76) // v
            result.add(0x30) // 0 (normal mode)
            result.add(0x00) // m (mode)
            
            // Width in bytes (width / 8)
            val widthBytes = (width + 7) / 8
            result.add((widthBytes and 0xFF).toByte())
            result.add(((widthBytes shr 8) and 0xFF).toByte())
            
            // Height in dots
            result.add((height and 0xFF).toByte())
            result.add(((height shr 8) and 0xFF).toByte())
            
            // Convert bitmap to monochrome bits
            // Use higher threshold to prevent "black blob" effect on QR codes
            for (y in 0 until height) {
                for (x in 0 until widthBytes) {
                    var byte = 0
                    for (bit in 0 until 8) {
                        val pixelX = x * 8 + bit
                        if (pixelX < width) {
                            val pixel = scaledBitmap.getPixel(pixelX, y)
                            // Extract RGB channels
                            val red = (pixel shr 16) and 0xFF
                            val green = (pixel shr 8) and 0xFF
                            val blue = pixel and 0xFF
                            
                            // Calculate brightness (0-255)
                            val brightness = (red + green + blue) / 3
                            
                            // Stricter threshold: only print truly dark pixels
                            // This prevents QR codes from becoming black blobs
                            if (brightness < 100) { // Only very dark pixels
                                byte = byte or (1 shl (7 - bit))
                            }
                        }
                    }
                    result.add(byte.toByte())
                }
            }
            
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            
            return result.toByteArray()
        }

        /**
         * Send test label to verify printer functionality
         */
        suspend fun sendTestLabel(socket: BluetoothSocket): Boolean = withContext(Dispatchers.IO) {
            var outputStream: OutputStream? = null
            try {
                outputStream = socket.outputStream

                // Force ZPL mode
                runCatching {
                    val sgd = "! U1 setvar \"device.languages\" \"zpl\"\r\n".toByteArray(Charsets.UTF_8)
                    Log.d(TAG, "Test: Sending SGD language switch")
                    outputStream.write(sgd)
                    outputStream.flush()
                    Thread.sleep(200)
                }

                // Send simple test ZPL label
                val testZpl = """
                    ^XA
                    ^PW384
                    ^LL200
                    ^FO50,30^A0N,35,35^FDTEST LABEL^FS
                    ^FO50,80^A0N,25,25^FDPrinter working!^FS
                    ^FO50,120^A0N,20,20^FD${Date()}^FS
                    ^PQ1
                    ^XZ
                """.trimIndent().toByteArray(Charsets.UTF_8)

                Log.d(TAG, "Test: Sending test ZPL (${testZpl.size} bytes)")
                outputStream.write(testZpl)
                outputStream.flush()
                Thread.sleep(500) // Wait for printing
                Log.d(TAG, "Test label sent successfully")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error sending test label", e)
                false
            } finally {
                try {
                    outputStream?.flush()
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing test output stream", e)
                }
            }
        }
        
        /**
         * Close Bluetooth connection
         */
        fun disconnect(socket: BluetoothSocket?) {
            try {
                socket?.close()
                Log.d(TAG, "Disconnected from printer")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }
        }
    }
}
