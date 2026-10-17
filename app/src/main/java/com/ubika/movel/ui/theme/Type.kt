package com.ubika.movel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ubika.movel.R

/** Orbitron variável — títulos/marca (wght 400–900 via FontVariation). */
@OptIn(ExperimentalTextApi::class)
private fun orbitron(weight: Int) = Font(
    R.font.orbitron,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)
val OrbitronFamily = FontFamily(
    orbitron(400), orbitron(500), orbitron(600), orbitron(700), orbitron(800), orbitron(900)
)

/** Rajdhani — corpo/labels: geométrica condensada usada em UIs de console. */
val RajdhaniFamily = FontFamily(
    Font(R.font.rajdhani_regular, FontWeight.Normal),
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = RajdhaniFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)
