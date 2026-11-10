# گزارش ممیزی و اصلاح سورس اندروید بومی «حق‌سنج»

نسخهٔ ورودی: `2.0.0-native` (کد ۳) — `hsai_native_final_source.zip`
نسخهٔ خروجی: **`2.0.1-native` (کد ۴)** — `hsai_native_fixed_source.zip`
تاریخ: ۱۴۰۵/۰۸/۱۲ (2026-11-03)

---

## ۱) خلاصهٔ اجرایی

کل سورس اندروید (۲۶ فایل Kotlin در `app/src/main`, ۷ کلاس تست, بدون WebView) ممیزی شد.
**۱۴ نقص واقعی پیدا و رفع شد**: ۴ باگ قرارداد API، ۲ باگ که می‌توانست اپ را با NPE ببندد،
۲ باگ ریسک حقوقی در نمایش نتیجه، ۳ نقص UI/UX و ۳ مورد کد/برچسبِ اضافی یا گمراه‌کننده.
**۴ هشدار مشکوک بررسی و رد شد** (کد سالم بود)، و یک تست خودِ کلاینت که رفتار غلط را «تأیید» می‌کرد بازنویسی شد.

مهم‌ترین یافته‌ها:
1. مسیر «تحلیل مدارک» در عمل **هیچ مهلتی تولید نمی‌کرد** (تاریخ ابلاغ هرگز ارسال نمی‌شد).
2. پاسخِ «برگه خوانده نشد» و «استناد تأییدنشده» دقیقاً مثل پاسخ سالم نمایش داده می‌شد — ریسک حقوقی، نه فقط ظاهر.
3. `JSONObject.objects()` با کلید آرایهٔ غایب **null** برمی‌گرداند در حالی که نوعش `List<JSONObject>` اعلام شده بود؛
   نوع پلتفرمیِ `JSONArray!` این را از کامپایلر پنهان می‌کرد و پاسخِ `{"ok":true,"data":{}}` اپ را با NPE می‌بست.
4. سند تولیدی، عنوان و شرح ماجرا را در هم می‌ریخت؛ شرح ماجرا هرگز به مدل نمی‌رسید.

### آنچه با شاهد اثبات شد (قابل تکرار)

| شاهد | نتیجه | فرمان |
|---|---|---|
| بررسی نحوی همهٔ ۳۳ فایل (۲۶ main + ۷ test) با `kotlinc 2.0.21` | **۰ خطای نحوی** | بخش ۵-الف |
| هارنس اجرایی (کد واقعیِ `ApiContract`, `JsonExt`, `UiPresentation`, `Domain`) | **۲۶/۲۶ assertion موفق** | `bash verify/run.sh` |
| تطبیق قرارداد با سرور زندهٔ `hsai.ir/api/v1` | `POST /calculators/compute` پاسخ `ok:true`؛ فیلدهای `deadlines`/`scanner`/`documents` با `server/routes/*.js` مطابق | بخش ۶ |
| تحلیل عمق آکولاد کل فایل‌ها | هیچ براکت اضافه/کم نیست | بخش ۵-ج |
| بازبینی نوع با android-35 + Compose 1.7.6 + Material3 1.3.1 | **۰ خطای نوع** روی کل ماژول | `bash verify/typecheck.sh` |
| برابری توکن‌های بصری با سایت | **۳۹/۳۹** | `python3 verify/tokens.py` |

### آنچه اثبات **نشد** (صادقانه)

- **APK ساخته نشد** و `./gradlew testDebugUnitTest`/`lintDebug` اجرا نشد: در این محیط `ANDROID_HOME` خالی است،
  Gradle/Android SDK نصب نیست و حافظهٔ ماشین (~۲GB) برای Gradle + Compose کافی نبود.
  پس **کامپایل کاملِ Compose و اجرای تست‌های Robolectric تأیید نشده است.**
- رفتار زمان‌اجرا روی دستگاه (Keystore، WorkManager، دوربین، اعلان) تست نشده است.

---

## ۲) باگ‌های رفع‌شده

### BUG-01 — تکرار مقصد «حساب کاربری» در ریل ناوبری (UI)
`WorkspaceShell.kt` · حلقهٔ `Screen.entries` در چیدمان RAIL همهٔ مقصدها را می‌کشید، درحالی‌که «حساب کاربری»
از قبل آیکون اختصاصی در `TopAppBar` دارد ⇒ دو دکمهٔ هم‌نام با یک مقصد.
**رفع:** `filterNot { it == Screen.ACCOUNT }`.

### BUG-02 — عنوان و شرح سند در هم ادغام می‌شدند و شرح به مدل نمی‌رسید (قرارداد API)
`ApiContract.documentBody` · کلاینت `subject` را `عنوان + "\n\nشرح ماجرا و خواسته:\n" + شرح` می‌ساخت و
`facts_summary` را **هرگز** نمی‌فرستاد. سرور (`server/routes/documents.js`) `subject` را **عنوان** برگه می‌کند
(`title = "${doc_type}: " + subject.slice(0,60)`) و متن واقعه را از `facts_summary` می‌خواند.
نتیجه: عنوان سند یک بلوک چندخطی بریده‌شده بود و شرح ماجرا در تولید سند گم می‌شد.
**رفع:** `title`/`subject` = عنوان خالص؛ `details`/`facts_summary` = شرح.

### BUG-03 — تست کلاینت رفتار غلط را «تأیید» می‌کرد
`ApiContractTest.productionDocumentRequiresSubjectAlias` انتظار داشت `subject` با `"عنوان\n"` شروع شود.
**رفع:** جایگزین با `productionDocumentSplitsTitleFromFacts` + تست‌های تازه.

### BUG-04 — اسکنر تاریخ ابلاغ را هرگز نمی‌فرستاد ⇒ هیچ مهلتی ساخته نمی‌شد
`ApiContract.scannerBody` · `AppViewModel.analyze` · `Screens.ScannerScreen` ·
`POST /scanner/analyze` فیلد `notice_date` را می‌پذیرد و بدون تاریخ معتبر `deadline.status = missing_notice_date`
برمی‌گرداند. کلاینت نه فیلدی برای تاریخ داشت و نه آن را می‌فرستاد ⇒ قابلیت «محاسبهٔ مهلت» عملاً بی‌اثر بود.
**رفع:** فیلد «تاریخ ابلاغ (میلادی)» فقط برای ابلاغیه + اعتبارسنجی `LocalDate.parse` با کلید `notice_date` + ارسال شرطی.

### BUG-05 — پاسخ `read_ok=false` مثل تحلیل سالم نمایش داده می‌شد
کارت نتیجه همیشه «نتیجه تحلیل» + هشدار عمومی نشان می‌داد، حتی وقتی سرور می‌گفت برگه خوانده نشده.
**رفع:** `ApiContract.scanUnreadable()` + کارت «این برگه خوانده نشد» با متن `plain_meaning` سرور.

### BUG-06 — `deadline.status = missing_notice_date` نادیده گرفته می‌شد
**رفع:** `ApiContract.scanNoticeDateMissing()` + راهنمای اقدام («تاریخ واقعی ابلاغ را وارد و دوباره تحلیل کنید»).

### BUG-07 — پاسخِ «تأییدنشده» در چت مثل پاسخ تأییدشده دیده می‌شد
`Screens.MessageCard` · قرارداد `verification` در نسخهٔ جدید سرور `verification_status` و `citation_issues[]` دارد،
اما کلاینت فقط «امتیاز اطمینان» را داخل بخش تاشو نشان می‌داد.
**رفع:** بنر همیشه‌پیدا «این پاسخ تأیید نشد» + فهرست استنادهای تأییدنشده (`reference` یا `citation`).

### BUG-08 — لرزش فهرست چت هنگام استریم پاسخ
کلید `LaunchedEffect` شامل `s.messages.lastOrNull()?.text` بود ⇒ با هر توکن تازه، `animateScrollToItem` از نو
اجرا می‌شد. **رفع:** کلیدها به `messages.size` و `posting` محدود شد.

### BUG-09 — دو «چیپ» ناوبری به‌شکل تب شکسته (UX)
`FilterChip(selected=false)` برای «اشتراک و پرداخت‌ها» و «تیکت‌های پشتیبانی» رندر می‌شد؛ کاربر فکر می‌کرد
تب انتخاب‌نشده است. **رفع:** `AssistChip`.

### BUG-10 — سرریز تصویر بین چت و اسکنر (UX/حریم خصوصی)
`image` یک جایگاه مشترک بود؛ تصویر انتخاب‌شده برای چت در فرم «تحلیل مدارک» هم ظاهر می‌شد.
**رفع:** در `navigate`، با خروج از `Screen.SCANNER` تصویر پاک می‌شود.

### BUG-11 — `objects()` در نبودِ کلید آرایه null برمی‌گرداند (**NPE**)
`JsonExt.kt` (منتقل‌شده از `AppViewModel.kt`) · نوع اعلام‌شده `List<JSONObject>` بود ولی زنجیرهٔ `?.let`
مقدار `null` می‌داد؛ نوع پلتفرمیِ `JSONArray!` خطای نوع را از کامپایلر پنهان می‌کرد.
هر پاسخی که کلید آرایه نداشت (`{"ok":true,"data":{}}`) در `.ifEmpty`/`.plus`/`.filter` اپ را می‌بست
(مسیرهای `page()`، فهرست تیکت‌ها، تاریخچه گفتگو، پیش‌نمایش مهلت).
**رفع:** `?: emptyList()` + تست‌های `JsonExtTest` و ۵ assertion در هارنس.

### BUG-12 — سه برچسب ورودی بدون واحد پول (UX)
`Domain.calculators` · «مبلغ دیه کامل سال جاری»، «قیمت خرید»، «قیمت فروش» واحد نداشتند، در حالی که همهٔ
محاسبات «تومان» است و کاربر ممکن بود ریال یا هزار تومان وارد کند.
**رفع:** افزودن «(تومان)» به هر سه برچسب (بقیهٔ برچسب‌های `_toman` از قبل واحد داشتند).

### BUG-13 — کد مرده در تم (بخش اضافی)
`Theme.kt` · `val Jade=SiteNavy` (فقط یک توکن سازگاری بی‌مصرف) و `@Composable fun AmbientBackground()`
(۳۶ خط، هیچ‌جا فراخوانی نمی‌شد). **رفع:** حذف هر دو + ۶ ایمپورت بی‌استفادهٔ ناشی از آن.

### BUG-14 — ۱۳ ایمپورت بی‌استفاده در ۸ فایل (بخش اضافی)
از جمله `androidx.compose.material.icons.outlined.*` در `Screens.kt` که به `material-icons-extended`
نیاز دارد ولی پروژه فقط `material-icons-core` را دارد (منبع سردرگمی و ریسک کامپایل آینده).
**رفع:** همه حذف شدند؛ اسکن مجدد: **۰ ایمپورت بی‌استفاده**.

---

## ۳) موارد مشکوکی که بررسی شد و **باگ نبود** (تصمیم آگاهانه)

1. **ارسال هدر `Authorization` روی مسیرهای عمومی** (`/config`, `/laws`, `/calculators/compute`):
   عمدی و درست است؛ سرور سهمیه/اعتبار کاربر لاگین‌شده را از توکن تشخیص می‌دهد و حذف آن می‌توانست
   سهمیهٔ کاربر را به «مهمان» تبدیل کند. توکن فقط به دامنهٔ خودی (`hsai.ir`) می‌رود و allow-list مسیرها دست‌نخورده است.
2. **پرانتز مشکوک بعد از `NativeApp` در `WorkspaceShell.kt`**: با تحلیل عمق آکولاد تأیید شد ساختار درست است
   و `WorkspaceMenu` در عمق صفر تعریف شده؛ هیچ براکت اضافه/کم نیست (هشدار اولیه نادرست بود).
3. **پنهان‌شدن فیلد `type` در نتایج ماشین‌حساب**: `UiPresentation.internal` آن را فیلتر می‌کند، اما نوع ابزار در عنوان
   صفحه هست و `CalculatorResults` صحت `type` را جدا بررسی می‌کند ⇒ حذف تکرار، نه حذف اطلاعات.
4. **پارامترهای ۱۴ ماشین‌حساب**: بدنهٔ **مسطح** و کلیدها با سرور تطبیق داده شد و با فراخوانی زنده تأیید شد
   (`expert_fee` → `claim_amount_toman`+`expert_kind`؛ `arbitration_fee` → `dispute_amount_toman`+`agreement_mode`+`agreed_fee_toman`؛ …).
5. **`calendar()`/`reminder()`** از قبل تاریخ را با `runCatching` اعتبارسنجی می‌کنند؛ نگرانی «کرش با تاریخ خالی» منتفی بود.

---

## ۴) تغییرات فایل‌به‌فایل

| فایل | تغییر |
|---|---|
| `app/src/main/java/ir/hsai/mobile/JsonExt.kt` | **فایل تازه**: `str`/`objects`/`safeLong` از `AppViewModel` جدا شد (بدون وابستگی اندروید، قابل آزمون) + رفع NPE |
| `.../AppViewModel.kt` | `analyze(..., noticeDate)` + اعتبارسنجی ISO؛ `navigate` تصویر را بین چت/اسکنر پاک می‌کند؛ سه افزونهٔ JSON به `JsonExt.kt` منتقل شد |
| `.../ApiContract.kt` | `documentBody` بازنویسی؛ `scannerBody(+noticeDate)`؛ `scanUnreadable`, `scanNoticeDateMissing`, `verificationFailed`, `citationIssues` |
| `.../Screens.kt` | فیلد تاریخ ابلاغ + راهنما؛ کارت «خوانده نشد» و راهنمای تاریخ گمشده؛ بنر «تأیید نشد»؛ اصلاح کلید اسکرول؛ `AssistChip`؛ حذف ۵ ایمپورت بی‌استفاده |
| `.../WorkspaceShell.kt` | حذف مقصد تکراری در ریل |
| `.../Theme.kt` | حذف `Jade` و `AmbientBackground` (کد مرده) + ۶ ایمپورت |
| `.../Domain.kt` | واحد «تومان» در سه برچسب ورودی |
| `.../LoginScreen.kt`, `MainActivity.kt`, `AdaptiveDetails.kt` | حذف ایمپورت‌های بی‌استفاده |
| `app/src/test/.../ApiContractTest.kt` | تست غلط جایگزین شد؛ ۳ تست تازه |
| `app/src/test/.../JsonExtTest.kt` | **فایل تازه**: ۴ تست برای `objects`/`str`/`safeLong` و `page()` |
| `app/build.gradle` | `versionCode 4` / `versionName 2.0.1-native` |
| `README.fa.md` | شمارهٔ نسخه + ارجاع به همین گزارش |
| `SOURCE-MANIFEST.sha256` | بازتولید شد |
| `docs/AUDIT-FIXES-v2.0.1-native.md` | همین گزارش |

**هیچ فایل قابلیتی حذف نشد**؛ بخش‌های اضافیِ واقعی همان کد مردهٔ تم، ایمپورت‌های بی‌استفاده و مقصد تکراری ناوبری بودند.

---

## ۵) بازتولید بررسی‌ها

### الف) بررسی نحوی بدون Android SDK

```bash
# JDK 17 + kotlinc 2.0.21
for f in app/src/main/java/ir/hsai/mobile/*.kt app/src/test/java/ir/hsai/mobile/*.kt; do
  kotlinc -nowarn -d /tmp/out "$f" 2>&1 | grep -Ei "expecting|unexpected token|syntax error"
done
# خروجی مورد انتظار: هیچ خطی. خطاهای «unresolved reference» طبیعی‌اند چون Android/Compose در classpath نیست.
```

### ب) هارنس اجرایی قرارداد (بدون اندروید)

```bash
cd verify && KOTLINC=/path/to/kotlinc bash run.sh
# خروجی مورد انتظار: pass=26 fail=0
```
`verify/` بیرون از درخت اپ است و در بستهٔ نهایی همراه شده (`Harness.kt`, `run.sh`, `vendor/json-20240303.jar`).
کامپایل می‌شود روی **کد واقعی**: `ApiContract.kt`, `JsonExt.kt`, `UiPresentation.kt`, `Domain.kt`.
فقط `ApiException` (از `Api.kt`) و `ChatMessage` (از `AppViewModel.kt`) آینه شده‌اند، چون بقیهٔ آن دو فایل به اندروید وابسته‌اند.

### ج) تحلیل عمق آکولاد (تشخیص براکت اضافه/کم)

```python
# برای هر فایل: شمارش { و } با نادیده‌گرفتن رشته‌ها/کامنت‌ها و چاپ عمق در محل تعریف توابع سطح‌بالا
# خروجی مورد انتظار: net = 0 برای همهٔ فایل‌ها و depth = 0 برای توابع سطح‌بالا
```

### د) تست‌های واحد پروژه (نیازمند محیط دارای SDK)

```bash
./gradlew testDebugUnitTest      # ۷ کلاس تست، شامل ۴ تست تازه در JsonExtTest و ۳ در ApiContractTest
./gradlew lintDebug assembleDebug
```

---

## ۶) تطبیق زندهٔ قرارداد (دادهٔ آزمایشی، بدون اعتبارنامه)

```
POST https://hsai.ir/api/v1/calculators/compute
{"type":"arbitration_fee","dispute_amount_toman":800000000,"agreement_mode":"agreed","agreed_fee_toman":50000000}
→ {"ok":true,"data":{"type":"arbitration_fee","payable_arbitration_fee_toman":50000000,"each_party_share_toman":25000000,…}}
```
هیچ توکن، شمارهٔ تماس یا دادهٔ پروفایل واقعی در تهیهٔ این گزارش استفاده نشده است؛ مقادیر نمونه ساختگی‌اند.

---

## ۷) بازطراحی بصری مطابق سایت (درخواست «UI اپ را دقیقاً مثل سایت کن»)

لایهٔ بصری از پایه بازنویسی شد تا با زبان طراحی سایت یکی شود: پالت کامل navy/brass،
سطوح `--soft-*` سایت، گردی‌های ۱۲/۱۶/۲۴، تایپوگرافی سیال Vazirmatn با نسبت خط ۱.۸۵،
کارت خمیری، حباب‌های چت با گوشهٔ ۹dp، نوار بالای کلای‌بار، ورودی چاهی و دکمهٔ سرمه‌ای/برنجی.
نگاشت کامل کلاس‌های CSS به Compose در `docs/UI-PARITY-SITE.md` آمده است.

فایل‌های بازنویسی‌شده: `Theme.kt` (پالت، سطوح، `SiteShape`، تایپوگرافی، `GlassCard`)،
`SiteDesign.kt` (شیشه، سربرگ، تب، برگهٔ کاغذی قضایی)، `Screens.kt` (ورودی/دکمه/حباب/کارت‌ها)،
`LoginScreen.kt`، `WorkspaceShell.kt` و `MainActivity.kt` (نوار وضعیت).

شاهد: `verify/tokens.py` (۳۹/۳۹ برابری توکن) و `verify/typecheck.sh` (۰ خطای نوع با
`android.jar` ۳۵ و Compose/Material3 واقعی؛ کدبایت نهایی همچنان نیازمند Gradle است).

## ۸) کارهای باقی‌مانده (پیشنهاد، انجام‌نشده)

1. اجرای `./gradlew testDebugUnitTest lintDebug assembleDebug` روی ماشین دارای Android SDK 35 (شرط قطعی پذیرش).
2. تست دستی روی دستگاه: ثبت یادآور (WorkManager + Doze)، بازخوانی نشست از Keystore پس از ری‌استارت، دوربین/گالری، اعلان اندروید ۱۳+.
3. اگر سرور تاریخ شمسی هم بپذیرد، ورودی تاریخ شمسی برای «تاریخ ابلاغ» ترجیح دارد (فعلاً میلادی + پیام صریح).
4. `integration/history-v53.patch` همچنان اعمال‌نشده و خارج از دامنهٔ این ممیزی است.
