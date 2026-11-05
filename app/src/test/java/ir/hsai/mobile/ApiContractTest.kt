package ir.hsai.mobile

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ApiContractTest {
 @Test fun productionDocumentRequiresSubjectAlias(){val b=ApiContract.documentBody("اظهارنامه"," عنوان ","شرح");assertTrue(b.getString("subject").startsWith("عنوان\n"));assertTrue(b.getString("subject").contains("شرح"));assertEquals("شرح",b.getString("details"))}
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
}
