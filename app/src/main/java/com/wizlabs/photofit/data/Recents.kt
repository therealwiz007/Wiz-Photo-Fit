package com.wizlabs.photofit.data

import android.content.Context

/** Last 5 presets the user opened (ids only, in SharedPreferences, never leaves the phone). */
object Recents {
    private const val KEY = "recent"
    const val MAX = 5
    private fun prefs(c: Context) = c.getSharedPreferences("photofit", Context.MODE_PRIVATE)

    fun load(c: Context): List<String> = prefs(c).getString(KEY, "")!!.split(',').filter { it.isNotBlank() }

    fun push(c: Context, id: String) { prefs(c).edit().putString(KEY, merge(load(c), id).joinToString(",")).apply() }

    /** Pure list logic: [id] first, no duplicates, at most [MAX]. */
    fun merge(old: List<String>, id: String): List<String> = (listOf(id) + old.filter { it != id }).take(MAX)

    fun seenIntro(c: Context) = prefs(c).getBoolean("intro", false)
    fun setSeenIntro(c: Context, v: Boolean = true) { prefs(c).edit().putBoolean("intro", v).apply() }
}
