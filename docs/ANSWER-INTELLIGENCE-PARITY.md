# هوشمندی‌های پاسخ: وب‌سایت ↔ اپ اندروید

**تاریخ:** ۱۴۰۵/۰۸/۱۸ · **موضوع:** «هوشمندی‌هایی که سایت هنگام جواب دادن دارد و در اپ نبود»

---

## ۰) خلاصهٔ یک‌خطی

این هوشمندی‌ها **در API بودند** و اپ آن‌ها را می‌گرفت ولی **دور می‌ریخت**: کل شیء `verification`
به‌صورت «کلید: مقدار» خام چاپ می‌شد. حالا هر کدام کارت بومی خودش را دارد، دقیقاً به همان ترتیب سایت.
تنها یک مورد واقعاً از API کم بود (فهرست `suggested_actions` برای **اولین** پیامِ هر گفتگو) که در
سورس سرور سایت اصلاح شد.

---

## ۱) سیاههٔ کامل هوشمندی‌ها

| # | هوشمندی | فیلد API | رندر سایت | اپ: قبل | اپ: حالا |
|---|---|---|---|---|---|
| I-01 | **پرسیدن سؤال شفاف‌سازی** (interview) | `verification.quick_questions` (و `verification.intake.questions`) | `QuickQuestionsWidget` | فرم ساده با دکمهٔ «ارسال پاسخ پرسش‌ها» | `QuickQuestionsWidget`: پرسش تک‌گزینه‌ای با **یک کلیک** ارسال می‌شود، شماره‌گذاری، کادر توضیح همیشه باز |
| I-02 | **کارت محاسبهٔ قانونی** | `verification.calc_card = {title, items[]}` | `LegalCalcSummaryCard` (سطح navy-900) | ❌ فقط JSON خام | `CalcSummaryCard` |
| I-03 | **کارت آرام‌سازی ابلاغیهٔ ثنا** | `verification.sana_reassurance` | `SanaCalmingCard` (سه ستون) | ❌ | `SanaCalmingCard` |
| I-04 | **خلاصهٔ خودمانی سه‌خطی** | `verification.citizen_guide.quick_summary` | `CitizenQuickSummaryCard` | ❌ | `CitizenQuickSummaryCard` |
| I-05 | **جعبه‌ابزار عملی** (اقدام فردا صبح / سنجش مدرک / مقایسهٔ مسیر + اشتباه‌های مرگبار) | `verification.citizen_guide` | `CitizenPracticalToolkit` با سه تب | ❌ | `CitizenToolkit` با سه تب |
| I-06 | **کارت بندهای پرخطر قرارداد** | `verification.scan_mode + risk_score + red_flags` | `ContractRiskReportCard` | ❌ | `ContractRiskCard` |
| I-07 | **برچسب حالت پاسخ** | `verification.mode` + `hide_badge` | `MODE_LABELS` | ❌ | `ModeBadge` با همان منطق `hideModeBadge` |
| I-08 | **سربرگ دستیار** | — | نشان در قاب سرمه‌ای + «دستیار هوشمند حق‌سنج» | آواتار کوچک + «حق‌سنج» | `AssistantHeader` |
| I-09 | **راستی‌آزمایی استناد** | `verification.verified / citation_issues / stages / competent_authority / total_checked_corpus` | `VerificationBadge` (بازشدنی) | هشدار شکست + **چاپ خام JSON** | `VerificationSection` (جمع‌وجور و بازشدنی؛ بدون چاپ خام) |
| I-10 | **بازنویسی «ساده‌تر بگو»** | `verification.simplified_content` | جعبهٔ سبز داخل همان پیام | یک پیام تازه از دستیار (تاریخچه شلوغ می‌شد) | `SimplifiedBox` داخل همان پیام |
| I-11 | **پیشنهادهای پاسخ** | `suggested_actions` | چیپ‌ها | بود، ولی برای **اولین** پیام می‌آمد؟ نه (باگ API) | حالا همیشه می‌آید |
| I-12 | **سهمیه/اشتراک** | `free_quota`, `subscription` | سایدبار | فقط نام پلن | ماندهٔ گفتگو/پیام/سند + سهمیهٔ رایگان |

---

## ۲) تنها چیزی که واقعاً در API کم بود — و در سایت اصلاح شد

`POST /api/v1/chat` دو شعبه دارد:

```js
if (!convId) {                     // گفتگوی تازه (اولین پیام)
  return ok(res, { …, quick_questions, cited_articles, verification, subscription });   // ❌ بدون suggested_actions
}
…
return ok(res, { …, quick_questions, cited_articles, verification, subscription });     // ✅ در شعبهٔ بعدی هم نبود
```

شعبهٔ «گفتگوی موجود» و رویداد `done` جویبار هر دو `suggested_actions` را برمی‌گردانند، ولی شعبهٔ
«گفتگوی تازه» نه. نتیجه: **اولین پاسخ هر گفتگو** در سایت و اپ «پیشنهادهای پاسخ» نداشت.

اصلاح (در سورس سرور سایت، `server/routes/v1.js`):

```js
+ suggested_actions: result.suggested_actions || [],
+ free_quota: await freeQuotaSnapshot(req.user.id)
```

نسخهٔ قابل‌اعمال: `docs/patches/site-v1-chat-suggested-actions.patch` · بررسی نحوی: `node --check routes/v1.js` → بدون خطا.

---

## ۳) چطور با اطمینان می‌گوییم کار می‌کند

```bash
bash verify/run.sh      # pass=50 fail=0   (۲۶ بررسی قبلی + ۲۴ بررسی تازه برای همین کارت‌ها)
bash verify/typecheck.sh # ok — ۰ خطای نوع  (کل ماژول روی android.jar 35 + Compose 1.7.6)
python3 verify/tokens.py # 39/39
```

بررسی‌های تازهٔ هارنس روی **همان JSONی** اجرا می‌شوند که سرور می‌فرستد:

```
mode_read · hide_badge_false · calc_card_items · calc_card_empty_hidden · sana_card ·
citizen_guide · quick_summary · quick_questions_from_verification · verification_not_failed ·
mode_hidden_conversational · mode_hidden_rag_no_citation · mode_shown_rag_with_citation ·
mode_shown_document · risk_flags_read · risk_score_read
```

---

## ۴) ترتیب کارت‌ها در اپ (هم‌ترتیب با سایت)

```
سربرگ دستیار + برچسب حالت
کارت آرام‌سازی ابلاغیه  →  کارت محاسبهٔ قانونی  →  کارت ریسک قرارداد
خلاصهٔ خودمانی سه‌خطی   →  جعبهٔ بازنویسی ساده  →  متن پاسخ
راستی‌آزمایی استناد  →  جعبه‌ابزار عملی  →  پرسش‌های شفاف‌سازی
پیشنهادهای پاسخ  →  استنادهای قانونی  →  برگهٔ قضایی  →  دکمه‌های کپی/خواندن/…
```

---

## ۵) آنچه تأیید نشده

- **رندر واقعی روی دستگاه**: `./gradlew assembleDebug` در این محیط اجرا نمی‌شود (بدون AAPT/پلاگین Compose)،
  پس ظاهر کارت‌ها دیده نشده است؛ درستی داده و امضاها ماشین‌بازبینی شده.
- **سرور زنده**: اصلاح `v1.js` فقط `node --check` شده و با دیتابیس واقعی اجرا نشده است.
