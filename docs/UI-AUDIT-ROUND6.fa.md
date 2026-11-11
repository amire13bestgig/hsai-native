# ممیزی جامع UI اپ اندروید در برابر وب‌سایت — دور ششم

**تاریخ:** ۱۴۰۴/۰۸/۱۲ (2026-11-03) · **دامنه:** ۱۲ صفحه/دیالوگ اپ × ۷ محور کیفیت · **نتیجه:** ۲۶ اصلاح اعمال و با آزمون اثبات شد.
**وضعیت تأیید:** استایل، آیکون و ساختار در سطح **سورس و حل نوع** تأیید شده‌اند؛ رندر واقعی Compose و APK **تأییدنشده** است (بخش «۹»).

---

## ۱) روش ممیزی

هر صفحه و هر جزء در سه لایه بررسی شد:

1. **استایل:** کلاس‌های واقعی سایت استخراج شد و مقادیر رنگ/شعاع/سایه/پدینگ آن‌ها با کد اپ مقایسه شد.
2. **آیکون:** فهرست `lucide-react` هر صفحهٔ سایت با `NativeIcons` اپ و با آیکونِ هر کارت/دکمه تطبیق داده شد.
3. **بقیهٔ موارد:** حالت تیره، RTL و اعداد فارسی، دسترس‌پذیری (TalkBack، heading، نقش‌ها، وضعیت خطا)، حالت‌های خالی/خطا/بارگذاری، و حد لمسی ≥ ۴۴dp.

### قاعدهٔ «منبع مرجع استایل»
معیار برابری، کلاس‌هایی است که در JSX سایت **واقعاً به‌کار رفته‌اند**، نه هر کلاسی که در `index.css` تعریف شده. شمارش خودکار روی `src/**/*.jsx` نشان داد فقط ۲۲ کلاس زنده وجود دارد؛ بقیه (`.soft-btn*`، `.soft-well`، `.soft-modal`، `.judicial-paper`، `.glass-pill`، …) **CSS مرده**‌اند و اپ اجازه دارد آن‌ها را تقلید نکند.

> ⚠️ همین قاعده یک **ادعای نادرست قدیمی** را آشکار کرد: `Field` اپ ادعا می‌کرد «ورودی سایت یک چاه `.soft-well` است». `.soft-well` در JSX سایت استفاده نمی‌شود؛ ورودی واقعی سایت `bg-white border-slate-300 + focus:ring-2 brass-400` است. اصلاح شد (ردیف ۲۲).

### ۲۲ کلاس زندهٔ سایت و معادل اپ

| کلاس سایت | تعداد کاربرد | معادل اپ | وضعیت |
|---|---|---|---|
| `hsai-glass` | ۸۲ | `GlassCard(glass=true)` / `SiteGlass()` | ✅ (دور ششم: حالت `glass` اضافه شد) |
| `soft-card` | ۴۳ | `GlassCard()` پیش‌فرض | ✅ |
| `bg-legal-grid` | ۱۳ | `LegalGrid()` | ✅ |
| `hsai-dark` | ۶ | `LocalSiteDark` + `HsaiTheme(dark)` | ✅ |
| `hsai-scroll` / `hsai-tap` | ۴ / ۴ | اسکرول پیش‌فرض / حد لمسی ۴۴dp | ✅ (دور ششم: ۴۰dp→۴۴dp) |
| `ui-minimal` / `ui-canvas` | ۴ / ۱ | `Page()` (عرض ۱۱۵۲dp) | ✅ |
| `hsai-app-shell` / `hsai-min-shell` | ۳ / ۲ | `workspaceLayout()` + `Scaffold` | ✅ |
| `glass-panel` / `glass-bar` / `hero-command-glow` | ۲ / ۱ / ۲ | `clayBarBrush()` / `SiteHero` | ✅ |
| `hsai-glass-soft` | ۲ | `SiteGlass(soft=true)` + `SiteChip` | ✅ (دور ششم) |
| `hsai-glass-strong` | ۱ | `SiteGlass(strong=true)` | ✅ |
| `hsai-safe-top` / `-bottom` / `hsai-skip-link` | ۲ / ۱ / ۱ | `statusBarsPadding()` / `navigationBarsPadding()` / semantics | ✅ |
| `text-gold-shimmer` | ۲ | `GoldShimmerText()` | ✅ (دور ششم) |
| `skeuo-key` / `skeuo-paper` / `soft-bubble-user` / `soft-bubble-ai` | ۱/۱/۱/۱ | `SoftButton` / `DocumentPaper` / `bubbleUser()` / `bubbleAi()` | ✅ |

---

## ۲) ماتریس صفحه‌به‌صفحه

| صفحه | آیکون (سایت → اپ) | تب/فهرست | حالت خالی | بارگذاری | تیره | RTL/اعداد | لمسی |
|---|---|---|---|---|---|---|---|
| گفتگو (`CHAT`) | `MessageSquare` → `ChatBubbleOutline` | ۶ کارت موضوع با آیکون `Wallet/Home/Heart/Briefcase/AlertTriangle/Users` ✅ **اصلاح** | `History` ✅ | اسکلت تپنده ✅ **اصلاح** | ✅ | ✅ `fa()` | ۴۶dp ارسال/۴۸dp پیوست |
| تاریخچه | `History` → `History` | — | `History` ✅ **اصلاح** | ✅ | ✅ | ✅ `dateFa` | ✅ |
| اسناد | `FileSignature` → `FileSignature` ✅ **اصلاح** | — | `FileSignature` ✅ | ✅ | ✅ | ✅ | ✅ |
| اسکن ابلاغیه | `ScanSearch` → `ScanSearch` ✅ **اصلاح** | — | متن ساده | ✅ | ✅ | ✅ | ✅ |
| بانک قوانین | `BookOpen` → `MenuBook` | ۴ تب با آیکون `Search/GitCompare/HelpCircle/Library` ✅ **اصلاح** | `Search` ✅ | ✅ | ✅ | ✅ | ۴۴dp تب ✅ **اصلاح** |
| ماشین‌حساب | `Calculator` → `Calculate` | — | — | ✅ | ✅ | ✅ اعداد فارسی | ✅ |
| مهلت‌ها | `CalendarClock` → `CalendarClock` ✅ **اصلاح** | — | `CalendarClock` ✅ | ✅ | ✅ | ✅ تاریخ شمسی | ✅ |
| تعرفه‌ها | `Crown` → `Crown` ✅ **اصلاح** | ۲ تب ماهانه/سالانه | `Crown` ✅ | ✅ | ✅ | ✅ | ✅ |
| پشتیبانی | `LifeBuoy` → `LifeBuoy` ✅ **اصلاح** | — | `LifeBuoy` ✅ | ✅ | ✅ | ✅ | ✅ |
| حساب کاربری | `User` → `PersonOutline` | تب‌های پروفایل | — | ✅ | ✅ | ایمیل/تلفن LTR+mono ✅ **اصلاح** | ۴۴dp خروج ✅ |
| ورود/عضویت | `Google` (SVG چهاررنگ) | تب پیامکی/رمز | — | اسپینر دکمه | روشنِ ثابت (مثل سایت) | ✅ | ۴۴dp «فراموشی رمز» ✅ |
| ۱۱ دیالوگ | — | — | — | — | ✅ | ✅ | سربرگ navy-900 سایت ✅ **اصلاح** |

### محورهای عرضی (همهٔ صفحات)
- **حالت خطا:** `ErrorNotice` + دکمهٔ «تلاش دوباره» (سراسری) ✅
- **حالت خالی:** `EmptyState(title, text, icon)` — آیکون هر صفحه اختصاصی شد ✅ **اصلاح**
- **بارگذاری:** `Loading()` = اسکلت تپنده (معادل `animate-pulse` سایت) به‌جای اسپینر M3 ✅ **اصلاح**
- **دسترس‌پذیری:** `heading()` روی تیتر صفحات/هیرو/دیالوگ، `stateDescription` روی خطای فیلد، `role=DropdownList` روی انتخابگر، `role=Button` روی لینک سایت ✅ **اصلاح**
- **حالت تیره:** همهٔ سطوح تازه از `LocalSiteDark`/توکن‌های Soft استفاده می‌کنند؛ هیچ رنگ ثابتِ روشن باقی نمانده (به‌جز صفحهٔ ورود که در سایت هم همیشه تیره-سرمه‌ای است).

---

## ۳) آیکون‌ها

`NativeIcons` از ۴۵ به **۶۴** آیکون رسید؛ همه با همان قرارداد قبلی (viewport ۲۴، خطی، `strokeLineWidth=1.65f`، `fill=null`) تا وزن بصری یکدست بماند.

| آیکون تازه | جای استفاده (سایت) | جای استفاده (اپ) |
|---|---|---|
| `Wallet` / `Home` / `Heart` / `Briefcase` / `AlertTriangle` / `Users` | `LIFE_SITUATION_CARDS` در `ChatAgentPage.jsx` | ۶ کارت موضوعی صفحهٔ گفتگو |
| `Search` / `GitCompare` / `HelpCircle` / `Library` | تب‌های جستجو/مقایسه/واژه‌نامه/مجموعه‌ها | تب‌های بانک قوانین + حالت خالی جستجو |
| `CalendarClock` | فهرست «مهلت‌ها» در `PublicLayout.jsx` | آیکون صفحه و حالت خالی مهلت‌ها |
| `Crown` | «تعرفه‌ها» در `PublicLayout.jsx` + `AccountSettingsPage` | صفحهٔ تعرفه‌ها، کارت اشتراک سایدبار، دکمهٔ ارتقا |
| `LifeBuoy` | پشتیبانی در `ChatAgentPage` | صفحهٔ پشتیبانی و حالت خالی آن |
| `ScanSearch` / `FileSignature` | `LandingPage` (اسکن/برگه) | کارت‌های اقدام صفحهٔ گفتگو + آیکون صفحه‌ها |
| `Download` / `CreditCard` / `CheckCircle` / `ShieldAlert` | دانلود، پرداخت، تأیید، هشدار | در دسترس برای اکشن‌های معادل |
| `Clock` / `Scale` / `AlertCircle` / `HeartHandshake` / `ShieldCheck` | `CitizenLegalCards.jsx` و کادرهای هشدار | کارت آرام‌سازی (`HeartHandshake`)، کارت محاسبه (`Scale`)، کادر هشدار گوگل (`AlertCircle`) |
| `GoogleMark` | `AuthPage.jsx` → `GoogleIcon` (SVG چهاررنگ) | دکمهٔ «ورود با حساب گوگل» |

**آیکون‌های حذف‌شده از جریان UI:** `WorkspacePremium`→`Crown`، `SupportAgent`→`LifeBuoy`، `EventAvailable`→`CalendarClock`، `PhotoCamera`→`ScanSearch`، `Balance`→`Scale`. (تعریفشان در فایل مانده تا هیچ ارجاع قدیمی نشکند، اما دیگر در UI استفاده نمی‌شوند.)

---

## ۴) ۲۶ اصلاح اعمال‌شده

| # | اصلاح | فایل |
|---|---|---|
| ۱ | `SiteGlass(soft=…)` + ثابت‌های `.hsai-glass-soft` (روشن `#F8F7F4/72%` + حاشیه `rgba(15,23,42,.07)`، تیره `white/5` + `white/10`) | `SiteDesign.kt` |
| ۲ | `SiteGlass(radiusDp=…)` برای کارت‌های ۱۶dp | `SiteDesign.kt` |
| ۳ | `SiteChip` = چیپ `hsai-glass-soft px-3 py-1.5 rounded-xl`؛ جای `SuggestionChip` M3 در چت و جستجوی سریع قوانین | `SiteDesign.kt`, `Screens.kt` |
| ۴ | `GoldShimmerText` + اعمال روی تیتر برنجی صفحهٔ ورود (`.text-gold-shimmer`) | `SiteDesign.kt` |
| ۵ | تب‌های سایت با آیکون (`SiteTabs(icon=…)`) | `SiteDesign.kt`, `Screens.kt` |
| ۶ | `GlassCard(glass, pad, radiusDp)` — دو کلاس کارت سایت در یک پریمیتیو | `Theme.kt` |
| ۷ | کارت‌های موضوعی = `hsai-glass p-3.5 rounded-2xl` سایت (پدینگ ۱۴، شعاع ۱۶، آیکون ۱۴dp، متن `font-black navy-950/slate-50`) | `Screens.kt` |
| ۸ | ۲۵ آیکون تازه + نشان رنگی گوگل | `NativeIcons.kt` |
| ۹ | `symbol()` پنج صفحه با آیکون آیتم متناظر فهرست سایت | `Screens.kt` |
| ۱۰ | `EmptyState(icon=…)` + آیکون اختصاصی برای ۷ حالت خالی | `Screens.kt` |
| ۱۱ | `Loading()` = اسکلت تپنده (بدون وابستگی به animation-core) | `Screens.kt` |
| ۱۲ | `DangerAction` = `bg-rose-600 text-white font-extrabold` برای تأیید حذف | `Screens.kt` |
| ۱۳ | `SoftDangerButton` = `bg-rose-50 border-rose-200 text-rose-700` برای خروج | `Screens.kt` |
| ۱۴ | سربرگ دیالوگ‌ها = نوار `navy-900` تمام‌عرض + تیتر سفید + بستن برنجی؛ شعاع `rounded-3xl` | `Screens.kt` |
| ۱۵ | اسکرول چت: رفع مسابقهٔ `totalItemsCount` (پرش به ابتدای فهرست) | `Screens.kt` |
| ۱۶ | دکمه‌های نوار بالا ۴۰dp→۴۴dp + رنگ‌های سایت (`bg-slate-100`/`white/10`)؛ کلید تم با حاشیهٔ `slate-200`/`white/15` و آیکون `navy-700`/`brass-300` | `WorkspaceShell.kt` |
| ۱۷ | لینک «مشاهده وب‌سایت اصلی»: حد لمسی ۴۴dp + `role=Button` | `WorkspaceShell.kt` |
| ۱۸ | آیکون کارت اشتراک سایدبار → `Crown` | `WorkspaceShell.kt` |
| ۱۹ | ایمیل/تلفن حساب با `TextDirection.Ltr` + `FontFamily.Monospace` (مثل `dir="ltr" font-mono` سایت) | `Screens.kt` |
| ۲۰ | دکمهٔ ارتقا با آیکون `Crown` ۱۶dp | `Screens.kt` |
| ۲۱ | دکمهٔ گوگل با نشان چهاررنگ + کادر هشدار `amber-50/amber-200/amber-900` و `AlertCircle` | `LoginScreen.kt` |
| ۲۲ | **ورودی‌ها** عیناً مثل سایت: `bg-white` / تیره `#10263A`، حاشیهٔ `slate-300` / `white/10`، فوکوس `brass-400` (۲dp)، وزن `bold` برای تک‌خطی | `Screens.kt`, `LoginScreen.kt` |
| ۲۳ | برچسب `Choice` هم‌سبک برچسب `Field` + `role=DropdownList` | `Screens.kt` |
| ۲۴ | خطای فیلد به‌صورت `stateDescription` به TalkBack | `Screens.kt` |
| ۲۵ | «فراموشی رمز عبور؟» حد لمسی ۴۴dp | `LoginScreen.kt` |
| ۲۶ | کارت‌های هوشمند: `HeartHandshake` برای آرام‌سازی، `Scale` برای محاسبهٔ قانونی | `IntelligenceCards.kt` |

---

## ۵) مواردی که عمداً تغییر نکرد

- **کلاس‌های مردهٔ CSS سایت** (`.soft-btn*`, `.soft-well`, `.soft-card-lg/-flat`, `.soft-tile`, `.soft-chip`, `.soft-modal/input/select/textarea`, `.soft-raised`, `.soft-clay-bar`, `.soft-divider`, `.soft-orb`, `.judicial-paper`, `.glass-pill`, `hsai-glass-dark/on-dark`) — در JSX سایت استفاده نمی‌شوند؛ تقلیدشان «برابری» نیست، افزودن استایل بی‌مرجع است.
- **صفحه‌های بیرون از دامنهٔ اپ** (تعیین‌شده در دور پنجم): `admin/*`، مستندات API، لندینگ/راهنمای شهروند، Command Palette، چاپ مرورگری.
- **انحراف آگاهانه ۱:** کلیک روی چیپ پیشنهاد در سایت پیام را **می‌فرستد**؛ در اپ فقط پیش‌نویس را پر می‌کند (در موبایل امکان ویرایش قبل از ارسال و بی‌هزینه‌تر است). یکسان‌سازی «حالت انتخاب‌شده» حفظ شد.
- **انحراف آگاهانه ۲:** `text-gold-shimmer` در سایت انیمیشن دارد؛ در اپ گرادیان **ثابت** است چون خود سایت روی موبایل و `prefers-reduced-motion` انیمیشن را خاموش می‌کند.
- **انحراف آگاهانه ۳:** `backdrop-blur` سایت در Compose معادل مستقیم ندارد؛ `SiteGlass` با گرادیان نیمه‌شفاف + سایه + هایلایت داخلی همان لایه‌بندی را می‌سازد.

---

## ۶) محورهایی که بازبینی شد و **درست** بود (بدون تغییر)

- حباب‌ها: `.soft-bubble-user` (`#1B3852→#0C1F2E`, ۲۲/۹dp) و `.soft-bubble-ai` (کارت + گرادیان `card2`).
- نوار بالای سایت: `clayBarBrush()` + هایلایت ۱٫۵dp + هایرلاین ۱dp.
- گرادیان هیرو `navy-900 → navy-950`.
- پالت کامل navy/brass و توکن‌های `--soft-*` (روشن/تیره).
- رنگ‌های سنجهٔ قدرت رمز: `rose-500/amber-500/yellow-500/emerald-500/emerald-600` — دقیقاً همان آرایهٔ `AuthPage.jsx:39`.
- `Action` (navy-950/brass-300 در روشن، brass-400/navy-950 در تیره)، `SoftButton`، `GhostAction`، `Pager`، `ErrorNotice`.
- RTL سراسری (`LocalLayoutDirection = Rtl`) و اعداد فارسی (`fa()`, `dateFa()` با تقویم فارسی).

---

## ۷) شواهد آزمون (همه تکرارپذیر)

| آزمون | فرمان | نتیجه |
|---|---|---|
| حل نوع کل ماژول | `bash verify/typecheck.sh` | **ok** (۰ خطای نوع) |
| هارنس قرارداد | `bash verify/run.sh` | **pass=104 fail=0** |
| برابری توکن/استایل/آیکون | `python3 verify/tokens.py` | **117/117** (از ۳۹ به ۱۱۷) |
| دروازهٔ سرور | `node verify/server-gateway.mjs` | **pass=12 fail=0** |
| دست‌دهی گوگل | `node verify/server-handoff.mjs` | **pass=11 fail=0** |
| مانیفست سورس | `sha256sum -c SOURCE-MANIFEST.sha256` | **102/102** |
| بازآزمون از داخل zip | استخراج در `/tmp/vfy3` و اجرای سه آزمون بالا | typecheck ok · run 104/0 · tokens 117/117 · manifest 102/102 |

بستهٔ تحویل: `native/hsai_native_fixed_source.zip` (۱۶۶ قلم). چون نوشتن هش داخل سندی که خودش
داخل همان آرشیو است، هش را بی‌نهایت تغییر می‌دهد، مقادیر دقیق فقط در دو فایل کنار آرشیو ثبت شده‌اند:
`native/hsai_native_fixed_source.zip.md5` و `native/hsai_native_fixed_source.zip.sha256`.

افزوده‌های `tokens.py` در این دور (۷۸ سنجهٔ تازه): حضور ۲۴ آیکون سایت، چهار رنگ نشان گوگل، چهار ثابت `.hsai-glass-soft`، وجود `SiteChip` و حذف `SuggestionChip`، حالت `glass` کارت‌ها، ترتیب آیکون ۶ کارت موضوعی، پنج نگاشت `symbol()`، رنگ‌های دکمهٔ خطر/خروج، حد لمسی ۴۴dp و حذف `size(40.dp)`، سربرگ دیالوگ، اسکلت بارگذاری، پنج قاعدهٔ دسترس‌پذیری، گرادیان تیتر برنجی، LTR بودن ایمیل/تلفن، آیکون کارت‌های هوشمند، و ۸ سنجهٔ استایل ورودی.

---

## ۸) آنچه **بدون SDK/APK تأییدنشده** ماند

1. **رندر واقعی Compose** — هیچ‌کدام از این تغییرات روی دستگاه/امیولاتور دیده نشده است. آنچه اثبات شده: کد کامپایل می‌شود (حل نوع)، مقادیر دقیقاً از سورس سایت گرفته شده‌اند (tokens)، و ساختار درست است.
2. **`backdrop-blur`** — در Compose تقریب زده می‌شود؛ ظاهر نهایی باید روی دستگاه دیده شود.
3. **اندازه‌گیری حد لمسی** — ۴۴dp از `.hsai-tap` سایت و از `heightIn` کد استخراج شده، نه از اندازه‌گیری روی دستگاه.
4. **TalkBack** — semantics اضافه شده، اما ترتیب اعلان‌ها روی دستگاه آزموده نشده است.
5. **جریان واقعی گوگل** — قرارداد بومی و آزمون سرور تأیید شده؛ ورود واقعی به سرویس گوگل نیازمند APK امضاشده و Client ID است.
6. **`ReminderMigration.kt`** در `typecheck.sh` کامپایل نمی‌شود (نیازمند guava)؛ در این دور تغییری نداشت.

---

## ۹) بازتولید

```bash
cd /home/user/native
export DEPS=/home/user/.cache/tc/cp
export SDK_JAR=$DEPS/sdk/android-35/android.jar
export KOTLINC=/home/user/.cache/tc/kotlinc/bin/kotlinc
export JAVA_HOME=/home/user/.cache/tc/jdk-17.0.21+9
export PATH=$JAVA_HOME/bin:$PATH
printf '1:1\n' >/dev/null

bash verify/typecheck.sh
STDLIB=/home/user/.cache/tc/kotlinc/lib/kotlin-stdlib.jar bash verify/run.sh
python3 verify/tokens.py
```

پچ‌های idempotent این دور (اجرای دوباره بی‌اثر است):

```bash
python3 docs/patches/apply-ui-audit-primitives.py
python3 docs/patches/apply-ui-audit-screens.py
python3 docs/patches/apply-ui-audit-fixups.py
```
