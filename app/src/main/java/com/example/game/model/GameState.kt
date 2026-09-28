package com.example.game.model

/**
 * State of the Flappy Bird game cycle.
 */
enum class GameStatus {
    IDLE,       // Waiting to start; bird gently floats
    PLAYING,    // Active gameplay with pipes moving and collision checks
    DYING,      // Hit pipe or obstacle; bird falling to ground
    GAME_OVER   // Fallen; score board displayed
}

/**
 * Difficulty levels affecting pipe speed, gap size, and gravity.
 */
enum class Difficulty(
    val displayName: String,
    val speedFactor: Float,
    val gapMultiplier: Float,
    val gravityFactor: Float,
    val jumpVelocityFactor: Float
) {
    CASUAL("Casual", 0.85f, 1.25f, 0.85f, 0.95f),
    CLASSIC("Classic", 1.0f, 1.0f, 1.0f, 1.0f),
    HARDCORE("Hardcore", 1.25f, 0.82f, 1.15f, 1.08f)
}

/**
 * Visual themes for the sky, ground, and background.
 */
enum class GameTheme(
    val displayName: String,
    val skyColorTop: Long,
    val skyColorBottom: Long,
    val pipeColor: Long,
    val pipeRimColor: Long,
    val pipeHighlight: Long,
    val groundColor: Long,
    val grassColor: Long
) {
    DAY(
        displayName = "Daylight",
        skyColorTop = 0xFF4EC0CA,
        skyColorBottom = 0xFF70D6E4,
        pipeColor = 0xFF73BF2E,
        pipeRimColor = 0xFF558022,
        pipeHighlight = 0xFF9DE64E,
        groundColor = 0xFFDED895,
        grassColor = 0xFF73BF2E
    ),
    SUNSET(
        displayName = "Sunset",
        skyColorTop = 0xFFE65C40,
        skyColorBottom = 0xFFF9A825,
        pipeColor = 0xFF2E7D32,
        pipeRimColor = 0xFF1B5E20,
        pipeHighlight = 0xFF4CAF50,
        groundColor = 0xFFBCAAA4,
        grassColor = 0xFF388E3C
    ),
    NIGHT(
        displayName = "Midnight",
        skyColorTop = 0xFF0D1B2A,
        skyColorBottom = 0xFF1B263B,
        pipeColor = 0xFF00B4D8,
        pipeRimColor = 0xFF0077B6,
        pipeHighlight = 0xFF90E0EF,
        groundColor = 0xFF4A4E69,
        grassColor = 0xFF22577A
    )
}

/**
 * Bird character skins with unique color palettes.
 */
enum class BirdSkin(
    val displayName: String,
    val bodyColor: Long,
    val shadowColor: Long,
    val wingColor: Long,
    val beakColor: Long,
    val bellyColor: Long,
    val eyeColor: Long = 0xFF2C2428,
    val glowColor: Long? = null
) {
    YELLOW(
        displayName = "Classic Chirp",
        bodyColor = 0xFFFFD338,
        shadowColor = 0xFFE59B20,
        wingColor = 0xFFFFFFFF,
        beakColor = 0xFFF4511E,
        bellyColor = 0xFFFFF6CC
    ),
    BLUE(
        displayName = "Blue Jay",
        bodyColor = 0xFF42A5F5,
        shadowColor = 0xFF1976D2,
        wingColor = 0xFFE3F2FD,
        beakColor = 0xFFFF9800,
        bellyColor = 0xFFBBDEFB
    ),
    RED(
        displayName = "Red Cardinal",
        bodyColor = 0xFFE53935,
        shadowColor = 0xFFB71C1C,
        wingColor = 0xFFFFCDD2,
        beakColor = 0xFFFFB300,
        bellyColor = 0xFFFFEBEE
    ),
    GOLDEN(
        displayName = "Phoenix Gold",
        bodyColor = 0xFFFFCA28,
        shadowColor = 0xFFFFA000,
        wingColor = 0xFFFFF9C4,
        beakColor = 0xFFFF6F00,
        bellyColor = 0xFFFFFDE7,
        glowColor = 0x66FFD700
    )
}

/**
 * Medals awarded at Game Over depending on score.
 */
enum class Medal(val minScore: Int, val displayName: String, val colorHex: Long) {
    NONE(0, "None", 0x00000000),
    BRONZE(10, "Bronze", 0xFFCD7F32),
    SILVER(20, "Silver", 0xFFC0C0C0),
    GOLD(30, "Gold", 0xFFFFD700),
    PLATINUM(40, "Platinum", 0xFFE5E4E2);

    companion object {
        fun forScore(score: Int): Medal {
            return when {
                score >= PLATINUM.minScore -> PLATINUM
                score >= GOLD.minScore -> GOLD
                score >= SILVER.minScore -> SILVER
                score >= BRONZE.minScore -> BRONZE
                else -> NONE
            }
        }
    }
}
