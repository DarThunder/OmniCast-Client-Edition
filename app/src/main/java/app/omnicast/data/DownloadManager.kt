package app.omnicast.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kotlin.time.Duration.Companion.milliseconds

data class DlState(
    val status: String = "starting",
    val percent: Float = 0f,
    val detail: String = "",
    val done: Boolean = false,
    val failed: Boolean = false,
) {
    val active: Boolean get() = !done && !failed
}

class DownloadManager(private val api: OmniApi, private val scope: CoroutineScope) {
    private val _states = MutableStateFlow<Map<String, DlState>>(emptyMap())
    val states: StateFlow<Map<String, DlState>> = _states.asStateFlow()

    private fun set(url: String, s: DlState) = _states.update { it + (url to s) }

    fun start(url: String) {
        if (_states.value[url]?.active == true) return
        set(url, DlState(detail = "Iniciando…"))

        scope.launch {
            try {
                val taskId = api.download(url).taskId
                var tries = 0
                while (tries++ < 600) {
                    delay(1500.milliseconds)
                    val p = try {
                        api.progress(taskId)
                    } catch (e: HttpException) {
                        if (e.code() == 404) {
                            set(url, DlState(status = "expired", failed = true, detail = "La tarea expiró"))
                            return@launch
                        }
                        throw e
                    }
                    val status = (p["status"] ?: "").lowercase()
                    val pct = (p["percent"] ?: "0").replace("%", "").trim().toFloatOrNull() ?: 0f
                    val detail = listOfNotNull(p["speed"], p["eta"]?.let { "ETA $it" }).joinToString(" · ")

                    when {
                        status == "completed" -> {
                            set(url, DlState(status, 1f, "Listo", done = true))
                            return@launch
                        }
                        isFailure(status) -> {
                            set(url, DlState(status, pct / 100f, p["error"] ?: "Falló la descarga", failed = true))
                            return@launch
                        }
                        else -> set(url, DlState(status, (pct / 100f).coerceIn(0f, 1f), detail))
                    }
                }
                set(url, DlState(status = "timeout", failed = true, detail = "Sin respuesta del servidor"))
            } catch (e: Exception) {
                set(url, DlState(status = "error", failed = true, detail = e.message ?: "Error"))
            }
        }
    }

    private fun isFailure(status: String) = status == "error"
}
