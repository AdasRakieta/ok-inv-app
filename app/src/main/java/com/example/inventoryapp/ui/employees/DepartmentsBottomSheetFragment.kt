package com.example.inventoryapp.ui.employees

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.inventoryapp.InventoryApplication
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.R
import com.example.inventoryapp.data.local.entities.DepartmentEntity
import com.example.inventoryapp.data.repository.DepartmentRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DepartmentsBottomSheetFragment : BottomSheetDialogFragment() {

    private val companyId: Long
        get() = requireArguments().getLong(ARG_COMPANY_ID, -1L)
    private val companyName: String
        get() = requireArguments().getString(ARG_COMPANY_NAME).orEmpty()
    private val allowUnsaved: Boolean
        get() = requireArguments().getBoolean(ARG_ALLOW_UNSAVED, false)
    private val initialDepartments: ArrayList<String>?
        get() = requireArguments().getStringArrayList(ARG_INITIAL_DEPARTMENTS)

    private lateinit var departmentRepository: DepartmentRepository
    private val departments = mutableListOf<DepartmentEntity>()
    private lateinit var deptAdapter: DepartmentsAdapter
    private var transientMode: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_manage_departments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as InventoryApplication
        departmentRepository = app.departmentRepository

        transientMode = allowUnsaved || companyId <= 0L
        if (companyId <= 0L && !allowUnsaved) {
            dismissAllowingStateLoss()
            return
        }

        val recycler = view.findViewById<RecyclerView>(R.id.departmentRecyclerView)
        val addEdit = view.findViewById<EditText>(R.id.addDepartmentEdit)
        val addButton = view.findViewById<Button>(R.id.addDepartmentButton)
        val title = view.findViewById<android.widget.TextView>(R.id.departmentsTitle)
        title.text = if (companyName.isBlank()) {
            "Zarządzaj działami"
        } else {
            "Działy • $companyName"
        }

        deptAdapter = DepartmentsAdapter(emptyList()) { dept, anchor ->
            showOptions(dept, anchor)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = deptAdapter
        val divider = DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL)
        val dividerDrawable = requireContext().getDrawable(com.example.inventoryapp.R.drawable.divider_department)
        if (dividerDrawable != null) divider.setDrawable(dividerDrawable)
        recycler.addItemDecoration(divider)

        // if transient mode, populate from initialDepartments; otherwise load from DB
        if (transientMode) {
            val cid = if (companyId > 0L) companyId else 0L
            initialDepartments?.forEach { name ->
                departments.add(DepartmentEntity(companyId = cid, name = name))
            }
            deptAdapter.submitList(departments.toList())
        } else {
            lifecycleScope.launchWhenStarted {
                loadDepartments(deptAdapter)
            }
        }

        addButton.setOnClickListener {
            val name = addEdit.text.toString().trim()
            if (name.isNotEmpty()) {
                if (transientMode) {
                    // local-only mode
                    val exists = departments.any { it.name.equals(name, ignoreCase = true) }
                    if (exists) {
                        Toast.makeText(requireContext(), "Dział o takiej nazwie już istnieje", Toast.LENGTH_SHORT).show()
                    } else {
                        val cid = if (companyId > 0L) companyId else 0L
                        val dept = DepartmentEntity(companyId = cid, name = name)
                        departments.add(dept)
                        deptAdapter.submitList(departments.toList())
                        addEdit.text.clear()
                        parentFragmentManager.setFragmentResult("departments_changed", bundleOf("departments" to ArrayList(departments.map { it.name })))
                    }
                } else {
                    lifecycleScope.launch {
                        departmentRepository.insert(companyId, name)
                        loadDepartments(deptAdapter)
                        addEdit.text.clear()
                        parentFragmentManager.setFragmentResult("departments_changed", bundleOf("companyId" to companyId))
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Podaj nazwę działu", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadDepartments(deptAdapter: DepartmentsAdapter) {
        if (transientMode) {
            withContext(Dispatchers.Main) {
                deptAdapter.submitList(departments.toList())
            }
            return
        }

        val list = withContext(Dispatchers.IO) { departmentRepository.getByCompany(companyId) }
        departments.clear()
        departments.addAll(list)
        withContext(Dispatchers.Main) {
            deptAdapter.submitList(departments.toList())
        }
    }

    private fun showOptions(dept: DepartmentEntity, anchor: View) {
        val popup = android.widget.PopupMenu(requireContext(), anchor)
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
                    if (transientMode) {
                        val idx = departments.indexOfFirst { it.name == dept.name }
                        if (idx >= 0) {
                            departments[idx] = departments[idx].copy(name = name)
                            deptAdapter.submitList(departments.toList())
                            Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.setFragmentResult("departments_changed", bundleOf("departments" to ArrayList(departments.map { it.name })))
                        }
                    } else {
                        lifecycleScope.launch {
                            departmentRepository.update(dept.id, name)
                            loadDepartments(deptAdapter)
                            Toast.makeText(requireContext(), "Zapisano zmiany", Toast.LENGTH_SHORT).show()
                            parentFragmentManager.setFragmentResult("departments_changed", bundleOf("companyId" to companyId))
                        }
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
                if (transientMode) {
                    val idx = departments.indexOfFirst { it.name == dept.name }
                    if (idx >= 0) {
                        departments.removeAt(idx)
                        deptAdapter.submitList(departments.toList())
                        Toast.makeText(requireContext(), "Usunięto dział", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult("departments_changed", bundleOf("departments" to ArrayList(departments.map { it.name })))
                    }
                } else {
                    lifecycleScope.launch {
                        departmentRepository.delete(dept.id)
                        loadDepartments(deptAdapter)
                        Toast.makeText(requireContext(), "Usunięto dział", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.setFragmentResult("departments_changed", bundleOf("companyId" to companyId))
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    companion object {
        private const val ARG_COMPANY_ID = "companyId"
        private const val ARG_COMPANY_NAME = "companyName"
        private const val ARG_ALLOW_UNSAVED = "allowUnsaved"
        private const val ARG_INITIAL_DEPARTMENTS = "initialDepartments"

        fun newInstance(
            companyId: Long,
            companyName: String,
            allowUnsaved: Boolean = false,
            initialDepartments: ArrayList<String>? = null
        ): DepartmentsBottomSheetFragment {
            return DepartmentsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_COMPANY_ID, companyId)
                    putString(ARG_COMPANY_NAME, companyName)
                    putBoolean(ARG_ALLOW_UNSAVED, allowUnsaved)
                    if (initialDepartments != null) putStringArrayList(ARG_INITIAL_DEPARTMENTS, initialDepartments)
                }
            }
        }
    }
}
