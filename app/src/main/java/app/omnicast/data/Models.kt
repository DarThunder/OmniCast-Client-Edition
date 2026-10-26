package app.omnicast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

/** Video de la biblioteca (feed, búsqueda local, favoritos, playlists). Todos los campos son tolerantes. */
@Serializable
data class Video(
    val hash: String? = null,
    val id: String? = null,
    val title: String? = null,
    val uploader: String? = null,
    val duration: Double? = null,
    @SerialName("added_at") val addedAt: String? = null,
) {
    val key: String get() = hash ?: id ?: ""
}

@Serializable
data class FeedResponse(
    val feed: List<Video> = emptyList(),
    @SerialName("next_skip") val nextSkip: Int? = null,
)

@Serializable
data class FavoritesResponse(val favorites: List<Video> = emptyList())

@Serializable
data class DiscoverItem(
    val title: String? = null,
    val uploader: String? = null,
    val url: String,
    val duration: Double? = null,
    val thumbnail: String? = null,
)

@Serializable
data class DiscoverResponse(
    val page: Int = 1,
    val limit: Int = 20,
    @SerialName("has_more") val hasMore: Boolean = false,
    val results: List<DiscoverItem> = emptyList(),
)

@Serializable
data class DownloadStarted(@SerialName("task_id") val taskId: String)

@Serializable
data class Profile(val username: String = "", val bio: String? = null)

@Serializable
data class PlaylistSummary(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
)

@Serializable
data class PlaylistsResponse(val playlists: List<PlaylistSummary> = emptyList())

@Serializable
data class PlaylistDetail(
    val id: String,
    val name: String,
    val description: String = "",
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
    val items: List<Video> = emptyList(),
)

@Serializable
data class PlaylistBody(val name: String, val description: String = "")

@Serializable
data class HistoryBody(@SerialName("position_ms") val positionMs: Long)

@Serializable
data class HistoryResponse(
    @SerialName("media_hash") val mediaHash: String = "",
    @SerialName("position_ms") val positionMs: Long = 0,
)

@Serializable
data class MessageResponse(val message: String = "")

/** Cache en memoria para pasar los metadatos de un video a la pantalla del reproductor. */
object MediaCache {
    private val map = ConcurrentHashMap<String, Video>()
    fun put(list: List<Video>) = list.forEach { if (it.key.isNotEmpty()) map[it.key] = it }
    operator fun get(key: String): Video? = map[key]
}
