package ir.hsai.mobile

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
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
 @Test fun fourteenCalculators() { assertEquals(14,Domain.calculators.size);assertEquals(14,Domain.calculators.map{it.id}.distinct().size) }
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
 @Test fun brandingCalendarFoldsPersianTextByUtf8Bytes(){
 val title="مهلت آزمایشی ".repeat(20)
 val lines=foldCalendarLine("SUMMARY:"+title)
 assertTrue(lines.all{it.toByteArray(Charsets.UTF_8).size<=75})
 assertEquals("SUMMARY:"+title,lines.first()+lines.drop(1).joinToString(""){it.removePrefix(" ")})
 }
 @Test fun brandingAllDayCalendarHasExclusiveEndDate(){assertTrue(calendarText("مهلت","2026-12-31").contains("DTEND;VALUE=DATE:20270101"))}

 @Test fun auditWordExportUsesAuthenticatedOfficialBinaryRoute()=runBlocking {
 val server=MockWebServer();server.start();try {
  val bytes=byteArrayOf(80,75,3,4,1,2,3)
  server.enqueue(MockResponse().setBody(okio.Buffer().write(bytes)))
  val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));api.token="test-only-token"
  assertArrayEquals(bytes,api.documentBytes(42));val req=server.takeRequest();assertEquals("/api/v1/documents/42/docx",req.path);assertEquals("Bearer test-only-token",req.getHeader("Authorization"))
 }finally{server.shutdown()}
 }
 @Test fun auditWordExportNeverRedirectsBearerToAnotherHost()=runBlocking {
 val origin=MockWebServer();val other=MockWebServer();origin.start();other.start();try {
  origin.enqueue(MockResponse().setResponseCode(302).setHeader("Location",other.url("/capture")));other.enqueue(MockResponse().setBody("not reached"))
  val api=ApiClient(origin.url("/api/v1").toString().trimEnd('/'));api.token="test-only-token"
  try{api.documentBytes(1);fail("redirect must fail")}catch(e:ApiException){assertEquals(302,e.status)}
  assertEquals(0,other.requestCount)
 }finally{origin.shutdown();other.shutdown()}
 }
 @Test fun auditWordExportRejectsJsonDisguisedAsSuccessfulFile()=runBlocking {
 val server=MockWebServer();server.start();try {
  server.enqueue(MockResponse().setBody("{\"ok\":false}"));val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));api.token="test"
  try{api.documentBytes(1);fail("expected invalid file")}catch(e:ApiException){assertEquals("INVALID_DOCUMENT",e.code)}
 }finally{server.shutdown()}
 }
 @Test fun auditWordExportBoundsChunkedResponsesToo()=runBlocking {
 val server=MockWebServer();server.start();try {
  server.enqueue(MockResponse().setChunkedBody("PK\u0003\u0004"+"x".repeat(20),4));val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'));api.token="test"
  try{api.documentBytes(1,8);fail("expected size limit")}catch(e:ApiException){assertEquals("DOCUMENT_TOO_LARGE",e.code)}
 }finally{server.shutdown()}
 }

 @Test fun internalTransportRejectsOversizeInsteadOfTruncating(){
  assertEquals("abcd",String(TransportLimits.read("abcd".byteInputStream(),4)))
  try{TransportLimits.read("abcde".byteInputStream(),4);fail("must reject")}catch(e:ApiException){assertEquals("RESPONSE_TOO_LARGE",e.code)}
 }
 @Test fun internalSseLinesAreBoundedAndUnterminatedLastLineIsPreserved(){
  val source=SseLines(okio.Buffer().writeUtf8("abc\nlast"),4);assertEquals("abc",source.read());assertEquals("last",source.read());assertNull(source.read())
  try{SseLines(okio.Buffer().writeUtf8("abcde\n"),4).read();fail("must reject")}catch(e:ApiException){assertEquals("RESPONSE_TOO_LARGE",e.code)}
 }
 @Test fun internalSsePreservesDataWhitespaceBomAndTrailingDataNewlines(){
  val parser=SseParser();parser.line("\uFEFFevent: delta");parser.line("data:   leading");parser.line("data:");val event=parser.line("")!!
  assertEquals("delta",event.name);assertEquals("  leading\n",event.data.getString("chunk"))
 }
 @Test fun internalSseLimitsWholeEventNotOnlyEachLine(){
  val parser=SseParser(5);parser.line("data: aa")
  try{parser.line("data: bb");fail("must reject")}catch(e:ApiException){assertEquals("RESPONSE_TOO_LARGE",e.code)}
 }
 @Test fun internalCameraRestoreRejectsTraversalAndMissingFiles(){
  val folder=java.nio.file.Files.createTempDirectory("hsai-camera-test").toFile()
  try{val image=java.io.File(folder,"legal-42.jpg");image.writeText("fixture");assertEquals(image.canonicalFile,PendingCamera.restore(folder,image.name));assertNull(PendingCamera.restore(folder,"../legal-42.jpg"));assertNull(PendingCamera.restore(folder,"legal-43.jpg"));assertNull(PendingCamera.restore(folder,"/tmp/legal-42.jpg"))}finally{folder.deleteRecursively()}
 }
 @Test fun internalMarkdownTablesPreserveEscapedPipesAndExtraColumns(){
  val blocks=MarkdownBlocks.parse("| عنوان | مبلغ |\n| --- | ---: |\n| الف\\|ب | 0 | اضافه |")
  val table=blocks.single() as MarkdownBlock.Table;assertEquals(listOf("الف|ب","0","اضافه"),table.rows.single());assertEquals(2,table.header.size)
 }
 @Test fun internalMarkdownFencedCodeIsNotParsedAsTableOrHtml(){
  val blocks=MarkdownBlocks.parse("```text\n<script>not executed</script>\n| a | b |\n```\nپایان")
  assertEquals("<script>not executed</script>\n| a | b |",(blocks[0] as MarkdownBlock.Code).text);assertEquals("پایان",(blocks[1] as MarkdownBlock.Line).text)
 }
 @Test fun internalReminderMigrationDoesNotCancelNewOrFinishedJobs(){assertTrue(ReminderMigration.legacy(setOf("hsai-deadline"),false));assertFalse(ReminderMigration.legacy(setOf("hsai-deadline",ReminderMigration.TAG),false));assertFalse(ReminderMigration.legacy(emptySet(),true))}
 @Test fun internalSummaryKeepsZeroAndUnknownFieldsWithoutRecalculating(){
  val data=JSONObject().put("type","court_fee").put("total_initial_cost_toman",0).put("future_field",false)
  assertEquals("total_initial_cost_toman",CalculatorResults.mainKey("court_fee",data));assertFalse(CalculatorResults.remainder(data,setOf("total_initial_cost_toman")).getBoolean("future_field"));assertNull(CalculatorResults.mainKey("diyeh",data))
 }
 @Test fun internalValidationCollectsAllFieldsInFormOrder(){
  try{validateFields("title" to {require(false){"عنوان لازم است"}},"date" to {java.time.LocalDate.parse("");Unit});fail("must reject")}catch(e:FormValidationException){assertEquals(listOf("title","date"),e.fields.keys.toList());assertEquals("عنوان لازم است",e.message)}
 }

 @Test fun internalSseAcceptsAllStandardLineEndings(){
  listOf("\n","\r\n","\r").forEach { eol->
   val reader=SseLines(okio.Buffer().writeUtf8(listOf("event: delta","data:  text","","").joinToString(eol)))
   val parser=SseParser();val events=mutableListOf<StreamEvent>()
   while(true){val line=reader.read()?:break;parser.line(line)?.let{events.add(it)}}
   assertEquals(1,events.size);assertEquals(" text",events.single().data.getString("chunk"))
  }
 }
 @Test fun internalSseTotalCountsCrlfBytes(){try{SseLines(okio.Buffer().writeUtf8("a\r\n\r\n"),4,4).let{it.read();it.read();it.read()};fail("must reject")}catch(e:ApiException){assertEquals("RESPONSE_TOO_LARGE",e.code)}}
 @Test fun internalSseRecognizesColonlessDataFields(){val parser=SseParser();parser.line("data: first");parser.line("data");assertEquals("first\n",parser.line("")!!.data.getString("chunk"))}
 @Test fun internalCancellationAfterHeadersDoesNotWaitForBody()=runBlocking {
  val server=MockWebServer();server.start()
  try {
   server.enqueue(MockResponse().setBody("{\"ok\":true,\"data\":{}}").setBodyDelay(2,java.util.concurrent.TimeUnit.SECONDS))
   val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'))
   val task=kotlinx.coroutines.CoroutineScope(coroutineContext).launch{api.request("/health",authenticated=false)}
   kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){server.takeRequest()};kotlinx.coroutines.delay(150)
   kotlinx.coroutines.withTimeout(800){task.cancel();task.join()}
  }finally{server.shutdown()}
 }
 @Test fun internalPostConnectionFailureDoesNotSilentlyRetry()=runBlocking {
  val server=MockWebServer();server.start()
  try {
   server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST));server.enqueue(MockResponse().setBody("{\"ok\":true,\"data\":{}}"))
   val api=ApiClient(server.url("/api/v1").toString().trimEnd('/'))
   api.token="test-only";assertTrue(runCatching{api.request("/chat","POST",JSONObject().put("message","fixture"))}.isFailure);assertEquals(1,server.requestCount)
  }finally{server.shutdown()}
 }

 @Test fun internalMalformedDoneCannotMarkTruncatedStreamComplete(){val parser=SseParser();parser.line("event: done");parser.line("data: {\"message_id\":");try{parser.line("");fail("must reject")}catch(e:ApiException){assertEquals("INVALID_RESPONSE",e.code)}}

 @Test fun internalEmptySuccessIsNotACompletedChatReply(){
  assertFalse(ApiContract.hasReply(ApiContract.chatMessage(JSONObject())))
  try{ApiContract.requireReply(ApiContract.chatMessage(JSONObject()));fail("must reject")}catch(e:ApiException){assertEquals("EMPTY_CHAT_RESPONSE",e.code)}
  assertTrue(ApiContract.hasReply(ApiContract.chatMessage(JSONObject().put("quick_questions",org.json.JSONArray().put(JSONObject().put("question","پرسش واقعی"))))))
 }

 @Test fun internalServerFailureIsNotProofThatChatWasNotProcessed(){assertTrue(ApiContract.definiteRejection(402));assertTrue(ApiContract.definiteRejection(422));listOf(0,200,302,408,409,499,500,503).forEach{assertFalse(ApiContract.definiteRejection(it))}}

 @Test fun internalTokensRejectUnsafeHeadersWithoutEchoingToken(){
  assertTrue(SessionTokens.headerSafe("fixture.abc_123-mock"));assertFalse(SessionTokens.headerSafe(""));assertFalse(SessionTokens.headerSafe("fixture\r\nprivate-fragment"))
  val api=ApiClient();try{api.token="fixture\r\nprivate-fragment";fail("must reject")}catch(e:IllegalArgumentException){assertFalse(e.message.orEmpty().contains("private-fragment"))};assertEquals("",api.token)
 }

 private fun tool(id:String)=Domain.calculators.first{it.id==id}
 @Test fun finalCatalogMatchesFourteenObservedServerTools(){assertEquals(14,Domain.calculators.size);assertEquals(14,Domain.calculators.map{it.id}.distinct().size);assertTrue(CalculatorResults.totals.keys.containsAll(Domain.calculators.map{it.id}))}
 @Test fun finalExpertFeeUsesExplicitNumericAmountAndKind(){val body=CalculatorContract.body(tool("expert_fee"),mapOf("claim_amount_toman" to "۵۰۰۰۰۰۰۰۰","expert_kind" to "technical"));assertEquals(500000000L,body.getLong("claim_amount_toman"));assertEquals("technical",body.getString("expert_kind"))}
 @Test fun finalArbitrationTariffOmitsHiddenAgreedAmount(){val body=CalculatorContract.body(tool("arbitration_fee"),mapOf("dispute_amount_toman" to "300000000","agreement_mode" to "tariff","agreed_fee_toman" to "not a number"));assertFalse(body.has("agreed_fee_toman"))}
 @Test fun finalArbitrationAgreementRequiresAmount(){try{CalculatorContract.body(tool("arbitration_fee"),mapOf("dispute_amount_toman" to "300000000","agreement_mode" to "agreed"));fail("must reject")}catch(e:FormValidationException){assertTrue(e.fields.containsKey("agreed_fee_toman"))}}
 @Test fun finalNotaryAcceptsObservedDeedOptions(){listOf("sale","lease","agency","settlement").forEach{kind->assertEquals(kind,CalculatorContract.body(tool("notary_tariff"),mapOf("deed_value_toman" to "8000000000","deed_kind" to kind)).getString("deed_kind"))}}
 @Test fun finalOfficeServicesSendsArrayNotCsvString(){val body=CalculatorContract.body(tool("office_services_fee"),mapOf("services" to "affidavit,petition,affidavit","copies" to "۲"));assertEquals(2,body.getJSONArray("services").length());assertEquals("petition",body.getJSONArray("services").getString(1));assertEquals(2,body.getInt("copies"))}
 @Test fun finalOfficeRejectsEmptyUnknownServicesAndFractionalCopies(){listOf("" to "1","unknown" to "1","affidavit" to "0","affidavit" to "1.5").forEach{(services,copies)->try{CalculatorContract.body(tool("office_services_fee"),mapOf("services" to services,"copies" to copies));fail("must reject")}catch(e:FormValidationException){assertTrue(e.fields.isNotEmpty())}}}
 @Test fun finalNewSummariesUseObservedServerKeysAndPreserveZero(){mapOf("expert_fee" to "payable_expert_fee_toman","arbitration_fee" to "payable_arbitration_fee_toman","notary_tariff" to "total_payable_toman","office_services_fee" to "total_payable_toman").forEach{(type,key)->assertEquals(key,CalculatorResults.mainKey(type,JSONObject().put("type",type).put(key,0)))}}
}
