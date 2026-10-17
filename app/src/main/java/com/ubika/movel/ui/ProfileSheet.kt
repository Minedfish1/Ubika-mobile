package com.ubika.movel.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ubika.movel.network.UbikaApi
import com.ubika.movel.ui.theme.OrbitronFamily
import com.ubika.movel.utils.SoundManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Perfil: foto da galeria com crop circular estilo WhatsApp (zoom + arrastar),
 * dados da conta e renovação de plano.
 */
@Composable
fun ProfileSheet(
    soundManager: SoundManager,
    avatarPath: String,
    avatarUrl: String,
    userName: String,
    email: String,
    onAvatar: (String) -> Unit,
    onName: (String) -> Unit,
    plan: String,
    planExpiresAt: Long,
    onClose: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var renewBusy by remember { mutableStateOf(false) }
    var nameField by remember { mutableStateOf(userName) }
    var savingName by remember { mutableStateOf(false) }
    var nameMsg by remember { mutableStateOf<String?>(null) }

    // Galeria → overlay de crop
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { cropUri = it }
    }

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable { onClose() }, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF140822))
                .border(1.dp, Color(0xFF311B52), RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cabeçalho: barra roxa + título Orbitron + fechar
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp, 18.dp).background(Color(0xFF7C4DFF)))
                Spacer(Modifier.width(10.dp))
                Text("PERFIL", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    fontFamily = OrbitronFamily, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
                Text("✕", color = Color(0xFFB39DDB), fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { soundManager.playBack(); onClose() })
            }

            // Avatar com badge de edição — toque troca a foto
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFF7C4DFF), CircleShape)
                        .clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    AvatarImage(avatarPath, avatarUrl, Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF7C4DFF))
                        .border(2.dp, Color(0xFF140822), CircleShape)
                        .clickable { picker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) { Text("✎", color = Color.White, fontSize = 13.sp) }
            }

            // Conta do usuário
            if (email.isNotBlank()) {
                Text(email, color = Color(0xFF8a7fa8), fontSize = 12.sp)
            }

            // Nome de exibição — salva no servidor (teclado de manete via A)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PadField(
                    value = nameField,
                    onValue = { nameField = it.take(40); nameMsg = null },
                    hint = "Seu nome de exibição",
                    maxLen = 40,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        if (nameField.isBlank() || savingName) return@TextButton
                        soundManager.playClickA()
                        savingName = true
                        scope.launch {
                            val ok = UbikaApi.updateMe(name = nameField.trim())
                            savingName = false
                            if (ok) { onName(nameField.trim()); nameMsg = "Salvo ✓" }
                            else nameMsg = "Falhou — tente de novo"
                        }
                    },
                    enabled = nameField.isNotBlank() && !savingName
                ) { Text(if (savingName) "…" else "SALVAR", color = Color(0xFF7C4DFF), fontSize = 12.sp, fontWeight = FontWeight.Black) }
            }
            nameMsg?.let { Text(it, color = if (it.endsWith("✓")) Color(0xFF66BB6A) else Color(0xFFEF5350), fontSize = 11.sp) }

            PlanRow(plan, planExpiresAt)

            if (plan == "expired" || plan == "trial") {
                UbikaSheetButton(if (renewBusy) "ABRINDO…" else "RENOVAR / ASSINAR PRO — R$ 19,90") {
                    scope.launch {
                        renewBusy = true
                        val r = UbikaApi.createCheckout()
                        renewBusy = false
                        if (r.url.isNotEmpty()) {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.url))) } catch (_: Exception) {}
                        }
                    }
                }
            }

            UbikaSheetButton("FECHAR", subtle = true) { soundManager.playBack(); onClose() }
            Text("SAIR DA CONTA", color = Color(0xFFEF5350), fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.clickable { soundManager.playBack(); onLogout() })
        }
    }

    cropUri?.let { uri ->
        AvatarCropOverlay(uri, onDone = { path ->
            onAvatar(path); cropUri = null
            // Sobe a foto pro servidor — perfil fica igual em qualquer aparelho
            scope.launch {
                try {
                    val b = File(path).readBytes()
                    UbikaApi.updateMe(avatarBase64 = android.util.Base64.encodeToString(b, android.util.Base64.NO_WRAP))
                } catch (_: Exception) {}
            }
        }) { cropUri = null }
    }
}

@Composable
private fun PlanRow(plan: String, planExpiresAt: Long) {
    val label = when (plan) {
        "pro" -> if (planExpiresAt > 0) "UBIKA PRO · renova ${fmtDate(planExpiresAt)}" else "UBIKA PRO · vitalício"
        "trial" -> "TRIAL"
        "expired" -> "PLANO EXPIRADO"
        else -> plan.uppercase()
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF1E0D33)).padding(14.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(label, color = Color(0xFFB39DDB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private fun fmtDate(ms: Long): String {
    val d = java.util.Date(ms)
    val f = java.text.SimpleDateFormat("dd/MM", java.util.Locale("pt", "BR"))
    return f.format(d)
}

/** Avatar: arquivo local > URL do servidor > placeholder "U". */
@Composable
fun AvatarImage(avatarPath: String, avatarUrl: String = "", modifier: Modifier = Modifier) {
    val f = if (avatarPath.isNotBlank()) File(avatarPath) else null
    val model: Any? = when {
        f?.exists() == true -> f
        avatarUrl.isNotBlank() -> avatarUrl
        else -> null
    }
    if (model != null) {
        AsyncImage(model = model, contentDescription = "avatar", contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier = modifier.background(Color(0xFF311B52)), contentAlignment = Alignment.Center) {
            Text("U", color = Color(0xFFB39DDB), fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
    }
}

/**
 * Crop circular estilo WhatsApp: arrasta/pinca a foto dentro do círculo,
 * SALVAR renderiza 512x512 e grava em filesDir/avatar.png.
 */
@Composable
private fun AvatarCropOverlay(uri: Uri, onDone: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(uri) {
        try {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) { null }
    }
    if (bitmap == null) { onCancel(); return }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val boxDp = 260.dp
    val boxPx = with(LocalDensity.current) { boxDp.toPx() }
    val baseScale = maxOf(boxPx / bitmap.width, boxPx / bitmap.height)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("AJUSTAR FOTO", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("Arraste e use dois dedos para dar zoom", color = Color.Gray, fontSize = 12.sp)

            Box(
                modifier = Modifier
                    .size(boxDp)
                    .clip(CircleShape)
                    .border(3.dp, Color(0xFF7C4DFF), CircleShape)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            // limita o pan para a imagem sempre cobrir o círculo
                            val maxX = (bitmap.width * baseScale * scale - boxPx) / 2f
                            val maxY = (bitmap.height * baseScale * scale - boxPx) / 2f
                            offset = Offset(
                                (offset.x + pan.x).coerceIn(-maxX, maxX),
                                (offset.y + pan.y).coerceIn(-maxY, maxY)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .size(
                            with(LocalDensity.current) { (bitmap.width * baseScale).toDp() },
                            with(LocalDensity.current) { (bitmap.height * baseScale).toDp() }
                        )
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            translationX = offset.x; translationY = offset.y
                        }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                UbikaSheetButton("CANCELAR", subtle = true) { onCancel() }
                UbikaSheetButton("SALVAR") {
                    val path = renderAvatar(context, bitmap, scale, offset, boxPx)
                    if (path != null) onDone(path) else onCancel()
                }
            }
        }
    }
}

/** Renderiza o recorte circular: base cover * zoom do usuário + pan, saída 512px. */
private fun renderAvatar(context: Context, src: Bitmap, zoom: Float, offset: Offset, boxPx: Float): String? {
    return try {
        val out = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val baseScale = maxOf(boxPx / src.width, boxPx / src.height)
        val k = 512f / boxPx
        val m = Matrix().apply {
            setScale(baseScale * zoom * k, baseScale * zoom * k)
            postTranslate(offset.x * k, offset.y * k)
        }
        canvas.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        val file = File(context.filesDir, "avatar.png")
        FileOutputStream(file).use { out.compress(Bitmap.CompressFormat.PNG, 95, it) }
        file.absolutePath
    } catch (_: Exception) { null }
}

@Composable
private fun UbikaSheetButton(text: String, subtle: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (subtle) Color(0xFF1E0D33) else Color(0xFF7C4DFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}
