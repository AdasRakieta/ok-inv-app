---
name: test-driven-development
description: Use when implementing any feature or bugfix in Android app, before writing implementation code
---

# Test-Driven Development (TDD) for Android

## Overview

Write the test first. Watch it fail. Write minimal code to pass.

**Core principle:** If you didn't watch the test fail, you don't know if it tests the right thing.

**This applies to:**
- New Android features (Activities, Fragments, ViewModels, Services)
- Bug fixes in existing code
- Refactoring Kotlin/Java code
- Behavior changes in UI or business logic

## The Iron Law

```
NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST
```

Write code before the test? Delete it. Start over.

## Red-Green-Refactor for Android

### RED - Write Failing Test

**For Unit Tests (ViewModel, Repository, Domain Logic):**
```kotlin
@Test
fun `loadEquipmentList emits success state with data`() = runTest {
    // Given
    val expected = listOf(Equipment(id = 1, name = "Laptop"))
    coEvery { repository.getEquipment() } returns Result.success(expected)
    
    // When
    viewModel.loadEquipmentList()
    
    // Then
    val state = viewModel.uiState.value
    assertTrue(state is UiState.Success)
    assertEquals(expected, (state as UiState.Success).data)
}
```

**For Instrumented Tests (UI, Database, Integration):**
```kotlin
@Test
fun scannerScreen_displaysBarcodeResult() {
    // Given
    val barcode = "ABC123"
    
    // When
    composeTestRule.setContent {
        ScannerScreen(onBarcodeScanned = { /* no-op */ })
    }
    
    // Simulate barcode scan
    composeTestRule.onNodeWithTag("barcode_input").performTextInput(barcode)
    composeTestRule.onNodeWithTag("scan_button").performClick()
    
    // Then
    composeTestRule.onNodeWithText(barcode).assertIsDisplayed()
}
```

### Verify RED - Watch It Fail

**Run the test:**
```bash
# Unit tests
./gradlew test

# Instrumented tests  
./gradlew connectedAndroidTest

# Specific test
./gradlew test --tests "ScannerViewModelTest.loadEquipmentList*"
```

Confirm:
- Test fails (not errors)
- Failure message is expected
- Fails because feature missing (not typos)

### GREEN - Write Minimal Code

**ViewModel Example:**
```kotlin
class ScannerViewModel(
    private val repository: EquipmentRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    fun loadEquipmentList() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = repository.getEquipment()
            _uiState.value = when {
                result.isSuccess -> UiState.Success(result.getOrNull()!!)
                else -> UiState.Error(result.exceptionOrNull()?.message)
            }
        }
    }
}
```

### REFACTOR - Clean Up

After green only:
- Extract reusable components
- Improve naming
- Remove duplication
- Keep tests green

## Android-Specific Testing Patterns

### Testing ViewModels

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: ScannerViewModel
    private val repository: EquipmentRepository = mockk()
    
    @Before
    fun setup() {
        viewModel = ScannerViewModel(repository)
    }
    
    @Test
    fun `test scenario`() = runTest {
        // Test implementation
    }
}
```

### Testing Room Database

```kotlin
@Test
fun insertEquipment_andRetrieveById() = runBlocking {
    // Given
    val equipment = Equipment(id = 1, name = "Laptop", barcode = "ABC123")
    
    // When
    db.equipmentDao().insert(equipment)
    val retrieved = db.equipmentDao().getById(1)
    
    // Then
    assertEquals(equipment, retrieved)
}
```

### Testing Compose UI

```kotlin
@Test
fun equipmentList_displaysItems() {
    val items = listOf(
        Equipment(id = 1, name = "Laptop"),
        Equipment(id = 2, name = "Mouse")
    )
    
    composeTestRule.setContent {
        EquipmentList(items = items)
    }
    
    composeTestRule.onNodeWithText("Laptop").assertIsDisplayed()
    composeTestRule.onNodeWithText("Mouse").assertIsDisplayed()
}
```

## Project-Specific Commands

```bash
# Run all unit tests
./gradlew test

# Run with coverage
./gradlew testDebugUnitTestCoverage

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "ScannerViewModelTest"

# Run single test method
./gradlew test --tests "ScannerViewModelTest.loadEquipmentList*"
```

## Common Android Testing Anti-Patterns

| Anti-Pattern | Why It's Wrong | Correct Approach |
|-------------|----------------|------------------|
| Testing LiveData with `getValue()` | Doesn't wait for async updates | Use `getOrAwaitValue()` test extension |
| Not using `runTest` for coroutines | Tests don't wait for coroutines | Wrap in `runTest { }` |
| Mocking ViewModels in UI tests | Tests mock, not real code | Test real ViewModel with test repository |
| No Espresso idling resources | UI tests fail randomly | Add idling resources for async operations |

## Dependencies Already Configured

Your project has:
- **JUnit 4.13.2** - Unit testing
- **Mockito 4.4.0** + mockito-kotlin - Mocking
- **Coroutines Test 1.6.0** - Async testing
- **Room Testing** - Database testing
- **Espresso 3.4.0** - UI testing
- **AndroidX Test 1.4.0** - Test infrastructure

## Red Flags - STOP and Start Over

- Code before test
- Test passes immediately
- Can't explain why test failed
- Skipping verification steps
- "I'll test it manually first"
- "Too simple to test"

**All of these mean: Delete code. Start over with TDD.**

## Verification Checklist

Before marking work complete:

- [ ] Every new function/method has a test
- [ ] Watched each test fail before implementing
- [ ] Each test failed for expected reason
- [ ] Wrote minimal code to pass each test
- [ ] All tests pass
- [ ] No errors or warnings in output
- [ ] Edge cases and errors covered

Can't check all boxes? You skipped TDD. Start over.

---

**Adapted from:** obra/superpowers test-driven-development skill
**Project:** ok-inv-app (Android Kotlin)
