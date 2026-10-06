package com.example

import com.example.flavor.tl
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
    Screen.CommandBridge, Screen.Practice, Screen.Profile -> this
    Screen.HangarAndGoals, Screen.CadetLogbook -> Screen.Profile
    else -> Screen.Practice
}

@Composable
fun MainAppContent(viewModel: BlasterViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val helperLanguage by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val isArabic = helperLanguage == com.example.data.model.HelperLanguage.ARABIC

    val onboarded by viewModel.onboarded.collectAsStateWithLifecycle()
    val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity

    BackHandler {
        if (!viewModel.navigateBack()) activity?.finish()
    }

    if (!onboarded) {
        OnboardingScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isArabic) tl("مستكشف الإسبانية") else tl("Spanish Blaster"),
                        color = SolarAmber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    if (currentScreen !in Screen.TABS) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = if (isArabic) "رجوع" else "Back", tint = TextPrimary)
                        }
                    }
                },
                actions = { StreakChip(viewModel = viewModel) },
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
                Screen.Practice -> PracticeScreen(viewModel = viewModel)
                Screen.GrammarLab -> GrammarLabScreen(viewModel = viewModel)
                Screen.Profile -> ProfileScreen(viewModel = viewModel)
            }
            StreakCelebration(viewModel = viewModel, language = helperLanguage)
        }
    }
}
