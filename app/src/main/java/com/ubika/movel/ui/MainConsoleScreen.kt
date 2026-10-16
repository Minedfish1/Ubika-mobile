package com.ubika.movel.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ubika.movel.model.Game
import com.ubika.movel.network.UbikaApi
import com.ubika.movel.network.UbikaSocket
import com.ubika.movel.utils.GamepadManager
import com.ubika.movel.utils.SoundManager
import com.ubika.movel.utils.rememberPadType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainConsoleScreen(onLogout: () -> Unit = {}) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf("Biblioteca") }
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    var isSearchActive by remember { mutableStateOf(false) }
    var globalSearchQuery by remember { mutableStateOf("") }
    var selectedGameForDetail by remember { mutableStateOf<Game?>(null) }

    // Perfil / plano
    val prefs = remember { context.getSharedPreferences("ubika_prefs", Context.MODE_PRIVATE) }
    var showProfile by remember { mutableStateOf(false) }
    var avatarPath by remember { mutableStateOf(prefs.getString("avatar_path", "") ?: "") }
    var avatarUrl by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf(prefs.getString("user_name", "") ?: "") }
    var plan by remember { mutableStateOf("") }
    var planExpiresAt by remember { mutableStateOf(0L) }
    var renewSid by remember { mutableStateOf<String?>(null) }

    val padType by rememberPadType()
    val glyphs = GamepadManager.glyphsFor(padType)

    LaunchedEffect(Unit) {
        UbikaSocket.connect(UbikaApi.userId)
        realDeviceRes(context)?.let { (w, h, fps) -> UbikaSocket.setClientResolution(w, h, fps) }
        val me = UbikaApi.getMe()
        if (me.effectivePlan.isNotEmpty()) { plan = me.effectivePlan; planExpiresAt = me.planExpiresAt }
        if (me.name.isNotEmpty()) { userName = me.name; prefs.edit().putString("user_name", me.name).apply() }
        if (me.avatarUrl.isNotEmpty()) avatarUrl = "${me.avatarUrl}?v=${System.currentTimeMillis() / 60000}"
        while (true) { delay(1000); currentTime = LocalTime.now() }
    }

    // Som suave ao trocar de tela (não toca na primeira composição)
    var firstScreen by remember { mutableStateOf(true) }
    LaunchedEffect(currentScreen) {
        if (firstScreen) { firstScreen = false } else soundManager.playTransition()
    }

    // Polling de renovação — se o usuário pagou no navegador, vira pro sozinho
    LaunchedEffect(renewSid) {
        val sid = renewSid ?: return@LaunchedEffect
        while (true) {
            delay(4000)
            if (UbikaApi.checkoutStatus(sid) == "paid") {
                val me = UbikaApi.getMe()
                plan = me.effectivePlan; planExpiresAt = me.planExpiresAt
                renewSid = null
                break
            }
        }
    }

    // App sai do background → socket morreu → reconecta e reanuncia resolução
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                UbikaSocket.connect(UbikaApi.userId)
                UbikaSocket.announceClient()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    if (selectedGameForDetail != null) {
        GameDetailScreen(
            game = selectedGameForDetail!!,
            soundManager = soundManager,
            onBack = { selectedGameForDetail = null }
        )
    } else {
        Box(Modifier.fillMaxSize()) {
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
                    DrawerItem("Stream", Icons.Default.SportsEsports, currentScreen) { currentScreen = "Stream"; scope.launch { drawerState.close() }; soundManager.playClickA() }
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFF7C4DFF), CircleShape)
                                            .clickable { soundManager.playClickA(); showProfile = true }
                                    ) {
                                        AvatarImage(avatarPath, avatarUrl, Modifier.fillMaxSize())
                                    }
                                }
                            }
                        }
                    }
                }

                // Banner de plano: expirado / expira em breve / trial
                val renewAction: () -> Unit = {
                    scope.launch {
                        val r = UbikaApi.createCheckout()
                        if (r.url.isNotEmpty()) {
                            renewSid = r.sid
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) } catch (_: Exception) {}
                        }
                    }
                }
                val daysLeft = if (planExpiresAt > 0) ((planExpiresAt - System.currentTimeMillis()) / 86400000f).toInt().coerceAtLeast(0) else -1
                when {
                    plan == "expired" -> RenewBanner("SEU PLANO EXPIROU — TOQUE PARA RENOVAR", Color(0xFF5B1B2E), renewAction)
                    plan == "pro" && daysLeft in 0..5 -> RenewBanner("UBIKA PRO EXPIRA EM ${daysLeft}d — TOQUE PARA RENOVAR", Color(0xFF311B52), renewAction)
                    plan == "trial" -> RenewBanner("TRIAL — TOQUE PARA ASSINAR PRO R$ 19,90", Color(0xFF1E0D33), renewAction)
                }

                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(targetState = currentScreen, transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) }, label = "screen_trans") { targetScreen ->
                        when (targetScreen) {
                            "Biblioteca" -> LibraryHubContent(soundManager, onGameSelected = { game -> selectedGameForDetail = game })
                            "Catálogo" -> CatalogScreen(soundManager, globalSearchQuery)
                            "Downloads" -> DownloadsScreen(soundManager)
                            "Stream" -> StreamScreen(soundManager)
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
                        ActionPrompt(glyphs.menu, "OPÇÕES")
                        ActionPrompt(glyphs.confirm, "JOGAR")
                        ActionPrompt(glyphs.cancel, "VOLTAR")
                    }
                }
            }
        }

        if (showProfile) {
            ProfileSheet(
                soundManager = soundManager,
                avatarPath = avatarPath,
                avatarUrl = avatarUrl,
                userName = userName,
                onAvatar = { path -> avatarPath = path; prefs.edit().putString("avatar_path", path).apply() },
                onName = { n -> userName = n; prefs.edit().putString("user_name", n).apply() },
                plan = plan,
                planExpiresAt = planExpiresAt,
                onClose = { showProfile = false },
                onLogout = {
                    prefs.edit().remove("session_token").remove("user_id").remove("avatar_path").apply()
                    UbikaApi.sessionToken = ""
                    UbikaApi.userId = ""
                    UbikaSocket.disconnect()
                    showProfile = false
                    onLogout()
                }
            )
        }
        }
    }
}

@Composable
private fun RenewBanner(text: String, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(bg).clickable { onClick() }.padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryHubContent(soundManager: SoundManager, onGameSelected: (Game) -> Unit) {
    var installed by remember { mutableStateOf<List<com.ubika.movel.network.InstalledGame>>(emptyList()) }
    var pcOnline by remember { mutableStateOf<Boolean?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            val st = UbikaApi.getPcStatus()
            pcOnline = st.online
            if (st.online) installed = UbikaApi.getInstalledGames()
            isLoading = false
            delay(8000)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C4DFF))
            }
            pcOnline == false -> EmptyHint(
                "PC offline",
                "Abra o UbikaPc e rode  npm start  — a biblioteca mostra os jogos baixados neste PC."
            )
            installed.isEmpty() -> EmptyHint(
                "Biblioteca vazia",
                "Jogos instalados pelo download remoto aparecem aqui quando terminam (100%)."
            )
            else -> LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                items(installed) { g ->
                    var focused by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF140822))
                            .border(
                                if (focused) 3.dp else 1.dp,
                                if (focused) Color(0xFF7C4DFF) else Color(0xFF311B52),
                                RoundedCornerShape(14.dp)
                            )
                            .focusable()
                            .onFocusChanged { focused = it.isFocused }
                            .clickable { soundManager.playClickA() }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFF7C4DFF), modifier = Modifier.size(40.dp))
                        Text(g.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 2)
                        Text("${"%.1f".format(g.sizeBytes / 1e9)} GB • instalado", color = Color.Gray, fontSize = 12.sp)
                        Text(g.savePath, color = Color.DarkGray, fontSize = 10.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, color = Color.Gray, fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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

/** Resolução física real do aparelho em paisagem (maior lado primeiro) + refresh rate. */
@Suppress("DEPRECATION")
private fun realDeviceRes(context: Context): Triple<Int, Int, Int>? {
    return try {
        val display = context.display // API 30+
        if (display != null) {
            val m = display.mode
            Triple(maxOf(m.physicalWidth, m.physicalHeight), minOf(m.physicalWidth, m.physicalHeight), m.refreshRate.toInt())
        } else null
    } catch (e: Exception) {
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val dm = DisplayMetrics()
            wm.defaultDisplay.getRealMetrics(dm)
            Triple(maxOf(dm.widthPixels, dm.heightPixels), minOf(dm.widthPixels, dm.heightPixels), wm.defaultDisplay.refreshRate.toInt())
        } catch (e2: Exception) { null }
    }
}