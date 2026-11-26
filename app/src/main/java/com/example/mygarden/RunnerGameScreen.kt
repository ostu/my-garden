package com.example.mygarden

import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.atomic.AtomicBoolean
import com.example.mygarden.ui.theme.MyGardenTheme
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val RunnerInitialLives = 3
private const val RunnerBackgroundMusicVolume = 0.3f
private const val RunnerBackgroundMusicGameOverVolume = 0.15f
private const val RunnerSfxVolume = 0.7f
private const val RunnerWorldSpeed = 230f
private const val RunnerGravity = 1500f
private const val RunnerJumpImpulse = 760f
private const val RunnerPhoneWorldSpeedMultiplier = 1.35f
private const val RunnerPhoneJumpImpulseMultiplier = 1.18f
private const val RunnerPhoneGravityMultiplier = 0.9f
private const val RunnerRunFrameCount = 16
private const val RunnerRunFrameColumns = 8
private const val RunnerJumpFrameCount = 16
private const val RunnerJumpFrameColumns = 8
private const val RunnerJumpLandingFrameCount = 4
private const val RunnerJumpLandingTriggerRatio = 0.2f
private const val RunnerJumpLandingStartFrame = RunnerJumpFrameCount - RunnerJumpLandingFrameCount
private const val RunnerJumpLandingVisualDownOffsetRatio = 0.06f
private const val RunnerFallFrameCount = 8
private const val RunnerFallFrameColumns = 8
private const val RunnerSpriteFrameDurationNanos = 90_000_000L
private const val RunnerJumpLandingAnimationDurationNanos =
    RunnerJumpLandingFrameCount * RunnerSpriteFrameDurationNanos
private const val RunnerHitAnimationDurationNanos =
    RunnerFallFrameCount * RunnerSpriteFrameDurationNanos
private const val RunnerDebugDrawHitboxes = false
private const val RunnerTrackSourceYRatio = 720f / 941f
private const val RunnerObstacleMinSpawnSeconds = 1.5f
private const val RunnerObstacleMaxSpawnSeconds = 2.5f
private const val RunnerStarMinSpawnSeconds = 1.0f
private const val RunnerStarMaxSpawnSeconds = 2.0f
private const val RunnerStarFrameCount = 8
private const val RunnerStarFrameColumns = 8
private const val RunnerRockHitboxScale = 0.52f
private const val RunnerBeehiveHitboxWidthScale = 0.12f
private const val RunnerBeehiveHitboxHeightScale = 0.24f
private const val RunnerBeehiveHitboxTopInsetBias = 0.78f
private const val RunnerBeehiveHitboxOffsetXRatio = -0.08f
private const val RunnerBeehiveHitboxOffsetYRatio = -0.18f
private const val RunnerPlayerHitboxWidthScale = 0.64f
private const val RunnerPlayerHitboxHeightScale = 0.82f
private const val RunnerTabletGroundLiftRatio = 0.05f
private const val RunnerPhoneGroundLiftRatio = 0f
private const val RunnerTabletRockVerticalLiftRatio = 0.005f
private const val RunnerPhoneRockVerticalLiftRatio = 0f
private const val RunnerPhonePlayerDownOffsetRatio = 0.035f
private const val RunnerPhoneRockDownOffsetRatio = 0.03f
private const val RunnerCompactPhoneLandscapePlayerDownOffsetRatio = 0.012f
private const val RunnerCompactPhoneLandscapeRockDownOffsetRatio = 0.02f
private const val RunnerCompactPhoneLandscapeBeehiveDownOffsetRatio = 0.02f
private const val RunnerCompactPhoneLandscapeControlsWidthRatio = 0.6f
private const val RunnerCompactPhoneLandscapeControlsVerticalPaddingDp = 8f
private const val RunnerDefaultControlsVerticalPaddingDp = 14f
private const val RunnerDefaultControlsWidthRatio = 0.84f
private const val RunnerTabletBeehiveVerticalLiftRatio = 0.03f
private const val RunnerPhoneBeehiveVerticalLiftRatio = 0.05f
private const val RunnerCompactPhoneLandscapeBeehiveVerticalLiftRatio = 0.075f
private const val RunnerTabletBeehiveVisualHeightMultiplier = 1f
private const val RunnerPhoneBeehiveVisualHeightMultiplier = 0.68f
private const val RunnerCompactPhoneLandscapeBeehiveVisualHeightMultiplier = 0.76f
private const val RunnerTabletBeehiveVisualTopOffsetRatio = 0.02f
private const val RunnerPhoneBeehiveVisualTopOffsetRatio = 0.02f
private const val RunnerCompactPhoneLandscapeBeehiveVisualTopOffsetRatio = 0f
private const val RunnerPhoneBeehiveDownOffsetRatio = 0.07f
private const val RunnerHiveBodyTopRatio = 0.545f
private const val RunnerHiveBodyBottomRatio = 0.995f
private const val RunnerHiveVisualLiftRatio = 0.08f
private val RunnerWorldOverlapWithHud = 8.dp
private const val RunnerTopHudHorizontalCropPx = 10
private const val RunnerFallSpriteVerticalOffsetRatio = 0.04f

private enum class RunnerPlayerState {
    RUNNING,
    JUMPING,
    FALLING
}

private enum class RunnerObstacleType {
    GROUND_ROCK,
    AIR_BEEHIVE
}

private data class RunnerObstacle(
    val id: Long,
    val type: RunnerObstacleType,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isActive: Boolean = true
)

private data class RunnerStar(
    val id: Long,
    val x: Float,
    val y: Float,
    val size: Float,
    val isActive: Boolean = true
)

private data class RunnerSfx(
    val soundPool: SoundPool,
    val jumpSoundId: Int,
    val landingSoundId: Int,
    val starSoundId: Int
)

private data class RunnerGameState(
    val score: Int = 0,
    val lives: Int = RunnerInitialLives,
    val playerX: Float = 0f,
    val playerY: Float = 0f,
    val playerWidth: Float = 0f,
    val playerHeight: Float = 0f,
    val groundY: Float = 0f,
    val velocityY: Float = 0f,
    val playerState: RunnerPlayerState = RunnerPlayerState.RUNNING,
    val isGameOver: Boolean = false
)

@Composable
fun RunnerGameScreen(
    onHomeClick: () -> Unit,
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resources = context.resources
    val backgroundBitmap1 = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_background1) }
    val backgroundBitmap2 = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_background2) }
    val rockBitmap = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_rock) }
    val hiveBitmap = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_hive2) }
    val starSpriteSheet = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_star_spritesheet) }
    val topPanelBitmap = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_top_panel) }
    val jumpButtonBitmap = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_button_jump) }
    val runSpriteSheet = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_max_run_spritesheet2) }
    val jumpSpriteSheet = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_max_jump_spritesheet2) }
    val fallSpriteSheet = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_max_fall_spritesheet) }
    LockScreenOrientation(
        orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
        enabled = !isInPreview
    )
    val backgroundMusicPlayer = remember(context, isInPreview) {
        if (isInPreview) {
            null
        } else {
            MediaPlayer.create(context, R.raw.background_run_music)?.apply {
                isLooping = true
                setVolume(RunnerBackgroundMusicVolume, RunnerBackgroundMusicVolume)
            }
        }
    }
    val runnerSfx = remember(isInPreview, context) {
        if (isInPreview) {
            null
        } else {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val soundPool = SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(3)
                .build()
            val jumpSoundId = soundPool.load(context, R.raw.run_game_sfx_jump, 1)
            val landingSoundId = soundPool.load(context, R.raw.run_game_sfx_landing, 1)
            val starSoundId = soundPool.load(context, R.raw.run_game_sfx_star, 1)
            RunnerSfx(soundPool, jumpSoundId, landingSoundId, starSoundId)
        }
    }
    var isGameOverForMusic by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    val musicEnabledState = rememberUpdatedState(musicEnabled)
    val musicVolumeState = rememberUpdatedState(
        if (isGameOverForMusic) {
            RunnerBackgroundMusicGameOverVolume
        } else {
            RunnerBackgroundMusicVolume
        }
    )

    DisposableEffect(lifecycleOwner, backgroundMusicPlayer) {
        val player = backgroundMusicPlayer ?: return@DisposableEffect onDispose { }
        var isReleased = false

        fun releasePlayer() {
            if (isReleased) return
            isReleased = true
            runCatching { player.release() }
        }

        fun syncPlayback() {
            if (isReleased) return
            val volume = musicVolumeState.value
            runCatching { player.setVolume(volume, volume) }
            if (musicEnabledState.value) {
                if (!player.isPlaying) {
                    runCatching { player.start() }
                }
            } else if (player.isPlaying) {
                runCatching { player.pause() }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> syncPlayback()
                Lifecycle.Event.ON_PAUSE -> if (!isReleased && player.isPlaying) {
                    runCatching { player.pause() }
                }

                Lifecycle.Event.ON_DESTROY -> releasePlayer()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        syncPlayback()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            releasePlayer()
        }
    }
    DisposableEffect(runnerSfx) {
        onDispose {
            runnerSfx?.soundPool?.release()
        }
    }

    LaunchedEffect(isGameOverForMusic, backgroundMusicPlayer, musicEnabled) {
        backgroundMusicPlayer?.runCatching {
            val volume = if (isGameOverForMusic) {
                RunnerBackgroundMusicGameOverVolume
            } else {
                RunnerBackgroundMusicVolume
            }
            setVolume(volume, volume)
            if (musicEnabled) {
                if (!isPlaying) start()
            } else if (isPlaying) {
                pause()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val isLandscapeLayout = screenWidthPx > screenHeightPx
        val isTabletDevice = maxWidth >= 900.dp
        val screenAspectRatio = if (screenHeightPx > 0f) screenWidthPx / screenHeightPx else 1f
        val isCompactPhoneLandscape = !isTabletDevice && isLandscapeLayout && screenAspectRatio >= 2.05f
        val worldWidthPx = max(0f, screenWidthPx - with(density) { 32.dp.toPx() })
        val contentVerticalPaddingPx = with(density) { 10.dp.toPx() }
        val contentSpacingPx = with(density) { if (isLandscapeLayout) 10.dp.toPx() else 12.dp.toPx() }
        val controlsHorizontalPaddingPx = with(density) { 18.dp.toPx() }
        val controlsVerticalPaddingPx = with(density) {
            if (isCompactPhoneLandscape) {
                RunnerCompactPhoneLandscapeControlsVerticalPaddingDp.dp.toPx()
            } else {
                RunnerDefaultControlsVerticalPaddingDp.dp.toPx()
            }
        }
        val topHudAspectRatio = topPanelBitmap.width.toFloat() / topPanelBitmap.height.toFloat()
        val jumpButtonAspectRatio = jumpButtonBitmap.width.toFloat() / jumpButtonBitmap.height.toFloat()
        val topHudHeightPx = if (topHudAspectRatio > 0f) worldWidthPx / topHudAspectRatio else 0f
        val topHudHeightDp = with(density) { topHudHeightPx.toDp() }
        val controlsWidthRatio = if (isCompactPhoneLandscape) {
            RunnerCompactPhoneLandscapeControlsWidthRatio
        } else {
            RunnerDefaultControlsWidthRatio
        }
        val controlsButtonWidthPx = max(0f, (worldWidthPx - controlsHorizontalPaddingPx * 2f) * controlsWidthRatio)
        val controlsPanelHeightPx = if (jumpButtonAspectRatio > 0f) {
            controlsButtonWidthPx / jumpButtonAspectRatio + controlsVerticalPaddingPx * 2f
        } else {
            controlsVerticalPaddingPx * 2f
        }
        val worldHeightPx = max(
            0f,
            screenHeightPx - (contentVerticalPaddingPx * 2f) - topHudHeightPx - controlsPanelHeightPx - (contentSpacingPx * 2f)
        )
        val backgroundScale = max(
            screenWidthPx / backgroundBitmap1.width.toFloat(),
            screenHeightPx / backgroundBitmap1.height.toFloat()
        )
        val background1WidthPx = backgroundBitmap1.width.toFloat() * backgroundScale
        val background1HeightPx = backgroundBitmap1.height.toFloat() * backgroundScale
        val background2WidthPx = backgroundBitmap2.width.toFloat() * backgroundScale
        val background2HeightPx = backgroundBitmap2.height.toFloat() * backgroundScale
        val backgroundTopPx = (screenHeightPx - background1HeightPx) / 2f
        val background2TopPx = (screenHeightPx - background2HeightPx) / 2f
        val trackScreenY = backgroundTopPx + backgroundBitmap1.height.toFloat() * RunnerTrackSourceYRatio * backgroundScale
        val worldTopOnScreenPx = contentVerticalPaddingPx + topHudHeightPx + contentSpacingPx
        val playerGroundLiftRatio = if (isTabletDevice) {
            RunnerTabletGroundLiftRatio
        } else {
            RunnerPhoneGroundLiftRatio
        }
        val rockVerticalLiftRatio = if (isTabletDevice) {
            RunnerTabletRockVerticalLiftRatio
        } else {
            RunnerPhoneRockVerticalLiftRatio
        }
        val beehiveVerticalLiftRatio = if (isTabletDevice) {
            RunnerTabletBeehiveVerticalLiftRatio
        } else if (isCompactPhoneLandscape) {
            RunnerCompactPhoneLandscapeBeehiveVerticalLiftRatio
        } else {
            RunnerPhoneBeehiveVerticalLiftRatio
        }
        val beehiveVisualHeightMultiplier = if (isTabletDevice) {
            RunnerTabletBeehiveVisualHeightMultiplier
        } else if (isCompactPhoneLandscape) {
            RunnerCompactPhoneLandscapeBeehiveVisualHeightMultiplier
        } else {
            RunnerPhoneBeehiveVisualHeightMultiplier
        }
        val beehiveVisualTopOffsetRatio = if (isTabletDevice) {
            RunnerTabletBeehiveVisualTopOffsetRatio
        } else if (isCompactPhoneLandscape) {
            RunnerCompactPhoneLandscapeBeehiveVisualTopOffsetRatio
        } else {
            RunnerPhoneBeehiveVisualTopOffsetRatio
        }
        val playerDownOffsetPx = when {
            isTabletDevice -> 0f
            isCompactPhoneLandscape -> screenWidthPx * RunnerCompactPhoneLandscapePlayerDownOffsetRatio
            else -> screenWidthPx * RunnerPhonePlayerDownOffsetRatio
        }
        val rockDownOffsetPx = when {
            isTabletDevice -> 0f
            isCompactPhoneLandscape -> screenWidthPx * RunnerCompactPhoneLandscapeRockDownOffsetRatio
            else -> screenWidthPx * RunnerPhoneRockDownOffsetRatio
        }
        val beehiveDownOffsetPx = when {
            isCompactPhoneLandscape -> screenWidthPx * (RunnerPhoneBeehiveDownOffsetRatio + RunnerCompactPhoneLandscapeBeehiveDownOffsetRatio)
            isTabletDevice -> 0f
            else -> screenWidthPx * RunnerPhoneBeehiveDownOffsetRatio
        }
        val runnerWorldSpeed = if (isTabletDevice) {
            RunnerWorldSpeed
        } else {
            RunnerWorldSpeed * RunnerPhoneWorldSpeedMultiplier
        }
        val runnerJumpImpulse = if (isTabletDevice) {
            RunnerJumpImpulse
        } else {
            RunnerJumpImpulse * RunnerPhoneJumpImpulseMultiplier
        }
        val runnerGravity = if (isTabletDevice) {
            RunnerGravity
        } else {
            RunnerGravity * RunnerPhoneGravityMultiplier
        }
        val playerWidth = if (isLandscapeLayout) worldWidthPx * 0.11f else screenWidthPx * 0.13f
        val playerHeight = playerWidth * 1.1f
        val groundY = (trackScreenY - worldTopOnScreenPx).coerceIn(playerHeight, worldHeightPx)
        val resolvedGroundY = groundY.coerceIn(playerHeight, worldHeightPx)
        val playerX = if (isLandscapeLayout) worldWidthPx * 0.12f else screenWidthPx * 0.14f
        val playerGroundTop = (resolvedGroundY - playerHeight - playerHeight * playerGroundLiftRatio)
            .plus(playerDownOffsetPx)
            .coerceAtLeast(0f)
        var background1X by remember(screenWidthPx, screenHeightPx) { mutableStateOf(0f) }
        var background2X by remember(screenWidthPx, screenHeightPx) { mutableStateOf(background1WidthPx) }

        var gameState by remember(screenWidthPx, screenHeightPx) {
            mutableStateOf(
                RunnerGameState(
                    playerX = playerX,
                    playerY = playerGroundTop,
                    playerWidth = playerWidth,
                    playerHeight = playerHeight,
                    groundY = resolvedGroundY
                )
            )
        }
        val obstacles = remember { mutableStateListOf<RunnerObstacle>() }
        val stars = remember { mutableStateListOf<RunnerStar>() }
        var nextObstacleId by remember { mutableLongStateOf(0L) }
        var nextStarId by remember { mutableLongStateOf(0L) }
        var restartToken by remember { mutableIntStateOf(0) }
        var playerAnimationState by remember { mutableStateOf(RunnerPlayerState.RUNNING) }
        var playerAnimationElapsedNanos by remember { mutableLongStateOf(0L) }
        var starAnimationElapsedNanos by remember { mutableLongStateOf(0L) }
        var wasAirborne by remember { mutableStateOf(false) }
        var hitAnimationRemainingNanos by remember { mutableLongStateOf(0L) }
        var jumpLandingRemainingNanos by remember { mutableLongStateOf(0L) }
        var pendingGameOver by remember { mutableStateOf(false) }
        val jumpRequested = remember { AtomicBoolean(false) }

        val resetGame = {
            obstacles.clear()
            stars.clear()
            nextObstacleId = 0L
            nextStarId = 0L
            gameState = RunnerGameState(
                playerX = playerX,
                playerY = playerGroundTop,
                playerWidth = playerWidth,
                playerHeight = playerHeight,
                groundY = resolvedGroundY
            )
            isGameOverForMusic = false
            isSettingsVisible = false
            playerAnimationState = RunnerPlayerState.RUNNING
            playerAnimationElapsedNanos = 0L
            wasAirborne = false
            hitAnimationRemainingNanos = 0L
            jumpLandingRemainingNanos = 0L
            pendingGameOver = false
            jumpRequested.set(false)
            background1X = 0f
            background2X = background1WidthPx
            restartToken += 1
        }

        LaunchedEffect(screenWidthPx, screenHeightPx) {
            if (screenWidthPx > 0f && screenHeightPx > 0f) {
                gameState = gameState.copy(
                    playerX = playerX,
                    playerY = if (gameState.playerY.isFinite()) min(gameState.playerY, playerGroundTop) else playerGroundTop,
                    playerWidth = playerWidth,
                    playerHeight = playerHeight,
                    groundY = resolvedGroundY
                )
            }
        }

        LaunchedEffect(restartToken, screenWidthPx, screenHeightPx, gameState.isGameOver, isSettingsVisible) {
            if (screenWidthPx <= 0f || screenHeightPx <= 0f || gameState.isGameOver || isSettingsVisible) {
                return@LaunchedEffect
            }

            var lastFrameNanos = 0L
            var obstacleSpawnTimer = randomBetween(RunnerObstacleMinSpawnSeconds, RunnerObstacleMaxSpawnSeconds)
            var starSpawnTimer = randomBetween(RunnerStarMinSpawnSeconds, RunnerStarMaxSpawnSeconds)

            while (true) {
                val frameNanos = withFrameNanos { it }
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameNanos
                    continue
                }

                val deltaNanos = frameNanos - lastFrameNanos
                val deltaSeconds = deltaNanos / 1_000_000_000f
                lastFrameNanos = frameNanos
                starAnimationElapsedNanos += deltaNanos

                val currentGameState = gameState
                if (currentGameState.playerState == RunnerPlayerState.FALLING && hitAnimationRemainingNanos > 0L) {
                    hitAnimationRemainingNanos = (hitAnimationRemainingNanos - deltaNanos).coerceAtLeast(0L)
                    playerAnimationState = RunnerPlayerState.FALLING
                    playerAnimationElapsedNanos =
                        (playerAnimationElapsedNanos + deltaNanos).coerceAtMost(RunnerHitAnimationDurationNanos)

                    if (hitAnimationRemainingNanos == 0L) {
                        if (pendingGameOver) {
                            gameState = gameState.copy(isGameOver = true)
                            obstacles.clear()
                            stars.clear()
                            isGameOverForMusic = true
                        } else {
                            gameState = gameState.copy(
                                playerY = playerGroundTop,
                                velocityY = 0f,
                                playerState = RunnerPlayerState.RUNNING
                            )
                            playerAnimationState = RunnerPlayerState.RUNNING
                            playerAnimationElapsedNanos = 0L
                            jumpLandingRemainingNanos = 0L
                            wasAirborne = false
                        }
                        pendingGameOver = false
                    }
                    continue
                }

                val isInLandingPhase =
                    jumpLandingRemainingNanos > 0L && currentGameState.playerY >= playerGroundTop - 1f
                if (isInLandingPhase) {
                    jumpLandingRemainingNanos =
                        (jumpLandingRemainingNanos - deltaNanos).coerceAtLeast(0L)
                    playerAnimationState = RunnerPlayerState.JUMPING
                    gameState = currentGameState.copy(
                        playerY = playerGroundTop,
                        velocityY = 0f,
                        playerState = if (jumpLandingRemainingNanos == 0L) {
                            RunnerPlayerState.RUNNING
                        } else {
                            RunnerPlayerState.JUMPING
                        }
                    )

                    if (jumpLandingRemainingNanos == 0L) {
                        playerAnimationState = RunnerPlayerState.RUNNING
                        playerAnimationElapsedNanos = 0L
                        wasAirborne = false
                    }
                    continue
                }

                background1X -= runnerWorldSpeed * deltaSeconds
                background2X -= runnerWorldSpeed * deltaSeconds
                if (background1X + background1WidthPx <= 0f) {
                    background1X = background2X + background2WidthPx
                }
                if (background2X + background2WidthPx <= 0f) {
                    background2X = background1X + background1WidthPx
                }

                val isOnGround = currentGameState.playerY >= playerGroundTop - 1f
                val isPlayingJumpLandingAnimation = isOnGround && jumpLandingRemainingNanos > 0L
                val shouldStartJump = isOnGround && !isPlayingJumpLandingAnimation && jumpRequested.getAndSet(false)
                if (shouldStartJump) {
                    if (soundEnabled) runnerSfx?.let { sfx ->
                        sfx.soundPool.play(sfx.jumpSoundId, RunnerSfxVolume, RunnerSfxVolume, 1, 0, 1f)
                    }
                    jumpLandingRemainingNanos = 0L
                }

                val velocityY =
                    (if (shouldStartJump) -runnerJumpImpulse else currentGameState.velocityY) +
                        runnerGravity * deltaSeconds
                val nextPlayerY = currentGameState.playerY + velocityY * deltaSeconds
                val grounded = nextPlayerY >= playerGroundTop
                val justLanded = wasAirborne && grounded && jumpLandingRemainingNanos == 0L
                if (justLanded) {
                    if (soundEnabled) runnerSfx?.let { sfx ->
                        sfx.soundPool.play(sfx.landingSoundId, RunnerSfxVolume, RunnerSfxVolume, 1, 0, 1f)
                    }
                    jumpLandingRemainingNanos = RunnerJumpLandingAnimationDurationNanos
                    playerAnimationState = RunnerPlayerState.JUMPING
                    playerAnimationElapsedNanos =
                        RunnerJumpLandingStartFrame * RunnerSpriteFrameDurationNanos
                }
                val resolvedPlayerY = if (grounded) playerGroundTop else nextPlayerY
                val resolvedVelocityY = if (grounded) 0f else velocityY
                val playerState = if (grounded && jumpLandingRemainingNanos == 0L) {
                    RunnerPlayerState.RUNNING
                } else {
                    RunnerPlayerState.JUMPING
                }

                if (playerAnimationState != playerState) {
                    playerAnimationState = playerState
                    playerAnimationElapsedNanos = 0L
                } else {
                    if (playerState == RunnerPlayerState.JUMPING && grounded && jumpLandingRemainingNanos > 0L) {
                        playerAnimationElapsedNanos =
                            (playerAnimationElapsedNanos + deltaNanos)
                                .coerceAtMost((RunnerJumpFrameCount - 1) * RunnerSpriteFrameDurationNanos)
                    } else {
                        playerAnimationElapsedNanos += deltaNanos
                    }
                }

                gameState = gameState.copy(
                    playerY = resolvedPlayerY,
                    velocityY = resolvedVelocityY,
                    playerState = playerState,
                    playerX = playerX,
                    playerWidth = playerWidth,
                    playerHeight = playerHeight,
                    groundY = resolvedGroundY
                )
                wasAirborne = !grounded

                obstacleSpawnTimer -= deltaSeconds
                starSpawnTimer -= deltaSeconds

                if (obstacleSpawnTimer <= 0f && canSpawnObstacle(obstacles, screenWidthPx)) {
                    val type = if (Random.nextBoolean()) {
                        RunnerObstacleType.GROUND_ROCK
                    } else {
                        RunnerObstacleType.AIR_BEEHIVE
                    }
                    obstacles += createRunnerObstacle(
                        id = nextObstacleId++,
                        type = type,
                        screenWidthPx = screenWidthPx,
                        groundY = resolvedGroundY,
                        worldTopPx = 0f,
                        rockVerticalLiftRatio = rockVerticalLiftRatio,
                        rockDownOffsetPx = rockDownOffsetPx,
                        beehiveVerticalLiftRatio = beehiveVerticalLiftRatio,
                        beehiveDownOffsetPx = beehiveDownOffsetPx
                    )
                    obstacleSpawnTimer = randomBetween(RunnerObstacleMinSpawnSeconds, RunnerObstacleMaxSpawnSeconds)
                }

                if (starSpawnTimer <= 0f && canSpawnStar(stars, screenWidthPx, obstacles)) {
                    stars += createRunnerStar(
                        id = nextStarId++,
                        screenWidthPx = screenWidthPx,
                        groundY = resolvedGroundY,
                        worldTopPx = 0f
                    )
                    starSpawnTimer = randomBetween(RunnerStarMinSpawnSeconds, RunnerStarMaxSpawnSeconds)
                }

                val playerRect = playerCollisionRect(gameState)

                var collisionDetected = false
                for (index in obstacles.lastIndex downTo 0) {
                    val obstacle = obstacles[index]
                    val movedObstacle = obstacle.copy(x = obstacle.x - runnerWorldSpeed * deltaSeconds)
                    val obstacleRect = obstacleCollisionRect(movedObstacle)

                    if (obstacleRect.right < 0f || !movedObstacle.isActive) {
                        obstacles.removeAt(index)
                        continue
                    }

                    if (obstacleRect.overlaps(playerRect)) {
                        if (soundEnabled) runnerSfx?.let { sfx ->
                            sfx.soundPool.play(sfx.landingSoundId, RunnerSfxVolume, RunnerSfxVolume, 1, 0, 1f)
                        }
                        val updatedLives = (gameState.lives - 1).coerceAtLeast(0)
                        obstacles.removeAt(index)
                        gameState = gameState.copy(
                            lives = updatedLives,
                            velocityY = 0f,
                            playerY = playerGroundTop,
                            playerState = RunnerPlayerState.FALLING
                        )
                        jumpRequested.set(false)
                        jumpLandingRemainingNanos = 0L
                        hitAnimationRemainingNanos = RunnerHitAnimationDurationNanos
                        pendingGameOver = updatedLives == 0
                        playerAnimationState = RunnerPlayerState.FALLING
                        playerAnimationElapsedNanos = 0L
                        collisionDetected = true
                        break
                    } else {
                        obstacles[index] = movedObstacle
                    }
                }

                if (collisionDetected) {
                    continue
                }

                if (gameState.isGameOver) {
                    continue
                }

                for (index in stars.lastIndex downTo 0) {
                    val star = stars[index]
                    val movedStar = star.copy(x = star.x - runnerWorldSpeed * deltaSeconds)
                    val starRect = Rect(
                        left = movedStar.x,
                        top = movedStar.y,
                        right = movedStar.x + movedStar.size,
                        bottom = movedStar.y + movedStar.size
                    )

                    if (starRect.right < 0f || !movedStar.isActive) {
                        stars.removeAt(index)
                        continue
                    }

                    if (starRect.overlaps(playerRect)) {
                        if (soundEnabled) runnerSfx?.let { sfx ->
                            sfx.soundPool.play(sfx.starSoundId, RunnerSfxVolume, RunnerSfxVolume, 1, 0, 1f)
                        }
                        gameState = gameState.copy(score = gameState.score + 1)
                        stars[index] = movedStar.copy(isActive = false)
                    } else {
                        stars[index] = movedStar
                    }
                }

                obstacles.removeAll { !it.isActive }
                stars.removeAll { !it.isActive }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    image = backgroundBitmap1,
                    dstOffset = androidx.compose.ui.unit.IntOffset(background1X.toInt(), backgroundTopPx.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        background1WidthPx.toInt().coerceAtLeast(1),
                        background1HeightPx.toInt().coerceAtLeast(1)
                    )
                )
                drawImage(
                    image = backgroundBitmap2,
                    dstOffset = androidx.compose.ui.unit.IntOffset(background2X.toInt(), background2TopPx.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(
                        background2WidthPx.toInt().coerceAtLeast(1),
                        background2HeightPx.toInt().coerceAtLeast(1)
                    )
                )
            }

            RunnerTopHud(
                score = gameState.score,
                lives = gameState.lives,
                onHomeClick = onHomeClick,
                onSettingsClick = { isSettingsVisible = true },
                compact = isLandscapeLayout,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 0.dp, bottom = 10.dp)
            ) {
                Spacer(modifier = Modifier.height(topHudHeightDp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .offset(y = -RunnerWorldOverlapWithHud)
                        .zIndex(0f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRunnerWorld(
                            gameState = gameState,
                            obstacles = obstacles,
                            stars = stars,
                            playerFrameIndex = resolveRunnerPlayerFrameIndex(
                                gameState = gameState,
                                playerState = playerAnimationState,
                                animationElapsedNanos = playerAnimationElapsedNanos,
                                jumpLandingRemainingNanos = jumpLandingRemainingNanos
                            ),
                            starFrameIndex = resolveRunnerStarFrameIndex(starAnimationElapsedNanos),
                            runSpriteSheet = runSpriteSheet,
                            jumpSpriteSheet = jumpSpriteSheet,
                            fallSpriteSheet = fallSpriteSheet,
                            rockBitmap = rockBitmap,
                            starSpriteSheet = starSpriteSheet,
                            hiveBitmap = hiveBitmap,
                            beehiveVisualHeightMultiplier = beehiveVisualHeightMultiplier,
                            beehiveVisualTopOffsetRatio = beehiveVisualTopOffsetRatio,
                            worldTop = 0f,
                            worldBottom = size.height
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(
                        if (isLandscapeLayout) 10.dp - RunnerWorldOverlapWithHud
                        else 12.dp - RunnerWorldOverlapWithHud
                    )
                )

                RunnerControlsPanel(
                    enabled = !gameState.isGameOver && !isSettingsVisible,
                    buttonWidthRatio = controlsWidthRatio,
                    verticalPaddingDp = if (isCompactPhoneLandscape) {
                        RunnerCompactPhoneLandscapeControlsVerticalPaddingDp.dp
                    } else {
                        RunnerDefaultControlsVerticalPaddingDp.dp
                    },
                    onJumpClick = {
                        if (!gameState.isGameOver) {
                            jumpRequested.set(true)
                        }
                    }
                )
            }

            if (gameState.isGameOver) {
                AppleStyleGameOverOverlay(
                    score = gameState.score,
                    onHomeClick = onHomeClick,
                    onRestartClick = resetGame,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(100f)
                )
            }

            if (isSettingsVisible) {
                SettingsOverlay(
                    musicEnabled = musicEnabled,
                    soundEnabled = soundEnabled,
                    onToggleMusic = onToggleMusic,
                    onToggleSound = onToggleSound,
                    onResume = { isSettingsVisible = false },
                    onRestart = {
                        isSettingsVisible = false
                        resetGame()
                    },
                    onHome = {
                        isSettingsVisible = false
                        onHomeClick()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun RunnerTopHud(
    score: Int,
    lives: Int,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val resources = LocalContext.current.resources
    val panelBitmap = remember(resources) { ImageBitmap.imageResource(resources, R.drawable.run_game_top_panel) }
    val homePainter = painterResource(id = R.drawable.common_button_home_circle)
    val settingsPainter = painterResource(id = R.drawable.common_button_settings_circle)
    val scoreIconPainter = painterResource(id = R.drawable.common_game_ui_star)
    val panelAspectRatio = panelBitmap.width.toFloat() / panelBitmap.height.toFloat()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(panelAspectRatio)
    ) {
        val scoreAreaStart = maxWidth * 0.115f
        val scoreAreaWidth = maxWidth * 0.15f
        val scoreCenterY = maxHeight * 0.485f
        val scoreIconSize = maxHeight * 0.43f
        val scoreTextStartPadding = scoreIconSize + maxWidth * 0.014f
        val scoreTextEndPadding = maxWidth * 0.010f
        val scoreFontSize = (maxHeight.value * if (compact) 0.28f else 0.31f).sp
        val heartRowEndPadding = maxWidth * 0.13f
        val heartRowTopPadding = maxHeight * 0.25f
        val heartSize = maxHeight * 0.42f
        val heartSpacing = maxWidth * 0.016f
        val iconButtonSize = maxHeight * 0.70f
        val iconButtonTopPadding = maxHeight * 0.15f
        val settingsButtonTopPadding = maxHeight * 0.15f
        val homeButtonStartPadding = maxWidth * 0.009f
        val settingsButtonEndPadding = maxWidth * 0.0125f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val horizontalCropPx = RunnerTopHudHorizontalCropPx
                .coerceAtMost((panelBitmap.width / 2) - 1)
                .coerceAtLeast(0)
            drawImage(
                image = panelBitmap,
                srcOffset = androidx.compose.ui.unit.IntOffset(horizontalCropPx, 0),
                srcSize = androidx.compose.ui.unit.IntSize(
                    panelBitmap.width - horizontalCropPx * 2,
                    panelBitmap.height
                ),
                dstSize = androidx.compose.ui.unit.IntSize(
                    size.width.toInt().coerceAtLeast(1),
                    size.height.toInt().coerceAtLeast(1)
                )
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.085f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHomeClick
                )
        ) {
            Image(
                painter = homePainter,
                contentDescription = "Home",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = homeButtonStartPadding, top = iconButtonTopPadding)
                    .size(iconButtonSize)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = scoreAreaStart, top = scoreCenterY - scoreIconSize / 2f)
                .width(scoreAreaWidth)
                .height(scoreIconSize)
        ) {
            Image(
                painter = scoreIconPainter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(scoreIconSize)
            )
            Text(
                text = score.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterEnd)
                    .padding(start = scoreTextStartPadding, end = scoreTextEndPadding),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = scoreFontSize),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3A2D1F),
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = heartRowEndPadding, top = heartRowTopPadding),
            horizontalArrangement = Arrangement.spacedBy(heartSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(RunnerInitialLives) { index ->
                Image(
                    painter = painterResource(
                        id = if (index < lives.coerceAtLeast(0)) {
                            R.drawable.common_ui_heart
                        } else {
                            R.drawable.common_ui_heart_disable
                        }
                    ),
                    contentDescription = "Life ${index + 1}",
                    modifier = Modifier.size(heartSize)
                )
            }
        }

        Image(
            painter = settingsPainter,
            contentDescription = "Settings",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = settingsButtonTopPadding, end = settingsButtonEndPadding)
                .size(iconButtonSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSettingsClick
                )
        )
    }
}

@Composable
private fun RunnerControlsPanel(
    enabled: Boolean,
    buttonWidthRatio: Float,
    verticalPaddingDp: androidx.compose.ui.unit.Dp,
    onJumpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jumpButtonPainter = painterResource(id = R.drawable.run_game_button_jump)
    val jumpButtonAspectRatio = remember(jumpButtonPainter) { jumpButtonPainter.aspectRatioOrFallback() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = verticalPaddingDp)
    ) {
        Image(
            painter = jumpButtonPainter,
            contentDescription = "\u041f\u0420\u042b\u0416\u041e\u041a",
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(buttonWidthRatio)
                .aspectRatio(jumpButtonAspectRatio)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onJumpClick
                ),
            contentScale = ContentScale.Fit,
            alpha = if (enabled) 1f else 0.72f
        )
    }
}

@Composable
private fun DecorativeFlower(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3A9C7))
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color(0xFFECCB5F))
        )
    }
}

@Composable
private fun RunnerGameOverOverlay(
    score: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelPainter = painterResource(id = R.drawable.common_game_over_panel)
    val restartButtonPainter = painterResource(id = R.drawable.common_button_repeat)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val restartAspectRatio = remember(restartButtonPainter) { restartButtonPainter.aspectRatioOrFallback() }

    Box(
        modifier = modifier
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .aspectRatio(panelAspectRatio)
        ) {
            Image(
                painter = panelPainter,
                contentDescription = "Game Over",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.72f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "\u0418\u0442\u043e\u0433\u043e\u0432\u044b\u0439 \u0441\u0447\u0451\u0442",
                    color = Color(0xFF6B4A28),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = score.toString(),
                    color = Color(0xFFC55A2E),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(restartAspectRatio)
                        .clickable(onClick = onRestart),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = restartButtonPainter,
                        contentDescription = "Restart",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "\u041d\u0430\u0447\u0430\u0442\u044c \u0441\u043d\u0430\u0447\u0430\u043b\u0430",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun createRunnerObstacle(
    id: Long,
    type: RunnerObstacleType,
    screenWidthPx: Float,
    groundY: Float,
    worldTopPx: Float,
    rockVerticalLiftRatio: Float,
    rockDownOffsetPx: Float,
    beehiveVerticalLiftRatio: Float,
    beehiveDownOffsetPx: Float
): RunnerObstacle {
    return when (type) {
        RunnerObstacleType.GROUND_ROCK -> {
            val width = screenWidthPx * 0.055f
            val height = width * 0.75f
            RunnerObstacle(
                id = id,
                type = type,
                x = screenWidthPx + width,
                y = groundY - height - screenWidthPx * rockVerticalLiftRatio + rockDownOffsetPx,
                width = width,
                height = height
            )
        }

        RunnerObstacleType.AIR_BEEHIVE -> {
            val width = screenWidthPx * 0.10f
            val height = width * 1.1f
            RunnerObstacle(
                id = id,
                type = type,
                x = screenWidthPx + width,
                y = max(
                    worldTopPx + 12f,
                    groundY - height - screenWidthPx * (0.12f + beehiveVerticalLiftRatio) + beehiveDownOffsetPx
                ),
                width = width,
                height = height
            )
        }
    }
}

private fun obstacleCollisionRect(obstacle: RunnerObstacle): Rect {
    return when (obstacle.type) {
        RunnerObstacleType.GROUND_ROCK -> {
            val hitboxWidth = obstacle.width * RunnerRockHitboxScale
            val hitboxHeight = obstacle.height * RunnerRockHitboxScale
            val insetX = (obstacle.width - hitboxWidth) * 0.5f
            val insetY = (obstacle.height - hitboxHeight) * 0.5f
            Rect(
                left = obstacle.x + insetX,
                top = obstacle.y + insetY,
                right = obstacle.x + obstacle.width - insetX,
                bottom = obstacle.y + obstacle.height - insetY
            )
        }

        RunnerObstacleType.AIR_BEEHIVE -> {
            val hitboxWidth = obstacle.width * RunnerBeehiveHitboxWidthScale
            val hitboxHeight = obstacle.height * RunnerBeehiveHitboxHeightScale
            val insetX = (obstacle.width - hitboxWidth) * 0.5f
            val removedHeight = obstacle.height - hitboxHeight
            val topInset = removedHeight * RunnerBeehiveHitboxTopInsetBias
            val bottomInset = removedHeight - topInset
            val offsetX = obstacle.width * RunnerBeehiveHitboxOffsetXRatio
            val offsetY = obstacle.height * RunnerBeehiveHitboxOffsetYRatio
            Rect(
                left = obstacle.x + insetX + offsetX,
                top = obstacle.y + topInset + offsetY,
                right = obstacle.x + obstacle.width - insetX + offsetX,
                bottom = obstacle.y + obstacle.height - bottomInset + offsetY
            )
        }
    }
}

private fun playerCollisionRect(gameState: RunnerGameState): Rect {
    val hitboxWidth = gameState.playerWidth * RunnerPlayerHitboxWidthScale
    val hitboxHeight = gameState.playerHeight * RunnerPlayerHitboxHeightScale
    val insetX = (gameState.playerWidth - hitboxWidth) * 0.5f
    val removedHeight = gameState.playerHeight - hitboxHeight
    val insetY = removedHeight * 0.5f
    return Rect(
        left = gameState.playerX + insetX,
        top = gameState.playerY + insetY,
        right = gameState.playerX + gameState.playerWidth - insetX,
        bottom = gameState.playerY + gameState.playerHeight - insetY
    )
}

private fun createRunnerStar(
    id: Long,
    screenWidthPx: Float,
    groundY: Float,
    worldTopPx: Float
): RunnerStar {
    val size = screenWidthPx * 0.04f
    val lane = Random.nextInt(3)
    val y = when (lane) {
        0 -> groundY - size - screenWidthPx * 0.04f
        1 -> groundY - size - screenWidthPx * 0.18f
        else -> worldTopPx + screenWidthPx * 0.10f
    }
    return RunnerStar(
        id = id,
        x = screenWidthPx + size,
        y = y,
        size = size
    )
}

private fun canSpawnObstacle(obstacles: List<RunnerObstacle>, screenWidthPx: Float): Boolean {
    val lastObstacleX = obstacles.maxOfOrNull { it.x }
    return lastObstacleX == null || lastObstacleX < screenWidthPx * 0.82f
}

private fun canSpawnStar(
    stars: List<RunnerStar>,
    screenWidthPx: Float,
    obstacles: List<RunnerObstacle>
): Boolean {
    val lastStarX = stars.maxOfOrNull { it.x } ?: 0f
    val lastObstacleX = obstacles.maxOfOrNull { it.x } ?: 0f
    return lastStarX < screenWidthPx * 0.86f && lastObstacleX < screenWidthPx * 0.92f
}

private fun randomBetween(min: Float, max: Float): Float {
    return Random.nextFloat() * (max - min) + min
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerWorld(
    gameState: RunnerGameState,
    obstacles: List<RunnerObstacle>,
    stars: List<RunnerStar>,
    playerFrameIndex: Int,
    starFrameIndex: Int,
    runSpriteSheet: ImageBitmap,
    jumpSpriteSheet: ImageBitmap,
    fallSpriteSheet: ImageBitmap,
    rockBitmap: ImageBitmap,
    starSpriteSheet: ImageBitmap,
    hiveBitmap: ImageBitmap,
    beehiveVisualHeightMultiplier: Float,
    beehiveVisualTopOffsetRatio: Float,
    worldTop: Float,
    worldBottom: Float
) {
    stars.forEach { star ->
        drawStar(
            star = star,
            starSpriteSheet = starSpriteSheet,
            frameIndex = starFrameIndex
        )
    }

    obstacles.forEach { obstacle ->
        when (obstacle.type) {
            RunnerObstacleType.GROUND_ROCK -> drawRock(obstacle, rockBitmap)
            RunnerObstacleType.AIR_BEEHIVE ->
                drawBeehive(
                    obstacle = obstacle,
                    worldTop = worldTop,
                    hiveBitmap = hiveBitmap,
                    visualHeightMultiplier = beehiveVisualHeightMultiplier,
                    visualTopOffsetRatio = beehiveVisualTopOffsetRatio
                )
        }
    }

    drawRunnerPlayer(
        gameState = gameState,
        frameIndex = playerFrameIndex,
        runSpriteSheet = runSpriteSheet,
        jumpSpriteSheet = jumpSpriteSheet,
        fallSpriteSheet = fallSpriteSheet
    )

    if (RunnerDebugDrawHitboxes) {
        drawRunnerHitboxes(gameState = gameState, obstacles = obstacles)
    }
}

private fun DrawScope.drawRunnerHitboxes(
    gameState: RunnerGameState,
    obstacles: List<RunnerObstacle>
) {
    val playerRect = playerCollisionRect(gameState)
    drawRect(
        color = Color(0xAA00C853),
        topLeft = Offset(playerRect.left, playerRect.top),
        size = Size(playerRect.width, playerRect.height),
        style = Stroke(width = size.minDimension * 0.006f)
    )

    obstacles.forEach { obstacle ->
        val rect = obstacleCollisionRect(obstacle)
        val color = when (obstacle.type) {
            RunnerObstacleType.GROUND_ROCK -> Color(0xAA2962FF)
            RunnerObstacleType.AIR_BEEHIVE -> Color(0xAAFF3D00)
        }
        drawRect(
            color = color,
            topLeft = Offset(rect.left, rect.top),
            size = Size(rect.width, rect.height),
            style = Stroke(width = size.minDimension * 0.006f)
        )
    }
}

private fun DrawScope.drawRunnerWorldFrame() {
    val innerLeft = size.width * 0.012f
    val innerTop = size.height * 0.014f
    val innerWidth = size.width * 0.976f
    val innerHeight = size.height * 0.972f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFCEEC8), Color(0xFFF7DFAB))),
        cornerRadius = CornerRadius(44f, 44f),
        size = size
    )
    drawRoundRect(
        color = Color(0xFFD5A756),
        cornerRadius = CornerRadius(44f, 44f),
        style = Stroke(width = size.minDimension * 0.014f),
        size = size
    )
    drawRoundRect(
        color = Color(0xFFF9EBCB),
        topLeft = Offset(innerLeft, innerTop),
        size = Size(innerWidth, innerHeight),
        cornerRadius = CornerRadius(36f, 36f),
        style = Stroke(width = size.minDimension * 0.004f)
    )
}

private fun DrawScope.drawBlurCloud(left: Float, top: Float, radius: Float) {
    drawCircle(Color.White.copy(alpha = 0.72f), radius, Offset(left, top))
    drawCircle(Color.White.copy(alpha = 0.72f), radius * 0.82f, Offset(left + radius * 0.82f, top - radius * 0.18f))
    drawCircle(Color.White.copy(alpha = 0.72f), radius * 0.68f, Offset(left + radius * 1.48f, top + radius * 0.06f))
}

private fun DrawScope.drawFence(top: Float, height: Float) {
    val slatWidth = size.width * 0.018f
    val gap = slatWidth * 0.18f
    var currentX = 0f
    while (currentX < size.width) {
        drawRoundRect(
            color = Color(0xFFF1E4BE),
            topLeft = Offset(currentX, top),
            size = Size(slatWidth, height),
            cornerRadius = CornerRadius(8f, 8f)
        )
        currentX += slatWidth + gap
    }
    drawRoundRect(
        color = Color(0xFFE8D7A8),
        topLeft = Offset(0f, top + height * 0.26f),
        size = Size(size.width, height * 0.12f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    drawRoundRect(
        color = Color(0xFFE8D7A8),
        topLeft = Offset(0f, top + height * 0.64f),
        size = Size(size.width, height * 0.12f),
        cornerRadius = CornerRadius(10f, 10f)
    )
}

private fun DrawScope.drawSceneTree(
    trunkX: Float,
    trunkBottom: Float,
    trunkWidth: Float,
    trunkHeight: Float,
    foliageRadius: Float
) {
    drawRoundRect(
        color = Color(0xFF7F4A1F),
        topLeft = Offset(trunkX, trunkBottom - trunkHeight),
        size = Size(trunkWidth, trunkHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )
    val centerX = trunkX + trunkWidth / 2f
    val centerY = trunkBottom - trunkHeight
    drawCircle(Color(0xFF76C83F), foliageRadius, Offset(centerX, centerY))
    drawCircle(Color(0xFF64B935), foliageRadius * 0.86f, Offset(centerX - foliageRadius * 0.56f, centerY + foliageRadius * 0.12f))
    drawCircle(Color(0xFF87D957), foliageRadius * 0.82f, Offset(centerX + foliageRadius * 0.62f, centerY + foliageRadius * 0.14f))
}

private fun DrawScope.drawHouse(
    baseLeft: Float,
    baseTop: Float,
    baseWidth: Float,
    baseHeight: Float
) {
    drawRoundRect(
        color = Color(0xFFF8E7BC),
        topLeft = Offset(baseLeft, baseTop),
        size = Size(baseWidth, baseHeight),
        cornerRadius = CornerRadius(24f, 24f)
    )
    val roof = Path().apply {
        moveTo(baseLeft - baseWidth * 0.04f, baseTop + baseHeight * 0.20f)
        lineTo(baseLeft + baseWidth * 0.32f, baseTop - baseHeight * 0.22f)
        lineTo(baseLeft + baseWidth * 0.70f, baseTop - baseHeight * 0.02f)
        lineTo(baseLeft + baseWidth * 1.02f, baseTop + baseHeight * 0.18f)
        close()
    }
    drawPath(roof, color = Color(0xFFC96E35))
    drawPath(roof, color = Color(0xFF9F4E24), style = Stroke(width = 5f))
    drawCircle(
        color = Color(0xFFB8E0F8),
        radius = baseWidth * 0.12f,
        center = Offset(baseLeft + baseWidth * 0.53f, baseTop + baseHeight * 0.42f)
    )
    drawCircle(
        color = Color(0xFF7F4A1F),
        radius = baseWidth * 0.125f,
        center = Offset(baseLeft + baseWidth * 0.53f, baseTop + baseHeight * 0.42f),
        style = Stroke(width = 4f)
    )
    drawRoundRect(
        color = Color(0xFF9C5D2C),
        topLeft = Offset(baseLeft + baseWidth * 0.40f, baseTop + baseHeight * 0.62f),
        size = Size(baseWidth * 0.18f, baseHeight * 0.30f),
        cornerRadius = CornerRadius(14f, 14f)
    )
}

private fun DrawScope.drawFlowerBush(
    center: Offset,
    bushWidth: Float,
    bushHeight: Float
) {
    drawRoundRect(
        color = Color(0xFF4A9D34),
        topLeft = Offset(center.x - bushWidth / 2f, center.y - bushHeight / 2f),
        size = Size(bushWidth, bushHeight),
        cornerRadius = CornerRadius(28f, 28f)
    )
    repeat(5) { index ->
        val flowerX = center.x - bushWidth * 0.32f + bushWidth * 0.16f * index
        val flowerY = center.y - bushHeight * 0.10f + if (index % 2 == 0) -bushHeight * 0.12f else bushHeight * 0.08f
        drawMiniFlower(Offset(flowerX, flowerY), bushWidth * 0.06f)
    }
}

private fun DrawScope.drawMiniFlower(center: Offset, petalRadius: Float) {
    repeat(4) { index ->
        val angle = Math.toRadians((index * 90).toDouble())
        drawCircle(
            color = Color(0xFFFFD8E8),
            radius = petalRadius,
            center = Offset(
                center.x + cos(angle).toFloat() * petalRadius,
                center.y + sin(angle).toFloat() * petalRadius
            )
        )
    }
    drawCircle(color = Color(0xFFF4C641), radius = petalRadius * 0.72f, center = center)
}

private fun DrawScope.drawDustPuff(center: Offset) {
    drawCircle(Color(0xFFFBE9C5), size.minDimension * 0.012f, center)
    drawCircle(Color(0xFFFBE9C5).copy(alpha = 0.85f), size.minDimension * 0.008f, Offset(center.x - size.minDimension * 0.012f, center.y + size.minDimension * 0.004f))
    drawCircle(Color(0xFFFBE9C5).copy(alpha = 0.7f), size.minDimension * 0.006f, Offset(center.x + size.minDimension * 0.014f, center.y - size.minDimension * 0.004f))
}

private fun resolveRunnerPlayerFrameIndex(
    gameState: RunnerGameState,
    playerState: RunnerPlayerState,
    animationElapsedNanos: Long,
    jumpLandingRemainingNanos: Long
): Int {
    if (playerState == RunnerPlayerState.JUMPING && jumpLandingRemainingNanos > 0L) {
        return resolveRunnerLandingFrameIndex(jumpLandingRemainingNanos)
    }

    val rawFrameIndex = (animationElapsedNanos / RunnerSpriteFrameDurationNanos).toInt()
    return when (playerState) {
        RunnerPlayerState.RUNNING -> rawFrameIndex % RunnerRunFrameCount
        RunnerPlayerState.JUMPING -> resolveRunnerJumpFrameIndex(gameState, rawFrameIndex)
        RunnerPlayerState.FALLING -> rawFrameIndex.coerceIn(0, RunnerFallFrameCount - 1)
    }
}

private fun resolveRunnerLandingFrameIndex(jumpLandingRemainingNanos: Long): Int {
    val elapsedNanos = (RunnerJumpLandingAnimationDurationNanos - jumpLandingRemainingNanos).coerceAtLeast(0L)
    val frameOffset = (elapsedNanos / RunnerSpriteFrameDurationNanos)
        .toInt()
        .coerceIn(0, RunnerJumpLandingFrameCount - 1)
    return RunnerJumpLandingStartFrame + frameOffset
}

private fun resolveRunnerJumpFrameIndex(
    gameState: RunnerGameState,
    rawFrameIndex: Int
): Int {
    val landingStartFrame = RunnerJumpFrameCount - RunnerJumpLandingFrameCount
    return rawFrameIndex.coerceIn(0, landingStartFrame - 1)
}

private fun resolveRunnerStarFrameIndex(animationElapsedNanos: Long): Int {
    val rawFrameIndex = (animationElapsedNanos / RunnerSpriteFrameDurationNanos).toInt()
    return rawFrameIndex % RunnerStarFrameCount
}

private fun DrawScope.drawSpriteFrame(
    spriteSheet: ImageBitmap,
    frameIndex: Int,
    frameCount: Int,
    frameColumns: Int,
    destinationRect: Rect
) {
    if (frameCount <= 0 || frameColumns <= 0) return
    val safeFrameIndex = frameIndex.coerceIn(0, frameCount - 1)
    val frameRows = ((frameCount + frameColumns - 1) / frameColumns).coerceAtLeast(1)
    val frameWidth = spriteSheet.width / frameColumns
    val frameHeight = spriteSheet.height / frameRows
    if (frameWidth <= 0 || frameHeight <= 0) return
    val srcColumn = safeFrameIndex % frameColumns
    val srcRow = safeFrameIndex / frameColumns

    drawImage(
        image = spriteSheet,
        srcOffset = androidx.compose.ui.unit.IntOffset(frameWidth * srcColumn, frameHeight * srcRow),
        srcSize = androidx.compose.ui.unit.IntSize(frameWidth, frameHeight),
        dstOffset = androidx.compose.ui.unit.IntOffset(destinationRect.left.toInt(), destinationRect.top.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(
            width = destinationRect.width.toInt().coerceAtLeast(1),
            height = destinationRect.height.toInt().coerceAtLeast(1)
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRunnerPlayer(
    gameState: RunnerGameState,
    frameIndex: Int,
    runSpriteSheet: ImageBitmap,
    jumpSpriteSheet: ImageBitmap,
    fallSpriteSheet: ImageBitmap
) {
    val playerRect = Rect(
        left = gameState.playerX,
        top = gameState.playerY,
        right = gameState.playerX + gameState.playerWidth,
        bottom = gameState.playerY + gameState.playerHeight
    )
    val spriteSheet = when (gameState.playerState) {
        RunnerPlayerState.RUNNING -> runSpriteSheet
        RunnerPlayerState.JUMPING -> jumpSpriteSheet
        RunnerPlayerState.FALLING -> fallSpriteSheet
    }
    val (frameCount, frameColumns) = when (gameState.playerState) {
        RunnerPlayerState.RUNNING -> RunnerRunFrameCount to RunnerRunFrameColumns
        RunnerPlayerState.JUMPING -> RunnerJumpFrameCount to RunnerJumpFrameColumns
        RunnerPlayerState.FALLING -> RunnerFallFrameCount to RunnerFallFrameColumns
    }
    val isGroundedJumpLanding =
        gameState.playerState == RunnerPlayerState.JUMPING &&
            gameState.velocityY == 0f &&
            (gameState.groundY - (gameState.playerY + gameState.playerHeight)).coerceAtLeast(0f) <= 1f
    val extraVerticalOffset = when {
        gameState.playerState == RunnerPlayerState.FALLING ->
            gameState.playerHeight * RunnerFallSpriteVerticalOffsetRatio
        isGroundedJumpLanding ->
            gameState.playerHeight * RunnerJumpLandingVisualDownOffsetRatio
        else -> 0f
    }
    drawSpriteFrame(
        spriteSheet = spriteSheet,
        frameIndex = frameIndex,
        frameCount = frameCount,
        frameColumns = frameColumns,
        destinationRect = Rect(
            left = playerRect.left - gameState.playerWidth * 0.10f,
            top = playerRect.top - gameState.playerHeight * 0.08f + extraVerticalOffset,
            right = playerRect.right + gameState.playerWidth * 0.16f,
            bottom = playerRect.bottom + gameState.playerHeight * 0.015f + extraVerticalOffset
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRock(
    obstacle: RunnerObstacle,
    rockBitmap: ImageBitmap
) {
    drawImage(
        image = rockBitmap,
        dstOffset = androidx.compose.ui.unit.IntOffset(obstacle.x.toInt(), obstacle.y.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(obstacle.width.toInt(), obstacle.height.toInt())
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBeehive(
    obstacle: RunnerObstacle,
    worldTop: Float,
    hiveBitmap: ImageBitmap,
    visualHeightMultiplier: Float,
    visualTopOffsetRatio: Float
) {
    val bitmapAspectRatio = hiveBitmap.width.toFloat() / hiveBitmap.height.toFloat()
    val hiveBodyHeightRatio = RunnerHiveBodyBottomRatio - RunnerHiveBodyTopRatio
    val visualHeight = obstacle.height / hiveBodyHeightRatio * visualHeightMultiplier
    val visualWidth = visualHeight * bitmapAspectRatio
    val visualLeft = obstacle.x + (obstacle.width - visualWidth) * 0.5f
    val visualTop = obstacle.y - visualHeight * RunnerHiveBodyTopRatio - visualHeight * visualTopOffsetRatio

    drawImage(
        image = hiveBitmap,
        dstOffset = androidx.compose.ui.unit.IntOffset(visualLeft.toInt(), visualTop.toInt()),
        dstSize = androidx.compose.ui.unit.IntSize(
            visualWidth.toInt().coerceAtLeast(1),
            visualHeight.toInt().coerceAtLeast(1)
        )
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    star: RunnerStar,
    starSpriteSheet: ImageBitmap,
    frameIndex: Int
) {
    drawSpriteFrame(
        spriteSheet = starSpriteSheet,
        frameIndex = frameIndex,
        frameCount = RunnerStarFrameCount,
        frameColumns = RunnerStarFrameColumns,
        destinationRect = Rect(
            left = star.x,
            top = star.y,
            right = star.x + star.size,
            bottom = star.y + star.size
        )
    )
}

private fun Painter.aspectRatioOrFallback(): Float {
    val intrinsicSize = intrinsicSize
    return if (
        intrinsicSize.width.isFinite() &&
        intrinsicSize.height.isFinite() &&
        intrinsicSize.width > 0f &&
        intrinsicSize.height > 0f
    ) {
        intrinsicSize.width / intrinsicSize.height
    } else {
        1f
    }
}

@Preview(name = "Runner Preview", showBackground = true)
@Composable
fun RunnerGameScreenPreview() {
    MyGardenTheme {
        RunnerGameScreen(
            onHomeClick = {},
            musicEnabled = true,
            soundEnabled = true,
            onToggleMusic = {},
            onToggleSound = {}
        )
    }
}

@Preview(name = "Runner Landscape Preview", showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun RunnerGameScreenLandscapePreview() {
    MyGardenTheme {
        RunnerGameScreen(
            onHomeClick = {},
            musicEnabled = true,
            soundEnabled = true,
            onToggleMusic = {},
            onToggleSound = {}
        )
    }
}

@Composable
private fun LockScreenOrientation(
    orientation: Int,
    enabled: Boolean
) {
    val context = LocalContext.current
    DisposableEffect(context, orientation, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val activity = context.findActivity()
            if (activity == null) {
                onDispose { }
            } else {
                val previousOrientation = activity.requestedOrientation
                activity.requestedOrientation = orientation
                onDispose {
                    activity.requestedOrientation = previousOrientation
                }
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
