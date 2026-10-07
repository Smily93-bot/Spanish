package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HelperLanguage
import com.example.data.model.Rank
import com.example.data.model.pick
import com.example.ui.components.AdventureCard
import com.example.ui.components.HudStatCard
import com.example.ui.components.ProgressBar
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

/** Me: progress, streak, trophies and every setting in one place. */
@Composable
fun ProfileScreen(viewModel: BlasterViewModel) {
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val cards by viewModel.wordCards.collectAsStateWithLifecycle()
    val tablets by viewModel.tabletProgress.collectAsStateWithLifecycle()
    val level = progress?.level ?: 1
    val rank = Rank.forLevel(level)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Rank and level
        AdventureCard(borderColor = NebulaPurple.copy(alpha = 0.4f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(rank.emoji, fontSize = 40.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(rank.label(language), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(language.pick("المستوى $level", "Level $level"), color = TextSecondary, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(progress = (progress?.currentXp ?: 0).toFloat() / (progress?.xpToNextLevel ?: 300), color = NebulaPurple)
            Text("${progress?.currentXp ?: 0} / ${progress?.xpToNextLevel ?: 300} XP", color = TextSecondary, fontSize = 12.sp)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudStatCard(language.pick("كلمات", "Words"), "${cards.values.count { it.introduced }}", "🧠", Modifier.weight(1f), SuccessGreen)
            HudStatCard(language.pick("فصول", "Chapters"), "${tablets.count { it.isCompleted }}", "📜", Modifier.weight(1f), SolarAmber)
            HudStatCard(language.pick("نجوم", "Stars"), "${progress?.starCredits ?: 0}", "⭐", Modifier.weight(1f), ExplorerBlue)
        }

        StreakCard(viewModel, language)

        LinkRow("📖", language.pick("كلماتي والقواعد", "My words & grammar")) { viewModel.navigateTo(Screen.CadetLogbook) }
        LinkRow("🏆", language.pick("الجوائز والسفينة", "Trophies & ship")) { viewModel.navigateTo(Screen.HangarAndGoals) }

        Text(language.pick("الإعدادات", "Settings"), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        AdventureCard {
            Text(language.pick("لغة المساعدة", "Helper language"), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LanguageButton("العربية", language == HelperLanguage.ARABIC, Modifier.weight(1f)) { viewModel.setHelperLanguage(HelperLanguage.ARABIC) }
                LanguageButton("English", language == HelperLanguage.ENGLISH, Modifier.weight(1f)) { viewModel.setHelperLanguage(HelperLanguage.ENGLISH) }
            }
        }
        AdventureCard { StreakSettings(viewModel, language) }
        AdventureCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(language.pick("🔊 المؤثرات الصوتية", "🔊 Sound effects"), color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = progress?.soundEnabled ?: true, onCheckedChange = { viewModel.setSoundEnabled(it) })
            }
            val speed = progress?.speechSpeed ?: 0.95f
            var sliderValue by remember(speed) { mutableFloatStateOf(speed) }
            Text(language.pick("🗣️ سرعة النطق", "🗣️ Voice speed") + "  ${"%.2f".format(sliderValue)}x", color = TextPrimary, fontWeight = FontWeight.Bold)
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { viewModel.setSpeechSpeed(sliderValue) },
                valueRange = 0.5f..1.3f,
                steps = 7
            )
            OutlinedButton(onClick = { viewModel.speakSpanish(tl("¡Hola! Bienvenida a Spanish Blaster.")) }) {
                Text(language.pick("جرّبي الصوت", "Test the voice"))
            }
            if (viewModel.speechEngine.spanishVoiceMissing) {
                Text(
                    language.pick(
                        tl("🔈 لا يوجد صوت إسباني على جهازك. ثبّتيه من: الإعدادات ← تحويل النص إلى كلام ← تثبيت بيانات الصوت ← Español."),
                        tl("🔈 No Spanish voice found. Install one in Settings → Text-to-speech → Install voice data → Español.")
                    ),
                    color = MeteorRed, fontSize = 12.sp
                )
            }
        }
        var confirmReset by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { confirmReset = true },
            border = BorderStroke(1.5.dp, MeteorRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(language.pick("🔄 إعادة ضبط التقدّم", "🔄 Reset progress"), color = MeteorRed, fontWeight = FontWeight.Bold) }
        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                containerColor = AdventureSurface,
                title = { Text(language.pick("البدء من جديد؟", "Start over?"), fontWeight = FontWeight.ExtraBold) },
                text = {
                    Text(
                        language.pick(
                            "سيُحذف كل تقدّمك: المستوى والنجوم والفصول والكلمات والسلسلة. لا يمكن التراجع عن ذلك.",
                            "All progress will be erased: level, stars, chapters, words and streak. This can't be undone."
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { confirmReset = false; viewModel.resetProgress() },
                        colors = ButtonDefaults.buttonColors(containerColor = MeteorRed)
                    ) { Text(language.pick("نعم، ابدئي من جديد", "Yes, reset"), fontWeight = FontWeight.Bold) }
                },
                dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(language.pick("إلغاء", "Cancel")) } }
            )
        }
        Text(
            language.pick("يعمل التطبيق بالكامل دون إنترنت.", "Works fully offline."),
            color = TextSecondary, fontSize = 11.sp
        )
    }
}

@Composable
private fun LinkRow(emoji: String, title: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AdventureSurface,
        border = BorderStroke(1.dp, AdventureCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = TextSecondary, fontSize = 24.sp)
        }
    }
}

@Composable
fun LanguageButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) ExplorerBlue else AdventureSurface,
        border = BorderStroke(1.5.dp, if (selected) ExplorerBlue else AdventureCardBorder),
        modifier = modifier.height(52.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
    }
}
