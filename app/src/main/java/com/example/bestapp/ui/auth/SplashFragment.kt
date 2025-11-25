package com.example.bestapp.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.bestapp.R
import com.example.bestapp.auth.AuthManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAuth()
    }

    private fun checkAuth() {
        lifecycleScope.launch {
            delay(1000)
            val authManager = AuthManager(requireContext())
            val userId = authManager.userId.first()
            
            if (userId != null) {
                findNavController().navigate(R.id.action_splash_to_home)
            } else {
                findNavController().navigate(R.id.action_splash_to_login)
            }
        }
    }
}







