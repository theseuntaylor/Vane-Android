package com.theseuntaylor.vane.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.theseuntaylor.vane.R

private val HeadingFontFamily = FontFamily(
    Font(R.font.heading_regular, FontWeight.Normal),
    Font(R.font.heading_medium, FontWeight.Medium),
)

private val BodyFontFamily = FontFamily(
    Font(R.font.body_regular, FontWeight.Normal),
    Font(R.font.body_medium, FontWeight.Medium),
)

/** M3 type scale: display, headline and title roles use the heading font; body and label use the body font. */
fun vaneTypography(): Typography {
    val base = Typography()
    return base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = HeadingFontFamily),
        displayMedium = base.displayMedium.copy(fontFamily = HeadingFontFamily),
        displaySmall = base.displaySmall.copy(fontFamily = HeadingFontFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = HeadingFontFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = HeadingFontFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = HeadingFontFamily),
        titleLarge = base.titleLarge.copy(fontFamily = HeadingFontFamily),
        titleMedium = base.titleMedium.copy(fontFamily = HeadingFontFamily),
        titleSmall = base.titleSmall.copy(fontFamily = HeadingFontFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = BodyFontFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = BodyFontFamily),
        bodySmall = base.bodySmall.copy(fontFamily = BodyFontFamily),
        labelLarge = base.labelLarge.copy(fontFamily = BodyFontFamily),
        labelMedium = base.labelMedium.copy(fontFamily = BodyFontFamily),
        labelSmall = base.labelSmall.copy(fontFamily = BodyFontFamily),
    )
}
