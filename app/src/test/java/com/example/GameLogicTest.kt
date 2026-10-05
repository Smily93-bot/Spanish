package com.example

import com.example.data.model.Rank
import com.example.data.model.normalizeAnswer
import com.example.data.repository.BlasterRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GameLogicTest {

    @Test
    fun answersIgnoreAccentsCaseAndPunctuation() {
        assertEquals(normalizeAnswer("entré"), normalizeAnswer("Entre"))
        assertEquals(normalizeAnswer("¿Qué tal?"), normalizeAnswer("que   tal"))
        assertEquals("las llaves", normalizeAnswer(" Las llaves. "))
    }

    @Test
    fun rankGradeThresholds() {
        assertEquals("C", BlasterRepository.rankGrade(0))
        assertEquals("B", BlasterRepository.rankGrade(300))
        assertEquals("A", BlasterRepository.rankGrade(700))
        assertEquals("S", BlasterRepository.rankGrade(1200))
        assertEquals("S+", BlasterRepository.rankGrade(2000))
    }

    @Test
    fun ranksFollowLevels() {
        assertEquals(Rank.CADET, Rank.forLevel(1))
        assertEquals(Rank.PILOT, Rank.forLevel(7))
        assertEquals(Rank.ADMIRAL, Rank.forLevel(40))
        assertEquals(Rank.EXPLORER, Rank.next(1))
    }
}
