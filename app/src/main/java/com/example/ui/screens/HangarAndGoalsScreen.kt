package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HelperLanguage
import com.example.data.model.SHIP_TIERS
import com.example.data.model.pick
import com.example.data.model.shipTier
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import java.text.DateFormat
import java.util.Date

@Composable
fun HangarAndGoalsScreen(viewModel: BlasterViewModel) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val milestones by viewModel.milestones.collectAsStateWithLifecycle()
    val recent by viewModel.recentArcadeScores.collectAsStateWithLifecycle()
    val stats by viewModel.arcadeStats.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val showScores by viewModel.showHighScoresDialog.collectAsStateWithLifecycle()
    val scores by viewModel.localHighScores.collectAsStateWithLifecycle()
    val pb by viewModel.arcadePersonalBest.collectAsStateWithLifecycle()
    val filter by viewModel.selectedHighScoreFilter.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC

    val credits = progress?.starCredits ?: 0
    val current = shipTier(progress?.shipTier ?: 1)
    val next = SHIP_TIERS.firstOrNull { it.tier == current.tier + 1 }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ship hangar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(language.pick("حظيرة السفن", "HANGAR"), color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text(current.emoji, fontSize = 56.sp)
                Text(current.name, color = StarWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("🛡️ ${current.maxShield} · " + language.pick("الفئة", "Nivel") + " ${current.tier}/${SHIP_TIERS.size}", color = StarWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                Text("⭐ $credits", color = SolarGold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                if (next != null) {
                    BlasterCyberButton(
                        text = language.pick("ترقية إلى ${next.name} (⭐ ${next.cost})", "Mejorar a ${next.name} (⭐ ${next.cost})"),
                        onClick = { viewModel.upgradeShip() },
                        enabled = credits >= next.cost,
                        color = SolarAmber,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        language.pick("الترقية تزيد الدرع في Meteor Blaster إلى ${next.maxShield}", "Upgrading raises your Meteor Blaster shield to ${next.maxShield}"),
                        color = StarWhite.copy(alpha = 0.7f), fontSize = 11.sp
                    )
                } else {
                    Text(language.pick("💎 سفينتك في أعلى فئة!", "💎 ¡Tu nave está al máximo!"), color = DiamondCyan, fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SHIP_TIERS.forEach { tier ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (tier.tier == current.tier) ExplorerBlue.copy(alpha = 0.15f) else AdventureSurface,
                        border = BorderStroke(1.dp, if (tier.tier <= current.tier) ExplorerBlue else AdventureCardBorder),
                        modifier = Modifier.weight(1f).alpha(if (tier.tier <= current.tier) 1f else 0.55f)
                    ) {
                        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(tier.emoji, fontSize = 20.sp)
                            Text(tier.name, fontSize = 9.sp, color = TextPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }

        // Hall of Fame
        item {
            SectionHeader(language.pick("قاعة المشاهير", "SALÓN DE LA FAMA"), language.pick("أفضل نتائجك في كل وضع", "Your best results per mode"))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TRANSLATION" to "🔤", "SYNONYM" to "🔁", "ANTONYM" to "⚖️", "CLOZE" to "🌀", "REACTOR" to "⚛️").forEach { (mode, emoji) ->
                    val s = stats.firstOrNull { it.gameMode == mode }
                    HudStatCard("${s?.totalGamesPlayed ?: 0} " + language.pick("جولة", "runs"), "${s?.highScore ?: 0}", emoji, Modifier.weight(1f), SolarAmber)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { viewModel.fetchLocalHighScores("ALL") },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.2.dp, SolarGold),
                modifier = Modifier.fillMaxWidth()
            ) { Text("🏆 " + language.pick("عرض لوحة الشرف", "Ver récords locales"), color = SolarAmber, fontWeight = FontWeight.Bold) }
        }
        if (recent.isNotEmpty()) {
            item { SectionHeader(language.pick("آخر الجولات", "PARTIDAS RECIENTES")) }
            items(recent.take(5)) { run ->
                AdventureCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LevelChip(run.rankGrade, SolarAmber)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(run.gameMode, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(run.timestamp)), color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("${run.score} pts", color = SolarAmber, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Milestones
        item {
            SectionHeader(
                language.pick("الإنجازات", "LOGROS"),
                "${milestones.count { it.isUnlocked }}/${milestones.size} " + language.pick("مفتوحة", "desbloqueados")
            )
        }
        items(milestones, key = { it.goalId }) { goal ->
            AdventureCard(borderColor = if (goal.isUnlocked) SuccessGreen else AdventureCardBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (goal.isUnlocked) "🎖️" else "🔒", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(goal.title, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(goal.description, color = TextSecondary, fontSize = 12.sp)
                    }
                    Text("⭐ ${goal.rewardCredits}", color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                ProgressBar(goal.currentProgress / goal.targetValue.toFloat(), color = if (goal.isUnlocked) SuccessGreen else ExplorerBlue, height = 6.dp)
                Text("${goal.currentProgress}/${goal.targetValue}", color = TextSecondary, fontSize = 10.sp)
            }
        }
    }

    if (showScores) {
        ArcadeHighScoresDialog(
            scores = scores,
            personalBest = pb,
            selectedFilter = filter,
            isArabic = isArabic,
            onSelectFilter = viewModel::setHighScoreFilter,
            onDismiss = viewModel::dismissHighScoresDialog
        )
    }
}
