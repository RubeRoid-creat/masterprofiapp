package com.example.bestapp.ui.wallet

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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class WalletFragment : Fragment() {
    private val viewModel: WalletViewModel by viewModels()
    private lateinit var adapter: TransactionsAdapter
    
    private var toolbar: MaterialToolbar? = null
    private var progressBar: ProgressBar? = null
    private var balanceText: TextView? = null
    private var pendingPayoutsText: TextView? = null
    private var totalEarnedText: TextView? = null
    private var availableForPayoutText: TextView? = null
    private var recyclerTransactions: RecyclerView? = null
    private var btnRequestPayout: MaterialButton? = null
    private var btnTopup: MaterialButton? = null
    private var errorText: TextView? = null
    
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("ru", "RU"))
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_wallet, container, false)
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
        balanceText = view.findViewById(R.id.text_balance)
        pendingPayoutsText = view.findViewById(R.id.text_pending_payouts)
        totalEarnedText = view.findViewById(R.id.text_total_earned)
        availableForPayoutText = view.findViewById(R.id.text_available_for_payout)
        recyclerTransactions = view.findViewById(R.id.recycler_transactions)
        btnRequestPayout = view.findViewById(R.id.btn_request_payout)
        btnTopup = view.findViewById(R.id.btn_topup)
        errorText = view.findViewById(R.id.text_error)
    }
    
    private fun setupToolbar() {
        toolbar?.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TransactionsAdapter()
        recyclerTransactions?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@WalletFragment.adapter
        }
    }
    
    private fun setupButtons() {
        btnRequestPayout?.setOnClickListener {
            showPayoutDialog()
        }
        btnTopup?.setOnClickListener {
            showTopupDialog()
        }
    }
    
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                progressBar?.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                
                state.wallet?.let { wallet ->
                    balanceText?.text = currencyFormat.format(wallet.balance)
                    pendingPayoutsText?.text = currencyFormat.format(wallet.pendingPayouts)
                    totalEarnedText?.text = currencyFormat.format(wallet.totalEarned)
                    availableForPayoutText?.text = currencyFormat.format(wallet.availableForPayout)
                    
                    btnRequestPayout?.isEnabled = wallet.availableForPayout > 0 && !state.isRequestingPayout && !state.isTopupInProgress
                    btnTopup?.isEnabled = !state.isRequestingPayout && !state.isTopupInProgress
                }
                
                adapter.submitList(state.transactions)
                
                state.errorMessage?.let { error ->
                    errorText?.text = error
                    errorText?.visibility = View.VISIBLE
                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                } ?: run {
                    errorText?.visibility = View.GONE
                }
            }
        }
    }
    
    private fun showPayoutDialog() {
        val viewModel = this.viewModel
        val wallet = viewModel.uiState.value.wallet ?: return
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_payout_request, null)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.input_amount)
        amountInput?.setText(wallet.availableForPayout.toString())
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Запросить выплату")
            .setView(dialogView)
            .setPositiveButton("Запросить") { _, _ ->
                val amount = amountInput?.text?.toString()?.toDoubleOrNull()
                if (amount != null && amount > 0 && amount <= wallet.availableForPayout) {
                    viewModel.requestPayout(amount)
                } else {
                    Toast.makeText(context, "Укажите корректную сумму", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun showTopupDialog() {
        val viewModel = this.viewModel
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_topup, null)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.input_amount)
        val paymentMethodInput = dialogView.findViewById<TextInputEditText>(R.id.input_payment_method)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Пополнить кошелек")
            .setView(dialogView)
            .setPositiveButton("Пополнить") { _, _ ->
                val amount = amountInput?.text?.toString()?.toDoubleOrNull()
                if (amount != null && amount >= 100 && amount <= 100000) {
                    val paymentMethod = paymentMethodInput?.text?.toString() ?: "card"
                    viewModel.topupWallet(amount, paymentMethod, "Пополнение кошелька")
                } else {
                    Toast.makeText(context, "Сумма должна быть от 100 до 100 000 ₽", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }
}



