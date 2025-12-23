package com.example.bestapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegistrationFragment : Fragment() {
    private val viewModel: RegistrationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val nameET = view.findViewById<TextInputEditText>(R.id.full_name_edit_text)
        val emailET = view.findViewById<TextInputEditText>(R.id.email_edit_text)
        val phoneET = view.findViewById<TextInputEditText>(R.id.phone_edit_text)
        val specET = view.findViewById<TextInputEditText>(R.id.specialization_edit_text)
        val pwdET = view.findViewById<TextInputEditText>(R.id.password_edit_text)
        val pwdConfirmET = view.findViewById<TextInputEditText>(R.id.password_confirm_edit_text)
        val registerBtn = view.findViewById<MaterialButton>(R.id.register_button)
        val goToLogin = view.findViewById<View>(R.id.go_to_login)
        val progressBar = view.findViewById<View>(R.id.progress_bar)
        
        // Настройка выбора специализаций (мультивыбор)
        setupSpecializationPicker(specET)
        
        registerBtn.setOnClickListener {
            viewModel.register(
                nameET.text.toString(),
                emailET.text.toString(),
                phoneET.text.toString(),
                specET.text.toString(),
                pwdET.text.toString(),
                pwdConfirmET.text.toString()
            )
        }
        
        goToLogin.setOnClickListener {
            findNavController().navigateUp()
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is RegUiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        registerBtn.isEnabled = false
                    }
                    is RegUiState.Success -> {
                        progressBar.visibility = View.GONE
                        Snackbar.make(view, "Регистрация успешна!", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_registration_to_home)
                    }
                    is RegUiState.Error -> {
                        progressBar.visibility = View.GONE
                        registerBtn.isEnabled = true
                        Snackbar.make(view, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                        registerBtn.isEnabled = true
                    }
                }
            }
        }
    }
    
    /**
     * Диалог с множественным выбором специализаций мастера
     * (основные типы техники из базы знаний).
     */
    private fun setupSpecializationPicker(specET: TextInputEditText) {
        val allSpecs = listOf(
            "Стиральная машина",
            "Посудомоечная машина",
            "Холодильник",
            "Морозильник",
            "Духовой шкаф",
            "Плита",
            "Варочная панель",
            "Микроволновка",
            "Кондиционер",
            "Водонагреватель",
            "Ноутбук",
            "Десктоп",
            "Кофемашина"
        )
        
        fun openDialog() {
            val current = specET.text?.toString()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.toSet() ?: emptySet()
            
            val selected = current.toMutableSet()
            val checked = BooleanArray(allSpecs.size) { index ->
                selected.contains(allSpecs[index])
            }
            
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_specialization)
                .setMultiChoiceItems(allSpecs.toTypedArray(), checked) { _, which, isChecked ->
                    val value = allSpecs[which]
                    if (isChecked) {
                        selected.add(value)
                    } else {
                        selected.remove(value)
                    }
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    specET.setText(selected.joinToString(", "))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        
        specET.setOnClickListener { openDialog() }
        specET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) openDialog()
        }
    }
}







