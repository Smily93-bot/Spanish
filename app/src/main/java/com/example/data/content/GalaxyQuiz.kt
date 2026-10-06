package com.example.data.content

import com.example.data.model.HelperLanguage
import com.example.data.model.VocabWord
import com.example.data.model.normalizeAnswer
import kotlin.random.Random

/** Exercise types used to make a word stick: recognise, recall, hear and spell it. */
enum class GalaxyExercise { MEANING, REVERSE, LISTEN, SPELL }

data class GalaxyQuestion(
    val word: VocabWord,
    val type: GalaxyExercise,
    /** Text options for MEANING / REVERSE / LISTEN; letter tiles for SPELL. */
    val options: List<String>,
    val answer: String
)

/** Builds Word Galaxy lessons and reviews from the frequency list. */
class GalaxyQuiz(private val frequency: List<VocabWord>) {

    private val byRank = frequency.associateBy { it.rank }

    fun word(rank: Int): VocabWord? = byRank[rank]

    /** New-word lesson: every word is met as meaning, then recall, then by ear or spelling. */
    fun lesson(ranks: List<Int>, language: HelperLanguage, random: Random = Random): List<GalaxyQuestion> {
        val words = ranks.mapNotNull { byRank[it] }
        val round1 = words.shuffled(random).map { question(it, GalaxyExercise.MEANING, language, random) }
        val round2 = words.shuffled(random).map { question(it, GalaxyExercise.REVERSE, language, random) }
        val round3 = words.shuffled(random).map {
            val type = if (canSpell(it) && random.nextBoolean()) GalaxyExercise.SPELL else GalaxyExercise.LISTEN
            question(it, type, language, random)
        }
        return round1 + round2 + round3
    }

    /** Review: one question per due word; harder types for words that have climbed higher. */
    fun review(ranks: List<Int>, boxes: Map<Int, Int>, language: HelperLanguage, random: Random = Random): List<GalaxyQuestion> =
        ranks.mapNotNull { byRank[it] }.shuffled(random).map { w ->
            val box = boxes[w.rank] ?: 1
            val types = buildList {
                add(GalaxyExercise.MEANING)
                add(GalaxyExercise.LISTEN)
                if (box >= 2) add(GalaxyExercise.REVERSE)
                if (box >= 3 && canSpell(w)) add(GalaxyExercise.SPELL)
            }
            question(w, types.random(random), language, random)
        }

    /** A second try after a miss uses a different exercise. */
    fun retry(previous: GalaxyQuestion, language: HelperLanguage, random: Random = Random): GalaxyQuestion {
        val type = when (previous.type) {
            GalaxyExercise.MEANING -> GalaxyExercise.REVERSE
            GalaxyExercise.REVERSE -> GalaxyExercise.MEANING
            GalaxyExercise.LISTEN -> GalaxyExercise.MEANING
            GalaxyExercise.SPELL -> GalaxyExercise.REVERSE
        }
        return question(previous.word, type, language, random)
    }

    fun question(word: VocabWord, type: GalaxyExercise, language: HelperLanguage, random: Random = Random): GalaxyQuestion =
        when (type) {
            GalaxyExercise.MEANING -> {
                val answer = word.shortMeaning(language)
                GalaxyQuestion(word, type, options(answer, neighbours(word, random).map { it.shortMeaning(language) }, random), answer)
            }
            GalaxyExercise.REVERSE, GalaxyExercise.LISTEN -> {
                val answer = word.shortSpanish
                GalaxyQuestion(word, type, options(answer, neighbours(word, random).map { it.shortSpanish }, random), answer)
            }
            GalaxyExercise.SPELL -> {
                val answer = word.shortSpanish.lowercase()
                val decoys = "aeiounrsltcdm".filter { it !in answer }.toList().shuffled(random).take(2)
                GalaxyQuestion(word, type, (answer.toList() + decoys).map { it.toString() }.shuffled(random), answer)
            }
        }

    private fun canSpell(word: VocabWord) = word.shortSpanish.length in 3..10 && word.shortSpanish.all { it.isLetter() }

    /** Distractors from nearby ranks, same part of speech when possible, so options feel equally plausible. */
    private fun neighbours(word: VocabWord, random: Random): List<VocabWord> {
        val window = frequency.filter { kotlin.math.abs(it.rank - word.rank) <= 200 && it.rank != word.rank }
        val samePart = window.filter { it.partOfSpeech == word.partOfSpeech }
        return (samePart.shuffled(random) + window.shuffled(random) + frequency.shuffled(random).take(30))
    }

    private fun options(answer: String, candidates: List<String>, random: Random): List<String> {
        val key = normalizeAnswer(answer)
        val picked = LinkedHashMap<String, String>()
        for (c in candidates) {
            val k = normalizeAnswer(c)
            if (c.isBlank() || k == key || k in picked) continue
            picked[k] = c
            if (picked.size == 3) break
        }
        return (picked.values + answer).shuffled(random)
    }
}
