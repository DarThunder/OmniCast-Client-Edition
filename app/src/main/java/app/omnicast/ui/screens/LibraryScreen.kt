package app.omnicast.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.omnicast.data.MediaCache
import app.omnicast.data.PlaylistBody
import app.omnicast.data.PlaylistSummary
import app.omnicast.data.ServiceLocator
import app.omnicast.data.Video
import app.omnicast.friendly
import app.omnicast.ui.components.MessageBox
import app.omnicast.ui.components.OmniTopBar
import app.omnicast.ui.components.Thumb
import app.omnicast.ui.theme.Palette
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

class LibraryVm : ViewModel() {
    var favorites by mutableStateOf<List<Video>>(emptyList()); private set
    var playlists by mutableStateOf<List<PlaylistSummary>>(emptyList()); private set
    var error by mutableStateOf<String?>(null); private set
    var loaded by mutableStateOf(false); private set

    fun load() {
        viewModelScope.launch {
            try {
                val f = ServiceLocator.api.favorites().favorites
                MediaCache.put(f)
                favorites = f
                playlists = ServiceLocator.api.playlists().playlists
                error = null
            } catch (e: Exception) {
                error = e.friendly()
            }
            loaded = true
        }
    }

    fun create(name: String, description: String) {
        viewModelScope.launch {
            try {
                ServiceLocator.api.createPlaylist(PlaylistBody(name.trim(), description.trim()))
                load()
            } catch (e: Exception) {
                error = e.friendly()
            }
        }
    }
}

@Composable
fun LibraryScreen(
    onOpenVideo: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onSettings: () -> Unit,
    vm: LibraryVm = viewModel(),
) {
    val settings by ServiceLocator.settings.state.collectAsState()
    LaunchedEffect(settings.sessionKey, settings.configured) { if (settings.configured) vm.load() }
    var showCreate by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        OmniTopBar(onSearch = {}, onAvatar = onSettings)

        if (vm.error != null) {
            Modifier.MessageBox(vm.error!!, "Reintentar", vm::load)
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { SectionTitle("Favoritos") }
            item {
                if (vm.favorites.isEmpty()) {
                    Text(
                        if (vm.loaded) "Aún no tienes favoritos." else "Cargando…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(vm.favorites, key = { it.key }) { v ->
                            Column(Modifier.width(168.dp).clickable { onOpenVideo(v.key) }) {
                                Thumb(
                                    ServiceLocator.thumbUrl(v.key),
                                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                                    v.duration,
                                )
                                Text(
                                    v.title ?: "Sin título",
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                v.uploader?.let {
                                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Playlists", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCreate = true }) {
                        Icon(Icons.Rounded.Add, null, tint = Palette.Accent)
                        Text("Nueva", color = Palette.Accent)
                    }
                }
            }
            items(vm.playlists, key = { it.id }) { p ->
                PlaylistRow(p, onClick = { onOpenPlaylist(p.id) })
            }
            if (vm.loaded && vm.playlists.isEmpty()) {
                item {
                    Text(
                        "Crea tu primera playlist.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(
            onDismiss = { showCreate = false },
            onCreate = { n, d -> showCreate = false; vm.create(n, d) },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
}

@Composable
fun PlaylistRow(p: PlaylistSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(120.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)).background(Palette.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            val cover = ServiceLocator.absolute(p.coverUrl)
            if (cover != null) {
                AsyncImage(cover, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Rounded.PlaylistPlay, null, tint = Palette.Titanium, modifier = Modifier.size(32.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(p.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${p.itemCount} video${if (p.itemCount == 1) "" else "s"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.SurfaceHigh,
        title = { Text("Nueva playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(desc, { desc = it }, label = { Text("Descripción (opcional)") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, desc) }, enabled = name.isNotBlank()) {
                Text("Crear", color = Palette.Accent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Palette.Titanium) } },
    )
}
