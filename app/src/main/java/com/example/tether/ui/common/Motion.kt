package com.example.tether.ui.common

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Shared motion utilities for the polish pass. All motion here respects the system
 * animator duration scale: when the user has animations turned off, scale and
 * translation motion is dropped while alpha and color changes are kept.
 */

/** True when the system animator duration scale is set to zero (reduced motion). */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}

/**
 * Press feedback: scales the element to 0.97f while pressed, 140ms ease-out.
 * Applied through graphicsLayer so it stays on the compositing layer and does
 * not trigger recomposition or relayout.
 */
fun Modifier.pressScale(interactionSource: InteractionSource): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = rememberReducedMotion()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) 0.97f else 1f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Staggered list item entrance: fade plus a small upward drift, delayed by
 * 40ms per item and capped at 8 items so long lists do not feel slow to finish.
 * Under reduced motion only the fade is kept.
 */
fun Modifier.staggeredEntrance(index: Int): Modifier = composed {
    val reduced = rememberReducedMotion()
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(minOf(index, 8) * 40L)
        entered = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    val drift by animateFloatAsState(
        targetValue = if (entered || reduced) 0f else 1f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "staggerDrift"
    )
    graphicsLayer {
        this.alpha = alpha
        translationY = drift * 8.dp.toPx()
    }
}
