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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.ubika.movel.R
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
    var userEmail by remember { mutableStateOf(prefs.getString("user_email", "") ?: "") }
    var plan by remember { mutableStateOf("") }
    var planExpiresAt by remember { mutableStateOf(0L) }
    var renewSid by remember { mutableStateOf<String?>(null) }

    val padType by rememberPadType()
    val glyphs = GamepadManager.glyphsFor(padType)

    LaunchedEffect(Unit) {
        UbikaSocket.connect(UbikaApi.userId)
        // Handoff GameSir: controle desplugou do PC → a sessão passa pro celular
        UbikaSocket.setControllerEventListener { ev ->
            val type = ev.optString("type")
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (type == "unplugged") {
                    soundManager.playOpen()
                    if (currentScreen != "Stream") currentScreen = "Stream"
                    android.widget.Toast.makeText(
                        context, "Controle desconectado — a sessão passou pro celular",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        realDeviceRes(context)?.let { (w, h, fps) -> UbikaSocket.setClientResolution(w, h, fps) }
        val me = UbikaApi.getMe()
        if (me.effectivePlan.isNotEmpty()) { plan = me.effectivePlan; planExpiresAt = me.planExpiresAt }
        if (me.name.isNotEmpty()) { userName = me.name; prefs.edit().putString("user_name", me.name).apply() }
        if (me.email.isNotEmpty()) { userEmail = me.email; prefs.edit().putString("user_email", me.email).apply() }
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
                                PadField(
                                    value = globalSearchQuery,
                                    onValue = { globalSearchQuery = it },
                                    hint = "Pesquisar...",
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
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            // Desliza suave na direção da navegação + fade cruzado
                            (slideInHorizontally(
                                animationSpec = tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                initialOffsetX = { it / 14 }
                            ) + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = tween(380, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                                targetOffsetX = { -it / 14 }
                            ) + fadeOut(tween(300)))
                        },
                        label = "screen_trans"
                    ) { targetScreen ->
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
                        // Diagrama do controle detectado = confirmação visual do pareamento
                        if (padType == GamepadManager.PadType.XBOX || padType == GamepadManager.PadType.GENERIC) {
                            Image(painterResource(R.drawable.xbox_diagram), null, modifier = Modifier.height(22.dp))
                        } else if (padType == GamepadManager.PadType.PLAYSTATION) {
                            Image(painterResource(R.drawable.ps_diagram), null, modifier = Modifier.height(22.dp))
                        }
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
                email = userEmail,
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
    var actionGame by remember { mutableStateOf<com.ubika.movel.network.InstalledGame?>(null) }

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
                            .onFocusChanged { if (it.isFocused && !focused) soundManager.playFocusTick(); focused = it.isFocused }
                            .clickable { soundManager.playClickA(); actionGame = g }
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

    actionGame?.let { g ->
        GameActionsSheet(
            game = g,
            soundManager = soundManager,
            onClose = { actionGame = null },
            onDeleted = { actionGame = null; installed = installed.filter { it.id != g.id } }
        )
    }
}

/**
 * Ações do jogo instalado: JOGAR, resolução/fps/bitrate específicos
 * (salvos no PC, aplicados ao abrir) e APAGAR com confirmação.
 * Tudo focável — navega por D-pad, A confirma, B fecha.
 */
@Composable
private fun GameActionsSheet(
    game: com.ubika.movel.network.InstalledGame,
    soundManager: SoundManager,
    onClose: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var busy by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var resW by remember { mutableIntStateOf(game.resWidth) }
    var resH by remember { mutableIntStateOf(game.resHeight) }
    var fpsSel by remember { mutableIntStateOf(game.fps) }
    var brSel by remember { mutableIntStateOf(game.bitrate) }
    var savedMsg by remember { mutableStateOf(false) }

    fun saveCfg() {
        scope.launch {
            UbikaApi.sendPcCommand("config_jogo", mapOf(
                "installId" to game.id,
                "settings" to mapOf("width" to resW, "height" to resH, "fps" to fpsSel, "bitrate" to brSel)
            ))
            savedMsg = true
        }
    }

    @Composable
    fun chip(label: String, selected: Boolean, onSel: () -> Unit) {
        var f by remember { mutableStateOf(false) }
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (selected) Color(0xFF7C4DFF) else Color(0xFF140822))
                .border(if (f) 2.dp else 1.dp, if (f) Color.White else if (selected) Color(0xFF9575CD) else Color(0xFF311B52), RoundedCornerShape(8.dp))
                .onFocusChanged { if (it.isFocused && !f) soundManager.playFocusTick(); f = it.isFocused }
                .focusable()
                .clickable { soundManager.playSelect(); onSel(); saveCfg() }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) { Text(label, color = if (selected) Color.White else Color(0xFFD1C4E9), fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).clickable { onClose() }, contentAlignment = Alignment.Center) {
            Card(
                modifier = Modifier.fillMaxWidth(0.82f).border(1.dp, Color(0xFF512DA8), RoundedCornerShape(16.dp)).clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0510))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(game.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                        TextButton(onClick = { onClose() }, contentPadding = PaddingValues(0.dp)) {
                            Text("✕", color = Color(0xFFB39DDB), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // JOGAR
                    var playF by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            soundManager.playClickA(); busy = true
                            scope.launch {
                                // Res/fps por jogo o PC já aplica via manifest (anexa o
                                // display virtual na res certa). Aqui só alinhamos as prefs
                                // locais de stream (bitrate/fps lidos pela aba Stream).
                                val sp = context.getSharedPreferences("ubika_stream", android.content.Context.MODE_PRIVATE)
                                sp.edit()
                                    .putInt("bitrate", if (brSel > 0) brSel else sp.getInt("bitrate", 20))
                                    .putInt("fps", if (fpsSel > 0) fpsSel else 60)
                                    .putString("resolution", if (resW > 0) "${resW}x${resH}" else "Nativo")
                                    .apply()
                                val r = UbikaApi.sendPcCommand("launch_game", mapOf("installId" to game.id))
                                busy = false
                                android.widget.Toast.makeText(context,
                                    if (r.bool("ok")) "Iniciando no PC…" else "Falhou: ${r.str("error") ?: "?"}",
                                    android.widget.Toast.LENGTH_LONG).show()
                                if (r.bool("ok")) onClose()
                            }
                        },
                        enabled = game.playable && !busy,
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                            .onFocusChanged { if (it.isFocused && !playF) soundManager.playFocusTick(); playF = it.isFocused }
                            .border(if (playF) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color(0xFF14301B)),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text(if (busy) "INICIANDO…" else "▶ JOGAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }

                    // RESOLUÇÃO
                    Text("RESOLUÇÃO AO JOGAR", color = Color(0xFFB39DDB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chip("AUTO (CELULAR)", resW == 0) { resW = 0; resH = 0 }
                        chip("720p", resW == 1280) { resW = 1280; resH = 720 }
                        chip("1080p", resW == 1920) { resW = 1920; resH = 1080 }
                        chip("1440p", resW == 2560) { resW = 2560; resH = 1440 }
                    }

                    // FPS
                    Text("FPS", color = Color(0xFFB39DDB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chip("AUTO", fpsSel == 0) { fpsSel = 0 }
                        chip("60", fpsSel == 60) { fpsSel = 60 }
                        chip("90", fpsSel == 90) { fpsSel = 90 }
                        chip("120", fpsSel == 120) { fpsSel = 120 }
                    }

                    // BITRATE
                    Text("BITRATE DO STREAM", color = Color(0xFFB39DDB), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        chip("AUTO", brSel == 0) { brSel = 0 }
                        chip("10M", brSel == 10) { brSel = 10 }
                        chip("20M", brSel == 20) { brSel = 20 }
                        chip("35M", brSel == 35) { brSel = 35 }
                        chip("50M", brSel == 50) { brSel = 50 }
                    }
                    if (savedMsg) Text("✓ Preferências salvas no PC", color = Color(0xFF66BB6A), fontSize = 10.sp)

                    // APAGAR — dois toques pra confirmar
                    var delF by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (!confirmDelete) { soundManager.playSelect(); confirmDelete = true; return@Button }
                            soundManager.playBack(); busy = true
                            scope.launch {
                                val r = UbikaApi.sendPcCommand("excluir_jogo", mapOf("installId" to game.id))
                                busy = false
                                if (r.bool("ok")) onDeleted()
                                else { confirmDelete = false; android.widget.Toast.makeText(context, "Falhou: ${r.str("error") ?: "?"}", android.widget.Toast.LENGTH_LONG).show() }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                            .onFocusChanged { if (it.isFocused && !delF) soundManager.playFocusTick(); delF = it.isFocused }
                            .border(if (delF) 2.5.dp else 0.dp, Color.White, RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = if (confirmDelete) Color(0xFFB71C1C) else Color(0xFF3A0D10)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            if (confirmDelete) "⚠ APAGAR DE VEZ? ${"%.1f".format(game.sizeBytes / 1e9)} GB — A CONFIRMA" else "🗑 APAGAR JOGO",
                            color = if (confirmDelete) Color.White else Color(0xFFEF5350),
                            fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                        )
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
fun ActionPrompt(iconRes: Int, actionText: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(26.dp)
        )
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