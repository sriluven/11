package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.GameUiState
import com.example.game.model.BirdSkin
import com.example.game.model.Difficulty
import com.example.game.model.GameTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationSheet(
    uiState: GameUiState,
    onDismiss: () -> Unit,
    onSelectBirdSkin: (BirdSkin) -> Unit,
    onSelectTheme: (GameTheme) -> Unit,
    onSelectDifficulty: (Difficulty) -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF9F7F1),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Customize Game",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF2C2428)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_customization_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF5D4037)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Bird Skins Section
            Text(
                text = "BIRD CHARACTER",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8B5A2B),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BirdSkin.values()) { skin ->
                    val isSelected = uiState.birdSkin == skin
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFFFFF3E0) else Color.White,
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .width(130.dp)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE65100) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectBirdSkin(skin) }
                            .testTag("skin_item_${skin.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            // Bird Color preview
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(skin.bodyColor), CircleShape)
                                    .border(2.dp, Color(0xFF2C2428), CircleShape)
                            ) {
                                // Wing dot
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(skin.wingColor), CircleShape)
                                        .border(1.dp, Color(0xFF2C2428), CircleShape)
                                        .align(Alignment.CenterStart)
                                )
                                // Beak tip
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(skin.beakColor), RoundedCornerShape(2.dp))
                                        .align(Alignment.CenterEnd)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = skin.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = Color(0xFF2C2428)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Visual Theme Section
            Text(
                text = "WORLD ENVIRONMENT",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8B5A2B),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                GameTheme.values().forEach { theme ->
                    val isSelected = uiState.theme == theme
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFFE65100) else Color(0xFFE0E0E0),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectTheme(theme) }
                            .testTag("theme_item_${theme.name.lowercase()}")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(theme.skyColorTop),
                                                Color(theme.skyColorBottom)
                                            )
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = theme.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = Color(0xFF2C2428)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Difficulty Level Section
            Text(
                text = "DIFFICULTY",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF8B5A2B),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Difficulty.values().forEach { diff ->
                    val isSelected = uiState.difficulty == diff
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectDifficulty(diff) },
                        label = { Text(diff.displayName) },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFE0B2),
                            selectedLabelColor = Color(0xFFE65100)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("difficulty_${diff.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Audio & Haptics Toggles
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Sound FX
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Sound Effects",
                                tint = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Sound Effects",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2C2428)
                            )
                        }
                        Switch(
                            checked = uiState.isSoundEnabled,
                            onCheckedChange = { onToggleSound() },
                            modifier = Modifier.testTag("toggle_sound_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Haptics
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Haptic Feedback",
                                tint = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Haptic Vibration",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2C2428)
                            )
                        }
                        Switch(
                            checked = uiState.isHapticsEnabled,
                            onCheckedChange = { onToggleHaptics() },
                            modifier = Modifier.testTag("toggle_haptics_switch")
                        )
                    }
                }
            }
        }
    }
}
