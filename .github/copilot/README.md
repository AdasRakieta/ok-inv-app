# Superpowers for ok-inv-app

Development workflow skills and agents adapted from [obra/superpowers](https://github.com/obra/superpowers) for Android mobile development.

## Overview

This directory contains **skills** (development methodologies) and **agents** (specialized AI assistants) that guide the development workflow for the ok-inv-app Android inventory management application.

## Installation Date
**Installed:** 2026-04-03  
**Source:** https://github.com/obra/superpowers  
**Adapted for:** Android Kotlin development with MVVM architecture

## Core Workflow

```
📋 Brainstorming → 📝 Writing Plans → 🔨 Implementation (TDD) → 🐛 Debugging → 👀 Code Review
```

### 1. Brainstorming (Before Any Feature Work)
**Skill:** `.github/copilot/skills/brainstorming.md`

**When:** Before creating any new Android feature, component, or modifying existing behavior

**Purpose:** Transform ideas into detailed Android app designs through collaborative dialogue

**Process:**
1. Explore current project context (Activities, Fragments, ViewModels, layouts)
2. Ask clarifying questions about UI/UX, data flow, navigation
3. Propose 2-3 approaches with Android-specific trade-offs
4. Present design in sections (UI, Architecture, Data Layer, Testing)
5. Get approval and write design doc to `docs/superpowers/specs/`

**Hard Rule:** NO code, layouts, or fragments until design is approved

### 2. Test-Driven Development (TDD)
**Skill:** `.github/copilot/skills/test-driven-development.md`

**When:** Implementing any feature, bug fix, or refactoring

**Iron Law:** NO PRODUCTION CODE WITHOUT A FAILING TEST FIRST

**Red-Green-Refactor Cycle:**
```bash
# RED - Write failing test
./gradlew test --tests "ScannerViewModelTest"

# GREEN - Minimal implementation
# Write just enough code to pass

# REFACTOR - Clean up
# Keep tests green while improving code
```

**Android Testing Stack:**
- **Unit Tests:** ViewModels, Repositories, Domain logic (JUnit, Mockito, Coroutines Test)
- **Instrumented Tests:** Database, UI, Integration (Room Testing, Espresso)
- **Compose UI Tests:** If using Jetpack Compose

### 3. Systematic Debugging
**Skill:** `.github/copilot/skills/systematic-debugging.md`

**When:** Any bug, test failure, crash, ANR, or unexpected behavior

**Iron Law:** NO FIXES WITHOUT ROOT CAUSE INVESTIGATION FIRST

**Four Phases:**
1. **Root Cause Investigation** - Logcat, stack traces, reproduction steps
2. **Pattern Analysis** - Compare against working code and Android docs
3. **Hypothesis Testing** - Test one variable at a time
4. **Implementation** - Write failing test, fix, verify

**Android-Specific Tools:**
```bash
# View app logs
adb logcat -s "ok.inv:*"

# Memory profiling
adb shell am dumpheap com.ok.inv /sdcard/heap.hprof

# Inspect database
adb shell "run-as com.ok.inv cat /data/data/com.ok.inv/databases/equipment.db" > equipment.db
```

### 4. Code Review
**Agent:** `.github/copilot/agents/code-reviewer.md`

**When:** After completing a major feature or project step

**Reviews:**
- Plan alignment (did implementation match design?)
- Android architecture patterns (MVVM, Repository)
- Code quality (ViewModels, coroutines, lifecycle)
- Performance (memory leaks, threading)
- Testing coverage and quality

**Invocation Example:**
> "I've completed the batch scanning feature from step 3. Please review against the plan."

## Directory Structure

```
.github/copilot/
├── README.md                          # This file
├── skills/                            # Development methodologies
│   ├── brainstorming.md              # Feature design workflow
│   ├── test-driven-development.md    # TDD for Android
│   └── systematic-debugging.md        # Debugging methodology
└── agents/                            # Specialized AI assistants
    └── code-reviewer.md              # Code review agent

docs/superpowers/
├── specs/                            # Design documents
│   └── YYYY-MM-DD-feature-name-android-design.md
└── plans/                            # Implementation plans
    └── YYYY-MM-DD-feature-name.md
```

## Project-Specific Context

### Technology Stack
- **Language:** Kotlin 1.6.21
- **Architecture:** MVVM + Repository Pattern
- **Database:** Room 2.4.2
- **Async:** Coroutines 1.6.0
- **UI:** XML Layouts + Material Design 3
- **Camera:** CameraX 1.1.0 + ML Kit Barcode 17.0.2
- **Testing:** JUnit 4.13.2, Mockito 4.4.0, Espresso 3.4.0

### Key Architectural Patterns
```
ui/
  ├── ScannerFragment.kt           # UI Layer
  └── ScannerViewModel.kt          # Presentation Logic
  
data/
  ├── repository/
  │   └── EquipmentRepository.kt   # Data Access Abstraction
  ├── local/
  │   ├── EquipmentDao.kt          # Room DAO
  │   └── EquipmentEntity.kt       # Database Entity
  └── model/
      └── Equipment.kt             # Domain Model
```

### Build Commands
```bash
# Quick build and deploy
./gradlew quickDeploy

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Build release APK
./gradlew assembleRelease
```

### Testing Patterns
```kotlin
// ViewModel Unit Test
@Test
fun `loadEquipment emits success state`() = runTest {
    // Given
    coEvery { repository.getEquipment() } returns Result.success(data)
    
    // When
    viewModel.loadEquipment()
    
    // Then
    val state = viewModel.uiState.value
    assertTrue(state is UiState.Success)
}

// Database Test
@Test
fun insertAndRetrieve() = runBlocking {
    val equipment = Equipment(id = 1, name = "Laptop")
    db.equipmentDao().insert(equipment)
    val retrieved = db.equipmentDao().getById(1)
    assertEquals(equipment, retrieved)
}
```

## How to Use These Skills

### Starting a New Feature

1. **Brainstorm the design:**
   ```
   User: "I want to add batch scanning capability"
   AI: "I'm using the brainstorming skill to explore this feature..."
   ```

2. **AI will:**
   - Ask clarifying questions about UI, data flow, navigation
   - Propose 2-3 approaches with trade-offs
   - Present detailed Android-specific design
   - Save design to `docs/superpowers/specs/`

3. **Once design approved:**
   - AI creates implementation plan
   - Breaks work into bite-sized TDD tasks
   - Each task: write test → watch fail → implement → verify → commit

### Fixing a Bug

1. **Systematic debugging:**
   ```
   User: "App crashes when scanning barcode"
   AI: "I'm using the systematic-debugging skill..."
   ```

2. **AI will:**
   - Check logcat for stack trace
   - Reproduce consistently
   - Trace root cause (not just symptoms)
   - Write failing test
   - Implement fix
   - Verify on device

### After Completing Work

1. **Code review:**
   ```
   User: "Finished batch scanning feature from step 3"
   AI: "Let me review this against the plan using code-reviewer agent"
   ```

2. **Agent will:**
   - Compare implementation to design
   - Check Android best practices
   - Identify issues (CRITICAL/IMPORTANT/SUGGESTIONS)
   - Verify test coverage

## Benefits for ok-inv-app

### 1. Reduced Bugs
- TDD catches issues before they reach devices
- Systematic debugging finds root causes, not symptoms
- Code review ensures Android best practices

### 2. Better Architecture
- Brainstorming enforces design-first thinking
- Plans ensure MVVM and Repository patterns followed
- No ad-hoc implementations that violate patterns

### 3. Maintainable Code
- Every feature has design doc and tests
- Clear separation: UI → ViewModel → Repository → Database
- Future developers understand intent

### 4. Faster Development
- Systematic workflows prevent thrashing
- Design approval prevents wasted implementation
- TDD is faster than debugging in production

## Android-Specific Considerations

### Memory Management
Skills enforce:
- No Context/View references in ViewModels
- Proper lifecycle scoping (viewModelScope, lifecycleScope)
- Leak-free coroutine usage

### Threading
Skills enforce:
- No network/database work on main thread
- Proper coroutine dispatchers (Main, IO, Default)
- LiveData/StateFlow for thread-safe UI updates

### Testing Strategy
Skills enforce:
- Unit tests for ViewModels and Repositories
- Instrumented tests for Room database
- UI tests for critical user flows
- Minimum 70% code coverage

## Common Scenarios

### Scenario 1: Adding New Screen

1. **Brainstorming** designs UI, navigation, ViewModel, data flow
2. **Writing Plans** breaks into tasks: Fragment → ViewModel → Repository → Tests
3. **TDD** implements with tests first
4. **Code Review** verifies architecture and patterns

### Scenario 2: Fixing ANR

1. **Systematic Debugging** traces what's blocking main thread
2. Identifies root cause (e.g., Room query on main thread)
3. **TDD** writes test proving fix (coroutine dispatcher)
4. **Code Review** verifies threading patterns

### Scenario 3: Refactoring Camera Code

1. **Brainstorming** evaluates CameraX vs Camera2 trade-offs
2. **Writing Plans** sequences migration steps
3. **TDD** migrates with tests ensuring behavior unchanged
4. **Code Review** confirms no regressions

## Integration with GitHub Copilot

These skills and agents are designed to work with **GitHub Copilot CLI** in VSCode or terminal.

**Trigger phrases:**
- "Let's brainstorm [feature]" → activates brainstorming
- "Debug [issue]" → activates systematic-debugging
- "Review this code" → activates code-reviewer agent

## Updating Skills

To update skills from upstream:

```bash
# Fetch latest from obra/superpowers
# Review changes for Android-specific adaptations needed
# Update files in .github/copilot/
```

## Contributing

When adding new skills or agents:

1. Follow existing file structure and naming
2. Include Android-specific examples and patterns
3. Reference ok-inv-app's architecture and tech stack
4. Test with real feature work before committing

## Resources

- **Superpowers Origin:** https://github.com/obra/superpowers
- **Android Docs:** https://developer.android.com
- **MVVM Guide:** https://developer.android.com/topic/architecture
- **Testing Guide:** https://developer.android.com/training/testing

---

**Adapted with ❤️ from obra/superpowers for Android development**
