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
 /**
  * آیا پرداخت قطعاً تسویه شده است؟ فقط برای پاک‌کردن «شناسهٔ پرداخت معلق» استفاده می‌شود؛
  * وضعیت نامشخص (pending/unknown) عمداً تسویه‌شده حساب نمی‌شود.
  */
 fun paymentSettled(data:JSONObject):Boolean {
  val payment=data.optJSONObject("payment")?:data.optJSONObject("data")?:data
  val status=payment.str("status",data.str("status")).lowercase()
  if(status in setOf("paid","settled","success","successful","verified","completed","active"))return true
  return payment.optBoolean("paid",false)||data.optBoolean("paid",false)
 }
 /** شناسهٔ گفتگو در هر دو قرارداد: `/api/v1` مقدار تخت می‌دهد و `/api/chat` شیء `conversation`. */
 fun conversationId(data:JSONObject):Long? = data.safeLong("conversation_id") ?: data.optJSONObject("conversation")?.safeLong("id")
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
 // ── هوشمندی‌های پاسخ (همه از میدان `verification` پاسخ سرور؛ هیچ‌کدام ساختگی نیست) ──
 /** حالت پاسخ: rag / calculator_and_rag / document / conversational / quick_question / scanner */
 fun mode(v:JSONObject?):String = v?.str("mode").orEmpty()
 fun hideBadge(v:JSONObject?):Boolean = v?.optBoolean("hide_badge",false)==true
 /** کارت محاسبهٔ قانونی: {title, items:[{label,value}]} */
 fun calcCard(v:JSONObject?):JSONObject? = v?.optJSONObject("calc_card")?.takeIf{it.objects("items").isNotEmpty()}
 /** کارت آرام‌سازی ابلاغیهٔ ثنا: {arrest_risk, attendance, deadline_summary} */
 fun sanaReassurance(v:JSONObject?):JSONObject? = v?.optJSONObject("sana_reassurance")?.takeIf{it.length()>0}
 /** راهنمای عملی شهروند: {quick_summary, deadly_mistakes, action_plan, evidence_meter, path_comparison} */
 fun citizenGuide(v:JSONObject?):JSONObject? = v?.optJSONObject("citizen_guide")?.takeIf{it.length()>0}
 fun quickSummary(v:JSONObject?):JSONObject? = citizenGuide(v)?.optJSONObject("quick_summary")?.takeIf{it.length()>0}
 /** متن بازنویسی‌شدهٔ «ساده‌تر بگو» — همان کلیدی که سایت رندر می‌کند. */
 fun riskScore(v:JSONObject?):Int = v?.optInt("risk_score",0) ?: 0
 fun redFlags(v:JSONObject?):List<JSONObject> = v?.objects("red_flags").orEmpty()
 /** شمارش استنادهای واقعی برای منطق «برچسب حالت» مثل سایت. */
 fun modeBadgeHidden(v:JSONObject?,cited:Int,hasDocument:Boolean):Boolean {
  if(v==null)return true
  if(hideBadge(v))return true
  val m=mode(v)
  if(m=="conversational"||m=="quick_question")return true
  return (m.isBlank()||m=="rag")&&cited==0&&!hasDocument
 }
 fun definiteRejection(status:Int)=status in 400..499&&status !in setOf(408,409,499)
 fun verificationKeys(data:JSONObject)=UiPresentation.keys(data).filterNot{it in setOf("printable_document","quick_questions","intake","mode","confidence","calc_card","sana_reassurance","citizen_guide","hide_badge","simplified_content","stages","competent_authority","authority","risk_score","red_flags","scan_mode")}
 /**
  * «پاسخ قابل نمایش» — کارت‌های هوشمندی هم محتوا هستند.
  * (وقتی این کارت‌ها از چاپ خام `verification` جدا شدند، باید اینجا هم شمرده شوند،
  * وگرنه پاسخی که فقط کارت محاسبه/راهنما دارد «خالی» تلقی می‌شد.)
  */
 fun hasReply(message:ChatMessage):Boolean {
  val v=message.verification
  if(message.text.isNotBlank())return true
  if(message.simplified?.isNotBlank()==true)return true
  if(message.document?.length()?.let{it>0}==true)return true
  if(message.questions.isNotEmpty()||message.citations.isNotEmpty()||message.suggestions.isNotEmpty())return true
  if(v==null)return false
  return verificationKeys(v).isNotEmpty()||calcCard(v)!=null||sanaReassurance(v)!=null||citizenGuide(v)!=null||mode(v).isNotBlank()
 }
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

 // ═══════════════════════════════════════════════════════════════════════════
 // دور پنجم — قراردادهای همان قابلیت‌هایی که وب‌سایت داشت و اپ نداشت.
 // همهٔ این توابع خالص‌اند (بدون اندروید) تا در verify/Harness.kt آزمون شوند.
 // ═══════════════════════════════════════════════════════════════════════════

 /** «قوانین و آراء مرتبط» از `GET /laws/:id/relations`؛ هیچ‌وقت null نمی‌دهد. */
 fun relations(data:JSONObject):List<JSONObject> = data.objects("relations")
 fun relationOutgoing(row:JSONObject,articleId:Long):Boolean = row.safeLong("source_article_id")==articleId
 fun relationLaw(row:JSONObject,articleId:Long):String =
  if(relationOutgoing(row,articleId))row.str("target_law_name")else row.str("source_law_name")
 fun relationType(row:JSONObject):String = row.str("relation_type",row.str("edge_type"))
 fun relationNote(row:JSONObject):String = row.str("note",row.str("description"))
 /**
  * عنوان سمت دیگر رابطه. اگر سرور عنوان مادهٔ مقصد را join نکرده باشد،
  * به «نام قانون + شمارهٔ ماده» برمی‌گردیم و اگر آن هم نبود، متن رابطه.
  */
 fun relationTitle(row:JSONObject,articleId:Long):String {
  val outgoing=relationOutgoing(row,articleId)
  val direct=if(outgoing)row.str("target_title")else row.str("source_title")
  if(direct.isNotBlank())return direct
  val law=(if(outgoing)row.str("target_law_name")else row.str("source_law_name")).ifBlank { row.str("target_law_name").ifBlank { row.str("source_law_name") } }
  val num=if(outgoing)row.str("target_article_number")else row.str("source_article_number")
  val composed=buildString {
   if(law.isNotBlank())append(law)
   if(num.isNotBlank()){if(isNotEmpty())append(" — ");append("ماده ");append(num)}
  }
  return composed.ifBlank { relationNote(row).ifBlank { "مادهٔ مرتبط" } }
 }

 /** صورت‌حساب `/billing/my-billing` با کلیدهای همیشه‌حاضر (بدون NPE در UI). */
 fun billing(data:JSONObject):JSONObject = JSONObject()
  .put("active_subscription",data.optJSONObject("active_subscription")?:JSONObject())
  .put("quota",data.optJSONObject("quota")?:JSONObject())
  .put("subscription_history",data.optJSONArray("subscription_history")?:org.json.JSONArray())
  .put("payments",data.optJSONArray("payments")?:org.json.JSONArray())

 /** وضعیت لغو تمدید خودکار: سرور هم بولین و هم ۰/۱ برمی‌گرداند. */
 fun autoRenewCancelled(subscription:JSONObject?):Boolean =
  subscription!=null&&(subscription.optBoolean("cancel_at_period_end",false)||subscription.optInt("cancel_at_period_end",0)==1)
 fun autoRenewReason(subscription:JSONObject?):String = subscription?.str("notes").orEmpty()

 /** تأیید ایمیل: سرور ایمیل تأییدنشده را با ۰ یا false برمی‌گرداند. */
 fun emailVerified(user:JSONObject?):Boolean =
  user!=null&&(user.optBoolean("email_verified",false)||user.optInt("email_verified",0)==1)

 /** nonce دستگاه برای جریان ورود گوگل؛ ۳۶ نویسهٔ هگز (۱۶..۶۴ و امن با قاعدهٔ سرور). */
 fun newHandoffNonce():String {
  val bytes=ByteArray(18);java.security.SecureRandom().nextBytes(bytes)
  val digits="0123456789abcdef";val out=StringBuilder(36)
  for(b in bytes){val v=b.toInt() and 0xff;out.append(digits[v shr 4]);out.append(digits[v and 0x0f])}
  return out.toString()
 }
 fun handoffNonceValid(nonce:String):Boolean = Regex("^[A-Za-z0-9_-]{16,64}$").matches(nonce)
 /** برگهٔ دست‌دهی: امضاشدهٔ base64url با یک نقطه؛ فقط شکل آن بررسی می‌شود (امضا را سرور می‌سنجد). */
 fun handoffTokenShapeOk(token:String):Boolean = token.length in 16..1024 && token.none{it.isWhitespace()}
 /** نشانی شروع ورود با گوگل در مرورگر — همان قراردادی که سرور وب برای اپ می‌شناسد. */
 /**
  * نشانی مرورگری ورود با گوگل برای اپ — مسیر اختصاصی API، جدا از جریان وب‌سایت.
  * سرور پس از تأیید گوگل به `hsai://auth/google?handoff=…&nonce=…` برمی‌گردد و
  * اندروید خودکار همین اپ را باز می‌کند (intent-filter در AndroidManifest).
  */
fun googleStartUrl(apiOrigin:String,nonce:String,path:String=GOOGLE_APP_SIGNIN_PATH):String =
  apiOrigin.trimEnd('/')+"/"+path.trim('/')+"?nonce="+java.net.URLEncoder.encode(nonce,"UTF-8")
/** مسیر پیش‌فرض جریان مرورگری؛ اگر سرور مسیر دیگری داد، همان استفاده می‌شود. */
const val GOOGLE_APP_SIGNIN_PATH:String = "/api/auth/google/app/start"
/** دکمهٔ گوگل فقط وقتی معنا دارد که جریان مرورگری روی سرور کامل باشد (client_id + secret). */
fun googleAppSignInEnabled(config:JSONObject):Boolean =
  config.optBoolean("google_redirect_enabled",config.optBoolean("google_sign_in_enabled",false))
/** مسیر شروع ورود که سرور اعلام کرده (پیش‌فرض همان قرارداد بالا). */
fun googleAppSignInPath(config:JSONObject):String =
  config.str("google_app_signin_path",GOOGLE_APP_SIGNIN_PATH).ifBlank{GOOGLE_APP_SIGNIN_PATH}

 /** عمر مجاز nonce ورود گوگل روی دستگاه (۱۰ دقیقه). */
const val OAUTH_NONCE_TTL_SECONDS:Long = 600
/** رشتهٔ ذخیره‌شده: «nonce:timestamp» — قابل آزمون و بدون وابستگی اندروید. */
fun encodeOauthNonce(nonce:String,nowMillis:Long):String = nonce+":"+nowMillis
/** nonce تازه را برمی‌گرداند و در غیر این صورت رشتهٔ خالی (منقضی/خراب/خالی). */
fun decodeOauthNonce(stored:String,nowMillis:Long):String {
  val raw=stored.trim()
  if(raw.isEmpty())return ""
  val parts=raw.split(':')
  if(parts.size!=2)return ""
  val nonce=parts[0].trim();val ts=parts[1].trim().toLongOrNull()?:return ""
  if(!handoffNonceValid(nonce))return ""
  val age=(nowMillis-ts)/1000
  if(age<0||age>OAUTH_NONCE_TTL_SECONDS)return ""
  return nonce
}

data class HandoffLink(val handoff:String,val nonce:String,val error:String,val linkToken:String,val email:String)
 /**
  * تحلیل پیوند بازگشتی `hsai://auth/google?...` که مرورگر به اپ می‌دهد.
  * بدون Uri اندروید نوشته شده تا در آزمون JVM هم اجرا شود.
  */
 fun parseHandoffLink(raw:String):HandoffLink? {
  val link=raw.trim()
  if(!link.startsWith("hsai://auth/google"))return null
  val query=link.substringAfter('?',"")
  if(query.isBlank())return null
  val params=HashMap<String,String>()
  query.split('&').forEach { part->
   if(part.isNotBlank()){val key=part.substringBefore('=');val value=part.substringAfter('=',"")
    if(key.isNotBlank())params[key]=runCatching{java.net.URLDecoder.decode(value,"UTF-8")}.getOrDefault(value)}
  }
  return HandoffLink(params["handoff"].orEmpty(),params["nonce"].orEmpty(),params["error"].orEmpty(),params["link_token"].orEmpty(),params["email"].orEmpty())
 }
 /** کد پیگیری فرم تماس عمومی. */
 fun contactTracking(data:JSONObject):String = data.str("tracking_code")
 /** روش‌های ورود متصل به حساب (Google) — `GET /auth/identities`. */
 fun identities(data:JSONObject):List<JSONObject> = data.objects("providers")
 fun hasGoogleIdentity(data:JSONObject):Boolean = identities(data).any { it.str("provider")=="google" }
}
