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
        val btnReports = view.findViewById<MaterialButton>(R.id.btn_reports)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)
        
        loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip)
        
        btnVerification.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_verification)
        }
        
        btnWallet?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_wallet)
        }
        
        btnStatistics?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_statistics)
        }
        
        btnReports?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_reports)
        }
        
        val btnSubscriptions = view.findViewById<MaterialButton>(R.id.btn_subscriptions)
        btnSubscriptions?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_subscription)
        }
        
        val btnPromotions = view.findViewById<MaterialButton>(R.id.btn_promotions)
        btnPromotions?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_promotion)
        }
        
        btnLogout.setOnClickListener {
            logout()
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
                    // response содержит master и stats
                    val masterData = response["master"] as? Map<*, *>
                    val statsData = response["stats"] as? Map<*, *>
                    
                    // Заполняем данные пользователя
                    masterData?.let { master ->
                        master["name"]?.let { name.text = it.toString() }
                        master["email"]?.let { email.text = it.toString() }
                        master["phone"]?.let { phone.text = it.toString() }
                        master["specialization"]?.let { specList ->
                            val specArray = specList as? List<*>
                            spec.text = specArray?.joinToString(", ") ?: ""
                        }
                        master["rating"]?.let { 
                            rating?.text = String.format("%.1f", (it as? Number)?.toDouble() ?: 0.0) 
                        }
                        master["completedOrders"]?.let { 
                            completedOrders?.text = it.toString() 
                        }
                        master["isOnShift"]?.let { isOnShift ->
                            val isShift = (isOnShift as? Boolean) ?: false
                            status?.text = if (isShift) "На смене" else "Не на смене"
                            statusIndicator?.setBackgroundResource(
                                if (isShift) R.drawable.circle_green else R.drawable.circle_red
                            )
                        }
                    }
                    
                    statsData?.let { stats ->
                        stats["averageRating"]?.let { 
                            rating?.text = String.format("%.1f", (it as? Number)?.toDouble() ?: 0.0) 
                        }
                        stats["reviewsCount"]?.let { 
                            reviewsCount?.text = "($it отзывов)" 
                        }
                    }
                }.onFailure {
                    Log.e("ProfileFragment", "Failed to load master stats: ${it.message}")
                    name.text = "Ошибка загрузки"
                    email.text = ""
                    phone.text = ""
                    spec.text = ""
                }
            } else {
                // Нет токена - пользователь не залогинен
                name.text = "Не авторизован"
                email.text = ""
                phone.text = ""
                spec.text = ""
            }
            
            // Статус верификации (пока demo)
            val verificationStatus = VerificationStatus.NOT_VERIFIED
            chip.text = verificationStatus.displayName
        }
    }
    
    private fun logout() {
        lifecycleScope.launch {
            val authManager = AuthManager(requireContext())
            authManager.clearUserId()
            Toast.makeText(context, "Выход выполнен", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }
}

