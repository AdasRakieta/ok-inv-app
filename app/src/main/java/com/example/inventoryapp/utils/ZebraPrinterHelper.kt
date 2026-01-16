package com.example.inventoryapp.utils

import android.os.Handler
import android.os.Looper
import com.zebra.sdk.comm.BluetoothConnectionInsecure
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.ZebraPrinterFactory
import com.zebra.sdk.printer.ZebraPrinterLinkOs
import java.util.concurrent.Executors
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

/**
 * Utility class for printing to Zebra printers via Bluetooth MAC address
 * Based on ok_mobile_zebra_printer Flutter plugin
 */
class ZebraPrinterHelper {

    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Print ZPL document to Zebra printer
     * @param macAddress MAC address of the printer (e.g., "00:11:22:33:44:55")
     * @param zplMessage ZPL formatted print data
     * @param labelLength Label length setting (optional)
     * @param callback Callback with result or error message
     */
    fun printDocument(
        macAddress: String,
        zplMessage: String,
        labelLength: String? = null,
        callback: (String?) -> Unit
    ) {
        executor.execute {
            try {
                // Create Bluetooth connection
                val printerConn: Connection = BluetoothConnectionInsecure(macAddress)

                // Open connection
                printerConn.open()

                // Get printer instance
                val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(printerConn)

                // Set label length if provided
                labelLength?.let {
                    linkOsPrinter.setSetting("zpl.label_length", it)
                }

                // Send ZPL data
                printerConn.write(zplMessage.toByteArray(charset("windows-1250")))

                // Wait for printing to complete
                Thread.sleep(1000)

                // Close connection
                printerConn.close()

                // Success callback on main thread
                Handler(Looper.getMainLooper()).post {
                    callback(null) // null means success
                }

            } catch (e: Exception) {
                // Error callback on main thread
                Handler(Looper.getMainLooper()).post {
                    callback(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Print ZPL document to Zebra printer over WiFi
     * @param ipAddress IP address of the printer (e.g., "192.168.1.100")
     * @param port Port number (default 9100 for Zebra printers)
     * @param zplMessage ZPL formatted print data
     * @param labelLength Label length setting (optional)
     * @param callback Callback with result or error message
     */
    fun printDocumentOverWifi(
        ipAddress: String,
        port: Int = 9100,
        zplMessage: String,
        labelLength: String? = null,
        callback: (String?) -> Unit
    ) {
        executor.execute {
            try {
                // Create TCP connection
                val printerConn: Connection = TcpConnection(ipAddress, port)

                // Open connection
                printerConn.open()

                // Get printer instance
                val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(printerConn)

                // Set label length if provided
                labelLength?.let {
                    linkOsPrinter.setSetting("zpl.label_length", it)
                }

                // Send ZPL data
                printerConn.write(zplMessage.toByteArray(charset("windows-1250")))

                // Wait for printing to complete
                Thread.sleep(1000)

                // Close connection
                printerConn.close()

                // Success callback on main thread
                Handler(Looper.getMainLooper()).post {
                    callback(null) // null means success
                }

            } catch (e: Exception) {
                // Error callback on main thread
                Handler(Looper.getMainLooper()).post {
                    callback(e.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * Test WiFi printer connection
     * @param ipAddress IP address of the printer
     * @param port Port number (default 9100)
     * @param callback Callback with connection status
     */
    fun testWifiConnection(ipAddress: String, port: Int = 9100, callback: (Boolean, String?) -> Unit) {
        executor.execute {
            try {
                val printerConn: Connection = TcpConnection(ipAddress, port)
                printerConn.open()

                // Try to get printer info
                val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(printerConn)
                val printerInfo = linkOsPrinter.printerControlLanguage

                printerConn.close()

                Handler(Looper.getMainLooper()).post {
                    callback(true, "Connected successfully. Printer: $printerInfo")
                }

            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    callback(false, e.message ?: "Connection failed")
                }
            }
        }
    }

    /**
     * Discover Zebra printers on the local network
     * @param subnet Subnet to scan (e.g., "192.168.1")
     * @param startIp Starting IP address (default 1)
     * @param endIp Ending IP address (default 254)
     * @param callback Callback with list of discovered printers (IP addresses)
     */
    fun discoverWifiPrinters(subnet: String = "192.168.1", startIp: Int = 1, endIp: Int = 254, callback: (List<String>) -> Unit) {
        executor.execute {
            val discoveredPrinters = mutableListOf<String>()

            try {
                // Scan IP range
                for (i in startIp..endIp) {
                    val ipAddress = "$subnet.$i"
                    try {
                        val inetAddress = InetAddress.getByName(ipAddress)
                        if (inetAddress.isReachable(100)) { // 100ms timeout
                            // Try to connect to port 9100 (standard Zebra port)
                            if (testZebraPrinterConnection(ipAddress)) {
                                discoveredPrinters.add(ipAddress)
                            }
                        }
                    } catch (e: Exception) {
                        // Skip unreachable hosts
                    }
                }
            } catch (e: Exception) {
                // Handle network scanning errors
            }

            Handler(Looper.getMainLooper()).post {
                callback(discoveredPrinters)
            }
        }
    }

    /**
     * Test connection to a potential Zebra printer
     */
    private fun testZebraPrinterConnection(ipAddress: String): Boolean {
        return try {
            val printerConn: Connection = TcpConnection(ipAddress, 9100)
            printerConn.open()

            // Try to get printer info to verify it's a Zebra printer
            val linkOsPrinter: ZebraPrinterLinkOs = ZebraPrinterFactory.getLinkOsPrinter(printerConn)
            val printerInfo = linkOsPrinter.printerControlLanguage

            printerConn.close()
            printerInfo != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up resources
     */
    fun shutdown() {
        executor.shutdown()
    }

    companion object {
        /**
         * Generate sample ZPL for testing
         */
        fun createTestZpl(productName: String, barcode: String): String {
            return """
                ^XA
                ^CF0,30
                ^FO50,50^FD$productName^FS
                ^FO50,100^BCN,100,Y,N,N^FD$barcode^FS
                ^XZ
            """.trimIndent()
        }
    }
}