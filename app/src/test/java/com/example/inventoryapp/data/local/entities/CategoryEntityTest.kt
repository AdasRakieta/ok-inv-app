package com.example.inventoryapp.data.local.entities

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for CategoryEntity, specifically for the destinationType computed property.
 * 
 * Tests verify that equipment categories are correctly classified as:
 * - OFFICE (Biurowe): for office employee equipment
 * - CONTRACTOR (Kontrahenckie): for contractor point equipment (CP/CC/DC)
 */
class CategoryEntityTest {

    @Test
    fun `destinationType returns OFFICE for parentId 1`() {
        // Given: A category with parentId = 1 (Urządzenia Biurowe)
        val category = CategoryEntity(
            id = 10L,
            name = "Laptop",
            description = "Office laptop",
            parentId = 1L
        )

        // When: Accessing destinationType
        val result = category.destinationType

        // Then: Should return OFFICE
        assertEquals(DestinationType.OFFICE, result)
    }

    @Test
    fun `destinationType returns CONTRACTOR for parentId 2`() {
        // Given: A category with parentId = 2 (Urządzenia dla Kontrahentów)
        val category = CategoryEntity(
            id = 20L,
            name = "Skaner",
            description = "Scanner for contractors",
            parentId = 2L
        )

        // When: Accessing destinationType
        val result = category.destinationType

        // Then: Should return CONTRACTOR
        assertEquals(DestinationType.CONTRACTOR, result)
    }

    @Test
    fun `destinationType returns OFFICE for null parentId (default)`() {
        // Given: A category with no parent (root category or uncategorized)
        val category = CategoryEntity(
            id = 30L,
            name = "Uncategorized",
            description = "No parent category",
            parentId = null
        )

        // When: Accessing destinationType
        val result = category.destinationType

        // Then: Should default to OFFICE
        assertEquals(DestinationType.OFFICE, result)
    }

    @Test
    fun `destinationType returns OFFICE for unknown parentId`() {
        // Given: A category with a parentId that is not 1 or 2
        val category = CategoryEntity(
            id = 40L,
            name = "Other Equipment",
            description = "Unknown parent category",
            parentId = 999L
        )

        // When: Accessing destinationType
        val result = category.destinationType

        // Then: Should default to OFFICE
        assertEquals(DestinationType.OFFICE, result)
    }

    @Test
    fun `destinationType is computed property and reflects parentId changes`() {
        // Given: A category initially without parent
        var category = CategoryEntity(
            id = 50L,
            name = "Monitor",
            description = "Display monitor"
        )

        // Then: Should default to OFFICE
        assertEquals(DestinationType.OFFICE, category.destinationType)

        // When: Category is updated with parentId = 1
        category = category.copy(parentId = 1L)

        // Then: Should remain OFFICE
        assertEquals(DestinationType.OFFICE, category.destinationType)

        // When: Category is updated with parentId = 2
        category = category.copy(parentId = 2L)

        // Then: Should change to CONTRACTOR
        assertEquals(DestinationType.CONTRACTOR, category.destinationType)
    }

    @Test
    fun `office equipment categories have correct destinationType`() {
        // Test all typical office equipment categories
        val officeCategories = listOf("Laptop", "Monitor", "Telefon", "Tablet", "Klawiatura", "Mysz")
        
        officeCategories.forEach { categoryName ->
            val category = CategoryEntity(
                name = categoryName,
                description = "Office equipment: $categoryName",
                parentId = 1L
            )
            
            assertEquals(
                "Category '$categoryName' should have OFFICE destination type",
                DestinationType.OFFICE,
                category.destinationType
            )
        }
    }

    @Test
    fun `contractor equipment categories have correct destinationType`() {
        // Test all typical contractor equipment categories
        val contractorCategories = listOf(
            "Skaner",
            "Drukarka mobilna",
            "Stacja dokująca Skaner",
            "Stacja dokująca drukarka"
        )
        
        contractorCategories.forEach { categoryName ->
            val category = CategoryEntity(
                name = categoryName,
                description = "Contractor equipment: $categoryName",
                parentId = 2L
            )
            
            assertEquals(
                "Category '$categoryName' should have CONTRACTOR destination type",
                DestinationType.CONTRACTOR,
                category.destinationType
            )
        }
    }
}
