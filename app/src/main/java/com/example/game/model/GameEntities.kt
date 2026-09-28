package com.example.game.model

/**
 * Bird physics and animation state.
 */
data class BirdState(
    val x: Float = 0f,
    val y: Float = 0f,
    val velocityY: Float = 0f,
    val rotation: Float = 0f,
    val wingFrame: Int = 1, // 0: up, 1: mid, 2: down
    val radius: Float = 18f
)

/**
 * Pipe pair obstacle (upper and lower pipe).
 */
data class PipePair(
    val id: Long,
    val x: Float,
    val width: Float,
    val topPipeHeight: Float,
    val gapY: Float,
    val gapHeight: Float,
    val passed: Boolean = false
) {
    val bottomPipeY: Float
        get() = gapY + gapHeight
}

/**
 * Particle effect for wing flap puffs, collision burst, and score sparkle.
 */
data class Particle(
    val id: Long,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val alpha: Float,
    val color: Long,
    val life: Float, // 1.0 -> 0.0
    val maxLife: Float
)

/**
 * Background decorative floating cloud.
 */
data class Cloud(
    val id: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float
)

/**
 * Distant city building or retro hill profile for parallax scrolling.
 */
data class BackgroundHill(
    val x: Float,
    val width: Float,
    val height: Float,
    val color: Long
)
