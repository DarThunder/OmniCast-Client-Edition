package app.omnicast

import retrofit2.HttpException
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun formatDuration(seconds: Double?): String {
    if (seconds == null || seconds <= 0) return ""
    val t = seconds.toLong()
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun plural(n: Long, one: String, many: String) = if (n == 1L) "hace 1 $one" else "hace $n $many"

fun timeAgo(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val instant: Instant = try {
        OffsetDateTime.parse(iso).toInstant()
    } catch (e: Exception) {
        try {
            LocalDateTime.parse(iso.trim().replace(' ', 'T')).toInstant(ZoneOffset.UTC)
        } catch (e2: Exception) {
            return ""
        }
    }
    val mins = Duration.between(instant, Instant.now()).toMinutes()
    return when {
        mins < 1 -> "justo ahora"
        mins < 60 -> "hace $mins min"
        mins < 60 * 24 -> plural(mins / 60, "hora", "horas")
        mins < 60 * 24 * 7 -> plural(mins / (60 * 24), "día", "días")
        mins < 60 * 24 * 30 -> plural(mins / (60 * 24 * 7), "semana", "semanas")
        mins < 60 * 24 * 365 -> plural(mins / (60 * 24 * 30), "mes", "meses")
        else -> plural(mins / (60 * 24 * 365), "año", "años")
    }
}

fun Throwable.friendly(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Tu sesión expiró. Inicia sesión de nuevo."
        403 -> "No tienes permiso para esto."
        429 -> "Demasiados intentos, espera un momento."
        404 -> "No encontrado."
        502, 503 -> "El servidor no está disponible."
        else -> "Error del servidor (${code()})."
    }
    is IOException -> message?.takeIf { it.startsWith("Configura") } ?: "No se pudo conectar con el servidor."
    else -> message ?: "Error inesperado."
}
