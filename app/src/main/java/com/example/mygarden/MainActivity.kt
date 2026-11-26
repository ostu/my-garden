package com.example.mygarden

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mygarden.ui.theme.MyGardenTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private const val InitialLives = 3
private const val BackgroundMusicVolume = 0.3f
private const val BackgroundMusicGameOverVolume = 0.15f
private const val LevelIntroOverlayAlpha = 0.46f
private const val SpawnIntervalSeconds = 1.5f
private const val SpriteFrameCount = 6
private const val SpriteFrameDurationNanos = 100_000_000L
private const val WaveSpeed = (2f * PI.toFloat()) / 3.5f
private const val SoundVolume = 0.6f
private const val AppleCatchSoundVolume = 0.9f
private const val AppleSpawnStartIntervalSeconds = 1.35f
private const val AppleSpawnMinIntervalSeconds = 0.8f
private const val AppleSpawnIntervalDecreasePerScore = 0.008f
private const val AppleBaseFallSpeed = 220f
private const val AppleFallSpeedPerScore = 2f
private const val AppleBasketMoveSpeed = 360f
private const val AppleBonusFrameCount = 8
private const val AppleBonusFrameDurationNanos = 100_000_000L
private const val AppleBonusSpawnDelayMillis = 7_000L
private const val AppleGoldenAppleScore = 5
private const val AppleHeartFallbackScore = 1
private const val AppleSlowMotionDurationMillis = 5_000L
private const val AppleBigBasketDurationMillis = 6_000L
private const val AppleSlowMotionMultiplier = 0.6f
private const val AppleBigBasketWidthMultiplier = 1.38f
private const val FloatingHeartEffectDurationMillis = 700L
private const val FloatingHeartRiseDistancePx = 60f

private enum class ScreenState {
    MAIN_MENU,
    BUTTERFLY_GAME,
    APPLE_GAME,
    RUNNER_GAME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUi()
        setContent {
            MyGardenTheme {
                AppRoot()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun AppRoot() {
    var screenState by rememberSaveable { mutableStateOf(ScreenState.MAIN_MENU) }
    val context = LocalContext.current
    val settingsManager = remember(context) { SettingsManager(context) }
    val settings by settingsManager.settingsFlow.collectAsState(initial = AppSettings())
    val coroutineScope = rememberCoroutineScope()

    val toggleMusic = {
        coroutineScope.launch {
            settingsManager.setMusicEnabled(!settings.musicEnabled)
        }
        Unit
    }
    val toggleSound = {
        coroutineScope.launch {
            settingsManager.setSoundEnabled(!settings.soundEnabled)
        }
        Unit
    }

    when (screenState) {
        ScreenState.MAIN_MENU -> MainMenuScreenIcons(
            onButterflyGameClick = { screenState = ScreenState.BUTTERFLY_GAME },
            onAppleGameClick = { screenState = ScreenState.APPLE_GAME },
            onRunnerGameClick = { screenState = ScreenState.RUNNER_GAME },
            musicEnabled = settings.musicEnabled,
            soundEnabled = settings.soundEnabled,
            onToggleMusic = toggleMusic,
            onToggleSound = toggleSound
        )
        ScreenState.BUTTERFLY_GAME -> GameScreen(
            onHomeClick = { screenState = ScreenState.MAIN_MENU },
            musicEnabled = settings.musicEnabled,
            soundEnabled = settings.soundEnabled,
            onToggleMusic = toggleMusic,
            onToggleSound = toggleSound
        )
        ScreenState.APPLE_GAME -> AppleGameScreen(
            onHomeClick = { screenState = ScreenState.MAIN_MENU },
            musicEnabled = settings.musicEnabled,
            soundEnabled = settings.soundEnabled,
            onToggleMusic = toggleMusic,
            onToggleSound = toggleSound
        )
        ScreenState.RUNNER_GAME -> RunnerGameScreen(
            onHomeClick = { screenState = ScreenState.MAIN_MENU },
            musicEnabled = settings.musicEnabled,
            soundEnabled = settings.soundEnabled,
            onToggleMusic = toggleMusic,
            onToggleSound = toggleSound
        )
    }
}

private data class MainMenuIconEntry(
    val iconRes: Int,
    val contentDescription: String,
    val isEnabled: Boolean,
    val onClick: (() -> Unit)?
)

@Composable
private fun MainMenuScreenIcons(
    onButterflyGameClick: () -> Unit,
    onAppleGameClick: () -> Unit,
    onRunnerGameClick: () -> Unit,
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val menuMusicPlayer = remember(context, isInPreview) {
        if (isInPreview) {
            null
        } else {
            MediaPlayer.create(context, R.raw.background_menu_music)?.apply {
                isLooping = true
                setVolume(BackgroundMusicVolume, BackgroundMusicVolume)
            }
        }
    }
    val musicEnabledState = rememberUpdatedState(musicEnabled)

    DisposableEffect(lifecycleOwner, menuMusicPlayer) {
        val player = menuMusicPlayer ?: return@DisposableEffect onDispose { }
        var isReleased = false

        fun releasePlayer() {
            if (isReleased) return
            isReleased = true
            runCatching { player.release() }
        }

        fun syncPlayback() {
            if (isReleased) return
            runCatching { player.setVolume(BackgroundMusicVolume, BackgroundMusicVolume) }
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

    LaunchedEffect(menuMusicPlayer, musicEnabled) {
        menuMusicPlayer?.runCatching {
            setVolume(BackgroundMusicVolume, BackgroundMusicVolume)
            if (musicEnabled) {
                if (!isPlaying) start()
            } else if (isPlaying) {
                pause()
            }
        }
    }
    var isSettingsVisible by remember { mutableStateOf(false) }

    val entries = listOf(
        MainMenuIconEntry(
            iconRes = R.drawable.menu_icon_butterfly_game,
            contentDescription = "Butterflies and bees",
            isEnabled = true,
            onClick = onButterflyGameClick
        ),
        MainMenuIconEntry(
            iconRes = R.drawable.menu_icon_apple_game,
            contentDescription = "Great harvest",
            isEnabled = true,
            onClick = onAppleGameClick
        ),
        MainMenuIconEntry(
            iconRes = R.drawable.menu_icon_run_game,
            contentDescription = "Funny run",
            isEnabled = true,
            onClick = onRunnerGameClick
        )
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isTabletLayout = screenWidth >= 700.dp
        val iconSize = if (isTabletLayout) screenWidth * 0.42f else screenWidth * 0.50f
        val iconSpacing = 18.dp
        val iconEndPadding = if (isTabletLayout) screenWidth * 0.06f else screenWidth * 0.04f

        Image(
            painter = painterResource(id = R.drawable.menu_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Image(
            painter = painterResource(id = R.drawable.common_button_settings),
            contentDescription = "Settings",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 16.dp)
                .size(54.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isSettingsVisible = true }
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = 60.dp)
                .padding(end = iconEndPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(iconSpacing)
        ) {
            entries.forEach { entry ->
                MainMenuIconButton(
                    entry = entry,
                    size = iconSize,
                    onClick = entry.onClick
                )
            }
        }

        if (isSettingsVisible) {
            SettingsOverlay(
                musicEnabled = musicEnabled,
                soundEnabled = soundEnabled,
                onToggleMusic = onToggleMusic,
                onToggleSound = onToggleSound,
                onResume = { isSettingsVisible = false },
                onRestart = { isSettingsVisible = false },
                onHome = { isSettingsVisible = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MainMenuIconButton(
    entry: MainMenuIconEntry,
    size: Dp,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val painter = painterResource(id = entry.iconRes)
    val iconAspectRatio = remember(painter) { painter.aspectRatioOrFallback() }

    Image(
        painter = painter,
        contentDescription = entry.contentDescription,
        modifier = modifier
            .width(size)
            .aspectRatio(iconAspectRatio)
            .clickable(
                enabled = onClick != null,
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick?.invoke()
            }
            .graphicsLayer { alpha = if (entry.isEnabled) 1f else 0.94f },
        contentScale = ContentScale.Fit
    )
}

private interface SoundManager {
    fun playButterflyClick()
    fun playBonus()
    fun playBeeHit()
    fun playAppleCatch()
    fun playAppleBonus()
    fun playAppleFail()
    fun playGameOver()
    fun release()
}

private object NoOpSoundManager : SoundManager {
    override fun playButterflyClick() = Unit
    override fun playBonus() = Unit
    override fun playBeeHit() = Unit
    override fun playAppleCatch() = Unit
    override fun playAppleBonus() = Unit
    override fun playAppleFail() = Unit
    override fun playGameOver() = Unit
    override fun release() = Unit
}

private class RealSoundManager(context: Context) : SoundManager {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val butterflyClickSoundId = soundPool.load(context, R.raw.butterfly_game_sfx_butterfly_click, 1)
    private val bonusSoundId = soundPool.load(context, R.raw.butterfly_game_sfx_bonus, 1)
    private val beeHitSoundId = soundPool.load(context, R.raw.butterfly_game_sfx_bee_hit, 1)
    private val appleCatchSoundId = soundPool.load(context, R.raw.apples_game_catch, 1)
    private val appleBonusSoundId = soundPool.load(context, R.raw.apples_game_sfx_bonus, 1)
    private val appleFailSoundId = soundPool.load(context, R.raw.apples_game_fail, 1)
    private val gameOverSoundId = soundPool.load(context, R.raw.common_sfx_game_over, 1)

    override fun playButterflyClick() {
        soundPool.play(butterflyClickSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun playBonus() {
        soundPool.play(bonusSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun playBeeHit() {
        soundPool.play(beeHitSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun playAppleCatch() {
        soundPool.play(appleCatchSoundId, AppleCatchSoundVolume, AppleCatchSoundVolume, 1, 0, 1f)
    }

    override fun playAppleBonus() {
        soundPool.play(appleBonusSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun playAppleFail() {
        soundPool.play(appleFailSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun playGameOver() {
        soundPool.play(gameOverSoundId, SoundVolume, SoundVolume, 1, 0, 1f)
    }

    override fun release() {
        soundPool.release()
    }
}

private enum class TargetType {
    BUTTERFLY_PURPLE,
    BUTTERFLY_GREEN,
    BUTTERFLY_RED,
    BEE
}

private enum class ButterflyColor {
    PURPLE,
    GREEN,
    RED
}

private sealed class LevelTask {
    data class CollectButterflies(
        val color: ButterflyColor,
        val targetCount: Int
    ) : LevelTask()

    data class ReachScore(
        val targetScore: Int
    ) : LevelTask()
}

private enum class FlightDirection {
    LEFT_TO_RIGHT,
    RIGHT_TO_LEFT
}

private data class TargetConfig(
    val spriteSheetRes: Int,
    val fallbackImageRes: Int,
    val contentDescription: String,
    val frameCount: Int,
    val scoreValue: Int,
    val speedMultiplier: Float,
    val sizeMultiplier: Float
)

private data class Target(
    val id: Long,
    val type: TargetType,
    val direction: FlightDirection,
    val velocityX: Float,
    val x: Float,
    val y: Float,
    val size: Float,
    val baseY: Float,
    val amplitude: Float,
    val phase: Float
)

private enum class EffectType {
    MAGIC_FLASH,
    BEE_HIT
}

private data class EffectConfig(
    val spriteSheetRes: Int,
    val frameCount: Int,
    val frameDurationMillis: Long,
    val sizeMultiplier: Float
)

private val effectConfigs = mapOf(
    EffectType.MAGIC_FLASH to EffectConfig(
        spriteSheetRes = R.drawable.butterfly_game_magic_flash_spritesheet,
        frameCount = 9,
        frameDurationMillis = 50L,
        sizeMultiplier = 0.7f
    ),
    EffectType.BEE_HIT to EffectConfig(
        spriteSheetRes = R.drawable.butterfly_game_bee_hit_spritesheet,
        frameCount = 8,
        frameDurationMillis = 50L,
        sizeMultiplier = 0.7f
    )
)

private data class GameEffect(
    val id: Long,
    val x: Float,
    val y: Float,
    val startTimeMillis: Long,
    val size: Float,
    val type: EffectType
)

private data class FloatingHeartEffect(
    val id: Long,
    val x: Float,
    val y: Float,
    val startTimeMillis: Long,
    val size: Float,
    val randomXOffset: Float,
    val minY: Float
)

private val targetConfigs = mapOf(
    TargetType.BUTTERFLY_PURPLE to TargetConfig(
        spriteSheetRes = R.drawable.butterfly_game_purple_spritesheet,
        fallbackImageRes = R.drawable.butterfly_game_purple_spritesheet,
        contentDescription = "Butterfly Purple",
        frameCount = 6,
        scoreValue = 1,
        speedMultiplier = 1f,
        sizeMultiplier = 1f
    ),
    TargetType.BUTTERFLY_GREEN to TargetConfig(
        spriteSheetRes = R.drawable.butterfly_game_green_spritesheet,
        fallbackImageRes = R.drawable.butterfly_game_green_spritesheet,
        contentDescription = "Butterfly Green",
        frameCount = 6,
        scoreValue = 1,
        speedMultiplier = 1f,
        sizeMultiplier = 1f
    ),
    TargetType.BUTTERFLY_RED to TargetConfig(
        spriteSheetRes = R.drawable.butterfly_game_red_spritesheet,
        fallbackImageRes = R.drawable.butterfly_game_red_spritesheet,
        contentDescription = "Butterfly Red",
        frameCount = 6,
        scoreValue = 2,
        speedMultiplier = 1.25f,
        sizeMultiplier = 0.88f
    ),
    TargetType.BEE to TargetConfig(
        spriteSheetRes = R.drawable.butterfly_game_bee_spritesheet,
        fallbackImageRes = R.drawable.butterfly_game_bee_spritesheet,
        contentDescription = "Bee",
        frameCount = 6,
        scoreValue = -1,
        speedMultiplier = 1f,
        sizeMultiplier = 1f
    )
)

private val trackedButterflyColors = listOf(
    ButterflyColor.PURPLE,
    ButterflyColor.GREEN,
    ButterflyColor.RED
)

private enum class FallingItemType {
    APPLE,
    GOLDEN_APPLE,
    HEART,
    CLOCK,
    BIG_BASKET
}

private data class FallingItem(
    val id: Long,
    val type: FallingItemType,
    val x: Float,
    val y: Float,
    val size: Float,
    val speed: Float
)

private data class AppleBonusSpriteConfig(
    val spriteSheetRes: Int,
    val fallbackImageRes: Int
)

private val appleBonusSpriteConfigs = mapOf(
    FallingItemType.GOLDEN_APPLE to AppleBonusSpriteConfig(
        spriteSheetRes = R.drawable.apples_game_bonus_golden_apple_spritesheet,
        fallbackImageRes = R.drawable.apples_game_ui_apple
    ),
    FallingItemType.HEART to AppleBonusSpriteConfig(
        spriteSheetRes = R.drawable.apples_game_bonus_heart_spritesheet,
        fallbackImageRes = R.drawable.common_ui_heart
    ),
    FallingItemType.CLOCK to AppleBonusSpriteConfig(
        spriteSheetRes = R.drawable.apples_game_bonus_time_spritesheet,
        fallbackImageRes = R.drawable.common_game_ui_star
    ),
    FallingItemType.BIG_BASKET to AppleBonusSpriteConfig(
        spriteSheetRes = R.drawable.apples_game_bonus_basket_spritesheet,
        fallbackImageRes = R.drawable.apples_game_ui_basket
    )
)

@Composable
fun GameScreen(
    onHomeClick: () -> Unit = {},
    musicEnabled: Boolean = true,
    soundEnabled: Boolean = true,
    onToggleMusic: () -> Unit = {},
    onToggleSound: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val soundManager = remember(context, isInPreview) {
        if (isInPreview) NoOpSoundManager else RealSoundManager(context)
    }
    val backgroundMusicPlayer = remember(context, isInPreview) {
        if (isInPreview) {
            null
        } else {
            MediaPlayer.create(context, R.raw.common_music_background)?.apply {
                isLooping = true
                setVolume(BackgroundMusicVolume, BackgroundMusicVolume)
            }
        }
    }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(InitialLives) }
    var isGameOver by remember { mutableStateOf(false) }
    var currentLevel by remember { mutableIntStateOf(1) }
    var currentLevelTask by remember { mutableStateOf(generateLevelTask(1)) }
    var isLevelIntroVisible by remember { mutableStateOf(true) }
    var isLevelCompleted by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var levelStartScore by remember { mutableIntStateOf(0) }
    var levelBonusPoints by remember { mutableIntStateOf(0) }
    var introStartTimeMillis by remember { mutableLongStateOf(0L) }
    val targets = remember { mutableStateListOf<Target>() }
    val effects = remember { mutableStateListOf<GameEffect>() }
    val floatingHeartEffects = remember { mutableStateListOf<FloatingHeartEffect>() }
    val collectedButterfliesByColor = remember {
        mutableStateMapOf<ButterflyColor, Int>().apply {
            trackedButterflyColors.forEach { color ->
                this[color] = 0
            }
        }
    }
    var nextTargetId by remember { mutableLongStateOf(0L) }
    var nextEffectId by remember { mutableLongStateOf(0L) }
    var nextFloatingHeartEffectId by remember { mutableLongStateOf(0L) }
    var animationTimeNanos by remember { mutableLongStateOf(0L) }
    var restartToken by remember { mutableIntStateOf(0) }
    var currentBonusTargetType by remember { mutableStateOf(randomBonusButterflyType()) }
    var bonusHudPulseToken by remember { mutableIntStateOf(0) }
    val musicEnabledState = rememberUpdatedState(musicEnabled)
    val musicVolumeState = rememberUpdatedState(
        if (isGameOver) BackgroundMusicGameOverVolume else BackgroundMusicVolume
    )
    val beginLevel: (Int, Boolean) -> Unit = { level, showIntro ->
        currentLevel = level
        currentLevelTask = generateLevelTask(level)
        isLevelIntroVisible = showIntro
        isLevelCompleted = false
        isSettingsVisible = false
        levelStartScore = score
        levelBonusPoints = 0
        introStartTimeMillis = System.currentTimeMillis()
        trackedButterflyColors.forEach { color ->
            collectedButterfliesByColor[color] = 0
        }
        nextTargetId = 0L
        nextEffectId = 0L
        nextFloatingHeartEffectId = 0L
        animationTimeNanos = 0L
        currentBonusTargetType = randomBonusButterflyType()
        bonusHudPulseToken = 0
        targets.clear()
        effects.clear()
        floatingHeartEffects.clear()
        restartToken += 1
    }
    val resetGame = {
        score = 0
        lives = InitialLives
        isGameOver = false
        isSettingsVisible = false
        beginLevel(1, true)
    }
    val isGameplayPaused = isGameOver || isLevelIntroVisible || isLevelCompleted || isSettingsVisible
    val levelScoreProgress = (score - levelStartScore).coerceAtLeast(0)

    DisposableEffect(soundManager, isInPreview) {
        onDispose {
            if (!isInPreview) {
                soundManager.release()
            }
        }
    }

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
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isReleased && player.isPlaying) {
                        runCatching { player.pause() }
                    }
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

    LaunchedEffect(isGameOver, backgroundMusicPlayer, musicEnabled) {
        backgroundMusicPlayer?.runCatching {
            val volume = if (isGameOver) BackgroundMusicGameOverVolume else BackgroundMusicVolume
            setVolume(volume, volume)
            if (musicEnabled) {
                if (!isPlaying) start()
            } else if (isPlaying) {
                pause()
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val defaultTargetSize = 72.dp
        val topSafeArea = 96.dp
        val density = LocalDensity.current
        val defaultTargetSizePx = with(density) { defaultTargetSize.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val topSafeAreaPx = with(density) { topSafeArea.toPx() }
        val minAmplitudePx = with(density) { 6.dp.toPx() }
        val maxAmplitudePx = with(density) { 12.dp.toPx() }
        val frameIndex = ((animationTimeNanos / SpriteFrameDurationNanos) % SpriteFrameCount).toInt()

        LaunchedEffect(restartToken, isGameplayPaused, screenWidthPx, screenHeightPx, topSafeAreaPx) {
            if (screenWidthPx <= defaultTargetSizePx || screenHeightPx <= defaultTargetSizePx) {
                targets.clear()
                return@LaunchedEffect
            }

            if (isGameplayPaused) return@LaunchedEffect

            var lastFrameNanos = 0L
            var spawnAccumulator = 0f

            while (true) {
                val frameTimeNanos = withFrameNanos { it }
                animationTimeNanos = frameTimeNanos

                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    continue
                }

                val deltaTimeSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameTimeNanos
                spawnAccumulator += deltaTimeSeconds

                while (spawnAccumulator >= SpawnIntervalSeconds) {
                    targets += createRandomTarget(
                        id = nextTargetId,
                        screenWidthPx = screenWidthPx,
                        screenHeightPx = screenHeightPx,
                        topSafeAreaPx = topSafeAreaPx,
                        defaultSizePx = defaultTargetSizePx,
                        minAmplitudePx = minAmplitudePx,
                        maxAmplitudePx = maxAmplitudePx
                    )
                    nextTargetId += 1
                    spawnAccumulator -= SpawnIntervalSeconds
                }

                val elapsedSeconds = frameTimeNanos / 1_000_000_000f
                val currentTimeMillis = frameTimeNanos / 1_000_000L

                for (index in targets.lastIndex downTo 0) {
                    val target = targets[index]
                    val newX = target.x + target.velocityX * deltaTimeSeconds
                    val newY = target.baseY +
                        sin(elapsedSeconds * WaveSpeed + target.phase) * target.amplitude
                    val updatedTarget = target.copy(
                        x = newX,
                        y = newY
                    )

                    val isOffScreen = when (updatedTarget.direction) {
                        FlightDirection.LEFT_TO_RIGHT -> updatedTarget.x > screenWidthPx + updatedTarget.size
                        FlightDirection.RIGHT_TO_LEFT -> updatedTarget.x < -updatedTarget.size
                    }

                    if (isOffScreen) {
                        targets.removeAt(index)
                    } else {
                        targets[index] = updatedTarget
                    }
                }

                for (index in effects.lastIndex downTo 0) {
                    val effect = effects[index]
                    val config = effectConfigs.getValue(effect.type)
                    val effectFrameIndex =
                        ((currentTimeMillis - effect.startTimeMillis) / config.frameDurationMillis).toInt()
                    if (effectFrameIndex >= config.frameCount) {
                        effects.removeAt(index)
                    }
                }

                for (index in floatingHeartEffects.lastIndex downTo 0) {
                    val effect = floatingHeartEffects[index]
                    if (currentTimeMillis - effect.startTimeMillis >= FloatingHeartEffectDurationMillis) {
                        floatingHeartEffects.removeAt(index)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
            // TODO: confirm whether this background should remain shared when apple/run games are added.
            painter = painterResource(id = R.drawable.common_background_garden),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            targets.forEach { target ->
                FlyingTarget(
                    target = target,
                    frameIndex = frameIndex,
                    isGameOver = isGameplayPaused,
                    onHit = {
                        if (isGameplayPaused) return@FlyingTarget
                        val targetIndex = targets.indexOfFirst { it.id == target.id }
                        if (targetIndex != -1) {
                            val hitTarget = targets[targetIndex]
                            val config = targetConfigs.getValue(hitTarget.type)
                            if (config.scoreValue >= 0) {
                                score += config.scoreValue
                                targetButterflyColor(hitTarget.type)?.let { color ->
                                    collectedButterfliesByColor[color] =
                                        (collectedButterfliesByColor[color] ?: 0) + 1
                                }
                                if (hitTarget.type == currentBonusTargetType) {
                                    score += 5
                                    levelBonusPoints += 5
                                    currentBonusTargetType = randomBonusButterflyType()
                                    bonusHudPulseToken += 1
                                    if (soundEnabled) {
                                        soundManager.playBonus()
                                    }
                                } else {
                                    if (soundEnabled) {
                                        soundManager.playButterflyClick()
                                    }
                                }
                                effects += createClickEffect(
                                    id = nextEffectId,
                                    target = hitTarget,
                                    effectType = EffectType.MAGIC_FLASH,
                                    startTimeMillis = animationTimeNanos / 1_000_000L
                                )
                                nextEffectId += 1
                            } else {
                                val updatedLives = (lives - 1).coerceAtLeast(0)
                                lives = updatedLives
                                if (updatedLives == 0) {
                                    isGameOver = true
                                    if (soundEnabled) {
                                        soundManager.playGameOver()
                                    }
                                } else {
                                    if (soundEnabled) {
                                        soundManager.playBeeHit()
                                    }
                                }
                                effects += createClickEffect(
                                    id = nextEffectId,
                                    target = hitTarget,
                                    effectType = EffectType.BEE_HIT,
                                    startTimeMillis = animationTimeNanos / 1_000_000L
                                )
                                nextEffectId += 1
                                floatingHeartEffects += createFloatingHeartEffect(
                                    id = nextFloatingHeartEffectId,
                                    target = hitTarget,
                                    startTimeMillis = animationTimeNanos / 1_000_000L,
                                    topBoundaryPx = topSafeAreaPx
                                )
                                nextFloatingHeartEffectId += 1
                            }
                            targets.removeAt(targetIndex)
                        }
                    }
                )
            }

            effects.forEach { effect ->
                GameEffectSprite(
                    effect = effect,
                    currentTimeMillis = animationTimeNanos / 1_000_000L
                )
            }

            floatingHeartEffects.forEach { effect ->
                FloatingHeartEffectSprite(
                    effect = effect,
                    currentTimeMillis = animationTimeNanos / 1_000_000L
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopTargetHud(
                        score = score,
                        lives = lives,
                        bonusTargetType = currentBonusTargetType,
                        frameIndex = frameIndex,
                        pulseToken = bonusHudPulseToken,
                        onHomeClick = onHomeClick,
                        onSettingsClick = { isSettingsVisible = true }
                    )
                    CompactLevelTaskHud(
                        currentLevel = currentLevel,
                        task = currentLevelTask,
                        levelScoreProgress = levelScoreProgress,
                        collectedButterfliesByColor = collectedButterfliesByColor,
                        levelBonusPoints = levelBonusPoints
                    )
                }
            }

            if (isLevelIntroVisible && !isGameOver) {
                LevelIntroOverlayAdaptive(
                    currentLevel = currentLevel,
                    task = currentLevelTask,
                    onStartLevel = {
                        isLevelIntroVisible = false
                        introStartTimeMillis = System.currentTimeMillis()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLevelCompleted && !isGameOver) {
                LevelWinOverlay(
                    currentLevel = currentLevel,
                    task = currentLevelTask,
                    score = score,
                    levelBonusPoints = levelBonusPoints,
                    onNextLevel = {
                        beginLevel(currentLevel + 1, true)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isGameOver) {
                GameOverOverlay(
                    onRestart = resetGame,
                    modifier = Modifier.fillMaxSize()
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

    LaunchedEffect(
        currentLevelTask,
        score,
        isGameOver,
        isLevelIntroVisible,
        isLevelCompleted,
        collectedButterfliesByColor.toMap()
    ) {
        if (isGameOver || isLevelIntroVisible || isLevelCompleted) return@LaunchedEffect
        if (isTaskCompleted(
                task = currentLevelTask,
                levelScoreProgress = levelScoreProgress,
                collectedButterfliesByColor = collectedButterfliesByColor
            )
        ) {
            isLevelCompleted = true
            targets.clear()
            effects.clear()
        }
    }
}

@Composable
private fun AppleGameScreen(
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
    val soundManager = remember(context, isInPreview) {
        if (isInPreview) NoOpSoundManager else RealSoundManager(context)
    }
    val backgroundMusicPlayer = remember(context, isInPreview) {
        if (isInPreview) {
            null
        } else {
            MediaPlayer.create(context, R.raw.background_apples_music)?.apply {
                isLooping = true
                setVolume(BackgroundMusicVolume, BackgroundMusicVolume)
            }
        }
    }
    var score by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(InitialLives) }
    var isGameOver by remember { mutableStateOf(false) }
    val fallingItems = remember { mutableStateListOf<FallingItem>() }
    var nextFallingItemId by remember { mutableLongStateOf(0L) }
    var basketCenterXPx by remember { mutableStateOf(Float.NaN) }
    var moveLeftPressed by remember { mutableStateOf(false) }
    var moveRightPressed by remember { mutableStateOf(false) }
    var restartToken by remember { mutableIntStateOf(0) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var slowMotionUntilTimeMillis by remember { mutableLongStateOf(0L) }
    var bigBasketUntilTimeMillis by remember { mutableLongStateOf(0L) }
    var appleGameTimeMillis by remember { mutableLongStateOf(0L) }
    var appleAnimationTimeNanos by remember { mutableLongStateOf(0L) }
    val musicEnabledState = rememberUpdatedState(musicEnabled)
    val musicVolumeState = rememberUpdatedState(
        if (isGameOver) BackgroundMusicGameOverVolume else BackgroundMusicVolume
    )

    DisposableEffect(soundManager, isInPreview) {
        onDispose {
            if (!isInPreview) {
                soundManager.release()
            }
        }
    }

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
                Lifecycle.Event.ON_PAUSE -> {
                    if (!isReleased && player.isPlaying) {
                        runCatching { player.pause() }
                    }
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

    LaunchedEffect(isGameOver, backgroundMusicPlayer, musicEnabled) {
        backgroundMusicPlayer?.runCatching {
            val volume = if (isGameOver) BackgroundMusicGameOverVolume else BackgroundMusicVolume
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
        val baseBasketWidthPx = screenWidthPx * 0.34f
        val basketBottomPaddingPx = with(density) { 92.dp.toPx() }
        val topHudReservedPx = with(density) { 132.dp.toPx() }
        val isBigBasketActive = appleGameTimeMillis < bigBasketUntilTimeMillis
        val currentBasketWidthPx = baseBasketWidthPx * if (isBigBasketActive) {
            AppleBigBasketWidthMultiplier
        } else {
            1f
        }
        val basketHeightPx = currentBasketWidthPx * (54f / 124f)
        val defaultItemSizePx = screenWidthPx * 0.065f
        val basketTopPx = screenHeightPx - basketBottomPaddingPx - basketHeightPx

        val resetGame = {
            score = 0
            lives = InitialLives
            isGameOver = false
            isSettingsVisible = false
            fallingItems.clear()
            nextFallingItemId = 0L
            basketCenterXPx = screenWidthPx / 2f
            moveLeftPressed = false
            moveRightPressed = false
            slowMotionUntilTimeMillis = 0L
            bigBasketUntilTimeMillis = 0L
            appleGameTimeMillis = 0L
            appleAnimationTimeNanos = 0L
            restartToken += 1
        }

        LaunchedEffect(screenWidthPx) {
            if (!basketCenterXPx.isFinite() && screenWidthPx > 0f) {
                basketCenterXPx = screenWidthPx / 2f
            }
        }

        LaunchedEffect(restartToken, isGameOver, isSettingsVisible, screenWidthPx, screenHeightPx) {
            if (isGameOver || isSettingsVisible || screenWidthPx <= 0f || screenHeightPx <= 0f) {
                return@LaunchedEffect
            }

            var lastFrameNanos = 0L
            var spawnAccumulator = 0f

            while (true) {
                val frameTimeNanos = withFrameNanos { it }
                appleGameTimeMillis = frameTimeNanos / 1_000_000L
                appleAnimationTimeNanos = frameTimeNanos
                if (lastFrameNanos == 0L) {
                    lastFrameNanos = frameTimeNanos
                    continue
                }

                val deltaTimeSeconds = (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                lastFrameNanos = frameTimeNanos
                val currentTimeMillis = frameTimeNanos / 1_000_000L
                val fallSpeedMultiplier = if (currentTimeMillis < slowMotionUntilTimeMillis) {
                    AppleSlowMotionMultiplier
                } else {
                    1f
                }

                val moveDirection = when {
                    moveLeftPressed && !moveRightPressed -> -1f
                    moveRightPressed && !moveLeftPressed -> 1f
                    else -> 0f
                }

                basketCenterXPx = (basketCenterXPx + moveDirection * AppleBasketMoveSpeed * deltaTimeSeconds)
                    .coerceIn(currentBasketWidthPx / 2f, screenWidthPx - currentBasketWidthPx / 2f)

                spawnAccumulator += deltaTimeSeconds
                val spawnInterval = (AppleSpawnStartIntervalSeconds - score * AppleSpawnIntervalDecreasePerScore)
                    .coerceAtLeast(AppleSpawnMinIntervalSeconds)

                while (spawnAccumulator >= spawnInterval) {
                    val itemType = randomFallingItemType(
                        elapsedGameTimeMillis = currentTimeMillis,
                        bonusSpawnDelayMillis = AppleBonusSpawnDelayMillis
                    )
                    val itemSize = defaultItemSizePx * fallingItemSizeMultiplier(itemType)
                    fallingItems += FallingItem(
                        id = nextFallingItemId,
                        type = itemType,
                        x = Random.nextFloat() * (screenWidthPx - itemSize) + itemSize / 2f,
                        y = topHudReservedPx,
                        size = itemSize,
                        speed = AppleBaseFallSpeed + score * AppleFallSpeedPerScore
                    )
                    nextFallingItemId += 1
                    spawnAccumulator -= spawnInterval
                }

                val basketLeft = basketCenterXPx - currentBasketWidthPx / 2f
                val basketRight = basketCenterXPx + currentBasketWidthPx / 2f
                val basketBottomPx = basketTopPx + basketHeightPx

                for (index in fallingItems.lastIndex downTo 0) {
                    val item = fallingItems[index]
                    val updatedItem = item.copy(y = item.y + item.speed * deltaTimeSeconds * fallSpeedMultiplier)
                    val itemBottom = updatedItem.y + updatedItem.size / 2f
                    val itemCenterX = updatedItem.x
                    val caught = itemBottom >= basketTopPx &&
                        updatedItem.y <= basketBottomPx &&
                        itemCenterX in basketLeft..basketRight

                    if (caught) {
                        when (updatedItem.type) {
                            FallingItemType.APPLE -> {
                                score += 1
                            }
                            FallingItemType.GOLDEN_APPLE -> {
                                score += AppleGoldenAppleScore
                            }
                            FallingItemType.HEART -> {
                                if (lives < InitialLives) {
                                    lives += 1
                                } else {
                                    score += AppleHeartFallbackScore
                                }
                            }
                            FallingItemType.CLOCK -> {
                                slowMotionUntilTimeMillis = currentTimeMillis + AppleSlowMotionDurationMillis
                            }
                            FallingItemType.BIG_BASKET -> {
                                bigBasketUntilTimeMillis = currentTimeMillis + AppleBigBasketDurationMillis
                                basketCenterXPx = basketCenterXPx
                                    .coerceIn(
                                        baseBasketWidthPx * AppleBigBasketWidthMultiplier / 2f,
                                        screenWidthPx - baseBasketWidthPx * AppleBigBasketWidthMultiplier / 2f
                                    )
                            }
                        }
                        if (soundEnabled) {
                            if (updatedItem.type == FallingItemType.APPLE) {
                                soundManager.playAppleCatch()
                            } else {
                                soundManager.playAppleBonus()
                            }
                        }
                        fallingItems.removeAt(index)
                        continue
                    }

                    if (updatedItem.y - updatedItem.size / 2f > screenHeightPx) {
                        fallingItems.removeAt(index)
                        if (updatedItem.type == FallingItemType.APPLE) {
                            val updatedLives = (lives - 1).coerceAtLeast(0)
                            lives = updatedLives
                            if (soundEnabled) {
                                soundManager.playAppleFail()
                            }
                            if (updatedLives == 0) {
                                isGameOver = true
                                if (soundEnabled) {
                                    soundManager.playGameOver()
                                }
                                moveLeftPressed = false
                                moveRightPressed = false
                                fallingItems.clear()
                                break
                            }
                        }
                    } else {
                        fallingItems[index] = updatedItem
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val appleBonusFrameIndex =
                ((appleAnimationTimeNanos / AppleBonusFrameDurationNanos) % AppleBonusFrameCount).toInt()
            Image(
                painter = painterResource(id = R.drawable.apples_game_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            fallingItems.forEach { item ->
                FallingItemSprite(
                    item = item,
                    bonusFrameIndex = appleBonusFrameIndex
                )
            }

            if (basketCenterXPx.isFinite()) {
                AppleBasket(
                    centerXPx = basketCenterXPx,
                    topPx = basketTopPx,
                    widthPx = currentBasketWidthPx,
                    heightPx = basketHeightPx
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AppleTopHud(
                    score = score,
                    lives = lives,
                    onHomeClick = onHomeClick,
                    onSettingsClick = { isSettingsVisible = true }
                )
                AppleControlsPanel(
                    isEnabled = !isGameOver && !isSettingsVisible,
                    isLeftPressed = moveLeftPressed,
                    isRightPressed = moveRightPressed,
                    onLeftPressedChange = { moveLeftPressed = it },
                    onRightPressedChange = { moveRightPressed = it }
                )
            }

            if (isGameOver) {
                AppleGameOverOverlay(
                    score = score,
                    onHomeClick = onHomeClick,
                    onRestartClick = resetGame,
                    modifier = Modifier.fillMaxSize()
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
private fun AppleTopHud(
    score: Int,
    lives: Int,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelPainter = painterResource(id = R.drawable.common_target_panel)
    val homePainter = painterResource(id = R.drawable.common_button_home)
    val settingsPainter = painterResource(id = R.drawable.common_button_settings)
    val scoreIconPainter = painterResource(id = R.drawable.common_game_ui_star)
    val centerApplePainter = painterResource(id = R.drawable.apples_game_ui_apple)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(panelAspectRatio),
        contentAlignment = Alignment.TopCenter
    ) {
        val heartAreaEndPadding = maxWidth * 0.18f
        val scoreStartPadding = maxWidth * 0.19f
        val livesBaselinePadding = maxHeight * 0.35f
        val scoreIconSize = maxHeight * 0.38f
        val scoreIconTextGap = maxWidth * 0.035f
        val scoreTextEndPadding = maxWidth * 0.025f
        val heartSize = maxHeight * 0.32f
        val heartSpacing = maxWidth * 0.014f
        val centerAppleSize = maxHeight * 0.44f
        val centerAppleVerticalOffset = maxHeight * 0.22f
        val scoreFontSize = (maxHeight.value * 0.3f).sp
        val iconButtonWidth = maxWidth * 0.12f
        val iconButtonHeight = maxHeight * 0.52f

        Image(
            painter = panelPainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.18f)
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
                    .align(Alignment.Center)
                    .size(width = iconButtonWidth, height = iconButtonHeight)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = scoreStartPadding)
                .fillMaxWidth(0.22f)
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
                    .padding(start = scoreIconSize + scoreIconTextGap, end = scoreTextEndPadding),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = scoreFontSize),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3A2D1F),
                textAlign = TextAlign.End
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = heartAreaEndPadding, top = livesBaselinePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(heartSpacing)
        ) {
            repeat(InitialLives) { index ->
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
            painter = centerApplePainter,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = centerAppleVerticalOffset)
                .size(centerAppleSize)
        )

        Image(
            painter = settingsPainter,
            contentDescription = "Settings",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = maxHeight * 0.24f, end = maxWidth * 0.02f)
                .size(width = iconButtonWidth, height = iconButtonHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSettingsClick
                )
        )
    }
}

@Composable
private fun AppleControlsPanel(
    isEnabled: Boolean,
    isLeftPressed: Boolean,
    isRightPressed: Boolean,
    onLeftPressedChange: (Boolean) -> Unit,
    onRightPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MovementControlButton(
            iconRes = R.drawable.apples_game_ui_arrow_left,
            contentDescription = "Left",
            isEnabled = isEnabled,
            isPressed = isLeftPressed,
            onPressedChange = onLeftPressedChange,
            modifier = Modifier.weight(1f)
        )
        MovementControlButton(
            iconRes = R.drawable.apples_game_ui_arrow_right,
            contentDescription = "Right",
            isEnabled = isEnabled,
            isPressed = isRightPressed,
            onPressedChange = onRightPressedChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MovementControlButton(
    iconRes: Int,
    contentDescription: String,
    isEnabled: Boolean,
    isPressed: Boolean,
    onPressedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val painter = painterResource(id = iconRes)

    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(isEnabled) {
                if (!isEnabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onPressedChange(true)
                    var keepPressed = true
                    while (keepPressed) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUpIgnoreConsumed()) {
                            keepPressed = false
                        }
                    }
                    onPressedChange(false)
                }
            }
            .clickable(
                enabled = false,
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .graphicsLayer {
                alpha = if (isEnabled) 1f else 0.55f
                scaleX = if (isPressed) 0.94f else 1f
                scaleY = if (isPressed) 0.94f else 1f
            }
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun FallingItemSprite(
    item: FallingItem,
    bonusFrameIndex: Int,
    modifier: Modifier = Modifier
) {
    val itemSizeDp = with(LocalDensity.current) { item.size.toDp() }
    val itemModifier = modifier
        .size(itemSizeDp)
        .offset {
            IntOffset(
                x = (item.x - item.size / 2f).roundToInt(),
                y = (item.y - item.size / 2f).roundToInt()
            )
        }

    if (item.type == FallingItemType.APPLE) {
        Image(
            painter = painterResource(id = R.drawable.apples_game_ui_apple),
            contentDescription = item.type.name,
            contentScale = ContentScale.Fit,
            modifier = itemModifier
        )
        return
    }

    val config = appleBonusSpriteConfigs.getValue(item.type)
    AnimatedSprite(
        modifier = itemModifier,
        targetSize = itemSizeDp,
        frameIndex = bonusFrameIndex,
        frameCount = AppleBonusFrameCount,
        spriteSheetRes = config.spriteSheetRes,
        fallbackImageRes = config.fallbackImageRes,
        contentDescription = item.type.name,
        mirrored = false
    )
}

private fun randomFallingItemType(
    elapsedGameTimeMillis: Long,
    bonusSpawnDelayMillis: Long
): FallingItemType {
    if (elapsedGameTimeMillis < bonusSpawnDelayMillis) {
        return FallingItemType.APPLE
    }

    val roll = Random.nextFloat()
    return when {
        roll < 0.85f -> FallingItemType.APPLE
        roll < 0.90f -> FallingItemType.GOLDEN_APPLE
        roll < 0.94f -> FallingItemType.HEART
        roll < 0.97f -> FallingItemType.CLOCK
        else -> FallingItemType.BIG_BASKET
    }
}

private fun fallingItemSizeMultiplier(type: FallingItemType): Float {
    return when (type) {
        FallingItemType.APPLE -> 1f
        FallingItemType.GOLDEN_APPLE -> 1.08f
        FallingItemType.HEART -> 1.1f
        FallingItemType.CLOCK -> 1.12f
        FallingItemType.BIG_BASKET -> 1.14f
    }
}

@Composable
private fun AppleBasket(
    centerXPx: Float,
    topPx: Float,
    widthPx: Float,
    heightPx: Float,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.apples_game_ui_basket),
        contentDescription = "Basket",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(
                width = with(LocalDensity.current) { widthPx.toDp() },
                height = with(LocalDensity.current) { heightPx.toDp() }
            )
            .offset {
                IntOffset(
                    x = (centerXPx - widthPx / 2f).roundToInt(),
                    y = topPx.roundToInt()
                )
            }
    )
}

@Composable
private fun AppleGameOverOverlay(
    score: Int,
    onHomeClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelPainter = painterResource(id = R.drawable.apples_game_over_panel)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
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

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawOutlinedText(
                    text = score.toString(),
                    centerX = size.width / 2f,
                    baselineY = size.height * 0.47f,
                    textSizePx = size.height * 0.118f,
                    fillColor = android.graphics.Color.parseColor("#C94D3A"),
                    strokeColor = android.graphics.Color.WHITE
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.64f)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppleOverlayButton(
                    label = "В главное меню",
                    onClick = onHomeClick
                )
                AppleOverlayButton(
                    label = "Начать сначала",
                    onClick = onRestartClick
                )
            }
        }
    }
}

@Composable
private fun AppleOverlayButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val painterRes = if (label.contains("глав")) {
        R.drawable.button_menu
    } else {
        R.drawable.common_button_repeat
    }
    val painter = painterResource(id = painterRes)
    val aspectRatio = remember(painter) { painter.aspectRatioOrFallback() }

    Image(
        painter = painter,
        contentDescription = label,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun LevelIntroOverlayAdaptive(
    currentLevel: Int,
    task: LevelTask,
    onStartLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: confirm these intro overlay assets are truly common across all mini-games.
    val panelPainter = painterResource(id = R.drawable.common_level_start_panel)
    val okPainter = painterResource(id = R.drawable.common_button_ok)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = LevelIntroOverlayAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(panelAspectRatio)
        ) {
            val titleTopPadding = maxHeight * 0.07f
            val titleFontSize = (maxHeight.value * 0.14f).sp
            val contentHorizontalPadding = maxWidth * 0.11f
            val contentTopPadding = maxHeight * 0.09f
            val contentBottomPadding = maxHeight * 0.16f
            val taskIconSize = maxHeight * 0.26f
            val taskSpacing = maxWidth * 0.025f
            val taskFontSize = (maxHeight.value * 0.11f).sp
            val buttonBottomPadding = maxHeight * 0.08f
            val buttonWidthFraction = 0.32f

            Image(
                painter = panelPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Box(modifier = Modifier.matchParentSize()) {
                Text(
                    text = "Уровень $currentLevel",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                    color = Color(0xFF7A4A10),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = titleTopPadding)
                )
                task.iconRes()?.let { iconRes ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(
                                start = contentHorizontalPadding,
                                end = contentHorizontalPadding,
                                top = contentTopPadding,
                                bottom = contentBottomPadding
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(taskSpacing)
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(taskIconSize)
                        )
                        Text(
                            text = task.title(),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = taskFontSize),
                            color = Color(0xFF4B2B07),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            lineHeight = taskFontSize * 1.05,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } ?: Text(
                    text = task.title(),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = taskFontSize),
                    color = Color(0xFF4B2B07),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = taskFontSize * 1.05,
                    maxLines = 2,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(
                            start = contentHorizontalPadding,
                            end = contentHorizontalPadding,
                            top = contentTopPadding,
                            bottom = contentBottomPadding
                        )
                )
                Image(
                    painter = okPainter,
                    contentDescription = "Start level",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = buttonBottomPadding)
                        .fillMaxWidth(buttonWidthFraction)
                        .aspectRatio(okAspectRatio)
                        .clickable(onClick = onStartLevel)
                )
            }
        }
    }
}

@Composable
private fun LevelIntroOverlayFramed(
    currentLevel: Int,
    task: LevelTask,
    onStartLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: confirm these intro overlay assets are truly common across all mini-games.
    val panelPainter = painterResource(id = R.drawable.common_level_start_panel)
    val okPainter = painterResource(id = R.drawable.common_button_ok)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = LevelIntroOverlayAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .aspectRatio(panelAspectRatio)
        ) {
            val titleTopPadding = maxHeight * 0.06f
            val titleFontSize = (maxHeight.value * 0.10f).sp
            val contentHorizontalPadding = maxWidth * 0.11f
            val contentTopPadding = maxHeight * 0.09f
            val contentBottomPadding = maxHeight * 0.16f
            val taskIconSize = maxHeight * 0.16f
            val taskSpacing = maxWidth * 0.025f
            val taskFontSize = (maxHeight.value * 0.072f).sp
            val buttonBottomPadding = maxHeight * 0.06f
            val buttonWidthFraction = 0.32f

            Image(
                painter = panelPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Box(modifier = Modifier.matchParentSize()) {
                Text(
                    text = "Уровень $currentLevel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF7A4A10),
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                )
                task.iconRes()?.let { iconRes ->
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = task.title(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF4B2B07),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            lineHeight = MaterialTheme.typography.titleMedium.fontSize * 1.05,
                            maxLines = 2,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } ?: Text(
                    text = task.title(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4B2B07),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.titleMedium.fontSize * 1.05,
                    maxLines = 2,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 42.dp, end = 42.dp, top = 18.dp, bottom = 32.dp)
                )
                Image(
                    painter = okPainter,
                    contentDescription = "Start level",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 13.dp)
                        .fillMaxWidth(0.32f)
                        .aspectRatio(okAspectRatio)
                        .clickable(onClick = onStartLevel)
                )
            }
        }
    }
}

@Composable
private fun LevelIntroOverlayCompact(
    currentLevel: Int,
    task: LevelTask,
    onStartLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: confirm these intro overlay assets are truly common across all mini-games.
    val panelPainter = painterResource(id = R.drawable.common_level_start_panel)
    val okPainter = painterResource(id = R.drawable.common_button_ok)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = LevelIntroOverlayAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(panelAspectRatio)
        ) {
            Image(
                painter = panelPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 28.dp, end = 28.dp, top = 10.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Уровень $currentLevel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF7A4A10),
                    fontWeight = FontWeight.ExtraBold
                )
                task.iconRes()?.let { iconRes ->
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = task.title(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4B2B07),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = okPainter,
                    contentDescription = "Start level",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .aspectRatio(okAspectRatio)
                        .clickable(onClick = onStartLevel)
                )
            }
        }
    }
}

@Composable
private fun LevelIntroOverlayPanel(
    currentLevel: Int,
    task: LevelTask,
    onStartLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: confirm these intro overlay assets are truly common across all mini-games.
    val panelPainter = painterResource(id = R.drawable.common_level_start_panel)
    val okPainter = painterResource(id = R.drawable.common_button_ok)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = LevelIntroOverlayAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .aspectRatio(panelAspectRatio)
        ) {
            Image(
                painter = panelPainter,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = 28.dp, end = 28.dp, top = 10.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Уровень $currentLevel",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF7A4A10),
                    fontWeight = FontWeight.ExtraBold
                )
                task.iconRes()?.let { iconRes ->
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp)
                    )
                }
                Text(
                    text = task.title(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF4B2B07),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Нажми, когда будешь готов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF775217),
                    textAlign = TextAlign.Center
                )
                Image(
                    painter = okPainter,
                    contentDescription = "Start level",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .aspectRatio(okAspectRatio)
                        .clickable(onClick = onStartLevel)
                )
            }
        }
    }
}

@Composable
private fun CompactLevelTaskHud(
    currentLevel: Int,
    task: LevelTask,
    levelScoreProgress: Int,
    collectedButterfliesByColor: Map<ButterflyColor, Int>,
    levelBonusPoints: Int,
    modifier: Modifier = Modifier
) {
    val progressText = task.progressText(levelScoreProgress, collectedButterfliesByColor)
    Box(
        modifier = modifier
            .fillMaxWidth(0.8f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
    ) {
        Image(
            // TODO: confirm this level HUD panel is shared across future mini-games.
            painter = painterResource(id = R.drawable.common_level_panel),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Уровень $currentLevel",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF734B11),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "|",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99734B11),
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                task.iconRes()?.let { iconRes ->
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = task.title(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4D2D08),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "|",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0x99734B11),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B4A17),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MyGardenTheme {
        GameScreen()
    }
}

@Preview(showBackground = true)
@Composable
private fun AppleGameScreenPreview() {
    MyGardenTheme {
        AppleGameScreen(
            onHomeClick = {},
            musicEnabled = true,
            soundEnabled = true,
            onToggleMusic = {},
            onToggleSound = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainMenuScreenPreview() {
    MyGardenTheme {
        MainMenuScreenIcons(
            onButterflyGameClick = {},
            onAppleGameClick = {},
            onRunnerGameClick = {},
            musicEnabled = true,
            soundEnabled = true,
            onToggleMusic = {},
            onToggleSound = {}
        )
    }
}

@Composable
private fun ScoreHud(score: Int) {
    ScoreHud(score = score, modifier = Modifier)
}

@Composable
private fun ScoreHud(
    score: Int,
    modifier: Modifier
) {
    Box(
        modifier = modifier.size(width = 108.dp, height = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.common_target_panel),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3A2D1F)
            )
        }
    }
}

@Composable
private fun TopTargetHud(
    score: Int,
    lives: Int,
    bonusTargetType: TargetType,
    frameIndex: Int,
    pulseToken: Int,
    onHomeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = targetConfigs.getValue(bonusTargetType)
    val pulseScale = remember { Animatable(1f) }
    // TODO: confirm this target HUD panel is shared across future mini-games.
    val panelPainter = painterResource(id = R.drawable.common_target_panel)
    val homePainter = painterResource(id = R.drawable.common_button_home)
    val settingsPainter = painterResource(id = R.drawable.common_button_settings)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }

    LaunchedEffect(pulseToken) {
        if (pulseToken == 0) return@LaunchedEffect
        pulseScale.snapTo(1f)
        pulseScale.animateTo(1.15f, animationSpec = tween(durationMillis = 120))
        pulseScale.animateTo(1f, animationSpec = tween(durationMillis = 140))
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth(1.00f)
            .aspectRatio(panelAspectRatio),
        contentAlignment = Alignment.TopCenter
    ) {
        val heartAreaEndPadding = maxWidth * 0.18f
        val scoreStartPadding = maxWidth * 0.19f
        val livesBaselinePadding = maxHeight * 0.35f
        val butterflyVerticalOffset = maxHeight * 0.22f
        val scoreIconSize = maxHeight * 0.38f
        val scoreIconTextGap = maxWidth * 0.035f
        val scoreTextEndPadding = maxWidth * 0.025f
        val heartSize = maxHeight * 0.32f
        val heartSpacing = maxWidth * 0.014f
        val bonusButterflySize = maxHeight * 0.5f
        val scoreFontSize = (maxHeight.value * 0.3f).sp
        val iconButtonWidth = maxWidth * 0.12f
        val iconButtonHeight = maxHeight * 0.52f

        Image(
            painter = panelPainter,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.18f)
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
                    .align(Alignment.Center)
                    .size(width = iconButtonWidth, height = iconButtonHeight)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = scoreStartPadding)
                .fillMaxWidth(0.22f)
        ) {
            Image(
            painter = painterResource(id = R.drawable.butterfly_game_ui_score_icon),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(scoreIconSize),
                contentScale = ContentScale.Fit
            )
            Text(
                text = score.toString(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = scoreIconSize + scoreIconTextGap, end = scoreTextEndPadding),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = scoreFontSize),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3A2D1F),
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = heartAreaEndPadding, top = livesBaselinePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(heartSpacing)
        ) {
            repeat(InitialLives) { index ->
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
                .padding(top = maxHeight * 0.24f, end = maxWidth * 0.02f)
                .size(width = iconButtonWidth, height = iconButtonHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSettingsClick
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = butterflyVerticalOffset)
                .graphicsLayer {
                    scaleX = pulseScale.value
                    scaleY = pulseScale.value
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedSprite(
                modifier = Modifier.size(bonusButterflySize),
                targetSize = bonusButterflySize,
                frameIndex = frameIndex,
                frameCount = config.frameCount,
                spriteSheetRes = config.spriteSheetRes,
                fallbackImageRes = config.fallbackImageRes,
                contentDescription = "Bonus Butterfly",
                mirrored = false
            )
        }
    }
}

@Composable
private fun LivesHud(lives: Int) {
    LivesHud(lives = lives, modifier = Modifier)
}

@Composable
private fun LivesHud(
    lives: Int,
    modifier: Modifier
) {
    Box(
        modifier = modifier.size(width = 108.dp, height = 44.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.common_target_panel),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 8.dp, end = 8.dp, top = 5.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(InitialLives) { index ->
                Image(
                    painter = painterResource(
                        id = if (index < lives.coerceAtLeast(0)) {
                            R.drawable.common_ui_heart
                        } else {
                            R.drawable.common_ui_heart_disable
                        }
                    ),
                    contentDescription = "Life ${index + 1}",
                    modifier = Modifier.size(22.dp)
                )
                if (index < InitialLives - 1) {
                    Spacer(modifier = Modifier.size(4.dp))
                }
            }
        }
    }
}

@Composable
private fun LevelTaskHud(
    currentLevel: Int,
    task: LevelTask,
    levelScoreProgress: Int,
    collectedButterfliesByColor: Map<ButterflyColor, Int>,
    levelBonusPoints: Int,
    modifier: Modifier = Modifier
) {
    val progressText = task.progressText(levelScoreProgress, collectedButterfliesByColor)
    Box(
        modifier = modifier
            .fillMaxWidth(0.78f)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(Color(0xD9FFF3D1))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Уровень $currentLevel",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF734B11),
                fontWeight = FontWeight.ExtraBold
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.iconRes()?.let { iconRes ->
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                Text(
                    text = task.title(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4D2D08),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = task.progressText(levelScoreProgress, collectedButterfliesByColor),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF6B4A17),
                fontWeight = FontWeight.SemiBold
            )
            if (levelBonusPoints > 0) {
                Text(
                    text = "Бонусные очки: +$levelBonusPoints",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2B7F37),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LevelIntroOverlay(
    currentLevel: Int,
    task: LevelTask,
    onStartLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val okPainter = painterResource(id = R.drawable.common_icon_ok)
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = LevelIntroOverlayAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFF7D8),
                            Color(0xFFFFE6A6)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Уровень $currentLevel",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF7A4A10),
                fontWeight = FontWeight.ExtraBold
            )
            task.iconRes()?.let { iconRes ->
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(54.dp)
                )
            }
            Text(
                text = task.title(),
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF4B2B07),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Нажми, когда будешь готов",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF775217),
                textAlign = TextAlign.Center
            )
            Image(
                painter = okPainter,
                contentDescription = "Start level",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.36f)
                    .aspectRatio(okAspectRatio)
                    .clickable(onClick = onStartLevel)
            )
        }
    }
}

@Composable
private fun LevelWinOverlay(
    currentLevel: Int,
    task: LevelTask,
    score: Int,
    levelBonusPoints: Int,
    onNextLevel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelPainter = painterResource(id = R.drawable.butterfly_game_win_panel)
    val nextButtonPainter = painterResource(id = R.drawable.butterfly_game_button_next)
    val okPainter = painterResource(id = R.drawable.common_icon_ok)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val nextButtonAspectRatio = remember(nextButtonPainter) { nextButtonPainter.aspectRatioOrFallback() }
    val okAspectRatio = remember(okPainter) { okPainter.aspectRatioOrFallback() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(panelAspectRatio)
        ) {
            val buttonWidthFraction = 0.48f
            val buttonHeight = maxWidth * buttonWidthFraction / nextButtonAspectRatio
            val buttonCenterY = maxHeight * 0.90f

            Image(
                painter = panelPainter,
                contentDescription = "Level complete",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            WinPanelStats(
                score = score,
                levelBonusPoints = levelBonusPoints,
                modifier = Modifier.fillMaxSize()
            )
            WinPanelTaskDecorations(
                task = task,
                okPainter = okPainter,
                okAspectRatio = okAspectRatio,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = nextButtonPainter,
                contentDescription = "Next level",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(buttonWidthFraction)
                    .aspectRatio(nextButtonAspectRatio)
                    .offset(y = buttonCenterY - buttonHeight / 2f)
                    .clickable(onClick = onNextLevel)
            )
        }
    }
}

@Composable
private fun WinPanelStats(
    score: Int,
    levelBonusPoints: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val scoreText = score.toString()
        val bonusText = "+$levelBonusPoints"

        drawOutlinedText(
            text = scoreText,
            centerX = size.width / 2f,
            baselineY = size.height * 0.47f,
            textSizePx = size.height * 0.1f,
            fillColor = android.graphics.Color.parseColor("#8F5BFF"),
            strokeColor = android.graphics.Color.WHITE
        )

        drawOutlinedText(
            text = bonusText,
            centerX = size.width * 0.74f,
            baselineY = size.height * 0.57f,
            textSizePx = size.height * 0.03f,
            fillColor = android.graphics.Color.parseColor("#58B947"),
            strokeColor = android.graphics.Color.WHITE
        )
    }
}

@Composable
private fun WinPanelTaskDecorations(
    task: LevelTask,
    okPainter: Painter,
    okAspectRatio: Float,
    modifier: Modifier = Modifier
) {
    val iconResList = completedTaskIcons(task)

    BoxWithConstraints(modifier = modifier) {
        val iconSize = maxWidth * 0.12f
        val okWidth = maxWidth * 0.085f

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = maxHeight * 0.675f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                iconResList.forEach { iconRes ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Image(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize)
                        )
                        Image(
                            painter = okPainter,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(width = okWidth, height = okWidth / okAspectRatio)
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawOutlinedText(
    text: String,
    centerX: Float,
    baselineY: Float,
    textSizePx: Float,
    fillColor: Int,
    strokeColor: Int
) {
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = strokeColor
        style = Paint.Style.STROKE
        strokeWidth = textSizePx * 0.14f
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    drawIntoCanvas { canvas ->
        val nativeCanvas = canvas.nativeCanvas
        nativeCanvas.drawText(text, centerX, baselineY, strokePaint)
        nativeCanvas.drawText(text, centerX, baselineY, fillPaint)
    }
}

@Composable
private fun GameOverOverlay(
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    // TODO: confirm this game over panel is shared across future mini-games.
    val panelPainter = painterResource(id = R.drawable.common_game_over_panel)
    // TODO: confirm this repeat button is shared across future mini-games.
    val restartButtonPainter = painterResource(id = R.drawable.common_button_repeat)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }
    val restartButtonAspectRatio = remember(restartButtonPainter) {
        restartButtonPainter.aspectRatioOrFallback()
    }
    val interactionSource = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val pressedButtonScale = 0.95f
    val pressedButtonAlpha = 0.94f
    val pressedButtonTranslationY = with(density) { 3.dp.toPx() }
    var isRestartPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(panelAspectRatio)
                .pointerInput(onRestart) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val baseRestartButtonRect = calculateRestartButtonRect(
                            panelWidth = size.width.toFloat(),
                            panelHeight = size.height.toFloat(),
                            buttonAspectRatio = restartButtonAspectRatio
                        )

                        if (!baseRestartButtonRect.contains(down.position)) {
                            return@awaitEachGesture
                        }

                        isRestartPressed = true
                        var shouldRestart = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val isInsideButton = baseRestartButtonRect.contains(change.position)

                            if (change.changedToUpIgnoreConsumed()) {
                                shouldRestart = isInsideButton
                                break
                            }

                            isRestartPressed = isInsideButton
                        }

                        isRestartPressed = false
                        if (shouldRestart) {
                            onRestart()
                        }
                    }
                }
        ) {
            val restartButtonWidthFraction = 0.62f
            val restartButtonHeight = maxWidth * restartButtonWidthFraction / restartButtonAspectRatio
            val restartButtonCenterY = maxHeight * 0.80f

            Image(
                painter = panelPainter,
                contentDescription = "Game Over",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
            Image(
                painter = restartButtonPainter,
                contentDescription = "Restart",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(restartButtonWidthFraction)
                    .aspectRatio(restartButtonAspectRatio)
                    .offset(y = restartButtonCenterY - restartButtonHeight / 2f)
                    .graphicsLayer {
                        scaleX = if (isRestartPressed) pressedButtonScale else 1f
                        scaleY = if (isRestartPressed) pressedButtonScale else 1f
                        translationY = if (isRestartPressed) pressedButtonTranslationY else 0f
                        alpha = if (isRestartPressed) pressedButtonAlpha else 1f
                    }
            )
        }
    }
}

private fun calculateRestartButtonRect(
    panelWidth: Float,
    panelHeight: Float,
    buttonAspectRatio: Float
): Rect {
    val buttonWidth = panelWidth * 0.62f
    val buttonHeight = buttonWidth / buttonAspectRatio
    val buttonLeft = (panelWidth - buttonWidth) / 2f
    val buttonTop = panelHeight * 0.80f - buttonHeight / 2f
    return Rect(
        left = buttonLeft,
        top = buttonTop,
        right = buttonLeft + buttonWidth,
        bottom = buttonTop + buttonHeight
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

@Composable
private fun FlyingTarget(
    target: Target,
    frameIndex: Int,
    isGameOver: Boolean,
    onHit: () -> Unit
) {
    val targetSizeDp = with(LocalDensity.current) { target.size.toDp() }
    val config = targetConfigs.getValue(target.type)

    val movingModifier = Modifier
        .offset {
            IntOffset(
                x = target.x.roundToInt(),
                y = target.y.roundToInt()
            )
        }
        .size(targetSizeDp)
        .clickable(enabled = !isGameOver, onClick = onHit)

    AnimatedSprite(
        modifier = movingModifier,
        targetSize = targetSizeDp,
        frameIndex = frameIndex,
        frameCount = config.frameCount,
        spriteSheetRes = config.spriteSheetRes,
        fallbackImageRes = config.fallbackImageRes,
        contentDescription = config.contentDescription,
        mirrored = target.direction == FlightDirection.RIGHT_TO_LEFT
    )
}

@Composable
private fun GameEffectSprite(
    effect: GameEffect,
    currentTimeMillis: Long
) {
    val config = effectConfigs.getValue(effect.type)
    val spriteSheet = rememberSpriteSheet(config.spriteSheetRes)
    val frameIndex = ((currentTimeMillis - effect.startTimeMillis) / config.frameDurationMillis).toInt()
    if (frameIndex !in 0 until config.frameCount) return

    val effectSizeDp = with(LocalDensity.current) { effect.size.toDp() }
    val modifier = Modifier
        .offset {
            IntOffset(
                x = effect.x.roundToInt(),
                y = effect.y.roundToInt()
            )
        }
        .size(effectSizeDp)

    if (spriteSheet != null) {
        Canvas(modifier = modifier) {
            drawEffectSpriteFrame(
                spriteSheet = spriteSheet,
                frameCount = config.frameCount,
                frameIndex = frameIndex,
                position = Offset.Zero,
                size = size
            )
        }
    }
}

@Composable
private fun FloatingHeartEffectSprite(
    effect: FloatingHeartEffect,
    currentTimeMillis: Long
) {
    val elapsedMillis = currentTimeMillis - effect.startTimeMillis
    if (elapsedMillis !in 0..FloatingHeartEffectDurationMillis) return

    val progress = (elapsedMillis.toFloat() / FloatingHeartEffectDurationMillis).coerceIn(0f, 1f)
    val alpha = 1f - progress
    val scale = 1f + 0.15f * progress
    val x = effect.x + effect.randomXOffset * progress
    val y = (effect.y - FloatingHeartRiseDistancePx * progress).coerceAtLeast(effect.minY)
    val effectSizeDp = with(LocalDensity.current) { effect.size.toDp() }

    Image(
        painter = painterResource(id = R.drawable.common_ui_heart_broken),
        contentDescription = null,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = x.roundToInt(),
                    y = y.roundToInt()
                )
            }
            .size(effectSizeDp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun AnimatedSprite(
    modifier: Modifier = Modifier,
    targetSize: Dp,
    frameIndex: Int,
    frameCount: Int,
    spriteSheetRes: Int,
    fallbackImageRes: Int,
    contentDescription: String,
    mirrored: Boolean
) {
    val spriteSheet = rememberSpriteSheet(spriteSheetRes)

    if (spriteSheet != null) {
        Canvas(modifier = modifier.size(targetSize)) {
            drawSpriteFrame(
                spriteSheet = spriteSheet,
                frameCount = frameCount,
                frameIndex = frameIndex,
                position = Offset.Zero,
                size = size,
                mirrored = mirrored
            )
        }
    } else {
        Image(
            painter = painterResource(id = fallbackImageRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = modifier
                .size(targetSize)
                .graphicsLayer {
                    scaleX = if (mirrored) -1f else 1f
                }
        )
    }
}

@Composable
private fun rememberSpriteSheet(drawableRes: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(drawableRes) {
        runCatching {
            BitmapFactory.decodeResource(context.resources, drawableRes)?.asImageBitmap()
        }.getOrNull()
    }
}

private fun DrawScope.drawSpriteFrame(
    spriteSheet: ImageBitmap,
    frameCount: Int,
    frameIndex: Int,
    position: Offset,
    size: Size,
    mirrored: Boolean
) {
    if (frameCount <= 0) return

    val frameWidth = spriteSheet.width / frameCount
    val frameHeight = spriteSheet.height
    val safeFrameIndex = frameIndex.coerceIn(0, frameCount - 1)

    if (mirrored) {
        withTransform({
            scale(
                scaleX = -1f,
                scaleY = 1f,
                pivot = Offset(position.x + size.width / 2f, position.y + size.height / 2f)
            )
        }) {
            drawImage(
                image = spriteSheet,
                srcOffset = IntOffset(x = safeFrameIndex * frameWidth, y = 0),
                srcSize = IntSize(width = frameWidth, height = frameHeight),
                dstOffset = IntOffset(position.x.roundToInt(), position.y.roundToInt()),
                dstSize = IntSize(width = size.width.toInt(), height = size.height.toInt())
            )
        }
    } else {
        drawImage(
            image = spriteSheet,
            srcOffset = IntOffset(x = safeFrameIndex * frameWidth, y = 0),
            srcSize = IntSize(width = frameWidth, height = frameHeight),
            dstOffset = IntOffset(position.x.roundToInt(), position.y.roundToInt()),
            dstSize = IntSize(width = size.width.toInt(), height = size.height.toInt())
        )
    }
}

private fun DrawScope.drawEffectSpriteFrame(
    spriteSheet: ImageBitmap,
    frameCount: Int,
    frameIndex: Int,
    position: Offset,
    size: Size
) {
    if (frameCount <= 0) return

    val frameWidth = spriteSheet.width / frameCount
    val frameHeight = spriteSheet.height
    val safeFrameIndex = frameIndex.coerceIn(0, frameCount - 1)

    drawImage(
        image = spriteSheet,
        srcOffset = IntOffset(x = safeFrameIndex * frameWidth, y = 0),
        srcSize = IntSize(width = frameWidth, height = frameHeight),
        dstOffset = IntOffset(position.x.roundToInt(), position.y.roundToInt()),
        dstSize = IntSize(width = size.width.toInt(), height = size.height.toInt())
    )
}

private fun createRandomTarget(
    id: Long,
    screenWidthPx: Float,
    screenHeightPx: Float,
    topSafeAreaPx: Float,
    defaultSizePx: Float,
    minAmplitudePx: Float,
    maxAmplitudePx: Float
): Target {
    val typeRoll = Random.nextFloat()
    val type = when {
        typeRoll < 0.3f -> TargetType.BUTTERFLY_PURPLE
        typeRoll < 0.5f -> TargetType.BUTTERFLY_GREEN
        typeRoll < 0.6f -> TargetType.BUTTERFLY_RED
        else -> TargetType.BEE
    }
    val config = targetConfigs.getValue(type)
    val objectSize = defaultSizePx * config.sizeMultiplier
    val speed = Random.nextInt(180, 281).toFloat() * config.speedMultiplier
    val direction = if (Random.nextBoolean()) {
        FlightDirection.LEFT_TO_RIGHT
    } else {
        FlightDirection.RIGHT_TO_LEFT
    }
    val velocityX = if (direction == FlightDirection.LEFT_TO_RIGHT) speed else -speed
    val maxY = (screenHeightPx - objectSize).coerceAtLeast(topSafeAreaPx)
    val baseY = topSafeAreaPx + Random.nextFloat() * (maxY - topSafeAreaPx).coerceAtLeast(0f)

    return Target(
        id = id,
        type = type,
        direction = direction,
        velocityX = velocityX,
        x = if (direction == FlightDirection.LEFT_TO_RIGHT) -objectSize else screenWidthPx + objectSize,
        y = baseY,
        size = objectSize,
        baseY = baseY,
        amplitude = Random.nextFloat() * (maxAmplitudePx - minAmplitudePx) + minAmplitudePx,
        phase = Random.nextFloat() * 2f * PI.toFloat()
    )
}

private fun randomBonusButterflyType(): TargetType {
    return when (Random.nextInt(3)) {
        0 -> TargetType.BUTTERFLY_PURPLE
        1 -> TargetType.BUTTERFLY_GREEN
        else -> TargetType.BUTTERFLY_RED
    }
}

private fun generateLevelTask(level: Int): LevelTask {
    val shouldCollectButterflies = Random.nextBoolean()
    return if (shouldCollectButterflies) {
        val minCount = if (level <= 2) 3 else 4 + level / 2
        val maxCount = if (level <= 3) 5 else (minCount + 3).coerceAtMost(10)
        LevelTask.CollectButterflies(
            color = ButterflyColor.entries.random(),
            targetCount = Random.nextInt(minCount, maxCount + 1)
        )
    } else {
        val minScore = if (level <= 2) 25 else 25 + (level - 1) * 7
        val maxScore = if (level <= 2) 40 else minScore + 15
        LevelTask.ReachScore(
            targetScore = Random.nextInt(minScore, maxScore + 1)
        )
    }
}

private fun targetButterflyColor(type: TargetType): ButterflyColor? {
    return when (type) {
        TargetType.BUTTERFLY_PURPLE -> ButterflyColor.PURPLE
        TargetType.BUTTERFLY_GREEN -> ButterflyColor.GREEN
        TargetType.BUTTERFLY_RED -> ButterflyColor.RED
        TargetType.BEE -> null
    }
}

private fun isTaskCompleted(
    task: LevelTask,
    levelScoreProgress: Int,
    collectedButterfliesByColor: Map<ButterflyColor, Int>
): Boolean {
    return when (task) {
        is LevelTask.CollectButterflies ->
            (collectedButterfliesByColor[task.color] ?: 0) >= task.targetCount

        is LevelTask.ReachScore -> levelScoreProgress >= task.targetScore
    }
}

private fun LevelTask.title(): String {
    return when (this) {
        is LevelTask.CollectButterflies ->
            "Собери $targetCount ${color.displayNameAccusative()}"

        is LevelTask.ReachScore -> "Набери $targetScore очков"
    }
}

private fun LevelTask.progressText(
    levelScoreProgress: Int,
    collectedButterfliesByColor: Map<ButterflyColor, Int>
): String {
    return when (this) {
        is LevelTask.CollectButterflies -> {
            val collected = collectedButterfliesByColor[color] ?: 0
            "$collected / $targetCount"
        }

        is LevelTask.ReachScore -> "$levelScoreProgress / $targetScore"
    }
}

private fun LevelTask.iconRes(): Int? {
    return when (this) {
        is LevelTask.CollectButterflies -> when (color) {
        ButterflyColor.GREEN -> R.drawable.butterfly_game_icon_green
        ButterflyColor.PURPLE -> R.drawable.butterfly_game_icon_purple
        ButterflyColor.RED -> R.drawable.butterfly_game_icon_red
        }

        is LevelTask.ReachScore -> null
    }
}

private fun ButterflyColor.displayNameAccusative(): String {
    return when (this) {
        ButterflyColor.RED -> "красных бабочек"
        ButterflyColor.GREEN -> "зеленых бабочек"
        ButterflyColor.PURPLE -> "фиолетовых бабочек"
    }
}

private fun completedTaskIcons(task: LevelTask): List<Int> {
    return when (task) {
        is LevelTask.CollectButterflies -> listOfNotNull(task.iconRes())
        is LevelTask.ReachScore -> listOf(
            R.drawable.butterfly_game_icon_purple,
            R.drawable.butterfly_game_icon_green,
            R.drawable.butterfly_game_icon_red
        )
    }
}

private fun createClickEffect(
    id: Long,
    target: Target,
    effectType: EffectType,
    startTimeMillis: Long
): GameEffect {
    val config = effectConfigs.getValue(effectType)
    val effectSize = target.size * config.sizeMultiplier
    val centerX = target.x + target.size / 2f
    val centerY = target.y + target.size / 2f

    return GameEffect(
        id = id,
        x = centerX - effectSize / 2f,
        y = centerY - effectSize / 2f,
        startTimeMillis = startTimeMillis,
        size = effectSize,
        type = effectType
    )
}

private fun createFloatingHeartEffect(
    id: Long,
    target: Target,
    startTimeMillis: Long,
    topBoundaryPx: Float
): FloatingHeartEffect {
    val effectSize = target.size * 0.42f
    val centerX = target.x + target.size / 2f
    val centerY = target.y + target.size / 2f

    return FloatingHeartEffect(
        id = id,
        x = centerX - effectSize / 2f,
        y = centerY - effectSize / 2f,
        startTimeMillis = startTimeMillis,
        size = effectSize,
        randomXOffset = Random.nextFloat() * 20f - 10f,
        minY = topBoundaryPx + 8f
    )
}
