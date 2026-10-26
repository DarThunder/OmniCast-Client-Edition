# OmniCast for Android

Native app (Kotlin + Jetpack Compose) for your OmniCast/OmniTune server, with a YouTube-style interface and your color palette.

## How to open it

1. Android Studio (Ladybug or newer) → **Open** → `OmniCastAndroid` folder.
2. Let Gradle sync (the wrapper is generated automatically; uses JDK 17).
3. Run on your phone or emulator (minSdk 26).
4. On first launch you'll see the login screen: OmniCast server URL, OSIRIS URL (empty = same host, port 8090), **audience** (the value of your server's `OSIRIS_AUDIENCE`; it must be among OSIRIS's allowed audiences), username and password.
   - On the emulator, your PC is `http://10.0.2.2:PORT`.

## Structure

```
data/        Retrofit (OmniApi), models, settings, download manager
ui/theme/    Palette and Material 3 theme
ui/components/  Video cards, top bar, logo
ui/screens/  Home, Discover, Library, Playlist, Player, Search, Settings
```

## Screen → endpoint map

| Screen             | Endpoints                                                                                                                                    |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------- |
| Home               | `GET /feed` (paginated with `next_skip`), `GET /users/me/favorites` (Favorites chip)                                                         |
| Search (magnifier) | `GET /search`                                                                                                                                |
| Discover           | `GET /discover`, `POST /videos?url=`, `GET /videos/progress/{id}`                                                                            |
| Library            | `GET /users/me/favorites`, `GET/POST /users/me/playlists`                                                                                    |
| Playlist           | `GET /users/me/playlists/{id}`, `DELETE .../items/{hash}`                                                                                    |
| Player             | `GET /video/{hash}` (Range), `GET/PATCH /users/me/history/{hash}`, `POST /users/me/favorites/{hash}`, `POST .../playlists/{id}/items/{hash}` |
