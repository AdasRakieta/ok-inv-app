package com.example.inventoryapp.data.local.dao

import androidx.room.*
import com.example.inventoryapp.data.local.entities.PrinterEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for printer operations
 */
@Dao
interface PrinterDao {
    
    @Query("SELECT * FROM printers ORDER BY isDefault DESC, name ASC")
    fun getAllPrinters(): Flow<List<PrinterEntity>>
    
    @Query("SELECT * FROM printers WHERE id = :printerId")
    suspend fun getPrinterById(printerId: Long): PrinterEntity?
    
    @Query("SELECT * FROM printers WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPrinter(): PrinterEntity?
    
    @Query("SELECT * FROM printers WHERE isDefault = 1 LIMIT 1")
    fun getDefaultPrinterFlow(): Flow<PrinterEntity?>
    
    @Insert
    suspend fun insertPrinter(printer: PrinterEntity): Long
    
    @Update
    suspend fun updatePrinter(printer: PrinterEntity)
    
    @Delete
    suspend fun deletePrinter(printer: PrinterEntity)
    
    @Query("DELETE FROM printers WHERE id = :printerId")
    suspend fun deletePrinterById(printerId: Long)
    
    @Transaction
    suspend fun setDefaultPrinter(printerId: Long) {
        // Clear all default flags
        clearAllDefaultFlags()
        // Set new default
        setDefaultFlag(printerId)
    }
    
    @Query("UPDATE printers SET isDefault = 0")
    suspend fun clearAllDefaultFlags()
    
    @Query("UPDATE printers SET isDefault = 1 WHERE id = :printerId")
    suspend fun setDefaultFlag(printerId: Long)
    
    @Query("SELECT COUNT(*) FROM printers")
    suspend fun getPrinterCount(): Int
}
