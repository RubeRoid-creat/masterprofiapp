package com.bestapp.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bestapp.client.di.AppContainer
import com.bestapp.client.ui.navigation.BottomNavigationBar
import com.bestapp.client.ui.navigation.NavGraph
import com.bestapp.client.ui.navigation.Screen
import com.bestapp.client.ui.theme.BestAppClientTheme
import com.bestapp.client.ui.update.UpdateDialog
import com.bestapp.client.ui.update.UpdateViewModel
import com.yandex.mapkit.MapKitFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }
    
    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Инициализируем AppContainer
        AppContainer.init(this)
        
        setContent {
            BestAppClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    
                    // ViewModel для проверки обновлений
                    val updateViewModel: UpdateViewModel = viewModel()
                    val updateUiState by updateViewModel.uiState.collectAsState()

                    // Check if user is logged in and check for updates
                    LaunchedEffect(Unit) {
                        try {
                            // Проверяем обновления при запуске
                            updateViewModel.checkForUpdate()
                            
                            val isLoggedIn = AppContainer.apiRepository.isLoggedIn()
                            startDestination = if (isLoggedIn) {
                                Screen.Home.route
                            } else {
                                Screen.Welcome.route
                            }
                        } catch (e: Exception) {
                            // If error, start at Welcome
                            startDestination = Screen.Welcome.route
                        }
                    }
                    
                    // Диалог обновления
                    if (updateUiState.showUpdateDialog && updateUiState.versionInfo != null) {
                        UpdateDialog(
                            versionInfo = updateUiState.versionInfo!!,
                            isForceUpdate = updateUiState.versionInfo!!.forceUpdate,
                            onUpdateClick = { updateViewModel.startUpdate() },
                            onDismiss = { updateViewModel.dismissDialog() },
                            downloadProgress = if (updateUiState.isDownloading) updateUiState.downloadProgress else null,
                            isDownloading = updateUiState.isDownloading
                        )
                    }

                    // Show navigation only after startDestination is determined
                    startDestination?.let { destination ->
                        // Показываем bottom navigation только на основных экранах
                        val showBottomNav = currentRoute in listOf(
                            Screen.Home.route,
                            Screen.Orders.route,
                            Screen.Profile.route
                        )
                        
                        Scaffold(
                            bottomBar = {
                                if (showBottomNav) {
                                    BottomNavigationBar(navController = navController)
                                }
                            }
                        ) {
                            NavGraph(
                                navController = navController,
                                startDestination = destination,
                                modifier = Modifier.padding(it)
                            )
                        }
                    }
                }
            }
        }
    }
}

