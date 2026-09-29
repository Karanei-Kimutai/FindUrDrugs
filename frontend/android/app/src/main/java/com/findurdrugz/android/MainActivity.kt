package com.findurdrugz.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.findurdrugz.android.data.api.ApiClient
import com.findurdrugz.android.data.local.TokenManager
import com.findurdrugz.android.data.repository.AuthRepository
import com.findurdrugz.android.data.repository.OrderRepository
import com.findurdrugz.android.data.repository.SearchRepository
import com.findurdrugz.android.ui.auth.AuthViewModel
import com.findurdrugz.android.ui.auth.AuthViewModelFactory
import com.findurdrugz.android.ui.auth.LoginScreen
import com.findurdrugz.android.ui.auth.RegisterScreen
import com.findurdrugz.android.ui.history.HistoryScreen
import com.findurdrugz.android.ui.history.HistoryViewModel
import com.findurdrugz.android.ui.history.HistoryViewModelFactory
import com.findurdrugz.android.ui.home.HomeScreen
import com.findurdrugz.android.ui.navigation.Screen
import com.findurdrugz.android.ui.order.OrderScreen
import com.findurdrugz.android.ui.order.OrderViewModel
import com.findurdrugz.android.ui.order.OrderViewModelFactory
import com.findurdrugz.android.ui.order.SelectedMedicineHolder
import com.findurdrugz.android.ui.search.SearchScreen
import com.findurdrugz.android.ui.search.SearchViewModel
import com.findurdrugz.android.ui.search.SearchViewModelFactory
import com.findurdrugz.android.ui.theme.FindUrDrugzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val tokenManager = TokenManager(applicationContext)
        val api = ApiClient.create(tokenManager)

        val authFactory = AuthViewModelFactory(AuthRepository(api, tokenManager))
        val searchFactory = SearchViewModelFactory(SearchRepository(api))
        val orderRepository = OrderRepository(api)
        val orderFactory = OrderViewModelFactory(orderRepository)
        val historyFactory = HistoryViewModelFactory(orderRepository) // reuses the same repository

        setContent {
            FindUrDrugzTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()
                    val authViewModel = viewModel<AuthViewModel>(factory = authFactory)
                    val searchViewModel = viewModel<SearchViewModel>(factory = searchFactory)
                    val orderViewModel = viewModel<OrderViewModel>(factory = orderFactory)
                    val historyViewModel = viewModel<HistoryViewModel>(factory = historyFactory)

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Login.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Login.route) {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToRegister = {
                                    navController.navigate(Screen.Register.route)
                                }
                            )
                        }
                        composable(Screen.Register.route) {
                            RegisterScreen(
                                viewModel = authViewModel,
                                onRegisterSuccess = {
                                    navController.navigate(Screen.Home.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                },
                                onNavigateToLogin = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(
                                onNavigateToSearch = {
                                    navController.navigate(Screen.Search.route)
                                },
                                onNavigateToHistory = {
                                    navController.navigate(Screen.History.route)
                                },
                                onLogout = {
                                    authViewModel.logout {
                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }
                        composable(Screen.Search.route) {
                            SearchScreen(
                                viewModel = searchViewModel,
                                onBack = { navController.popBackStack() },
                                onResultSelected = { result ->
                                    SelectedMedicineHolder.selected = result
                                    navController.navigate(Screen.Order.route)
                                }
                            )
                        }
                        composable(Screen.Order.route) {
                            OrderScreen(
                                viewModel = orderViewModel,
                                onBack = { navController.popBackStack() },
                                onOrderComplete = {
                                    navController.popBackStack(Screen.Home.route, inclusive = false)
                                }
                            )
                        }
                        composable(Screen.History.route) {
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}