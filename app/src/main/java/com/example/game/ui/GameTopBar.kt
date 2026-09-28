package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.engine.GameUiState
import com.example.game.model.GameStatus

@Composable
fun GameTopBar(
    uiState: GameUiState,
    onToggleSound: () -> Unit,
    onTogglePause: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Left controls: Sound FX & Customization
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HudIconButton(
                icon = if (uiState.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = if (uiState.isSoundEnabled) "Mute Sound" else "Enable Sound",
                onClick = onToggleSound,
                testTag = "hud_sound_button"
            )

            if (uiState.status == GameStatus.IDLE) {
                HudIconButton(
                    icon = Icons.Default.Palette,
                    contentDescription = "Customize Skins & Themes",
                    onClick = onOpenCustomization,
                    testTag = "hud_customize_button"
                )
            }
        }

        // Right controls: Leaderboard & Pause
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.status == GameStatus.PLAYING) {
                HudIconButton(
                    icon = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (uiState.isPaused) "Resume Game" else "Pause Game",
                    onClick = onTogglePause,
                    testTag = "hud_pause_button"
                )
            } else if (uiState.status == GameStatus.IDLE) {
                HudIconButton(
                    icon = Icons.Default.Leaderboard,
                    contentDescription = "Leaderboard",
                    onClick = onOpenLeaderboard,
                    testTag = "hud_leaderboard_button"
                )
            }
        }
    }
}

@Composable
private fun HudIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .background(Color(0xBBFFFFFF), CircleShape)
            .border(1.5.dp, Color(0xFF5D4037), CircleShape)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF2C2428),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
