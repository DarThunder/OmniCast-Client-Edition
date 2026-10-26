package app.omnicast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

@Serializable
data class LoginBody(val username: String, val password: String, val audience: String)

@Serializable
data class RefreshBody(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_in") val refreshExpiresIn: Long = 0,
    val audience: String = "",
)

/** API de OSIRIS (servicio aparte del servidor de video, normalmente en :8090). */
interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginBody): TokenResponse

    // Síncrono a propósito: se llama desde interceptor/authenticator de OkHttp.
    @POST("api/v1/auth/refresh")
    fun refresh(@Body body: RefreshBody): Call<TokenResponse>

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshBody): MessageResponse
}

/**
 * Renueva el access token con el refresh token.
 *
 * OSIRIS rota los refresh tokens (un solo uso) y revoca toda la sesión si se reutiliza uno,
 * así que TODA renovación pasa por este lock y se re-evalúa dentro de él.
 */
class TokenManager(private val settings: SettingsStore, private val authApi: AuthApi) {
    private val lock = Any()

    /** Renueva de forma proactiva si el access token vence en menos de 30 s. */
    fun ensureFresh() {
        val s = settings.current
        if (s.refreshToken.isNotBlank() && s.accessExp - nowSec() < 30) refresh(s.accessToken)
    }

    /** Devuelve un access token válido, o null si no se pudo renovar. */
    fun refresh(staleAccess: String): String? = synchronized(lock) {
        val s = settings.current
        if (s.refreshToken.isBlank()) return null
        // otro hilo ya renovó mientras esperábamos el lock
        if (s.accessToken != staleAccess && s.accessExp - nowSec() > 5) return s.accessToken

        try {
            val r = authApi.refresh(RefreshBody(s.refreshToken)).execute()
            val body = r.body()
            if (r.isSuccessful && body != null) {
                settings.saveSession(null, body)
                body.accessToken
            } else {
                if (r.code() == 401) settings.clearSession() // sesión revocada/expirada → volver a login
                null
            }
        } catch (e: IOException) {
            null // sin red: se conserva la sesión y se reintentará
        }
    }

    private fun nowSec() = System.currentTimeMillis() / 1000
}
