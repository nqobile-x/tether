package com.example.tether.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tether.R

sealed class Screen(val route: String, @StringRes val resourceId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Pairing : Screen("pairing", R.string.nav_pairing, Icons.Default.Build)
    object Contacts : Screen("contacts", R.string.nav_contacts, Icons.Default.Person)
    object History : Screen("history", R.string.nav_history, Icons.Default.List)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Pairing,
    Screen.Contacts,
    Screen.History,
    Screen.Settings
)