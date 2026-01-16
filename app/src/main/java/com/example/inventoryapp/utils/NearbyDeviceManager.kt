package com.example.inventoryapp.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Comprehensive device discovery manager for nearby devices
 * Supports Bluetooth, WiFi network scanning, and other connection types
 */
class NearbyDeviceManager(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Device discovery listeners
    private var deviceDiscoveryListener: ((DeviceInfo) -> Unit)? = null
    private var discoveryCompleteListener: ((List<DeviceInfo>) -> Unit)? = null

    // Bluetooth discovery
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceInfo = DeviceInfo(
                            id = it.address,
                            name = it.name ?: "Unknown Device",
                            type = DeviceType.BLUETOOTH_DEVICE,
                            address = it.address,
                            connectionType = ConnectionType.BLUETOOTH,
                            isAvailable = true
                        )
                        mainHandler.post {
                            deviceDiscoveryListener?.invoke(deviceInfo)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    mainHandler.post {
                        // Notify discovery complete
                    }
                }
            }
        }
    }

    init {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        registerBluetoothReceiver()
    }

    /**
     * Start continuous device discovery
     */
    fun startDeviceDiscovery(
        onDeviceFound: (DeviceInfo) -> Unit,
        onDiscoveryComplete: (List<DeviceInfo>) -> Unit = {}
    ) {
        deviceDiscoveryListener = onDeviceFound
        discoveryCompleteListener = onDiscoveryComplete

        // Start all discovery methods
        startBluetoothDiscovery()
        startNetworkDiscovery()
        startContinuousDiscovery()
    }

    /**
     * Stop device discovery
     */
    fun stopDeviceDiscovery() {
        stopBluetoothDiscovery()
        executor.shutdown()
        deviceDiscoveryListener = null
        discoveryCompleteListener = null
    }

    /**
     * Start Bluetooth device discovery
     */
    private fun startBluetoothDiscovery() {
        if (bluetoothAdapter == null) return

        // Check permissions
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(context, "android.permission.BLUETOOTH_SCAN") != PackageManager.PERMISSION_GRANTED) {
                return
            }
        } else {
            // For API 30 and below, check legacy Bluetooth permissions
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        if (bluetoothAdapter?.isEnabled == true && !bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter?.startDiscovery()
        }
    }

    /**
     * Stop Bluetooth device discovery
     */
    private fun stopBluetoothDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
    }

    /**
     * Start network device discovery
     */
    private fun startNetworkDiscovery() {
        executor.execute {
            try {
                val subnet = detectCurrentSubnet()
                discoverNetworkDevices(subnet)
            } catch (e: Exception) {
                // Handle network discovery errors
            }
        }
    }

    /**
     * Start continuous discovery loop
     */
    private fun startContinuousDiscovery() {
        executor.execute {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    // Refresh Bluetooth devices
                    getPairedBluetoothDevices()

                    // Refresh network devices
                    val subnet = detectCurrentSubnet()
                    discoverNetworkDevices(subnet)

                    // Wait before next discovery cycle
                    Thread.sleep(10000) // 10 seconds
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    // Handle discovery errors
                }
            }
        }
    }

    /**
     * Get paired Bluetooth devices
     */
    private fun getPairedBluetoothDevices() {
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            val deviceInfo = DeviceInfo(
                id = device.address,
                name = device.name ?: "Unknown Device",
                type = DeviceType.BLUETOOTH_PRINTER,
                address = device.address,
                connectionType = ConnectionType.BLUETOOTH,
                isAvailable = true
            )
            mainHandler.post {
                deviceDiscoveryListener?.invoke(deviceInfo)
            }
        }
    }

    /**
     * Discover devices on the network
     */
    private fun discoverNetworkDevices(subnet: String) {
        for (i in 1..254) {
            val ipAddress = "$subnet.$i"
            executor.execute {
                try {
                    val inetAddress = InetAddress.getByName(ipAddress)
                    if (inetAddress.isReachable(100)) {
                        // Check if it's a printer
                        if (isPrinterDevice(ipAddress)) {
                            val deviceInfo = DeviceInfo(
                                id = ipAddress,
                                name = "Network Printer ($ipAddress)",
                                type = DeviceType.NETWORK_PRINTER,
                                address = ipAddress,
                                connectionType = ConnectionType.WIFI,
                                isAvailable = true
                            )
                            mainHandler.post {
                                deviceDiscoveryListener?.invoke(deviceInfo)
                            }
                        } else {
                            // Generic network device
                            val deviceInfo = DeviceInfo(
                                id = ipAddress,
                                name = "Network Device ($ipAddress)",
                                type = DeviceType.NETWORK_DEVICE,
                                address = ipAddress,
                                connectionType = ConnectionType.WIFI,
                                isAvailable = true
                            )
                            mainHandler.post {
                                deviceDiscoveryListener?.invoke(deviceInfo)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Skip unreachable hosts
                }
            }
        }
    }

    /**
     * Check if device at IP address is a printer
     */
    private fun isPrinterDevice(ipAddress: String): Boolean {
        return try {
            // Try to connect to common printer ports
            val printerPorts = arrayOf(9100, 515, 631) // Zebra, LPD, IPP
            for (port in printerPorts) {
                try {
                    val socket = java.net.Socket()
                    socket.connect(java.net.InetSocketAddress(ipAddress, port), 1000)
                    socket.close()
                    return true
                } catch (e: Exception) {
                    // Continue to next port
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detect current network subnet
     */
    private fun detectCurrentSubnet(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses

                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val hostAddress = address.hostAddress
                        val parts = hostAddress.split(".")
                        if (parts.size == 4) {
                            return "${parts[0]}.${parts[1]}.${parts[2]}"
                        }
                    }
                }
            }
            "192.168.1" // Fallback
        } catch (e: Exception) {
            "192.168.1" // Fallback
        }
    }

    /**
     * Register Bluetooth broadcast receiver
     */
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    /**
     * Unregister Bluetooth broadcast receiver
     */
    private fun unregisterBluetoothReceiver() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        stopDeviceDiscovery()
        unregisterBluetoothReceiver()
        executor.shutdown()
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            // Handle interruption
        }
    }
}

/**
 * Device information data class
 */
data class DeviceInfo(
    val id: String,
    val name: String,
    val type: DeviceType,
    val address: String,
    val connectionType: ConnectionType,
    val isAvailable: Boolean,
    val lastSeen: Long = System.currentTimeMillis()
)

/**
 * Device type enumeration
 */
enum class DeviceType {
    BLUETOOTH_PRINTER,
    BLUETOOTH_DEVICE,
    NETWORK_PRINTER,
    NETWORK_DEVICE,
    USB_DEVICE,
    NFC_DEVICE
}

/**
 * Connection type enumeration
 */
enum class ConnectionType {
    BLUETOOTH,
    WIFI,
    USB,
    NFC,
    ETHERNET
}