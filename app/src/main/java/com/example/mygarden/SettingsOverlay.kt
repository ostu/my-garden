package com.example.mygarden

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun SettingsOverlay(
    musicEnabled: Boolean,
    soundEnabled: Boolean,
    onToggleMusic: () -> Unit,
    onToggleSound: () -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val panelPainter = painterResource(id = R.drawable.common_settings_panel)
    val switchOnPainter = painterResource(id = R.drawable.common_settings_button_switch_on)
    val switchOffPainter = painterResource(id = R.drawable.common_settings_button_switch_off)
    val continuePainter = painterResource(id = R.drawable.common_settings_button_continue)
    val againPainter = painterResource(id = R.drawable.common_settings_button_again)
    val homePainter = painterResource(id = R.drawable.common_settings_button_home)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallback() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints {
            val maxPanelWidth = maxWidth * 0.9f
            val maxPanelHeight = maxHeight * 0.88f
            val widthFromHeight = maxPanelHeight * panelAspectRatio
            val panelWidth = if (widthFromHeight < maxPanelWidth) widthFromHeight else maxPanelWidth
            val panelHeight = panelWidth / panelAspectRatio
            val switchWidth = panelWidth * 0.20f
            val continueButtonWidth = panelWidth * 0.72f
            val againButtonWidth = panelWidth * 0.65f
            val homeButtonWidth = panelWidth * 0.645f

            Box(
                modifier = Modifier
                    .size(width = panelWidth, height = panelHeight)
            ) {
                Image(
                    painter = panelPainter,
                    contentDescription = "Settings Panel",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                SettingsImageButton(
                    painter = if (musicEnabled) switchOnPainter else switchOffPainter,
                    contentDescription = "Music Switch",
                    width = switchWidth,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = panelWidth * 0.645f, y = panelHeight * 0.256f),
                    onClick = onToggleMusic
                )

                SettingsImageButton(
                    painter = if (soundEnabled) switchOnPainter else switchOffPainter,
                    contentDescription = "Sound Switch",
                    width = switchWidth,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = panelWidth * 0.645f, y = panelHeight * 0.405f),
                    onClick = onToggleSound
                )

                SettingsImageButton(
                    painter = continuePainter,
                    contentDescription = "Continue",
                    width = continueButtonWidth,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = panelHeight * 0.534f),
                    onClick = onResume
                )

                SettingsImageButton(
                    painter = againPainter,
                    contentDescription = "Restart",
                    width = againButtonWidth,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = panelHeight * 0.682f),
                    onClick = onRestart
                )

                SettingsImageButton(
                    painter = homePainter,
                    contentDescription = "Home",
                    width = homeButtonWidth,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = panelHeight * 0.818f),
                    onClick = onHome
                )
            }
        }
    }
}

@Composable
private fun SettingsImageButton(
    painter: Painter,
    contentDescription: String,
    width: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val aspectRatio = remember(painter) { painter.aspectRatioOrFallback() }

    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(width = width, height = width / aspectRatio)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
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
