package com.ubika.movel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.ubika.movel.network.UbikaSocket
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DownloadItem(
    val id: String,
    val gameName: String,
    var progress: Float, // 0.0 a 100.0
    var speed: String,
    var state: String,
    var peers: Int = 0,
    var installProgress: Float = -1f // -1 = indeterminado
)

private fun JSONObject.toDownloadItem(existing: DownloadItem? = null): DownloadItem {
    val downSpeed = optLong("downSpeed", 0)
    val speedStr = when {
        downSpeed >= 1_000_000 -> "%.1f MB/s".format(downSpeed / 1_000_000f)
        downSpeed >= 1_000 -> "%.0f KB/s".format(downSpeed / 1_000f)
        else -> "0 KB/s"
    }
    return DownloadItem(
        id = optString("id", existing?.id ?: ""),
        gameName = optString("gameName", existing?.gameName ?: "download"),
        progress = optDouble("progress", 0.0).toFloat(),
        speed = speedStr,
        state = optString("state", existing?.state ?: "downloading"),
        peers = optInt("peers", existing?.peers ?: 0),
        installProgress = optDouble("installProgress", -1.0).toFloat()
    )
}

private fun isDone(state: String) = state == "done" || state == "finished" || state == "ready"
private fun isTerminalOk(state: String) = state == "ready"

private fun stateLabel(state: String) = when (state) {
    "fetching-metadata" -> "Buscando metadados..."
    "downloading" -> "Baixando"
    "paused" -> "Pausado"
    "installing" -> "Instalando"
    "install_error" -> "Falha na instalação"
    "ready" -> "Pronto p/ jogar"
    "done", "finished" -> "Concluído"
    "error" -> "Erro"
    "removed" -> "Removido"
    else -> state
}

@Composable
fun DownloadsScreen(soundManager: SoundManager) {
    val activeDownloads = remember { mutableStateListOf<DownloadItem>() }
    val scope = rememberCoroutineScope()

    fun upsert(json: JSONObject) {
        val id = json.optString("id")
        if (id.isBlank()) return
        val idx = activeDownloads.indexOfFirst { it.id == id }
        val old = activeDownloads.getOrNull(idx)
        val item = json.toDownloadItem(old)
        // Transição para concluído/instalado → som + notificação do sistema
        if (item.state == "ready" && old?.state != "ready") {
            soundManager.playConfirm()
            soundManager.showSystemNotification("Jogo pronto p/ jogar", item.gameName)
        } else if (item.state == "done" && old != null && old.state != "done" && old.state != "ready") {
            soundManager.playNotification()
            soundManager.showSystemNotification("Download concluído — instalando", item.gameName)
        }
        if (item.state == "removed") {
            if (idx >= 0) activeDownloads.removeAt(idx)
        } else if (idx >= 0) {
            activeDownloads[idx] = item
        } else {
            activeDownloads.add(item)
        }
    }

    DisposableEffect(Unit) {
        // Escuta progresso vindo do PC via VPS
        UbikaSocket.setDownloadProgressListener { json ->
            scope.launch(Dispatchers.Main) { upsert(json) }
        }
        // Pede o estado atual ao conectar na tela
        UbikaSocket.requestDownloads { ok, resp ->
            val arr = resp?.optJSONArray("downloads")
            if (ok && arr != null) {
                scope.launch(Dispatchers.Main) {
                    for (i in 0 until arr.length()) upsert(arr.getJSONObject(i))
                }
            }
        }
        onDispose { UbikaSocket.setDownloadProgressListener(null) }
    }

    Box(modifier = Modifier.fillMaxSize().background(marbledBackground).padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("DOWNLOADS EM TEMPO REAL", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)

            if (activeDownloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhum download ativo no momento.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(activeDownloads, key = { it.id }) { download ->
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

                                if (download.state == "installing" && download.installProgress < 0f) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = Color(0xFFFFB300),
                                        trackColor = Color(0xFF1E0D33)
                                    )
                                } else {
                                    val shown = if (download.state == "installing") download.installProgress else download.progress
                                    LinearProgressIndicator(
                                        progress = { (shown / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                        color = when {
                                            download.state == "installing" -> Color(0xFFFFB300)
                                            isDone(download.state) -> Color(0xFF00C853)
                                            download.state == "install_error" -> Color(0xFFE53935)
                                            else -> Color(0xFF7C4DFF)
                                        },
                                        trackColor = Color(0xFF1E0D33)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        when {
                                            download.state == "installing" ->
                                                if (download.installProgress >= 0f) "Instalando — ${download.installProgress.toInt()}%"
                                                else "Instalando (setup)…"
                                            download.state == "install_error" -> "Falha na instalação — veja o PC"
                                            download.state == "ready" -> "Pronto p/ jogar ✓"
                                            else -> "${stateLabel(download.state)} — ${download.progress}% • ${download.peers} peers"
                                        },
                                        color = Color(0xFFB39DDB), fontSize = 11.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (download.state == "downloading" || download.state == "fetching-metadata") {
                                            DownloadAction("PAUSAR") {
                                                UbikaSocket.downloadAction("pausar_download", download.id)
                                            }
                                        }
                                        if (download.state == "paused") {
                                            DownloadAction("RETOMAR") {
                                                UbikaSocket.downloadAction("retomar_download", download.id)
                                            }
                                        }
                                        if (download.state == "paused" || isDone(download.state) || download.state == "error" || download.state == "install_error") {
                                            DownloadAction("REMOVER") {
                                                UbikaSocket.downloadAction("remover_download", download.id)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = Color(0xFFB39DDB),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .border(1.dp, Color(0xFF512DA8), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
