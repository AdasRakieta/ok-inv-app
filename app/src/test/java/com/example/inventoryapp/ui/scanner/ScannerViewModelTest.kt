package com.example.inventoryapp.ui.scanner

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.inventoryapp.data.local.entities.ProductEntity
import com.example.inventoryapp.data.repository.ProductRepository
import com.example.inventoryapp.data.repository.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

@ExperimentalCoroutinesApi
class ScannerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var scanRepository: ScanRepository

    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = ScannerViewModel(productRepository, scanRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onBarcodeScanned with valid unique code returns success`() = runTest {
        val code = "ABC123"
        `when`(productRepository.isSerialNumberExists(code)).thenReturn(false)
        `when`(scanRepository.insertScan(any())).thenReturn(1L)

        viewModel.onBarcodeScanned(code, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.scanResult.value
        assertTrue(result is ScannerViewModel.ScanState.Success)
        assertEquals(code, (result as ScannerViewModel.ScanState.Success).code)
    }

    @Test
    fun `onBarcodeScanned with duplicate code returns error`() = runTest {
        val code = "ABC123"
        `when`(productRepository.isSerialNumberExists(code)).thenReturn(true)

        viewModel.onBarcodeScanned(code, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.scanResult.value
        assertTrue(result is ScannerViewModel.ScanState.Error)
        assertEquals("Serial number already exists", (result as ScannerViewModel.ScanState.Error).message)
    }

    @Test
    fun `onBarcodeScanned with empty code returns error`() = runTest {
        val code = ""

        viewModel.onBarcodeScanned(code, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.scanResult.value
        assertTrue(result is ScannerViewModel.ScanState.Error)
    }

    @Test
    fun `onBarcodeScanned with invalid characters returns error`() = runTest {
        val code = "ABC@123"

        viewModel.onBarcodeScanned(code, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        val result = viewModel.scanResult.value
        assertTrue(result is ScannerViewModel.ScanState.Error)
    }

    @Test
    fun `assignSerialNumberToProduct with valid data returns success`() = runTest {
        val productId = 1L
        val serialNumber = "ABC123"
        `when`(productRepository.isSerialNumberExists(serialNumber)).thenReturn(false)

        viewModel.assignSerialNumberToProduct(productId, serialNumber)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(productRepository).updateSerialNumber(productId, serialNumber)
        val result = viewModel.assignmentResult.value
        assertTrue(result is ScannerViewModel.AssignmentState.Success)
    }

    @Test
    fun `assignSerialNumberToProduct with duplicate serial number returns error`() = runTest {
        val productId = 1L
        val serialNumber = "ABC123"
        `when`(productRepository.isSerialNumberExists(serialNumber)).thenReturn(true)

        viewModel.assignSerialNumberToProduct(productId, serialNumber)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(productRepository, never()).updateSerialNumber(any(), any())
        val result = viewModel.assignmentResult.value
        assertTrue(result is ScannerViewModel.AssignmentState.Error)
    }
}
