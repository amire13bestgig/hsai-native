package ir.hsai.mobile

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * «خواندن پاسخ» با موتور گفتار دستگاه.
 *
 * سایت این کار را با `window.speechSynthesis` انجام می‌دهد (ChatAgentPage.jsx → handleSpeakMessage).
 * در اندروید معادل بومی‌اش `TextToSpeech` است؛ همان دکمه، همان رفتار:
 *   • اگر موتور/زبان فارسی نبود، به‌جای سکوت، پیام روشن به کاربر داده می‌شود.
 *   • با شروع پاسخ تازه یا خروج از گفتگو، پخش قبلی متوقف نمی‌شود مگر کاربر «توقف» بزند
 *     (هم‌رفتار با سایت که تا پایان متن یا توقف دستی ادامه می‌دهد).
 * هیچ مجوزی لازم ندارد؛ سرویس گفتار خودِ سیستم است.
 */
object Speaker {
    /** آیا همین حالا چیزی در حال خواندن است؟ (برای toggle دکمهٔ «خواندن/توقف») */
    val speaking = kotlinx.coroutines.flow.MutableStateFlow(false)
    private var engine: TextToSpeech? = null
    private var ready = false
    private var failed = false
    private var pending: Pair<String, () -> Unit>? = null
    /** سرعت خواندن (۱٫۰ = معمولی). از صفحهٔ تنظیمات قابل تغییر است. */
    @Volatile var rate: Float = 1f

    private fun ensure(context: Context) {
        if (engine != null || failed) return
        val app = context.applicationContext
        engine = TextToSpeech(app) { status ->
            synchronized(this) {
                if (status != TextToSpeech.SUCCESS) {
                    failed = true
                    engine?.shutdown()
                    engine = null
                    val job = pending
                    pending = null
                    job?.second?.invoke()
                } else {
                    val tts = engine
                    // اگر سازنده هنوز مقداردهی نشده باشد، کاری نمی‌کنیم؛ speak() بعدی وضعیت را می‌بیند.
                    val result = tts?.setLanguage(Locale("fa", "IR")) ?: TextToSpeech.LANG_AVAILABLE
                    if (tts != null && (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                        // موتور فارسی ندارد: صریح اعلام می‌کنیم، به‌جای خواندن با تلفظ اشتباه.
                        failed = true
                        tts?.shutdown()
                        engine = null
                        val job = pending
                        pending = null
                        job?.second?.invoke()
                    } else {
                        ready = true
                        tts?.setSpeechRate(0.95f)
                        val job = pending
                        pending = null
                        if (job != null) queue(job.first)
                    }
                }
            }
        }
    }

    private fun queue(text: String) {
        val tts = engine ?: return
        tts.setSpeechRate(rate.coerceIn(0.7f, 1.6f))
        // FIX(ux): «توقف خواندن» قبلاً همیشه دیده می‌شد حتی وقتی چیزی خوانده نمی‌شد،
        // و پایان خودکار خواندن هم وضعیت دکمه را برنمی‌گرداند. اکنون وضعیت واقعی گزارش می‌شود.
        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { speaking.value = true }
            override fun onDone(utteranceId: String?) { speaking.value = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { speaking.value = false }
        })
        speaking.value = true
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hsai-answer")
    }

    /** متن را می‌خواند؛ اگر گفتار در دسترس نباشد onUnavailable صدا زده می‌شود. */
    fun speak(context: Context, text: String, onUnavailable: () -> Unit = {}) {
        if (text.isBlank()) return
        synchronized(this) {
            if (failed) { onUnavailable(); return }
            ensure(context)
            if (ready) queue(text) else pending = text to onUnavailable
        }
    }

    fun stop() {
        speaking.value = false
        engine?.stop()
    }

    fun shutdown() {
        synchronized(this) {
            pending = null
            ready = false
            engine?.stop()
            engine?.shutdown()
            engine = null
        }
    }
}
