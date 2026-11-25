package com.aimp.player.lyrics

object LyricsParser {
    private val lineRegex = Regex("^\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})](.*)$")

    fun parseSynced(synced: String?): List<LyricLine> {
        if (synced.isNullOrBlank()) return emptyList()
        val out = mutableListOf<LyricLine>()
        for (raw in synced.lines()) {
            val m = lineRegex.find(raw.trim()) ?: continue
            val (mm, ss, frac, text) = m.destructured
            val ms = mm.toLong() * 60_000 + ss.toLong() * 1_000 + frac.padEnd(3, '0').toLong()
            out.add(LyricLine(ms, text.trim()))
        }
        return out.sortedBy { it.timeMs }
    }

    fun parsePlain(plain: String?): List<LyricLine> {
        if (plain.isNullOrBlank()) return emptyList()
        return plain.lines().mapIndexed { i, t -> LyricLine(i * 0L, t) }
    }
}
