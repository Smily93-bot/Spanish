package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GrammarExample
import com.example.data.model.GrammarPattern
import com.example.data.model.GrammarQuestion
import com.example.data.model.GrammarTopic
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.AudioButton
import com.example.ui.components.bidiSafe
import com.example.ui.components.LoadingContent
import com.example.ui.components.ProgressBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import com.example.ui.viewmodel.GrammarResult
import com.example.ui.viewmodel.GrammarRound

/**
 * Grammar ¿Por qué?: Nilo the detective shows one rule on a card, then six quick questions.
 * After every answer the full sentence is shown with the reason it is right.
 */
@Composable
fun GrammarLabScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val round by viewModel.grammarRound.collectAsStateWithLifecycle()
    val result by viewModel.grammarResult.collectAsStateWithLifecycle()
    val data = content ?: return LoadingContent(language == HelperLanguage.ARABIC)

    val r = round
    val done = result
    when {
        r != null -> key(r) { RoundView(r, viewModel, language) }
        done != null -> ResultView(done, viewModel, language)
        else -> TopicMap(data.grammarTopics, viewModel, language)
    }
}

private fun levelLabel(level: String, language: HelperLanguage) = when (level.take(2)) {
    "A1" -> language.pick("A1 · مبتدئ", "A1 · Beginner")
    "A2" -> language.pick("A2 · أساسي", "A2 · Elementary")
    "B1" -> language.pick("B1 · متوسط", "B1 · Intermediate")
    "B2" -> language.pick("B2 · فوق المتوسط", "B2 · Upper intermediate")
    "C1" -> language.pick("C1 · متقدم", "C1 · Advanced")
    else -> language.pick("C2 · إتقان", "C2 · Mastery")
}

// ---------------------------------------------------------------------------------------------- Map

@Composable
private fun TopicMap(topics: List<GrammarTopic>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val stars by viewModel.grammarStars.collectAsStateWithLifecycle()
    val next = remember(topics, stars) { viewModel.nextGrammarTopic() }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(SpaceNavy, Color(0xFF2A1F6B))))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tl("🕵️ ¿Por qué?"), color = SolarGold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    language.pick(
                        "نيلو المحقق يشرح لكِ سبب كل إجابة: قاعدة واحدة، ثم 6 أسئلة سريعة.",
                        "Detective Nilo shows you the reason behind every answer: one rule, then 6 quick questions."
                    ),
                    color = StarWhite, fontSize = 14.sp
                )
                Text(
                    language.pick("⭐ ${stars.values.sum()} نجمة من ${topics.size * 3}", "⭐ ${stars.values.sum()} of ${topics.size * 3} stars"),
                    color = StarWhite.copy(alpha = 0.75f), fontSize = 12.sp
                )
            }
        }
        if (next != null) {
            Button(
                onClick = { viewModel.startGrammarRound(next) },
                colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(language.pick("ابدئي ▶", "START ▶"), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("${next.titleEs} · ${next.title(language)}", fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        topics.groupBy { it.level.take(2) }.forEach { (level, group) ->
            Text(levelLabel(level, language), color = TextSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            group.forEach { topic ->
                val s = stars[topic.id] ?: 0
                Surface(
                    onClick = { viewModel.startGrammarRound(topic) },
                    shape = RoundedCornerShape(16.dp),
                    color = AdventureSurface,
                    border = BorderStroke(if (topic == next) 2.dp else 1.dp, if (topic == next) SolarAmber else AdventureCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(topic.titleEs, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1)
                            Text(topic.title(language), color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                        }
                        Text("★".repeat(s) + "☆".repeat(3 - s), color = if (s > 0) SolarGold else AdventureCardBorder, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Round

/** A grammar round in small bites: a short start card, then one piece of the rule, two questions, the next piece… */
private sealed interface LabStep {
    data object Start : LabStep
    data class Piece(val pattern: GrammarPattern?, val example: GrammarExample?, val tip: String?) : LabStep
    data class Ask(val q: Int) : LabStep
}

private fun labSteps(topic: GrammarTopic, questions: Int, language: HelperLanguage): List<LabStep> = buildList {
    add(LabStep.Start)
    val patterns = topic.patterns.filter { it.formula.isNotBlank() }
    val pieces = buildList {
        patterns.forEachIndexed { i, p -> add(LabStep.Piece(p, topic.examples.getOrNull(i), null)) }
        if (topic.tip(language).isNotBlank()) add(LabStep.Piece(null, topic.examples.getOrNull(patterns.size), topic.tip(language)))
        if (isEmpty()) add(LabStep.Piece(null, topic.examples.firstOrNull(), null))
    }
    // Spread the questions after the pieces: every piece is followed by its share of questions.
    var q = 0
    pieces.forEachIndexed { i, piece ->
        add(piece)
        val until = if (i == pieces.lastIndex) questions else minOf(questions, (questions * (i + 1)) / pieces.size)
        while (q < until) add(LabStep.Ask(q++))
    }
}

@Composable
private fun RoundView(round: GrammarRound, viewModel: BlasterViewModel, language: HelperLanguage) {
    val steps = remember(round) { labSteps(round.topic, round.questions.size, language) }
    var index by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    val total = round.questions.size

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.quitGrammarRound() }) {
                Icon(Icons.Default.Close, contentDescription = language.pick("خروج", "Quit"), tint = TextSecondary)
            }
            val progress by animateFloatAsState(index / steps.size.toFloat(), label = "progress")
            ProgressBar(progress = progress, color = NebulaPurple, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(if (combo >= 2) "🔥 x$combo" else "🕵️", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(10.dp))
        key(index) {
            when (val step = steps[index]) {
                LabStep.Start -> StartCard(round.topic, viewModel, language) { index++ }
                is LabStep.Piece -> PieceCard(round.topic, step, viewModel, language) { index++ }
                is LabStep.Ask -> QuestionCard(round.questions[step.q], step.q, total, viewModel, language) { ok ->
                    if (ok) { correct++; combo++ } else combo = 0
                    if (index + 1 < steps.size) index++ else viewModel.finishGrammarRound(correct)
                }
            }
        }
    }
}

/** Just the name of the rule and one line about it, then START. */
@Composable
private fun StartCard(topic: GrammarTopic, viewModel: BlasterViewModel, language: HelperLanguage, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(SpaceNavy, Color(0xFF2A1F6B))))
                .padding(22.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🕵️ " + topic.level, color = DiamondCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(topic.titleEs, color = StarWhite, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    AudioButton(onClick = { viewModel.speakSpanish(topic.titleEs) }, size = 36.dp)
                }
                Text(topic.title(language), color = SolarGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    language.pick("خطوات صغيرة: جزء من القاعدة، ثم سؤالان عليه.", "Small steps: one piece of the rule, then two questions on it."),
                    color = StarWhite.copy(alpha = 0.85f), fontSize = 14.sp
                )
            }
        }
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) { Text(language.pick("ابدأ ▶", "START ▶"), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp) }
    }
}

/** One small piece of the rule: a pattern (or the tip) with one example to hear. */
@Composable
private fun PieceCard(topic: GrammarTopic, piece: LabStep.Piece, viewModel: BlasterViewModel, language: HelperLanguage, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            if (piece.tip != null) language.pick("🧠 تذكّر", "🧠 Remember") else language.pick("📘 القاعدة", "📘 The rule"),
            color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp
        )
        piece.pattern?.let { p ->
            Surface(shape = RoundedCornerShape(20.dp), color = SpaceNavy, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p.label.isNotBlank()) Text(p.label, color = DiamondCyan, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Text(p.formula, color = StarWhite, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 28.sp)
                    if (p.note(language).isNotBlank()) {
                        Text(if (language == HelperLanguage.ARABIC) bidiSafe(p.note(language)) else p.note(language), color = SolarGold, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }
        }
        piece.tip?.let {
            Surface(shape = RoundedCornerShape(20.dp), color = SolarGold.copy(alpha = 0.18f), modifier = Modifier.fillMaxWidth()) {
                Text(if (language == HelperLanguage.ARABIC) bidiSafe(it) else it, color = TextPrimary, fontSize = 17.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(18.dp))
            }
        }
        if (piece.pattern == null && piece.tip == null && topic.intro(language).isNotBlank()) {
            Text(if (language == HelperLanguage.ARABIC) bidiSafe(topic.intro(language)) else topic.intro(language), color = TextPrimary, fontSize = 17.sp, lineHeight = 26.sp)
        }
        piece.example?.let { e ->
            Text(language.pick("💬 مثال", "💬 Example"), color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(e.spanish, color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    if (e.translation(language).isNotBlank()) Text(e.translation(language), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
                }
                AudioButton(onClick = { viewModel.speakSpanish(e.spanish) }, size = 40.dp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = NebulaPurple),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text(language.pick("فهمت! جرّب ▶", "Got it! Try it ▶"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
    }
}

/** The sentence with the gap(s) shown as a blank, or filled with [fill] and highlighted. */
private fun sentence(q: GrammarQuestion, fill: String?, color: Color): AnnotatedString = buildAnnotatedString {
    val parts = fill?.split(" / ")
    var last = 0
    var i = 0
    GrammarQuestion.GAP.findAll(q.question).forEach { m ->
        append(q.question.substring(last, m.range.first))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.ExtraBold, background = color.copy(alpha = 0.12f))) {
            append(if (parts == null) " ____ " else " ${parts.getOrElse(i) { parts.last() }} ")
        }
        i++
        last = m.range.last + 1
    }
    append(q.question.substring(last))
}

@Composable
private fun QuestionCard(
    q: GrammarQuestion,
    index: Int,
    total: Int,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onNext: (Boolean) -> Unit
) {
    var picked by remember { mutableStateOf<Int?>(null) }
    val answered = picked != null
    val ok = picked == q.answer

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            language.pick("السؤال ${index + 1} من $total · أكملي الجملة", "Question ${index + 1} of $total · Complete the sentence"),
            color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
        Surface(shape = RoundedCornerShape(20.dp), color = AdventureSurface, border = BorderStroke(1.5.dp, AdventureCardBorder), modifier = Modifier.fillMaxWidth()) {
            Text(
                sentence(q, if (answered) q.correct else null, if (answered) SuccessGreen else NebulaPurple),
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp).fillMaxWidth()
            )
        }
        q.options.forEachIndexed { i, option ->
            val border = when {
                answered && i == q.answer -> SuccessGreen
                answered && i == picked -> MeteorRed
                else -> AdventureCardBorder
            }
            Surface(
                onClick = {
                    if (picked == null) {
                        picked = i
                        viewModel.grammarAnswered(i == q.answer)
                        viewModel.speakSpanish(q.filled())
                    }
                },
                enabled = !answered,
                shape = RoundedCornerShape(16.dp),
                color = when {
                    answered && i == q.answer -> SuccessGreen.copy(alpha = 0.15f)
                    answered && i == picked -> MeteorRed.copy(alpha = 0.12f)
                    else -> AdventureSurface
                },
                border = BorderStroke(2.dp, border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(option, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(14.dp))
            }
        }
        if (answered) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (ok) SuccessGreen.copy(alpha = 0.10f) else MeteorRed.copy(alpha = 0.08f),
                border = BorderStroke(1.5.dp, if (ok) SuccessGreen else MeteorRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (ok) language.pick("✅ صحيح!", "✅ Correct!") else language.pick("❌ ليس تمامًا", "❌ Not quite"),
                        color = if (ok) SuccessGreen else MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(q.filled(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (q.translation(language).isNotBlank()) Text(q.translation(language), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                        }
                        AudioButton(onClick = { viewModel.speakSpanish(q.filled()) }, size = 34.dp)
                    }
                    if (q.why(language).isNotBlank()) {
                        Surface(shape = RoundedCornerShape(12.dp), color = NebulaPurple.copy(alpha = 0.10f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(language.pick("🕵️ لماذا؟", "🕵️ Why?"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                Text(q.why(language), color = TextPrimary, fontSize = 15.sp)
                            }
                        }
                    }
                    Button(
                        onClick = { onNext(ok) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (ok) SuccessGreen else NebulaPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            if (index + 1 < total) language.pick("التالي", "Next") else language.pick("النتيجة", "See result"),
                            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Result

@Composable
private fun ResultView(result: GrammarResult, viewModel: BlasterViewModel, language: HelperLanguage) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text("★".repeat(result.stars) + "☆".repeat(3 - result.stars), color = SolarGold, fontSize = 52.sp)
        Text(result.topic.titleEs, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(
            language.pick("${result.correct} من ${result.total} صحيحة · +${result.xp} نقطة", "${result.correct} of ${result.total} correct · +${result.xp} XP"),
            color = TextSecondary, fontSize = 15.sp
        )
        NiloSays(
            line = when (result.stars) {
                3 -> NiloLine(tl("¡Caso resuelto! Eres una detective."), "حُلّت القضية! أنتِ محققة بارعة.", "Case solved! You're a real detective.")
                2 -> NiloLine(tl("¡Muy bien! Casi perfecto."), "أحسنتِ! شبه مثالي.", "Very good! Almost perfect.")
                else -> NiloLine(tl("Repasamos la regla y lo intentamos otra vez."), "لنراجع القاعدة ونحاول مرة أخرى.", "Let's review the rule and try again.")
            },
            language = language,
            onSpeak = { viewModel.speakSpanish(if (result.stars == 3) tl("¡Caso resuelto!") else tl("¡Muy bien!")) },
            size = 52.dp
        )
        if (result.newBest && result.stars > 0) {
            Text(language.pick("🏅 أفضل نتيجة جديدة!", "🏅 New best!"), color = NebulaPurple, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { viewModel.dismissGrammarResult() },
            colors = ButtonDefaults.buttonColors(containerColor = SolarAmber),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text(language.pick("متابعة", "Continue"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        if (result.stars < 3) {
            OutlinedButton(
                onClick = { viewModel.startGrammarRound(result.topic) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(language.pick("أعيدي المحاولة", "Try again"), fontWeight = FontWeight.Bold) }
        }
    }
}
