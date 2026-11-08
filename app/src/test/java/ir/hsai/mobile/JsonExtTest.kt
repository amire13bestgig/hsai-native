package ir.hsai.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * تست‌های افزونه‌های JSON. این کلاس عمداً به اندروید/Robolectric نیاز ندارد، چون
 * JsonExt.kt از AppViewModel جدا شده است (همان کد در verify/run.sh هم اجرا می‌شود).
 */
class JsonExtTest {
 // BUG-11: نبودِ کلید آرایه نباید null بدهد؛ قبلاً نوع پلتفرمیِ JSONArray! این را از
 // کامپایلر پنهان می‌کرد و مسیرهایی مثل page()/tickets با NPE بسته می‌شدند.
 @Test fun missingArrayIsEmptyListNotNpe(){
  assertTrue(JSONObject().objects("items").isEmpty())
  assertTrue(JSONObject().put("items",JSONObject.NULL).objects("items").isEmpty())
  assertEquals(2,JSONObject().put("items",org.json.JSONArray().put(JSONObject()).put(JSONObject())).objects("items").size)
 }
 @Test fun pageWithoutItemsIsEmptyAndFinal(){
  val p=ApiContract.page(JSONObject(),1,20)
  assertTrue(p.items.isEmpty());assertFalse(p.hasNext)
 }
 @Test fun strFallsBackOnNullAndBlank(){
  assertEquals("پیش‌فرض",JSONObject().put("x",JSONObject.NULL).str("x","پیش‌فرض"))
  assertEquals("پیش‌فرض",JSONObject().str("missing","پیش‌فرض"))
  assertEquals("مقدار",JSONObject().put("x","مقدار").str("x"))
 }
 @Test fun safeLongRejectsMissingAndNegative(){
  assertNull(JSONObject().safeLong("id"))
  assertNull(JSONObject().put("id",-1).safeLong("id"))
  assertEquals(7L,JSONObject().put("id",7).safeLong("id"))
 }
}
