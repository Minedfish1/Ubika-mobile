package com.ubika.movel.model

data class Game(
    val id: String,
    val title: String,
    val coverUrl: String,
    val description: String,
    val requirements: String = "• SO: Windows 10/11\n• RAM: 8/16 GB\n• DirectX 12\n• GPU Dedicada",
    val versions: List<GameVersion>
)

data class GameVersion(
    val source: String,
    val size: String,
    val magnet: String,
    /** Links diretos (fontes sem magnet — steamgg, datanodes...). */
    val links: List<String> = emptyList()
)
