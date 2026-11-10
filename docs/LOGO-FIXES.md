# اصلاح نشان و آیکون‌های برند در اپ اندروید — حق‌سنج

**تاریخ:** ۱۴۰۵/۰۸/۱۸ · **مبنا:** `/home/user/src/hsai/public/brand/` (تک‌منبع سایت)
**شاهد بصری:** `docs/brand-preview.html` (بدون منبع بیرونی؛ همهٔ تصاویر data-URI)

---

## ۱) مشکل چه بود؟ (پنج ایراد واقعی، نه سلیقه‌ای)

| # | ایراد | چرا باگ است |
|---|---|---|
| L-01 | `brand_mark.png` در `drawable-nodpi` با ابعاد **۶۲۰×۶۴۰** (غیرمربع) و نمایش با `Modifier.size(40.dp)` | تصویر در قاب مربع می‌رفت؛ نسبت تصویر سایت (۱:۱) حفظ نمی‌شد و نشان ۳٫۲٪ کشیده بود. `nodpi` هم یعنی هیچ مقیاس‌بندی چگالی انجام نمی‌شود و همان بیت‌مپ ۱٫۵ مگابایتی برای ۲۲ تا ۴۸dp بار می‌شود. |
| L-02 | `BrandEmblem` اندازه را از فراخواننده **نمی‌گرفت** (`modifier.size(40.dp)` در پایان زنجیره) | «نشان ۲۲dp کنار هر پیام» و «نشان ۴۸dp در صفحهٔ خالی» در واقع یک اندازه داشتند؛ تغییر اندازه در آینده بی‌اثر بود. |
| L-03 | آیکون تطبیقی: `<foreground>` = `app_icon_art` که **خودش یک قاب گرد مشکی با حاشیهٔ طلایی** بود، روی `background=black` | زیر ماسک دایره‌ای/مربعی لانچر، **دو قاب گرد روی هم** می‌افتاد و گوشهٔ طلایی بریده می‌شد. |
| L-04 | اسپلش اندروید ۱۲+: `windowSplashScreenAnimatedIcon = @drawable/brand_mark` که محتوا لبه‌به‌لبه است | آیکون اسپلش در دایرهٔ میانی ماسک می‌شود؛ لبه‌های نشان (دم قوس پایین) **بریده** می‌شد. |
| L-05 | `contentDescription = "نشان حق‌سنج"` روی **همهٔ** نمونه‌ها، از جمله آواتار کنار هر پیام | سایت `aria-hidden` می‌گذارد (تزئینی). در TalkBack هر پاسخ با «نشان حق‌سنج، شما…» شروع می‌شد. ضمناً `monochrome` (تم‌پذیر اندروید ۱۳) هم از همان فایل غیرمربع ساخته شده بود. |

---

## ۲) چه کاری انجام شد؟

### الف) بازتولید دارایی‌ها از تک‌منبع سایت
`tools/make-brand-assets.py` (قابل اجرا و تکرارپذیر) از `public/brand/logo-mark.png` می‌سازد:

| فایل | ابعاد | کاربرد |
|---|---|---|
| `drawable-mdpi/brand_mark.png` | ۴۰×۴۰ | نشان درون‌برنامه (۴۰dp) |
| `drawable-hdpi/brand_mark.png` | ۶۰×۶۰ | 〃 |
| `drawable-xhdpi/brand_mark.png` | ۸۰×۸۰ | 〃 |
| `drawable-xxhdpi/brand_mark.png` | ۱۲۰×۱۲۰ | 〃 |
| `drawable-xxxhdpi/brand_mark.png` | ۱۶۰×۱۶۰ | 〃 |
| `drawable-nodpi/brand_icon_foreground.png` | ۵۱۲×۵۱۲، محتوا ۶۲٪ | پیش‌زمینهٔ آداپتیو (ناحیهٔ ایمن ۷۲/۱۰۸) |
| `drawable-nodpi/brand_icon_monochrome.png` | ۵۱۲×۵۱۲، سیلوئت سفید | آیکون تم‌پذیر اندروید ۱۳ |
| `drawable-nodpi/brand_splash_icon.png` | ۵۱۲×۵۱۲، محتوا ۵۸٪ | اسپلش اندروید ۱۲+ |

همهٔ خروجی‌ها **مربع** و وسط‌چین‌اند؛ برش بر اساس کانال آلفا انجام می‌شود، پس نشان بدون کشیدگی و بدون حاشیهٔ نامتوازن می‌نشیند.

### ب) آیکون لانچر
- `drawable/ic_launcher_background.xml` → گرادیان `#132042 → #07141E` (همان `favicon.svg` و آیکون سایت).
- `mipmap-anydpi/ic_launcher.xml` و `ic_launcher_round.xml` → `background` گرادیان + `foreground` نشان بدون قاب + `monochrome` مربع.
- فایل‌های قدیمی `app_icon_art.png`، `brand_monochrome.png` و `drawable-nodpi/brand_mark.png` حذف شدند (دارایی مرده و منبع باگ).

### ج) اسپلش
`values-v31/styles.xml`: `windowSplashScreenAnimatedIcon = @drawable/brand_splash_icon`،
`windowSplashScreenBackground` و `windowSplashScreenIconBackgroundColor` = `@color/brand_navy_950`.
`values/styles.xml` هم `windowBackground` را به همان رنگ توکنی برد (قبلاً رشتهٔ هاردکد `#07141E` بود).

### د) کد
```kotlin
@Composable fun BrandEmblem(size:Dp=40.dp,contentDescription:String?=null,modifier:Modifier=Modifier)
@Composable fun BrandLockup(size:String="md",darkText:Boolean=false,subtitle:String="اول حقت را بسنج، بعد قدم بردار")
```
- اندازه پارامتر شد و `ContentScale.Fit` اضافه شد (L-02).
- `contentDescription` پیش‌فرض `null` = تزئینی، مطابق `aria-hidden` سایت (L-05).
- `BrandLockup` دقیقاً ساختار `BrandMark.jsx` سایت را می‌آورد (نشان + «حق‌سنج» + شعار برنجی ۱۱sp) و در **صفحهٔ ورود** و **سایدبار** جای نسخهٔ دست‌ساز قبلی را گرفت.

---

## ۳) شاهد

```bash
python3 tools/make-brand-assets.py        # جدول ابعاد/حجم/alpha_bbox را چاپ می‌کند
python3 tools/make-brand-preview.py       # docs/brand-preview.html را می‌سازد
```

خروجی اجرای واقعی (۱۶ آبان):

```
app/src/main/res/drawable-mdpi/brand_mark.png               40x40     3044 B  alpha_bbox=(0, 0, 39, 40)
app/src/main/res/drawable-hdpi/brand_mark.png               60x60     5340 B  alpha_bbox=(1, 0, 59, 60)
app/src/main/res/drawable-xhdpi/brand_mark.png              80x80     8172 B  alpha_bbox=(1, 0, 79, 80)
app/src/main/res/drawable-xxhdpi/brand_mark.png            120x120   14191 B  alpha_bbox=(2, 0, 118, 120)
app/src/main/res/drawable-xxxhdpi/brand_mark.png           160x160   21257 B  alpha_bbox=(2, 0, 157, 160)
app/src/main/res/drawable-nodpi/brand_icon_foreground.png  512x512   62142 B  alpha_bbox=(102, 97, 409, 414)
app/src/main/res/drawable-nodpi/brand_icon_monochrome.png  512x512   16108 B  alpha_bbox=(102, 97, 409, 414)
app/src/main/res/drawable-nodpi/brand_splash_icon.png      512x512   56621 B  alpha_bbox=(112, 107, 400, 404)
```

- همهٔ فایل‌ها مربع‌اند (عرض = ارتفاع) — ایراد L-01 از بین رفت.
- `alpha_bbox` پیش‌زمینه «۱۰۲…۴۰۹» در بوم ۵۱۲ یعنی محتوا حدود ۶۰٪ بوم است: داخل ناحیهٔ ایمن آداپتیو و اسپلش.
- شبیه‌سازی ماسک دایره‌ای/اسکوئرکل با همین فایل‌ها انجام شد و نشان در هیچ ماسکی بریده نمی‌شود (تصویر ۲ و ۳ در `brand-preview.html`).

---

## ۴) آنچه تأیید نشده

- **رندر واقعی روی دستگاه/امولاتور**: در این محیط `aapt2` و پلاگین Compose نیست، پس `./gradlew assembleDebug` اجرا نشده است.
  درستی *مسیرها، نام منابع و امضاهای Compose* با `verify/typecheck.sh` روی `android.jar 35` بررسی شده، ولی
  ظاهر آیکون در لانچر واقعی (ماسک‌های مختلف OEM) دیده نشده است.
- آیکون **اعلان** (`ic_notification.xml`) عمداً همان ترازوی برداری مانده است: نشان برند در ۲۴dp شلوغ و ناخوانا می‌شود
  و آیکون نوار وضعیت باید تک‌رنگ و ساده باشد. این انتخاب آگاهانه است، نه جامانده.
