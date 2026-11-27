package com.theseuntaylor.vane.core.backdrop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/** The backdrop the app draws behind every screen. Screens with weather set it; others leave it empty. */
@Stable
class WeatherBackdropState {
    var backdrop by mutableStateOf<WeatherBackdrop?>(null)
}

val LocalWeatherBackdrop = staticCompositionLocalOf { WeatherBackdropState() }

/** Shows [backdrop] behind the app while the calling screen is on screen. */
@Composable
fun ShowWeatherBackdrop(backdrop: WeatherBackdrop?) {
    val state = LocalWeatherBackdrop.current
    DisposableEffect(state, backdrop) {
        state.backdrop = backdrop
        // Leaving the screen clears it, unless the next screen has already set its own.
        onDispose { if (state.backdrop == backdrop) state.backdrop = null }
    }
}

/**
 * Draws [state]'s backdrop as a vertical gradient, fading between backdrops when it changes. With
 * none, it's the plain [background]. Tagged with the backdrop's name for E2E checks.
 */
@Composable
fun Modifier.weatherBackdrop(state: WeatherBackdropState, background: Color, dark: Boolean): Modifier {
    val target = backdropGradient(state.backdrop, background, dark)
    val top by animateColorAsState(target[0], tween(FADE_MILLIS), label = "backdropTop")
    val middle by animateColorAsState(target[1], tween(FADE_MILLIS), label = "backdropMiddle")
    val bottom by animateColorAsState(target[2], tween(FADE_MILLIS), label = "backdropBottom")
    return this
        .background(Brush.verticalGradient(listOf(top, middle, bottom)))
        .testTag(state.backdrop?.tag ?: "backdrop-none")
}

private const val FADE_MILLIS = 600
