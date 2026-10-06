package com.example.data.engagement

/**
 * Leitner spaced repetition for the 5000-word Galaxy. Pure Kotlin so it can be unit-tested.
 *
 * Box 0 = never introduced. A new word enters box 1 after its first lesson. Each correct review moves it
 * up one box and pushes the next review further away ([INTERVALS]); a miss drops it back to box 1.
 */
data class WordCard(
    val rank: Int,
    val box: Int = 0,
    val dueDay: Long = 0,
    val seen: Int = 0,
    val correct: Int = 0
) {
    val introduced get() = box > 0
    val memorized get() = box >= MEMORIZED_BOX
    fun isDue(day: Long) = introduced && dueDay <= day

    fun review(day: Long, success: Boolean): WordCard {
        val nextBox = if (success) (box + 1).coerceAtMost(MAX_BOX) else 1
        return copy(
            box = nextBox,
            dueDay = day + INTERVALS[nextBox],
            seen = seen + 1,
            correct = correct + if (success) 1 else 0
        )
    }

    /** First lesson: the word is due again tomorrow (or today again after a miss). */
    fun learn(day: Long, success: Boolean): WordCard =
        copy(box = 1, dueDay = day + if (success) 1 else 0, seen = seen + 1, correct = correct + if (success) 1 else 0)

    companion object {
        const val MAX_BOX = 7
        const val MEMORIZED_BOX = 5
        /** Days until the next review once a word lands in box i. */
        val INTERVALS = longArrayOf(0, 1, 2, 4, 7, 15, 30, 60)
    }
}

/** Galaxy layout: the 5000 words in frequency order, split into constellations. */
object Galaxy {
    const val WORDS = 5000
    const val PER_CONSTELLATION = 50
    const val CONSTELLATIONS = WORDS / PER_CONSTELLATION
    const val NEW_PER_LESSON = 5
    const val MAX_REVIEW = 20
    /** A constellation unlocks once this many words of the previous one have been introduced. */
    const val UNLOCK_AT = 40

    fun constellationOf(rank: Int) = (rank - 1) / PER_CONSTELLATION
    fun ranksOf(constellation: Int): IntRange =
        (constellation * PER_CONSTELLATION + 1)..((constellation + 1) * PER_CONSTELLATION)

    fun introducedIn(constellation: Int, cards: Map<Int, WordCard>) =
        ranksOf(constellation).count { cards[it]?.introduced == true }

    fun memorizedIn(constellation: Int, cards: Map<Int, WordCard>) =
        ranksOf(constellation).count { cards[it]?.memorized == true }

    fun isUnlocked(constellation: Int, cards: Map<Int, WordCard>) =
        constellation == 0 || introducedIn(constellation - 1, cards) >= UNLOCK_AT

    /** Next ranks to learn in [constellation], in frequency order. */
    fun nextNew(constellation: Int, cards: Map<Int, WordCard>, count: Int = NEW_PER_LESSON): List<Int> =
        ranksOf(constellation).filter { cards[it]?.introduced != true }.take(count)

    /** Most overdue first, then weakest box. */
    fun dueRanks(day: Long, cards: Map<Int, WordCard>, limit: Int = MAX_REVIEW): List<Int> =
        cards.values.filter { it.isDue(day) }
            .sortedWith(compareBy<WordCard> { it.dueDay }.thenBy { it.box }.thenBy { it.rank })
            .take(limit)
            .map { it.rank }

    /** The lowest unlocked constellation that still has words to introduce. */
    fun currentConstellation(cards: Map<Int, WordCard>): Int =
        (0 until CONSTELLATIONS).firstOrNull { isUnlocked(it, cards) && introducedIn(it, cards) < PER_CONSTELLATION }
            ?: (0 until CONSTELLATIONS).last { isUnlocked(it, cards) }

    /** Deterministic word of the day, from the first 2000 words. */
    fun wordOfDay(day: Long): Int = ((day * 7919L) % 2000L).toInt().let { if (it < 0) it + 2000 else it } + 1

    /** Constellation names: Spanish star/sky words so the map feels like a sky atlas. */
    val NAMES = listOf(
        "Aurora", "Brújula", "Cometa", "Delfín", "Estrella", "Faro", "Galaxia", "Halcón", "Isla", "Jaguar",
        "Lince", "Luna", "Marea", "Nube", "Órbita", "Pegaso", "Quetzal", "Río", "Sol", "Trueno",
        "Unicornio", "Volcán", "Zafiro", "Ancla", "Búho", "Cóndor", "Dragón", "Eclipse", "Fénix", "Girasol"
    )

    fun name(constellation: Int): String {
        val base = NAMES[constellation % NAMES.size]
        val cycle = constellation / NAMES.size
        return if (cycle == 0) base else "$base ${listOf("", "II", "III", "IV")[cycle.coerceAtMost(3)]}"
    }
}
