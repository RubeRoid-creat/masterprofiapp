package com.bestapp.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bestapp.client.ui.auth.LoginScreen
import com.bestapp.client.ui.auth.RegisterScreen
import com.bestapp.client.ui.auth.WelcomeScreen
import com.bestapp.client.ui.home.HomeScreen
import com.bestapp.client.ui.orders.CreateOrderScreen
import com.bestapp.client.ui.orders.OrderDetailsScreen
import com.bestapp.client.ui.orders.OrdersScreen
import com.bestapp.client.ui.masters.MasterProfileScreen
import com.bestapp.client.ui.chat.ChatScreen
import com.bestapp.client.ui.chat.ChatViewModel
import com.bestapp.client.ui.reports.ReportSignScreen
import com.bestapp.client.ui.loyalty.LoyaltyScreen
import com.bestapp.client.ui.profile.ProfileScreen
import com.bestapp.client.ui.notifications.NotificationsScreen
import com.bestapp.client.ui.services.ServicesScreen
import com.bestapp.client.ui.promotions.PromotionsScreen
import com.bestapp.client.ui.settings.SettingsScreen
import com.bestapp.client.ui.help.HelpScreen
import com.bestapp.client.ui.payments.PaymentsScreen

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Orders : Screen("orders")
    object CreateOrder : Screen("create_order")
    object OrderDetails : Screen("order_details/{orderId}") {
        fun createRoute(orderId: Long) = "order_details/$orderId"
    }
    object MasterProfile : Screen("master_profile/{masterId}") {
        fun createRoute(masterId: Long) = "master_profile/$masterId"
    }
    object Chat : Screen("chat/{orderId}") {
        fun createRoute(orderId: Long) = "chat/$orderId"
    }
    object ReportSign : Screen("report_sign/{reportId}/{orderId}") {
        fun createRoute(reportId: Long, orderId: Long) = "report_sign/$reportId/$orderId"
    }
    object Loyalty : Screen("loyalty")
    object Profile : Screen("profile")
    object Notifications : Screen("notifications")
    object Services : Screen("services")
    object Promotions : Screen("promotions")
    object Settings : Screen("settings")
    object Help : Screen("help")
    object Payments : Screen("payments")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController)
        }
        
        composable(Screen.Login.route) {
            LoginScreen(navController)
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(navController)
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        composable(Screen.Orders.route) {
            OrdersScreen(navController)
        }
        
        composable(Screen.CreateOrder.route) {
            CreateOrderScreen(navController)
        }
        
        composable(
            route = Screen.OrderDetails.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            OrderDetailsScreen(navController, orderId)
        }
        
        composable(
            route = Screen.MasterProfile.route,
            arguments = listOf(navArgument("masterId") { type = NavType.LongType })
        ) { backStackEntry ->
            val masterId = backStackEntry.arguments?.getLong("masterId") ?: 0L
            MasterProfileScreen(navController, masterId)
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            ChatScreen(
                navController = navController,
                orderId = orderId,
                viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = ChatViewModel.provideFactory(orderId)
                )
            )
        }
        
        composable(
            route = Screen.ReportSign.route,
            arguments = listOf(
                navArgument("reportId") { type = NavType.LongType },
                navArgument("orderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            ReportSignScreen(navController, reportId, orderId)
        }
        
        composable(Screen.Loyalty.route) {
            LoyaltyScreen(navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(navController)
        }
        
        composable(Screen.Notifications.route) {
            NotificationsScreen(navController)
        }
        
        composable(Screen.Services.route) {
            ServicesScreen(navController)
        }
        
        composable(Screen.Promotions.route) {
            PromotionsScreen(navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        
        composable(Screen.Help.route) {
            HelpScreen(navController)
        }
        
        composable(Screen.Payments.route) {
            PaymentsScreen(navController)
        }
    }
}



