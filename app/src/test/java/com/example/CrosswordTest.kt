package com.example

import com.example.data.content.buildCrossword
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CrosswordTest {
    @Test
    fun wordsCrossWithoutClashing() {
        val words = listOf("perro", "gato", "pato", "oso", "rana", "vaca", "pollo", "caballo")
        repeat(20) { seed ->
            val entries = buildCrossword(words, Random(seed))
            assertTrue("at least four words fit", entries.size >= 4)
            val letters = HashMap<Pair<Int, Int>, Char>()
            entries.forEach { e ->
                e.cells().forEachIndexed { k, cell ->
                    val old = letters.put(cell, e.answer[k])
                    assertTrue("cells agree", old == null || old == e.answer[k])
                    assertTrue(cell.first >= 0 && cell.second >= 0)
                }
            }
            // Every word after the first shares a cell with another word.
            entries.forEach { e ->
                val others = entries.filter { it !== e }.flatMap { it.cells() }.toSet()
                assertTrue(entries.size == 1 || e.cells().any { it in others })
            }
            assertEquals(entries.size, entries.map { it.index }.distinct().size)
        }
    }
}
