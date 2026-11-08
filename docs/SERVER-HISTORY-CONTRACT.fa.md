# تکمیل تاریخچه گفتگو در سرور — آماده برای مالک سرور

## وضعیت
این تغییر **روی سایت تولید اعمال نشده است**؛ دامنه کار، سورس اپ است. نسخه OpenAPI 2.1.0 پیوست کاربر نیز در schema `Message` هنوز `verification` را تعریف نمی‌کند. کلاینت بومی اکنون داده برگشتی `verification` و رشته `verification_json` را می‌خواند و سند/پرسش‌های تکمیلی موجود را نگه می‌دارد؛ نبود داده را با اطلاعات ساختگی پر نمی‌کند.

برای کامل شدن پیشنهاد تاریخچه، مالک سرور باید در handler موجود `GET /api/v1/consultations/:id` در `server/routes/v1.js` تغییر زیر را پس از بازبینی اعمال کند. شرط مالکیت گفتگو (`id = ? AND user_id = ?`) و `requireAuth` حتماً حفظ شوند. مسیر مدیریتی یا مجوز تازه لازم نیست.

## پچ همراه

فایل `integration/history-v53.patch` تغییر حداقلی handler v53 را آماده می‌کند. در مخزن **وب**، پس از تطبیق نسخه فعلی فایل و ستون دیتابیس، ابتدا `git apply --check` اجرا شود. این پچ بر اساس متن handler v53 تهیه شده است؛ روی مخزن یا دیتابیس تولید apply نشده و جای آزمون مالک را نمی‌گیرد. schema OpenAPI نیز باید طبق توضیح پایین تکمیل شود.

## تغییر موردنیاز

در SELECT پیام‌های همان گفتگوی مجاز، `verification_json` نیز خوانده شود:

```sql
SELECT id, conversation_id, role, content,
       cited_articles_json, suggested_actions_json,
       verification_json, feedback, created_at
FROM messages
WHERE conversation_id = ?
ORDER BY id ASC
```

در نگاشت خروجی، علاوه بر فیلدهای فعلی:

```js
const parseObject = (raw) => {
  try {
    const value = JSON.parse(raw || '{}');
    return value && typeof value === 'object' && !Array.isArray(value) ? value : {};
  } catch { return {}; }
};

const verification = parseObject(m.verification_json);
// در آبجکت همان پیام اضافه شوند؛ فیلدهای قبلی حذف نشوند.
verification,
quick_questions: Array.isArray(verification.quick_questions)
  ? verification.quick_questions
  : (Array.isArray(verification.intake?.questions) ? verification.intake.questions : [])
```

این قطعه، راهنمای ادغام داخل handler فعلی است، نه فایل کامل قابل اجرای مستقل. ستون موردنظر در مسیر ذخیره پاسخ سورس v53 وجود دارد؛ پیش از اعمال روی نسخه واقعی دیتابیس نیز بررسی شود.

## قرارداد OpenAPI

به schema `Message.properties`، همان schema `Verification` و آرایه `quick_questions` افزوده شوند. `verification` بهتر است nullable/اختیاری بماند تا پیام‌های قدیمی و نقش user بدون داده اضافی معتبر باشند. داده داخلی نامرتبط، کلیدهای محرمانه یا استدلال پنهان مدل نباید به خروجی اضافه شود؛ فقط فراداده نمایشی فعلی پاسخ چت برگردد.

## آزمون پیش از انتشار

۱. گفتگو با سند/پرسش ساختگی بسازید؛ سپس با همان کاربر دوباره باز کنید و برابری سند و پرسش‌ها را بررسی کنید.
۲. پیام قدیمی با مقدار خالی/JSON نامعتبر نباید کل endpoint را با خطای ۵۰۰ متوقف کند.
۳. درخواست کاربر دوم برای شناسه گفتگوی کاربر اول باید همچنان ۴۰۴/عدم دسترسی بگیرد و هیچ فراداده‌ای نشت نکند.
۴. ترتیب پیام‌ها، استنادها و بازخورد قبلی حفظ شود.
۵. به‌روزرسانی فقط پس از آزمون محلی و staging مالک انجام شود؛ این پروژه هیچ deploy یا دستکاری دیتابیس تولید انجام نداده است.
