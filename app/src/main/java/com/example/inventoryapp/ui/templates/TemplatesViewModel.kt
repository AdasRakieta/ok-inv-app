package com.example.inventoryapp.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.inventoryapp.data.local.entities.ProductTemplateEntity
import com.example.inventoryapp.data.repository.ProductTemplateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TemplatesViewModel(
    private val repository: ProductTemplateRepository
) : ViewModel() {

    private val _templates = MutableStateFlow<List<ProductTemplateEntity>>(emptyList())
    val templates: StateFlow<List<ProductTemplateEntity>> = _templates

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            repository.getAllTemplates().collect { templateList ->
                _templates.value = templateList
            }
        }
    }

    fun deleteTemplate(template: ProductTemplateEntity) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
        }
    }

    suspend fun isTemplateNameExists(name: String): Boolean {
        return repository.isTemplateNameExists(name)
    }

    fun addTemplate(name: String, categoryId: Long?, description: String?) {
        viewModelScope.launch {
            val template = ProductTemplateEntity(
                name = name,
                categoryId = categoryId,
                description = description
            )
            repository.insertTemplate(template)
        }
    }

    fun updateTemplate(templateId: Long, name: String, categoryId: Long?, description: String?) {
        viewModelScope.launch {
            val template = ProductTemplateEntity(
                id = templateId,
                name = name,
                categoryId = categoryId,
                description = description,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateTemplate(template)
        }
    }
}
