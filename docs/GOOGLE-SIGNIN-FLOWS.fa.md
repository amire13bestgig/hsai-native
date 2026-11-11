# ورود با گوگل — دو جریان کاملاً جدا (وب‌سایت و اپ)

**تاریخ:** ۱۴۰۴/۰۸/۱۲ · **دامنه:** `server/routes/auth.js` · `server/routes/appGoogleAuth.js` · `server/services/appGoogleFlow.js` · `app/.../ApiContract.kt` · `AppViewModel.kt` · `SessionStore`

---

## ۱) خلاصهٔ تصمیم

| چه‌کسی | چه مسیری | نتیجه |
|---|---|---|
| **وب‌سایت** (مرورگر دسکتاپ/موبایل) | `/api/auth/google/start` → گوگل → `/api/auth/google/callback` | همان رفتار **اصلی**: ریدایرکت داخلی به `/auth#google_token=…` یا `/auth?google_error=…` / `/auth?google_link_token=…` |
| **اپ اندروید** | `{API}/api/auth/google/app/start?nonce=…` → گوگل → `/api/auth/google/app/callback` | `302` به `hsai://auth/google?handoff=…&nonce=…` ⇒ اندروید **خودکار اپ را باز می‌کند** |

جریان اپ هیچ شاخه‌ای در کد وب ندارد. هر تغییر مربوط به دور پنجم (`app_return`، `app_nonce`، `installAppRedirect`، `translateWebAuthRedirect`، `createAppHandoff` در `auth.js`) **حذف شد** و تنها تغییر باقی‌مانده در `auth.js` سه کلمهٔ `export` روی توابع موجود است (`getAuthModeSettings`، `signOAuthState`، `verifyOAuthState`، `resolveGoogleWebSignIn`) تا روتر اپ همان منطق را دوباره‌نویسی نکند — بدون هیچ تغییر رفتاری.

---

## ۲) جریان وب (حالت اصلی)

```
GET /api/auth/google/start
  → state = signOAuthState({ next, redirectUri, nonce, ts })      ← بدون هیچ پرچم اپ
  → 302 https://accounts.google.com/o/oauth2/v2/auth?…&redirect_uri=…/api/auth/google/callback
GET /api/auth/google/callback?code&state
  → تبادل code → userinfo → resolveGoogleSignIn
  → 302 /auth#google_token=…            (موفق)
  → 302 /auth?google_error=…            (خطا/لغو)
  → 302 /auth?google_link_token=…       (نیاز به اثبات مالکیت حساب موجود)
```

شاهد ماشینیِ «برگشتن به اصل»: `verify/server-handoff.mjs` متن `auth.js` را می‌خواند و نبودِ `app_return`، `app_nonce`، `installAppRedirect`، `translateWebAuthRedirect` و هر `hsai://` را تأیید می‌کند؛ `verify/server-app-google.mjs` هم با درخواست واقعی HTTP نشان می‌دهد `<api>/auth/google/start?app_return=1&app_nonce=…` هیچ اثری روی جریان وب ندارد (کلیدهای state و مقصد ریدایرکت یکسان می‌مانند).

---

## ۳) جریان اپ (مرورگر → بازگشت خودکار)

```
۱. کاربر «ورود با حساب گوگل» را در اپ می‌زند
   AppViewModel.startGoogleSignIn()
     • nonce = ۳۶ نویسهٔ هگز (۱۲ بایت تصادفی)
     • nonce در مخزن دستگاه ذخیره می‌شود: «nonce:timestamp» با عمر ۱۰ دقیقه
     • Intent.ACTION_VIEW → مرورگر دستگاه:
         https://hsai.ir/api/auth/google/app/start?nonce=<nonce>

۲. سرور (appGoogleAuth.js)
     • nonce را اعتبارسنجی می‌کند (۱۶–۶۴ نویسه، الفبای URL)
     • محدودیت نرخ ۲۵ درخواست / ۱۵ دقیقه برای هر IP
     • state = signOAuthState({ app:true, appNonce, next:'/app', redirectUri, nonce, ts })
     • redirect_uri = https://hsai.ir/api/auth/google/app/callback
     → 302 به صفحهٔ انتخاب حساب گوگل

۳. گوگل → /api/auth/google/app/callback?code&state
     • تبادل code با client_secret، سپس userinfo
     • resolveGoogleSignIn (همان منطق وب: اتصال خودکار فقط با ایمیل تأییدشده)
     • برگهٔ یک‌بارمصرف می‌سازد: createAppHandoff({ userId, nonce })  (عمر ۱۸۰ ثانیه، مقید به nonce)
     → 302 به hsai://auth/google?handoff=…&nonce=…
     حالت‌های دیگر:
     → hsai://auth/google?error=link_required&link_token=…&email=…&has_password=…
     → hsai://auth/google?error=<پیام فارسی>

۴. اندروید deep link را به اپ می‌دهد
   AndroidManifest: scheme=hsai host=auth pathPrefix=/google، launchMode=singleTask
   MainActivity.onNewIntent → handleIntent → completeGoogleSignIn(link)

۵. اپ برگه را نقد می‌کند
   • غیرفعال‌کردن حالت‌های حمله: تطبیق nonce با nonce ذخیره‌شده (و TTL)
   • POST /api/v1/auth/google/handoff { handoff, nonce }  →  توکن نشست
   • پاک‌سازی nonce و ورود به میز کار
```

نکتهٔ تاب‌آوری: اگر اندروید فرایند اپ را در حین ورود در مرورگر بکشد، nonce از مخزن خوانده می‌شود (`ApiContract.decodeOauthNonce`، عمر ۱۰ دقیقه، ردِ timestamp آینده) و ورود همچنان کامل می‌شود.

### گیت نمایش دکمه
`/api/v1/auth/config` حالا دو کلید تازه دارد:

```json
{ "google_redirect_enabled": true, "google_app_signin_path": "/api/auth/google/app/start" }
```

- `google_redirect_enabled` فقط وقتی `true` است که هم `auth_google_enabled` روشن باشد و هم **client_id و client_secret** موجود باشند؛ دکمهٔ اپ بر همین اساس نمایش داده می‌شود (`ApiContract.googleAppSignInEnabled`).
- `google_app_signin_path` مسیر شروع را از سرور می‌گیرد (اپ پیش‌فرض `/api/auth/google/app/start` را دارد) تا تغییر مسیر در آینده نیازمند به‌روزرسانی اپ نباشد.

---

## ۴) پیش‌نیاز عملیاتی (باید یک‌بار در Google Console انجام شود)

در **Authorized redirect URIs** کلاینت OAuth وب:

```
https://hsai.ir/api/auth/google/app/callback     ← تازه (برای اپ)
https://hsai.ir/api/auth/google/callback         ← قبلی (برای وب، دست‌نخورده)
```

بدون ثبت مسیر اول، گوگل `redirect_uri_mismatch` می‌دهد. این تنها کاری است که از بیرون از مخزن لازم است و در این محیط قابل انجام/تأیید نیست.

---

## ۵) امنیت

- توکن نشست **هرگز** در URL نمی‌آید؛ فقط برگهٔ یک‌بارمصرف ۱۸۰ ثانیه‌ای که به nonce همان نصب مقید است.
- هر برگه فقط یک‌بار قابل نقد است (سرور `verifyAppHandoff` + TTL).
- state جریان اپ امضاشده است (`signOAuthState`) و بدون `app:true` معتبر، callback هرگز برگه نمی‌سازد.
- محدودیت نرخ روی `app/start` و روی `POST /auth/google/handoff`.
- nonce در مخزن دستگاه ذخیره می‌شود ولی رمز نیست: شناسهٔ تصادفی کوتاه‌عمر است و هیچ اختیار یا داده‌ای به آن گره نخورده؛ اگر پاک شود فقط «دوباره تلاش کنید» می‌شود.

---

## ۶) آزمون‌ها (همه سبز، تکرارپذیر)

| آزمون | فرمان | نتیجه |
|---|---|---|
| جریان اپ + برگشت وب به اصل (متنی) | `node verify/server-handoff.mjs` | **22/0** |
| جریان اپ + رفتار وب (HTTP واقعی) | `node verify/server-app-google.mjs` | **11/0** |
| دروازهٔ v1 | `node verify/server-gateway.mjs` | **12/0** |
| قراردادهای کاتلین (شامل ۱۹ سنجهٔ گوگل) | `bash verify/run.sh` | **123/0** |
| حل نوع کل ماژول | `bash verify/typecheck.sh` | ok |
| برابری توکن/آیکون/استایل | `python3 verify/tokens.py` | 117/117 |

سنجه‌های تازهٔ کاتلین: شکل نشانی (`/api/auth/google/app/start?nonce=…`)، نبودِ مسیر وب در نشانی اپ، گیت `google_redirect_enabled`، مسیر قابل‌تنظیم از سرور، و چرخهٔ `encode/decode` nonce با انقضا/ساعت آینده/ورودی خراب.

---

## ۷) تأییدنشده

- **ورود واقعی گوگل** نیازمند APK امضاشده + ثبت redirect URI بالا + اعتبارنامه‌های واقعی است؛ در این محیط فقط مسیرها و پاسخ‌های ۳۰۲ و منطق برگه آزموده شده‌اند (هیچ تماسی با accounts.google.com انجام نشد).
- **رفتار مرورگر در بازگشت خودکار**: با اسکیم سفارشی، کروم معمولاً بدون پرسش به اپ برمی‌گردد ولی در برخی نسخه‌ها یک تأیید «Open» نشان می‌دهد. تبدیل به App Link واقعی (`https://hsai.ir/auth/app`) نیازمند `assetlinks.json` و اثر انگشت امضای اپ است.
- رندر UI دکمهٔ گوگل روی دستگاه (ممیزی UI دور ششم) هم همچنان تأییدنشده است.
