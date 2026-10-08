package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseUnit
import com.example.data.model.CourseWord
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlin.math.abs
import kotlin.random.Random

private const val COLS = 5
private const val ROWS = 6
private const val ROUNDS = 3
private const val HEARTS = 3

/** One board: the rule (a unit's topic), and a word in every cell (null once eaten). */
private class MunchBoard(val unit: CourseUnit, val cells: List<CourseWord?>) {
    private val words = unit.words.map { it.word }.toSet()
    fun fits(w: CourseWord) = w.word in words
}

private fun newBoard(open: List<CourseUnit>, random: Random): MunchBoard {
    val unit = open.random(random)
    val right = unit.words.shuffled(random).take(6)
    val others = open.filter { it != unit }.flatMap { it.words }.ifEmpty { unit.words }
    val rightWords = unit.words.map { it.word }.toSet()
    val wrong = others.filter { it.word !in rightWords }.distinctBy { it.word }.shuffled(random).take(COLS * ROWS - right.size)
    val cells = (right + wrong).shuffled(random) + List((COLS * ROWS - right.size - wrong.size).coerceAtLeast(0)) { null }
    return MunchBoard(unit, cells.take(COLS * ROWS))
}

/**
 * Practice → Word Munchers (after the old MECC game): eat only the words that fit the rule.
 * Turn based: the robot takes one step towards Lía every time she moves or eats.
 */
@Composable
fun MunchersScreen(viewModel: BlasterViewModel) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val language by viewModel.helperLanguage.collectAsStateWithLifecycle()
    val stars by viewModel.courseStars.collectAsStateWithLifecycle()
    val reward by viewModel.practiceReward.collectAsStateWithLifecycle()
    val data = content ?: return LoadingContent(language == HelperLanguage.ARABIC)
    val open = data.course.take(nextCourseUnit(data.course, stars) + 1).ifEmpty { data.course }
    if (open.size < 2) {
        Text(language.pick("ابدئي الدورة أولًا لتفتحي الكلمات.", "Start the course first to unlock words."), color = TextSecondary, modifier = Modifier.padding(24.dp))
        return
    }
    var game by remember { mutableIntStateOf(0) }
    var lost by remember { mutableStateOf(false) }
    key(game) { MunchGame(open, viewModel, language) { lost = it } }
    reward?.let {
        val title = if (lost) language.pick("💔 انتهت القلوب، حاولي مرة أخرى", "💔 Out of hearts, try again") else language.pick("😋 وجبة رائعة!", "😋 Great munching!")
        RewardDialog(it, language == HelperLanguage.ARABIC, title) {
            viewModel.dismissPracticeReward()
            game++
        }
    }
}

@Composable
private fun MunchGame(open: List<CourseUnit>, viewModel: BlasterViewModel, language: HelperLanguage, onOver: (lost: Boolean) -> Unit) {
    var round by remember { mutableIntStateOf(1) }
    var board by remember { mutableStateOf(newBoard(open, Random)) }
    val cells = remember(board) { board.cells.toMutableStateList() }
    var lia by remember(board) { mutableIntStateOf(0) }
    var robot by remember(board) { mutableIntStateOf(COLS * ROWS - 1) }
    var hearts by remember { mutableIntStateOf(HEARTS) }
    var score by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var streak by remember { mutableIntStateOf(0) }
    var bestStreak by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<String?>(null) }
    /** Why the last eaten word was wrong: the word, its meaning and picture. */
    var wrongWord by remember { mutableStateOf<CourseWord?>(null) }
    var over by remember { mutableStateOf(false) }
    /** Lía's moves so far; the robot only steps on every second one, so she can get away. */
    var moves by remember(board) { mutableIntStateOf(0) }
    /** Turns the robot stays knocked out (a right word stuns it). */
    var stunned by remember(board) { mutableIntStateOf(2) }

    fun finish() {
        if (over) return
        over = true
        onOver(hearts <= 0)
        viewModel.finishPracticeRun("MUNCH", score, correct, bestStreak)
    }

    /** After Lía moves, the robot steps towards her every second turn (along the longer distance first). */
    fun robotTurn() {
        if (stunned > 0) { stunned--; return }
        moves++
        if (moves % 2 == 1) return
        val (lx, ly) = lia % COLS to lia / COLS
        val (rx, ry) = robot % COLS to robot / COLS
        val dx = (lx - rx).coerceIn(-1, 1)
        val dy = (ly - ry).coerceIn(-1, 1)
        robot = if (abs(lx - rx) >= abs(ly - ry) && dx != 0) robot + dx else if (dy != 0) robot + dy * COLS else robot + dx
        if (robot == lia) {
            hearts--
            streak = 0
            flash = "🤖💥"
            viewModel.soundEngine.error()
            // The robot goes back to the far corner and rests a moment.
            robot = if (lia < COLS * ROWS / 2) COLS * ROWS - 1 else 0
            stunned = 3
            if (hearts <= 0) finish()
        }
    }

    fun move(dx: Int, dy: Int) {
        if (over) return
        val x = lia % COLS + dx
        val y = lia / COLS + dy
        if (x !in 0 until COLS || y !in 0 until ROWS) return
        lia = y * COLS + x
        flash = null
        wrongWord = null
        robotTurn()
    }

    fun eat() {
        if (over) return
        val w = cells[lia] ?: return
        cells[lia] = null
        viewModel.speakSpanish(w.word)
        if (board.fits(w)) {
            correct++
            streak++
            bestStreak = maxOf(bestStreak, streak)
            score += 10 + 2 * streak
            flash = "😋 ${w.emoji}"
            wrongWord = null
            // A right word knocks the robot out for a few turns.
            stunned = 3
            viewModel.soundEngine.hit()
        } else {
            hearts--
            streak = 0
            flash = "🤢"
            wrongWord = w
            viewModel.soundEngine.error()
            if (hearts <= 0) {
                finish()
                return
            }
        }
        if (cells.none { it != null && board.fits(it) }) {
            wrongWord = null
            // Board cleared: next rule, or the end.
            viewModel.soundEngine.fanfare()
            if (round >= ROUNDS) finish() else {
                round++
                board = newBoard(open, Random)
            }
            return
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❤️".repeat(hearts.coerceAtLeast(0)) + "🖤".repeat((HEARTS - hearts).coerceAtLeast(0)), fontSize = 18.sp)
            Text("$round / $ROUNDS", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text("⭐ $score", color = SolarAmber, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        // The rule, with the unit's picture so it is clear without reading.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(SpaceNavy).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(board.unit.emoji, fontSize = 34.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(language.pick("كُلي فقط كلمات:", "Eat only words about:"), color = StarWhite.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
                Text("${board.unit.title} · ${board.unit.helperTitle(language)}", color = StarWhite, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
            flash?.let { Text(it, fontSize = 20.sp) }
        }
        // A wrong word: say what it means and that it is not from this topic.
        wrongWord?.let { w ->
            Text(
                bidiSafe(language.pick("✗ ${w.word} ${w.emoji} = ${w.arabic} — ليست من ${board.unit.titleAr} ${board.unit.emoji}", "✗ ${w.word} ${w.emoji} = ${w.english}, not ${board.unit.titleEn} ${board.unit.emoji}")),
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MeteorRed.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        // The board.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (y in 0 until ROWS) {
                    Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (x in 0 until COLS) {
                            val i = y * COLS + x
                            val here = i == lia
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (here) SolarGold.copy(alpha = 0.35f) else Color(0xFF243156))
                                    .border(if (here) 3.dp else 1.dp, if (here) SolarGold else DiamondCyan.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            ) {
                                cells[i]?.let {
                                    Text(it.word, color = StarWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 13.sp, modifier = Modifier.padding(2.dp))
                                }
                                if (i == robot) Text(if (stunned > 0) "😵" else "🤖", fontSize = 26.sp)
                                if (here) Text("🐸", fontSize = 22.sp, modifier = Modifier.align(Alignment.TopEnd))
                            }
                        }
                    }
                }
            }
            // Controls: arrows on the left, Eat on the right.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PadButton("▲", ExplorerBlue, size = 50.dp, onPress = { move(0, -1) })
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PadButton("◀", ExplorerBlue, size = 50.dp, onPress = { move(-1, 0) })
                        PadButton("▼", ExplorerBlue, size = 50.dp, onPress = { move(0, 1) })
                        PadButton("▶", ExplorerBlue, size = 50.dp, onPress = { move(1, 0) })
                    }
                }
                PadButton("😋", SuccessGreen, size = 76.dp, onPress = { eat() })
            }
        }
    }
}
