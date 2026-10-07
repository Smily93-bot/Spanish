package com.example.flavor

import com.example.data.content.ant
import com.example.data.content.syn
import java.util.Locale

/** Everything that makes this build Spanish Blaster: the TTS voice and the synonym/antonym pairs. */
object TargetLanguage {
    /** Voices tried in order; the first one the device has is used. */
    val voiceLocales = listOf(Locale("es", "ES"), Locale("es", "MX"), Locale("es", "US"), Locale("es"))

    val synonyms = listOf(
        syn("bonito", "lindo", "pretty", "جميل"),
        syn("rápido", "veloz", "fast", "سريع"),
        syn("contento", "alegre", "happy / cheerful", "مسرور"),
        syn("empezar", "comenzar", "to begin", "يبدأ"),
        syn("terminar", "acabar", "to finish", "يُنهي"),
        syn("casa", "hogar", "house / home", "بيت"),
        syn("coche", "auto", "car", "سيارة"),
        syn("mirar", "observar", "to look / to observe", "ينظر"),
        syn("caminar", "andar", "to walk", "يمشي"),
        syn("enorme", "gigante", "huge", "ضخم"),
        syn("listo", "inteligente", "clever", "ذكي"),
        syn("fácil", "sencillo", "easy / simple", "سهل"),
        syn("difícil", "complicado", "difficult / complicated", "صعب"),
        syn("cara", "rostro", "face", "وجه"),
        syn("volver", "regresar", "to return", "يعود"),
        syn("conseguir", "lograr", "to achieve", "يحقق"),
        syn("pelo", "cabello", "hair", "شعر"),
        syn("alumno", "estudiante", "student", "طالب"),
        syn("trabajo", "empleo", "job", "عمل"),
        syn("idioma", "lengua", "language", "لغة"),
        syn("delgado", "flaco", "thin", "نحيف"),
        syn("enfadado", "enojado", "angry", "غاضب"),
        syn("contestar", "responder", "to answer", "يجيب"),
        syn("querer", "desear", "to want / to wish", "يريد"),
        syn("elegir", "escoger", "to choose", "يختار"),
        syn("comida", "alimento", "food", "طعام"),
        syn("anciano", "viejo", "old (person)", "مُسن"),
        syn("barco", "buque", "ship", "سفينة"),
        syn("cansado", "agotado", "tired / exhausted", "متعب"),
        syn("enseguida", "inmediatamente", "right away", "فورًا"),
        syn("rico", "adinerado", "rich", "غني"),
        syn("miedo", "temor", "fear", "خوف"),
        syn("chico", "muchacho", "boy", "صبي"),
        syn("hablar", "conversar", "to talk", "يتحدث"),
        syn("ayudar", "asistir", "to help", "يساعد"),
        syn("lugar", "sitio", "place", "مكان"),
        syn("dinero", "plata", "money", "مال"),
        syn("error", "fallo", "mistake", "خطأ")
    )

    val antonyms = listOf(
        ant("grande", "pequeño", "big", "small", "كبير", "صغير"),
        ant("feliz", "triste", "happy", "sad", "سعيد", "حزين"),
        ant("arriba", "abajo", "up", "down", "فوق", "تحت"),
        ant("rápido", "lento", "fast", "slow", "سريع", "بطيء"),
        ant("nuevo", "viejo", "new", "old", "جديد", "قديم"),
        ant("fácil", "difícil", "easy", "difficult", "سهل", "صعب"),
        ant("entrar", "salir", "to enter", "to leave", "يدخل", "يخرج"),
        ant("abrir", "cerrar", "to open", "to close", "يفتح", "يغلق"),
        ant("alto", "bajo", "tall", "short", "طويل", "قصير"),
        ant("cerca", "lejos", "near", "far", "قريب", "بعيد"),
        ant("caliente", "frío", "hot", "cold", "ساخن", "بارد"),
        ant("dentro", "fuera", "inside", "outside", "داخل", "خارج"),
        ant("día", "noche", "day", "night", "نهار", "ليل"),
        ant("comprar", "vender", "to buy", "to sell", "يشتري", "يبيع"),
        ant("antes", "después", "before", "after", "قبل", "بعد"),
        ant("siempre", "nunca", "always", "never", "دائمًا", "أبدًا"),
        ant("bueno", "malo", "good", "bad", "جيد", "سيئ"),
        ant("mucho", "poco", "a lot", "a little", "كثير", "قليل"),
        ant("ganar", "perder", "to win", "to lose", "يفوز", "يخسر"),
        ant("rico", "pobre", "rich", "poor", "غني", "فقير"),
        ant("limpio", "sucio", "clean", "dirty", "نظيف", "متسخ"),
        ant("lleno", "vacío", "full", "empty", "ممتلئ", "فارغ"),
        ant("claro", "oscuro", "light", "dark", "فاتح", "مظلم"),
        ant("fuerte", "débil", "strong", "weak", "قوي", "ضعيف"),
        ant("subir", "bajar", "to go up", "to go down", "يصعد", "ينزل"),
        ant("encender", "apagar", "to switch on", "to switch off", "يُشغّل", "يُطفئ"),
        ant("recordar", "olvidar", "to remember", "to forget", "يتذكر", "ينسى"),
        ant("aceptar", "rechazar", "to accept", "to reject", "يقبل", "يرفض"),
        ant("joven", "mayor", "young", "older", "شاب", "أكبر سنًا"),
        ant("temprano", "tarde", "early", "late", "باكرًا", "متأخرًا"),
        ant("verdad", "mentira", "truth", "lie", "حقيقة", "كذبة"),
        ant("amor", "odio", "love", "hate", "حب", "كره"),
        ant("preguntar", "responder", "to ask", "to answer", "يسأل", "يجيب"),
        ant("ancho", "estrecho", "wide", "narrow", "عريض", "ضيق"),
        ant("caro", "barato", "expensive", "cheap", "غالٍ", "رخيص"),
        ant("primero", "último", "first", "last", "أول", "آخر"),
        ant("mejor", "peor", "better", "worse", "أفضل", "أسوأ"),
        ant("ruido", "silencio", "noise", "silence", "ضجيج", "صمت"),
        ant("llegar", "partir", "to arrive", "to depart", "يصل", "يغادر"),
        ant("guerra", "paz", "war", "peace", "حرب", "سلام")
    )
}

/** On-screen text in the target language. The shared code is written in Spanish, so this build shows it unchanged. */
fun tl(text: String): String = text

/** Same word with the wrong gender or number, used as tempting wrong answers: roja → rojo, rojas. */
fun wordEndingVariants(word: String): List<String> {
    if (word.contains(' ')) return emptyList()
    val out = mutableListOf<String>()
    when {
        word.endsWith("a") -> { out += word.dropLast(1) + "o"; out += word + "s" }
        word.endsWith("o") -> { out += word.dropLast(1) + "a"; out += word + "s" }
        word.endsWith("as") || word.endsWith("os") -> { out += word.dropLast(1) }
    }
    return out
}
