package ir.hsai.mobile

import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/* ============================================================================
   توابع خالصِ دور نهم — هیچ‌کدام به اندروید/Compose وابسته نیستند تا در
   verify/Harness.kt قابل آزمون باشند.
   ========================================================================= */

/** سطح هشدار نشست، بر پایهٔ زمان باقی‌ماندهٔ توکن. */
enum class SessionWarning { NONE, SOON, EXPIRED }

object SessionClock {
    /** آستانهٔ «نشست در شرف پایان»: ۵ دقیقه. */
    const val SOON_SECONDS: Long = 300

    /** ثانیهٔ انقضای توکن JWT؛ null اگر توکن JWT نباشد یا `exp` معتبر نداشته باشد. */
    fun expiryEpochSeconds(token: String): Long? {
        val parts = token.split('.')
        if (parts.size != 3) return null
        val payload = runCatching {
            val raw = parts[1]
            val padded = raw + "=".repeat((4 - raw.length % 4) % 4)
            String(Base64.getUrlDecoder().decode(padded), Charsets.UTF_8)
        }.getOrNull() ?: return null
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        if (!json.has("exp")) return null
        val exp = json.optLong("exp", 0L)
        // مقدارهای بی‌معنا (۰، منفی، یا بیشتر از ۱۰۰ سال) «نامشخص» حساب می‌شوند، نه «منقضی».
        return if (exp in 1_000_000_000L..4_000_000_000L) exp else null
    }

    fun secondsLeft(token: String, nowSeconds: Long): Long? =
        expiryEpochSeconds(token)?.minus(nowSeconds)

    fun warning(secondsLeft: Long?): SessionWarning = when {
        secondsLeft == null -> SessionWarning.NONE
        secondsLeft <= 0 -> SessionWarning.EXPIRED
        secondsLeft <= SOON_SECONDS -> SessionWarning.SOON
        else -> SessionWarning.NONE
    }

    /** متن بنر؛ برای مقدار نامشخص رشتهٔ خالی (یعنی بنری نمایش داده نمی‌شود). */
    fun warningText(secondsLeft: Long?): String {
        val left = secondsLeft ?: return ""
        if (left <= 0) return "نشست شما پایان یافته است؛ برای ادامه دوباره وارد شوید."
        val minutes = (left + 59) / 60
        return "نشست شما تا حدود ${faDigits(minutes)} دقیقهٔ دیگر پایان می‌یابد؛ اگر کاری نیمه‌کاره دارید، الان ذخیره کنید."
    }
}

/** برچسب «چند روز مانده» برای کارت مهلت — عدد از خادم (`days_left`) می‌آید. */
object DaysLeft {
    fun label(days: Int?): String = when {
        days == null -> ""
        days == 0 -> "امروز آخرین روز"
        days == 1 -> "فردا آخرین روز"
        days > 1 -> "${faDigits(days.toLong())} روز مانده"
        days == -1 -> "۱ روز گذشته"
        else -> "${faDigits((-days).toLong())} روز گذشته"
    }

    /** آستانهٔ هشدار: سه روز یا کمتر (شامل گذشته). */
    fun urgent(days: Int?): Boolean = days != null && days <= 3

    fun overdue(days: Int?): Boolean = days != null && days < 0
}

/** شبکهٔ ماهِ تقویم برای نمای مهلت‌ها — فقط ریاضیات چیدمان، بدون تقویم واقعی. */
object JalaliGrid {
    const val COLUMNS: Int = 7
    val weekdayLabels: List<String> = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    /**
     * @param firstWeekday روزِ هفتهٔ اولین روز ماه، شنبه=۰ … جمعه=۶
     * @param daysInMonth تعداد روزهای ماه (۲۹…۳۱)
     * @return فهرست خانه‌ها؛ `null` = خانهٔ خالی، عدد = شمارهٔ روز. طول همیشه مضرب ۷ است.
     */
    fun cells(firstWeekday: Int, daysInMonth: Int): List<Int?> {
        if (daysInMonth <= 0) return emptyList()
        val lead = Math.floorMod(firstWeekday, COLUMNS)
        val slots = lead + daysInMonth
        val rows = (slots + COLUMNS - 1) / COLUMNS
        return (0 until rows * COLUMNS).map { index ->
            val day = index - lead + 1
            if (day in 1..daysInMonth) day else null
        }
    }

    fun weeks(cells: List<Int?>): List<List<Int?>> = cells.chunked(COLUMNS)
}

/** کش فهرست‌ها برای حالت بی‌اتصال — فقط رشته/JSON، بدون وابستگی به ذخیره‌گاه. */
object ListCache {
    fun key(screen:String, page:Int, extra:String = ""):String =
        if (extra.isBlank()) "cache:$screen:$page" else "cache:$screen:$page:$extra"

    fun encode(items:List<JSONObject>):String = JSONArray(items.toList()).toString()

    fun decode(raw:String?):List<JSONObject> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    /** متن «چند دقیقه پیش» برای بنر داده‌های ذخیره‌شده. */
    fun ageLabel(minutesAgo:Long):String = when {
        minutesAgo < 1 -> "همین حالا"
        minutesAgo < 60 -> "${faDigits(minutesAgo)} دقیقه پیش"
        minutesAgo < 24 * 60 -> "${faDigits(minutesAgo / 60)} ساعت پیش"
        else -> "${faDigits(minutesAgo / (24 * 60))} روز پیش"
    }
}

/** رقم‌های فارسی برای متن‌هایی که از Compose بیرون‌اند (Screens.fa به Compose وابسته است). */
fun faDigits(value:Long):String {
    val latin = value.toString()
    val out = StringBuilder(latin.length)
    for (ch in latin) out.append(if (ch in '0'..'9') ('۰' + (ch - '0')) else ch)
    return out.toString()
}
