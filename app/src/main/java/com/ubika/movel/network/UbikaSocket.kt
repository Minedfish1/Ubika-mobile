package com.ubika.movel.network

import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

object UbikaSocket {
    private var mSocket: Socket? = null

    fun connect() {
        try {
            val opts = IO.Options()
            opts.query = "device=android&userId=utilizador_teste_123"
            mSocket = IO.socket("http://18.223.108.137:3000", opts)
            mSocket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Método genérico para comandos simples
    fun sendCommand(gameName: String) {
        sendInstallCommand(gameName, "magnet:?xt=teste_do_app")
    }

    // Método completo para enviar o jogo e o magnet link escolhido dinamicamente
    fun sendInstallCommand(gameName: String, magnetLink: String) {
        try {
            val data = JSONObject()
            data.put("userId", "utilizador_teste_123")

            val payload = JSONObject()
            payload.put("action", "iniciar_download")
            payload.put("gameName", gameName)
            payload.put("magnetLink", magnetLink)

            data.put("payload", payload)

            // Dispara a requisição para a AWS, que encaminhará ao PC
            mSocket?.emit("send_to_pc", data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}