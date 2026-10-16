package com.ubika.movel.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ubika.movel.model.Game
import com.ubika.movel.network.UbikaApi
import com.ubika.movel.network.UbikaSocket
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainConsoleScreen() {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("Biblioteca") }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    var isSearchActive by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }
    var selectedGameForDetail by remember { mutableStateOf<Game?>(null) }

    LaunchedEffect(Unit) {
        UbikaSocket.connect()
        while (true) { delay(1000); currentTime = LocalTime.now() }
    }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    if (selectedGameForDetail != null) {
        GameDetailScreen(
            game = selectedGameForDetail!!,
            soundManager = soundManager,
            onBack = { selectedGameForDetail = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(drawerContainerColor = Color(0xFF140822), modifier = Modifier.width(260.dp)) {
                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Box(modifier = Modifier.background(Color(0xFF673AB7), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text("U", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("MENU", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    DrawerItem("Biblioteca", Icons.Default.LibraryBooks, currentScreen) { currentScreen = "Biblioteca"; scope.launch { drawerState.close() }; soundManager.playClickA() }
                    DrawerItem("Catálogo", Icons.Default.Store, currentScreen) { currentScreen = "Catálogo"; scope.launch { drawerState.close() }; soundManager.playClickA() }
                    DrawerItem("Downloads", Icons.Default.Download, currentScreen) { currentScreen = "Downloads"; scope.launch { drawerState.close() }; soundManager.playClickA() }
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize().background(marbledBackground)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "search_anim"
                    ) { searchActive ->
                        if (searchActive) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = globalSearchQuery,
                                    onValueChange = { globalSearchQuery = it },
                                    placeholder = { Text("Pesquisar...", color = Color.Gray) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF140822), unfocusedContainerColor = Color(0xFF140822),
                                        focusedBorderColor = Color(0xFF7C4DFF), unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = Color.White, unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(50.dp)
                                )
                                IconButton(onClick = { isSearchActive = false; globalSearchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fechar Pesquisa", tint = Color.White)
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(currentScreen.uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    IconButton(onClick = { isSearchActive = true }) {
                                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White, modifier = Modifier.size(24.dp))
                                    }
                                    Text(currentTime.format(timeFormatter), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    AsyncImage(model = "https://i.imgur.com/6V7z8zG.png", contentDescription = "Perfil", contentScale = ContentScale.Crop, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "screen_trans") { targetScreen ->
                        when (targetScreen) {
                            "Biblioteca" -> LibraryHubContent(soundManager, onGameSelected = { game -> selectedGameForDetail = game })
                            "Catálogo" -> CatalogScreen(soundManager, globalSearchQuery)
                            "Downloads" -> DownloadsScreen(soundManager)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF0A0510)).padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.clickable { soundManager.playSlideRB(); scope.launch { drawerState.open() } }) {
                        Box(modifier = Modifier.background(Color(0xFF673AB7), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                            Text("U", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                        Text("MENU", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ActionPrompt("≡", "OPÇÕES")
                        ActionPrompt("A", "JOGAR")
                        ActionPrompt("B", "VOLTAR")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryHubContent(soundManager: SoundManager, onGameSelected: (Game) -> Unit) {
    var trendingGames by remember { mutableStateOf<List<Game>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var heroGame by remember { mutableStateOf<Game?>(null) }

    LaunchedEffect(Unit) {
        try {
            val games = UbikaApi.getTrendingGames()
            trendingGames = games
            if (games.isNotEmpty()) {
                heroGame = games[0]
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C4DFF))
            }
        } else if (trendingGames.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum destaque encontrado na RAWG.", color = Color.Gray, fontSize = 14.sp)
            }
        } else {
            val currentHero = heroGame ?: trendingGames[0]

            Crossfade(targetState = currentHero.coverUrl, animationSpec = tween(800), label = "") { coverUrl ->
                AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().alpha(0.2f))
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF0A0510)))))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Column {
                        Text("DESTAQUES DA RAWG", color = Color(0xFFB39DDB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedContent(
                            targetState = currentHero,
                            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                            label = "hero_anim"
                        ) { activeGame ->
                            Box(
                                modifier = Modifier
                                    .width(300.dp).height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(2.dp, Color(0xFF512DA8), RoundedCornerShape(12.dp))
                                    .clickable {
                                        soundManager.playClickA()
                                        onGameSelected(activeGame)
                                    }
                            ) {
                                AsyncImage(model = activeGame.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))).padding(16.dp), contentAlignment = Alignment.BottomStart) {
                                    Text(activeGame.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                item {
                    Column {
                        Text("OUTROS DESTAQUES", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            trendingGames.forEach { game ->
                                if (game.id != currentHero.id) {
                                    AsyncImage(
                                        model = game.coverUrl, contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.width(120.dp).height(160.dp).clip(RoundedCornerShape(8.dp)).clickable { soundManager.playClickA(); heroGame = game }
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

@Composable
fun DrawerItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentScreen: String, onClick: () -> Unit) {
    val isSelected = currentScreen == title
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(8.dp)).background(if (isSelected) Color(0xFF311B52) else Color.Transparent).clickable { onClick() }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, contentDescription = title, tint = if (isSelected) Color(0xFFB39DDB) else Color.Gray)
        Text(title, color = if (isSelected) Color.White else Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionPrompt(iconLabel: String, actionText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.size(26.dp).background(Color(0xFF1E0D33), if (iconLabel == "≡") RoundedCornerShape(50) else CircleShape).border(1.dp, Color(0xFF512DA8), if (iconLabel == "≡") RoundedCornerShape(50) else CircleShape), contentAlignment = Alignment.Center) {
            Text(text = iconLabel, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Text(text = actionText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}