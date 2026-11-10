#!/usr/bin/env python3
"""
پیش‌نمایش مستقل دارایی‌های برند — شاهد بصری قابل بازکردن بدون اندروید.

خروجی: docs/brand-preview.html  (همهٔ تصاویر data-URI هستند، بدون منبع بیرونی،
پس داخل پیش‌نمایش سندباکس هم کامل نمایش داده می‌شود)

اجرا:  python3 tools/make-brand-preview.py
"""
import base64
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
OUT = os.path.join(ROOT, "docs", "brand-preview.html")


def uri(path):
    with open(path, "rb") as fh:
        return "data:image/png;base64," + base64.b64encode(fh.read()).decode()


def circle_mask(src, size=220):
    """شبیه‌سازی ماسک دایره‌ای لانچر اندروید روی پیش‌زمینهٔ آداپتیو + پس‌زمینهٔ گرادیانی."""
    fg = Image.open(src).convert("RGBA").resize((size, size), Image.LANCZOS)
    bg = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    # گرادیان سرمه‌ای سایت
    for y in range(size):
        t = y / max(1, size - 1)
        r = int(19 + (7 - 19) * t)
        g = int(32 + (20 - 32) * t)
        b = int(66 + (30 - 66) * t)
        for x in range(size):
            bg.putpixel((x, y), (r, g, b, 255))
    bg.alpha_composite(fg)
    mask = Image.new("L", (size, size), 0)
    from PIL import ImageDraw
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(bg, (0, 0), mask)
    return out


def save_temp(img, name):
    path = os.path.join("/tmp", name)
    img.save(path)
    return path


def main():
    d = lambda name: os.path.join(RES, "drawable-nodpi", name)
    fore = uri(d("brand_icon_foreground.png"))
    mono = uri(d("brand_icon_monochrome.png"))
    splash = uri(d("brand_splash_icon.png"))
    marks = {px: uri(os.path.join(RES, "drawable-%s" % den, "brand_mark.png"))
             for den, px in (("mdpi", 40), ("hdpi", 60), ("xhdpi", 80), ("xxhdpi", 120), ("xxxhdpi", 160))}
    circle = uri(save_temp(circle_mask(d("brand_icon_foreground.png")), "brand-circle.png"))
    mono_circle = uri(save_temp(circle_mask(d("brand_icon_monochrome.png")), "brand-mono-circle.png"))

    rows = "".join(
        '<div class="cell"><span class="lbl">%sdp</span>'
        '<img src="%s" style="width:%dpx;height:%dpx" alt="نشان %sdp"></div>' % (px, u, px, px, px)
        for px, u in marks.items())

    html = """<!doctype html>
<html lang="fa" dir="rtl"><head><meta charset="utf-8"><title>نشان حق‌سنج در اپ — پیش‌نمایش</title>
<style>
 body{margin:0;background:#f4f2ec;color:#0f172a;font-family:Vazirmatn,Tahoma,system-ui,sans-serif}
 .wrap{max-width:1000px;margin:0 auto;padding:28px 20px 60px}
 h1{font-size:22px;margin:0 0 6px;font-weight:900}
 h2{font-size:16px;margin:34px 0 10px;color:#955628}
 p{line-height:1.9;font-size:13.5px;color:#334155;margin:6px 0}
 .card{background:#fbf9f5;border:1px solid #e4e0d6;border-radius:24px;padding:18px 20px;margin-top:10px}
 .row{display:flex;flex-wrap:wrap;gap:14px;align-items:flex-end}
 .cell{display:flex;flex-direction:column;align-items:center;gap:6px;padding:10px 12px;background:#efece4;border-radius:14px}
 .lbl{font-size:11px;color:#64748b;font-family:monospace}
 .dark{background:#0b1b28;color:#f8fafc;border-color:#1b2f3e}
 .dark h2{color:#dcbe86} .dark p{color:#cbd5e1}
 .dark .card{background:#12293a;border-color:#1b2f3e}
 .dark .cell{background:#0a1825}
 .gallery{display:flex;gap:18px;flex-wrap:wrap;align-items:center}
 .kv{display:grid;grid-template-columns:auto 1fr;gap:6px 14px;font-size:12.5px}
 .kv b{color:#955628;font-weight:800}
 .ok{color:#15803d;font-weight:800}
 .bad{color:#b91c1c;font-weight:800;text-decoration:line-through}
 .note{border-right:3px solid #ce9f58;padding-right:10px;margin:8px 0;font-size:13px}
</style></head><body><div class="wrap">
<h1>نشان حق‌سنج در اپ اندروید — پیش از/پس از اصلاح</h1>
<p>همهٔ تصاویر پایین همان فایل‌های داخل APK هستند و با <code>tools/make-brand-assets.py</code> از تک‌منبع سایت
(<code>public/brand/logo-mark.png</code>) ساخته شده‌اند. پیش‌نمایش بدون منبع بیرونی است.</p>

<h2>۱) نشان در اندازه‌های واقعی برنامه</h2>
<div class="card"><div class="row">%s</div>
<p class="note">قبلاً یک فایل ۶۲۰×۶۴۰ در <code>drawable-nodpi</code> بود و در قاب مربع کشیده می‌شد
(۳٫۲٪ کشیدگی افقی). حالا هر چگالی فایل مربعِ همان اندازه را دارد و نسبت تصویر حفظ می‌شود.</p></div>

<h2>۲) آیکون لانچر — پیش‌زمینهٔ آداپتیو (ناحیهٔ ایمن ۶۲٪)</h2>
<div class="card"><div class="gallery">
 <div class="cell"><img src="%s" style="width:120px;height:120px" alt="foreground"><span class="lbl">foreground 108dp</span></div>
 <div class="cell"><img src="%s" style="width:120px;height:120px" alt="monochrome"><span class="lbl">monochrome</span></div>
 <div class="cell"><img src="%s" style="width:120px;height:120px" alt="cercle"><span class="lbl">ماسک دایره‌ای</span></div>
 <div class="cell"><img src="%s" style="width:120px;height:120px" alt="mono circle"><span class="lbl">مونوکروم + ماسک</span></div>
</div>
<p class="note">قبلاً «تصویر آمادهٔ قاب‌دار» به‌عنوان پیش‌زمینه استفاده می‌شد؛ زیر ماسک سیستم
<span class="bad">دو قاب گرد روی هم</span> می‌افتاد. حالا پس‌زمینه گرادیان سرمه‌ای و پیش‌زمینه فقط نشان است.</p></div>

<h2>۳) آیکون اسپلش (اندروید ۱۲+)</h2>
<div class="card"><div class="gallery">
 <div class="cell" style="background:#07141e"><img src="%s" style="width:110px;height:110px" alt="splash"></div>
</div>
<p class="note">محتوا داخل دایرهٔ میانی می‌ماند؛ نسخهٔ قبل لبه‌های نشان را از دست می‌داد.</p></div>

<h2>۴) حالت تیره</h2>
<div class="dark"><div class="card"><div class="row">%s</div>
<p>همان فایل‌ها روی سطح تیرهٔ سایت (<code>#0b1b28</code>). پس‌زمینهٔ شفاف نشان است، پس جعبهٔ سیاه پشت آن نمی‌ماند.</p></div></div>

<h2>۵) چه چیزی عوض شد؟</h2>
<div class="card"><div class="kv">
 <b>قبل</b><span>۶۲۰×۶۴۰ nodpi · فشرده‌شدن در قاب مربع · قاب دوتایی در آیکون · بریدن اسپلش · alt اجباری «نشان حق‌سنج»</span>
 <b>حالا</b><span class="ok">مربع در ۵ چگالی · Fit بدون کشیدگی · آداپتیو با ناحیهٔ ایمن · مونوکروم تم‌پذیر · اسپلش امن · alt فقط وقتی معنا دارد</span>
 <b>اندازه‌ها</b><span>40 / 60 / 80 / 120 / 160 px · foreground و splash و monochrome 512px</span>
</div></div>
<p style="margin-top:24px;font-size:12px;color:#64748b">بازتولید: <code>python3 tools/make-brand-assets.py &amp;&amp; python3 tools/make-brand-preview.py</code></p>
</div></body></html>
""" % (rows, fore, mono, circle, mono_circle, splash, rows)

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(html)
    print("نوشته شد:", os.path.relpath(OUT, ROOT), os.path.getsize(OUT), "بایت")


if __name__ == "__main__":
    main()
