package ir.hsai.mobile

import org.json.JSONObject

/**
 * افزونه‌های خواندنِ JSON. این سه تابع عمداً در فایل مستقل و بدون وابستگی اندروید نگه داشته شده‌اند
 * تا با هارنس اجرایی (verify/) تست شوند؛ پیش‌تر داخل AppViewModel.kt بودند و فقط با Robolectric
 * قابل آزمون بودند.
 */
fun JSONObject.str(key:String,default:String=""):String = if(isNull(key)) default else optString(key,default)

// BUG-11: نوع اعلام‌شده غیرnullable بود ولی زنجیرهٔ ?.let در نبودِ آرایه null برمی‌گرداند؛
// نوع پلتفرمیِ JSONArray! این را از کامپایلر پنهان می‌کرد و هر پاسخی که کلید آرایه را نداشت
// (مثلاً {"ok":true,"data":{}}) در .ifEmpty/.plus/.filter با NPE برنامه را می‌بست.
fun JSONObject.objects(key:String):List<JSONObject> = optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList()

fun JSONObject.safeLong(key:String):Long? = if(has(key)&&!isNull(key)&&optLong(key,-1)>=0) optLong(key) else null

/** عناصر آرایه به‌صورت Any (رشته/شیء) — برای فهرست‌هایی که ترکیبی‌اند (مثل deadly_mistakes). */
fun JSONObject.array(key:String):List<Any> = optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.opt(it) } } ?: emptyList()

/** لیست رشته‌ای با پشتیبانی از آرایهٔ شیء‌هایی که متن را در کلیدهای رایج دارند. */
fun JSONObject.textList(key:String):List<String> = array(key).mapNotNull { value ->
 when(value){ is String->value.takeIf{it.isNotBlank()}; is JSONObject->listOf("text","title","label","name").firstNotNullOfOrNull{v->value.str(v).takeIf{it.isNotBlank()}}; else->null }
}
