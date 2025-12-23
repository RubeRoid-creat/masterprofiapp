package com.example.bestapp.ui.settings

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.example.bestapp.auth.AuthManager
import com.example.bestapp.data.PreferencesManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val prefsManager by lazy { PreferencesManager.getInstance(requireContext()) }
    private val apiRepository = ApiRepository()
    
    private var inputName: TextInputEditText? = null
    private var inputPhone: TextInputEditText? = null
    private var inputEmail: TextInputEditText? = null
    private var switchPushNotifications: SwitchMaterial? = null
    private var switchEmailNotifications: SwitchMaterial? = null
    private var btnAutoAcceptSettings: MaterialButton? = null
    private var btnMLM: MaterialButton? = null
    private var btnProfileSettings: MaterialButton? = null
    private var btnLogout: MaterialButton? = null
    private var btnPrivacyPolicy: MaterialButton? = null
    private var btnTerms: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        inputName = view.findViewById(R.id.input_name)
        inputPhone = view.findViewById(R.id.input_phone)
        inputEmail = view.findViewById(R.id.input_email)
        switchPushNotifications = view.findViewById(R.id.switch_push_notifications)
        switchEmailNotifications = view.findViewById(R.id.switch_email_notifications)
        btnAutoAcceptSettings = view.findViewById(R.id.btn_auto_accept_settings)
        btnMLM = view.findViewById(R.id.btn_mlm)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnPrivacyPolicy = view.findViewById(R.id.btn_privacy_policy)
        btnTerms = view.findViewById(R.id.btn_terms)
        
        val textAppVersion = view.findViewById<android.widget.TextView>(R.id.text_app_version)
        
        // Загружаем версию приложения
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            textAppVersion.text = "Версия: ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            textAppVersion.text = "Версия: 1.0.0"
        }
        
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        lifecycleScope.launch {
            // Загружаем данные мастера
            val statsResult = apiRepository.getMasterStats()
            statsResult.onSuccess { response ->
                val masterData = response["master"] as? Map<*, *>
                masterData?.let { master ->
                    master["name"]?.let { inputName?.setText(it.toString()) }
                    master["phone"]?.let { inputPhone?.setText(it.toString()) }
                    master["email"]?.let { inputEmail?.setText(it.toString()) }
                }
            }
            
            // Загружаем настройки уведомлений
            val pushNotificationsEnabled = prefsManager.getBoolean("push_notifications_enabled", true)
            val emailNotificationsEnabled = prefsManager.getBoolean("email_notifications_enabled", false)
            
            switchPushNotifications?.isChecked = pushNotificationsEnabled
            switchEmailNotifications?.isChecked = emailNotificationsEnabled
        }
    }
    
    private fun setupListeners() {
        // Сохранение настроек уведомлений
        switchPushNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setBoolean("push_notifications_enabled", isChecked)
        }
        
        switchEmailNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setBoolean("email_notifications_enabled", isChecked)
        }
        
        // Сохранение личных данных при потере фокуса
        inputName?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                savePersonalData()
            }
        }
        
        inputPhone?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                savePersonalData()
            }
        }
        
        inputEmail?.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                savePersonalData()
            }
        }
        
        // Навигация к настройкам автоприема
        btnAutoAcceptSettings?.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_autoAcceptSettingsFragment)
        }
        
        // Навигация к MLM
        btnMLM?.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_mlm)
        }
        
        // Выход из аккаунта
        btnLogout?.setOnClickListener {
            logout()
        }
        
        // Политика конфиденциальности
        btnPrivacyPolicy?.setOnClickListener {
            Toast.makeText(context, "Политика конфиденциальности (в разработке)", Toast.LENGTH_SHORT).show()
        }
        
        // Условия использования
        btnTerms?.setOnClickListener {
            Toast.makeText(context, "Условия использования (в разработке)", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun savePersonalData() {
        lifecycleScope.launch {
            val name = inputName?.text?.toString() ?: ""
            val phone = inputPhone?.text?.toString() ?: ""
            val email = inputEmail?.text?.toString() ?: ""
            
            // Обновляем профиль через API
            val result = apiRepository.updateMasterProfile(
                name = if (name.isNotBlank()) name else null,
                phone = if (phone.isNotBlank()) phone else null,
                email = if (email.isNotBlank()) email else null
            )
            
            result.onSuccess {
                Toast.makeText(context, "Данные сохранены", Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Toast.makeText(context, "Ошибка сохранения: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            val authManager = AuthManager(requireContext())
            authManager.clearUserId()
            prefsManager.setAuthToken(null)
            Toast.makeText(context, "Выход выполнен", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_settings_to_login)
        }
    }
}
