package app.omnicast.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.omnicast.data.PlaylistSummary
import app.omnicast.data.ServiceLocator
import app.omnicast.data.Video
import app.omnicast.formatDuration
import app.omnicast.timeAgo
import app.omnicast.ui.theme.Palette
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun OmniLogo() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 30.dp, height = 21.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Palette.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Palette.OnAccent, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text("OmniCast", fontWeight = FontWeight.Bold, fontSize = 20.sp, letterSpacing = (-0.5).sp)
    }
}

@Composable
fun OmniTopBar(onSearch: () -> Unit, onAvatar: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OmniLogo()
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, "Buscar") }
        IconButton(onClick = onAvatar) {
            AsyncImage(
                model = ServiceLocator.pfpUrl(),
                contentDescription = "Perfil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Palette.SurfaceHigh),
            )
        }
    }
}

@Composable
fun ChannelAvatar(name: String?, size: Int = 36) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Palette.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            (name?.trim()?.firstOrNull() ?: '•').uppercase(),
            color = Palette.Accent,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.42f).sp,
        )
    }
}

@Composable
fun Thumb(url: String?, modifier: Modifier, duration: Double? = null) {
    Box(modifier.background(Palette.SurfaceHigh)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        val d = formatDuration(duration)
        if (d.isNotEmpty()) {
            Text(
                d,
                color = Palette.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Palette.Background.copy(alpha = 0.85f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
    }
}

private fun Video.subtitle(): String =
    listOf(uploader.orEmpty(), timeAgo(addedAt)).filter { it.isNotBlank() }.joinToString(" · ")

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Thumb(
            ServiceLocator.thumbUrl(video.key),
            Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            video.duration,
        )
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ChannelAvatar(video.uploader)
            Column(Modifier.weight(1f)) {
                Text(
                    video.title ?: "Sin título",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                val sub = video.subtitle()
                if (sub.isNotEmpty()) {
                    Text(
                        sub,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (onMenuClick != null) {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.MoreVert, "Más opciones", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
fun VideoRow(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumb(
            ServiceLocator.thumbUrl(video.key),
            Modifier.width(168.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
            video.duration,
        )
        Column(Modifier.weight(1f)) {
            Text(
                video.title ?: "Sin título",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            val sub = video.subtitle()
            if (sub.isNotEmpty()) {
                Text(
                    sub,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun Modifier.MessageBox(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = Palette.Accent) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoOptionsSheet(
    video: Video,
    isFavorite: Boolean,
    pickingPlaylist: Boolean,
    playlists: List<PlaylistSummary>,
    playlistsLoading: Boolean,
    playlistsError: String?,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onBack: () -> Unit,
    onRetryPlaylists: () -> Unit,
    onSelectPlaylist: (PlaylistSummary) -> Unit,
    onCreatePlaylist: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val closeThen: (() -> Unit) -> Unit = { action ->
        action()
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
 
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Palette.SurfaceHigh,
        contentColor = Palette.TextPrimary,
    ) {
        if (!pickingPlaylist) {
            Text(
                video.title ?: "Sin título",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider(color = Palette.Divider, thickness = 0.5.dp)
            SheetRow(
                label = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                icon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                onClick = { closeThen(onToggleFavorite) },
            )
            SheetRow(
                label = "Agregar a playlist…",
                icon = { Icon(Icons.Rounded.Add, null) },
                onClick = onAddToPlaylist,
            )
        } else {
            PlaylistPicker(
                playlists = playlists,
                loading = playlistsLoading,
                error = playlistsError,
                onBack = onBack,
                onRetry = onRetryPlaylists,
                onSelect = { p -> closeThen { onSelectPlaylist(p) } },
                onCreate = { name -> closeThen { onCreatePlaylist(name) } },
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
 
@Composable
private fun SheetRow(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = icon,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
 
@Composable
private fun PlaylistPicker(
    playlists: List<PlaylistSummary>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSelect: (PlaylistSummary) -> Unit,
    onCreate: (String) -> Unit,
) {
    var creating by rememberSaveable { mutableStateOf(false) }
    var newName by rememberSaveable { mutableStateOf("") }
 
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver") }
        Text("Agregar a playlist", fontWeight = FontWeight.Medium)
    }
    HorizontalDivider(color = Palette.Divider, thickness = 0.5.dp)
 
    SheetRow(
        label = "Nueva playlist",
        icon = { Icon(Icons.Rounded.Add, null, tint = Palette.Accent) },
        onClick = { creating = !creating },
    )
    if (creating) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onCreate(newName.trim()) },
                enabled = newName.isNotBlank(),
            ) { Text("Crear", color = Palette.Accent) }
        }
    }
 
    when {
        loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Palette.Accent)
        }
        error != null -> Modifier.MessageBox(error, "Reintentar", onRetry)
        playlists.isEmpty() -> Modifier.MessageBox("Aún no tienes playlists.")
        else -> LazyColumn(Modifier.heightIn(max = 320.dp)) {
            items(playlists, key = { it.id }) { p ->
                ListItem(
                    headlineContent = { Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = {
                        Text(
                            if (p.itemCount == 1) "1 video" else "${p.itemCount} videos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelect(p) },
                )
            }
        }
    }
}