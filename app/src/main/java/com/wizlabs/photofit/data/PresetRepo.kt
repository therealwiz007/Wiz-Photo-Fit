package com.wizlabs.photofit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Bundled presets + the user's own ("Mine"), saved in filesDir/user_presets.json. */
class PresetRepo(private val ctx: Context) {
    val bundle: PresetJson.Bundle = ctx.assets.open("presets.json").bufferedReader().use { PresetJson.parseBundle(it.readText()) }
    private val file = File(ctx.filesDir, "user_presets.json")
    var mine by mutableStateOf(loadMine())
        private set

    val groups: List<PresetGroup> get() = bundle.groups
    val builtIn: List<Preset> get() = bundle.presets

    fun all(): List<Preset> = mine + builtIn
    fun find(id: String): Preset? = mine.firstOrNull { it.id == id } ?: builtIn.firstOrNull { it.id == id }

    fun search(q: String): List<Preset> {
        val t = q.trim().lowercase()
        if (t.isEmpty()) return all()
        return all().filter { p -> p.name.lowercase().contains(t) || p.id.contains(t) || groups.firstOrNull { it.id == p.group }?.label?.lowercase()?.contains(t) == true }
    }

    fun saveMine(p: Preset) {
        val list = mine.filterNot { it.id == p.id }.toMutableList(); list.add(0, p.copy(mine = true, group = "mine"))
        mine = list; persist()
    }

    fun deleteMine(id: String) { mine = mine.filterNot { it.id == id }; persist() }

    private fun loadMine(): List<Preset> = runCatching {
        if (!file.exists()) return emptyList()
        val a = JSONObject(file.readText()).getJSONArray("presets")
        (0 until a.length()).mapNotNull { runCatching { PresetJson.parsePreset(a.getJSONObject(it), mine = true) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun persist() {
        val o = JSONObject().put("schema", 1).put("presets", JSONArray().apply { mine.forEach { put(PresetJson.toJson(it)) } })
        val tmp = File(ctx.filesDir, "user_presets.json.tmp")
        tmp.writeText(o.toString()); tmp.renameTo(file)
    }

    companion object { fun newId() = "u-" + System.currentTimeMillis().toString(36) }
}
