package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                viewModel.completeTablet(tablet, results.values.count { it }, totalQuestions, bonusCredits = collected.size * 5)
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
        say(if (i == Station.entries.lastIndex) NiloLines.portalOpen else NiloLines.praise.random())
        if (i == Station.entries.lastIndex) targetX = PORTAL_X
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            IconButton(onClick = { viewModel.closeTablet() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column(Modifier.weight(1f)) {
                Text("${tablet.level} · ${tablet.title(language)}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, maxLines = 1)
                Text(tablet.expeditionGoal(language), color = TextSecondary, fontSize = 11.sp, maxLines = 2)
            }
            Text("💎 ${collected.size}", color = SolarAmber, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
        }

        // The game world
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(0.dp))
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

        val station = openStation
        if (station == null) {
            WalkPanel(
                tablet = tablet,
                language = language,
                progress = solved.size,
                niloLine = niloLine,
                onSpeakNilo = { viewModel.speakSpanish((niloLine ?: NiloLines.idle).es) },
                onLeft = { holdLeft = it },
                onRight = { holdRight = it },
                onJump = { jump() }
            )
        } else {
            MissionPanel(
                station = Station.entries[station],
                tablet = tablet,
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
    niloLine: NiloLine?,
    onSpeakNilo: () -> Unit,
    onLeft: (Boolean) -> Unit,
    onRight: (Boolean) -> Unit,
    onJump: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(AdventureBg).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AdventureCard(borderColor = SolarGold) {
            Text(
                language.pick("المهمة ${progress + 1} من ${Station.entries.size}", "Misión ${minOf(progress + 1, Station.entries.size)} / ${Station.entries.size}"),
                color = SolarAmber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold
            )
            Spacer(Modifier.height(4.dp))
            ProgressBar(progress / Station.entries.size.toFloat(), color = SolarGold, height = 8.dp)
        }
        NiloSays(niloLine ?: NiloLines.idle, language, onSpeak = onSpeakNilo)
        Spacer(Modifier.weight(1f))
        // Controls stay left-to-right even in Arabic so ◀ and ▶ point the way Lía walks.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                HoldButton("◀", onLeft)
                HoldButton("▶", onRight)
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = CircleShape,
                    color = SolarAmber,
                    modifier = Modifier.size(78.dp).pointerInput(Unit) { detectTapGestures(onPress = { onJump() }) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(language.pick("قفز", "SALTO"), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }
            }
        }
        Text(
            language.pick("يمكنك أيضًا لمس المشهد لتمشي ليا إلى هناك.", "Tip: tap the scene to walk there, or tap above Lía to jump."),
            color = TextSecondary, fontSize = 11.sp
        )
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

@Composable
private fun MissionPanel(
    station: Station,
    tablet: ReadingTablet,
    scene: HiddenScene?,
    searchTargets: List<HiddenObject>,
    tableCells: Set<Pair<Int, Int>>,
    results: MutableMap<String, Boolean>,
    viewModel: BlasterViewModel,
    language: HelperLanguage,
    onClose: () -> Unit,
    onSolved: () -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AdventureBg)
            .verticalScroll(scroll)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${station.emoji} ${station.es}", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(station.label(language), color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("✕", color = TextSecondary, fontSize = 18.sp) }
        }
        val intro = NiloLines.missionIntro[station.ordinal]
        NiloSays(intro, language, onSpeak = { viewModel.speakSpanish(intro.es) })

        val complete: Boolean = when (station) {
            Station.STORY -> {
                StoryCard(language.pick("📡 رسالة واردة", "📡 Transmisión entrante"), tablet.story, tablet.storyAr, viewModel, language)
                true
            }
            Station.OPENING -> {
                QuestionGroup("opening", tablet.opening, results, viewModel, language)
                tablet.opening.indices.all { "opening-$it" in results }
            }
            Station.SEARCH -> {
                if (scene == null || searchTargets.isEmpty()) true
                else {
                    HiddenObjectSearch(scene, searchTargets, results, viewModel, language)
                    searchTargets.all { "search-${it.id}" in results }
                }
            }
            Station.CONSOLE -> {
                LessonCard(tablet, viewModel, language)
                GrammarTableQuiz(tablet.table, tableCells, results, viewModel, language)
                tableCells.all { "table-${it.first}-${it.second}" in results }
            }
            Station.ORDER -> {
                OrderPuzzle(tablet.order, tablet.orderTranslation(language), results, viewModel, language)
                "order" in results
            }
            Station.MISSION -> {
                AdventureCard(borderColor = NebulaPurple) {
                    Text(language.pick("🛰️ المهمة", "🛰️ Misión"), color = NebulaPurple, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    Text(language.pick(tablet.missionAr, tablet.mission), color = TextPrimary, fontSize = 14.sp)
                }
                QuestionGroup("fields", tablet.fields, results, viewModel, language)
                tablet.fields.indices.all { "fields-$it" in results }
            }
            Station.PORTAL -> {
                StoryCard(language.pick("📖 خاتمة الفصل", "📖 Final del capítulo"), tablet.ending, null, viewModel, language)
                QuestionGroup("gate", tablet.gate, results, viewModel, language)
                val done = tablet.gate.indices.all { "gate-$it" in results }
                if (done) {
                    AdventureCard(borderColor = SuccessGreen) {
                        Text("🏁 " + tablet.expeditionPayoff(language), color = SuccessGreen, fontWeight = FontWeight.Bold)
                        Text(language.pick("المكافأة: ", "Recompensa: ") + tablet.reward(language), color = TextSecondary, fontSize = 12.sp)
                    }
                }
                done
            }
        }

        BlasterCyberButton(
            text = if (station == Station.PORTAL) language.pick("افتحي البوابة 🌀", "Abrir el portal 🌀")
            else language.pick("تمّ! تابعي المشي ▶", "¡Hecho! Seguir caminando ▶"),
            onClick = onSolved,
            enabled = complete,
            color = SuccessGreen,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
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
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var mistakes by remember { mutableIntStateOf(0) }
    var wrongTap by remember { mutableStateOf<Offset?>(null) }
    val found = targets.filter { "search-${it.id}" in results }
    val remaining = targets.filter { "search-${it.id}" !in results }
    val hinted = if (mistakes >= 3) remaining.firstOrNull() else null

    AdventureCard(borderColor = ExplorerBlue) {
        Text(language.pick("🔍 ابحثي عن هذه الأشياء في الغرفة واضغطي عليها:", "🔍 Find these objects in the room and tap them:"), color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            targets.forEach { obj ->
                val done = obj in found
                Surface(
                    onClick = { viewModel.speakSpanish(obj.spanish) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (done) SuccessGreen.copy(alpha = 0.15f) else AdventureSurfaceVariant,
                    border = BorderStroke(1.dp, if (done) SuccessGreen else ExplorerBlue)
                ) {
                    Text(
                        (if (done) "✓ " else "🔊 ") + obj.spanish,
                        color = if (done) SuccessGreen else TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1536f / 1024f)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, ExplorerBlue, RoundedCornerShape(14.dp))
            .onSizeChanged { imageSize = it }
            .pointerInput(remaining.size) {
                detectTapGestures { tap ->
                    if (imageSize.width == 0) return@detectTapGestures
                    val fx = tap.x / imageSize.width
                    val fy = tap.y / imageSize.height
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
                    } else {
                        mistakes++
                        wrongTap = tap
                        viewModel.soundEngine.error()
                    }
                }
            }
    ) {
        Image(
            painter = painterResource(sceneDrawable(scene.asset)),
            contentDescription = scene.name,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )
        Canvas(Modifier.matchParentSize()) {
            found.forEach { obj ->
                obj.boxes.forEach { b ->
                    drawRoundRect(
                        SuccessGreen,
                        topLeft = Offset(b[0] * size.width, b[1] * size.height),
                        size = Size(b[2] * size.width, b[3] * size.height),
                        cornerRadius = CornerRadius(10f),
                        style = Stroke(width = 5f)
                    )
                }
            }
            hinted?.boxes?.firstOrNull()?.let { b ->
                drawCircle(
                    SolarGold.copy(alpha = 0.5f),
                    radius = maxOf(b[2] * size.width, b[3] * size.height) * 0.9f,
                    center = Offset((b[0] + b[2] / 2) * size.width, (b[1] + b[3] / 2) * size.height),
                    style = Stroke(width = 6f)
                )
            }
            wrongTap?.let { drawCircle(MeteorRed, radius = 18f, center = it, style = Stroke(width = 4f)) }
        }
    }
    if (hinted != null) {
        NiloSays(NiloLines.searchHint, language, onSpeak = { viewModel.speakSpanish(NiloLines.searchHint.es) }, size = 44.dp)
    }
    Text(
        language.pick("اضغطي على الكلمة لسماع نطقها.", "Tap a word to hear it."),
        color = TextSecondary, fontSize = 11.sp
    )
}
