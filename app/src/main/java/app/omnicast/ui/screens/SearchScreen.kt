package app.omnicast.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.omnicast.data.MediaCache
import app.omnicast.data.ServiceLocator
import app.omnicast.data.Video
import app.omnicast.friendly
import app.omnicast.ui.components.MessageBox
import app.omnicast.ui.components.VideoRow
import app.omnicast.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Video>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList(); searched = false; error = null
            return@LaunchedEffect
        }
        delay(300.milliseconds)
        try {
            val r = ServiceLocator.api.search(query.trim())
            MediaCache.put(r)
            results = r
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
        }
        searched = true
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Volver") }
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar en tu biblioteca") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Borrar") }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Palette.SurfaceHigh,
                    unfocusedContainerColor = Palette.SurfaceHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = Palette.Accent,
                ),
                modifier = Modifier.weight(1f).focusRequester(focus),
            )
        }

        when {
            error != null -> Modifier.MessageBox(error!!)
            searched && results.isEmpty() -> Modifier.MessageBox("Sin resultados en tu biblioteca. Prueba en Explorar para descargarlo.")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(results, key = { it.key }) { v -> VideoRow(v, onClick = { onOpen(v.key) }) }
            }
        }
    }
}
