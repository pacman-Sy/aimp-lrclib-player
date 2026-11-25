package com.aimp.player.lyrics

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("artist_name") artistName: String? = null,
        @Query("track_name") trackName: String? = null,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): Response<LrcLibTrack>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String
    ): Response<List<LrcLibTrack>>
}
