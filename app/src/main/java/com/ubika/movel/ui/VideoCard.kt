package com.ubika.movel.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun VideoPreviewCard(videoUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // Inicializa o leitor de vídeo moderno (Media3) em loop e mudo para preview
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f // Mudo para não colidir com a música de fundo do card
            prepare()
            playWhenReady = true
        }
    }

    // Liberta a memória automaticamente quando o componente sai do ecrã
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false // Esconde os botões de play/pause padrão da consola
            }
        },
        modifier = modifier
    )
}