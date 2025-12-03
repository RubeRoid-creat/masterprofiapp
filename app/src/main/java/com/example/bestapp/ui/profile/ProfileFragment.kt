package com.example.bestapp.ui.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bestapp.R
import com.example.bestapp.auth.AuthManager
import com.example.bestapp.api.ApiRepository
import android.util.Log
import com.example.bestapp.data.VerificationStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileFragment : Fragment() {
    
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadAvatar(uri)
            }
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Нужно разрешение для выбора изображения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        Log.d("ProfileFragment", "=== onViewCreated START ===")
        
        val masterName = view.findViewById<TextView>(R.id.master_name)
        val masterEmail = view.findViewById<TextView>(R.id.master_email)
        val masterPhone = view.findViewById<TextView>(R.id.master_phone)
        val masterSpec = view.findViewById<TextView>(R.id.master_specialization)
        val masterAvatar = view.findViewById<ImageView>(R.id.master_avatar)
        val masterRating = view.findViewById<TextView>(R.id.master_rating)
        val masterReviewsCount = view.findViewById<TextView>(R.id.master_reviews_count)
        val masterStatus = view.findViewById<TextView>(R.id.master_status)
        val statusIndicator = view.findViewById<View>(R.id.status_indicator)
        val masterCompletedOrders = view.findViewById<TextView>(R.id.master_completed_orders)
        val verificationChip = view.findViewById<Chip>(R.id.verification_status_chip)
        val btnEditMasterInfo = view.findViewById<MaterialButton>(R.id.btn_edit_master_info)
        val btnVerification = view.findViewById<MaterialButton>(R.id.btn_verification)
        val btnWallet = view.findViewById<MaterialButton>(R.id.btn_wallet)
        val btnStatistics = view.findViewById<MaterialButton>(R.id.btn_statistics)
        
        loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip, masterAvatar)

        // Делаем специализацию кликабельной для изменения
        // Находим родительский CardView и делаем его кликабельным
        val specCardView = findParentCardView(masterSpec)
        setupSpecializationEditor(masterSpec, specCardView)
        
        // Делаем аватар кликабельным для загрузки фото
        setupAvatarEditor(masterAvatar)
        
        btnVerification.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_verification)
        }
        
        // Настройка кнопки MLM
        val btnMLM = view.findViewById<MaterialButton>(R.id.btn_mlm)
        if (btnMLM != null) {
            btnMLM.visibility = View.VISIBLE
            btnMLM.setOnClickListener {
                Log.d("ProfileFragment", "MLM button clicked, navigating to MLM fragment")
                try {
                    findNavController().navigate(R.id.action_profile_to_mlm)
                } catch (e: Exception) {
                    Log.e("ProfileFragment", "Error navigating to MLM", e)
                    Toast.makeText(context, "Ошибка перехода к MLM: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            Log.d("ProfileFragment", "MLM button setup completed")
        } else {
            Log.e("ProfileFragment", "ERROR: btn_mlm button not found in layout!")
        }
        
        btnWallet?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_wallet)
        }
        
        btnStatistics?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_statistics)
        }
        
        val btnSubscriptions = view.findViewById<MaterialButton>(R.id.btn_subscriptions)
        btnSubscriptions?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_subscription)
        }
        
        val btnPromotions = view.findViewById<MaterialButton>(R.id.btn_promotions)
        btnPromotions?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_promotion)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Перезагружаем данные профиля при возвращении на экран для обновления статуса верификации
        view?.let {
            val masterName = it.findViewById<TextView>(R.id.master_name)
            val masterEmail = it.findViewById<TextView>(R.id.master_email)
            val masterPhone = it.findViewById<TextView>(R.id.master_phone)
            val masterSpec = it.findViewById<TextView>(R.id.master_specialization)
            val masterRating = it.findViewById<TextView>(R.id.master_rating)
            val masterReviewsCount = it.findViewById<TextView>(R.id.master_reviews_count)
            val masterStatus = it.findViewById<TextView>(R.id.master_status)
            val statusIndicator = it.findViewById<View>(R.id.status_indicator)
            val masterCompletedOrders = it.findViewById<TextView>(R.id.master_completed_orders)
            val verificationChip = it.findViewById<Chip>(R.id.verification_status_chip)
            val masterAvatar = it.findViewById<ImageView>(R.id.master_avatar)
            loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip, masterAvatar)
            
            // Восстанавливаем кликабельность специализации после обновления данных
            val specCardView = findParentCardView(masterSpec)
            setupSpecializationEditor(masterSpec, specCardView)
            
            // Убеждаемся, что кнопка MLM настроена и видима
            val btnMLM = it.findViewById<MaterialButton>(R.id.btn_mlm)
            if (btnMLM != null) {
                btnMLM.visibility = View.VISIBLE
                btnMLM.setOnClickListener {
                    Log.d("ProfileFragment", "MLM button clicked from onResume")
                    try {
                        findNavController().navigate(R.id.action_profile_to_mlm)
                    } catch (e: Exception) {
                        Log.e("ProfileFragment", "Error navigating to MLM", e)
                        Toast.makeText(context, "Ошибка перехода к MLM: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Log.e("ProfileFragment", "btn_mlm not found in layout in onResume")
            }
        }
    }
    
    private fun loadMasterInfo(
        name: TextView, 
        email: TextView, 
        phone: TextView, 
        spec: TextView,
        rating: TextView?,
        reviewsCount: TextView?,
        status: TextView?,
        statusIndicator: View?,
        completedOrders: TextView?,
        chip: Chip,
        avatar: ImageView
    ) {
        lifecycleScope.launch {
            val prefsManager = com.example.bestapp.data.PreferencesManager.getInstance(requireContext())
            val token = prefsManager.getAuthToken()
            
            if (token != null) {
                // Загружаем данные через API
                val apiRepository = ApiRepository()
                
                // Получаем статистику мастера (теперь включает имя, email, телефон)
                val statsResult = apiRepository.getMasterStats()
                statsResult.onSuccess { response ->
                    Log.d("ProfileFragment", "Received response: $response")
                    
                    // response содержит master и stats
                    val masterData = response["master"] as? Map<*, *>
                    val statsData = response["stats"] as? Map<*, *>
                    
                    Log.d("ProfileFragment", "Master data: $masterData")
                    Log.d("ProfileFragment", "Stats data: $statsData")
                    
                    // Заполняем данные пользователя
                    if (masterData != null) {
                        masterData["name"]?.let { 
                            name.text = it.toString()
                            Log.d("ProfileFragment", "Set name: ${it.toString()}")
                        } ?: run {
                            Log.w("ProfileFragment", "Name not found in master data")
                            name.text = "Имя не указано"
                        }
                        
                        masterData["email"]?.let { 
                            email.text = it.toString()
                            Log.d("ProfileFragment", "Set email: ${it.toString()}")
                        } ?: run {
                            Log.w("ProfileFragment", "Email not found in master data")
                            email.text = "Email не указан"
                        }
                        
                        masterData["phone"]?.let { 
                            phone.text = it.toString()
                            Log.d("ProfileFragment", "Set phone: ${it.toString()}")
                        } ?: run {
                            Log.w("ProfileFragment", "Phone not found in master data")
                            phone.text = "Телефон не указан"
                        }
                        
                        masterData["specialization"]?.let { specList ->
                            try {
                                val specArray = when (specList) {
                                    is List<*> -> specList
                                    is String -> {
                                        // Попробуем распарсить JSON строку
                                        try {
                                            com.google.gson.Gson().fromJson(specList, Array<String>::class.java).toList()
                                        } catch (e: Exception) {
                                            Log.e("ProfileFragment", "Error parsing specialization JSON", e)
                                            emptyList()
                                        }
                                    }
                                    else -> {
                                        Log.w("ProfileFragment", "Unknown specialization type: ${specList.javaClass}")
                                        emptyList()
                                    }
                                }
                                spec.text = specArray.joinToString(", ")
                                Log.d("ProfileFragment", "Set specialization: ${spec.text}")
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Error processing specialization", e)
                                spec.text = "Специализация не указана"
                            }
                        } ?: run {
                            Log.w("ProfileFragment", "Specialization not found in master data")
                            spec.text = "Специализация не указана"
                        }
                        
                        masterData["rating"]?.let { 
                            try {
                                val ratingValue = when (it) {
                                    is Number -> it.toDouble()
                                    is String -> it.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                rating?.text = String.format("%.1f", ratingValue)
                                Log.d("ProfileFragment", "Set rating: $ratingValue")
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Error processing rating", e)
                                rating?.text = "0.0"
                            }
                        } ?: run {
                            rating?.text = "0.0"
                        }
                        
                        masterData["completedOrders"]?.let { 
                            completedOrders?.text = it.toString()
                            Log.d("ProfileFragment", "Set completed orders: ${it.toString()}")
                        } ?: run {
                            completedOrders?.text = "0"
                        }
                        
                        masterData["isOnShift"]?.let { isOnShift ->
                            val isShift = when (isOnShift) {
                                is Boolean -> isOnShift
                                is Number -> isOnShift.toInt() != 0
                                is String -> isOnShift == "true" || isOnShift == "1"
                                else -> false
                            }
                            status?.text = if (isShift) "На смене" else "Не на смене"
                            statusIndicator?.setBackgroundResource(
                                if (isShift) R.drawable.circle_green else R.drawable.circle_red
                            )
                            Log.d("ProfileFragment", "Set shift status: $isShift")
                        } ?: run {
                            status?.text = "Не на смене"
                            statusIndicator?.setBackgroundResource(R.drawable.circle_red)
                        }
                        
                        // Статус верификации из API
                        masterData["verificationStatus"]?.let { statusStr ->
                            val verificationStatus = when (statusStr.toString().lowercase()) {
                                "verified" -> VerificationStatus.VERIFIED
                                "pending" -> VerificationStatus.PENDING
                                "rejected" -> VerificationStatus.REJECTED
                                else -> VerificationStatus.NOT_VERIFIED
                            }
                            chip.text = verificationStatus.displayName
                            
                            // Устанавливаем цвет чипа в зависимости от статуса
                            chip.setChipBackgroundColorResource(when (verificationStatus) {
                                VerificationStatus.VERIFIED -> R.color.verification_verified
                                VerificationStatus.PENDING -> R.color.verification_pending
                                VerificationStatus.REJECTED -> R.color.verification_rejected
                                VerificationStatus.NOT_VERIFIED -> R.color.verification_not_verified
                            })
                            Log.d("ProfileFragment", "Set verification status: $verificationStatus")
                        } ?: run {
                            // Если статус не получен - показываем не верифицирован
                            chip.text = VerificationStatus.NOT_VERIFIED.displayName
                            chip.setChipBackgroundColorResource(R.color.verification_not_verified)
                            Log.w("ProfileFragment", "Verification status not found, defaulting to NOT_VERIFIED")
                        }
                        
                        // Загружаем аватар, если есть photo_url
                        // TODO: добавить photo_url в ответ API getMasterStats
                        // Пока используем заглушку - аватар уже установлен в layout
                    } else {
                        Log.e("ProfileFragment", "Master data is null in response")
                        name.text = "Данные не найдены"
                        email.text = ""
                        phone.text = ""
                        spec.text = ""
                        chip.text = VerificationStatus.NOT_VERIFIED.displayName
                    }
                    
                    statsData?.let { stats ->
                        stats["averageRating"]?.let { 
                            try {
                                val ratingValue = when (it) {
                                    is Number -> it.toDouble()
                                    is String -> it.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                rating?.text = String.format("%.1f", ratingValue)
                            } catch (e: Exception) {
                                Log.e("ProfileFragment", "Error processing average rating from stats", e)
                            }
                        }
                        stats["reviewsCount"]?.let { 
                            reviewsCount?.text = "($it отзывов)"
                        }
                    }
                }.onFailure { error ->
                    Log.e("ProfileFragment", "Failed to load master stats", error)
                    error.printStackTrace()
                    name.text = "Ошибка загрузки"
                    email.text = ""
                    phone.text = ""
                    spec.text = ""
                    chip.text = VerificationStatus.NOT_VERIFIED.displayName
                    
                    // Показываем Toast с информацией об ошибке
                    Toast.makeText(
                        context, 
                        "Ошибка загрузки профиля: ${error.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                // Нет токена - пользователь не залогинен
                name.text = "Не авторизован"
                email.text = ""
                phone.text = ""
                spec.text = ""
                chip.text = VerificationStatus.NOT_VERIFIED.displayName
            }
        }
    }
    
    /**
     * Находит родительский MaterialCardView для указанного View
     */
    private fun findParentCardView(view: View?): MaterialCardView? {
        var parent = view?.parent
        while (parent != null) {
            if (parent is MaterialCardView) {
                return parent
            }
            parent = parent.parent
        }
        return null
    }
    
    /**
     * Делает поле специализации кликабельным и открывает диалог выбора специализаций
     * с последующим сохранением через API.
     */
    private fun setupSpecializationEditor(specView: TextView, cardView: MaterialCardView? = null) {
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
            // Текущее значение специализаций из текста
            val current = specView.text?.toString()
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() && it != "Специализация не указана" }
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
                    // Сохраняем выбор через API
                    val newSpecs = selected.toList()
                    lifecycleScope.launch {
                        try {
                            val apiRepository = ApiRepository()
                            val result = apiRepository.updateMasterProfile(
                                specialization = newSpecs
                            )
                            result.onSuccess {
                                // Обновляем текст в профиле
                                if (newSpecs.isNotEmpty()) {
                                    specView.text = newSpecs.joinToString(", ")
                                } else {
                                    specView.text = "Специализация не указана"
                                }
                                // Восстанавливаем кликабельность после обновления текста
                                val specCardView = findParentCardView(specView)
                                setupSpecializationEditor(specView, specCardView)
                                Toast.makeText(
                                    requireContext(),
                                    "Специализация обновлена",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    requireContext(),
                                    "Ошибка сохранения специализации: ${error.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e: Exception) {
                            Log.e("ProfileFragment", "Error updating specialization", e)
                            Toast.makeText(
                                requireContext(),
                                "Ошибка сохранения специализации: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }

        // Убеждаемся, что TextView всегда кликабелен, даже если текст пустой
        specView.isClickable = true
        specView.isFocusable = true
        specView.isEnabled = true
        
        // Отключаем перехват кликов у родительских LinearLayout
        var parent = specView.parent
        while (parent != null && parent !is MaterialCardView) {
            if (parent is ViewGroup) {
                (parent as ViewGroup).isClickable = false
                (parent as ViewGroup).isFocusable = false
            }
            parent = parent.parent
        }
        
        // Если есть CardView, делаем его кликабельным и устанавливаем обработчик на него
        // Это гарантирует, что клик будет работать даже если TextView перекрыт
        if (cardView != null) {
            Log.d("ProfileFragment", "Found CardView for specialization, setting up click handler")
            cardView.isClickable = true
            cardView.isFocusable = true
            cardView.isFocusableInTouchMode = true
            cardView.setOnClickListener(null)
            cardView.setOnClickListener { 
                Log.d("ProfileFragment", "CardView clicked, opening specialization dialog")
                openDialog() 
            }
            // Также устанавливаем обработчик на TextView для надежности
            specView.setOnClickListener(null)
            specView.setOnClickListener { 
                Log.d("ProfileFragment", "TextView clicked, opening specialization dialog")
                openDialog() 
            }
        } else {
            Log.w("ProfileFragment", "CardView not found for specialization, using TextView only")
            // Если CardView не найден, используем только TextView
            specView.setOnClickListener(null)
            specView.setOnClickListener { 
                Log.d("ProfileFragment", "TextView clicked (no CardView), opening specialization dialog")
                openDialog() 
            }
        }
    }

    /**
     * Делает аватар кликабельным для загрузки фото
     */
    private fun setupAvatarEditor(avatarView: ImageView) {
        avatarView.isClickable = true
        avatarView.isFocusable = true
        avatarView.setOnClickListener {
            checkPermissionAndOpenPicker()
        }
    }
    
    /**
     * Проверяет разрешение и открывает выбор изображения
     */
    private fun checkPermissionAndOpenPicker() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    /**
     * Открывает галерею для выбора изображения
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
    
    /**
     * Загружает выбранное изображение на сервер
     */
    private fun uploadAvatar(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Показываем индикатор загрузки
                Toast.makeText(context, "Загрузка аватара...", Toast.LENGTH_SHORT).show()
                
                // Получаем файл из URI
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val file = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                // Создаем MultipartBody.Part
                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, requestFile)
                
                val apiRepository = ApiRepository()
                val result = apiRepository.uploadMasterAvatar(photoPart)
                result.onSuccess { response ->
                    Log.d("ProfileFragment", "Avatar uploaded successfully: ${response.photoUrl}")
                    Toast.makeText(context, "Аватар успешно загружен", Toast.LENGTH_SHORT).show()
                    
                    // Обновляем аватар в UI
                    val avatarView = view?.findViewById<ImageView>(R.id.master_avatar)
                    avatarView?.let { av ->
                        // Загружаем изображение с сервера
                        val baseUrl = com.example.bestapp.api.RetrofitClient.BASE_URL
                        val fullUrl = if (response.photoUrl.startsWith("http")) {
                            response.photoUrl
                        } else {
                            baseUrl.removeSuffix("/") + response.photoUrl
                        }
                        Glide.with(requireContext())
                            .load(fullUrl)
                            .circleCrop()
                            .into(av)
                    }
                }.onFailure { error ->
                    Log.e("ProfileFragment", "Ошибка загрузки аватара", error)
                    Toast.makeText(context, "Ошибка загрузки: ${error.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("ProfileFragment", "Ошибка загрузки аватара", e)
                Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Функция logout больше не используется, так как кнопка выхода убрана из профиля
}
