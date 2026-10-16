package com.ubika.movel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ubika.movel.model.Game
import com.ubika.movel.model.GameVersion
import com.ubika.movel.network.UbikaSocket
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.launch

@Composable
fun GameDetailScreen(
    game: Game,
    soundManager: SoundManager,
    onBack: () -> Unit
) {
    var selectedVersion by remember { mutableStateOf<GameVersion?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(marbledBackground)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- BARRA SUPERIOR (BOTÃO VOLTAR + TÍTULO) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = {
                        soundManager.playClickA()
                        onBack()
                    },
                    modifier = Modifier
                        .background(Color(0xFF140822), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF311B52), RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text(
                    text = game.title.uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // --- CORPO DA PÁGINA (LAYOUT EM DUAS COLUNAS PRINCIPAIS) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // COLUNA ESQUERDA: Capa Oficial do Jogo
                AsyncImage(
                    model = game.coverUrl,
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF512DA8), RoundedCornerShape(16.dp))
                )

                // COLUNA DIREITA: Descrição, Requisitos e Fontes/Download
                Column(
                    modifier = Modifier
                        .weight(1.5f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Descrição à Direita do Topo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF140822), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF311B52), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("Sobre o Jogo", color = Color(0xFFB39DDB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (game.description.isNotBlank()) game.description else "Nenhuma descrição fornecida para este título.",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // 2. Requisitos Embaixo
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color(0xFF140822), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF311B52), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("Requisitos do Sistema", color = Color(0xFFB39DDB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = game.requirements,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // 3. Fontes / Versões com Botão de Download na Base
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .background(Color(0xFF140822), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF311B52), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fontes e Versões Disponíveis", color = Color(0xFFB39DDB), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))

                            // Lista de Versões
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(game.versions) { version ->
                                    val isSelected = selectedVersion == version
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) Color(0xFF512DA8) else Color(0xFF1E0D33), RoundedCornerShape(8.dp))
                                            .border(1.dp, if (isSelected) Color(0xFF9575CD) else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedVersion = version }
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(version.source, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            val tipo = if (version.magnet.isNotBlank()) "TORRENT" else if (version.links.isNotEmpty()) "DIRETO" else "INDISP."
                                            Box(modifier = Modifier.background(Color(0xFF311B52), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                Text(tipo, color = Color(0xFFB39DDB), fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                        Text(version.size, color = Color(0xFFD1C4E9), fontSize = 11.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Botão de Download / Instalação (Dispara via Socket.io para o PC)
                            val downloadable = selectedVersion?.let { it.magnet.isNotBlank() || it.links.isNotEmpty() } == true
                            val btnText = if (downloadable) "INICIAR DOWNLOAD (${selectedVersion!!.size})" else "SELECIONE UMA VERSÃO"
                            val btnColor = if (downloadable) Color(0xFF673AB7) else Color(0xFF1E0D33)

                            Button(
                                onClick = {
                                    selectedVersion?.let { ver ->
                                        soundManager.playClickA()
                                        coroutineScope.launch {
                                            // Dispara a ordem de download para o servidor da AWS / Daemon do PC
                                            UbikaSocket.sendInstallCommand(game.title, ver.magnet, ver.links.firstOrNull() ?: "")
                                        }
                                    }
                                },
                                enabled = downloadable,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(45.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = btnColor,
                                    disabledContainerColor = Color(0xFF140822)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = btnText,
                                    color = if (selectedVersion != null) Color.White else Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}