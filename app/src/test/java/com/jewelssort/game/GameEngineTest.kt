package com.jewelssort.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {
    @Test
    fun pourMovesMatchingJewelsUntilFull() {
        val tubes = listOf(
            mutableListOf(0, 0),
            mutableListOf(0, 0)
        )
        val moved = GameEngine.pour(tubes, 0, 1, 4)
        assertEquals(2, moved)
        assertTrue(tubes[0].isEmpty())
        assertEquals(listOf(0, 0, 0, 0), tubes[1])
    }

    @Test
    fun cannotPourDifferentColors() {
        val tubes = listOf(
            mutableListOf(1),
            mutableListOf(0)
        )
        assertFalse(GameEngine.canPour(tubes[0], tubes[1], 4))
    }

    @Test
    fun wonWhenEveryTubeIsEmptyOrComplete() {
        val won = listOf(listOf(0, 0, 0, 0), listOf(1, 1, 1, 1), emptyList())
        val notWon = listOf(listOf(0, 1, 0, 0), emptyList())
        assertTrue(GameEngine.isWon(won, 4))
        assertFalse(GameEngine.isWon(notWon, 4))
    }
}
