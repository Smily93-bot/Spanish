package com.example.data.model

import com.example.flavor.tl

/** Language used for hints, translations and UI helper text. Spanish is always the target language. */
enum class HelperLanguage { ARABIC, ENGLISH }

/** Picks the Arabic or English variant of a helper string. */
fun HelperLanguage.pick(arabic: String, english: String): String =
    if (this == HelperLanguage.ARABIC) isolateLatin(arabic) else english

private val LATIN_RUN = Regex(
    "[¿¡]?[A-Za-zÀ-ÖØ-öø-ÿ][A-Za-zÀ-ÖØ-öø-ÿ0-9'’\\-]*" +
        "(?:[ ,/·:+]+[¿¡]?[A-Za-zÀ-ÖØ-öø-ÿ][A-Za-zÀ-ÖØ-öø-ÿ0-9'’\\-]*)*"
)

/**
 * Wraps each run of Spanish/English words inside Arabic text in Unicode isolates (LRI…PDI) so
 * mixed sentences such as "يشترك ir وser في fui، fuiste، fue" keep the Spanish in reading order.
 */
fun isolateLatin(text: String): String {
    if (text.none { it in '\u0600'..'\u06FF' }) return text
    return LATIN_RUN.replace(text) { "\u2066${it.value}\u2069" }
}

/** CEFR levels covered by the bundled content. */
enum class CefrLevel(val code: String) {
    A1("A1"), A2("A2"), B1("B1"), B2("B2"), C1("C1"), C2("C2");

    companion object {
        fun fromCode(code: String): CefrLevel =
            entries.firstOrNull { code.startsWith(it.code) } ?: A1
    }
}

/** A single vocabulary entry (topic collection or Frequency 5000 list). */
data class VocabWord(
    val spanish: String,
    val english: String,
    val arabic: String,
    val level: String,
    val category: String,
    val partOfSpeech: String = "",
    val exampleEs: String = "",
    val exampleEn: String = "",
    val exampleAr: String = "",
    val rank: Int = 0
) {
    /** The first form of entries such as "profesor / profesora". */
    val shortSpanish: String get() = spanish.split("/").first().trim()
    fun meaning(language: HelperLanguage) = language.pick(arabic, english)
    /** The main sense only, e.g. "of" from "of; from" — used for quiz options. */
    fun shortMeaning(language: HelperLanguage): String =
        meaning(language).split(';', '/', '؛').first().trim().ifEmpty { meaning(language) }
}

data class VocabCategory(
    val id: String,
    val icon: String,
    val title: String,
    val english: String,
    val arabic: String,
    val words: List<VocabWord>
)

data class Phrase(
    val spanish: String,
    val english: String,
    val arabic: String,
    val level: String,
    val unit: String,
    val ruleEn: String,
    val ruleAr: String
)

data class GrammarGuide(
    val id: String,
    val level: String,
    val title: String,
    val titleAr: String,
    val english: String,
    val arabic: String,
    val formula: String,
    val examples: List<String>,
    val mistakeEn: String,
    val mistakeAr: String
)

/** A written-answer prompt in a reading tablet. Any of [answers] is accepted. */
data class TabletField(
    val label: String,
    val answers: List<String>,
    val hint: String,
    val hintAr: String
)

data class GrammarTable(val headers: List<String>, val rows: List<List<String>>)

data class TabletLesson(val title: String, val titleAr: String, val english: String, val arabic: String, val example: String)

/** A tappable object in a hidden-object scene. Boxes are (x, y, width, height) as fractions of the image. */
data class HiddenObject(
    val id: String,
    val spanish: String,
    val english: String,
    val arabic: String,
    val boxes: List<List<Float>>
) {
    fun meaning(language: HelperLanguage) = language.pick(arabic, english)
    /** The main sense only, e.g. "of" from "of; from" — used for quiz options. */
    fun shortMeaning(language: HelperLanguage): String =
        meaning(language).split(';', '/', '؛').first().trim().ifEmpty { meaning(language) }
}

/** A detailed room illustration (cabin, lab, archive) used by the expedition search missions. */
data class HiddenScene(val id: String, val asset: String, val name: String, val objects: List<HiddenObject>)

/** One chapter of the Órbita campaign, presented as a "Reading Tablet". */
data class ReadingTablet(
    val id: String,
    val level: String,
    val title: String,
    val titleAr: String,
    val goal: String,
    val goalAr: String,
    val reward: String,
    val rewardAr: String,
    val story: String,
    val storyAr: String,
    val opening: List<TabletField>,
    val lesson: TabletLesson,
    val table: GrammarTable,
    val order: List<String>,
    val orderEn: String,
    val orderAr: String,
    val mission: String,
    val missionAr: String,
    val fields: List<TabletField>,
    val ending: String,
    val gate: List<TabletField>,
    val expeditionGoal: String,
    val expeditionGoalAr: String,
    val expeditionPayoff: String,
    val expeditionPayoffAr: String,
    /** Hidden-object scene id (cabin, lab, archive) and the Spanish objects to find in it. */
    val scene: String,
    val targets: List<String>
) {
    fun title(language: HelperLanguage) = language.pick(titleAr, title)
    fun reward(language: HelperLanguage) = language.pick(rewardAr, reward)
    fun goal(language: HelperLanguage) = language.pick(goalAr, goal)
    fun orderTranslation(language: HelperLanguage) = language.pick(orderAr, orderEn)
    fun expeditionGoal(language: HelperLanguage) = language.pick(expeditionGoalAr, expeditionGoal)
    fun expeditionPayoff(language: HelperLanguage) = language.pick(expeditionPayoffAr, expeditionPayoff)

    val cefr: CefrLevel get() = CefrLevel.fromCode(level)
    val allQuestions: List<TabletField> get() = opening + fields + gate
}

/** The three Meteor Blaster game modes. Names are stored in the arcade_scores table. */
enum class BlasterMode(val labelEs: String, val labelEn: String, val labelAr: String) {
    TRANSLATION(tl("Significado"), "Meaning", "المعنى"),
    SYNONYM(tl("Sinónimos"), "Synonyms", "المرادفات"),
    ANTONYM(tl("Antónimos"), "Antonyms", "الأضداد");

    fun label(language: HelperLanguage) = language.pick(labelAr, labelEn)
}

/** A meteor carrying the Spanish prompt; the player blasts the matching answer. */
data class MeteorWord(
    val prompt: String,
    val answer: String,
    val options: List<String>,
    val spanishToSpeak: String,
    val category: String,
    /** Meaning of the prompt in the helper language (shown under synonym/antonym prompts). */
    val hint: String,
    /** English meaning, stored in the word-mastery table. */
    val englishMeaning: String
)

/** A fill-in-the-gap question built from a Frequency 5000 example sentence. */
data class ClozeQuestion(
    val sentenceWithGap: String,
    val fullSentence: String,
    val translation: String,
    val answer: String,
    val options: List<String>,
    val meaning: String,
    val level: String,
    /** Short grammar reason for the answer, in the helper language. */
    val why: String = ""
)

// ------------------------------------------------------------------ Grammar ¿Por qué?

data class GrammarPattern(val label: String, val formula: String, val english: String, val arabic: String) {
    fun note(language: HelperLanguage) = language.pick(arabic, english)
}

data class GrammarExample(val spanish: String, val english: String, val arabic: String) {
    fun translation(language: HelperLanguage) = language.pick(arabic, english)
}

/** A fill-the-gap grammar question; [question] contains ___ once (or twice, with "a / b" options). */
data class GrammarQuestion(
    val question: String,
    val english: String,
    val arabic: String,
    val options: List<String>,
    val answer: Int,
    val whyEn: String,
    val whyAr: String
) {
    val correct: String get() = options[answer]
    fun translation(language: HelperLanguage) = language.pick(arabic, english)
    fun why(language: HelperLanguage) = language.pick(whyAr, whyEn)

    /** The sentence with [option] written into the gap(s). */
    fun filled(option: String = correct): String {
        val parts = option.split(" / ")
        var i = 0
        return GAP.replace(question) { parts.getOrElse(i++) { parts.last() } }
    }

    companion object {
        val GAP = Regex("_{3,}")
    }
}

/** One grammar rule with its explanation card and practice questions. */
data class GrammarTopic(
    val id: String,
    val level: String,
    val kind: String,
    val titleEs: String,
    val titleEn: String,
    val titleAr: String,
    val introEn: String,
    val introAr: String,
    val tipEn: String,
    val tipAr: String,
    val patterns: List<GrammarPattern>,
    val examples: List<GrammarExample>,
    val questions: List<GrammarQuestion>
) {
    fun title(language: HelperLanguage) = language.pick(titleAr, titleEn)
    fun intro(language: HelperLanguage) = language.pick(introAr, introEn)
    fun tip(language: HelperLanguage) = language.pick(tipAr, tipEn)
}

/** Explorer ranks unlocked by player level. */
enum class Rank(val minLevel: Int, val spanish: String, val english: String, val arabic: String, val emoji: String) {
    CADET(1, tl("Cadete"), "Cadet", "طالب طيران", "🧑‍🚀"),
    EXPLORER(3, tl("Exploradora"), "Explorer", "مستكشفة", "🛰️"),
    PILOT(6, tl("Piloto"), "Pilot", "طيّارة", "🚀"),
    CAPTAIN(10, tl("Capitana"), "Captain", "قبطانة", "🌟"),
    COMMANDER(15, tl("Comandante"), "Commander", "قائدة", "🪐"),
    ADMIRAL(22, tl("Almirante galáctica"), "Galactic Admiral", "أميرال المجرة", "👑");

    fun label(language: HelperLanguage) = language.pick(arabic, english)

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
        fun next(level: Int): Rank? = entries.firstOrNull { it.minLevel > level }
    }
}

/** Ship upgrades bought with star credits in the Hangar. Higher tiers grant more shield. */
data class ShipTier(
    val tier: Int,
    val name: String,
    val cost: Int,
    val maxShield: Int,
    val emoji: String
)

val SHIP_TIERS = listOf(
    ShipTier(1, tl("Colibrí"), 0, 100, "🛸"),
    ShipTier(2, tl("Halcón"), 250, 120, "🚀"),
    ShipTier(3, tl("Cóndor"), 600, 140, "🛰️"),
    ShipTier(4, tl("Quetzal"), 1200, 165, "🌠"),
    ShipTier(5, tl("Estrella Azul"), 2200, 200, "💎")
)

fun shipTier(tier: Int): ShipTier = SHIP_TIERS.firstOrNull { it.tier == tier } ?: SHIP_TIERS.first()

/** Normalises answers so accents, punctuation and case don't make a correct answer fail. */
fun normalizeAnswer(text: String): String =
    java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()
        .replace(Regex("[¿?¡!.,;:«»\"“”]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
