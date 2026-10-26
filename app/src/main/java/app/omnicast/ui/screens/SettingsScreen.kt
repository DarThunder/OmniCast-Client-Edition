package app.omnicast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.omnicast.data.LoginBody
import app.omnicast.data.SettingsStore
import app.omnicast.data.ServiceLocator
import app.omnicast.friendly
import app.omnicast.ui.components.OmniLogo
import app.omnicast.ui.theme.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException

/** Si no se indica la URL de OSIRIS, se asume el mismo host que el servidor en el puerto 8090. */
private fun deriveOsiris(server: String): String {
    val u = SettingsStore.normalizeUrl(server).toHttpUrlOrNull() ?: return ""
    return "${u.scheme}://${u.host}:8090"
}

private fun loginError(e: Exception): String = when {
    e is HttpException && e.code() == 401 -> "Usuario o contraseña incorrectos."
    e is HttpException && e.code() == 400 -> "Audiencia no permitida. Revisa el campo Audiencia."
    e is HttpException && e.code() == 429 -> "Demasiados intentos, espera un momento."
    else -> e.friendly()
}

@Composable
fun SettingsScreen(onBack: () -> Unit, onSaved: () -> Unit) {
    val session by ServiceLocator.settings.state.collectAsState()
    val scope = rememberCoroutineScope()

    var server by remember { mutableStateOf(session.serverUrl) }
    var osiris by remember { mutableStateOf(session.osirisUrl) }
    var audience by remember { mutableStateOf(session.audience) }
    var username by remember { mutableStateOf(session.username) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Palette.Accent,
        unfocusedBorderColor = Palette.Divider,
        focusedLabelColor = Palette.Accent,
        cursorColor = Palette.Accent,
    )

    Column(Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (session.configured) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Volver") }
            }
            Text(
                "Ajustes",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = if (session.configured) 0.dp else 16.dp, top = 12.dp, bottom = 12.dp),
            )
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OmniLogo()

            if (session.configured) {
                // ---- Sesión iniciada ----
                Text("Sesión iniciada como ${session.username}", fontWeight = FontWeight.Medium)
                Text(session.serverUrl, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                OutlinedButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        scope.launch {
                            ServiceLocator.logout() // la navegación a login ocurre sola al perder la sesión
                            busy = false
                            password = ""
                        }
                    },
                ) { Text("Cerrar sesión", color = Palette.TextPrimary) }
            } else {
                // ---- Login ----
                Text("Conecta con tu servidor", color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(
                    value = server,
                    onValueChange = { server = it },
                    label = { Text("Servidor OmniCast") },
                    placeholder = { Text("http://192.168.1.50:8000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = osiris,
                    onValueChange = { osiris = it },
                    label = { Text("Servidor OSIRIS") },
                    placeholder = { Text(deriveOsiris(server).ifEmpty { "http://192.168.1.50:8090" }) },
                    supportingText = { Text("Vacío = mismo host, puerto 8090") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = audience,
                    onValueChange = { audience = it },
                    label = { Text("Audiencia") },
                    supportingText = { Text("Debe coincidir con OSIRIS_AUDIENCE del servidor") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )

                error?.let { Text(it, color = Palette.Error, fontSize = 13.sp) }

                Button(
                    enabled = !busy && server.isNotBlank() && username.isNotBlank() && password.isNotEmpty(),
                    onClick = {
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val osirisUrl = osiris.ifBlank { deriveOsiris(server) }
                                ServiceLocator.settings.saveEndpoints(server, osirisUrl, audience)
                                val aud = ServiceLocator.settings.current.audience
                                val user = username.trim()
                                val tokens = ServiceLocator.authApi.login(LoginBody(user, password, aud))
                                ServiceLocator.settings.saveSession(user, tokens)
                                password = ""
                                onSaved()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = loginError(e)
                            }
                            busy = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.OnAccent),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (busy) "Entrando…" else "Iniciar sesión") }
            }
        }
    }
}
