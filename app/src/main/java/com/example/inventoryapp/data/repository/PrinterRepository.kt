package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.PrinterDao
import com.example.inventoryapp.data.local.entities.PrinterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for printer operations
 */
class PrinterRepository(private val printerDao: PrinterDao) {
    
    fun getAllPrinters(): Flow<List<PrinterEntity>> = printerDao.getAllPrinters()
    
    fun getDefaultPrinter(): Flow<PrinterEntity?> = printerDao.getDefaultPrinterFlow()
    
    suspend fun getPrinterById(printerId: Long): PrinterEntity? {
        return printerDao.getPrinterById(printerId)
    }
    
    suspend fun getDefaultPrinterSync(): PrinterEntity? {
        return printerDao.getDefaultPrinter()
    }
    
    suspend fun insertPrinter(printer: PrinterEntity): Long {
        return printerDao.insertPrinter(printer)
    }
    
    suspend fun updatePrinter(printer: PrinterEntity) {
        printerDao.updatePrinter(printer)
    }
    
    suspend fun deletePrinter(printer: PrinterEntity) {
        printerDao.deletePrinter(printer)
    }
    
    suspend fun deletePrinterById(printerId: Long) {
        printerDao.deletePrinterById(printerId)
    }
    
    suspend fun setDefaultPrinter(printerId: Long) {
        printerDao.setDefaultPrinter(printerId)
    }
    
    suspend fun getPrinterCount(): Int {
        return printerDao.getPrinterCount()
    }
}
