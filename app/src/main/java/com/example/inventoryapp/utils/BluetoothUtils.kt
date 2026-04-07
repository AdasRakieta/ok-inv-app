package com.example.inventoryapp.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build

/**
 * Return a BluetoothAdapter in a backward-compatible way.
 * Uses BluetoothManager on newer SDKs when a Context is available.
 */
fun getBluetoothAdapter(context: Context?): BluetoothAdapter? {
    if (context == null) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(BluetoothManager::class.java)
        return manager?.adapter
    } else {
        @Suppress("DEPRECATION")
        return BluetoothAdapter.getDefaultAdapter()
    }
}
