# AGENTS.md — Ubika-mobile

App Android do Ubika (cliente de gaming remoto). O contexto completo da plataforma — VPS, daemon do PC, display virtual, downloads, endpoints e segredos — está no `AGENTS.md` do repo principal: `github.com/Minedfish1/Ubika`.

## Stack

Kotlin + Jetpack Compose. Coil p/ capas (DiskCache 512MB em `UbikaApp`). Socket.IO client em `UbikaSocket.kt`. Gamepad-friendly (`GamepadManager`), sons suaves (`SoundManager`).

## Arquivos-chave

- `network/UbikaApi.kt` — REST da VPS (`x-ubika-session`, `x-ubika-key`)
- `network/UbikaSocket.kt` — anuncia `preparar_stream` com `realDeviceRes` (resolução real do celular), pareamento
- `ui/CatalogScreen.kt` — pódio top-3 + EM ALTA + prateleiras por gênero (`getTrendingGames(genre)`); jogo trending sem `versions` → busca na biblioteca ao abrir detalhe
- `ui/StreamScreen.kt` — card "PAREAR PC" + diagnóstico + `JOGAR` → `moonlight://[IPv6]` (prefere v6 > v4 > LAN)
- `ui/ProfileSheet.kt` — só foto + email + nome editável (`POST /api/me`, avatar base64)
- `ui/MainConsoleScreen.kt`, `ui/GameDetailScreen.kt`, `ui/DownloadsScreen.kt`

## Build

```bash
export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"  # PATH não tem java
./gradlew assembleDebug
# instala: C:\Users\Ian\AppData\Local\Android\Sdk\platform-tools\adb.exe install app-debug.apk
```

## Regras

- Stream sempre direto PC↔celular (VPS só faz match). Perfil = usuário+nome+foto; pareamento fica na aba Stream. UI pt-BR.
