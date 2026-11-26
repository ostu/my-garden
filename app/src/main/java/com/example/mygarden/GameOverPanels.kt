package com.example.mygarden

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun AppleStyleGameOverOverlay(
    score: Int,
    onHomeClick: () -> Unit,
    onRestartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelPainter = painterResource(id = R.drawable.apples_game_over_panel)
    val panelAspectRatio = remember(panelPainter) { panelPainter.aspectRatioOrFallbackShared() }
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .zIndex(100f)
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints {
            val isLandscape = maxWidth > maxHeight
            val maxPanelHeight = if (isLandscape) maxHeight * 0.78f else maxHeight * 0.86f
            val maxWidthFractionByHeight = if (maxWidth.value > 0f && panelAspectRatio > 0f) {
                ((maxPanelHeight.value * panelAspectRatio) / maxWidth.value).coerceAtMost(0.82f)
            } else {
                0.82f
            }
            val panelWidthFraction = if (isLandscape) {
                maxWidthFractionByHeight.coerceAtMost(0.72f)
            } else {
                maxWidthFractionByHeight.coerceAtMost(0.82f)
            }.coerceAtLeast(0.45f)

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(panelWidthFraction)
                    .heightIn(max = maxPanelHeight)
                    .aspectRatio(panelAspectRatio)
            ) {
                Image(
                    painter = panelPainter,
                    contentDescription = "Game Over",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSharedOutlinedText(
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
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppleStyleOverlayButton(
                        label = "Main menu",
                        painterRes = R.drawable.button_menu,
                        onClick = onHomeClick
                    )
                    AppleStyleOverlayButton(
                        label = "Restart",
                        painterRes = R.drawable.common_button_repeat,
                        onClick = onRestartClick
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleStyleOverlayButton(
    label: String,
    painterRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val painter = painterResource(id = painterRes)
    val aspectRatio = remember(painter) { painter.aspectRatioOrFallbackShared() }

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

private fun DrawScope.drawSharedOutlinedText(
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

private fun Painter.aspectRatioOrFallbackShared(): Float {
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
