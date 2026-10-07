package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.delay
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.content.SpanishContent
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlin.math.abs
import kotlin.math.sin

/** The seven rooms of the ship. Each room has its own colour and its own kind of game. */
private enum class Station(val emoji: String, val es: String, val ar: String, val en: String, val color: Color) {
    STORY("📡", tl("Sala de radio"), "غرفة الراديو", "Radio room", DiamondCyan),
    OPENING("✏️", tl("Primeras preguntas"), "الأسئلة الأولى", "First questions", ExplorerBlue),
    SEARCH("🔍", tl("Bodega"), "المخزن", "Cargo hold", SolarAmber),
    CONSOLE("🔧", tl("Sala de máquinas"), "غرفة المحركات", "Engine room", MeteorRed),
    ORDER("🧩", tl("Mensaje roto"), "الرسالة المبعثرة", "Broken message", NebulaPurple),
    MISSION("🛰️", tl("Puente de mando"), "غرفة القيادة", "Bridge", SuccessGreen),
    PORTAL("☄️", tl("Lluvia de palabras"), "مطر الكلمات", "Word shower", SolarGold);

    fun label(language: HelperLanguage) = language.pick(ar, en)
}

// --------------------------------------------------------------------------- Ship floor plan

/**
 * The ship seen from above, one character per tile:
 * `#` wall, `.` floor, `P` start, `0`–`6` the pad of each room, `a`–`g` the door into room 0–6
 * (it opens when the room before is solved), `X` the exit door (needs the key), `K` where the key
 * appears, `E` the exit hatch, `*` a diamond and `+` a star.
 *
 * Rooms snake upwards: start → 0 → 1 along the bottom, 2 → 3 → 4 back along the middle,
 * 5 → 6 → exit along the top.
 */
private val SHIP = listOf(
    "#############",
    "#*.+#*..#..E#",
    "#.5.g.6.X...#",
    "#...#.K.#...#",
    "#+.*#*.+#...#",
    "##f##########",
    "#*.+#+.*#*.+#",
    "#.4.e.3.d.2.#",
    "#...#...#...#",
    "#+.*#*.+#+.*#",
    "##########c##",
    "#...#*.+#*.+#",
    "#.P.a.0.b.1.#",
    "#...#...#...#",
    "#...#+.*#+.*#",
    "#############"
)
private const val MAP_W = 13
private const val MAP_H = 16
private const val START_ROOM = -1
private const val EXIT_ROOM = 7
private const val STEP_TIME = 0.17f   // seconds to walk one tile

/** Which room a tile is in, from its 3×3 block on the plan (walls and doors belong to none). */
private fun roomAt(x: Int, y: Int): Int? {
    if (x % 4 == 0 || y % 5 == 0) return null
    return when (x / 4 to y / 5) {
        0 to 2 -> START_ROOM
        1 to 2 -> 0
        2 to 2 -> 1
        2 to 1 -> 2
        1 to 1 -> 3
        0 to 1 -> 4
        0 to 0 -> 5
        1 to 0 -> 6
        else -> EXIT_ROOM
    }
}

/** Every other chapter uses the mirrored ship, so the levels don't all look the same. */
private class ShipMap(mirrored: Boolean) {
    private val rows = if (mirrored) SHIP.map { it.reversed() } else SHIP
    fun at(p: IntOffset): Char = rows.getOrNull(p.y)?.getOrNull(p.x) ?: '#'
    fun find(c: Char): IntOffset {
        rows.forEachIndexed { y, row -> val x = row.indexOf(c); if (x >= 0) return IntOffset(x, y) }
        return IntOffset(1, 1)
    }
    val pads = (0..6).map { find('0' + it) }
    val start = find('P')
    val key = find('K')
    val tiles: List<Pair<IntOffset, Char>> = rows.flatMapIndexed { y, row -> row.mapIndexed { x, c -> IntOffset(x, y) to c } }
    val mirror = mirrored
    fun room(p: IntOffset): Int? = roomAt(if (mirror) MAP_W - 1 - p.x else p.x, p.y)
}

/** Walk-cycle frames inside explorer_walk.webp (x, y, width, height). */
internal val WALK_FRAMES = listOf(
    intArrayOf(164, 33, 387, 582),
    intArrayOf(727, 34, 375, 582),
    intArrayOf(144, 630, 420, 583),
    intArrayOf(729, 630, 390, 580)
)

private fun sceneDrawable(asset: String): Int = when (asset) {
    "hidden_laboratory" -> R.drawable.hidden_laboratory
    "hidden_archive" -> R.drawable.hidden_archive
    else -> R.drawable.hidden_cabin
}

@Composable
fun ExpeditionScreen(tablet: ReadingTablet, data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage) {
    val walkSprite = ImageBitmap.imageResource(R.drawable.explorer_walk)
    val niloSprite = ImageBitmap.imageResource(R.drawable.nilo_walk)
    val textMeasurer = rememberTextMeasurer()
    val chapterNumber = tablet.id.substringAfter('-').toIntOrNull() ?: 1
    val ship = remember(tablet.id) { ShipMap(mirrored = chapterNumber % 2 == 0) }

    // Lía walks tile by tile: from → to, progress 0..1.
    var liaFrom by remember { mutableStateOf(ship.start) }
    var liaTo by remember { mutableStateOf(ship.start) }
    var niloFrom by remember { mutableStateOf(ship.start - IntOffset(1, 0)) }
    var niloTo by remember { mutableStateOf(ship.start - IntOffset(1, 0)) }
    var progress by remember { mutableFloatStateOf(1f) }
    var facing by remember { mutableFloatStateOf(1f) }
    var held by remember { mutableStateOf<IntOffset?>(null) }
    var clock by remember { mutableFloatStateOf(0f) }

    // Mission state.
    val solved = remember { mutableStateListOf<Int>() }
    val solvedAt = remember { mutableStateMapOf<Int, Float>() }
    val collected = remember { mutableStateListOf<IntOffset>() }
    // Each wrong answer costs one collected diamond, so diamonds are worth protecting.
    var lostDiamonds by remember { mutableIntStateOf(0) }
    var lostAt by remember { mutableFloatStateOf(-10f) }
    var showStory by remember { mutableStateOf(false) }
    val results = remember { mutableStateMapOf<String, Boolean>() }
    var openStation by remember { mutableStateOf<Int?>(null) }
    var hasKey by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // Nilo, Lía's co-pilot: walks one step behind her and comments in the target language.
    var niloLine by remember { mutableStateOf<NiloLine?>(null) }
    var niloLineUntil by remember { mutableFloatStateOf(0f) }
    var lastMoveAt by remember { mutableFloatStateOf(0f) }
    var lastBumpAt by remember { mutableFloatStateOf(-10f) }

    fun say(line: NiloLine, speak: Boolean = true) {
        niloLine = line
        niloLineUntil = clock + 3.5f
        if (speak) viewModel.speakSpanish(line.es)
    }

    val scene = data.scenes[tablet.scene]
    val searchTargets = remember(tablet.id) { tablet.targets.mapNotNull { id -> scene?.objects?.firstOrNull { it.id == id } } }
    val tableCells = remember(tablet.id) {
        tablet.table.rows.indices.flatMap { r -> (1 until tablet.table.headers.size).map { c -> r to c } }
            .shuffled().take(4).toSet()
    }
    val totalQuestions = tablet.allQuestions.size + tableCells.size + 1 + searchTargets.size
    val nextStation = Station.entries.indices.firstOrNull { it !in solved } // null once every room is solved
    // The last sentence of the chapter, revealed word by word as rooms are won.
    val secretWords = remember(tablet.id) {
        tablet.ending.split(Regex("(?<=[.!?])\\s+")).lastOrNull { it.isNotBlank() }.orEmpty().split(" ").filter { it.isNotBlank() }
    }
    val secretOrder = remember(tablet.id) { secretWords.indices.shuffled(kotlin.random.Random(tablet.id.hashCode())) }

    fun doorOpen(c: Char): Boolean = when (c) {
        in 'a'..'g' -> c == 'a' || (c - 'a' - 1) in solved
        'X' -> hasKey
        else -> true
    }

    /** Lía has just stepped onto [p]: pick things up, open the room's game, take the key, leave. */
    fun arrive(p: IntOffset) {
        when (val c = ship.at(p)) {
            '*', '+' -> if (p !in collected) {
                collected += p
                viewModel.soundEngine.click()
                if (collected.size == 1) say(NiloLines.diamond)
            }
            in '0'..'6' -> {
                val i = c - '0'
                if (i == Station.entries.indices.firstOrNull { it !in solved }) {
                    openStation = i
                    held = null
                    viewModel.soundEngine.powerUp()
                }
            }
            'K' -> if (solved.size == Station.entries.size && !hasKey) {
                hasKey = true
                viewModel.soundEngine.powerUp()
                say(NiloLines.gotKey)
            }
            'E' -> if (hasKey && !finished) {
                finished = true
                held = null
                say(NiloLines.home)
                val kept = (collected.size - lostDiamonds).coerceAtLeast(0)
                viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions, bonusCredits = kept * 5)
            }
        }
    }

    // Game loop: walks Lía one tile at a time while an arrow is held.
    LaunchedEffect(tablet.id) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            clock += dt
            if (niloLine != null && clock > niloLineUntil && openStation == null) niloLine = null
            if (clock in 0.6f..0.7f && niloLine == null) say(NiloLines.start)
            if (progress < 1f) {
                progress = (progress + dt / STEP_TIME).coerceAtMost(1f)
                if (progress >= 1f) arrive(liaTo)
                continue
            }
            if (openStation != null || finished) continue
            val dir = held
            if (dir == null) {
                if (clock - lastMoveAt > 9f && niloLine == null) {
                    say(NiloLines.idle, speak = false)
                    lastMoveAt = clock
                }
                continue
            }
            if (dir.x != 0) facing = dir.x.toFloat()
            val next = liaTo + dir
            val c = ship.at(next)
            when {
                c == '#' -> Unit
                !doorOpen(c) -> if (clock - lastBumpAt > 3f) {
                    lastBumpAt = clock
                    viewModel.soundEngine.error()
                    say(if (c == 'X') NiloLines.needKey else NiloLines.locked)
                }
                else -> {
                    niloFrom = niloTo
                    niloTo = liaTo
                    liaFrom = liaTo
                    liaTo = next
                    progress = 0f
                    lastMoveAt = clock
                }
            }
        }
    }

    fun solve(i: Int) {
        if (i in solved) return
        solved += i
        solvedAt[i] = clock
        openStation = null
        viewModel.soundEngine.fanfare()
        say(if (i == Station.entries.lastIndex) NiloLines.findKey else NiloLines.doorOpen)
    }

    val diamonds = (collected.size - lostDiamonds).coerceAtLeast(0)
    val onMistake: () -> Unit = {
        if (collected.size - lostDiamonds > 0) {
            lostDiamonds++
            lostAt = clock
        }
    }

    Column(Modifier.fillMaxSize().background(AdventureBg)) {
        // Header: chapter, a button to reread the story, and the diamonds still held.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { showStory = true }) {
                Text(language.pick("📖 القصة", "📖 Story"), color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (hasKey) Text("🔑", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text("💎 $diamonds", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (clock - lostAt < 1.5f) Text("−1", color = MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        val station = openStation
        if (station == null) {
            GoalStrip(
                tablet = tablet,
                language = language,
                progress = solved.size,
                secret = secretWords,
                secretOrder = secretOrder,
                newPiece = solvedAt.values.any { clock - it < 4f },
                onSpeak = { viewModel.speakSpanish(it) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Canvas(Modifier.fillMaxSize()) {
                    drawShip(
                        ship = ship,
                        walkSprite = walkSprite,
                        niloSprite = niloSprite,
                        textMeasurer = textMeasurer,
                        clock = clock,
                        lia = lerpTile(liaFrom, liaTo, progress),
                        facing = facing,
                        walking = progress < 1f,
                        nilo = lerpTile(niloFrom, niloTo, progress),
                        niloFacing = if (niloTo.x != niloFrom.x) (niloTo.x - niloFrom.x).toFloat() else facing,
                        niloSays = niloLine?.es,
                        solved = solved,
                        solvedAt = solvedAt,
                        next = nextStation,
                        collected = collected,
                        hasKey = hasKey,
                        isOpen = ::doorOpen
                    )
                }
                // A short title card when a level starts.
                if (clock < 2.6f) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .background(SpaceNavy.copy(alpha = 0.88f))
                            .padding(horizontal = 22.dp, vertical = 14.dp)
                    ) {
                        Text("🚀 ${language.pick("المستوى", tl("Nivel"))} $chapterNumber", color = SolarGold, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Text("${tablet.level} · ${tablet.title(language)}", color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            ArrowPad(onHold = { held = it })
        } else {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalOnMistake provides onMistake) {
                    MissionPanel(
                        station = Station.entries[station],
                        tablet = tablet,
                        data = data,
                        scene = scene,
                        searchTargets = searchTargets,
                        tableCells = tableCells,
                        results = results,
                        viewModel = viewModel,
                        language = language,
                        onClose = { openStation = null },
                        onSolved = { solve(station) }
                    )
                }
            }
        }
    }

    if (showStory) {
        AlertDialog(
            onDismissRequest = { showStory = false },
            containerColor = AdventureBg,
            confirmButton = { TextButton(onClick = { showStory = false }) { Text(language.pick("إغلاق", "Close")) } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    StoryCard(language.pick("📡 القصة", tl("📡 La historia")), tablet.story, tablet.storyAr, viewModel, language)
                }
            }
        )
    }
}

private fun lerpTile(from: IntOffset, to: IntOffset, t: Float) =
    Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)

/** ▲ ◀ ▼ ▶ arrows; hold one to keep walking that way. */
@Composable
private fun ArrowPad(onHold: (IntOffset?) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().background(AdventureBg).padding(vertical = 6.dp)
        ) {
            PadButton("▲", ExplorerBlue, size = 54.dp, onPress = { onHold(IntOffset(0, -1)) }, onRelease = { onHold(null) })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PadButton("◀", ExplorerBlue, size = 54.dp, onPress = { onHold(IntOffset(-1, 0)) }, onRelease = { onHold(null) })
                PadButton("▼", ExplorerBlue, size = 54.dp, onPress = { onHold(IntOffset(0, 1)) }, onRelease = { onHold(null) })
                PadButton("▶", ExplorerBlue, size = 54.dp, onPress = { onHold(IntOffset(1, 0)) }, onRelease = { onHold(null) })
            }
        }
    }
}

// --------------------------------------------------------------------------- Drawing

private val WALL = Color(0xFF7D879E)
private val WALL_LIGHT = Color(0xFFA9B2C6)
private val WALL_DARK = Color(0xFF545D73)
private val FLOOR = Color(0xFFE4E8F0)

private fun DrawScope.drawShip(
    ship: ShipMap,
    walkSprite: ImageBitmap,
    niloSprite: ImageBitmap,
    textMeasurer: TextMeasurer,
    clock: Float,
    lia: Offset,
    facing: Float,
    walking: Boolean,
    nilo: Offset,
    niloFacing: Float,
    niloSays: String?,
    solved: List<Int>,
    solvedAt: Map<Int, Float>,
    next: Int?,
    collected: List<IntOffset>,
    hasKey: Boolean,
    isOpen: (Char) -> Boolean
) {
    drawRect(SpaceDeep)
    val ts = minOf(size.width / MAP_W, size.height / MAP_H)
    val ox = (size.width - ts * MAP_W) / 2f
    val oy = (size.height - ts * MAP_H) / 2f
    fun topLeft(x: Number, y: Number) = Offset(ox + x.toFloat() * ts, oy + y.toFloat() * ts)
    fun center(p: IntOffset) = topLeft(p.x + 0.5f, p.y + 0.5f)
    fun text(s: String, c: Offset, scale: Float, color: Color = Color.White, maxWidth: Float? = null) {
        val layout = textMeasurer.measure(
            s,
            TextStyle(color = color, fontSize = (ts * scale).toSp(), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
            constraints = maxWidth?.let { Constraints(maxWidth = it.toInt()) } ?: Constraints()
        )
        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
    }
    // Rooms you can reach are lit; rooms still behind locked doors stay dark.
    val reached = next ?: EXIT_ROOM
    fun roomColor(room: Int): Color = when (room) {
        START_ROOM -> DiamondCyan
        EXIT_ROOM -> SolarGold
        else -> Station.entries[room].color
    }

    ship.tiles.forEach { (p, c) ->
        val tl = topLeft(p.x, p.y)
        val tile = Size(ts, ts)
        if (c == '#') {
            // Bevelled block, like the walls of an old arcade maze.
            drawRect(WALL, tl, tile)
            drawRect(WALL_LIGHT, tl, Size(ts, ts * 0.14f))
            drawRect(WALL_LIGHT, tl, Size(ts * 0.14f, ts))
            drawRect(WALL_DARK, tl + Offset(0f, ts * 0.86f), Size(ts, ts * 0.14f))
            drawRect(WALL_DARK, tl + Offset(ts * 0.86f, 0f), Size(ts * 0.14f, ts))
            return@forEach
        }
        drawRect(FLOOR, tl, tile)
        drawRect(Color(0xFFC9CFDB), tl, tile, style = Stroke(1f))
        val room = ship.room(p)
        if (room != null) {
            drawRect(roomColor(room).copy(alpha = 0.22f), tl, tile)
            if (room > reached || (room == EXIT_ROOM && next != null)) drawRect(SpaceNavy.copy(alpha = 0.55f), tl, tile)
        }
        when (c) {
            in 'a'..'g', 'X' -> {
                val color = if (c == 'X') SolarGold else Station.entries[c - 'a'].color
                if (isOpen(c)) {
                    // Open door: just the frame.
                    drawRect(color, tl, Size(ts, ts * 0.12f))
                    drawRect(color, tl + Offset(0f, ts * 0.88f), Size(ts, ts * 0.12f))
                } else {
                    drawRoundRect(color, tl + Offset(ts * 0.06f, ts * 0.06f), Size(ts * 0.88f, ts * 0.88f), CornerRadius(ts * 0.15f))
                    text(if (c == 'X') "🔑" else "🔒", center(p), 0.42f)
                }
            }
            in '0'..'6' -> {
                val i = c - '0'
                val st = Station.entries[i]
                val done = i in solved
                val active = i == next
                if (active) {
                    val pulse = 0.5f + 0.5f * sin(clock * 4f)
                    drawCircle(st.color.copy(alpha = 0.25f + 0.25f * pulse), radius = ts * (0.55f + 0.12f * pulse), center = center(p))
                }
                val age = clock - (solvedAt[i] ?: -10f)
                if (done && age < 1.2f) {
                    drawCircle(SolarGold.copy(alpha = (1.2f - age) / 1.2f), radius = ts * (0.5f + age * 2.5f), center = center(p), style = Stroke(ts * 0.08f))
                }
                drawCircle(st.color.copy(alpha = if (done || active) 1f else 0.4f), radius = ts * 0.42f, center = center(p))
                drawCircle(Color.White, radius = ts * 0.42f, center = center(p), style = Stroke(ts * 0.06f))
                text(if (done) "✓" else st.emoji, center(p), 0.38f)
            }
            '*' -> if (p !in collected) {
                val bob = sin(clock * 3f + p.x + p.y) * ts * 0.05f
                val cc = center(p) + Offset(0f, bob)
                drawCircle(SolarGold.copy(alpha = 0.3f), radius = ts * 0.3f, center = cc)
                rotate(45f, pivot = cc) { drawRect(SolarGold, cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f)) }
                rotate(45f, pivot = cc) { drawRect(Color.White.copy(alpha = 0.7f), cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f), style = Stroke(ts * 0.04f)) }
            }
            '+' -> if (p !in collected) {
                val bob = sin(clock * 3f + p.x * 2 + p.y) * ts * 0.05f
                text("⭐", center(p) + Offset(0f, bob), 0.45f)
            }
            'K' -> if (next == null && !hasKey) {
                val pulse = 0.5f + 0.5f * sin(clock * 5f)
                drawCircle(SolarGold.copy(alpha = 0.3f + 0.3f * pulse), radius = ts * (0.45f + 0.1f * pulse), center = center(p))
                text("🔑", center(p), 0.55f)
            }
            'E' -> {
                drawRoundRect(if (hasKey) SolarGold else Color(0xFF8B93A6), tl + Offset(ts * 0.08f, ts * 0.08f), Size(ts * 0.84f, ts * 0.84f), CornerRadius(ts * 0.2f))
                text("🚪", center(p), 0.5f)
            }
        }
    }

    // Room names next to the pads of the rooms you can reach.
    Station.entries.forEachIndexed { i, st ->
        if (i > reached) return@forEachIndexed
        val p = ship.pads[i]
        text(st.es, center(p) + Offset(0f, ts * 0.78f), 0.24f, TextPrimary, maxWidth = ts * 3f)
    }

    // Characters: whoever is lower on the screen is drawn last, in front.
    val spriteH = ts * 1.45f
    val drawNilo = {
        val feet = topLeft(nilo.x + 0.5f, nilo.y + 0.9f)
        drawOval(Color(0x55000000), topLeft = feet - Offset(ts * 0.25f, ts * 0.07f), size = Size(ts * 0.5f, ts * 0.14f))
        withTransform({
            translate(feet.x, feet.y)
            scale(niloFacing, 1f, pivot = Offset.Zero)
        }) { drawNiloSprite(niloSprite, if (walking) ((clock * 9f).toInt() + 2) % 4 else 0, spriteH * 0.92f) }
    }
    val drawLia = {
        val feet = topLeft(lia.x + 0.5f, lia.y + 0.9f)
        val r = WALK_FRAMES[if (walking) (clock * 9f).toInt() % 4 else 0]
        val w = r[2].toFloat() / r[3] * spriteH
        drawOval(Color(0x66000000), topLeft = feet - Offset(ts * 0.28f, ts * 0.07f), size = Size(ts * 0.56f, ts * 0.14f))
        withTransform({
            translate(feet.x, feet.y)
            scale(facing, 1f, pivot = Offset.Zero)
        }) {
            drawImage(
                walkSprite,
                srcOffset = IntOffset(r[0], r[1]),
                srcSize = IntSize(r[2], r[3]),
                dstOffset = IntOffset((-w / 2).toInt(), (-spriteH).toInt()),
                dstSize = IntSize(w.toInt(), spriteH.toInt())
            )
        }
    }
    if (nilo.y <= lia.y) { drawNilo(); drawLia() } else { drawLia(); drawNilo() }
    niloSays?.let {
        val head = topLeft(nilo.x + 0.5f, nilo.y + 0.9f) - Offset(0f, spriteH + 4f)
        drawBubble(textMeasurer, it, head.x.coerceIn(size.width * 0.2f, size.width * 0.8f), head.y.coerceAtLeast(40f))
    }
}

/** Speech bubble whose bottom edge sits at [bottomY], centred on [centerX]. */
private fun DrawScope.drawBubble(textMeasurer: TextMeasurer, text: String, centerX: Float, bottomY: Float) {
    val layout = textMeasurer.measure(text, TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val w = layout.size.width + 16.dp.toPx()
    val h = layout.size.height + 10.dp.toPx()
    val left = centerX - w / 2
    val top = bottomY - h
    drawRoundRect(AdventureSurface, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9.dp.toPx()))
    drawRoundRect(SolarAmber, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9.dp.toPx()), style = Stroke(2.dp.toPx()))
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top + h / 2 - layout.size.height / 2f))
}

// --------------------------------------------------------------------------- Panels

/** Small strip above the deck: the chapter goal, rooms done and the secret message. */
@Composable
private fun GoalStrip(
    tablet: ReadingTablet,
    language: HelperLanguage,
    progress: Int,
    secret: List<String>,
    secretOrder: List<Int>,
    newPiece: Boolean,
    onSpeak: (String) -> Unit
) {
    val total = Station.entries.size
    // Words of the secret sentence revealed so far, spread over the seven rooms.
    val shown = secretOrder.take((secret.size * progress + total - 1) / total).toSet()
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 " + tablet.expeditionGoal(language), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2, modifier = Modifier.weight(1f))
            Text("🚪 $progress/$total", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🔐 " + secret.mapIndexed { i, w -> if (i in shown) w else "▁".repeat(w.trim('.', ',', '¡', '!', '¿', '?').length.coerceIn(2, 8)) }.joinToString(" "),
                color = if (newPiece) SuccessGreen else NebulaPurple, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp, modifier = Modifier.weight(1f)
            )
            if (progress >= total) AudioButton(onClick = { onSpeak(secret.joinToString(" ")) }, size = 32.dp)
        }
    }
}

/** One screen of a room's game: rooms are played one card at a time. */
private sealed interface Step {
    data class Sentence(val text: String, val number: Int, val total: Int, val translation: String?) : Step
    /** Typed answer. */
    data class Write(val key: String, val field: TabletField) : Step
    /** Word Jump: Lía jumps onto the right word. */
    data class Choice(val key: String, val prompt: String, val answers: List<String>, val hint: String, val options: List<String>) : Step
    /** Falling words: tap the right one before it lands. */
    data class Falling(val key: String, val prompt: String, val answers: List<String>, val hint: String, val options: List<String>) : Step
    data object Lesson : Step
    data object Table : Step
    data object Search : Step
    data object Order : Step
    data class Note(val emoji: String, val text: String) : Step
}

private fun storySentences(text: String) = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }

/** Each room plays differently: reading, writing, searching, the grammar table, ordering, jumping and falling words. */
private fun buildSteps(
    station: Station,
    tablet: ReadingTablet,
    data: SpanishContent,
    scene: HiddenScene?,
    searchTargets: List<HiddenObject>,
    language: HelperLanguage
): List<Step> {
    val choices = data.answerChoices
    return when (station) {
        Station.STORY -> storySentences(tablet.story).let { s ->
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, tablet.storyAr.takeIf { language == HelperLanguage.ARABIC }) }
        }
        Station.OPENING -> tablet.opening.mapIndexed { i, f -> Step.Write("opening-$i", f) }
        Station.SEARCH -> if (scene == null || searchTargets.isEmpty()) listOf(Step.Note("🔍", "")) else listOf(Step.Search)
        Station.CONSOLE -> listOf(Step.Lesson, Step.Table)
        Station.ORDER -> listOf(Step.Order)
        Station.MISSION -> listOf(Step.Note("🛰️", language.pick(tablet.missionAr, tablet.mission))) +
            tablet.fields.mapIndexed { i, f -> Step.Choice("fields-$i", f.label, f.answers, language.pick(f.hintAr, f.hint), choices.options(f.answers)) }
        Station.PORTAL -> storySentences(tablet.ending).let { s ->
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, null) }
        } + tablet.gate.mapIndexed { i, f ->
            Step.Falling("gate-$i", f.label, f.answers, language.pick(f.hintAr, f.hint), choices.options(f.answers))
        } + Step.Note("🏁", tablet.expeditionPayoff(language) + "\n" + language.pick("المكافأة: ", tl("Recompensa: ")) + tablet.reward(language))
    }
}

@Composable
private fun MissionPanel(
    station: Station,
    tablet: ReadingTablet,
    data: SpanishContent,
    scene: HiddenScene?,
    searchTargets: List<HiddenObject>,
    tableCells: Set<Pair<Int, Int>>,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onClose: () -> Unit,
    onSolved: () -> Unit
) {
    val steps = remember(station, tablet.id) { buildSteps(station, tablet, data, scene, searchTargets, language) }
    var index by remember(station) { mutableIntStateOf(0) }
    val step = steps[index]
    val done = when (step) {
        is Step.Write -> step.key in results
        is Step.Choice -> step.key in results
        is Step.Falling -> step.key in results
        Step.Table -> tableCells.all { (r, c) -> "table-$r-$c" in results }
        Step.Search -> searchTargets.all { "search-${it.id}" in results }
        Step.Order -> "order" in results
        else -> true
    }
    val last = index == steps.lastIndex
    val accent = station.color

    // Every room has its own colour, so each game feels like a different place on the ship.
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(0f to accent.copy(alpha = 0.22f), 0.45f to AdventureBg))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = accent) {
                Text(
                    "${station.emoji} ${station.es}",
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(station.label(language), color = TextSecondary, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f).padding(start = 8.dp))
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (i == index) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i <= index) accent else AdventureCardBorder)
                )
            }
            TextButton(onClick = onClose) { Text("✕", color = TextSecondary, fontSize = 18.sp) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(station, index) {
                val scroll = rememberScrollState()
                Column(Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (step) {
                        is Step.Sentence -> SentenceCard(step, viewModel, language)
                        is Step.Write -> QuestionCard(step.field, step.key, results, viewModel, language)
                        is Step.Choice -> ChoiceCard(step, results, viewModel, language)
                        is Step.Falling -> FallingWordsCard(step, results, viewModel, language)
                        Step.Lesson -> LessonCard(tablet, viewModel, language)
                        Step.Table -> GrammarTableQuiz(tablet.table, tableCells, results, viewModel, language)
                        Step.Search -> if (scene != null) HiddenObjectSearch(scene, searchTargets, results, viewModel, language)
                        Step.Order -> OrderPuzzle(tablet.order, tablet.orderTranslation(language), results, viewModel, language)
                        is Step.Note -> if (step.text.isNotBlank()) AdventureCard(borderColor = accent) {
                            Text(step.emoji, fontSize = 30.sp)
                            Text(step.text, color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                ScrollMoreHint(scroll, language, AdventureBg)
            }
        }
        BlasterCyberButton(
            text = when {
                !last -> language.pick("التالي ▶", tl("Siguiente ▶"))
                station == Station.PORTAL -> language.pick("هيا إلى المفتاح! 🔑", tl("¡A por la llave! 🔑"))
                else -> language.pick("تمّ! تابعي المشي ▶", tl("¡Hecho! Seguir caminando ▶"))
            },
            onClick = { if (last) onSolved() else index++ },
            enabled = done,
            color = if (last) SuccessGreen else accent,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }
}

/** One story sentence in Lía's speech bubble, read aloud. */
@Composable
private fun SentenceCard(step: Step.Sentence, viewModel: BlasterViewModel, language: HelperLanguage) {
    var showTranslation by remember { mutableStateOf(false) }
    LaunchedEffect(step.text) { viewModel.speakSpanish(step.text) }
    Row(verticalAlignment = Alignment.Top) {
        Image(painterResource(R.drawable.lia_happy), contentDescription = "Lía", modifier = Modifier.size(70.dp))
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = { viewModel.speakSpanish(step.text) },
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 22.dp, bottomEnd = 22.dp, bottomStart = 22.dp),
            color = AdventureSurface,
            border = BorderStroke(2.dp, SolarGold),
            modifier = Modifier.weight(1f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(step.text, color = TextPrimary, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${step.number} / ${step.total}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    AudioButton(onClick = { viewModel.speakSpanish(step.text) }, size = 40.dp)
                }
            }
        }
    }
    if (step.translation != null) {
        TextButton(onClick = { showTranslation = !showTranslation }) {
            Text(if (showTranslation) "إخفاء الترجمة" else "🌐 ترجمة القصة", color = ExplorerBlue)
        }
        if (showTranslation) Text(step.translation, color = TextSecondary, fontSize = 15.sp, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth())
    }
}

/** A question answered by jumping onto the right word bubble (Word Jump). Wrong jumps cost a diamond. */
@Composable
private fun ChoiceCard(step: Step.Choice, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val onMistake = LocalOnMistake.current
    val wrong = remember { mutableStateListOf<String>() }
    val accepted = remember(step) { step.answers.flatMap { it.split("/") }.map { normalizeAnswer(it) } }
    val solved = step.key in results
    AdventureCard(borderColor = if (solved) SuccessGreen else ExplorerBlue) {
        Text(step.prompt, color = TextPrimary, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
    Text(
        language.pick("اضغطي على الإجابة الصحيحة لتقفز ليا إليها!", "Tap the right answer and Lía jumps onto it!"),
        color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
    )
    WordJump(
        options = step.options,
        solved = solved,
        wrong = wrong,
        check = { normalizeAnswer(it) in accepted },
        onLanded = { option ->
            if (step.key !in results) {
                if (normalizeAnswer(option) in accepted) {
                    results[step.key] = wrong.isEmpty()
                    viewModel.soundEngine.hit()
                    viewModel.speakSpanish(option)
                } else {
                    wrong += option
                    viewModel.soundEngine.error()
                    onMistake()
                    // After two misses the right answer lights up so the player can continue.
                    if (wrong.size >= 2) results[step.key] = false
                }
            }
        }
    )
    if (wrong.isNotEmpty() && step.hint.isNotBlank()) {
        Text("💡 Nilo: " + step.hint, color = SolarAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * The meteor game inside the ship: the answers fall as meteors and must be shot before they land.
 * A wrong shot or a meteor shower that lands costs a diamond; after two misses the answer is shown.
 */
@Composable
private fun FallingWordsCard(step: Step.Falling, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val onMistake = LocalOnMistake.current
    val wrong = remember(step.key) { mutableStateListOf<Int>() }
    val accepted = remember(step) { step.answers.flatMap { it.split("/") }.map { normalizeAnswer(it) } }
    val correctIndex = step.options.indexOfFirst { normalizeAnswer(it) in accepted }
    val solved = step.key in results
    val fall = remember(step.key) { Animatable(0f) }
    var wave by remember(step.key) { mutableIntStateOf(0) }
    var misses by remember(step.key) { mutableIntStateOf(0) }
    var blasted by remember(step.key) { mutableStateOf<Int?>(null) }

    fun miss() {
        misses++
        viewModel.soundEngine.error()
        onMistake()
        if (misses >= 2) results[step.key] = false
    }

    LaunchedEffect(step.key, wave) {
        if (step.key in results) return@LaunchedEffect
        fall.snapTo(0f)
        fall.animateTo(1f, tween(9000, easing = LinearEasing))
        // The meteors landed before the right one was shot.
        if (step.key !in results) {
            miss()
            if (step.key !in results) {
                delay(1200)
                wave++
            }
        }
    }
    LaunchedEffect(solved) { if (solved) fall.stop() }

    AdventureCard(borderColor = if (solved) SuccessGreen else SolarGold) {
        Text(step.prompt, color = TextPrimary, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
    Text(
        language.pick("☄️ اضغطي على النيزك الصحيح قبل أن يسقط!", "☄️ Shoot the right meteor before it lands!"),
        color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
    ) {
        StarField(Modifier.matchParentSize())
        val laneWidth = maxWidth / step.options.size.coerceAtLeast(1)
        val meteorSize = minOf(laneWidth - 8.dp, 92.dp)
        val travel = maxHeight - meteorSize - 44.dp
        step.options.forEachIndexed { index, option ->
            val stagger = (index % 2) * 0.06f
            val progress = (fall.value * (1f + stagger) - stagger).coerceIn(0f, 1f)
            Meteor(
                text = option,
                wrong = index in wrong,
                blasted = blasted == index,
                reveal = solved && blasted == null && index == correctIndex,
                modifier = Modifier
                    .offset(x = laneWidth * index + (laneWidth - meteorSize) / 2, y = travel * progress)
                    .size(meteorSize),
                onClick = {
                    if (step.key !in results) {
                        if (index == correctIndex) {
                            blasted = index
                            results[step.key] = wrong.isEmpty() && misses == 0
                            viewModel.soundEngine.hit()
                            viewModel.speakSpanish(option)
                        } else {
                            wrong += index
                            miss()
                        }
                    }
                }
            )
        }
        Text("🛸", fontSize = 34.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
    }
    if ((wrong.isNotEmpty() || misses > 0) && step.hint.isNotBlank()) {
        Text("💡 Nilo: " + step.hint, color = SolarAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
    }
}

/** Tap the Spanish objects in the illustrated room (the Órbita hidden-object scenes). */
@Composable
private fun HiddenObjectSearch(
    scene: HiddenScene,
    targets: List<HiddenObject>,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage
) {
    var mistakes by remember { mutableIntStateOf(0) }
    val onMistake = LocalOnMistake.current
    /** Last wrong tap, as a fraction of the picture. */
    var wrongTap by remember { mutableStateOf<Offset?>(null) }
    var fullScreen by remember { mutableStateOf(false) }
    val found = targets.filter { "search-${it.id}" in results }
    val remaining = targets.filter { "search-${it.id}" !in results }
    val hinted = if (mistakes >= 3) remaining.firstOrNull() else null

    val onTap: (Float, Float) -> Unit = { fx, fy ->
        val hit = remaining.firstOrNull { obj ->
            obj.boxes.any { b -> fx in (b[0] - 0.02f)..(b[0] + b[2] + 0.02f) && fy in (b[1] - 0.02f)..(b[1] + b[3] + 0.02f) }
        }
        if (hit != null) {
            results["search-${hit.id}"] = mistakes == 0
            mistakes = 0
            wrongTap = null
            viewModel.soundEngine.hit()
            viewModel.speakSpanish(hit.spanish)
            viewModel.recordWord(hit.spanish.substringAfter(' '), hit.english, "search", true)
            if (remaining.size == 1) fullScreen = false
        } else {
            mistakes++
            wrongTap = Offset(fx, fy)
            viewModel.soundEngine.error()
            onMistake()
        }
    }

    AdventureCard(borderColor = ExplorerBlue) {
        Text(language.pick("🔍 ابحثي عن هذه الأشياء في الغرفة واضغطي عليها:", "🔍 Find these objects in the room and tap them:"), color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        TargetChips(targets, found, viewModel)
    }

    ZoomableScene(
        scene = scene,
        found = found,
        hinted = hinted,
        wrongTap = wrongTap,
        fillHeight = false,
        onTap = onTap,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SCENE_ASPECT)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, ExplorerBlue, RoundedCornerShape(14.dp))
    )
    Button(
        onClick = { fullScreen = true },
        colors = ButtonDefaults.buttonColors(containerColor = ExplorerBlue),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        Text(language.pick("⛶ تكبير الصورة إلى ملء الشاشة", "⛶ Make the picture full screen"), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
    Text(
        language.pick(
            "🤏 قرّبي بإصبعين أو اضغطي مرتين للتكبير · اضغطي على الكلمة لسماعها.",
            "🤏 Pinch or double-tap to zoom · tap a word to hear it."
        ),
        color = TextSecondary, fontSize = 12.sp
    )
    if (hinted != null) {
        NiloSays(NiloLines.searchHint, language, onSpeak = { viewModel.speakSpanish(NiloLines.searchHint.es) }, size = 44.dp)
    }

    if (fullScreen) {
        Dialog(
            onDismissRequest = { fullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                ZoomableScene(
                    scene = scene,
                    found = found,
                    hinted = hinted,
                    wrongTap = wrongTap,
                    fillHeight = true,
                    onTap = onTap,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            language.pick("ابحثي عن: ${found.size}/${targets.size}", "Find: ${found.size}/${targets.size}"),
                            color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { fullScreen = false }) {
                            Icon(Icons.Default.Close, contentDescription = language.pick("إغلاق", "Close"), tint = Color.White)
                        }
                    }
                    TargetChips(targets, found, viewModel)
                }
                if (hinted != null) {
                    Text(
                        "💡 " + NiloLines.searchHint.meaning(language),
                        color = SolarGold, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.55f)).padding(12.dp)
                    )
                }
            }
        }
    }
}

private const val SCENE_ASPECT = 1536f / 1024f

@Composable
private fun TargetChips(targets: List<HiddenObject>, found: List<HiddenObject>, viewModel: BlasterViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        targets.forEach { obj ->
            val done = obj in found
            Surface(
                onClick = { viewModel.speakSpanish(obj.spanish) },
                shape = RoundedCornerShape(10.dp),
                color = if (done) SuccessGreen.copy(alpha = 0.25f) else AdventureSurfaceVariant,
                border = BorderStroke(1.dp, if (done) SuccessGreen else ExplorerBlue)
            ) {
                Text(
                    (if (done) "✓ " else "🔊 ") + obj.spanish,
                    color = if (done) SuccessGreen else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * The hidden-object picture with pinch-zoom, drag and double-tap zoom. Taps are converted back to
 * fractions of the picture so hit boxes work at any zoom. With [fillHeight] the picture starts
 * zoomed so it fills the screen height (drag sideways to look around).
 */
@Composable
private fun ZoomableScene(
    scene: HiddenScene,
    found: List<HiddenObject>,
    hinted: HiddenObject?,
    wrongTap: Offset?,
    fillHeight: Boolean,
    onTap: (Float, Float) -> Unit,
    modifier: Modifier
) {
    var container by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var initialised by remember { mutableStateOf(false) }
    val currentOnTap by rememberUpdatedState(onTap)

    val w = container.width.toFloat()
    val h = w / SCENE_ASPECT                       // picture height when it fits the width
    val top = (container.height - h) / 2f
    val minScale = if (fillHeight && h > 0f) maxOf(1f, container.height / h) else 1f

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = maxOf(0f, (w * s - w) / 2f)
        val maxY = maxOf(0f, (h * s - container.height) / 2f)
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    LaunchedEffect(container) {
        if (container.width > 0 && !initialised) {
            scale = minScale
            offset = Offset.Zero
            initialised = true
        }
    }

    Box(
        modifier
            .onSizeChanged { container = it }
            .pointerInput(container, minScale) {
                // One finger only pans once zoomed (or in full screen), so the page can still scroll.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || fillHeight || scale > minScale + 0.01f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            if (zoom != 1f || pan != Offset.Zero) {
                                val s = (scale * zoom).coerceIn(minScale, minScale * 4f)
                                scale = s
                                offset = clamp(offset + pan, s)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(container, minScale) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        val zoomIn = scale < minScale * 1.8f
                        val s = if (zoomIn) minScale * 2.5f else minScale
                        // Zoom towards the tapped point.
                        val centre = Offset(w / 2f, top + h / 2f)
                        offset = if (zoomIn) clamp((centre - tap) * (s / scale - 1f) + offset * (s / scale), s) else Offset.Zero
                        scale = s
                    },
                    onTap = { tap ->
                        if (w == 0f) return@detectTapGestures
                        val centre = Offset(w / 2f, top + h / 2f)
                        val local = (tap - centre - offset) / scale
                        val fx = (local.x + w / 2f) / w
                        val fy = (local.y + h / 2f) / h
                        if (fx in 0f..1f && fy in 0f..1f) currentOnTap(fx, fy)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(SCENE_ASPECT)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        ) {
            Image(
                painter = painterResource(sceneDrawable(scene.asset)),
                contentDescription = scene.name,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )
            Canvas(Modifier.matchParentSize()) {
                val line = 5f / scale
                found.forEach { obj ->
                    obj.boxes.forEach { b ->
                        drawRoundRect(
                            SuccessGreen,
                            topLeft = Offset(b[0] * size.width, b[1] * size.height),
                            size = Size(b[2] * size.width, b[3] * size.height),
                            cornerRadius = CornerRadius(10f / scale),
                            style = Stroke(width = line)
                        )
                    }
                }
                hinted?.boxes?.firstOrNull()?.let { b ->
                    drawCircle(
                        SolarGold.copy(alpha = 0.6f),
                        radius = maxOf(b[2] * size.width, b[3] * size.height) * 0.9f,
                        center = Offset((b[0] + b[2] / 2) * size.width, (b[1] + b[3] / 2) * size.height),
                        style = Stroke(width = 6f / scale)
                    )
                }
                wrongTap?.let {
                    drawCircle(MeteorRed, radius = 18f / scale, center = Offset(it.x * size.width, it.y * size.height), style = Stroke(width = 4f / scale))
                }
            }
        }
    }
}
