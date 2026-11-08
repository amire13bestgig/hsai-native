@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset
import java.text.NumberFormat
import java.util.Locale

fun Screen.symbol():ImageVector=when(this){Screen.CHAT->NativeIcons.ChatBubbleOutline;Screen.HISTORY->NativeIcons.History;Screen.DOCUMENTS->NativeIcons.Description;Screen.SCANNER->NativeIcons.DocumentScanner;Screen.LAWS->NativeIcons.MenuBook;Screen.CALCULATORS->NativeIcons.Calculate;Screen.DEADLINES->NativeIcons.EventAvailable;Screen.PLANS->NativeIcons.WorkspacePremium;Screen.TICKETS->NativeIcons.SupportAgent;Screen.ACCOUNT->NativeIcons.PersonOutline}
fun fa(value:Number)=NumberFormat.getNumberInstance(Locale("fa")).format(value)
fun dateFa(raw:String):String { if(raw.isBlank())return "تاریخ ثبت نشده";if((raw.take(4).toIntOrNull()?:2000)<1700)return raw;return runCatching { val d=LocalDate.parse(raw.take(10));android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.MEDIUM,android.icu.util.ULocale("fa_IR@calendar=persian")).apply{timeZone=android.icu.util.TimeZone.getTimeZone("UTC")}.format(java.util.Date.from(d.atTime(12,0).toInstant(ZoneOffset.UTC))) }.getOrDefault(raw.take(10)) }

@Composable fun Page(hero:(@Composable ()->Unit)?=null,content:@Composable ColumnScope.()->Unit) {
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
  Column(Modifier.widthIn(max=1152.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).imePadding()) {
   if(hero!=null)hero()
   Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
  }
 }
}
@Composable fun PageHeader(title:String,description:String) {
 Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text(title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Text(description,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}
@Composable fun EmptyState(title:String,text:String) { GlassCard {Icon(NativeIcons.FolderOpen,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(32.dp));Text(title,style=MaterialTheme.typography.titleMedium);Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)} }
@Composable fun ErrorNotice(s:AppUiState,retry:(()->Unit)?=null) { if(s.error.isNotBlank())Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(15.dp)){Column(Modifier.fillMaxWidth().padding(14.dp)){Text(s.error,style=MaterialTheme.typography.bodyMedium);if(retry!=null)TextButton(onClick=retry){Text("تلاش دوباره")}}} }
@Composable fun Loading(s:AppUiState) { if(s.loading)Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.Center){CircularProgressIndicator(Modifier.size(27.dp),strokeWidth=2.dp)} }
@Composable fun Pager(s:AppUiState,vm:AppViewModel) { if(s.page>1||s.hasNext)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){OutlinedButton(onClick={vm.refresh(s.page-1)},enabled=s.page>1&&!s.loading){Text("صفحه قبل")};Text("صفحه ${fa(s.page)}",style=MaterialTheme.typography.bodySmall);OutlinedButton(onClick={vm.refresh(s.page+1)},enabled=s.hasNext&&!s.loading){Text("صفحه بعد")}} }
@Composable fun Field(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,password:Boolean=false,numeric:Boolean=false,multiline:Boolean=false,error:String?=null,firstError:Boolean=false,attempt:Int=0,enabled:Boolean=true,onEdited:(()->Unit)?=null) {
 var show by remember{mutableStateOf(false)}
 Column(modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Text(label,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
  OutlinedTextField(value=value,onValueChange={onValue(it);onEdited?.invoke()},enabled=enabled,modifier=Modifier.fillMaxWidth().then(Modifier.fieldFeedback(error,firstError,attempt)).semantics{contentDescription=label},isError=!error.isNullOrBlank(),shape=RoundedCornerShape(16.dp),singleLine=!multiline,minLines=if(multiline)4 else 1,maxLines=if(multiline)12 else 1,
   keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else if(numeric)KeyboardType.Decimal else KeyboardType.Text),visualTransformation=if(password&&!show)PasswordVisualTransformation()else VisualTransformation.None,
   textStyle=MaterialTheme.typography.bodyMedium,
   colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),focusedContainerColor=MaterialTheme.colorScheme.surface,unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant),
   supportingText=if(!error.isNullOrBlank()){{Text(error,style=MaterialTheme.typography.labelSmall)}}else if(numeric&&label.contains("تومان")&&value.isNotBlank()){{Text(runCatching{fa(Validation.number("amount",value))+" تومان"}.getOrDefault("عدد را به تومان وارد کنید."),style=MaterialTheme.typography.labelSmall)}}else null,
   trailingIcon=if(password){{IconButton(onClick={show=!show}){Icon(if(show)NativeIcons.VisibilityOff else NativeIcons.Visibility,"نمایش یا پنهان کردن رمز")}}}else null)
 }
}
@Composable fun Choice(label:String,value:String,options:List<Pair<String,String>>,error:String?=null,firstError:Boolean=false,attempt:Int=0,enabled:Boolean=true,onEdited:(()->Unit)?=null,onChoose:(String)->Unit) {
 var expanded by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Box {
   OutlinedButton(onClick={expanded=true},enabled=enabled,modifier=Modifier.fillMaxWidth().then(Modifier.fieldFeedback(error,firstError,attempt)).semantics{contentDescription=label},shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(13.dp)) {
    Text(options.find{it.first==value}?.second?:value,Modifier.weight(1f),textAlign=TextAlign.Start);Icon(NativeIcons.ExpandMore,null)
   }
   DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){options.forEach{(k,l)->DropdownMenuItem(text={Text(l)},enabled=enabled,onClick={onChoose(k);onEdited?.invoke();expanded=false})}}
  }
  if(!error.isNullOrBlank())Text(error,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.labelSmall)
 }
}
@Composable fun Action(text:String,busy:Boolean,enabled:Boolean=true,onClick:()->Unit) { Button(onClick=onClick,enabled=!busy&&enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){if(busy){CircularProgressIndicator(Modifier.size(18.dp),color=MaterialTheme.colorScheme.onPrimary,strokeWidth=2.dp);Spacer(Modifier.width(8.dp))};Text(text)} }
@Composable fun DataResult(data:JSONObject,depth:Int=0) {
 if(depth>6)return
 var details by remember(data){mutableStateOf(false)}
 val main=UiPresentation.primary(data);val extra=UiPresentation.secondary(data)
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  val row:@Composable (String)->Unit={ k->
   val v=data.opt(k)?:""
   Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
    Text(Domain.labels[k]?:k.replace('_',' '),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    when(v) {
     is JSONObject->DataResult(v,depth+1)
     is JSONArray->(0 until v.length()).forEach { i->val item=v.opt(i);if(item is JSONObject)DataResult(item,depth+1)else Text(item.toString()) }
     else->SelectionContainer{Text(when {
      v is Boolean->if(v)"بله"else "خیر"
      v is Number->fa(v)+(if(k.contains("toman"))" تومان"else "")
      k=="status"->mapOf("pending" to "در انتظار پرداخت","paid" to "پرداخت‌شده","verified" to "تأییدشده","success" to "موفق","failed" to "ناموفق","open" to "باز","closed" to "بسته‌شده")[v.toString()]?:v.toString()
      else->v.toString()
     })}
    }
   }
  }
  main.forEach{row(it)}
  if(extra.isNotEmpty()) {
   TextButton(onClick={details=!details}){Text(if(details)"بستن جزئیات تکمیلی"else "جزئیات تکمیلی (${fa(extra.size)})")}
   if(details)extra.forEach{row(it)}
  }
 }
}
@Composable fun OverflowActions(label:String,enabled:Boolean=true,content:@Composable (()->Unit)->Unit) {
 var open by remember{mutableStateOf(false)}
 Box {
  IconButton(onClick={open=true},enabled=enabled,modifier=Modifier.size(48.dp)){Icon(NativeIcons.MoreVert,label)}
  DropdownMenu(expanded=open,onDismissRequest={open=false}){content{open=false}}
 }
}

@Composable fun ChatScreen(s:AppUiState,vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEditImage:()->Unit) {
 val list=rememberLazyListState()
 LaunchedEffect(s.messages.size,s.messages.lastOrNull()?.text,s.posting){if(s.messages.isNotEmpty())list.animateScrollToItem((list.layoutInfo.totalItemsCount-1).coerceAtLeast(0))}
 Column(Modifier.fillMaxSize().imePadding(),horizontalAlignment=Alignment.CenterHorizontally) {
 LazyColumn(state=list,modifier=Modifier.weight(1f).widthIn(max=1000.dp).fillMaxWidth(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  if(s.messages.isEmpty())item {HomeContent(s,vm)}
  itemsIndexed(s.messages){index,m->MessageCard(m,s.posting,vm,index==s.messages.lastIndex)}
  if(s.posting&&s.chatStatus in setOf(ChatStatus.SENDING,ChatStatus.RECEIVING))item{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp);Text(s.stage.ifBlank{"در حال بررسی و آماده‌سازی پاسخ…"},style=MaterialTheme.typography.bodySmall)}}
  if(s.failedChat!=null)item {ErrorNotice(s);OutlinedButton(onClick={if(s.chatStatus==ChatStatus.UNKNOWN)vm.openDialog("retryChat","بررسی پیش از ارسال دوباره")else vm.sendChat(true)},enabled=!s.posting){Text("ارسال دوباره همان پیام")}}
 }
 Surface(modifier=Modifier.widthIn(max=1000.dp).fillMaxWidth(),color=MaterialTheme.colorScheme.background) { Column(Modifier.padding(horizontal=13.dp,vertical=9.dp)) {
  if(s.image!=null)ImageAttachment(s.image,onEditImage,{vm.setImage(null)})
  if(s.draftReady)Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(12.dp)) {
   Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=6.dp)) {
    Text("پیامت محفوظ و آماده ارسال است",style=MaterialTheme.typography.labelMedium)
    TextButton(onClick={vm.sendChat()},enabled=!s.posting,modifier=Modifier.fillMaxWidth()){Text("ارسال پیام آماده‌شده")}
   }
  }
  if(!s.posting&&s.chatStatus in setOf(ChatStatus.STOPPED,ChatStatus.UNKNOWN,ChatStatus.FAILED)) {
   Text(when(s.chatStatus){ChatStatus.STOPPED->"دریافت متوقف شد؛ ثبت درخواست در سرور ممکن است ادامه داشته باشد.";ChatStatus.UNKNOWN->"وضعیت ارسال نامشخص است؛ قبل از تکرار، تاریخچه را بررسی کنید.";else->"درخواست انجام نشد؛ پیام خطا را بررسی کنید."},Modifier.fillMaxWidth().padding(8.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   TextButton(onClick={vm.navigate(Screen.HISTORY)},enabled=s.authenticated){Text("بررسی تاریخچه گفتگوها")}
  }
  SiteGlass(strong=true) {Row(Modifier.fillMaxWidth().padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   var attachMenu by remember{mutableStateOf(false)}
   Box{IconButton(onClick={attachMenu=true}){Icon(NativeIcons.AttachFile,"افزودن تصویر")};DropdownMenu(expanded=attachMenu,onDismissRequest={attachMenu=false}){DropdownMenuItem(text={Text("انتخاب تصویر")},onClick={attachMenu=false;onImage()});DropdownMenuItem(text={Text("دوربین")},onClick={attachMenu=false;onCamera()})}}
   OutlinedTextField(value=s.draft,onValueChange=vm::setDraft,placeholder={Text("ماجرا را ساده بنویسید…",style=MaterialTheme.typography.bodySmall)},modifier=Modifier.weight(1f),maxLines=5,shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,focusedContainerColor=Color.Transparent))
   FilledIconButton(onClick={vm.sendChat()},enabled=!s.posting&&(s.draft.isNotBlank()||s.image!=null),modifier=Modifier.size(46.dp),colors=IconButtonDefaults.filledIconButtonColors(containerColor=SiteNavy,contentColor=SiteGold,disabledContainerColor=MaterialTheme.colorScheme.onSurface.copy(alpha=.06f),disabledContentColor=MaterialTheme.colorScheme.onSurface.copy(alpha=.38f))){Icon(NativeIcons.Send,"ارسال پیام")}
  }
  }
  if(s.posting&&s.stage.isNotBlank())TextButton(onClick=vm::stopChat){Text("توقف دریافت پاسخ")}
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("پاسخ مستند؛ جایگزین وکالت در جلسه دادگاه نیست.",modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(verticalAlignment=Alignment.CenterVertically){Text("جریانی",style=MaterialTheme.typography.labelSmall);Switch(checked=s.streaming,onCheckedChange=vm::setStream,enabled=!s.posting,modifier=Modifier.height(38.dp).semantics{contentDescription="پاسخ جریانی"},colors=SwitchDefaults.colors(checkedThumbColor=SiteGold,checkedTrackColor=SiteNavy,uncheckedThumbColor=MaterialTheme.colorScheme.outline,uncheckedTrackColor=Color.Transparent,uncheckedBorderColor=MaterialTheme.colorScheme.outlineVariant))}}
 } }
 }
}
@Composable fun HomeContent(s:AppUiState,vm:AppViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  BrandEmblem(Modifier.size(48.dp))
  Text("چه کمکی از دستم برمی‌آید؟",modifier=Modifier.widthIn(max=720.dp),style=MaterialTheme.typography.titleLarge.copy(lineHeight=30.sp),fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
  Text("موضوع حقوقی‌تان را بنویسید؛ مدارک را هم می‌توانید پیوست کنید.",modifier=Modifier.widthIn(max=760.dp),color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val action:@Composable (Screen,Modifier)->Unit={screen,modifier ->
    val scan=screen==Screen.SCANNER
    Surface(onClick={vm.navigate(screen)},modifier=modifier,shape=RoundedCornerShape(18.dp),color=if(scan)SiteNavy else MaterialTheme.colorScheme.secondaryContainer,border=BorderStroke(1.dp,SiteGold.copy(alpha=.7f))) {
     Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
      Icon(if(scan)NativeIcons.PhotoCamera else NativeIcons.Description,null,Modifier.size(28.dp),tint=if(scan)SiteGold else Copper)
      Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
       Text(if(scan)"عکس ابلاغیه ثنا، پیامک دادگاه یا قرارداد را بفرستید"else "تنظیم برگه شکایت",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold,color=if(scan)Color(0xFFE8C477)else MaterialTheme.colorScheme.onSecondaryContainer)
       Text(if(scan)"با قابلیت پوشاندن مشخصات حساس، قبل از ارسال"else "پرسش‌وپاسخ گام‌به‌گام، بدون فرم سخت اداری",style=MaterialTheme.typography.bodySmall,color=if(scan)Color(0xFFBCC9D4)else MaterialTheme.colorScheme.onSecondaryContainer)
      }
     }
    }
   }
   if(maxWidth>=600.dp)Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){action(Screen.SCANNER,Modifier.weight(1.7f));action(Screen.DOCUMENTS,Modifier.weight(1f))}
   else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){action(Screen.SCANNER,Modifier.fillMaxWidth());action(Screen.DOCUMENTS,Modifier.fillMaxWidth())}
  }
  var moreTopics by rememberSaveable{mutableStateOf(false)}
  val topics=listOf("پول، چک و بدهی" to "طلبم را نمی‌دهند یا چکم برگشت خورده","خانه، مستأجر و صاحب‌خانه" to "مشکل پول پیش، تخلیه خانه یا قولنامه ملک","خانواده و ازدواج" to "مهریه، نفقه، طلاق یا حضانت فرزند","محل کار و بیمه" to "کارفرما حقوق، بیمه یا سنواتم را نداده","اینترنت، کلاهبرداری و جرم" to "کلاه سرم رفته، فیشینگ شده یا مزاحمم شده‌اند","ارث و میراث خانوادگی" to "انحصار وراثت و تقسیم ارث بعد از فوت")
  BoxWithConstraints(Modifier.fillMaxWidth()) {
   val scale=androidx.compose.ui.platform.LocalDensity.current.fontScale
   val columns=if(maxWidth>=900.dp&&scale<1.3f)3 else if(maxWidth>=600.dp&&scale<1.4f)2 else 1
   Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
    (if(moreTopics)topics else topics.take(3)).chunked(columns).forEach { row->Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
     row.forEach { (title,description)->
      Surface(onClick={vm.chooseTopic(description)},modifier=Modifier.weight(1f).fillMaxHeight(),shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant.copy(alpha=.7f)),tonalElevation=0.dp,shadowElevation=0.dp) {
       Column(Modifier.fillMaxWidth().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold)
        Text(description,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Medium)
       }
      }
     }
     repeat(columns-row.size){Spacer(Modifier.weight(1f))}
    }}
   }
  }
  TextButton(onClick={moreTopics=!moreTopics}){Text(if(moreTopics)"موضوع‌های کمتر"else "موضوع‌های بیشتر")}
 }
}
@Composable fun MessageCard(m:ChatMessage,busy:Boolean,vm:AppViewModel,isLatest:Boolean=false) {
 val user=m.role=="user"
 val content:@Composable ColumnScope.()->Unit = {
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){if(user)Icon(NativeIcons.PersonOutline,null,Modifier.size(18.dp),tint=SiteDesign.Brass300)else BrandEmblem(Modifier.size(22.dp));Text(if(m.role=="user")"شما"else "حق‌سنج",style=MaterialTheme.typography.labelSmall,color=if(user)SiteDesign.Brass300 else MaterialTheme.colorScheme.primary)}
  if(m.image!=null)NativeImage(m.image)
  if(m.text.isNotBlank())SelectionContainer {NativeMarkdown(m.text)}
  else if(busy&&isLatest)Text("در حال دریافت پاسخ…",style=MaterialTheme.typography.bodySmall)
  else if(m.document==null&&m.questions.isEmpty()&&m.citations.isEmpty()&&m.verification==null&&m.suggestions.isEmpty())Text("پاسخ متنی در خروجی موجود نیست.",style=MaterialTheme.typography.bodySmall)
  if(!user&&m.verification!=null) {
   val details=JSONObject();ApiContract.verificationKeys(m.verification).forEach{k->details.put(k,m.verification.opt(k))}
   if(details.length()>0){var show by remember{mutableStateOf(false)};TextButton(onClick={show=!show}){Text(if(show)"بستن راهنمای عملی"else "راهنمای عملی و نتیجه بررسی")};if(show){Text("راستی‌آزمایی و امتیاز اطمینان، ادعای سرور است و تأیید مستقل حقوقی محسوب نمی‌شود.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error);DataResult(details)}}
  }
  if(!user&&isLatest&&m.questions.isNotEmpty())QuickQuestionForm(m.questions,busy,vm::answerQuestions)
  if(!user&&isLatest&&m.suggestions.isNotEmpty()) {
   var all by remember(m.suggestions){mutableStateOf(false)}
   Text("پیشنهادهای پاسخ",style=MaterialTheme.typography.labelMedium)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    (if(all)m.suggestions else m.suggestions.take(2)).forEach { suggestion->
     SuggestionChip(onClick={vm.setDraft(suggestion)},enabled=!busy,modifier=Modifier.semantics{contentDescription="افزودن به پیش‌نویس: "+suggestion},label={Text(suggestion,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)})
    }
   }
   if(m.suggestions.size>2)TextButton(onClick={all=!all}){Text(if(all)"پیشنهادهای کمتر"else "پیشنهادهای بیشتر")}
  }
  if(m.citations.isNotEmpty()){var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Icon(NativeIcons.MenuBook,null,Modifier.size(17.dp));Text("${fa(m.citations.size)} استناد قانونی")};if(expanded)m.citations.forEach { c->Text(c.str("law_name")+" — ماده "+c.str("article_number"),fontWeight=FontWeight.Medium);SelectionContainer{Text(c.str("official_text"),style=MaterialTheme.typography.bodySmall)};if(c.str("plain_explanation").isNotBlank())Text(c.str("plain_explanation"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);if(c.optLong("id")>0)TextButton(onClick={vm.viewArticle(c.optLong("id"))},enabled=!busy){Text("مشاهده ماده و توضیحات")} }}
  if(m.document!=null)OutlinedButton(onClick={vm.openDialog("documentView",m.document.str("title","برگه قضایی آماده"),m.document)}){Text("مشاهده برگه قضایی آماده")}
  if(m.role=="assistant"&&m.text.isNotBlank())FlowRow(horizontalArrangement=Arrangement.spacedBy(3.dp)){TextButton(onClick={vm.copy(m.text)}){Icon(NativeIcons.ContentCopy,"کپی",Modifier.size(16.dp));Text("کپی")};if(m.id!=null){TextButton(onClick={vm.feedback(m.id,"like")},enabled=!busy){Icon(if(m.feedback=="like")NativeIcons.Check else NativeIcons.ThumbUp,"مفید بود",Modifier.size(16.dp))};TextButton(onClick={vm.feedback(m.id,"dislike")},enabled=!busy){Icon(NativeIcons.ThumbDown,"مفید نبود",Modifier.size(16.dp))};TextButton(onClick={vm.simplify(m.id)},enabled=!busy){Text("ساده‌تر بگو")}}}
 }
 if(user)Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.TopStart) {
  Surface(Modifier.fillMaxWidth(.88f),shape=RoundedCornerShape(topStart=4.dp,topEnd=16.dp,bottomStart=16.dp,bottomEnd=16.dp),color=SiteNavy,contentColor=Color.White,border=BorderStroke(1.dp,Color.White.copy(alpha=.1f)),shadowElevation=3.dp) {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
  }
 } else GlassCard(content=content)
}
@Composable fun NativeImage(data:String,modifier:Modifier=Modifier) {val bitmap=remember(data){runCatching{val bytes=Base64.decode(data.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(bytes,0,bytes.size)}.getOrNull()};if(bitmap!=null)Image(bitmap.asImageBitmap(),"تصویر انتخاب‌شده",modifier.fillMaxWidth().heightIn(max=180.dp))}
@Composable fun ImageAttachment(data:String,onEdit:()->Unit,onRemove:()->Unit,showPreview:Boolean=true) {Row(verticalAlignment=Alignment.CenterVertically){if(showPreview)NativeImage(data,Modifier.width(58.dp).height(52.dp));TextButton(onClick=onEdit){Text("پوشاندن مشخصات")};IconButton(onClick=onRemove){Icon(NativeIcons.Close,"حذف تصویر")}}}
@Composable fun AccountScreen(s:AppUiState,vm:AppViewModel) {Page {
 PageHeader("میز کار و تنظیمات حساب","اطلاعات هویتی، امنیت حساب، اشتراک و پشتیبانی")
 GlassCard {
  Icon(NativeIcons.PersonOutline,null,tint=MaterialTheme.colorScheme.secondary,modifier=Modifier.size(32.dp))
  Text(s.user.str("full_name","حساب کاربری"),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
  Text(s.subscription.str("plan_name","حساب حق‌سنج"),color=MaterialTheme.colorScheme.secondary)
  if(s.user.str("email").isNotBlank())Text(s.user.str("email"))
  if(s.user.str("phone").isNotBlank())Text(s.user.str("phone"))
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedButton(onClick={vm.navigate(Screen.PLANS)}){Text("ارتقا / تمدید اشتراک")}
   TextButton(onClick={vm.openDialog("logout","خروج از حساب")},enabled=!s.posting){Text("خروج")}
  }
 }
 var tab by rememberSaveable{mutableStateOf("profile")}
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  FilterChip(selected=tab=="profile",onClick={tab="profile"},label={Text("پروفایل و هویت")})
  FilterChip(selected=tab=="security",onClick={tab="security"},label={Text("امنیت و رمز عبور")})
  FilterChip(selected=false,onClick={vm.navigate(Screen.PLANS)},label={Text("اشتراک و پرداخت‌ها")})
  FilterChip(selected=false,onClick={vm.navigate(Screen.TICKETS)},label={Text("تیکت‌های پشتیبانی")})
 }
 Loading(s);ErrorNotice(s,{vm.refresh()})
 if(tab=="profile") {
  val owner=s.user.optLong("id")
  var name by remember(owner){mutableStateOf(s.user.str("full_name"))}
  var email by remember(owner){mutableStateOf(s.user.str("email"))}
  var national by remember(owner){mutableStateOf(s.user.str("national_id"))}
  var dirty by remember(owner){mutableStateOf(false)}
  var revision by remember(owner){mutableIntStateOf(s.profileRevision)}
  LaunchedEffect(s.user,s.profileRevision){if(revision!=s.profileRevision){dirty=false;revision=s.profileRevision};if(!dirty){name=s.user.str("full_name");email=s.user.str("email");national=s.user.str("national_id")}}
  AdaptiveSplit(first={GlassCard {
   Text("مشخصات هویتی",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Field("نام و نام خانوادگی رسمی",name,{name=it;dirty=true},error=s.fieldErrors["full_name"],enabled=!s.posting&&!s.loading,onEdited={vm.inputEdited("full_name")},firstError=s.firstField("full_name"),attempt=s.validationAttempt)
   Field("کد ملی ۱۰ رقمی (اختیاری)",national,{national=it;dirty=true},numeric=true,error=s.fieldErrors["national_id"],enabled=!s.posting&&!s.loading,onEdited={vm.inputEdited("national_id")},firstError=s.firstField("national_id"),attempt=s.validationAttempt)
   Field("نشانی ایمیل (اختیاری)",email,{email=it;dirty=true},error=s.fieldErrors["email"],enabled=!s.posting&&!s.loading,onEdited={vm.inputEdited("email")},firstError=s.firstField("email"),attempt=s.validationAttempt)
   Action("ذخیره مشخصات هویتی",s.posting,enabled=!s.loading){vm.saveProfile(name,email,national)}
  }},second={GlassCard {
   Text("اشتراک شما",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(s.subscription.str("plan_name","اطلاعات اشتراک موجود نیست."))
   if(s.subscription.has("days_remaining"))Text("${fa(s.subscription.optInt("days_remaining"))} روز اعتبار باقی‌مانده")
   OutlinedButton(onClick={vm.navigate(Screen.PLANS)}){Text("اشتراک و پرداخت‌ها")}
  }})
 } else GlassCard {
  Text("امنیت و رمز عبور",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  Text("رمز عبور روی این دستگاه ذخیره نمی‌شود.")
  OutlinedButton(onClick={vm.openDialog("password","تغییر رمز عبور")}){Text("تغییر رمز عبور")}
 }
} }
@Composable fun HistoryScreen(s:AppUiState,vm:AppViewModel) {WorkspaceListDetail(s,vm,setOf("historyPreview"),list={wide->Page {
 PageHeader("گفتگوهای من","گفتگوی قبلی را ادامه دهید یا موضوع تازه‌ای شروع کنید.")
 OutlinedButton(onClick=vm::newChat){Icon(NativeIcons.Add,null);Text("گفتگوی جدید")}
 Loading(s);ErrorNotice(s,{vm.refresh()})
 if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())EmptyState("هنوز گفتگویی ندارید","اولین سؤال خود را در بخش گفتگو مطرح کنید.")
 AdaptiveCards(s.items) { c->
  Text(c.str("title","گفتگوی حقوقی"),fontWeight=FontWeight.Medium)
  Text(dateFa(c.str("updated_at",c.str("created_at"))),style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Button(onClick={if(wide)vm.previewConversation(c.optLong("id"))else vm.openConversation(c.optLong("id"))},enabled=!s.posting){Text(if(wide)"پیش‌نمایش گفتگو"else "ادامه گفتگو")};Spacer(Modifier.weight(1f))
   OverflowActions("گزینه‌های گفتگو "+c.str("title"),!s.posting){close->
    DropdownMenuItem(text={Text("حذف گفتگو",color=MaterialTheme.colorScheme.error)},onClick={close();vm.openDialog("delete","حذف گفتگو",JSONObject().put("kind","consultations").put("id",c.optLong("id")))})
   }
  }
 };Pager(s,vm)
}},detail={d->HistoryDetail(d,s,vm,Modifier.fillMaxSize())}) }
@Composable fun DocumentScreen(s:AppUiState,vm:AppViewModel) {WorkspaceListDetail(s,vm,setOf("documentView","documentEdit"),list={wide->Page {
 PageHeader("اوراق قضایی شما","پیش‌نویس‌ها را پیش از چاپ و ثبت بررسی کنید.")
 Button(onClick={vm.openDialog("documentCreate","تنظیم برگه قضایی")}){Icon(NativeIcons.Add,null);Text("تنظیم برگه جدید")}
 Loading(s);ErrorNotice(s,{vm.refresh()})
 if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())EmptyState("برگه‌ای ثبت نشده","شرح ماجرا را به پیش‌نویس سند تبدیل کنید.")
 AdaptiveCards(s.items) { d->
  Text(d.str("title",d.str("doc_type","سند قضایی")),fontWeight=FontWeight.Medium)
  Text(d.str("doc_type")+" · "+dateFa(d.str("created_at")),style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Button(onClick={vm.viewDocument(d.optLong("id"),inline=wide)},enabled=!s.posting){Text("مشاهده")};Spacer(Modifier.weight(1f))
   OverflowActions("گزینه‌های سند "+d.str("title"),!s.posting){close->
    DropdownMenuItem(text={Text("ویرایش")},onClick={close();vm.viewDocument(d.optLong("id"),true,wide)})
    DropdownMenuItem(text={Text("دریافت Word")},onClick={close();vm.download(d.optLong("id"))})
    DropdownMenuItem(text={Text("حذف سند",color=MaterialTheme.colorScheme.error)},onClick={close();vm.openDialog("delete","حذف سند",JSONObject().put("kind","documents").put("id",d.optLong("id")))})
   }
  }
 };Pager(s,vm)
}},detail={d->ScrollableDetail(d,s,vm,false)}) }
@Composable fun ScannerScreen(s:AppUiState,vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEdit:()->Unit) {Page{
 PageHeader("تحلیل مدارک","ابلاغیه یا قرارداد را برای بررسی انتخاب کنید.")
 var kind by rememberSaveable{mutableStateOf("notice")};var text by rememberSaveable{mutableStateOf("")};var consent by rememberSaveable{mutableStateOf(false)}
 GlassCard{Choice("نوع برگه",kind,listOf("notice" to "ابلاغیه دادگاه","contract" to "قرارداد"),error=s.fieldErrors["kind"],enabled=!s.posting,onEdited={vm.inputEdited("kind",true)},firstError=s.firstField("kind"),attempt=s.validationAttempt){kind=it};FlowRow{OutlinedButton(onClick=onImage){Icon(NativeIcons.AddPhotoAlternate,null);Text("گالری")};Spacer(Modifier.width(10.dp));OutlinedButton(onClick=onCamera){Icon(NativeIcons.PhotoCamera,null);Text("دوربین")}};if(s.image!=null){NativeImage(s.image);ImageAttachment(s.image,onEdit,{vm.setImage(null)},showPreview=false)};Field("یا متن برگه را وارد کنید",text,{text=it},multiline=true,error=s.fieldErrors["text"],enabled=!s.posting,onEdited={vm.inputEdited("text",true)},firstError=s.firstField("text"),attempt=s.validationAttempt);Row(Modifier.then(Modifier.fieldFeedback(s.fieldErrors["consent"],s.firstField("consent"),s.validationAttempt)).toggleable(value=consent,enabled=!s.posting,role=Role.Checkbox,onValueChange={consent=it;vm.inputEdited("consent")}),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=consent,onCheckedChange=null,enabled=!s.posting);Text("مشخصات حساس را بررسی کرده‌ام و با ارسال برگه برای تحلیل موافقم.",style=MaterialTheme.typography.bodySmall)};s.fieldErrors["consent"]?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)};Action("تحلیل برگه",s.posting){vm.analyze(kind,text,s.image,consent)}}
 ErrorNotice(s);s.result?.let{GlassCard{Text("نتیجه تحلیل",style=MaterialTheme.typography.titleMedium);Text("این تحلیل و هر مهلت پیشنهادی، خروجی هوش مصنوعی است و ممکن است نامرتبط یا اشتباه باشد. نوع برگه، تاریخ ابلاغ و مهلت قانونی را با اصل سند و مرجع قضایی مستقلاً تأیید کنید؛ اپ یادآور خودکار ثبت نمی‌کند.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);DataResult(it)}}
 Text("تحلیل‌های قبلی",style=MaterialTheme.typography.titleMedium);Loading(s);if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())Text("تحلیلی ثبت نشده است.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant);AdaptiveCards(s.items) { a->Text(a.str("title","تحلیل برگه"),fontWeight=FontWeight.Medium);var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Text(if(expanded)"بستن تحلیل"else "نمایش تحلیل")};if(expanded)DataResult(a.optJSONObject("analysis")?:JSONObject().put("text",a.str("raw_text")));if(a.optLong("id")>0)TextButton(onClick={vm.openDialog("delete","حذف تحلیل",JSONObject().put("kind","scanner").put("id",a.optLong("id")))}){Text("حذف تحلیل")}};Pager(s,vm)
} }
@Composable fun LawScreen(s:AppUiState,vm:AppViewModel) {WorkspaceListDetail(s,vm,if(s.lawTab=="search")setOf("article")else emptySet(),list={wide->Page(hero={SiteHero("قانون موردنظرتان را پیدا کنید","متن قانون، توضیح ساده و مقایسه تغییرات.","جستجوی رایگان در متن رسمی قوانین",NativeIcons.MenuBook,footer={SiteTabs(listOf("search" to "جستجو","comparisons" to "قدیم و جدید","glossary" to "واژه‌نامه","collections" to "مجموعه‌ها"),s.lawTab,vm::lawTab)})}) {
 if(s.lawTab=="search"){if(s.lawSlug.isNotBlank())AssistChip(onClick=vm::clearLawCollection,label={Text("حذف فیلتر مجموعه")});var q by rememberSaveable{mutableStateOf(s.lawQuery)};GlassCard{Field("کلمه یا موضوع حقوقی",q,{q=it});Choice("نوع منبع",s.lawSource,listOf("all" to "همه قوانین و رأی‌ها","statute" to "قوانین مجلس","precedent" to "رأی‌های وحدت رویه","advisory" to "نظرات مشورتی قضات","judiciary_bylaw" to "بخشنامه‌های قوه قضاییه"),onChoose=vm::lawSource);Action("جستجو در قوانین",s.loading){vm.searchLaws(q)};FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("دادگاه صلح","چک صیادی","پول پیش و تخلیه","کلاهبرداری رایانه‌ای","مهریه ۱۱۰ سکه","خیانت در امانت").forEach{t->SuggestionChip(onClick={q=t;vm.searchLaws(t)},label={Text(t)})}}}}
 if(s.lawTab=="glossary")LegalGlossary()else {
 Loading(s);ErrorNotice(s,{vm.refresh(s.page)})
 if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())EmptyState(if(s.lawQuery.isBlank()&&s.lawTab=="search")"دنبال چه قانونی می‌گردید؟"else "نتیجه‌ای یافت نشد","موضوع را کوتاه و دقیق وارد کنید یا دسته دیگری را ببینید.")
 AdaptiveCards(s.items) { a->if(s.lawTab=="search"){Text(a.str("law_name")+" — ماده "+a.str("article_number"),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);SelectionContainer{Text(a.str("official_text"))};if(a.str("plain_explanation").isNotBlank()){var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Text("شرح ساده")};if(expanded)Text(a.str("plain_explanation"))};if(a.optLong("id")>0)TextButton(onClick={vm.viewArticle(a.optLong("id"),wide)},enabled=!s.posting){Text("متن کامل و کاربرد ماده")}}else if(s.lawTab=="comparisons"){LawComparison(a)}else{Text(a.str("title",a.str("name",a.str("law_name","اطلاعات قانونی"))),fontWeight=FontWeight.Medium);if(a.has("article_count"))Text("${fa(a.optInt("article_count"))} ماده",style=MaterialTheme.typography.bodySmall);if(a.str("category").isNotBlank())Text(a.str("category"),style=MaterialTheme.typography.bodySmall);if(a.str("slug").isNotBlank())TextButton(onClick={vm.openLawCollection(a.str("slug"))}){Text("مشاهده مواد این مجموعه")}} };Pager(s,vm)
 }
}},detail={d->ScrollableDetail(d,s,vm,true)}) }
@Composable fun CalculatorScreen(s:AppUiState,vm:AppViewModel) {Page(hero={SiteHero("محاسبه هزینه‌ها و حقوق مالی","مبالغ را به تومان و تاریخ‌های ورودی را شمسی وارد کنید.","همه عددها به تومان، نه ریال",NativeIcons.Calculate)}) {
 val calc=Domain.calculators.first {it.id==s.calculator}
 val values=remember(s.calculator){mutableStateMapOf<String,String>().apply{calc.fields.forEach { put(it.key,if(it.key=="copies")"1"else it.options.firstOrNull()?.first?:if(it.key.contains("count")||it.key.contains("will")||it.key.contains("debts")||it.key.contains("stocks")||it.key.contains("deposits"))"0"else "") }}}
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val wide=maxWidth>=800.dp
  val selection:@Composable ()->Unit={
   if(wide)Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Domain.calculators.forEach { c->OutlinedButton(onClick={vm.calculator(c.id)},enabled=!s.posting,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(c.id==s.calculator)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {Text(c.title,Modifier.fillMaxWidth(),textAlign=TextAlign.Start)} }
   } else Choice("انتخاب محاسبه‌گر",s.calculator,Domain.calculators.map{it.id to it.title},enabled=!s.posting,onChoose=vm::calculator)
  }
  val form:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
   GlassCard {
    Text(calc.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    CalculatorContract.fields(calc,values).forEach { f->
     if(f.multiple) {
      val selected=values[f.key].orEmpty().split(',').filter{it.isNotBlank()}.toSet()
      Column(Modifier.then(Modifier.fieldFeedback(s.fieldErrors[f.key],s.firstField(f.key),s.validationAttempt)),verticalArrangement=Arrangement.spacedBy(6.dp)) {
       Text(f.label,style=MaterialTheme.typography.labelLarge)
       f.options.forEach { (key,label)->Row(Modifier.fillMaxWidth().toggleable(value=key in selected,enabled=!s.posting,role=Role.Checkbox,onValueChange={checked->values[f.key]=(if(checked)selected+key else selected-key).joinToString(",");vm.inputEdited(f.key,true)}),verticalAlignment=Alignment.CenterVertically){Checkbox(checked=key in selected,onCheckedChange=null,enabled=!s.posting);Text(label)} }
       s.fieldErrors[f.key]?.let{Text(it,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)}
      }
     }else if(f.options.isNotEmpty())Choice(f.label,values[f.key].orEmpty(),f.options,error=s.fieldErrors[f.key],enabled=!s.posting,onEdited={vm.inputEdited(f.key,true)},firstError=s.firstField(f.key),attempt=s.validationAttempt){values[f.key]=it}
     else Field(f.label,values[f.key].orEmpty(),{values[f.key]=it},numeric=f.numeric,error=s.fieldErrors[f.key],enabled=!s.posting,onEdited={vm.inputEdited(f.key,true)},firstError=s.firstField(f.key),attempt=s.validationAttempt)
    }
    if(s.calculator in setOf("expert_fee","arbitration_fee","notary_tariff","office_services_fee"))Text("این ابزار برآورد سرور را نمایش می‌دهد؛ رقم قطعی و تعرفه معتبر را با مرجع مربوط بررسی کنید.",style=MaterialTheme.typography.bodySmall)

    if(s.calculator=="inheritance")Text("برای تقسیم‌های پیچیده، طبقات دیگر و تعدد زوجات از گفتگو راهنمایی بگیرید.",style=MaterialTheme.typography.bodySmall)
    if(s.calculator=="inheritance_tax")Text("برای ملک، ارزش مالیاتی ملاک است؛ دارایی ناموجود را صفر بگذارید.",style=MaterialTheme.typography.bodySmall)
    Action("محاسبه و نمایش نتیجه",s.posting){vm.compute(values.toMap())}
   }
   ErrorNotice(s)
   s.result?.let { result->GlassCard {
    Text("نتیجه محاسبه",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    CalculatorResult(s.calculator,result)
    Text("نتیجه وابسته به ورودی‌ها و منطق سرور است؛ قبل از اقدام رسمی بررسی شود.",style=MaterialTheme.typography.bodySmall)
   } }
  }}
  if(wide)Row(horizontalArrangement=Arrangement.spacedBy(24.dp)){Column(Modifier.width(260.dp)){selection()};Column(Modifier.weight(1f)){form()}}
  else Column(verticalArrangement=Arrangement.spacedBy(20.dp)){selection();form()}
 }
} }
@Composable fun DeadlineScreen(s:AppUiState,vm:AppViewModel) {Page(hero={SiteHero("مهلت‌های شما","تاریخ پایان را با مرجع قضایی تطبیق دهید؛ یادآور جایگزین پیگیری شخصی نیست.","مهلت و یادآور",NativeIcons.CalendarMonth)}) {
 Button(onClick={vm.openDialog("deadline","ثبت مهلت جدید")}){Icon(NativeIcons.Add,null);Text("ثبت مهلت")}
 Text("یادآور اختیاری: یک روز قبل، ساعت ۹ صبح گوشی",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 Loading(s);ErrorNotice(s,{vm.refresh()})
 if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())EmptyState("مهلتی ثبت نشده","عنوان و تاریخ دقیق پایان مهلت را وارد کنید.")
 AdaptiveCards(s.items) { d->
  val title=d.str("title",d.str("case_title","مهلت قانونی"));val date=d.str("due_date",d.str("deadline_date"))
  Text(title,fontWeight=FontWeight.Medium);Text(dateFa(date))
  Text("میلادی: \u2066"+date.take(10)+"\u2069",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  Text(if(d.str("status")=="done")"انجام‌شده"else "باز",color=MaterialTheme.colorScheme.primary)
  if(d.str("notes").isNotBlank())Text(d.str("notes"),style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   if(d.str("status")!="done")OutlinedButton(onClick={vm.completeDeadline(d.optLong("id"))},enabled=!s.posting){Text("انجام شد")}
   Spacer(Modifier.weight(1f))
   OverflowActions("گزینه‌های مهلت "+title,!s.posting){close->
    DropdownMenuItem(text={Text("فایل تقویم")},onClick={close();vm.calendar(title,date)})
    DropdownMenuItem(text={Text("یادآور یک روز قبل")},onClick={close();vm.reminder(d.optLong("id"),title,date)})
    DropdownMenuItem(text={Text("حذف مهلت",color=MaterialTheme.colorScheme.error)},onClick={close();vm.openDialog("delete","حذف مهلت",JSONObject().put("kind","deadlines").put("id",d.optLong("id")))})
   }
  }
 };Pager(s,vm)
} }
@Composable fun PlansScreen(s:AppUiState,vm:AppViewModel) {Page(hero={SiteHero("طرح مناسب خود را انتخاب کنید","ماهانه یا سالانه؛ مبلغ نهایی را پیش از پرداخت بررسی کنید.","طرح‌های ماهانه و سالانه • قیمت مستقیم از سرور",NativeIcons.WorkspacePremium,footer={SiteTabs(listOf("monthly" to "طرح‌های ماهانه","yearly" to "طرح‌های سالانه"),s.planCycle,vm::cycle)})}) {
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){if(s.authenticated)TextButton(onClick=vm::paymentHistory){Text("تاریخچه پرداخت")}}
 if(vm.pendingPayment)OutlinedButton(onClick={vm.verifyPayment()}){Text("استعلام آخرین پرداخت")}
 val plans=s.items.filter{it.str("billing_cycle")==s.planCycle};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.error.isBlank()&&plans.isEmpty())EmptyState("طرحی در این دوره موجود نیست","دوره دیگری را انتخاب کنید یا بعداً دوباره بررسی کنید.");AdaptiveCards(plans,featured={it.optBoolean("is_popular")||it.optInt("is_popular")==1}) { p->if(p.str("badge_text").isNotBlank())Text(p.str("badge_text"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(p.str("name"),style=MaterialTheme.typography.titleMedium);Text(p.str("description"),style=MaterialTheme.typography.bodySmall);Text("${fa(p.optLong("price_toman"))} تومان",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${fa(p.optInt("duration_days"))} روز اعتبار",style=MaterialTheme.typography.bodySmall);val features=p.optJSONArray("features")?:runCatching{JSONArray(p.str("features_json","[]"))}.getOrDefault(JSONArray());(0 until features.length()).forEach { i->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(NativeIcons.Check,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Text(features.optString(i),Modifier.weight(1f))}};Action("انتخاب این اشتراک",s.posting){vm.selectPlan(p)}}
} }
@Composable fun TicketsScreen(s:AppUiState,vm:AppViewModel) {Page{
 PageHeader("کنارتان هستیم","مشکلات حساب یا خدمات را برای تیم پشتیبانی بفرستید.")
 Button(onClick={vm.openDialog("ticket","درخواست پشتیبانی جدید")}){Icon(NativeIcons.Add,null);Text("درخواست جدید")};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.error.isBlank()&&s.items.isEmpty())EmptyState("درخواست پشتیبانی ندارید","رمز عبور و اطلاعات بانکی را در تیکت ننویسید.")
 AdaptiveCards(s.items) { t->
  Text(t.str("subject"),fontWeight=FontWeight.Medium)
  Text(mapOf("open" to "باز","answered" to "پاسخ داده‌شده","closed" to "بسته‌شده")[t.str("status")]?:t.str("status"),color=MaterialTheme.colorScheme.primary)
  val messages=t.objects("replies").plus(t.objects("messages"));var expanded by remember(t){mutableStateOf(false)}
  if(t.str("message").isNotBlank()&&messages.none{it.str("message",it.str("content",it.str("body",it.str("text"))))==t.str("message")})Text(t.str("message"))
  (if(expanded)messages else messages.takeLast(1)).forEach { r->
   Text(if(r.str("sender") in listOf("admin","support"))"پشتیبانی"else "پیام",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text(r.str("message",r.str("content",r.str("body",r.str("text")))),style=MaterialTheme.typography.bodySmall)
  }
  if(messages.size>1)TextButton(onClick={expanded=!expanded}){Text(if(expanded)"بستن گفتگو"else "نمایش گفتگو (${fa(messages.size)})")}
  if(t.str("status")!="closed")OutlinedButton(onClick={vm.openDialog("reply","ارسال توضیح بیشتر",t)}){Text("ارسال پاسخ")}
 };Pager(s,vm)
} }

@Composable fun AppDialogs(s:AppUiState,vm:AppViewModel) {
 val d=s.dialog?:return
 androidx.compose.ui.window.Dialog(onDismissRequest=vm::closeDialog,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!s.posting,dismissOnClickOutside=!s.posting)) {
 Surface(Modifier.widthIn(max=700.dp).fillMaxWidth().padding(16.dp).imePadding().heightIn(max=720.dp),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
 key(d.kind,d.data.optLong("id"),d.title){Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(d.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);IconButton(onClick=vm::closeDialog,enabled=!s.posting){Icon(NativeIcons.Close,"بستن پنجره")}}
  when(d.kind){
   "historyPreview"->{HistoryDetail(d,s,vm,Modifier.fillMaxWidth().height(440.dp))}
   "delete"->{Text("این مورد حذف می‌شود. این عمل قابل بازگشت نیست.");Action("تأیید حذف",s.posting){vm.delete(d.data.str("kind"),d.data.optLong("id"))}}
   "retryChat"->{
    var checked by remember{mutableStateOf(false)}
    Text("در قطع ارتباط ممکن است پیام در سرور ثبت شده و سهمیه مصرف شده باشد. ارسال مجدد می‌تواند یک درخواست تکراری بسازد.")
    OutlinedButton(onClick={vm.closeDialog();vm.navigate(Screen.HISTORY)}){Text("بررسی تاریخچه گفتگوها")}
    Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=checked,onCheckedChange={checked=it});Text("تاریخچه را بررسی کرده‌ام یا با آگاهی از احتمال تکرار ادامه می‌دهم.",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)}
    Action("ارسال مجدد با تأیید من",s.posting||!checked){vm.confirmRetryAfterReview()}
   }
   "logout"->{Text("اطلاعات روی سرور باقی می‌مانند؛ نشست و پیش‌نویس این دستگاه پاک می‌شود.");Action("خروج از حساب",s.posting,onClick=vm::logout)}
   "external"->{Text("این بخش در مرورگر امن گوشی باز می‌شود؛ رابط اصلی اپ بومی است.");Action("باز کردن در مرورگر",s.posting){vm.openExternal(d.data.str("url"));vm.closeDialog()}}
   "otp"->{var code by remember{mutableStateOf("")};Text("کد ارسال‌شده به ${d.data.str("phone")} را وارد کنید.");Field("کد یک‌بارمصرف",code,{code=it},numeric=true,error=s.fieldErrors["otp"],enabled=!s.posting,onEdited={vm.inputEdited("otp")},firstError=s.firstField("otp"),attempt=s.validationAttempt);Action("تأیید کد",s.posting){vm.verifyOtp(d.data.str("phone"),code)}}
   "password"->{var old by remember{mutableStateOf("")};var new by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};Field("رمز فعلی",old,{old=it},password=true,error=s.fieldErrors["old_password"],enabled=!s.posting,onEdited={vm.inputEdited("old_password")},firstError=s.firstField("old_password"),attempt=s.validationAttempt);Field("رمز جدید (حداقل ۶ نویسه)",new,{new=it},password=true,error=s.fieldErrors["new_password"],enabled=!s.posting,onEdited={vm.inputEdited("new_password")},firstError=s.firstField("new_password"),attempt=s.validationAttempt);Field("تکرار رمز جدید",confirm,{confirm=it},password=true,error=s.fieldErrors["confirm_password"],enabled=!s.posting,onEdited={vm.inputEdited("confirm_password")},firstError=s.firstField("confirm_password"),attempt=s.validationAttempt);Action("تغییر رمز عبور",s.posting){vm.changePassword(old,new,confirm)}}
   "documentCreate"->{var title by rememberSaveable{mutableStateOf("")};var detail by rememberSaveable{mutableStateOf("")};var type by rememberSaveable{mutableStateOf("دادخواست")};Choice("نوع برگه",type,listOf("دادخواست","اظهارنامه","شکواییه","لایحه دفاعیه","قرارداد").map{it to it},enabled=!s.posting){type=it};Field("عنوان برگه",title,{title=it},error=s.fieldErrors["title"],enabled=!s.posting,onEdited={vm.inputEdited("title")},firstError=s.firstField("title"),attempt=s.validationAttempt);Field("شرح ماجرا و خواسته",detail,{detail=it},multiline=true,error=s.fieldErrors["details"],enabled=!s.posting,onEdited={vm.inputEdited("details")},firstError=s.firstField("details"),attempt=s.validationAttempt);Text("اسامی، تاریخ‌ها و مبلغ را دقیق بنویسید و متن نهایی را قبل از ثبت بررسی کنید.",style=MaterialTheme.typography.bodySmall);Action("تنظیم هوشمند برگه",s.posting){vm.createDocument(type,title,detail)}}
   "documentView"->{DocumentDetailContent(d,s,vm)}
   "article"->{ArticleDetailContent(d,vm)}
   "documentEdit"->{DocumentDetailContent(d,s,vm)}
   "deadline"->{var title by rememberSaveable{mutableStateOf("")};var date by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var picker by remember{mutableStateOf(false)};Field("عنوان مهلت",title,{title=it},error=s.fieldErrors["title"],enabled=!s.posting,onEdited={vm.inputEdited("title")},firstError=s.firstField("title"),attempt=s.validationAttempt);OutlinedButton(onClick={picker=true},enabled=!s.posting,modifier=Modifier.fieldFeedback(s.fieldErrors["date"],s.firstField("date"),s.validationAttempt)){Icon(NativeIcons.CalendarMonth,null);Text(if(date.isBlank())"انتخاب تاریخ پایان مهلت"else dateFa(date)+" / "+date)};if(s.fieldErrors["date"]!=null)Text(s.fieldErrors.getValue("date"),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);if(picker){val dateState=rememberDatePickerState();DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{date=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString();vm.inputEdited("date")};picker=false}){Text("انتخاب")}},dismissButton={TextButton(onClick={picker=false}){Text("انصراف")}}){DatePicker(state=dateState,title={Text("تاریخ پایان (تقویم میلادی)",Modifier.padding(20.dp))})}};Field("یادداشت (اختیاری)",notes,{notes=it},multiline=true);Text("این اپ مهلت قانونی را از روی نوع دعوا حدس نمی‌زند؛ تاریخ تأییدشده را وارد کنید.",style=MaterialTheme.typography.bodySmall);Action("ثبت مهلت",s.posting){vm.addDeadline(title,date,notes)}}
   "ticket"->{var subject by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};Field("موضوع",subject,{subject=it},error=s.fieldErrors["subject"],enabled=!s.posting,onEdited={vm.inputEdited("subject")},firstError=s.firstField("subject"),attempt=s.validationAttempt);Field("شرح درخواست",message,{message=it},multiline=true,error=s.fieldErrors["message"],enabled=!s.posting,onEdited={vm.inputEdited("message")},firstError=s.firstField("message"),attempt=s.validationAttempt);Text("رمز، کد ورود و اطلاعات بانکی را ننویسید.",style=MaterialTheme.typography.bodySmall);Action("ارسال درخواست",s.posting){vm.createTicket(subject,message)}}
   "reply"->{var message by rememberSaveable{mutableStateOf("")};Field("پاسخ شما",message,{message=it},multiline=true,error=s.fieldErrors["message"],enabled=!s.posting,onEdited={vm.inputEdited("message")},firstError=s.firstField("message"),attempt=s.validationAttempt);Action("ارسال پاسخ",s.posting){vm.replyTicket(d.data.optLong("id"),message)}}
   "checkout"->{var code by rememberSaveable{mutableStateOf("")};Text(d.data.str("name"),fontWeight=FontWeight.Bold);Text("مبلغ اولیه: ${fa(d.data.optLong("price_toman"))} تومان");Field("کد تخفیف (اختیاری)",code,{code=it},error=s.fieldErrors["code"],enabled=!s.posting,onEdited={vm.inputEdited("code")},firstError=s.firstField("code"),attempt=s.validationAttempt);OutlinedButton(onClick={vm.validateDiscount(d.data.optLong("id"),code)},enabled=!s.posting){Text("بررسی کد تخفیف")};Text("با تأیید، سفارش ایجاد می‌شود و درگاه در مرورگر امن باز خواهد شد. بازگشت به اپ به معنی پرداخت موفق نیست؛ وضعیت از سرور استعلام می‌شود.",style=MaterialTheme.typography.bodySmall);Action("تأیید و ادامه به درگاه",s.posting){vm.checkout(d.data.optLong("id"),code)}}
   "payments"->{val payments=d.data.objects("items");if(payments.isEmpty())Text("پرداختی ثبت نشده است.");payments.forEach { p->DataResult(p);if(ApiContract.paymentAuthority(p).isNotBlank())TextButton(onClick={vm.verifyPayment(ApiContract.paymentAuthority(p))}){Text("استعلام این پرداخت")}}}
   "result"->{DataResult(d.data);Text("وضعیت مستقیماً از سرویس دریافت شده است.",style=MaterialTheme.typography.bodySmall);TextButton(onClick={vm.verifyPayment()}){Text("استعلام دوباره")}}
  }
  ErrorNotice(s)
 }}
 }
 }
}

@Composable fun NativeMarkdown(text:String){
 val blocks=remember(text){MarkdownBlocks.parse(text)}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  blocks.forEach { block->when(block) {
   is MarkdownBlock.Line->MarkdownLine(block.text)
   is MarkdownBlock.Code->Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(10.dp)) {
    Box(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
     Text(block.text,Modifier.widthIn(max=4096.dp),style=MaterialTheme.typography.bodySmall.copy(fontFamily=androidx.compose.ui.text.font.FontFamily.Monospace,textDirection=androidx.compose.ui.text.style.TextDirection.ContentOrLtr))
    }
   }
   is MarkdownBlock.Table->{
    val columns=maxOf(block.header.size,block.rows.maxOfOrNull{it.size}?:0)
    Column(Modifier.horizontalScroll(rememberScrollState())) {
     (listOf(block.header)+block.rows).forEachIndexed { row,values->
      Row(Modifier.height(IntrinsicSize.Min)) {
       repeat(columns){index->Surface(Modifier.width(160.dp).fillMaxHeight(),color=if(row==0)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
        Text(values.getOrNull(index).orEmpty(),Modifier.padding(10.dp),style=MaterialTheme.typography.bodySmall.copy(textDirection=androidx.compose.ui.text.style.TextDirection.ContentOrLtr),fontWeight=if(row==0)FontWeight.Bold else FontWeight.Normal)
       }}
      }
     }
    }
   }
  }}
 }
}
@Composable private fun MarkdownLine(line:String) {
 val heading=line.startsWith('#');val raw=if(heading)line.trimStart('#',' ')else if(line.trimStart().startsWith("- ")||line.trimStart().startsWith("* "))"• "+line.trimStart().drop(2)else if(line.startsWith("> "))line.drop(2)else line
 if(raw.isNotBlank()){
  val annotated=androidx.compose.ui.text.buildAnnotatedString{var end=0;Regex("\\*\\*(.+?)\\*\\*").findAll(raw).forEach { m->append(raw.substring(end,m.range.first));pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight=FontWeight.Bold));append(m.groupValues[1]);pop();end=m.range.last+1 };append(raw.substring(end))}
  Text(annotated,style=if(heading)MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,fontWeight=if(heading)FontWeight.Bold else FontWeight.Normal)
 }
}

@Composable fun QuickQuestionForm(questions:List<JSONObject>,busy:Boolean,onSubmit:(String)->Unit) {
 val identity=questions.joinToString{it.toString()}
 var selected by remember(identity){mutableStateOf<Map<Int,String>>(emptyMap())}
 var note by rememberSaveable(identity){mutableStateOf("")}
 var showNote by remember(identity){mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("برای دقیق‌تر شدن پاسخ",fontWeight=FontWeight.Bold)
  questions.forEachIndexed { index,q->
   Text(q.str("question",q.str("label")),style=MaterialTheme.typography.labelLarge)
   val options=q.optJSONArray("options")?:JSONArray()
   FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {(0 until options.length()).forEach { i->val option=options.optString(i)
    FilterChip(selected=selected[index]==option,enabled=!busy,onClick={selected=selected+(index to option)},label={Text(option)})
   }}
  }
  if(showNote||note.isNotBlank())Field("توضیح تکمیلی (اختیاری)",note,{note=it},multiline=true)else TextButton(onClick={showNote=true}){Text("افزودن توضیح اختیاری")}
  Button(enabled=!busy&&(selected.isNotEmpty()||note.isNotBlank()),onClick={
   val parts=questions.mapIndexedNotNull { index,q->selected[index]?.let{q.str("question",q.str("label"))+" "+it} }.toMutableList()
   if(note.isNotBlank())parts.add(note.trim());onSubmit(parts.joinToString(" | "))
  }){Text("ارسال پاسخ پرسش‌ها")}
 }
}
