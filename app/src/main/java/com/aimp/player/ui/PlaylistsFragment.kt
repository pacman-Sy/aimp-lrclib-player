package com.aimp.player.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.aimp.player.data.Track
import com.aimp.player.databinding.FragmentPlaylistsBinding
import com.aimp.player.playback.PlayerViewModel
import kotlinx.coroutines.launch

class PlaylistsFragment : Fragment() {
    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!
    private val playerVm: PlayerViewModel by viewModels({ requireActivity() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = QueueAdapter { idx -> playerVm.playAt(idx) }
        binding.queueRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.queueRecycler.adapter = adapter
        viewLifecycleOwner.lifecycleScope.launch {
            playerVm.queue.collect { adapter.submit(it) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class QueueAdapter(private val onClick: (Int) -> Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<QueueAdapter.VH>() {
        private val items = mutableListOf<Track>()
        fun submit(list: List<Track>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = items[position]
            holder.title.text = t.title
            holder.subtitle.text = "${t.artist} — ${t.album}"
            holder.itemView.setOnClickListener { onClick(position) }
        }
        override fun getItemCount() = items.size
        class VH(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
            val title: android.widget.TextView = v.findViewById(android.R.id.text1)
            val subtitle: android.widget.TextView = v.findViewById(android.R.id.text2)
        }
    }
}
