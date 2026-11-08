package ir.hsai.mobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.net.URLEncoder

enum class Screen(val title:String) {
 CHAT("گفتگوی حقوقی"), HISTORY("گفتگوهای من"), DOCUMENTS("اوراق قضایی"), SCANNER("تحلیل مدارک"),
 LAWS("بانک قوانین"), CALCULATORS("محاسبه‌گرها"), DEADLINES("مهلت‌های قانونی"), PLANS("اشتراک و پرداخت‌ها"), TICKETS("پشتیبانی"), ACCOUNT("حساب کاربری")
}
enum class ChatStatus { IDLE, SENDING, RECEIVING, COMPLETED, STOPPED, FAILED, UNKNOWN }
data class ChatMessage(val role:String,val text:String,val id:Long?=null,val citations:List<JSONObject> = emptyList(),val image:String?=null,val document:JSONObject?=null,val feedback:String="",val verification:JSONObject?=null,val questions:List<JSONObject> = emptyList(),val suggestions:List<String> = emptyList())
data class DocumentDraft(val id:Long,val title:String,val content:String)
data class AppDialog(val kind:String,val title:String,val data:JSONObject=JSONObject())
data class AppUiState(
 val screen:Screen=Screen.ACCOUNT,val entryComplete:Boolean=false,val backDepth:Int=0,val dark:Boolean=false,val initialized:Boolean=false,val authenticated:Boolean=false,
 val reminderNotice:Boolean=false,val checkingConnection:Boolean=false,val profileRevision:Int=0,val fieldErrors:Map<String,String> = emptyMap(),val validationAttempt:Int=0,val pane:AppDialog?=null,val documentDrafts:Map<Long,DocumentDraft> = emptyMap(),
 val sidebarHistory:List<JSONObject> = emptyList(),val user:JSONObject=JSONObject(),val subscription:JSONObject=JSONObject(),val config:JSONObject=JSONObject(),
 val loading:Boolean=false,val posting:Boolean=false,val error:String="",val online:Boolean=true,
 val items:List<JSONObject> = emptyList(),val data:JSONObject=JSONObject(),val page:Int=1,val hasNext:Boolean=false,
 val messages:List<ChatMessage> = emptyList(),val draft:String="",val image:String?=null,val streaming:Boolean=false,val stage:String="",
 val draftReady:Boolean=false,val pendingGuestMessage:Boolean=false,val chatStatus:ChatStatus=ChatStatus.IDLE,val retryReviewed:Boolean=false,
 val conversationId:Long?=null,val failedChat:JSONObject?=null,val dialog:AppDialog?=null,
 val lawSlug:String="",val lawQuery:String="",val lawSource:String="all",val lawTab:String="search",val planCycle:String="monthly",val calculator:String="court_fee",val result:JSONObject?=null
)
sealed interface AppEffect {
 data class Toast(val text:String):AppEffect
 data class OpenUrl(val url:String):AppEffect
 data class Download(val id:Long,val session:Int):AppEffect
 data class Copy(val text:String):AppEffect
 data class Calendar(val title:String,val date:String):AppEffect
 data class Remind(val id:Long,val title:String,val date:String,val owner:String):AppEffect
}
class AppViewModel @JvmOverloads constructor(app:Application,private val saved:SavedStateHandle,val api:ApiClient=ApiClient(),private val vault:SessionStore=SecureSession(app)):AndroidViewModel(app) {
 private val mutable=MutableStateFlow(AppUiState(draft=saved["draft"] ?: ""))
 val state:StateFlow<AppUiState> = mutable.asStateFlow()
 private val events=MutableSharedFlow<AppEffect>(extraBufferCapacity=16)
 val effects=events.asSharedFlow()
 private var loadJob:Job?=null
 private var postJob:Job?=null
 private var generation=0
 private var sessionEpoch:Int=saved["session_epoch"]?:0
 private fun nextSession(){sessionEpoch++;saved["session_epoch"]=sessionEpoch}
 private var returnTo=Screen.CHAT
 private val back=mutableListOf<Screen>()
 private var connectionJob:Job?=null
 private var reminderDismissed=false
 private var otpSentAt=0L
 private fun change(block:(AppUiState)->AppUiState) { mutable.update(block) }
 fun notice(text:String) { events.tryEmit(AppEffect.Toast(text)) }
 init {
  viewModelScope.launch {
   val session=withContext(Dispatchers.IO) { vault.read() }
   api.token=session?.str("token").orEmpty()
   change { it.copy(initialized=true,screen=Screen.CHAT,entryComplete=true,authenticated=api.token.isNotBlank(),user=session?.optJSONObject("user")?:JSONObject(),dark=vault.dark) }
   launch { runCatching { request("/config",authenticated=false) }.onSuccess { cfg->change { it.copy(config=cfg) } } }
   checkConnection(false)
   change{it.copy(reminderNotice=vault.reminderNotice)}
   if(!vault.reminderMigrationDone)launch { try { val pending=ReminderMigration.run(getApplication());if(reminderDismissed)withContext(Dispatchers.IO){vault.reminderNotice=false};change{it.copy(reminderNotice=pending&&!reminderDismissed)} }catch(c:CancellationException){throw c}catch(_:Exception){/* Retry migration on the next start; never mark failed work as migrated. */} }
   val initialToken=api.token;val initialEpoch=sessionEpoch
   if(initialToken.isNotBlank()) runCatching { request("/auth/refresh","POST",JSONObject()) }.onSuccess { d->
    if(initialEpoch==sessionEpoch&&api.token==initialToken&&SessionTokens.headerSafe(d.str("token"))) {
     api.token=d.str("token");val u=d.optJSONObject("user")?:state.value.user
     change{it.copy(user=u,subscription=d.optJSONObject("subscription")?:it.subscription)};persist(u)
    }
   }.onFailure { if(initialEpoch==sessionEpoch&&api.token==initialToken&&it is ApiException&&it.status==401)expire(initialEpoch) }
   if(initialToken.isNotBlank()&&initialEpoch==sessionEpoch&&state.value.authenticated)loadSidebar()
  }
 }
 /** Never publish a private response or payment authority into a replacement session. */
 private suspend fun request(path:String,method:String="GET",body:JSONObject?=null,authenticated:Boolean=true):JSONObject {
  val epoch=sessionEpoch
  val response=api.request(path,method,body,authenticated)
  if(epoch!=sessionEpoch&&(authenticated||path.startsWith("/auth/")))throw ApiException("پاسخ نشست قبلی کنار گذاشته شد.",0,"STALE_SESSION")
  return response
 }
 private suspend fun persist(user:JSONObject) {
  val token=api.token;val epoch=sessionEpoch
  val data=JSONObject().put("token",token).put("user",user)
  try {withContext(Dispatchers.IO){synchronized(ReminderPrivacy){if(api.token==token&&sessionEpoch==epoch&&token.isNotBlank())vault.save(data)}}}
  catch(c:CancellationException){throw c}catch(_:Exception){notice("نشست فقط تا بستن اپ باقی می‌ماند؛ ذخیره امن انجام نشد.")}
 }
 private suspend fun expire(expected:Int?=null):Boolean {
  if(expected!=null&&expected!=sessionEpoch)return false
  if(state.value.entryComplete)returnTo=state.value.screen
  runCatching{androidx.work.WorkManager.getInstance(getApplication()).cancelAllWorkByTag("hsai-deadline")}
  back.clear();generation++;nextSession();val closing=sessionEpoch;api.token=""
  withContext(Dispatchers.IO){synchronized(ReminderPrivacy){if(sessionEpoch==closing&&api.token.isBlank()){vault.clear();getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancelAll()}}}
  if(sessionEpoch!=closing||api.token.isNotBlank())return false
  change{it.copy(authenticated=false,entryComplete=false,backDepth=0,sidebarHistory=emptyList(),user=JSONObject(),subscription=JSONObject(),items=emptyList(),data=JSONObject(),messages=emptyList(),conversationId=null,image=null,failedChat=null,dialog=null,pane=null,documentDrafts=emptyMap(),fieldErrors=emptyMap(),result=null,draftReady=false,pendingGuestMessage=false,chatStatus=ChatStatus.IDLE,retryReviewed=false)}
  return true
 }
 fun dismissReminderNotice(){reminderDismissed=true;vault.reminderNotice=false;change{it.copy(reminderNotice=false)}}
 fun toggleTheme() { val v=!state.value.dark;vault.dark=v;change { it.copy(dark=v) } }
 fun setOnline(online:Boolean) { change { it.copy(online=online) } }
 suspend fun health() {
  try {kotlinx.coroutines.withTimeout(10000){request("/health",authenticated=false)};change{it.copy(online=true)}}
  catch(t:kotlinx.coroutines.TimeoutCancellationException){change{it.copy(online=false)}}
  catch(c:CancellationException){throw c}
  catch(e:Exception){change{it.copy(online=e is ApiException&&e.status>0)}}
 }
 fun checkConnection(refreshConfig:Boolean=true){
  if(connectionJob?.isActive==true)return
  change{it.copy(checkingConnection=true)}
  connectionJob=viewModelScope.launch{try{health();if(refreshConfig&&state.value.online){val cfg=request("/config",authenticated=false);change{it.copy(config=cfg)}}}catch(c:CancellationException){throw c}catch(_:Exception){}finally{change{it.copy(checkingConnection=false)}}}
 }

 fun navigate(screen:Screen,remember:Boolean=true) {
  if(!state.value.authenticated&&(screen in privateScreens||screen==Screen.ACCOUNT)){loginRequired(screen);return}
  if(state.value.screen!=screen&&remember)back.add(state.value.screen)
  generation++;loadJob?.cancel()
  // FIX(ux): تصویر انتخاب‌شده در چت نباید به اسکنر برود و برعکس؛ قبلاً یک جایگاه مشترک داشتند
  // و کاربر تصویر گفتگو را ناگهان در فرم تحلیل مدارک می‌دید.
  val leavingScan = state.value.screen==Screen.SCANNER && screen!=Screen.SCANNER
  change { it.copy(screen=screen,backDepth=back.size,dialog=null,pane=null,fieldErrors=emptyMap(),error="",items=emptyList(),data=JSONObject(),result=null,page=1,hasNext=false,loading=false,image=if(leavingScan)null else it.image) }
  if(screen in privateScreens && !state.value.authenticated)returnTo=screen
  refresh()
 }
 fun goBack():Boolean { if(state.value.dialog!=null){closeDialog();return true};if(state.value.pane!=null){closePane();return true};if(back.isNotEmpty()){navigate(back.removeAt(back.lastIndex),false);return true};if(state.value.screen!=Screen.CHAT){navigate(Screen.CHAT,false);return true};return false }
 fun openDialog(kind:String,title:String,data:JSONObject=JSONObject()) { change { it.copy(dialog=AppDialog(kind,title,data),error="",fieldErrors=emptyMap()) } }
 fun closeDialog() { if(!state.value.posting)change { it.copy(dialog=null,error="",fieldErrors=emptyMap()) } }
 fun updateDocumentDraft(id:Long,title:String,content:String){change{it.copy(documentDrafts=it.documentDrafts+(id to DocumentDraft(id,title,content)))}}
 fun discardDocumentDraft(id:Long){if(!state.value.posting)change{it.copy(documentDrafts=it.documentDrafts-id)}}
 fun inputEdited(key:String,clearResult:Boolean=false){change{val errors=it.fieldErrors-key;it.copy(fieldErrors=errors,error=if(it.fieldErrors.isNotEmpty())errors.values.firstOrNull().orEmpty()else "",result=if(clearResult)null else it.result)}}
 fun clearFormErrors(){change{it.copy(error="",fieldErrors=emptyMap())}}
 fun closePane(){if(!state.value.posting)change{it.copy(pane=null,error="",fieldErrors=emptyMap())}}
 fun adaptDetail(wide:Boolean,kinds:Set<String>){change {
  when {
   !wide&&it.pane?.kind in kinds&&it.dialog==null->it.copy(dialog=it.pane,pane=null)
   wide&&it.dialog?.kind in kinds&&it.pane==null->it.copy(pane=it.dialog,dialog=null)
   else->it
  }
 }}
 fun previewConversation(id:Long){if(state.value.posting)return;if(!state.value.authenticated){loginRequired(Screen.HISTORY);return};change{it.copy(pane=null)};val g=generation;post {
  val data=request("/consultations/$id");if(g==generation)change{it.copy(pane=AppDialog("historyPreview",data.optJSONObject("conversation")?.str("title","پیش‌نمایش گفتگو")?:"پیش‌نمایش گفتگو",data.put("id",id)))}
 }}
 fun setDraft(text:String) { val clean=text.take(12000);saved["draft"]=clean;change { it.copy(draft=clean,draftReady=it.draftReady&&clean.isNotBlank()) } }
 fun setImage(data:String?) { change { it.copy(image=data,result=if(it.screen==Screen.SCANNER)null else it.result,fieldErrors=it.fieldErrors-"text") } }
 fun setStream(v:Boolean) { change { it.copy(streaming=v) } }
 fun chooseTopic(text:String) { setDraft("$text؛ ") }
 fun newChat() { if(state.value.posting){notice("تا پایان درخواست صبر کنید.");return};setDraft("");change { it.copy(messages=emptyList(),conversationId=null,failedChat=null,image=null,draftReady=false,pendingGuestMessage=false,chatStatus=ChatStatus.IDLE) };navigate(Screen.CHAT) }
 fun loginRequired(target:Screen=state.value.screen) {
  returnTo=target;generation++;loadJob?.cancel()
  change{it.copy(screen=Screen.ACCOUNT,entryComplete=false,pendingGuestMessage=target==Screen.CHAT&&(it.draft.isNotBlank()||it.image!=null),dialog=null,pane=null,fieldErrors=emptyMap(),error="",items=emptyList(),data=JSONObject(),result=null,loading=false,page=1,hasNext=false)}
 }
 fun browseAsGuest() {
  if(state.value.posting)return
  back.clear();returnTo=Screen.CHAT;change{it.copy(entryComplete=true)};navigate(Screen.CHAT,false)
 }
 private fun post(block:suspend ()->Unit) {
  if(state.value.posting){notice("یک درخواست در حال انجام است.");return}
  val origin=state.value.screen;val originDialog=state.value.dialog?:state.value.pane;val originEntry=state.value.entryComplete;val operationEpoch=sessionEpoch
  change { it.copy(posting=true,error="",fieldErrors=emptyMap()) }
  postJob=viewModelScope.launch {
   try { block() } catch(c:CancellationException){throw c} catch(e:Exception) {
    if(e is ApiException && e.status==401 && e.code!="AUTH_FORM"&&operationEpoch==sessionEpoch) { expire(operationEpoch);notice("نشست پایان یافته؛ دوباره وارد شوید.") }
    val sameForm=operationEpoch==sessionEpoch&&state.value.screen==origin&&(state.value.dialog?:state.value.pane)==originDialog&&state.value.entryComplete==originEntry
    val inline=sameForm&&(origin!=Screen.CHAT||state.value.failedChat!=null)
    change { if(sameForm)it.copy(error=e.message?:"درخواست انجام نشد.",fieldErrors=(e as? FormValidationException)?.fields.orEmpty(),validationAttempt=it.validationAttempt+1)else it }
    if(!inline)notice(e.message?:"خطا در ارتباط")
   } finally { change { it.copy(posting=false,stage="") } }
  }
 }
 fun authenticate(register:Boolean,values:Map<String,String>) { post {
  val data=JSONObject()
  if(register) {
   validateFields(
    "full_name" to {require(values["full_name"].orEmpty().trim().length>=2){"نام و نام خانوادگی را کامل بنویسید."}},
    "phone" to {Validation.phone(values["phone"].orEmpty());Unit},
    "email" to {val e=values["email"].orEmpty().trim();require(e.isBlank()||android.util.Patterns.EMAIL_ADDRESS.matcher(e).matches()){"ایمیل معتبر وارد کنید."}},
    "password" to {require(values["password"].orEmpty().length>=6){"رمز عبور حداقل ۶ نویسه باشد."}})
   data.put("full_name",values["full_name"]!!.trim()).put("phone",Validation.phone(values["phone"].orEmpty())).put("password",values["password"])
   values["email"]?.trim()?.takeIf { it.isNotBlank() }?.let { require(android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()){ "ایمیل معتبر وارد کنید." };data.put("email",it) }
  } else {
   val id=values["identifier"].orEmpty().trim()
   validateFields(
    "identifier" to {require(id.isNotBlank()){"ایمیل یا شماره موبایل را وارد کنید."};if('@' in id)require(android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()){"ایمیل معتبر وارد کنید."}else {Validation.phone(id);Unit}},
    "password" to {require(values["password"].orEmpty().isNotBlank()){ "رمز عبور را وارد کنید." }})
   data.put("identifier",if('@' in id)id else Validation.phone(id)).put("password",values["password"])
  }
  val d=try { request(if(register)"/auth/register" else "/auth/login","POST",data,false) }catch(e:ApiException){throw ApiException(e.message.orEmpty(),e.status,"AUTH_FORM")}
  acceptAuth(d)
 } }
 private suspend fun acceptAuth(d:JSONObject) {
  require(d.str("token").isNotBlank()){ "توکن در پاسخ سرویس موجود نیست." }
  nextSession();api.token=d.str("token");val u=d.optJSONObject("user")?:JSONObject();persist(u)
  change { it.copy(authenticated=true,entryComplete=true,draftReady=it.pendingGuestMessage&&(it.draft.isNotBlank()||it.image!=null),pendingGuestMessage=false,user=u,subscription=d.optJSONObject("subscription")?:JSONObject(),dialog=null,error="") }
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
   runCatching { request("/consultations?limit=30&page=1") }.onSuccess { response ->
    if(epoch==sessionEpoch&&state.value.authenticated&&state.value.entryComplete)change{it.copy(sidebarHistory=response.objects("items"))}
   }
  }
 }
 fun sendOtp(phone:String) { post {
  require(state.value.config.optBoolean("sms_otp_enabled")){"ورود پیامکی در سرور فعال نیست."}
  require(System.currentTimeMillis()-otpSentAt>=60000){"برای ارسال دوباره یک دقیقه صبر کنید."}
  validateFields("phone" to {Validation.phone(phone);Unit});val p=Validation.phone(phone);request("/auth/otp/send","POST",JSONObject().put("phone",p),false);otpSentAt=System.currentTimeMillis()
  openDialog("otp","تأیید شماره موبایل",JSONObject().put("phone",p))
 } }
 fun verifyOtp(phone:String,code:String) { post {
  val c=Validation.digits(code);validateFields("otp" to {require(Regex("[0-9]{4,8}").matches(c)){"کد پیامکی معتبر وارد کنید."}})
  val d=try { request("/auth/otp/verify","POST",JSONObject().put("phone",phone).put("code",c),false) } catch(e:ApiException){throw ApiException(e.message.orEmpty(),e.status,"AUTH_FORM")}
  acceptAuth(d)
 } }
 fun logout() { if(state.value.posting)return;viewModelScope.launch { if(!expire())return@launch;setDraft("");change { it.copy(image=null,failedChat=null) };back.clear();returnTo=Screen.CHAT;browseAsGuest();notice("از حساب خارج شدید.") } }
 fun sendChat(retry:Boolean=false) {
  val s=state.value
  if(!retry&&s.draft.isBlank()&&s.image==null)return
  if(!s.authenticated){loginRequired(Screen.CHAT);return}
  if(s.posting)return
  if(retry&&s.chatStatus==ChatStatus.UNKNOWN&&!s.retryReviewed){openDialog("retryChat","بررسی پیش از ارسال دوباره");return}
  val payload=if(retry)s.failedChat else JSONObject().put("message",s.draft.trim().ifBlank { if(s.image!=null)"این تصویر را تحلیل کن" else "" })
   .put("conversation_id",s.conversationId?:JSONObject.NULL).also { if(s.image!=null)it.put("image_base64",s.image) }
  if(payload==null||payload.str("message").isBlank())return
  val epoch=sessionEpoch
  if(!retry) { change { it.copy(messages=it.messages+ChatMessage("user",payload.str("message"),image=it.image),image=null) };setDraft("") }
  change { it.copy(failedChat=null,draftReady=false,pendingGuestMessage=false,chatStatus=ChatStatus.SENDING,retryReviewed=false,stage="در حال ارسال پیام…") }
  post {
   try {
    if(s.streaming) streamChat(payload,epoch) else {
     val d=request("/chat","POST",payload)
     if(epoch==sessionEpoch)change { it.copy(conversationId=d.safeLong("conversation_id")?:it.conversationId,messages=it.messages+ApiContract.requireReply(ApiContract.chatMessage(d)),chatStatus=ChatStatus.COMPLETED,subscription=d.optJSONObject("subscription")?:it.subscription) };loadSidebar()
    }
   } catch(e:Exception) {
    if(e is CancellationException)throw e
    var preserved=false
    if(e is ApiException&&e.status==401){if(epoch==sessionEpoch&&expire(epoch)){setDraft(payload.str("message"));setImage(payload.str("image_base64").ifBlank { null });loginRequired(Screen.CHAT);preserved=true}}
    else if(epoch==sessionEpoch)change { it.copy(failedChat=payload,error=e.message.orEmpty(),chatStatus=if(e is ApiException&&ApiContract.definiteRejection(e.status))ChatStatus.FAILED else ChatStatus.UNKNOWN) }
    throw if(e is ApiException&&e.status==401)ApiException(if(preserved)"پیام حفظ شد؛ دوباره وارد شوید."else "پاسخ نشست قبلی کنار گذاشته شد.",0) else e
   }
  }
 }
 private suspend fun streamChat(payload:JSONObject,epoch:Int) {
  val buffer=StringBuilder();var done=false
  change { it.copy(messages=it.messages+ChatMessage("assistant",""),chatStatus=ChatStatus.SENDING,stage="در حال برقراری ارتباط…") }
  try { api.stream(payload).takeWhile { event ->
   if(epoch!=sessionEpoch)return@takeWhile false
   when(event.name) {
    "stage" -> change { it.copy(chatStatus=ChatStatus.RECEIVING,stage=ApiContract.stageTitle(event.data)) }
    "delta" -> { buffer.append(event.data.str("chunk"));change { it.copy(chatStatus=ChatStatus.RECEIVING,messages=it.messages.dropLast(1)+it.messages.last().copy(text=buffer.toString())) } }
    "articles" -> change { it.copy(messages=it.messages.dropLast(1)+it.messages.last().copy(citations=event.data.objects("articles"))) }
    "done" -> { val m=ApiContract.requireReply(ApiContract.chatMessage(event.data,buffer.toString(),state.value.messages.last().citations));done=true;change { it.copy(conversationId=event.data.safeLong("conversation_id")?:it.conversationId,messages=it.messages.dropLast(1)+m,chatStatus=ChatStatus.COMPLETED,subscription=event.data.optJSONObject("subscription")?:it.subscription) } }
    "error" -> throw ApiException(ApiContract.streamError(event.data))
   }
   event.name!="done"
  }.collect();if(done)loadSidebar();if(!done)throw ApiException("پاسخ جریانی کامل نشد؛ قبل از ارسال دوباره تاریخچه را بررسی کنید.")
  } catch(e:Exception) { if(state.value.messages.lastOrNull()?.role=="assistant"&&!ApiContract.hasReply(state.value.messages.last()))change { it.copy(messages=it.messages.dropLast(1)) };throw e }
 }
 fun feedback(id:Long,value:String) { post { request("/messages/$id/feedback","POST",JSONObject().put("feedback",value));change { it.copy(messages=it.messages.map { m->if(m.id==id)m.copy(feedback=value)else m }) };notice("بازخورد ثبت شد.") } }
 fun simplify(id:Long) { val conv=state.value.conversationId;post { val d=request("/messages/$id/simplify","POST",JSONObject());val text=ApiContract.simplifiedText(d);if(conv==state.value.conversationId)change { it.copy(messages=it.messages+ChatMessage("assistant",text)) } } }
 fun openConversation(id:Long) { if(!state.value.authenticated){loginRequired(Screen.HISTORY);return};if(state.value.posting)return;navigate(Screen.CHAT);val g=generation;post { val d=request("/consultations/$id");if(g!=generation)return@post;change { it.copy(conversationId=id,messages=d.objects("messages").filter { m->m.str("role") in listOf("user","assistant") }.map { m->if(m.str("role")=="assistant")ApiContract.chatMessage(m)else ChatMessage("user",m.str("content")) },failedChat=null,chatStatus=ChatStatus.IDLE) } } }
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
  val g=++generation;val epoch=sessionEpoch;loadJob?.cancel();change { it.copy(loading=true,error="",page=page) }
  loadJob=viewModelScope.launch {
   try {
    val d=request(endpoint,authenticated=s.screen !in listOf(Screen.LAWS,Screen.PLANS));if(g!=generation||epoch!=sessionEpoch)return@launch
    val paged=ApiContract.page(d,page,if(s.screen==Screen.LAWS)15 else if(s.screen in listOf(Screen.HISTORY,Screen.DOCUMENTS))30 else 20,s.lawTab)
    change { it.copy(loading=false,data=d,items=paged.items,hasNext=paged.hasNext) }
    if(s.screen==Screen.HISTORY&&page==1)change{it.copy(sidebarHistory=paged.items)}
    if(s.screen==Screen.ACCOUNT){val u=d.optJSONObject("user")?:d;change { it.copy(user=u,subscription=d.optJSONObject("subscription")?:JSONObject()) };persist(u)}
   } catch(c:CancellationException){throw c} catch(e:Exception){if(g==generation&&epoch==sessionEpoch){if(e is ApiException&&e.status==401){if(expire(epoch))notice("نشست پایان یافته؛ دوباره وارد شوید.")}else change { it.copy(loading=false,error=e.message.orEmpty()) }}}
  }
 }
 fun searchLaws(query:String) { change { it.copy(lawQuery=query.trim(),pane=null) };refresh() }
 fun lawTab(tab:String) { generation++;loadJob?.cancel();change { it.copy(lawTab=tab,pane=null,items=emptyList(),data=JSONObject(),loading=false,error="",page=1,hasNext=false) };refresh() }
 fun lawSource(source:String) { change{it.copy(lawSource=source,pane=null)};refresh() }
 fun openLawCollection(slug:String) { change{it.copy(lawSlug=slug,lawQuery="",lawSource="all")};lawTab("search") }
 fun clearLawCollection() { change{it.copy(lawSlug="")};refresh() }
 fun viewArticle(id:Long,inline:Boolean=false) { if(state.value.posting)return;if(inline)change{it.copy(pane=null)};val g=generation;post {val d=request("/laws/articles/$id",authenticated=false);if(g==generation){val article=d.optJSONObject("article")?:d;if(inline)change{it.copy(pane=AppDialog("article",article.str("law_name","متن قانون"),article),error="")}else openDialog("article",article.str("law_name","متن قانون"),article)}} }
 fun askAboutArticle(article:JSONObject) {setDraft("توضیح ساده و کاربردی ماده ${article.str("article_number")} ${article.str("law_name")} را بگو و کاربرد آن را توضیح بده.");change{it.copy(dialog=null)};navigate(Screen.CHAT)}
 fun answerQuestions(text:String) {if(state.value.posting||text.isBlank())return;setDraft(text);sendChat()}
 fun confirmRetryAfterReview() {change{it.copy(retryReviewed=true,dialog=null)};sendChat(true)}
 fun stopChat() { if(state.value.posting&&state.value.chatStatus in setOf(ChatStatus.SENDING,ChatStatus.RECEIVING)){postJob?.cancel();change{it.copy(chatStatus=ChatStatus.STOPPED,failedChat=null)}} }
 fun cycle(cycle:String) { change { it.copy(planCycle=cycle) } }
 fun calculator(id:String) { generation++;change { it.copy(calculator=id,result=null,error="") } }
 fun compute(values:Map<String,String>) { val g=generation;val calculator=Domain.calculators.first { it.id==state.value.calculator };post {
  change{it.copy(result=null)}
  val body=CalculatorContract.body(calculator,values)
  val result=request("/calculators/compute","POST",body,false);if(g==generation&&state.value.screen==Screen.CALCULATORS&&state.value.calculator==calculator.id)change { it.copy(result=result) }
 } }
 fun viewDocument(id:Long,edit:Boolean=false,inline:Boolean=false) { if(state.value.posting)return;if(inline)change{it.copy(pane=null)};val g=generation;post { val d=request("/documents/$id");if(g!=generation)return@post;val item=d.optJSONObject("document")?:d;val title=item.str("title","برگه قضایی");val kind=if(edit)"documentEdit"else "documentView";if(inline)change{it.copy(pane=AppDialog(kind,title,item),error="",fieldErrors=emptyMap())}else openDialog(kind,title,item) } }
 fun createDocument(type:String,title:String,details:String) { post { validateFields("title" to {require(title.isNotBlank()){"عنوان برگه را بنویسید."}},"details" to {require(details.isNotBlank()){"شرح ماجرا را بنویسید."}});request("/documents","POST",ApiContract.documentBody(type,title,details));change { it.copy(dialog=null) };notice("برگه تنظیم شد.");if(state.value.screen==Screen.DOCUMENTS)refresh() } }
 fun saveDocument(id:Long,title:String,content:String) { post { validateFields("title" to {require(title.isNotBlank()){"عنوان را بنویسید."}},"content" to {require(content.isNotBlank()){"متن سند را بنویسید."}});request("/documents/$id","PUT",JSONObject().put("title",title).put("content",content));change { val pane=it.pane;it.copy(dialog=null,pane=if(pane?.kind=="documentEdit"&&pane.data.optLong("id")==id)AppDialog("documentView",title,JSONObject(pane.data.toString()).put("title",title).put("content",content))else pane,documentDrafts=it.documentDrafts-id) };refresh();notice("ذخیره شد.") } }
 fun delete(kind:String,id:Long) { post { request("/$kind/$id","DELETE");if(kind=="deadlines")DeadlineReminder.cancel(getApplication(),id);change { it.copy(dialog=null,pane=if(it.pane?.data?.optLong("id")==id) null else it.pane,documentDrafts=if(kind=="documents")it.documentDrafts-id else it.documentDrafts) };if(kind=="consultations"){change{it.copy(sidebarHistory=it.sidebarHistory.filterNot{item->item.optLong("id")==id})};if(state.value.conversationId==id)change { it.copy(conversationId=null,messages=emptyList()) }};refresh();notice("حذف شد.") } }
 val sessionVersion:Int get()=sessionEpoch
 fun downloadUnauthorized(requestToken:String,expectedSession:Int=sessionEpoch) { viewModelScope.launch { if(api.token==requestToken&&sessionEpoch==expectedSession&&expire(expectedSession)){loginRequired(Screen.DOCUMENTS);notice("نشست پایان یافته؛ برای دریافت سند دوباره وارد شوید.")} } }
 fun download(id:Long) { if(id<=0){notice("شناسه سند معتبر نیست.");return};if(api.token.isBlank())loginRequired()else events.tryEmit(AppEffect.Download(id,sessionEpoch)) }
 fun openExternal(url:String) { runCatching { val u=java.net.URI(url);require(u.scheme=="https"&&u.host!=null);events.tryEmit(AppEffect.OpenUrl(url)) }.onFailure { notice("نشانی معتبر نیست.") } }
 fun copy(text:String) { events.tryEmit(AppEffect.Copy(text)) }
 fun analyze(kind:String,text:String,image:String?,consent:Boolean,noticeDate:String="") { val g=generation;post {
  change{it.copy(result=null)}
  validateFields("kind" to {require(kind in listOf("notice","contract")){"نوع برگه معتبر انتخاب کنید."}},"text" to {require(text.isNotBlank()||image!=null){"متن یا تصویر برگه را انتخاب کنید."}},"consent" to {require(consent){"رضایت ارسال برگه را تأیید کنید."}},"notice_date" to { if(noticeDate.isNotBlank()){ try { java.time.LocalDate.parse(noticeDate.trim()) } catch(_:Exception) { throw IllegalArgumentException("تاریخ ابلاغ را به شکل ۲۰۲۶-۱۰-۰۱ (سال-ماه-روز میلادی) وارد کنید.") } } })
  val body=ApiContract.scannerBody(kind,text,image,noticeDate)
  val d=request("/scanner/analyze","POST",body);if(g!=generation)return@post;change { it.copy(result=d.optJSONObject("analysis")?:d) };if(state.value.screen==Screen.SCANNER)refresh()
 } }
 fun addDeadline(title:String,date:String,notes:String) { post {
  validateFields("title" to {require(title.isNotBlank()){ "عنوان مهلت را وارد کنید." }},"date" to {java.time.LocalDate.parse(date);Unit})
  request("/deadlines","POST",ApiContract.deadlineBody(title,date,notes));change { it.copy(dialog=null) };refresh();notice("مهلت ثبت شد.")
 } }
 fun completeDeadline(id:Long) { post { request("/deadlines/$id","PATCH",JSONObject().put("status","done"));DeadlineReminder.cancel(getApplication(),id);refresh() } }
 fun calendar(title:String,date:String) { runCatching { java.time.LocalDate.parse(date) }.onSuccess { events.tryEmit(AppEffect.Calendar(title,date)) }.onFailure { notice("تاریخ سرویس قابل تبدیل به تقویم نیست.") } }
 fun reminder(id:Long,title:String,date:String) { runCatching { java.time.LocalDate.parse(date) }.onSuccess { events.tryEmit(AppEffect.Remind(id,title,date,state.value.user.str("id"))) }.onFailure { notice("تاریخ میلادی معتبر لازم است.") } }
 fun createTicket(subject:String,message:String) { post { validateFields("subject" to {require(subject.isNotBlank()){"موضوع را بنویسید."}},"message" to {require(message.isNotBlank()){"متن درخواست را بنویسید."}});request("/tickets","POST",JSONObject().put("subject",subject).put("message",message));change { it.copy(dialog=null) };refresh();notice("تیکت ارسال شد.") } }
 fun replyTicket(id:Long,message:String) { post { validateFields("message" to {require(message.isNotBlank()){ "متن پاسخ را بنویسید." }});request("/tickets/$id/reply","POST",JSONObject().put("message",message));change { it.copy(dialog=null) };refresh();notice("پاسخ ارسال شد.") } }
 fun saveProfile(name:String,email:String,nationalId:String) { post {
  val id=Validation.digits(nationalId)
  validateFields("full_name" to {require(name.trim().length>=2){"نام را کامل بنویسید."}},"email" to {require(email.isBlank()||android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()){ "ایمیل معتبر وارد کنید." }},"national_id" to {require(id.isBlank()||Validation.nationalId(id)){"کد ملی معتبر نیست."}})
  request("/me","PATCH",JSONObject().put("full_name",name.trim()).put("email",email.trim()).put("national_id",id));change{it.copy(profileRevision=it.profileRevision+1,user=JSONObject(it.user.toString()).put("full_name",name.trim()).put("email",email.trim()).put("national_id",id))};refresh();notice("مشخصات ذخیره شد.")
 } }
 fun changePassword(old:String,new:String,confirm:String) { post {
  validateFields("old_password" to {require(old.isNotBlank()){"رمز فعلی را وارد کنید."}},"new_password" to {require(new.length>=6){"رمز جدید حداقل ۶ نویسه باشد."}},"confirm_password" to {require(new==confirm){"تکرار رمز یکسان نیست."}})
  val id=state.value.user.str("email").ifBlank { state.value.user.str("phone") }
  try { request("/auth/login","POST",JSONObject().put("identifier",id).put("password",old),false) }catch(e:ApiException){if(e.status in listOf(400,401))throw FormValidationException(mapOf("old_password" to "رمز فعلی یا مشخصات ورود صحیح نیست."));throw ApiException(e.message.orEmpty(),e.status,"AUTH_FORM")}
  request("/me","PATCH",JSONObject().put("new_password",new));change { it.copy(dialog=null) };notice("رمز عبور تغییر کرد.")
 } }
 fun selectPlan(p:JSONObject) { if(!state.value.authenticated){loginRequired(Screen.PLANS);return};openDialog("checkout","بررسی سفارش",p) }
 fun validateDiscount(plan:Long,code:String) { post {
  validateFields("code" to {require(code.isNotBlank()){ "کد تخفیف را وارد کنید." }});val d=request("/billing/discount/validate","POST",JSONObject().put("plan_id",plan).put("code",code.trim()))
  require(d.optBoolean("valid",true)){d.str("message","کد نامعتبر است.")};notice("کد توسط سرور بررسی شد؛ مبلغ نهایی را در درگاه کنترل کنید.")
 } }
 fun checkout(plan:Long,code:String) { post {
  val body=JSONObject().put("plan_id",plan).put("app_return_url","hsai://payment/callback");if(code.isNotBlank())body.put("discount_code",code.trim())
  val d=request("/billing/checkout","POST",body)
  if(d.optBoolean("instant_activated")){change { it.copy(dialog=null) };navigate(Screen.ACCOUNT);notice("اشتراک طبق پاسخ سرور فعال شد.");return@post}
  val url=d.str("gateway_url");val parsed=java.net.URI(url);require(parsed.scheme=="https"&&parsed.host!=null){"نشانی درگاه امن نیست."}
  require(d.str("authority").isNotBlank()){ "شناسه پرداخت دریافت نشد." };vault.authority=d.str("authority");change { it.copy(dialog=null) };events.emit(AppEffect.OpenUrl(url))
 } }
 fun verifyPayment(authority:String=vault.authority) { if(authority.isBlank())return;if(!state.value.authenticated){loginRequired(Screen.PLANS);return};post { val d=request("/billing/payments/${encode(authority)}");openDialog("result","وضعیت پرداخت از سرور",d) } }
 fun paymentHistory() { post { val d=request("/billing/payments");openDialog("payments","تاریخچه پرداخت‌ها",d) } }
 val pendingPayment:Boolean get()=vault.authority.isNotBlank()
 private fun encode(s:String)=URLEncoder.encode(s,"UTF-8").replace("+","%20")
 companion object { val privateScreens=setOf(Screen.HISTORY,Screen.DOCUMENTS,Screen.SCANNER,Screen.DEADLINES,Screen.TICKETS) }
}
