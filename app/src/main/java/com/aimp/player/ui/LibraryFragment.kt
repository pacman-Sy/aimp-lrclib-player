package com.aimp.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aimp.player.data.Track
import com.aimp.player.databinding.FragmentLibraryBinding
import com.aimp.player.playback.PlayerViewModel

class LibraryFragment : Fragment() {
    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val playerVm: PlayerViewModel by viewModels({ requireActivity() })
    private val tracks = mutableListOf<Track>()
    private lateinit var adapter: TrackAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = TrackAdapter { track -> playerVm.play(track) }
        binding.libraryRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.libraryRecycler.adapter = adapter
        binding.scanBtn.setOnClickListener { scan() }
    }

    private fun scan() {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(requireContext(), perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(perm), 100)
            return
        }
        doScan()
    }

    private fun doScan() {
        tracks.clear()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC}=1"
        requireContext().contentResolver.query(uri, projection, selection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val contentUri = android.content.ContentUris.withAppendedId(uri, id)
                tracks.add(Track(
                    id = id,
                    title = c.getString(titleCol) ?: "Unknown",
                    artist = c.getString(artistCol) ?: "Unknown",
                    album = c.getString(albumCol) ?: "",
                    durationMs = c.getLong(durCol),
                    uri = contentUri
                ))
            }
        }
        adapter.submit(tracks)
        playerVm.setQueue(tracks)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doScan()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class TrackAdapter(private val onClick: (Track) -> Unit) : RecyclerView.Adapter<TrackAdapter.VH>() {
        private val items = mutableListOf<Track>()
        fun submit(list: List<Track>) { items.clear(); items.addAll(list); notifyDataSetChanged() }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val t = items[position]
            holder.title.text = t.title
            holder.subtitle.text = "${t.artist} — ${t.album}"
            holder.itemView.setOnClickListener { onClick(t) }
        }
        override fun getItemCount() = items.size
        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val title: android.widget.TextView = v.findViewById(android.R.id.text1)
            val subtitle: android.widget.TextView = v.findViewById(android.R.id.text2)
        }
    }
}
