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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.compositeOver
import com.example.data.ship.*
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

// --------------------------------------------------------------------------- Ship levels (see data/ship)

private const val STEP_TIME = 0.16f   // seconds to walk one tile
private const val ZOOM = 1.3f         // how much closer than "whole ship on screen"

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

/** Bright, clear colours for the coloured keys and doors (red, blue, green, yellow). */
private val KEY_TINTS = listOf(Color(0xFFE5484D), Color(0xFF1E6FE0), Color(0xFF2E9F5B), Color(0xFFF2B53A))

/** Which learning room each floor tile belongs to (rooms are flooded out from their pads). */
private fun roomsOf(level: ShipLevel): Map<Cell, Int> {
    val out = HashMap<Cell, Int>()
    val stop = "#RBGYH=-"
    level.pads.forEachIndexed { i, pad ->
        val stack = ArrayDeque(listOf(pad))
        while (stack.isNotEmpty()) {
            val p = stack.removeLast()
            if (p in out || level.at(p) in stop || level.at(p) in "TU") continue
            out[p] = i
            for (d in listOf(UP, DOWN, LEFT, RIGHT)) stack.addLast(p + d)
        }
    }
    return out
}

@Composable
fun ExpeditionScreen(tablet: ReadingTablet, data: SpanishContent, viewModel: BlasterViewModel, language: HelperLanguage) {
    val walkSprite = ImageBitmap.imageResource(R.drawable.explorer_walk)
    val niloSprite = ImageBitmap.imageResource(R.drawable.nilo_walk)
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val chapterNumber = data.tablets.indexOf(tablet) + 1
    val levels = remember {
        runCatching { ShipLevel.parseAll(context.assets.open("ship_levels.json").bufferedReader().use { it.readText() }) }.getOrDefault(emptyList())
    }
    if (levels.isEmpty()) return LoadingContent(language == HelperLanguage.ARABIC)
    val level = levels[(chapterNumber - 1).mod(levels.size)]
    val rooms = remember(level) { roomsOf(level) }

    // Fetch quest: a thing for each fetch spot (from units with things you can carry); one is wanted.
    val things = remember(tablet.id) {
        val carryable = data.course.filter { it.id in setOf("u6", "u7", "u8", "u10", "u12") }.flatMap { it.words }
        carryable.ifEmpty { data.courseWords }.distinctBy { it.emoji }.shuffled().take(level.fetchSpots.size)
    }
    val wanted = remember(tablet.id) { if (things.isEmpty()) 0 else things.indices.random() }

    // The game itself: an immutable state, a list for undo, and the last checkpoint.
    var state by remember(tablet.id) { mutableStateOf(ShipRules.start(level)) }
    val history = remember(tablet.id) { mutableStateListOf<ShipState>() }
    var checkpoint by remember(tablet.id) { mutableStateOf(state) }
    // Animation between tiles.
    var fromPos by remember(tablet.id) { mutableStateOf(level.start) }
    var fromRobots by remember(tablet.id) { mutableStateOf(level.robots.map { it.at }) }
    var niloFrom by remember(tablet.id) { mutableStateOf(level.start) }
    var niloTo by remember(tablet.id) { mutableStateOf(level.start) }
    var progress by remember { mutableFloatStateOf(1f) }
    var pending by remember { mutableStateOf<ShipEvent?>(null) }
    var held by remember { mutableStateOf<Cell?>(null) }
    var clock by remember { mutableFloatStateOf(0f) }
    var lastBumpAt by remember { mutableFloatStateOf(-10f) }

    // Learning rooms and diamonds.
    val solvedAt = remember { mutableStateMapOf<Int, Float>() }
    // Each wrong answer costs one diamond, so diamonds are worth protecting.
    var lostDiamonds by remember { mutableIntStateOf(0) }
    var lostAt by remember { mutableFloatStateOf(-10f) }
    // With no diamonds left, a mistake pauses the game until a short video earns one back.
    var adDiamonds by remember { mutableIntStateOf(0) }
    var outOfDiamonds by remember { mutableStateOf(false) }
    var adMisses by remember { mutableIntStateOf(0) }
    var showStory by remember { mutableStateOf(false) }
    var showTip by remember { mutableStateOf(false) }
    // Every chapter starts with its lesson and words, before the ship.
    var briefed by remember { mutableStateOf(false) }
    val results = remember { mutableStateMapOf<String, Boolean>() }
    var openStation by remember { mutableStateOf<Int?>(null) }
    var finished by remember { mutableStateOf(false) }

    // Nilo, Lía's co-pilot: walks one step behind her and only speaks when it matters.
    var niloLine by remember { mutableStateOf<NiloLine?>(null) }
    var niloLineUntil by remember { mutableFloatStateOf(0f) }

    fun say(line: NiloLine, speak: Boolean = true) {
        niloLine = line
        niloLineUntil = clock + 2.5f
        if (speak) viewModel.speakSpanish(line.es)
    }
    /** A picture-only bubble (🔒, ⚡…): clear without reading. */
    fun show(icon: String) {
        niloLine = NiloLine(icon, icon, icon)
        niloLineUntil = clock + 1.4f
    }

    val scene = data.scenes[tablet.scene]
    val searchTargets = remember(tablet.id) { tablet.targets.mapNotNull { id -> scene?.objects?.firstOrNull { it.id == id } } }
    val tableCells = remember(tablet.id) {
        tablet.table.rows.indices.flatMap { r -> (1 until tablet.table.headers.size).map { c -> r to c } }
            .shuffled().take(4).toSet()
    }
    val totalQuestions = tablet.allQuestions.size + tableCells.size + 1 + searchTargets.size
    val nextStation = state.solved.takeIf { it < 7 }
    // The last sentence of the chapter, revealed word by word as rooms are won.
    val secretWords = remember(tablet.id) {
        tablet.ending.split(Regex("(?<=[.!?])\\s+")).lastOrNull { it.isNotBlank() }.orEmpty().split(" ").filter { it.isNotBlank() }
    }
    val secretOrder = remember(tablet.id) { secretWords.indices.shuffled(kotlin.random.Random(tablet.id.hashCode())) }
    val diamonds = (state.diamonds.size + adDiamonds - lostDiamonds).coerceAtLeast(0)

    fun loseDiamond() {
        if (state.diamonds.size + adDiamonds - lostDiamonds > 0) {
            lostDiamonds++
            lostAt = clock
        }
    }

    fun backToCheckpoint() {
        state = checkpoint
        history.clear()
        fromPos = checkpoint.pos
        fromRobots = checkpoint.robots.map { it.at }
        niloFrom = checkpoint.pos
        niloTo = checkpoint.pos
        progress = 1f
        pending = null
    }

    /** Lía has arrived on her tile: open a room's game, ask for a thing, or finish the level. */
    fun arrived(event: ShipEvent?) {
        when (event) {
            is ShipEvent.RoomOpen -> {
                if (event.room == level.fetchPad) say(NiloLines.thanks)
                openStation = event.room
                held = null
                viewModel.soundEngine.powerUp()
            }
            is ShipEvent.NeedThing -> {
                val want = things.getOrNull(wanted)
                if (want != null) {
                    viewModel.soundEngine.error()
                    say(
                        if (event.carrying >= 0) NiloLine("${tl("No, eso no es")} ${want.word}.", "", "")
                        else NiloLine("${tl("Necesito")} ${want.word}.", "", "")
                    )
                }
            }
            ShipEvent.Win -> if (!finished) {
                finished = true
                held = null
                say(NiloLines.home)
                val kept = (state.diamonds.size + adDiamonds - lostDiamonds).coerceAtLeast(0)
                viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions, bonusCredits = kept * 5)
            }
            else -> Unit
        }
    }

    fun tryStep(dir: Cell) {
        val result = ShipRules.step(level, state, dir, wanted)
        when (val e = result.event) {
            is ShipEvent.Blocked -> {
                state = result.state
                if (clock - lastBumpAt > 1f) {
                    lastBumpAt = clock
                    viewModel.soundEngine.error()
                    // Say it with a picture: what this obstacle needs.
                    when {
                        e.tile in DOOR_COLORS -> show("🔒")
                        e.tile == 'H' -> show("🔋 ➜ ⚡")
                        e.tile == '=' || e.tile == '-' -> show("🔘 ➜ ⚡")
                        e.tile == 'c' -> show("🔋 ✋")
                    }
                }
            }
            ShipEvent.Hit -> {
                // A robot caught Lía: −1 diamond and back to the last checkpoint.
                viewModel.soundEngine.error()
                loseDiamond()
                backToCheckpoint()
                say(NiloLines.robot)
            }
            else -> {
                history += state
                if (history.size > 300) history.removeAt(0)
                niloFrom = niloTo
                niloTo = state.pos
                fromPos = state.pos
                fromRobots = state.robots.map { it.at }
                state = result.state
                when {
                    result.gotKey != null || result.openedDoor || result.teleported || result.switched -> viewModel.soundEngine.powerUp()
                    result.gotDiamond || result.pushed || result.pickedThing -> viewModel.soundEngine.click()
                }
                result.pickedThing.takeIf { it }?.let {
                    things.getOrNull(state.carry)?.let { t -> say(NiloLine("🙌 ${t.word}", "", "")) }
                }
                if (result.teleported) {
                    fromPos = state.pos
                    niloFrom = state.pos
                    niloTo = state.pos
                    progress = 1f
                    arrived(e)
                } else {
                    progress = 0f
                    pending = e
                }
            }
        }
    }

    // Game loop: moves Lía one tile at a time while an arrow is held (turn based: nothing else moves on its own).
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
                if (progress >= 1f) {
                    val e = pending
                    pending = null
                    arrived(e)
                }
                continue
            }
            if (openStation != null || finished || !briefed) continue
            held?.let { tryStep(it) }
        }
    }

    fun solve(room: Int) {
        state = ShipRules.solveRoom(level, state, room)
        checkpoint = state
        history.clear()
        solvedAt[room] = clock
        openStation = null
        viewModel.soundEngine.fanfare()
        // The reward key appears above Lía for a moment.
        level.rewards.getOrNull(room)?.takeIf { it in KEY_COLORS }?.let { show("🔑 +1") }
    }

    val onMistake: () -> Unit = {
        if (state.diamonds.size + adDiamonds - lostDiamonds > 0) {
            lostDiamonds++
            lostAt = clock
        } else {
            outOfDiamonds = true
        }
    }

    Column(Modifier.fillMaxSize().background(AdventureBg)) {
        // Header: chapter, the lesson and story buttons, and the diamonds still held.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { briefed = false }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(language.pick("📘 الدرس", "📘 Lesson"), color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            TextButton(onClick = { showStory = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text(language.pick("📖 القصة", "📖 Story"), color = ExplorerBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text("💎 $diamonds", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                if (clock - lostAt < 1.5f) Text("−1", color = MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        val station = openStation
        if (!briefed) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                MissionBriefing(tablet, chapterNumber, level, data, viewModel, language, onStart = { briefed = true })
            }
        } else if (station == null) {
            GoalStrip(
                tablet = tablet,
                language = language,
                progress = state.solved,
                secret = secretWords,
                secretOrder = secretOrder,
                newPiece = solvedAt.values.any { clock - it < 4f },
                onSpeak = { viewModel.speakSpanish(it) }
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                // Clipped so the zoomed-in ship never draws over the goal text above it.
                Canvas(Modifier.fillMaxSize().clipToBounds()) {
                    val t = progress
                    drawShip(
                        level = level,
                        state = state,
                        rooms = rooms,
                        walkSprite = walkSprite,
                        niloSprite = niloSprite,
                        textMeasurer = textMeasurer,
                        clock = clock,
                        lia = lerpCell(fromPos, state.pos, t),
                        walking = t < 1f,
                        nilo = lerpCell(niloFrom, niloTo, t),
                        robots = state.robots.mapIndexed { i, r -> lerpCell(fromRobots.getOrElse(i) { r.at }, r.at, t) },
                        niloSays = niloLine?.es,
                        solvedAt = solvedAt,
                        thingEmoji = { things.getOrNull(it)?.emoji ?: "🎁" },
                        wantedEmoji = things.getOrNull(wanted)?.emoji
                    )
                }
                // Keys Lía holds, and the thing she carries.
                val heldKeys = state.keys.withIndex().filter { it.value > 0 }
                if (heldKeys.isNotEmpty() || state.carry >= 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceNavy.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        heldKeys.forEach { (i, n) ->
                            Canvas(Modifier.size(22.dp)) { drawKeyIcon(KEY_TINTS[i], center, size.minDimension) }
                            if (n > 1) Text("×$n", color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        things.getOrNull(state.carry)?.let { Text("🙌 ${it.emoji}", color = SolarGold, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
                    }
                }
                // The current fetch request, in the target language (tap to hear it).
                val want = things.getOrNull(wanted)
                if (want != null && nextStation == level.fetchPad) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SpaceNavy.copy(alpha = 0.85f))
                            .clickable { viewModel.speakSpanish(want.word) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("🛰️ ${tl("Necesito")}: ${want.word} 🔊", color = StarWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
            ShipControls(
                canUndo = history.isNotEmpty(),
                onHold = { held = it },
                onUndo = {
                    history.removeLastOrNull()?.let { previous ->
                        state = previous
                        fromPos = previous.pos
                        fromRobots = previous.robots.map { it.at }
                        niloFrom = previous.pos
                        niloTo = previous.pos
                        progress = 1f
                        pending = null
                    }
                },
                onRestart = { backToCheckpoint() },
                onTip = { showTip = true }
            )
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

    if (showTip) {
        AlertDialog(
            onDismissRequest = { showTip = false },
            containerColor = AdventureSurface,
            title = { Text(language.pick("💡 تلميح", "💡 Tip"), color = SolarAmber, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(language.pick(bidiSafe(level.newAr), level.newEn), color = TextPrimary, fontSize = 16.sp, lineHeight = 23.sp)
                    Text(
                        language.pick(
                            "↶ تراجعي خطوة · ↺ عودي إلى آخر غرفة · الروبوتات تتحرك فقط عندما تتحركين.",
                            "↶ undo a step · ↺ back to the last room · robots only move when you move."
                        ),
                        color = TextSecondary, fontSize = 13.sp
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showTip = false }) { Text(language.pick("حسنًا", "OK")) } }
        )
    }

    if (outOfDiamonds) {
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

private fun lerpCell(from: Cell, to: Cell, t: Float) = Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)

/** ▲ ◀ ▼ ▶ in the middle (hold to keep walking), undo on the left, tip and restart on the right. */
@Composable
private fun ShipControls(canUndo: Boolean, onHold: (Cell?) -> Unit, onUndo: () -> Unit, onRestart: () -> Unit, onTip: () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth().background(AdventureBg).padding(vertical = 6.dp)
        ) {
            PadButton("↶", if (canUndo) NebulaPurple else NebulaPurple.copy(alpha = 0.35f), size = 50.dp, onPress = { if (canUndo) onUndo() })
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PadButton("▲", ExplorerBlue, size = 54.dp, onPress = { onHold(UP) }, onRelease = { onHold(null) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PadButton("◀", ExplorerBlue, size = 54.dp, onPress = { onHold(LEFT) }, onRelease = { onHold(null) })
                    PadButton("▼", ExplorerBlue, size = 54.dp, onPress = { onHold(DOWN) }, onRelease = { onHold(null) })
                    PadButton("▶", ExplorerBlue, size = 54.dp, onPress = { onHold(RIGHT) }, onRelease = { onHold(null) })
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PadButton("💡", SolarGold, size = 46.dp, onPress = onTip)
                PadButton("↺", MeteorRed, size = 46.dp, onPress = onRestart)
            }
        }
    }
}

// --------------------------------------------------------------------------- Drawing

private val HULL = Color(0xFF1E2A48)
private val HULL_LIGHT = Color(0xFF2E3D63)
// Bright, Chip's-Challenge-like tiles: light floor plates, strong blue walls with bevels.
private val FLOOR = Color(0xFFE3E9F3)
private val FLOOR_LIGHT = Color(0xFFFFFFFF)
private val FLOOR_SHADE = Color(0xFFB4BED0)
private val WALL = Color(0xFF3D5A99)
private val WALL_LIGHT = Color(0xFF7193DC)
private val WALL_SHADE = Color(0xFF263B68)

/** Fixed star field (fractions of the view), so it doesn't flicker between frames. */
private val SPACE_STARS: List<Offset> = List(80) { i ->
    val r = kotlin.random.Random(i * 7919 + 13)
    Offset(r.nextFloat(), r.nextFloat())
}

/** A key drawn as a shape (ring, shaft and teeth), so every colour is clear. */
private fun DrawScope.drawKeyIcon(color: Color, c: Offset, s: Float) {
    val ring = s * 0.22f
    val ringCenter = c + Offset(-s * 0.18f, 0f)
    drawCircle(color, radius = ring, center = ringCenter)
    drawCircle(Color.White.copy(alpha = 0.9f), radius = ring * 0.45f, center = ringCenter)
    drawRect(color, topLeft = Offset(ringCenter.x + ring * 0.8f, c.y - s * 0.06f), size = Size(s * 0.42f, s * 0.12f))
    drawRect(color, topLeft = Offset(c.x + s * 0.12f, c.y), size = Size(s * 0.08f, s * 0.16f))
    drawRect(color, topLeft = Offset(c.x + s * 0.26f, c.y), size = Size(s * 0.08f, s * 0.12f))
    drawCircle(Color.Black.copy(alpha = 0.25f), radius = ring, center = ringCenter, style = Stroke(s * 0.04f))
}

private fun DrawScope.drawShip(
    level: ShipLevel,
    state: ShipState,
    rooms: Map<Cell, Int>,
    walkSprite: ImageBitmap,
    niloSprite: ImageBitmap,
    textMeasurer: TextMeasurer,
    clock: Float,
    lia: Offset,
    walking: Boolean,
    nilo: Offset,
    robots: List<Offset>,
    niloSays: String?,
    solvedAt: Map<Int, Float>,
    thingEmoji: (Int) -> String,
    wantedEmoji: String?
) {
    val mapW = level.width
    val mapH = level.height
    // Space all around the ship.
    drawRect(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
    SPACE_STARS.forEachIndexed { i, s ->
        drawCircle(StarWhite.copy(alpha = 0.35f + 0.3f * sin(clock * 1.5f + i)), radius = 1.6f + (i % 3), center = Offset(s.x * size.width, s.y * size.height))
    }

    // A little closer than the whole ship: the camera follows Lía and stops at the ship's edges.
    val ts = minOf(size.width / (mapW + 3f), size.height / (mapH + 2.4f)) * ZOOM
    fun follow(view: Float, total: Float, margin: Float, focus: Float): Float {
        if (total <= view) return (view - total) / 2f + margin
        return (view / 2f - focus).coerceIn(view - total + margin, margin)
    }
    val ox = follow(size.width, ts * (mapW + 3f), ts * 1.5f, (lia.x + 0.5f) * ts)
    val oy = follow(size.height, ts * (mapH + 2.4f), ts * 1.5f, (lia.y + 0.5f) * ts)
    fun topLeft(x: Number, y: Number) = Offset(ox + x.toFloat() * ts, oy + y.toFloat() * ts)
    fun center(p: Cell) = topLeft(p.x + 0.5f, p.y + 0.5f)
    fun text(s: String, c: Offset, scale: Float, color: Color = Color.White, maxWidth: Float? = null) {
        val layout = textMeasurer.measure(
            s,
            TextStyle(color = color, fontSize = (ts * scale).toSp(), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
            constraints = maxWidth?.let { Constraints(maxWidth = it.toInt()) } ?: Constraints()
        )
        drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
    }
    val shipW = ts * mapW
    val shipH = ts * mapH

    // Engines, wings and nose around the hull.
    listOf(mapW * 0.27f, mapW * 0.73f).forEachIndexed { k, ex ->
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
    for (side in listOf(-1f, 1f)) {
        val edge = if (side < 0) ox else ox + shipW
        val wing = Path().apply {
            moveTo(edge, oy + shipH * 0.4f)
            lineTo(edge + side * ts * 1.5f, oy + shipH * 0.7f)
            lineTo(edge + side * ts * 1.5f, oy + shipH * 0.92f)
            lineTo(edge, oy + shipH * 0.92f)
            close()
        }
        drawPath(wing, HULL)
        drawPath(wing, DiamondCyan.copy(alpha = 0.5f), style = Stroke(ts * 0.06f))
        drawCircle(if (side < 0) MeteorRed else SuccessGreen, radius = ts * 0.15f * (0.7f + 0.3f * sin(clock * 4f)), center = Offset(edge + side * ts * 1.5f, oy + shipH * 0.7f))
    }
    drawOval(HULL, topLeft = Offset(ox, oy - ts * 1.5f), size = Size(shipW, ts * 3f))
    drawOval(
        Brush.verticalGradient(listOf(DiamondCyan, ExplorerBlue), startY = oy - ts * 1.2f, endY = oy - ts * 0.4f),
        topLeft = Offset(ox + shipW / 2 - ts * 1.6f, oy - ts * 1.2f), size = Size(ts * 3.2f, ts * 0.9f)
    )
    drawRoundRect(HULL, topLeft = Offset(ox, oy), size = Size(shipW, shipH), cornerRadius = CornerRadius(ts * 0.6f))

    val next = state.solved
    val gateOpen = ShipRules.gateOpen(level, state)
    fun bevel(tl: Offset, base: Color, light: Color, shade: Color) {
        val tile = Size(ts, ts)
        drawRect(base, tl, tile)
        val b = ts * 0.1f
        drawRect(light, tl, Size(ts, b))
        drawRect(light, tl, Size(b, ts))
        drawRect(shade, tl + Offset(0f, ts - b), Size(ts, b))
        drawRect(shade, tl + Offset(ts - b, 0f), Size(b, ts))
    }

    level.cells().forEach { p ->
        val c = level.at(p)
        val tl = topLeft(p.x, p.y)
        val tile = Size(ts, ts)
        if (c == '#') {
            bevel(tl, WALL, WALL_LIGHT, WALL_SHADE)
            return@forEach
        }
        // Floor plate, tinted with its room's colour (rooms not reached yet are a little darker).
        bevel(tl, FLOOR, FLOOR_LIGHT, FLOOR_SHADE)
        rooms[p]?.let { room ->
            drawRect(Station.entries[room].color.copy(alpha = 0.22f), tl, tile)
            if (room > next) drawRect(SpaceNavy.copy(alpha = 0.12f), tl, tile)
        }
        when (c) {
            in DOOR_COLORS -> if (p !in state.opened) {
                val color = KEY_TINTS[DOOR_COLORS.indexOf(c)]
                bevel(tl, color, color.copy(alpha = 0.6f).compositeOver(Color.White), color.copy(alpha = 0.7f).compositeOver(Color.Black))
                // Keyhole.
                drawCircle(Color.Black.copy(alpha = 0.7f), radius = ts * 0.11f, center = center(p) - Offset(0f, ts * 0.06f))
                drawRect(Color.Black.copy(alpha = 0.7f), topLeft = center(p) + Offset(-ts * 0.05f, -ts * 0.04f), size = Size(ts * 0.1f, ts * 0.22f))
            }
            in KEY_COLORS -> if (p !in state.takenKeys) {
                val bob = sin(clock * 3f + p.x) * ts * 0.04f
                drawKeyIcon(KEY_TINTS[KEY_COLORS.indexOf(c)], center(p) + Offset(0f, bob), ts * 0.8f)
            }
            'o' -> {
                drawCircle(DiamondCyan, radius = ts * 0.36f, center = center(p), style = Stroke(ts * 0.08f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ts * 0.12f, ts * 0.08f))))
                text("⚡", center(p), 0.32f)
            }
            'H' -> if (!gateOpen) {
                // Closed gate: yellow and black stripes.
                drawRect(SolarGold, tl, tile)
                clipRect(tl.x, tl.y, tl.x + ts, tl.y + ts) {
                    for (k in -2..4) {
                        val x0 = tl.x + k * ts * 0.3f
                        drawLine(Color(0xFF222222), Offset(x0, tl.y + ts), Offset(x0 + ts * 0.6f, tl.y), strokeWidth = ts * 0.12f)
                    }
                }
            } else {
                drawRect(SolarGold.copy(alpha = 0.5f), tl, tile, style = Stroke(ts * 0.06f))
            }
            'T', 'U' -> {
                val color = if (c == 'T') ExplorerBlue else NebulaPurple
                val pulse = 0.5f + 0.5f * sin(clock * 4f)
                for (k in 0..2) {
                    val inset = ts * (0.1f + k * 0.13f + 0.03f * pulse)
                    drawRect(color.copy(alpha = 0.9f - k * 0.2f), tl + Offset(inset, inset), Size(ts - inset * 2, ts - inset * 2), style = Stroke(ts * 0.07f))
                }
            }
            'x' -> {
                drawCircle(Color(0xFF555F73), radius = ts * 0.36f, center = center(p))
                drawCircle(if (state.toggled) SuccessGreen else MeteorRed, radius = ts * 0.26f, center = center(p))
                drawCircle(Color.White.copy(alpha = 0.5f), radius = ts * 0.1f, center = center(p) - Offset(ts * 0.08f, ts * 0.08f))
            }
            '=', '-' -> if (ShipRules.fieldOn(level, state, p)) {
                // Force field: glowing violet bars.
                drawRect(NebulaPurple.copy(alpha = 0.55f), tl, tile)
                for (k in 0..3) {
                    val x = tl.x + ts * (0.15f + k * 0.23f) + sin(clock * 6f + k) * ts * 0.03f
                    drawLine(Color.White.copy(alpha = 0.8f), Offset(x, tl.y + ts * 0.08f), Offset(x, tl.y + ts * 0.92f), strokeWidth = ts * 0.05f)
                }
            } else {
                drawRect(NebulaPurple.copy(alpha = 0.6f), tl + Offset(ts * 0.08f, ts * 0.08f), Size(ts * 0.84f, ts * 0.84f), style = Stroke(ts * 0.04f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(ts * 0.1f, ts * 0.08f))))
            }
            'f' -> {
                val i = level.fetchSpots.indexOf(p)
                if (i in state.items) {
                    val bob = sin(clock * 2.5f + p.x) * ts * 0.04f
                    drawCircle(Color.White, radius = ts * 0.38f, center = center(p) + Offset(0f, bob))
                    drawCircle(SolarGold, radius = ts * 0.38f, center = center(p) + Offset(0f, bob), style = Stroke(ts * 0.05f))
                    text(thingEmoji(i), center(p) + Offset(0f, bob), 0.46f)
                }
            }
            '*' -> if (p !in state.diamonds) {
                val bob = sin(clock * 3f + p.x + p.y) * ts * 0.05f
                val cc = center(p) + Offset(0f, bob)
                drawCircle(SolarGold.copy(alpha = 0.3f), radius = ts * 0.3f, center = cc)
                rotate(45f, pivot = cc) { drawRect(SolarGold, cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f)) }
                rotate(45f, pivot = cc) { drawRect(Color.White.copy(alpha = 0.8f), cc - Offset(ts * 0.15f, ts * 0.15f), Size(ts * 0.3f, ts * 0.3f), style = Stroke(ts * 0.04f)) }
            }
            in '0'..'6' -> {
                val i = c - '0'
                val st = Station.entries[i]
                val done = i < next
                val active = i == next
                if (active) {
                    val pulse = 0.5f + 0.5f * sin(clock * 4f)
                    drawCircle(st.color.copy(alpha = 0.3f + 0.3f * pulse), radius = ts * (0.55f + 0.12f * pulse), center = center(p))
                }
                val age = clock - (solvedAt[i] ?: -10f)
                if (done && age < 1.2f) {
                    drawCircle(SolarGold.copy(alpha = (1.2f - age) / 1.2f), radius = ts * (0.5f + age * 2.5f), center = center(p), style = Stroke(ts * 0.08f))
                }
                drawCircle(st.color.copy(alpha = if (done || active) 1f else 0.45f), radius = ts * 0.42f, center = center(p))
                drawCircle(Color.White, radius = ts * 0.42f, center = center(p), style = Stroke(ts * 0.06f))
                text(if (done) "✓" else st.emoji, center(p), 0.38f)
                // What this room gives (its key colour) or needs (the fetch thing).
                val corner = center(p) + Offset(ts * 0.38f, -ts * 0.38f)
                if (!done && i == level.fetchPad && wantedEmoji != null) {
                    drawCircle(Color.White, radius = ts * 0.24f, center = corner)
                    text(wantedEmoji, corner, 0.28f)
                } else if (!done) {
                    level.rewards.getOrNull(i)?.takeIf { it in KEY_COLORS }?.let { r ->
                        drawCircle(Color.White, radius = ts * 0.24f, center = corner)
                        drawKeyIcon(KEY_TINTS[KEY_COLORS.indexOf(r)], corner, ts * 0.42f)
                    }
                }
            }
            'E' -> {
                val open = next >= 7
                drawRoundRect(if (open) SolarGold else Color(0xFF8B93A6), tl + Offset(ts * 0.08f, ts * 0.08f), Size(ts * 0.84f, ts * 0.84f), CornerRadius(ts * 0.2f))
                text(if (open) "🚪" else "🔒", center(p), 0.42f)
            }
            'P' -> drawCircle(DiamondCyan.copy(alpha = 0.5f), radius = ts * 0.3f, center = center(p), style = Stroke(ts * 0.05f))
        }
    }

    // Batteries: one red bar while empty, full and green on a charger.
    state.crates.forEach { p ->
        val tl = topLeft(p.x, p.y) + Offset(ts * 0.18f, ts * 0.14f)
        val charging = level.at(p) == 'o'
        val body = Size(ts * 0.64f, ts * 0.76f)
        drawRect(HULL_LIGHT, tl + Offset(ts * 0.22f, -ts * 0.08f), Size(ts * 0.2f, ts * 0.1f))
        drawRoundRect(Color(0xFF2B2F3A), tl, body, CornerRadius(ts * 0.1f))
        repeat(if (charging) 3 else 1) { k ->
            drawRoundRect(if (charging) SuccessGreen else MeteorRed, tl + Offset(ts * 0.1f, ts * (0.54f - k * 0.2f)), Size(ts * 0.44f, ts * 0.14f), CornerRadius(ts * 0.04f))
        }
        drawRoundRect(if (charging) SuccessGreen else StarWhite, tl, body, CornerRadius(ts * 0.1f), style = Stroke(ts * 0.05f))
    }
    // Security robots, with an arrow showing where they go next.
    robots.forEachIndexed { i, pos ->
        val c = topLeft(pos.x + 0.5f, pos.y + 0.5f)
        drawCircle(MeteorRed.copy(alpha = 0.25f), radius = ts * 0.46f, center = c)
        text("🤖", c, 0.55f)
        state.robots.getOrNull(i)?.dir?.let { d ->
            val tip = c + Offset(d.x * ts * 0.55f, d.y * ts * 0.55f)
            val side = Offset(-d.y * ts * 0.12f, d.x * ts * 0.12f)
            val back = tip - Offset(d.x * ts * 0.18f, d.y * ts * 0.18f)
            drawPath(Path().apply {
                moveTo(tip.x, tip.y); lineTo(back.x + side.x, back.y + side.y); lineTo(back.x - side.x, back.y - side.y); close()
            }, MeteorRed)
        }
    }

    // Room names under the pads of the rooms you can reach, on a dark label.
    Station.entries.forEachIndexed { i, st ->
        if (i > next) return@forEachIndexed
        val c = center(level.pads[i]) + Offset(0f, ts * 0.8f)
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

    // Characters: whoever is lower on the screen is drawn last, in front.
    val spriteH = ts * 1.45f
    val facing = state.facing.x.toFloat().takeIf { it != 0f } ?: 1f
    val niloFacing = if (lia.x >= nilo.x) 1f else -1f
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
    // What Lía is carrying floats above her head.
    if (state.carry >= 0) {
        val head = topLeft(lia.x + 0.5f, lia.y + 0.9f) - Offset(0f, spriteH + ts * 0.15f)
        drawCircle(SolarGold.copy(alpha = 0.5f), radius = ts * 0.3f, center = head)
        text(thingEmoji(state.carry), head, 0.42f)
    }
    niloSays?.let {
        val head = topLeft(nilo.x + 0.5f, nilo.y + 0.9f) - Offset(0f, spriteH + 4f)
        drawBubble(textMeasurer, it, head.x, head.y.coerceAtLeast(40f))
    }
}

/** Speech bubble whose bottom edge sits at [bottomY], centred on [centerX]. */
private fun DrawScope.drawBubble(textMeasurer: TextMeasurer, text: String, centerX: Float, bottomY: Float) {
    // Long lines wrap, and the bubble is kept fully inside the map.
    val margin = 8.dp.toPx()
    val layout = textMeasurer.measure(
        text,
        TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        constraints = Constraints(maxWidth = (size.width * 0.8f).toInt())
    )
    val w = layout.size.width + 16.dp.toPx()
    val h = layout.size.height + 10.dp.toPx()
    val left = (centerX - w / 2).coerceIn(margin, (size.width - w - margin).coerceAtLeast(margin))
    val top = (bottomY - h).coerceAtLeast(margin)
    drawRoundRect(AdventureSurface, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9.dp.toPx()))
    drawRoundRect(SolarAmber, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9.dp.toPx()), style = Stroke(2.dp.toPx()))
    drawText(layout, topLeft = Offset(left + w / 2 - layout.size.width / 2f, top + h / 2 - layout.size.height / 2f))
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
    ship: ShipLevel,
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

                // 3. What's new on this ship (one new idea per level).
                if (ship.newEn.isNotBlank()) {
                    AdventureCard(borderColor = NebulaPurple) {
                        Text(language.pick("🆕 جديد في هذا المستوى", "🆕 New in this level"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text(language.pick(bidiSafe(ship.newAr), ship.newEn), color = TextPrimary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }

                // 4. How the ship works.
                AdventureCard(borderColor = SolarGold) {
                    Text(language.pick("🎮 كيف تلعبين", "🎮 How to play"), color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    listOf(
                        language.pick("1. حرّكي ليا بالأسهم ▲ ◀ ▼ ▶. لا يوجد وقت: الروبوتات تتحرك فقط عندما تتحركين.", "1. Move Lía with the arrows ▲ ◀ ▼ ▶. No timer: robots only move when you move."),
                        language.pick("2. قفي على دائرة الغرفة المضيئة لتبدأ لعبتها.", "2. Step on the glowing room's circle to start its game."),
                        language.pick("3. كل غرفة تنجحين فيها تعطيك مفتاحًا ملوّنًا يفتح الباب الذي بلونه.", "3. Every room you finish gives a coloured key for the door of the same colour."),
                        language.pick("4. بعد الغرفة الأخيرة اخرجي من الباب 🚪.", "4. After the last room, leave through the door 🚪."),
                        language.pick("↶ تراجعي خطوة · ↺ عودي إلى آخر غرفة · 💡 تلميح", "↶ undo a step · ↺ back to the last room · 💡 tip"),
                        language.pick("💎 الإجابة الخاطئة أو الروبوت يكلّفانك ماسة.", "💎 A wrong answer or a robot costs a diamond.")
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
