package com.pixeldodge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixeldodge.ui.theme.PixelDodgeTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.random.Random

private const val PLAYER_SIZE = 52f
private const val PLAYER_MARGIN = 24f

private data class Enemy(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
    val drift: Float,
)

private data class Pickup(
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float,
)

private enum class GamePhase { READY, PLAYING, GAME_OVER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelDodgeTheme {
                PixelDodgeGame()
            }
        }
    }
}

@Composable
fun PixelDodgeGame() {
    var phase by remember { mutableStateOf(GamePhase.READY) }
    var playerX by remember { mutableFloatStateOf(0f) }
    var playerY by remember { mutableFloatStateOf(0f) }
    var score by remember { mutableIntStateOf(0) }
    var bestScore by remember { mutableIntStateOf(0) }
    var gameWidth by remember { mutableFloatStateOf(0f) }
    var gameHeight by remember { mutableFloatStateOf(0f) }
    var enemies by remember { mutableStateOf(emptyList<Enemy>()) }
    var pickups by remember { mutableStateOf(emptyList<Pickup>()) }
    var spawnTimer by remember { mutableFloatStateOf(0.6f) }
    var pickupTimer by remember { mutableFloatStateOf(3.0f) }
    var elapsed by remember { mutableFloatStateOf(0f) }
    var shieldTime by remember { mutableFloatStateOf(0f) }
    var pulse by remember { mutableFloatStateOf(0f) }

    val resetGame: () -> Unit = {
        enemies = emptyList()
        pickups = emptyList()
        spawnTimer = 0.7f
        pickupTimer = 3.5f
        elapsed = 0f
        shieldTime = 0f
        pulse = 0f
        score = 0
        if (gameWidth > 0f) {
            playerX = (gameWidth / 2f) - PLAYER_SIZE / 2f
        }
        if (gameHeight > 0f) {
            playerY = gameHeight - PLAYER_SIZE - PLAYER_MARGIN
        }
    }

    LaunchedEffect(phase) {
        if (phase != GamePhase.PLAYING) return@LaunchedEffect
        while (phase == GamePhase.PLAYING) {
            delay(16)
            elapsed += 0.016f
            score = (elapsed * 18).toInt()
            bestScore = maxOf(bestScore, score)
            shieldTime = (shieldTime - 0.016f).coerceAtLeast(0f)
            pulse = (pulse - 0.016f).coerceAtLeast(0f)

            spawnTimer -= 0.016f
            pickupTimer -= 0.016f

            if (spawnTimer <= 0f) {
                val size = 28f + Random.nextFloat() * 26f
                val enemyX = Random.nextFloat() * (gameWidth - size)
                val speed = 170f + elapsed * 16f + Random.nextFloat() * 120f
                enemies = enemies + Enemy(
                    x = enemyX,
                    y = -size,
                    size = size,
                    speed = speed,
                    drift = (Random.nextFloat() - 0.5f) * 80f,
                )
                spawnTimer = (1.15f - min(elapsed / 40f, 0.55f)).coerceAtLeast(0.35f)
            }

            if (pickupTimer <= 0f) {
                val size = 18f
                val x = Random.nextFloat() * (gameWidth - size)
                pickups = pickups + Pickup(x = x, y = -size, size = size, speed = 240f)
                pickupTimer = 4.5f + Random.nextFloat() * 2.5f
            }

            enemies = enemies
                .map { enemy ->
                    val nextX = (enemy.x + enemy.drift * 0.016f)
                        .coerceIn(0f, (gameWidth - enemy.size).coerceAtLeast(0f))
                    enemy.copy(y = enemy.y + enemy.speed * 0.016f, x = nextX)
                }
                .filter { it.y < gameHeight + it.size + 20f }

            pickups = pickups
                .map { pickup -> pickup.copy(y = pickup.y + pickup.speed * 0.016f) }
                .filter { it.y < gameHeight + it.size + 20f }

            val playerRect = Rect(
                left = playerX,
                top = playerY,
                right = playerX + PLAYER_SIZE,
                bottom = playerY + PLAYER_SIZE,
            )

            val survivingEnemies = mutableListOf<Enemy>()
            for (enemy in enemies) {
                val enemyRect = Rect(
                    left = enemy.x,
                    top = enemy.y,
                    right = enemy.x + enemy.size,
                    bottom = enemy.y + enemy.size,
                )
                if (enemyRect.overlaps(playerRect)) {
                    if (shieldTime > 0f) {
                        pulse = 1f
                        continue
                    }
                    phase = GamePhase.GAME_OVER
                    break
                }
                survivingEnemies.add(enemy)
            }
            enemies = survivingEnemies

            val remainingPickups = mutableListOf<Pickup>()
            for (pickup in pickups) {
                val pickupRect = Rect(
                    left = pickup.x,
                    top = pickup.y,
                    right = pickup.x + pickup.size,
                    bottom = pickup.y + pickup.size,
                )
                if (pickupRect.overlaps(playerRect)) {
                    shieldTime = 4f
                    pulse = 1f
                } else {
                    remainingPickups.add(pickup)
                }
            }
            pickups = remainingPickups

            if (phase == GamePhase.GAME_OVER) {
                break
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    if (gameWidth <= 0f) gameWidth = size.width.toFloat()
                    if (gameHeight <= 0f) gameHeight = size.height.toFloat()
                    if (playerX == 0f && gameWidth > 0f) {
                        playerX = (gameWidth / 2f) - PLAYER_SIZE / 2f
                    }
                    if (playerY == 0f && gameHeight > 0f) {
                        playerY = gameHeight - PLAYER_SIZE - PLAYER_MARGIN
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { _, dragAmount ->
                        if (phase == GamePhase.PLAYING) {
                            playerX = (playerX + dragAmount.x)
                                .coerceIn(0f, (gameWidth - PLAYER_SIZE).coerceAtLeast(0f))
                            playerY = (playerY + dragAmount.y * 0.45f)
                                .coerceIn(0f, (gameHeight - PLAYER_SIZE).coerceAtLeast(0f))
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (phase == GamePhase.READY) {
                            resetGame()
                            phase = GamePhase.PLAYING
                            return@detectTapGestures
                        }

                        if (phase == GamePhase.GAME_OVER) {
                            resetGame()
                            phase = GamePhase.PLAYING
                            return@detectTapGestures
                        }

                        val dx = offset.x - (playerX + PLAYER_SIZE / 2f)
                        val dy = offset.y - (playerY + PLAYER_SIZE / 2f)
                        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        if (distance > 0f) {
                            val dashX = (dx / distance) * 180f
                            val dashY = (dy / distance) * 120f
                            playerX = (playerX + dashX).coerceIn(0f, (gameWidth - PLAYER_SIZE).coerceAtLeast(0f))
                            playerY = (playerY + dashY).coerceIn(0f, (gameHeight - PLAYER_SIZE).coerceAtLeast(0f))
                        }
                    }
                }
        ) {
            drawRect(color = Color(0xFF090B1A), size = size)

            val gridSize = 22f
            for (x in 0..((size.width / gridSize).toInt() + 1)) {
                for (y in 0..((size.height / gridSize).toInt() + 1)) {
                    val cellColor = if ((x + y) % 2 == 0) Color(0xFF10192E) else Color(0xFF0B1324)
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(x * gridSize, y * gridSize),
                        size = Size(gridSize, gridSize),
                    )
                }
            }

            val stars = listOf(
                Offset(40f, 90f),
                Offset(120f, 150f),
                Offset(260f, 95f),
                Offset(340f, 210f),
                Offset(500f, 120f),
                Offset(620f, 80f),
                Offset(760f, 200f),
            )
            stars.forEach { star ->
                drawRect(
                    color = Color(0xFFBFE8FF),
                    topLeft = star,
                    size = Size(4f, 4f),
                )
            }

            if (shieldTime > 0f) {
                drawCircle(
                    color = Color(0xFF5BE7FF).copy(alpha = 0.35f),
                    radius = PLAYER_SIZE * 0.9f,
                    center = Offset(playerX + PLAYER_SIZE / 2f, playerY + PLAYER_SIZE / 2f),
                )
            }

            val playerFill = if (shieldTime > 0f) Color(0xFF82F1FF) else Color(0xFF8EFEAC)
            drawRoundRect(
                color = Color(0xFF7BE0FF),
                topLeft = Offset(playerX, playerY),
                size = Size(PLAYER_SIZE, PLAYER_SIZE),
                cornerRadius = CornerRadius(0f),
                style = Stroke(width = 4f),
            )
            drawRoundRect(
                color = playerFill,
                topLeft = Offset(playerX + 9f, playerY + 9f),
                size = Size(PLAYER_SIZE - 18f, PLAYER_SIZE - 18f),
                cornerRadius = CornerRadius(0f),
            )

            pickups.forEach { pickup ->
                drawCircle(
                    color = Color(0xFFFFD559),
                    radius = pickup.size / 2f,
                    center = Offset(pickup.x + pickup.size / 2f, pickup.y + pickup.size / 2f),
                )
                drawCircle(
                    color = Color(0xFFFFF4BB),
                    radius = pickup.size / 3f,
                    center = Offset(pickup.x + pickup.size / 2f, pickup.y + pickup.size / 2f),
                )
            }

            enemies.forEach { enemy ->
                drawRoundRect(
                    color = Color(0xFFFF6B6B),
                    topLeft = Offset(enemy.x, enemy.y),
                    size = Size(enemy.size, enemy.size),
                    cornerRadius = CornerRadius(0f),
                    style = Stroke(width = 4f),
                )
                drawRoundRect(
                    color = Color(0xFFFFC164),
                    topLeft = Offset(enemy.x + 7f, enemy.y + 7f),
                    size = Size(enemy.size - 14f, enemy.size - 14f),
                    cornerRadius = CornerRadius(0f),
                )
            }

            if (pulse > 0f) {
                drawCircle(
                    color = Color(0xFFFFFFFF).copy(alpha = pulse * 0.3f),
                    radius = PLAYER_SIZE * 1.3f + (1f - pulse) * 20f,
                    center = Offset(playerX + PLAYER_SIZE / 2f, playerY + PLAYER_SIZE / 2f),
                    style = Stroke(width = 4f),
                )
            }
        }

        Text(
            text = "SCORE $score",
            color = Color(0xFFEAF6FF),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        )

        if (shieldTime > 0f) {
            Text(
                text = "SHIELD ${"%.1f".format(shieldTime)}s",
                color = Color(0xFF7BE0FF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 22.dp, end = 18.dp),
            )
        }

        if (phase != GamePhase.PLAYING) {
            Surface(
                color = Color(0xCC0A1222),
                modifier = Modifier.fillMaxSize(),
                tonalElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(
                        color = Color(0xFF111B32),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(32.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 26.dp, vertical = 20.dp)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (phase == GamePhase.READY) "PIXEL DODGE\nTap to start" else "GAME OVER\nScore: $score\nBest: $bestScore",
                                color = Color(0xFFEAF6FF),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 56.dp),
                            )

                            Button(
                                onClick = {
                                    resetGame()
                                    phase = GamePhase.PLAYING
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF7BE0FF),
                                    contentColor = Color(0xFF081827),
                                ),
                                modifier = Modifier.align(Alignment.BottomCenter),
                            ) {
                                Text(
                                    text = if (phase == GamePhase.READY) "START" else "PLAY AGAIN",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
