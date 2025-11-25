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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val emailET = view.findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordET = view.findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginBtn = view.findViewById<MaterialButton>(R.id.login_button)
        val goToRegister = view.findViewById<View>(R.id.go_to_register)
        val progressBar = view.findViewById<View>(R.id.progress_bar)
        
        loginBtn.setOnClickListener {
            val email = emailET.text.toString()
            val password = passwordET.text.toString()
            viewModel.login(email, password)
        }
        
        goToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_registration)
        }
        
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Loading -> {
                        progressBar.visibility = View.VISIBLE
                        loginBtn.isEnabled = false
                    }
                    is LoginUiState.Success -> {
                        progressBar.visibility = View.GONE
                        findNavController().navigate(R.id.action_login_to_home)
                    }
                    is LoginUiState.Error -> {
                        progressBar.visibility = View.GONE
                        loginBtn.isEnabled = true
                        Snackbar.make(view, state.message, Snackbar.LENGTH_LONG).show()
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                        loginBtn.isEnabled = true
                    }
                }
            }
        }
    }
}







