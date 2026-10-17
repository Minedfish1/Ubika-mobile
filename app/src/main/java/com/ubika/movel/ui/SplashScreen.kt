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
 * as duas metades da manete telescópica deslizam das bordas e TRAVAM →
 * os botões acendem em sequência (power-on) → o trilho se desenha →
 * o U despenca no berço central → pulso + wordmark expande tracking.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(-1) }
    // -1 vazio | 0 metades travam | 1 botões acendem | 2 trilho + U | 3 pulso + wordmark | 4 saída

    val gripIn by animateFloatAsState(
        targetValue = if (phase >= 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = Spring.StiffnessMediumLow),
        label = "gripIn"
    )
    val buttonsIn by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(650, easing = LinearOutSlowInEasing),
        label = "buttonsIn"
    )
    val rail by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "rail"
    )
    val uDrop by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "uDrop"
    )
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
        delay(750)   // metades travam
        phase = 1
        delay(700)   // botões acendem
        phase = 2
        delay(750)   // trilho + U assenta
        phase = 3
        delay(1100)  // pulso + wordmark
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
            val dark = Color(0xFF0B0614)
            val edge = Color(0xFF2E1760)

            // ---- Geometria (mesma do ícone, em frações) ----
            val bodyW = 0.155f * w
            val bodyH = 0.42f * h
            val bodyY = 0.20f * h
            val leftX = 0.045f * w
            val rightX = 0.955f * w - bodyW
            val bodyR = 12.dp.toPx()
            val lobeR = 0.062f * w
            val lobeY = 0.74f * h

            // Metades deslizam de fora da tela → posição final com snap
            val lIn = leftX - (1f - gripIn) * (leftX + bodyW + lobeR + 30.dp.toPx())
            val rIn = rightX + (1f - gripIn) * (w - rightX + lobeR + 30.dp.toPx())
            val lCx = lIn + bodyW / 2f          // centro do corpo esquerdo
            val rCx = rIn + bodyW / 2f          // centro do corpo direito
            val lLobeX = lIn + 0.10f * bodyW    // lobo do grip: base externa
            val rLobeX = rIn + 0.90f * bodyW

            // Glow radial quando montado
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

            // ---- Corpos + lobos dos grips ----
            for ((bx, lx) in listOf(lIn to lLobeX, rIn to rLobeX)) {
                drawRoundRect(
                    color = dark, topLeft = Offset(bx, bodyY),
                    size = Size(bodyW, bodyH), cornerRadius = CornerRadius(bodyR, bodyR)
                )
                drawRoundRect(
                    color = edge, topLeft = Offset(bx, bodyY),
                    size = Size(bodyW, bodyH), cornerRadius = CornerRadius(bodyR, bodyR),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(color = dark, radius = lobeR, center = Offset(lx, lobeY))
                drawCircle(
                    color = edge, radius = lobeR, center = Offset(lx, lobeY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // ---- Botões acendem em sequência (pop escalonado) ----
            // janela de 6 slots: stick L, dpad L, Y X B A (losango), stick R
            fun slot(i: Int): Float = ((buttonsIn * 6f) - i).coerceIn(0f, 1f)
            val stickR = 0.030f * w
            val dotR = 0.017f * w

            // Stick esquerdo (donut) — slot 0
            run {
                val t = slot(0); if (t > 0f) {
                    val c = Offset(lCx, bodyY + bodyH * 0.28f)
                    drawCircle(color = purple.copy(alpha = t), radius = stickR * t, center = c)
                    drawCircle(color = dark, radius = stickR * 0.45f * t, center = c)
                }
            }
            // Dpad esquerdo (cruz) — slot 1
            run {
                val t = slot(1); if (t > 0f) {
                    val c = Offset(lCx, bodyY + bodyH * 0.66f)
                    val arm = 0.030f * w * t
                    val th = 0.014f * w * t
                    drawRoundRect(color = purple.copy(alpha = t),
                        topLeft = Offset(c.x - th / 2, c.y - arm), size = Size(th, arm * 2),
                        cornerRadius = CornerRadius(th / 2, th / 2))
                    drawRoundRect(color = purple.copy(alpha = t),
                        topLeft = Offset(c.x - arm, c.y - th / 2), size = Size(arm * 2, th),
                        cornerRadius = CornerRadius(th / 2, th / 2))
                }
            }
            // Losango ABXY direito — slots 2..5
            val btnC = Offset(rCx, bodyY + bodyH * 0.30f)
            val btnOff = 0.035f * w
            listOf(
                Offset(0f, -btnOff), Offset(btnOff, 0f),
                Offset(0f, btnOff), Offset(-btnOff, 0f)
            ).forEachIndexed { i, off ->
                val t = slot(2 + i); if (t > 0f)
                    drawCircle(color = purple.copy(alpha = t), radius = dotR * t, center = btnC + off)
            }
            // Stick direito (donut) — slot 5
            run {
                val t = slot(5); if (t > 0f) {
                    val c = Offset(rCx, bodyY + bodyH * 0.68f)
                    drawCircle(color = purple.copy(alpha = t), radius = stickR * t, center = c)
                    drawCircle(color = dark, radius = stickR * 0.45f * t, center = c)
                }
            }

            // ---- Trilho telescópico: desenha do centro pras laterais ----
            if (rail > 0.01f) {
                val railY = bodyY + bodyH * 0.78f
                val railHalf = (rCx - lCx) * rail / 2f
                drawRoundRect(
                    color = dark,
                    topLeft = Offset(0.5f * w - railHalf, railY),
                    size = Size(railHalf * 2, 6.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }

            // ---- Pulso de energia quando o U assenta ----
            if (pulse in 0.01f..0.99f) {
                val pr = pulse * 0.72f * w
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
            fontSize = 60.sp,
            fontWeight = FontWeight.Black,
            fontFamily = OrbitronFamily,
            color = Color(0xFFE6DEFA),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-20).dp)
                .graphicsLayer {
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
