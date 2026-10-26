package app.omnicast.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.omnicast.data.ServiceLocator
import app.omnicast.ui.screens.ExploreScreen
import app.omnicast.ui.screens.HomeScreen
import app.omnicast.ui.screens.LibraryScreen
import app.omnicast.ui.screens.PlayerScreen
import app.omnicast.ui.screens.PlaylistScreen
import app.omnicast.ui.screens.SearchScreen
import app.omnicast.ui.screens.SettingsScreen
import app.omnicast.ui.theme.Palette

private object Route {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player/{hash}"
    const val PLAYLIST = "playlist/{id}"
    fun player(hash: String) = "player/$hash"
    fun playlist(id: String) = "playlist/$id"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector)

private val tabs = listOf(
    Tab(Route.HOME, "Inicio", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab(Route.EXPLORE, "Explorar", Icons.Outlined.Explore, Icons.Rounded.Explore),
    Tab(Route.LIBRARY, "Biblioteca", Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
)

@Composable
fun OmniNavHost() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = route in tabs.map { it.route }

    val session by ServiceLocator.settings.state.collectAsState()
    val start = remember { if (ServiceLocator.settings.current.configured) Route.HOME else Route.SETTINGS }

    // si la sesión se revoca/expira (refresh rechazado), volver a la pantalla de login
    LaunchedEffect(session.configured) {
        if (!session.configured && nav.currentDestination?.route != Route.SETTINGS) {
            nav.navigate(Route.SETTINGS) { popUpTo(0) { inclusive = true } }
        }
    }

    fun openVideo(hash: String) {
        nav.navigate(Route.player(hash)) {
            // al saltar de un video a otro, no apiles reproductores
            if (route == Route.PLAYER) popUpTo(Route.PLAYER) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        containerColor = Palette.Background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = Palette.Surface) {
                    tabs.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) tab.selected else tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Palette.OnAccent,
                                selectedTextColor = Palette.TextPrimary,
                                indicatorColor = Palette.Accent,
                                unselectedIconColor = Palette.Titanium,
                                unselectedTextColor = Palette.Titanium,
                            ),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = start, modifier = Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
            composable(Route.HOME) {
                HomeScreen(
                    onOpen = ::openVideo,
                    onSearch = { nav.navigate(Route.SEARCH) },
                    onSettings = { nav.navigate(Route.SETTINGS) },
                )
            }
            composable(Route.EXPLORE) {
                ExploreScreen()
            }
            composable(Route.LIBRARY) {
                LibraryScreen(
                    onOpenVideo = ::openVideo,
                    onOpenPlaylist = { nav.navigate(Route.playlist(it)) },
                    onSettings = { nav.navigate(Route.SETTINGS) },
                )
            }
            composable(Route.SEARCH) {
                SearchScreen(onBack = { nav.popBackStack() }, onOpen = ::openVideo)
            }
            composable(Route.SETTINGS) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.navigate(Route.HOME) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable(Route.PLAYER, arguments = listOf(navArgument("hash") { type = NavType.StringType })) {
                PlayerScreen(
                    initialHash = it.arguments?.getString("hash").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpen = ::openVideo,
                )
            }
            composable(Route.PLAYLIST, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                PlaylistScreen(
                    id = it.arguments?.getString("id").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpen = ::openVideo,
                )
            }
        }
    }
}
