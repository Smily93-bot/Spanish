package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.BorderStroke
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

// World units: the ship deck is seen from above and always fits the screen.
private const val DECK_W = 360f
private const val DECK_H = 600f
private const val ROOM_W = 128f
private const val ROOM_H = 96f
private const val LIA_H = 62f
private const val WALK_SPEED = 150f
private const val REACH = 22f

private val ROOMS = listOf(
    Offset(92f, 478f), Offset(268f, 478f),
    Offset(268f, 342f), Offset(92f, 342f),
    Offset(92f, 206f), Offset(268f, 206f),
    Offset(92f, 74f)
)
private val START = Offset(180f, 566f)
private val KEY_SPOT = Offset(180f, 140f)
private val EXIT = Offset(268f, 72f)

/** The corridor Lía follows: start → every room in order → the key → the exit hatch. */
private val CORRIDOR = listOf(START) + ROOMS + listOf(KEY_SPOT, EXIT)

/** Two diamonds along every stretch of corridor. */
private val DIAMONDS: List<Offset> = CORRIDOR.zipWithNext().flatMap { (a, b) -> listOf(lerp(a, b, 0.4f), lerp(a, b, 0.62f)) }

/** Fixed star field (fractions of the view), so it doesn't flicker between frames. */
internal val STARS: List<Offset> = List(70) { i ->
    val r = kotlin.random.Random(i * 7919 + 13)
    Offset(r.nextFloat(), r.nextFloat())
}

/** Walk-cycle frames inside explorer_walk.webp (x, y, width, height). */
internal val WALK_FRAMES = listOf(
    intArrayOf(164, 33, 387, 582),
    intArrayOf(727, 34, 375, 582),
    intArrayOf(144, 630, 420, 583),
    intArrayOf(729, 630, 390, 580)
)

private fun clampDeck(p: Offset) = Offset(p.x.coerceIn(26f, DECK_W - 26f), p.y.coerceIn(34f, DECK_H - 18f))

private fun deckScale(w: Float, h: Float) = minOf(w / DECK_W, h / DECK_H)

/** Screen position → deck position (the deck is centred and scaled to fit). */
private fun toDeck(p: Offset, w: Float, h: Float): Offset {
    val s = deckScale(w, h)
    return Offset((p.x - (w - DECK_W * s) / 2f) / s, (p.y - (h - DECK_H * s) / 2f) / s)
}

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

    // Lía (deck units).
    var lia by remember { mutableStateOf(START) }
    var facing by remember { mutableFloatStateOf(1f) }
    var moving by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<Offset?>(null) }
    var walkedOnce by remember { mutableStateOf(false) }
    var clock by remember { mutableFloatStateOf(0f) }

    // Mission state.
    val solved = remember { mutableStateListOf<Int>() }
    val solvedAt = remember { mutableStateMapOf<Int, Float>() }
    val collected = remember { mutableStateListOf<Int>() }
    // Each wrong answer costs one collected diamond, so diamonds are worth protecting.
    var lostDiamonds by remember { mutableIntStateOf(0) }
    var lostAt by remember { mutableFloatStateOf(-10f) }
    var showStory by remember { mutableStateOf(false) }
    val results = remember { mutableStateMapOf<String, Boolean>() }
    var openStation by remember { mutableStateOf<Int?>(null) }
    var hasKey by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // Nilo, Lía's co-pilot: follows her around the ship and comments in the target language.
    var nilo by remember { mutableStateOf(START + Offset(-30f, 12f)) }
    var niloFacing by remember { mutableFloatStateOf(1f) }
    var niloMoving by remember { mutableStateOf(false) }
    var niloLine by remember { mutableStateOf<NiloLine?>(null) }
    var niloLineUntil by remember { mutableFloatStateOf(0f) }
    var lastMoveAt by remember { mutableFloatStateOf(0f) }
    var lastLockedAt by remember { mutableFloatStateOf(-10f) }

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

    // Game loop: runs every frame while no mission panel is open.
    LaunchedEffect(tablet.id) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            clock += dt
            if (niloLine != null && clock > niloLineUntil && openStation == null) niloLine = null
            if (clock in 0.6f..0.7f && niloLine == null) say(NiloLines.start)
            if (openStation != null || finished) {
                moving = false
                niloMoving = false
                continue
            }
            // Walk towards the finger.
            val goal = target
            if (goal != null) {
                val d = goal - lia
                val dist = d.getDistance()
                if (dist < 2f) {
                    target = null
                    moving = false
                } else {
                    lia = clampDeck(lia + d / dist * minOf(dist, WALK_SPEED * dt))
                    if (abs(d.x) > 1f) facing = if (d.x > 0) 1f else -1f
                    moving = true
                    lastMoveAt = clock
                }
            } else {
                moving = false
            }
            if (!moving && clock - lastMoveAt > 8f && niloLine == null) {
                say(NiloLines.idle, speak = false)
                lastMoveAt = clock
            }
            // Nilo keeps a step behind Lía and catches up when she runs ahead.
            val gap = lia + Offset(-26f * facing, 14f) - nilo
            val gapDist = gap.getDistance()
            niloMoving = gapDist > 3f
            if (niloMoving) {
                nilo += gap / gapDist * minOf(gapDist * 4f, WALK_SPEED * 1.3f) * dt
                if (abs(gap.x) > 1f) niloFacing = if (gap.x > 0) 1f else -1f
            } else {
                niloFacing = facing
            }
            // Collect diamonds.
            DIAMONDS.forEachIndexed { i, d ->
                if (i !in collected && (d - lia).getDistance() < 18f) {
                    collected += i
                    viewModel.soundEngine.click()
                    if (collected.size == 1) say(NiloLines.diamond)
                }
            }
            // Read the next room fresh each frame (the composition-time value would be stale here).
            val next = Station.entries.indices.firstOrNull { it !in solved }
            ROOMS.forEachIndexed { i, room ->
                if ((room - lia).getDistance() < REACH) {
                    if (i == next) {
                        openStation = i
                        target = null
                        viewModel.soundEngine.powerUp()
                    } else if (i !in solved && clock - lastLockedAt > 4f) {
                        // A locked room: Nilo points to the one that glows.
                        lastLockedAt = clock
                        say(NiloLines.locked)
                    }
                }
            }
            // Every room solved: pick up the key, then walk out through the hatch.
            if (next == null && !hasKey && (KEY_SPOT - lia).getDistance() < REACH) {
                hasKey = true
                viewModel.soundEngine.powerUp()
                say(NiloLines.gotKey)
            }
            if (hasKey && !finished && (EXIT - lia).getDistance() < REACH + 6f) {
                finished = true
                say(NiloLines.home)
                val kept = (collected.size - lostDiamonds).coerceAtLeast(0)
                viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions, bonusCredits = kept * 5)
            }
        }
    }

    fun solve(i: Int) {
        if (i in solved) return
        solved += i
        solvedAt[i] = clock
        openStation = null
        viewModel.soundEngine.fanfare()
        say(
            when {
                i == Station.entries.lastIndex -> NiloLines.findKey
                solved.size >= Station.entries.size - 2 -> NiloLines.almost
                else -> NiloLines.praise.random()
            }
        )
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { showStory = true }) {
                Text(language.pick("📖 القصة", "📖 Story"), color = ExplorerBlue, fontWeight = FontWeight.Bold)
            }
            if (hasKey) Text("🔑", fontSize = 20.sp, modifier = Modifier.padding(end = 6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text("💎 $diamonds", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                if (clock - lostAt < 1.5f) Text("−1", color = MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Tap to walk somewhere, or keep the finger down and Lía follows it.
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val first = toDeck(down.position, w, h)
                                if ((first - (nilo - Offset(0f, LIA_H / 2))).getDistance() < 24f) {
                                    say(NiloLines.greeting)
                                    return@awaitEachGesture
                                }
                                target = clampDeck(first)
                                walkedOnce = true
                                do {
                                    val event = awaitPointerEvent()
                                    event.changes.forEach { change ->
                                        if (change.pressed) {
                                            target = clampDeck(toDeck(change.position, w, h))
                                            change.consume()
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                ) {
                    drawRect(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy)))
                    STARS.forEach { drawCircle(StarWhite.copy(alpha = 0.55f), radius = 2f, center = Offset(it.x * size.width, it.y * size.height)) }
                    val s = deckScale(size.width, size.height)
                    withTransform({
                        translate((size.width - DECK_W * s) / 2f, (size.height - DECK_H * s) / 2f)
                        scale(s, s, pivot = Offset.Zero)
                    }) {
                        drawDeck(
                            walkSprite = walkSprite,
                            niloSprite = niloSprite,
                            textMeasurer = textMeasurer,
                            unitScale = s,
                            clock = clock,
                            lia = lia,
                            facing = facing,
                            moving = moving,
                            nilo = nilo,
                            niloFacing = niloFacing,
                            niloMoving = niloMoving,
                            niloSays = niloLine?.es,
                            solved = solved,
                            solvedAt = solvedAt,
                            next = nextStation,
                            collected = collected,
                            hasKey = hasKey,
                            exitLabel = language.pick("المخرج", tl("SALIDA"))
                        )
                    }
                }
                if (!walkedOnce) {
                    Text(
                        language.pick("👆 اضغطي حيث تريدين أن تمشي ليا", "👆 Tap where Lía should walk"),
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
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
                        onClose = {
                            openStation = null
                            // Step back towards the corridor so the room doesn't open again straight away.
                            val back = CORRIDOR[station] - ROOMS[station]
                            lia = clampDeck(ROOMS[station] + back / back.getDistance() * (REACH + 18f))
                        },
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

// --------------------------------------------------------------------------- Drawing

private fun DrawScope.drawDeck(
    walkSprite: ImageBitmap,
    niloSprite: ImageBitmap,
    textMeasurer: TextMeasurer,
    unitScale: Float,
    clock: Float,
    lia: Offset,
    facing: Float,
    moving: Boolean,
    nilo: Offset,
    niloFacing: Float,
    niloMoving: Boolean,
    niloSays: String?,
    solved: List<Int>,
    solvedAt: Map<Int, Float>,
    next: Int?,
    collected: List<Int>,
    hasKey: Boolean,
    exitLabel: String
) {
    // Hull and floor plates.
    val hullTopLeft = Offset(8f, 8f)
    val hullSize = Size(DECK_W - 16f, DECK_H - 16f)
    drawRoundRect(Color(0xFF1B2540), topLeft = hullTopLeft, size = hullSize, cornerRadius = CornerRadius(42f))
    var plate = 38f
    while (plate < DECK_W - 10f) {
        drawLine(Color.White.copy(alpha = 0.05f), Offset(plate, 16f), Offset(plate, DECK_H - 16f), strokeWidth = 1f)
        plate += 30f
    }
    plate = 38f
    while (plate < DECK_H - 10f) {
        drawLine(Color.White.copy(alpha = 0.05f), Offset(14f, plate), Offset(DECK_W - 14f, plate), strokeWidth = 1f)
        plate += 30f
    }
    drawRoundRect(DiamondCyan.copy(alpha = 0.45f), topLeft = hullTopLeft, size = hullSize, cornerRadius = CornerRadius(42f), style = Stroke(4f))

    // Corridor, and a moving guide line along the stretch that leads to the next goal.
    val corridor = Path().apply {
        moveTo(CORRIDOR[0].x, CORRIDOR[0].y)
        CORRIDOR.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(corridor, Color(0xFF2C3A63), style = Stroke(width = 30f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    val goal = when {
        next != null -> next + 1
        !hasKey -> CORRIDOR.size - 2
        else -> CORRIDOR.lastIndex
    }
    drawLine(
        DiamondCyan.copy(alpha = 0.8f), CORRIDOR[goal - 1], CORRIDOR[goal], strokeWidth = 4f, cap = StrokeCap.Round,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), -clock * 30f)
    )

    // Rooms: lit when solved or next, dim and locked otherwise.
    Station.entries.forEachIndexed { i, st ->
        val c = ROOMS[i]
        val topLeft = Offset(c.x - ROOM_W / 2, c.y - ROOM_H / 2)
        val done = i in solved
        val active = i == next
        val lit = done || active
        drawRoundRect(st.color.copy(alpha = if (done) 0.38f else if (active) 0.28f else 0.10f), topLeft = topLeft, size = Size(ROOM_W, ROOM_H), cornerRadius = CornerRadius(16f))
        drawRoundRect(st.color.copy(alpha = if (lit) 0.95f else 0.35f), topLeft = topLeft, size = Size(ROOM_W, ROOM_H), cornerRadius = CornerRadius(16f), style = Stroke(if (active) 4f else 2.5f))
        if (active) {
            val pulse = 0.5f + 0.5f * sin(clock * 4f)
            drawCircle(st.color.copy(alpha = 0.2f + 0.25f * pulse), radius = 24f + 8f * pulse, center = c)
        }
        val age = clock - (solvedAt[i] ?: -10f)
        if (done && age < 1.2f) {
            drawCircle(SolarGold.copy(alpha = (1.2f - age) / 1.2f), radius = 20f + age * 90f, center = c, style = Stroke(4f))
        }
        drawCircle(st.color.copy(alpha = if (lit) 1f else 0.35f), radius = 13f, center = c)
        drawCircle(Color.White.copy(alpha = 0.6f), radius = 13f, center = c, style = Stroke(2f))
        drawLabel(textMeasurer, unitScale, if (done) "✓" else if (active) st.emoji else "🔒", c.x, c.y - 16f, Color.White, 18)
        drawLabel(textMeasurer, unitScale, st.es, c.x, topLeft.y + ROOM_H - 4f, Color.White.copy(alpha = if (lit) 1f else 0.5f), 10, maxWidth = ROOM_W - 8f)
    }

    // Diamonds.
    DIAMONDS.forEachIndexed { i, d ->
        if (i in collected) return@forEachIndexed
        val bob = sin(clock * 3f + i) * 2f
        drawCircle(SolarGold.copy(alpha = 0.25f), radius = 11f, center = Offset(d.x, d.y + bob))
        rotate(45f, pivot = Offset(d.x, d.y + bob)) {
            drawRect(SolarGold, topLeft = Offset(d.x - 5f, d.y + bob - 5f), size = Size(10f, 10f))
        }
    }

    // The key appears in the corridor once every room is solved.
    if (next == null && !hasKey) {
        val bob = sin(clock * 3f) * 4f
        val pulse = 0.5f + 0.5f * sin(clock * 5f)
        drawCircle(SolarGold.copy(alpha = 0.25f + 0.25f * pulse), radius = 22f + 6f * pulse, center = KEY_SPOT + Offset(0f, bob))
        drawLabel(textMeasurer, unitScale, "🔑", KEY_SPOT.x, KEY_SPOT.y + bob + 12f, Color.White, 26)
    }

    // Exit hatch: grey and locked until Lía carries the key.
    val hatchTopLeft = Offset(EXIT.x - 38f, EXIT.y - 28f)
    val hatchColor = if (hasKey) SolarGold else Color(0xFF55607A)
    if (hasKey) {
        val pulse = 0.5f + 0.5f * sin(clock * 5f)
        drawRoundRect(SolarGold.copy(alpha = 0.25f * pulse), topLeft = hatchTopLeft - Offset(8f, 8f), size = Size(92f, 72f), cornerRadius = CornerRadius(18f))
    }
    drawRoundRect(hatchColor.copy(alpha = 0.35f), topLeft = hatchTopLeft, size = Size(76f, 56f), cornerRadius = CornerRadius(12f))
    drawRoundRect(hatchColor, topLeft = hatchTopLeft, size = Size(76f, 56f), cornerRadius = CornerRadius(12f), style = Stroke(3f))
    drawLabel(textMeasurer, unitScale, if (hasKey) "🚪" else "🔒", EXIT.x, EXIT.y + 6f, Color.White, 18)
    drawLabel(textMeasurer, unitScale, exitLabel, EXIT.x, EXIT.y + 24f, Color.White, 10)

    // Characters, the one further down drawn last so she stands in front.
    val drawNilo = {
        val frame = if (niloMoving) ((clock * 9f).toInt() + 2) % 4 else 0
        drawOval(Color(0x66000000), topLeft = Offset(nilo.x - 13f, nilo.y - 4f), size = Size(26f, 8f))
        withTransform({
            translate(nilo.x, nilo.y)
            scale(niloFacing, 1f, pivot = Offset.Zero)
        }) { drawNiloSprite(niloSprite, frame, LIA_H * 0.95f) }
    }
    val drawLia = {
        val r = WALK_FRAMES[if (moving) (clock * 9f).toInt() % 4 else 0]
        val drawW = r[2].toFloat() / r[3] * LIA_H
        drawOval(Color(0x66000000), topLeft = Offset(lia.x - 14f, lia.y - 4f), size = Size(28f, 8f))
        withTransform({
            translate(lia.x, lia.y)
            scale(facing, 1f, pivot = Offset.Zero)
        }) {
            drawImage(
                walkSprite,
                srcOffset = IntOffset(r[0], r[1]),
                srcSize = IntSize(r[2], r[3]),
                dstOffset = IntOffset((-drawW / 2).toInt(), (-LIA_H).toInt()),
                dstSize = IntSize(drawW.toInt(), LIA_H.toInt())
            )
        }
    }
    if (nilo.y <= lia.y) { drawNilo(); drawLia() } else { drawLia(); drawNilo() }
    niloSays?.let { drawBubble(textMeasurer, unitScale, it, nilo.x.coerceIn(80f, DECK_W - 80f), nilo.y - LIA_H - 4f) }
}

private fun DrawScope.drawLabel(
    textMeasurer: TextMeasurer,
    unitScale: Float,
    text: String,
    centerX: Float,
    y: Float,
    color: Color,
    fontSize: Int = 12,
    maxWidth: Float? = null
) {
    val layout = textMeasurer.measure(
        text,
        TextStyle(color = color, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        constraints = maxWidth?.let { Constraints(maxWidth = (it * unitScale).toInt()) } ?: Constraints()
    )
    // The deck is drawn scaled; undo that around the label's anchor so text keeps its normal size.
    withTransform({ scale(1f / unitScale, 1f / unitScale, pivot = Offset(centerX, y)) }) {
        drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, y - layout.size.height))
    }
}

/** Speech bubble whose bottom edge sits at [bottomY], centred on [centerX]. Text keeps its natural size. */
private fun DrawScope.drawBubble(
    textMeasurer: TextMeasurer,
    unitScale: Float,
    text: String,
    centerX: Float,
    bottomY: Float
) {
    val layout = textMeasurer.measure(text, TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
    val w = layout.size.width / unitScale + 18f
    val h = layout.size.height / unitScale + 10f
    val left = centerX - w / 2
    val top = bottomY - h
    drawRoundRect(AdventureSurface, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9f))
    drawRoundRect(SolarAmber, topLeft = Offset(left, top), size = Size(w, h), cornerRadius = CornerRadius(9f), style = Stroke(2f))
    withTransform({ scale(1f / unitScale, 1f / unitScale, pivot = Offset(centerX, top + h / 2)) }) {
        drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, top + h / 2 - layout.size.height / 2f))
    }
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
                    color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                            Text(step.text, color = TextPrimary, fontSize = 17.sp, lineHeight = 26.sp, modifier = Modifier.fillMaxWidth())
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
                Text(step.text, color = TextPrimary, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
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
        Text(step.prompt, color = TextPrimary, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
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

/** Words fall through space like meteors: tap the right one. Wrong taps cost a diamond. */
@Composable
private fun FallingWordsCard(step: Step.Falling, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val onMistake = LocalOnMistake.current
    val wrong = remember(step.key) { mutableStateListOf<String>() }
    val accepted = remember(step) { step.answers.flatMap { it.split("/") }.map { normalizeAnswer(it) } }
    val solved = step.key in results
    var clock by remember(step.key) { mutableFloatStateOf(0f) }
    LaunchedEffect(step.key, solved) {
        if (solved) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            clock += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
        }
    }
    AdventureCard(borderColor = if (solved) SuccessGreen else SolarGold) {
        Text(step.prompt, color = TextPrimary, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
    Text(
        language.pick("☄️ اضغطي على الكلمة الصحيحة وهي تسقط!", "☄️ Tap the right word as it falls!"),
        color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy, Color(0xFF2A1F6B))))
    ) {
        Canvas(Modifier.matchParentSize()) {
            STARS.forEach { drawCircle(StarWhite.copy(alpha = 0.5f), radius = 2f, center = Offset(it.x * size.width, it.y * size.height)) }
        }
        val count = step.options.size.coerceAtLeast(1)
        val laneW = maxWidth / count
        step.options.forEachIndexed { i, option ->
            val isAnswer = normalizeAnswer(option) in accepted
            // Each word falls in its own lane at its own speed, then starts again from the top.
            val speed = 0.10f + 0.03f * ((i * 2) % count)
            val t = if (solved && isAnswer) 0.45f else (i * 0.29f + clock * speed) % 1f
            val y = (maxHeight + 52.dp) * t - 52.dp
            val color = when {
                solved && isAnswer -> SuccessGreen
                option in wrong -> MeteorRed
                else -> SolarGold
            }
            Surface(
                onClick = {
                    if (!solved && option !in wrong) {
                        if (isAnswer) {
                            results[step.key] = wrong.isEmpty()
                            viewModel.soundEngine.hit()
                            viewModel.speakSpanish(option)
                        } else {
                            wrong += option
                            viewModel.soundEngine.error()
                            onMistake()
                            if (wrong.size >= 2) results[step.key] = false
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                color = color,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset(x = laneW * i + 4.dp, y = y)
                    .width(laneW - 8.dp)
            ) {
                Text(
                    option,
                    color = if (color == SolarGold) SpaceNavy else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
                )
            }
        }
    }
    if (wrong.isNotEmpty() && step.hint.isNotBlank()) {
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
