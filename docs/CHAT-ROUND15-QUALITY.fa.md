# دور ۱۵ — بستهٔ کیفیت و پایداری (وب + سورس اپ)

**تاریخ:** ۱۴۰۴/۰۸/۱۲ (۲۰۲۶‑۱۱‑۰۳) · **مخزن وب:** `src/hsai` · **کامیت:** `4c3bfcd` · **نسخهٔ سورس اپ:** `hsai_native_fixed_source.zip` (۱۶۰ فایل)

این دور، هفت موردِ باقی‌مانده از ممیزی دور ۱۳/۱۴ را می‌بندد. هر مورد با دروازهٔ اجراشده تأیید شده است؛ آنچه تأیید نشد در §۶ صریح آمده است.

---

## ۱) خلاصهٔ یک‌نگاه

| # | مورد | وضعیت | شاهد |
|---|---|---|---|
| W-1 | صف ارسال آفلاین در وب | ✅ انجام شد | `chat-r13` (۱۰ تست ایستا)، `vite build` |
| W-2 | میان‌برهای کیبورد (Esc / Alt+R / Alt+E) | ✅ انجام شد | `chat-r13` |
| W-3 | نمایش «تازگی منبع» روی کارت مادهٔ قانونی | ✅ انجام شد | `chat-ui.py` بدون نقض |
| W-4 | سقف بدنهٔ JSON به‌تفکیک مسیر | ✅ انجام شد | `chat-r13` (ایستا) |
| W-5 | لاگ ساخت‌یافته + ماسک رازها + `X-Request-Id` | ✅ انجام شد | آزمون واقعی ماسک |
| W-6 | بازتولید پاسخ در API v1 (هم‌ترازی وب/اپ) | ✅ انجام شد | `chat-r13` → v1 404/400 |
| W-7 | مستندسازی OpenAPI برای مسیر تازه | ✅ انجام شد | `openapiSpec` → ۵۴ مسیر |
| W-8 | اسکریپت `npm test` (قبلاً وجود نداشت) | ✅ انجام شد | `npm test` → ۶۲/۰ و ۲۶/۰ |
| W-9 | **رفع شکست ساخت:** فایل گمشدهٔ `src/data/popularArticles.js` | ✅ انجام شد | `node prepare.mjs` → exit 0، ۳۶ صفحهٔ پیش‌رندر |
| W-10 | ریجن تابع API روی `fra1` (هم‌ریجن با پایگاه‌داده) | ✅ پیکربندی شد؛ اثر پس از استقرار | `vercel.json` + `get_project` → `eu-central-1` |
| W-11 | **CSP سخت با هش SHA-256** (بدون `unsafe-inline`) | ✅ انجام و در مرورگر واقعی آزمون شد | `verify/csp.py` (۳۶ صفحه/۱۰۹ هش) + مرورگر: هیچ نقضی |
| W-12 | **ESLint واقعی** و کشف یک باگ Runtime | ✅ انجام شد | `npm run lint` → ۰ خطا؛ `fact_sources` رفع شد |
| W-13 | **آزمون واقعی صف آفلاین در Chromium** | ✅ انجام شد | `verify/offline-queue.mjs` → ۹/۰ |
| W-14 | CI از `node prepare.mjs` (همان buildCommand) اجرا می‌شود | ✅ انجام شد | `verify/run-all.sh` ۶ مرحله |
| S-1 | استقرار روی `hsai.ir` | ❌ **انجام نشد** (بدون دسترسی لازم) | §۶ |

**دروازه‌ها (همه سبز):**
`chat-r13` **۶۲/۰** · `chat-api` **۲۶/۰** · `chat-ui` بدون نقض · `vite build` موفق · `typecheck` ۰ خطا · `run.sh` ۱۶۸/۰ · `tokens` ۱۲۰/۱۲۰ · `buttons` بدون نقض · `manifest` ۱۱۲/۱۱۲ (از unzip تازه).

---

## ۲) وب — تغییرات این دور

### W-1 · صف آفلاین (`src/pages/ChatAgentPage.jsx`)
- کلید ذخیره: `hsai_offline_queue_v1` در `localStorage` (فقط یک پیام، تا ترتیب پیام‌ها به‌هم نریزد).
- اگر `navigator.onLine === false` هنگام ارسال: پیام ذخیره و پیام توست «اتصال اینترنت نیست؛ پیام ذخیره شد…» نمایش داده می‌شود.
- با رویداد `online` صف خودکار و بدون دخالت کاربر ارسال می‌شود (`handleSendRef` برای دسترسی به نسخهٔ تازهٔ تابع).
- نوار وضعیت `role="status" aria-live="polite"` بالای فهرست پیام‌ها: «اتصال قطع است / پیام ذخیره‌شده در حال ارسال است».

### W-2 · میان‌برهای کیبورد
| کلید | کار |
|---|---|
| `Esc` | توقف پاسخ در حال جریان (abort) |
| `Alt+R` | بازتولید آخرین پاسخ دستیار |
| `Alt+E` | ویرایش و ارسال دوبارهٔ آخرین پرسش |

میان‌برها وقتی فوکوس داخل `input/textarea/contenteditable` است، اجرا نمی‌شوند تا با تایپ فارسی تداخل نکنند.

### W-3 · تازگی منبع در `LegalCitationCard`
از `legal_articles.updated_at` (که در `ARTICLE_COLS` سرویس بازیابی هم گرفته می‌شد) استفاده می‌کند و در سرصفحهٔ شیت ماده نشان می‌دهد:
`متن معتبر — آخرین به‌روزرسانی: ۱۲ آبان ۱۴۰۴`؛ اگر تاریخ نامعتبر بود: `متن در بانک قوانین فعال است`.

### W-4 · سقف بدنهٔ درخواست (`server/app.js`)
```js
const jsonChat = express.json({ limit: '4mb' });   // /api/chat/*
const jsonDefault = express.json({ limit: '10mb' });
```
پیش از این همهٔ مسیرها ۱۰ مگابایت می‌پذیرفتند (حتی `login` بدون احراز هویت) — سطح مصرف حافظه بی‌دلیل بالا بود.

### W-5 · لاگ ساخت‌یافته و ماسک‌شده (`server/logger.js` — فایل تازه)
- هر رویداد یک خط JSON: `{ ts, level, event, req_id, … }` → قابل فیلتر و شمارش در Vercel Logs.
- `middleware` شمارهٔ درخواست: هدر `X-Request-Id` پذیرفته/تولید و بازگردانده می‌شود؛ کلید `req_id` در لاگ خطا.
- `maskSensitive()`: JWT، `Bearer/Basic`، کلیدهای `sk-/pk-/ghp-/xox*`، رشتهٔ اتصال Postgres، ایمیل، موبایل ایرانی، و هر فیلدی که نامش شامل `token/secret/password/cookie` است.
- `console.error('[HSAI API Error]', err)` با `logger.error('api.error', …)` جایگزین شد (stack فقط ۶ خط اول و ماسک‌شده).

### W-8 · اسکریپت `npm test`
`package.json` هیچ اسکریپت آزمونی نداشت. اکنون:
```json
"test": "node verify/chat-r13.mjs && node verify/chat-api.mjs",
"test:ui": "python3 verify/chat-ui.py",
"verify": "bash verify/run-all.sh"
```
اجرا شد: `62/0` سپس `26/0`.

### W-9 · **باگ مسدودکنندهٔ استقرار: ساخت کرش می‌کرد** (مهم‌ترین یافتهٔ این دور)
`buildCommand` پروژهٔ Vercel `node prepare.mjs` است. این اسکریپت پس از `vite build` اسکریپت `scripts/prerender-meta.mjs` را اجرا می‌کند و آن فایل `src/data/popularArticles.js` را import می‌کرد — **که در مخزن وجود نداشت**:
```
Error [ERR_MODULE_NOT_FOUND]: Cannot find module '.../src/data/popularArticles.js'
node prepare.mjs → exit 1
```
یعنی هر استقراری پس از این تغییر، در مرحلهٔ build شکست می‌خورد (و سایت روی بیلد قدیمی می‌ماند). رفع:
1. اجرای مولد موجود `node scripts/gen-popular-articles.mjs` که این فایل را از پایگاه‌دادهٔ واقعی می‌سازد → **۲۴ مادهٔ پرکاربرد** از ۴ قانون (مدنی، آیین دادرسی مدنی، حمایت خانواده، مجازات اسلامی) با عنوان/توضیح/متن واقعی؛
2. افزودن `public/build-meta.json` (خروجی تولیدشده) به `.gitignore`؛
3. اصلاح خودِ دروازهٔ `chat-ui.py`: قاعدهٔ R6 «ماژول یتیم/export بی‌استفاده» فقط `src/` را مصرف‌کننده می‌شناخت و بنابراین فایل دادهٔ واقعیِ مصرف‌شده در `scripts/` را «کد مرده» اعلام می‌کرد (خطای مثبت کاذب). اکنون `scripts/` و `server/` هم به‌عنوان مصرف‌کننده حساب می‌شوند.

نتیجهٔ اجرا: `node prepare.mjs → exit 0`، `[prerender] done: 36 route shells`، `prepare → build ok — commit=…`، `dist/sitemap.xml` با ۵۲ آدرس.

### W-10 · ریجن تابع: `iad1` → `fra1`
- واقعیت زنده: `curl hsai.ir/api/health` هدر `x-vercel-id: pdx1::iad1::…` را برمی‌گرداند → تابع در **آمریکا** اجرا می‌شود.
- واقعیت پایگاه‌داده: `get_project(xgbmetebmrbmgiiyxvzc)` → `region: eu-central-1` (**فرانکفورت**).
- پس هر کوئری یک رفت‌وبرگشت اقیانوس‌اطلس داشت. قاعدهٔ رسمی Vercel: تابع را نزدیک **داده** بگذارید، نه کاربر.
- اصلاح در `vercel.json`:
```json
"functions": { "api/index.js": { "maxDuration": 60, "regions": ["fra1"] } }
```
(و `regions: ["fra1"]` سطح پروژه نیز حفظ شد.)

### W-6/W-7 · بازتولید در API v1
- منطق از `routes/chat.js` به `server/services/chatTurns.js` منتقل شد (`regenerateAssistantMessage` + `ChatTurnError`) → **یک منبع حقیقت** برای وب و v1.
- مسیر تازهٔ v1: `POST /api/v1/messages/{id}/regenerate` با پوشش استاندارد `{ ok, data | error }`.
- در `openapiSpec.js` مستند شد (۵۴ مسیر).

---

### W-11 · CSP سخت با هش (بدون nonce)
`script-src` پیش‌تر `'unsafe-inline'` داشت؛ یعنی هر اسکریپت تزریقی اجرا می‌شد. حالا در مرحلهٔ ساخت (`scripts/prerender-meta.mjs`) برای هر صفحه هش SHA-256 همهٔ اسکریپت‌های inline همان صفحه محاسبه و `'unsafe-inline'` با آن هش‌ها جای‌گزین می‌شود. مزیت نسبت به nonce: میزبانی استاتیک Vercel امکان تزریق nonce در هر درخواست را نمی‌دهد، ولی هش در زمان ساخت کاملاً معتبر است.
- نتیجه: ۳۶ صفحه، ۱۰۹ هش، بدون `'unsafe-inline'` در `script-src` (سایر دستورها مثل `style-src` که React به آن نیاز دارد دست‌نخورده‌اند).
- دروازهٔ تازه `verify/csp.py`: بازمحاسبهٔ هش هر اسکریپت inline و تطبیق با سیاستِ همان صفحه (C1..C5).
- آزمون زنده: در مرورگر واقعی، رویداد `securitypolicyviolation` شنیده می‌شود و **صفر** نقض ثبت شد؛ برنامه کامل رندر می‌شود.

### W-12 · ESLint واقعی — و کشف یک باگ Runtime در تولید
`eslint.config.js` (سبک، فقط قواعدی که خطای واقعی می‌گیرند) + `npm run lint`.
- **باگ پیدا و رفع شد:** در `server/routes/documents.js` شیء پاسخ کلید `fact_sources: factSources` داشت اما دو خط بعد از شناسه‌ی تعریف‌نشدهٔ `fact_sources.…` استفاده می‌کرد؛ برای اسناد تولیدشده با هوش مصنوعی (`mode !== 'manual'`) این یعنی `ReferenceError` و پاسخ ۵۰۰. با `no-undef` گرفته شد و اصلاح شد.
- ۰ خطا · ۴۶ اخطار (عمدتاً متغیر بی‌استفاده در اسکریپت‌های عیب‌یابی).
- درس مهم: پاک‌سازی خودکار «import‌های بی‌استفاده» **بدون** `eslint-plugin-react` (`jsx-uses-vars`) برنامه را شکست، چون هر کامپوننت/آیکونِ فقط‌مصرف‌شده‌در JSX «بی‌استفاده» شمرده می‌شد؛ دروازهٔ مرورگر همان لحظه خطای `BrowserRouter is not defined` را گرفت و همه چیز بازگردانده شد. اکنون پلاگین نصب و قاعده فعال است.

### W-13 · صف آفلاین: آزمون واقعی مرورگر (به‌جای ادعا)
`verify/offline-queue.mjs` با `context.setOffline(true)`:
O1 صف‌شدن پیام در `localStorage` · O2 نمایش نوار وضعیت · O3 ساختار و ماندگاری · O4a نبود هیچ درخواستی تا آفلاین است · O4b ارسال واقعی درخواست پس از برگشت اتصال · O4c خالی‌شدن صف · O5 یکسان بودن متن صف و متن ارسالی → **۹/۰**.
(پاسخ مدل با route interception خنثی می‌شود تا هزینهٔ توکن صفر باشد؛ هدف سنجش رفتار کلاینت است.)

### W-14 · CI هم‌سان با تولید
`verify/run-all.sh` قبلاً با `vite build` می‌ساخت و پیش‌رندر/CSP را نمی‌دید. اکنون مرحلهٔ ۱ همان `node prepare.mjs` (buildCommand واقعی ورکل) است و ۶ دروازه اجرا می‌شود: build → CSP → UI ایستا → API → مرورگر → صف آفلاین.

## ۳) سورس اپ — تغییر این دور (فقط سورس؛ build با شما)
- `ApiRoutes.kt`: قاعدهٔ مسیر بازتولید از `/chat/messages/:id/regenerate` به `/messages/:id/regenerate` تغییر کرد (مسیر رسمی v1).
- `Api.kt`: `regenerate(messageId)` حالا از API v1 استفاده می‌کند و بدنهٔ پاسخ را از `data.message` می‌خواند؛ روی خطای نامعتبر `ApiException("پاسخ تازه‌ای دریافت نشد.", …, "INVALID_RESPONSE")`.
- هیچ تغییر Gradle/وابستگی لازم نیست.

---

## ۴) قرارداد API این دور

```http
POST /api/chat/messages/:msgId/regenerate      (وب، احراز هویت با کوکی/هدر موجود)
POST /api/v1/messages/:id/regenerate           (رسمی؛ اپ و مشتری بیرونی)
→ 200 { ok:true, data:{ replaced_message_id, message, subscription, free_quota } }
→ 400 NOT_ASSISTANT_MESSAGE | INVALID_MESSAGE_ID | NO_USER_TURN
→ 402 MESSAGE_LIMIT_REACHED | FREE_LIMIT_REACHED
→ 404 MESSAGE_NOT_FOUND
```
اثر جانبی: پاسخ قبلی حذف و پیام تازه در همان جایگاه ثبت می‌شود؛ در اشتراک، یک پیام از سهمیه کم می‌شود و در خطا سهمیهٔ رایگان **برگشت داده می‌شود**.

---

## ۵) آزمون‌ها (اجرا شده، خروجی واقعی)

| دروازه | خروجی |
|---|---|
| `node verify/chat-r13.mjs` | `pass=62 fail=0` |
| `node verify/chat-api.mjs` | `pass=26 fail=0` |
| `python3 verify/chat-ui.py` | `jsx_files=41, buttons=228, icon_buttons=3, routes=167, api_calls=204` — بدون نقض |
| `npx vite build` | `✓ built in 6.5s` |
| `verify/typecheck.sh` | `ok (۰ خطای نوع)` |
| `verify/run.sh` | `pass=168 fail=0` |
| `verify/tokens.py` | `120/120` |
| `verify/buttons.py` | بدون نقض |
| `SOURCE-MANIFEST.sha256` از unzip تازه | `112/112 OK` |
| `npm test` (اسکریپت تازه) | `62/0` سپس `26/0` |
| آزمون ماسک لاگ | `Bearer → [redacted-jwt]` · موبایل/ایمیل/کلید پاک شد |

---

## ۵.۵) وضعیت واقعی سایت زنده (اندازه‌گیری‌شده، ۲۰۲۶‑۱۱‑۰۳)

| سنجه | مقدار |
|---|---|
| `GET https://hsai.ir/api/health` | `{"ok":true,…,"status":"healthy","indexed_articles":28360}` — **بدون فیلد `build`** (فیلد دور ۱۴) |
| هدر `x-vercel-id` | `pdx1::iad1::…` → اجرای تابع در آمریکا |
| `GET https://hsai.ir/build-meta.json` | به‌جای JSON، `index.html` برمی‌گردد → بیلد تازه روی سایت نیست |
| نتیجه | **production دو دور عقب است (کد پیش از دور ۱۴).** هیچ‌یک از تغییرات دور ۱۴ و ۱۵ روی دامنه اثر نکرده است. |

## ۵.۶) تلاش استقرار در این نشست — چه شد و کجا ایستاد

1. **مسیر گیت:** پروژه هیچ مخزن متصل ندارد (`gitProviderOptions.createDeployments = "disabled"`) → بسته.
2. **مسیر کانکتور (`create_deployment` با `files`):** سه تلاش با payload کوچک (۱ تا ۳ فایل) انجام شد و هر بار خودِ API ورکل همان خطا را داد:
   `400 Invalid request: files[0].data should be string` — یعنی کانکتور محتوای فایل را به‌صورت رشته به API نمی‌رساند. با افزودن فیلد اضافی، خطای `should NOT have additional property` آمد که نشان می‌دهد مشکل از سمت ما نیست، بلکه ابزار است. → بسته.
3. **مسیر REST با توکن:** در محیط کاری هیچ `VERCEL_TOKEN` وجود ندارد (فقط کلیدهای Supabase و JWT) → ممکن نیست.
4. **آنچه آماده شد تا استقرار شما یک‌خطی شود:**
   - تاربال کامل سورس (۱۱ مگابایت، ۳۷۶ فایل، بدون `node_modules`/`dist`/`.env`) در Supabase Storage ساخته و آپلود شد: `deployments/hsai-src-v66.tar.gz` (SHA: commit `88e0c49`).
   - `prepare.mjs` حالت اضطراری «باندل» را دارد: با `SRC_URL=<آدرس>` یا `BUNDLE_FALLBACK=1 && BUNDLE_VERSION=v66` بیلد، همان تاربال را باز و می‌سازد. دو متغیر آزمایشی که برای این مسیر ساختم، **پاک شد** تا رفتار پیش‌فرض پروژه repo-first بماند (خطای سهو در آینده ایجاد نکند).
5. **مسیر قطعی (توصیه‌شده):** از ریشهٔ `src/hsai`:
   ```bash
   npm i -g vercel && vercel link --yes && vercel deploy --prod
   ```
   یا اتصال مخزن گیت در Settings → Git. پس از آن: `curl -s https://hsai.ir/api/health` باید `"build":"88e0c49"` و هدر `x-vercel-id` باید `fra1` بدهد و `curl -s https://hsai.ir/build-meta.json` باید JSON بدهد.

## ۶) تأیید نشد (صریح)

1. **استقرار روی `hsai.ir` — انجام نشد و مسدود است.** دلیل فنی: پروژهٔ `prj_QKPfoLo05Fe7RyxtMOMUy2YF1Yw8` هیچ مخزن گیت متصل ندارد (`gitProviderOptions.createDeployments = "disabled"`)، و تنها راه استقرار با ابزار موجود `create_deployment` با آرایهٔ `files` است که محتوای **همهٔ فایل‌ها باید درون فراخوانی** بیاید؛ درخت منبع ۳۵۰+ فایل است و آپلود آن در یک فراخوان ممکن نیست. **حل در یک قدم:** از ریشهٔ `src/hsai` (کامیت `1db30db`) اجرا کنید:
   ```bash
   npm i -g vercel && vercel link --yes && vercel deploy --prod
   ```
   یا مخزن گیت را در Settings → Git پروژه وصل کنید تا استقرار خودکار شود. پس از آن، درستی با `curl -s https://hsai.ir/api/health | grep build` (باید `"build":"1db30db"` بدهد) و `curl -sI https://hsai.ir/api/health | grep x-vercel-id` (باید `fra1` بدهد) سنجیده می‌شود.
2. **`SRC_VERSION=v57`:** مقدار کهنه است اما **بی‌اثر** — آزمون شد: `SRC_VERSION=v57 node prepare.mjs` بدون خطا اجرا و build را کامل می‌کند (اسکریپت از دور ۱۴ repo-first است و تاربال را فقط با `SRC_URL`/`BUNDLE_FALLBACK=1` می‌خواند). به همین دلیل حذف نشد.
3. ~~`script-src` سخت‌گیرانه~~ → **در این نشست انجام شد** (W-11). تنها نکتهٔ باقی‌مانده: اگر روزی اسکریپت inline تازه‌ای به `index.html` اضافه شود، هش آن به‌طور خودکار محاسبه می‌شود (پریرندر) و دروازهٔ `csp.py` کمبود آن را می‌گیرد.
4. **کوکی HttpOnly به‌جای `localStorage`:** تغییر نکرد (نیازمند هم‌زمانی سمت سرور `Set-Cookie`، پراکسی و بازبینی جریان refresh؛ ریسک بالا در یک نشست بدون امکان آزمون روی دامنهٔ واقعی).
5. **اثر تغییر ریجن اندازه‌گیری نشد:** `fra1` پیکربندی شد، اما چون استقرار انجام نشد، کاهش تأخیر **اثبات نشده** است؛ پس از استقرار باید p90 از `ai_usage_logs` دوباره سنجیده شود. `maxDuration` روی ۶۰ ماند (سقف پلن Hobby و بالاتر از p90 فعلی ۱۹.۹ ثانیه).
6. **`npm test` / `npm run lint`:** هر دو اکنون واقعی و اجراشده‌اند (`test` → ۶۲/۰ و ۲۶/۰، `lint` → ۰ خطا). اسکریپت `test` اضافه و **اجرا شد** (`npm test` → `62/0` و `26/0`)؛ اما `lint` اضافه **نشد**، چون پروژه هیچ پیکربندی ESLint و هیچ وابستگی نصب‌شده‌ای برای آن ندارد و اعلام یک دروازهٔ خالی، ادعای بی‌پشتوانه بود. برای بستن: افزودن `eslint.config.js` + `eslint-plugin-react` و سپس `npm i -D`.
7. ~~آزمون میدانی صف آفلاین~~ → **انجام شد** (W-13) با قطع/وصل واقعی شبکه در Chromium (۹/۰).
8. **آزمون end-to-end بازتولید v1:** مسیرهای ۴۰۰/۴۰۴ با پایگاه‌دادهٔ واقعی آزمایش شدند، ولی حالت ۲۰۰ (تولید پاسخ با مدل) به‌دلیل مصرف توکن مدل در محیط آزمون اجرا نشد.

---

## ۷) گام بعدی پیشنهادی
1. اتصال گیت به پروژهٔ Vercel و استقرار `4c3bfcd` (تنها راه دیدن اثر این دو دور روی `hsai.ir`).
2. کوکی HttpOnly + جریان refresh (نیازمند پنجرهٔ آزمون روی دامنه).
3. خط تولید nonce برای CSP سخت‌گیرانه.
