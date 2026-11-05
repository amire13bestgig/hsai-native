package ir.hsai.mobile

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.json.JSONObject
import java.io.File

/** Opt-in read-only native transport check. The token lives outside source and is never logged. */
class AuthenticatedApiTest {
 @Test fun nativeClientReadsAuthenticatedContracts()=runBlocking {
  val path=System.getenv("HSAI_AUTH_TOKEN_FILE")
  assumeTrue("Requires a private temporary token file",!path.isNullOrBlank())
  val api=ApiClient();api.token=JSONObject(File(path!!).readText()).getString("token")
  val me=api.request("/me");assertTrue(me.has("user"));assertTrue(me.has("subscription"))
  for(route in listOf("/consultations?limit=2&page=1","/documents","/scanner","/deadlines","/billing/payments","/tickets")) {
   assertNotNull(route,api.request(route).optJSONArray("items"))
  }
 }
}
