package ir.hsai.mobile

import org.json.JSONObject

/** Compatibility with observed production responses, in addition to the published OpenAPI. */
object ApiContract {
 data class Page(val items:List<JSONObject>,val hasNext:Boolean)
 fun page(data:JSONObject,number:Int,size:Int,alternate:String=""):Page {
  val all=data.objects("items").ifEmpty { if(alternate.isNotBlank())data.objects(alternate)else emptyList() }
  val pagination=data.optJSONObject("pagination")
  if(pagination!=null) {
   val current=pagination.optInt("page",number)
   val limit=pagination.optInt("limit",size).coerceAtLeast(1)
   val next=when {
    pagination.has("total_pages")->current<pagination.optInt("total_pages")
    pagination.has("total")->current.toLong()*limit<pagination.optLong("total")
    else->all.size>=limit
   }
   return Page(all,next)
  }
  // These production list routes return the entire collection and ignore page/limit.
  val offset=(number.coerceAtLeast(1)-1).toLong()*size
  val start=offset.coerceAtMost(all.size.toLong()).toInt()
  return Page(all.drop(start).take(size),offset+size<all.size)
 }
 fun documentBody(type:String,title:String,details:String)=JSONObject()
  .put("doc_type",type).put("title",title.trim()).put("subject",title.trim()+"\n\nشرح ماجرا و خواسته:\n"+details.trim()).put("details",details.trim())
 fun deadlineBody(title:String,date:String,notes:String)=JSONObject()
  .put("title",title.trim()).put("case_title",title.trim()).put("due_date",date).put("deadline_date",date).put("notes",notes)
 fun scannerBody(kind:String,text:String,image:String?):JSONObject {
  val body=JSONObject().put("kind",kind).put("text",text.trim())
  if(kind=="contract")body.put("scan_mode","contract_risk")
  if(kind=="notice")body.put("scan_mode","sana_notice")
  if(image!=null)body.put("image_base64",image)
  return body
 }
 fun simplifiedText(data:JSONObject):String = listOf("simplified_content","content","simplified","simplified_answer","answer","text")
  .firstNotNullOfOrNull { data.str(it).takeIf(String::isNotBlank) } ?: throw ApiException("متن ساده‌شده در خروجی نیست.")
 fun quickQuestions(data:JSONObject):List<JSONObject> {
  val v=data.optJSONObject("verification")
  return data.objects("quick_questions").ifEmpty{v?.objects("quick_questions").orEmpty()}.ifEmpty{v?.optJSONObject("intake")?.objects("questions").orEmpty()}
 }
 fun paymentAuthority(payment:JSONObject)=payment.str("authority",payment.str("authority_code"))
 fun stageTitle(data:JSONObject)=data.str("title",data.str("message",data.optJSONObject("stage")?.str("title").orEmpty()))
 fun streamError(data:JSONObject):String = data.optJSONObject("error")?.str("message")?.takeIf(String::isNotBlank)
  ?: data.str("message").ifBlank { data.str("error").ifBlank { "دریافت پاسخ ناموفق بود." } }
}
