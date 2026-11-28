#!/usr/bin/env python3
"""Frames the REAL emulator captures (store-assets/raw/*.png) into 1080x1920 store screenshots + the feature graphic.
HTML here is only the frame and headline; every device screen is an unedited adb screencap.
Run: python3 tools/store-art/make_shots.py"""
import pathlib, subprocess
root = pathlib.Path(__file__).resolve().parents[2]
art = root / "tools/store-art"; out = root / "store-assets/screenshots"; out.mkdir(parents=True, exist_ok=True)
tmp = pathlib.Path("/tmp/wpf-shots"); tmp.mkdir(exist_ok=True)
SHOTS = [
    ("raw1_result.png", "Exam photo & sign in exact KB"),
    ("raw2_home_search.png", "36 exam presets, searchable"),
    ("raw3_crop_editor.png", "Crop with guides"),
    ("raw4_signature_compare.png", "Clean, sharp signatures"),
    ("raw5_id_editor.png", "Passport & ID photos"),
    ("raw6_print.png", "Print 4×6 or A4 sheets"),
    ("raw7_builder.png", "Your own custom sizes"),
    ("raw8_dark_home.png", "Light and dark themes"),
]
FONT = f"@font-face{{font-family:J;src:url('file://{art}/jakarta.ttf');font-weight:200 800}}@font-face{{font-family:M;src:url('file://{art}/dmmono.ttf')}}"
def render(html, png, w, h):
    f = tmp / (pathlib.Path(png).stem + ".html"); f.write_text(html)
    subprocess.run(["sh", str(art / "render.sh"), str(f), str(png), str(w), str(h)], check=True)
for i, (raw, head) in enumerate(SHOTS, 1):
    html = f"""<!doctype html><meta charset=utf-8><style>{FONT}
html,body{{margin:0}}body{{width:1080px;height:1920px;background:#F7F8FC;position:relative;overflow:hidden;font-family:J,sans-serif}}
.blob{{position:absolute;right:-220px;top:250px;width:760px;height:760px;border-radius:50%;background:linear-gradient(135deg,#4338CA,#312E81)}}
.blob2{{position:absolute;left:-260px;bottom:120px;width:520px;height:520px;border-radius:50%;background:#EEF0F9}}
h1{{position:absolute;left:70px;right:70px;top:72px;margin:0;font-size:78px;line-height:88px;font-weight:800;color:#12142A;letter-spacing:-1.5px}}
.bar{{position:absolute;left:70px;top:262px;width:140px;height:10px;border-radius:5px;background:#38BDF8}}
.dev{{position:absolute;left:110px;top:320px;width:860px;height:1700px;border-radius:78px 78px 0 0;border:14px solid #12142A;border-bottom:0;overflow:hidden;box-shadow:0 30px 80px rgba(18,20,42,.35);background:#000}}
.dev img{{width:860px;display:block}}</style>
<div class=blob></div><div class=blob2></div><h1>{head}</h1><div class=bar></div>
<div class=dev><img src="file://{root}/store-assets/raw/{raw}"></div>"""
    render(html, out / f"{i}.png", 1080, 1920)
# feature graphic: wordmark + value line + real capture card + Snip
card = f"file://{root}/store-assets/raw/raw1_result.png"
fg = f"""<!doctype html><meta charset=utf-8><style>{FONT}
html,body{{margin:0}}body{{width:1024px;height:500px;background:#F7F8FC;position:relative;overflow:hidden;font-family:J,sans-serif}}
.blob{{position:absolute;right:-120px;top:-90px;width:620px;height:620px;border-radius:50%;background:linear-gradient(135deg,#4338CA,#312E81)}}
.w{{position:absolute;left:96px;top:150px;font-size:64px;font-weight:800;color:#12142A;letter-spacing:-1px;white-space:nowrap}}.w b{{color:#4338CA}}
.v{{position:absolute;left:98px;top:238px;font-size:30px;font-weight:600;color:#5A5F7D}}
.bar{{position:absolute;left:98px;top:300px;width:110px;height:8px;border-radius:4px;background:#38BDF8}}
.card{{position:absolute;left:590px;top:150px;width:340px;height:152px;border-radius:26px;overflow:hidden;background:#fff;box-shadow:0 14px 40px rgba(18,20,42,.35)}}
.card div{{position:absolute;inset:0;background:url('{card}') -14.7px -416px/369.4px auto no-repeat}}
.pill{{position:absolute;left:590px;top:326px;padding:10px 20px;border-radius:30px;background:#fff;font-family:M;font-size:22px;color:#12142A;box-shadow:0 10px 30px rgba(18,20,42,.3)}}
.snip{{position:absolute;left:700px;top:290px;width:190px;height:190px}}</style>
<div class=blob></div><div class=w>Wiz Photo <b>Fit</b></div><div class=v>Exam photo & sign, exact KB</div><div class=bar></div>
<div class=card><div></div></div><img class=snip src="file://{root}/store-assets/mascot/snip-proud.svg">"""
render(fg, root / "store-assets/feature-graphic-1024x500.png", 1024, 500)
print("done")
