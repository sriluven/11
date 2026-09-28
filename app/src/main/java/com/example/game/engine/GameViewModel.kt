package com.example.game.engine

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.data.GameDatabase
import com.example.game.data.GameRecord
import com.example.game.data.GameRepository
import com.example.game.model.BackgroundHill
import com.example.game.model.BirdSkin
import com.example.game.model.BirdState
import com.example.game.model.Cloud
import com.example.game.model.Difficulty
import com.example.game.model.GameStatus
import com.example.game.model.GameTheme
import com.example.game.model.Medal
import com.example.game.model.Particle
import com.example.game.model.PipePair
import com.example.game.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val status: GameStatus = GameStatus.IDLE,
    val score: Int = 0,
    val bestScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val awardedMedal: Medal = Medal.NONE,
    val difficulty: Difficulty = Difficulty.CLASSIC,
    val theme: GameTheme = GameTheme.DAY,
    val birdSkin: BirdSkin = BirdSkin.YELLOW,
    val isSoundEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isPaused: Boolean = false,
    val flashAlpha: Float = 0f
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val soundManager = SoundManager(application)
    private val repository: GameRepository

    val topRecords: StateFlow<List<GameRecord>>
    val totalGamesPlayed: StateFlow<Int>
    val totalScoreSum: StateFlow<Int?>

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Physics & world state exposed for high-frequency Compose rendering
    var bird = BirdState()
        private set
    var pipes = mutableListOf<PipePair>()
        private set
    var particles = mutableListOf<Particle>()
        private set
    var clouds = mutableListOf<Cloud>()
        private set
    var hills = mutableListOf<BackgroundHill>()
        private set

    var groundScrollX: Float = 0f
        private set

    // Dimensions in canvas pixels
    private var screenWidth: Float = 1080f
    private var screenHeight: Float = 2160f
    private var groundHeight: Float = 260f
    private var groundY: Float = 1900f
    private var pipeWidth: Float = 160f
    private var pipeGap: Float = 380f
    private var pipeSpacing: Float = 550f
    private var pipeSpeed: Float = 6.2f
    private var gravity: Float = 0.82f
    private var jumpVelocity: Float = -15.8f
    private var birdBaseRadius: Float = 34f

    private var idleTime: Float = 0f
    private var wingTimer: Float = 0f
    private var nextPipeId: Long = 1L
    private var nextParticleId: Long = 1L

    init {
        val db = GameDatabase.getInstance(application)
        repository = GameRepository(db.gameDao())

        topRecords = repository.topRecords.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        totalGamesPlayed = repository.totalGamesCount.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )
        totalScoreSum = repository.totalScoreSum.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0
        )

        // Observe high score for the active difficulty
        viewModelScope.launch {
            repository.overallHighScore.collect { high ->
                val best = high ?: 0
                _uiState.update { it.copy(bestScore = max(it.bestScore, best)) }
            }
        }
    }

    /**
     * Initializes or adapts game parameters to the actual Canvas dimensions.
     */
    fun onCanvasSizeChanged(width: Float, height: Float, density: Float) {
        if (width <= 0f || height <= 0f) return
        val wasUnset = screenWidth == 1080f && screenHeight == 2160f

        screenWidth = width
        screenHeight = height
        groundHeight = max(180f, height * 0.15f)
        groundY = height - groundHeight

        pipeWidth = min(width * 0.22f, 74f * density)
        pipeSpacing = max(width * 0.65f, 240f * density)
        birdBaseRadius = max(24f, 18f * density)

        updateDifficultyPhysics()

        if (wasUnset || _uiState.value.status == GameStatus.IDLE) {
            resetToIdle()
            generateBackgroundElements()
        }
    }

    private fun updateDifficultyPhysics() {
        val diff = _uiState.value.difficulty
        val densityFactor = screenWidth / 400f
        pipeSpeed = (3.4f * diff.speedFactor) * densityFactor.coerceIn(1.0f, 2.2f)
        pipeGap = (165f * diff.gapMultiplier) * densityFactor.coerceIn(1.0f, 2.4f)
        gravity = (0.58f * diff.gravityFactor) * densityFactor.coerceIn(1.0f, 1.8f)
        jumpVelocity = (-11.8f * diff.jumpVelocityFactor) * densityFactor.coerceIn(1.0f, 1.8f)
    }

    private fun generateBackgroundElements() {
        // Distant background clouds
        clouds.clear()
        for (i in 0..4) {
            clouds.add(
                Cloud(
                    id = i.toLong(),
                    x = Random.nextFloat() * screenWidth,
                    y = Random.nextFloat() * (screenHeight * 0.45f) + 40f,
                    scale = Random.nextFloat() * 0.6f + 0.7f,
                    speed = Random.nextFloat() * 0.5f + 0.4f,
                    alpha = Random.nextFloat() * 0.35f + 0.55f
                )
            )
        }

        // Parallax background hills / skyline
        hills.clear()
        var currentX = 0f
        while (currentX < screenWidth * 1.5f) {
            val hillWidth = Random.nextFloat() * 180f + 140f
            val hillHeight = Random.nextFloat() * 140f + 70f
            hills.add(
                BackgroundHill(
                    x = currentX,
                    width = hillWidth,
                    height = hillHeight,
                    color = 0x2B000000
                )
            )
            currentX += hillWidth * 0.75f
        }
    }

    fun resetToIdle() {
        _uiState.update {
            it.copy(
                status = GameStatus.IDLE,
                score = 0,
                isNewHighScore = false,
                awardedMedal = Medal.NONE,
                isPaused = false,
                flashAlpha = 0f
            )
        }
        val birdStartX = screenWidth * 0.28f
        val birdStartY = (screenHeight - groundHeight) * 0.45f
        bird = BirdState(
            x = birdStartX,
            y = birdStartY,
            velocityY = 0f,
            rotation = 0f,
            wingFrame = 1,
            radius = birdBaseRadius
        )
        pipes.clear()
        particles.clear()
    }

    fun onUserTap() {
        when (_uiState.value.status) {
            GameStatus.IDLE -> {
                soundManager.playFlap()
                _uiState.update { it.copy(status = GameStatus.PLAYING) }
                performFlap()
            }
            GameStatus.PLAYING -> {
                if (!_uiState.value.isPaused) {
                    soundManager.playFlap()
                    performFlap()
                }
            }
            GameStatus.DYING -> {
                // Ignore taps while falling
            }
            GameStatus.GAME_OVER -> {
                // Restart
                soundManager.playSwoosh()
                resetToIdle()
            }
        }
    }

    private fun performFlap() {
        bird = bird.copy(
            velocityY = jumpVelocity,
            rotation = -26f,
            wingFrame = 0
        )
        spawnWingPuffParticles(bird.x - bird.radius * 0.8f, bird.y + bird.radius * 0.2f)
    }

    /**
     * Primary physics and game loop step called on each display frame.
     */
    fun onGameTick(deltaNanos: Long) {
        val dt = (deltaNanos / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        val state = _uiState.value

        if (state.isPaused) return

        // Update flash overlay
        if (state.flashAlpha > 0f) {
            val newAlpha = max(0f, state.flashAlpha - dt * 4.5f)
            _uiState.update { it.copy(flashAlpha = newAlpha) }
        }

        // Always update background scrolling & clouds
        updateEnvironment(dt, state.status)

        when (state.status) {
            GameStatus.IDLE -> {
                idleTime += dt * 4.8f
                wingTimer += dt * 9f
                val bobbingY = (screenHeight - groundHeight) * 0.45f + sin(idleTime) * 16f
                val frame = (wingTimer.toInt() % 3)
                bird = bird.copy(
                    y = bobbingY,
                    velocityY = 0f,
                    rotation = sin(idleTime * 0.8f) * 6f,
                    wingFrame = frame
                )
            }

            GameStatus.PLAYING -> {
                updatePlayingPhysics(dt)
            }

            GameStatus.DYING -> {
                updateDyingPhysics(dt)
            }

            GameStatus.GAME_OVER -> {
                // Game over state (bird on ground, particles fading)
                updateParticles(dt)
            }
        }
    }

    private fun updatePlayingPhysics(dt: Float) {
        // 1. Gravity and velocity
        val newVy = min(bird.velocityY + gravity * (dt * 60f), 24f)
        val newY = bird.y + newVy * (dt * 60f)

        // 2. Bird rotation smoothly dives
        var newRot = bird.rotation
        if (newVy < 0) {
            newRot = max(-28f, newRot - 4f)
        } else {
            newRot = min(82f, newRot + 4.2f * (dt * 60f))
        }

        // 3. Wing animation
        val frame = if (newVy < 0) {
            wingTimer += dt * 14f
            (wingTimer.toInt() % 3)
        } else {
            1 // glide
        }

        // Clamp ceiling
        val clampedY = max(bird.radius, newY)

        bird = bird.copy(
            y = clampedY,
            velocityY = newVy,
            rotation = newRot,
            wingFrame = frame
        )

        // 4. Ground collision
        if (clampedY + bird.radius >= groundY) {
            bird = bird.copy(y = groundY - bird.radius)
            triggerGameOver()
            return
        }

        // 5. Pipes update & spawn
        updatePipes(dt)

        // 6. Check pipe collisions
        checkCollisions()

        // 7. Update particles
        updateParticles(dt)
    }

    private fun updateDyingPhysics(dt: Float) {
        val newVy = min(bird.velocityY + gravity * 1.5f * (dt * 60f), 30f)
        val newY = bird.y + newVy * (dt * 60f)
        val newRot = min(90f, bird.rotation + 8f * (dt * 60f))

        if (newY + bird.radius >= groundY) {
            bird = bird.copy(y = groundY - bird.radius, velocityY = 0f, rotation = 90f)
            soundManager.playDie()
            finalizeGameOver()
        } else {
            bird = bird.copy(y = newY, velocityY = newVy, rotation = newRot)
        }

        updateParticles(dt)
    }

    private fun updatePipes(dt: Float) {
        val moveDist = pipeSpeed * (dt * 60f)

        // Move existing pipes
        for (i in pipes.indices) {
            val pipe = pipes[i]
            val newX = pipe.x - moveDist
            var passed = pipe.passed

            // Check if passed for score
            if (!passed && newX + pipe.width / 2 < bird.x) {
                passed = true
                onScorePoint(pipe)
            }
            pipes[i] = pipe.copy(x = newX, passed = passed)
        }

        // Remove offscreen pipes
        pipes.removeAll { it.x + it.width < -40f }

        // Spawn new pipes if needed
        val rightMostX = pipes.maxOfOrNull { it.x } ?: (screenWidth * 0.85f)
        if (rightMostX < screenWidth + pipeSpacing - pipeWidth) {
            spawnPipePair(screenWidth + 40f)
        }
    }

    private fun spawnPipePair(startX: Float) {
        val minPipeHeight = 100f
        val availableHeight = groundY - pipeGap - (minPipeHeight * 2)
        val randomOffset = Random.nextFloat() * max(40f, availableHeight)
        val topHeight = minPipeHeight + randomOffset
        val gapY = topHeight

        pipes.add(
            PipePair(
                id = nextPipeId++,
                x = startX,
                width = pipeWidth,
                topPipeHeight = topHeight,
                gapY = gapY,
                gapHeight = pipeGap
            )
        )
    }

    private fun checkCollisions() {
        val bx = bird.x
        val by = bird.y
        val br = bird.radius * 0.78f // forgiving hitbox for enjoyable gameplay

        for (pipe in pipes) {
            // Check top pipe rect: [pipe.x, 0, pipe.x + pipe.width, pipe.topPipeHeight]
            if (circleIntersectsRect(bx, by, br, pipe.x, 0f, pipe.x + pipe.width, pipe.topPipeHeight)) {
                onHitObstacle()
                return
            }

            // Check bottom pipe rect: [pipe.x, pipe.bottomPipeY, pipe.x + pipe.width, groundY]
            if (circleIntersectsRect(bx, by, br, pipe.x, pipe.bottomPipeY, pipe.x + pipe.width, groundY)) {
                onHitObstacle()
                return
            }
        }
    }

    private fun circleIntersectsRect(
        cx: Float, cy: Float, cr: Float,
        rx1: Float, ry1: Float, rx2: Float, ry2: Float
    ): Boolean {
        val nearestX = cx.coerceIn(rx1, rx2)
        val nearestY = cy.coerceIn(ry1, ry2)
        val dx = cx - nearestX
        val dy = cy - nearestY
        return (dx * dx + dy * dy) < (cr * cr)
    }

    private fun onScorePoint(pipe: PipePair) {
        soundManager.playPoint()
        _uiState.update { current ->
            val newScore = current.score + 1
            current.copy(score = newScore)
        }
        // Emit sparkling star particles around pipe opening
        val sparkleX = bird.x
        val sparkleY = pipe.gapY + pipe.gapHeight * 0.5f
        spawnScoreSparkles(sparkleX, sparkleY)
    }

    private fun onHitObstacle() {
        soundManager.playHit()
        _uiState.update { it.copy(status = GameStatus.DYING, flashAlpha = 0.85f) }
        spawnHitExplosion(bird.x, bird.y)
    }

    private fun triggerGameOver() {
        soundManager.playHit()
        soundManager.playDie()
        finalizeGameOver()
    }

    private fun finalizeGameOver() {
        val finalScore = _uiState.value.score
        val currentBest = _uiState.value.bestScore
        val isNewBest = finalScore > currentBest
        val newBest = max(finalScore, currentBest)
        val medal = Medal.forScore(finalScore)

        if (medal != Medal.NONE) {
            soundManager.playMedal()
        }

        _uiState.update {
            it.copy(
                status = GameStatus.GAME_OVER,
                bestScore = newBest,
                isNewHighScore = isNewBest,
                awardedMedal = medal
            )
        }

        // Save run to Room database
        viewModelScope.launch {
            repository.recordGame(
                GameRecord(
                    score = finalScore,
                    difficulty = _uiState.value.difficulty.name,
                    birdSkin = _uiState.value.birdSkin.name,
                    theme = _uiState.value.theme.name,
                    medal = medal.name
                )
            )
        }
    }

    private fun updateEnvironment(dt: Float, status: GameStatus) {
        val scrollSpeed = if (status == GameStatus.PLAYING || status == GameStatus.IDLE) {
            pipeSpeed * (dt * 60f)
        } else {
            0f
        }

        // Ground scrolling
        groundScrollX = (groundScrollX + scrollSpeed) % 48f

        // Clouds moving
        for (i in clouds.indices) {
            val c = clouds[i]
            var newX = c.x - c.speed * (dt * 45f)
            if (newX < -150f) {
                newX = screenWidth + 120f
            }
            clouds[i] = c.copy(x = newX)
        }
    }

    // Particle FX
    private fun spawnWingPuffParticles(x: Float, y: Float) {
        for (i in 0..2) {
            particles.add(
                Particle(
                    id = nextParticleId++,
                    x = x + (Random.nextFloat() * 8f - 4f),
                    y = y + (Random.nextFloat() * 8f - 4f),
                    vx = -(Random.nextFloat() * 2f + 1f),
                    vy = Random.nextFloat() * 2f - 1f,
                    radius = Random.nextFloat() * 5f + 4f,
                    alpha = 0.7f,
                    color = 0xAAFFFFFF,
                    life = 0.28f,
                    maxLife = 0.28f
                )
            )
        }
    }

    private fun spawnScoreSparkles(x: Float, y: Float) {
        for (i in 0..6) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 4f + 2f
            particles.add(
                Particle(
                    id = nextParticleId++,
                    x = x,
                    y = y + (Random.nextFloat() * 60f - 30f),
                    vx = (kotlin.math.cos(angle) * speed),
                    vy = (sin(angle) * speed),
                    radius = Random.nextFloat() * 6f + 3f,
                    alpha = 1f,
                    color = if (i % 2 == 0) 0xFFFFD700 else 0xFFFFFFFF,
                    life = 0.45f,
                    maxLife = 0.45f
                )
            )
        }
    }

    private fun spawnHitExplosion(x: Float, y: Float) {
        for (i in 0..12) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 6f + 2f
            particles.add(
                Particle(
                    id = nextParticleId++,
                    x = x,
                    y = y,
                    vx = (kotlin.math.cos(angle) * speed),
                    vy = (sin(angle) * speed),
                    radius = Random.nextFloat() * 7f + 4f,
                    alpha = 1f,
                    color = 0xFFFFA000,
                    life = 0.4f,
                    maxLife = 0.4f
                )
            )
        }
    }

    private fun updateParticles(dt: Float) {
        for (i in particles.indices) {
            val p = particles[i]
            val newLife = p.life - dt
            val lifeRatio = (newLife / p.maxLife).coerceIn(0f, 1f)
            particles[i] = p.copy(
                x = p.x + p.vx,
                y = p.y + p.vy,
                life = newLife,
                alpha = lifeRatio
            )
        }
        particles.removeAll { it.life <= 0f }
    }

    // User settings & controls
    fun setDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty) }
        updateDifficultyPhysics()
        viewModelScope.launch {
            repository.getHighScoreForDifficulty(difficulty.name).collect { high ->
                val best = high ?: 0
                _uiState.update { it.copy(bestScore = best) }
            }
        }
    }

    fun setTheme(theme: GameTheme) {
        _uiState.update { it.copy(theme = theme) }
    }

    fun setBirdSkin(skin: BirdSkin) {
        _uiState.update { it.copy(birdSkin = skin) }
    }

    fun toggleSound() {
        val next = !_uiState.value.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = next) }
        soundManager.isSoundEnabled = next
    }

    fun toggleHaptics() {
        val next = !_uiState.value.isHapticsEnabled
        _uiState.update { it.copy(isHapticsEnabled = next) }
        soundManager.isHapticsEnabled = next
    }

    fun togglePause() {
        if (_uiState.value.status == GameStatus.PLAYING) {
            _uiState.update { it.copy(isPaused = !it.isPaused) }
        }
    }

    fun resumeGame() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun clearStats() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(bestScore = 0) }
        }
    }
}
