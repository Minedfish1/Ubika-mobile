package com.ubika.movel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.UbikaDarkBackground
import com.ubika.movel.UbikaPurple
import com.ubika.movel.network.UbikaApi
import kotlinx.coroutines.launch

private enum class Step { INTRO, AUTH, PLAN, PAIR }

/**
 * Onboarding: apresentação -> login/registro -> plano -> código do PC.
 * Chama onDone(sessionToken, userId) quando tudo pronto.
 */
@Composable
fun OnboardingScreen(onDone: (token: String, userId: String) -> Unit) {
    var step by remember { mutableStateOf(Step.INTRO) }
    var authToken by remember { mutableStateOf("") }
    var authUserId by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(UbikaDarkBackground),
        contentAlignment = Alignment.Center
    ) {
        when (step) {
            Step.INTRO -> IntroStep { step = Step.AUTH }
            Step.AUTH -> AuthStep { token, uid, p ->
                authToken = token; authUserId = uid; plan = p
                UbikaApi.sessionToken = token; UbikaApi.userId = uid
                step = Step.PLAN
            }
            Step.PLAN -> PlanStep(plan) { step = Step.PAIR }
            Step.PAIR -> PairStep { onDone(authToken, authUserId) }
        }
    }
}

// ---------- Passo 1: apresentação ----------
@Composable
private fun IntroStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.padding(32.dp).widthIn(max = 520.dp)
    ) {
        UbikaControllerIcon(modifier = Modifier.size(180.dp, 120.dp))
        Text("UBIKA", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black, letterSpacing = 6.sp)
        Text(
            "Seu PC vira um console na nuvem.\nBaixe jogos remotamente, acompanhe downloads e jogue em qualquer lugar com seu controle.",
            color = Color.Gray, fontSize = 15.sp, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        UbikaButton("COMEÇAR") { onNext() }
    }
}

// ---------- Passo 2: login / registro ----------
@Composable
private fun AuthStep(onAuth: (token: String, userId: String, plan: String) -> Unit) {
    var isRegister by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp).widthIn(max = 420.dp)
    ) {
        Text(
            if (isRegister) "CRIAR CONTA" else "ENTRAR",
            color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black
        )

        UbikaField(value = email, onValue = { email = it }, hint = "E-mail", keyboard = KeyboardType.Email)
        UbikaField(value = password, onValue = { password = it }, hint = "Senha (mín. 6)", keyboard = KeyboardType.Password, password = true)

        error?.let { Text(it, color = Color(0xFFEF5350), fontSize = 13.sp) }

        UbikaButton(if (busy) "AGUARDE…" else if (isRegister) "CRIAR CONTA" else "ENTRAR", enabled = !busy) {
            busy = true; error = null
            scope.launch {
                val r = if (isRegister) UbikaApi.register(email.trim(), password)
                        else UbikaApi.login(email.trim(), password)
                busy = false
                if (r.error != null) error = traduzErro(r.error)
                else onAuth(r.token, r.userId, r.plan)
            }
        }

        Text(
            if (isRegister) "Já tem conta? Entrar" else "Não tem conta? Criar",
            color = Color(0xFFB39DDB), fontSize = 13.sp,
            modifier = Modifier.clickable { isRegister = !isRegister; error = null }
        )
    }
}

private fun traduzErro(code: String) = when {
    code.contains("EMAIL_JA_CADASTRADO") -> "E-mail já cadastrado — faça login."
    code.contains("CREDENCIAIS_INVALIDAS") -> "E-mail ou senha incorretos."
    code.contains("SENHA_CURTA") -> "Senha muito curta (mínimo 6)."
    code.contains("EMAIL_INVALIDO") -> "E-mail inválido."
    code.contains("Failed to connect") || code.contains("timeout") -> "Servidor fora do ar — tente de novo."
    else -> "Erro: $code"
}

// ---------- Passo 3: plano / checkout ----------
@Composable
private fun PlanStep(plan: String, onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp).widthIn(max = 420.dp)
    ) {
        Text("SEU PLANO", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF140822))
                .border(1.dp, UbikaPurple, RoundedCornerShape(14.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("UBIKA TRIAL", color = UbikaPurple, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("7 dias grátis — todos os recursos", color = Color.White, fontSize = 14.sp)
            Text("Downloads remotos ilimitados\nStream em qualquer lugar\nCatálogo completo", color = Color.Gray, fontSize = 12.sp, lineHeight = 18.sp)
            Text("Depois R$ 19,90/mês", color = Color.Gray, fontSize = 11.sp)
        }
        Text(
            "O pagamento será pela Play Store quando o app for publicado — por enquanto seu trial já está ativo.",
            color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center
        )
        UbikaButton("CONTINUAR COM TRIAL") { onNext() }
    }
}

// ---------- Passo 4: código do PC ----------
@Composable
private fun PairStep(onDone: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pairedDevice by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(32.dp).widthIn(max = 420.dp)
    ) {
        Text("CONECTAR SEU PC", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(
            "No PC, abra a pasta UbikaPc e rode:  npm start\nO código de 6 dígitos aparece no console (e no arquivo PAREAR.txt).",
            color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp
        )

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) code = it },
            placeholder = { Text("000000", color = Color.DarkGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White, fontSize = 28.sp,
                fontWeight = FontWeight.Black, letterSpacing = 8.sp, textAlign = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = UbikaPurple, unfocusedBorderColor = Color(0xFF512DA8),
                focusedContainerColor = Color(0xFF140822), unfocusedContainerColor = Color(0xFF140822)
            ),
            modifier = Modifier.width(240.dp)
        )

        error?.let { Text(it, color = Color(0xFFEF5350), fontSize = 13.sp) }
        pairedDevice?.let { Text("PC pareado: $it", color = Color(0xFF66BB6A), fontSize = 14.sp, fontWeight = FontWeight.Bold) }

        if (pairedDevice == null) {
            UbikaButton(if (busy) "CONECTANDO…" else "PAREAR", enabled = !busy && code.length == 6) {
                busy = true; error = null
                scope.launch {
                    try {
                        val r = UbikaApi.pairPc(code)
                        if (r.bool("ok")) pairedDevice = r.str("deviceId") ?: "PC"
                        else error = r.str("error") ?: "Falha no pareamento"
                    } catch (e: Exception) {
                        error = when {
                            e.message?.contains("CODIGO_INVALIDO") == true -> "Código inválido ou expirado — gere outro no PC."
                            e.message?.contains("PC_DESCONECTOU") == true -> "O PC desconectou — rode o daemon de novo."
                            else -> e.message
                        }
                    } finally { busy = false }
                }
            }
            Text("Pular por agora", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.clickable { onDone() })
        } else {
            UbikaButton("ENTRAR NO UBIKA") { onDone() }
        }
    }
}

// ---------- Componentes ----------
@Composable
private fun UbikaField(value: String, onValue: (String) -> Unit, hint: String, keyboard: KeyboardType, password: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text(hint, color = Color.DarkGray) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = UbikaPurple, unfocusedBorderColor = Color(0xFF512DA8),
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedContainerColor = Color(0xFF140822), unfocusedContainerColor = Color(0xFF140822)
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun UbikaButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) UbikaPurple else Color(0xFF2A1A3E))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (enabled) Color.White else Color.Gray, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}
