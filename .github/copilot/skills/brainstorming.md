---
name: brainstorming  
description: "MUST use before any Android feature work - creating features, building components, adding functionality, or modifying behavior"
---

# Brainstorming Android Features

Help turn ideas into fully formed Android app designs and specs through natural collaborative dialogue.

Start by understanding the current project context, then ask questions one at a time to refine the idea. Once you understand what you're building, present the design and get user approval.

<HARD-GATE>
Do NOT write any Android code, modify layouts, create fragments, or take any implementation action until you have presented a design and the user has approved it. This applies to EVERY Android feature regardless of perceived simplicity.
</HARD-GATE>

## Checklist for Android Features

1. **Explore project context** — check existing Activities, Fragments, ViewModels, layouts, navigation graph
2. **Ask clarifying questions** — one at a time, understand feature purpose, UI/UX flow, data requirements
3. **Propose 2-3 approaches** — with Android-specific trade-offs (MVVM vs MVI, XML vs Compose, etc.)
4. **Present design** — in sections: UI/UX, Architecture, Data Layer, Navigation, Testing
5. **Write design doc** — save to `docs/superpowers/specs/YYYY-MM-DD-<feature>-android-design.md`
6. **Spec self-review** — check for placeholders, contradictions, missing Android details
7. **User reviews spec** — ask user to review before proceeding
8. **Transition to implementation** — invoke writing-plans skill

## Android-Specific Design Sections

### 1. UI/UX Design
- Screen layouts and components (RecyclerView, ViewPager, Bottom Sheet, etc.)
- Material Design 3 components
- Navigation flow (Navigation Component graph)
- User interactions and gestures
- Accessibility considerations

### 2. Architecture Design
- MVVM pattern with ViewModel + LiveData/StateFlow
- Repository pattern for data access
- Use cases or interactors if complex
- Dependency injection (if using Hilt/Koin)

### 3. Data Layer Design
- Room database schema changes
- Data entities and DTOs
- Repository interfaces
- Coroutine flows for reactive data

### 4. Integration Points
- Camera/Barcode scanning integration
- Bluetooth printer communication
- Network calls (if applicable)
- Background work (WorkManager if needed)

### 5. Testing Strategy
- Unit tests for ViewModels and Repositories
- Instrumented tests for Database
- UI tests for critical user flows
- Test coverage goals

## Example Android Feature Design Flow

**User Request:** "Add ability to scan multiple barcodes in batch mode"

**Clarifying Questions:**
1. Should users see all scanned items before saving, or save each immediately?
2. What happens if duplicate barcode is scanned?
3. Should there be a limit on batch size?
4. How should users finalize/cancel the batch?

**Proposed Approaches:**

**Option A: Live List with Confirmation**
- Scan adds to list immediately
- RecyclerView shows scanned items
- FAB to finalize batch
- Pro: Immediate feedback, easy to remove mistakes
- Con: More UI complexity

**Option B: Counter with Review Screen**
- Show count during scanning
- Navigate to review screen when done
- Pro: Simpler scanning UI
- Con: Can't fix mistakes until review

**Recommendation: Option A** - Better UX, follows Material Design patterns

**Design Document Sections:**

```markdown
# Batch Barcode Scanning - Android Design

## UI Components

### ScannerBatchFragment
- CameraX preview surface
- RecyclerView for scanned items (bottom sheet style)
- FAB for "Complete Batch"
- Material 3 styling

### BatchReviewFragment  
- List of scanned equipment
- Edit/Remove options
- Confirm button

## Architecture

### ScannerBatchViewModel
```kotlin
class ScannerBatchViewModel : ViewModel() {
    private val _scannedItems = MutableStateFlow<List<Equipment>>(emptyList())
    val scannedItems: StateFlow<List<Equipment>> = _scannedItems
    
    fun addScannedItem(barcode: String)
    fun removeItem(id: Long)
    fun completeBatch()
}
```

### Data Layer
- `EquipmentRepository.addBatch(items: List<Equipment>)`
- Room transaction for batch insert
- Validation logic in domain layer

## Navigation Flow
```
ScannerFragment 
  -> ScannerBatchFragment (new) 
  -> BatchReviewFragment (new)
  -> EquipmentListFragment (existing)
```

## Testing
- ViewModel unit tests: add/remove items, batch completion
- Database tests: batch insert with transaction
- UI tests: scan -> review -> confirm flow
```

## Key Android Patterns to Consider

### When Designing UI:
- **Single Activity + Fragments** vs **Multiple Activities**
- **XML Layouts** vs **Jetpack Compose**
- **Material Design 3** components
- **Configuration changes** (rotation, dark mode)

### When Designing Architecture:
- **ViewModel lifecycle** and scope
- **Kotlin Coroutines** for async operations
- **StateFlow/LiveData** for reactive UI
- **Repository pattern** for data abstraction

### When Designing Data:
- **Room database migrations** if schema changes
- **Offline-first** approach (already in use)
- **Data validation** at domain layer
- **Transaction handling** for multi-step operations

## Project Context (ok-inv-app)

**Existing Architecture:**
- MVVM with Repository pattern
- Room database for offline storage
- Coroutines for async operations
- Navigation Component for fragment navigation
- CameraX + ML Kit for barcode scanning

**Key Files to Check:**
- `app/src/main/java/com/ok/inv/ui/` - Fragments and ViewModels
- `app/src/main/java/com/ok/inv/data/` - Repositories and database
- `app/src/main/res/navigation/` - Navigation graph
- `app/src/main/res/layout/` - XML layouts

## Red Flags - Missing Android Details

Design specs MUST include:
- Fragment/Activity names and relationships
- ViewModel class names and responsibilities
- Room entity changes (if any)
- Navigation graph modifications
- Layout file names
- Testing approach for each layer

Missing any of these? Spec is incomplete.

## After Design Approval

1. Write spec to `docs/superpowers/specs/YYYY-MM-DD-<feature>-android-design.md`
2. Self-review for Android-specific completeness
3. Ask user to review
4. Invoke **writing-plans** skill to create implementation plan

## Integration with Other Skills

- **After brainstorming** → use `writing-plans` for implementation plan
- **During planning** → use `test-driven-development` principles
- **During implementation** → use `systematic-debugging` if issues arise

---

**Adapted from:** obra/superpowers brainstorming skill
**Project:** ok-inv-app (Android Kotlin)
