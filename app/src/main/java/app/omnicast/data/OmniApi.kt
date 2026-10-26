package app.omnicast.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OmniApi {
    @GET("feed")
    suspend fun feed(@Query("skip") skip: Int, @Query("limit") limit: Int = 30): FeedResponse

    @GET("search")
    suspend fun search(@Query("q") q: String): List<Video>

    @GET("discover")
    suspend fun discover(
        @Query("q") q: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20,
    ): DiscoverResponse

    @POST("videos")
    suspend fun download(@Query("url") url: String): DownloadStarted

    @GET("videos/progress/{id}")
    suspend fun progress(@Path("id") id: String): Map<String, String>
    @GET("users/me/profile")
    suspend fun profile(): Profile

    @GET("users/me/favorites")
    suspend fun favorites(): FavoritesResponse

    @DELETE("users/me/favorites/{hash}")
    suspend fun removeFavorite(@Path("hash") hash: String): MessageResponse
    @POST("users/me/favorites/{hash}")
    suspend fun addFavorite(@Path("hash") hash: String): MessageResponse

    @GET("users/me/playlists")
    suspend fun playlists(): PlaylistsResponse

    @POST("users/me/playlists")
    suspend fun createPlaylist(@Body body: PlaylistBody): PlaylistSummary

    @GET("users/me/playlists/{id}")
    suspend fun playlist(@Path("id") id: String): PlaylistDetail

    @DELETE("users/me/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") id: String): MessageResponse

    @POST("users/me/playlists/{id}/items/{hash}")
    suspend fun addToPlaylist(@Path("id") id: String, @Path("hash") hash: String): MessageResponse

    @DELETE("users/me/playlists/{id}/items/{hash}")
    suspend fun removeFromPlaylist(@Path("id") id: String, @Path("hash") hash: String): MessageResponse

    @GET("users/me/history/{hash}")
    suspend fun history(@Path("hash") hash: String): HistoryResponse

    @PATCH("users/me/history/{hash}")
    suspend fun saveHistory(@Path("hash") hash: String, @Body body: HistoryBody): MessageResponse
}
