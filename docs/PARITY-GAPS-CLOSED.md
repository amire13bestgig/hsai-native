# بستن شکاف‌های اپ بومی نسبت به وب‌سایت — دور پنجم

> این سند پاسخ عملی به پرسش «الان اپ از تمام قابلیت‌های وب‌سایت استفاده می‌کند؟» است.
> هر ادعا با یک آزمون تکرارپذیر در `verify/` پشتیبانی می‌شود. هرچه تأیید نشد، در بخش
> «محدودیت‌های صریح» نام برده شده است.

## ۱. خلاصهٔ اجرایی

پیش از این دور، اپ **۷ قابلیت واقعی** کمتر از وب‌سایت داشت. همهٔ آن‌ها بسته شد:

| # | قابلیت وب | وضعیت قبل | وضعیت بعد | مسیر تأییدشده در v1 |
|---|---|---|---|---|
| ۱ | روابط مواد قانونی (قوانین و آراء مرتبط) | ❌ هیچ | ✅ پنجرهٔ «قوانین و آراء مرتبط» روی هر ماده | `GET /api/v1/laws/:id/relations` |
| ۲ | تب «قانون قدیم و جدید» (مقایسهٔ تطبیقی) | ✅ از قبل وصل بود¹ | ✅ تغییری لازم نبود | `GET /api/v1/laws/comparisons` |
| ۳ | لغو / بازگرداندن تمدید خودکار | ❌ | ✅ در کارت اشتراک + پنجرهٔ صورت‌حساب | `POST /api/v1/billing/{cancel,resume}` |
| ۴ | صورت‌حساب، سهمیه و تاریخچهٔ اشتراک | ❌ | ✅ دکمهٔ «صورت‌حساب و تاریخچه» | `GET /api/v1/billing/my-billing` |
| ۵ | تأیید ایمیل | ❌ | ✅ بنر «ایمیل تأیید نشده» + پنجرهٔ کد | `POST /api/v1/auth/email/{send-verification,verify}` |
| ۶ | بازیابی رمز عبور (بومی) | 🟡 فقط باز کردن مرورگر | ✅ کامل درون‌اپ: درخواست کد + تعیین رمز جدید | `POST /api/v1/auth/password/{forgot,reset}` |
| ۷ | فرم تماس / تیکت مهمان | ❌ | ✅ از صفحهٔ ورود و صفحهٔ حساب، بدون نیاز به ورود | `POST /api/v1/auth/contact` |
| ۸ | ورود با گوگل + اتصال حساب موجود | ❌ (صریحاً کنار گذاشته شده بود) | ✅ جریان بومی مرورگر→اپ با برگهٔ دست‌دهی | `POST /api/v1/auth/google/handoff` + `/auth/identities` |

¹ بازبینی دوبارهٔ کد نشان داد `LawComparison` در `Screens.kt` **وصل بود** و `lawTab`
مسیر `/laws/comparisons` را می‌گرفت. گزارش قبلی این مورد را اشتباه «وصل‌نشده» خوانده بود؛
این سند آن خطا را تصحیح می‌کند.

**آنچه عمداً در اپ نیست** (و نیستنش نقص نیست): پنل مدیریت (`/api/admin/*`)، صفحات
بازاریابی و محتوایی (لندینگ، درباره، قوانین، حریم خصوصی، راهنماها، سؤالات متداول،
مستندات API)، Command Palette و چاپ مرورگری.

## ۲. تغییرات سرور (سورس سایت)

| فایل | تغییر |
|---|---|
| `server/services/appAuthHandoff.js` | **فایل نو.** برگهٔ دست‌دهی نشست: HMAC-SHA256، عمر ۱۸۰ ثانیه، مقید به nonce دستگاه، بدون جدول دیتابیس. |
| `server/middleware/auth.js` | `readUserToken()` — خواندن امضاشدهٔ شناسهٔ کاربر از JWT بدون کوئری (فقط برای جریان دست‌دهی). |
| `server/routes/auth.js` | `/google/start` پرچم `app_return`/`app_nonce` را در state امضاشده می‌گذارد؛ `/google/callback` هر ریدایرکت (موفق، خطا، نیاز به اتصال حساب) را به `hsai://auth/google?…` ترجمه می‌کند. |
| `server/routes/v1.js` | `POST /auth/google/handoff`، `GET /laws/:id/relations`، و **دروازهٔ سازگاری** برای مسیرهای قدیمی. |
| `server/openapiSpec.js` | ۱۰ مسیر تازه مستند شد (۵۱ مسیر در مجموع). |

### چرا «دروازهٔ سازگاری» و نه بازنویسی کد؟

مسیرهای `password/*`، `email/*`، `contact`، `phone/*` و `plans/{cancel,resume,my-billing}`
منطق امنیتی حساس دارند: محدودیت نرخ دولایه، توکن هش‌شده، ضد افشای وجود حساب
(anti-enumeration)، همگام‌سازی سهمیه. بازنویسی آن‌ها در `v1` یعنی دو نسخهٔ موازی که
به‌سرعت واگرا می‌شوند — همان اشتباهی که در دورهای قبل با `suggested_actions` گران تمام شد.

راه‌حل: همان روتر، پشت یک مبدل پوشش پاسخ سرو می‌شود:

```js
router.use('/auth', legacyEnvelope(authLegacyRouter));
router.use('/billing', legacyEnvelope(plansLegacyRouter));
```

`legacyEnvelope` فقط شکل پاسخ را عوض می‌کند: `{...}` → `{ok:true,data:{...}}` و
`{error}` → `{ok:false,error:{code,message}}`. مسیرهای صریح `v1` (مثل `/auth/login` و
`/billing/checkout`) **بالاتر** از این دو خط تعریف شده‌اند، پس هرگز به دروازه نمی‌رسند و
رفتارشان دست‌نخورده است. این نکته در آزمون `server-gateway` به‌صورت ماشینی بررسی می‌شود.

## ۳. جریان ورود با گوگل در اپ (بدون Google Sign-In SDK)

```
اپ                       مرورگر امن (Custom Tab)                سرور
 │ nonce می‌سازد (36 hex)                                          │
 │── OpenUrl(/api/auth/google/start?app_return=1&app_nonce=…) ──▶ │  state امضاشده
 │                                   کاربر با گوگل وارد می‌شود ──▶ │  تبادل کد با گوگل
 │◀── hsai://auth/google?handoff=…&nonce=… ─────────────────────── │  createAppHandoff()
 │── POST /api/v1/auth/google/handoff {handoff, nonce} ──────────▶ │  verifyAppHandoff()
 │◀── {token, user, subscription} ──────────────────────────────── │
```

**مدل تهدید و چرا امن است**

* برگهٔ دست‌دهی امضاشده (HMAC با `JWT_SECRET`) و کوتاه‌عمر (۱۸۰ ثانیه) است.
* برگه به `nonce` ای گره خورده که **خود اپ** ساخته و هرگز از دستگاه بیرون نمی‌رود.
  اگر برنامهٔ دیگری روی گوشی، deep link را برباید، بدون آن nonce نمی‌تواند برگه را نقد کند
  (آزمون `nonce دیگر (اپ مهاجم) رد می‌شود`).
* برگه در حافظهٔ فرایند نگه داشته می‌شود؛ اگر اپ kill شود، کاربر پیام روشن می‌گیرد و
  باید دوباره تلاش کند.
* حالت «ایمیل گوگل با حساب موجود هم‌نام است» به‌جای اتصال خودکار، به اپ برمی‌گردد و
  اثبات مالکیت (رمز یا کد یک‌بارمصرف) می‌خواهد — همان سیاست امن وب‌سایت.

**باقی‌ماندهٔ ریسک (صریح):** اگر مهاجم هم deep link را برباید و هم nonce را (یعنی کد اپ را
بخواند یا در همان فرایند اجرا شود)، حمله ممکن می‌شود. برای بستن کامل این راه، به
«App Links تأییدشده با دامنه» (`assetlinks.json` با اثرانگشت کلید امضا) نیاز است؛
امضای این پروژه در محیط ما در دسترس نبود (APK ساخته نشد)، پس این کار انجام نشد.

## ۴. تغییرات اپ (اندروید)

| فایل | تغییر |
|---|---|
| `ApiRoutes.kt` | ۱۴ قاعدهٔ مسیر تازه (relations، billing، password، email، contact، google/handoff، google/link، identities). |
| `ApiContract.kt` | خواننده‌های خالص و آزمون‌پذیر: `relations/relationTitle/relationType/relationNote`، `billing()`، `autoRenewCancelled()`، `emailVerified()`، `newHandoffNonce()`، `googleStartUrl()`، `parseHandoffLink()`، `identities()`، `contactTracking()`. |
| `AppViewModel.kt` | توابع: `articleRelations`، `billingSummary`، `cancelAutoRenew`، `resumeAutoRenew`، `sendEmailVerification`، `verifyEmail`، `requestPasswordReset`، `resetPassword`، `sendContact`، `startGoogleSignIn`، `completeGoogleSignIn`، `googleLinkWithPassword`، `googleLinkSendOtp`، `googleLinkWithOtp`، `loadIdentities`، `unlinkGoogle`. |
| `Screens.kt` | بنر تأیید ایمیل، کارت اشتراک با لغو/بازگرداندن تمدید، دکمه‌های صورت‌حساب و روش‌های ورود، و ۱۱ پنجرهٔ تازه (`relations، billing، cancelRenew، emailVerify، forgot، resetPassword، contact، identities، googleLink، googleLinkOtp، info`). |
| `AdaptiveDetails.kt` | دکمهٔ «قوانین و آراء مرتبط» روی متن هر ماده. |
| `LoginScreen.kt` | «فراموشی رمز عبور؟» دیگر مرورگر را باز نمی‌کند؛ پنجرهٔ بومی بازیابی است. دکمهٔ «ورود با حساب گوگل» (وقتی سرور اجازه دهد) و «پیام به پشتیبانی (بدون حساب)». |
| `MainActivity.kt` | پردازش `hsai://auth/google` در `handleIntent`. |
| `AndroidManifest.xml` | `intent-filter` تازه برای `hsai://auth/google`. |

قواعد هم‌ارزی که رعایت شد:

* رمز عبور و کد یک‌بارمصرف هرگز در `rememberSaveable`/دیسک نوشته نمی‌شوند.
* حداقل طول رمز جدید در اپ **۸** است (قاعدهٔ سرور)، نه ۶ که در تغییر رمز درون‌حساب است.
* هر پنجره تکلیفش را با پاسخ واقعی سرور مشخص می‌کند؛ هیچ پیام موفقیت ساختگی نیست.
* قطع اتصال گوگل اگر تنها روش ورود باشد، توسط سرور رد می‌شود و پیام فارسی‌اش نمایش داده می‌شود.

## ۵. شاهد ماشینی (دستورها و اعداد)

```bash
# ۱) قراردادهای کلاینت (Kotlin، بدون Android SDK)
KOTLINC=…/kotlinc JAVA_HOME=…/jdk-17 STDLIB=…/kotlin-stdlib.jar bash verify/run.sh
#   → pass=104 fail=0   (۵۰ بررسی دور چهارم + ۵۴ بررسی تازه)

# ۲) حل نوع‌های کل ماژول
bash verify/typecheck.sh          #   → ok (۰ خطای نوع)

# ۳) توکن‌های طراحی
python3 verify/tokens.py          #   → 39/39

# ۴) برگهٔ دست‌دهی و ترجمهٔ ریدایرکت گوگل
node verify/server-handoff.mjs    #   → pass=11 fail=0

# ۵) دروازهٔ v1 با درخواست HTTP واقعی (بدون دیتابیس)
node verify/server-gateway.mjs    #   → pass=12 fail=0
```

نمونه‌هایی از بررسی‌های تازه در `verify/Harness.kt`:

* `relation_title_fallback_composed` — اگر سرور عنوان مادهٔ مقصد را join نکند، عنوان از
  «نام قانون + شمارهٔ ماده» ساخته می‌شود (به‌جای متن خالی در UI).
* `handoff_rejects_other_scheme` / `handoff_rejects_other_host` — پیوند
  `https://evil.example/…` و `hsai://payment/…` هرگز به‌عنوان بازگشت ورود پذیرفته نمی‌شوند.
* `billing_keys_present` — پاسخ کوتاه سرور باعث NPE در UI نمی‌شود.
* `route_denies_wrong_method` — مسیر تازه با متد اشتباه همچنان بلوکه است.

## ۶. محدودیت‌های صریح (تأییدنشده)

1. **APK ساخته و اجرا نشد.** پلاگین Compose Gradle در این محیط در دسترس نیست، بنابراین
   `./gradlew assembleDebug` اجرا نشد. نتیجه: رندر واقعی پنجره‌های تازه، deep link روی
   دستگاه، و باز شدن Custom Tab **تأیید نشده‌اند**. تنها چیزی که تأیید شده، حل نوع
   (typecheck) و منطق قراردادهاست.
2. **آزمون انتها-به-انتها روی سرور واقعی اجرا نشد.** آزمون‌های سرور با `DATABASE_URL`
   خالی اجرا شدند؛ یعنی «مسیر mount شده و پوشش v1 درست است» تأیید شد، ولی رفتار
   دیتابیسی (مثلاً ثبت واقعی تیکت تماس) تأیید نشد.
3. **ورود با گوگل به پیکربندی مدیریت وابسته است.** بدون `GOOGLE_CLIENT_ID/SECRET` و
   روشن‌بودن کلید در پنل، دکمه در اپ نمایش داده نمی‌شود (کلید `google_sign_in_enabled`
   از `/api/v1/config`).
4. **تأیید ایمیل به درگاه ایمیل وابسته است.** اگر SMTP/Resend/Brevo تنظیم نشده باشد،
   سرور پیام فارسی «درگاه ایمیل تنظیم نشده» می‌دهد و اپ همان را نشان می‌دهد؛ هیچ
   موفقیت ساختگی نمایش داده نمی‌شود.
5. **بازیابی رمز پیامکی** به درگاه پیامک وابسته است؛ اگر ایمیل هم فعال نباشد، اپ پیام
   خطای سرور را نشان می‌دهد.
6. **App Links تأییدشده** انجام نشد (بخش ۳).
7. **پنل مدیریت، صفحات محتوایی و چاپ مرورگری** عمداً پیاده نشدند.

## ۷. چطور بازتولید کنید

```bash
SITE_ROOT=/home/user/src/hsai python3 docs/patches/apply-server-parity.py   # سورس سایت
APP_ROOT=/home/user/native/src/hsai-native python3 docs/patches/apply-app-contract.py
APP_ROOT=/home/user/native/src/hsai-native python3 docs/patches/apply-app-viewmodel.py
APP_ROOT=/home/user/native/src/hsai-native python3 docs/patches/apply-app-ui.py
```

سه اسکریپت اول idempotent هستند؛ اجرای دوباره چیزی را خراب نمی‌کند (`SKIP` می‌دهند).

## ۸. بستهٔ تحویل

| قلم | مقدار |
|---|---|
| فایل | `hsai_native_fixed_source.zip` |
| تعداد قلم در آرشیو (فایل + پوشه) | ۱۴۷ |
| حجم | حدود ۱٫۲۷ مگابایت |
| `SOURCE-MANIFEST.sha256` | ۱۰۱ خط، همهٔ هش‌ها تأییدشده (`sha256sum -c` = ۱۰۱ OK) |

هش‌های md5/sha256 آرشیو در فایل کنار آن (`hsai_native_fixed_source.zip.sha256`) آمده است
تا خود سند داخل آرشیو نیازی به بازنویسی هش نداشته باشد.

همهٔ آزمون‌های بخش ۵ **از داخل همین آرشیو** (پس از `unzip` در یک مسیر تازه) دوباره اجرا و
سبز شدند؛ یعنی بستهٔ تحویلی خودبسنده است. دو آزمون `server-*.mjs` به سورس سایت نیاز دارند
(`SITE_ROOT`) و همین‌طور در سند آرشیو هستند.
