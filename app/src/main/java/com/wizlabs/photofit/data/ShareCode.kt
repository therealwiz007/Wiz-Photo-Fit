package com.wizlabs.photofit.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Preset share codes: "WPF1." + base64url(raw DEFLATE(short-key JSON)). Works offline as plain text
 * (WhatsApp, SMS) or as a wizphotofit://p/<code> link that opens the Import screen.
 */
object ShareCode {
    private const val PREFIX = "WPF1."
    const val LINK = "wizphotofit://p/"

    fun encode(p: Preset): String {
        val o = JSONObject().put("n", p.name.take(60)).put("p", JSONArray().apply {
            p.parts.take(8).forEach { t ->
                put(JSONObject().apply {
                    put("k", t.kind.ordinal); if (t.kind == PartKind.EXTRA) put("l", t.label.take(40))
                    val px = t.size.px; val ph = t.size.phys
                    // exact pixels win: they are what the form checks
                    if (px != null) { put("w", px.w); put("h", px.h); put("u", LenUnit.PX.ordinal) }
                    else if (ph != null) { put("w", ph.first.toDouble()); put("h", ph.second.toDouble()); put("u", t.size.unit.ordinal) }
                    put("d", t.size.dpi); t.kbMin?.let { put("a", it) }; t.kbMax?.let { put("b", it) }
                    put("f", t.format.ordinal); put("g", t.background.ordinal); if (t.nameDate) put("s", 1)
                    t.hint?.let { put("t", it.take(60)) }
                })
            }
        })
        val raw = o.toString().toByteArray(Charsets.UTF_8)
        val d = Deflater(9, true).apply { setInput(raw); finish() }
        val buf = ByteArray(raw.size + 64); val n = d.deflate(buf); d.end()
        return PREFIX + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(buf.copyOf(n))
    }

    /** Parses a code, a link, or text containing either. Throws IllegalArgumentException with a plain message. */
    fun decode(input: String, newId: String): Preset {
        val text = input.trim()
        require(text.length <= 4096) { "This code is too long." }
        val start = text.indexOf("WPF")
        require(start >= 0) { "No Photo Fit code found. It starts with WPF1." }
        val code = text.substring(start).takeWhile { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
        require(code.startsWith(PREFIX)) { "This code is from a newer version of the app. Please update." }
        val bytes = runCatching { java.util.Base64.getUrlDecoder().decode(code.removePrefix(PREFIX)) }.getOrNull()
            ?: throw IllegalArgumentException("The code is broken. Copy it again.")
        val inf = Inflater(true).apply { setInput(bytes) }
        val out = ByteArrayOutputStream(); val tmp = ByteArray(1024)
        try { while (!inf.finished()) { val n = inf.inflate(tmp); if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break; out.write(tmp, 0, n); require(out.size() < 16_384) { "This code is too long." } } }
        catch (e: java.util.zip.DataFormatException) { throw IllegalArgumentException("The code is broken. Copy it again.") }
        finally { inf.end() }
        val o = runCatching { JSONObject(out.toString("UTF-8")) }.getOrNull() ?: throw IllegalArgumentException("The code is broken. Copy it again.")
        val a = o.optJSONArray("p") ?: throw IllegalArgumentException("The code has no sizes.")
        require(a.length() in 1..8) { "The code has too many items." }
        val parts = (0 until a.length()).map { i ->
            val q = a.getJSONObject(i)
            val kind = PartKind.entries.getOrElse(q.optInt("k")) { PartKind.EXTRA }
            val unit = LenUnit.entries.getOrElse(q.optInt("u")) { LenUnit.PX }
            val w = q.optDouble("w").toFloat(); val h = q.optDouble("h").toFloat()
            require(w > 0 && h > 0) { "The code has a bad size." }
            val dpi = q.optInt("d", 200).coerceIn(50, 1200)
            val size = if (unit == LenUnit.PX) SizeSpec(px = IntSz(w.toInt(), h.toInt()), dpi = dpi) else SizeSpec(phys = w to h, unit = unit, dpi = dpi)
            val t = size.targetPx(); require(t.w <= 6000 && t.h <= 6000) { "The size in the code is too big." }
            Part(kind, if (kind == PartKind.EXTRA) q.optString("l", "Extra").take(40) else kind.label, size,
                if (q.has("a")) q.getInt("a") else null, if (q.has("b")) q.getInt("b") else null,
                ImgFormat.entries.getOrElse(q.optInt("f")) { ImgFormat.JPG }, Bg.entries.getOrElse(q.optInt("g")) { Bg.KEEP },
                nameDate = q.optInt("s") == 1, hint = q.optString("t").ifEmpty { null }?.take(60))
        }
        return Preset(newId, "mine", o.optString("n", "Shared size").take(60).ifBlank { "Shared size" }, parts, Confidence.USER, mine = true)
    }

    fun shareText(p: Preset): String {
        val c = encode(p)
        return "Photo Fit size: ${p.name}\nOpen in Wiz Photo Fit: $LINK$c\nOr paste this code in the app: $c"
    }
}
