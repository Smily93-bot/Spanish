package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.content.SpanishContent
import com.example.data.model.CefrLevel
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel

/** One sentence to rebuild in the reactor. */
private data class ReactorSentence(val spanish: String, val translation: String, val ruleOrNote: String) {
    val words: List<String> = spanish.split(" ").filter { it.isNotBlank() }
}

private const val SENTENCES_PER_RUN = 8

private fun buildRun(data: SpanishContent, level: CefrLevel, language: HelperLanguage): List<ReactorSentence> {
    val phrases = data.phrases.filter { CefrLevel.fromCode(it.level) == level && it.spanish.split(" ").size >= 2 }
        .map { ReactorSentence(it.spanish, language.pick(it.arabic, it.english), language.pick(it.ruleAr, it.ruleEn)) }
    // The frequency list stops at C1, so C2 runs mix C2 phrases with C1 example sentences.
    val exampleLevel = if (level == CefrLevel.C2) CefrLevel.C1 else level
    val examples = data.frequency.asSequence()
        .filter { CefrLevel.fromCode(it.level) == exampleLevel && it.exampleEs.split(" ").size in 3..8 }
        .map { ReactorSentence(it.exampleEs, language.pick(it.exampleAr, it.exampleEn), "${it.shortSpanish} = ${it.meaning(language)}") }
        .toList()
    val shuffledPhrases = phrases.shuffled()
    val pool = (shuffledPhrases.take(4) + examples.shuffled().take(SENTENCES_PER_RUN) + shuffledPhrases.drop(4))
        .distinctBy { it.spanish }
    return pool.take(SENTENCES_PER_RUN).shuffled()
}

@Composable
fun GrammarReactorScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val reward by viewModel.practiceReward.collectAsStateWithLifecycle()
    val isArabic = language == HelperLanguage.ARABIC
    val data = content ?: return LoadingContent(isArabic)

    var level by rememberSaveable { mutableStateOf<CefrLevel?>(null) }
    var runKey by remember { mutableIntStateOf(0) }

    val selected = level
    if (selected == null) {
        LevelPicker(
            title = tl("⚛️ Reactor Gramatical"),
            subtitle = language.pick(
                "رتّبي أجزاء الجملة لتشغيل المفاعل. كل جملة صحيحة من المحاولة الأولى تمنحك طاقة إضافية.",
                "Put the sentence fragments in order to power the reactor. First-try answers charge it faster."
            ),
            color = NebulaPurple,
            language = language,
            onPick = { level = it; runKey++ }
        )
    } else {
        key(runKey) {
            ReactorRun(data, selected, viewModel, language, onExit = { level = null })
        }
    }

    reward?.let {
        RewardDialog(it, isArabic, language.pick("⚛️ المفاعل مشحون بالكامل!", tl("⚛️ ¡Reactor cargado!"))) {
            viewModel.dismissPracticeReward()
            level = null
        }
    }
}

@Composable
fun LevelPicker(title: String, subtitle: String, color: Color, language: HelperLanguage, onPick: (CefrLevel) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(SpaceNavy, color)))
                .padding(18.dp)
        ) {
            Text(title, color = StarWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = StarWhite.copy(alpha = 0.85f), fontSize = 13.sp)
        }
        SectionHeader(language.pick("اختاري المستوى", tl("Elige tu nivel")))
        CefrLevel.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { lvl ->
                    AdventureCard(modifier = Modifier.weight(1f), borderColor = color.copy(alpha = 0.5f), onClick = { onPick(lvl) }) {
                        Text(lvl.code, color = color, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Text(levelName(lvl, language), color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

fun levelName(level: CefrLevel, language: HelperLanguage) = when (level) {
    CefrLevel.A1 -> language.pick("مبتدئ", "Beginner")
    CefrLevel.A2 -> language.pick("أساسي", "Elementary")
    CefrLevel.B1 -> language.pick("متوسط", "Intermediate")
    CefrLevel.B2 -> language.pick("فوق المتوسط", "Upper intermediate")
    CefrLevel.C1 -> language.pick("متقدم", "Advanced")
    CefrLevel.C2 -> language.pick("إتقان", "Mastery")
}

@Composable
private fun ReactorRun(
    data: SpanishContent,
    level: CefrLevel,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onExit: () -> Unit
) {
    val run = remember { buildRun(data, level, language) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    if (run.isEmpty()) {
        Text("—", modifier = Modifier.padding(16.dp))
        return
    }
    val sentence = run[index]
    val shuffled = remember(index) {
        var s = sentence.words.withIndex().shuffled()
        if (sentence.words.size > 1) while (s.map { it.index } == sentence.words.indices.toList()) s = s.shuffled()
        s
    }
    val placed = remember(index) { mutableStateListOf<IndexedValue<String>>() }
    var mistakes by remember(index) { mutableIntStateOf(0) }
    var solved by remember(index) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("⚛️ ${level.code} · ${index + 1}/${run.size}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("$score", color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
            TextButton(onClick = onExit) { Text("✕", color = TextSecondary) }
        }
        ProgressBar((index + if (solved) 1 else 0) / run.size.toFloat(), color = NebulaPurple)
        if (streak >= 2) Text("🔥 x$streak", color = SolarAmber, fontWeight = FontWeight.Bold)

        AdventureCard(borderColor = NebulaPurple.copy(alpha = 0.5f)) {
            Text(language.pick(tl("ابني هذه الجملة بالإسبانية:"), tl("Build this sentence in Spanish:")), color = TextSecondary, fontSize = 12.sp)
            Text(sentence.translation, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        // Reactor core: placed words
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpaceNavy)
                .border(2.dp, if (solved) SuccessGreen else if (mistakes > 0) MeteorRed else NebulaPurple, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            placed.forEach { w ->
                WordTile(w.value, if (solved) SuccessGreen else DiamondCyan, dark = true) { if (!solved) placed.remove(w) }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            shuffled.filter { it !in placed }.forEach { w ->
                WordTile(w.value, NebulaPurple, dark = false) {
                    viewModel.soundEngine.click()
                    placed.add(w)
                }
            }
        }

        if (!solved && placed.size == sentence.words.size) {
            BlasterCyberButton(language.pick("شغّلي المفاعل", tl("Activar reactor")), {
                if (placed.map { it.value } == sentence.words) {
                    solved = true
                    val firstTry = mistakes == 0
                    if (firstTry) {
                        streak++
                        bestStreak = maxOf(bestStreak, streak)
                        correctCount++
                        score += 100 + streak * 20
                    } else {
                        streak = 0
                        score += 40
                    }
                    viewModel.soundEngine.hit()
                    viewModel.speakSpanish(sentence.spanish)
                } else {
                    mistakes++
                    streak = 0
                    viewModel.soundEngine.error()
                    placed.clear()
                }
            }, Modifier.fillMaxWidth(), NebulaPurple)
        }
        if (!solved && mistakes >= 2) {
            OutlinedButton(onClick = {
                placed.clear()
                placed.addAll(sentence.words.withIndex())
                solved = true
                streak = 0
            }, modifier = Modifier.fillMaxWidth()) { Text(language.pick("أظهري الحل", tl("Ver la solución")), color = TextSecondary) }
        }

        if (solved) {
            AdventureCard(borderColor = SuccessGreen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✅ " + sentence.spanish, color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    AudioButton(onClick = { viewModel.speakSpanish(sentence.spanish) })
                }
                if (sentence.ruleOrNote.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("💡 " + sentence.ruleOrNote, color = TextSecondary, fontSize = 13.sp)
                }
            }
            BlasterCyberButton(
                if (index + 1 < run.size) language.pick("الجملة التالية ›", tl("Siguiente ›")) else language.pick("إنهاء الجولة", tl("Terminar")),
                {
                    if (index + 1 < run.size) index++
                    else if (!finished) {
                        finished = true
                        viewModel.finishPracticeRun("REACTOR", score, correctCount, bestStreak)
                    }
                },
                Modifier.fillMaxWidth(),
                SuccessGreen,
                enabled = !finished
            )
        }
    }
}

@Composable
private fun WordTile(text: String, color: Color, dark: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (dark) color.copy(alpha = 0.18f) else AdventureSurface,
        border = BorderStroke(1.5.dp, color)
    ) {
        Text(
            text,
            color = if (dark) StarWhite else TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
