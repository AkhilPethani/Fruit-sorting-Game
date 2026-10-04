package com.jewelssort.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// ૧. સ્ક્રીન નેવિગેશન માટે Enum
enum class Screen { HOME, GAME }

// ૨. ગેમની બધી ડેટા સ્ટેટ સાચવતો ક્લાસ
data class GameUiState(
    val currentScreen: Screen = Screen.HOME, // શરૂઆતમાં હોમ સ્ક્રીન દેખાશે
    val level: Int = 1,
    val tubes: List<List<Int>> = emptyList(),
    val selectedTube: Int? = null,
    val moves: Int = 0,
    val isWon: Boolean = false,
    val invalidShakeTrigger: Int = 0
)

// ૩. મુખ્ય ગેમ વ્યુમોડેલ લોજિક ક્લાસ
class GameViewModel : ViewModel() {

    // ગેમની લાઇવ સ્ટેટ
    var uiState by mutableStateOf(GameUiState())
        private set

    // ઇતિહાસ સાચવવા માટે (Undo functionality)
    private val history = ArrayDeque<List<List<Int>>>()

    // ટ્યુબમાં કેટલા કલર સમાઈ શકે તે નક્કી કરવા
    private val capacity: Int
        get() = GameEngine.configFor(uiState.level).capacity

    init {
        // એપ્લિકેશન શરૂ થતા જ બેકગ્રાઉન્ડમાં લેવલ ૧ લોડ થઈ જશે
        loadLevel(1)
    }

    private fun loadLevel(newLevel: Int) {
        val levelIndex = newLevel.coerceAtLeast(1)
        val newBoard = GameEngine.newBoard(levelIndex)

        uiState = uiState.copy(
            level = levelIndex,
            tubes = newBoard,
            moves = 0,
            selectedTube = null,
            isWon = false
        )
        history.clear()
    }

    // હોમ સ્ક્રીન પરથી ગેમ શરૂ કરવા માટે
    fun startGame() {
        uiState = uiState.copy(currentScreen = Screen.GAME)
    }

    // ગેમમાંથી હોમ સ્ક્રીન પર પાછા જવા માટે
    fun goToHome() {
        uiState = uiState.copy(currentScreen = Screen.HOME)
    }

    fun startLevel(newLevel: Int) {
        loadLevel(newLevel)
    }

    fun restart() {
        loadLevel(uiState.level)
    }

    fun nextLevel() {
        loadLevel(uiState.level + 1)
    }

    fun undo() {
        if (history.isEmpty() || uiState.isWon) return

        uiState = uiState.copy(
            tubes = history.removeLast(),
            moves = (uiState.moves - 1).coerceAtLeast(0),
            selectedTube = null
        )
    }

    fun onTubeTapped(index: Int) {
        if (uiState.isWon) return

        val currentSelected = uiState.selectedTube

        if (currentSelected == null) {
            if (uiState.tubes[index].isNotEmpty()) {
                uiState = uiState.copy(selectedTube = index)
            }
        } else if (currentSelected == index) {
            uiState = uiState.copy(selectedTube = null)
        } else {
            tryPour(currentSelected, index)
        }
    }

    private fun tryPour(from: Int, to: Int) {
        val currentTubes = uiState.tubes.map { it.toMutableList() }
        val movedCount = GameEngine.pour(currentTubes, from, to, capacity)

        if (movedCount == 0) {
            uiState = uiState.copy(
                invalidShakeTrigger = uiState.invalidShakeTrigger + 1,
                selectedTube = null
            )
            return
        }

        history.addLast(uiState.tubes)
        if (history.size > 80) history.removeFirst()

        val updatedTubes = currentTubes.map { it.toList() }

        uiState = uiState.copy(
            tubes = updatedTubes,
            moves = uiState.moves + 1,
            selectedTube = null,
            isWon = GameEngine.isWon(updatedTubes, capacity)
        )
    }
}