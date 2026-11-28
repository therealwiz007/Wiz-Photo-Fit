#!/usr/bin/env python3
"""Writes Snip SVGs (same 100x100 geometry and the same emotion table as ui/mascot/SnipArt.kt + SnipPose.kt SnipLooks).
Usage: python3 tools/store-art/snip_svg.py   -> store-assets/mascot/snip*.svg and store-assets/qa/emotion-sheet.svg"""
import re, math, pathlib
root = pathlib.Path(__file__).resolve().parents[2]
src = (root / "app/src/main/java/com/wizlabs/photofit/ui/mascot/SnipPose.kt").read_text()
names = ["eh","arc","bt","by","byr","mc","mo","mw","lean","arms","chin","blush","spark","sweat","zz","bang","qm","tear","droop","bounce"]
defaults = dict(blush=0.3, chin=0, spark=0, sweat=0, zz=0, bang=0, qm=0, tear=0, droop=0, bounce=0)
looks = {}
for m in re.finditer(r"SnipExpression\.(\w+) -> row\((.*)\)", src):
    parts = [p.strip() for p in m.group(2).split(",")]
    d = dict(defaults); pos = names[:10]
    for k, p in enumerate(parts):
        if "=" in p: n, v = [x.strip() for x in p.split("=")]
        else: n, v = pos[k], p
        d[n] = float(v.replace("f", ""))
    looks[m.group(1)] = d
ORDER = ["NEUTRAL","HAPPY","EXCITED","PROUD","CURIOUS","THINKING","FOCUSED","SURPRISED","WORRIED","SAD","SLEEPY"]
INK, TAPE, TICK, PHOTO = "#12142A", "#38BDF8", "#4338CA", "#BAE6FD"

def snip(e, dark=False, flourish=0.0):
    c = looks[e]; frame = "#EEF0FF" if dark else INK
    o = []
    a = o.append
    a(f'<rect x="33" y="90" width="13" height="6" rx="3" fill="{frame}"/><rect x="54" y="90" width="13" height="6" rx="3" fill="{frame}"/>')
    lift = -flourish * 14
    a(f'<path d="M74,72 C84,{76+lift} 90,{84+lift*1.4} 88,{94+lift*.6} L94,{93+lift*.6} C96,{82+lift*1.4} 90,{72+lift} 80,68z" fill="{TAPE}"/>')
    if dark: a('<rect x="20.5" y="16.5" width="59" height="63" rx="12" fill="#171A2E"/>')
    a(f'<rect x="22" y="18" width="56" height="60" rx="10" fill="{frame}"/><rect x="27" y="23" width="46" height="50" rx="6" fill="#fff"/><rect x="30" y="26" width="40" height="44" rx="4" fill="{PHOTO}"/>')
    bl = min(max(c["blush"] * .55, 0), .8)
    a(f'<circle cx="36" cy="55" r="3.4" fill="#FDA4AF" opacity="{bl:.2f}"/><circle cx="64" cy="55" r="3.4" fill="#FDA4AF" opacity="{bl:.2f}"/>')
    for i in (0, 1):
        cx = 41 + 18 * i; h = 6 * c["eh"]; w = 6 * (1 + .3 * max(min(c["eh"] - 1, .6), -.5))
        if c["arc"] < .99: a(f'<ellipse cx="{cx}" cy="46" rx="{w/2:.2f}" ry="{max(h,1.1)/2:.2f}" fill="{INK}" opacity="{1-c["arc"]:.2f}"/>')
        if c["arc"] > .01: a(f'<path d="M{cx-3.6},48 Q{cx},41.5 {cx+3.6},48" fill="none" stroke="{INK}" stroke-width="3.2" stroke-linecap="round" opacity="{c["arc"]:.2f}"/>')
        th = math.radians(c["bt"]); s = 1 if i == 0 else -1; by = 38.5 - (c["by"] if i == 0 else c["byr"])
        a(f'<line x1="{cx+s*4.2*math.cos(th):.2f}" y1="{by+4.2*math.sin(th):.2f}" x2="{cx-s*4.2*math.cos(th):.2f}" y2="{by-4.2*math.sin(th):.2f}" stroke="{INK}" stroke-width="2.4" stroke-linecap="round"/>')
    hw = 7 * c["mw"]; op = min(max(c["mo"], 0), 1)
    if op < .99: a(f'<path d="M{50-hw:.2f},{57-c["mc"]*1.6:.2f} Q50,{57+c["mc"]*6:.2f} {50+hw:.2f},{57-c["mc"]*1.6:.2f}" fill="none" stroke="{INK}" stroke-width="3.2" stroke-linecap="round" opacity="{1-op:.2f}"/>')
    if op > .01: a(f'<ellipse cx="50" cy="{55.5+(3+7*op*(.65+.35*max(c["mc"],0)))/2:.2f}" rx="{hw:.2f}" ry="{(3+7*op*(.65+.35*max(c["mc"],0)))/2:.2f}" fill="{INK}" opacity="{op:.2f}"/>')
    a(f'<rect x="19" y="66" width="62" height="8" rx="4" fill="{TAPE}"/>')
    for x in range(25, 78, 6): a(f'<line x1="{x}" y1="66" x2="{x}" y2="{71 if (x-25)%12==0 else 69}" stroke="{TICK}" stroke-width="1.1"/>')
    arms = min(c["arms"], 1); ch = c["chin"]
    a(f'<path transform="rotate({-arms*22:.1f} 80 {16-arms*7:.1f})" d="M71,{16-arms*7:.1f} L80,{16-arms*7:.1f} L80,{25-arms*7:.1f}" fill="none" stroke="{frame}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>')
    hx = 20 + 17 * ch + (12 - 20) * arms * (1 - ch); hy = 84 - 21 * ch + (44 - 84) * arms * (1 - ch) + c["droop"] * 3
    a(f'<path d="M{hx+9:.1f},{hy:.1f} L{hx:.1f},{hy:.1f} L{hx:.1f},{hy-9:.1f}" fill="none" stroke="{frame}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>')
    body = f'<g transform="translate(50 95) rotate({c["lean"]}) scale(1 {1-c["droop"]*.03:.3f}) translate(-50 -95)">' + "".join(o) + "</g>"
    ex = []
    if c["spark"] > .02:
        for x, y, r in ((88, 30, 5 * c["spark"]), (13, 30, 3.6 * c["spark"])): ex.append(f'<path d="M{x},{y-r} Q{x},{y} {x+r},{y} Q{x},{y} {x},{y+r} Q{x},{y} {x-r},{y} Q{x},{y} {x},{y-r}z" fill="{TAPE}"/>')
    if c["sweat"] > .02: ex.append('<path d="M70,29 Q66.5,34 70,36 Q73.5,34 70,29z" fill="#7DD3FC"/>')
    if c["tear"] > .02: ex.append('<circle cx="41" cy="54" r="1.9" fill="#7DD3FC"/>')
    if c["zz"] > .02: ex.append(f'<path d="M82,20 h4 l-4 4 h4 M87,12 h5 l-5 5 h5" fill="none" stroke="{frame}" stroke-width="2.4" stroke-linecap="round"/>')
    if c["bang"] > .02: ex.append(f'<line x1="89" y1="6" x2="89" y2="17" stroke="{frame}" stroke-width="3.2" stroke-linecap="round"/><circle cx="89" cy="22" r="1.9" fill="{frame}"/>')
    if c["qm"] > .02: ex.append(f'<path d="M83,14 C83,6 95,6 95,14 C95,19 89,19 89,25" fill="none" stroke="{frame}" stroke-width="2.4" stroke-linecap="round"/><circle cx="89" cy="30" r="1.6" fill="{frame}"/>')
    return body + "".join(ex)

out = root / "store-assets/mascot"; out.mkdir(parents=True, exist_ok=True)
def svg(inner, vb="0 0 100 100", w=512, h=512): return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{w}" height="{h}">{inner}</svg>\n'
(out / "snip.svg").write_text(svg(snip("HAPPY")))
(out / "snip-proud.svg").write_text(svg(snip("PROUD", flourish=.8)))
cells = "".join(f'<g transform="translate({(i%6)*110+5} {(i//6)*135+8})">{snip(e)}<text x="50" y="112" font-family="Plus Jakarta Sans, Arial" font-weight="700" font-size="9" text-anchor="middle" fill="{INK}">{e.title()}</text></g>' for i, e in enumerate(ORDER))
q = root / "store-assets/qa"; q.mkdir(parents=True, exist_ok=True)
(q / "emotion-sheet.svg").write_text(svg(f'<rect width="670" height="280" fill="#F7F8FC"/>{cells}', "0 0 670 280", 1340, 560))
print("emotions", len(looks), "wrote", out, q)
