# Equipment Distribution System - Android Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement comprehensive equipment distribution system with multi-company support, contractor points (CP/CC/DC), and intelligent equipment assignment based on categories.

**Architecture:** MVVM with Repository pattern, Room database extensions, hierarchical category system with automatic destination type detection.

**Tech Stack:** Kotlin 1.6.21, Room 2.4.2, Coroutines 1.6.0, Material Design 3, Navigation Component

---

## Problem Statement

Obecny system obsługuje wydawanie sprzętu tylko pracownikom z pojedynczej organizacji. Potrzebujemy rozszerzyć go o:

1. **Wielofirmowość** - pracownicy i punkty należą do różnych firm
2. **Punkty kontrahenckie** - CP (zbiórki), CC (liczenia), DC (dystrybucji) jako odrębni odbiorcy sprzętu
3. **Inteligentne reguły wydawania**:
   - Pracownicy mogą otrzymać sprzęt biurowy i kontrahencki
   - Punkty CP/CC/DC mogą otrzymać TYLKO sprzęt kontrahencki
4. **Kategoryzacja sprzętu** przez hierarchię kategorii:
   - Urządzenia Biurowe → Laptop, Monitor, Telefon
   - Urządzenia dla Kontrahentów → Skaner, Drukarka mobilna, Stacja dokująca

---

## Design Overview

### Database Schema Changes

#### New Entities

**CompanyEntity** (`companies` table)
```kotlin
@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "tax_id") val taxId: String?, // NIP
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "city") val city: String?,
    @ColumnInfo(name = "postal_code") val postalCode: String?,
    @ColumnInfo(name = "country") val country: String? = "Polska",
    @ColumnInfo(name = "contact_person") val contactPerson: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

**ContractorPointEntity** (`contractor_points` table)
```kotlin
enum class PointType {
    CP,  // Collection Point - punkt zbiórki
    CC,  // Counting Point - punkt liczenia
    DC   // Distribution Center - punkt dystrybucji
}

@Entity(
    tableName = "contractor_points",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("company_id"), Index("code", unique = true)]
)
data class ContractorPointEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "code") val code: String, // Unique identifier like "CP001"
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "point_type") val pointType: PointType,
    @ColumnInfo(name = "company_id") val companyId: Long,
    @ColumnInfo(name = "address") val address: String?,
    @ColumnInfo(name = "city") val city: String?,
    @ColumnInfo(name = "postal_code") val postalCode: String?,
    @ColumnInfo(name = "contact_person") val contactPerson: String?,
    @ColumnInfo(name = "email") val email: String?,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
```

#### Modified Entities

**EmployeeEntity** - Add company relationship
```kotlin
// Add field:
@ColumnInfo(name = "company_id") val companyId: Long?

// Add foreign key constraint in @Entity annotation
```

**ProductEntity** - Add contractor point assignment
```kotlin
// Add fields for contractor point assignment:
@ColumnInfo(name = "assigned_to_contractor_point_id") val assignedToContractorPointId: Long? = null
```

**CategoryEntity** - Add destination type indicator
```kotlin
enum class DestinationType {
    OFFICE,      // Biurowe - dla pracowników
    CONTRACTOR   // Kontrahenckie - dla CP/CC/DC i pracowników
}

// Add field (derived from parent category):
// This will be computed, not stored
val destinationType: DestinationType
    get() = when (parentId) {
        OFFICE_CATEGORY_PARENT_ID -> DestinationType.OFFICE
        CONTRACTOR_CATEGORY_PARENT_ID -> DestinationType.CONTRACTOR
        else -> if (parentId == null) DestinationType.OFFICE else DestinationType.OFFICE
    }
```

### Assignment Rules Engine

**AssignmentValidator** - Validates equipment assignments
```kotlin
class AssignmentValidator {
    fun canAssignToEmployee(product: ProductEntity, employee: EmployeeEntity): ValidationResult {
        // Employees can receive BOTH office and contractor equipment
        return ValidationResult.Success
    }
    
    fun canAssignToContractorPoint(
        product: ProductEntity, 
        point: ContractorPointEntity,
        category: CategoryEntity
    ): ValidationResult {
        // Contractor points can ONLY receive contractor equipment
        if (category.destinationType == DestinationType.OFFICE) {
            return ValidationResult.Error(
                "Punkty CP/CC/DC mogą otrzymać tylko sprzęt kontrahencki (skanery, drukarki, stacje dokujące)"
            )
        }
        return ValidationResult.Success
    }
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
```

### UI Structure

#### New Screens

1. **CompaniesFragment** - Zarządzanie firmami
   - Lista firm z wyszukiwaniem
   - CRUD operations (Create, Read, Update, Delete)
   - Wyświetlanie liczby pracowników i punktów kontrahenckich dla każdej firmy

2. **ContractorPointsFragment** - Lista punktów CP/CC/DC
   - Filtrowanie według typu (CP/CC/DC) i firmy
   - Wyszukiwanie według kodu, nazwy, miasta
   - Wyświetlanie przypisanego sprzętu

3. **ContractorPointDetailsFragment** - Szczegóły punktu
   - Informacje o punkcie (typ, firma, adres, kontakt)
   - Lista przypisanego sprzętu kontrahenciego
   - Przyciski: "Wydaj sprzęt" (scan/dialog)

4. **AddEditContractorPointFragment** - Tworzenie/edycja punktu
   - Wybór typu (CP/CC/DC)
   - Wybór firmy
   - Dane adresowe i kontaktowe
   - Walidacja kodu (unikalność)

5. **AddEditCompanyFragment** - Tworzenie/edycja firmy
   - Nazwa, NIP, adres, kontakt
   - Walidacja NIP

#### Modified Screens

1. **EmployeesListFragment** - Add company filter
   - Dodaj filtr według firmy
   - Wyświetl nazwę firmy przy pracowniku

2. **EmployeeDetailsFragment** - Show company info
   - Wyświetl nazwę firmy pracownika
   - Zachowaj istniejącą funkcjonalność

3. **AddEmployeeFragment** - Add company selection
   - Dodaj pole wyboru firmy (Spinner/Dropdown)
   - Firma jako wymagane pole

4. **AssignByScanFragment** - Support contractor points
   - Rozszerz o obsługę punktów CP/CC/DC
   - Walidacja według typu odbiorcy (employee vs contractor point)
   - Dynamiczny nagłówek: 👤 Employee, 📦 Location, 📍 Contractor Point

5. **AssignEquipmentDialogFragment** - Filter by destination type
   - Dodaj automatyczne filtrowanie według typu odbiorcy:
     - Employee → pokaż wszystko
     - Contractor Point → pokaż tylko kontrahenckie
   - Wskaźnik typu kategorii (badge: "Biurowe" / "Kontrahenckie")

### Navigation Updates

```
HomeFragment
  ├─ [New] CompaniesFragment
  │  └─ AddEditCompanyFragment
  │
  ├─ [New] ContractorPointsFragment
  │  ├─ ContractorPointDetailsFragment
  │  │  ├─ AssignEquipmentDialogFragment (contractor)
  │  │  └─ AssignByScanFragment (contractorPointId)
  │  └─ AddEditContractorPointFragment
  │
  └─ [Modified] EmployeesListFragment (+ company filter)
```

---

## Implementation Tasks

### Phase 1: Database Layer (Foundation)

#### Task 1.1: Create Company Entity and DAO

**Files:**
- Create: `app/src/main/java/com/ok/inv/data/local/entity/CompanyEntity.kt`
- Create: `app/src/main/java/com/ok/inv/data/local/dao/CompanyDao.kt`
- Modify: `app/src/main/java/com/ok/inv/data/local/AppDatabase.kt`

- [ ] **Step 1: Write failing test for CompanyDao**

```kotlin
// File: app/src/test/java/com/ok/inv/data/local/dao/CompanyDaoTest.kt
@RunWith(AndroidJUnit4::class)
class CompanyDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var companyDao: CompanyDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        companyDao = database.companyDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertCompany_andRetrieveById() = runBlocking {
        // Given
        val company = CompanyEntity(
            name = "OK Polska",
            taxId = "1234567890",
            address = "ul. Testowa 1",
            city = "Warszawa",
            postalCode = "00-001",
            email = "kontakt@okpolska.pl",
            phone = "+48 123 456 789"
        )
        
        // When
        val id = companyDao.insert(company)
        val retrieved = companyDao.getById(id)
        
        // Then
        assertNotNull(retrieved)
        assertEquals("OK Polska", retrieved?.name)
        assertEquals("1234567890", retrieved?.taxId)
    }
    
    @Test
    fun getAllCompanies_returnsAllInserted() = runBlocking {
        // Given
        val companies = listOf(
            CompanyEntity(name = "OK Polska", taxId = "111"),
            CompanyEntity(name = "OK Germany", taxId = "222"),
            CompanyEntity(name = "Partner Logistyka", taxId = "333")
        )
        companies.forEach { companyDao.insert(it) }
        
        // When
        val result = companyDao.getAll()
        
        // Then
        assertEquals(3, result.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "CompanyDaoTest" -i`
Expected: FAIL with compilation errors (CompanyEntity, CompanyDao don't exist)

- [ ] **Step 3: Create CompanyEntity**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/entity/CompanyEntity.kt
package com.ok.inv.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "name") 
    val name: String,
    
    @ColumnInfo(name = "tax_id") 
    val taxId: String?,
    
    @ColumnInfo(name = "address") 
    val address: String?,
    
    @ColumnInfo(name = "city") 
    val city: String?,
    
    @ColumnInfo(name = "postal_code") 
    val postalCode: String?,
    
    @ColumnInfo(name = "country") 
    val country: String? = "Polska",
    
    @ColumnInfo(name = "contact_person") 
    val contactPerson: String?,
    
    @ColumnInfo(name = "email") 
    val email: String?,
    
    @ColumnInfo(name = "phone") 
    val phone: String?,
    
    @ColumnInfo(name = "notes") 
    val notes: String?,
    
    @ColumnInfo(name = "created_at") 
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at") 
    val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 4: Create CompanyDao**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/dao/CompanyDao.kt
package com.ok.inv.data.local.dao

import androidx.room.*
import com.ok.inv.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Query("SELECT * FROM companies ORDER BY name ASC")
    fun getAllFlow(): Flow<List<CompanyEntity>>
    
    @Query("SELECT * FROM companies ORDER BY name ASC")
    suspend fun getAll(): List<CompanyEntity>
    
    @Query("SELECT * FROM companies WHERE id = :id")
    suspend fun getById(id: Long): CompanyEntity?
    
    @Query("SELECT * FROM companies WHERE name LIKE '%' || :query || '%' OR tax_id LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<CompanyEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(company: CompanyEntity): Long
    
    @Update
    suspend fun update(company: CompanyEntity)
    
    @Delete
    suspend fun delete(company: CompanyEntity)
    
    @Query("DELETE FROM companies WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 5: Add CompanyDao to AppDatabase**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/AppDatabase.kt
@Database(
    entities = [
        // ... existing entities
        CompanyEntity::class  // ADD THIS
    ],
    version = 33,  // INCREMENT VERSION
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase {
    // ... existing DAOs
    abstract fun companyDao(): CompanyDao  // ADD THIS
}
```

- [ ] **Step 6: Create database migration 32 → 33**

```kotlin
// In AppDatabase.kt, add migration:
val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS companies (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                tax_id TEXT,
                address TEXT,
                city TEXT,
                postal_code TEXT,
                country TEXT DEFAULT 'Polska',
                contact_person TEXT,
                email TEXT,
                phone TEXT,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// Update database builder to include migration:
.addMigrations(MIGRATION_32_33)
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew test --tests "CompanyDaoTest"`
Expected: PASS (all tests green)

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/ok/inv/data/local/entity/CompanyEntity.kt
git add app/src/main/java/com/ok/inv/data/local/dao/CompanyDao.kt
git add app/src/main/java/com/ok/inv/data/local/AppDatabase.kt
git add app/src/test/java/com/ok/inv/data/local/dao/CompanyDaoTest.kt
git commit -m "feat: add Company entity, DAO, and database migration 32→33

- Created CompanyEntity with full company details (name, NIP, address, contact)
- Implemented CompanyDao with CRUD operations and search
- Added database migration 32→33 to create companies table
- Added comprehensive unit tests for CompanyDao"
```

---

#### Task 1.2: Create ContractorPoint Entity and DAO

**Files:**
- Create: `app/src/main/java/com/ok/inv/data/local/entity/ContractorPointEntity.kt`
- Create: `app/src/main/java/com/ok/inv/data/local/entity/PointType.kt`
- Create: `app/src/main/java/com/ok/inv/data/local/dao/ContractorPointDao.kt`
- Modify: `app/src/main/java/com/ok/inv/data/local/AppDatabase.kt`

- [ ] **Step 1: Write failing test for ContractorPointDao**

```kotlin
// File: app/src/test/java/com/ok/inv/data/local/dao/ContractorPointDaoTest.kt
@RunWith(AndroidJUnit4::class)
class ContractorPointDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var contractorPointDao: ContractorPointDao
    private lateinit var companyDao: CompanyDao
    private var testCompanyId: Long = 0
    
    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contractorPointDao = database.contractorPointDao()
        companyDao = database.companyDao()
        
        // Create test company
        testCompanyId = companyDao.insert(
            CompanyEntity(name = "Test Company", taxId = "123")
        )
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertContractorPoint_andRetrieveById() = runBlocking {
        // Given
        val point = ContractorPointEntity(
            code = "CP001",
            name = "Punkt Zbiórki Warszawa",
            pointType = PointType.CP,
            companyId = testCompanyId,
            city = "Warszawa"
        )
        
        // When
        val id = contractorPointDao.insert(point)
        val retrieved = contractorPointDao.getById(id)
        
        // Then
        assertNotNull(retrieved)
        assertEquals("CP001", retrieved?.code)
        assertEquals(PointType.CP, retrieved?.pointType)
    }
    
    @Test
    fun getPointsByType_filtersCorrectly() = runBlocking {
        // Given
        contractorPointDao.insert(ContractorPointEntity(code = "CP001", name = "CP 1", pointType = PointType.CP, companyId = testCompanyId))
        contractorPointDao.insert(ContractorPointEntity(code = "CC001", name = "CC 1", pointType = PointType.CC, companyId = testCompanyId))
        contractorPointDao.insert(ContractorPointEntity(code = "DC001", name = "DC 1", pointType = PointType.DC, companyId = testCompanyId))
        
        // When
        val cpPoints = contractorPointDao.getByType(PointType.CP)
        
        // Then
        assertEquals(1, cpPoints.size)
        assertEquals("CP001", cpPoints[0].code)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "ContractorPointDaoTest"`
Expected: FAIL (classes don't exist)

- [ ] **Step 3: Create PointType enum**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/entity/PointType.kt
package com.ok.inv.data.local.entity

enum class PointType {
    CP,  // Collection Point - punkt zbiórki
    CC,  // Counting Point - punkt liczenia  
    DC   // Distribution Center - punkt dystrybucji
}
```

- [ ] **Step 4: Create ContractorPointEntity**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/entity/ContractorPointEntity.kt
package com.ok.inv.data.local.entity

import androidx.room.*

@Entity(
    tableName = "contractor_points",
    foreignKeys = [
        ForeignKey(
            entity = CompanyEntity::class,
            parentColumns = ["id"],
            childColumns = ["company_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("company_id"), 
        Index("code", unique = true)
    ]
)
data class ContractorPointEntity(
    @PrimaryKey(autoGenerate = true) 
    val id: Long = 0,
    
    @ColumnInfo(name = "code") 
    val code: String,
    
    @ColumnInfo(name = "name") 
    val name: String,
    
    @ColumnInfo(name = "point_type") 
    val pointType: PointType,
    
    @ColumnInfo(name = "company_id") 
    val companyId: Long,
    
    @ColumnInfo(name = "address") 
    val address: String? = null,
    
    @ColumnInfo(name = "city") 
    val city: String? = null,
    
    @ColumnInfo(name = "postal_code") 
    val postalCode: String? = null,
    
    @ColumnInfo(name = "contact_person") 
    val contactPerson: String? = null,
    
    @ColumnInfo(name = "email") 
    val email: String? = null,
    
    @ColumnInfo(name = "phone") 
    val phone: String? = null,
    
    @ColumnInfo(name = "notes") 
    val notes: String? = null,
    
    @ColumnInfo(name = "created_at") 
    val createdAt: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "updated_at") 
    val updatedAt: Long = System.currentTimeMillis()
)
```

- [ ] **Step 5: Create ContractorPointDao**

```kotlin
// File: app/src/main/java/com/ok/inv/data/local/dao/ContractorPointDao.kt
package com.ok.inv.data.local.dao

import androidx.room.*
import com.ok.inv.data.local.entity.ContractorPointEntity
import com.ok.inv.data.local.entity.PointType
import kotlinx.coroutines.flow.Flow

@Dao
interface ContractorPointDao {
    @Query("SELECT * FROM contractor_points ORDER BY code ASC")
    fun getAllFlow(): Flow<List<ContractorPointEntity>>
    
    @Query("SELECT * FROM contractor_points ORDER BY code ASC")
    suspend fun getAll(): List<ContractorPointEntity>
    
    @Query("SELECT * FROM contractor_points WHERE id = :id")
    suspend fun getById(id: Long): ContractorPointEntity?
    
    @Query("SELECT * FROM contractor_points WHERE code = :code")
    suspend fun getByCode(code: String): ContractorPointEntity?
    
    @Query("SELECT * FROM contractor_points WHERE point_type = :type ORDER BY code ASC")
    suspend fun getByType(type: PointType): List<ContractorPointEntity>
    
    @Query("SELECT * FROM contractor_points WHERE company_id = :companyId ORDER BY code ASC")
    suspend fun getByCompany(companyId: Long): List<ContractorPointEntity>
    
    @Query("""
        SELECT * FROM contractor_points 
        WHERE code LIKE '%' || :query || '%' 
           OR name LIKE '%' || :query || '%'
           OR city LIKE '%' || :query || '%'
        ORDER BY code ASC
    """)
    suspend fun search(query: String): List<ContractorPointEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: ContractorPointEntity): Long
    
    @Update
    suspend fun update(point: ContractorPointEntity)
    
    @Delete
    suspend fun delete(point: ContractorPointEntity)
}
```

- [ ] **Step 6: Add to AppDatabase**

```kotlin
// In AppDatabase.kt:
@Database(
    entities = [
        // ... existing
        CompanyEntity::class,
        ContractorPointEntity::class  // ADD THIS
    ],
    version = 34,  // INCREMENT
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase {
    // ...
    abstract fun contractorPointDao(): ContractorPointDao  // ADD THIS
}
```

- [ ] **Step 7: Create migration 33 → 34**

```kotlin
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS contractor_points (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                code TEXT NOT NULL,
                name TEXT NOT NULL,
                point_type TEXT NOT NULL,
                company_id INTEGER NOT NULL,
                address TEXT,
                city TEXT,
                postal_code TEXT,
                contact_person TEXT,
                email TEXT,
                phone TEXT,
                notes TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(company_id) REFERENCES companies(id) ON DELETE RESTRICT
            )
        """.trimIndent())
        
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_contractor_points_code ON contractor_points(code)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_contractor_points_company_id ON contractor_points(company_id)")
    }
}

// Add to migrations list
.addMigrations(MIGRATION_32_33, MIGRATION_33_34)
```

- [ ] **Step 8: Run tests**

Run: `./gradlew test --tests "ContractorPointDaoTest"`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/ok/inv/data/local/entity/ContractorPointEntity.kt
git add app/src/main/java/com/ok/inv/data/local/entity/PointType.kt
git add app/src/main/java/com/ok/inv/data/local/dao/ContractorPointDao.kt
git add app/src/main/java/com/ok/inv/data/local/AppDatabase.kt
git add app/src/test/java/com/ok/inv/data/local/dao/ContractorPointDaoTest.kt
git commit -m "feat: add ContractorPoint entity with CP/CC/DC types

- Created ContractorPointEntity with company relationship
- Implemented PointType enum (CP, CC, DC)
- Created ContractorPointDao with filtering by type and company
- Added migration 33→34 for contractor_points table
- Added unique constraint on point code
- Comprehensive tests for contractor point operations"
```

---

### Phase 2: Repository Layer & Business Logic

*(Continuing with remaining tasks...)*

---

## Database Migrations Summary

| Migration | Changes |
|-----------|---------|
| 32 → 33 | Create `companies` table |
| 33 → 34 | Create `contractor_points` table with FK to companies |
| 34 → 35 | Add `company_id` to `employees` table |
| 35 → 36 | Add `assigned_to_contractor_point_id` to `products` table |
| 36 → 37 | Update default categories with parent hierarchy (Biurowe/Kontrahenckie) |

---

## Testing Strategy

### Unit Tests
- CompanyDao operations (CRUD, search)
- ContractorPointDao operations (filter by type, company)
- EmployeeDao with company filter
- ProductDao with contractor point assignment
- AssignmentValidator rules

### Instrumented Tests
- Database migrations (32→37)
- Foreign key constraints
- Category hierarchy queries
- End-to-end assignment workflow

### UI Tests (Espresso)
- Company management flow
- Contractor point creation
- Equipment assignment to CP/CC/DC
- Filtering and validation

---

## Success Criteria

✅ **Database**
- Companies table with full contact details
- Contractor points table with CP/CC/DC types
- Employees linked to companies
- Products assignable to contractor points
- All migrations tested and working

✅ **Business Logic**
- Assignment validator prevents office equipment → CP/CC/DC
- Assignment validator allows contractor equipment → anyone
- Search and filtering by company, point type

✅ **UI/UX**
- Companies management screen (CRUD)
- Contractor points management screen (CRUD)
- Enhanced employee screen with company display
- Assignment dialogs with intelligent filtering
- Visual indicators for equipment categories (Biurowe/Kontrahenckie)

✅ **Data Integrity**
- Foreign key constraints enforced
- Unique codes for contractor points
- Proper cascade/restrict rules

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Complex migrations break existing data | Test migrations on copy of production DB, fallback to destructive migration |
| Assignment validation too restrictive | Make rules configurable, add override mechanism |
| Performance with large number of points | Add pagination, indexed queries |
| User confusion with new UI | Clear labels, help text, visual indicators |

---

**Plan created:** 2026-04-03  
**Estimated effort:** 8-12 development days  
**Database version:** 32 → 37 (5 migrations)  
**New screens:** 5 (Companies, ContractorPoints, Details, Add/Edit × 2)  
**Modified screens:** 3 (EmployeesList, EmployeeDetails, AssignByScan)
