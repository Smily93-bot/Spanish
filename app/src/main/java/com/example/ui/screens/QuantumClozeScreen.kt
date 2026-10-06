package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.delay

private const val CLOZE_QUESTIONS = 10
private const val CLOZE_SECONDS = 20

@Composable
fun QuantumClozeScreen(viewModel: BlasterViewModel) {
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
            title = tl("🌀 Cloze Cuántico"),
            subtitle = language.pick(
                "اختاري الكلمة التي تكمل الجملة قبل أن ينهار الحقل الكمي. الجمل مأخوذة من قائمة أكثر 5000 كلمة استخدامًا.",
                "Pick the word that completes the sentence before the quantum field collapses. Sentences come from the 5000 most frequent words."
            ),
            color = ExplorerBlue,
            language = language,
            onPick = { level = it; runKey++ }
        )
    } else {
        key(runKey) { ClozeRun(data, selected, viewModel, language, onExit = { level = null }) }
    }

    reward?.let {
        RewardDialog(it, isArabic, language.pick("🌀 استقر الحقل الكمي!", tl("🌀 ¡Campo cuántico estabilizado!"))) {
            viewModel.dismissPracticeReward()
            level = null
        }
    }
}

@Composable
private fun ClozeRun(
    data: SpanishContent,
    level: CefrLevel,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onExit: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    val question = remember(index) { data.clozeQuestion(language, level) }
    var picked by remember(index) { mutableStateOf<String?>(null) }
    var secondsLeft by remember(index) { mutableIntStateOf(CLOZE_SECONDS) }

    fun answer(option: String?) {
        if (picked != null) return
        picked = option ?: ""
        val correct = option == question.answer
        if (correct) {
            streak++
            bestStreak = maxOf(bestStreak, streak)
            correctCount++
            score += 50 + secondsLeft * 5 + streak * 10
            viewModel.soundEngine.hit()
        } else {
            streak = 0
            viewModel.soundEngine.error()
        }
        viewModel.recordWord(question.answer, question.meaning, "cloze", correct)
        viewModel.speakSpanish(question.fullSentence)
    }

    LaunchedEffect(index) {
        while (secondsLeft > 0 && picked == null) {
            delay(1000)
            if (picked == null) secondsLeft--
        }
        if (picked == null) answer(null)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌀 ${level.code} · ${index + 1}/$CLOZE_QUESTIONS", color = TextPrimary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            Text("$score", color = ExplorerBlue, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
            TextButton(onClick = onExit) { Text("✕", color = TextSecondary) }
        }
        ProgressBar(secondsLeft / CLOZE_SECONDS.toFloat(), color = if (secondsLeft > 6) ExplorerBlue else MeteorRed, height = 8.dp)
        Row {
            Text("⏱️ ${secondsLeft}s", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (streak >= 2) Text("🔥 x$streak", color = SolarAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        AdventureCard(borderColor = ExplorerBlue.copy(alpha = 0.5f)) {
            LevelChip(question.level, ExplorerBlue)
            Spacer(Modifier.height(8.dp))
            Text(
                if (picked != null) question.fullSentence else question.sentenceWithGap,
                color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(question.translation, color = TextSecondary, fontSize = 14.sp)
        }

        question.options.forEach { option ->
            val isAnswer = option == question.answer
            val color = when {
                picked == null -> ExplorerBlue
                isAnswer -> SuccessGreen
                option == picked -> MeteorRed
                else -> TextSecondary
            }
            OutlinedButton(
                onClick = { answer(option) },
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, color),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (picked != null && (isAnswer || option == picked)) color.copy(alpha = 0.12f) else Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(option, color = color, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }

        if (picked != null) {
            AdventureCard(borderColor = if (picked == question.answer) SuccessGreen else MeteorRed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (picked == question.answer) language.pick("✅ أحسنتِ!", tl("✅ ¡Correcto!"))
                            else if (picked == "") language.pick("⏱️ انتهى الوقت", tl("⏱️ ¡Tiempo!"))
                            else language.pick("❌ الإجابة الصحيحة: ", tl("❌ Respuesta: ")) + question.answer,
                            color = TextPrimary, fontWeight = FontWeight.Bold
                        )
                        Text("${question.answer} = ${question.meaning}", color = TextSecondary, fontSize = 13.sp)
                    }
                    AudioButton(onClick = { viewModel.speakSpanish(question.fullSentence) })
                }
            }
            BlasterCyberButton(
                if (index + 1 < CLOZE_QUESTIONS) language.pick("التالي ›", tl("Siguiente ›")) else language.pick("إنهاء الجولة", tl("Terminar")),
                {
                    if (index + 1 < CLOZE_QUESTIONS) index++
                    else if (!finished) {
                        finished = true
                        viewModel.finishPracticeRun("CLOZE", score, correctCount, bestStreak)
                    }
                },
                Modifier.fillMaxWidth(),
                SuccessGreen,
                enabled = !finished
            )
        }
    }
}
