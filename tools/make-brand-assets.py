#!/usr/bin/env python3
"""
بازتولید دارایی‌های برند اندروید از «تک‌منبع» سایت.

منبع: public/brand/logo-mark.png (۱۰۲۴×۱۰۲۴، تک‌منبع سایت) و public/brand/monochrome
(اگر نبود، از همان نشان ساخته می‌شود).

چرا این اسکریپت وجود دارد:
  نسخهٔ قبلی اپ یک PNG ۶۲۰×۶۴۰ را در drawable-nodpi گذاشته بود؛ در Compose با
  Modifier.size(40.dp) در قاب *مربع* کشیده می‌شد (۳٪ کشیدگی افقی)، در آیکون
  تطبیقی اندروید ۱۳ هم همان تصویر با گوشه‌های گردِ *داخل خودش* زیر ماسک سیستم
  می‌رفت و «قاب دوتایی» می‌ساخت. اینجا همهٔ مشتقات از یک مربعِ تراز‌شده و با
  ناحیهٔ ایمن درست ساخته می‌شوند.

اجرا:  python3 tools/make-brand-assets.py [--site /path/to/hsai]
"""
import argparse
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

# چگالی‌های اندروید؛ اندازهٔ پایه ۴۰dp (اندازهٔ پیش‌فرض BrandEmblem)
DENSITIES = {"mdpi": 40, "hdpi": 60, "xhdpi": 80, "xxhdpi": 120, "xxxhdpi": 160}
# آیکون تطبیقی: بوم ۱۰۸dp، ناحیهٔ ایمن ۷۲dp → محتوا حداکثر ۶۶٫۶٪ بوم
ADAPTIVE_CANVAS = 512
ADAPTIVE_CONTENT = 0.62
# اسپلش اندروید ۱۲+: محتوا داخل دایرهٔ میانی بماند
SPLASH_CANVAS = 512
SPLASH_CONTENT = 0.58


def trimmed(img):
    """برش شفافیت اضافی، تا محتوا دقیقاً وسط مربع بنشیند."""
    box = img.getchannel("A").getbbox()
    return img.crop(box) if box else img


def square(img, size, content_ratio=1.0):
    """محتوا را به نسبت content_ratio از بوم مربعی، وسط‌چین و بدون تغییر نسبت تصویر می‌گذارد."""
    img = trimmed(img)
    inner = max(1, int(round(size * content_ratio)))
    scale = min(inner / img.width, inner / img.height)
    w, h = max(1, int(round(img.width * scale))), max(1, int(round(img.height * scale)))
    resized = img.resize((w, h), Image.LANCZOS)
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((size - w) // 2, (size - h) // 2))
    return canvas


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--site", default="/home/user/src/hsai",
                    help="ریشهٔ مخزن سایت (پوشهٔ public/brand داخل آن است)")
    ap.add_argument("--mono", default="",
                    help="مسیر اختیاری فایل monochrome سفید؛ پیش‌فرض از خود نشان ساخته می‌شود")
    args = ap.parse_args()

    master = os.path.join(args.site, "public", "brand", "logo-mark.png")
    if not os.path.exists(master):
        sys.exit("نشان اصلی پیدا نشد: " + master)
    mark = Image.open(master).convert("RGBA")
    if mark.width != mark.height:
        sys.exit("نشان اصلی باید مربع باشد؛ این نیست: %dx%d" % mark.size)

    written = []

    # ۱) نشان داخل برنامه: مربع، در هر چگالی، دقیقاً اندازهٔ فایل = dp×density
    for density, px in DENSITIES.items():
        folder = os.path.join(RES, "drawable-" + density)
        os.makedirs(folder, exist_ok=True)
        out = os.path.join(folder, "brand_mark.png")
        square(mark, px).save(out, optimize=True)
        written.append((out, px))

    nodpi = os.path.join(RES, "drawable-nodpi")
    os.makedirs(nodpi, exist_ok=True)

    # ۲) پیش‌زمینهٔ آیکون تطبیقی: شفاف، بدون قاب/پس‌زمینهٔ سوخته، داخل ناحیهٔ ایمن
    out = os.path.join(nodpi, "brand_icon_foreground.png")
    square(mark, ADAPTIVE_CANVAS, ADAPTIVE_CONTENT).save(out, optimize=True)
    written.append((out, ADAPTIVE_CANVAS))

    # ۳) آیکون مونوکروم (تم‌پذیر اندروید ۱۳): سیلوئت سفید، مربع و با ناحیهٔ ایمن
    mono_src = args.mono
    if not mono_src or not os.path.exists(mono_src):
        for candidate in ("brand_monochrome.png", "logo-monochrome.png", "../monochrome.png"):
            p = os.path.join(args.site, "public", "brand", candidate)
            if os.path.exists(p):
                mono_src = p
                break
    if mono_src and os.path.exists(mono_src):
        mono = Image.open(mono_src).convert("RGBA")
        # هر رنگ غیرشفاف را سفید خالص می‌کنیم تا ماسک تم درست کار کند
        alpha = mono.getchannel("A")
        white = Image.new("RGBA", mono.size, (255, 255, 255, 255))
        white.putalpha(alpha)
        mono = white
    else:
        # از خود نشان: آلفا را به‌عنوان سیلوئت بگیر (نشان روی زمینهٔ شفاف است)
        alpha = mark.getchannel("A").point(lambda v: 255 if v > 90 else 0)
        mono = Image.new("RGBA", mark.size, (255, 255, 255, 255))
        mono.putalpha(alpha)
    out = os.path.join(nodpi, "brand_icon_monochrome.png")
    square(mono, ADAPTIVE_CANVAS, ADAPTIVE_CONTENT).save(out, optimize=True)
    written.append((out, ADAPTIVE_CANVAS))

    # ۴) آیکون اسپلش اندروید ۱۲+: کمی کوچک‌تر تا ماسک دایره‌ای نبُرد
    out = os.path.join(nodpi, "brand_splash_icon.png")
    square(mark, SPLASH_CANVAS, SPLASH_CONTENT).save(out, optimize=True)
    written.append((out, SPLASH_CANVAS))

    for path, px in written:
        img = Image.open(path)
        rel = os.path.relpath(path, ROOT)
        print("%-56s %4dx%-4d %6d B  alpha_bbox=%s" % (
            rel, img.width, img.height, os.path.getsize(path),
            img.getchannel("A").getbbox()))


if __name__ == "__main__":
    main()
