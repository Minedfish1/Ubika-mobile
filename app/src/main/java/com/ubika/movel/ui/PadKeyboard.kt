package com.ubika.movel.ui

import android.view.KeyEvent as AKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ubika.movel.UbikaPurple
import com.ubika.movel.ui.theme.OrbitronFamily

private val Bg = Color(0xFF0A0510)
private val Surface2 = Color(0xFF140822)
private val Border = Color(0xFF512DA8)
private val BorderFocus = Color.White
private val Txt2 = Color(0xFFB39DDB)

/**
 * Campo de texto que funciona com manete: foca pelo D-pad, A/toque abre o
 * teclado virtual; teclado físico digita direto (não precisa abrir o overlay).
 */
@Composable
fun PadField(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    digitsOnly: Boolean = false,
    maxLen: Int = 64,
    center: Boolean = false,
    bigText: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    var open by remember { mutableStateOf(false) }

    fun append(s: String) {
        var v = value
        for (c in s) {
            if (v.length >= maxLen) break
            if (digitsOnly && !c.isDigit()) continue
            v += c
        }
        onValue(v)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Surface2)
            .border(if (focused) 2.dp else 1.dp, if (focused) UbikaPurple else Border, RoundedCornerShape(8.dp))
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                if (ev.type == KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (ev.nativeKeyEvent.keyCode) {
                    AKeyEvent.KEYCODE_DPAD_CENTER, AKeyEvent.KEYCODE_ENTER,
                    AKeyEvent.KEYCODE_NUMPAD_ENTER, AKeyEvent.KEYCODE_BUTTON_A -> { open = true; true }
                    AKeyEvent.KEYCODE_DEL -> { if (value.isNotEmpty()) onValue(value.dropLast(1)); true }
                    else -> {
                        val ch = ev.nativeKeyEvent.unicodeChar
                        if (ch > 0 && !ch.toChar().isISOControl()) { append(ch.toChar().toString()); true } else false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { open = true }
            .focusable()
            .padding(horizontal = 14.dp, vertical = if (bigText) 14.dp else 12.dp)
    ) {
        val shown = if (password) "•".repeat(value.length) else value
        Text(
            text = shown.ifEmpty { hint },
            color = if (shown.isEmpty()) Color.DarkGray else Color.White,
            fontSize = if (bigText) 26.sp else 15.sp,
            fontWeight = if (bigText) FontWeight.Black else FontWeight.Normal,
            letterSpacing = if (bigText) 8.sp else 0.5.sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (open) {
        PadKeyboardDialog(
            value = value,
            onValue = onValue,
            hint = hint,
            password = password,
            digitsOnly = digitsOnly,
            maxLen = maxLen,
            onClose = { open = false }
        )
    }
}

/**
 * Teclado virtual estilo console: grade QWERTY navegável por D-pad,
 * A confirma a tecla, B fecha. Teclado físico digita direto.
 */
@Composable
private fun PadKeyboardDialog(
    value: String,
    onValue: (String) -> Unit,
    hint: String,
    password: Boolean,
    digitsOnly: Boolean,
    maxLen: Int,
    onClose: () -> Unit
) {
    var caps by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }

    fun type(ch: String) {
        var v = value
        for (c in ch) {
            if (v.length >= maxLen) break
            if (digitsOnly && !c.isDigit()) continue
            v += if (caps) c.uppercaseChar() else c
        }
        onValue(v)
        if (caps) caps = false
    }

    fun onSpecial(k: String) {
        when (k) {
            "BKSP" -> if (value.isNotEmpty()) onValue(value.dropLast(1))
            "CLEAR" -> onValue("")
            "CAPS" -> caps = !caps
            "SPACE" -> type(" ")
            "DONE", "CLOSE" -> onClose()
        }
    }

    // Teclas especiais são strings >1 char; letras/dígitos são de 1
    val rows: List<List<String>> = if (digitsOnly) listOf(
        listOf("1", "2", "3", "4", "5"),
        listOf("6", "7", "8", "9", "0"),
        listOf("BKSP", "CLEAR", "DONE"),
    ) else listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "BKSP"),
        listOf("CAPS", "z", "x", "c", "v", "b", "n", "m", ".", "DONE"),
        listOf("@", "_", "-", "SPACE", "CLEAR", "CLOSE"),
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .onPreviewKeyEvent { ev ->
                    if (ev.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (ev.nativeKeyEvent.keyCode) {
                        AKeyEvent.KEYCODE_DEL -> { onSpecial("BKSP"); true }
                        AKeyEvent.KEYCODE_ENTER, AKeyEvent.KEYCODE_NUMPAD_ENTER -> { onClose(); true }
                        AKeyEvent.KEYCODE_ESCAPE, AKeyEvent.KEYCODE_BUTTON_B, AKeyEvent.KEYCODE_BACK -> { onClose(); true }
                        else -> {
                            val ch = ev.nativeKeyEvent.unicodeChar
                            if (ch > 0 && !ch.toChar().isISOControl()) { type(ch.toChar().toString()); true } else false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Bg)
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Preview do que está sendo digitado
                val shown = if (password) "•".repeat(value.length) else value
                Text(
                    text = shown.ifEmpty { hint },
                    color = if (shown.isEmpty()) Color.DarkGray else Color.White,
                    fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface2)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )

                Spacer(Modifier.height(4.dp))

                rows.forEachIndexed { ri, keys ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        keys.forEachIndexed { ki, k ->
                            val wide = k == "SPACE"
                            KbKey(
                                label = keyLabel(k, caps),
                                wide = wide,
                                modifier = if (ri == 0 && ki == 0) Modifier.focusRequester(firstKey) else Modifier,
                                onPress = { if (k.length == 1) type(k) else onSpecial(k) }
                            )
                        }
                    }
                }

                Text(
                    "D-pad navega  ·  A digita  ·  B fecha  ·  teclado físico funciona",
                    color = Txt2, fontSize = 10.sp, letterSpacing = 1.sp,
                    fontFamily = OrbitronFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    LaunchedEffect(Unit) { try { firstKey.requestFocus() } catch (_: Exception) {} }
}

private fun keyLabel(k: String, caps: Boolean): String = when (k) {
    "BKSP" -> "⌫"; "CAPS" -> "⇧"; "SPACE" -> "ESPAÇO"
    "CLEAR" -> "LIMPAR"; "DONE" -> "✓ PRONTO"; "CLOSE" -> "✕"
    else -> if (caps && k.length == 1 && k[0].isLetter()) k.uppercase() else k
}

@Composable
private fun RowScope.KbKey(label: String, wide: Boolean, modifier: Modifier = Modifier, onPress: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .weight(if (wide) 4f else 1f)
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (focused) Color(0xFF3A1F66) else Surface2)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) BorderFocus else Border,
                RoundedCornerShape(8.dp)
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable { onPress() }
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (focused) Color.White else Txt2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
