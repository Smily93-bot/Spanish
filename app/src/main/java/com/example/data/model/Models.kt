package com.example.data.model

/** Language used for hints, translations and UI helper text. Spanish is always the target language. */
enum class HelperLanguage { ARABIC, ENGLISH }

/** Picks the Arabic or English variant of a helper string. */
fun HelperLanguage.pick(arabic: String, english: String): String =
    if (this == HelperLanguage.ARABIC) arabic else english

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

data class TabletLesson(val title: String, val english: String, val arabic: String, val example: String)

/** One chapter of the Órbita campaign, presented as a "Reading Tablet". */
data class ReadingTablet(
    val id: String,
    val level: String,
    val title: String,
    val goal: String,
    val goalAr: String,
    val reward: String,
    val story: String,
    val storyAr: String,
    val opening: List<TabletField>,
    val lesson: TabletLesson,
    val table: GrammarTable,
    val order: List<String>,
    val orderEn: String,
    val mission: String,
    val missionAr: String,
    val fields: List<TabletField>,
    val ending: String,
    val gate: List<TabletField>,
    val expeditionGoal: String,
    val expeditionPayoff: String
) {
    val cefr: CefrLevel get() = CefrLevel.fromCode(level)
    val allQuestions: List<TabletField> get() = opening + fields + gate
}

/** The three Meteor Blaster game modes. Names are stored in the arcade_scores table. */
enum class BlasterMode(val labelEs: String, val labelEn: String, val labelAr: String) {
    TRANSLATION("Significado", "Meaning", "المعنى"),
    SYNONYM("Sinónimos", "Synonyms", "المرادفات"),
    ANTONYM("Antónimos", "Antonyms", "الأضداد");

    fun label(language: HelperLanguage) = language.pick(labelAr, labelEn)
}

/** A meteor carrying the Spanish prompt; the player blasts the matching answer. */
data class MeteorWord(
    val prompt: String,
    val answer: String,
    val options: List<String>,
    val spanishToSpeak: String,
    val category: String,
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
    val level: String
)

/** Explorer ranks unlocked by player level. */
enum class Rank(val minLevel: Int, val spanish: String, val english: String, val arabic: String, val emoji: String) {
    CADET(1, "Cadete", "Cadet", "طالب طيران", "🧑‍🚀"),
    EXPLORER(3, "Exploradora", "Explorer", "مستكشفة", "🛰️"),
    PILOT(6, "Piloto", "Pilot", "طيّارة", "🚀"),
    CAPTAIN(10, "Capitana", "Captain", "قبطانة", "🌟"),
    COMMANDER(15, "Comandante", "Commander", "قائدة", "🪐"),
    ADMIRAL(22, "Almirante galáctica", "Galactic Admiral", "أميرال المجرة", "👑");

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
    ShipTier(1, "Colibrí", 0, 100, "🛸"),
    ShipTier(2, "Halcón", 250, 120, "🚀"),
    ShipTier(3, "Cóndor", 600, 140, "🛰️"),
    ShipTier(4, "Quetzal", 1200, 165, "🌠"),
    ShipTier(5, "Estrella Azul", 2200, 200, "💎")
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
