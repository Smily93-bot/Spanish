package com.example.data.content

import android.content.Context
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * All learning content bundled with the app (no network, no API keys):
 *  - assets/vocab.json    → Parliva topic collections, Frequency 5000, course phrases, grammar guides
 *  - assets/campaign.json → the twelve Órbita chapters (A1.1 → C2.2) used as Reading Tablets
 */
class SpanishContent(
    val categories: List<VocabCategory>,
    val frequency: List<VocabWord>,
    val phrases: List<Phrase>,
    val grammar: List<GrammarGuide>,
    val tablets: List<ReadingTablet>
) {
    val topicWords: List<VocabWord> = categories.flatMap { it.words }
    private val bySpanish: Map<String, VocabWord> =
        (topicWords + frequency).associateBy { normalizeAnswer(it.shortSpanish) }

    fun lookup(spanish: String): VocabWord? = bySpanish[normalizeAnswer(spanish)]

    /** Words the player sees for a given level: topic collections first, then frequency words up to that CEFR band. */
    fun wordPool(playerLevel: Int): List<VocabWord> {
        val maxRank = when {
            playerLevel <= 2 -> 300
            playerLevel <= 5 -> 1000
            playerLevel <= 9 -> 2500
            else -> 5000
        }
        return topicWords.filter { !it.spanish.contains(' ') || it.spanish.contains('/') } +
            frequency.filter { it.rank <= maxRank && it.partOfSpeech !in SKIPPED_PARTS && it.shortSpanish.length > 2 }
    }

    // ---------------------------------------------------------------- Meteor Blaster

    fun meteorRound(mode: BlasterMode, language: HelperLanguage, playerLevel: Int, random: Random = Random): MeteorWord =
        when (mode) {
            BlasterMode.TRANSLATION -> translationMeteor(language, playerLevel, random)
            BlasterMode.SYNONYM -> pairMeteor(SYNONYMS, random)
            BlasterMode.ANTONYM -> pairMeteor(ANTONYMS, random)
        }

    private fun translationMeteor(language: HelperLanguage, playerLevel: Int, random: Random): MeteorWord {
        val pool = wordPool(playerLevel)
        val target = pool.random(random)
        val answer = target.meaning(language).split("/").first().trim()
        val distractors = pool.asSequence()
            .shuffled(random)
            .map { it.meaning(language).split("/").first().trim() }
            .filter { normalizeAnswer(it) != normalizeAnswer(answer) && it.isNotBlank() }
            .distinct()
            .take(3)
            .toList()
        return MeteorWord(
            prompt = target.shortSpanish,
            answer = answer,
            options = (distractors + answer).shuffled(random),
            spanishToSpeak = target.shortSpanish,
            category = target.category,
            englishMeaning = target.english
        )
    }

    private fun pairMeteor(pairs: List<WordPair>, random: Random): MeteorWord {
        val pair = pairs.random(random)
        val flipped = random.nextBoolean()
        val prompt = if (flipped) pair.second else pair.first
        val answer = if (flipped) pair.first else pair.second
        val distractors = pairs.asSequence()
            .filter { it != pair }
            .shuffled(random)
            .map { if (random.nextBoolean()) it.first else it.second }
            .distinct()
            .take(3)
            .toList()
        return MeteorWord(
            prompt = prompt,
            answer = answer,
            options = (distractors + answer).shuffled(random),
            spanishToSpeak = prompt,
            category = "pairs",
            englishMeaning = if (flipped) pair.secondEn else pair.firstEn
        )
    }

    // ---------------------------------------------------------------- Quantum Cloze

    fun clozeQuestion(language: HelperLanguage, level: CefrLevel?, random: Random = Random): ClozeQuestion {
        val candidates = clozeReady.filter { level == null || it.level == level.code }.ifEmpty { clozeReady }
        val word = candidates.random(random)
        val gapRegex = wordRegex(word.shortSpanish)
        val match = gapRegex.find(word.exampleEs)!!
        val answer = match.value
        val gapped = word.exampleEs.replaceRange(match.range, "_____")
        val samePart = clozeReady.filter { it.partOfSpeech == word.partOfSpeech && it.shortSpanish != word.shortSpanish }
        val distractors = (samePart.ifEmpty { clozeReady }).asSequence()
            .shuffled(random)
            .map { matchCase(it.shortSpanish, answer) }
            .filter { normalizeAnswer(it) != normalizeAnswer(answer) }
            .distinct()
            .take(3)
            .toList()
        return ClozeQuestion(
            sentenceWithGap = gapped,
            fullSentence = word.exampleEs,
            translation = language.pick(word.exampleAr, word.exampleEn),
            answer = answer,
            options = (distractors + answer).shuffled(random),
            meaning = word.meaning(language),
            level = word.level
        )
    }

    private val clozeReady: List<VocabWord> by lazy {
        frequency.filter {
            it.exampleEs.isNotBlank() && it.shortSpanish.length > 1 && it.partOfSpeech !in SKIPPED_PARTS &&
                wordRegex(it.shortSpanish).containsMatchIn(it.exampleEs)
        }
    }

    private fun wordRegex(word: String) =
        Regex("(?<![\\p{L}])" + Regex.escape(word) + "(?![\\p{L}])", setOf(RegexOption.IGNORE_CASE))

    private fun matchCase(word: String, model: String) =
        if (model.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() } else word

    companion object {
        private val SKIPPED_PARTS = setOf("article", "punctuation", "number", "contraction")

        fun load(context: Context): SpanishContent {
            val vocab = JSONObject(context.assets.open("vocab.json").bufferedReader().use { it.readText() })
            val campaign = JSONObject(context.assets.open("campaign.json").bufferedReader().use { it.readText() })

            val categories = vocab.getJSONArray("categories").objects().map { c ->
                VocabCategory(
                    id = c.getString("id"),
                    icon = c.optString("icon"),
                    title = c.getString("title"),
                    english = c.optString("en"),
                    arabic = c.optString("ar"),
                    words = c.getJSONArray("items").objects().map { w ->
                        VocabWord(
                            spanish = w.getString("word"),
                            english = w.optString("en"),
                            arabic = w.optString("ar"),
                            level = w.optString("level", "A1"),
                            category = c.getString("id"),
                            exampleEs = w.optString("example")
                        )
                    }
                )
            }
            val frequency = vocab.getJSONArray("frequency").objects().map { w ->
                VocabWord(
                    spanish = w.getString("w"),
                    english = w.optString("en"),
                    arabic = w.optString("ar"),
                    level = w.optString("l", "A1"),
                    category = "frequency",
                    partOfSpeech = w.optString("p"),
                    exampleEs = w.optString("esx"),
                    exampleEn = w.optString("enx"),
                    exampleAr = w.optString("arx"),
                    rank = w.optInt("r")
                )
            }
            val phrases = vocab.getJSONArray("phrases").objects().map { p ->
                Phrase(
                    spanish = p.getString("es"),
                    english = p.optString("en"),
                    arabic = p.optString("ar"),
                    level = p.optString("level", "A1"),
                    unit = p.optString("unit"),
                    ruleEn = p.optString("ruleEn"),
                    ruleAr = p.optString("ruleAr")
                )
            }
            val grammar = vocab.getJSONArray("grammar").objects().map { g ->
                GrammarGuide(
                    id = g.getString("id"),
                    level = g.optString("level"),
                    title = g.optString("title"),
                    english = g.optString("en"),
                    arabic = g.optString("ar"),
                    formula = g.optString("formula"),
                    examples = g.optJSONArray("examples")?.strings().orEmpty(),
                    mistakeEn = g.optString("mistakeEn"),
                    mistakeAr = g.optString("mistakeAr")
                )
            }
            val tablets = campaign.getJSONArray("levels").objects().map { l ->
                val lesson = l.getJSONObject("lesson")
                val table = l.getJSONObject("table")
                val expedition = l.optJSONObject("expedition")
                ReadingTablet(
                    id = l.getString("id"),
                    level = l.getString("level"),
                    title = l.getString("title"),
                    goal = l.optString("goal"),
                    goalAr = l.optString("goalAr"),
                    reward = l.optString("reward"),
                    story = l.getString("story"),
                    storyAr = l.optString("storyAr"),
                    opening = l.getJSONArray("opening").fields(),
                    lesson = TabletLesson(
                        title = lesson.optString("title"),
                        english = lesson.optString("en"),
                        arabic = lesson.optString("ar"),
                        example = lesson.optString("example")
                    ),
                    table = GrammarTable(
                        headers = table.getJSONArray("headers").strings(),
                        rows = table.getJSONArray("rows").let { rows -> (0 until rows.length()).map { rows.getJSONArray(it).strings() } }
                    ),
                    order = l.getJSONArray("order").strings(),
                    orderEn = l.optString("orderEn"),
                    mission = l.optString("mission"),
                    missionAr = l.optString("missionAr"),
                    fields = l.getJSONArray("fields").fields(),
                    ending = l.optString("ending"),
                    gate = l.getJSONArray("gate").fields(),
                    expeditionGoal = expedition?.optString("goal").orEmpty(),
                    expeditionPayoff = expedition?.optString("payoff").orEmpty()
                )
            }
            return SpanishContent(categories, frequency, phrases, grammar, tablets)
        }

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
        private fun JSONArray.fields(): List<TabletField> = objects().map { f ->
            TabletField(
                label = f.getString("label"),
                answers = f.getJSONArray("answers").strings(),
                hint = f.optString("hint"),
                hintAr = f.optString("ar")
            )
        }
    }
}

data class WordPair(val first: String, val second: String, val firstEn: String, val secondEn: String)

private fun pair(a: String, b: String, aEn: String, bEn: String = aEn) = WordPair(a, b, aEn, bEn)

val SYNONYMS = listOf(
    pair("bonito", "lindo", "pretty"),
    pair("bello", "hermoso", "beautiful"),
    pair("rápido", "veloz", "fast"),
    pair("contento", "alegre", "happy / cheerful"),
    pair("empezar", "comenzar", "to begin"),
    pair("terminar", "acabar", "to finish"),
    pair("casa", "hogar", "house / home"),
    pair("coche", "auto", "car"),
    pair("mirar", "observar", "to look / to observe"),
    pair("caminar", "andar", "to walk"),
    pair("enorme", "gigante", "huge"),
    pair("listo", "inteligente", "clever"),
    pair("fácil", "sencillo", "easy / simple"),
    pair("difícil", "complicado", "difficult / complicated"),
    pair("cara", "rostro", "face"),
    pair("volver", "regresar", "to return"),
    pair("conseguir", "lograr", "to achieve"),
    pair("pelo", "cabello", "hair"),
    pair("alumno", "estudiante", "student"),
    pair("trabajo", "empleo", "job"),
    pair("idioma", "lengua", "language"),
    pair("delgado", "flaco", "thin"),
    pair("enfadado", "enojado", "angry"),
    pair("contestar", "responder", "to answer"),
    pair("querer", "desear", "to want / to wish"),
    pair("elegir", "escoger", "to choose"),
    pair("comida", "alimento", "food"),
    pair("anciano", "viejo", "old (person)"),
    pair("barco", "buque", "ship"),
    pair("cansado", "agotado", "tired / exhausted"),
    pair("enseguida", "inmediatamente", "right away"),
    pair("rico", "adinerado", "rich"),
    pair("miedo", "temor", "fear"),
    pair("chico", "muchacho", "boy"),
    pair("hablar", "conversar", "to talk"),
    pair("ayudar", "asistir", "to help"),
    pair("lugar", "sitio", "place"),
    pair("dinero", "plata", "money"),
    pair("feliz", "dichoso", "happy"),
    pair("error", "fallo", "mistake")
)

val ANTONYMS = listOf(
    pair("grande", "pequeño", "big", "small"),
    pair("feliz", "triste", "happy", "sad"),
    pair("arriba", "abajo", "up", "down"),
    pair("rápido", "lento", "fast", "slow"),
    pair("nuevo", "viejo", "new", "old"),
    pair("fácil", "difícil", "easy", "difficult"),
    pair("entrar", "salir", "to enter", "to leave"),
    pair("abrir", "cerrar", "to open", "to close"),
    pair("alto", "bajo", "tall", "short"),
    pair("cerca", "lejos", "near", "far"),
    pair("caliente", "frío", "hot", "cold"),
    pair("dentro", "fuera", "inside", "outside"),
    pair("día", "noche", "day", "night"),
    pair("comprar", "vender", "to buy", "to sell"),
    pair("antes", "después", "before", "after"),
    pair("siempre", "nunca", "always", "never"),
    pair("bueno", "malo", "good", "bad"),
    pair("mucho", "poco", "a lot", "a little"),
    pair("ganar", "perder", "to win", "to lose"),
    pair("rico", "pobre", "rich", "poor"),
    pair("limpio", "sucio", "clean", "dirty"),
    pair("lleno", "vacío", "full", "empty"),
    pair("claro", "oscuro", "light", "dark"),
    pair("fuerte", "débil", "strong", "weak"),
    pair("subir", "bajar", "to go up", "to go down"),
    pair("encender", "apagar", "to switch on", "to switch off"),
    pair("recordar", "olvidar", "to remember", "to forget"),
    pair("aceptar", "rechazar", "to accept", "to reject"),
    pair("joven", "mayor", "young", "older"),
    pair("temprano", "tarde", "early", "late"),
    pair("verdad", "mentira", "truth", "lie"),
    pair("amor", "odio", "love", "hate"),
    pair("preguntar", "responder", "to ask", "to answer"),
    pair("ancho", "estrecho", "wide", "narrow"),
    pair("caro", "barato", "expensive", "cheap"),
    pair("primero", "último", "first", "last"),
    pair("mejor", "peor", "better", "worse"),
    pair("ruido", "silencio", "noise", "silence"),
    pair("llegar", "partir", "to arrive", "to depart"),
    pair("guerra", "paz", "war", "peace")
)
