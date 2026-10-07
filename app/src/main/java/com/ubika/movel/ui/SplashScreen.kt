package com.ubika.movel.ui

import android.media.MediaPlayer // Importe o MediaPlayer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext // Importe o Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.R // Importe a classe R do seu projeto
import com.ubika.movel.UbikaDarkBackground
import com.ubika.movel.UbikaPurple
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val context = LocalContext.current

    // Inicialize os tocadores de som (crie a pasta res/raw e adicione os arquivos mp3)
    // Descomente as linhas abaixo quando tiver os arquivos de áudio
    /*
    val pullSound = remember { MediaPlayer.create(context, R.raw.pull_sound) }
    val zoomSound = remember { MediaPlayer.create(context, R.raw.zoom_impact) }
    */

    var triggerExit by remember { mutableStateOf(false) }
    var startZoom by remember { mutableStateOf(false) }

    val uThrowToCenter by animateFloatAsState(
        targetValue = if (triggerExit) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "uThrowToCenter"
    )

    val bikaPullProgress by animateFloatAsState(
        targetValue = if (triggerExit) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "bikaPullProgress"
    )

    val zoomDuration = 600
    val uScale by animateFloatAsState(
        targetValue = if (startZoom) 15f else 1f,
        animationSpec = tween(durationMillis = zoomDuration, easing = FastOutSlowInEasing),
        label = "uScale"
    )
    val uAlpha by animateFloatAsState(
        targetValue = if (startZoom) 0f else 1f,
        animationSpec = tween(durationMillis = zoomDuration, easing = LinearEasing),
        label = "uAlpha"
    )

    LaunchedEffect(key1 = true) {
        delay(1000)

        // Toca o som de puxar o BIKA
        // pullSound?.start()

        triggerExit = true
        delay(1000)

        // Toca o som de impacto do Zoom do U
        // zoomSound?.start()

        startZoom = true
        delay(zoomDuration.toLong())

        // Libera a memória dos sons
        /*
        pullSound?.release()
        zoomSound?.release()
        */

        onFinished()
    }

    // ... Restante do código da UI exatamente igual ao anterior (Box, Row, etc) ...
    // ... [Mantenha a mesma Box e Rows que montamos no passo anterior] ...
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UbikaDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                translationX = 100.dp.toPx() * uThrowToCenter
            }
        ) {
            Box(
                modifier = Modifier
                    .scale(uScale)
                    .alpha(uAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "U",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    color = UbikaPurple
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val letters = listOf("B", "I", "K", "A")
                letters.forEachIndexed { index, char ->
                    val letterProgress = (bikaPullProgress * (1f + index * 0.2f)).coerceIn(0f, 1f)
                    val alpha = 1f - letterProgress
                    val translationX = letterProgress * (120f + index * 80f)

                    Text(
                        text = char,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier
                            .graphicsLayer {
                                this.alpha = alpha
                                this.translationX = translationX
                            }
                            .padding(start = 2.dp)
                    )
                }
            }
        }
    }
}