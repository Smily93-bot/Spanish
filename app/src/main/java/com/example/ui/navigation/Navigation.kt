package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Type-safe destinations. Navigation state lives in BlasterViewModel.currentScreen. */
sealed class Screen(val route: String) {
    data object CommandBridge : Screen("bridge")
    data object AdventureMap : Screen("map")
    data object TabletCodex : Screen("codex")
    data object MeteorBlaster : Screen("blaster")
    data object GrammarReactor : Screen("reactor")
    data object QuantumCloze : Screen("cloze")
    data object HangarAndGoals : Screen("hangar")
    data object CadetLogbook : Screen("logbook")
    data object WordGalaxy : Screen("galaxy")
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val labelArabic: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.CommandBridge, "Bridge", "القيادة", Icons.Default.RocketLaunch),
    BottomNavItem(Screen.AdventureMap, "Map", "الخريطة", Icons.Default.Public),
    BottomNavItem(Screen.WordGalaxy, "Words", "الكلمات", Icons.Default.AutoAwesome),
    BottomNavItem(Screen.MeteorBlaster, "Arcade", "الألعاب", Icons.Default.SportsEsports),
    BottomNavItem(Screen.HangarAndGoals, "Hangar", "الحظيرة", Icons.Default.EmojiEvents),
    BottomNavItem(Screen.CadetLogbook, "Logbook", "السجل", Icons.Default.MenuBook)
)
