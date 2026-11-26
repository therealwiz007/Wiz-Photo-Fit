package com.wizlabs.photofit.engine

/** What the form asks for. Null fields are "not required" and produce no row. */
data class ComplianceSpec(
    val widthPx: Int, val heightPx: Int, val kbMin: Int?, val kbMax: Int?, val format: String,
    val dpi: Int? = null, val background: BgRule? = null, val headRatio: Float? = null,
)

/** What the exported file really is, measured from the output (never guessed). */
data class Measured(
    val widthPx: Int, val heightPx: Int, val bytes: Int, val format: String, val dpi: Int,
    val cornerRgb: Int? = null, val headRatio: Float? = null,
)

enum class BgRule { WHITE, LIGHT }

data class Check(val name: String, val pass: Boolean, val detail: String, val fix: String? = null)

object Compliance {
    /** Same limits as the encoder: max is decimal KB (×1000), min is binary (×1024). */
    fun evaluate(s: ComplianceSpec, m: Measured): List<Check> = buildList {
        val dimOk = m.widthPx == s.widthPx && m.heightPx == s.heightPx
        add(Check("Dimensions", dimOk, "${m.widthPx} × ${m.heightPx} px (need ${s.widthPx} × ${s.heightPx})", if (dimOk) null else "Crop again to the exact size."))
        val kb = m.bytes / 1024f
        val tooBig = s.kbMax != null && m.bytes > s.kbMax * 1000
        val tooSmall = s.kbMin != null && m.bytes < s.kbMin * 1024
        val range = when { s.kbMin != null && s.kbMax != null -> "${s.kbMin}–${s.kbMax} KB"; s.kbMax != null -> "up to ${s.kbMax} KB"; s.kbMin != null -> "at least ${s.kbMin} KB"; else -> "any size" }
        add(Check("File size", !tooBig && !tooSmall, "${"%.1f".format(kb)} KB (need $range)",
            when { tooBig -> "Tap Smaller."; tooSmall -> "Tap Bigger."; else -> null }))
        val fmtOk = m.format.equals(s.format, ignoreCase = true)
        add(Check("Format", fmtOk, "${m.format.uppercase()} (need ${s.format.uppercase()})", if (fmtOk) null else "Export as ${s.format.uppercase()}."))
        if (s.dpi != null) add(Check("Resolution", m.dpi == s.dpi, "${m.dpi} dpi (need ${s.dpi})", if (m.dpi == s.dpi) null else "Re-save to reset the dpi."))
        if (s.background != null && m.cornerRgb != null) {
            val ok = backgroundOk(s.background, m.cornerRgb)
            add(Check("Background", ok, if (s.background == BgRule.WHITE) "white needed" else "light colour needed", if (ok) null else "Use the Background tool."))
        }
        if (s.headRatio != null && m.headRatio != null) {
            val ok = kotlin.math.abs(m.headRatio - s.headRatio) <= 0.08f
            add(Check("Face size", ok, "${(m.headRatio * 100).toInt()} % of height (aim ${(s.headRatio * 100).toInt()} %)", if (ok) null else "Use Auto-fit face."))
        }
    }

    fun backgroundOk(rule: BgRule, rgb: Int): Boolean {
        val r = rgb shr 16 and 0xFF; val g = rgb shr 8 and 0xFF; val b = rgb and 0xFF
        return when (rule) {
            BgRule.WHITE -> r >= 235 && g >= 235 && b >= 235
            BgRule.LIGHT -> (0.299 * r + 0.587 * g + 0.114 * b) >= 190
        }
    }

    fun failed(checks: List<Check>) = checks.count { !it.pass }
}
