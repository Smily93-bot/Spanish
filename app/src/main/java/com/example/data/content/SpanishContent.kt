package com.example.data.content

import com.example.flavor.lemmaCandidates
import android.content.Context
import com.example.data.model.*
import com.example.flavor.TargetLanguage
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

/**
 * All learning content bundled with the app (no network, no API keys):
 *  - assets/vocab.json    → Parliva topic collections, Frequency 5000, course phrases, grammar guides
 *  - assets/campaign.json → the twelve Órbita chapters (A1.1 → C2.2) and their hidden-object scenes
 *
 * Everything expensive (word pools, cloze sentences) is computed once in [parse], which runs on a
 * background thread, so starting a game never stalls the UI.
 */
class SpanishContent(
    val categories: List<VocabCategory>,
    val frequency: List<VocabWord>,
    val phrases: List<Phrase>,
    val grammar: List<GrammarGuide>,
    val tablets: List<ReadingTablet>,
    val scenes: Map<String, HiddenScene>,
    val grammarTopics: List<GrammarTopic> = emptyList()
) {
    val topicWords: List<VocabWord> = categories.flatMap { it.words }
    private val bySpanish: Map<String, VocabWord> =
        (topicWords + frequency).associateBy { normalizeAnswer(it.shortSpanish) }

    fun lookup(spanish: String): VocabWord? = bySpanish[normalizeAnswer(spanish)]

    /** Dictionary entry for a word as it appears in a sentence: the word itself, else its likely base forms. */
    fun glossary(word: String): VocabWord? {
        val w = word.lowercase().trim()
        return lookup(w) ?: lemmaCandidates(w).firstNotNullOfOrNull { lookup(it) }
    }

    /** Tap-to-choose answers for the expedition questions. */
    val answerChoices: AnswerChoices by lazy { AnswerChoices(tablets) }

    /** Word Galaxy lessons over the 5000 frequency words. */
    val galaxy: GalaxyQuiz by lazy { GalaxyQuiz(frequency) }

    private val singleTopicWords = topicWords.filter { !it.spanish.contains(' ') || it.spanish.contains('/') }
    private val poolCache = HashMap<Int, List<VocabWord>>()

    /** Words the player sees for a given level: topic collections first, then frequency words up to that CEFR band. */
    fun wordPool(playerLevel: Int): List<VocabWord> {
        val maxRank = when {
            playerLevel <= 2 -> 300
            playerLevel <= 5 -> 1000
            playerLevel <= 9 -> 2500
            else -> 5000
        }
        return synchronized(poolCache) {
            poolCache.getOrPut(maxRank) {
                singleTopicWords + frequency.filter {
                    it.rank <= maxRank && it.partOfSpeech !in SKIPPED_PARTS && it.shortSpanish.length > 2
                }
            }
        }
    }

    // ---------------------------------------------------------------- Meteor Blaster

    fun meteorRound(mode: BlasterMode, language: HelperLanguage, playerLevel: Int, random: Random = Random): MeteorWord =
        when (mode) {
            BlasterMode.TRANSLATION -> translationMeteor(language, playerLevel, random)
            BlasterMode.SYNONYM -> pairMeteor(TargetLanguage.synonyms, language, random)
            BlasterMode.ANTONYM -> pairMeteor(TargetLanguage.antonyms, language, random)
        }

    private fun translationMeteor(language: HelperLanguage, playerLevel: Int, random: Random): MeteorWord {
        val pool = wordPool(playerLevel)
        val target = pool.random(random)
        val answer = target.shortMeaning(language)
        val distractors = generateSequence { pool.random(random) }
            .take(60)
            .map { it.shortMeaning(language) }
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
            hint = answer,
            englishMeaning = target.english
        )
    }

    private fun pairMeteor(pairs: List<WordPair>, language: HelperLanguage, random: Random): MeteorWord {
        val pair = pairs.random(random)
        val flipped = random.nextBoolean()
        val prompt = if (flipped) pair.second else pair.first
        val answer = if (flipped) pair.first else pair.second
        val distractors = pairs.asSequence()
            .filter { it != pair }
            .shuffled(random)
            .map { if (random.nextBoolean()) it.first else it.second }
            .filter { it != prompt && it != answer }
            .distinct()
            .take(3)
            .toList()
        return MeteorWord(
            prompt = prompt,
            answer = answer,
            options = (distractors + answer).shuffled(random),
            spanishToSpeak = prompt,
            category = "pairs",
            hint = if (flipped) language.pick(pair.secondAr, pair.secondEn) else language.pick(pair.firstAr, pair.firstEn),
            englishMeaning = if (flipped) pair.secondEn else pair.firstEn
        )
    }

    // ---------------------------------------------------------------- Quantum Cloze

    /** A frequency word whose example sentence contains it, with the position of the gap precomputed. */
    private class ClozeSource(val word: VocabWord, val range: IntRange)

    private val clozeReady: List<ClozeSource> = frequency.mapNotNull { w ->
        if (w.exampleEs.isBlank() || w.shortSpanish.length <= 1 || w.partOfSpeech in SKIPPED_PARTS) return@mapNotNull null
        findWord(w.exampleEs, w.shortSpanish)?.let { ClozeSource(w, it) }
    }
    private val clozeByLevel: Map<String, List<ClozeSource>> = clozeReady.groupBy { it.word.level }
    private val clozeByPart: Map<String, List<ClozeSource>> = clozeReady.groupBy { it.word.partOfSpeech }

    fun clozeQuestion(language: HelperLanguage, level: CefrLevel?, random: Random = Random): ClozeQuestion {
        // The frequency list stops at C1, so C2 players get the hardest (C1) sentences.
        val band = if (level == CefrLevel.C2) CefrLevel.C1 else level
        val candidates = band?.let { clozeByLevel[it.code] }.orEmpty().ifEmpty { clozeReady }
        val source = candidates.random(random)
        val word = source.word
        val answer = word.exampleEs.substring(source.range)
        val gapped = word.exampleEs.replaceRange(source.range, "_____")
        // Prefer distractors with the same part of speech; top up from the whole list if there are too few.
        val samePart = clozeByPart[word.partOfSpeech].orEmpty()
        val distractors = (generateSequence { samePart.random(random) }.take(40) + generateSequence { clozeReady.random(random) }.take(40))
            .map { matchCase(it.word.shortSpanish, answer) }
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
            level = word.level,
            why = ClozeWhy.explain(word, word.exampleEs.substring(0, source.range.first), answer, language)
        )
    }

    private fun matchCase(word: String, model: String) =
        if (model.firstOrNull()?.isUpperCase() == true) word.replaceFirstChar { it.uppercase() } else word

    companion object {
        private val SKIPPED_PARTS = setOf("article", "punctuation", "number", "contraction")

        /** Case-insensitive whole-word search without regex (fast enough to run over all 5000 sentences). */
        fun findWord(sentence: String, word: String): IntRange? {
            var from = 0
            while (true) {
                val i = sentence.indexOf(word, from, ignoreCase = true)
                if (i < 0) return null
                val end = i + word.length
                val before = i == 0 || !sentence[i - 1].isLetter()
                val after = end >= sentence.length || !sentence[end].isLetter()
                if (before && after) return i until end
                from = i + 1
            }
        }

        fun load(context: Context): SpanishContent = parse(
            vocabJson = context.assets.open("vocab.json").bufferedReader().use { it.readText() },
            campaignJson = context.assets.open("campaign.json").bufferedReader().use { it.readText() },
            grammarJson = runCatching { context.assets.open("grammar_lab.json").bufferedReader().use { it.readText() } }.getOrNull()
        )

        fun parseGrammar(json: String): List<GrammarTopic> = JSONArray(json).objects().map { t ->
            GrammarTopic(
                id = t.getString("id"),
                level = t.getString("level"),
                kind = t.optString("kind"),
                titleEs = t.optString("titleEs"),
                titleEn = t.optString("titleEn"),
                titleAr = t.optString("titleAr"),
                introEn = t.optString("introEn"),
                introAr = t.optString("introAr"),
                tipEn = t.optString("tipEn"),
                tipAr = t.optString("tipAr"),
                patterns = t.getJSONArray("patterns").objects().map {
                    GrammarPattern(it.optString("label"), it.optString("formula"), it.optString("en"), it.optString("ar"))
                },
                examples = t.getJSONArray("examples").objects().map {
                    GrammarExample(it.optString("es"), it.optString("en"), it.optString("ar"))
                },
                questions = t.getJSONArray("questions").objects().map {
                    GrammarQuestion(
                        question = it.getString("q"),
                        english = it.optString("en"),
                        arabic = it.optString("ar"),
                        options = it.getJSONArray("options").strings(),
                        answer = it.getInt("answer"),
                        whyEn = it.optString("whyEn"),
                        whyAr = it.optString("whyAr")
                    )
                }
            )
        }

        fun parse(vocabJson: String, campaignJson: String, grammarJson: String? = null): SpanishContent {
            val vocab = JSONObject(vocabJson)
            val campaign = JSONObject(campaignJson)

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
                    titleAr = g.optString("titleAr", g.optString("title")),
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
                    titleAr = l.optString("titleAr", l.getString("title")),
                    goal = l.optString("goal"),
                    goalAr = l.optString("goalAr"),
                    reward = l.optString("reward"),
                    rewardAr = l.optString("rewardAr", l.optString("reward")),
                    story = l.getString("story"),
                    storyAr = l.optString("storyAr"),
                    opening = l.getJSONArray("opening").fields(),
                    lesson = TabletLesson(
                        title = lesson.optString("title"),
                        titleAr = lesson.optString("titleAr", lesson.optString("title")),
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
                    orderAr = l.optString("orderAr", l.optString("orderEn")),
                    mission = l.optString("mission"),
                    missionAr = l.optString("missionAr"),
                    fields = l.getJSONArray("fields").fields(),
                    ending = l.optString("ending"),
                    gate = l.getJSONArray("gate").fields(),
                    expeditionGoal = expedition?.optString("goal").orEmpty(),
                    expeditionGoalAr = expedition?.optString("goalAr").orEmpty(),
                    expeditionPayoff = expedition?.optString("payoff").orEmpty(),
                    expeditionPayoffAr = expedition?.optString("payoffAr").orEmpty(),
                    scene = l.optString("scene", "cabin"),
                    targets = l.optJSONArray("targets")?.strings().orEmpty()
                )
            }
            val scenesJson = campaign.optJSONObject("scenes") ?: JSONObject()
            val scenes = scenesJson.keys().asSequence().associateWith { id ->
                val s = scenesJson.getJSONObject(id)
                HiddenScene(
                    id = id,
                    asset = s.getString("asset"),
                    name = s.optString("name"),
                    objects = s.getJSONArray("objects").objects().map { o ->
                        val boxes = mutableListOf(o.getJSONArray("box").floats())
                        o.optJSONArray("extra")?.let { extra -> (0 until extra.length()).forEach { boxes += extra.getJSONArray(it).floats() } }
                        HiddenObject(
                            id = o.getString("id"),
                            spanish = o.getString("es"),
                            english = o.optString("en"),
                            arabic = o.optString("ar"),
                            boxes = boxes
                        )
                    }
                )
            }
            return SpanishContent(categories, frequency, phrases, grammar, tablets, scenes, grammarJson?.let { parseGrammar(it) }.orEmpty())
        }

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
        private fun JSONArray.floats(): List<Float> = (0 until length()).map { getDouble(it).toFloat() }
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

data class WordPair(
    val first: String,
    val second: String,
    val firstEn: String,
    val secondEn: String,
    val firstAr: String,
    val secondAr: String
)

/** Synonym pair: both words share one meaning. */
fun syn(a: String, b: String, en: String, ar: String) = WordPair(a, b, en, en, ar, ar)

/** Antonym pair with the meaning of each side. */
fun ant(a: String, b: String, aEn: String, bEn: String, aAr: String, bAr: String) = WordPair(a, b, aEn, bEn, aAr, bAr)
