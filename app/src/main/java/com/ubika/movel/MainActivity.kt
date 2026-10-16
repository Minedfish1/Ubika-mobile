package com.ubika.movel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// Paleta de Cores Tema Escuro + Roxo Ubika
val UbikaDarkBackground = Color(0xFF121212)
val UbikaSurface = Color(0xFF1E1E1E)
val UbikaPurple = Color(0xFF8A2BE2) // Roxo vibrante (BlueViolet)
val UbikaPurpleDark = Color(0xFF4B0082)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Diz ao app para usar a tela inteira (além das bordas seguras)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 2. Esconde a barra cinza de cima (Status Bar) e a de navegação de baixo
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            // Faz com que as barras só apareçam se o utilizador deslizar o dedo do topo para baixo
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        // Permissão de notificação (API 33+) — sem ela o aviso de "download concluído" não aparece
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 7)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = UbikaDarkBackground,
                    surface = UbikaSurface,
                    primary = UbikaPurple,
                    onPrimary = Color.White,
                    onBackground = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val prefs = remember { getSharedPreferences("ubika_prefs", MODE_PRIVATE) }
                    var showSplash by remember { mutableStateOf(true) }
                    var loggedIn by remember { mutableStateOf(prefs.getString("session_token", null) != null) }

                    // Restaura sessão persistida — e valida no servidor: se morreu, volta pro login
                    LaunchedEffect(Unit) {
                        val token = prefs.getString("session_token", null)
                        if (token == null) return@LaunchedEffect
                        com.ubika.movel.network.UbikaApi.sessionToken = token
                        prefs.getString("user_id", null)?.let { com.ubika.movel.network.UbikaApi.userId = it }
                        val me = com.ubika.movel.network.UbikaApi.getMe()
                        if (me.userId.isEmpty()) {
                            prefs.edit().remove("session_token").remove("user_id").apply()
                            com.ubika.movel.network.UbikaApi.sessionToken = ""
                            loggedIn = false
                        }
                    }

                    when {
                        showSplash -> com.ubika.movel.ui.SplashScreen(onFinished = { showSplash = false })
                        !loggedIn -> com.ubika.movel.ui.OnboardingScreen(onDone = { token, uid ->
                            prefs.edit()
                                .putString("session_token", token)
                                .putString("user_id", uid)
                                .apply()
                            loggedIn = true
                        })
                        else -> com.ubika.movel.ui.MainConsoleScreen(onLogout = {
                            prefs.edit().remove("session_token").remove("user_id").apply()
                            com.ubika.movel.network.UbikaApi.sessionToken = ""
                            com.ubika.movel.network.UbikaApi.userId = ""
                            loggedIn = false
                        })
                    }
                }
            }
        }
    }
}


@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "UBIKA",
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            color = UbikaPurple
        )
        // Ícone de Perfil com borda roxa
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(UbikaSurface)
                .border(2.dp, UbikaPurple, CircleShape)
                .clickable { /* Ação do perfil */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Perfil",
                tint = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarConsole() {
    var isFocused by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    // O onFocusChanged detecta se o usuário navegou para cá com o controle (D-Pad)
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = { Text("Buscar jogos (Pressione A para digitar)") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UbikaPurple,
            unfocusedBorderColor = Color.DarkGray,
            focusedContainerColor = UbikaSurface,
            unfocusedContainerColor = UbikaSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) UbikaPurple else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .focusable() // Permite que o controle selecione este item
    )
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp),
        color = Color.White
    )
}

@Composable
fun CarrosselDestaques() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(3) { index ->
            GameCardBanner(title = "Destaque ${index + 1}")
        }
    }
}

@Composable
fun CarrosselJogosNormais() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(6) { index ->
            GameCardPoster(title = "Jogo ${index + 1}")
        }
    }
}

// Card grande horizontal (Estilo Banner) com animação de foco
@Composable
fun GameCardBanner(title: String) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UbikaSurface),
        modifier = Modifier
            .width(300.dp)
            .height(160.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { /* Abrir jogo */ }
            // Animação suave de tamanho ao receber foco do controle
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessLow))
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) UbikaPurple else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
            // Futuramente a imagem do Coil vai aqui no fundo
            Text(
                text = title,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

// Card vertical (Estilo Capa de DVD/Steam)
@Composable
fun GameCardPoster(title: String) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = UbikaSurface),
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { /* Abrir jogo */ }
            .border(
                width = if (isFocused) 3.dp else 0.dp,
                color = if (isFocused) UbikaPurple else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = title, fontWeight = FontWeight.SemiBold)
        }
    }
}