package com.ubika.movel.network

import android.util.Log
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import com.ubika.movel.BuildConfig

object UbikaSocket {
    private const val TAG = "UbikaSocket"
    private val SERVER_URL = BuildConfig.UBIKA_SERVER.ifEmpty { "http://18.223.108.137:3000" }
    private val USER_ID = BuildConfig.UBIKA_USER_ID.ifEmpty { "utilizador_teste_123" } // tem de ser igual ao do client.js
    private val SHARED_SECRET = BuildConfig.UBIKA_SHARED_SECRET

    private var mSocket: Socket? = null
    private var progressListener: ((JSONObject) -> Unit)? = null

    val isConnected: Boolean
        get() = mSocket?.connected() == true

    fun connect() {
        if (mSocket != null && (mSocket?.connected() == true)) return
        try {
            val opts = IO.Options().apply {
                query = "device=android&userId=$USER_ID"
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 10000
                timeout = 10000
                if (SHARED_SECRET.isNotEmpty()) {
                    auth = mapOf("token" to SHARED_SECRET)
                }
            }
            mSocket = IO.socket(SERVER_URL, opts)
            mSocket?.on(Socket.EVENT_CONNECT) { Log.d(TAG, "conectado: ${mSocket?.id()}") }
            mSocket?.on(Socket.EVENT_CONNECT_ERROR) { a -> Log.e(TAG, "connect_error: ${a.firstOrNull()}") }
            mSocket?.on(Socket.EVENT_DISCONNECT) { a -> Log.d(TAG, "disconnect: ${a.firstOrNull()}") }
            // Reaplica listener registrado antes do socket existir/reconectar
            progressListener?.let { l ->
                mSocket?.on("download_progress") { args ->
                    (args.firstOrNull() as? JSONObject)?.let(l)
                }
            }
            mSocket?.connect()
        } catch (e: Exception) {
            Log.e(TAG, "connect() falhou", e)
        }
    }

    /**
     * Envia comando ao PC via VPS e recebe o ack do daemon.
     * Callback roda na thread do socket — use Handler/mainScope para atualizar UI.
     */
    fun sendCommand(payload: JSONObject, onResult: ((ok: Boolean, error: String?, resp: JSONObject?) -> Unit)? = null) {
        try {
            val data = JSONObject()
            data.put("userId", USER_ID)
            data.put("payload", payload)

            if (onResult == null) {
                mSocket?.emit("send_to_pc", data)
                return
            }
            mSocket?.emit("send_to_pc", data, Ack { args ->
                val resp = args.firstOrNull() as? JSONObject
                val ok = resp?.optBoolean("ok") == true
                val err = resp?.optString("error")?.takeIf { it.isNotEmpty() }
                Log.d(TAG, "ack send_to_pc: ok=$ok err=$err")
                onResult(ok, err, resp)
            })
        } catch (e: Exception) {
            Log.e(TAG, "sendCommand falhou", e)
            onResult?.invoke(false, e.message, null)
        }
    }

    /** Inicia um download remoto no PC host. */
    fun sendInstallCommand(
        gameName: String,
        magnetLink: String,
        onResult: ((ok: Boolean, error: String?, resp: JSONObject?) -> Unit)? = null
    ) {
        val payload = JSONObject()
            .put("action", "iniciar_download")
            .put("gameName", gameName)
            .put("magnetLink", magnetLink)
        sendCommand(payload, onResult)
    }

    /** Pede ao PC a lista de downloads ativos. */
    fun requestDownloads(onResult: (ok: Boolean, resp: JSONObject?) -> Unit) {
        sendCommand(JSONObject().put("action", "listar_downloads")) { ok, _, resp ->
            onResult(ok, resp)
        }
    }

    /** Ação sobre um download existente (pausar_download, retomar_download, remover_download). */
    fun downloadAction(action: String, id: String, deleteFiles: Boolean = false, onResult: ((Boolean) -> Unit)? = null) {
        val payload = JSONObject()
            .put("action", action)
            .put("id", id)
            .put("deleteFiles", deleteFiles)
        sendCommand(payload) { ok, _, _ -> onResult?.invoke(ok) }
    }

    /** Registra listener de progresso vindo do PC (evento `download_progress`). */
    fun setDownloadProgressListener(listener: ((JSONObject) -> Unit)?) {
        progressListener = listener
        mSocket?.off("download_progress")
        if (listener != null) {
            mSocket?.on("download_progress") { args ->
                (args.firstOrNull() as? JSONObject)?.let(listener)
            }
        }
    }

    fun disconnect() {
        mSocket?.off()
        mSocket?.disconnect()
        mSocket = null
    }
}
