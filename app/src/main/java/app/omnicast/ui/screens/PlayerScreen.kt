package app.omnicast.ui.screens

import android.Manifest
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import app.omnicast.data.MediaCache
import app.omnicast.data.PlaylistSummary
import app.omnicast.data.ServiceLocator
import app.omnicast.data.Video
import app.omnicast.friendly
import app.omnicast.playback.PlaybackService
import app.omnicast.timeAgo
import app.omnicast.ui.components.ChannelAvatar
import app.omnicast.ui.components.VideoRow
import app.omnicast.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import app.omnicast.R
import app.omnicast.ui.components.Thumb
import kotlin.math.abs

class PlayerVm : ViewModel() {
    var related by mutableStateOf<List<Video>>(emptyList()); private set
    var isFavorite by mutableStateOf(false); private set
    var playlists by mutableStateOf<List<PlaylistSummary>>(emptyList()); private set

    private var loadedFor: String? = null

    fun load(hash: String) {
        if (loadedFor == hash) return
        loadedFor = hash
        viewModelScope.launch {
            try {
                val feed = ServiceLocator.api.feed(0, 30).feed
                MediaCache.put(feed)
                related = feed.filter { it.key != hash }
            } catch (_: Exception) { }
            try {
                isFavorite = ServiceLocator.api.favorites().favorites.any { it.key == hash }
            } catch (_: Exception) { }
        }
    }

    fun favorite(hash: String, onMsg: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ServiceLocator.api.addFavorite(hash)
                isFavorite = true
                onMsg("Añadido a favoritos")
            } catch (e: Exception) {
                onMsg(e.friendly())
            }
        }
    }

    fun unfavorite(hash: String, onMsg: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ServiceLocator.api.removeFavorite(hash)
                isFavorite = false
                onMsg("Quitado de favoritos")
            } catch (e: Exception) {
                onMsg(e.friendly())
            }
        }
    }

    fun loadPlaylists(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                playlists = ServiceLocator.api.playlists().playlists
            } catch (e: Exception) {
                onError(e.friendly())
            }
        }
    }

    fun addToPlaylist(playlistId: String, hash: String, onMsg: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ServiceLocator.api.addToPlaylist(playlistId, hash)
                onMsg("Añadido a la playlist")
            } catch (e: Exception) {
                onMsg(e.friendly())
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun setFullscreen(activity: Activity?, on: Boolean) {
    activity ?: return
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    if (on) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}

private fun pipParams(aspect: Float, autoEnter: Boolean): PictureInPictureParams =
    PictureInPictureParams.Builder()
        .setAspectRatio(Rational((aspect.coerceIn(0.5f, 2.3f) * 1000).toInt(), 1000))
        .apply { if (Build.VERSION.SDK_INT >= 31) setAutoEnterEnabled(autoEnter) }
        .build()

private fun enterPip(activity: Activity, aspect: Float) {
    activity.enterPictureInPictureMode(pipParams(aspect, false))
}

private fun formatPlayerTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

@Composable
private fun GlowSeekBar(
    progress: Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.Accent,
) {
    var active by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(if (active) 1.6f else 1f, label = "thumbScale")
    val glowAlpha by animateFloatAsState(if (active) 0.60f else 0.35f, label = "glowAlpha")
    val currentScrub by rememberUpdatedState(onScrub)
    val currentFinished by rememberUpdatedState(onScrubFinished)

    Canvas(
        modifier
            .height(28.dp)
            .pointerInput(Unit) {
                val pad = 10.dp.toPx()
                fun frac(x: Float) = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                detectTapGestures { offset ->
                    currentScrub(frac(offset.x))
                    currentFinished()
                }
            }
            .pointerInput(Unit) {
                val pad = 10.dp.toPx()
                fun frac(x: Float) = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                detectHorizontalDragGestures(
                    onDragStart = { active = true; currentScrub(frac(it.x)) },
                    onDragEnd = { active = false; currentFinished() },
                    onDragCancel = { active = false; currentFinished() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentScrub(frac(change.position.x))
                    },
                )
            },
    ) {
        val pad = 10.dp.toPx()
        val cy = size.height / 2f
        val end = size.width - pad
        val x = pad + (end - pad) * progress.coerceIn(0f, 1f)
        val track = 2.5.dp.toPx()

        drawLine(Color.White.copy(alpha = 0.25f), Offset(pad, cy), Offset(end, cy), track, StrokeCap.Round)
        if (x > pad) {
            drawLine(color.copy(alpha = glowAlpha * 0.45f), Offset(pad, cy), Offset(x, cy), track * 3.2f, StrokeCap.Round)
            drawLine(color, Offset(pad, cy), Offset(x, cy), track, StrokeCap.Round)
        }
        val glowR = 14.dp.toPx() * thumbScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(x, cy),
                radius = glowR,
            ),
            radius = glowR,
            center = Offset(x, cy),
        )
        drawCircle(color, radius = 5.dp.toPx() * thumbScale, center = Offset(x, cy))
        drawCircle(Color.White.copy(alpha = 0.9f), radius = 2.dp.toPx() * thumbScale, center = Offset(x, cy))
    }
}

@Composable
private fun SeekFeedback(forward: Boolean, seconds: Int) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    if (forward) listOf(Color.Transparent, Color.White.copy(alpha = 0.16f))
                    else listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                if (forward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
            Text(
                "${if (forward) "+" else "−"}$seconds s",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun UpNextPanel(
    videos: List<Video>,
    onPick: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))))
            .padding(top = 28.dp, bottom = 12.dp)
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Text(
            "A continuación",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(videos, key = { it.key }) { v ->
                Column(
                    Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPick(v) },
                ) {
                    Thumb(
                        ServiceLocator.thumbUrl(v.key),
                        Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(10.dp)),
                        v.duration,
                    )
                    Text(
                        v.title ?: "Sin título",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    initialHash: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    vm: PlayerVm = viewModel(),
) {
    var hash by rememberSaveable(initialHash) { mutableStateOf(initialHash) }
    val ctx = LocalContext.current
    val view = LocalView.current
    val activity = remember(ctx) { ctx.findActivity() }
    val video = remember(hash) { MediaCache[hash] }

    var fullscreen by remember { mutableStateOf(false) }
    var showPlaylists by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    var controller by remember { mutableStateOf<MediaController?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var buffering by remember { mutableStateOf(false) }
    var videoAspect by remember { mutableFloatStateOf(16f / 9f) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var draggingProgress by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    var inPip by remember { mutableStateOf(false) }
    val playingNow by rememberUpdatedState(isPlaying)
    val aspectNow by rememberUpdatedState(videoAspect)

    DisposableEffect(activity) {
        val a = activity as? ComponentActivity ?: return@DisposableEffect onDispose { }
        val pipListener =
            Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        val leaveListener = Runnable {
            if (Build.VERSION.SDK_INT in 26..30 && playingNow) enterPip(a, aspectNow)
        }
        a.addOnPictureInPictureModeChangedListener(pipListener)
        a.addOnUserLeaveHintListener(leaveListener)
        onDispose {
            a.removeOnPictureInPictureModeChangedListener(pipListener)
            a.removeOnUserLeaveHintListener(leaveListener)
        }
    }

    LaunchedEffect(isPlaying, videoAspect) {
        activity?.setPictureInPictureParams(pipParams(videoAspect, isPlaying))
    }
    LaunchedEffect(ended) { if (ended) controlsVisible = true }
    LaunchedEffect(inPip) { if (inPip) controlsVisible = false }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    DisposableEffect(Unit) {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        future.addListener(
            { controller = try { future.get() } catch (_: Exception) { null } },
            ContextCompat.getMainExecutor(ctx),
        )
        onDispose {
            controller?.let { c ->
                val id = c.currentMediaItem?.mediaId ?: hash
                val finished = c.playbackState == Player.STATE_ENDED
                val pos = if (finished) 0L else c.currentPosition
                if (finished || pos > 0) ServiceLocator.saveHistory(c.currentMediaItem?.mediaId ?: hash, pos)

                if (id == hash && activity?.isChangingConfigurations != true) {
                    c.stop()
                    c.clearMediaItems()
                }
            }
            controller = null
            MediaController.releaseFuture(future)
            setFullscreen(activity, false)
            activity?.setPictureInPictureParams(pipParams(16f / 9f, false))
        }
    }

    LaunchedEffect(controller, hash) {
        val c = controller ?: return@LaunchedEffect
        if (c.currentMediaItem?.mediaId != hash) {
            val resumeAt = try {
                ServiceLocator.api.history(hash).positionMs
            } catch (_: Exception) { 0L }
            c.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(hash)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(video?.title)
                            .setArtist(video?.uploader)
                            .build(),
                    )
                    .build(),
                resumeAt,
            )
            c.prepare()
            c.playWhenReady = true
        }
    }

    DisposableEffect(controller) {
        val c = controller
        if (c != null) {
            isPlaying = c.isPlaying
            buffering = c.playbackState == Player.STATE_BUFFERING
            ended = c.playbackState == Player.STATE_ENDED
            c.videoSize.let { s ->
                if (s.width > 0 && s.height > 0) videoAspect = s.width * s.pixelWidthHeightRatio / s.height
            }
            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(size: VideoSize) {
                    if (size.width > 0 && size.height > 0) {
                        videoAspect = size.width * size.pixelWidthHeightRatio / size.height
                    }
                }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    ended = state == Player.STATE_ENDED
                }
            }
            c.addListener(listener)
            onDispose { c.removeListener(listener) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(controller) {
        val c = controller ?: return@LaunchedEffect
        while (true) {
            positionMs = c.currentPosition.coerceAtLeast(0L)
            durationMs = c.duration.takeIf { it > 0L } ?: 0L
            delay(250.milliseconds)
        }
    }

    LaunchedEffect(controller) {
        val c = controller ?: return@LaunchedEffect
        while (true) {
            delay(60_000.milliseconds)
            if (c.isPlaying) ServiceLocator.saveHistory(c.currentMediaItem?.mediaId ?: hash, c.currentPosition)
        }
    }

    DisposableEffect(isPlaying) {
        view.keepScreenOn = isPlaying
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(controlsVisible, isPlaying, draggingProgress) {
        if (controlsVisible && isPlaying && !draggingProgress) {
            delay(3_000.milliseconds)
            controlsVisible = false
        }
    }

    LaunchedEffect(hash) { vm.load(hash) }
    LaunchedEffect(fullscreen) { setFullscreen(activity, fullscreen) }
    BackHandler(fullscreen) { fullscreen = false }

    val toast = { m: String -> Toast.makeText(ctx, m, Toast.LENGTH_SHORT).show() }
    val scope = rememberCoroutineScope()
    val dragOffset = remember { Animatable(0f) }
    var showUpNext by remember { mutableStateOf(false) }

    LaunchedEffect(fullscreen) { if (!fullscreen) showUpNext = false }
    LaunchedEffect(showUpNext) { if (showUpNext) controlsVisible = false }
    LaunchedEffect(ended) { if (ended && fullscreen && vm.related.isNotEmpty()) showUpNext = true }

    val playRelated: (Video) -> Unit = { v ->
        controller?.let { c ->
            val finished = c.playbackState == Player.STATE_ENDED
            val pos = if (finished) 0L else c.currentPosition
            if (finished || pos > 0) ServiceLocator.saveHistory(hash, pos)
        }
        hash = v.key
        showUpNext = false
    }

    val playerBox: @Composable (Modifier) -> Unit = { mod ->
        var seekSide by remember { mutableIntStateOf(0) }
        var seekSeconds by remember { mutableIntStateOf(0) }
        var seekTick by remember { mutableIntStateOf(0) }

        LaunchedEffect(seekTick) {
            if (seekTick > 0) {
                delay(700.milliseconds)
                seekSide = 0
                seekSeconds = 0
            }
        }

        fun doSeek(side: Int) {
            seekSeconds = if (side == seekSide) seekSeconds + 10 else 10
            seekSide = side
            seekTick++
            controller?.let {
                val target = it.currentPosition + side * 10_000L
                val max = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                it.seekTo(target.coerceIn(0L, max))
            }
        }

        Box(
            modifier = mod
                .graphicsLayer {
                    val range = 220.dp.toPx()
                    val p = (dragOffset.value / range).coerceIn(-1f, 1f)
                    val s = when {
                        p < 0f && fullscreen -> 1f
                        p < 0f -> 1f + 0.30f * -p
                        else -> 1f - (if (fullscreen) 0.25f else 0.15f) * p
                    }
                    scaleX = s
                    scaleY = s
                    translationY = dragOffset.value * 0.4f
                }
                .background(Color.Black),
        ) {
            Box(modifier = mod.background(Color.Black)) {
                AndroidView(
                    factory = { c ->
                        (LayoutInflater.from(c)
                            .inflate(R.layout.player_texture, null) as PlayerView).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    update = { it.player = controller },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .aspectRatio(videoAspect),
                )

                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->
                                    when {
                                        showUpNext -> showUpNext = false
                                        seekSide != 0 -> doSeek(if (offset.x < size.width / 2f) -1 else 1)
                                        else -> controlsVisible = !controlsVisible
                                    }
                                },
                                onDoubleTap = { offset ->
                                    doSeek(if (offset.x < size.width / 2f) -1 else 1)
                                },
                            )
                        }.pointerInput(Unit) {
                            val range = 220.dp.toPx()
                            val commit = 80.dp.toPx()
                            detectVerticalDragGestures(
                                onDragStart = { scope.launch { dragOffset.stop() } },
                                onVerticalDrag = { change, dy ->
                                    change.consume()
                                    val next = dragOffset.value + dy
                                    val clamped = next.coerceIn(-range, range)
                                    scope.launch { dragOffset.snapTo(clamped) }
                                },
                                onDragCancel = {
                                    scope.launch { dragOffset.animateTo(0f, spring(stiffness = 400f)) }
                                },
                                onDragEnd = {
                                    val v = dragOffset.value
                                    scope.launch {
                                        when {
                                            v < -commit && fullscreen -> {
                                                showUpNext = true
                                                dragOffset.animateTo(0f, spring(stiffness = 400f))
                                            }
                                            v < -commit -> { dragOffset.snapTo(0f); fullscreen = true }
                                            v > commit && fullscreen && showUpNext -> {
                                                showUpNext = false
                                                dragOffset.animateTo(0f, spring(stiffness = 400f))
                                            }
                                            v > commit && fullscreen -> { dragOffset.snapTo(0f); fullscreen = false }
                                            v > commit -> { dragOffset.snapTo(0f); activity?.let { enterPip(it, videoAspect) } }
                                            else -> dragOffset.animateTo(0f, spring(stiffness = 400f))
                                        }
                                    }
                                },
                            )
                        },
                )

                AnimatedVisibility(
                    visible = seekSide == -1,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight()
                        .fillMaxWidth(0.4f),
                ) { SeekFeedback(forward = false, seconds = seekSeconds) }
                AnimatedVisibility(
                    visible = seekSide == 1,
                    enter = fadeIn(), exit = fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        .fillMaxWidth(0.4f),
                ) { SeekFeedback(forward = true, seconds = seekSeconds) }
                AnimatedVisibility(
                    visible = controlsVisible && !inPip,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.65f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                ),
                        )

                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                if (fullscreen) fullscreen = false else onBack()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowBack,
                                    "Volver",
                                    tint = Color.White
                                )
                            }
                            if (fullscreen) {
                                Text(
                                    video?.title ?: "",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        Row(
                            Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(68.dp)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(Palette.Accent.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)),
                                        ),
                                        CircleShape,
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (buffering) {
                                    CircularProgressIndicator(
                                        Modifier.size(30.dp),
                                        color = Palette.Accent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(
                                        onClick = {
                                            controller?.let {
                                                when {
                                                    ended -> { it.seekTo(0L); it.play() }
                                                    it.isPlaying -> it.pause()
                                                    else -> it.play()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(68.dp),
                                    ) {
                                        Icon(
                                            when {
                                                ended -> Icons.Rounded.Replay
                                                isPlaying -> Icons.Rounded.Pause
                                                else -> Icons.Rounded.PlayArrow
                                            },
                                            contentDescription = when {
                                                ended -> "Repetir"
                                                isPlaying -> "Pausar"
                                                else -> "Reproducir"
                                            },
                                            tint = Color.White,
                                            modifier = Modifier.size(38.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                        ) {
                            GlowSeekBar(
                                progress = if (draggingProgress) dragProgress
                                else if (durationMs > 0L) positionMs.toFloat() / durationMs else 0f,
                                onScrub = {
                                    draggingProgress = true
                                    dragProgress = it
                                },
                                onScrubFinished = {
                                    if (durationMs > 0L) controller?.seekTo((dragProgress * durationMs).toLong())
                                    draggingProgress = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val shown =
                                    if (draggingProgress) (dragProgress * durationMs).toLong() else positionMs
                                Text(
                                    "${formatPlayerTime(shown)} / ${formatPlayerTime(durationMs)}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    letterSpacing = 0.3.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                if (fullscreen && vm.related.isNotEmpty()) {
                                    TextButton(
                                        onClick = { showUpNext = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp),
                                    ) {
                                        Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White)
                                    }
                                }
                                IconButton(onClick = { fullscreen = !fullscreen }) {
                                    Icon(
                                        if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                        contentDescription = if (fullscreen) "Salir de pantalla completa" else "Pantalla completa",
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = showUpNext && fullscreen && !inPip,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                ) {
                    UpNextPanel(vm.related, onPick = playRelated, modifier = Modifier.fillMaxWidth())
                }

                if (buffering && !controlsVisible) {
                    CircularProgressIndicator(
                        Modifier.align(Alignment.Center).size(34.dp),
                        color = Palette.Accent,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }

    val bigPlayer = fullscreen || inPip

    Column(
        Modifier
            .fillMaxSize()
            .background(if (bigPlayer) Color.Black else MaterialTheme.colorScheme.background)
            .then(if (bigPlayer) Modifier else Modifier.navigationBarsPadding()),
    ) {
        Column(
            (if (bigPlayer) Modifier.fillMaxSize()
            else Modifier.background(Color.Black).statusBarsPadding())
                .zIndex(1f),
        ) {
            playerBox(
                if (bigPlayer) Modifier.fillMaxSize()
                else Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
        }

        if (!bigPlayer) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val t = (abs(dragOffset.value) / 220.dp.toPx()).coerceIn(0f, 1f)
                        alpha = 1f - 0.7f * t
                    },
            ) {
                item {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
                        Text(
                            video?.title ?: "Reproduciendo",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 19.sp,
                            lineHeight = 25.sp,
                        )
                        val sub = timeAgo(video?.addedAt)
                        if (sub.isNotEmpty()) {
                            Text(
                                sub,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
                item {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ChannelAvatar(video?.uploader)
                        Text(
                            video?.uploader ?: "",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp).weight(1f),
                        )
                    }
                }
                item {
                    Row(
                        Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val btnColors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Palette.SurfaceHigh,
                            contentColor = Palette.TextPrimary,
                        )
                        FilledTonalButton(
                            onClick = { if (vm.isFavorite) vm.unfavorite(hash, toast) else vm.favorite(hash, toast) },
                            colors = btnColors,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                if (vm.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                null,
                                tint = if (vm.isFavorite) Palette.Accent else Palette.TextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(if (vm.isFavorite) "En favoritos" else "Favorito")
                        }
                        FilledTonalButton(
                            onClick = {
                                vm.loadPlaylists(toast)
                                showPlaylists = true
                            },
                            colors = btnColors,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Guardar")
                        }
                    }
                }
                item {
                    HorizontalDivider(
                        color = Palette.Divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                if (vm.related.isNotEmpty()) {
                    item {
                        Text(
                            "A continuación",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
                        )
                    }
                }
                items(vm.related, key = { it.key }) { v ->
                    VideoRow(v, onClick = { onOpen(v.key) })
                }
            }
        }

        if (showPlaylists) {
            ModalBottomSheet(
                onDismissRequest = { showPlaylists = false },
                containerColor = Palette.SurfaceHigh,
            ) {
                Column(Modifier.padding(bottom = 24.dp).navigationBarsPadding()) {
                    Text(
                        "Guardar en…",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    if (vm.playlists.isEmpty()) {
                        Text(
                            "No tienes playlists. Crea una desde Biblioteca.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    } else {
                        vm.playlists.forEach { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPlaylists = false
                                        vm.addToPlaylist(p.id, hash, toast)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    null,
                                    tint = Palette.TextPrimary
                                )
                                Text(p.name, modifier = Modifier.padding(start = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}