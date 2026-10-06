package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.AppDatabase
import com.example.data.repository.BlasterRepository
import com.example.ui.navigation.BottomNavItems
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BlasterViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(applicationContext)
                val repository = BlasterRepository(db.dao)
                @Suppress("UNCHECKED_CAST")
                return BlasterViewModel(application, repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOpenIntent(intent)
        setContent {
            SpanishBlasterTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    /** The widget and reminder open the Word Galaxy directly. */
    private fun handleOpenIntent(intent: Intent?) {
        if (intent?.getStringExtra(EXTRA_OPEN) == OPEN_GALAXY) {
            intent.removeExtra(EXTRA_OPEN)
            viewModel.navigateTo(Screen.WordGalaxy)
        }
    }

    override fun onPause() {
        // Stop the meteor timer when the app goes to the background so the shield isn't drained.
        viewModel.pauseMeteorGame()
        super.onPause()
    }

    companion object {
        const val EXTRA_OPEN = "open"
        const val OPEN_GALAXY = "galaxy"
    }
}

/** Which bottom-bar tab is highlighted for screens that aren't in the bar themselves. */
private fun Screen.tabOwner(): Screen = when (this) {
    Screen.TabletCodex -> Screen.AdventureMap
    Screen.GrammarReactor, Screen.QuantumCloze -> Screen.MeteorBlaster
    else -> this
}

@Composable
fun MainAppContent(viewModel: BlasterViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedTablet by viewModel.selectedTabletId.collectAsStateWithLifecycle()
    val userProgress by viewModel.userProgress.collectAsStateWithLifecycle()
    val helperLanguage by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val isArabic = helperLanguage == com.example.data.model.HelperLanguage.ARABIC

    BackHandler(enabled = currentScreen != Screen.CommandBridge || selectedTablet != null) {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isArabic) "مستكشف الإسبانية" else "SPANISH BLASTER",
                            color = SolarAmber,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = AdventureSurfaceVariant
                        ) {
                            Text(
                                text = "NIVEL ${userProgress?.level ?: 1}",
                                color = ExplorerBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    StreakChip(viewModel = viewModel)
                    // Helper language switcher
                    Surface(
                        onClick = { viewModel.toggleHelperLanguage() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = AdventureSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdventureCardBorder),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = "Language",
                                tint = ExplorerBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "عربي" else "English",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AdventureSurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AdventureSurface,
                tonalElevation = 2.dp
            ) {
                BottomNavItems.forEach { item ->
                    val isSelected = currentScreen.tabOwner() == item.screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(item.screen) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) ExplorerBlue else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = if (isArabic) item.labelArabic else item.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) ExplorerBlue else TextSecondary
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .fillMaxSize()
                .background(AdventureBg)
        ) {
            when (currentScreen) {
                Screen.CommandBridge -> CommandBridgeScreen(viewModel = viewModel)
                Screen.AdventureMap -> AdventureMapScreen(viewModel = viewModel)
                Screen.TabletCodex -> TabletCodexScreen(viewModel = viewModel)
                Screen.MeteorBlaster -> MeteorBlasterScreen(viewModel = viewModel)
                Screen.GrammarReactor -> GrammarReactorScreen(viewModel = viewModel)
                Screen.QuantumCloze -> QuantumClozeScreen(viewModel = viewModel)
                Screen.HangarAndGoals -> HangarAndGoalsScreen(viewModel = viewModel)
                Screen.CadetLogbook -> CadetLogbookScreen(viewModel = viewModel)
                Screen.WordGalaxy -> WordGalaxyScreen(viewModel = viewModel)
            }
            StreakCelebration(viewModel = viewModel, language = helperLanguage)
        }
    }
}
