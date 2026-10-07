package com.ubika.movel.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ubika.movel.R

class SoundManager(private val context: Context) {

    fun playClickA() = MediaPlayer.create(context, R.raw.click_a)?.start()
    fun playClickB() = MediaPlayer.create(context, R.raw.click_b)?.start()

    // Agora ambos usam o slide_a (som melhor)
    fun playSlideRB() = MediaPlayer.create(context, R.raw.slide_a)?.start()
    fun playSlideLB() = MediaPlayer.create(context, R.raw.slide_a)?.start()

    fun playNotification() = MediaPlayer.create(context, R.raw.notificacao)?.start()

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