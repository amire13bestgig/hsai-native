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
 // FIX(documents): عنوانِ برگه و شرح ماجرا دو فیلد جدا هستند. نسخهٔ قبلی شرح را داخل «موضوع»
 // می‌ریخت، پس در برگهٔ تولیدی عنوان تبدیل به یک بلوک چندخطی می‌شد و «شرح» هم به‌عنوان موضوع
 // به مدل می‌رفت. خادم (v66) هم subject و هم details/facts_summary را می‌پذیرد.
 fun documentBody(type:String,title:String,details:String)=JSONObject()
  .put("doc_type",type)
  .put("title",title.trim())
  .put("subject",title.trim())
  .put("details",details.trim())
  .put("facts_summary",details.trim())
 fun deadlineBody(title:String,date:String,notes:String)=JSONObject()
  .put("title",title.trim()).put("case_title",title.trim()).put("due_date",date).put("deadline_date",date).put("notes",notes)
 /**
  * FIX(scanner): تاریخ ابلاغ اختیاری اما مهم است. سرور پس از v66 بدون تاریخ معتبر هیچ
  * مهلتی نمی‌سازد؛ پس اگر کاربر تاریخ را بداند باید همان را بفرستیم تا سررسید واقعی
  * محاسبه شود (پیش‌تر این فیلد هرگز ارسال نمی‌شد و ابزار مهلت عملاً بی‌اثر بود).
  */
 fun scannerBody(kind:String,text:String,image:String?,noticeDate:String=""):JSONObject {
  val body=JSONObject().put("kind",kind).put("text",text.trim())
  if(kind=="contract")body.put("scan_mode","contract_risk")
  if(kind=="notice")body.put("scan_mode","sana_notice")
  if(image!=null)body.put("image_base64",image)
  if(noticeDate.isNotBlank())body.put("notice_date",noticeDate.trim())
  return body
 }

 /** تازه در v66: سرور وقتی برگه خوانده نشود read_ok=false برمی‌گرداند و هیچ عنوان/مهلتی نمی‌سازد. */
 fun scanUnreadable(analysis:JSONObject?):Boolean = analysis!=null && analysis.has("read_ok") && !analysis.isNull("read_ok") && !analysis.optBoolean("read_ok",true)
 fun scanNoticeDateMissing(analysis:JSONObject?):Boolean = analysis?.optJSONObject("deadline")?.str("status") in setOf("missing_notice_date","invalid_notice_date")
 /** تازه در v66: وضعیت واقعی راستی‌آزمایی استنادها؛ verified=false یعنی پاسخ تأیید نشده است. */
 fun verificationFailed(v:JSONObject?):Boolean = v!=null && (v.str("verification_status")=="failed" || (v.has("verified") && !v.isNull("verified") && !v.optBoolean("verified",true)))
 fun citationIssues(v:JSONObject?):List<JSONObject> { val a=v?.optJSONArray("citation_issues")?:return emptyList();return (0 until a.length()).mapNotNull{a.optJSONObject(it)} }
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
 /** شناسهٔ گفتگو در هر دو قرارداد: `/api/v1` مقدار تخت می‌دهد و `/api/chat` شیء `conversation`. */
 fun conversationId(data:JSONObject):Long? = data.safeLong("conversation_id") ?: data.optJSONObject("conversation")?.safeLong("id")
 fun isNewConversation(data:JSONObject)=data.optBoolean("is_new_conversation",false)
 /** رویداد `quota` فقط در قرارداد وب هست: سهمیهٔ رایگان پس از هر پیام. */
 fun quotaEvent(data:JSONObject):JSONObject? = freeQuota(data)
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
 /**
  * FIX(chat-parity): رویداد `error` جویبار SSE هیچ کد HTTP ندارد؛ سرور فقط `code` می‌فرستد.
  * نسخهٔ قبلی همهٔ این خطاها را «وضعیت نامشخص» می‌دید و کاربر را به جریان «ارسال دوباره»
  * می‌برد، در حالی که وب‌سایت همان کد را به ۴۰۲ نگاشت می‌کند و پنجرهٔ ارتقای اشتراک را باز
  * می‌کند. اینجا همان نگاشت، تک‌جا و آزمون‌پذیر.
  */
 fun quotaStatus(code:String):Int = when {
  code.isEmpty()->0
  code.contains("SUBSCRIPTION")||code.contains("LIMIT")||code.contains("QUOTA")->402
  code=="VALIDATION_ERROR"->400
  code in setOf("NOT_FOUND","FORBIDDEN","UNAUTHORIZED")->if(code=="NOT_FOUND")404 else 403
  else->0
 }
 fun isQuotaCode(code:String)=quotaStatus(code)==402
 fun streamErrorCode(data:JSONObject):String = data.str("code").ifBlank { data.optJSONObject("error")?.str("code").orEmpty() }
 /** سهمیهٔ رایگان ماهانهٔ مهمان/بدون‌اشتراک — وب‌سایت آن را در سایدبار نشان می‌دهد. */
 fun freeQuota(data:JSONObject):JSONObject? = data.optJSONObject("free_quota") ?: data.optJSONObject("quota")?.takeIf { it.has("remaining") }
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
