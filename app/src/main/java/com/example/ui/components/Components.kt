package com.example.ui.components

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.database.ArcadeScoreEntity
import com.example.data.repository.RewardResult
import com.example.ui.theme.*

@Composable
fun BlasterCyberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ExplorerBlue,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 0.5.sp, textAlign = TextAlign.Center)
    }
}

/** Speaker button that reads Spanish text aloud with the device's TTS voice. */
@Composable
fun AudioButton(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    // Every sound button looks the same: a solid cyan circle with a white speaker.
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = DiamondCyan,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.VolumeUp, contentDescription = tl("Escuchar"), tint = Color.White, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
fun AdventureCard(
    modifier: Modifier = Modifier,
    borderColor: Color = AdventureCardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(AdventureSurface)
        .border(1.dp, borderColor, RoundedCornerShape(18.dp))
    Column(
        modifier = (if (onClick != null) base.clickable(onClick = onClick) else base).padding(16.dp),
        content = content
    )
}

@Composable
fun HudStatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier, accent: Color = ExplorerBlue) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AdventureSurface),
        border = BorderStroke(1.dp, AdventureCardBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 20.sp)
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, fontFamily = FontFamily.Monospace)
            Text(label, color = TextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun ProgressBar(progress: Float, modifier: Modifier = Modifier, color: Color = ExplorerBlue, height: Dp = 10.dp) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height))
            .background(AdventureSurfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(height))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color)))
        )
    }
}

@Composable
fun LevelChip(text: String, color: Color = NebulaPurple) {
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LoadingContent(isArabic: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ExplorerBlue)
            Spacer(Modifier.height(12.dp))
            Text(if (isArabic) "جارٍ تحميل المجرة…" else tl("Cargando la galaxia…"), color = TextSecondary)
        }
    }
}

/** Shown after a tablet, cloze or reactor run. */
@Composable
fun RewardDialog(reward: RewardResult, isArabic: Boolean, title: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdventureSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(title, color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reward.rankGrade.isNotEmpty()) {
                    Text(
                        (if (isArabic) "التقدير: " else tl("Rango: ")) + reward.rankGrade,
                        color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                    )
                }
                Text("+${reward.xpGained} XP   ⭐ +${reward.creditsGained}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (reward.isNewPersonalBest) {
                    Text(if (isArabic) "🏆 رقم قياسي جديد!" else tl("🏆 ¡Nuevo récord personal!"), color = SolarGold, fontWeight = FontWeight.Bold)
                }
                if (reward.leveledUp) {
                    Text(
                        if (isArabic) "🚀 ارتقيتِ إلى المستوى ${reward.newLevel}! (+50 ⭐)" else tl("🚀 ¡Subiste al nivel %d! (+50 ⭐)").format(reward.newLevel),
                        color = ExplorerBlue, fontWeight = FontWeight.Bold
                    )
                }
                reward.unlockedMilestones.forEach {
                    Text("🎖️ ${it.title} (+${it.rewardCredits} ⭐)", color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            BlasterCyberButton(text = if (isArabic) "متابعة" else tl("Continuar"), onClick = onDismiss)
        }
    )
}

/** Localised name of an arcade mode stored in the scores table. */
fun gameModeLabel(mode: String, isArabic: Boolean): String =
    HIGH_SCORE_FILTERS.firstOrNull { it.first == mode }?.let { if (isArabic) it.second else it.third } ?: mode

private val HIGH_SCORE_FILTERS = listOf(
    Triple("ALL", "الكل", tl("Todos")),
    Triple("TRANSLATION", "المعنى", tl("Significado")),
    Triple("SYNONYM", "المرادفات", tl("Sinónimos")),
    Triple("ANTONYM", "الأضداد", tl("Antónimos")),
    Triple("CLOZE", "الفراغات", tl("Huecos")),
    Triple("REACTOR", "الجمل", tl("Frases")),
    Triple("JUMP", "القفز", "Word Jump")
)

@Composable
fun ArcadeHighScoresDialog(
    scores: List<ArcadeScoreEntity>,
    personalBest: Int,
    selectedFilter: String,
    isArabic: Boolean,
    onSelectFilter: (String) -> Unit,
    onPlayAgain: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .fillMaxWidth()
            .testTag("arcade_high_scores_dialog"),
        containerColor = AdventureSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = SolarGold,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "لوحة الشرف والأرقام القياسية" else tl("SALÓN DE RÉCORDS LOCALES"),
                        color = SolarAmber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Personal Best High-Score Highlight Card (Encourages Replayability)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AdventureSurfaceVariant),
                    border = BorderStroke(1.5.dp, SolarGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(SolarGold.copy(alpha = 0.2f))
                                .border(1.5.dp, SolarGold, CircleShape)
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = SolarGold, modifier = Modifier.size(26.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isArabic) "رقمك القياسي الشخصي (Personal Best)" else tl("TU RÉCORD PERSONAL MÁXIMO"),
                                color = SolarAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (personalBest > 0) "$personalBest PTS" else (if (isArabic) "لا يوجد بعد" else tl("Sin récord")),
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (personalBest > 0) {
                                    if (isArabic) "🚀 تحدَّ نفسك وحطم هذا الرقم في جولتك القادمة!" else tl("🚀 ¡Supera tu récord en tu próxima partida!")
                                } else {
                                    if (isArabic) "✨ العب جولة الآن لتسجيل أول رقم قياسي لك!" else tl("✨ ¡Juega una partida para registrar tu primera marca!")
                                },
                                color = ExplorerBlue,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Filter chips (ALL, TRANSLATION, SYNONYM, ANTONYM, CLOZE, REACTOR)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HIGH_SCORE_FILTERS.forEach { (key, arabic, spanish) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            onClick = { onSelectFilter(key) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) ExplorerBlue else AdventureSurfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) ExplorerBlue else AdventureCardBorder)
                        ) {
                            Text(
                                text = if (isArabic) arabic else spanish,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                if (scores.isEmpty()) {
                    Text(
                        if (isArabic) "لا توجد جولات مسجلة في هذا الوضع بعد." else tl("Aún no hay partidas en este modo."),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                // High Scores List
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(scores) { index, scoreItem ->
                        val rankColor = when (index) {
                            0 -> SolarGold
                            1 -> Color(0xFF94A3B8)
                            2 -> Color(0xFFD97706)
                            else -> ExplorerBlue
                        }
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = AdventureSurfaceVariant),
                            border = BorderStroke(1.dp, if (index == 0) SolarGold else AdventureCardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = rankColor) {
                                        Text(
                                            text = when (index) { 0 -> "1º 👑"; 1 -> "2º 🥈"; 2 -> "3º 🥉"; else -> "#${index + 1}" },
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("${gameModeLabel(scoreItem.gameMode, isArabic)} · ${scoreItem.rankGrade}", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(if (isArabic) "${scoreItem.wordsBlasted} كلمة • سلسلة x${scoreItem.maxComboStreak}" else "${scoreItem.wordsBlasted} ${tl("palabras")} • ${tl("Racha")} x${scoreItem.maxComboStreak}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Text("${scoreItem.score} pts", color = SolarAmber, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (onPlayAgain != null) {
                BlasterCyberButton(
                    text = if (isArabic) "العب لتحطيم الرقم" else tl("JUGAR (NUEVO RÉCORD)"),
                    onClick = { onDismiss(); onPlayAgain() },
                    modifier = Modifier.fillMaxWidth(),
                    color = SolarAmber
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.2.dp, ExplorerBlue),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isArabic) "إغلاق النافذة" else tl("Cerrar"), color = ExplorerBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * In Arabic text, wraps every run of Latin words (Spanish/Italian examples such as "soy, eres") in a
 * left-to-right isolate so it reads in the right order instead of being scrambled by the RTL layout.
 */
fun bidiSafe(text: String): String =
    Regex("[A-Za-zÀ-ÿ'’¿¡]+(?:[ ,/·-]+[A-Za-zÀ-ÿ'’¿¡!?.]+)*").replace(text) { "⁦${it.value}⁩" }
