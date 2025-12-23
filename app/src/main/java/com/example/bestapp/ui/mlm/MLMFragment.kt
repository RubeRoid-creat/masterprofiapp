package com.example.bestapp.ui.mlm

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.api.ApiRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MLMFragment : Fragment() {
    
    private val viewModel: MLMViewModel by viewModels { MLMViewModelFactory(ApiRepository()) }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mlm, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        val rankText = view.findViewById<TextView>(R.id.mlm_rank)
        val teamSizeText = view.findViewById<TextView>(R.id.mlm_team_size)
        val totalCommissionsText = view.findViewById<TextView>(R.id.mlm_total_commissions)
        val commissions30DaysText = view.findViewById<TextView>(R.id.mlm_commissions_30_days)
        val level1Text = view.findViewById<TextView>(R.id.mlm_level_1)
        val level2Text = view.findViewById<TextView>(R.id.mlm_level_2)
        val level3Text = view.findViewById<TextView>(R.id.mlm_level_3)
        val referralCodeText = view.findViewById<TextView>(R.id.mlm_referral_code)
        val btnCopyReferral = view.findViewById<MaterialButton>(R.id.btn_copy_referral)
        val errorText = view.findViewById<TextView>(R.id.mlm_error)
        
        // Загружаем данные
        lifecycleScope.launch {
            viewModel.loadMLMData()
        }
        
        // Наблюдаем за статистикой
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            if (stats != null) {
                rankText.text = getRankName(stats.rank ?: "junior_master")
                teamSizeText.text = stats.downline?.total?.toString() ?: "0"
                totalCommissionsText.text = String.format("%.2f ₽", stats.commissions?.total?.amount ?: 0.0)
                commissions30DaysText.text = String.format("%.2f ₽", stats.commissions?.last30Days?.amount ?: 0.0)
                level1Text.text = stats.downline?.level1?.toString() ?: "0"
                level2Text.text = stats.downline?.level2?.toString() ?: "0"
                level3Text.text = stats.downline?.level3?.toString() ?: "0"
                errorText.visibility = View.GONE
            } else {
                // Показываем значения по умолчанию
                rankText.text = "Младший мастер"
                teamSizeText.text = "0"
                totalCommissionsText.text = "0.00 ₽"
                commissions30DaysText.text = "0.00 ₽"
                level1Text.text = "0"
                level2Text.text = "0"
                level3Text.text = "0"
            }
        }
        
        // Наблюдаем за реферальным кодом
        viewModel.referralCode.observe(viewLifecycleOwner) { code ->
            referralCodeText.text = code ?: "Не удалось загрузить"
        }
        
        // Наблюдаем за ошибками
        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                errorText.visibility = View.VISIBLE
                errorText.text = error
            } else {
                errorText.visibility = View.GONE
            }
        }
        
        // Копирование реферального кода
        btnCopyReferral.setOnClickListener {
            val code = referralCodeText.text.toString()
            if (code.isNotBlank() && code != "Загрузка..." && code != "Не удалось загрузить") {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Referral Code", code)
                clipboard.setPrimaryClip(clip)
                Snackbar.make(view, "Реферальный код скопирован", Snackbar.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getRankName(rank: String): String {
        return when (rank) {
            "junior_master" -> "Младший мастер"
            "senior_master" -> "Старший мастер"
            "team_leader" -> "Лидер команды"
            "regional_manager" -> "Региональный менеджер"
            else -> rank
        }
    }
}

