package app.omnicast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import app.omnicast.ui.components.VideoCard
import app.omnicast.ui.components.VideoOptionsSheet
import app.omnicast.ui.theme.Palette
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class HomeFilter(val label: String) { ALL("Todo"), FAVORITES("Favoritos") }

class HomeVm : ViewModel() {
    var items by mutableStateOf<List<Video>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var filter by mutableStateOf(HomeFilter.ALL); private set

    var favoriteKeys by mutableStateOf<Set<String>>(emptySet()); private set
    var menuVideo by mutableStateOf<Video?>(null); private set
    var pickingPlaylist by mutableStateOf(false); private set
    var playlists by mutableStateOf<List<PlaylistSummary>>(emptyList()); private set
    var playlistsLoading by mutableStateOf(false); private set
    var playlistsError by mutableStateOf<String?>(null); private set
    var message by mutableStateOf<String?>(null); private set

    private var nextSkip: Int? = 0
    private var generation = 0

    fun selectFilter(f: HomeFilter) {
        if (f == filter) return
        filter = f
        refresh()
    }

    fun refresh() {
        generation++
        nextSkip = 0
        items = emptyList()
        error = null
        loading = false
        loadMore()
        if (filter == HomeFilter.ALL) syncFavorites()
    }

    fun loadMore() {
        if (loading) return
        val skip = nextSkip ?: return
        val gen = generation
        loading = true
        viewModelScope.launch {
            try {
                if (filter == HomeFilter.FAVORITES) {
                    val favs = ServiceLocator.api.favorites().favorites
                    if (gen != generation) return@launch
                    MediaCache.put(favs)
                    items = favs
                    favoriteKeys = favs.map { it.key }.toSet()
                    nextSkip = null
                } else {
                    val r = ServiceLocator.api.feed(skip)
                    if (gen != generation) return@launch
                    MediaCache.put(r.feed)
                    items = items + r.feed
                    nextSkip = r.nextSkip
                }
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (gen == generation) error = e.friendly()
            } finally {
                if (gen == generation) loading = false
            }
        }
    }

    private fun syncFavorites() {
        viewModelScope.launch {
            try {
                favoriteKeys = ServiceLocator.api.favorites().favorites.map { it.key }.toSet()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun openMenu(v: Video) {
        menuVideo = v
        pickingPlaylist = false
    }

    fun closeMenu() {
        menuVideo = null
        pickingPlaylist = false
    }

    fun showPlaylistPicker() {
        pickingPlaylist = true
        loadPlaylists()
    }

    fun backToMenu() {
        pickingPlaylist = false
    }

    fun loadPlaylists() {
        if (playlistsLoading) return
        playlistsLoading = true
        playlistsError = null
        viewModelScope.launch {
            try {
                playlists = ServiceLocator.api.playlists().playlists
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                playlistsError = e.friendly()
            } finally {
                playlistsLoading = false
            }
        }
    }

    fun toggleFavorite(v: Video) {
        val key = v.key
        if (key.isEmpty()) return
        val wasFavorite = key in favoriteKeys
        viewModelScope.launch {
            try {
                if (wasFavorite) {
                    ServiceLocator.api.removeFavorite(key)
                    favoriteKeys = favoriteKeys - key
                    if (filter == HomeFilter.FAVORITES) items = items.filterNot { it.key == key }
                    message = "Quitado de favoritos"
                } else {
                    ServiceLocator.api.addFavorite(key)
                    favoriteKeys = favoriteKeys + key
                    message = "Agregado a favoritos"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.friendly()
            }
        }
    }

    fun addToPlaylist(p: PlaylistSummary, v: Video) {
        if (v.key.isEmpty()) return
        viewModelScope.launch {
            try {
                ServiceLocator.api.addToPlaylist(p.id, v.key)
                message = "Agregado a «${p.name}»"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.friendly()
            }
        }
    }

    fun createPlaylistAndAdd(name: String, v: Video) {
        if (name.isBlank() || v.key.isEmpty()) return
        viewModelScope.launch {
            try {
                val created = ServiceLocator.api.createPlaylist(PlaylistBody(name = name))
                ServiceLocator.api.addToPlaylist(created.id, v.key)
                message = "Agregado a «${created.name}»"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                message = e.friendly()
            }
        }
    }

    fun consumeMessage() {
        message = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpen: (String) -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    vm: HomeVm = viewModel(),
) {
    val settings by ServiceLocator.settings.state.collectAsState()
    LaunchedEffect(settings.sessionKey, settings.configured) {
        if (settings.configured) vm.refresh()
    }

    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd) { if (nearEnd) vm.loadMore() }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OmniTopBar(onSearch = onSearch, onAvatar = onSettings)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(HomeFilter.entries.toList()) { f ->
                    FilterChip(
                        selected = vm.filter == f,
                        onClick = { vm.selectFilter(f) },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Palette.SurfaceHigh,
                            labelColor = Palette.TextPrimary,
                            selectedContainerColor = Palette.TextPrimary,
                            selectedLabelColor = Palette.Background,
                        ),
                        border = null,
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = vm.loading && vm.items.isEmpty() && vm.error == null,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    !settings.configured -> Modifier.MessageBox(
                        "Configura el servidor en Ajustes para empezar.",
                        "Abrir ajustes",
                        onSettings
                    )
                    vm.error != null && vm.items.isEmpty() -> Modifier.MessageBox(
                        vm.error!!,
                        "Reintentar",
                        vm::refresh
                    )
                    !vm.loading && vm.items.isEmpty() -> Modifier.MessageBox(
                        if (vm.filter == HomeFilter.FAVORITES) "Aún no tienes favoritos." else "Tu biblioteca está vacía. Descarga algo desde Explorar."
                    )
                    else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(vm.items, key = { it.key }) { v ->
                            VideoCard(
                                v,
                                onClick = { onOpen(v.key) },
                                onMenuClick = { vm.openMenu(v) },
                            )
                            HorizontalDivider(color = Palette.Divider, thickness = 0.5.dp)
                        }
                        if (vm.loading && vm.items.isNotEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Palette.Accent, modifier = Modifier.padding(4.dp))
                                }
                            }
                        }
                        if (vm.error != null && vm.items.isNotEmpty()) {
                            item { Modifier.MessageBox(vm.error!!, "Reintentar", vm::loadMore) }
                        }
                    }
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))

        vm.menuVideo?.let { v ->
            VideoOptionsSheet(
                video = v,
                isFavorite = v.key in vm.favoriteKeys,
                pickingPlaylist = vm.pickingPlaylist,
                playlists = vm.playlists,
                playlistsLoading = vm.playlistsLoading,
                playlistsError = vm.playlistsError,
                onDismiss = vm::closeMenu,
                onToggleFavorite = { vm.toggleFavorite(v) },
                onAddToPlaylist = vm::showPlaylistPicker,
                onBack = vm::backToMenu,
                onRetryPlaylists = vm::loadPlaylists,
                onSelectPlaylist = { p -> vm.addToPlaylist(p, v) },
                onCreatePlaylist = { name -> vm.createPlaylistAndAdd(name, v) },
            )
        }
    }
}