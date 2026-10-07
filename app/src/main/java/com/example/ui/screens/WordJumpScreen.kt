package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.content.SpanishContent
import com.example.data.model.BlasterMode
import com.example.data.model.HelperLanguage
import com.example.data.model.MeteorWord
import com.example.data.model.normalizeAnswer
import com.example.data.model.pick
import com.example.ui.components.AudioButton
import com.example.ui.components.LoadingContent
import com.example.ui.components.RewardDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlinx.coroutines.delay

private const val JUMP_ROUNDS = 10
private const val JUMP_LIVES = 3

/** Practice → Word Jump: ten quick rounds of meaning, synonym and opposite, three lives. */
@Composable
fun WordJumpScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val reward by viewModel.practiceReward.collectAsStateWithLifecycle()
    val data = content ?: return LoadingContent(language == HelperLanguage.ARABIC)
    var run by remember { mutableIntStateOf(0) }   // 0 = start card

    if (run == 0) {
        JumpStart(language) { run++ }
    } else {
        key(run) { JumpRun(data, viewModel, language) }
    }
    reward?.let {
        RewardDialog(it, language == HelperLanguage.ARABIC, language.pick("🦘 قفزة رائعة!", "🦘 " + tl("¡Gran salto!"))) {
            viewModel.dismissPracticeReward()
            run = 0
        }
    }
}

@Composable
private fun JumpStart(language: HelperLanguage, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text("🦘", fontSize = 72.sp)
        Text("Word Jump", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            language.pick(
                "اضغطي على الكلمة الصحيحة لتقفز ليا إليها.\n10 جولات و3 قلوب.",
                "Tap the right word and Lía jumps onto it.\n10 rounds, 3 hearts."
            ),
            color = TextSecondary, fontSize = 16.sp, textAlign = TextAlign.Center
        )
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) { Text(language.pick("ابدئي ▶", "START ▶"), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun JumpRun(data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage) {
    val level = viewModel.userProgress.collectAsStateWithLifecycle().value?.level ?: 1
    var round by remember { mutableIntStateOf(1) }
    var lives by remember { mutableIntStateOf(JUMP_LIVES) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var over by remember { mutableStateOf(false) }
    val modes = remember { listOf(BlasterMode.TRANSLATION, BlasterMode.SYNONYM, BlasterMode.ANTONYM) }
    val word: MeteorWord = remember(round) { data.meteorRound(modes[(round - 1) % modes.size], language, level) }
    val mode = modes[(round - 1) % modes.size]
    val wrong = remember(round) { mutableStateListOf<String>() }
    var solved by remember(round) { mutableStateOf(false) }

    fun finish() {
        if (over) return
        over = true
        viewModel.finishPracticeRun("JUMP", score, correct, bestCombo)
    }

    // Short pause on the right bubble, then the next round.
    LaunchedEffect(solved) {
        if (solved) {
            delay(1100)
            if (round >= JUMP_ROUNDS) finish() else round++
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp)) {
        // One quiet status line.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❤️".repeat(lives) + "🤍".repeat(JUMP_LIVES - lives), fontSize = 18.sp)
            Text("$round / $JUMP_ROUNDS", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("⭐ $score", color = SolarAmber, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        // The question.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    when (mode) {
                        BlasterMode.TRANSLATION -> language.pick("ما معنى", "What does it mean?")
                        BlasterMode.SYNONYM -> language.pick("اقفزي على المرادف", "Jump on the synonym")
                        BlasterMode.ANTONYM -> language.pick("اقفزي على العكس", "Jump on the opposite")
                    },
                    color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
                Text(word.prompt, color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            }
            AudioButton(onClick = { viewModel.speakSpanish(word.spanishToSpeak) }, size = 46.dp)
            if (combo >= 2) Text("  🔥x$combo", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            key(round) {
                WordJump(
                    options = word.options,
                    solved = solved,
                    wrong = wrong,
                    check = { normalizeAnswer(it) == normalizeAnswer(word.answer) },
                    boardHeight = maxHeight,
                    onLanded = { option ->
                        if (solved || over) return@WordJump
                        if (normalizeAnswer(option) == normalizeAnswer(word.answer)) {
                            solved = true
                            combo++
                            bestCombo = maxOf(bestCombo, combo)
                            correct++
                            score += 10 + 5 * combo + if (wrong.isEmpty()) 10 else 0
                            viewModel.soundEngine.hit()
                            viewModel.speakSpanish(word.spanishToSpeak)
                        } else {
                            wrong += option
                            combo = 0
                            lives--
                            viewModel.soundEngine.error()
                            if (lives <= 0) finish()
                        }
                    }
                )
            }
        }
    }
    LaunchedEffect(Unit) { viewModel.speakSpanish(word.spanishToSpeak) }
    LaunchedEffect(round) { if (round > 1) viewModel.speakSpanish(word.spanishToSpeak) }
}
