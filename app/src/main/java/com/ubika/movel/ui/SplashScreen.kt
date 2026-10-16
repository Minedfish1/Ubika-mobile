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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.UbikaDarkBackground
import com.ubika.movel.UbikaPurple
import kotlinx.coroutines.delay

/**
 * Animação inicial: "UBIKA" entra, o BIKA se desfaz e a perninha do U
 * desenha a silhueta de uma manete (dois traços que partem do ponto sob o U
 * e se encontram na base). O U então se acomoda dentro da silhueta.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var phase by remember { mutableIntStateOf(-1) }
    // -1 = tela vazia | 0 = letras entrando | 1 = BIKA sai | 2 = traço da manete + U desce | 3 = brilho | 4 = saída

    val lettersIn by animateFloatAsState(
        targetValue = if (phase >= 0) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "lettersIn"
    )
    val bikaPull by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "bikaPull"
    )
    val uDrop by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "uDrop"
    )
    val padDraw by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(1200, delayMillis = 120, easing = FastOutSlowInEasing),
        label = "padDraw"
    )
    val glow by animateFloatAsState(
        targetValue = if (phase >= 3) 1f else 0f,
        animationSpec = tween(600, easing = LinearEasing),
        label = "glow"
    )
    val exitAlpha by animateFloatAsState(
        targetValue = if (phase >= 4) 0f else 1f,
        animationSpec = tween(420, easing = LinearEasing),
        label = "exitAlpha"
    )

    LaunchedEffect(Unit) {
        phase = 0
        delay(950)
        phase = 1
        delay(850)
        phase = 2
        delay(1500)
        phase = 3
        delay(900)
        phase = 4
        delay(460)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UbikaDarkBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        // Silhueta da manete: dois traços saem do ponto sob o U e se encontram na base
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 40.dp)
                .size(width = 300.dp, height = 200.dp)
                .alpha(exitAlpha)
        ) {
            val stroke = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val w = size.width
            val h = size.height

            // Metade direita: topo-centro -> ombro -> grip -> cintura inferior
            val right = Path().apply {
                moveTo(0.50f * w, 0.10f * h)
                cubicTo(0.60f * w, 0.06f * h, 0.70f * w, 0.07f * h, 0.76f * w, 0.16f * h)
                cubicTo(0.84f * w, 0.27f * h, 0.92f * w, 0.40f * h, 0.95f * w, 0.55f * h)
                cubicTo(0.97f * w, 0.63f * h, 0.95f * w, 0.73f * h, 0.88f * w, 0.78f * h)
                cubicTo(0.81f * w, 0.83f * h, 0.73f * w, 0.80f * h, 0.70f * w, 0.72f * h)
                cubicTo(0.66f * w, 0.63f * h, 0.62f * w, 0.57f * h, 0.56f * w, 0.55f * h)
                cubicTo(0.53f * w, 0.545f * h, 0.515f * w, 0.55f * h, 0.50f * w, 0.58f * h)
            }
            // Metade esquerda: espelho da direita
            val left = Path().apply {
                moveTo(0.50f * w, 0.10f * h)
                cubicTo(0.40f * w, 0.06f * h, 0.30f * w, 0.07f * h, 0.24f * w, 0.16f * h)
                cubicTo(0.16f * w, 0.27f * h, 0.08f * w, 0.40f * h, 0.05f * w, 0.55f * h)
                cubicTo(0.03f * w, 0.63f * h, 0.05f * w, 0.73f * h, 0.12f * w, 0.78f * h)
                cubicTo(0.19f * w, 0.83f * h, 0.27f * w, 0.80f * h, 0.30f * w, 0.72f * h)
                cubicTo(0.34f * w, 0.63f * h, 0.38f * w, 0.57f * h, 0.44f * w, 0.55f * h)
                cubicTo(0.47f * w, 0.545f * h, 0.485f * w, 0.55f * h, 0.50f * w, 0.58f * h)
            }

            val pmR = PathMeasure().apply { setPath(right, false) }
            val pmL = PathMeasure().apply { setPath(left, false) }
            val segR = Path().also { pmR.getSegment(0f, pmR.length * padDraw, it, true) }
            val segL = Path().also { pmL.getSegment(0f, pmL.length * padDraw, it, true) }

            // Brilho: traço largo e translúcido por trás, pulsando no final
            if (padDraw > 0.02f) {
                val glowStroke = Stroke(
                    width = (14f + 6f * glow) * density,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
                val glowAlpha = 0.10f + 0.16f * glow
                drawPath(segR, UbikaPurple.copy(alpha = glowAlpha), style = glowStroke)
                drawPath(segL, UbikaPurple.copy(alpha = glowAlpha), style = glowStroke)
            }
            drawPath(segR, UbikaPurple, style = stroke)
            drawPath(segL, UbikaPurple, style = stroke)

            // Fagulha na ponta dos traços enquanto desenham
            if (padDraw in 0.02f..0.98f) {
                val tipR = FloatArray(2)
                pmR.getPosTan(pmR.length * padDraw, tipR, FloatArray(2))
                val tipL = FloatArray(2)
                pmL.getPosTan(pmL.length * padDraw, tipL, FloatArray(2))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(tipR[0], tipR[1]))
                drawCircle(Color.White, radius = 3.dp.toPx(), center = Offset(tipL[0], tipL[1]))
            }
        }

        // Wordmark: U sobe/acompana o traço; BIKA se desfaz puxado para a direita
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-95).dp)
                .alpha(exitAlpha)
        ) {
            Text(
                text = "U",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = UbikaPurple,
                modifier = Modifier.graphicsLayer {
                    // A perninha do U "cai" para dentro da manete formada
                    translationY = uDrop * 150.dp.toPx()
                    val s = 1f - 0.45f * uDrop
                    scaleX = s
                    scaleY = s
                    alpha = lettersIn
                }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("B", "I", "K", "A").forEachIndexed { index, char ->
                    val enter = (lettersIn * 2.2f - index * 0.35f).coerceIn(0f, 1f)
                    val letterProgress = (bikaPull * (1f + index * 0.2f)).coerceIn(0f, 1f)
                    Text(
                        text = char,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .graphicsLayer {
                                alpha = enter * (1f - letterProgress)
                                translationX = letterProgress * (120f + index * 80f)
                            }
                            .padding(start = 2.dp)
                    )
                }
            }
        }

        Text(
            text = "UBIKA",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 6.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 165.dp)
                .alpha(glow * exitAlpha)
        )
    }
}
