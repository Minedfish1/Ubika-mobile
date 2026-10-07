package com.ubika.movel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.utils.SoundManager

data class DownloadItem(
    val gameName: String,
    var progress: Float, // 0.0 a 100.0
    var speed: String
)

@Composable
fun DownloadsScreen(soundManager: SoundManager) {
    // Lista reativa de downloads ativos no PC
    var activeDownloads = remember { mutableStateListOf<DownloadItem>() }

    // Aqui escutaria os eventos de progresso vindos do socket da AWS
    // Ex: UbikaSocket.onProgressUpdate { name, prog, speed -> ... }

    Box(modifier = Modifier.fillMaxSize().background(marbledBackground).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("DOWNLOADS EM TEMPO REAL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)

            if (activeDownloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum download ativo no momento.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activeDownloads) { download ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF140822), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF311B52), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(download.gameName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(download.speed, color = Color(0xFF00C853), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                LinearProgressIndicator(
                                    progress = { download.progress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(8.dp),
                                    color = Color(0xFF7C4DFF),
                                    trackColor = Color(0xFF1E0D33)
                                )

                                Text("${download.progress}% concluído", color = Color(0xFFB39DDB), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}