package com.example.bestapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.auth.AuthManager
import com.example.bestapp.api.ApiRepository
import android.util.Log
import com.example.bestapp.data.VerificationStatus
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

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
        
        loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip)
        
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
            loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip)
            
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
        chip: Chip
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
    
    // Функция logout больше не используется, так как кнопка выхода убрана из профиля
}
