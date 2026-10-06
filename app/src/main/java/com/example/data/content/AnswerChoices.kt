package com.example.data.content

import com.example.data.model.ReadingTablet
import com.example.data.model.normalizeAnswer
import kotlin.random.Random

/**
 * Turns the expedition's written answers into tap-to-choose answers (Reading Blaster style):
 * wrong options are real Spanish forms that look like the answer (roja → rojo, tiene → tienes).
 */
class AnswerChoices(tablets: List<ReadingTablet>) {

    /** Every answer and conjugation form in the campaign: real Spanish words to draw from. */
    private val pool: List<String> = tablets.flatMap { t ->
        t.allQuestions.flatMap { it.answers } + t.table.rows.flatMap { it.drop(1) }
    }.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    fun options(answers: List<String>, random: Random = Random, count: Int = 3): List<String> {
        val correct = answers.first().split("/").first().trim()
        val accepted = answers.flatMap { it.split("/") }.map { normalizeAnswer(it) }.toSet()
        fun ok(c: String) = c.isNotBlank() && normalizeAnswer(c) !in accepted
        val picked = LinkedHashSet<String>()
        // 1. Same word, wrong ending (gender / number): roja → rojo, rojas.
        variants(correct).filter(::ok).shuffled(random).take(2).forEach { picked += it }
        // 2. Real forms that share the beginning of the answer: tiene → tienes, tienen, tengo.
        val stem = normalizeAnswer(correct).take(3)
        pool.filter { ok(it) && normalizeAnswer(it).startsWith(stem) && it !in picked }
            .shuffled(random).forEach { if (picked.size < count) picked += it }
        // 3. Anything else of a similar length.
        pool.filter { ok(it) && it !in picked && kotlin.math.abs(it.length - correct.length) <= 3 }
            .shuffled(random).forEach { if (picked.size < count) picked += it }
        val distinct = picked.distinctBy { normalizeAnswer(it) }.take(count)
        return (distinct + correct).shuffled(random)
    }

    private fun variants(word: String): List<String> {
        if (word.contains(' ')) return emptyList()
        val out = mutableListOf<String>()
        when {
            word.endsWith("a") -> { out += word.dropLast(1) + "o"; out += word + "s" }
            word.endsWith("o") -> { out += word.dropLast(1) + "a"; out += word + "s" }
            word.endsWith("as") || word.endsWith("os") -> { out += word.dropLast(1) }
        }
        return out
    }
}
