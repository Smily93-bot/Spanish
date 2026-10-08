package com.example.data.content

import kotlin.random.Random

/** One word placed in a crossword: its first letter's cell, direction and clue number. */
data class CrossEntry(val index: Int, val answer: String, val row: Int, val col: Int, val across: Boolean, var number: Int = 0) {
    fun cells(): List<Pair<Int, Int>> = answer.indices.map { if (across) row to col + it else row + it to col }
}

/**
 * Builds a small crossword from [answers] (lowercase single words). Words are placed longest first;
 * every word after the first crosses one already placed on a shared letter, and words never touch
 * side by side. Words that don't fit are left out. Rows and columns are shifted to start at 0.
 */
fun buildCrossword(answers: List<String>, random: Random = Random, maxSize: Int = 12): List<CrossEntry> {
    val grid = HashMap<Pair<Int, Int>, Char>()
    val placed = mutableListOf<CrossEntry>()

    fun fits(word: String, row: Int, col: Int, across: Boolean): Boolean {
        val dr = if (across) 0 else 1
        val dc = if (across) 1 else 0
        // The cells just before and after the word must be empty.
        if (grid.containsKey(row - dr to col - dc)) return false
        if (grid.containsKey(row + dr * word.length to col + dc * word.length)) return false
        var crossings = 0
        for (k in word.indices) {
            val cell = row + dr * k to col + dc * k
            val existing = grid[cell]
            if (existing != null) {
                if (existing != word[k]) return false
                crossings++
            } else {
                // A new letter may not sit right next to another word.
                val side1 = cell.first + dc to cell.second + dr
                val side2 = cell.first - dc to cell.second - dr
                if (grid.containsKey(side1) || grid.containsKey(side2)) return false
            }
        }
        // Keep it small enough for a phone screen.
        val rows = grid.keys.map { it.first } + listOf(row, row + dr * (word.length - 1))
        val cols = grid.keys.map { it.second } + listOf(col, col + dc * (word.length - 1))
        if (rows.max() - rows.min() >= maxSize || cols.max() - cols.min() >= maxSize) return false
        return placed.isEmpty() || crossings > 0
    }

    fun place(entry: CrossEntry) {
        entry.cells().forEachIndexed { k, cell -> grid[cell] = entry.answer[k] }
        placed += entry
    }

    val order = answers.withIndex().sortedByDescending { it.value.length }
    for ((index, word) in order) {
        if (placed.isEmpty()) {
            place(CrossEntry(index, word, 0, 0, across = true))
            continue
        }
        val options = mutableListOf<CrossEntry>()
        for (other in placed) {
            for ((j, letter) in other.answer.withIndex()) {
                for ((i, mine) in word.withIndex()) {
                    if (mine != letter) continue
                    val (r, c) = other.cells()[j]
                    val across = !other.across
                    val row = if (across) r else r - i
                    val col = if (across) c - i else c
                    if (fits(word, row, col, across)) options += CrossEntry(index, word, row, col, across)
                }
            }
        }
        options.randomOrNull(random)?.let { place(it) }
    }
    if (placed.isEmpty()) return emptyList()
    // Shift to (0, 0) and number the clues in reading order.
    val top = placed.minOf { e -> e.cells().minOf { it.first } }
    val left = placed.minOf { e -> e.cells().minOf { it.second } }
    val shifted = placed.map { it.copy(row = it.row - top, col = it.col - left) }
    val starts = shifted.map { it.row to it.col }.distinct().sortedWith(compareBy({ it.first }, { it.second }))
    shifted.forEach { it.number = starts.indexOf(it.row to it.col) + 1 }
    return shifted.sortedBy { it.number }
}
