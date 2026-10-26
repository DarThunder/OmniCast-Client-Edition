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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.omnicast.data.MediaCache
import app.omnicast.data.PlaylistDetail
import app.omnicast.data.ServiceLocator
import app.omnicast.friendly
import app.omnicast.ui.components.MessageBox
import app.omnicast.ui.components.VideoRow
import app.omnicast.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun PlaylistScreen(id: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var detail by remember { mutableStateOf<PlaylistDetail?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        try {
            val d = ServiceLocator.api.playlist(id)
            MediaCache.put(d.items)
            detail = d
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.friendly()
        }
    }
    LaunchedEffect(id) { load() }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Volver") }
        }

        val d = detail
        when {
            error != null && d == null -> Modifier.MessageBox(error!!)
            d == null -> Modifier.MessageBox("Cargando…")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(d.name, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        if (d.description.isNotBlank()) {
                            Text(d.description, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text(
                            "${d.itemCount} video${if (d.itemCount == 1) "" else "s"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        d.items.firstOrNull()?.let { first ->
                            Button(
                                onClick = { onOpen(first.key) },
                                colors = ButtonDefaults.buttonColors(containerColor = Palette.TextPrimary, contentColor = Palette.Background),
                                modifier = Modifier.padding(top = 12.dp),
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null)
                                Text("Reproducir", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
                items(d.items, key = { it.key }) { v ->
                    VideoRow(
                        v,
                        onClick = { onOpen(v.key) },
                        trailing = {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        ServiceLocator.api.removeFromPlaylist(id, v.key)
                                        load()
                                    } catch (e: Exception) {
                                        error = e.friendly()
                                    }
                                }
                            }) { Icon(Icons.Rounded.Close, "Quitar", tint = Palette.Titanium) }
                        },
                    )
                }
            }
        }
    }
}
