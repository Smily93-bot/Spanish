package com.example.data.content

import com.example.data.model.ReadingTablet
import com.example.flavor.wordEndingVariants
import com.example.data.model.normalizeAnswer
import kotlin.random.Random

/**
 * Turns the expedition's written answers into tap-to-choose answers (Reading Blaster style):
 * wrong options are real forms that look like the answer (roja → rojo, tiene → tienes).
 * They come from the same chapter or earlier ones, so a beginner never sees an advanced word,
 * and a changed ending is only offered when it is a real word ([isWord]).
 */
class AnswerChoices(private val tablets: List<ReadingTablet>, private val isWord: (String) -> Boolean) {

    private fun formsOf(t: ReadingTablet): List<String> =
        (t.allQuestions.flatMap { it.answers } + t.table.rows.flatMap { it.drop(1) })
            .flatMap { it.split("/") }.map { it.trim() }.filter { it.isNotEmpty() }

    /** Every answer and conjugation form in the campaign: real words to draw from. */
    private val pool: List<String> = tablets.flatMap(::formsOf).distinct()

    fun options(answers: List<String>, tablet: ReadingTablet? = null, random: Random = Random, count: Int = 3): List<String> {
        val correct = answers.first().split("/").first().trim()
        val accepted = answers.flatMap { it.split("/") }.map { normalizeAnswer(it) }.toSet()
        fun ok(c: String) = c.isNotBlank() && normalizeAnswer(c) !in accepted
        // Closest first: this chapter, then the chapters before it.
        val upTo = tablet?.let { tablets.indexOf(it) }?.takeIf { it >= 0 } ?: tablets.lastIndex
        val local = tablet?.let { formsOf(it).distinct() }.orEmpty()
        val earlier = tablets.take(upTo + 1).flatMap(::formsOf).distinct() - local.toSet()
        val known = pool.toSet()
        val picked = LinkedHashSet<String>()
        // 1. Same word, wrong ending (gender / number) — only if that is a real word: roja → rojo, rojas.
        wordEndingVariants(correct).filter { ok(it) && (it in known || isWord(it)) }.shuffled(random).take(2).forEach { picked += it }
        // 2. Real forms that share the beginning of the answer: tiene → tienes, tienen, tengo.
        val stem = normalizeAnswer(correct).take(3)
        for (source in listOf(local, earlier)) {
            source.filter { ok(it) && normalizeAnswer(it).startsWith(stem) && it !in picked }
                .shuffled(random).forEach { if (picked.size < count) picked += it }
        }
        // 3. Other answers of a similar length from this chapter, then earlier ones.
        for (source in listOf(local, earlier)) {
            source.filter { ok(it) && it !in picked && kotlin.math.abs(it.length - correct.length) <= 3 }
                .shuffled(random).forEach { if (picked.size < count) picked += it }
        }
        val distinct = picked.distinctBy { normalizeAnswer(it) }.take(count)
        return (distinct + correct).shuffled(random)
    }
}
