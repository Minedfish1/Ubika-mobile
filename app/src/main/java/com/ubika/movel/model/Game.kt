package com.ubika.movel.model

data class Game(
    val id: String,
    val title: String,
    val coverUrl: String,
    val description: String,
    // Gson instancia sem construtor — campos ausentes no JSON chegam null
    // mesmo declarados non-null; trato na UI com ?: default.
    val requirements: String? = null,
    /** Instalado no PC — o detalhe mostra só JOGAR (não reinstala). */
    val installed: Boolean? = null,
    /** Tem executável localizado — habilita o JOGAR de verdade. */
    val playable: Boolean? = null,
    val installId: String? = null,
    val versions: List<GameVersion>? = null
) {
    fun requirementsOrDefault() = requirements ?: "• SO: Windows 10/11\n• RAM: 8/16 GB\n• DirectX 12\n• GPU dedicada"
    fun versionsOrEmpty() = versions ?: emptyList()
}

data class GameVersion(
    val source: String,
    val size: String,
    val magnet: String? = null,
    /** Links diretos (fontes sem magnet — steamgg, datanodes...). */
    val links: List<String>? = null
) {
    fun magnetOrEmpty() = magnet ?: ""
    fun linksOrEmpty() = links ?: emptyList()
}
