package com.wizlabs.photofit.data

import com.wizlabs.photofit.engine.Units

enum class LenUnit(val label: String) { PX("px"), CM("cm"), MM("mm"), IN("inch") }
enum class ImgFormat(val ext: String, val mime: String) {
    JPG("jpg", "image/jpeg"), PNG("png", "image/png"), PDF("pdf", "application/pdf"), WEBP("webp", "image/webp")
}
enum class Bg { KEEP, WHITE, LIGHT, CUSTOM }
enum class Confidence { HIGH, MEDIUM, LOW, USER }
enum class PartKind(val label: String) {
    PHOTO("Photo"), SIGNATURE("Signature"), THUMB("Thumb impression"), DECLARATION("Declaration"),
    PARENT_SIGNATURE("Parent signature"), POSTCARD("Postcard photo"), EXTRA("Extra")
}

data class IntSz(val w: Int, val h: Int)

/** A target size: exact pixels, or a physical size in [unit] converted at [dpi]. */
data class SizeSpec(
    val px: IntSz? = null,
    val phys: Pair<Float, Float>? = null,
    val unit: LenUnit = LenUnit.CM,
    val dpi: Int = 200,
    val pxMin: IntSz? = null,
    val pxMax: IntSz? = null,
) {
    fun targetPx(): IntSz = px ?: phys?.let { Units.toPx(it.first, it.second, unit, dpi) } ?: IntSz(200, 230)
    /** Physical size in cm at the dpi (for display), even when the spec is in pixels. */
    fun cm(): Pair<Float, Float> { val t = targetPx(); return Units.pxToCm(t.w, dpi) to Units.pxToCm(t.h, dpi) }
}

data class Part(
    val kind: PartKind,
    val label: String,
    val size: SizeSpec,
    val kbMin: Int?,
    val kbMax: Int?,
    val format: ImgFormat = ImgFormat.JPG,
    val background: Bg = Bg.KEEP,
    val bgColor: Int? = null,
    val nameDate: Boolean = false,
    val colour: Boolean = false,
    val tripleSign: Boolean = false,
    val liveCapture: Boolean = false,
    val hint: String? = null,
)

data class Preset(
    val id: String,
    val group: String,
    val name: String,
    val parts: List<Part>,
    val confidence: Confidence,
    val remarks: String? = null,
    val sources: List<String> = emptyList(),
    val mine: Boolean = false,
    val copiedFrom: String? = null,
)

data class PresetGroup(val id: String, val label: String)
