package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.AudioButton
import com.example.ui.theme.*
import kotlin.math.sin

/** Something Nilo says: Spanish line plus its meaning in each helper language. */
data class NiloLine(val es: String, val ar: String, val en: String) {
    fun meaning(language: HelperLanguage) = language.pick(ar, en)
}

object NiloLines {
    val greeting = NiloLine("¡Hola! Soy Nilo, el piloto de la nave.", "مرحبًا! أنا نيلو، طيّار السفينة.", "Hi! I'm Nilo, the ship's pilot.")
    val start = NiloLine("¡Vamos, Lía!", "هيا يا ليا!", "Let's go, Lía!")
    val signal = NiloLine("¡Mira, una señal!", "انظري، إشارة!", "Look, a signal!")
    val diamond = NiloLine("¡Un diamante!", "ماسة!", "A diamond!")
    val idle = NiloLine("Camina hacia la luz azul.", "امشي نحو الضوء الأزرق.", "Walk to the blue light.")
    val portalOpen = NiloLine("¡El portal está abierto!", "البوابة مفتوحة!", "The portal is open!")
    val home = NiloLine("¡Lo logramos! ¡Una página más!", "نجحنا! صفحة أخرى!", "We did it! One more page!")
    val praise = listOf(
        NiloLine("¡Muy bien!", "أحسنتِ!", "Well done!"),
        NiloLine("¡Genial!", "رائع!", "Great!"),
        NiloLine("¡Excelente, Lía!", "ممتاز يا ليا!", "Excellent, Lía!"),
        NiloLine("¡Lo lograste!", "لقد نجحتِ!", "You did it!")
    )
    val missionIntro = listOf(
        NiloLine("Lía, llega un mensaje. ¡Escucha!", "ليا، وصلت رسالة. استمعي!", "Lía, a message is coming in. Listen!"),
        NiloLine("Responde para abrir el camino.", "أجيبي لفتح الطريق.", "Answer to open the path."),
        NiloLine("Necesito estas cosas. ¿Me ayudas a buscarlas?", "أحتاج هذه الأشياء. هل تساعدينني في البحث عنها؟", "I need these things. Can you help me find them?"),
        NiloLine("La consola está rota. ¡Repárala!", "لوحة التحكم معطلة. أصلحيها!", "The console is broken. Fix it!"),
        NiloLine("El mensaje está desordenado. ¡Ordénalo!", "الرسالة مبعثرة. رتّبيها!", "The message is scrambled. Put it in order!"),
        NiloLine("¡Es hora de la misión!", "حان وقت المهمة!", "Mission time!"),
        NiloLine("Última prueba antes del portal.", "آخر اختبار قبل البوابة.", "Last test before the portal.")
    )
    val searchHint = NiloLine("¡Mira dentro del círculo dorado!", "انظري داخل الدائرة الذهبية!", "Look inside the gold circle!")
}

private val OUTLINE = Color(0xFF1A2238)
private val SUIT = Color(0xFFE07A10)
private val SUIT_DARK = Color(0xFFB85F0A)
private val WHITE = Color(0xFFF5F7FB)
private val GREY = Color(0xFF9AA4B8)
private val GLASS = Color(0xFFBFE9FA)
private val SKIN = Color(0xFF8D5524)
private val HAIR = Color(0xFF2B1B10)

private fun DrawScope.part(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) {
    drawRoundRect(color, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(r))
    drawRoundRect(OUTLINE, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = CornerRadius(r), style = Stroke(2.5f))
}

private fun DrawScope.ball(color: Color, x: Float, y: Float, r: Float, outline: Boolean = true) {
    drawCircle(color, radius = r, center = Offset(x, y))
    if (outline) drawCircle(OUTLINE, radius = r, center = Offset(x, y), style = Stroke(2.5f))
}

/**
 * Draws Nilo with his feet at the current origin, facing right, about 135 units tall
 * (Lía is 112 units, the helmet makes him look a little taller).
 */
fun DrawScope.drawNilo(phase: Float, moving: Boolean, airborne: Boolean) {
    val swing = if (moving) sin(phase) * 31f else 0f // degrees
    val legs = if (airborne) 23f else swing

    fun leg(angle: Float, color: Color) = withTransform({
        translate(0f, -30f)
        rotate(angle, pivot = Offset.Zero)
    }) {
        part(color, -7f, 0f, 14f, 22f, 6f)
        part(WHITE, -9f, 18f, 20f, 12f, 5f)
    }

    fun arm(angle: Float, color: Color) = withTransform({
        translate(0f, -58f)
        rotate(angle, pivot = Offset.Zero)
    }) {
        part(color, -6f, 0f, 12f, 22f, 6f)
        ball(WHITE, 0f, 24f, 7f)
    }

    part(GREY, -27f, -64f, 12f, 30f, 5f) // backpack
    leg(-legs, SUIT_DARK)
    arm(swing, SUIT_DARK)
    part(SUIT, -19f, -66f, 38f, 38f, 12f) // body
    part(WHITE, -10f, -58f, 20f, 14f, 4f) // chest panel
    ball(DiamondCyan, -3f, -51f, 2.5f, outline = false)
    ball(MeteorRed, 4f, -51f, 2.5f, outline = false)
    part(Color(0xFF0A1633), -19f, -35f, 38f, 6f, 3f) // belt
    leg(legs, SUIT)

    // Helmet and face
    ball(WHITE, 0f, -90f, 29f)
    ball(GLASS, 4f, -89f, 22f)
    ball(SKIN, 5f, -88f, 16f, outline = false)
    drawArc(HAIR, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(-12f, -107f), size = Size(32f, 16f))
    drawOval(Color.White, topLeft = Offset(-3.2f, -91.8f), size = Size(6.4f, 7.6f))
    drawOval(Color.White, topLeft = Offset(7.8f, -91.8f), size = Size(6.4f, 7.6f))
    ball(Color(0xFF111111), 0.8f, -87.5f, 2f, outline = false)
    ball(Color(0xFF111111), 11.8f, -87.5f, 2f, outline = false)
    drawArc(Color(0xFF111111), startAngle = 14f, sweepAngle = 152f, useCenter = false, topLeft = Offset(1.5f, -86.5f), size = Size(9f, 9f), style = Stroke(1.8f))
    drawArc(Color.White.copy(alpha = 0.8f), startAngle = -143f, sweepAngle = 46f, useCenter = false, topLeft = Offset(-14f, -107f), size = Size(36f, 36f), style = Stroke(3f))
    part(GREY, -2f, -128f, 4f, 12f, 2f) // antenna
    ball(SolarGold, 0f, -130f, 5f)

    arm(-swing, SUIT)
}

/** Nilo's portrait with what he says, shown above mission panels. */
@Composable
fun NiloSays(line: NiloLine, language: HelperLanguage, onSpeak: () -> Unit, size: Dp = 56.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(size)) {
            val s = this.size.height / 140f
            withTransform({
                translate(this@Canvas.size.width / 2f, this@Canvas.size.height)
                scale(s, s, pivot = Offset.Zero)
            }) { drawNilo(phase = 0f, moving = false, airborne = false) }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
            color = AdventureSurface,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, SolarAmber),
            modifier = Modifier.weight(1f)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Nilo: " + line.es, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(line.meaning(language), color = TextSecondary, fontSize = 12.sp)
                }
                AudioButton(onClick = onSpeak, size = 32.dp, tint = SolarAmber)
            }
        }
    }
}
