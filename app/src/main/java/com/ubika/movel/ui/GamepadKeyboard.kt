package com.ubika.movel.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubika.movel.UbikaPurple

val keyboardRows = listOf(
    listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Backspace"),
    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "[", "]", "\\"),
    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", ";", "'", "Buscar"),
    listOf("Shift", "z", "x", "c", "v", "b", "n", "m", ",", ".", "/", "Shift"),
    listOf("Space")
)

@Composable
fun GamepadKeyboard(
    onKeyPressed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0F12)) // Fundo escuro do teclado
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyboardRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    val weight = when (key) {
                        "Space" -> 1f
                        "Backspace", "Buscar", "Shift" -> 0.15f
                        else -> 0.08f
                    }
                    KeyboardKey(
                        text = key,
                        modifier = Modifier.weight(weight),
                        onClick = { onKeyPressed(key) }
                    )
                }
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                if (isFocused) UbikaPurple else Color(0xFF1A1A1E),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                if (isFocused) Color.White else Color.Transparent,
                RoundedCornerShape(4.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_A ||
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                        onClick()
                    }
                    true
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        if (text == "Backspace") {
            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = Color.White)
        } else {
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
