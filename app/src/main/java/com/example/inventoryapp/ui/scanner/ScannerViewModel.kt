package com.example.inventoryapp.ui.scanner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ScanHistoryEntity
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ScanRepository
import com.example.inventoryapp.domain.validators.SerialNumberValidator
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val productRepository: ProductRepository,
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val validator = SerialNumberValidator()

    private val _scanResult = MutableLiveData<ScanState>()
    val scanResult: LiveData<ScanState> = _scanResult

    private val _assignmentResult = MutableLiveData<AssignmentState>()
    val assignmentResult: LiveData<AssignmentState> = _assignmentResult

    fun onBarcodeScanned(code: String, format: Int) {
        viewModelScope.launch {
            // Validate the scanned code
            when (val validationResult = validator.validate(code)) {
                is SerialNumberValidator.ValidationResult.Success -> {
                    // Check if serial number already exists
                    val exists = productRepository.isSerialNumberExists(code)
                    if (exists) {
                        _scanResult.value = ScanState.Error("Serial number already exists")
                    } else {
                        _scanResult.value = ScanState.Success(code, format)
                        
                        // Save scan to history
                        scanRepository.insertScan(
                            ScanHistoryEntity(
                                scannedCode = code,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                is SerialNumberValidator.ValidationResult.Error -> {
                    _scanResult.value = ScanState.Error(validationResult.message)
                }
            }
        }
    }

    fun assignSerialNumberToProduct(productId: Long, serialNumber: String) {
        viewModelScope.launch {
            try {
                // Double-check that serial number doesn't exist
                if (productRepository.isSerialNumberExists(serialNumber)) {
                    _assignmentResult.value = AssignmentState.Error("Serial number already exists")
                    return@launch
                }

                productRepository.updateSerialNumber(productId, serialNumber)
                _assignmentResult.value = AssignmentState.Success(productId, serialNumber)
            } catch (e: Exception) {
                _assignmentResult.value = AssignmentState.Error(e.message ?: "Failed to assign serial number")
            }
        }
    }

    fun resetScanState() {
        _scanResult.value = ScanState.Idle
    }

    fun resetAssignmentState() {
        _assignmentResult.value = AssignmentState.Idle
    }

    sealed class ScanState {
        object Idle : ScanState()
        data class Success(val code: String, val format: Int) : ScanState()
        data class Error(val message: String) : ScanState()
    }

    sealed class AssignmentState {
        object Idle : AssignmentState()
        data class Success(val productId: Long, val serialNumber: String) : AssignmentState()
        data class Error(val message: String) : AssignmentState()
    }
}
