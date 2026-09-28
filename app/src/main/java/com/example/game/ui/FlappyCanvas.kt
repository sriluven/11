package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.example.game.engine.GameUiState
import com.example.game.engine.GameViewModel
import com.example.game.model.BirdSkin
import com.example.game.model.BirdState
import com.example.game.model.GameStatus
import com.example.game.model.GameTheme
import com.example.game.model.PipePair

@Composable
fun FlappyCanvas(
    viewModel: GameViewModel,
    uiState: GameUiState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density

    // Ultra-smooth 60+ FPS game frame ticker
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val delta = frameTimeNanos - lastFrameTimeNanos
                    viewModel.onGameTick(delta)
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("flappy_game_canvas")
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        viewModel.onUserTap()
                    }
                )
            }
    ) {
        // Report canvas dimensions to ViewModel
        viewModel.onCanvasSizeChanged(size.width, size.height, density)

        val groundHeight = maxOf(180f, size.height * 0.15f)
        val groundY = size.height - groundHeight

        // 1. Draw Sky Background
        drawSkyBackground(uiState.theme, groundY)

        // 2. Draw Clouds or Stars
        if (uiState.theme == GameTheme.NIGHT) {
            drawStars(groundY)
        } else {
            drawClouds(viewModel)
        }

        // 3. Draw Parallax Hills / Skyline
        drawHills(viewModel, groundY)

        // 4. Draw Pipes
        drawPipes(viewModel.pipes, uiState.theme, groundY)

        // 5. Draw Ground
        drawGround(groundY, groundHeight, viewModel.groundScrollX, uiState.theme)

        // 6. Draw Particles
        drawParticles(viewModel)

        // 7. Draw Bird
        drawBird(viewModel.bird, uiState.birdSkin)

        // 8. Screen Flash on hit
        if (uiState.flashAlpha > 0f) {
            drawRect(
                color = Color.White.copy(alpha = uiState.flashAlpha.coerceIn(0f, 1f)),
                size = size
            )
        }

        // 9. Floating Score during gameplay
        if (uiState.status == GameStatus.PLAYING || uiState.status == GameStatus.DYING) {
            drawArcadeScore(uiState.score, size.width, size.height * 0.13f)
        }
    }
}

private fun DrawScope.drawSkyBackground(theme: GameTheme, groundY: Float) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(theme.skyColorTop),
                Color(theme.skyColorBottom)
            ),
            startY = 0f,
            endY = groundY
        ),
        size = Size(size.width, groundY)
    )
}

private fun DrawScope.drawStars(groundY: Float) {
    val starCoords = listOf(
        Pair(0.12f, 0.10f), Pair(0.24f, 0.22f), Pair(0.38f, 0.08f),
        Pair(0.55f, 0.18f), Pair(0.72f, 0.12f), Pair(0.85f, 0.25f),
        Pair(0.18f, 0.35f), Pair(0.48f, 0.32f), Pair(0.68f, 0.38f),
        Pair(0.90f, 0.06f), Pair(0.30f, 0.45f), Pair(0.82f, 0.48f)
    )
    for ((rx, ry) in starCoords) {
        val sx = rx * size.width
        val sy = ry * groundY
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 2.5f,
            center = Offset(sx, sy)
        )
        // Star sparkle halo
        drawCircle(
            color = Color(0x33FFFFFF),
            radius = 5.5f,
            center = Offset(sx, sy)
        )
    }

    // Gentle Moon
    val moonX = size.width * 0.82f
    val moonY = groundY * 0.18f
    drawCircle(
        color = Color(0xFFF0E68C),
        radius = 28f,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color(0xFF141829),
        radius = 24f,
        center = Offset(moonX - 9f, moonY - 7f)
    )
}

private fun DrawScope.drawClouds(viewModel: GameViewModel) {
    for (cloud in viewModel.clouds) {
        val cx = cloud.x
        val cy = cloud.y
        val scale = cloud.scale
        val color = Color.White.copy(alpha = cloud.alpha)
        val shadowColor = Color(0x228B9BB4)

        // Multi-circle cloud puff
        val baseR = 24f * scale
        // Soft cloud shadow under puff
        drawCircle(shadowColor, radius = baseR * 1.15f, center = Offset(cx, cy + 4f))
        drawCircle(shadowColor, radius = baseR * 1.4f, center = Offset(cx + 28f * scale, cy + 6f))

        // Main white cloud puffs
        drawCircle(color, radius = baseR, center = Offset(cx, cy))
        drawCircle(color, radius = baseR * 1.35f, center = Offset(cx + 26f * scale, cy - 2f))
        drawCircle(color, radius = baseR * 1.1f, center = Offset(cx + 54f * scale, cy + 2f))
        drawCircle(color, radius = baseR * 0.9f, center = Offset(cx - 18f * scale, cy + 4f))
    }
}

private fun DrawScope.drawHills(viewModel: GameViewModel, groundY: Float) {
    for (hill in viewModel.hills) {
        val path = Path().apply {
            moveTo(hill.x, groundY)
            quadraticTo(
                hill.x + hill.width * 0.5f,
                groundY - hill.height,
                hill.x + hill.width,
                groundY
            )
            close()
        }
        drawPath(path, color = Color(hill.color))
    }
}

private fun DrawScope.drawPipes(
    pipes: List<PipePair>,
    theme: GameTheme,
    groundY: Float
) {
    val rimOverhang = 7f
    val rimHeight = 42f
    val mainColor = Color(theme.pipeColor)
    val highlightColor = Color(theme.pipeHighlight)
    val rimColor = Color(theme.pipeRimColor)
    val outlineColor = Color(0xFF1E3A08)

    for (pipe in pipes) {
        val px = pipe.x
        val pw = pipe.width
        val topH = pipe.topPipeHeight
        val botY = pipe.bottomPipeY

        // ================= TOP PIPE (extends from y=0 down to topH) =================
        // Pipe Shaft
        drawRect(
            color = mainColor,
            topLeft = Offset(px, 0f),
            size = Size(pw, topH - rimHeight)
        )
        // Highlight stripe on shaft
        drawRect(
            color = highlightColor,
            topLeft = Offset(px + 8f, 0f),
            size = Size(pw * 0.18f, topH - rimHeight)
        )
        // Shadow on right side of shaft
        drawRect(
            color = rimColor,
            topLeft = Offset(px + pw * 0.76f, 0f),
            size = Size(pw * 0.24f, topH - rimHeight)
        )
        // Shaft Outline
        drawRect(
            color = outlineColor,
            topLeft = Offset(px, 0f),
            size = Size(pw, topH - rimHeight),
            style = Stroke(width = 3.5f)
        )

        // Top Pipe Rim Collar (at bottom of top pipe)
        val rimTopY = topH - rimHeight
        drawRect(
            color = mainColor,
            topLeft = Offset(px - rimOverhang, rimTopY),
            size = Size(pw + rimOverhang * 2, rimHeight)
        )
        drawRect(
            color = highlightColor,
            topLeft = Offset(px - rimOverhang + 8f, rimTopY),
            size = Size((pw + rimOverhang * 2) * 0.18f, rimHeight)
        )
        drawRect(
            color = rimColor,
            topLeft = Offset(px + pw * 0.74f, rimTopY),
            size = Size((pw + rimOverhang * 2) * 0.24f, rimHeight)
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(px - rimOverhang, rimTopY),
            size = Size(pw + rimOverhang * 2, rimHeight),
            style = Stroke(width = 3.5f)
        )

        // ================= BOTTOM PIPE (extends from botY down to groundY) =================
        // Bottom Pipe Rim Collar (at top of bottom pipe)
        drawRect(
            color = mainColor,
            topLeft = Offset(px - rimOverhang, botY),
            size = Size(pw + rimOverhang * 2, rimHeight)
        )
        drawRect(
            color = highlightColor,
            topLeft = Offset(px - rimOverhang + 8f, botY),
            size = Size((pw + rimOverhang * 2) * 0.18f, rimHeight)
        )
        drawRect(
            color = rimColor,
            topLeft = Offset(px + pw * 0.74f, botY),
            size = Size((pw + rimOverhang * 2) * 0.24f, rimHeight)
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(px - rimOverhang, botY),
            size = Size(pw + rimOverhang * 2, rimHeight),
            style = Stroke(width = 3.5f)
        )

        // Pipe Shaft
        val shaftTopY = botY + rimHeight
        val shaftHeight = maxOf(0f, groundY - shaftTopY)
        drawRect(
            color = mainColor,
            topLeft = Offset(px, shaftTopY),
            size = Size(pw, shaftHeight)
        )
        drawRect(
            color = highlightColor,
            topLeft = Offset(px + 8f, shaftTopY),
            size = Size(pw * 0.18f, shaftHeight)
        )
        drawRect(
            color = rimColor,
            topLeft = Offset(px + pw * 0.76f, shaftTopY),
            size = Size(pw * 0.24f, shaftHeight)
        )
        drawRect(
            color = outlineColor,
            topLeft = Offset(px, shaftTopY),
            size = Size(pw, shaftHeight),
            style = Stroke(width = 3.5f)
        )
    }
}

private fun DrawScope.drawGround(
    groundY: Float,
    groundHeight: Float,
    scrollX: Float,
    theme: GameTheme
) {
    // 1. Dirt base
    drawRect(
        color = Color(theme.groundColor),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, groundHeight)
    )

    // 2. Scrolling diagonal stripe pattern for dirt
    val stripeWidth = 24f
    val stripeSpacing = 48f
    var sx = -stripeSpacing - scrollX
    while (sx < size.width + stripeSpacing) {
        val path = Path().apply {
            moveTo(sx, groundY + 24f)
            lineTo(sx + stripeWidth, groundY + 24f)
            lineTo(sx + stripeWidth - 28f, groundY + groundHeight)
            lineTo(sx - 28f, groundY + groundHeight)
            close()
        }
        drawPath(path, color = Color(0x18000000))
        sx += stripeSpacing
    }

    // 3. Top Grass Layer
    val grassHeight = 22f
    drawRect(
        color = Color(theme.grassColor),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, grassHeight)
    )

    // Grass highlight upper lip
    drawRect(
        color = Color(0x44FFFFFF),
        topLeft = Offset(0f, groundY),
        size = Size(size.width, 4.5f)
    )

    // Border line separating sky from grass
    drawLine(
        color = Color(0xFF2B231D),
        start = Offset(0f, groundY),
        end = Offset(size.width, groundY),
        strokeWidth = 4f
    )
    // Border line separating grass from dirt
    drawLine(
        color = Color(0xFF2B231D),
        start = Offset(0f, groundY + grassHeight),
        end = Offset(size.width, groundY + grassHeight),
        strokeWidth = 3f
    )
}

private fun DrawScope.drawBird(bird: BirdState, skin: BirdSkin) {
    val bx = bird.x
    val by = bird.y
    val r = bird.radius

    rotate(degrees = bird.rotation, pivot = Offset(bx, by)) {
        // Optional Phoenix golden halo aura
        skin.glowColor?.let { glow ->
            drawCircle(
                color = Color(glow),
                radius = r * 1.55f,
                center = Offset(bx, by)
            )
        }

        val outlineColor = Color(0xFF251F23)

        // 1. Bird Body (Ellipse/Circle)
        // Shadow/Underbelly
        drawCircle(
            color = Color(skin.shadowColor),
            radius = r,
            center = Offset(bx, by)
        )
        // Main Body highlight
        drawCircle(
            color = Color(skin.bodyColor),
            radius = r * 0.92f,
            center = Offset(bx - 1f, by - 1f)
        )

        // Body Outline
        drawCircle(
            color = outlineColor,
            radius = r,
            center = Offset(bx, by),
            style = Stroke(width = 3.2f)
        )

        // Belly Patch
        val bellyPath = Path().apply {
            addOval(
                Rect(
                    left = bx - r * 0.75f,
                    top = by + r * 0.05f,
                    right = bx + r * 0.15f,
                    bottom = by + r * 0.85f
                )
            )
        }
        drawPath(bellyPath, color = Color(skin.bellyColor))

        // Cute Rosy Cheek
        drawCircle(
            color = Color(0x66FF8A80),
            radius = r * 0.24f,
            center = Offset(bx + r * 0.15f, by + r * 0.28f)
        )

        // 2. Wing with Frame States (0=up, 1=mid, 2=down)
        val wingPath = Path()
        when (bird.wingFrame) {
            0 -> { // Wing pointing up
                wingPath.moveTo(bx - r * 0.8f, by - r * 0.1f)
                wingPath.quadraticTo(bx - r * 0.5f, by - r * 0.95f, bx - r * 0.1f, by - r * 0.4f)
                wingPath.quadraticTo(bx - r * 0.3f, by + r * 0.1f, bx - r * 0.8f, by - r * 0.1f)
            }
            2 -> { // Wing pointing down
                wingPath.moveTo(bx - r * 0.8f, by)
                wingPath.quadraticTo(bx - r * 0.4f, by + r * 0.85f, bx, by + r * 0.35f)
                wingPath.quadraticTo(bx - r * 0.3f, by - r * 0.2f, bx - r * 0.8f, by)
            }
            else -> { // Wing mid
                wingPath.moveTo(bx - r * 0.85f, by - r * 0.05f)
                wingPath.quadraticTo(bx - r * 0.3f, by - r * 0.55f, bx, by - r * 0.05f)
                wingPath.quadraticTo(bx - r * 0.35f, by + r * 0.45f, bx - r * 0.85f, by - r * 0.05f)
            }
        }
        wingPath.close()

        drawPath(wingPath, color = Color(skin.wingColor), style = Fill)
        drawPath(wingPath, color = outlineColor, style = Stroke(width = 2.8f))

        // 3. Eye
        val eyeCenterX = bx + r * 0.35f
        val eyeCenterY = by - r * 0.3f
        val eyeR = r * 0.38f

        // Eye White
        drawCircle(
            color = Color.White,
            radius = eyeR,
            center = Offset(eyeCenterX, eyeCenterY)
        )
        drawCircle(
            color = outlineColor,
            radius = eyeR,
            center = Offset(eyeCenterX, eyeCenterY),
            style = Stroke(width = 2.8f)
        )

        // Pupil
        val pupilR = eyeR * 0.48f
        val pupilX = eyeCenterX + eyeR * 0.28f
        val pupilY = eyeCenterY
        drawCircle(
            color = Color(skin.eyeColor),
            radius = pupilR,
            center = Offset(pupilX, pupilY)
        )
        // Eye Reflection Glint
        drawCircle(
            color = Color.White,
            radius = pupilR * 0.42f,
            center = Offset(pupilX + 1.2f, pupilY - 1.2f)
        )

        // 4. Beak
        val beakTopPath = Path().apply {
            moveTo(bx + r * 0.45f, by - r * 0.08f)
            lineTo(bx + r * 1.35f, by + r * 0.12f)
            lineTo(bx + r * 0.4f, by + r * 0.22f)
            close()
        }
        drawPath(beakTopPath, color = Color(skin.beakColor), style = Fill)
        drawPath(beakTopPath, color = outlineColor, style = Stroke(width = 2.8f))

        val beakBottomPath = Path().apply {
            moveTo(bx + r * 0.42f, by + r * 0.22f)
            lineTo(bx + r * 1.15f, by + r * 0.24f)
            lineTo(bx + r * 0.46f, by + r * 0.42f)
            close()
        }
        drawPath(beakBottomPath, color = Color(skin.beakColor).copy(alpha = 0.88f), style = Fill)
        drawPath(beakBottomPath, color = outlineColor, style = Stroke(width = 2.5f))
    }
}

private fun DrawScope.drawParticles(viewModel: GameViewModel) {
    for (p in viewModel.particles) {
        val color = Color(p.color).copy(alpha = p.alpha)
        drawCircle(
            color = color,
            radius = p.radius,
            center = Offset(p.x, p.y)
        )
    }
}

/**
 * Draws crisp arcade score numbers with thick border in the center of the canvas.
 */
private fun DrawScope.drawArcadeScore(score: Int, screenWidth: Float, centerY: Float) {
    val scoreStr = score.toString()
    val digitWidth = 32f
    val digitHeight = 48f
    val spacing = 8f
    val totalWidth = scoreStr.length * digitWidth + (scoreStr.length - 1) * spacing
    var startX = (screenWidth - totalWidth) / 2f

    for (ch in scoreStr) {
        val digit = ch.digitToIntOrNull() ?: 0
        drawArcadeDigit(digit, startX, centerY - digitHeight / 2f, digitWidth, digitHeight)
        startX += digitWidth + spacing
    }
}

private fun DrawScope.drawArcadeDigit(
    digit: Int,
    x: Float,
    y: Float,
    w: Float,
    h: Float
) {
    // 7-segment style bold arcade digits
    val t = 7.5f // segment thickness
    val halfH = h / 2f

    // Segment active matrix: [top, topLeft, topRight, middle, bottomLeft, bottomRight, bottom]
    val segments = when (digit) {
        0 -> booleanArrayOf(true, true, true, false, true, true, true)
        1 -> booleanArrayOf(false, false, true, false, false, true, false)
        2 -> booleanArrayOf(true, false, true, true, true, false, true)
        3 -> booleanArrayOf(true, false, true, true, false, true, true)
        4 -> booleanArrayOf(false, true, true, true, false, true, false)
        5 -> booleanArrayOf(true, true, false, true, false, true, true)
        6 -> booleanArrayOf(true, true, false, true, true, true, true)
        7 -> booleanArrayOf(true, false, true, false, false, true, false)
        8 -> booleanArrayOf(true, true, true, true, true, true, true)
        9 -> booleanArrayOf(true, true, true, true, false, true, true)
        else -> booleanArrayOf(false, false, false, false, false, false, false)
    }

    val outlineColor = Color(0xFF1B1B1B)
    val fillColor = Color.White

    // Top
    if (segments[0]) drawSegment(x, y, w, t, fillColor, outlineColor)
    // Top-Left
    if (segments[1]) drawSegment(x, y, t, halfH, fillColor, outlineColor)
    // Top-Right
    if (segments[2]) drawSegment(x + w - t, y, t, halfH, fillColor, outlineColor)
    // Middle
    if (segments[3]) drawSegment(x, y + halfH - t / 2f, w, t, fillColor, outlineColor)
    // Bottom-Left
    if (segments[4]) drawSegment(x, y + halfH, t, halfH, fillColor, outlineColor)
    // Bottom-Right
    if (segments[5]) drawSegment(x + w - t, y + halfH, t, halfH, fillColor, outlineColor)
    // Bottom
    if (segments[6]) drawSegment(x, y + h - t, w, t, fillColor, outlineColor)
}

private fun DrawScope.drawSegment(
    x: Float, y: Float, w: Float, h: Float,
    fillColor: Color, outlineColor: Color
) {
    drawRect(
        color = fillColor,
        topLeft = Offset(x, y),
        size = Size(w, h)
    )
    drawRect(
        color = outlineColor,
        topLeft = Offset(x, y),
        size = Size(w, h),
        style = Stroke(width = 2.5f)
    )
}
