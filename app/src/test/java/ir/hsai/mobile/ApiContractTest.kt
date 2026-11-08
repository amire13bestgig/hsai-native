package ir.hsai.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ApiContractTest {
 // BUG-04: عنوان و شرح ماجرا نباید در هم ادغام شوند. سرور «subject» را عنوان برگه می‌کند
 // (و در ۶۰ نویسه می‌بُرد) و شرح را از «facts_summary» می‌خواند؛ نسخهٔ قبلی شرح را داخل
 // subject می‌ریخت، پس هم عنوان خراب می‌شد و هم شرح به مدل نمی‌رسید.
 @Test fun productionDocumentSplitsTitleFromFacts(){
  val b=ApiContract.documentBody("اظهارنامه"," عنوان ","شرح")
  assertEquals("عنوان",b.getString("subject"))
  assertEquals("عنوان",b.getString("title"))
  assertEquals("شرح",b.getString("facts_summary"))
  assertEquals("شرح",b.getString("details"))
  assertFalse(b.getString("subject").contains("شرح"))}
 // BUG-05: بدون تاریخ ابلاغ واقعی، سرور هیچ مهلتی نمی‌سازد؛ کلاینت باید آن را بفرستد.
 @Test fun scannerSendsRealNoticeDateOnlyWhenGiven(){
  assertFalse(ApiContract.scannerBody("notice","متن",null).has("notice_date"))
  assertEquals("2026-10-01",ApiContract.scannerBody("notice","متن",null,"2026-10-01").getString("notice_date"))}
 // BUG-06/07: وضعیت «خوانده نشد» و «تأییدنشده» باید از پاسخ سرور تشخیص داده شود.
 @Test fun unreadableScanAndFailedVerificationAreDetected(){
  assertTrue(ApiContract.scanUnreadable(JSONObject().put("read_ok",false)))
  assertFalse(ApiContract.scanUnreadable(JSONObject().put("read_ok",true)))
  assertTrue(ApiContract.verificationFailed(JSONObject().put("verification_status","failed")))
  assertTrue(ApiContract.verificationFailed(JSONObject().put("verified",false)))
  assertFalse(ApiContract.verificationFailed(JSONObject().put("verified",true)))
  assertEquals(2,ApiContract.citationIssues(JSONObject().put("citation_issues",JSONArray().put(JSONObject().put("reference","ماده ۱۹۰")).put(JSONObject().put("citation","ماده ۱۰")))).size)}
 @Test fun missingNoticeDateDeadlineIsFlagged(){
  assertTrue(ApiContract.scanNoticeDateMissing(JSONObject().put("deadline",JSONObject().put("status","missing_notice_date"))))
  assertFalse(ApiContract.scanNoticeDateMissing(JSONObject().put("deadline",JSONObject().put("status","computed"))))}
 @Test fun productionDeadlineRequiresCaseAndDateAliases(){val b=ApiContract.deadlineBody("آزمون","2026-12-15","");assertEquals("آزمون",b.getString("case_title"));assertEquals(b.getString("due_date"),b.getString("deadline_date"))}
 @Test fun contractScannerMustNotDefaultToCourtNotice(){assertEquals("contract_risk",ApiContract.scannerBody("contract","متن",null).getString("scan_mode"))}
 @Test fun noticeScannerModeIsExplicit(){assertEquals("sana_notice",ApiContract.scannerBody("notice","متن",null).getString("scan_mode"))}
 @Test fun noImageIsNotSentAsNullString(){assertFalse(ApiContract.scannerBody("contract","متن",null).has("image_base64"))}
 @Test fun nativeSimplifyReadsActualServerKey(){assertEquals("متن ساده",ApiContract.simplifiedText(JSONObject().put("simplified_content","متن ساده")))}
 @Test fun paymentHistoryUsesAuthorityCodeAlias(){assertEquals("a-123",ApiContract.paymentAuthority(JSONObject().put("authority_code","a-123")))}
 @Test fun paginationTotalWithNoTotalPagesStopsAtLastPage(){val p=JSONObject().put("items",JSONArray().put(JSONObject())).put("pagination",JSONObject().put("page",2).put("limit",2).put("total",3));assertFalse(ApiContract.page(p,2,2).hasNext)}
 @Test fun fullServerPageDoesNotImplyMoreIfTotalEqualsLimit(){val p=JSONObject().put("items",JSONArray().put(JSONObject()).put(JSONObject())).put("pagination",JSONObject().put("page",1).put("limit",2).put("total",2));assertFalse(ApiContract.page(p,1,2).hasNext)}
 @Test fun unpaginatedRoutesAreSlicedLocally(){val a=JSONArray();repeat(5){a.put(JSONObject().put("id",it))};val d=JSONObject().put("items",a);val p=ApiContract.page(d,2,2);assertEquals(listOf(2,3),p.items.map{it.getInt("id")});assertTrue(p.hasNext);assertFalse(ApiContract.page(d,3,2).hasNext)}
 @Test fun outOfRangeLocalPageIsEmpty(){assertTrue(ApiContract.page(JSONObject().put("items",JSONArray()),99,20).items.isEmpty())}
 @Test fun stageAcceptsMessageFallback(){assertEquals("در حال بررسی",ApiContract.stageTitle(JSONObject().put("message","در حال بررسی")))}
 @Test fun structuredStreamErrorDoesNotLeakRawJson(){assertEquals("خطای روشن",ApiContract.streamError(JSONObject().put("error",JSONObject().put("code","E").put("message","خطای روشن"))))}
 @Test fun auditV53AllowsOnlyOwnedScannerDeleteRoute(){assertTrue(ApiRoutes.allowed("/scanner/123","DELETE"));assertFalse(ApiRoutes.allowed("/scanner/123","PUT"));assertFalse(ApiRoutes.allowed("/scanner/all","DELETE"))}
 @Test fun auditQuickQuestionsUseTopLevelVerificationAndIntakeFallback(){
 val q=JSONArray().put(JSONObject().put("label","پرسش ساختگی"))
 assertEquals(1,ApiContract.quickQuestions(JSONObject().put("quick_questions",q)).size)
 assertEquals(1,ApiContract.quickQuestions(JSONObject().put("verification",JSONObject().put("quick_questions",q))).size)
 assertEquals(1,ApiContract.quickQuestions(JSONObject().put("verification",JSONObject().put("intake",JSONObject().put("questions",q)))).size)
 }
 @Test fun brandingHistoryUnderstandsStoredVerificationJsonWithoutInventingData(){
 val stored=JSONObject().put("verification_json",JSONObject().put("printable_document",JSONObject().put("content","متن ساختگی" )).toString())
 assertEquals("متن ساختگی",ApiContract.verification(stored)!!.getJSONObject("printable_document").getString("content"))
 assertNull(ApiContract.verification(JSONObject()))
 assertNull(ApiContract.verification(JSONObject().put("verification_json","bad-json")))
 }
 @Test fun cleanupPresentationHidesOnlyInternalAndEmptyFieldsNotZeroOrFalse(){
 val d=JSONObject().put("id",42).put("token","mock-only").put("blank","").put("none",JSONObject.NULL).put("empty",JSONArray()).put("total_toman",0).put("valid",false).put("future_field","محفوظ")
 assertEquals(setOf("total_toman","valid","future_field"),UiPresentation.keys(d).toSet())
 assertTrue(UiPresentation.primary(d).contains("total_toman"));assertEquals(listOf("future_field"),UiPresentation.secondary(d));assertEquals(42,d.getInt("id"))
 }

 @Test fun auditNormalStreamAndHistoryDecodeTheSameRichReply(){
  val v=JSONObject().put("printable_document",JSONObject().put("title","پیش‌نویس").put("content","متن سند"))
   .put("intake",JSONObject().put("questions",JSONArray().put(JSONObject().put("question","پرسش"))))
  val citations=JSONArray().put(JSONObject().put("law_name","قانون ساختگی"))
  val base=JSONObject().put("message_id",7).put("answer","پاسخ نمونه").put("verification",v).put("cited_articles",citations).put("suggestions",JSONArray().put("ادامه نمونه"))
  val stream=JSONObject().put("message_id",7).put("verification",v).put("cited_articles",citations).put("suggestions",JSONArray().put("ادامه نمونه")).put("assistant_message",JSONObject().put("id",7).put("content","پاسخ نمونه"))
  val history=JSONObject().put("id",7).put("content","پاسخ نمونه").put("verification_json",v.toString()).put("cited_articles",citations).put("suggestions","[\"ادامه نمونه\"]")
  val normal=ApiContract.chatMessage(base)
  listOf(stream,history).forEach { data->val actual=ApiContract.chatMessage(data);assertEquals(normal.text,actual.text);assertEquals(normal.id,actual.id);assertEquals(normal.document.toString(),actual.document.toString());assertEquals(1,actual.questions.size);assertEquals(1,actual.citations.size);assertEquals(normal.suggestions,actual.suggestions)}
 }
 @Test fun auditNestedVerificationPreservesOuterFieldsAndInnerTakesPriority(){
  val data=JSONObject().put("verification",JSONObject().put("citizen_guide","راهنمای بیرونی").put("calculator",JSONObject().put("total_toman",0)))
   .put("assistant_message",JSONObject().put("content","").put("verification",JSONObject().put("citizen_guide","راهنمای درونی")))
  val message=ApiContract.chatMessage(data,"متن جریانی",listOf(JSONObject().put("law_name","مرجع")))
  assertEquals("متن جریانی",message.text);assertEquals("راهنمای درونی",message.verification!!.getString("citizen_guide"));assertEquals(0,message.verification!!.getJSONObject("calculator").getInt("total_toman"));assertEquals(1,message.citations.size)
 }
 @Test fun auditVerificationJsonObjectIsNotSilentlyLost(){assertNotNull(ApiContract.verification(JSONObject().put("verification_json",JSONObject().put("mode","document"))))}
 @Test fun auditReminderRejectsMissingAndDifferentAccounts(){assertTrue(ReminderPrivacy.sameOwner("42","42"));assertFalse(ReminderPrivacy.sameOwner("42","43"));assertFalse(ReminderPrivacy.sameOwner(null,"42"));assertFalse(ReminderPrivacy.sameOwner("42",null));assertFalse(ReminderPrivacy.sameOwner("",""))}
 @Test fun auditRejectsEncodedPathTraversalButNotEncodedLawQueries(){assertFalse(ApiRoutes.allowed("/billing/payments/%2e%2e%2fme","GET"));assertFalse(ApiRoutes.allowed("/billing/payments/%252e%252e","GET"));assertTrue(ApiRoutes.allowed("/laws?q=%2e%2e%2f","GET"))}
 @Test fun auditHistorySuggestedActionsAliasRemainsVisibleWithoutExecutingActions(){
 val data=JSONObject().put("suggested_actions",JSONArray().put("پیشنهاد اول").put(JSONObject().put("title","پیشنهاد دوم").put("url","https://example.com")))
 assertEquals(listOf("پیشنهاد اول","پیشنهاد دوم"),ApiContract.chatMessage(data).suggestions)
 }
 @Test fun auditGenericDetailsHideCredentialAndInternalPromptKeysCaseInsensitively(){
 val d=JSONObject().put("TOKEN","mock-token").put("client_secret","mock-secret").put("system_prompt","internal fixture").put("chain_of_thought","internal fixture").put("quick_summary","خلاصه قابل نمایش")
 assertEquals(listOf("quick_summary"),UiPresentation.keys(d))
 }
}
