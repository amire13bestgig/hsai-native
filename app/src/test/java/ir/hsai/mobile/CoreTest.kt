package ir.hsai.mobile

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CoreTest {
 @Test fun persianDigits() { assertEquals("1234567890",Validation.digits("۱۲۳۴۵۶۷۸۹۰")) }
 @Test fun arabicDigitsAndSeparators() { assertEquals("12345",Validation.digits("١٢٬٣٤٥")) }
 @Test fun iranPhone() { assertEquals("09121234567",Validation.phone("+۹۸۹۱۲۱۲۳۴۵۶۷")) }
 @Test fun domesticPhone() { assertEquals("09121234567",Validation.phone("۰۹۱۲۱۲۳۴۵۶۷")) }
 @Test(expected=IllegalArgumentException::class) fun rejectsBadPhone() { Validation.phone("123") }
 @Test fun tomanNumber() { assertEquals(150000000L,Validation.number("amount","۱۵۰٬۰۰۰٬۰۰۰")) }
 @Test(expected=IllegalArgumentException::class) fun rejectsNegative() { Validation.number("amount","-1") }
 @Test(expected=IllegalArgumentException::class) fun rejectsScientific() { Validation.number("amount","1e8") }
 @Test(expected=IllegalArgumentException::class) fun rejectsMonth13() { Validation.number("due_month","13") }
 @Test(expected=IllegalArgumentException::class) fun rejectsFractionalCount() { Validation.number("sons_count","1.5") }
 @Test(expected=IllegalArgumentException::class) fun rejectsLargePercent() { Validation.number("injury_percent","101") }
 @Test(expected=IllegalArgumentException::class) fun rejectsUnsafeLargeNumber() { Validation.number("amount","9999999999999999999") }
 @Test fun nationalIdRepeatedRejected() { assertFalse(Validation.nationalId("1111111111")) }
 @Test fun missingNationalIdRejected() { assertFalse(Validation.nationalId("123")) }
 @Test fun routesAllowReadQuery() { assertTrue(ApiRoutes.allowed("/laws?q=%DA%86%DA%A9&page=2","GET")) }
 @Test fun routesRejectWrongMethod() { assertFalse(ApiRoutes.allowed("/health","POST")) }
 @Test fun routesRejectTraversal() { assertFalse(ApiRoutes.allowed("/documents/../me","GET")) }
 @Test fun routesRejectExternal() { assertFalse(ApiRoutes.allowed("https://evil.example/me","GET")) }
 @Test fun routesRejectNonNumericId() { assertFalse(ApiRoutes.allowed("/documents/abc","DELETE")) }
 @Test fun routesAllowCheckoutAndLookup() { assertTrue(ApiRoutes.allowed("/billing/checkout","POST"));assertTrue(ApiRoutes.allowed("/billing/payments/AUTH-123","GET")) }
 @Test fun tenCalculators() { assertEquals(10,Domain.calculators.size);assertEquals(10,Domain.calculators.map{it.id}.distinct().size) }
 @Test fun inheritanceUsesProductionSpouse() { val f=Domain.calculators.first{it.id=="inheritance"}.fields.map{it.key};assertTrue("spouse_type" in f);assertFalse("wife_count" in f) }
 @Test fun inheritanceTaxUsesProductionAssets() { val f=Domain.calculators.first{it.id=="inheritance_tax"}.fields.map{it.key};assertTrue("bank_deposits_toman" in f);assertTrue("real_estate_tax_value_toman" in f);assertFalse("estate_value_toman" in f) }
 @Test fun sseDelta() { val p=SseParser();p.line("event: delta");p.line("data: {\"chunk\":\"سلام\"}");val e=p.line("");assertEquals("delta",e!!.name);assertEquals("سلام",e.data.getString("chunk")) }
 @Test fun sseMultilineAndComments() {val p=SseParser();p.line(": keep alive");p.line("event: done");p.line("data: {\"conversation_id\":2,");p.line("data: \"message_id\":3}");assertEquals(2,p.line("")!!.data.getInt("conversation_id"));assertNull(p.line(""))}
 @Test fun sseResetsEvent() {val p=SseParser();p.line("event: stage");p.line("data: {}");assertEquals("stage",p.line("")!!.name);p.line("data: {}");assertEquals("message",p.line("")!!.name)}
 @Test fun calendarIsAllDayAndEscapesInjection() {val s=calendarText("test\nBEGIN:EVIL,;","2027-01-12");assertTrue(s.contains("DTSTART;VALUE=DATE:20270112"));assertFalse(s.contains("\r\nBEGIN:EVIL"));assertTrue(s.contains("TRIGGER:-P1D"))}
 @Test fun apiReadsEnvelope()=runBlocking {val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setBody("{\"ok\":true,\"data\":{\"status\":\"healthy\"}}"));val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));assertEquals("healthy",api.request("/health",authenticated=false).getString("status"));assertEquals("/api/v1/health",server.takeRequest().path)}finally{server.shutdown()}}
 @Test fun apiSendsBearerAndJsonBody()=runBlocking {val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setBody("{\"ok\":true,\"data\":{\"answer\":\"ok\"}}"));val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));api.token="test-only-jwt";api.request("/chat","POST",JSONObject().put("message","سلام"));val req=server.takeRequest();assertEquals("Bearer test-only-jwt",req.getHeader("Authorization"));assertEquals("POST",req.method);assertEquals("سلام",JSONObject(req.body.readUtf8()).getString("message"))}finally{server.shutdown()}}
 @Test fun apiDoesNotInventSuccessOnError()=runBlocking {val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setResponseCode(402).setBody("{\"ok\":false,\"error\":{\"code\":\"NO_SUBSCRIPTION\",\"message\":\"اشتراک لازم است\"}}"));val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));api.token="test";try{api.request("/chat","POST",JSONObject());fail("Expected error")}catch(e:ApiException){assertEquals(402,e.status);assertEquals("NO_SUBSCRIPTION",e.code)}}finally{server.shutdown()}}
 @Test fun apiMissingTokenIsLocalError()=runBlocking {try{ApiClient().request("/me");fail("Expected auth error")}catch(e:ApiException){assertEquals("LOGIN_REQUIRED",e.code)}}
 @Test fun apiRejectsInvalidJson()=runBlocking {val server=MockWebServer();server.start();try{server.enqueue(MockResponse().setBody("<html>error</html>"));try{ApiClient(server.url("/api/v1").toString().trimEnd('/')).request("/health",authenticated=false);fail("Expected parse error")}catch(e:ApiException){assertEquals("INVALID_RESPONSE",e.code)}}finally{server.shutdown()}}
}
