package com.aimp.player.lyrics

import com.aimp.player.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class LyricsResult {
    data class Success(val track: LrcLibTrack, val lines: List<LyricLine>, val synced: Boolean) : LyricsResult()
    data class NotFound(val message: String = "No lyrics found") : LyricsResult()
    data class Error(val throwable: Throwable) : LyricsResult()
}

class LyricsRepository(private val api: LrcLibApi = NetworkModule.api) {

    suspend fun fetchForTrack(track: Track): LyricsResult = withContext(Dispatchers.IO) {
        try {
            val durationSec = (track.durationMs / 1000).toInt().coerceAtLeast(1)
            val resp = api.getLyrics(
                artistName = track.artist.takeIf { it.isNotBlank() },
                trackName = track.title.takeIf { it.isNotBlank() },
                albumName = track.album.takeIf { it.isNotBlank() },
                duration = durationSec
            )
            if (resp.isSuccessful) {
                val body = resp.body()
                if (body != null && (!body.syncedLyrics.isNullOrBlank() || !body.plainLyrics.isNullOrBlank())) {
                    val synced = !body.syncedLyrics.isNullOrBlank()
                    val lines = if (synced) LyricsParser.parseSynced(body.syncedLyrics)
                    else LyricsParser.parsePlain(body.plainLyrics)
                    return@withContext LyricsResult.Success(body, lines, synced)
                }
            }
            // fallback search
            val q = "${track.artist} ${track.title}".trim()
            val searchResp = api.search(q)
            val found = searchResp.body()?.firstOrNull {
                it.syncedLyrics?.isNotBlank() == true || it.plainLyrics?.isNotBlank() == true
            }
            if (found != null) {
                val synced = !found.syncedLyrics.isNullOrBlank()
                val lines = if (synced) LyricsParser.parseSynced(found.syncedLyrics)
                else LyricsParser.parsePlain(found.plainLyrics)
                return@withContext LyricsResult.Success(found, lines, synced)
            }
            LyricsResult.NotFound()
        } catch (t: Throwable) {
            LyricsResult.Error(t)
        }
    }
}
