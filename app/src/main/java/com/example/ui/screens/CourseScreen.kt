package com.example.ui.screens

import com.example.flavor.tl
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.content.CrossEntry
import com.example.data.content.SpanishContent
import com.example.data.content.buildCrossword
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

/** The beginner course: a path of units, each one taught with pictures and played as small games. */
@Composable
fun CourseScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val stars by viewModel.courseStars.collectAsStateWithLifecycle()
    val data = content ?: return LoadingContent(language == HelperLanguage.ARABIC)
    var openUnit by rememberSaveable { mutableStateOf<String?>(null) }
    val requested by viewModel.openCourseUnit.collectAsStateWithLifecycle()
    LaunchedEffect(requested) {
        requested?.let { openUnit = it; viewModel.consumeOpenCourseUnit() }
    }
    val unit = data.course.firstOrNull { it.id == openUnit }
    BackHandler(enabled = unit != null) { openUnit = null }
    if (unit == null) {
        CoursePath(data.course, stars, language) { openUnit = it }
    } else {
        key(unit.id) {
            UnitLesson(unit, data.course.indexOf(unit) + 1, viewModel, language, onExit = { openUnit = null })
        }
    }
}

/** Index of the first unit not finished yet (the one to play next). */
fun nextCourseUnit(course: List<CourseUnit>, stars: Map<String, Int>): Int =
    course.indexOfFirst { (stars[it.id] ?: 0) == 0 }.let { if (it < 0) course.lastIndex else it }

@Composable
private fun CoursePath(course: List<CourseUnit>, stars: Map<String, Int>, language: HelperLanguage, onOpen: (String) -> Unit) {
    val next = nextCourseUnit(course, stars)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(language.pick("📚 الدورة: من الصفر", "📚 Course: from zero"), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            language.pick(
                "كل وحدة تعلّمك ٨ كلمات بالصور والصوت، ثم تلعبين بها. بعدها تصبح مغامرة ليا أسهل!",
                "Each unit teaches 8 words with pictures and sound, then you play with them. After that, Lía's adventure gets easier!"
            ),
            color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp
        )
        if (course.isEmpty()) {
            Text(language.pick("لا توجد وحدات بعد.", "No units yet."), color = TextSecondary)
        }
        course.forEachIndexed { i, unit ->
            val got = stars[unit.id] ?: 0
            val unlocked = i <= next
            // Zig-zag path, like a board game.
            val shift = if (i % 2 == 0) 0.dp else 40.dp
            Row(Modifier.fillMaxWidth().padding(start = shift, end = 40.dp - shift)) {
                Surface(
                    onClick = { if (unlocked) onOpen(unit.id) },
                    enabled = unlocked,
                    shape = RoundedCornerShape(22.dp),
                    color = if (i == next) SolarGold.copy(alpha = 0.25f) else AdventureSurface,
                    border = BorderStroke(2.dp, if (i == next) SolarAmber else if (got > 0) SuccessGreen else AdventureCardBorder),
                    modifier = Modifier.fillMaxWidth().alpha(if (unlocked) 1f else 0.5f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(54.dp).clip(CircleShape).background(if (got > 0) SuccessGreen.copy(alpha = 0.2f) else ExplorerBlue.copy(alpha = 0.12f))
                        ) { Text(if (unlocked) unit.emoji else "🔒", fontSize = 28.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(language.pick("المستوى ${i + 1}", "Level ${i + 1}"), color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(unit.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text(unit.helperTitle(language), color = TextSecondary, fontSize = 13.sp)
                        }
                        Text(
                            when {
                                got > 0 -> "⭐".repeat(got)
                                i == next -> "▶"
                                else -> ""
                            },
                            color = SolarAmber, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// --------------------------------------------------------------------------- One unit

private sealed interface UnitStep {
    data class Learn(val word: CourseWord) : UnitStep
    data object Phrases : UnitStep
    data object Tip : UnitStep
    data class Match(val words: List<CourseWord>) : UnitStep
    data class Listen(val word: CourseWord, val options: List<CourseWord>) : UnitStep
    data object Cross : UnitStep
    data object Done : UnitStep
}

private fun unitSteps(unit: CourseUnit, random: Random): List<UnitStep> = buildList {
    // First see and hear every word, then the phrases and the tip, then play.
    unit.words.forEach { add(UnitStep.Learn(it)) }
    if (unit.phrases.isNotEmpty()) add(UnitStep.Phrases)
    add(UnitStep.Tip)
    // Four words (picked at random) are matched to their pictures, the other four are heard and
    // picked, and then all eight come back in the crossword.
    val mixed = unit.words.shuffled(random)
    add(UnitStep.Match(mixed.take(4)))
    mixed.drop(4).forEach { w ->
        add(UnitStep.Listen(w, (unit.words.filter { it != w }.shuffled(random).take(3) + w).shuffled(random)))
    }
    add(UnitStep.Cross)
    add(UnitStep.Done)
}

@Composable
private fun UnitLesson(unit: CourseUnit, number: Int, viewModel: BlasterViewModel, language: HelperLanguage, onExit: () -> Unit) {
    val steps = remember(unit.id) { unitSteps(unit, Random) }
    var index by rememberSaveable(unit.id) { mutableIntStateOf(0) }
    var mistakes by rememberSaveable(unit.id) { mutableIntStateOf(0) }
    var stepDone by remember(index) { mutableStateOf(false) }
    val step = steps[index]
    val canGoOn = when (step) {
        is UnitStep.Match, is UnitStep.Listen, UnitStep.Cross -> stepDone
        else -> true
    }
    val stars = when {
        mistakes <= 2 -> 3
        mistakes <= 6 -> 2
        else -> 1
    }
    LaunchedEffect(step) { if (step is UnitStep.Done) viewModel.finishCourseUnit(unit.id, stars) }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) { Text("✕", color = TextSecondary, fontSize = 18.sp) }
            Text("${unit.emoji} ${language.pick("المستوى", "Level")} $number · ${unit.title}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
        }
        ProgressBar(index / (steps.size - 1).toFloat(), color = SolarAmber, height = 8.dp)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(index) {
                val onMistake: () -> Unit = { mistakes++ }
                val done = { stepDone = true }
                when (step) {
                    is UnitStep.Learn -> LearnCard(step.word, viewModel, language)
                    UnitStep.Phrases -> PhrasesCard(unit, viewModel, language)
                    UnitStep.Tip -> TipCard(unit, language)
                    is UnitStep.Match -> MatchPairs(step.words, viewModel, language, onMistake, done)
                    is UnitStep.Listen -> ListenPick(step.word, step.options, viewModel, language, onMistake, done)
                    UnitStep.Cross -> CrosswordGame(unit.words, viewModel, language, onMistake, done)
                    UnitStep.Done -> UnitDone(unit, stars, language)
                }
            }
        }
        BlasterCyberButton(
            text = if (step is UnitStep.Done) language.pick("🗺️ العودة إلى الطريق", "🗺️ Back to the path") else language.pick("التالي ▶", tl("Siguiente ▶")),
            onClick = { if (step is UnitStep.Done) onExit() else index++ },
            enabled = canGoOn,
            color = if (step is UnitStep.Done) SuccessGreen else ExplorerBlue,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
    }
}

/** A new word: big picture, the word, its sound and meaning. */
@Composable
private fun LearnCard(word: CourseWord, viewModel: BlasterViewModel, language: HelperLanguage) {
    LaunchedEffect(word) { viewModel.speakSpanish(word.word) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(language.pick("🆕 كلمة جديدة", "🆕 New word"), color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Text(word.emoji, fontSize = 96.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word.word, color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.width(10.dp))
            AudioButton(onClick = { viewModel.speakSpanish(word.word) }, size = 44.dp)
        }
        Text(word.meaning(language), color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PhrasesCard(unit: CourseUnit, viewModel: BlasterViewModel, language: HelperLanguage) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(language.pick("💬 جمل قصيرة — اضغطي لتسمعي", "💬 Short sentences — tap to listen"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        unit.phrases.forEach { p ->
            AdventureCard(borderColor = NebulaPurple, onClick = { viewModel.speakSpanish(p.text) }) {
                Text("🔊 " + p.text, color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text(p.meaning(language), color = TextSecondary, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun TipCard(unit: CourseUnit, language: HelperLanguage) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        AdventureCard(borderColor = SolarGold) {
            Text(language.pick("💡 قاعدة صغيرة", "💡 A small rule"), color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Spacer(Modifier.height(6.dp))
            Text(if (language == HelperLanguage.ARABIC) bidiSafe(unit.tip(language)) else unit.tip(language), color = TextPrimary, fontSize = 17.sp, lineHeight = 26.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(language.pick("والآن… لنلعب! 🎮", "Now… let's play! 🎮"), color = ExplorerBlue, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun UnitDone(unit: CourseUnit, stars: Int, language: HelperLanguage) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("⭐".repeat(stars), fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(language.pick("أنهيتِ الوحدة!", "Unit complete!"), color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp)
        Text(unit.words.joinToString("  ") { it.emoji }, fontSize = 26.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
        Text(
            language.pick("تعرفين الآن ${unit.words.size} كلمات جديدة.", "You now know ${unit.words.size} new words."),
            color = TextSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// --------------------------------------------------------------------------- Games

/** Matching pairs: tap a word, then its picture (or the other way round). */
@Composable
fun MatchPairs(words: List<CourseWord>, viewModel: BlasterViewModel, language: HelperLanguage, onMistake: () -> Unit, onDone: () -> Unit) {
    val pictures = remember(words) { words.shuffled() }
    var pickedWord by remember(words) { mutableStateOf<CourseWord?>(null) }
    var pickedPicture by remember(words) { mutableStateOf<CourseWord?>(null) }
    val matched = remember(words) { mutableStateListOf<CourseWord>() }
    var wrongFlash by remember(words) { mutableStateOf(false) }

    LaunchedEffect(pickedWord, pickedPicture) {
        val w = pickedWord
        val p = pickedPicture
        if (w != null && p != null) {
            if (w == p) {
                matched += w
                viewModel.soundEngine.hit()
                if (matched.size == words.size) onDone()
            } else {
                wrongFlash = true
                viewModel.soundEngine.error()
                onMistake()
                delay(450)
                wrongFlash = false
            }
            pickedWord = null
            pickedPicture = null
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(language.pick("🔗 صِلي كل كلمة بصورتها", "🔗 Match each word to its picture"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                words.forEach { w ->
                    val done = w in matched
                    val selected = pickedWord == w
                    Surface(
                        onClick = {
                            pickedWord = w
                            viewModel.speakSpanish(w.word)
                        },
                        enabled = !done,
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            done -> SuccessGreen.copy(alpha = 0.2f)
                            selected && wrongFlash -> MeteorRed.copy(alpha = 0.2f)
                            selected -> ExplorerBlue.copy(alpha = 0.15f)
                            else -> AdventureSurface
                        },
                        border = BorderStroke(2.dp, if (done) SuccessGreen else if (selected) ExplorerBlue else AdventureCardBorder),
                        modifier = Modifier.fillMaxWidth().height(66.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(w.word, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                pictures.forEach { p ->
                    val done = p in matched
                    Surface(
                        onClick = { pickedPicture = p },
                        enabled = !done,
                        shape = RoundedCornerShape(16.dp),
                        color = if (done) SuccessGreen.copy(alpha = 0.2f) else if (pickedPicture == p) ExplorerBlue.copy(alpha = 0.15f) else AdventureSurface,
                        border = BorderStroke(2.dp, if (done) SuccessGreen else AdventureCardBorder),
                        modifier = Modifier.fillMaxWidth().height(66.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(p.emoji, fontSize = 34.sp) }
                    }
                }
            }
        }
    }
}

/** Hear a word and pick its picture. */
@Composable
private fun ListenPick(word: CourseWord, options: List<CourseWord>, viewModel: BlasterViewModel, language: HelperLanguage, onMistake: () -> Unit, onDone: () -> Unit) {
    var picked by remember(word) { mutableStateOf<CourseWord?>(null) }
    val wrong = remember(word) { mutableStateListOf<CourseWord>() }
    LaunchedEffect(word) { viewModel.speakSpanish(word.word) }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(language.pick("👂 استمعي واختاري الصورة", "👂 Listen and pick the picture"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        AudioButton(onClick = { viewModel.speakSpanish(word.word) }, size = 72.dp)
        if (picked == word) Text(word.word, color = SuccessGreen, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        options.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { o ->
                    Surface(
                        onClick = {
                            if (picked != word) {
                                if (o == word) {
                                    picked = o
                                    viewModel.soundEngine.hit()
                                    onDone()
                                } else if (o !in wrong) {
                                    wrong += o
                                    viewModel.soundEngine.error()
                                    onMistake()
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            picked == word && o == word -> SuccessGreen.copy(alpha = 0.25f)
                            o in wrong -> MeteorRed.copy(alpha = 0.15f)
                            else -> AdventureSurface
                        },
                        border = BorderStroke(2.dp, if (picked == word && o == word) SuccessGreen else AdventureCardBorder),
                        modifier = Modifier.size(110.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text(o.emoji, fontSize = 52.sp) }
                    }
                }
            }
        }
    }
}

/**
 * Crossword with picture clues. Tap a word's square (or its clue) to choose it, then tap the letters
 * in order; a wrong letter just shakes. Crossing squares are shared, so one word helps the other.
 */
@Composable
fun CrosswordGame(words: List<CourseWord>, viewModel: BlasterViewModel, language: HelperLanguage, onMistake: () -> Unit, onDone: () -> Unit) {
    val entries = remember(words) { buildCrossword(words.map { it.bare.lowercase() }) }
    val filled = remember(words) { mutableStateMapOf<Pair<Int, Int>, Char>() }
    var selected by remember(words) { mutableStateOf(entries.firstOrNull()) }
    var wrongAt by remember(words) { mutableLongStateOf(0L) }
    fun complete(e: CrossEntry) = e.cells().all { it in filled }
    val current = selected
    val letters = remember(current) {
        current?.let { e ->
            val decoys = ('a'..'z').filter { it !in e.answer }.shuffled().take(2)
            (e.answer.toList().distinct() + decoys).shuffled()
        }.orEmpty()
    }

    fun type(c: Char) {
        val e = selected ?: return
        val cell = e.cells().firstOrNull { it !in filled } ?: return
        val expected = e.answer[e.cells().indexOf(cell)]
        if (c == expected) {
            filled[cell] = c
            viewModel.soundEngine.click()
            if (complete(e)) {
                viewModel.speakSpanish(words[e.index].word)
                viewModel.soundEngine.hit()
                if (entries.all { complete(it) }) {
                    onDone()
                } else {
                    selected = entries.firstOrNull { !complete(it) }
                }
            }
        } else {
            wrongAt = System.currentTimeMillis()
            viewModel.soundEngine.error()
            onMistake()
        }
    }

    if (entries.isEmpty()) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val rows = entries.maxOf { e -> e.cells().maxOf { it.first } } + 1
    val cols = entries.maxOf { e -> e.cells().maxOf { it.second } } + 1
    val starts = entries.associate { (it.row to it.col) to it.number }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(language.pick("🔠 الكلمات المتقاطعة: الصورة هي التلميح", "🔠 Crossword: the picture is the clue"), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        // The grid (always left-to-right, like the words).
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = minOf(maxWidth / cols, 34.dp)
                Column(Modifier.align(Alignment.Center)) {
                    for (r in 0 until rows) {
                        Row {
                            for (c in 0 until cols) {
                                val owners = entries.filter { (r to c) in it.cells() }
                                if (owners.isEmpty()) {
                                    Spacer(Modifier.size(cell))
                                } else {
                                    val inSelected = current != null && (r to c) in current.cells()
                                    val letter = filled[r to c]
                                    val shaking = inSelected && System.currentTimeMillis() - wrongAt < 400
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(cell)
                                            .padding(1.dp)
                                            .background(
                                                when {
                                                    shaking -> MeteorRed.copy(alpha = 0.3f)
                                                    inSelected -> SolarGold.copy(alpha = 0.35f)
                                                    owners.all { complete(it) } -> SuccessGreen.copy(alpha = 0.2f)
                                                    else -> AdventureSurface
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, TextSecondary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .clickable {
                                                selected = owners.firstOrNull { it.across == current?.across } ?: owners.first()
                                            }
                                    ) {
                                        starts[r to c]?.let { n ->
                                            Text("$n", color = TextSecondary, fontSize = 8.sp, modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp))
                                        }
                                        Text(letter?.uppercase() ?: "", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // Clues: the pictures, numbered.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { e ->
                val w = words[e.index]
                Surface(
                    onClick = { selected = e },
                    shape = RoundedCornerShape(12.dp),
                    color = if (complete(e)) SuccessGreen.copy(alpha = 0.2f) else if (e == current) SolarGold.copy(alpha = 0.35f) else AdventureSurface,
                    border = BorderStroke(1.dp, if (e == current) SolarAmber else AdventureCardBorder)
                ) {
                    Text("${e.number}${if (e.across) "→" else "↓"} ${w.emoji}", fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
        // Letters for the chosen word.
        if (current != null && !complete(current)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${current.number}${if (current.across) "→" else "↓"} ${words[current.index].emoji}", fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                AudioButton(onClick = { viewModel.speakSpanish(words[current.index].bare) }, size = 36.dp)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                letters.forEach { ch ->
                    Surface(
                        onClick = { type(ch) },
                        shape = RoundedCornerShape(12.dp),
                        color = SolarGold,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(ch.uppercase(), color = SpaceNavy, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------- Quick games (Practice tab)

/** Practice → Matching or Crossword with words from the units already opened. */
@Composable
fun QuickWordGameScreen(viewModel: BlasterViewModel, crossword: Boolean) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val stars by viewModel.courseStars.collectAsStateWithLifecycle()
    val data: SpanishContent = content ?: return LoadingContent(language == HelperLanguage.ARABIC)
    var round by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }
    var mistakes by remember { mutableIntStateOf(0) }
    val open = data.course.take(nextCourseUnit(data.course, stars) + 1).flatMap { it.words }
    if (open.size < 4) {
        Text(language.pick("ابدئي الدورة أولًا لتفتحي الكلمات.", "Start the course first to unlock words."), color = TextSecondary, modifier = Modifier.padding(24.dp))
        return
    }
    val words = remember(round) { open.shuffled().take(if (crossword) 8 else 4) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(round) {
                if (crossword) CrosswordGame(words, viewModel, language, { mistakes++ }, { finished = true })
                else MatchPairs(words, viewModel, language, { mistakes++ }, { finished = true })
            }
        }
        if (finished) {
            Text(
                language.pick("🎉 أحسنتِ!", "🎉 Well done!") + if (mistakes == 0) " ⭐⭐⭐" else "",
                color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
            )
            BlasterCyberButton(
                text = language.pick("🔁 مرة أخرى", "🔁 Play again"),
                onClick = {
                    finished = false
                    mistakes = 0
                    round++
                },
                color = SuccessGreen,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
