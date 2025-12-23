package com.example.bestapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.data.*
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class ProfileSettingsFragment : Fragment() {

    private val skills = mutableListOf<Skill>()
    private val zones = mutableListOf<WorkZone>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        val recyclerSkills = view.findViewById<RecyclerView>(R.id.recycler_skills)
        val recyclerZones = view.findViewById<RecyclerView>(R.id.recycler_zones)
        
        recyclerSkills?.layoutManager = LinearLayoutManager(context)
        recyclerZones?.layoutManager = LinearLayoutManager(context)
        
        val btnAddSkill = view.findViewById<MaterialButton>(R.id.btn_add_skill)
        val btnAddZone = view.findViewById<MaterialButton>(R.id.btn_add_zone)
        val btnEditSchedule = view.findViewById<MaterialButton>(R.id.btn_edit_schedule)
        val btnSave = view.findViewById<MaterialButton>(R.id.btn_save)
        
        btnAddSkill?.setOnClickListener {
            showAddSkillDialog()
        }
        
        btnAddZone?.setOnClickListener {
            showAddZoneDialog()
        }
        
        btnEditSchedule?.setOnClickListener {
            Toast.makeText(context, "Редактирование графика (в разработке)", Toast.LENGTH_SHORT).show()
        }
        
        btnSave?.setOnClickListener {
            saveProfile()
        }
    }
    
    private fun showAddSkillDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_skill, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_skill_name)
        val expInput = dialogView.findViewById<TextInputEditText>(R.id.input_experience)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить навык")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameInput.text.toString()
                val exp = expInput.text.toString().toIntOrNull() ?: 0
                
                if (name.isNotBlank()) {
                    skills.add(Skill(UUID.randomUUID().toString(), name, exp))
                    updateRecyclerViews()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showAddZoneDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_zone, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_zone_name)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Добавить зону работы")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val name = nameInput.text.toString()
                
                if (name.isNotBlank()) {
                    zones.add(WorkZone(UUID.randomUUID().toString(), name, ZoneType.DISTRICT))
                    updateRecyclerViews()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    
    private fun updateRecyclerViews() {
        val recyclerSkills = view?.findViewById<RecyclerView>(R.id.recycler_skills)
        val recyclerZones = view?.findViewById<RecyclerView>(R.id.recycler_zones)
        
        recyclerSkills?.visibility = if (skills.isNotEmpty()) View.VISIBLE else View.GONE
        recyclerZones?.visibility = if (zones.isNotEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun saveProfile() {
        // TODO: Сохранить профиль в репозиторий
        
        Toast.makeText(context, R.string.profile_settings_success, Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }
}

