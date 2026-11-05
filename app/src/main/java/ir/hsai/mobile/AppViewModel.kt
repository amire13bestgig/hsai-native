package ir.hsai.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

enum class Screen(val title:String) {
 CHAT("گفتگوی حقوقی"), HISTORY("گفتگوهای من"), DOCUMENTS("اوراق قضایی"), SCANNER("تحلیل ابلاغیه"),
 LAWS("بانک قوانین"), CALCULATORS("محاسبه‌گرها"), DEADLINES("مهلت‌های قانونی"), PLANS("اشتراک و پرداخت‌ها"), TICKETS("پشتیبانی"), ACCOUNT("حساب کاربری")
}
data class ChatMessage(val role:String,val text:String,val id:Long?=null,val citations:List<JSONObject> = emptyList(),val image:String?=null,val document:JSONObject?=null,val feedback:String="",val verification:JSONObject?=null,val questions:List<JSONObject> = emptyList())
data class AppDialog(val kind:String,val title:String,val data:JSONObject=JSONObject())
data class AppUiState(
 val screen:Screen=Screen.ACCOUNT,val entryComplete:Boolean=false,val backDepth:Int=0,val dark:Boolean=false,val initialized:Boolean=false,val authenticated:Boolean=false,
 val sidebarHistory:List<JSONObject> = emptyList(),val user:JSONObject=JSONObject(),val subscription:JSONObject=JSONObject(),val config:JSONObject=JSONObject(),
 val loading:Boolean=false,val posting:Boolean=false,val error:String="",val online:Boolean=true,
 val items:List<JSONObject> = emptyList(),val data:JSONObject=JSONObject(),val page:Int=1,val hasNext:Boolean=false,
 val messages:List<ChatMessage> = emptyList(),val draft:String="",val image:String?=null,val streaming:Boolean=false,val stage:String="",
 val conversationId:Long?=null,val failedChat:JSONObject?=null,val dialog:AppDialog?=null,
 val lawSlug:String="",val lawQuery:String="",val lawSource:String="all",val lawTab:String="search",val planCycle:String="monthly",val calculator:String="court_fee",val result:JSONObject?=null
)
sealed interface AppEffect {
 data class Toast(val text:String):AppEffect
 data class OpenUrl(val url:String):AppEffect
 data class Download(val id:Long,val token:String):AppEffect
 data class Copy(val text:String):AppEffect
 data class Calendar(val title:String,val date:String):AppEffect
 data class Remind(val id:Long,val title:String,val date:String):AppEffect
}
fun JSONObject.str(key:String,default:String=""):String = if(isNull(key)) default else optString(key,default)
fun JSONObject.objects(key:String):List<JSONObject> = optJSONArray(key)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } } ?: emptyList()
fun JSONObject.safeLong(key:String):Long? = if(has(key)&&!isNull(key)&&optLong(key,-1)>=0) optLong(key) else null

class AppViewModel(app:Application,private val saved:SavedStateHandle):AndroidViewModel(app) {
 val api=ApiClient()
 private val vault=SecureSession(app)
 private val mutable=MutableStateFlow(AppUiState(draft=saved["draft"] ?: ""))
 val state:StateFlow<AppUiState> = mutable.asStateFlow()
 private val events=MutableSharedFlow<AppEffect>(extraBufferCapacity=16)
 val effects=events.asSharedFlow()
 private var loadJob:Job?=null
 private var postJob:Job?=null
 private var generation=0
 private var sessionEpoch=0
 private var returnTo=Screen.CHAT
 private val back=mutableListOf<Screen>()
 private var otpSentAt=0L
 private fun change(block:(AppUiState)->AppUiState) { mutable.update(block) }
 fun notice(text:String) { events.tryEmit(AppEffect.Toast(text)) }
 init {
  viewModelScope.launch {
   val session=withContext(Dispatchers.IO) { vault.read() }
   api.token=session?.str("token").orEmpty()
   change { it.copy(initialized=true,screen=if(api.token.isBlank())Screen.CHAT else it.screen,entryComplete=api.token.isBlank(),authenticated=api.token.isNotBlank(),user=session?.optJSONObject("user")?:JSONObject(),dark=vault.dark) }
   launch { runCatching { api.request("/config",authenticated=false) }.onSuccess { cfg->change { it.copy(config=cfg) } } }
   launch { health() }
   val initialToken=api.token;val initialEpoch=sessionEpoch
   if(initialToken.isNotBlank()) runCatching { api.request("/auth/refresh","POST",JSONObject()) }.onSuccess { d->
    if(initialEpoch==sessionEpoch&&api.token==initialToken&&d.str("token").isNotBlank()) {
     api.token=d.str("token");val u=d.optJSONObject("user")?:state.value.user
     change{it.copy(user=u,subscription=d.optJSONObject("subscription")?:it.subscription)};persist(u)
    }
   }.onFailure { if(initialEpoch==sessionEpoch&&api.token==initialToken&&it is ApiException&&it.status==401)expire() }
  }
 }
 private suspend fun persist(user:JSONObject) {
  val d=JSONObject().put("token",api.token).put("user",user)
  try { withContext(Dispatchers.IO) { vault.save(d) } } catch(_:Exception) { notice("نشست فقط تا بستن اپ باقی می‌ماند؛ ذخیره امن انجام نشد.") }
 }
 private suspend fun expire() {
  if(state.value.entryComplete)returnTo=state.value.screen
  back.clear();generation++;sessionEpoch++;api.token="";withContext(Dispatchers.IO) { vault.clear() }
  change { it.copy(authenticated=false,entryComplete=false,backDepth=0,sidebarHistory=emptyList(),user=JSONObject(),subscription=JSONObject(),items=emptyList(),data=JSONObject(),messages=emptyList(),conversationId=null,image=null,failedChat=null,dialog=null,result=null) }
 }
 fun toggleTheme() { val v=!state.value.dark;vault.dark=v;change { it.copy(dark=v) } }
 fun setOnline(online:Boolean) { change { it.copy(online=online) } }
 suspend fun health() { try { api.request("/health",authenticated=false);change { it.copy(online=true) } } catch(_:Exception) { change { it.copy(online=false) } } }
 fun navigate(screen:Screen,remember:Boolean=true) {
  if(!state.value.authenticated&&(screen in privateScreens||screen==Screen.ACCOUNT)){loginRequired(screen);return}
  if(state.value.screen!=screen&&remember)back.add(state.value.screen)
  generation++;loadJob?.cancel()
  change { it.copy(screen=screen,backDepth=back.size,dialog=null,error="",items=emptyList(),data=JSONObject(),result=null,page=1,hasNext=false,loading=false) }
  if(screen in privateScreens && !state.value.authenticated)returnTo=screen
  refresh()
 }
 fun goBack():Boolean { if(state.value.dialog!=null){closeDialog();return true};if(back.isNotEmpty()){navigate(back.removeAt(back.lastIndex),false);return true};if(state.value.screen!=Screen.CHAT){navigate(Screen.CHAT,false);return true};return false }
 fun openDialog(kind:String,title:String,data:JSONObject=JSONObject()) { change { it.copy(dialog=AppDialog(kind,title,data)) } }
 fun closeDialog() { if(!state.value.posting)change { it.copy(dialog=null) } }
 fun setDraft(text:String) { saved["draft"]=text;change { it.copy(draft=text.take(12000)) } }
 fun setImage(data:String?) { change { it.copy(image=data) } }
 fun setStream(v:Boolean) { change { it.copy(streaming=v) } }
 fun chooseTopic(text:String) { setDraft("$text؛ ") }
 fun newChat() { if(state.value.posting){notice("تا پایان درخواست صبر کنید.");return};setDraft("");change { it.copy(messages=emptyList(),conversationId=null,failedChat=null,image=null) };navigate(Screen.CHAT) }
 fun loginRequired(target:Screen=state.value.screen) {
  returnTo=target;generation++;loadJob?.cancel()
  change{it.copy(screen=Screen.ACCOUNT,entryComplete=false,dialog=null,error="",items=emptyList(),data=JSONObject(),result=null,loading=false,page=1,hasNext=false)}
 }
 fun browseAsGuest() {
  if(state.value.posting)return
  back.clear();returnTo=Screen.CHAT;change{it.copy(entryComplete=true)};navigate(Screen.CHAT,false)
 }
 private fun post(block:suspend ()->Unit) {
  if(state.value.posting){notice("یک درخواست در حال انجام است.");return}
  val origin=state.value.screen
  change { it.copy(posting=true,error="") }
  postJob=viewModelScope.launch {
   try { block() } catch(c:CancellationException){throw c} catch(e:Exception) {
    if(e is ApiException && e.status==401 && e.code!="AUTH_FORM") { expire();notice("نشست پایان یافته؛ دوباره وارد شوید.") }
    change { if(it.screen==origin)it.copy(error=e.message?:"درخواست انجام نشد.")else it };notice(e.message?:"خطا در ارتباط")
   } finally { change { it.copy(posting=false,stage="") } }
  }
 }
 fun authenticate(register:Boolean,values:Map<String,String>) { post {
  val data=JSONObject()
  if(register) {
   require(values["full_name"].orEmpty().trim().length>=2){"نام و نام خانوادگی را کامل بنویسید."}
   require(values["password"].orEmpty().length>=6){"رمز عبور حداقل ۶ نویسه باشد."}
   data.put("full_name",values["full_name"]!!.trim()).put("phone",Validation.phone(values["phone"].orEmpty())).put("password",values["password"])
   values["email"]?.trim()?.takeIf { it.isNotBlank() }?.let { require(android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()){ "ایمیل معتبر وارد کنید." };data.put("email",it) }
  } else {
   val id=values["identifier"].orEmpty().trim()
   require(values["password"].orEmpty().isNotBlank()){ "رمز عبور را وارد کنید." }
   data.put("identifier",if('@' in id)id else Validation.phone(id)).put("password",values["password"])
  }
  val d=try { api.request(if(register)"/auth/register" else "/auth/login","POST",data,false) }catch(e:ApiException){throw ApiException(e.message.orEmpty(),e.status,"AUTH_FORM")}
  acceptAuth(d)
 } }
 private suspend fun acceptAuth(d:JSONObject) {
  require(d.str("token").isNotBlank()){ "توکن در پاسخ سرویس موجود نیست." }
  sessionEpoch++;api.token=d.str("token");val u=d.optJSONObject("user")?:JSONObject();persist(u)
  change { it.copy(authenticated=true,entryComplete=true,user=u,subscription=d.optJSONObject("subscription")?:JSONObject(),dialog=null,error="") }
  notice("به حق‌سنج خوش آمدید.");val target=returnTo;returnTo=Screen.CHAT;back.clear();navigate(target,false);loadSidebar()
 }
 fun enterWorkspace() {
  if(!state.value.initialized||!state.value.authenticated)return
  change { it.copy(entryComplete=true) };back.clear();navigate(returnTo,false);returnTo=Screen.CHAT;loadSidebar()
 }
 private fun loadSidebar() {
  if(!state.value.authenticated||!state.value.entryComplete)return
  val epoch=sessionEpoch
  viewModelScope.launch {
   runCatching { api.request("/consultations?limit=30&page=1") }.onSuccess { response ->
    if(epoch==sessionEpoch&&state.value.authenticated&&state.value.entryComplete)change{it.copy(sidebarHistory=response.objects("items"))}
   }
  }
 }
 fun sendOtp(phone:String) { post {
  require(state.value.config.optBoolean("sms_otp_enabled")){"ورود پیامکی در سرور فعال نیست."}
  require(System.currentTimeMillis()-otpSentAt>=60000){"برای ارسال دوباره یک دقیقه صبر کنید."}
  val p=Validation.phone(phone);api.request("/auth/otp/send","POST",JSONObject().put("phone",p),false);otpSentAt=System.currentTimeMillis()
  openDialog("otp","تأیید شماره موبایل",JSONObject().put("phone",p))
 } }
 fun verifyOtp(phone:String,code:String) { post {
  val c=Validation.digits(code);require(Regex("[0-9]{4,8}").matches(c)){"کد پیامکی معتبر وارد کنید."}
  val d=try { api.request("/auth/otp/verify","POST",JSONObject().put("phone",phone).put("code",c),false) } catch(e:ApiException){throw ApiException(e.message.orEmpty(),e.status,"AUTH_FORM")}
  acceptAuth(d)
 } }
 fun logout() { if(state.value.posting)return;viewModelScope.launch { androidx.work.WorkManager.getInstance(getApplication()).cancelAllWorkByTag("hsai-deadline");expire();setDraft("");change { it.copy(image=null,failedChat=null) };back.clear();returnTo=Screen.CHAT;browseAsGuest();notice("از حساب خارج شدید.") } }
 fun sendChat(retry:Boolean=false) {
  val s=state.value
  if(!retry&&s.draft.isBlank()&&s.image==null)return
  if(!s.authenticated){loginRequired(Screen.CHAT);notice("پیش‌نویس حفظ شد؛ برای ارسال وارد شوید.");return}
  if(s.posting)return
  val payload=if(retry)s.failedChat else JSONObject().put("message",s.draft.trim().ifBlank { if(s.image!=null)"این تصویر را تحلیل کن" else "" })
   .put("conversation_id",s.conversationId?:JSONObject.NULL).also { if(s.image!=null)it.put("image_base64",s.image) }
  if(payload==null||payload.str("message").isBlank())return
  val epoch=sessionEpoch
  if(!retry) { change { it.copy(messages=it.messages+ChatMessage("user",payload.str("message"),image=it.image),image=null) };setDraft("") }
  change { it.copy(failedChat=null) }
  post {
   try {
    if(s.streaming) streamChat(payload,epoch) else {
     val d=api.request("/chat","POST",payload)
     if(epoch==sessionEpoch)change { it.copy(conversationId=d.safeLong("conversation_id")?:it.conversationId,messages=it.messages+ChatMessage("assistant",d.str("answer","پاسخ متنی دریافت نشد."),d.safeLong("message_id"),d.objects("cited_articles"),document=d.optJSONObject("verification")?.optJSONObject("printable_document"),verification=d.optJSONObject("verification"),questions=ApiContract.quickQuestions(d)),subscription=d.optJSONObject("subscription")?:it.subscription) };loadSidebar()
    }
   } catch(e:Exception) {
    if(e is CancellationException)throw e
    if(e is ApiException&&e.status==401){expire();setDraft(payload.str("message"));setImage(payload.str("image_base64").ifBlank { null });loginRequired(Screen.CHAT)}
    else if(epoch==sessionEpoch)change { it.copy(failedChat=payload,error=e.message.orEmpty()) }
    throw if(e is ApiException&&e.status==401)ApiException("پیام حفظ شد؛ دوباره وارد شوید.",0) else e
   }
  }
 }
 private suspend fun streamChat(payload:JSONObject,epoch:Int) {
  val buffer=StringBuilder();var done=false
  change { it.copy(messages=it.messages+ChatMessage("assistant",""),stage="در حال بررسی قوانین…") }
  try { api.stream(payload).collect { event ->
   if(epoch!=sessionEpoch)return@collect
   when(event.name) {
    "stage" -> change { it.copy(stage=ApiContract.stageTitle(event.data)) }
    "delta" -> { buffer.append(event.data.str("chunk"));change { it.copy(messages=it.messages.dropLast(1)+it.messages.last().copy(text=buffer.toString())) } }
    "articles" -> change { it.copy(messages=it.messages.dropLast(1)+it.messages.last().copy(citations=event.data.objects("articles"))) }
    "done" -> { done=true;val m=event.data.optJSONObject("assistant_message")?:event.data;change { it.copy(conversationId=event.data.safeLong("conversation_id")?:it.conversationId,messages=it.messages.dropLast(1)+it.messages.last().copy(text=m.str("content",m.str("answer",buffer.toString())),id=m.safeLong("id")?:event.data.safeLong("message_id"),citations=m.objects("cited_articles").ifEmpty { it.messages.last().citations },document=m.optJSONObject("verification")?.optJSONObject("printable_document"),verification=m.optJSONObject("verification"),questions=ApiContract.quickQuestions(m)),subscription=event.data.optJSONObject("subscription")?:it.subscription) } }
    "error" -> throw ApiException(ApiContract.streamError(event.data))
   }
  };if(done)loadSidebar();if(!done)throw ApiException("پاسخ جریانی کامل نشد؛ قبل از ارسال دوباره تاریخچه را بررسی کنید.")
  } catch(e:Exception) { if(state.value.messages.lastOrNull()?.role=="assistant"&&state.value.messages.last().text.isBlank())change { it.copy(messages=it.messages.dropLast(1)) };throw e }
 }
 fun feedback(id:Long,value:String) { post { api.request("/messages/$id/feedback","POST",JSONObject().put("feedback",value));change { it.copy(messages=it.messages.map { m->if(m.id==id)m.copy(feedback=value)else m }) };notice("بازخورد ثبت شد.") } }
 fun simplify(id:Long) { val conv=state.value.conversationId;post { val d=api.request("/messages/$id/simplify","POST",JSONObject());val text=ApiContract.simplifiedText(d);if(conv==state.value.conversationId)change { it.copy(messages=it.messages+ChatMessage("assistant",text)) } } }
 fun openConversation(id:Long) { if(!state.value.authenticated){loginRequired(Screen.HISTORY);return};if(state.value.posting)return;navigate(Screen.CHAT);val g=generation;post { val d=api.request("/consultations/$id");if(g!=generation)return@post;change { it.copy(conversationId=id,messages=d.objects("messages").filter { m->m.str("role") in listOf("user","assistant") }.map { m->ChatMessage(m.str("role"),m.str("content"),if(m.str("role")=="assistant")m.safeLong("id")else null,m.objects("cited_articles"),feedback=m.str("feedback"),verification=m.optJSONObject("verification"),document=m.optJSONObject("verification")?.optJSONObject("printable_document"),questions=ApiContract.quickQuestions(m)) },failedChat=null) } } }
 fun refresh(page:Int=1) {
  val s=state.value;if(!s.initialized)return
  if(s.screen in privateScreens && !s.authenticated)return
  val endpoint=when(s.screen) {
   Screen.HISTORY->"/consultations?limit=30&page=$page"
   Screen.DOCUMENTS->"/documents?limit=30&page=$page"
   Screen.SCANNER->"/scanner?limit=20&page=$page"
   Screen.DEADLINES->"/deadlines?page=$page"
   Screen.PLANS->"/billing/plans"
   Screen.TICKETS->"/tickets?page=$page"
   Screen.ACCOUNT->if(s.authenticated)"/me" else return
   Screen.LAWS->when(s.lawTab){"search"->"/laws?q=${encode(s.lawQuery)}&limit=15&page=$page"+(if(s.lawSource!="all")"&source_type=${encode(s.lawSource)}"else "")+(if(s.lawSlug.isNotBlank())"&law_slug=${encode(s.lawSlug)}"else "");"glossary"->return;else->"/laws/${s.lawTab}"}
   else->return
  }
  val g=++generation;loadJob?.cancel();change { it.copy(loading=true,error="",page=page) }
  loadJob=viewModelScope.launch {
   try {
    val d=api.request(endpoint,authenticated=s.screen !in listOf(Screen.LAWS,Screen.PLANS));if(g!=generation)return@launch
    val paged=ApiContract.page(d,page,if(s.screen==Screen.LAWS)15 else if(s.screen in listOf(Screen.HISTORY,Screen.DOCUMENTS))30 else 20,s.lawTab)
    change { it.copy(loading=false,data=d,items=paged.items,hasNext=paged.hasNext) }
    if(s.screen==Screen.HISTORY&&page==1)change{it.copy(sidebarHistory=paged.items)}
    if(s.screen==Screen.ACCOUNT){val u=d.optJSONObject("user")?:d;change { it.copy(user=u,subscription=d.optJSONObject("subscription")?:JSONObject()) };persist(u)}
   } catch(c:CancellationException){throw c} catch(e:Exception){if(g==generation){if(e is ApiException&&e.status==401)expire();change { it.copy(loading=false,error=e.message.orEmpty()) }}}
  }
 }
 fun searchLaws(query:String) { change { it.copy(lawQuery=query.trim()) };refresh() }
 fun lawTab(tab:String) { generation++;loadJob?.cancel();change { it.copy(lawTab=tab,items=emptyList(),data=JSONObject(),loading=false,error="",page=1,hasNext=false) };refresh() }
 fun lawSource(source:String) { change{it.copy(lawSource=source)};refresh() }
 fun openLawCollection(slug:String) { change{it.copy(lawSlug=slug,lawQuery="",lawSource="all")};lawTab("search") }
 fun clearLawCollection() { change{it.copy(lawSlug="")};refresh() }
 fun viewArticle(id:Long) { val g=generation;post {val d=api.request("/laws/articles/$id",authenticated=false);if(g==generation){val article=d.optJSONObject("article")?:d;openDialog("article",article.str("law_name","متن قانون"),article)}} }
 fun askAboutArticle(article:JSONObject) {setDraft("توضیح ساده و کاربردی ماده ${article.str("article_number")} ${article.str("law_name")} را بگو و کاربرد آن را توضیح بده.");change{it.copy(dialog=null)};navigate(Screen.CHAT)}
 fun answerQuestions(text:String) {if(state.value.posting||text.isBlank())return;setDraft(text);sendChat()}
 fun stopChat() { if(state.value.posting&&state.value.stage.isNotBlank()){postJob?.cancel();notice("دریافت متوقف شد؛ ممکن است درخواست در سرور ثبت شده باشد. قبل از ارسال دوباره تاریخچه را بررسی کنید.")} }
 fun cycle(cycle:String) { change { it.copy(planCycle=cycle) } }
 fun calculator(id:String) { generation++;change { it.copy(calculator=id,result=null,error="") } }
 fun compute(values:Map<String,String>) { val g=generation;val calculator=Domain.calculators.first { it.id==state.value.calculator };post {
  val body=JSONObject().put("type",calculator.id)
  calculator.fields.forEach { f-> val raw=values[f.key]?:f.options.firstOrNull()?.first.orEmpty();body.put(f.key,when { f.boolean->raw=="true";f.numeric->Validation.number(f.key,raw);f.key=="heir_class"->raw.toInt();else->raw }) }
  if(calculator.id=="delay_damage")require(body.getInt("pay_year")*12+body.getInt("pay_month")>=body.getInt("due_year")*12+body.getInt("due_month")){"تاریخ پرداخت پیش از سررسید است."}
  if(calculator.id=="inheritance")require(body.getDouble("debts_toman")+body.getDouble("will_toman")<=body.getDouble("estate_value_toman")){"جمع بدهی و وصیت از ارزش ماترک بیشتر است."}
  val result=api.request("/calculators/compute","POST",body,false);if(g==generation&&state.value.screen==Screen.CALCULATORS&&state.value.calculator==calculator.id)change { it.copy(result=result) }
 } }
 fun viewDocument(id:Long,edit:Boolean=false) { val g=generation;post { val d=api.request("/documents/$id");if(g!=generation)return@post;openDialog(if(edit)"documentEdit" else "documentView",d.str("title","برگه قضایی"),d.optJSONObject("document")?:d) } }
 fun createDocument(type:String,title:String,details:String) { post { require(title.isNotBlank()&&details.isNotBlank()){ "عنوان و شرح را تکمیل کنید." };api.request("/documents","POST",ApiContract.documentBody(type,title,details));change { it.copy(dialog=null) };notice("برگه تنظیم شد.");if(state.value.screen==Screen.DOCUMENTS)refresh() } }
 fun saveDocument(id:Long,title:String,content:String) { post { require(title.isNotBlank()&&content.isNotBlank()){ "عنوان و متن لازم است." };api.request("/documents/$id","PUT",JSONObject().put("title",title).put("content",content));change { it.copy(dialog=null) };refresh();notice("ذخیره شد.") } }
 fun delete(kind:String,id:Long) { post { api.request("/$kind/$id","DELETE");if(kind=="deadlines")DeadlineReminder.cancel(getApplication(),id);change { it.copy(dialog=null) };if(kind=="consultations"){change{it.copy(sidebarHistory=it.sidebarHistory.filterNot{item->item.optLong("id")==id})};if(state.value.conversationId==id)change { it.copy(conversationId=null,messages=emptyList()) }};refresh();notice("حذف شد.") } }
 fun download(id:Long) { if(api.token.isBlank())loginRequired()else events.tryEmit(AppEffect.Download(id,api.token)) }
 fun openExternal(url:String) { runCatching { val u=java.net.URI(url);require(u.scheme=="https"&&u.host!=null);events.tryEmit(AppEffect.OpenUrl(url)) }.onFailure { notice("نشانی معتبر نیست.") } }
 fun copy(text:String) { events.tryEmit(AppEffect.Copy(text)) }
 fun analyze(kind:String,text:String,image:String?,consent:Boolean) { val g=generation;post {
  require(consent){"رضایت ارسال برگه را تأیید کنید."};require(text.isNotBlank()||image!=null){"متن یا تصویر برگه را انتخاب کنید."}
  val body=ApiContract.scannerBody(kind,text,image)
  val d=api.request("/scanner/analyze","POST",body);if(g!=generation)return@post;change { it.copy(result=d.optJSONObject("analysis")?:d) };if(state.value.screen==Screen.SCANNER)refresh()
 } }
 fun addDeadline(title:String,date:String,notes:String) { post {
  require(title.isNotBlank()){ "عنوان مهلت را وارد کنید." };java.time.LocalDate.parse(date)
  api.request("/deadlines","POST",ApiContract.deadlineBody(title,date,notes));change { it.copy(dialog=null) };refresh();notice("مهلت ثبت شد.")
 } }
 fun completeDeadline(id:Long) { post { api.request("/deadlines/$id","PATCH",JSONObject().put("status","done"));DeadlineReminder.cancel(getApplication(),id);refresh() } }
 fun calendar(title:String,date:String) { runCatching { java.time.LocalDate.parse(date) }.onSuccess { events.tryEmit(AppEffect.Calendar(title,date)) }.onFailure { notice("تاریخ سرویس قابل تبدیل به تقویم نیست.") } }
 fun reminder(id:Long,title:String,date:String) { runCatching { java.time.LocalDate.parse(date) }.onSuccess { events.tryEmit(AppEffect.Remind(id,title,date)) }.onFailure { notice("تاریخ میلادی معتبر لازم است.") } }
 fun createTicket(subject:String,message:String) { post { require(subject.isNotBlank()&&message.isNotBlank()){ "موضوع و پیام لازم است." };api.request("/tickets","POST",JSONObject().put("subject",subject).put("message",message));change { it.copy(dialog=null) };refresh();notice("تیکت ارسال شد.") } }
 fun replyTicket(id:Long,message:String) { post { require(message.isNotBlank()){ "متن پاسخ را بنویسید." };api.request("/tickets/$id/reply","POST",JSONObject().put("message",message));change { it.copy(dialog=null) };refresh();notice("پاسخ ارسال شد.") } }
 fun saveProfile(name:String,email:String,nationalId:String) { post {
  require(name.trim().length>=2){"نام را کامل بنویسید."};require(email.isBlank()||android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){ "ایمیل معتبر وارد کنید." }
  val id=Validation.digits(nationalId);require(id.isBlank()||Validation.nationalId(id)){"کد ملی معتبر نیست."}
  api.request("/me","PATCH",JSONObject().put("full_name",name.trim()).put("email",email.trim()).put("national_id",id));refresh();notice("مشخصات ذخیره شد.")
 } }
 fun changePassword(old:String,new:String,confirm:String) { post {
  require(old.isNotBlank()){"رمز فعلی را وارد کنید."};require(new.length>=6){"رمز جدید حداقل ۶ نویسه باشد."};require(new==confirm){"تکرار رمز یکسان نیست."}
  val id=state.value.user.str("email").ifBlank { state.value.user.str("phone") }
  try { api.request("/auth/login","POST",JSONObject().put("identifier",id).put("password",old),false) }catch(e:ApiException){throw ApiException("رمز فعلی یا مشخصات ورود صحیح نیست.",e.status,"AUTH_FORM")}
  api.request("/me","PATCH",JSONObject().put("new_password",new));change { it.copy(dialog=null) };notice("رمز عبور تغییر کرد.")
 } }
 fun selectPlan(p:JSONObject) { if(!state.value.authenticated){loginRequired(Screen.PLANS);return};openDialog("checkout","بررسی سفارش",p) }
 fun validateDiscount(plan:Long,code:String) { post {
  require(code.isNotBlank()){ "کد تخفیف را وارد کنید." };val d=api.request("/billing/discount/validate","POST",JSONObject().put("plan_id",plan).put("code",code.trim()))
  require(d.optBoolean("valid",true)){d.str("message","کد نامعتبر است.")};notice("کد توسط سرور بررسی شد؛ مبلغ نهایی را در درگاه کنترل کنید.")
 } }
 fun checkout(plan:Long,code:String) { post {
  val body=JSONObject().put("plan_id",plan).put("app_return_url","hsai://payment/callback");if(code.isNotBlank())body.put("discount_code",code.trim())
  val d=api.request("/billing/checkout","POST",body)
  if(d.optBoolean("instant_activated")){change { it.copy(dialog=null) };navigate(Screen.ACCOUNT);notice("اشتراک طبق پاسخ سرور فعال شد.");return@post}
  val url=d.str("gateway_url");val parsed=java.net.URI(url);require(parsed.scheme=="https"&&parsed.host!=null){"نشانی درگاه امن نیست."}
  require(d.str("authority").isNotBlank()){ "شناسه پرداخت دریافت نشد." };vault.authority=d.str("authority");change { it.copy(dialog=null) };events.emit(AppEffect.OpenUrl(url))
 } }
 fun verifyPayment(authority:String=vault.authority) { if(authority.isBlank())return;if(!state.value.authenticated){loginRequired(Screen.PLANS);return};post { val d=api.request("/billing/payments/${encode(authority)}");openDialog("result","وضعیت پرداخت از سرور",d) } }
 fun paymentHistory() { post { val d=api.request("/billing/payments");openDialog("payments","تاریخچه پرداخت‌ها",d) } }
 val pendingPayment:Boolean get()=vault.authority.isNotBlank()
 private fun encode(s:String)=URLEncoder.encode(s,"UTF-8").replace("+","%20")
 companion object { val privateScreens=setOf(Screen.HISTORY,Screen.DOCUMENTS,Screen.SCANNER,Screen.DEADLINES,Screen.TICKETS) }
}
