package com.aimp.player.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aimp.player.R
import com.aimp.player.data.Track
import com.aimp.player.databinding.FragmentLyricsBinding
import com.aimp.player.lyrics.LyricsUiState
import com.aimp.player.lyrics.LyricsViewModel
import com.aimp.player.playback.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LyricsFragment : Fragment() {

    private var _binding: FragmentLyricsBinding? = null
    private val binding get() = _binding!!

    private val playerVm: PlayerViewModel by viewModels({ requireActivity() })
    private val lyricsVm: LyricsViewModel by viewModels()

    private lateinit var adapter: LyricAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLyricsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = LyricAdapter()
        binding.lyricsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.lyricsRecycler.adapter = adapter

        binding.autoSwitch.isChecked = true
        binding.autoSwitch.setOnCheckedChangeListener { _, checked ->
            lyricsVm.setAutoFetch(checked)
        }

        // Observe current track -> auto fetch
        viewLifecycleOwner.lifecycleScope.launch {
            playerVm.currentTrack.collect { track ->
                track?.let {
                    binding.trackTitle.text = "${it.title} — ${it.artist}"
                    if (binding.autoSwitch.isChecked) lyricsVm.load(it)
                }
            }
        }

        // Observe lyrics state
        viewLifecycleOwner.lifecycleScope.launch {
            lyricsVm.state.collect { state ->
                when (state) {
                    is LyricsUiState.Idle -> binding.status.text = ""
                    is LyricsUiState.Loading -> {
                        binding.progress.visibility = View.VISIBLE
                        binding.status.text = getString(R.string.lyrics_loading)
                    }
                    is LyricsUiState.Loaded -> {
                        binding.progress.visibility = View.GONE
                        binding.status.text = ""
                        adapter.submit(state.lines, state.currentIndex)
                        if (state.synced && state.currentIndex >= 0) {
                            binding.lyricsRecycler.scrollToPosition(state.currentIndex)
                        }
                    }
                    is LyricsUiState.NotFound -> {
                        binding.progress.visibility = View.GONE
                        binding.status.text = getString(R.string.lyrics_not_found)
                        adapter.submit(emptyList(), -1)
                    }
                    is LyricsUiState.Error -> {
                        binding.progress.visibility = View.GONE
                        binding.status.text = getString(R.string.lyrics_error)
                        adapter.submit(emptyList(), -1)
                    }
                }
            }
        }

        // Poll playback position for synced highlight
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                lyricsVm.updatePosition(playerVm.player.currentPosition)
                delay(250)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class LyricAdapter : RecyclerView.Adapter<LyricAdapter.VH>() {
        private var lines: List<com.aimp.player.lyrics.LyricLine> = emptyList()
        private var currentIndex: Int = -1

        fun submit(newLines: List<com.aimp.player.lyrics.LyricLine>, idx: Int) {
            lines = newLines
            currentIndex = idx
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_lyric, parent, false)
            return VH(v as android.widget.TextView)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val line = lines[position]
            holder.text.text = line.text.ifBlank { "…" }
            val color = when {
                position == currentIndex -> androidx.core.content.ContextCompat.getColor(holder.text.context, R.color.lyric_current)
                position < currentIndex -> androidx.core.content.ContextCompat.getColor(holder.text.context, R.color.lyric_dim)
                else -> androidx.core.content.ContextCompat.getColor(holder.text.context, R.color.lyric_normal)
            }
            holder.text.setTextColor(color)
        }

        override fun getItemCount() = lines.size

        class VH(val text: android.widget.TextView) : RecyclerView.ViewHolder(text)
    }
}
