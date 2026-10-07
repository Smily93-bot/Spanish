package com.example.ui.screens

import com.example.flavor.tl
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.HelperLanguage
import com.example.data.model.pick
import com.example.ui.components.AudioButton
import com.example.ui.theme.*

/** Something Nilo says: Spanish line plus its meaning in each helper language. */
data class NiloLine(val es: String, val ar: String, val en: String) {
    fun meaning(language: HelperLanguage) = language.pick(ar, en)
}

object NiloLines {
    val greeting = NiloLine(tl("¡Hola! Soy Nilo, el piloto de la nave."), "مرحبًا! أنا نيلو، طيّار السفينة.", "Hi! I'm Nilo, the ship's pilot.")
    val start = NiloLine(tl("¡Vamos, Lía!"), "هيا يا ليا!", "Let's go, Lía!")
    val signal = NiloLine(tl("¡Mira, una señal!"), "انظري، إشارة!", "Look, a signal!")
    val diamond = NiloLine(tl("¡Un diamante!"), "ماسة!", "A diamond!")
    val idle = NiloLine(tl("Camina hacia la luz azul."), "امشي نحو الضوء الأزرق.", "Walk to the blue light.")
    val portalOpen = NiloLine(tl("¡El portal está abierto!"), "البوابة مفتوحة!", "The portal is open!")
    val almost = NiloLine(tl("¡Ya casi lo tenemos!"), "اقتربنا كثيرًا!", "We're almost there!")
    val locked = NiloLine(tl("¡Esta puerta está cerrada!"), "هذا الباب مغلق!", "This door is locked!")
    val needKey = NiloLine(tl("¡Necesitamos la llave!"), "نحتاج المفتاح!", "We need the key!")
    val doorOpen = NiloLine(tl("¡La puerta se abrió!"), "انفتح الباب!", "The door is open!")
    val findKey = NiloLine(tl("¡Mira, la llave!"), "انظري، المفتاح!", "Look, the key!")
    val gotKey = NiloLine(tl("¡Tenemos la llave! ¡A la salida!"), "معنا المفتاح! إلى المخرج!", "We have the key! To the exit!")
    val home = NiloLine(tl("¡Lo logramos! ¡Una página más!"), "نجحنا! صفحة أخرى!", "We did it! One more page!")
    val praise = listOf(
        NiloLine(tl("¡Muy bien!"), "أحسنتِ!", "Well done!"),
        NiloLine(tl("¡Genial!"), "رائع!", "Great!"),
        NiloLine(tl("¡Excelente, Lía!"), "ممتاز يا ليا!", "Excellent, Lía!"),
        NiloLine(tl("¡Lo lograste!"), "لقد نجحتِ!", "You did it!")
    )
    val missionIntro = listOf(
        NiloLine(tl("Lía, llega un mensaje. ¡Escucha!"), "ليا، وصلت رسالة. استمعي!", "Lía, a message is coming in. Listen!"),
        NiloLine(tl("Responde para abrir el camino."), "أجيبي لفتح الطريق.", "Answer to open the path."),
        NiloLine(tl("Necesito estas cosas. ¿Me ayudas a buscarlas?"), "أحتاج هذه الأشياء. هل تساعدينني في البحث عنها؟", "I need these things. Can you help me find them?"),
        NiloLine(tl("La consola está rota. ¡Repárala!"), "لوحة التحكم معطلة. أصلحيها!", "The console is broken. Fix it!"),
        NiloLine(tl("El mensaje está desordenado. ¡Ordénalo!"), "الرسالة مبعثرة. رتّبيها!", "The message is scrambled. Put it in order!"),
        NiloLine(tl("¡Es hora de la misión!"), "حان وقت المهمة!", "Mission time!"),
        NiloLine(tl("Última prueba antes del portal."), "آخر اختبار قبل البوابة.", "Last test before the portal.")
    )
    val searchHint = NiloLine(tl("¡Mira dentro del círculo dorado!"), "انظري داخل الدائرة الذهبية!", "Look inside the gold circle!")
}

/**
 * Draws one frame of Nilo's walk cycle (nilo_walk.webp, built from Lía's sheet by tools/make_nilo.py)
 * with his feet at the current origin, facing right, [height] units tall.
 */
fun DrawScope.drawNiloSprite(sprite: ImageBitmap, frame: Int, height: Float) {
    val r = WALK_FRAMES[frame % WALK_FRAMES.size]
    val w = r[2].toFloat() / r[3] * height
    drawImage(
        sprite,
        srcOffset = IntOffset(r[0], r[1]),
        srcSize = IntSize(r[2], r[3]),
        dstOffset = IntOffset((-w / 2).toInt(), (-height).toInt()),
        dstSize = IntSize(w.toInt(), height.toInt())
    )
}

/** Nilo's portrait with what he says, shown above mission panels. */
@Composable
fun NiloSays(line: NiloLine, language: HelperLanguage, onSpeak: () -> Unit, size: Dp = 56.dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val sprite = ImageBitmap.imageResource(R.drawable.nilo_walk)
        Canvas(Modifier.size(size)) {
            translate(this.size.width / 2f, this.size.height) {
                drawNiloSprite(sprite, frame = 0, height = this@Canvas.size.height)
            }
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
                AudioButton(onClick = onSpeak, size = 32.dp)
            }
        }
    }
}
