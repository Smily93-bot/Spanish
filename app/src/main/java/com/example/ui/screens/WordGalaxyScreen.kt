package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.content.GalaxyExercise
import com.example.data.content.GalaxyQuestion
import com.example.data.engagement.EngagementStore
import com.example.data.engagement.Galaxy
import com.example.data.engagement.WordCard
import com.example.data.model.HelperLanguage
import com.example.data.model.VocabWord
import com.example.data.model.normalizeAnswer
import com.example.data.model.pick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import com.example.ui.viewmodel.GalaxySession
import com.example.ui.viewmodel.GalaxySummary

/** Word Galaxy: the 5000 most-used words as 100 constellations, learned and kept with spaced repetition. */
@Composable
fun WordGalaxyScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val session by viewModel.galaxySession.collectAsStateWithLifecycle()
    val summary by viewModel.galaxySummary.collectAsStateWithLifecycle()
    if (content == null) return LoadingContent(language == HelperLanguage.ARABIC)

    val current = session
    val done = summary
    when {
        current != null -> GalaxySessionView(current, viewModel, language)
        done != null -> GalaxySummaryView(done, language) { viewModel.dismissGalaxySummary() }
        else -> GalaxyMap(viewModel, language)
    }
}

// ---------------------------------------------------------------------------------------------- Map

@Composable
private fun GalaxyMap(viewModel: BlasterViewModel, language: HelperLanguage) {
    val cards by viewModel.wordCards.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val today = EngagementStore.today()
    val due = remember(cards, today) { Galaxy.dueRanks(today, cards, limit = Int.MAX_VALUE).size }
    val learned = remember(cards) { cards.values.count { it.introduced } }
    val memorized = remember(cards) { cards.values.count { it.memorized } }
    val current = remember(cards) { Galaxy.currentConstellation(cards) }
    var opened by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(SpaceDeep, SpaceNavy, Color(0xFF2A1F6B))))
                    .padding(18.dp)
            ) {
                StarField(Modifier.matchParentSize())
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.pick("مجرة الكلمات", "WORD GALAXY"), color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        language.pick("أكثر 5000 كلمة استخدامًا", "The 5000 most-used words"),
                        color = StarWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold
                    )
                    ProgressBar(progress = learned / Galaxy.WORDS.toFloat(), color = SolarGold)
                    Text(
                        language.pick(
                            "تعلّمتِ $learned · حفظتِ $memorized · من 5000",
                            "$learned learned · $memorized memorized · of 5000"
                        ),
                        color = StarWhite.copy(alpha = 0.8f), fontSize = 12.sp
                    )
                    DailyGoalLine(streak.xpOn(today), streak.dailyGoal, streak.liveStreak(today), language)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigAction(
                    emoji = "🔁",
                    title = language.pick("مراجعة", "Review"),
                    subtitle = if (due > 0) language.pick("$due كلمة تنتظرك", "$due words waiting") else language.pick("لا شيء الآن", "Nothing due"),
                    color = NebulaPurple,
                    enabled = due > 0,
                    modifier = Modifier.weight(1f)
                ) { viewModel.startGalaxyReview() }
                BigAction(
                    emoji = "✨",
                    title = language.pick("5 كلمات جديدة", "5 new words"),
                    subtitle = Galaxy.name(current),
                    color = SolarAmber,
                    enabled = Galaxy.nextNew(current, cards).isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { viewModel.startGalaxyLesson(current) }
            }
        }

        item {
            NiloSays(
                line = if (due > 0) NiloLine("¡Repasa primero, así no olvidas!", "راجعي أولًا حتى لا تنسي!", "Review first so you don't forget!")
                else NiloLine("Cada día, cinco palabras nuevas.", "كل يوم، خمس كلمات جديدة.", "Five new words every day."),
                language = language,
                onSpeak = { viewModel.speakSpanish(if (due > 0) "¡Repasa primero, así no olvidas!" else "Cada día, cinco palabras nuevas.") },
                size = 48.dp
            )
        }

        item {
            SectionHeader(
                language.pick("الكوكبات", "CONSTELACIONES"),
                language.pick("كل كوكبة 50 كلمة. تُفتح التالية بعد 40 كلمة.", "50 words each. The next one opens after 40 words.")
            )
        }

        items((0 until Galaxy.CONSTELLATIONS).chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { c ->
                    ConstellationTile(
                        index = c,
                        introduced = Galaxy.introducedIn(c, cards),
                        memorized = Galaxy.memorizedIn(c, cards),
                        unlocked = Galaxy.isUnlocked(c, cards),
                        isCurrent = c == current,
                        modifier = Modifier.weight(1f)
                    ) { opened = c }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    opened?.let { c ->
        ConstellationDialog(c, cards, viewModel, language, onDismiss = { opened = null })
    }
}

@Composable
private fun DailyGoalLine(xp: Int, goal: Int, streak: Int, language: HelperLanguage) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (xp >= goal) "🔥" else "🕯️", fontSize = 18.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            if (xp >= goal) language.pick("أنجزتِ هدف اليوم · سلسلة $streak يوم", "Daily goal done · $streak-day streak")
            else language.pick("هدف اليوم: $xp / $goal نقطة", "Today's goal: $xp / $goal XP"),
            color = SolarGold, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StarField(modifier: Modifier) {
    val stars = remember { List(40) { Offset((it * 37 % 100) / 100f, (it * 61 % 100) / 100f) to (1f + it % 3) } }
    Canvas(modifier) {
        stars.forEach { (p, r) ->
            drawCircle(StarWhite.copy(alpha = 0.25f + 0.15f * r), radius = r * 1.2f, center = Offset(p.x * size.width, p.y * size.height))
        }
    }
}

@Composable
private fun BigAction(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) color else AdventureSurfaceVariant,
        modifier = modifier.height(96.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 24.sp)
            Text(title, color = if (enabled) Color.White else TextSecondary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            Text(subtitle, color = if (enabled) Color.White.copy(alpha = 0.85f) else TextSecondary, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ConstellationTile(
    index: Int,
    introduced: Int,
    memorized: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val range = Galaxy.ranksOf(index)
    val complete = memorized == Galaxy.PER_CONSTELLATION
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (unlocked) SpaceNavy else AdventureSurfaceVariant,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, if (isCurrent) SolarGold else AdventureCardBorder),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
                ProgressRing(
                    learned = introduced / Galaxy.PER_CONSTELLATION.toFloat(),
                    memorized = memorized / Galaxy.PER_CONSTELLATION.toFloat(),
                    dim = !unlocked
                )
                if (unlocked) {
                    Text(if (complete) "🌟" else "★", color = SolarGold, fontSize = if (complete) 20.sp else 22.sp)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                Galaxy.name(index),
                color = if (unlocked) StarWhite else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                "${range.first}–${range.last}",
                color = if (unlocked) StarWhite.copy(alpha = 0.6f) else TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ProgressRing(learned: Float, memorized: Float, dim: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val stroke = 5.dp.toPx()
        drawCircle(if (dim) TextSecondary.copy(alpha = 0.25f) else StarWhite.copy(alpha = 0.15f), style = Stroke(stroke))
        drawArc(DiamondCyan, -90f, 360f * learned, false, style = Stroke(stroke, cap = StrokeCap.Round))
        drawArc(SolarGold, -90f, 360f * memorized, false, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun ConstellationDialog(
    index: Int,
    cards: Map<Int, WordCard>,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onDismiss: () -> Unit
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val galaxy = content?.galaxy ?: return
    val unlocked = Galaxy.isUnlocked(index, cards)
    val canLearn = unlocked && Galaxy.nextNew(index, cards).isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdventureSurface,
        title = {
            Column {
                Text("✦ ${Galaxy.name(index)}", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(
                    language.pick(
                        "الكلمات ${Galaxy.ranksOf(index).first}–${Galaxy.ranksOf(index).last} · تعلّمتِ ${Galaxy.introducedIn(index, cards)}",
                        "Words ${Galaxy.ranksOf(index).first}–${Galaxy.ranksOf(index).last} · ${Galaxy.introducedIn(index, cards)} learned"
                    ),
                    fontSize = 12.sp, color = TextSecondary
                )
            }
        },
        text = {
            if (!unlocked) {
                Text(
                    language.pick(
                        "🔒 تعلّمي 40 كلمة من الكوكبة السابقة لفتح هذه.",
                        "🔒 Learn 40 words of the previous constellation to open this one."
                    ),
                    color = TextPrimary
                )
            } else {
                Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                    Galaxy.ranksOf(index).forEach { rank ->
                        val w = galaxy.word(rank) ?: return@forEach
                        val card = cards[rank]
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = card?.introduced == true) { viewModel.speakSpanish(w.shortSpanish) }
                                .padding(vertical = 5.dp)
                        ) {
                            Text("$rank", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                            if (card?.introduced == true) {
                                Column(Modifier.weight(1f)) {
                                    Text(w.shortSpanish, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(w.meaning(language), color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                }
                                BoxStars(card.box)
                            } else {
                                Text("• • •", color = TextSecondary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (canLearn) {
                Button(
                    onClick = { onDismiss(); viewModel.startGalaxyLesson(index) },
                    colors = ButtonDefaults.buttonColors(containerColor = SolarAmber)
                ) { Text(language.pick("تعلّمي 5 كلمات", "Learn 5 words"), fontWeight = FontWeight.Bold) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("إغلاق", "Close")) } }
    )
}

/** Memory strength: one star per Leitner box reached (5 = memorized). */
@Composable
private fun BoxStars(box: Int) {
    Row {
        repeat(WordCard.MEMORIZED_BOX) { i ->
            Text(if (i < box) "★" else "☆", color = if (i < box) SolarGold else AdventureCardBorder, fontSize = 13.sp)
        }
    }
}

// ---------------------------------------------------------------------------------------------- Session

@Composable
private fun GalaxySessionView(session: GalaxySession, viewModel: BlasterViewModel, language: HelperLanguage) {
    var introIndex by remember(session) { mutableIntStateOf(0) }
    val queue = remember(session) { mutableStateListOf<GalaxyQuestion>().apply { addAll(session.questions) } }
    var position by remember(session) { mutableIntStateOf(0) }
    val firstTry = remember(session) { mutableStateMapOf<Int, Boolean>() }
    val retries = remember(session) { mutableStateMapOf<Int, Int>() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.quitGalaxySession() }) {
                Icon(Icons.Default.Close, contentDescription = language.pick("خروج", "Quit"), tint = TextSecondary)
            }
            val total = session.intro.size + queue.size
            val done = (if (introIndex < session.intro.size) introIndex else session.intro.size + position)
            val animated by animateFloatAsState(done / total.coerceAtLeast(1).toFloat(), label = "progress")
            ProgressBar(progress = animated, color = SuccessGreen, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            Text(Galaxy.name(session.constellation), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))

        if (introIndex < session.intro.size) {
            IntroCard(
                word = session.intro[introIndex],
                number = introIndex + 1,
                total = session.intro.size,
                language = language,
                viewModel = viewModel
            ) { introIndex++ }
        } else if (position < queue.size) {
            val question = queue[position]
            QuestionView(question, language, viewModel, key = position) { correct ->
                val rank = question.word.rank
                if (rank !in firstTry) firstTry[rank] = correct
                if (!correct && (retries[rank] ?: 0) < 2) {
                    retries[rank] = (retries[rank] ?: 0) + 1
                    viewModel.galaxyRetry(question)?.let { queue.add(it) }
                }
                position++
                if (position >= queue.size) viewModel.finishGalaxySession(firstTry.toMap())
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SolarAmber) }
        }
    }
}

@Composable
private fun IntroCard(
    word: VocabWord,
    number: Int,
    total: Int,
    language: HelperLanguage,
    viewModel: BlasterViewModel,
    onNext: () -> Unit
) {
    LaunchedEffect(word.rank) { viewModel.speakSpanish(word.shortSpanish) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            language.pick("كلمة جديدة $number من $total", "New word $number of $total"),
            color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(SpaceNavy, Color(0xFF15367A))))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("#${word.rank} · ${word.level}", color = DiamondCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(word.shortSpanish, color = StarWhite, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                if (word.partOfSpeech.isNotBlank()) {
                    Text(partLabel(word.partOfSpeech, language), color = StarWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(word.meaning(language), color = SolarGold, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                AudioButton(onClick = { viewModel.speakSpanish(word.shortSpanish) }, size = 52.dp, tint = DiamondCyan)
            }
        }
        if (word.exampleEs.isNotBlank()) {
            AdventureCard(borderColor = NebulaPurple.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(word.exampleEs, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(language.pick(word.exampleAr, word.exampleEn), color = TextSecondary, fontSize = 13.sp)
                    }
                    AudioButton(onClick = { viewModel.speakSpanish(word.exampleEs) }, size = 36.dp, tint = NebulaPurple)
                }
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = ExplorerBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Text(
                if (number == total) language.pick("ابدئي التحدي ☄️", "Start the challenge ☄️") else language.pick("التالي", "Next"),
                fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
            )
        }
    }
}

private fun partLabel(part: String, language: HelperLanguage): String = when (part) {
    "noun" -> language.pick("اسم", "noun")
    "verb" -> language.pick("فعل", "verb")
    "adjective" -> language.pick("صفة", "adjective")
    "adverb" -> language.pick("ظرف", "adverb")
    "pronoun" -> language.pick("ضمير", "pronoun")
    "preposition" -> language.pick("حرف جر", "preposition")
    "conjunction" -> language.pick("حرف عطف", "conjunction")
    "article" -> language.pick("أداة تعريف", "article")
    "determiner", "demonstrative" -> language.pick("اسم إشارة/محدِّد", part)
    "interjection" -> language.pick("تعجّب", "interjection")
    "number", "numeral" -> language.pick("عدد", "number")
    else -> part
}

@Composable
private fun QuestionView(
    question: GalaxyQuestion,
    language: HelperLanguage,
    viewModel: BlasterViewModel,
    key: Int,
    onDone: (Boolean) -> Unit
) {
    var picked by remember(key) { mutableStateOf<String?>(null) }
    val spelled = remember(key) { mutableStateListOf<Int>() }
    val word = question.word
    val answered = picked != null
    val correct = picked?.let { normalizeAnswer(it) == normalizeAnswer(question.answer) } ?: false

    LaunchedEffect(key) {
        if (question.type == GalaxyExercise.LISTEN || question.type == GalaxyExercise.MEANING) viewModel.speakSpanish(word.shortSpanish)
    }

    fun answer(choice: String) {
        if (picked != null) return
        picked = choice
        val ok = normalizeAnswer(choice) == normalizeAnswer(question.answer)
        viewModel.galaxyAnswered(ok)
        if (ok) viewModel.speakSpanish(word.shortSpanish)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when (question.type) {
                GalaxyExercise.MEANING -> language.pick("ما معنى هذه الكلمة؟", "What does this word mean?")
                GalaxyExercise.REVERSE -> language.pick("كيف نقول هذا بالإسبانية؟", "How do you say this in Spanish?")
                GalaxyExercise.LISTEN -> language.pick("استمعي واختاري الكلمة", "Listen and pick the word")
                GalaxyExercise.SPELL -> language.pick("اكتبي الكلمة بالحروف", "Spell the word")
            },
            color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold
        )

        // Prompt
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(SpaceNavy)
                .padding(vertical = 22.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (question.type) {
                GalaxyExercise.MEANING -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.shortSpanish, color = StarWhite, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(10.dp))
                    AudioButton(onClick = { viewModel.speakSpanish(word.shortSpanish) }, size = 40.dp, tint = DiamondCyan)
                }
                GalaxyExercise.REVERSE, GalaxyExercise.SPELL -> Text(
                    word.meaning(language), color = SolarGold, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
                )
                GalaxyExercise.LISTEN -> Surface(
                    onClick = { viewModel.speakSpanish(word.shortSpanish) },
                    shape = CircleShape,
                    color = DiamondCyan,
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = SpaceNavy, modifier = Modifier.size(44.dp))
                    }
                }
            }
        }

        if (question.type == GalaxyExercise.SPELL) {
            SpellBoard(question, spelled, enabled = !answered, onComplete = { answer(it) })
        } else {
            question.options.forEach { option ->
                val isAnswer = normalizeAnswer(option) == normalizeAnswer(question.answer)
                val color = when {
                    !answered -> AdventureSurface
                    isAnswer -> SuccessGreen.copy(alpha = 0.18f)
                    option == picked -> MeteorRed.copy(alpha = 0.18f)
                    else -> AdventureSurface
                }
                val border = when {
                    answered && isAnswer -> SuccessGreen
                    answered && option == picked -> MeteorRed
                    else -> AdventureCardBorder
                }
                Surface(
                    onClick = { answer(option) },
                    enabled = !answered,
                    shape = RoundedCornerShape(16.dp),
                    color = color,
                    border = BorderStroke(2.dp, border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        option,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)
                    )
                }
            }
        }

        if (answered) {
            Feedback(correct, word, language, key, viewModel) { onDone(correct) }
        }
    }
}

@Composable
private fun SpellBoard(
    question: GalaxyQuestion,
    spelled: MutableList<Int>,
    enabled: Boolean,
    onComplete: (String) -> Unit
) {
    val target = question.answer
    val typed = spelled.joinToString("") { question.options[it] }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        target.forEachIndexed { i, _ ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 28.dp, height = 38.dp)
                    .border(BorderStroke(2.dp, if (i < typed.length) ExplorerBlue else AdventureCardBorder), RoundedCornerShape(8.dp))
            ) {
                Text(typed.getOrNull(i)?.toString() ?: "", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        question.options.forEachIndexed { index, letter ->
            val used = index in spelled
            Surface(
                onClick = {
                    spelled.add(index)
                    if (spelled.size == target.length) onComplete(spelled.joinToString("") { question.options[it] })
                },
                enabled = enabled && !used,
                shape = RoundedCornerShape(12.dp),
                color = if (used) AdventureSurfaceVariant else SolarGold,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (used) "" else letter, color = SpaceNavy, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Surface(
            onClick = { if (spelled.isNotEmpty()) spelled.removeAt(spelled.lastIndex) },
            enabled = enabled && spelled.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            color = AdventureSurfaceVariant,
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Backspace, contentDescription = null, tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun Feedback(
    correct: Boolean,
    word: VocabWord,
    language: HelperLanguage,
    key: Int,
    viewModel: BlasterViewModel,
    onContinue: () -> Unit
) {
    val praise = remember(key) { NiloLines.praise.random() }
    val miss = NiloLine("¡Casi! Lo repetimos luego.", "كدتِ! سنعيدها بعد قليل.", "Almost! We'll try it again soon.")
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (correct) SuccessGreen.copy(alpha = 0.12f) else MeteorRed.copy(alpha = 0.10f),
        border = BorderStroke(1.5.dp, if (correct) SuccessGreen else MeteorRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NiloSays(
                line = if (correct) praise else miss,
                language = language,
                onSpeak = { viewModel.speakSpanish(if (correct) praise.es else miss.es) },
                size = 44.dp
            )
            Text(
                "${word.shortSpanish} = ${word.meaning(language)}",
                color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp
            )
            if (word.exampleEs.isNotBlank()) {
                Text(word.exampleEs, color = TextPrimary, fontSize = 13.sp)
                Text(language.pick(word.exampleAr, word.exampleEn), color = TextSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = if (correct) SuccessGreen else MeteorRed),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) { Text(language.pick("متابعة", "Continue"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        }
    }
}

// ---------------------------------------------------------------------------------------------- Summary

@Composable
private fun GalaxySummaryView(summary: GalaxySummary, language: HelperLanguage, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(20.dp))
        Text("🌟", fontSize = 64.sp)
        Text(
            if (summary.isLesson) language.pick("كلمات جديدة في مجرتك!", "New stars in your galaxy!")
            else language.pick("أنهيتِ المراجعة!", "Review complete!"),
            color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudStatCard(language.pick("كلمات", "Words"), "${summary.wordsPracticed}", "📚", Modifier.weight(1f))
            HudStatCard(language.pick("صحيحة", "Correct"), "${summary.firstTryCorrect}", "🎯", Modifier.weight(1f), SuccessGreen)
            HudStatCard("XP", "+${summary.xp}", "⚡", Modifier.weight(1f), SolarAmber)
        }
        if (summary.newlyMemorized > 0) {
            Text(
                language.pick("🧠 حفظتِ ${summary.newlyMemorized} كلمة بشكل دائم!", "🧠 ${summary.newlyMemorized} words now memorized!"),
                color = NebulaPurple, fontWeight = FontWeight.Bold
            )
        }
        Text(
            language.pick(
                "ستعود الكلمات للمراجعة في الوقت المناسب قبل أن تنسيها.",
                "Words come back for review right before you would forget them."
            ),
            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = ExplorerBlue),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text(language.pick("العودة إلى المجرة", "Back to the galaxy"), fontWeight = FontWeight.ExtraBold) }
    }
}
