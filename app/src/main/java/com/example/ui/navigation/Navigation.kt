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
    data object Practice : Screen("practice")
    data object GrammarLab : Screen("grammar")
    data object Profile : Screen("profile")

    companion object {
        /** Bottom-bar destinations; opening one clears the back stack. */
        val TABS: Set<Screen> by lazy { setOf(CommandBridge, Practice, Profile) }
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val labelArabic: String,
    val icon: ImageVector
)

val BottomNavItems = listOf(
    BottomNavItem(Screen.CommandBridge, "Home", "الرئيسية", Icons.Default.Home),
    BottomNavItem(Screen.Practice, "Practice", "تمارين", Icons.Default.SportsEsports),
    BottomNavItem(Screen.Profile, "Me", "أنا", Icons.Default.Person)
)
