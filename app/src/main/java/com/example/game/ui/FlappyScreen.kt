package com.example.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.game.engine.GameViewModel
import com.example.game.model.GameStatus

@Composable
fun FlappyScreen(
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val topRecords by viewModel.topRecords.collectAsStateWithLifecycle()
    val totalGames by viewModel.totalGamesPlayed.collectAsStateWithLifecycle()
    val totalScoreSum by viewModel.totalScoreSum.collectAsStateWithLifecycle()

    var showCustomizationSheet by remember { mutableStateOf(false) }
    var showLeaderboardDialog by remember { mutableStateOf(false) }

    // Android System Back Button handling
    BackHandler(enabled = showCustomizationSheet || showLeaderboardDialog || uiState.isPaused || uiState.status == GameStatus.PLAYING) {
        when {
            showCustomizationSheet -> showCustomizationSheet = false
            showLeaderboardDialog -> showLeaderboardDialog = false
            uiState.isPaused -> viewModel.resumeGame()
            uiState.status == GameStatus.PLAYING -> viewModel.togglePause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. High-Performance 60FPS Game Canvas
        FlappyCanvas(
            viewModel = viewModel,
            uiState = uiState,
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD Top Bar
        GameTopBar(
            uiState = uiState,
            onToggleSound = { viewModel.toggleSound() },
            onTogglePause = { viewModel.togglePause() },
            onOpenLeaderboard = { showLeaderboardDialog = true },
            onOpenCustomization = { showCustomizationSheet = true }
        )

        // 3. IDLE State Overlay ("GET READY" / "TAP TO FLY")
        if (uiState.status == GameStatus.IDLE) {
            ReadyOverlay(
                uiState = uiState,
                onTapToFly = { viewModel.onUserTap() },
                onOpenCustomization = { showCustomizationSheet = true },
                onOpenLeaderboard = { showLeaderboardDialog = true }
            )
        }

        // 4. Game Over State Overlay (Scoreboard, Medals, Play Again)
        if (uiState.status == GameStatus.GAME_OVER) {
            GameOverOverlay(
                uiState = uiState,
                onRestart = { viewModel.resetToIdle() },
                onOpenLeaderboard = { showLeaderboardDialog = true },
                onOpenCustomization = { showCustomizationSheet = true }
            )
        }

        // 5. Game Paused Overlay
        if (uiState.isPaused && uiState.status == GameStatus.PLAYING) {
            PauseOverlay(
                onResume = { viewModel.resumeGame() },
                onRestart = { viewModel.resetToIdle() }
            )
        }

        // 6. Customization Sheet (Skins, Themes, Difficulty, Sound FX)
        if (showCustomizationSheet) {
            CustomizationSheet(
                uiState = uiState,
                onDismiss = { showCustomizationSheet = false },
                onSelectBirdSkin = { viewModel.setBirdSkin(it) },
                onSelectTheme = { viewModel.setTheme(it) },
                onSelectDifficulty = { viewModel.setDifficulty(it) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() }
            )
        }

        // 7. Leaderboard & Career Stats Dialog
        if (showLeaderboardDialog) {
            LeaderboardDialog(
                topRecords = topRecords,
                totalGames = totalGames,
                totalPipes = totalScoreSum ?: 0,
                bestScore = uiState.bestScore,
                onDismiss = { showLeaderboardDialog = false },
                onClearStats = { viewModel.clearStats() }
            )
        }
    }
}
