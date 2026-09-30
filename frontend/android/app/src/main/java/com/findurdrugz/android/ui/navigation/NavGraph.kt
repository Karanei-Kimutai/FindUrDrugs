package com.findurdrugz.android.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")

    object Search : Screen("search")

    object Order : Screen("order")

    object History : Screen("history")

    object Premium : Screen("premium")
}