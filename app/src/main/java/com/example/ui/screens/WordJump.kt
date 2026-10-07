package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.abs
import kotlin.math.sin

/**
 * Word Jump: the answers float across the sky as bubbles. Walk Lía with ◀ ▶, jump with ⤒ to land on
 * a bubble (and from a low bubble up to a high one), then press ✓ to choose the word she stands on.
 * Right words glow green; wrong ones turn red and drop her back to the ground.
 *
 * [check] decides whether an option is right; [onLanded] is called when the player chooses a bubble.
 */
@Composable
fun WordJump(
    options: List<String>,
    solved: Boolean,
    wrong: List<String>,
    check: (String) -> Boolean,
    onLanded: (String) -> Unit,
    modifier: Modifier = Modifier,
    boardHeight: Dp = 340.dp
) {
    val lia = ImageBitmap.imageResource(R.drawable.explorer_walk)
    val measurer = rememberTextMeasurer()
    val u = with(LocalDensity.current) { 1.dp.toPx() }          // all sizes below are in dp
    val currentCheck by rememberUpdatedState(check)
    val currentOnLanded by rememberUpdatedState(onLanded)
    val currentWrong by rememberUpdatedState(wrong)
    var clock by remember { mutableFloatStateOf(0f) }
    var boardSize by remember { mutableStateOf(Size.Zero) }

    // Lía's feet, in pixels on the board; vy is her vertical speed (negative is up).
    var x by remember { mutableFloatStateOf(-1f) }
    var y by remember { mutableFloatStateOf(0f) }
    var vy by remember { mutableFloatStateOf(0f) }
    var facing by remember { mutableFloatStateOf(1f) }
    var walking by remember { mutableStateOf(false) }
    var holdLeft by remember { mutableStateOf(false) }
    var holdRight by remember { mutableStateOf(false) }
    var riding by remember { mutableStateOf<Int?>(null) }       // bubble Lía stands on
    var sparkleAt by remember { mutableFloatStateOf(-10f) }     // burst of stars on a right answer
    var sparklePos by remember { mutableStateOf(Offset.Zero) }

    val bubbleH = 40f * u
    fun groundY(size: Size) = size.height - 14f * u
    // Two heights: low bubbles can be reached from the ground, high ones from a low bubble.
    fun bubbleCenter(i: Int, t: Float, size: Size): Offset {
        val tier = if (i % 2 == 0) 1 else 2
        val cy = groundY(size) - tier * 72f * u + bubbleH / 2 + sin(t * 1.4f + i) * 3f * u
        val speed = (16f + 6f * (i % 3)) * u
        val dir = if ((i / 2) % 2 == 0) 1f else -1f
        val span = size.width + 160f * u
        val raw = (i * 151f * u + dir * speed * t) % span
        val cx = (if (raw < 0) raw + span else raw) - 80f * u
        return Offset(cx, cy)
    }
    fun bubbleWidth(i: Int) = maxOf(96f * u, measurer.measure(options[i], TextStyle(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)).size.width + 30f * u)

    LaunchedEffect(Unit) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            val before = bubbleCenter(riding ?: 0, clock, boardSize)
            clock += dt
            val size = boardSize
            if (size.width == 0f) continue
            if (x < 0f) { x = size.width / 2f; y = groundY(size) }
            // Walking.
            val dir = (if (holdRight) 1f else 0f) - (if (holdLeft) 1f else 0f)
            walking = dir != 0f
            if (dir != 0f) facing = dir
            x = (x + dir * 150f * u * dt).coerceIn(16f * u, size.width - 16f * u)
            val ride = riding
            if (ride != null) {
                // Carried along by the bubble; walking off its edge makes her fall.
                val c = bubbleCenter(ride, clock, size)
                x = (x + (c.x - before.x)).coerceIn(16f * u, size.width - 16f * u)
                y = c.y - bubbleH / 2
                if (abs(x - c.x) > bubbleWidth(ride) / 2 || options[ride] in currentWrong) {
                    riding = null
                    vy = 0f
                }
            } else if (y < groundY(size) || vy < 0f) {
                val prev = y
                vy += 1500f * u * dt
                y += vy * dt
                // Land on top of a bubble while falling onto it.
                if (vy > 0f) {
                    val hit = options.indices.firstOrNull { i ->
                        val c = bubbleCenter(i, clock, size)
                        val top = c.y - bubbleH / 2
                        options[i] !in currentWrong && prev <= top + 2f * u && y >= top && abs(x - c.x) < bubbleWidth(i) / 2
                    }
                    if (hit != null) {
                        riding = hit
                        vy = 0f
                        y = bubbleCenter(hit, clock, size).y - bubbleH / 2
                    }
                }
                if (y >= groundY(size)) {
                    y = groundY(size)
                    vy = 0f
                }
            }
        }
    }

    fun jump() {
        if (solved) return
        val onGround = boardSize.height > 0f && y >= groundY(boardSize) - 0.5f
        if (onGround || riding != null) {
            riding = null
            vy = -560f * u
            y -= 1f
        }
    }

    fun choose() {
        val ride = riding ?: return
        if (solved) return
        val word = options[ride]
        if (word in wrong) return
        if (currentCheck(word)) {
            sparkleAt = clock
            sparklePos = bubbleCenter(ride, clock, boardSize)
        }
        currentOnLanded(word)
    }

    Column(modifier.fillMaxWidth().height(boardHeight)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
        ) {
            boardSize = size
            drawRect(Brush.verticalGradient(listOf(SpaceDeep, SpaceNavy, Color(0xFF2A1F6B))))
            repeat(28) { k ->
                val sx = (k * 97f * u) % size.width
                val sy = (k * 53f * u) % (size.height - 40f * u)
                drawCircle(StarWhite.copy(alpha = 0.25f + 0.25f * sin(clock * 2f + k)), radius = 1.5f * u, center = Offset(sx, sy))
            }
            drawRect(Color(0xFF1C2E5C), topLeft = Offset(0f, groundY(size)), size = Size(size.width, size.height - groundY(size)))

            // Bubbles.
            options.forEachIndexed { i, word ->
                val c = bubbleCenter(i, clock, size)
                val color = when {
                    solved && check(word) -> SuccessGreen
                    word in wrong -> MeteorRed
                    riding == i -> SolarGold
                    else -> DiamondCyan
                }
                val layout = measurer.measure(word, TextStyle(color = if (riding == i && !solved) SpaceNavy else Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold))
                val w = bubbleWidth(i)
                drawRoundRect(color.copy(alpha = if (word in wrong) 0.35f else 0.9f), topLeft = Offset(c.x - w / 2, c.y - bubbleH / 2), size = Size(w, bubbleH), cornerRadius = CornerRadius(bubbleH / 2))
                drawRoundRect(Color.White.copy(alpha = 0.6f), topLeft = Offset(c.x - w / 2, c.y - bubbleH / 2), size = Size(w, bubbleH), cornerRadius = CornerRadius(bubbleH / 2), style = Stroke(1.5f * u))
                drawText(layout, topLeft = Offset(c.x - layout.size.width / 2f, c.y - layout.size.height / 2f))
            }

            // Lía.
            if (x >= 0f) {
                val frame = when {
                    riding == null && y < groundY(size) - 0.5f -> 1
                    walking -> (clock * 9f).toInt() % 4
                    else -> 0
                }
                withTransform({
                    translate(x, y)
                    scale(facing, 1f, pivot = Offset.Zero)
                }) {
                    drawNiloSprite(lia, frame = frame, height = 72f * u)
                }
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
        Spacer(Modifier.height(8.dp))
        // Controls stay left-to-right even in Arabic so ◀ and ▶ point the way Lía walks.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PadButton("◀", ExplorerBlue, onPress = { holdLeft = true }, onRelease = { holdLeft = false })
                PadButton("▶", ExplorerBlue, onPress = { holdRight = true }, onRelease = { holdRight = false })
                Spacer(Modifier.weight(1f))
                PadButton("⤒", SolarAmber, onPress = { jump() })
                PadButton("✓", if (riding != null && !solved) SuccessGreen else SuccessGreen.copy(alpha = 0.4f), onPress = { choose() })
            }
        }
    }
}

/** Round control button; [onRelease] fires when the finger lifts (for hold-to-walk). */
@Composable
internal fun PadButton(symbol: String, color: Color, size: Dp = 58.dp, onPress: () -> Unit, onRelease: () -> Unit = {}) {
    val press by rememberUpdatedState(onPress)
    val release by rememberUpdatedState(onRelease)
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    press()
                    tryAwaitRelease()
                    release()
                })
            }
    ) {
        Box(contentAlignment = Alignment.Center) { Text(symbol, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold) }
    }
}
