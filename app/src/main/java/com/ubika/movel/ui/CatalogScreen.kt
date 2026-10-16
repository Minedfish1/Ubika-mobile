package com.ubika.movel.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ubika.movel.model.Game
import com.ubika.movel.model.GameVersion
import com.ubika.movel.network.UbikaApi
import com.ubika.movel.network.UbikaSocket
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val marbledBackground = Brush.linearGradient(
    colors = listOf(Color(0xFF0A0510), Color(0xFF1E0D33), Color(0xFF0A0510))
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CatalogScreen(soundManager: SoundManager, globalSearchQuery: String) {
    val coroutineScope = rememberCoroutineScope()

    // Estados de Paginação e Dados
    var allGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var isLastPage by remember { mutableStateOf(false) }

    // Estados do Modal
    var expandedGame by remember { mutableStateOf<Game?>(null) }
    var gameToDisplay by remember { mutableStateOf<Game?>(null) }
    var selectedVersion by remember { mutableStateOf<GameVersion?>(null) }

    // Pódio: 3 mais jogados da semana (trending RAWG — traz capas de verdade)
    var trending by remember { mutableStateOf<List<Game>>(emptyList()) }
    LaunchedEffect(Unit) {
        try { trending = UbikaApi.getTrendingGames() } catch (_: Exception) {}
    }

    // Monitor de rolagem
    val listState = rememberLazyListState()

    LaunchedEffect(expandedGame) {
        if (expandedGame != null) gameToDisplay = expandedGame
    }

    // 1. Efeito de Busca (Debounce)
    LaunchedEffect(globalSearchQuery) {
        delay(500) // Aguarda o utilizador parar de digitar por 500ms
        isLoading = true
        page = 1
        isLastPage = false
        try {
            val response = UbikaApi.getCatalogGames(search = globalSearchQuery, page = 1)
            allGames = response.games
            isLastPage = response.isLastPage
        } catch (e: Exception) {
            // Tratar erro
        } finally {
            isLoading = false
        }
    }

    // 2. Detetive de Fim da Lista (Infinite Scroll)
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            // Se o último item visível for um dos 2 últimos da lista, carrega mais
            lastVisibleItem >= totalItems - 2
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isLoadingMore && !isLastPage) {
            isLoadingMore = true
            page++
            try {
                val response = UbikaApi.getCatalogGames(search = globalSearchQuery, page = page)
                allGames = allGames + response.games // Junta os novos jogos à lista existente
                isLastPage = response.isLastPage
            } catch (e: Exception) {
                page-- // Reverte a página se falhar
            } finally {
                isLoadingMore = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(marbledBackground)) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && allGames.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF7C4DFF))
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {

                    // --- PÓDIO: 3 mais jogados da semana (só sem busca ativa) ---
                    if (globalSearchQuery.isBlank() && trending.isNotEmpty()) {
                        item {
                            Column {
                                Text("MAIS JOGADOS DA SEMANA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                PodiumRow(trending.take(3)) { game ->
                                    soundManager.playClickA(); expandedGame = game; selectedVersion = null
                                }
                            }
                        }
                    }

                    // --- CATÁLOGO COMPLETO ---
                    item {
                        Column {
                            Text(if (globalSearchQuery.isNotBlank()) "RESULTADOS DA PESQUISA" else "TODOS OS JOGOS DISPONÍVEIS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            // Os jogos dinâmicos — entrada escalonada (fade + subida)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                itemsIndexed(allGames) { index, game ->
                                    AnimatedEntry(index) {
                                        Column(modifier = Modifier.width(110.dp).clickable { soundManager.playClickA(); expandedGame = game; selectedVersion = null }) {
                                            GameCover(game, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)))
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(game.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- INDICADOR DE CARREGAMENTO NO FINAL DA LISTA ---
                    if (isLoadingMore) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF7C4DFF), modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(100.dp)) }
                }
            }
        }

        // --- O RESTO DO CÓDIGO DO MODAL FICA AQUI EXATAMENTE COMO ESTAVA ---
        // (O código do AnimatedVisibility e do painel expandido mantém-se igual)
        AnimatedVisibility(
            visible = expandedGame != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.9f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.9f)
        ) {
            gameToDisplay?.let { game ->
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { expandedGame = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.70f).border(1.dp, Color(0xFF512DA8).copy(alpha = 0.6f), RoundedCornerShape(24.dp)).clickable(enabled = false) {},
                        shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0510))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF180A2B), Color(0xFF0A0510)))).padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(game.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1)
                                TextButton(onClick = { expandedGame = null }, contentPadding = PaddingValues(0.dp)) {
                                    Text("✕ FECHAR", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                GameCover(game, Modifier.weight(0.9f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF4A148C), RoundedCornerShape(12.dp)))

                                Box(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF140822), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF311B52), RoundedCornerShape(12.dp)).padding(16.dp)) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text("Requisitos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val reqs = listOf("SO: Win 10/11", "RAM: 8/16 GB", "DirectX: 12", "Arm: 70GB", "GPU Dedicada", "CPU: Quad-Core")
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            Column(modifier = Modifier.weight(1f)) { reqs.take(3).forEach { req -> Text("• $req", color = Color(0xFFB39DDB), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp)) } }
                                            Column(modifier = Modifier.weight(1f)) { reqs.drop(3).forEach { req -> Text("• $req", color = Color(0xFFB39DDB), fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp)) } }
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1.2f).fillMaxHeight().background(Color(0xFF140822), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF311B52), RoundedCornerShape(12.dp)).padding(16.dp)) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text("Versões / Fontes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(game.versions) { version ->
                                                val isSelected = selectedVersion == version
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0xFF512DA8) else Color(0xFF1E0D33), RoundedCornerShape(8.dp)).border(1.dp, if (isSelected) Color(0xFF9575CD) else Color.Transparent, RoundedCornerShape(8.dp)).clickable { selectedVersion = version }.padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(version.source, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text(version.size, color = Color(0xFFD1C4E9), fontSize = 11.sp)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val btnText = if (selectedVersion != null) "Instalar" else "Selecione versão"
                                        val btnColor = if (selectedVersion != null) Color.White else Color.Gray

                                        Button(
                                            onClick = {
                                                selectedVersion?.let { ver ->
                                                    soundManager.playClickA()
                                                    coroutineScope.launch { UbikaSocket.sendInstallCommand(game.title, ver.magnet) }
                                                    expandedGame = null
                                                }
                                            },
                                            enabled = selectedVersion != null, modifier = Modifier.fillMaxWidth().height(42.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7), disabledContainerColor = Color(0xFF1E0D33)),
                                            shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(text = btnText, color = btnColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

// ------------------------- Pódio / animações -------------------------

private val medalByRank = mapOf(
    1 to Color(0xFFFFD700), // ouro
    2 to Color(0xFFC0C0C0), // prata
    3 to Color(0xFFCD7F32), // bronze
)

/** Pódio: prata à esquerda, ouro no centro (maior), bronze à direita — sobe escalonado. */
@Composable
private fun PodiumRow(games: List<Game>, onSelect: (Game) -> Unit) {
    data class Slot(val game: Game, val rank: Int)
    val slots = listOfNotNull(
        games.getOrNull(1)?.let { Slot(it, 2) },
        games.getOrNull(0)?.let { Slot(it, 1) },
        games.getOrNull(2)?.let { Slot(it, 3) },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        slots.forEachIndexed { order, s ->
            PodiumSlot(s.game, s.rank, order, Modifier.weight(1f)) { onSelect(s.game) }
        }
    }
}

@Composable
private fun PodiumSlot(game: Game, rank: Int, order: Int, modifier: Modifier, onClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(order * 180L); visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(500), label = "pod_a")
    val rise by animateDpAsState(if (visible) 0.dp else 44.dp, tween(500), label = "pod_r")

    val medal = medalByRank[rank] ?: Color.Gray
    val coverH = when (rank) { 1 -> 148.dp; 2 -> 116.dp; else -> 102.dp }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .graphicsLayer { this.alpha = alpha; translationY = rise.toPx() }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF140822))
            .border(1.5.dp, medal.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Text("${rank}º", color = medal, fontSize = if (rank == 1) 30.sp else 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        GameCover(game, Modifier.fillMaxWidth().height(coverH).clip(RoundedCornerShape(8.dp)))
        Spacer(Modifier.height(6.dp))
        Text(game.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2)
    }
}

/** Capa do jogo; sem URL mostra um bloco gradiente com a inicial — nunca fica cinza. */
@Composable
private fun GameCover(game: Game, modifier: Modifier = Modifier) {
    if (game.coverUrl.isBlank()) {
        Box(
            modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF311B52), Color(0xFF140822)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                game.title.firstOrNull()?.uppercase() ?: "🎮",
                color = Color(0xFF7C4DFF), fontSize = 28.sp, fontWeight = FontWeight.Black
            )
        }
    } else {
        AsyncImage(model = game.coverUrl, contentDescription = game.title, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

/** Entrada escalonada: cada item espera index*45ms e sobe desvanecendo. */
@Composable
private fun AnimatedEntry(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(index.coerceAtMost(14) * 45L); visible = true }
    val alpha by animateFloatAsState(if (visible) 1f else 0f, tween(350), label = "e_a")
    val dy by animateDpAsState(if (visible) 0.dp else 22.dp, tween(350), label = "e_y")
    Box(Modifier.graphicsLayer { this.alpha = alpha; translationY = dy.toPx() }) { content() }
}