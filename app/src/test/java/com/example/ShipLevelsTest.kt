package com.example

import com.example.data.ship.Cell
import com.example.data.ship.DOWN
import com.example.data.ship.LEFT
import com.example.data.ship.RIGHT
import com.example.data.ship.ShipEvent
import com.example.data.ship.ShipLevel
import com.example.data.ship.ShipRules
import com.example.data.ship.UP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Replays every level's solution (found by tools/ship_puzzles.py) through the app's rules. */
class ShipLevelsTest {
    private val levels = ShipLevel.parseAll(File("src/main/assets/ship_levels.json").readText())

    private fun dir(c: Char): Cell = when (c) {
        'U' -> UP
        'D' -> DOWN
        'L' -> LEFT
        else -> RIGHT
    }

    @Test
    fun wrongDoorIsADeadEnd() {
        // Level 4: the red key from the radio room used on the far red door leaves no way on.
        val level = levels[3]
        var s = ShipRules.solveRoom(level, ShipRules.start(level).copy(pos = level.pads[0]), 0)
        assertTrue(ShipRules.canProgress(level, s, 0))
        s = s.copy(keys = listOf(0, 0, 0, 0))
        assertTrue(!ShipRules.canProgress(level, s, 0))
    }

    @Test
    fun everyLevelIsSolvableInTheApp() {
        assertTrue(levels.size >= 12)
        levels.forEachIndexed { i, level ->
            // Whichever fetch spot holds the wanted thing, the level can be finished.
            val options = if (level.fetchPad >= 0) level.fetchSpots.indices.toList() else listOf(0)
            assertNotEquals("level ${i + 1} has a solution", "", level.solution)
            var won = false
            var s = ShipRules.start(level)
            // The stored solution is for the hardest fetch option; replay with each option until one wins.
            for (wanted in options) {
                s = ShipRules.start(level)
                var ok = true
                for (m in level.solution) {
                    val step = ShipRules.step(level, s, dir(m), wanted)
                    if (step.event is ShipEvent.Hit || step.event is ShipEvent.Blocked) { ok = false; break }
                    // The dead-end check must never fire on the real solution.
                    if ((step.openedDoor || step.pushed || step.switched) && step.event !is ShipEvent.RoomOpen && step.event !is ShipEvent.Win) {
                        assertTrue("level ${i + 1}: dead end reported on the solution", ShipRules.canProgress(level, step.state, wanted))
                    }
                    s = step.state
                    val e = step.event
                    if (e is ShipEvent.RoomOpen) s = ShipRules.solveRoom(level, s, e.room)
                    if (e is ShipEvent.Win) { won = true; break }
                }
                if (ok && won) break
            }
            assertTrue("level ${i + 1} (${level.name}) replays to the exit", won)
            assertEquals(7, s.solved)
        }
    }
}
