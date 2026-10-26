package app.omnicast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.omnicast.data.DiscoverItem
import app.omnicast.data.DlState
import app.omnicast.data.ServiceLocator
import app.omnicast.friendly
import app.omnicast.ui.components.ChannelAvatar
import app.omnicast.ui.components.MessageBox
import app.omnicast.ui.components.Thumb
import app.omnicast.ui.theme.Palette
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ExploreVm : ViewModel() {
    var query by mutableStateOf("")
    var results by mutableStateOf<List<DiscoverItem>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var searched by mutableStateOf(false); private set

    private var hasMore = false
    private var page = 0
    private var activeQuery = ""
    private var generation = 0

    fun search() {
        val q = query.trim()
        if (q.isEmpty()) return
        generation++
        activeQuery = q
        page = 0
        hasMore = true
        results = emptyList()
        error = null
        loading = false
        searched = true
        loadMore()
    }

    fun loadMore() {
        if (loading || !hasMore || activeQuery.isEmpty()) return
        val gen = generation
        loading = true
        viewModelScope.launch {
            try {
                val r = ServiceLocator.api.discover(activeQuery, page + 1)
                if (gen != generation) return@launch
                page = r.page
                hasMore = r.hasMore
                results = results + r.results
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen == generation) { error = e.friendly(); hasMore = false }
            } finally {
                if (gen == generation) loading = false
            }
        }
    }
}

private fun String.isUrl() = startsWith("http://") || startsWith("https://")

/** Busca en YouTube vía tu servidor (GET /discover) y descarga a tu biblioteca (POST /videos). */
@Composable
fun ExploreScreen(vm: ExploreVm = viewModel()) {
    val downloads by ServiceLocator.downloads.states.collectAsState()
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

    Column(Modifier.fillMaxSize()) {
        TextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            placeholder = { Text("Busca en YouTube o pega un enlace") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { vm.search() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Palette.SurfaceHigh,
                unfocusedContainerColor = Palette.SurfaceHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Palette.Accent,
            ),
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        )

        val pasted = vm.query.trim()
        if (pasted.isUrl()) {
            DirectLinkCard(pasted, downloads[pasted])
        }

        when {
            vm.error != null && vm.results.isEmpty() -> Modifier.MessageBox(
                vm.error!!,
                "Reintentar",
                vm::search
            )
            !vm.searched -> Modifier.MessageBox("Encuentra videos y descárgalos a tu servidor para verlos sin conexión.")
            vm.searched && !vm.loading && vm.results.isEmpty() -> Modifier.MessageBox("Sin resultados.")
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                items(vm.results, key = { it.url }) { item ->
                    DiscoverCard(item, downloads[item.url])
                }
                if (vm.loading) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Palette.Accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectLinkCard(url: String, state: DlState?) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text("Enlace detectado", fontWeight = FontWeight.Medium)
            Text(url, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DownloadAction(url, state)
    }
}

@Composable
private fun DiscoverCard(item: DiscoverItem, state: DlState?) {
    Column(Modifier.fillMaxWidth()) {
        Thumb(item.thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f), item.duration)
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ChannelAvatar(item.uploader)
            Column(Modifier.weight(1f)) {
                Text(
                    item.title ?: "Sin título",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                )
                item.uploader?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1)
                }
            }
        }
        Box(Modifier.padding(start = 60.dp, end = 12.dp, bottom = 12.dp)) {
            DownloadAction(item.url, state)
        }
    }
}

@Composable
private fun DownloadAction(url: String, state: DlState?) {
    when {
        state == null || state.failed -> Column {
            Button(
                onClick = { ServiceLocator.downloads.start(url) },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Accent, contentColor = Palette.OnAccent),
            ) {
                Icon(Icons.Rounded.Download, null, modifier = Modifier.padding(end = 6.dp))
                Text(if (state?.failed == true) "Reintentar" else "Descargar")
            }
            if (state?.failed == true && state.detail.isNotBlank()) {
                Text(state.detail, color = Palette.Error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        state.done -> Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Palette.Accent)
            Text("En tu biblioteca", color = Palette.Accent, modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.Medium)
        }
        else -> Column(Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { state.percent },
                color = Palette.Accent,
                trackColor = Palette.SurfaceHigh,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "${(state.percent * 100).toInt()}%  ${state.detail}".trim(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
