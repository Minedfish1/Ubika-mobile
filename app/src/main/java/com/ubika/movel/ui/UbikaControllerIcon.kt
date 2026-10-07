package com.ubika.movel.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ubika.movel.UbikaPurple

@Composable
fun UbikaControllerIcon(
    modifier: Modifier = Modifier,
    color: Color = UbikaPurple,
    strokeWidth: Dp = 3.dp,
    progress: Float = 1f   // 0f a 1f controlando o arraste contínuo da ponta
) {
    Canvas(modifier = modifier) {
        val sw = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        drawDraggingControllerPath(color, sw, progress)
    }
}

fun DrawScope.drawDraggingControllerPath(color: Color, stroke: Stroke, progress: Float) {
    val w = size.width
    val h = size.height

    // Traço master unificado: desenha a silhueta completa da manete partindo do ponto inicial do "U"
    // até a ponta exata do gancho que é puxada na animação.
    val masterPath = Path().apply {
        // Início no topo direito do U / ombro direito da manete
        moveTo(w * 0.70f, h * 0.25f)
        cubicTo(w * 0.70f, h * 0.12f, w * 0.58f, h * 0.06f, w * 0.50f, h * 0.06f)
        cubicTo(w * 0.42f, h * 0.06f, w * 0.30f, h * 0.12f, w * 0.30f, h * 0.25f)

        // Desce pelo lado direito até o grip direito
        cubicTo(w * 0.08f, h * 0.28f, w * 0.08f, h * 0.52f, w * 0.24f, h * 0.78f)

        // Base inferior e curva interna
        cubicTo(w * 0.26f, h * 0.76f, w * 0.28f, h * 0.70f, w * 0.26f, h * 0.68f)
        cubicTo(w * 0.22f, h * 0.65f, w * 0.18f, h * 0.72f, w * 0.20f, h * 0.78f)

        // Puxando o grip esquerdo e a perna final que faz o gancho da imagem
        cubicTo(w * 0.24f, h * 0.88f, w * 0.40f, h * 0.92f, w * 0.44f, h * 0.78f)
        cubicTo(w * 0.46f, h * 0.70f, w * 0.54f, h * 0.70f, w * 0.56f, h * 0.78f)
        cubicTo(w * 0.60f, h * 0.92f, w * 0.76f, h * 0.88f, w * 0.82f, h * 0.72f)
        cubicTo(w * 0.95f, h * 0.52f, w * 0.92f, h * 0.28f, w * 0.70f, h * 0.25f)
    }

    val pm = PathMeasure()
    pm.setPath(masterPath, false)
    val totalLength = pm.length

    // Corta o traço exatamente na porcentagem do progresso (fazendo a ponta ser "arrastada" na tela)
    val currentLength = totalLength * progress.coerceIn(0.01f, 1f)
    val animatedPath = Path()
    pm.getSegment(0f, currentLength, animatedPath, true)

    // Desenha o traço em movimento fluido
    drawPath(path = animatedPath, color = color, style = stroke)

    // O botão central (home/touchpad) só surge quando a perna da manete estiver quase totalmente formada
    if (progress > 0.85f) {
        val circleAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        drawCircle(
            color = color.copy(alpha = circleAlpha),
            radius = w * 0.045f,
            center = Offset(w * 0.50f, h * 0.20f),
            style = stroke
        )
    }
}