package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.content.SpanishContent
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlasterViewModel
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

/** The seven stops of every expedition, in the order Lía reaches them. */
private enum class Station(val emoji: String, val es: String, val ar: String, val en: String) {
    STORY("📡", "Transmisión", "الرسالة", "Transmission"),
    OPENING("❓", "Primeras preguntas", "الأسئلة الأولى", "First questions"),
    SEARCH("🔍", "Búsqueda", "البحث", "Search"),
    CONSOLE("🔧", "Consola gramatical", "لوحة القواعد", "Grammar console"),
    ORDER("🧩", "Mensaje roto", "الرسالة المبعثرة", "Broken message"),
    MISSION("🛰️", "Misión", "المهمة", "Mission"),
    PORTAL("🌀", "Portal", "البوابة", "Portal");

    fun label(language: HelperLanguage) = language.pick(ar, en)
}

// World units: the game view is always 320 units tall and scrolls horizontally.
private const val VIEW_H = 320f
private const val GROUND = 268f
private const val START_X = 90f
private const val FIRST_STATION = 430f
private const val STATION_GAP = 560f
private const val LIA_H = 112f
private const val WALK_SPEED = 240f
private const val GRAVITY = 1700f
private const val JUMP_VELOCITY = -660f

private fun stationX(i: Int) = FIRST_STATION + i * STATION_GAP
private val PORTAL_X = stationX(Station.entries.lastIndex) + 230f
private val WORLD_END = PORTAL_X + 220f

/** Walk-cycle frames inside explorer_walk.webp (x, y, width, height). */
internal val WALK_FRAMES = listOf(
    intArrayOf(164, 33, 387, 582),
    intArrayOf(727, 34, 375, 582),
    intArrayOf(144, 630, 420, 583),
    intArrayOf(729, 630, 390, 580)
)

private class Diamond(val x: Float, val y: Float)

/** Three diamonds between every pair of stations: one on the ground, two that need a jump. */
private val DIAMONDS: List<Diamond> = buildList {
    for (i in 0 until Station.entries.size) {
        val from = if (i == 0) START_X + 120f else stationX(i - 1) + 120f
        val to = stationX(i) - 90f
        val step = (to - from) / 3f
        add(Diamond(from + step * 0.5f, GROUND - 30f))
        add(Diamond(from + step * 1.5f, GROUND - 130f))
        add(Diamond(from + step * 2.5f, GROUND - 150f))
    }
}

private fun backgroundFor(tablet: ReadingTablet): Int =
    when ((tablet.id.substringAfter('-').toIntOrNull() ?: 1) % 3) {
        1 -> R.drawable.space_background
        2 -> R.drawable.signal_ruins
        else -> R.drawable.time_observatory
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
    val wisp = ImageBitmap.imageResource(R.drawable.word_wisp)
    val background = ImageBitmap.imageResource(backgroundFor(tablet))
    val textMeasurer = rememberTextMeasurer()

    // Physics state (world units).
    var liaX by remember { mutableFloatStateOf(START_X) }
    var liaY by remember { mutableFloatStateOf(0f) } // height above the ground, negative is up
    var velocityY by remember { mutableFloatStateOf(0f) }
    var facing by remember { mutableFloatStateOf(1f) }
    var moving by remember { mutableStateOf(false) }
    var clock by remember { mutableFloatStateOf(0f) }
    var holdLeft by remember { mutableStateOf(false) }
    var holdRight by remember { mutableStateOf(false) }
    var targetX by remember { mutableStateOf<Float?>(null) }

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
    var finished by remember { mutableStateOf(false) }

    // Nilo, Lía's co-pilot: follows her, jumps after her and comments in Spanish.
    var niloX by remember { mutableFloatStateOf(START_X - 72f) }
    var niloY by remember { mutableFloatStateOf(0f) }
    var niloVelocityY by remember { mutableFloatStateOf(0f) }
    var niloFacing by remember { mutableFloatStateOf(1f) }
    var niloMoving by remember { mutableStateOf(false) }
    var niloJumpAt by remember { mutableStateOf<Float?>(null) }
    var niloLine by remember { mutableStateOf<NiloLine?>(null) }
    var niloLineUntil by remember { mutableFloatStateOf(0f) }
    var lastMoveAt by remember { mutableFloatStateOf(0f) }

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
    val nextStation = Station.entries.indices.firstOrNull { it !in solved } // null once every station is solved
    // The last sentence of the chapter, revealed word by word as missions are won.
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
            val target = targetX
            val dir = when {
                holdRight -> 1f
                holdLeft -> -1f
                target != null && abs(target - liaX) > 6f -> if (target > liaX) 1f else -1f
                else -> 0f
            }
            if (target != null && abs(target - liaX) <= 6f) targetX = null
            // Read the next station fresh each frame (the composition-time value would be stale here).
            val nextStation = Station.entries.indices.firstOrNull { it !in solved }
            val maxX = nextStation?.let { stationX(it) } ?: PORTAL_X
            val before = liaX
            liaX = (liaX + dir * WALK_SPEED * dt).coerceIn(40f, maxX)
            moving = abs(liaX - before) > 0.01f
            if (dir != 0f) facing = dir
            if (moving) lastMoveAt = clock
            if (!moving && clock - lastMoveAt > 8f && niloLine == null) {
                say(NiloLines.idle, speak = false)
                lastMoveAt = clock
            }
            // Nilo keeps a few steps behind Lía and catches up when she runs ahead.
            val niloTarget = (liaX - 72f * facing).coerceAtLeast(30f)
            val gap = niloTarget - niloX
            val niloStep = (if (gap > 0) 1f else -1f) * minOf(abs(gap) * 4f, WALK_SPEED * 1.2f) * dt
            niloMoving = abs(gap) > 3f
            if (niloMoving) {
                niloX += niloStep
                niloFacing = if (gap > 0) 1f else -1f
            } else {
                niloFacing = facing
            }
            niloJumpAt?.let { at ->
                if (clock >= at && niloY == 0f) {
                    niloVelocityY = JUMP_VELOCITY * 0.9f
                    niloY = -0.1f
                    niloJumpAt = null
                }
            }
            if (niloY < 0f || niloVelocityY < 0f) {
                niloVelocityY += GRAVITY * dt
                niloY = (niloY + niloVelocityY * dt).coerceAtMost(0f)
                if (niloY >= 0f) niloVelocityY = 0f
            }
            // Jump physics.
            if (liaY < 0f || velocityY < 0f) {
                velocityY += GRAVITY * dt
                liaY = (liaY + velocityY * dt).coerceAtMost(0f)
                if (liaY >= 0f) velocityY = 0f
            }
            // Collect diamonds.
            DIAMONDS.forEachIndexed { i, d ->
                if (i !in collected && abs(d.x - liaX) < 30f && abs(d.y - (GROUND + liaY - LIA_H / 2)) < 62f) {
                    collected += i
                    viewModel.soundEngine.click()
                    if (collected.size == 1) say(NiloLines.diamond)
                }
            }
            // Reaching the next glowing wisp opens its mission.
            if (nextStation != null && abs(liaX - stationX(nextStation)) < 4f && liaY == 0f) {
                openStation = nextStation
                targetX = null
                viewModel.soundEngine.powerUp()
            }
            // Walking into the open portal finishes the expedition.
            if (nextStation == null && liaX >= PORTAL_X - 4f && !finished) {
                finished = true
                say(NiloLines.home)
                val kept = (collected.size - lostDiamonds).coerceAtLeast(0)
                viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions, bonusCredits = kept * 5)
            }
        }
    }

    fun jump() {
        if (liaY == 0f && openStation == null) {
            velocityY = JUMP_VELOCITY
            liaY = -0.1f
            niloJumpAt = clock + 0.2f
            viewModel.soundEngine.laser()
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
                i == Station.entries.lastIndex -> NiloLines.portalOpen
                solved.size >= Station.entries.size - 2 -> NiloLines.almost
                else -> NiloLines.praise.random()
            }
        )
        if (i == Station.entries.lastIndex) targetX = PORTAL_X
    }

    val diamonds = (collected.size - lostDiamonds).coerceAtLeast(0)
    val onMistake: () -> Unit = {
        if (collected.size - lostDiamonds > 0) {
            lostDiamonds++
            lostAt = clock
        }
    }
    val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    Column(Modifier.fillMaxSize()) {
        // Header: chapter, a button to reread the story, and the diamonds still held.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1, modifier = Modifier.weight(1f))
            TextButton(onClick = { showStory = true }) {
                Text(language.pick("📖 القصة", "📖 Story"), color = ExplorerBlue, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 6.dp)) {
                Text("💎 $diamonds", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                if (clock - lostAt < 1.5f) Text("−1", color = MeteorRed, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
        }

        // Questions on top, so they are always in view (also while the keyboard is open).
        val station = openStation
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (station == null) {
                WalkPanel(
                    tablet = tablet,
                    language = language,
                    progress = solved.size,
                    picture = scene?.let { sceneDrawable(it.asset) } ?: backgroundFor(tablet),
                    secret = secretWords,
                    secretOrder = secretOrder,
                    newPiece = solvedAt.values.any { clock - it < 4f },
                    niloLine = niloLine,
                    onSpeak = { viewModel.speakSpanish(it) }
                )
            } else {
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
                            liaX = stationX(station) - 70f
                            facing = -1f
                        },
                        onSolved = { solve(station) }
                    )
                }
            }
        }

        // Lía's walk at the bottom of the page; hidden while typing so the answer box stays visible.
        if (!(keyboardOpen && station != null)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (station == null) 240.dp else 150.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            // Tap above Lía to jump, anywhere else to walk there.
                            val s = size.height / VIEW_H
                            val viewW = size.width / s
                            val camera = (liaX - viewW * 0.35f).coerceIn(0f, (WORLD_END - viewW).coerceAtLeast(0f))
                            val worldX = tap.x / s + camera
                            val worldY = tap.y / s
                            when {
                                abs(worldX - niloX) < 32f && worldY > GROUND - 140f -> say(NiloLines.greeting)
                                abs(worldX - liaX) < 50f && worldY < GROUND - LIA_H * 0.5f -> jump()
                                else -> targetX = worldX
                            }
                        }
                    }
            ) {
                val s = size.height / VIEW_H
                val viewW = size.width / s
                val camera = (liaX - viewW * 0.35f).coerceIn(0f, (WORLD_END - viewW).coerceAtLeast(0f))
                withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                    drawWorld(
                        background = background,
                        wisp = wisp,
                        walkSprite = walkSprite,
                        niloSprite = niloSprite,
                        camera = camera,
                        viewW = viewW,
                        clock = clock,
                        liaX = liaX,
                        liaY = liaY,
                        facing = facing,
                        moving = moving,
                        solved = solved,
                        solvedAt = solvedAt,
                        nextStation = nextStation,
                        collected = collected,
                        niloX = niloX,
                        niloY = niloY,
                        niloFacing = niloFacing,
                        niloMoving = niloMoving,
                        niloSays = niloLine?.es,
                        textMeasurer = textMeasurer,
                        unitScale = s,
                        language = language
                    )
                }
            }
        }
        // Walking controls at the very bottom, under Lía, so you can watch her walk.
        if (station == null) {
            WalkControls(language, onLeft = { holdLeft = it }, onRight = { holdRight = it }, onJump = { jump() })
        }
    }

    if (showStory) {
        AlertDialog(
            onDismissRequest = { showStory = false },
            containerColor = AdventureBg,
            confirmButton = { TextButton(onClick = { showStory = false }) { Text(language.pick("إغلاق", "Close")) } },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    StoryCard(language.pick("📡 القصة", "📡 La historia"), tablet.story, tablet.storyAr, viewModel, language)
                }
            }
        )
    }
}

// --------------------------------------------------------------------------- Drawing

private fun DrawScope.drawWorld(
    background: ImageBitmap,
    wisp: ImageBitmap,
    walkSprite: ImageBitmap,
    niloSprite: ImageBitmap,
    camera: Float,
    viewW: Float,
    clock: Float,
    liaX: Float,
    liaY: Float,
    facing: Float,
    moving: Boolean,
    solved: List<Int>,
    solvedAt: Map<Int, Float>,
    nextStation: Int?,
    collected: List<Int>,
    niloX: Float,
    niloY: Float,
    niloFacing: Float,
    niloMoving: Boolean,
    niloSays: String?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    unitScale: Float,
    language: HelperLanguage
) {
    // Parallax background, tiled horizontally.
    val bgW = background.width * VIEW_H / background.height
    val parallax = camera * 0.4f
    var tile = floor(parallax / bgW)
    while (tile * bgW - parallax < viewW) {
        val left = tile * bgW - parallax
        // Mirror every other tile so the repeat has no visible seam.
        val mirrored = tile.toInt() % 2 != 0
        withTransform({ if (mirrored) scale(-1f, 1f, pivot = Offset(left + bgW / 2, VIEW_H / 2)) }) {
            drawImage(
                background,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(background.width, background.height),
                dstOffset = IntOffset(left.toInt(), 0),
                dstSize = IntSize(bgW.toInt() + 1, VIEW_H.toInt())
            )
        }
        tile += 1f
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x33040A1C), Color(0x99040A1C))), size = Size(viewW, VIEW_H))

    translate(left = -camera) {
        // Ground
        drawRect(Color(0xCC0A1633), topLeft = Offset(camera, GROUND), size = Size(viewW, VIEW_H - GROUND))
        drawLine(DiamondCyan, Offset(camera, GROUND), Offset(camera + viewW, GROUND), strokeWidth = 3f)

        // Portal
        val portalOpen = nextStation == null
        val portalColor = if (portalOpen) SolarGold else StarWhite.copy(alpha = 0.25f)
        rotate(degrees = clock * 40f, pivot = Offset(PORTAL_X, GROUND - 70f)) {
            drawOval(portalColor, topLeft = Offset(PORTAL_X - 34f, GROUND - 140f), size = Size(68f, 140f), style = Stroke(width = 6f))
        }
        if (portalOpen) {
            drawOval(
                Brush.radialGradient(listOf(SolarGold.copy(alpha = 0.6f), Color.Transparent), center = Offset(PORTAL_X, GROUND - 70f), radius = 80f),
                topLeft = Offset(PORTAL_X - 34f, GROUND - 140f),
                size = Size(68f, 140f)
            )
        }
        drawLabel(textMeasurer, unitScale, language.pick("صفحة الأطلس", "PÁGINA"), PORTAL_X, GROUND - 168f, portalColor)

        // Diamonds
        DIAMONDS.forEachIndexed { i, d ->
            if (i in collected) return@forEachIndexed
            val bob = sin(clock * 3f + i) * 4f
            rotate(45f, pivot = Offset(d.x, d.y + bob)) {
                drawRect(SolarGold, topLeft = Offset(d.x - 7f, d.y + bob - 7f), size = Size(14f, 14f))
            }
            drawCircle(SolarGold.copy(alpha = 0.25f), radius = 14f, center = Offset(d.x, d.y + bob))
        }

        // Stations: glowing wisp for the next mission, gold stars for solved ones, dim wisps ahead.
        Station.entries.forEachIndexed { i, st ->
            val x = stationX(i)
            val bob = sin(clock * 2.5f + i) * 5f
            when {
                i in solved -> {
                    val age = clock - (solvedAt[i] ?: 0f)
                    if (age < 1.2f) {
                        drawCircle(SolarGold.copy(alpha = (1.2f - age) / 1.2f), radius = 30f + age * 120f, center = Offset(x, GROUND - 70f), style = Stroke(4f))
                    }
                    drawCircle(SolarGold, radius = 16f, center = Offset(x, GROUND - 70f + bob))
                    drawLabel(textMeasurer, unitScale, "✓ ${st.emoji}", x, GROUND - 120f + bob, SolarGold)
                }
                else -> {
                    val active = i == nextStation
                    val alpha = if (active) 1f else 0.35f
                    if (active) {
                        val pulse = 0.5f + 0.5f * sin(clock * 4f)
                        drawCircle(DiamondCyan.copy(alpha = 0.25f + 0.25f * pulse), radius = 46f + 8f * pulse, center = Offset(x, GROUND - 70f + bob))
                    }
                    val w = 110f
                    val h = w * wisp.height / wisp.width
                    drawImage(
                        wisp,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(wisp.width, wisp.height),
                        dstOffset = IntOffset((x - w / 2).toInt(), (GROUND - 70f - h / 2 + bob).toInt()),
                        dstSize = IntSize(w.toInt(), h.toInt()),
                        alpha = alpha
                    )
                    drawLabel(textMeasurer, unitScale, "${st.emoji} ${st.es}", x, GROUND - 132f + bob, StarWhite.copy(alpha = alpha))
                    if (active) {
                        // Energy barrier just past the wisp: it opens when the mission is solved.
                        val flicker = 0.35f + 0.25f * sin(clock * 9f)
                        drawRect(
                            Brush.horizontalGradient(listOf(Color.Transparent, DiamondCyan.copy(alpha = flicker), Color.Transparent)),
                            topLeft = Offset(x + 34f, GROUND - 170f),
                            size = Size(26f, 170f)
                        )
                    }
                }
            }
        }

        // Nilo (drawn behind Lía; his walk cycle runs half a step out of phase with hers)
        val niloFrame = when {
            niloY < 0f -> 1
            niloMoving -> ((clock * 9f).toInt() + 2) % 4
            else -> 0
        }
        val niloBob = if (niloMoving && niloY == 0f) sin(clock * 12f + 1.5f) * 1.5f else 0f
        drawOval(Color(0x55000000), topLeft = Offset(niloX - 24f, GROUND - 5f), size = Size(48f, 10f))
        withTransform({
            translate(niloX, GROUND + niloY + niloBob)
            scale(niloFacing, 1f, pivot = Offset.Zero)
        }) {
            drawNiloSprite(niloSprite, niloFrame, LIA_H * 0.98f)
        }
        niloSays?.let { drawBubble(textMeasurer, unitScale, it, niloX, GROUND + niloY - LIA_H - 10f) }

        // Lía
        val frame = when {
            liaY < 0f -> 1
            moving -> (clock * 9f).toInt() % 4
            else -> 0
        }
        val r = WALK_FRAMES[frame]
        val drawW = r[2].toFloat() / r[3] * LIA_H
        val bob = if (moving && liaY == 0f) sin(clock * 12f) * 1.5f else 0f
        // Shadow
        drawOval(Color(0x66000000), topLeft = Offset(liaX - 26f, GROUND - 5f), size = Size(52f, 10f))
        withTransform({
            translate(liaX, GROUND + liaY + bob)
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
}

private fun DrawScope.drawLabel(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    unitScale: Float,
    text: String,
    centerX: Float,
    y: Float,
    color: Color
) {
    val layout = textMeasurer.measure(text, TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold))
    // The world is drawn scaled up; undo that around the label's anchor so text keeps its normal size.
    withTransform({ scale(1f / unitScale, 1f / unitScale, pivot = Offset(centerX, y)) }) {
        drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, y - layout.size.height))
    }
}

/** Speech bubble whose bottom edge sits at [bottomY], centred on [centerX]. Text keeps its natural size. */
private fun DrawScope.drawBubble(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
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

@Composable
private fun WalkPanel(
    tablet: ReadingTablet,
    language: HelperLanguage,
    progress: Int,
    picture: Int,
    secret: List<String>,
    secretOrder: List<Int>,
    newPiece: Boolean,
    niloLine: NiloLine?,
    onSpeak: (String) -> Unit
) {
    val total = Station.entries.size
    // Words of the secret sentence revealed so far, spread over the seven missions.
    val shownWords = (secret.size * progress + total - 1) / total
    val shown = secretOrder.take(shownWords).toSet()
    Column(
        modifier = Modifier.fillMaxSize().background(AdventureBg).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdventureCard(borderColor = SolarGold) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎯 " + tablet.expeditionGoal(language), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("🧩 $progress/$total", color = SolarAmber, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(6.dp))
            // The chapter picture, uncovered one piece per finished mission.
            val image = ImageBitmap.imageResource(picture)
            Canvas(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp))) {
                val pieceW = size.width / total
                for (i in 0 until total) {
                    val left = i * pieceW
                    if (i < progress) {
                        val srcLeft = (image.width * i / total)
                        drawImage(
                            image,
                            srcOffset = IntOffset(srcLeft, image.height / 4),
                            srcSize = IntSize(image.width / total, image.height / 2),
                            dstOffset = IntOffset(left.toInt(), 0),
                            dstSize = IntSize(pieceW.toInt() + 1, size.height.toInt())
                        )
                    } else {
                        drawRect(SpaceNavy, topLeft = Offset(left, 0f), size = Size(pieceW - 2f, size.height))
                        drawCircle(StarWhite.copy(alpha = 0.25f), radius = 6f, center = Offset(left + pieceW / 2, size.height / 2))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // The secret sentence: hidden words are blanks the size of the word.
            Text(language.pick("🔐 الرسالة السرية", "🔐 Secret message"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    secret.mapIndexed { i, w -> if (i in shown) w else "▁".repeat(w.trim('.', ',', '¡', '!', '¿', '?').length.coerceIn(2, 8)) }.joinToString(" "),
                    color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, modifier = Modifier.weight(1f)
                )
                if (progress >= total) AudioButton(onClick = { onSpeak(secret.joinToString(" ")) }, size = 34.dp)
            }
            if (newPiece) {
                Text(
                    language.pick("✨ قطعة جديدة وكلمات جديدة!", "✨ A new piece and new words!"),
                    color = SuccessGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
                )
            }
        }
        NiloSays(niloLine ?: NiloLines.idle, language, onSpeak = { onSpeak((niloLine ?: NiloLines.idle).es) }, size = 44.dp)
    }
}

@Composable
private fun WalkControls(language: HelperLanguage, onLeft: (Boolean) -> Unit, onRight: (Boolean) -> Unit, onJump: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(AdventureBg).padding(horizontal = 14.dp, vertical = 8.dp)) {
        // Controls stay left-to-right even in Arabic so ◀ and ▶ point the way Lía walks.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HoldButton("◀", onLeft)
                HoldButton("▶", onRight)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = SolarAmber,
                    modifier = Modifier.size(72.dp).pointerInput(Unit) { detectTapGestures(onPress = { onJump() }) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(language.pick("قفز", "SALTO"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldButton(symbol: String, onHold: (Boolean) -> Unit) {
    Surface(
        shape = CircleShape,
        color = ExplorerBlue,
        modifier = Modifier
            .size(70.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHold(true)
                    tryAwaitRelease()
                    onHold(false)
                })
            }
    ) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, color = Color.White, fontSize = 28.sp) }
    }
}

/** One screen of a mission: missions are played one card at a time, like Reading Blaster. */
private sealed interface Step {
    data class Sentence(val text: String, val number: Int, val total: Int, val translation: String?) : Step
    data class Choice(val key: String, val prompt: String, val answers: List<String>, val hint: String, val options: List<String>) : Step
    data object Lesson : Step
    data object Search : Step
    data object Order : Step
    data class Note(val emoji: String, val text: String) : Step
}

private fun storySentences(text: String) = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }

private fun buildSteps(
    station: Station,
    tablet: ReadingTablet,
    data: SpanishContent,
    scene: HiddenScene?,
    searchTargets: List<HiddenObject>,
    tableCells: Set<Pair<Int, Int>>,
    language: HelperLanguage
): List<Step> {
    val choices = data.answerChoices
    fun fieldSteps(prefix: String, fields: List<TabletField>) = fields.mapIndexed { i, f ->
        Step.Choice("$prefix-$i", f.label, f.answers, language.pick(f.hintAr, f.hint), choices.options(f.answers))
    }
    return when (station) {
        Station.STORY -> storySentences(tablet.story).let { s ->
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, tablet.storyAr.takeIf { language == HelperLanguage.ARABIC }) }
        }
        Station.OPENING -> fieldSteps("opening", tablet.opening)
        Station.SEARCH -> if (scene == null || searchTargets.isEmpty()) listOf(Step.Note("🔍", "")) else listOf(Step.Search)
        Station.CONSOLE -> listOf(Step.Lesson) + tableCells.sortedWith(compareBy({ it.first }, { it.second })).map { (r, c) ->
            val column = tablet.table.rows.map { it[c] }
            val answer = tablet.table.rows[r][c]
            val wrong = column.filter { normalizeAnswer(it) != normalizeAnswer(answer) }.distinct().shuffled().take(3)
            Step.Choice(
                key = "table-$r-$c",
                prompt = "${tablet.table.rows[r][0]}  ___   (${tablet.table.headers[c]})",
                answers = listOf(answer),
                hint = "",
                options = (wrong + answer).shuffled()
            )
        }
        Station.ORDER -> listOf(Step.Order)
        Station.MISSION -> listOf(Step.Note("🛰️", language.pick(tablet.missionAr, tablet.mission))) + fieldSteps("fields", tablet.fields)
        Station.PORTAL -> storySentences(tablet.ending).let { s ->
            s.mapIndexed { i, line -> Step.Sentence(line, i + 1, s.size, null) }
        } + fieldSteps("gate", tablet.gate) + Step.Note("🏁", tablet.expeditionPayoff(language) + "\n" + language.pick("المكافأة: ", "Recompensa: ") + tablet.reward(language))
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
    val steps = remember(station, tablet.id) { buildSteps(station, tablet, data, scene, searchTargets, tableCells, language) }
    var index by remember(station) { mutableIntStateOf(0) }
    val step = steps[index]
    val done = when (step) {
        is Step.Choice -> step.key in results
        Step.Search -> searchTargets.all { "search-${it.id}" in results }
        Step.Order -> "order" in results
        else -> true
    }
    val last = index == steps.lastIndex

    Column(Modifier.fillMaxSize().background(AdventureBg).padding(horizontal = 14.dp, vertical = 8.dp)) {
        // Title, progress dots and close.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${station.emoji} ${station.label(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (i == index) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i <= index) SolarAmber else AdventureCardBorder)
                )
            }
            TextButton(onClick = onClose) { Text("✕", color = TextSecondary, fontSize = 18.sp) }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            key(station, index) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (step) {
                        is Step.Sentence -> SentenceCard(step, viewModel, language)
                        is Step.Choice -> ChoiceCard(step, results, viewModel, language)
                        Step.Lesson -> LessonCard(tablet, viewModel, language)
                        Step.Search -> if (scene != null) HiddenObjectSearch(scene, searchTargets, results, viewModel, language)
                        Step.Order -> OrderPuzzle(tablet.order, tablet.orderTranslation(language), results, viewModel, language)
                        is Step.Note -> if (step.text.isNotBlank()) AdventureCard(borderColor = NebulaPurple) {
                            Text(step.emoji, fontSize = 30.sp)
                            Text(step.text, color = TextPrimary, fontSize = 17.sp, lineHeight = 26.sp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        BlasterCyberButton(
            text = when {
                !last -> language.pick("التالي ▶", "Siguiente ▶")
                station == Station.PORTAL -> language.pick("افتحي البوابة 🌀", "Abrir el portal 🌀")
                else -> language.pick("تمّ! تابعي المشي ▶", "¡Hecho! Seguir caminando ▶")
            },
            onClick = { if (last) onSolved() else index++ },
            enabled = done,
            color = if (last) SuccessGreen else ExplorerBlue,
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

/** A question answered by tapping one of four big answers. Wrong taps cost a diamond. */
@Composable
private fun ChoiceCard(step: Step.Choice, results: MutableMap<String, Boolean>, viewModel: BlasterViewModel, language: HelperLanguage) {
    val onMistake = LocalOnMistake.current
    val wrong = remember { mutableStateListOf<String>() }
    val accepted = remember(step) { step.answers.flatMap { it.split("/") }.map { normalizeAnswer(it) } }
    val solved = step.key in results
    AdventureCard(borderColor = if (solved) SuccessGreen else ExplorerBlue) {
        Text(step.prompt, color = TextPrimary, fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
    }
    step.options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { option ->
                val isRight = normalizeAnswer(option) in accepted
                val color = when {
                    solved && isRight -> SuccessGreen
                    option in wrong -> MeteorRed
                    else -> ExplorerBlue
                }
                Surface(
                    onClick = {
                        if (solved || option in wrong) return@Surface
                        if (isRight) {
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
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = color.copy(alpha = if (solved && isRight) 0.18f else 0.08f),
                    border = BorderStroke(2.dp, color),
                    modifier = Modifier.weight(1f).height(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(option, color = color, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
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
