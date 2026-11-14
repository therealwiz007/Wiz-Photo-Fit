package com.wizlabs.photofit

import com.wizlabs.photofit.data.IntSz
import com.wizlabs.photofit.data.LenUnit
import com.wizlabs.photofit.data.PresetJson
import com.wizlabs.photofit.engine.Codec
import com.wizlabs.photofit.engine.CropMath
import com.wizlabs.photofit.engine.CropState
import com.wizlabs.photofit.engine.EncodeResult
import com.wizlabs.photofit.engine.Padding
import com.wizlabs.photofit.engine.PdfWriter
import com.wizlabs.photofit.engine.TargetEncoder
import com.wizlabs.photofit.engine.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class UnitsTest {
    @Test fun cmAt200() = assertEquals(IntSz(276, 354), Units.toPx(3.5f, 4.5f, LenUnit.CM, 200))
    @Test fun mmAt300() = assertEquals(IntSz(413, 531), Units.toPx(35f, 45f, LenUnit.MM, 300))
    @Test fun inchAt300() = assertEquals(IntSz(600, 600), Units.toPx(2f, 2f, LenUnit.IN, 300))
    @Test fun pxIsPx() = assertEquals(IntSz(200, 230), Units.toPx(200f, 230f, LenUnit.PX, 72))
    @Test fun ratio() { assertEquals("20:23", Units.ratio(200, 230)); assertEquals("7:9", Units.ratio(350, 450)) }
}

/** Fake JPEG-like codec: size grows with quality and with pixel scale². */
private class FakeCodec(private val base: Int, override val lossy: Boolean = true) : Codec {
    var calls = 0
    override fun encode(scale: Float, quality: Int, keepSize: Boolean): ByteArray {
        calls++
        val px = if (keepSize) 1f else scale * scale
        val detail = if (keepSize) scale * scale else 1f
        val n = (base * px * detail * (0.2f + 0.8f * quality / 100f)).toInt().coerceAtLeast(64)
        return ByteArray(n).also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte() }
    }
}

class TargetEncoderTest {
    @Test fun landsInRange() {
        val c = FakeCodec(120_000)
        val r = TargetEncoder.run(c, 20 * 1024, 50 * 1000, allowFewerPixels = false)
        assertTrue(r is EncodeResult.Ok)
        val b = (r as EncodeResult.Ok).bytes.size
        assertTrue("size $b", b in 20 * 1024..50_000)
        assertTrue("calls ${c.calls}", c.calls <= 10)
    }

    @Test fun softensWhenQuality20TooBig() {
        val r = TargetEncoder.run(FakeCodec(400_000), null, 30_000, allowFewerPixels = false)
        assertTrue(r is EncodeResult.Ok); r as EncodeResult.Ok
        assertTrue(r.keepSize); assertTrue(r.scale < 1f); assertTrue(r.bytes.size <= 30_000)
    }

    @Test fun fewerPixelsWhenAllowed() {
        val r = TargetEncoder.run(FakeCodec(400_000), null, 30_000, allowFewerPixels = true) as EncodeResult.Ok
        assertTrue(!r.keepSize && r.scale < 1f)
    }

    @Test fun padsBelowMin() {
        val r = TargetEncoder.run(FakeCodec(5_000), 20 * 1024, 50_000, allowFewerPixels = false) as EncodeResult.Ok
        assertTrue(r.padded); assertTrue(r.bytes.size >= 20 * 1024 && r.bytes.size <= 50_000)
    }

    @Test fun impossibleFails() {
        val r = TargetEncoder.run(FakeCodec(50_000_000), null, 1_000, allowFewerPixels = false)
        assertTrue(r is EncodeResult.Fail)
    }

    @Test fun noLimit() { assertTrue(TargetEncoder.run(FakeCodec(80_000), null, null, false) is EncodeResult.Ok) }
}

class PaddingTest {
    /** Gaps under 4 bytes can't be filled exactly (smallest COM segment is 4 bytes): overshoot ≤ 3. */
    @Test fun tinyGapOvershootsAtMost3() {
        val j = ByteArray(1000).also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte() }
        assertTrue(Padding.padJpeg(j, 1001).size in 1001..1004)
    }
    private fun jpeg(n: Int) = ByteArray(n).also { it[0] = 0xFF.toByte(); it[1] = 0xD8.toByte(); it[n - 2] = 0xFF.toByte(); it[n - 1] = 0xD9.toByte() }
    @Test fun exactLength() {
        for (t in listOf(1004, 1005, 5000, 70_000, 200_001)) {
            val out = Padding.padJpeg(jpeg(1000), t)
            assertEquals(t, out.size)
            assertEquals(0xFF.toByte(), out[0]); assertEquals(0xD8.toByte(), out[1])
            assertEquals(0xD9.toByte(), out.last())
        }
    }
}

class PdfWriterTest {
    @Test fun wrapsJpeg() {
        val j = ByteArray(10_000) { 7 }
        val pdf = PdfWriter.wrapJpeg(j, 200, 230, 200)
        val s = String(pdf, Charsets.ISO_8859_1)
        assertTrue(s.startsWith("%PDF-1.4")); assertTrue(s.trimEnd().endsWith("%%EOF"))
        assertTrue(pdf.size - j.size < 1000)
        // xref offsets point at "n 0 obj"
        val xref = s.substring(s.indexOf("xref"))
        val offs = Regex("(\\d{10}) 00000 n").findAll(xref).map { it.groupValues[1].toInt() }.toList()
        offs.forEachIndexed { i, o -> assertTrue(s.startsWith("${i + 1} 0 obj", o)) }
    }
}

class PresetJsonTest {
    @Test fun allBundledPresetsParse() {
        val f = listOf("src/main/assets/presets.json", "app/src/main/assets/presets.json").map(::File).first { it.exists() }
        val text = f.readText()
        val total = org.json.JSONObject(text).getJSONArray("presets").length()
        val b = PresetJson.parseBundle(text)
        assertEquals(total, b.presets.size)
        for (p in b.presets) for (part in p.parts) {
            val t = part.size.targetPx()
            assertTrue("${p.id} ${part.label} px", t.w > 0 && t.h > 0)
            if (part.kbMin != null && part.kbMax != null) assertTrue("${p.id} kb", part.kbMin!! <= part.kbMax!!)
            assertTrue("${p.id} group", b.groups.any { it.id == p.group })
        }
    }

    @Test fun userPresetRoundTrip() {
        val b = PresetJson.parseBundle(listOf("src/main/assets/presets.json", "app/src/main/assets/presets.json").map(::File).first { it.exists() }.readText())
        val p = b.presets.first { it.id == "neet-ug" }.copy(id = "u-1", mine = true)
        val back = PresetJson.parsePreset(PresetJson.toJson(p), mine = true)
        assertEquals(p.parts.map { it.size.targetPx() }, back.parts.map { it.size.targetPx() })
        assertEquals(p.parts.map { it.kbMin to it.kbMax }, back.parts.map { it.kbMin to it.kbMax })
    }
}

class CropMathTest {
    @Test fun baseFitsInside() {
        val r = CropMath.rect(CropState.centered(4000, 3000), 4000, 3000, 200f / 230f)
        assertTrue(r.width <= 4000 && r.height <= 3000 + 0.5f)
        assertEquals(200f / 230f, r.width / r.height, 0.001f)
    }
    @Test fun panClamps() {
        val s = CropMath.pan(CropState.centered(1000, 1000).copy(zoom = 2f), 10_000f, 10_000f, 300f, 1000, 1000, 1f)
        val r = CropMath.rect(s, 1000, 1000, 1f)
        assertEquals(0f, r.left, 0.5f); assertEquals(0f, r.top, 0.5f)
    }
}

class ShareCodeTest {
    private fun bundle() = PresetJson.parseBundle(listOf("src/main/assets/presets.json", "app/src/main/assets/presets.json").map(::File).first { it.exists() }.readText())
    @Test fun roundTrip() {
        for (p in bundle().presets) {
            val back = com.wizlabs.photofit.data.ShareCode.decode("hello " + com.wizlabs.photofit.data.ShareCode.encode(p) + " bye", "u-x")
            assertEquals(p.id, p.parts.map { it.size.targetPx() }, back.parts.map { it.size.targetPx() })
            assertEquals(p.parts.map { it.kbMin to it.kbMax }, back.parts.map { it.kbMin to it.kbMax })
        }
    }
    @Test fun rejectsGarbage() {
        for (bad in listOf("", "hello", "WPF1.!!!!", "WPF2.abc", "WPF1." + "A".repeat(5000)))
            assertTrue(runCatching { com.wizlabs.photofit.data.ShareCode.decode(bad, "u") }.isFailure)
    }
}
