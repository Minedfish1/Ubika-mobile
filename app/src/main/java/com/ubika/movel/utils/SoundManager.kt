package com.ubika.movel.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ubika.movel.R

class SoundManager(private val context: Context) {

    // Volume baixo + liberação no fim: suave e sem vazar MediaPlayer
    private fun soft(res: Int, vol: Float = 0.32f) {
        MediaPlayer.create(context, res)?.apply {
            setVolume(vol, vol)
            setOnCompletionListener { release() }
            start()
        }
    }

    fun playClickA() = soft(R.raw.click_a)
    fun playClickB() = soft(R.raw.click_b, 0.25f) // ainda mais suave

    // Transições de tela: slide suave
    fun playSlideRB() = soft(R.raw.slide_a, 0.30f)
    fun playSlideLB() = soft(R.raw.slide_a, 0.30f)
    fun playTransition() = soft(R.raw.slide_a, 0.22f) // o mais suave

    fun playNotification() = soft(R.raw.notificacao, 0.5f)

    // --- SISTEMA DE NOTIFICAÇÃO DO ANDROID ---
    private val channelId = "ubika_downloads"

    init {
        // Cria o canal de notificações (Obrigatório no Android moderno)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads Concluídos", NotificationManager.IMPORTANCE_HIGH)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showSystemNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done) // Ícone padrão do Android
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}