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

    // Silhueta limpa de gamepad: ombros arredondados + duas pegas (grips)
    val masterPath = Path().apply {
        // Ombro direito → topo → ombro esquerdo
        moveTo(w * 0.72f, h * 0.22f)
        cubicTo(w * 0.72f, h * 0.08f, w * 0.60f, h * 0.04f, w * 0.50f, h * 0.04f)
        cubicTo(w * 0.40f, h * 0.04f, w * 0.28f, h * 0.08f, w * 0.28f, h * 0.22f)
        // Lateral esquerda descendo até a pega
        cubicTo(w * 0.10f, h * 0.26f, w * 0.06f, h * 0.50f, w * 0.12f, h * 0.72f)
        // Pega esquerda (contorno interno voltando)
        cubicTo(w * 0.16f, h * 0.90f, w * 0.30f, h * 0.94f, w * 0.34f, h * 0.80f)
        cubicTo(w * 0.36f, h * 0.72f, w * 0.38f, h * 0.66f, w * 0.44f, h * 0.64f)
        // Base central → pega direita
        cubicTo(w * 0.56f, h * 0.64f, w * 0.64f, h * 0.72f, w * 0.66f, h * 0.80f)
        cubicTo(w * 0.70f, h * 0.94f, w * 0.84f, h * 0.90f, w * 0.88f, h * 0.72f)
        // Lateral direita subindo e fechando no ombro
        cubicTo(w * 0.94f, h * 0.50f, w * 0.90f, h * 0.26f, w * 0.72f, h * 0.22f)
    }

    val pm = PathMeasure()
    pm.setPath(masterPath, false)
    val totalLength = pm.length

    val currentLength = totalLength * progress.coerceIn(0.01f, 1f)
    val animatedPath = Path()
    pm.getSegment(0f, currentLength, animatedPath, true)

    drawPath(path = animatedPath, color = color, style = stroke)

    // Detalhes só surgem quando o contorno está quase completo
    if (progress > 0.5f) {
        val detailAlpha = ((progress - 0.5f) / 0.4f).coerceIn(0f, 1f)
        val c = color.copy(alpha = detailAlpha)

        // "U" central — detalhe da logo dentro da manete
        val uPath = Path().apply {
            moveTo(w * 0.44f, h * 0.22f)
            lineTo(w * 0.44f, h * 0.38f)
            cubicTo(w * 0.44f, h * 0.47f, w * 0.56f, h * 0.47f, w * 0.56f, h * 0.38f)
            lineTo(w * 0.56f, h * 0.22f)
        }
        drawPath(uPath, c, style = stroke)

        // D-pad esquerdo (cruz pequena)
        drawLine(c, Offset(w * 0.30f, h * 0.34f), Offset(w * 0.30f, h * 0.46f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(c, Offset(w * 0.25f, h * 0.40f), Offset(w * 0.35f, h * 0.40f), strokeWidth = stroke.width, cap = StrokeCap.Round)

        // Botões direitos (2 pontos)
        drawCircle(c, radius = w * 0.028f, center = Offset(w * 0.67f, h * 0.37f), style = stroke)
        drawCircle(c, radius = w * 0.028f, center = Offset(w * 0.74f, h * 0.43f), style = stroke)
    }
}
