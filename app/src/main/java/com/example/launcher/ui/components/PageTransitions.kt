package com.example.launcher.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.example.launcher.model.PageTransitionStyle
import kotlin.math.absoluteValue

fun Modifier.applyPageTransition(
    pagerState: PagerState,
    page: Int,
    style: PageTransitionStyle
): Modifier = this.graphicsLayer {
    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
    val absOffset = pageOffset.absoluteValue.coerceIn(0f, 1.5f)

    when (style) {
        PageTransitionStyle.FLUID_GLASS -> {
            val scale = 1f - (absOffset * 0.10f)
            scaleX = scale
            scaleY = scale
            alpha = (1f - absOffset * 0.42f).coerceIn(0.25f, 1f)
            translationX = pageOffset * size.width * 0.08f
        }

        PageTransitionStyle.CUBE_3D -> {
            cameraDistance = 18f * density
            transformOrigin = TransformOrigin(
                pivotFractionX = if (pageOffset < 0f) 0f else 1f,
                pivotFractionY = 0.5f
            )
            rotationY = (-42f * pageOffset).coerceIn(-65f, 65f)
            alpha = (1f - absOffset * 0.35f).coerceIn(0.2f, 1f)
        }

        PageTransitionStyle.DEPTH_STACK -> {
            if (pageOffset < 0f) {
                // Incoming page from right rises from behind
                val scale = (0.86f + (1f - absOffset) * 0.14f).coerceIn(0.82f, 1f)
                scaleX = scale
                scaleY = scale
                alpha = (1f - absOffset * 0.55f).coerceIn(0.2f, 1f)
                translationX = size.width * -pageOffset * 0.45f
            } else {
                // Current page glides away cleanly
                scaleX = 1f
                scaleY = 1f
                alpha = (1f - absOffset * 0.25f).coerceIn(0.4f, 1f)
            }
        }

        PageTransitionStyle.SMOOTH_FAN -> {
            transformOrigin = TransformOrigin(0.5f, 1.35f)
            rotationZ = (-11f * pageOffset).coerceIn(-18f, 18f)
            val scale = 1f - (absOffset * 0.08f)
            scaleX = scale
            scaleY = scale
            translationY = absOffset * 36f
            alpha = (1f - absOffset * 0.35f).coerceIn(0.25f, 1f)
        }

        PageTransitionStyle.ELASTIC_SLIDE -> {
            val stretch = 1f + (absOffset * 0.04f)
            val squeeze = 1f - (absOffset * 0.06f)
            scaleX = stretch
            scaleY = squeeze
            alpha = (1f - absOffset * 0.18f).coerceIn(0.55f, 1f)
        }
    }
}
