package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HelperLanguage
import com.example.data.model.Rank
import com.example.data.model.pick
import com.example.ui.components.*
import com.example.ui.navigation.Screen
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import java.util.Calendar

@Composable
fun CommandBridgeScreen(viewModel: BlasterViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val tablets by viewModel.tabletProgress.collectAsStateWithLifecycle()
    val personalBest by viewModel.personalBestScore.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC
    val data = content ?: return LoadingContent(isArabic)

    val level = progress?.level ?: 1
    val rank = Rank.forLevel(level)
    val nextRank = Rank.next(level)
    val completedIds = tablets.filter { it.isCompleted }.map { it.tabletId }.toSet()
    val nextTablet = data.tablets.firstOrNull { it.id !in completedIds }
    val wordOfDay = remember(data) {
        val pool = data.frequency.filter { it.rank <= 1500 && it.partOfSpeech in setOf("noun", "verb", "adjective") }
        pool[Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % pool.size]
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Explorer header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(SpaceNavy, Color(0xFF15367A), NebulaPurple)))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    language.pick("مرحبًا أيتها المستكشفة!", "¡Hola, exploradora!"),
                    color = DiamondCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rank.emoji, fontSize = 34.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(rank.spanish, color = StarWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${rank.label(language)} · NIVEL $level", color = StarWhite.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                }
                ProgressBar(
                    progress = (progress?.currentXp ?: 0).toFloat() / (progress?.xpToNextLevel ?: 300),
                    color = SolarGold
                )
                Text(
                    "${progress?.currentXp ?: 0} / ${progress?.xpToNextLevel ?: 300} XP" +
                        (nextRank?.let { "  ·  " + language.pick("الرتبة التالية: ", "Next: ") + "${it.spanish} (Nv ${it.minLevel})" } ?: ""),
                    color = StarWhite.copy(alpha = 0.8f), fontSize = 11.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudStatCard(language.pick("نجوم", "Créditos"), "${progress?.starCredits ?: 0}", "⭐", Modifier.weight(1f), SolarAmber)
            HudStatCard(language.pick("ألواح", "Tablillas"), "${completedIds.size}/${data.tablets.size}", "📜", Modifier.weight(1f))
            HudStatCard(language.pick("كلمات", "Palabras"), "${progress?.totalWordsMastered ?: 0}", "🧠", Modifier.weight(1f), SuccessGreen)
            HudStatCard(language.pick("رقم قياسي", "Récord"), "$personalBest", "🏆", Modifier.weight(1f), NebulaPurple)
        }

        if (viewModel.speechEngine.spanishVoiceMissing) {
            AdventureCard(borderColor = MeteorRed) {
                Text(
                    language.pick(
                        "🔈 لا يوجد صوت إسباني على جهازك. ثبّتيه من: الإعدادات ← تحويل النص إلى كلام ← تثبيت بيانات الصوت ← Español.",
                        "🔈 No Spanish voice found. Install one in Settings → Text-to-speech → Install voice data → Español."
                    ),
                    color = TextPrimary, fontSize = 12.sp
                )
            }
        }

        // Word of the day
        AdventureCard(borderColor = SolarGold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(language.pick("كلمة اليوم", "PALABRA DEL DÍA"), color = SolarAmber, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    Text(wordOfDay.shortSpanish, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    Text(wordOfDay.meaning(language), color = ExplorerBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                AudioButton(onClick = { viewModel.speakSpanish(wordOfDay.shortSpanish) }, size = 44.dp)
            }
            if (wordOfDay.exampleEs.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("“${wordOfDay.exampleEs}”", color = TextPrimary, fontSize = 13.sp)
                        Text(language.pick(wordOfDay.exampleAr, wordOfDay.exampleEn), color = TextSecondary, fontSize = 12.sp)
                    }
                    AudioButton(onClick = { viewModel.speakSpanish(wordOfDay.exampleEs) }, size = 32.dp, tint = NebulaPurple)
                }
            }
        }

        SectionHeader(language.pick("المهمات", "MISIONES"), language.pick("اختاري مهمة للانطلاق", "Choose a mission to launch"))

        if (nextTablet != null) {
            MissionCard(
                emoji = "📜",
                title = language.pick("تابعي البعثة: ", "Continue: ") + nextTablet.title(language),
                subtitle = "${nextTablet.level} · " + nextTablet.goal(language),
                color = SolarAmber
            ) { viewModel.openTablet(nextTablet.id) }
        } else {
            MissionCard("🏁", language.pick("أكملتِ الحملة كاملة!", "Campaign complete!"), language.pick("أعيدي أي فصل لتحسين نتيجتك", "Replay any chapter to improve your score"), SuccessGreen) {
                viewModel.navigateTo(Screen.AdventureMap)
            }
        }
        MissionCard("☄️", language.pick("مدفع النيازك", "Meteor Blaster"), language.pick("دمّري النيازك بالكلمة الصحيحة", "Blast meteors with the right word"), MeteorRed) {
            viewModel.navigateTo(Screen.MeteorBlaster)
        }
        MissionCard("⚛️", "Reactor Gramatical", language.pick("رتّبي الكلمات لبناء الجملة", "Rebuild sentences word by word"), NebulaPurple) {
            viewModel.navigateTo(Screen.GrammarReactor)
        }
        MissionCard("🌀", "Cloze Cuántico", language.pick("أكملي الجملة بالكلمة المفقودة", "Fill the missing word in context"), ExplorerBlue) {
            viewModel.navigateTo(Screen.QuantumCloze)
        }
        MissionCard("🗺️", language.pick("خريطة المجرة", "Mapa galáctico"), language.pick("12 فصلًا من A1 إلى C2", "12 chapters from A1 to C2"), DiamondCyan) {
            viewModel.navigateTo(Screen.AdventureMap)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MissionCard(emoji: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    AdventureCard(borderColor = color.copy(alpha = 0.5f), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(color.copy(alpha = 0.15f))
            ) { Text(emoji, fontSize = 24.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp, maxLines = 2)
            }
            Text("›", color = color, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }
    }
}
