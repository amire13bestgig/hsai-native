# دور یازدهم — ایمیل مشکل دیتابیس + ممیزی کامل UI صفحهٔ چت سایت

**تاریخ:** ۱۱ نوامبر ۲۰۲۶ · **دامنهٔ کار:** `src/hsai` (سایت React + API اکسپرس) و پروژه‌های Supabase
**ترتیب اجرا طبق درخواست:** ۱) خواندن ایمیل دیتابیس → ۲) رفع مشکل → ۳) ممیزی صفحهٔ چت (کارکرد + UI + جریان) + حذف کد مرده + تمیزکاری پروژه

---

## بخش ۱ — ایمیل «مشکل دیتابیس» و کاری که انجام شد

### ۱.۱ چه ایمیلی بود؟
دو ایمیل از Supabase به `info2@amirshopmodel.ir` (هر دو در Inbox، خوانده‌نشده):

| تاریخ | فرستنده | موضوع | متن اصلی (خلاصه) |
|---|---|---|---|
| ۶ نوامبر ۲۰۲۶ | `ant.wilson@supabase.com` | **Your Supabase Project tandem-workspace has been paused.** | «برای بهینه‌سازی منابع، پروژه‌های پلن رایگان بعد از ۷ روز بی‌فعالیتی خودکار متوقف می‌شوند… پروژهٔ `tandem-workspace` (ID: `czmtuwwznozntkwqeiyq`) متوقف شد. تا ۹۰ روز می‌توانید از داشبورد آن را روشن کنید؛ بعد از آن پروژه قابل بازگردانی نیست.» |
| ۱۰ نوامبر ۲۰۲۶ | `noreply@supabase.com` | **Action required: security vulnerabilities detected in your projects** | «Critical issue — Table publicly accessible — `rls_disabled_in_public` — Project: **dadban-ai** (`xgbmetebmrbmgiiyxvzc`). هر کسی با URL پروژه می‌تواند همهٔ دادهٔ این جدول را بخواند/تغییر دهد/حذف کند، چون Row-Level Security فعال نیست.» (گزارش تا ۸ نوامبر) |

### ۱.۲ وضعیت واقعی دو پروژه (با کانکتور Supabase، نه حدس)

| پروژه | Ref | وضعیت پیش از کار | نقش در این مخزن |
|---|---|---|---|
| `dadban-ai` | `xgbmetebmrbmgiiyxvzc` | ACTIVE_HEALTHY | **همین پروژهٔ دیتابیس سایت است** — `DATABASE_URL` روی `aws-1-eu-central-1.pooler.supabase.com:6543` با کاربر `postgres.xgbmetebmrbmgiiyxvzc` و `SUPABASE_URL=https://xgbmetebmrbmgiiyxvzc.supabase.co` |
| `tandem-workspace` | `czmtuwwznozntkwqeiyq` | **INACTIVE (متوقف)** | هیچ ارجاعی به آن در این مخزن نیست (پروژهٔ جدا در همان سازمان) |

نتیجهٔ مهم: **ایمیلی که «دیتابیس مشکل دارد» به آن اشاره داشت، دو موضوع جدا بود** — یکی توقف پروژهٔ دوم، دیگری باز بودن RLS در پروژهٔ اصلی سایت.

### ۱.۳ اقدام‌های انجام‌شده (واقعی، روی همان پروژه‌ها)

1. **بازگردانی پروژهٔ متوقف‌شده**
   - `restore_project(czmtuwwznozntkwqeiyq)` → `{"success": true, "message": "Project restored successfully"}`
   - بازبینی: `get_project` از `COMING_UP` به **`ACTIVE_HEALTHY`** رسید و `get_project_health(db,rest)` هر دو **healthy=true**.
2. **رفع یافتهٔ Critical «جدول بدون RLS» در پروژهٔ اصلی سایت**
   - سه جدول در `public` بدون RLS بودند: `free_chat_quota`، `oauth_identities` (هویت‌های گوگلِ کاربران)، `oauth_link_tickets` (بلیت‌های اتصال حساب). بقیهٔ ۲۴ جدول از قبل RLS داشتند.
   - مهاجرت `enable_rls_public_tables_advisor_fix`:
     `ENABLE ROW LEVEL SECURITY` روی هر سه + یک policy صریح `deny_all_anon` (`TO anon, authenticated USING (false) WITH CHECK (false)`).
   - **چرا policy مسدودکننده و نه policy مجاز؟** چون کل دسترسی برنامه از سمت سرور و با نقش مالک جدول (`postgres.<ref>`) انجام می‌شود که RLS برایش اعمال نمی‌شود؛ کلید `anon` در مرورگر هیچ‌جا برای این سه جدول استفاده نمی‌شود (`src/` هیچ ارجاعی به `supabase-js` یا این جدول‌ها ندارد). پس مسدودسازی کامل، امن است و چیزی را نمی‌شکند.
3. **بستن هشدار `function_search_path_mutable` برای توابع خودِ پروژه** (خانوادهٔ همان Advisor؛ ۱۴۵ مورد دیگر توابع افزونه‌های `pgvector`/`pg_trgm` هستند و دست‌نخورده ماندند)
   - مهاجرت `pin_search_path_app_functions`: `ALTER FUNCTION … SET search_path = pg_catalog, public` برای `norm_fa(text)`، `now_iso()`، `match_legal_corpus_v12(...)`.
4. **جلوگیری از تکرار «توقف خودکار پروژه»**
   - `vercel.json` یک کران روزانه گرفت: `{"path": "/api/health", "schedule": "0 4 * * *"}`. مسیر `/api/health` واقعاً به دیتابیس کوئری می‌زند (`SELECT SUM(article_count) FROM legal_laws`)، پس فعالیت پروژه هیچ‌وقت به ۷ روز بی‌حرکتی نمی‌رسد.

### ۱.۴ تأییدهای تکرارپذیر

| بررسی | دستور/ابزار | نتیجه |
|---|---|---|
| هیچ جدولی بدون RLS نمانده | SQL روی `pg_class` | `rows: []` (صفر جدول) |
| برنامه هنوز با نقش مالک می‌خواند | اسکریپت `pg` روی `DATABASE_URL` | `SELECT` روی `free_chat_quota`, `oauth_identities`, `oauth_link_tickets`, `users`, `conversations`, `messages`, `legal_articles` → همه `ok` (مثلاً `legal_articles=28360`) |
| کلید anon دیگر داده نمی‌بیند | REST با `apikey: anon` | هر سه ⇢ `200 []` (خالی) |
| جست‌وجوی فارسی نشکسته | `select * from match_legal_corpus_v12('مهریه', …, 5)` | ۵ ردیف برگشت؛ `norm_fa('چك')='چک'`، `now_iso()` سالم |
| داده آزمایشی در دیتابیس نماند | SQL | `leftover_users=0, leftover_convs=0, leftover_tickets=0, leftover_msgs=0` |

### ۱.۵ محدودیت‌ها (صریح) — با یک به‌روزرسانی: مورد `tandem-workspace` بعداً با تأیید کاربر انجام شد

- **سبزشدن قطعی یافته در Advisor:** Supabase Advisor زمان‌بندی‌شده اجرا می‌شود؛ رفع در سطح دیتابیس با SQL تأیید شد، اما «سبز شدن نشانگر داشبورد» را نمی‌توانم از این محیط تأیید کنم تا اجرای بعدی گزارش.
- **پروژهٔ `tandem-workspace` — انجام شد (به درخواست صریح کاربر، ۱۱ نوامبر):**
  - پیش از کار: ۱۶ جدول در `public` **همه بدون RLS** و بدون هیچ policy؛ `auth.users` صفر بود (یعنی اپ از Supabase Auth استفاده نمی‌کند و احراز هویتش سمت سرور است). دادهٔ واقعی موجود بود: `users=2`, `members=3`, `sessions=2`, `projects=1`, `activities=2`, `ai_usage=2`, `ai_settings=1`.
  - مهاجرت `enable_rls_all_public_tables`: `ENABLE ROW LEVEL SECURITY` روی هر ۱۶ جدول + policy `deny_all_anon` برای `anon, authenticated` + `REVOKE ALL` روی جدول‌ها و سکانس‌های schema از همان دو نقش.
  - تأیید: `tables_with_rls=16/16`، `total_policies=16`، `funcs_mutable_searchpath=0`، `public_views=0`.
  - تأیید عملی از بیرون: `anon GET` روی `users/sessions/members/projects/tasks` → **401 `42501 permission denied`** و `anon POST tasks` هم **401** (قبلاً همه باز بود).
  - تأیید دسترسی سرور: کانکتور با نقش `postgres` همان داده را می‌خواند (`users=2, members=3, sessions=2, projects=1`) — دست‌نخورده.
  - **ریسک اعلام‌شده:** اگر آن اپ در مرورگر با کلید publishable/anon کوئری می‌زند، از این پس ۴۰۱ می‌گیرد و باید سمت سرور با `service_role` کار کند (یا policy دقیق per-table بگیرد).
  - **بازگشت (اگر لازم شد):**
    ```sql
    DO $$ DECLARE t text; BEGIN
      FOR t IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
        EXECUTE format('DROP POLICY IF EXISTS deny_all_anon ON public.%I;', t);
        EXECUTE format('ALTER TABLE public.%I DISABLE ROW LEVEL SECURITY;', t);
      END LOOP;
    END $$;
    GRANT ALL ON ALL TABLES IN SCHEMA public TO anon, authenticated;
    GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;
    ```
- پروژهٔ رایگان `tandem-workspace` هم دوباره پس از ۷ روز بی‌فعالیتی متوقف می‌شود؛ یا باید Pro شود یا کران keepalive بگیرد (این پروژه در Vercel این مخزن deploy نیست).

---

## بخش ۲ — ممیزی صفحهٔ چت سایت (مثل دور دهم: کارکرد + نمایش + جریان)

**فایل اصلی:** `src/pages/ChatAgentPage.jsx` (۱۹۷۳ → **۱۸۶۳** خط؛ ۵۶۶ خط تغییر/حذف)
**روش سه‌محوره:** برای هر عنصر تعاملی سه سؤال پرسیده شد — ۱) آیا کار می‌کند و به کدام endpoint/تابع می‌رود؟ ۲) آیا درست دیده/خوانده/لمس می‌شود (کنتراست، اندازهٔ ۴۴px، نام دسترس‌پذیر، حالت غیرفعال)؟ ۳) آیا جریان پیش/پس از آن درست است (حالت‌های بارگذاری، خطا، بازگشت فوکوس، داده‌های باقی‌مانده)؟
**مجموع عناصر بررسی‌شده در این صفحه:** ۳۹ `<button>`، ۷ `<Link>`، ۲ `<form>`، ۳ دیالوگ/مودال، ۴ ورودی متنی و ۲ ورودی فایل — همه در ۱۲ بخش زیر:

| # | بخش | عناصر |
|---|---|---|
| ۱ | سایدبار — برند و ابزار | لینک برند (→`/`)، دکمهٔ بستن، «گفتگوی جدید»، «بانک قوانین»، «محاسبه‌گرها»، جستجوی گفتگوها |
| ۲ | سایدبار — فهرست گفتگوها | ردیف گفتگو (باز کردن)، دکمهٔ حذف دومرحله‌ای، حالت خالی/بارگذاری/خطا |
| ۳ | سایدبار — اشتراک و حساب | کارت اشتراک، سهمیهٔ رایگان، مانده‌ها، «حساب کاربری…»، «پنل مدیریت» (فقط ادمین)، خروج، نام کاربر |
| ۴ | نوار بالا | منو (موبایل)، عنوان گفتگو، چیپ اشتراک (موبایل)، تم تاریک/روشن، «محاسبهٔ مهریه»، «معنی کلمات دادگاه»، «چت جدید» |
| ۵ | صفحهٔ خوش‌آمد | کارت پیوست عکس ابلاغیه، «تنظیم برگه شکایت»، ۶ کارت موقعیت زندگی |
| ۶ | پیام کاربر | تصویر پیوست + متن |
| ۷ | سربرگ پاسخ | نشان موتور پاسخ (rag / calculator / document / scanner)، کارت‌های تسلی خاطر ثنا، محاسبهٔ قانونی، ریسک قرارداد، خلاصهٔ سه‌خطی |
| ۸ | برگهٔ رسمی | «چاپ/PDF (A4)»، «فایل Word (.docx)»، «ویرایش مشخصات/متن»، «تمام‌صفحه»، ادیتور درون‌خطی (انصراف/ذخیره)، نکتهٔ ثبت |
| ۹ | پرسش‌های گام‌به‌گام | گزینه‌های تک‌انتخابی/چندانتخابی (QuickQuestionsWidget)، ورودی پاسخ دلخواه، «ثبت پاسخ» |
| ۱۰ | پایین پاسخ | باز/بستهٔ «مستندات قانونی»، کارت‌های ماده، نشان اعتبارسنجی، چیپ‌های پیگیری (تا ۳ مورد)، «کپی»، «به زبان خیلی ساده‌تر بگو»، «شنیدن پاسخ/توقف صدا»، 👍/👎، «اصلاح این پاسخ» |
| ۱۱ | کارت استریم | مرحلهٔ SSE، چیپ مقاله‌های در حال یافتن، متن در حال تایپ |
| ۱۲ | کامپوزر و مودال‌ها | سنجاق فایل، میکروفون (تایپ صوتی فارسی)، textarea، «ارسال»/«توقف»، نوار پیوست + «پوشاندن کد ملی»، مودال پیش‌نمایش سند، مودال پوشاندن اطلاعات تصویر |

### ۲.۱ جدول باگ → رفع (۲۳ مورد)

| کد | محور | باگ | رفع |
|---|---|---|---|
| B-1 | جریان | **دو مسیر موازی برای حساب کاربری:** مودال «حساب کاربری و پشتیبانی» داخل چت (۲۳۵ خط) همان کار صفحهٔ `/app/account` را می‌کرد، ولی رمز عبور را بدون حداقل ۸ نویسه ذخیره می‌کرد و تأیید ایمیل/موبایل نداشت | مودال و ۱۰ حالت state آن حذف شد؛ ورودی سایدبار به `حساب کاربری، پروفایل، پشتیبانی و فاکتورها` → `/app/account` یکپارچه شد |
| B-2 | جریان | «خروج از حساب» با یک کلیک و بدون تأیید (کلیک تصادفی = خروج) | خروج دومرحله‌ای (`خروج؟` + `انصراف`) با ریست در تغییر گفتگو |
| B-3 | کارکرد | `catch {}` خاموش در 👍/👎 — خطای سرور بی‌صدا گم می‌شد | toast خطا + حالت `feedbackPending` + `aria-pressed` |
| B-4 | کارکرد | بازخورد قابل برداشتن نبود؛ کلیک دوم همان دکمه دوباره ارسال می‌کرد و سرور با `null` خطای ۴۰۰ می‌داد | کلیک دوم بازخورد را برمی‌دارد + **سمت سرور** `chat.js` مقدار `null` را برای پاک‌کردن می‌پذیرد (400 فقط برای مقدار نامعتبر) |
| B-5 | کارکرد | کپی پیام، موفقیت را بررسی نمی‌کرد؛ در HTTP یا مرورگر قدیمی `navigator.clipboard` نیست ⇒ unhandled rejection و پیام دروغین «کپی شد» | `await` + fallback `textarea/execCommand` + toast خطا؛ متن خالی هم گزارش می‌شود |
| B-6 | جریان | فایل `.txt` به‌عنوان «عکس پیوست» نمایش داده می‌شد («آماده تحلیل محرمانه» + دکمهٔ پوشاندن کد ملی) | فقط متن به پیام افزوده می‌شود + toast نام فایل؛ نوار پیوست فقط برای تصویر |
| B-7 | کارکرد | Enter در حین تایپ ترکیبی فارسی (IME) پیام را نیمه‌کاره می‌فرستاد | گارد `e.nativeEvent.isComposing` |
| B-8 | جریان | کادر جستجو با ≤۳ گفتگو پنهان می‌شد ولی فیلترِ قبلی روی فهرست باقی می‌ماند (فهرست «خالی» به‌نظر می‌رسید) | `showHistorySearch` + ریست خودکار فیلتر |
| B-9 | نمایش/جریان | ردیف گفتگو با کیبورد قابل انتخاب نبود (فقط ماوس/لمس) | `role="button"`، `tabIndex=0`، Enter/Space، `aria-current`، حلقهٔ فوکوس |
| B-10 | نمایش | ۹ دکمهٔ آیکونی بدون نام دسترس‌پذیر (بستن سایدبار، حذف گفتگو، خروج، سنجاق، میکروفون، حذف پیوست، بستن پیش‌نمایش، بستن مودال پوشاندن، راهنمای فوری PublicLayout) | `aria-label` فارسی برای همه |
| B-11 | نمایش | چند دکمهٔ آیکونی هدف لمسی < ۴۴px داشتند | کلاس `hsai-tap` (۴۴×۴۴) |
| B-12 | جریان | خطای بارگذاری فهرست گفتگوها بی‌صدا بود؛ کاربر فقط فهرست خالی می‌دید و راه بازیابی نداشت | حالت خطا + دکمهٔ «تلاش مجدد» (تابع `loadConversations`) |
| B-13 | نمایش/جریان | مودال تمام‌صفحهٔ سند بدون `role="dialog"`/`aria-modal` و بدون بستن با Escape | نقش دیالوگ + هندلر Escape + `aria-label` |
| B-14 | کارکرد | خواندن صوتی: متن خالی یا خطای `speechSynthesis` باعث گیر کردن دکمه در حالت «توقف صدا» | گارد متن خالی + `try/catch` + ریست حالت |
| B-15 | نمایش | پیام‌های تازه/استریم برای صفحه‌خوان اعلام نمی‌شد | `role="log" aria-live="polite" aria-relevant="additions text"` |
| B-16 | جریان | بعد از بستن دراور موبایل (Escape/ضربه) فوکوس به دکمهٔ منو برنمی‌گشت | بازگردانی فوکوس با `useRef` |
| B-17 | نمایش | دکمهٔ ارسال غیرفعال در موبایل تقریباً دیده نمی‌شد (خاکستری روی شیشهٔ سفید) | `disabled:bg-slate-300/80` + `dark:disabled:bg-white/15` + `cursor-not-allowed` |
| B-18 | جریان | «اصلاح این پاسخ» متن بذر را با فاصله/خط اضافی می‌نوشت و به کامپوزر اسکرول نمی‌کرد | متن تمیز + `focus` + `scrollIntoView` |
| B-19 | کد مرده | `handlePrintWindow` شاخهٔ دوم (title, body, docType, metadata) داشت که هیچ‌جا صدا زده نمی‌شد | تک‌امضایی شد (فقط شیء سند) |
| B-20 | جریان | «گفتگوی جدید»/انتخاب گفتگو، تأییدهای نیمه‌کارهٔ حذف و خروج را پاک نمی‌کرد | ریست `confirmDeleteId` و `confirmLogout` |
| B-21 | نمایش | چیپ اشتراک موبایل ترتیب کلاس بحرانی (`hidden` + `min-[430px]` + `lg:hidden`) و بدون نام داشت | ترتیب متعارف + `aria-label` + `hsai-tap` |
| B-22 | کارکرد | `aria-pressed`/`disabled` روی 👍/👎 نبود؛ در حین ارسال چند کلیک پشت‌سرهم می‌شد | `disabled={feedbackPendingId === m.id}` + `aria-pressed` |
| B-23 | نمایش | مودال پوشاندن اطلاعات تصویر: دکمهٔ بستن بی‌نام و < ۴۴px | `aria-label` + `hsai-tap` |

---

## بخش ۳ — کد مرده و تمیزکاری پروژه

| مورد | نوع | اقدام |
|---|---|---|
| `src/data/popularArticles.js` | ماژول یتیم (هیچ‌جا import نمی‌شد؛ `POPULAR_ARTICLES` مصرف‌کننده نداشت) | **فایل حذف شد** |
| `ShieldAlert` (ChatAgentPage) | import بی‌استفاده | حذف |
| `Globe` (PublicLayout)، `FadeInView`+`Globe` (GuidesHubPage) | import بی‌استفاده | حذف |
| `Smartphone`, `apiFetch`, `useEffect` (ApiDocsPage) | import بی‌استفاده | حذف |
| `Check`, `Layers`, `Mic`, `ScanSearch`, `ShieldCheck`, `formatToman` (LandingPage) | import بی‌استفاده | حذف |
| `Mail` (AccountSettingsPage)، `AlertCircle` (AboutAndTermsPage) | import بی‌استفاده | حذف |
| `BRAND_FA` (SeoHead) | constant مرده (کپی `BRAND_FULL`) | حذف |
| `formatJalali`, `SUBSCRIPTION_STATUS_FA` (utils/format) | تابع/نقشهٔ مرده | حذف |
| `BRAND_FULL`, `CITIZEN_LEGAL_GLOSSARY`, `PAYMENT_STATUS_FA`, `parseJudicialSheet`, `toIcsDate`, `toIcsDay`, `toPersianDigits` | فقط مصرف داخلی بودند ولی `export` شده بودند | `export` برداشته شد |
| `import React` در ۳۵ فایل | با JSX runtime جدید Vite لازم نیست | حذف (build سبز) |
| مودال حساب کاربری چت (۲۳۵ خط) | کد تکراری/مرده بعد از یکپارچه‌سازی | حذف (B-1) |

نتیجهٔ اسکن خودکار پس از پاک‌سازی: `orphan: []`، `unused_imports: {}`، `unused_exports: {}`.

---

## بخش ۴ — دروازه‌های تازه و نتایج عددی

سه دروازهٔ تازه در `src/hsai/verify/` ساخته شد تا این دور تکرارپذیر باشد:

1. **`verify/chat-ui.py`** — دروازهٔ ایستا با ۷ قاعده: نام دسترس‌پذیر دکمه‌های آیکونی، حد لمسی ۴۴px، هندلر توخالی/`href="#"`، `role="dialog"`+`aria-modal`+Escape، `catch` خاموش، کد مرده (ماژول/import/export)، و **قرارداد API**: هر `/api/...` در `src/` باید در `server/` مسیر واقعی داشته باشد (۱۶۴ مسیر استخراج‌شده از `app.js` + `routes/*`).
2. **`verify/chat-api.mjs`** — اکسپرس واقعی + دیتابیس واقعی: کاربر آزمایشی، JWT، و ۲۶ سنجه روی endpointهای همین صفحه (فهرست/باز کردن/حذف گفتگو، بازخورد like/null/نامعتبر/پیام ناموجود، simplify، تیکت، پروفایل، SSE بدون توکن و با پیام خالی، docx، جداسازی داده).
3. **`verify/chat-ui-browser.mjs`** — **Chromium واقعی** (بستهٔ `playwright-core` موجود) روی سرور production: ۲۶ سنجه روی رندر واقعی صفحه (خوش‌آمد، ۶ کارت، فعال/غیرفعال شدن ارسال، گفتگوی جدید، جستجوی پنهان، خروج دومرحله‌ای، دراور موبایل + Escape + `aria-expanded`، اندازهٔ واقعی هدف لمس، صفر خطای کنسول).

`verify/run-all.sh` همه را پشت سر هم اجرا می‌کند. نتیجهٔ اجرای نهایی:

| دروازه | نتیجه |
|---|---|
| `npm run build` (vite) | ✅ built in ~7s |
| `python3 verify/chat-ui.py` | ✅ بدون نقض — `jsx_files=41, buttons=225, icon_buttons=3, src_modules=41, server_routes=164, api_calls=200` |
| `node verify/chat-api.mjs` | ✅ **pass=26 fail=0** |
| `node verify/chat-ui-browser.mjs` | ✅ **pass=26 fail=0** + اسکرین‌شات `verify/out/chat-desktop.png` و `verify/out/chat-mobile.png` |
| SQL تأیید دیتابیس | ✅ صفر جدول بدون RLS، anon خالی، کوئری‌های برنامه سالم، صفر دادهٔ آزمایشی باقی‌مانده |

اعتبارسنجی خودِ دروازه (ضدِ مثبت کاذب): یک فایل آزمایشی با `apiFetch('/api/definitely-not-a-route/42')` ساخته شد؛ `R7` آن را گرفت و `R6` ماژول یتیم را — سپس فایل حذف شد و دروازه سبز ماند.

---

## بخش ۵ — چیزهایی که تأیید نشده‌اند (صریح)

- **ارسال واقعی پیام به مدل زبانی و کیفیت پاسخ** آزموده نشد (هزینهٔ API و کلید ارائه‌دهنده). آنچه تأیید شد: قرارداد SSE، مسیرهای خطا، مصرف‌نشدن سهمیه در پیام خالی، و رندر کارت‌های پاسخ با دادهٔ ساختگی در کد، نه با پاسخ واقعی مدل.
- **پنل مدیریت، درگاه پرداخت، ورود گوگل، صفحهٔ قوانین** خارج از محدودهٔ این دور بودند و آزمون تازه‌ای برایشان اجرا نشد.
- **اپ اندروید دست‌نخورده است**؛ چون در این دور تغییری نداشت، دروازه‌های دور دهم (`typecheck`, `run.sh` 168/0, `tokens.py`, `buttons.py`) بازاجرا نشدند و بستهٔ zip دور دهم همان نسخهٔ معتبر می‌ماند.
- **رندر روی دستگاه واقعی/مرورگرهای غیر Chromium** (Safari/Firefox، موتور گفتار `fa-IR`) آزموده نشد؛ آزمون مرورگر فقط Chromium بی‌سر با ویوپورت ۱۳۶۰ و ۳۹۰ است.
- **نشانگر Advisor در داشبورد Supabase** تا اجرای بعدی گزارش، «سبز» تأیید نمی‌شود (اقدام در سطح SQL و کوئری تأیید شد).
