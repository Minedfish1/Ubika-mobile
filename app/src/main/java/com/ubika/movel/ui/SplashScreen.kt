package com.ubika.movel.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.UbikaDarkBackground
import com.ubika.movel.UbikaPurple
import com.ubika.movel.ui.theme.OrbitronFamily
import kotlinx.coroutines.delay

/**
 * Splash estilo Steam Big Picture, releitura Ubika:
 * os grips da manete telescópica deslizam das bordas e TRAVAM com snap
 * mecânico → o trilho se desenha → o U despenca no berço (a "vaga do celular")
 * → pulso de energia liga o conjunto → wordmark expande tracking.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(-1) }
    // -1 vazio | 0 grips deslizam | 1 trilho desenha | 2 U cai no berço | 3 pulso + wordmark | 4 saída

    // Grips entram rápido e travam com leve overshoot — encaixe mecânico
    val gripIn by animateFloatAsState(
        targetValue = if (phase >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
        label = "gripIn"
    )
    // Trilho inferior da manete se desenha do centro pras pontas
    val rail by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(550, easing = FastOutSlowInEasing),
        label = "rail"
    )
    // O U cai de cima e ASSENTA no berço — o momento "chave vira" do Big Picture
    val uDrop by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "uDrop"
    )
    // Pulso de energia: anel expande uma vez (power-on)
    val pulse by animateFloatAsState(
        targetValue = if (phase >= 3) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "pulse"
    )
    val markIn by animateFloatAsState(
        targetValue = if (phase >= 3) 1f else 0f,
        animationSpec = tween(650, delayMillis = 200, easing = FastOutSlowInEasing),
        label = "markIn"
    )
    val exitAlpha by animateFloatAsState(
        targetValue = if (phase >= 4) 0f else 1f,
        animationSpec = tween(420, easing = LinearEasing),
        label = "exitAlpha"
    )

    LaunchedEffect(Unit) {
        phase = 0
        delay(700)   // grips travam
        phase = 1
        delay(420)   // trilho
        phase = 2
        delay(800)   // U assenta
        phase = 3
        delay(1150)  // pulso + wordmark
        phase = 4
        delay(440)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0616), UbikaDarkBackground, Color(0xFF070310))
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        // ---------- Manete telescópica (Canvas) ----------
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-14).dp)
                .size(width = 340.dp, height = 210.dp)
                .alpha(exitAlpha)
        ) {
            val w = size.width
            val h = size.height
            val purple = UbikaPurple
            val dark = Color(0xFF140822)
            val edge = Color(0xFF2E1760)

            // Geometria dos grips: blocos laterais com "cara" inclinada
            val gripW = 0.135f * w
            val gripH = 0.62f * h
            val gripY = 0.19f * h
            val leftX = 0.055f * w
            val rightX = 0.945f * w - gripW
            val r = 14.dp.toPx()

            // Grips deslizam de fora da tela → posição final com snap
            val lIn = leftX - (1f - gripIn) * (leftX + gripW + 40.dp.toPx())
            val rIn = rightX + (1f - gripIn) * (w - rightX + 40.dp.toPx())

            // Glow radial central quando montado (ambiente Big Picture)
            if (uDrop > 0.5f) {
                val gl = (uDrop - 0.5f) * 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(purple.copy(alpha = 0.22f * gl), Color.Transparent),
                        center = Offset(0.5f * w, 0.47f * h),
                        radius = 0.52f * w
                    ),
                    radius = 0.52f * w,
                    center = Offset(0.5f * w, 0.47f * h)
                )
            }

            // Grip esquerdo
            drawRoundRect(
                color = dark, topLeft = Offset(lIn, gripY),
                size = Size(gripW, gripH), cornerRadius = CornerRadius(r, r)
            )
            drawRoundRect(
                color = edge, topLeft = Offset(lIn, gripY),
                size = Size(gripW, gripH), cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 2.dp.toPx())
            )
            // Grip direito
            drawRoundRect(
                color = dark, topLeft = Offset(rIn, gripY),
                size = Size(gripW, gripH), cornerRadius = CornerRadius(r, r)
            )
            drawRoundRect(
                color = edge, topLeft = Offset(rIn, gripY),
                size = Size(gripW, gripH), cornerRadius = CornerRadius(r, r),
                style = Stroke(width = 2.dp.toPx())
            )
            // detalhe lateral nos grips (botão/ombro) — acende quando trava
            val accent = purple.copy(alpha = 0.35f + 0.65f * gripIn)
            drawRoundRect(
                color = accent, topLeft = Offset(lIn + gripW - 4.dp.toPx(), gripY + 8.dp.toPx()),
                size = Size(2.5.dp.toPx(), gripH - 16.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            drawRoundRect(
                color = accent, topLeft = Offset(rIn + 1.5.dp.toPx(), gripY + 8.dp.toPx()),
                size = Size(2.5.dp.toPx(), gripH - 16.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Trilho inferior: barra telescópica que se desenha do centro pras laterais
            if (rail > 0.01f) {
                val railY = gripY + gripH - 10.dp.toPx()
                val railHalf = (0.5f * w - leftX) * rail
                drawRoundRect(
                    color = edge,
                    topLeft = Offset(0.5f * w - railHalf, railY),
                    size = Size(railHalf * 2, 5.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // Berço central (onde o celular encaixa): colunas laterais finas
            if (rail > 0.5f) {
                val crA = (rail - 0.5f) * 2f
                val crW = 0.19f * w
                val crH = 0.40f * h
                val crY = 0.24f * h
                drawRoundRect(
                    color = purple.copy(alpha = 0.55f * crA),
                    topLeft = Offset(0.5f * w - crW / 2 - crW / 2.6f, crY),
                    size = Size(2.5.dp.toPx(), crH), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = purple.copy(alpha = 0.55f * crA),
                    topLeft = Offset(0.5f * w + crW / 2 + crW / 2.6f - 2.5.dp.toPx(), crY),
                    size = Size(2.5.dp.toPx(), crH), cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // Pulso de energia — anel que expande uma vez quando o U assenta
            if (pulse in 0.01f..0.99f) {
                val pr = pulse * 0.75f * w
                drawCircle(
                    color = purple.copy(alpha = (1f - pulse) * 0.45f),
                    radius = pr,
                    center = Offset(0.5f * w, 0.47f * h),
                    style = Stroke(width = (1f - pulse) * 6.dp.toPx() + 1.dp.toPx())
                )
            }
        }

        // ---------- U caindo no berço ----------
        Text(
            text = "U",
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OrbitronFamily,
            color = UbikaPurple,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-16).dp)
                .graphicsLayer {
                    // de cima da tela → centro do berço, com assentar (spring)
                    translationY = (1f - uDrop) * (-180.dp.toPx())
                    val s = 0.9f + 0.1f * uDrop
                    scaleX = s; scaleY = s
                    alpha = uDrop.coerceIn(0f, 1f) * exitAlpha
                }
        )

        // ---------- Wordmark: tracking expande (Big Picture) ----------
        val tracking = (2 + 10 * markIn).sp
        Text(
            text = "UBIKA",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OrbitronFamily,
            letterSpacing = tracking,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 128.dp)
                .alpha(markIn * exitAlpha)
        )
    }
}
