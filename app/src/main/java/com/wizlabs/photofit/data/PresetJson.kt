package com.wizlabs.photofit.data

import org.json.JSONArray
import org.json.JSONObject

/** Reads and writes presets (bundled assets/presets.json and the user's own). Unknown keys are ignored. */
object PresetJson {
    private val PART_KEYS = listOf(
        "photo" to PartKind.PHOTO, "signature" to PartKind.SIGNATURE, "thumb" to PartKind.THUMB,
        "declaration" to PartKind.DECLARATION, "parentSignature" to PartKind.PARENT_SIGNATURE, "postcard" to PartKind.POSTCARD,
    )

    data class Bundle(val groups: List<PresetGroup>, val presets: List<Preset>, val compiled: String?)

    fun parseBundle(text: String): Bundle {
        val o = JSONObject(text)
        val groups = o.optJSONArray("groups")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).let { g -> PresetGroup(g.getString("id"), g.getString("label")) } } } ?: emptyList()
        val presets = o.optJSONArray("presets")?.let { a -> (0 until a.length()).mapNotNull { runCatching { parsePreset(a.getJSONObject(it)) }.getOrNull() } } ?: emptyList()
        return Bundle(groups, presets, o.optString("compiled").ifEmpty { null })
    }

    fun parsePreset(o: JSONObject, mine: Boolean = false): Preset {
        val parts = ArrayList<Part>()
        for ((key, kind) in PART_KEYS) o.optJSONObject(key)?.let { parts += parsePart(it, kind, kind.label) }
        o.optJSONArray("extra")?.let { a -> for (i in 0 until a.length()) a.getJSONObject(i).let { parts += parsePart(it, PartKind.EXTRA, it.optString("label", "Extra")) } }
        o.optJSONArray("parts")?.let { a -> // user preset form: explicit ordered parts
            parts.clear()
            for (i in 0 until a.length()) a.getJSONObject(i).let { p ->
                val kind = runCatching { PartKind.valueOf(p.getString("kind")) }.getOrDefault(PartKind.EXTRA)
                parts += parsePart(p, kind, p.optString("label", kind.label))
            }
        }
        require(parts.isNotEmpty()) { "preset has no parts" }
        val conf = when (o.optString("confidence")) { "high" -> Confidence.HIGH; "medium" -> Confidence.MEDIUM; "low" -> Confidence.LOW; else -> Confidence.USER }
        return Preset(
            id = o.getString("id"), group = o.optString("group", "mine"), name = o.getString("name"), parts = parts,
            confidence = conf, remarks = o.optString("remarks").ifEmpty { null },
            sources = o.optJSONArray("sources")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            mine = mine || o.optBoolean("mine"), copiedFrom = o.optString("copiedFrom").ifEmpty { null },
        )
    }

    private fun pair(o: JSONObject, k: String): Pair<Float, Float>? = o.optJSONArray(k)?.takeIf { it.length() == 2 }?.let { it.getDouble(0).toFloat() to it.getDouble(1).toFloat() }
    private fun isz(o: JSONObject, k: String): IntSz? = pair(o, k)?.let { IntSz(it.first.toInt(), it.second.toInt()) }

    fun parsePart(o: JSONObject, kind: PartKind, label: String): Part {
        val px = isz(o, "px")
        var unit = LenUnit.CM
        var phys = pair(o, "cm")
        if (phys == null) pair(o, "mm")?.let { phys = it; unit = LenUnit.MM }
        if (phys == null) pair(o, "in")?.let { phys = it; unit = LenUnit.IN }
        o.optString("unit").takeIf { it.isNotEmpty() }?.let { u -> runCatching { unit = LenUnit.valueOf(u.uppercase()) } }
        if (phys == null) pair(o, "phys")?.let { phys = it }
        val kb = o.optJSONArray("kb")
        val kbMin = kb?.let { if (it.isNull(0)) null else it.getInt(0) }
        val kbMax = kb?.let { if (it.length() < 2 || it.isNull(1)) null else it.getInt(1) }
        val fmt = when (o.optString("format", "jpg").lowercase()) { "png" -> ImgFormat.PNG; "pdf" -> ImgFormat.PDF; "webp" -> ImgFormat.WEBP; else -> ImgFormat.JPG }
        val bg = when (o.optString("background", "keep").lowercase()) { "white" -> Bg.WHITE; "light" -> Bg.LIGHT; "custom" -> Bg.CUSTOM; else -> Bg.KEEP }
        return Part(
            kind = kind, label = label,
            size = SizeSpec(px = px, phys = phys, unit = unit, dpi = o.optInt("dpi", 200), pxMin = isz(o, "pxMin"), pxMax = isz(o, "pxMax")),
            kbMin = kbMin, kbMax = kbMax, format = fmt, background = bg,
            bgColor = if (o.has("bgColor")) o.getInt("bgColor") else null,
            nameDate = o.optBoolean("requiresName") || o.optBoolean("requiresDate") || o.optBoolean("nameDate"),
            colour = o.optBoolean("colour"), tripleSign = o.optString("mode") == "triple" || o.optBoolean("triple"),
            liveCapture = o.optBoolean("liveCapture"), hint = o.optString("hint").ifEmpty { null },
        )
    }

    /** User preset JSON (ordered parts, explicit units). */
    fun toJson(p: Preset): JSONObject = JSONObject().apply {
        put("id", p.id); put("group", p.group); put("name", p.name); put("mine", p.mine)
        p.copiedFrom?.let { put("copiedFrom", it) }
        put("confidence", "user")
        put("parts", JSONArray().apply { p.parts.forEach { put(partJson(it)) } })
    }

    fun partJson(t: Part): JSONObject = JSONObject().apply {
        put("kind", t.kind.name); put("label", t.label)
        t.size.px?.let { put("px", JSONArray(listOf(it.w, it.h))) }
        t.size.phys?.let { put("phys", JSONArray(listOf(it.first.toDouble(), it.second.toDouble()))); put("unit", t.size.unit.name) }
        put("dpi", t.size.dpi)
        put("kb", JSONArray().apply { put(t.kbMin ?: JSONObject.NULL); put(t.kbMax ?: JSONObject.NULL) })
        put("format", t.format.ext); put("background", t.background.name.lowercase())
        t.bgColor?.let { put("bgColor", it) }
        put("nameDate", t.nameDate); put("triple", t.tripleSign)
        t.hint?.let { put("hint", it) }
    }
}
