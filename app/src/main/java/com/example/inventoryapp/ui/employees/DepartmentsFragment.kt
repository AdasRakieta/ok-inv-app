package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.InventoryApplication
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.DepartmentEntity
import com.example.inventoryapp.databinding.FragmentDepartmentsListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class DepartmentsFragment : Fragment() {

    private var _binding: FragmentDepartmentsListBinding? = null
    private val binding get() = _binding!!

    private lateinit var departmentRepository: com.example.inventoryapp.data.repository.DepartmentRepository
    private lateinit var adapter: DepartmentsAdapter
    private val companyId: Long
        get() = arguments?.getLong("companyId", -1L) ?: -1L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDepartmentsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as InventoryApplication
        departmentRepository = app.departmentRepository

        if (companyId <= 0L) {
            Toast.makeText(requireContext(), "Zarządzanie działami dostępne jest z poziomu firmy", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        adapter = DepartmentsAdapter(emptyList()) { dept, anchor -> showOptions(dept, anchor) }

        binding.departmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.departmentsRecyclerView.adapter = adapter

        binding.addDepartmentFab.setOnClickListener { showAddDialog() }

        loadDepartments()
    }

    private fun loadDepartments() {
        lifecycleScope.launch {
            val list = departmentRepository.getByCompany(companyId)
            adapter.submitList(list)
        }
    }

    private fun showAddDialog() {
        val input = EditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dodaj dział")
            .setView(input)
            .setPositiveButton("Dodaj") { dialog, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    lifecycleScope.launch {
                        departmentRepository.insert(companyId, name)
                        loadDepartments()
                        Toast.makeText(requireContext(), "Dodano dział", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showOptions(dept: DepartmentEntity, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Edytuj")
        popup.menu.add("Usuń")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edytuj" -> showEditDialog(dept)
                "Usuń" -> showDeleteConfirm(dept)
            }
            true
        }
        popup.show()
    }

    private fun showEditDialog(dept: DepartmentEntity) {
        val input = EditText(requireContext())
        input.setText(dept.name)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edytuj dział")
            .setView(input)
            .setPositiveButton("Zapisz") { dialog, _ ->
                val name = input.text.toString().trim()
                if (name.isNotBlank()) {
                    lifecycleScope.launch {
                        departmentRepository.update(dept.id, name)
                        loadDepartments()
                        Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun showDeleteConfirm(dept: DepartmentEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Usuń dział")
            .setMessage("Czy na pewno usunąć dział \"${dept.name}\"?")
            .setPositiveButton("Usuń") { dialog, _ ->
                lifecycleScope.launch {
                    departmentRepository.delete(dept.id)
                    loadDepartments()
                    Toast.makeText(requireContext(), "Usunięto dział", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
