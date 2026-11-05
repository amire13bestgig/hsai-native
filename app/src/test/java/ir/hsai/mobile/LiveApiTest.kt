package ir.hsai.mobile
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in live PUBLIC API tests. Never creates chat, users, documents or payments. */
class LiveApiTest {
 @Test fun nativeClientPublicEndpoints()=runBlocking {
  assumeTrue("Set HSAI_LIVE_TESTS=1 to run live public checks",System.getenv("HSAI_LIVE_TESTS")=="1")
  val api=ApiClient()
  assertEquals("healthy",api.request("/health",authenticated=false).getString("status"))
  assertTrue(api.request("/config",authenticated=false).has("calculators"))
  assertTrue(api.request("/billing/plans",authenticated=false).getJSONArray("items").length()>0)
  assertTrue(api.request("/laws?q=%DA%86%DA%A9&limit=1&page=1",authenticated=false).has("items"))
 }
 @Test fun nativeClientAllTenCalculators()=runBlocking {
  assumeTrue("Set HSAI_LIVE_TESTS=1 to run live calculator checks",System.getenv("HSAI_LIVE_TESTS")=="1")
  val api=ApiClient();val samples=JSONArray(javaClass.getResource("/live-calculator-inputs.json")!!.readText())
  assertEquals(10,samples.length())
  for(i in 0 until samples.length()){
   val input=samples.getJSONObject(i);val result=api.request("/calculators/compute","POST",input,false)
   assertEquals(input.getString("type"),result.getString("type"))
   if(input.getString("type")=="inheritance")assertTrue(result.getJSONObject("shares").getLong("spouse_toman")>0)
   if(input.getString("type")=="inheritance_tax")assertTrue(result.getLong("total_inheritance_tax_toman")>0)
  }
 }
}
