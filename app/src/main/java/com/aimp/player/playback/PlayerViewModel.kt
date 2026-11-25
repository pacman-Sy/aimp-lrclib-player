package com.aimp.player.playback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aimp.player.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    val player: ExoPlayer = ExoPlayer.Builder(app).build()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var currentIndex = -1

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) next()
            }
        })
    }

    fun setQueue(tracks: List<Track>) {
        _queue.value = tracks
    }

    fun play(track: Track) {
        val list = _queue.value
        currentIndex = list.indexOfFirst { it.id == track.id }.let { if (it >= 0) it else 0 }
        if (list.isNotEmpty() && list[currentIndex].id != track.id) {
            // track not in queue: replace first slot
            _queue.value = list.toMutableList().also { it[0] = track }
            currentIndex = 0
        }
        _currentTrack.value = track
        player.setMediaItem(MediaItem.fromUri(track.uri))
        player.prepare()
        player.play()
    }

    fun playAt(index: Int) {
        val list = _queue.value
        if (index in list.indices) play(list[index])
    }

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        val list = _queue.value
        if (list.isEmpty()) return
        currentIndex = (currentIndex + 1) % list.size
        playAt(currentIndex)
    }

    fun prev() {
        val list = _queue.value
        if (list.isEmpty()) return
        currentIndex = if (currentIndex <= 0) list.lastIndex else currentIndex - 1
        playAt(currentIndex)
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
