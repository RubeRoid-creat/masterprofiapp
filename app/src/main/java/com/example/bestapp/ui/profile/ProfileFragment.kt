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
import com.example.bestapp.auth.SimpleUserRepository
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
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btn_edit_profile)
        val btnVerification = view.findViewById<MaterialButton>(R.id.btn_verification)
        val btnWallet = view.findViewById<MaterialButton>(R.id.btn_wallet)
        val btnStatistics = view.findViewById<MaterialButton>(R.id.btn_statistics)
        val btnSchedule = view.findViewById<MaterialButton>(R.id.btn_schedule)
        val btnReports = view.findViewById<MaterialButton>(R.id.btn_reports)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btn_logout)
        
        loadMasterInfo(masterName, masterEmail, masterPhone, masterSpec, masterRating, masterReviewsCount, masterStatus, statusIndicator, masterCompletedOrders, verificationChip)
        
        btnEditMasterInfo?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        
        btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        
        btnVerification.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_verification)
        }
        
        btnWallet?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_wallet)
        }
        
        btnStatistics?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_statistics)
        }
        
        btnSchedule?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_schedule)
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
        
        val btnRejectedOrders = view.findViewById<MaterialButton>(R.id.btn_rejected_orders)
        btnRejectedOrders?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_rejected_orders)
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
            val authManager = AuthManager(requireContext())
            val userId = authManager.userId.first()
            
            if (userId != null) {
                val repo = SimpleUserRepository(requireContext())
                val user = repo.getUserByEmail("master@test.ru") // Demo: загрузить по userId
                
                user?.let {
                    name.text = it.fullName
                    email.text = it.email
                    phone.text = "+7 ${it.phone}"
                    spec.text = it.specialization
                }
            }
            
            // Demo данные для новых полей
            rating?.text = "4.8"
            reviewsCount?.text = "(135 отзывов)"
            status?.text = "На смене"
            statusIndicator?.setBackgroundResource(R.drawable.circle_green)
            completedOrders?.text = "247"
            
            // Demo: статус верификации
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

