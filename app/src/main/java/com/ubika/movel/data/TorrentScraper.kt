package com.ubika.movel.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

data class TorrentResult(
    val title: String,
    val size: String,
    val seeders: Int,
    val magnetLink: String
)

object TorrentScraper {

    /**
     * Motor de busca genérico com JSoup.
     * Ele entra na página de resultados da comunidade, encontra as linhas da tabela
     * e extrai o Magnet Link diretamente.
     */
    suspend fun searchTorrents(query: String): List<TorrentResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<TorrentResult>()
        
        try {
            // NOTA: URL Genérica de exemplo. Você deve adaptar para a fonte P2P da sua escolha.
            // Exemplo genérico: https://site-comunidade.com/search/$query/1/
            val searchUrl = "https://example-torrent-site.com/search/${query.replace(" ", "+")}/1/"
            
            // Conecta e baixa o HTML da página (Disfarçado de navegador para evitar bloqueio)
            val doc = Jsoup.connect(searchUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(10000)
                .get()

            // ATENÇÃO: Os seletores CSS (.table-list tbody tr) variam dependendo do site alvo.
            // Estes são apenas um padrão clássico de sites indexadores.
            val rows = doc.select(".table-list tbody tr")

            for (row in rows) {
                // Extrai Título
                val titleNode = row.select(".name a:not(.icon)").first()
                val title = titleNode?.text() ?: "Desconhecido"

                // Extrai Magnet Link (Geralmente no href de um ícone de imã)
                val magnetNode = row.select("a[href^=magnet:]").first()
                val magnetLink = magnetNode?.attr("href") ?: ""

                // Extrai Tamanho
                val sizeNode = row.select(".size").first()
                val size = sizeNode?.text() ?: "0 GB"

                // Extrai Seeders
                val seedsNode = row.select(".seeds").first()
                val seeders = seedsNode?.text()?.toIntOrNull() ?: 0

                // Só adiciona se encontrou um Magnet Link válido
                if (magnetLink.isNotEmpty()) {
                    results.add(TorrentResult(title, size, seeders, magnetLink))
                }
                
                // Limite de 15 resultados para não travar a interface mobile
                if (results.size >= 15) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return@withContext results
    }
}
