package com.ubika.movel.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.ubika.movel.BuildConfig
import com.ubika.movel.model.Game
import com.ubika.movel.model.GameVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object UbikaApi {
    private const val TAG = "UbikaApi"
    val SERVER = BuildConfig.UBIKA_SERVER.ifEmpty { "http://18.223.108.137:3000" }
    private val BASE_URL = "$SERVER/api"
    private val SHARED_SECRET = BuildConfig.UBIKA_SHARED_SECRET

    /** userId/token de sessão — preenchidos após login; fallback de dev. */
    var sessionToken: String = ""
    var userId: String = BuildConfig.UBIKA_USER_ID.ifEmpty { "utilizador_teste_123" }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS) // maior que os 20s de espera da AWS pelo PC
        .addInterceptor { chain ->
            val b = chain.request().newBuilder().header("x-ubika-token", SHARED_SECRET)
            if (sessionToken.isNotEmpty()) b.header("x-ubika-session", sessionToken)
            chain.proceed(b.build())
        }
        .addInterceptor(HttpLoggingInterceptor { Log.d("UbikaHTTP", it) }
            .apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val gson = Gson()

    private fun buildUrl(path: String, vararg params: Pair<String, String>) =
        "$BASE_URL/$path".toHttpUrl().newBuilder().apply {
            if (sessionToken.isNotEmpty()) addQueryParameter("session", sessionToken)
            else addQueryParameter("userId", userId)
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

    private fun errorMessage(code: Int, body: String?): String {
        val motivo = try {
            JsonParser.parseString(body ?: "").asJsonObject.get("error")?.asString
        } catch (e: Exception) { null }
        return "HTTP $code${motivo?.let { ": $it" } ?: ""}"
    }

    suspend fun getTrendingGames(): List<Game> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildUrl("trending")).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful) throw Exception(errorMessage(response.code, body))
                if (body.isNullOrBlank()) return@use emptyList<Game>()

                val root = JsonParser.parseString(body)
                // Aceita tanto { "games": [...] } como [ ... ]
                val array = if (root.isJsonArray) root.asJsonArray
                else root.asJsonObject.getAsJsonArray("games")

                val type = object : TypeToken<List<Game>>() {}.type
                val lista: List<Game> = (gson.fromJson(array, type) ?: emptyList<Game>())
                    .map(::sanitizeGame)
                Log.d(TAG, "trending: ${lista.size} jogos")
                lista
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTrendingGames falhou: ${e::class.simpleName}: ${e.message}", e)
            throw e
        }
    }

    suspend fun getCatalogGames(search: String = "", page: Int = 1): CatalogResponse =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(buildUrl("games", "search" to search, "page" to page.toString()))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                    if (!response.isSuccessful) throw Exception(errorMessage(response.code, body))
                    if (body.isNullOrBlank()) throw Exception("Corpo vazio")

                    val resp = gson.fromJson(body, CatalogResponse::class.java)
                    // Protege contra null usando .orEmpty() e normaliza cada Game
                    val seguro = CatalogResponse(
                        resp?.games.orEmpty().map(::sanitizeGame),
                        resp?.isLastPage ?: true
                    )
                    Log.d(TAG, "catalogo: ${seguro.games.size} jogos, last=${seguro.isLastPage}")
                    seguro
                }
            } catch (e: Exception) {
                Log.e(TAG, "getCatalogGames falhou: ${e::class.simpleName}: ${e.message}", e)
                throw e
            }
        }

    /**
     * Gson ignora a null-safety do Kotlin: campos declarados non-null chegam
     * null quando o JSON omite o campo (ex.: coverUrl) e estouram NPE no
     * hashCode/equals do data class. Normaliza tudo na fronteira.
     */
    @Suppress("USELESS_ELVIS")
    private fun sanitizeGame(g: Game?): Game {
        if (g == null) return Game("", "", "", "", versions = emptyList())
        return Game(
            id = g.id ?: "",
            title = g.title ?: "",
            coverUrl = g.coverUrl ?: "",
            description = g.description ?: "",
            requirements = g.requirements ?: "• SO: Windows 10/11\n• RAM: 8/16 GB\n• DirectX 12\n• GPU Dedicada",
            versions = (g.versions ?: emptyList()).map { v ->
                GameVersion(
                    source = v?.source ?: "",
                    size = v?.size ?: "",
                    magnet = v?.magnet ?: ""
                )
            }
        )
    }

    /** Status do PC host visto pelo relay: { online, streamHost }. */
    suspend fun getPcStatus(): PcStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildUrl("pc/status")).build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body.isNullOrBlank()) return@use PcStatus()
                val o = JsonParser.parseString(body).asJsonObject
                PcStatus(
                    online = o.get("online")?.asBoolean == true,
                    streamHost = o.get("streamHost")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getPcStatus falhou: ${e.message}")
            PcStatus()
        }
    }

    // ------------------------- Auth / Pareamento -------------------------

    private suspend fun authCall(path: String, email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            val body = """{"email":${com.google.gson.JsonPrimitive(email)},"password":${com.google.gson.JsonPrimitive(password)}}"""
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$BASE_URL/$path").post(body).build()
            try {
                client.newCall(request).execute().use { response ->
                    val respBody = response.body?.string()
                    if (!response.isSuccessful) return@withContext AuthResult(error = errorMessage(response.code, respBody))
                    val o = JsonParser.parseString(respBody).asJsonObject
                    AuthResult(
                        token = o.get("token")?.asString ?: "",
                        userId = o.get("userId")?.asString ?: "",
                        email = o.get("email")?.asString ?: email,
                        plan = o.get("plan")?.asString ?: ""
                    )
                }
            } catch (e: Exception) {
                AuthResult(error = e.message ?: "FALHA_DE_REDE")
            }
        }

    suspend fun register(email: String, password: String) = authCall("auth/register", email, password)
    suspend fun login(email: String, password: String) = authCall("auth/login", email, password)

    /** Pareia o PC cujo daemon exibe `code` (6 dígitos) à conta logada. */
    suspend fun pairPc(code: String): JsonObjectHolder = withContext(Dispatchers.IO) {
        val body = """{"code":${com.google.gson.JsonPrimitive(code)}}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/pair").post(body).build()
        client.newCall(request).execute().use { response ->
            val respBody = response.body?.string()
            if (!response.isSuccessful) throw Exception(errorMessage(response.code, respBody))
            JsonObjectHolder(respBody ?: "{}")
        }
    }

    /** Cria sessão de checkout web → retorna {sid, url} para abrir no navegador. */
    suspend fun createCheckout(plan: String = "pro"): CheckoutResult = withContext(Dispatchers.IO) {
        val body = """{"plan":${com.google.gson.JsonPrimitive(plan)}}"""
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/checkout").post(body).build()
        try {
            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string()
                if (!response.isSuccessful) return@withContext CheckoutResult(error = errorMessage(response.code, respBody))
                val o = JsonParser.parseString(respBody).asJsonObject
                CheckoutResult(
                    sid = o.get("sid")?.asString ?: "",
                    url = o.get("url")?.asString ?: ""
                )
            }
        } catch (e: Exception) {
            CheckoutResult(error = e.message)
        }
    }

    /** Status do checkout: "pending" | "paid" — polling após voltar do navegador. */
    suspend fun checkoutStatus(sid: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildUrl("checkout/$sid/status")).build()
        try {
            client.newCall(request).execute().use { response ->
                val o = JsonParser.parseString(response.body?.string() ?: "").asJsonObject
                o?.get("status")?.asString ?: "pending"
            }
        } catch (e: Exception) { "pending" }
    }

    /** Dados frescos da conta (plano atualizado após pagamento). */
    suspend fun getMe(): AuthResult = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(buildUrl("auth/me")).build()
        try {
            client.newCall(request).execute().use { response ->
                val o = JsonParser.parseString(response.body?.string() ?: "").asJsonObject
                AuthResult(
                    token = sessionToken,
                    userId = o?.get("userId")?.asString ?: "",
                    email = o?.get("email")?.asString ?: "",
                    plan = o?.get("plan")?.asString ?: ""
                )
            }
        } catch (e: Exception) { AuthResult(error = e.message) }
    }

    /** Jogos instalados = downloads concluídos no PC (state == "done"). */
    suspend fun getInstalledGames(): List<InstalledGame> = withContext(Dispatchers.IO) {
        try {
            val r = sendPcCommand("listar_downloads")
            val arr = r.obj?.getAsJsonArray("downloads") ?: return@withContext emptyList()
            arr.mapNotNull { el ->
                val o = el.asJsonObject
                if (o.get("state")?.asString != "done") return@mapNotNull null
                InstalledGame(
                    id = o.get("id")?.asString ?: "",
                    name = o.get("gameName")?.asString ?: "Jogo",
                    sizeBytes = o.get("length")?.asLong ?: 0L,
                    savePath = o.get("savePath")?.asString ?: "",
                    finishedAt = o.get("finishedAt")?.asLong ?: 0L
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "getInstalledGames falhou: ${e.message}")
            emptyList()
        }
    }

    /** POST /api/pc/command — retorna o JSON de resposta do PC ou lança exceção. */
    suspend fun sendPcCommand(action: String, payload: Map<String, Any> = emptyMap()): JsonObjectHolder =
        withContext(Dispatchers.IO) {
            val json = com.google.gson.JsonObject().apply {
                addProperty("userId", userId)
                addProperty("action", action)
                if (payload.isNotEmpty()) add("payload", gson.toJsonTree(payload))
            }
            val body = json.toString()
                .toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(buildUrl("pc/command")).post(body).build()

            client.newCall(request).execute().use { response ->
                val respBody = response.body?.string()
                if (!response.isSuccessful) throw Exception(errorMessage(response.code, respBody))
                JsonObjectHolder(respBody ?: "{}")
            }
        }
}

// Corrigido para garantir que games é sempre uma lista não nula
data class CatalogResponse(
    val games: List<Game> = emptyList(),
    val isLastPage: Boolean = true
)

data class PcStatus(
    val online: Boolean = false,
    val streamHost: String = ""
)

data class AuthResult(
    val token: String = "",
    val userId: String = "",
    val email: String = "",
    val plan: String = "",
    val error: String? = null
)

data class CheckoutResult(
    val sid: String = "",
    val url: String = "",
    val error: String? = null
)

data class InstalledGame(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val savePath: String,
    val finishedAt: Long
)

/** JSON bruto de resposta do PC — a tela extrai os campos que precisar. */
data class JsonObjectHolder(val raw: String) {
    val obj get() = try { JsonParser.parseString(raw).asJsonObject } catch (e: Exception) { null }
    fun str(key: String) = obj?.get(key)?.let { if (it.isJsonPrimitive) it.asString else it.toString() }
    fun bool(key: String) = obj?.get(key)?.asBoolean == true
}