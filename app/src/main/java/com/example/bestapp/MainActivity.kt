package com.example.bestapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.bestapp.ui.auth.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // MapKit уже инициализирован в BestApp
        
        setContentView(R.layout.activity_main)
        setupNavigation()
        setupAuth()
    }
    
    private fun setupAuth() {
        // Быстрая авторизация для тестирования (мастер Смирнов)
        lifecycleScope.launch {
            authViewModel.isLoggedIn.collect { isLoggedIn ->
                if (!isLoggedIn) {
                    Log.d("MainActivity", "Auto-login as master...")
                    authViewModel.quickLoginAsMaster(0)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment, R.id.loginFragment, R.id.registrationFragment -> {
                    bottomNav.visibility = View.GONE
                }
                else -> {
                    bottomNav.visibility = View.VISIBLE
                }
            }
        }
    }
}