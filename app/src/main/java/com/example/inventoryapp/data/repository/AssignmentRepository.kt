package com.example.inventoryapp.data.repository

import com.example.inventoryapp.data.local.dao.EquipmentAssignmentDao
import com.example.inventoryapp.data.local.dao.EquipmentDao
import com.example.inventoryapp.data.local.entities.EquipmentAssignmentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssignmentRepository(
    private val assignmentDao: EquipmentAssignmentDao,
    private val equipmentDao: EquipmentDao
) {
    
    suspend fun getActiveAssignments(): List<EquipmentAssignmentEntity> = withContext(Dispatchers.IO) {
        assignmentDao.getActiveAssignments()
    }
    
    suspend fun getActiveAssignmentsForEmployee(employeeId: Long): List<EquipmentAssignmentEntity> = withContext(Dispatchers.IO) {
        assignmentDao.getActiveAssignmentsForEmployee(employeeId)
    }
    
    suspend fun getActiveAssignmentForEquipment(equipmentId: Long): EquipmentAssignmentEntity? = withContext(Dispatchers.IO) {
        assignmentDao.getActiveAssignmentForEquipment(equipmentId)
    }
    
    /**
     * Assign equipment to employee
     * Automatically updates equipment status to ASSIGNED
     */
    suspend fun assignEquipment(
        employeeId: Long,
        equipmentId: Long,
        note: String? = null
    ): Long = withContext(Dispatchers.IO) {
        // Check if equipment is already assigned
        val existingAssignment = assignmentDao.getActiveAssignmentForEquipment(equipmentId)
        if (existingAssignment != null) {
            throw IllegalStateException("Equipment is already assigned to employee ${existingAssignment.employeeId}")
        }
        
        // Create assignment
        val assignment = EquipmentAssignmentEntity(
            employeeId = employeeId,
            equipmentId = equipmentId,
            assignedAt = System.currentTimeMillis(),
            note = note
        )
        val assignmentId = assignmentDao.insert(assignment)
        
        // Update equipment status
        val equipment = equipmentDao.getById(equipmentId)
        equipment?.let {
            equipmentDao.update(it.copy(
                status = "ASSIGNED",
                updatedAt = System.currentTimeMillis()
            ))
        }
        
        assignmentId
    }
    
    /**
     * Return equipment from employee
     * Automatically updates equipment status to AVAILABLE
     */
    suspend fun returnEquipment(
        equipmentId: Long,
        note: String? = null
    ) = withContext(Dispatchers.IO) {
        val assignment = assignmentDao.getActiveAssignmentForEquipment(equipmentId)
            ?: throw IllegalStateException("No active assignment found for equipment $equipmentId")
        
        // Mark assignment as returned
        val updatedAssignment = assignment.copy(
            returnedAt = System.currentTimeMillis(),
            note = note ?: assignment.note
        )
        assignmentDao.update(updatedAssignment)
        
        // Update equipment status back to AVAILABLE
        val equipment = equipmentDao.getById(equipmentId)
        equipment?.let {
            equipmentDao.update(it.copy(
                status = "AVAILABLE",
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
