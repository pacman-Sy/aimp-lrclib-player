package com.aimp.player.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aimp.player.databinding.FragmentPlayerBinding
import com.aimp.player.playback.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlayerFragment : Fragment() {
    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val vm: PlayerViewModel by viewModels({ requireActivity() })
    private var dragging = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.prevBtn.setOnClickListener { vm.prev() }
        binding.nextBtn.setOnClickListener { vm.next() }
        binding.playPauseBtn.setOnClickListener { vm.togglePlayPause() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) { dragging = true }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                dragging = false
                seekBar?.let { vm.seekTo(it.progress.toLong()) }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            vm.currentTrack.collect { t ->
                binding.nowPlaying.text = t?.let { "${it.title}\n${it.artist}" } ?: "No track"
                binding.seekBar.max = t?.durationMs?.toInt() ?: 100
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            vm.isPlaying.collect { playing ->
                binding.playPauseBtn.text = if (playing) "❚❚" else "▶"
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                if (!dragging) binding.seekBar.progress = vm.player.currentPosition.toInt()
                delay(500)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
