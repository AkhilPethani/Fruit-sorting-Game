package com.jewelssort.game

import kotlin.random.Random

data class LevelConfig(
    val numColors: Int,
    val emptyTubes: Int,
    val capacity: Int = 4
)

object GameEngine {

    // પર્ફેક્ટ લેવલ ડિઝાઇન: કલર વધવાની સાથે ખાલી ટ્યુબ પણ વધશે જેથી લેવલ અશક્ય ના બને
    fun configFor(level: Int): LevelConfig {
        return when {
            level <= 3 -> LevelConfig(numColors = 3, emptyTubes = 2)  // સાવ સરળ (૩ કલર, ૨ ખાલી ટ્યુબ)
            level <= 7 -> LevelConfig(numColors = 4, emptyTubes = 2)  // સરળ (૪ કલર, ૨ ખાલી ટ્યુબ)
            level <= 12 -> LevelConfig(numColors = 5, emptyTubes = 2) // મધ્યમ (૫ કલર, ૨ ખાલી ટ્યુબ)
            level <= 20 -> LevelConfig(numColors = 6, emptyTubes = 3) // અઘરું પણ સોલ્વ થાય તેવું (૬ કલર, ૩ ખાલી ટ્યુબ)
            else -> LevelConfig(numColors = 8.coerceAtMost(10), emptyTubes = 3) // એક્સપર્ટ (૮ કલર, ૩ ખાલી ટ્યુબ)
        }
    }

    // નવું બોર્ડ જનરેટ કરવું અને કલર મિક્સ કરવા
    fun newBoard(level: Int): List<List<Int>> {
        val config = configFor(level)
        val colorPool = mutableListOf<Int>()

        // દરેક કલરને 4 વાર લિસ્ટમાં ઉમેરવા
        for (i in 1..config.numColors) {
            repeat(config.capacity) { colorPool.add(i) }
        }

        // કલરને બરાબર મિક્સ (Shuffle) કરવા
        val random = Random(level.toLong())
        colorPool.shuffle(random)

        val result = mutableListOf<List<Int>>()

        // મિક્સ થયેલા કલરને ટ્યુબમાં વહેંચવા
        for (i in 0 until config.numColors) {
            val start = i * config.capacity
            val end = start + config.capacity
            result.add(colorPool.subList(start, end).toList())
        }

        // પૂરતી ખાલી ટ્યુબ (Buffer Space) ઉમેરવી જેથી લોક ના થઈ જાય
        repeat(config.emptyTubes) {
            result.add(emptyList())
        }

        return result
    }

    // ગેમનો અસલી ઓરિજિનલ નિયમ (મેચિંગ કલર જ ઉપર આવશે)
    fun pour(tubes: List<MutableList<Int>>, from: Int, to: Int, capacity: Int): Int {
        if (from == to || tubes[from].isEmpty()) return 0
        if (tubes[to].size >= capacity) return 0

        val colorToMove = tubes[from].last()

        // સાચો નિયમ: જો સામેની ટ્યુબ ખાલી ના હોય અને કલર મેચ ના થતો હોય, તો મૂવ ના કરવા દેવો
        if (tubes[to].isNotEmpty() && tubes[to].last() != colorToMove) {
            return 0
        }

        // એક સરખા કલરના બોલ એકસાથે ટ્રાન્સફર કરવા
        var moved = 0
        while (tubes[from].isNotEmpty() && tubes[from].last() == colorToMove && tubes[to].size < capacity) {
            tubes[to].add(tubes[from].removeAt(tubes[from].size - 1))
            moved++
        }
        return moved
    }

    // ગેમ જીત્યા છો કે નહીં તે ચેક કરવા
    fun isWon(tubes: List<List<Int>>, capacity: Int): Boolean {
        for (tube in tubes) {
            if (tube.isEmpty()) continue
            if (tube.size != capacity || tube.distinct().size != 1) return false
        }
        return true
    }
}