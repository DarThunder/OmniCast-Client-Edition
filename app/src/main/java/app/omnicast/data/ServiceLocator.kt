package app.omnicast.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val SERVER_HOST = "server.invalid"
private const val OSIRIS_HOST = "osiris.invalid"

private class RewriteInterceptor(private val settings: SettingsStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val (baseUrl, msg) = when (req.url.host) {
            SERVER_HOST -> settings.current.serverUrl to "Configura la URL del servidor en Ajustes"
            OSIRIS_HOST -> settings.current.osirisUrl to "Configura la URL de OSIRIS en Ajustes"
            else -> return chain.proceed(req)
        }
        val base = baseUrl.toHttpUrlOrNull() ?: throw IOException(msg)
        val url = req.url.newBuilder()
            .scheme(base.scheme).host(base.host).port(base.port)
            .encodedPath(base.encodedPath.trimEnd('/') + req.url.encodedPath)
            .build()
        return chain.proceed(req.newBuilder().url(url).build())
    }
}

private fun Request.isForServer(settings: SettingsStore): Boolean {
    val base = settings.current.serverUrl.toHttpUrlOrNull() ?: return false
    return url.host == base.host && url.port == base.port
}

private class AuthInterceptor(
    private val settings: SettingsStore,
    private val tokens: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var req = chain.request()
        if (req.isForServer(settings)) {
            tokens.ensureFresh()
            val token = settings.current.accessToken
            if (token.isNotBlank()) req = req.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(req)
    }
}

private class TokenAuthenticator(
    private val settings: SettingsStore,
    private val tokens: TokenManager,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (!response.request.isForServer(settings)) return null
        if (response.priorResponse != null) return null // ya reintentamos
        val stale = response.request.header("Authorization")?.removePrefix("Bearer ").orEmpty()
        val fresh = tokens.refresh(stale) ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }
}

object ServiceLocator {
    lateinit var settings: SettingsStore
        private set
    lateinit var http: OkHttpClient
        private set
    lateinit var api: OmniApi
        private set
    lateinit var authApi: AuthApi
        private set
    lateinit var downloads: DownloadManager
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(context: Context) {
        settings = SettingsStore(context.applicationContext)

        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
            encodeDefaults = true
        }
        val converter = json.asConverterFactory("application/json".toMediaType())

        // Cliente de OSIRIS: sin Bearer ni authenticator (evita bucles al refrescar).
        val authHttp = OkHttpClient.Builder()
            .addInterceptor(RewriteInterceptor(settings))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        authApi = Retrofit.Builder()
            .baseUrl("http://$OSIRIS_HOST/")
            .client(authHttp)
            .addConverterFactory(converter)
            .build()
            .create(AuthApi::class.java)

        val tokens = TokenManager(settings, authApi)

        http = OkHttpClient.Builder()
            .addInterceptor(RewriteInterceptor(settings))
            .addInterceptor(AuthInterceptor(settings, tokens))
            .authenticator(TokenAuthenticator(settings, tokens))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // /discover usa yt-dlp y puede tardar
            .build()

        api = Retrofit.Builder()
            .baseUrl("http://$SERVER_HOST/")
            .client(http)
            .addConverterFactory(converter)
            .build()
            .create(OmniApi::class.java)

        downloads = DownloadManager(api, appScope)
    }

    suspend fun logout() {
        val rt = settings.current.refreshToken
        if (rt.isNotBlank()) {
            try {
                authApi.logout(RefreshBody(rt))
            } catch (e: Exception) {
            }
        }
        settings.clearSession()
    }

    fun absolute(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return settings.current.serverUrl + path
    }

    fun thumbUrl(hash: String): String = settings.current.serverUrl + "/thumbnail/$hash"
    fun videoUrl(hash: String): String = settings.current.serverUrl + "/video/$hash"
    fun pfpUrl(): String = settings.current.serverUrl + "/users/me/pfp"

    fun saveHistory(hash: String, positionMs: Long) {
        appScope.launch {
            try {
                api.saveHistory(hash, HistoryBody(positionMs))
            } catch (e: Exception) {
            }
        }
    }
}
