package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.sin

/**
 * Word Jump (after Reading Blaster's jumping game): the answers drift across the sky as glowing
 * bubbles; tap one and Lía jumps onto it. Right answers glow green and carry her; wrong ones turn
 * red and drop her back to the ground.
 *
 * [check] decides whether an option is right; [onLanded] is called once Lía lands on a bubble.
 */
@Composable
fun WordJump(
    options: List<String>,
    solved: Boolean,
    wrong: List<String>,
    check: (String) -> Boolean,
    onLanded: (String) -> Unit,
    modifier: Modifier = Modifier,
    boardHeight: Dp = 270.dp
) {
    val lia = ImageBitmap.imageResource(R.drawable.explorer_walk)
    val measurer = rememberTextMeasurer()
    val u = with(LocalDensity.current) { 1.dp.toPx() }          // all sizes below are in dp
    val currentCheck by rememberUpdatedState(check)
    val currentOnLanded by rememberUpdatedState(onLanded)
    var clock by remember { mutableFloatStateOf(0f) }
    var jumpTo by remember { mutableStateOf<Int?>(null) }      // bubble Lía is jumping to
    var jumpStart by remember { mutableFloatStateOf(0f) }
    var jumpFrom by remember { mutableStateOf<Int?>(null) }    // bubble she jumped from (null = ground)
    var riding by remember { mutableStateOf<Int?>(null) }      // bubble Lía stands on
    var fallStart by remember { mutableFloatStateOf(-10f) }
    var fallFrom by remember { mutableStateOf(Offset.Zero) }
    var boardSize by remember { mutableStateOf(Size.Zero) }
    var sparkleAt by remember { mutableFloatStateOf(-10f) }        // burst of stars on a right answer
    var sparklePos by remember { mutableStateOf(Offset.Zero) }
    val jumpTime = 0.55f

    // Each bubble drifts sideways in its own lane and wraps around the edges.
    fun bubbleCenter(i: Int, t: Float, size: Size): Offset {
        val lanes = options.size
        val laneH = (size.height - 150f * u) / lanes
        val y = 40f * u + laneH * i + laneH / 2f + sin(t * 1.6f + i) * 5f * u
        val speed = (40f + 14f * (i % 3)) * u
        val dir = if (i % 2 == 0) 1f else -1f
        val span = size.width + 180f * u
        val raw = (i * 137f * u + dir * speed * t) % span
        val x = (if (raw < 0) raw + span else raw) - 90f * u
        return Offset(x, y)
    }
    fun ground(size: Size) = Offset(size.width * 0.5f, size.height - 14f * u)
    fun bubbleTop(i: Int, t: Float, size: Size) = bubbleCenter(i, t, size) - Offset(0f, 22f * u)

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            clock += ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            val target = jumpTo
            if (target != null && clock - jumpStart >= jumpTime) {
                jumpTo = null
                val word = options[target]
                if (currentCheck(word)) {
                    riding = target
                    sparkleAt = clock
                    sparklePos = bubbleCenter(target, clock, boardSize)
                } else {
                    fallStart = clock
                    fallFrom = bubbleTop(target, clock, boardSize)
                }
                currentOnLanded(word)
            }
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(boardHeight)
            .clip(RoundedCornerShape(20.dp))
            .pointerInput(options, solved, wrong.size) {
                detectTapGestures { tap ->
                    if (solved || jumpTo != null) return@detectTapGestures
                    val hit = options.indices.firstOrNull { i ->
                        val c = bubbleCenter(i, clock, boardSize)
                        options[i] !in wrong && kotlin.math.abs(tap.x - c.x) < 70f * u && kotlin.math.abs(tap.y - c.y) < 28f * u
                    }
                    if (hit != null) {
                        jumpFrom = riding
                        riding = null
                        jumpTo = hit
                        jumpStart = clock
                    }
                }
            }
    ) {
        boardSize = size
        drawRect(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy, Color(0xFF2A1F6B))))
        repeat(28) { k ->
            val sx = (k * 97f * u) % size.width
            val sy = (k * 53f * u) % (size.height - 40f * u)
            drawCircle(StarWhite.copy(alpha = 0.25f + 0.25f * sin(clock * 2f + k)), radius = 1.5f * u, center = Offset(sx, sy))
        }
        drawRect(Color(0xFF1C2E5C), topLeft = Offset(0f, size.height - 14f * u), size = Size(size.width, 14f * u))

        // Bubbles.
        options.forEachIndexed { i, word ->
            val c = bubbleCenter(i, clock, size)
            val color = when {
                solved && check(word) -> SuccessGreen
                word in wrong -> MeteorRed
                else -> DiamondCyan
            }
            val layout = measurer.measure(word, TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold))
            val w = maxOf(96f * u, layout.size.width + 32f * u)
            val h = 44f * u
            drawRoundRect(color.copy(alpha = if (word in wrong) 0.35f else 0.9f), topLeft = Offset(c.x - w / 2, c.y - h / 2), size = Size(w, h), cornerRadius = CornerRadius(h / 2))
            drawRoundRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(c.x - w / 2, c.y - h / 2), size = Size(w, h), cornerRadius = CornerRadius(h / 2), style = Stroke(1.5f * u))
            drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
        }

        // Lía: on the ground, mid-jump, riding a bubble, or falling back down.
        val target = jumpTo
        val ride = riding
        val feet: Offset = when {
            target != null -> {
                val p = ((clock - jumpStart) / jumpTime).coerceIn(0f, 1f)
                val start = jumpFrom?.let { bubbleTop(it, clock, size) } ?: ground(size)
                val end = bubbleTop(target, clock, size)
                val arc = -110f * u * 4f * p * (1 - p)
                Offset(start.x + (end.x - start.x) * p, start.y + (end.y - start.y) * p + arc)
            }
            ride != null -> bubbleTop(ride, clock, size)
            clock - fallStart < 0.45f -> {
                val p = (clock - fallStart) / 0.45f
                val g = ground(size)
                Offset(fallFrom.x + (g.x - fallFrom.x) * p, fallFrom.y + (g.y - fallFrom.y) * p * p)
            }
            else -> ground(size)
        }
        translate(feet.x, feet.y) {
            drawNiloSprite(lia, frame = if (target != null) 1 else 0, height = 84f * u)
        }

        // Celebration: gold stars fly out from the right bubble.
        val age = clock - sparkleAt
        if (age in 0f..0.8f) {
            val alpha = 1f - age / 0.8f
            repeat(12) { k ->
                val angle = k * (2 * Math.PI / 12).toFloat()
                val r = (20f + 120f * age) * u
                val c = sparklePos + Offset(kotlin.math.cos(angle) * r, sin(angle) * r)
                drawCircle(SolarGold.copy(alpha = alpha), radius = (5f - 3f * age) * u, center = c)
            }
            drawCircle(SuccessGreen.copy(alpha = 0.35f * alpha), radius = (30f + 90f * age) * u, center = sparklePos, style = Stroke(3f * u))
        }
    }
}
