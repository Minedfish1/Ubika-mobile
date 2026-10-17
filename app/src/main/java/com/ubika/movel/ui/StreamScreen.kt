package com.ubika.movel.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.network.UbikaApi
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tela de Stream: status do PC host, diagnóstico Sunshine/VDD,
 * preferências de sessão e botão que abre o Moonlight no host certo.
 */
@Composable
fun StreamScreen(soundManager: SoundManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("ubika_stream", Context.MODE_PRIVATE) }

    var pcOnline by remember { mutableStateOf(false) }
    var streamHost by remember { mutableStateOf("") }
    var streamHostV6 by remember { mutableStateOf("") }
    var lanHost by remember { mutableStateOf("") }
    var portsOpen by remember { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var diag by remember { mutableStateOf<Map<String, String>?>(null) }
    var speedResult by remember { mutableStateOf<String?>(null) }

    // Preferências persistidas localmente (o Moonlight aplica de fato)
    var resolution by remember { mutableStateOf(prefs.getString("resolution", "Nativo") ?: "Nativo") }
    var fps by remember { mutableIntStateOf(prefs.getInt("fps", 60)) }
    var bitrate by remember { mutableIntStateOf(prefs.getInt("bitrate", 20)) }

    LaunchedEffect(Unit) {
        while (true) {
            val st = UbikaApi.getPcStatus()
            pcOnline = st.online
            streamHost = st.streamHost
            streamHostV6 = st.streamHostV6
            lanHost = st.lanHost
            portsOpen = st.portsOpen
            delay(5000)
        }
    }

    fun launchMoonlight() {
        soundManager.playClickA()
        // IPv6 público vence: conecta de fora sem depender de UPnP/CGNAT.
        // Depois IPv4 público; lanHost só alcança na mesma rede — último recurso.
        val host = when {
            streamHostV6.isNotBlank() -> "[$streamHostV6]"
            streamHost.isNotBlank() -> streamHost
            else -> lanHost
        }
        if (!pcOnline || host.isBlank()) {
            feedback = "PC offline ou sem IP de stream — inicie o daemon no PC."
            return
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("moonlight://$host")))
            feedback = "Abrindo Moonlight em $host…"
        } catch (e: ActivityNotFoundException) {
            val moonlight = context.packageManager.getLaunchIntentForPackage("com.limelight")
            if (moonlight != null) {
                context.startActivity(moonlight)
                feedback = "Abra o PC $host dentro do Moonlight."
            } else {
                feedback = "Moonlight não instalado. Instale pela Play Store e pareie com $host."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---- Status do PC ----
        StreamCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (pcOnline) Color(0xFF66BB6A) else Color(0xFF616161))
                )
                Column {
                    Text(
                        if (pcOnline) "PC ONLINE" else "PC OFFLINE",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black
                    )
                    Text(
                        if (streamHost.isNotBlank() || streamHostV6.isNotBlank())
                            "Host: ${if (streamHostV6.isNotBlank()) "[$streamHostV6] (IPv6 direto)" else streamHost}"
                        else "Inicie o daemon no PC (UbikaPC.exe)",
                        color = Color.Gray, fontSize = 12.sp
                    )
                    if (portsOpen != null) {
                        Text(
                            if (portsOpen == true) "Acesso externo: portas abertas ✓"
                            else "Acesso externo: portas fechadas — ISP com CGNAT ou UPnP desligado",
                            color = if (portsOpen == true) Color(0xFF66BB6A) else Color(0xFFEF5350),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // ---- Ações ----
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StreamButton(
                text = "JOGAR",
                enabled = pcOnline,
                primary = true,
                modifier = Modifier.weight(1f)
            ) { launchMoonlight() }

            StreamButton(
                text = if (busy == "status") "VERIFICANDO…" else "DIAGNÓSTICO",
                enabled = pcOnline && busy == null,
                modifier = Modifier.weight(1f)
            ) {
                soundManager.playClickA()
                scope.launch {
                    busy = "status"; feedback = null
                    try {
                        val r = UbikaApi.sendPcCommand("status_stream")
                        val s = r.obj?.getAsJsonObject("stream")
                        diag = mapOf(
                            "Sunshine instalado" to if (s?.get("sunshineInstalled")?.asBoolean == true) "OK" else "NÃO",
                            "Sunshine rodando" to if (s?.get("sunshineRunning")?.asBoolean == true) "OK" else "NÃO",
                            "VDD instalado" to if (s?.get("vddInstalled")?.asBoolean == true) "OK" else "NÃO",
                            "Display dinâmico" to if (s?.get("prepConfigured")?.asBoolean == true) "OK" else "NÃO",
                            "Resolução VDD" to (s?.get("ddResolutionOption")?.asString ?: "—")
                        )
                    } catch (e: Exception) {
                        feedback = e.message
                    } finally { busy = null }
                }
            }

            StreamButton(
                text = if (busy == "setup") "CONFIGURANDO…" else "CONFIGURAR PC",
                enabled = pcOnline && busy == null,
                modifier = Modifier.weight(1f)
            ) {
                soundManager.playClickA()
                scope.launch {
                    busy = "setup"; feedback = null
                    try {
                        val r = UbikaApi.sendPcCommand("instalar_stream_setup")
                        val steps = r.obj?.getAsJsonArray("steps")?.joinToString("\n") { "• ${it.asString}" }
                        feedback = if (r.bool("ok")) "Setup aplicado. Reinicie o Sunshine." else (steps ?: r.str("error") ?: "Falha no setup")
                    } catch (e: Exception) {
                        feedback = e.message
                    } finally { busy = null }
                }
            }
        }

        // ---- Pareamento: código do painel do UbikaPC.exe ----
        StreamCard {
            Text("PAREAR PC", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Digite o código que aparece no painel do PC (troca a cada ~10min)", color = Color.Gray, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            PairRow(soundManager)
        }

        feedback?.let {
            Text(it, color = Color(0xFFB39DDB), fontSize = 13.sp)
        }

        diag?.let { d ->
            StreamCard {
                Text("DIAGNÓSTICO DO STREAM", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                d.forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(k, color = Color.White, fontSize = 13.sp)
                        Text(v, color = if (v == "OK") Color(0xFF66BB6A) else if (v == "NÃO") Color(0xFFEF5350) else Color.Gray, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                StreamButton(
                    text = if (busy == "speed") "TESTANDO… (~25 MB)" else "TESTAR VELOCIDADE",
                    enabled = pcOnline && busy == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    soundManager.playClickA()
                    scope.launch {
                        busy = "speed"; speedResult = null
                        try {
                            val r = UbikaApi.sendPcCommand("speed_test")
                            speedResult = if (r.bool("ok")) {
                                "⬇ ${r.obj?.get("mbps")?.asString} Mbps  ·  ping ${r.obj?.get("pingMs")?.asString}ms"
                            } else "Falhou: ${r.obj?.get("detail")?.asString ?: r.str("error") ?: "erro"}"
                        } catch (e: Exception) {
                            speedResult = "Falhou: ${e.message}"
                        } finally { busy = null }
                    }
                }
                speedResult?.let {
                    Text(it, color = Color(0xFFD1C4E9), fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        // ---- Preferências de sessão ----
        StreamCard {
            Text("PREFERÊNCIAS DE STREAM", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Text("Resolução", color = Color.White, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("Nativo", "1080p", "720p").forEach { opt ->
                    PrefChip(opt, opt == resolution) {
                        resolution = opt; prefs.edit().putString("resolution", opt).apply(); soundManager.playClickA()
                    }
                }
            }

            Text("Taxa de quadros", color = Color.White, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf(30, 60, 120).forEach { opt ->
                    PrefChip("${opt}fps", opt == fps) {
                        fps = opt; prefs.edit().putInt("fps", opt).apply(); soundManager.playClickA()
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Bitrate", color = Color.White, fontSize = 13.sp)
                Text("$bitrate Mbps", color = Color(0xFF7C4DFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = bitrate.toFloat(),
                onValueChange = { bitrate = it.toInt() },
                onValueChangeFinished = { prefs.edit().putInt("bitrate", bitrate).apply() },
                valueRange = 5f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF7C4DFF),
                    activeTrackColor = Color(0xFF7C4DFF),
                    inactiveTrackColor = Color(0xFF311B52)
                )
            )
            Text(
                "Resolução/bitrate são aplicados nas opções do Moonlight; o display virtual do PC segue a tela do celular automaticamente.",
                color = Color.Gray, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StreamCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF140822))
            .border(1.dp, Color(0xFF311B52), RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun StreamButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !enabled -> Color(0xFF1A1025)
                    primary -> Color(0xFF7C4DFF)
                    else -> Color(0xFF311B52)
                }
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = Color(0xFFB39DDB),
                shape = RoundedCornerShape(12.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable(enabled = enabled)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PrefChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF1A1025))
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Color(0xFFB39DDB) else Color(0xFF311B52),
                shape = RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.White else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Pareamento dentro do app: digita o código que o PC mostra no painel. */
@Composable
private fun PairRow(soundManager: SoundManager) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    var ok by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("PAREAR PC", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadField(
                value = code,
                onValue = { code = it },
                hint = "000000",
                digitsOnly = true,
                maxLen = 6,
                modifier = Modifier.weight(1f).height(52.dp)
            )
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (code.length == 6 && !busy) Color(0xFF7C4DFF) else Color(0xFF2A1A3E))
                    .clickable(enabled = code.length == 6 && !busy) {
                        soundManager.playClickA()
                        busy = true; msg = null
                        scope.launch {
                            try {
                                val r = UbikaApi.pairPc(code)
                                ok = r.bool("ok")
                                msg = if (ok) "PC pareado: ${r.str("deviceId") ?: "PC"}"
                                      else r.str("error") ?: "Falha no pareamento"
                            } catch (e: Exception) {
                                ok = false
                                msg = when {
                                    e.message?.contains("CODIGO") == true -> "Código inválido ou expirado."
                                    e.message?.contains("DESCONECTOU") == true -> "O PC desconectou."
                                    else -> e.message
                                }
                            } finally { busy = false }
                        }
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (busy) "…" else "PAREAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
        msg?.let {
            Text(it, color = if (ok) Color(0xFF66BB6A) else Color(0xFFEF5350), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

