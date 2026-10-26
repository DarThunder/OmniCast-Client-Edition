package app.omnicast.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Settings(
    val serverUrl: String = "",
    val osirisUrl: String = "",
    val audience: String = "omnicast",
    val username: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val accessExp: Long = 0, // epoch en segundos
) {
    /** Hay sesión iniciada y servidor configurado. */
    val configured: Boolean get() = serverUrl.isNotBlank() && refreshToken.isNotBlank()
    val sessionKey: String get() = "$serverUrl|$username"
}

/** Endpoints + sesión de OSIRIS. Lectura síncrona para poder usarse desde interceptores. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("omnicast", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        Settings(
            serverUrl = prefs.getString("server_url", "") ?: "",
            osirisUrl = prefs.getString("osiris_url", "") ?: "",
            audience = prefs.getString("audience", "omnicast") ?: "omnicast",
            username = prefs.getString("username", "") ?: "",
            accessToken = prefs.getString("access_token", "") ?: "",
            refreshToken = prefs.getString("refresh_token", "") ?: "",
            accessExp = prefs.getLong("access_exp", 0),
        )
    )
    val state: StateFlow<Settings> = _state.asStateFlow()
    val current: Settings get() = _state.value

    @Synchronized
    fun saveEndpoints(serverUrl: String, osirisUrl: String, audience: String) {
        val s = current.copy(
            serverUrl = normalizeUrl(serverUrl),
            osirisUrl = normalizeUrl(osirisUrl),
            audience = audience.trim().ifEmpty { "omnicast" },
        )
        prefs.edit()
            .putString("server_url", s.serverUrl)
            .putString("osiris_url", s.osirisUrl)
            .putString("audience", s.audience)
            .apply()
        _state.value = s
    }

    /** Guarda el par de tokens. Si [username] es null conserva el actual (caso refresh). */
    @Synchronized
    fun saveSession(username: String?, t: TokenResponse) {
        val s = current.copy(
            username = username ?: current.username,
            accessToken = t.accessToken,
            refreshToken = t.refreshToken,
            accessExp = System.currentTimeMillis() / 1000 + t.expiresIn,
        )
        prefs.edit()
            .putString("username", s.username)
            .putString("access_token", s.accessToken)
            .putString("refresh_token", s.refreshToken)
            .putLong("access_exp", s.accessExp)
            .apply()
        _state.value = s
    }

    @Synchronized
    fun clearSession() {
        prefs.edit().remove("access_token").remove("refresh_token").remove("access_exp").apply()
        _state.value = current.copy(accessToken = "", refreshToken = "", accessExp = 0)
    }

    companion object {
        fun normalizeUrl(url: String): String {
            var u = url.trim().trimEnd('/')
            if (u.isNotEmpty() && !u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
            return u
        }
    }
}
