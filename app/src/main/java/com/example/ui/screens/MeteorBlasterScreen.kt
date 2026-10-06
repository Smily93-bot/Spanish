package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BlasterMode
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.data.model.shipTier
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import com.example.ui.viewmodel.MeteorGameState
import kotlin.random.Random

@Composable
fun MeteorBlasterScreen(viewModel: BlasterViewModel) {
    val state by viewModel.meteorState.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()
    val showScores by viewModel.showHighScoresDialog.collectAsStateWithLifecycle()
    val scores by viewModel.localHighScores.collectAsStateWithLifecycle()
    val pb by viewModel.arcadePersonalBest.collectAsStateWithLifecycle()
    val filter by viewModel.selectedHighScoreFilter.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC

    if (content == null) return LoadingContent(isArabic)

    when {
        state.isGameOver -> GameOverPanel(state, viewModel, language)
        state.isRunning -> Arena(state, viewModel, language)
        else -> ModeSelect(viewModel, language)
    }

    if (showScores) {
        ArcadeHighScoresDialog(
            scores = scores,
            personalBest = pb,
            selectedFilter = filter,
            isArabic = isArabic,
            onSelectFilter = viewModel::setHighScoreFilter,
            onPlayAgain = BlasterMode.entries.firstOrNull { it.name == filter }?.let { mode -> { viewModel.startMeteorGame(mode) } },
            onDismiss = viewModel::dismissHighScoresDialog
        )
    }
}

@Composable
private fun ModeSelect(viewModel: BlasterViewModel, language: HelperLanguage) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val ship = shipTier(progress?.shipTier ?: 1)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
        ) {
            StarField(Modifier.matchParentSize())
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(language.pick("☄️ مدفع النيازك ☄️", "☄️ METEOR BLASTER ☄️"), color = SolarGold, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Text(
                    language.pick("اضغطي على النيزك الذي يحمل الإجابة الصحيحة قبل أن يصطدم بسفينتك!", "Tap the meteor with the right answer before it hits your ship!"),
                    color = StarWhite, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("${ship.emoji} ${ship.name} · 🛡️ ${ship.maxShield}", color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        ModeCard("🔤", BlasterMode.TRANSLATION, language.pick("كلمة إسبانية ← معناها", "Spanish word → its meaning"), ExplorerBlue, viewModel, language)
        ModeCard("🔁", BlasterMode.SYNONYM, language.pick("اختاري الكلمة الإسبانية المرادفة", "Pick the Spanish synonym"), NebulaPurple, viewModel, language)
        ModeCard("⚖️", BlasterMode.ANTONYM, language.pick("اختاري الكلمة الإسبانية المضادة", "Pick the Spanish opposite"), SolarAmber, viewModel, language)
        OutlinedButton(
            onClick = { viewModel.fetchLocalHighScores("ALL") },
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.2.dp, SolarGold),
            modifier = Modifier.fillMaxWidth()
        ) { Text("🏆 " + language.pick("لوحة الشرف", "Salón de récords"), color = SolarAmber, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ModeCard(emoji: String, mode: BlasterMode, subtitle: String, color: Color, viewModel: BlasterViewModel, language: HelperLanguage) {
    AdventureCard(borderColor = color.copy(alpha = 0.6f), onClick = { viewModel.startMeteorGame(mode) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("${mode.labelEs} · ${mode.label(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Text("▶", color = color, fontSize = 20.sp)
        }
    }
}

@Composable
private fun Arena(state: MeteorGameState, viewModel: BlasterViewModel, language: HelperLanguage) {
    val progress by viewModel.userProgress.collectAsStateWithLifecycle()
    val word = state.currentWord ?: return
    val fall = remember { Animatable(0f) }
    LaunchedEffect(state.roundId) {
        fall.snapTo(0f)
        fall.animateTo(1f, tween(state.fallDurationMs, easing = LinearEasing))
    }
    LaunchedEffect(state.blastedIndex) { if (state.blastedIndex != null) fall.stop() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
    ) {
        // HUD
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("${state.score}", color = SolarGold, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                Text("PB ${maxOf(state.personalBest, state.score)} · x${state.comboStreak}", color = StarWhite.copy(alpha = 0.7f), fontSize = 11.sp)
            }
            Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.End) {
                Text("🛡️ ${state.shield}/${state.maxShield}", color = StarWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ProgressBar(
                    state.shield / state.maxShield.toFloat(),
                    color = if (state.shield > state.maxShield / 3) SuccessGreen else MeteorRed,
                    height = 8.dp
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { viewModel.exitMeteorGame() }) { Text("✕", color = StarWhite, fontSize = 18.sp) }
        }

        // Prompt
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StarWhite.copy(alpha = 0.08f))
                .border(1.dp, DiamondCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.promptQuestion, color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(word.prompt, color = StarWhite, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                if (state.mode != BlasterMode.TRANSLATION) {
                    Text("(${word.hint})", color = StarWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
            AudioButton(onClick = { viewModel.speakSpanish(word.spanishToSpeak) }, size = 42.dp)
        }

        state.lastHitEffect?.let {
            Text(
                it,
                color = if (state.blastedIndex != null) SolarGold else MeteorRed,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }

        // Falling meteors
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            StarField(Modifier.matchParentSize())
            val laneWidth = maxWidth / state.options.size.coerceAtLeast(1)
            val meteorSize = minOf(laneWidth - 8.dp, 92.dp)
            val travel = maxHeight - meteorSize - 56.dp
            state.options.forEachIndexed { index, option ->
                val stagger = (index % 2) * 0.06f
                val progress = (fall.value * (1f + stagger) - stagger).coerceIn(0f, 1f)
                Meteor(
                    text = option,
                    wrong = index in state.wrongPicks,
                    blasted = state.blastedIndex == index,
                    reveal = state.blastedIndex == null && fall.value >= 1f && index == state.correctIndex,
                    modifier = Modifier
                        .offset(x = laneWidth * index + (laneWidth - meteorSize) / 2, y = travel * progress)
                        .size(meteorSize),
                    onClick = { viewModel.shootOption(index) }
                )
            }
            // Player ship
            Text(
                shipTier(progress?.shipTier ?: 1).emoji,
                fontSize = 40.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun Meteor(text: String, wrong: Boolean, blasted: Boolean, reveal: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (blasted) 1.4f else 1f, tween(250), label = "blast")
    val alpha by animateFloatAsState(if (blasted) 0f else if (wrong) 0.35f else 1f, tween(450), label = "fade")
    val rotation = rememberInfiniteTransition(label = "spin")
    val wobble by rotation.animateFloat(-6f, 6f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "wobble")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .alpha(alpha.coerceAtLeast(if (blasted) 0f else 0.2f))
            .offset(x = wobble.dp / 3)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    if (reveal) listOf(SolarGold, SolarAmber)
                    else if (wrong) listOf(MeteorRed.copy(alpha = 0.6f), Color(0xFF3B1A1A))
                    else listOf(Color(0xFF9C7A5B), Color(0xFF4A3426))
                )
            )
            .border(2.dp, if (reveal) SolarGold else Color(0xFFFFB86B).copy(alpha = 0.7f), CircleShape)
            .clickable(enabled = !wrong && !blasted, onClick = onClick)
            .padding(6.dp)
    ) {
        Text(
            if (blasted) "💥" else text,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (blasted) 30.sp else if (text.length > 12) 11.sp else 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            maxLines = 3
        )
    }
}

@Composable
private fun StarField(modifier: Modifier) {
    val stars = remember { List(60) { Triple(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 2.5f + 0.5f) } }
    val twinkle = rememberInfiniteTransition(label = "stars")
    val alpha by twinkle.animateFloat(0.4f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "twinkle")
    Canvas(modifier) {
        stars.forEachIndexed { i, (x, y, r) ->
            drawCircle(
                color = StarWhite.copy(alpha = if (i % 3 == 0) alpha else 0.7f),
                radius = r,
                center = Offset(x * size.width, y * size.height)
            )
        }
    }
}

@Composable
private fun GameOverPanel(state: MeteorGameState, viewModel: BlasterViewModel, language: HelperLanguage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(language.pick("انتهت الجولة", "FIN DE LA PARTIDA"), color = MeteorRed, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("${state.score}", color = SolarGold, fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        state.reward?.let { Text(language.pick("التقدير ", "Rango ") + it.rankGrade, color = DiamondCyan, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        if (state.isNewPersonalBest) {
            Text(language.pick("🏆 رقم قياسي شخصي جديد!", "🏆 ¡NUEVO RÉCORD PERSONAL!"), color = SolarGold, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        } else if (state.personalBest > 0) {
            Text(language.pick("رقمك القياسي: ", "Tu récord: ") + state.personalBest, color = StarWhite.copy(alpha = 0.8f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudStatCard(language.pick("كلمات", "Palabras"), "${state.wordsBlasted}", "☄️", Modifier.weight(1f))
            HudStatCard(language.pick("أفضل سلسلة", "Racha"), "x${state.maxStreak}", "🔥", Modifier.weight(1f), SolarAmber)
            HudStatCard("XP", "+${state.reward?.xpGained ?: 0}", "✨", Modifier.weight(1f), NebulaPurple)
            HudStatCard(language.pick("نجوم", "Créditos"), "+${state.reward?.creditsGained ?: 0}", "⭐", Modifier.weight(1f), SolarAmber)
        }
        state.reward?.let { reward ->
            if (reward.leveledUp) Text(language.pick("🚀 المستوى ${reward.newLevel}!", "🚀 ¡Nivel ${reward.newLevel}!"), color = DiamondCyan, fontWeight = FontWeight.Bold)
            reward.unlockedMilestones.forEach { Text("🎖️ ${it.title} (+${it.rewardCredits} ⭐)", color = SuccessGreen, fontWeight = FontWeight.Bold) }
        }
        BlasterCyberButton(language.pick("العبي مجددًا", "JUGAR OTRA VEZ"), { viewModel.startMeteorGame(state.mode) }, Modifier.fillMaxWidth(), SolarAmber)
        BlasterCyberButton(language.pick("🏆 لوحة الشرف", "🏆 Récords"), { viewModel.fetchLocalHighScores(state.mode.name) }, Modifier.fillMaxWidth(), NebulaPurple)
        OutlinedButton(
            onClick = { viewModel.exitMeteorGame() },
            border = BorderStroke(1.dp, StarWhite.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) { Text(language.pick("تغيير الوضع", "Cambiar modo"), color = StarWhite) }
    }
}
