package com.aimp.player

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aimp.player.databinding.ActivityMainBinding
import com.aimp.player.playback.PlayerViewModel
import com.aimp.player.ui.LibraryFragment
import com.aimp.player.ui.LyricsFragment
import com.aimp.player.ui.PlayerFragment
import com.aimp.player.ui.PlaylistsFragment
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var playerVm: PlayerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playerVm = ViewModelProvider(this)[PlayerViewModel::class.java]

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> LibraryFragment()
                1 -> PlayerFragment()
                2 -> LyricsFragment()
                else -> PlaylistsFragment()
            }
        }
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> getString(R.string.tab_library)
                1 -> getString(R.string.tab_player)
                2 -> getString(R.string.tab_lyrics)
                else -> getString(R.string.tab_playlists)
            }
        }.attach()

        val miniTitle = findViewById<TextView>(R.id.miniTitle)
        val miniArtist = findViewById<TextView>(R.id.miniArtist)
        val miniPlay = findViewById<Button>(R.id.miniPlay)
        findViewById<Button>(R.id.miniPrev).setOnClickListener { playerVm.prev() }
        findViewById<Button>(R.id.miniNext).setOnClickListener { playerVm.next() }
        miniPlay.setOnClickListener { playerVm.togglePlayPause() }

        lifecycleScope.launch {
            playerVm.currentTrack.collect { t ->
                miniTitle.text = t?.title ?: "No track"
                miniArtist.text = t?.artist ?: ""
            }
        }
        lifecycleScope.launch {
            playerVm.isPlaying.collect { playing ->
                miniPlay.text = if (playing) "❚❚" else "▶"
            }
        }
    }
}
