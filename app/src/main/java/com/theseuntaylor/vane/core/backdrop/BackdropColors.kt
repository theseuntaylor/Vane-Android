package com.theseuntaylor.vane.core.backdrop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * The background's gradient, top to bottom, for [backdrop] over the theme's [background]. The
 * weather's colour is blended into the theme background rather than replacing it, strongest at the
 * top and fading to the plain background, so text keeps the theme's contrast in light and dark mode.
 */
fun backdropGradient(backdrop: WeatherBackdrop?, background: Color, dark: Boolean): List<Color> {
    if (backdrop == null) return listOf(background, background, background)
    val hue = hueFor(backdrop)
    val strength = if (dark) DARK_STRENGTH else LIGHT_STRENGTH
    return listOf(
        lerp(background, hue, strength),
        lerp(background, hue, strength / 2),
        background,
    )
}

private fun hueFor(backdrop: WeatherBackdrop): Color {
    val sky = when (backdrop.timeOfDay) {
        TimeOfDay.NIGHT -> nightHue(backdrop.sky)
        else -> dayHue(backdrop.sky)
    }
    // Dawn and dusk warm the sky; clear skies take the full sunrise colour, cloudy ones a hint of it.
    val glow = when (backdrop.timeOfDay) {
        TimeOfDay.DAWN -> Dawn
        TimeOfDay.DUSK -> Dusk
        else -> return sky
    }
    val warmth = if (backdrop.sky == Sky.CLEAR || backdrop.sky == Sky.PARTLY_CLOUDY) 0.85f else 0.4f
    return lerp(sky, glow, warmth)
}

private fun dayHue(sky: Sky) = when (sky) {
    Sky.CLEAR -> Color(0xFF4FA3F7)
    Sky.PARTLY_CLOUDY -> Color(0xFF7FB2E5)
    Sky.CLOUDY -> Color(0xFF9AA7B4)
    Sky.FOG -> Color(0xFFB9BEC4)
    Sky.RAIN -> Color(0xFF5F7A99)
    Sky.SNOW -> Color(0xFFCFE3F5)
    Sky.STORM -> Color(0xFF4B4F6B)
}

private fun nightHue(sky: Sky) = when (sky) {
    Sky.CLEAR -> Color(0xFF1B2A55)
    Sky.PARTLY_CLOUDY -> Color(0xFF26324F)
    Sky.CLOUDY -> Color(0xFF2E3440)
    Sky.FOG -> Color(0xFF3A3F47)
    Sky.RAIN -> Color(0xFF1F2E40)
    Sky.SNOW -> Color(0xFF33415A)
    Sky.STORM -> Color(0xFF241F3D)
}

private val Dawn = Color(0xFFF6A97A)
private val Dusk = Color(0xFFE07A5F)

private const val LIGHT_STRENGTH = 0.35f
private const val DARK_STRENGTH = 0.45f
