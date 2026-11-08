# اعتبارسنجی سورس نهایی — پوشش ۱۴ محاسبه‌گر

## نتیجه همین نسخه

| بررسی | نتیجه |
|---|---|
| `compileDebugUnitTestKotlin` | موفق، کد خروج ۰ |
| `testDebugUnitTest` | موفق، کد خروج ۰؛ **۱۷۸ تست: ۱۷۵ موفق، ۳ اجرا‌نشده، صفر شکست/خطا** |
| `lintDebug` | موفق، کد خروج ۰؛ **صفر خطا، ۶ هشدار GradleDependency** |
| ساخت/تحویل APK | انجام نشد؛ تحویل فقط سورس |
| استقرار سایت/API | انجام نشد |
| بازاجرای حساب/مصرف AI برای این اصلاحات | انجام نشد؛ گزارش سرور از ممیزی زنده کامل‌شده قبلی است |

سه مورد اجرا‌نشده عبارت‌اند از دو تست opt-in عمومی `LiveApiTest` و یک `AuthenticatedApiTest`؛ آن‌ها را تست موفق حساب نمی‌شماریم. آزمون‌های UI، Compose بومی با Robolectric API 29 هستند، نه اجرای گوشی/شبیه‌ساز کامل. ذخیره screenshot جدید با `HSAI_SKIP_SCREENSHOTS=1` خاموش بود.

## شمارش از XML نهایی آزمون‌ها

| مجموعه | کل | موفق | اجرا‌نشده | شکست/خطا |
|---|---:|---:|---:|---:|
| CoreTest | ۶۵ | ۶۵ | ۰ | ۰ |
| ApiContractTest | ۲۴ | ۲۴ | ۰ | ۰ |
| CalculatorOutputTest | ۳ | ۳ | ۰ | ۰ |
| NativeUiTest | ۸۳ | ۸۳ | ۰ | ۰ |
| LiveApiTest | ۲ | ۰ | ۲ | ۰ |
| AuthenticatedApiTest | ۱ | ۰ | ۱ | ۰ |
| **جمع** | **۱۷۸** | **۱۷۵** | **۳** | **۰** |

مبنای قبل از این اصلاحات ۱۶۷ تست بود؛ ۱۱ تست اضافه شد: ۸ تست قرارداد/فهرست/خروجی، یک تست سازگاری عددی fixture چهار ابزار جدید و دو تست Compose. انتظار قدیمی تعداد ۱۰ ابزار نیز به ۱۴ اصلاح شد. تست‌ها شکل ورودی، شرط داوری، آرایه خدمات و رد ورودی نامعتبر را می‌سنجند؛ تأیید نرخ قانونی یا پوشش تمام ترکیب‌های ممکن نیستند.

Fixture عمومی چهار ابزار در `app/src/test/resources/calculator-extra-observed-output.json` از پاسخ‌های مشاهده‌شده قبلی گرفته شده است؛ حاوی داده حساب نیست. آزمون fixture، بررسی آفلاین سازگاری داخلی پاسخ است و درخواست تازه به سرور نیست.

## فایل‌های مؤثر اصلاح

- `Domain.kt`: چهار ابزار و برچسب نتیجه، مشخصه چندانتخابی `FormField.multiple`.
- `CalculatorContract.kt`: قرارداد ورودی واحد؛ فیلد شرطی، enum معتبر، services آرایه، copies صحیح ≥۱؛ حفظ اعتبارسنجی عدد و ترتیب تاریخ قبلی.
- `AppViewModel.kt`: استفاده از قرارداد ورودی مشترک در compute، با حفظ مسیر API و محافظ پاسخ دیررس.
- `Screens.kt`: فرم‌های جدید، خدمات checkbox، مقدار اولیه copies=1، قفل انتخاب حین ارسال و هشدار تأیید سرور.
- `CalculatorResults.kt`: چهار کلید مبلغ اصلی جدید؛ نتیجه از API، نه محاسبه/نرخ محلی.
- `CoreTest.kt`، `CalculatorOutputTest.kt`، `NativeUiTest.kt`: تست‌های رگرسیون جدید.

## هشدارهای باقیمانده Lint

هر شش هشدار از نوع `GradleDependency` درباره موجودبودن نسخه جدیدتر Compose BOM، Activity Compose، Lifecycle ViewModel Compose، Lifecycle Runtime Compose، WorkManager و Core KTX هستند. این مرحله مهاجرت عمده وابستگی‌ها انجام نداد؛ چنین مهاجرتی باید جداگانه با سازگاری، حداقل Android و آزمون دستگاه بررسی شود. هشدارها مخفی یا به «صفر هشدار» گزارش نشده‌اند؛ صفر خطا هم گواهی امنیت مستقل نیست.

## بازاجرای محلی در محیط کم‌حافظه

JDK 21 و SDK 35 استفاده شد؛ `ANDROID_HOME` باید به SDK محلی اشاره کند. در محیط آزمون، Gradle Wrapper 8.9، AGP 8.7.3 و Kotlin 2.0.21 بود. برای جلوگیری از فشار حافظه، کامپایل و آزمون در فرایندهای جدا اجرا شدند:

```sh
export HSAI_SKIP_SCREENSHOTS=1
./gradlew --no-daemon \
  -Dorg.gradle.jvmargs='-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -Dfile.encoding=UTF-8' \
  compileDebugUnitTestKotlin

./gradlew --no-daemon \
  -Dorg.gradle.jvmargs='-Xmx192m -XX:MaxMetaspaceSize=192m -XX:ReservedCodeCacheSize=64m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -Dfile.encoding=UTF-8' \
  testDebugUnitTest

./gradlew --no-daemon \
  -Dorg.gradle.jvmargs='-Xmx384m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:ActiveProcessorCount=2 -XX:+UseSerialGC -Dfile.encoding=UTF-8' \
  lintDebug
```

این محدودیت حافظه برای اجرای ترتیبی پس از کامپایل است؛ اگر Gradle به علت تغییر سورس نیاز به کامپایل در مرحله دوم داشته باشد، ابتدا مرحله اول را تکرار کنید یا حافظه کافی بدهید. در ماشین توسعه با RAM کافی می‌توان از تنظیمات عادی پروژه استفاده کرد.

## چه چیزی تأیید نشده است؟

اجرای گوشی، نصب/آپدیت امضاشده، SAF/دوربین، اعلان واقعی، بازگشت درگاه، پرداخت/OTP، کنترل بین دو حساب، اتمام سهمیه و صحت حقوقی خروجی‌ها هنوز تأیید نشده‌اند. خطاهای اثبات‌شده سرور در `SERVER-API-FINDINGS.fa.md` **باز هستند**. سورس جدید نباید به‌عنوان رفع استناد، OCR یا محاسبه مهلت سرور معرفی شود.

## کنترل بسته تحویلی

ZIP فقط فایل‌های سورس، resource، wrapper، آزمون/fixture و مستندات لازم را دارد. پوشه‌های build، cache، `.gradle`، `.kotlin`، اسکریپت‌های داخلی ممیزی، نتیجه خام حساب، اعتبارنامه، keystore و APK/AAB در آن قرار نمی‌گیرند. `openapi.json` نسخه دریافت‌شده اولیه است؛ اختلاف مستندات در گزارش زنده مستند شده، نه آنکه مخفیانه استاندارد اصلاح‌شده معرفی شود.
