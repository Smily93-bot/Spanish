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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.ads.RewardedAds
import com.example.data.content.SpanishContent
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlin.math.abs
import kotlin.math.sin

/** The seven rooms of the ship. Each room has its own colour and its own kind of game. */
private enum class Station(
    val emoji: String, val es: String, val ar: String, val en: String, val color: Color,
    /** What to do in this room: in the helper language at first, in the target language later on. */
    val howAr: String, val howEn: String, val howEs: String
) {
    STORY("📡", tl("Sala de radio"), "غرفة الراديو", "Radio room", DiamondCyan,
        "استمعي إلى القصة جملةً جملة. اضغطي على أي كلمة لتعرفي معناها.", "Listen to the story one sentence at a time. Tap any word to see what it means.",
        tl("Escucha la historia frase por frase. Toca una palabra para ver qué significa.")),
    OPENING("✏️", tl("Primeras preguntas"), "الأسئلة الأولى", "First questions", ExplorerBlue,
        "اكتبي الكلمة الناقصة. التلميح تحت السؤال يساعدك.", "Type the missing word. The tip under the question helps you.",
        tl("Escribe la palabra que falta. La pista te ayuda.")),
    SEARCH("🔍", tl("Bodega"), "المخزن", "Cargo hold", SolarAmber,
        "اسمعي اسم كل شيء ثم ابحثي عنه في الصورة واضغطي عليه.", "Listen to each word, then find it in the picture and tap it.",
        tl("Escucha cada palabra y búscala en la imagen.")),
    CONSOLE("🔧", tl("Sala de máquinas"), "غرفة المحركات", "Engine room", MeteorRed,
        "اقرئي القاعدة أولًا، ثم أكملي الجدول.", "Read the rule first, then fill in the table.",
        tl("Lee la regla y completa la tabla.")),
    ORDER("🔀", tl("Mensaje roto"), "الرسالة المبعثرة", "Broken message", NebulaPurple,
        "رتّبي الكلمات لتكوّني الجملة. المعنى مكتوب فوقها.", "Put the words in order to build the sentence. Its meaning is shown above.",
        tl("Ordena las palabras para formar la frase.")),
    MISSION("🛰️", tl("Puente de mando"), "غرفة القيادة", "Bridge", SuccessGreen,
        "امشي بالأسهم واقفزي على الإجابة الصحيحة ثم اضغطي اختيار.", "Walk with the arrows, jump onto the right answer and press Choose.",
        tl("Camina con las flechas, salta a la respuesta correcta y pulsa Elegir.")),
    PORTAL("☄️", tl("Lluvia de palabras"), "مطر الكلمات", "Word shower", SolarGold,
        "اضغطي على النيزك الذي يحمل الإجابة الصحيحة قبل أن يسقط.", "Tap the meteor with the right answer before it lands.",
        tl("Toca el meteorito con la respuesta correcta antes de que caiga."));

    fun label(language: HelperLanguage) = language.pick(ar, en)
    fun how(language: HelperLanguage) = language.pick(howAr, howEn)
}

// --------------------------------------------------------------------------- Ship floor plans

/*
 * Ships seen from above, one character per tile:
 * `#` wall, `.` floor, `P` start, `0`–`6` the pad of each room, `a`–`g` the door into room 0–6
 * (it opens when the room before is solved), `X` the exit door (needs the key), `K` where the key
 * appears and `E` the exit hatch. Diamonds (`*`) are added per level.
 */
private val SHIP_PLANS = listOf(
    // Three rows of three square rooms.
    listOf(
        "#############",
        "#...#...#..E#",
        "#.5.g.6.X...#",
        "#...#.K.#...#",
        "#...#...#...#",
        "##f##########",
        "#...#...#...#",
        "#.4.e.3.d.2.#",
        "#...#...#...#",
        "#...#...#...#",
        "##########c##",
        "#...#...#...#",
        "#.P.a.0.b.1.#",
        "#...#...#...#",
        "#...#...#...#",
        "#############"
    ),
    // Long halls and small cabins.
    listOf(
        "#############",
        "#..#....#...#",
        "#E.X..6.g.5.#",
        "#..#.K..#...#",
        "##########f##",
        "#.......#...#",
        "#...3...e.4.#",
        "#.......#...#",
        "##d##########",
        "#.....#.....#",
        "#..2..c..1..#",
        "#.....#.....#",
        "##########b##",
        "#.P.a..0....#",
        "#...#.......#",
        "#############"
    ),
    // Three tall decks side by side.
    listOf(
        "#############",
        "#...#...#.E.#",
        "#...c...#...#",
        "#.1.#.2.##X##",
        "#...#...#...#",
        "##b##...#...#",
        "#...##d##.6.#",
        "#...#...#K..#",
        "#.0.#.3.#...#",
        "#...#...##g##",
        "#...##e##...#",
        "##a##...#.5.#",
        "#...#...f...#",
        "#.P.#.4.#...#",
        "#...#...#...#",
        "#############"
    )
)
private const val MAP_W = 13
private const val MAP_H = 16
private const val START_ROOM = -1
private const val EXIT_ROOM = 7
private const val STEP_TIME = 0.17f   // seconds to walk one tile
private const val ZOOM = 1.3f         // how much closer than "whole ship on screen"
private val DOORS = "abcdefgX"
private val SIDES = listOf(IntOffset(1, 0), IntOffset(-1, 0), IntOffset(0, 1), IntOffset(0, -1))

/**
 * Spaceship puzzles some rooms get before their game opens (after the old PC games Chip's Challenge
 * and Sokoban). The names in code stay simple; on screen they are ship things.
 */
private enum class Challenge(val icon: String) {
    KEY("💳"),      // find the room's access card first
    BOXES("🔋"),    // push the batteries onto the chargers (Sokoban)
    REVERSE("🔄"),  // broken gravity magnets: the arrows push Lía the opposite way
    ROBOT("🤖"),    // a patrolling security robot: touching it costs a diamond
    FETCH("🎁")     // a crew member asks (in the target language) for a thing kept in an earlier room
}

/** Fetch quest: the crew in [room] needs [wanted]; it lies in an earlier room next to two decoys. */
private class Fetch(val room: Int, val wanted: CourseWord, val items: List<Pair<IntOffset, CourseWord>>)

/** A robot walking back and forth over [length] tiles from [from] in direction [dir]. */
private class Robot(val room: Int, val from: IntOffset, val dir: IntOffset, val length: Int) {
    /** Position in tiles at time [clock] (two tiles a second). */
    fun at(clock: Float): Offset {
        val span = (length - 1).coerceAtLeast(1)
        val s = (clock * 2f) % (2f * span)
        val k = if (s <= span) s else 2f * span - s
        return Offset(from.x + dir.x * k, from.y + dir.y * k)
    }
}

/**
 * The ship for one level: the floor plans take turns (and are mirrored on the next round), each
 * level scatters its own diamonds, and from level to level more rooms get a puzzle.
 */
private class ShipMap(level: Int, private val things: List<CourseWord>) {
    private val grid: Array<CharArray>
    private val roomOf = HashMap<IntOffset, Int>()
    val challenges = HashMap<Int, Challenge>()
    /** Boxes (room, start tile) and the circles they must be pushed onto. */
    val crateStarts = mutableListOf<Pair<Int, IntOffset>>()
    val targets = HashMap<Int, MutableList<IntOffset>>()
    val robots = mutableListOf<Robot>()
    val fetches = HashMap<Int, Fetch>()
    /** Tiles taken by a puzzle, kept free of diamonds (declared before init, which fills it). */
    private val used = hashSetOf<IntOffset>()

    fun at(p: IntOffset): Char = grid.getOrNull(p.y)?.getOrNull(p.x) ?: '#'
    private fun inside(p: IntOffset) = p.x in 0 until MAP_W && p.y in 0 until MAP_H
    private fun find(c: Char): IntOffset {
        grid.forEachIndexed { y, row -> val x = row.indexOf(c); if (x >= 0) return IntOffset(x, y) }
        return IntOffset(1, 1)
    }

    init {
        val plan = SHIP_PLANS[(level - 1).mod(SHIP_PLANS.size)]
        val mirrored = ((level - 1) / SHIP_PLANS.size) % 2 == 1
        grid = Array(MAP_H) { y -> (if (mirrored) plan[y].reversed() else plan[y]).toCharArray() }
        val random = kotlin.random.Random(level * 7919 + 17)
        // Rooms: flood out from each pad (and the start and exit) without passing walls or doors.
        val seeds = listOf(find('P') to START_ROOM, find('E') to EXIT_ROOM) + (0..6).map { find('0' + it) to it }
        seeds.forEach { (seed, id) ->
            val stack = ArrayDeque(listOf(seed))
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                if (!inside(p) || p in roomOf || at(p) == '#' || at(p) in DOORS) continue
                roomOf[p] = id
                SIDES.forEach { stack.addLast(p + it) }
            }
        }
        placeChallenges(level, random)
        // Diamonds in different places every level: two in each room, one in the start room.
        (listOf(START_ROOM) + (0..6)).forEach { id ->
            roomOf.filter { (p, r) -> r == id && at(p) == '.' && p !in used }.keys.shuffled(random)
                .take(if (id == START_ROOM) 1 else 2)
                .forEach { grid[it.y][it.x] = '*' }
        }
    }

    // ------------------------------------------------------------------ Puzzles

    private fun doorInto(room: Int) = find(DOORS[room])
    fun doorTo(room: Int) = doorInto(room)

    /** A plain floor tile of [room] that isn't next to a door, a pad or the start/key/exit. */
    private fun free(p: IntOffset, room: Int) =
        roomOf[p] == room && at(p) == '.' && p !in used && SIDES.none { at(p + it) in DOORS || at(p + it) in "0123456PKE" }

    private fun freeTiles(room: Int) = roomOf.keys.filter { free(it, room) }

    /** Tiles reachable from the start with every door open. */
    private fun reachable(blocked: Set<IntOffset> = emptySet()): Set<IntOffset> {
        fun open(p: IntOffset) = inside(p) && at(p) != '#' && p !in blocked
        val start = find('P')
        val seen = hashSetOf(start)
        val queue = ArrayDeque(listOf(start))
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            for (d in SIDES) {
                val q = p + d
                if (open(q) && seen.add(q)) queue.addLast(q)
            }
        }
        return seen
    }

    /** Every pad, the key spot and the exit can be reached. */
    private fun solvable(blocked: Set<IntOffset> = emptySet()): Boolean {
        val seen = reachable(blocked)
        return (0..6).all { find('0' + it) in seen } && find('K') in seen && find('E') in seen
    }

    private fun placeChallenges(level: Int, random: kotlin.random.Random) {
        // More rooms get a puzzle as the levels go on; fetch quests send Lía back to earlier rooms.
        val kinds = when (level) {
            1 -> listOf(Challenge.FETCH, Challenge.KEY)
            2 -> listOf(Challenge.FETCH, Challenge.BOXES, Challenge.KEY)
            3 -> listOf(Challenge.FETCH, Challenge.BOXES, Challenge.REVERSE, Challenge.FETCH)
            else -> listOf(
                Challenge.FETCH, Challenge.FETCH, Challenge.BOXES, Challenge.BOXES,
                Challenge.KEY, Challenge.REVERSE, Challenge.ROBOT
            ).shuffled(random).take(minOf(7, 3 + level / 2))
        }
        val rooms = (0..6).shuffled(random).toMutableList()
        for (kind in kinds) {
            val room = rooms.firstOrNull { place(kind, it, level, random) } ?: continue
            rooms.remove(room)
            challenges[room] = kind
        }
    }

    private fun place(kind: Challenge, room: Int, level: Int, random: kotlin.random.Random): Boolean {
        val tiles = freeTiles(room)
        when (kind) {
            Challenge.KEY -> {
                // The key hides in the corner furthest from the door.
                val door = doorInto(room)
                val p = tiles.maxByOrNull { abs(it.x - door.x) + abs(it.y - door.y) } ?: return false
                grid[p.y][p.x] = 'k'
                used += p
                return true
            }
            Challenge.BOXES -> {
                // Each box sits in a straight line: where Lía stands, the box, (free tiles,) the circle.
                val pushes = if (level >= 4) 2 else 1
                var placed = 0
                for (start in tiles.shuffled(random)) {
                    if (placed == 3) break
                    for (d in SIDES.shuffled(random)) {
                        val line = (0..pushes + 1).map { IntOffset(start.x + d.x * it, start.y + d.y * it) }
                        if (!line.all { free(it, room) }) continue
                        val crate = line[1]
                        val target = line.last()
                        val boxesNow = crateStarts.filter { it.first == room }.map { it.second }.toSet() + crate
                        val boxesDone = targets[room].orEmpty().toSet() + target
                        if (!solvable(boxesNow) || !solvable(boxesDone) || line[0] !in reachable(boxesNow)) continue
                        crateStarts += room to crate
                        targets.getOrPut(room) { mutableListOf() } += target
                        grid[target.y][target.x] = 'T'
                        used += line
                        placed++
                        break
                    }
                }
                if (placed == 0) return false
                return true
            }
            // The whole room is affected; nothing to place.
            Challenge.REVERSE -> return true
            Challenge.FETCH -> {
                // Only things you can carry: nouns (taught with their article), not "hello" or "sad".
                val articles = setOf("el", "la", "los", "las", "il", "lo", "i", "gli", "le")
                val nouns = things.filter { w ->
                    val first = w.word.lowercase().substringBefore(' ')
                    (w.word.contains(' ') && first in articles) || w.word.lowercase().startsWith("l'")
                }
                val picks = nouns.distinctBy { it.emoji }.shuffled(random).take(3)
                if (picks.size < 3) return false
                // The thing waits in a room Lía has already passed, so she has to walk back for it.
                val source = (START_ROOM until room).shuffled(random).firstOrNull { freeTiles(it).size >= 3 } ?: return false
                val spots = freeTiles(source).shuffled(random).take(3)
                fetches[room] = Fetch(room, picks[0], spots.zip(picks.shuffled(random)))
                used += spots
                return true
            }
            Challenge.ROBOT -> {
                for (start in tiles.shuffled(random)) {
                    for (d in listOf(IntOffset(1, 0), IntOffset(0, 1))) {
                        var length = 1
                        while (length < 5 && free(IntOffset(start.x + d.x * length, start.y + d.y * length), room)) length++
                        if (length >= 3) {
                            robots += Robot(room, start, d, length)
                            return true
                        }
                    }
                }
                return false
            }
        }
    }

    val pads = (0..6).map { find('0' + it) }
    val start = find('P')
    val tiles: List<Pair<IntOffset, Char>> = grid.flatMapIndexed { y, row -> row.mapIndexed { x, c -> IntOffset(x, y) to c } }
    fun room(p: IntOffset): Int? = roomOf[p]
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
    val chapterNumber = data.tablets.indexOf(tablet) + 1
    val ship = remember(tablet.id) {
        // Things to fetch come from the units with objects you can pick up (school, food, animals, clothes, space).
        val carryable = data.course.filter { it.id in setOf("u6", "u7", "u8", "u10", "u12") }.flatMap { it.words }
        ShipMap(level = chapterNumber, things = carryable.ifEmpty { data.courseWords })
    }

    // Lía walks tile by tile: from → to, progress 0..1.
    var liaFrom by remember { mutableStateOf(ship.start) }
    var liaTo by remember { mutableStateOf(ship.start) }
    var niloFrom by remember { mutableStateOf(ship.start - IntOffset(1, 0)) }
    var niloTo by remember { mutableStateOf(ship.start - IntOffset(1, 0)) }
    var progress by remember { mutableFloatStateOf(1f) }
    var facing by remember { mutableFloatStateOf(1f) }
    var held by remember { mutableStateOf<IntOffset?>(null) }
    // Puzzles: where the batteries are now, access cards picked up, robot bumps.
    val crates = remember(tablet.id) { ship.crateStarts.map { it.second }.toMutableStateList() }
    val roomKeys = remember { mutableStateListOf<Int>() }
    // Fetch quests: things lying around, the one Lía carries, and rooms that got what they needed.
    val things = remember(tablet.id) { ship.fetches.values.flatMap { it.items }.toMutableStateMap() }
    var carrying by remember { mutableStateOf<CourseWord?>(null) }
    val delivered = remember { mutableStateListOf<Int>() }
    var lastHintAt by remember { mutableFloatStateOf(-10f) }
    var lastRobotHit by remember { mutableFloatStateOf(-10f) }
    var clock by remember { mutableFloatStateOf(0f) }

    // Mission state.
    val solved = remember { mutableStateListOf<Int>() }
    val solvedAt = remember { mutableStateMapOf<Int, Float>() }
    val collected = remember { mutableStateListOf<IntOffset>() }
    // Each wrong answer costs one collected diamond, so diamonds are worth protecting.
    var lostDiamonds by remember { mutableIntStateOf(0) }
    var lostAt by remember { mutableFloatStateOf(-10f) }
    // With no diamonds left, a mistake pauses the game until a short video earns one back.
    var adDiamonds by remember { mutableIntStateOf(0) }
    var outOfDiamonds by remember { mutableStateOf(false) }
    var adMisses by remember { mutableIntStateOf(0) }
    var showStory by remember { mutableStateOf(false) }
    // Every chapter starts with its lesson and words, before the ship.
    var briefed by remember { mutableStateOf(false) }
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
        niloLineUntil = clock + 2.5f
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

    /** A room's puzzle is done (or it has none), so its game can open. */
    fun puzzleDone(room: Int): Boolean = when (ship.challenges[room]) {
        Challenge.KEY -> room in roomKeys
        Challenge.BOXES -> ship.targets[room].orEmpty().all { it in crates }
        Challenge.FETCH -> room in delivered
        else -> true
    }

    fun hint(line: NiloLine) {
        if (clock - lastHintAt > 3f) {
            lastHintAt = clock
            say(line)
        }
    }

    /** Lía has just stepped onto [p]: pick things up, open the room's game, take the key, leave. */
    fun arrive(p: IntOffset) {
        if (ship.challenges[ship.room(p) ?: -1] == Challenge.REVERSE) hint(NiloLines.reversed)
        things[p]?.let { thing ->
            // Pick it up; whatever she was carrying is left here instead.
            things.remove(p)
            carrying?.let { things[p] = it }
            carrying = thing
            viewModel.soundEngine.click()
            say(NiloLine("🎒 ${thing.word}", "🎒 ${thing.word}", "🎒 ${thing.word}"))
        }
        when (val c = ship.at(p)) {
            'k' -> ship.room(p)?.let { room ->
                if (room !in roomKeys) {
                    roomKeys += room
                    viewModel.soundEngine.powerUp()
                    say(NiloLines.roomKey)
                }
            }
            '*' -> if (p !in collected) {
                collected += p
                viewModel.soundEngine.click()
            }
            in '0'..'6' -> {
                val i = c - '0'
                val fetch = ship.fetches[i]
                if (fetch != null && i !in delivered && i == Station.entries.indices.firstOrNull { it !in solved }) {
                    val inHand = carrying
                    when {
                        inHand == fetch.wanted -> {
                            delivered += i
                            carrying = null
                            viewModel.soundEngine.powerUp()
                            say(NiloLines.thanks)
                        }
                        inHand != null -> say(NiloLine("${tl("No, eso es")} ${inHand.word}. ${tl("Necesito")} ${fetch.wanted.word}.", "", ""))
                        else -> say(NiloLine("${tl("Necesito")} ${fetch.wanted.word}.", "", ""))
                    }
                }
                if (i == Station.entries.indices.firstOrNull { it !in solved } && !puzzleDone(i)) {
                    viewModel.soundEngine.error()
                    when (ship.challenges[i]) {
                        Challenge.KEY -> hint(NiloLines.findRoomKey)
                        Challenge.BOXES -> hint(NiloLines.pushBoxes)
                        else -> Unit
                    }
                } else if (i == Station.entries.indices.firstOrNull { it !in solved }) {
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
                val kept = (collected.size + adDiamonds - lostDiamonds).coerceAtLeast(0)
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
            if (progress < 1f) {
                progress = (progress + dt / STEP_TIME).coerceAtMost(1f)
                if (progress >= 1f) arrive(liaTo)
                continue
            }
            if (openStation != null || finished) continue
            // Robots: bumping into one costs a diamond and sends Lía back a step.
            if (clock - lastRobotHit > 1.5f && ship.robots.any { (it.at(clock) - Offset(liaTo.x.toFloat(), liaTo.y.toFloat())).getDistance() < 0.6f }) {
                lastRobotHit = clock
                if (collected.size + adDiamonds - lostDiamonds > 0) {
                    lostDiamonds++
                    lostAt = clock
                }
                viewModel.soundEngine.error()
                say(NiloLines.robot)
                if (liaFrom != liaTo) {
                    val back = liaFrom
                    liaFrom = liaTo
                    liaTo = back
                    progress = 0f
                    continue
                }
            }
            // In a room with broken gravity magnets every arrow works the other way round.
            val reversed = ship.challenges[ship.room(liaTo) ?: -1] == Challenge.REVERSE
            val dir = held?.let { if (reversed) IntOffset(-it.x, -it.y) else it }
            if (dir == null) {
                continue
            }
            if (dir.x != 0) facing = dir.x.toFloat()
            val next = liaTo + dir
            val c = ship.at(next)
            val crate = crates.indexOf(next)
            when {
                c == '#' -> Unit
                !doorOpen(c) -> {
                    if (clock - lastBumpAt > 3f) {
                        lastBumpAt = clock
                        viewModel.soundEngine.error()
                        say(if (c == 'X') NiloLines.needKey else NiloLines.locked)
                    }
                }
                crate >= 0 -> {
                    // Sokoban: a box moves one tile if the tile behind it is empty floor in the same room.
                    val beyond = next + dir
                    if (ship.at(beyond) in ".T" && beyond !in crates && ship.room(beyond) == ship.room(next)) {
                        crates[crate] = beyond
                        viewModel.soundEngine.click()
                        niloFrom = niloTo
                        niloTo = liaTo
                        liaFrom = liaTo
                        liaTo = next
                        progress = 0f
                        lastMoveAt = clock
                        val room = ship.room(beyond)
                        if (room != null && ship.challenges[room] == Challenge.BOXES && puzzleDone(room)) {
                            viewModel.soundEngine.powerUp()
                            say(NiloLines.boxesDone)
                        }
                    }
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
        // Only the last room speaks (the key appears); a door opening is visible on the map.
        if (i == Station.entries.lastIndex) say(NiloLines.findKey)
    }

    val diamonds = (collected.size + adDiamonds - lostDiamonds).coerceAtLeast(0)
    val onMistake: () -> Unit = {
        if (collected.size + adDiamonds - lostDiamonds > 0) {
            lostDiamonds++
            lostAt = clock
        } else {
            outOfDiamonds = true
        }
    }

    Column(Modifier.fillMaxSize().background(AdventureBg)) {
        // Header: chapter, a button to reread the story, and the diamonds still held.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { briefed = false }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(language.pick("📘 الدرس", "📘 Lesson"), color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            TextButton(onClick = { showStory = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(language.pick("📖 القصة", "📖 Story"), color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (hasKey) Text("🔑", fontSize = 18.sp, modifier = Modifier.padding(end = 6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text("💎 $diamonds", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (clock - lostAt < 1.5f) Text("−1", color = MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        val station = openStation
        if (!briefed) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                MissionBriefing(tablet, chapterNumber, data, viewModel, language, onStart = {
                    briefed = true
                })
            }
        } else if (station == null) {
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
                // Clipped so the zoomed-in ship never draws over the goal text above it.
                Canvas(Modifier.fillMaxSize().clipToBounds()) {
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
                        isOpen = ::doorOpen,
                        crates = crates,
                        roomKeys = roomKeys,
                        puzzleDone = ::puzzleDone,
                        things = things,
                        carrying = carrying
                    )
                }
                // The current fetch request, in the target language (tap to hear it).
                val request = nextStation?.let { ship.fetches[it] }?.takeIf { it.room !in delivered }
                if (request != null || carrying != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceNavy.copy(alpha = 0.85f))
                            .clickable { request?.let { viewModel.speakSpanish(it.wanted.word) } }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        if (request != null) {
                            Text("🛰️ ${tl("Necesito")}: ${request.wanted.word} 🔊", color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        carrying?.let { Text("   🙌 ${it.emoji}", color = SolarGold, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    }
                }
                // Battery rooms (Sokoban): put the batteries back if one gets stuck.
                val hereRoom = ship.room(liaTo)
                if (hereRoom != null && ship.challenges[hereRoom] == Challenge.BOXES && !puzzleDone(hereRoom) && hereRoom == nextStation) {
                    Surface(
                        onClick = {
                            ship.crateStarts.forEachIndexed { k, (room, start) -> if (room == hereRoom) crates[k] = start }
                            // Back to the door so Lía isn't standing inside a box.
                            val door = ship.doorTo(hereRoom)
                            liaFrom = door
                            liaTo = door
                            niloFrom = door
                            niloTo = door
                            progress = 1f
                        },
                        shape = RoundedCornerShape(50),
                        color = SolarAmber,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Text(
                            language.pick("↺ أعيدي البطاريات", "↺ Reset batteries"),
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
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
                        level = chapterNumber,
                        onClose = { openStation = null },
                        onSolved = { solve(station) }
                    )
                }
            }
        }
    }

    if (outOfDiamonds) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = {},
            containerColor = AdventureSurface,
            title = { Text(language.pick("💎 نفدت الماسات!", "💎 Out of diamonds!"), color = SolarAmber, fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    if (adMisses == 0) language.pick("شاهدي فيديو قصيرًا لتحصلي على ماسة واحدة وتكملي.", "Watch a short video to get 1 diamond and keep playing.")
                    else language.pick("الفيديو يُحمَّل… جرّبي مرة أخرى بعد لحظة.", "The video is loading… try again in a moment."),
                    color = TextPrimary, fontSize = 16.sp
                )
            },
            confirmButton = {
                BlasterCyberButton(
                    text = language.pick("▶ شاهدي الفيديو", "▶ Watch video"),
                    onClick = {
                        val shown = RewardedAds.show(context, onReward = {
                            adDiamonds++
                            outOfDiamonds = false
                            adMisses = 0
                            viewModel.soundEngine.powerUp()
                        })
                        if (!shown) {
                            adMisses++
                            // No video available (e.g. offline): don't leave the player stuck.
                            if (adMisses >= 3) {
                                adDiamonds++
                                outOfDiamonds = false
                                adMisses = 0
                            }
                        }
                    },
                    color = SuccessGreen
                )
            }
        )
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

private val HULL = Color(0xFF1E2A48)
private val HULL_LIGHT = Color(0xFF2E3D63)
private val FLOOR_PLATE = Color(0xFF3A4A72)
private val FLOOR_SEAM = Color(0xFF28365A)

/** Fixed star field (fractions of the view), so it doesn't flicker between frames. */
private val SPACE_STARS: List<Offset> = List(80) { i ->
    val r = kotlin.random.Random(i * 7919 + 13)
    Offset(r.nextFloat(), r.nextFloat())
}

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
    isOpen: (Char) -> Boolean,
    crates: List<IntOffset>,
    roomKeys: List<Int>,
    puzzleDone: (Int) -> Boolean,
    things: Map<IntOffset, CourseWord>,
    carrying: CourseWord?
) {
    // Space all around the ship.
    drawRect(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
    SPACE_STARS.forEachIndexed { i, s ->
        drawCircle(StarWhite.copy(alpha = 0.35f + 0.3f * sin(clock * 1.5f + i)), radius = 1.6f + (i % 3), center = Offset(s.x * size.width, s.y * size.height))
    }

    // A little closer than the whole ship: the camera follows Lía and stops at the ship's edges.
    // (The extra 3 × 2.4 tiles leave room for the wings, the nose and the engines.)
    val ts = minOf(size.width / (MAP_W + 3f), size.height / (MAP_H + 2.4f)) * ZOOM
    fun follow(view: Float, total: Float, margin: Float, focus: Float): Float {
        if (total <= view) return (view - total) / 2f + margin
        return (view / 2f - focus).coerceIn(view - total + margin, margin)
    }
    val ox = follow(size.width, ts * (MAP_W + 3f), ts * 1.5f, (lia.x + 0.5f) * ts)
    val oy = follow(size.height, ts * (MAP_H + 2.4f), ts * 1.5f, (lia.y + 0.5f) * ts)
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
    val shipW = ts * MAP_W
    val shipH = ts * MAP_H

    // Engines: two nozzles with flickering flames.
    listOf(3.5f, MAP_W - 3.5f).forEachIndexed { k, ex ->
        val cx = ox + ex * ts
        val flame = ts * (1.0f + 0.25f * sin(clock * 18f + k * 2f))
        drawPath(
            Path().apply {
                moveTo(cx - ts * 0.55f, oy + shipH)
                lineTo(cx + ts * 0.55f, oy + shipH)
                lineTo(cx, oy + shipH + flame)
                close()
            },
            Brush.verticalGradient(listOf(SolarGold, SolarAmber, Color.Transparent), startY = oy + shipH, endY = oy + shipH + flame)
        )
        drawRect(HULL_LIGHT, topLeft = Offset(cx - ts * 0.7f, oy + shipH - ts * 0.2f), size = Size(ts * 1.4f, ts * 0.35f))
    }
    // Wings.
    for (side in listOf(-1f, 1f)) {
        val edge = if (side < 0) ox else ox + shipW
        val wing = Path().apply {
            moveTo(edge, oy + ts * 6f)
            lineTo(edge + side * ts * 1.5f, oy + ts * 11f)
            lineTo(edge + side * ts * 1.5f, oy + ts * 14.5f)
            lineTo(edge, oy + ts * 14.5f)
            close()
        }
        drawPath(wing, HULL)
        drawPath(wing, DiamondCyan.copy(alpha = 0.5f), style = Stroke(ts * 0.06f))
        // Blinking light at the wing tip.
        drawCircle(if (side < 0) MeteorRed else SuccessGreen, radius = ts * 0.15f * (0.7f + 0.3f * sin(clock * 4f)), center = Offset(edge + side * ts * 1.5f, oy + ts * 11f))
    }
    // Nose with the cockpit window.
    drawOval(HULL, topLeft = Offset(ox, oy - ts * 1.5f), size = Size(shipW, ts * 3f))
    drawOval(DiamondCyan.copy(alpha = 0.5f), topLeft = Offset(ox, oy - ts * 1.5f), size = Size(shipW, ts * 3f), style = Stroke(ts * 0.06f))
    drawOval(
        Brush.verticalGradient(listOf(DiamondCyan, ExplorerBlue), startY = oy - ts * 1.2f, endY = oy - ts * 0.4f),
        topLeft = Offset(ox + shipW / 2 - ts * 1.6f, oy - ts * 1.2f), size = Size(ts * 3.2f, ts * 0.9f)
    )
    // Hull body.
    drawRoundRect(HULL, topLeft = Offset(ox, oy), size = Size(shipW, shipH), cornerRadius = CornerRadius(ts * 0.6f))

    // Rooms you can reach are lit; rooms still behind locked doors stay dark.
    val reached = next ?: EXIT_ROOM
    fun roomColor(room: Int): Color = when (room) {
        START_ROOM -> DiamondCyan
        EXIT_ROOM -> SolarGold
        else -> Station.entries[room].color
    }
    fun walkable(p: IntOffset) = ship.at(p) != '#'

    ship.tiles.forEach { (p, c) ->
        val tl = topLeft(p.x, p.y)
        val tile = Size(ts, ts)
        if (c == '#') {
            // Hull wall: dark metal with a glowing edge where it meets a room.
            drawRect(HULL, tl, tile)
            drawRect(HULL_LIGHT, tl + Offset(ts * 0.12f, ts * 0.12f), Size(ts * 0.76f, ts * 0.76f))
            val glow = DiamondCyan.copy(alpha = 0.55f)
            val w = ts * 0.06f
            if (walkable(p + IntOffset(0, -1))) drawRect(glow, tl, Size(ts, w))
            if (walkable(p + IntOffset(0, 1))) drawRect(glow, tl + Offset(0f, ts - w), Size(ts, w))
            if (walkable(p + IntOffset(-1, 0))) drawRect(glow, tl, Size(w, ts))
            if (walkable(p + IntOffset(1, 0))) drawRect(glow, tl + Offset(ts - w, 0f), Size(w, ts))
            // Portholes along the outer hull.
            val outer = p.x == 0 || p.x == MAP_W - 1
            if (outer && p.y % 3 == 1 && p.y in 1 until MAP_H - 1) {
                drawCircle(SpaceDeep, radius = ts * 0.3f, center = center(p))
                drawCircle(DiamondCyan.copy(alpha = 0.35f), radius = ts * 0.22f, center = center(p))
                drawCircle(StarWhite, radius = ts * 0.05f, center = center(p) + Offset(-ts * 0.08f, -ts * 0.08f))
                drawCircle(HULL_LIGHT, radius = ts * 0.3f, center = center(p), style = Stroke(ts * 0.07f))
            }
            return@forEach
        }
        // Floor plates with seams and rivets.
        drawRect(FLOOR_PLATE, tl, tile)
        drawRect(FLOOR_SEAM, tl, tile, style = Stroke(ts * 0.04f))
        val rivet = ts * 0.035f
        drawCircle(FLOOR_SEAM, rivet, tl + Offset(ts * 0.15f, ts * 0.15f))
        drawCircle(FLOOR_SEAM, rivet, tl + Offset(ts * 0.85f, ts * 0.85f))
        val room = ship.room(p)
        if (room != null) {
            drawRect(roomColor(room).copy(alpha = 0.30f), tl, tile)
            if (ship.challenges[room] == Challenge.REVERSE) {
                // Broken gravity magnets: striped floor with a turning arrow here and there.
                drawRect(SolarAmber.copy(alpha = 0.25f), tl, tile)
                drawLine(SolarAmber.copy(alpha = 0.6f), tl + Offset(0f, ts), tl + Offset(ts, 0f), strokeWidth = ts * 0.06f)
                if ((p.x + p.y) % 3 == 0 && c == '.') {
                    rotate(clock * 90f, pivot = center(p)) { text("🔄", center(p), 0.32f) }
                }
            }
            if (room > reached || (room == EXIT_ROOM && next != null)) drawRect(Color.Black.copy(alpha = 0.45f), tl, tile)
        }
        when (c) {
            in 'a'..'g', 'X' -> {
                val color = if (c == 'X') SolarGold else Station.entries[c - 'a'].color
                val acrossX = walkable(p + IntOffset(-1, 0)) && walkable(p + IntOffset(1, 0))
                if (isOpen(c)) {
                    // Open sliding door: only the glowing frame is left.
                    if (acrossX) {
                        drawRect(color, tl, Size(ts, ts * 0.1f))
                        drawRect(color, tl + Offset(0f, ts * 0.9f), Size(ts, ts * 0.1f))
                    } else {
                        drawRect(color, tl, Size(ts * 0.1f, ts))
                        drawRect(color, tl + Offset(ts * 0.9f, 0f), Size(ts * 0.1f, ts))
                    }
                } else {
                    // Closed: two door halves meeting in the middle, with a lock.
                    drawRect(color, tl, tile)
                    drawRect(color.copy(alpha = 0.6f), tl + Offset(ts * 0.1f, ts * 0.1f), Size(ts * 0.8f, ts * 0.8f))
                    if (acrossX) drawLine(HULL, tl + Offset(0f, ts / 2), tl + Offset(ts, ts / 2), strokeWidth = ts * 0.05f)
                    else drawLine(HULL, tl + Offset(ts / 2, 0f), tl + Offset(ts / 2, ts), strokeWidth = ts * 0.05f)
                    text(if (c == 'X') "🔑" else "🔒", center(p), 0.4f)
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
                // The room's puzzle comes first: show what it needs on the pad.
                ship.challenges[i]?.let { ch ->
                    if (!done && !puzzleDone(i) && (ch == Challenge.KEY || ch == Challenge.BOXES || ch == Challenge.FETCH)) {
                        // For a fetch quest, the picture of the thing this room needs.
                        val icon = if (ch == Challenge.FETCH) ship.fetches[i]?.wanted?.emoji ?: ch.icon else ch.icon
                        drawCircle(StarWhite, radius = ts * 0.24f, center = center(p) + Offset(ts * 0.36f, -ts * 0.36f))
                        text(icon, center(p) + Offset(ts * 0.36f, -ts * 0.36f), 0.28f)
                    }
                }
            }
            '*' -> if (p !in collected) {
                val bob = sin(clock * 3f + p.x + p.y) * ts * 0.05f
                val cc = center(p) + Offset(0f, bob)
                drawCircle(SolarGold.copy(alpha = 0.3f), radius = ts * 0.3f, center = cc)
                rotate(45f, pivot = cc) { drawRect(SolarGold, cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f)) }
                rotate(45f, pivot = cc) { drawRect(Color.White.copy(alpha = 0.7f), cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f), style = Stroke(ts * 0.04f)) }
            }
            'T' -> {
                // Charger: where a battery must go.
                drawCircle(DiamondCyan, radius = ts * 0.34f, center = center(p), style = Stroke(ts * 0.07f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ts * 0.12f, ts * 0.08f))))
                text("⚡", center(p), 0.3f)
            }
            'k' -> if (ship.room(p)?.let { it in roomKeys } != true) {
                val bob = sin(clock * 3f + p.x) * ts * 0.05f
                val room = ship.room(p) ?: 0
                drawCircle(Station.entries[room.coerceIn(0, 6)].color.copy(alpha = 0.5f), radius = ts * 0.34f, center = center(p) + Offset(0f, bob))
                text("💳", center(p) + Offset(0f, bob), 0.42f)
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


    // Batteries: empty until pushed onto a charger, then they fill up green.
    crates.forEach { p ->
        val tl = topLeft(p.x, p.y) + Offset(ts * 0.18f, ts * 0.14f)
        val charging = ship.at(p) == 'T'
        val body = Size(ts * 0.64f, ts * 0.76f)
        drawRect(HULL_LIGHT, tl + Offset(ts * 0.22f, -ts * 0.08f), Size(ts * 0.2f, ts * 0.1f))   // terminal
        drawRoundRect(Color(0xFF2B2F3A), tl, body, CornerRadius(ts * 0.1f))
        val bars = if (charging) 3 else 1
        repeat(bars) { k ->
            drawRoundRect(
                if (charging) SuccessGreen else MeteorRed,
                tl + Offset(ts * 0.1f, ts * (0.54f - k * 0.2f)),
                Size(ts * 0.44f, ts * 0.14f),
                CornerRadius(ts * 0.04f)
            )
        }
        drawRoundRect(if (charging) SuccessGreen else StarWhite, tl, body, CornerRadius(ts * 0.1f), style = Stroke(ts * 0.05f))
    }
    // Things for the fetch quests.
    things.forEach { (p, thing) ->
        val c = center(p) + Offset(0f, sin(clock * 2.5f + p.x) * ts * 0.04f)
        drawCircle(StarWhite.copy(alpha = 0.35f), radius = ts * 0.36f, center = c)
        text(thing.emoji, c, 0.5f)
    }
    // Patrolling security robots.
    ship.robots.forEach { r ->
        val pos = r.at(clock)
        val c = topLeft(pos.x + 0.5f, pos.y + 0.5f)
        drawCircle(MeteorRed.copy(alpha = 0.3f + 0.2f * sin(clock * 8f)), radius = ts * 0.48f, center = c)
        text("🤖", c, 0.55f)
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
    // Room names under the pads of the rooms you can reach, on a dark label drawn above everything
    // else so they stay readable when Lía and Nilo walk past.
    Station.entries.forEachIndexed { i, st ->
        if (i > reached) return@forEachIndexed
        val c = center(ship.pads[i]) + Offset(0f, ts * 0.8f)
        val layout = textMeasurer.measure(
            st.es,
            TextStyle(color = StarWhite, fontSize = (ts * 0.26f).toSp(), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            constraints = Constraints(maxWidth = (ts * 3f).toInt())
        )
        val pad = ts * 0.08f
        drawRoundRect(
            SpaceNavy.copy(alpha = 0.85f),
            topLeft = Offset(c.x - layout.size.width / 2f - pad * 2, c.y - layout.size.height / 2f - pad),
            size = Size(layout.size.width + pad * 4, layout.size.height + pad * 2),
            cornerRadius = CornerRadius(ts * 0.2f)
        )
        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
    }
    // What Lía is carrying floats above her head.
    carrying?.let { thing ->
        val head = topLeft(lia.x + 0.5f, lia.y + 0.9f) - Offset(0f, spriteH + ts * 0.15f)
        drawCircle(SolarGold.copy(alpha = 0.5f), radius = ts * 0.3f, center = head)
        text(thing.emoji, head, 0.42f)
    }
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
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, tablet.storyLines.getOrNull(i)?.meaning(language)) }
        }
        Station.OPENING -> tablet.opening.mapIndexed { i, f -> Step.Write("opening-$i", f) }
        Station.SEARCH -> if (scene == null || searchTargets.isEmpty()) listOf(Step.Note("🔍", "")) else listOf(Step.Search)
        Station.CONSOLE -> listOf(Step.Lesson, Step.Table)
        Station.ORDER -> listOf(Step.Order)
        Station.MISSION -> listOf(Step.Note("🛰️", language.pick(tablet.missionAr, tablet.mission))) +
            tablet.fields.mapIndexed { i, f -> Step.Choice("fields-$i", f.label, f.answers, language.pick(bidiSafe(f.hintAr), f.hint), choices.options(f.answers, tablet)) }
        Station.PORTAL -> storySentences(tablet.ending).let { s ->
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, tablet.endingLines.getOrNull(i)?.meaning(language)) }
        } + tablet.gate.mapIndexed { i, f ->
            Step.Falling("gate-$i", f.label, f.answers, language.pick(bidiSafe(f.hintAr), f.hint), choices.options(f.answers, tablet))
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
    level: Int,
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
        // Instructions: translated in the first levels; later in the target language (words already learnt),
        // with every word tappable and the translation one tap away.
        if (level <= 3) {
            Text("👉 " + station.how(language), color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
        } else {
            var translate by remember(station) { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(bottom = 6.dp)) {
                Box(Modifier.weight(1f)) { TappableSentence("👉 " + station.howEs, data, viewModel, language, fontSize = 13) }
                Text("🌐", fontSize = 18.sp, modifier = Modifier.clickable { translate = !translate }.padding(start = 6.dp))
            }
            if (translate) Text(station.how(language), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(station, index) {
                val scroll = rememberScrollState()
                // Word Jump fills the space itself (no scrolling, so nothing covers its buttons).
                if (step is Step.Choice) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoiceCard(step, results, viewModel, language)
                    }
                } else Column(Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (step) {
                        is Step.Sentence -> SentenceCard(step, data, viewModel, language)
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
                if (step !is Step.Choice) ScrollMoreHint(scroll, language, AdventureBg)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            // Back to the previous card, e.g. to read the story again before answering.
            if (index > 0) {
                OutlinedButton(
                    onClick = { index-- },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, accent),
                    modifier = Modifier.height(52.dp).padding(end = 8.dp)
                ) { Text(language.pick("◀ رجوع", "◀"), color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
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
            modifier = Modifier.weight(1f)
        )
        }
    }
}

/**
 * A sentence in the target language whose words can be tapped: the word is spoken and its meaning
 * (in Arabic or English) appears underneath, so beginners can follow every sentence.
 */
@Composable
private fun TappableSentence(text: String, data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage, fontSize: Int = 18, showTapHint: Boolean = false) {
    var picked by remember(text) { mutableStateOf<String?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        text.split(" ").filter { it.isNotBlank() }.forEachIndexed { i, token ->
            val clean = token.trim('.', ',', '¡', '!', '¿', '?', ':', ';', '"', '«', '»', '(', ')')
            val selected = picked == "$i:$clean"
            Text(
                token,
                color = if (selected) ExplorerBlue else TextPrimary,
                fontSize = fontSize.sp,
                lineHeight = (fontSize + 7).sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) ExplorerBlue.copy(alpha = 0.12f) else Color.Transparent)
                    // A dotted line under every word shows it can be tapped.
                    .drawBehind {
                        drawLine(
                            ExplorerBlue.copy(alpha = 0.5f),
                            Offset(0f, size.height - 1.dp.toPx()),
                            Offset(size.width, size.height - 1.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                        )
                    }
                    .clickable {
                        picked = "$i:$clean"
                        viewModel.speakSpanish(clean)
                    }
                    .padding(horizontal = 2.dp)
            )
        }
    }
    if (picked == null && showTapHint) {
        Text(language.pick("👆 اضغطي على أي كلمة لتعرفي معناها", "👆 Tap any word to see what it means"), color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
    picked?.substringAfter(':')?.let { word ->
        val meaning = data.glossary(word)?.meaning(language)
        Text(
            "🔎 $word = " + (meaning ?: language.pick("(لا توجد ترجمة لهذه الكلمة بعد)", "(no translation for this word yet)")),
            color = NebulaPurple, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/**
 * Shown before Lía boards the ship: what this chapter teaches (in Arabic or English), its new
 * words with their meanings, and how the map works. It can be opened again from the header.
 */
@Composable
private fun MissionBriefing(
    tablet: ReadingTablet,
    level: Int,
    data: SpanishContent,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onStart: () -> Unit
) {
    // The chapter's words that are in the dictionary, in story order.
    val words = remember(tablet.id) {
        "${tablet.story} ${tablet.ending}".split(" ")
            .map { it.trim('.', ',', '¡', '!', '¿', '?', ':', ';', '"').lowercase() }
            // Skip names and tiny function words; keep the words worth learning.
            .filter { it.length > 2 && it !in setOf("lía", "nilo", "los", "las", "una", "uno", "del", "con", "por", "gli", "che", "per", "una", "dei", "nel", "sul") }
            .distinct()
            .mapNotNull { w -> data.glossary(w)?.let { w to it } }
            .distinctBy { it.second.spanish }
            .take(12)
    }
    val examples = remember(tablet.id) { storySentences(tablet.lesson.example) }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize().background(AdventureBg)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🚀 ${language.pick("المستوى", tl("Nivel"))} $level · ${tablet.level}", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("🎯 " + bidiSafe(tablet.goal(language)), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                // 1. The rule, explained in the helper language.
                AdventureCard(borderColor = ExplorerBlue) {
                    Text(language.pick("📘 ماذا ستتعلمين", "📘 What you'll learn"), color = ExplorerBlue, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(language.pick(tablet.lesson.titleAr, tablet.lesson.title), color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(language.pick(bidiSafe(tablet.lesson.arabic), tablet.lesson.english), color = TextPrimary, fontSize = 15.sp, lineHeight = 23.sp)
                    // The forms to learn, as a clear table (tap a form to hear it).
                    if (tablet.table.rows.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ExplorerBlue.copy(alpha = 0.06f))
                                    .padding(8.dp)
                            ) {
                                Row {
                                    tablet.table.headers.forEach { h ->
                                        Text(h, color = ExplorerBlue, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                                tablet.table.rows.forEach { row ->
                                    Row(Modifier.padding(vertical = 3.dp)) {
                                        row.forEachIndexed { c, cell ->
                                            Text(
                                                cell,
                                                color = if (c == 0) TextSecondary else TextPrimary,
                                                fontWeight = if (c == 0) FontWeight.Normal else FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                modifier = Modifier.weight(1f).clickable { viewModel.speakSpanish(cell) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (examples.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(language.pick("💬 أمثلة — اضغطي على أي كلمة", "💬 Examples — tap any word"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        examples.forEach { sentence ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(Modifier.weight(1f)) { TappableSentence(sentence, data, viewModel, language, fontSize = 16) }
                                AudioButton(onClick = { viewModel.speakSpanish(sentence) }, size = 32.dp)
                            }
                        }
                    }
                }

                // 2. The words of this chapter.
                if (words.isNotEmpty()) {
                    AdventureCard(borderColor = SuccessGreen) {
                        Text(language.pick("🆕 كلمات هذه المهمة", "🆕 Words in this mission"), color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        words.forEach { (form, entry) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.speakSpanish(form) }.padding(vertical = 3.dp)
                            ) {
                                Text("🔊 $form", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                Text(entry.shortMeaning(language), color = TextSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // 3. How the ship works.
                AdventureCard(borderColor = SolarGold) {
                    Text(language.pick("🎮 كيف تلعبين", "🎮 How to play"), color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    listOf(
                        language.pick("1. حرّكي ليا بالأسهم ▲ ◀ ▼ ▶.", "1. Move Lía with the arrows ▲ ◀ ▼ ▶."),
                        language.pick("2. ادخلي الغرفة المضيئة وقفي على دائرتها لتبدأ اللعبة.", "2. Walk into the glowing room and step on its circle to start its game."),
                        language.pick("3. كل غرفة تنجحين فيها تفتح باب الغرفة التالية.", "3. Every room you finish opens the door to the next one."),
                        language.pick("4. بعد الغرفة الأخيرة خذي المفتاح 🔑 واخرجي من الباب 🚪.", "4. After the last room, take the key 🔑 and leave through the door 🚪."),
                        language.pick("💎 الإجابة الخاطئة تكلّف ماسة.", "💎 A wrong answer costs a diamond.")
                    ).forEach { Text(it, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp) }
                }
            }
            ScrollMoreHint(scroll, language, AdventureBg)
        }
        BlasterCyberButton(
            text = language.pick("🚀 ابدئي المهمة", "🚀 Start the mission"),
            onClick = onStart,
            color = SuccessGreen,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

/** One story sentence in Lía's speech bubble, read aloud. */
@Composable
private fun SentenceCard(step: Step.Sentence, data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage) {
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
                TappableSentence(step.text, data, viewModel, language, fontSize = 20, showTapHint = true)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${step.number} / ${step.total}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    AudioButton(onClick = { viewModel.speakSpanish(step.text) }, size = 40.dp)
                }
            }
        }
    }
    // Translation of this sentence only (not the whole story).
    if (!step.translation.isNullOrBlank()) {
        TextButton(onClick = { showTranslation = !showTranslation }) {
            Text(
                if (showTranslation) language.pick("إخفاء الترجمة", "Hide translation")
                else language.pick("🌐 ترجمة هذه الجملة", "🌐 Translate this sentence"),
                color = ExplorerBlue, fontWeight = FontWeight.Bold
            )
        }
        if (showTranslation) {
            Text(step.translation, color = TextPrimary, fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** A question answered by jumping onto the right word bubble (Word Jump). Wrong jumps cost a diamond. */
@Composable
private fun ColumnScope.ChoiceCard(step: Step.Choice, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val onMistake = LocalOnMistake.current
    val wrong = remember { mutableStateListOf<String>() }
    val accepted = remember(step) { step.answers.flatMap { it.split("/") }.map { normalizeAnswer(it) } }
    val solved = step.key in results
    AdventureCard(borderColor = if (solved) SuccessGreen else ExplorerBlue) {
        Text(step.prompt, color = TextPrimary, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
    // The tip is shown from the start: it explains the question in the helper language.
    if (step.hint.isNotBlank()) {
        Text("💡 " + step.hint, color = SolarAmber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.fillMaxWidth())
    }
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
    WordJump(
        options = step.options,
        language = language,
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
        },
        boardHeight = maxHeight
    )
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
        MeteorStarField(Modifier.matchParentSize())
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
    if (step.hint.isNotBlank()) {
        Text("💡 " + step.hint, color = SolarAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
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
            "👆 قرّبي بإصبعين أو اضغطي مرتين للتكبير · اضغطي على الكلمة لسماعها.",
            "👆 Pinch or double-tap to zoom · tap a word to hear it."
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
