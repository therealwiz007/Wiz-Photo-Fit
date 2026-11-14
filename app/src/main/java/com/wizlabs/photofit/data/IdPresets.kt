package com.wizlabs.photofit.data

/** ID / passport / visa photo sizes (common published sizes; embassies change rules, so users can copy and edit). */
object IdPresets {
    const val GROUP = "id"
    val group = PresetGroup(GROUP, "ID / Passport")

    /** Head height as a share of the photo height, used to auto-fit the face. */
    val headRatio = mapOf("id-us" to 0.6f, "id-schengen" to 0.75f, "id-uk" to 0.72f, "id-in-passport" to 0.7f, "id-canada" to 0.5f)

    private fun mm(id: String, name: String, w: Float, h: Float, bg: Bg, kbMax: Int? = null, dpi: Int = 300, px: IntSz? = null, hint: String? = null) =
        Preset(id, GROUP, name, listOf(Part(PartKind.PHOTO, "Photo", SizeSpec(px = px, phys = w to h, unit = LenUnit.MM, dpi = dpi), null, kbMax,
            background = bg, hint = hint)), Confidence.MEDIUM, remarks = "Common published size. Check the embassy or passport office rules before printing.")

    val all = listOf(
        mm("id-in-passport", "India passport / OCI / visa (35 × 45 mm)", 35f, 45f, Bg.WHITE, hint = "White background, face 70-80 %"),
        mm("id-us", "USA visa / passport (2 × 2 in)", 51f, 51f, Bg.WHITE, kbMax = 240, px = IntSz(600, 600), hint = "600 × 600 px, under 240 KB for online"),
        mm("id-schengen", "Schengen visa (35 × 45 mm)", 35f, 45f, Bg.LIGHT),
        mm("id-uk", "UK passport / visa (35 × 45 mm)", 35f, 45f, Bg.LIGHT),
        mm("id-canada", "Canada passport / visa (50 × 70 mm)", 50f, 70f, Bg.WHITE),
        mm("id-australia", "Australia passport (35 × 45 mm)", 35f, 45f, Bg.WHITE),
        mm("id-uae", "UAE visa (43 × 55 mm)", 43f, 55f, Bg.WHITE),
        mm("id-china", "China visa (33 × 48 mm)", 33f, 48f, Bg.WHITE),
        mm("id-stamp", "Stamp size (20 × 25 mm)", 20f, 25f, Bg.WHITE),
        mm("id-passport-in-std", "Passport size, India (3.5 × 4.5 cm)", 35f, 45f, Bg.WHITE),
    )
}
