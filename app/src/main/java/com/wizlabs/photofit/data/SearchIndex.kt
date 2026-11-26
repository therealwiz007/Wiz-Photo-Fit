package com.wizlabs.photofit.data

/** One searchable item (preset, ID preset or custom size). Pure data so ranking is unit-tested. */
data class SearchEntry(val id: String, val name: String, val group: String = "", val extra: String = "")

/** Ranking: name starts with query (3) > a word of the name starts with it (2) > anything contains it (1). */
object SearchIndex {
    private val splitter = Regex("[^\\p{L}\\p{N}]+")

    fun tokens(s: String): List<String> = s.lowercase().split(splitter).filter { it.isNotEmpty() }

    fun score(e: SearchEntry, query: String): Int {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return 1
        val name = e.name.lowercase()
        val qt = tokens(q)
        if (qt.isEmpty()) return 0
        val nameTokens = tokens(name)
        val hay = (name + " " + e.group.lowercase() + " " + e.extra.lowercase() + " " + e.id.lowercase())
        // every query word has to match somewhere
        if (!qt.all { hay.contains(it) }) return 0
        val best = when {
            name.startsWith(q) -> 3
            qt.all { w -> nameTokens.any { it.startsWith(w) } } -> 2
            else -> 1
        }
        return best
    }

    /** Entries matching [query], best first; ties keep the original order. */
    fun search(entries: List<SearchEntry>, query: String): List<SearchEntry> {
        if (query.isBlank()) return entries
        return entries.withIndex().map { (i, e) -> Triple(e, score(e, query), i) }
            .filter { it.second > 0 }.sortedWith(compareByDescending<Triple<SearchEntry, Int, Int>> { it.second }.thenBy { it.third })
            .map { it.first }
    }
}
