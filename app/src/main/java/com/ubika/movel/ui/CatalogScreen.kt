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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.ubika.movel.model.Game
import com.ubika.movel.model.GameVersion
import com.ubika.movel.network.UbikaApi
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
    var totalResults by remember { mutableStateOf(0) }

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
        expandedGame?.let { g ->
            gameToDisplay = g
            // Destaques/trending chegam sem fontes — busca na biblioteca pelo título
            // pra obter as versions baixáveis (senão o botão fica preso em "SELECIONE UMA FONTE")
            if (g.versionsOrEmpty().isEmpty()) {
                try {
                    val title = g.title.replace(Regex("\\s*[\\[(].*"), "").trim()
                    val r = UbikaApi.getCatalogGames(search = title, page = 1)
                    val match = r.games.firstOrNull { it.versionsOrEmpty().isNotEmpty() }
                    if (match != null) {
                        // Mantém capa/descrição do trending, adiciona as fontes da biblioteca
                        gameToDisplay = match.copy(
                            coverUrl = g.coverUrl.ifBlank { match.coverUrl },
                            description = g.description.ifBlank { match.description },
                            requirements = if (g.requirements.isNullOrBlank()) match.requirements else g.requirements
                        )
                    }
                } catch (_: Exception) {}
            }
        }
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
            totalResults = response.total
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

                    // --- MODO DESTAQUES (sem busca): pódio + EM ALTA ---
                    if (globalSearchQuery.isBlank()) {
                        if (trending.isNotEmpty()) {
                            item {
                                Column {
                                    Text("MAIS JOGADOS DA SEMANA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PodiumRow(trending.take(3)) { game ->
                                        soundManager.playClickA(); expandedGame = game; selectedVersion = null
                                    }
                                }
                            }
                            item {
                                Column {
                                    Text("EM ALTA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        itemsIndexed(trending.drop(3)) { index, game ->
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
                        }
                        item {
                            Text(
                                "Use a barra de pesquisa acima para achar qualquer jogo do catálogo.",
                                color = Color.Gray, fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        // --- MODO BUSCA: resultados em lista vertical ---
                        item {
                            Text("RESULTADOS ($totalResults)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        itemsIndexed(allGames) { index, game ->
                            AnimatedEntry(index) {
                                SearchResultRow(game) {
                                    soundManager.playClickA(); expandedGame = game; selectedVersion = null
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

        // Detalhe do jogo — modelo antigo (2 colunas em landscape, feito pra manete):
        // capa à esquerda | título, descrição, requisitos, fontes e BAIXAR à direita
        AnimatedVisibility(
            visible = expandedGame != null,
            enter = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.94f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.94f)
        ) {
            gameToDisplay?.let { game ->
                val context = LocalContext.current
                // Auto-seleciona a primeira fonte baixável — A no controle já baixa direto
                LaunchedEffect(game.id) {
                    if (selectedVersion == null) {
                        selectedVersion = game.versionsOrEmpty().firstOrNull {
                            it.magnetOrEmpty().isNotBlank() || it.linksOrEmpty().isNotEmpty()
                        }
                    }
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)).clickable { expandedGame = null },
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f).border(1.dp, Color(0xFF512DA8).copy(alpha = 0.6f), RoundedCornerShape(20.dp)).clickable(enabled = false) {},
                        shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0510))
                    ) {
                        Row(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF180A2B), Color(0xFF0A0510)))).padding(16.dp)) {

                            // COLUNA ESQUERDA: capa (poster 2:3) — não engole a tela
                            GameCover(game, Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF4A148C), RoundedCornerShape(12.dp)))

                            Spacer(modifier = Modifier.width(16.dp))

                            // COLUNA DIREITA: tudo que interessa + baixar
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(game.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 2, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { soundManager.playClickA(); expandedGame = null }, contentPadding = PaddingValues(0.dp)) {
                                        Text("✕", color = Color(0xFFB39DDB), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // descrição + requisitos (rolagem)
                                    Column(modifier = Modifier.weight(1.1f).verticalScroll(rememberScrollState())) {
                                        Text("Sobre", color = Color(0xFFB39DDB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            game.description.take(400),
                                            color = Color.White, fontSize = 11.sp, lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Requisitos", color = Color(0xFFB39DDB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(game.requirementsOrDefault(), color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                                    }

                                    // fontes (rolagem, foco p/ manete)
                                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                                        Text("FONTE", color = Color(0xFFB39DDB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        game.versionsOrEmpty().forEach { version ->
                                            val isSelected = selectedVersion == version
                                            val hasMagnet = version.magnetOrEmpty().isNotBlank()
                                            var focused by remember { mutableStateOf(false) }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) Color(0xFF512DA8) else Color(0xFF140822))
                                                    .border(
                                                        if (isSelected || focused) 2.dp else 1.dp,
                                                        if (isSelected) Color(0xFF9575CD) else if (focused) Color(0xFFB39DDB) else Color(0xFF311B52),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .onFocusChanged { focused = it.isFocused }
                                                    .focusable()
                                                    .clickable { soundManager.playClickA(); selectedVersion = version }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(version.source.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                                    Text(if (hasMagnet) "TORRENT" else "DIRETO", color = Color(0xFFB39DDB), fontSize = 9.sp)
                                                }
                                                Text(version.size, color = Color(0xFFD1C4E9), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                val sel = selectedVersion
                                var sending by remember { mutableStateOf(false) }
                                val canDownload = sel != null && !sending && (sel.magnetOrEmpty().isNotBlank() || sel.linksOrEmpty().isNotEmpty())
                                Button(
                                    onClick = {
                                        sel?.let { ver ->
                                            soundManager.playClickA()
                                            sending = true
                                            coroutineScope.launch {
                                                // HTTP via VPS — chega ao PC mesmo com o socket do app offline
                                                try {
                                                    val r = UbikaApi.sendPcCommand("iniciar_download", mapOf(
                                                        "gameName" to game.title,
                                                        "magnetLink" to ver.magnetOrEmpty(),
                                                        "link" to (ver.linksOrEmpty().firstOrNull() ?: ""),
                                                        "coverUrl" to game.coverUrl,
                                                        "description" to game.description
                                                    ))
                                                    if (r.bool("ok")) {
                                                        android.widget.Toast.makeText(context, "Download iniciado no PC — veja na aba Downloads", android.widget.Toast.LENGTH_LONG).show()
                                                        expandedGame = null
                                                    } else {
                                                        android.widget.Toast.makeText(context, "PC recusou: ${r.str("error") ?: "?"}", android.widget.Toast.LENGTH_LONG).show()
                                                    }
                                                } catch (e: Exception) {
                                                    android.widget.Toast.makeText(context, "Falha: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                                }
                                                sending = false
                                            }
                                        }
                                    },
                                    enabled = canDownload,
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF), disabledContainerColor = Color(0xFF1E0D33)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        if (sending) "ENVIANDO AO PC…" else if (sel == null) "SELECIONE UMA FONTE" else "BAIXAR NO PC — ${sel.size}",
                                        color = if (canDownload) Color.White else Color.Gray,
                                        fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                                    )
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

/** Capa do jogo; sem URL ou falha de carga mostra um bloco gradiente com a inicial — nunca cinza. */
@Composable
private fun GameCover(game: Game, modifier: Modifier = Modifier) {
    @Composable
    fun Placeholder() {
        Box(
            modifier = modifier.background(Brush.linearGradient(listOf(Color(0xFF311B52), Color(0xFF140822)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                game.title.firstOrNull()?.uppercase() ?: "🎮",
                color = Color(0xFF7C4DFF), fontSize = 28.sp, fontWeight = FontWeight.Black
            )
        }
    }
    if (game.coverUrl.isBlank()) { Placeholder(); return }
    coil.compose.SubcomposeAsyncImage(
        model = game.coverUrl,
        contentDescription = game.title,
        contentScale = ContentScale.Crop,
        modifier = modifier
    ) {
        when (painter.state) {
            is coil.compose.AsyncImagePainter.State.Error -> Placeholder()
            is coil.compose.AsyncImagePainter.State.Loading -> Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF140822)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color(0xFF7C4DFF), modifier = Modifier.size(22.dp), strokeWidth = 2.dp) }
            else -> SubcomposeAsyncImageContent()
        }
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
/** Linha de resultado da busca: capa + título + chips de fonte + tamanho. */
@Composable
private fun SearchResultRow(game: Game, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF140822))
            .border(1.dp, Color(0xFF311B52), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GameCover(game, Modifier.size(52.dp, 68.dp).clip(RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f)) {
            Text(game.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                game.versionsOrEmpty().take(3).forEach { v ->
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF311B52)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(v.source, color = Color(0xFFB39DDB), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (game.versionsOrEmpty().size > 3) Text("+${game.versionsOrEmpty().size - 3}", color = Color.Gray, fontSize = 9.sp)
            }
        }
        Text(game.versionsOrEmpty().firstOrNull()?.size ?: "", color = Color(0xFFB39DDB), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
