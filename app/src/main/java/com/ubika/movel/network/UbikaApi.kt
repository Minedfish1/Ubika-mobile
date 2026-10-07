package com.ubika.movel.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.ubika.movel.model.Game
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object UbikaApi {
    private const val TAG = "UbikaApi"
    private const val BASE_URL = "http://18.223.108.137:3000/api"
    private const val USER_ID = "utilizador_teste_123" // tem de ser igual ao do client.js

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS) // maior que os 20s de espera da AWS pelo PC
        .addInterceptor(HttpLoggingInterceptor { Log.d("UbikaHTTP", it) }
            .apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private val gson = Gson()

    private fun buildUrl(path: String, vararg params: Pair<String, String>) =
        "$BASE_URL/$path".toHttpUrl().newBuilder().apply {
            addQueryParameter("userId", USER_ID)
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
                val lista: List<Game> = gson.fromJson(array, type) ?: emptyList()
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
                    // Protege contra null usando .orEmpty()
                    val seguro = CatalogResponse(resp?.games.orEmpty(), resp?.isLastPage ?: true)
                    Log.d(TAG, "catalogo: ${seguro.games.size} jogos, last=${seguro.isLastPage}")
                    seguro
                }
            } catch (e: Exception) {
                Log.e(TAG, "getCatalogGames falhou: ${e::class.simpleName}: ${e.message}", e)
                throw e
            }
        }
}

// Corrigido para garantir que games é sempre uma lista não nula
data class CatalogResponse(
    val games: List<Game> = emptyList(),
    val isLastPage: Boolean = true
)