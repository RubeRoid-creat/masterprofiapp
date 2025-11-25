package com.example.bestapp.ui.promotion

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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bestapp.R
import com.example.bestapp.api.models.ApiPromotion
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PromotionFragment : Fragment() {
    private val viewModel: PromotionViewModel by viewModels()
    
    private var toolbar: com.google.android.material.appbar.MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null
    private var recyclerActivePromotions: RecyclerView? = null
    private var textNoActivePromotions: TextView? = null
    private lateinit var activePromotionsAdapter: ActivePromotionsAdapter
    
    private var cardTopListing: MaterialCardView? = null
    private var textTopListingDescription: TextView? = null
    private var textTopListingPrice: TextView? = null
    private var textTopListingDuration: TextView? = null
    private var btnPurchaseTopListing: MaterialButton? = null
    
    private var cardHighlighted: MaterialCardView? = null
    private var textHighlightedDescription: TextView? = null
    private var textHighlightedPrice: TextView? = null
    private var textHighlightedDuration: TextView? = null
    private var btnPurchaseHighlighted: MaterialButton? = null
    
    private var cardFeatured: MaterialCardView? = null
    private var textFeaturedDescription: TextView? = null
    private var textFeaturedPrice: TextView? = null
    private var textFeaturedDuration: TextView? = null
    private var btnPurchaseFeatured: MaterialButton? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_promotion, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        observeUiState()
    }
    
    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        progressBar = view.findViewById(R.id.progress_loading)
        errorText = view.findViewById(R.id.text_error)
        recyclerActivePromotions = view.findViewById(R.id.recycler_active_promotions)
        textNoActivePromotions = view.findViewById(R.id.text_no_active_promotions)
        
        cardTopListing = view.findViewById(R.id.card_top_listing)
        textTopListingDescription = view.findViewById(R.id.text_top_listing_description)
        textTopListingPrice = view.findViewById(R.id.text_top_listing_price)
        textTopListingDuration = view.findViewById(R.id.text_top_listing_duration)
        btnPurchaseTopListing = view.findViewById(R.id.btn_purchase_top_listing)
        
        cardHighlighted = view.findViewById(R.id.card_highlighted)
        textHighlightedDescription = view.findViewById(R.id.text_highlighted_description)
        textHighlightedPrice = view.findViewById(R.id.text_highlighted_price)
        textHighlightedDuration = view.findViewById(R.id.text_highlighted_duration)
        btnPurchaseHighlighted = view.findViewById(R.id.btn_purchase_highlighted)
        
        cardFeatured = view.findViewById(R.id.card_featured)
        textFeaturedDescription = view.findViewById(R.id.text_featured_description)
        textFeaturedPrice = view.findViewById(R.id.text_featured_price)
        textFeaturedDuration = view.findViewById(R.id.text_featured_duration)
        btnPurchaseFeatured = view.findViewById(R.id.btn_purchase_featured)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupRecyclerView() {
        activePromotionsAdapter = ActivePromotionsAdapter { promotion ->
            viewModel.cancelPromotion(promotion.id)
        }
        recyclerActivePromotions?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = activePromotionsAdapter
        }
    }
    
    private fun setupButtons() {
        btnPurchaseTopListing?.setOnClickListener {
            viewModel.purchasePromotion("top_listing")
        }
        
        btnPurchaseHighlighted?.setOnClickListener {
            viewModel.purchasePromotion("highlighted_profile")
        }
        
        btnPurchaseFeatured?.setOnClickListener {
            viewModel.purchasePromotion("featured")
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progressBar?.visibility = if (state.isLoading || state.isPurchasing || state.isCanceling) View.VISIBLE else View.GONE
                
                state.errorMessage?.let { error ->
                    errorText?.text = error
                    errorText?.visibility = View.VISIBLE
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } ?: run {
                    errorText?.visibility = View.GONE
                }
                
                // Активные продвижения
                state.promotionInfo?.let { info ->
                    val activePromotions = info.activePromotions ?: emptyList()
                    if (activePromotions.isNotEmpty()) {
                        activePromotionsAdapter.submitList(activePromotions)
                        recyclerActivePromotions?.visibility = View.VISIBLE
                        textNoActivePromotions?.visibility = View.GONE
                    } else {
                        recyclerActivePromotions?.visibility = View.GONE
                        textNoActivePromotions?.visibility = View.VISIBLE
                    }
                    
                    // Доступные продвижения
                    state.promotionTypes["top_listing"]?.let { type ->
                        textTopListingDescription?.text = type.description
                        textTopListingPrice?.text = currencyFormat.format(type.price)
                        textTopListingDuration?.text = "Длительность: ${type.duration} дней"
                        btnPurchaseTopListing?.visibility = if (info.hasTopListing != true) View.VISIBLE else View.GONE
                    }
                    
                    state.promotionTypes["highlighted_profile"]?.let { type ->
                        textHighlightedDescription?.text = type.description
                        textHighlightedPrice?.text = currencyFormat.format(type.price)
                        textHighlightedDuration?.text = "Длительность: ${type.duration} дней"
                        btnPurchaseHighlighted?.visibility = if (info.hasHighlighted != true) View.VISIBLE else View.GONE
                    }
                    
                    state.promotionTypes["featured"]?.let { type ->
                        textFeaturedDescription?.text = type.description
                        textFeaturedPrice?.text = currencyFormat.format(type.price)
                        textFeaturedDuration?.text = "Длительность: ${type.duration} дней"
                        btnPurchaseFeatured?.visibility = if (info.hasFeatured != true) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
    
    private class ActivePromotionsAdapter(
        private val onCancelClick: (ApiPromotion) -> Unit
    ) : RecyclerView.Adapter<ActivePromotionsAdapter.ViewHolder>() {
        
        private var promotions: List<ApiPromotion> = emptyList()
        
        fun submitList(newList: List<ApiPromotion>) {
            promotions = newList
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_promotion, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(promotions[position])
        }
        
        override fun getItemCount() = promotions.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val nameText: TextView = itemView.findViewById(R.id.text_promotion_name)
            private val expiresText: TextView = itemView.findViewById(R.id.text_promotion_expires)
            private val cancelButton: MaterialButton = itemView.findViewById(R.id.btn_cancel_promotion)
            
            private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            
            fun bind(promotion: ApiPromotion) {
                val promotionName = when (promotion.promotionType) {
                    "top_listing" -> "Топ в выдаче"
                    "highlighted_profile" -> "Выделенный профиль"
                    "featured" -> "Рекомендуемый мастер"
                    else -> promotion.promotionType
                }
                nameText.text = promotionName
                
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        .parse(promotion.expiresAt)
                    expiresText.text = "Действует до: ${date?.let { dateFormat.format(it) } ?: promotion.expiresAt}"
                } catch (e: Exception) {
                    expiresText.text = "Действует до: ${promotion.expiresAt}"
                }
                
                cancelButton.setOnClickListener {
                    onCancelClick(promotion)
                }
            }
        }
    }
}

