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
 fun verification(data:JSONObject):JSONObject? = data.optJSONObject("verification")
  ?: data.optJSONObject("verification_json")
  ?: data.str("verification_json").takeIf{it.isNotBlank()}?.let{runCatching{JSONObject(it)}.getOrNull()}
 fun quickQuestions(data:JSONObject):List<JSONObject> {
  val v=verification(data)
  return data.objects("quick_questions").ifEmpty{v?.objects("quick_questions").orEmpty()}.ifEmpty{v?.optJSONObject("intake")?.objects("questions").orEmpty()}
 }
 fun paymentAuthority(payment:JSONObject)=payment.str("authority",payment.str("authority_code"))
 fun stageTitle(data:JSONObject)=data.str("title",data.str("message",data.optJSONObject("stage")?.str("title").orEmpty()))
 fun streamError(data:JSONObject):String = data.optJSONObject("error")?.str("message")?.takeIf(String::isNotBlank)
  ?: data.str("message").ifBlank { data.str("error").ifBlank { "دریافت پاسخ ناموفق بود." } }

 /** One decoder for normal replies, SSE done and reopened assistant history. No fabricated metadata. */
 fun chatMessage(data:JSONObject,fallbackText:String="",fallbackCitations:List<JSONObject> = emptyList()):ChatMessage {
  val message=data.optJSONObject("assistant_message")?:data
  val outer=verification(data);val inner=verification(message)
  val v=if(outer==null&&inner==null)null else JSONObject().also { target->
   listOfNotNull(outer,inner).forEach { value->value.keys().forEach { key->if(!value.isNull(key))target.put(key,value.opt(key)) } }
  }
  val text=listOf(message.str("content"),message.str("answer"),data.str("answer"),fallbackText).firstOrNull{it.isNotBlank()}.orEmpty()
  val questions=quickQuestions(message).ifEmpty{quickQuestions(data)}.ifEmpty{if(v==null)emptyList()else quickQuestions(JSONObject().put("verification",v))}
  return ChatMessage("assistant",text,message.safeLong("id")?:message.safeLong("message_id")?:data.safeLong("message_id"),
   message.objects("cited_articles").ifEmpty{data.objects("cited_articles")}.ifEmpty{fallbackCitations},
   document=message.optJSONObject("printable_document")?:data.optJSONObject("printable_document")?:v?.optJSONObject("printable_document"),
   feedback=message.str("feedback"),verification=v,questions=questions,suggestions=suggestions(message).ifEmpty{suggestions(data)})
 }
 fun definiteRejection(status:Int)=status in 400..499&&status !in setOf(408,409,499)
 fun verificationKeys(data:JSONObject)=UiPresentation.keys(data).filterNot{it in setOf("printable_document","quick_questions","intake","mode","confidence")}
 fun hasReply(message:ChatMessage)=message.text.isNotBlank()||message.document?.length()?.let{it>0}==true||message.verification?.let{verificationKeys(it).isNotEmpty()}==true||message.questions.isNotEmpty()||message.citations.isNotEmpty()||message.suggestions.isNotEmpty()
 fun requireReply(message:ChatMessage):ChatMessage {if(!hasReply(message))throw ApiException("پاسخ قابل نمایش دریافت نشد؛ پیش از ارسال دوباره تاریخچه را بررسی کنید.",0,"EMPTY_CHAT_RESPONSE");return message}
 fun suggestions(data:JSONObject):List<String> {
  val lists=listOf("suggestions","suggested_actions").mapNotNull { key->
   data.optJSONArray(key)?:data.str(key).takeIf{it.isNotBlank()}?.let{runCatching{org.json.JSONArray(it)}.getOrNull()}
  }
  return lists.flatMap { list->(0 until list.length()).mapNotNull { i->when(val value=list.opt(i)) {
   is String->value.takeIf{it.isNotBlank()}
   is JSONObject->listOf("text","question","label","title").firstNotNullOfOrNull{key->value.str(key).takeIf{it.isNotBlank()}}
   else->null
  }}}.distinct()
 }
}
