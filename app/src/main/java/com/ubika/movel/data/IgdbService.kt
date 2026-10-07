package com.ubika.movel.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// Modelos de Resposta da IGDB
data class IgdbGame(
    val id: Long,
    val name: String,
    val cover: IgdbCover?
)

data class IgdbCover(
    val id: Long,
    val url: String?
)

// Interface da API usando Retrofit
interface IgdbApi {
    @POST("games")
    suspend fun searchGames(
        @Header("Client-ID") clientId: String,
        @Header("Authorization") auth: String, // Bearer Token
        @Body query: String
    ): List<IgdbGame>
}

// Singleton do Cliente Retrofit
object IgdbClient {
    private const val BASE_URL = "https://api.igdb.com/v4/"

    // INSIRA SUAS CHAVES DA TWITCH/IGDB AQUI
    var clientId = "SEU_CLIENT_ID_AQUI"
    var accessToken = "SEU_APP_ACCESS_TOKEN_AQUI"

    val api: IgdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IgdbApi::class.java)
    }

    suspend fun searchGameByName(name: String): List<IgdbGame> {
        // Query no formato da IGDB (Apicalypse)
        val query = "search \"$name\"; fields name, cover.url; limit 10;"
        return try {
            api.searchGames(clientId, "Bearer $accessToken", query)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // A IGDB retorna URLs como "//images.igdb.com/...", então precisamos formatar
    fun formatCoverUrl(url: String?, size: String = "t_1080p"): String {
        if (url == null) return ""
        val httpsUrl = if (url.startsWith("//")) "https:$url" else url
        return httpsUrl.replace("t_thumb", size) // Troca a miniatura pela versão HD
    }
}
