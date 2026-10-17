package com.ubika.movel.utils

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ubika.movel.R

/**
 * Detecta controles conectados (Bluetooth/USB) e classifica o padrão
 * para exibir os glifos corretos na UI (A/B/X/Y vs ✕/○/△/□).
 */
object GamepadManager {

    enum class PadType { NONE, XBOX, PLAYSTATION, GENERIC }

    /** Ícones reais dos botões (Xelu CC0) por padrão de controle. */
    data class Glyphs(
        val confirm: Int,   // Xbox A / PS Cross
        val cancel: Int,    // Xbox B / PS Circle
        val altTop: Int,    // Xbox Y / PS Triangle
        val altLeft: Int,   // Xbox X / PS Square
        val menu: Int
    )

    fun glyphsFor(type: PadType): Glyphs = when (type) {
        PadType.PLAYSTATION -> Glyphs(
            confirm = R.drawable.ps_cross, cancel = R.drawable.ps_circle,
            altTop = R.drawable.ps_triangle, altLeft = R.drawable.ps_square,
            menu = R.drawable.ps_options
        )
        else -> Glyphs(
            confirm = R.drawable.xbox_a, cancel = R.drawable.xbox_b,
            altTop = R.drawable.xbox_y, altLeft = R.drawable.xbox_x,
            menu = R.drawable.xbox_menu
        )
    }

    fun detectNow(): PadType {
        var found: PadType = PadType.NONE
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (!isGamepad(device)) continue

            val type = classify(device)
            // Prioriza um padrão conhecido sobre genérico
            if (type != PadType.GENERIC) return type
            found = PadType.GENERIC
        }
        return found
    }

    private fun isGamepad(device: InputDevice): Boolean {
        val sources = device.sources
        val hasPad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        val hasJoy = (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
        return (hasPad || hasJoy) && device.controllerNumber > 0
    }

    private fun classify(device: InputDevice): PadType {
        // Vendor IDs USB/BT mais confiáveis que o nome
        when (device.vendorId) {
            0x045E -> return PadType.XBOX        // Microsoft
            0x054C -> return PadType.PLAYSTATION // Sony
        }
        val name = device.name.lowercase()
        return when {
            name.contains("xbox") || name.contains("x-box") -> PadType.XBOX
            name.contains("dualsense") || name.contains("dualshock") ||
                name.contains("playstation") || name.contains("sony") ||
                name == "wireless controller" -> PadType.PLAYSTATION // nome padrão do DS4
            else -> PadType.GENERIC
        }
    }
}

/** Estado Compose vivo do padrão de controle conectado (NONE = toque). */
@Composable
fun rememberPadType(): State<GamepadManager.PadType> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(GamepadManager.detectNow()) }

    DisposableEffect(Unit) {
        val im = context.getSystemService(Context.INPUT_SERVICE) as InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) { state.value = GamepadManager.detectNow() }
            override fun onInputDeviceRemoved(deviceId: Int) { state.value = GamepadManager.detectNow() }
            override fun onInputDeviceChanged(deviceId: Int) { state.value = GamepadManager.detectNow() }
        }
        im.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        onDispose { im.unregisterInputDeviceListener(listener) }
    }
    return state
}
