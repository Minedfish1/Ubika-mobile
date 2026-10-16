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

    private var mSocket: Socket? = null
    private var progressListener: ((JSONObject) -> Unit)? = null
    private var userId: String = ""
    private var clientRes: Triple<Int, Int, Int>? = null // width, height, fps do aparelho

    val isConnected: Boolean
        get() = mSocket?.connected() == true

    fun connect(userId: String = "") {
        if (userId.isNotEmpty()) this.userId = userId
        if (mSocket?.connected() == true) return
        // Sem sessão não conecta: o segredo compartilhado é só do daemon do PC —
        // se saísse no APK, qualquer um registrava PC falso na conta alheia.
        val sess = UbikaApi.sessionToken
        if (sess.isEmpty()) return
        // Socket morto/meio-aberto (app voltou do background, queda de rede):
        // descarta o objeto velho senão os listeners duplicam na reconexão
        mSocket?.off()
        mSocket?.disconnect()
        mSocket = null
        try {
            val opts = IO.Options().apply {
                query = "device=android&userId=${this@UbikaSocket.userId}"
                reconnection = true
                reconnectionDelay = 1000
                reconnectionDelayMax = 10000
                timeout = 10000
                auth = mapOf("session" to sess)
            }
            mSocket = IO.socket(SERVER_URL, opts)
            mSocket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "conectado: ${mSocket?.id()}")
                announceClient() // primeira ação: PC fica sabendo a resolução exata
            }
            mSocket?.on(Socket.EVENT_CONNECT_ERROR) { a -> Log.e(TAG, "connect_error: ${a.firstOrNull()}") }
            mSocket?.on(Socket.EVENT_DISCONNECT) { a -> Log.d(TAG, "disconnect: ${a.firstOrNull()}") }
            // PC apareceu → reanuncia a resolução (ele pode ter trocado de sessão)
            mSocket?.on("pc_status") { args ->
                val obj = args.firstOrNull() as? JSONObject ?: return@on
                if (obj.optBoolean("online")) announceClient()
            }
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
            data.put("userId", userId)
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

    /** Inicia um download remoto no PC host — magnet ou link direto (fontes sem torrent). */
    fun sendInstallCommand(
        gameName: String,
        magnetLink: String,
        directLink: String = "",
        onResult: ((ok: Boolean, error: String?, resp: JSONObject?) -> Unit)? = null
    ) {
        val payload = JSONObject()
            .put("action", "iniciar_download")
            .put("gameName", gameName)
            .put("magnetLink", magnetLink)
            .put("link", directLink)
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

    /** Guarda a resolução real do aparelho (chame uma vez com os metrics reais). */
    fun setClientResolution(width: Int, height: Int, fps: Int) {
        clientRes = Triple(width, height, fps)
        announceClient()
    }

    /** Envia ao PC a resolução exata do celular — display virtual acompanha. */
    fun announceClient() {
        val (w, h, fps) = clientRes ?: return
        if (mSocket?.connected() != true) return
        val payload = JSONObject()
            .put("action", "preparar_stream")
            .put("width", w).put("height", h).put("fps", fps)
        sendCommand(payload) { ok, err, _ ->
            Log.d(TAG, "preparar_stream ${w}x${h}@${fps} ok=$ok err=$err")
        }
    }

    /** Pede ao PC para voltar o display ao normal (se não houver stream). */
    fun restoreDisplay() {
        sendCommand(JSONObject().put("action", "restaurar_display"))
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
        restoreDisplay()
        mSocket?.off()
        mSocket?.disconnect()
        mSocket = null
    }
}
