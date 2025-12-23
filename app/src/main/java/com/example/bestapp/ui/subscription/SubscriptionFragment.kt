package com.example.bestapp.ui.subscription

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class SubscriptionFragment : Fragment() {
    private val viewModel: SubscriptionViewModel by viewModels()
    
    private var toolbar: com.google.android.material.appbar.MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var currentTypeText: TextView? = null
    private var commissionText: TextView? = null
    private var expiresAtText: TextView? = null
    private var btnCancelSubscription: MaterialButton? = null
    
    private var cardBasic: MaterialCardView? = null
    private var textBasicPrice: TextView? = null
    private var textBasicCommission: TextView? = null
    private var textBasicFeatures: TextView? = null
    private var btnActivateBasic: MaterialButton? = null
    
    private var cardPremium: MaterialCardView? = null
    private var textPremiumPrice: TextView? = null
    private var textPremiumCommission: TextView? = null
    private var textPremiumFeatures: TextView? = null
    private var btnActivatePremium: MaterialButton? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_subscription, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupButtons()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        currentTypeText = view.findViewById(R.id.text_current_type)
        commissionText = view.findViewById(R.id.text_commission)
        expiresAtText = view.findViewById(R.id.text_expires_at)
        btnCancelSubscription = view.findViewById(R.id.btn_cancel_subscription)
        
        cardBasic = view.findViewById(R.id.card_basic)
        textBasicPrice = view.findViewById(R.id.text_basic_price)
        textBasicCommission = view.findViewById(R.id.text_basic_commission)
        textBasicFeatures = view.findViewById(R.id.text_basic_features)
        btnActivateBasic = view.findViewById(R.id.btn_activate_basic)
        
        cardPremium = view.findViewById(R.id.card_premium)
        textPremiumPrice = view.findViewById(R.id.text_premium_price)
        textPremiumCommission = view.findViewById(R.id.text_premium_commission)
        textPremiumFeatures = view.findViewById(R.id.text_premium_features)
        btnActivatePremium = view.findViewById(R.id.btn_activate_premium)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupButtons() {
        btnCancelSubscription?.setOnClickListener {
            viewModel.cancelSubscription()
        }
        
        btnActivateBasic?.setOnClickListener {
            viewModel.activateSubscription("basic")
        }
        
        btnActivatePremium?.setOnClickListener {
            viewModel.activateSubscription("premium")
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progressBar?.visibility = if (state.isLoading || state.isActivating || state.isCanceling) View.VISIBLE else View.GONE
                
                state.errorMessage?.let { error ->
                    errorText?.text = error
                    errorText?.visibility = View.VISIBLE
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } ?: run {
                    errorText?.visibility = View.GONE
                }
                
                state.subscriptionInfo?.let { info ->
                    // Текущая подписка
                    val currentTypeName = when (info.currentType) {
                        "basic" -> "Базовый"
                        "premium" -> "Премиум"
                        else -> info.currentType
                    }
                    currentTypeText?.text = currentTypeName
                    info.subscriptionData?.let { subscriptionData ->
                        commissionText?.text = "Комиссия: ${subscriptionData.commission}%"
                    } ?: run {
                        commissionText?.text = "Комиссия: -"
                    }
                    
                    info.currentSubscription?.let { subscription ->
                        subscription.expiresAt?.let { expiresAt ->
                            try {
                                val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                    .parse(expiresAt)
                                expiresAtText?.text = "Действует до: ${date?.let { dateFormat.format(it) } ?: expiresAt}"
                                expiresAtText?.visibility = View.VISIBLE
                            } catch (e: Exception) {
                                expiresAtText?.visibility = View.GONE
                            }
                        } ?: run {
                            expiresAtText?.visibility = View.GONE
                        }
                        
                        if (info.currentType == "premium") {
                            btnCancelSubscription?.visibility = View.VISIBLE
                        } else {
                            btnCancelSubscription?.visibility = View.GONE
                        }
                    } ?: run {
                        expiresAtText?.visibility = View.GONE
                        btnCancelSubscription?.visibility = View.GONE
                    }
                    
                    // Базовый тариф
                    info.allTypes?.get("basic")?.let { basicType ->
                        textBasicPrice?.text = if (basicType.price == 0.0) "Бесплатно" else currencyFormat.format(basicType.price)
                        textBasicCommission?.text = "Комиссия: ${basicType.commission}%"
                        textBasicFeatures?.text = basicType.features?.joinToString("\n") { "• $it" } ?: ""
                        btnActivateBasic?.visibility = if (info.currentType != "basic") View.VISIBLE else View.GONE
                    } ?: run {
                        textBasicPrice?.text = "Бесплатно"
                        textBasicCommission?.text = "Комиссия: -"
                        textBasicFeatures?.text = ""
                        btnActivateBasic?.visibility = View.GONE
                    }
                    
                    // Премиум тариф
                    info.allTypes?.get("premium")?.let { premiumType ->
                        textPremiumPrice?.text = "${currencyFormat.format(premiumType.price)}/месяц"
                        textPremiumCommission?.text = "Комиссия: ${premiumType.commission}%"
                        textPremiumFeatures?.text = premiumType.features?.joinToString("\n") { "• $it" } ?: ""
                        btnActivatePremium?.visibility = if (info.currentType != "premium") View.VISIBLE else View.GONE
                    } ?: run {
                        textPremiumPrice?.text = "-/месяц"
                        textPremiumCommission?.text = "Комиссия: -"
                        textPremiumFeatures?.text = ""
                        btnActivatePremium?.visibility = View.GONE
                    }
                    
                    // Выделяем текущую подписку
                    if (info.currentType == "basic") {
                        cardBasic?.strokeWidth = 2
                        cardPremium?.strokeWidth = 0
                    } else if (info.currentType == "premium") {
                        cardBasic?.strokeWidth = 0
                        cardPremium?.strokeWidth = 2
                    } else {
                        cardBasic?.strokeWidth = 0
                        cardPremium?.strokeWidth = 0
                    }
                }
            }
        }
    }
}

