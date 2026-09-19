package com.thermalprinter.app.ui.navigation

sealed class NavRoute(val route: String) {
    data object Home : NavRoute("home")
    data object Preview : NavRoute("preview")
    data object History : NavRoute("history")
    data object Settings : NavRoute("settings")
}
