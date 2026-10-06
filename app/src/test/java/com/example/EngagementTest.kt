package com.example

import com.example.data.content.GalaxyExercise
import com.example.data.content.GalaxyQuiz
import com.example.data.engagement.Galaxy
import com.example.data.engagement.StreakEvent
import com.example.data.engagement.StreakState
import com.example.data.engagement.WordCard
import com.example.data.model.HelperLanguage
import com.example.data.model.VocabWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EngagementTest {

    @Test
    fun streakGrowsOncePerDayWhenGoalReached() {
        var s = StreakState(dailyGoal = 50)
        var (next, event) = s.addXp(100, 30)
        assertEquals(StreakEvent.None, event)
        s = next
        val r = s.addXp(100, 25)
        s = r.first
        assertTrue(r.second is StreakEvent.GoalReached)
        assertEquals(1, s.streak)
        // More XP on the same day does not count twice.
        assertEquals(StreakEvent.None, s.addXp(100, 80).second)
        s = s.addXp(101, 60).first
        assertEquals(2, s.streak)
        assertTrue(s.goalMetOn(101))
    }

    @Test
    fun missedDayUsesFreezeThenResets() {
        var s = StreakState(dailyGoal = 10, freezes = 1)
        s = s.addXp(10, 10).first
        s = s.addXp(11, 10).first
        // Day 12 missed, freeze covers it.
        assertEquals(2, s.liveStreak(13))
        s = s.addXp(13, 10).first
        assertEquals(3, s.streak)
        assertEquals(0, s.freezes)
        assertTrue(12L in s.frozenDays)
        // Two missed days with no freeze left: reset.
        assertEquals(0, s.liveStreak(16))
        s = s.addXp(16, 10).first
        assertEquals(1, s.streak)
        assertEquals(3, s.bestStreak)
    }

    @Test
    fun freezeEarnedEverySevenDays() {
        var s = StreakState(dailyGoal = 1, freezes = 0)
        var earned = false
        for (day in 0L until 7L) {
            val (next, e) = s.addXp(day, 5)
            s = next
            if (e is StreakEvent.GoalReached && e.earnedFreeze) earned = true
        }
        assertTrue(earned)
        assertEquals(1, s.freezes)
    }

    @Test
    fun leitnerBoxesAndIntervals() {
        var c = WordCard(7).learn(day = 0, success = true)
        assertEquals(1, c.box)
        assertEquals(1L, c.dueDay)
        c = c.review(1, true).review(3, true).review(7, true).review(14, true)
        assertEquals(5, c.box)
        assertTrue(c.memorized)
        c = c.review(29, false)
        assertEquals(1, c.box)
        assertEquals(30L, c.dueDay)
    }

    @Test
    fun constellationsUnlockAfterFortyWords() {
        val cards = (1..39).associateWith { WordCard(it, box = 1, dueDay = 5) }
        assertFalse(Galaxy.isUnlocked(1, cards))
        val more = cards + (40 to WordCard(40, box = 1, dueDay = 5))
        assertTrue(Galaxy.isUnlocked(1, more))
        assertEquals(listOf(41, 42, 43, 44, 45), Galaxy.nextNew(0, more))
        assertEquals(0, Galaxy.currentConstellation(more))
        assertEquals(40, Galaxy.dueRanks(5, more, limit = 100).size)
        assertTrue(Galaxy.dueRanks(4, more).isEmpty())
    }

    @Test
    fun quizHasFourDistinctOptionsIncludingAnswer() {
        val words = (1..300).map {
            VocabWord("palabra$it", "meaning$it; other", "معنى$it", "A1", "frequency", "noun", rank = it)
        }
        val quiz = GalaxyQuiz(words)
        val lesson = quiz.lesson(listOf(1, 2, 3, 4, 5), HelperLanguage.ENGLISH, Random(3))
        assertEquals(15, lesson.size)
        lesson.filter { it.type != GalaxyExercise.SPELL }.forEach { q ->
            assertEquals(4, q.options.size)
            assertEquals(4, q.options.distinct().size)
            assertTrue(q.answer in q.options)
        }
        assertEquals("meaning1", quiz.question(words[0], GalaxyExercise.MEANING, HelperLanguage.ENGLISH).answer)
    }
}
