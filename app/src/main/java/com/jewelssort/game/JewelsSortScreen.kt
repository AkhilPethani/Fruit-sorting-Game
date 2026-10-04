package com.jewelssort.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// FlowRow માટેના ઈમ્પોર્ટ
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ૧. ફ્રૂટ ડેટા અને કલર પેલેટ
data class FruitInfo(val name: String, val color: Color)

object FruitPalette {
    val fruits = listOf(
        FruitInfo("APPLE", Color(0xFFFF1744)),
        FruitInfo("ORANGE", Color(0xFFFF9100)),
        FruitInfo("GRAPE", Color(0xFF9C27B0)),
        FruitInfo("LEMON", Color(0xFFFFEA00)),
        FruitInfo("KIWI", Color(0xFF4CAF50)),
        FruitInfo("BERRY", Color(0xFFE91E63)),
        FruitInfo("BLUEBERRY", Color(0xFF2979FF))
    )
    val goldCoin = Color(0xFFFFD700)
}

@Composable
fun JewelsSortScreen(viewModel: GameViewModel) {
    val state = viewModel.uiState

    // જો તમારા ViewModel માં કોઈન્સ ના હોય તો લોકલી સ્ટેટ મેનેજ કરવા (સેફ્ટી માટે)
    var localCoins by remember { mutableStateOf(100) }

    when (state.currentScreen) {
        Screen.HOME -> HomeScreen(currentLevel = state.level, totalCoins = localCoins) { viewModel.startGame() }
        Screen.GAME -> MainGameContent(
            viewModel = viewModel,
            state = state,
            coins = localCoins,
            onCoinsChanged = { localCoins = it }
        )
    }
}

// કોઈન બાર કમ્પોઝેબલ (ટોપ રાઇટ ડિસ્પ્લે માટે)
@Composable
fun CoinDisplay(coins: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, FruitPalette.goldCoin, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🪙 ", fontSize = 16.sp)
            Text(
                text = "$coins",
                color = FruitPalette.goldCoin,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun HomeScreen(currentLevel: Int, totalCoins: Int, onStartClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF140727), Color(0xFF05010C)))),
        contentAlignment = Alignment.Center
    ) {
        // ટોપ રાઇટ કોઈન્સ બાર
        Box(modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(), contentAlignment = Alignment.TopEnd) {
            CoinDisplay(coins = totalCoins)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("FRUIT JUICE", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("SORT PUZZLE", color = FruitPalette.goldCoin, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)

            Spacer(Modifier.height(40.dp))

            // ફ્રેન્સી લાઈવ એપ ડેમો (૩ નાની એનિમેટેડ બોટલ્સ)
            Text("HOW TO PLAY DEMO", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniDemoBottle(colors = listOf(Color(0xFFFF1744), Color(0xFFFF9100)), isJuice = false)
                MiniDemoBottle(colors = listOf(Color(0xFFFF1744), Color(0xFFFF1744)), isJuice = true) // full juice
                MiniDemoBottle(colors = emptyList(), isJuice = false) // empty
            }

            Spacer(Modifier.height(50.dp))

            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(containerColor = FruitPalette.goldCoin),
                shape = RoundedCornerShape(30.dp),
                modifier = Modifier.width(220.dp).height(60.dp)
            ) {
                Text("START GAME", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("LEVEL $currentLevel", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainGameContent(viewModel: GameViewModel, state: GameUiState, coins: Int, onCoinsChanged: (Int) -> Unit) {
    val capacity = GameEngine.configFor(state.level).capacity
    val tubeIsJuice = remember(state.level) { mutableStateMapOf<Int, Boolean>() }
    var hintText by remember { mutableStateOf("") }
    var isWatchingAd by remember { mutableStateOf(false) }

    LaunchedEffect(state.tubes, state.moves) {
        if (state.moves == 0) tubeIsJuice.clear()
        state.tubes.forEachIndexed { index, tube ->
            if (tube.isEmpty()) tubeIsJuice[index] = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0618)).statusBarsPadding()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // ટોપ કંટ્રોલ બાર
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.goToHome() }) { Icon(Icons.Rounded.Home, "Home", tint = Color.White) }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVEL ${state.level}", color = FruitPalette.goldCoin, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Moves: ${state.moves}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }

                CoinDisplay(coins = coins)
            }

            // હેલ્પ (Bulb) અને કોઈન અનડુ એક્શન બાર
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bulb Button (Hint - Costs 20 Coins)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                    IconButton(
                        onClick = {
                            if (coins >= 20) {
                                onCoinsChanged(coins - 20)
                                hintText = "Tip: Find same fruits and match them!"
                            } else {
                                hintText = "Not enough coins! Need 20 🪙"
                            }
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Rounded.Lightbulb, "Hint", tint = FruitPalette.goldCoin)
                    }
                    Text("Hint (-20 🪙)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }

                // Undo Button (Costs 5 Coins)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                    IconButton(
                        onClick = {
                            if (coins >= 5) {
                                onCoinsChanged(coins - 5)
                                viewModel.undo()
                                hintText = ""
                            } else {
                                hintText = "Not enough coins! Need 5 🪙"
                            }
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Rounded.Undo, "Undo", tint = Color.White)
                    }
                    Text("Undo (-5 🪙)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }

                // Restart Button (Free)
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 16.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.restart()
                            hintText = ""
                        },
                        modifier = Modifier.background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Rounded.Refresh, "Restart", tint = Color.White)
                    }
                    Text("Restart", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }

            // હિન્ટ મેસેજ ડિસ્પ્લે
            if (hintText.isNotEmpty()) {
                Text(hintText, color = FruitPalette.goldCoin, fontSize = 14.sp, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))

            // મુખ્ય બોટલ ગ્રીડ
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                state.tubes.forEachIndexed { index, tube ->
                    val isComplete = tube.size == capacity && tube.distinct().size == 1
                    val isJuice = (tubeIsJuice[index] ?: false) || (tube.size == 1)

                    BottleView(
                        items = tube,
                        capacity = capacity,
                        selected = state.selectedTube == index,
                        isComplete = isComplete,
                        isJuice = isJuice,
                        onClick = {
                            if (state.selectedTube != null && state.selectedTube != index && tube.isEmpty()) {
                                tubeIsJuice[index] = true
                            }
                            viewModel.onTubeTapped(index)
                        }
                    )
                }
            }
        }

        // લેવલ કમ્પ્લીટ સ્ક્રિન પોપઅપ વિથ કોઈન એન્ડ એડ ક્લેઈમ
        if (state.isWon) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xEE05010C)), contentAlignment = Alignment.Center) {
                Surface(color = Color(0xFF1B0D33), shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(24.dp).border(2.dp, FruitPalette.goldCoin, RoundedCornerShape(24.dp))) {
                    Column(modifier = Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LEVEL COMPLETE!", color = FruitPalette.goldCoin, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(12.dp))
                        Text("You Won: 🪙 +50 Coins", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))

                        if (isWatchingAd) {
                            CircularProgressIndicator(color = FruitPalette.goldCoin)
                            Spacer(Modifier.height(8.dp))
                            Text("Loading Ad Video...", color = Color.White)

                            LaunchedEffect(Unit) {
                                delay(1500) // ૧.૫ સેકન્ડ એડ લોડિંગ ડ્રામા
                                onCoinsChanged(coins + 100) // ડબલ કોઈન (૫૦ + ૫૦ બોનસ)
                                isWatchingAd = false
                                viewModel.nextLevel()
                            }
                        } else {
                            // ઓપ્શન ૧: નોર્મલ ક્લેઇમ
                            Button(
                                onClick = {
                                    onCoinsChanged(coins + 50)
                                    viewModel.nextLevel()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Claim Normal (+50)", color = Color.White)
                            }

                            Spacer(Modifier.height(12.dp))

                            // ઓપ્શન ૨: એડ જોઈને ડબલ ક્લેઇમ
                            Button(
                                onClick = { isWatchingAd = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FruitPalette.goldCoin),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("🎬 Double Coins (+100)", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottleView(items: List<Int>, capacity: Int, selected: Boolean, isComplete: Boolean, isJuice: Boolean, onClick: () -> Unit) {
    val fruitIndex = if (items.isNotEmpty()) items.first() else 0
    val fruitInfo = if (items.isNotEmpty()) FruitPalette.fruits[fruitIndex % FruitPalette.fruits.size] else null
    val fluidColor = fruitInfo?.color ?: Color.Transparent
    val borderColor = if (isComplete) fluidColor else Color.White.copy(alpha = 0.3f)

    val lift by animateDpAsState(if (selected) (-20).dp else 0.dp, label = "")
    val capOffset by animateDpAsState(if (isComplete) 0.dp else (-25).dp, label = "")
    val capAlpha by animateFloatAsState(if (isComplete) 1f else 0f, label = "")

    Column(modifier = Modifier.offset(y = lift).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        // ૧. ફ્રૂટ લેબલ
        AnimatedVisibility(visible = isComplete, enter = fadeIn() + expandVertically()) {
            Text(text = fruitInfo?.name ?: "", color = fluidColor, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(bottom = 4.dp))
        }

        // ૨. ઓપન મોઢું અને એનિમેટેડ ક્લોઝ કેપ
        Box(modifier = Modifier.width(28.dp).height(20.dp).border(2.dp, borderColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)), contentAlignment = Alignment.TopCenter) {
            Box(modifier = Modifier.offset(y = capOffset).alpha(capAlpha).fillMaxWidth().height(14.dp).background(fluidColor, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
        }

        // ૩. મુખ્ય બોડી
        Box(
            modifier = Modifier
                .width(65.dp).height(180.dp)
                .border(2.dp, borderColor, RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxSize()) {
                val emptySlots = capacity - items.size
                repeat(emptySlots) { Spacer(Modifier.size(38.dp)) }

                items.asReversed().forEachIndexed { index, colorIndex ->
                    key(index, colorIndex) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (isJuice) {
                                JuiceSegment(fluidColor, isTopElement = index == 0)
                            } else {
                                FruitPiece(typeIndex = colorIndex % FruitPalette.fruits.size)
                            }
                        }
                    }
                }
            }
        }
    }
}

// હોમ સ્ક્રિન ના ડેમો માટે નાની સુંદર સજાવટી બોટલ
@Composable
fun MiniDemoBottle(colors: List<Color>, isJuice: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 35.dp, height = 90.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(verticalArrangement = Arrangement.Bottom) {
            colors.forEach { color ->
                if (isJuice) {
                    Box(modifier = Modifier.fillMaxWidth().height(40.getBoxHeight(isJuice).dp).background(color))
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(2.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(16.dp)) { drawCircle(color) }
                    }
                }
            }
        }
    }
}
private fun Int.getBoxHeight(juice: Boolean) = if (juice) 70 else 20

@Composable
fun FruitPiece(typeIndex: Int) {
    Canvas(modifier = Modifier.size(36.dp).padding(2.dp)) {
        val w = size.width
        val h = size.height
        when (typeIndex) {
            0 -> { // APPLE
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.3f)
                    cubicTo(w * 0.2f, h * 0.1f, w * 0.05f, h * 0.4f, w * 0.15f, h * 0.7f)
                    cubicTo(w * 0.25f, h * 0.95f, w * 0.45f, h * 0.9f, w * 0.5f, h * 0.85f)
                    cubicTo(w * 0.55f, h * 0.9f, w * 0.75f, h * 0.95f, w * 0.85f, h * 0.7f)
                    cubicTo(w * 0.95f, h * 0.4f, w * 0.8f, h * 0.1f, w * 0.5f, h * 0.3f)
                    close()
                }
                drawPath(path, Color(0xFFFF1744))
                drawLine(Color(0xFF5D4037), Offset(w * 0.5f, h * 0.25f), Offset(w * 0.5f, h * 0.05f), strokeWidth = 2.dp.toPx())
            }
            1 -> { // ORANGE
                drawCircle(Color(0xFFFF9100), radius = size.minDimension / 2.3f)
                drawCircle(Color(0xFFFFE082), radius = size.minDimension / 2.7f)
            }
            2 -> { // GRAPE
                val r = 5.dp.toPx()
                drawCircle(Color(0xFF9C27B0), r, Offset(w * 0.5f, h * 0.3f))
                drawCircle(Color(0xFF8E24AA), r, Offset(w * 0.35f, h * 0.5f))
                drawCircle(Color(0xFF8E24AA), r, Offset(w * 0.65f, h * 0.5f))
                drawCircle(Color(0xFF7B1FA2), r, Offset(w * 0.5f, h * 0.65f))
            }
            3 -> { // LEMON
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.15f, w * 0.85f, h * 0.5f)
                    quadraticBezierTo(w * 0.5f, h * 0.85f, w * 0.15f, h * 0.5f)
                    close()
                }
                drawPath(path, Color(0xFFFFEA00))
            }
            4 -> { // KIWI
                drawCircle(Color(0xFF8D6E63), radius = size.minDimension / 2.2f)
                drawCircle(Color(0xFF7CB342), radius = size.minDimension / 2.5f)
            }
            5 -> { // BERRY
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    quadraticBezierTo(w * 0.9f, h * 0.2f, w * 0.75f, h * 0.65f)
                    quadraticBezierTo(w * 0.55f, h * 0.95f, w * 0.5f, h * 0.95f)
                    close()
                }
                drawPath(path, Color(0xFFE91E63))
            }
            else -> { // BLUEBERRY
                drawCircle(Color(0xFF1976D2), radius = size.minDimension / 2.3f)
            }
        }
    }
}

@Composable
fun JuiceSegment(color: Color, isTopElement: Boolean) {
    Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
        val path = Path().apply {
            if (isTopElement) {
                moveTo(0f, size.height * 0.2f)
                quadraticBezierTo(size.width / 2f, -size.height * 0.1f, size.width, size.height * 0.2f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            } else {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
        }
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.8f), color)))
    }
}