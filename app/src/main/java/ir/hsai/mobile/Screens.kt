@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
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
fun dateFa(raw:String):String { if(raw.isBlank())return "تاریخ ثبت نشده";if((raw.take(4).toIntOrNull()?:2000)<1700)return raw;return runCatching { val d=LocalDate.parse(raw.take(10));android.icu.text.DateFormat.getDateInstance(android.icu.text.DateFormat.MEDIUM,android.icu.util.ULocale("fa_IR@calendar=persian")).format(java.util.Date.from(d.atTime(12,0).toInstant(ZoneOffset.UTC))) }.getOrDefault(raw.take(10)) }

@Composable fun Page(content:@Composable ColumnScope.()->Unit) {
 Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
  Column(Modifier.widthIn(max=1080.dp).fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).imePadding().padding(horizontal=20.dp,vertical=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp),content=content)
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
@Composable fun Field(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,password:Boolean=false,numeric:Boolean=false,multiline:Boolean=false) {
 var show by remember{mutableStateOf(false)}
 OutlinedTextField(value=value,onValueChange=onValue,label={Text(label)},modifier=modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),singleLine=!multiline,minLines=if(multiline)4 else 1,maxLines=if(multiline)12 else 1,
 keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else if(numeric)KeyboardType.Decimal else KeyboardType.Text),visualTransformation=if(password&&!show)PasswordVisualTransformation()else VisualTransformation.None,
 colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=MaterialTheme.colorScheme.surfaceVariant.copy(alpha=.45f),focusedContainerColor=MaterialTheme.colorScheme.surface,unfocusedBorderColor=MaterialTheme.colorScheme.outlineVariant),
 trailingIcon=if(password){{IconButton(onClick={show=!show}){Icon(if(show)NativeIcons.VisibilityOff else NativeIcons.Visibility,"نمایش یا پنهان کردن رمز")}}}else null)
}
@Composable fun Choice(label:String,value:String,options:List<Pair<String,String>>,onChoose:(String)->Unit) {var expanded by remember{mutableStateOf(false)};Column {Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);Box{OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(13.dp)){Text(options.find{it.first==value}?.second?:value,Modifier.weight(1f),textAlign=TextAlign.Start);Icon(NativeIcons.ExpandMore,null)};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){options.forEach{(k,l)->DropdownMenuItem(text={Text(l)},onClick={onChoose(k);expanded=false})}}}} }
@Composable fun Action(text:String,busy:Boolean,onClick:()->Unit) { Button(onClick=onClick,enabled=!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(14.dp)){if(busy){CircularProgressIndicator(Modifier.size(18.dp),color=MaterialTheme.colorScheme.onPrimary,strokeWidth=2.dp);Spacer(Modifier.width(8.dp))};Text(text)} }
@Composable fun DataResult(data:JSONObject,depth:Int=0) { if(depth>6)return;Column(verticalArrangement=Arrangement.spacedBy(11.dp)){data.keys().asSequence().filter{it !in listOf("id","user_id","type","analysis_json","features_json")}.forEach { k->val v=data.opt(k);val label=Domain.labels[k]?:k.replace('_',' ');HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(alpha=.07f));Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary);when(v){is JSONObject->DataResult(v,depth+1);is JSONArray->(0 until v.length()).forEach { i->val item=v.opt(i);if(item is JSONObject)DataResult(item,depth+1)else Text(item.toString()) };else->SelectionContainer{Text(when{v==null||v==JSONObject.NULL->"—";v is Boolean->if(v)"بله" else "خیر";v is Number->fa(v)+(if(k.contains("toman"))" تومان"else "");k=="status"->mapOf("pending" to "در انتظار پرداخت","paid" to "پرداخت‌شده","verified" to "تأییدشده","success" to "موفق","failed" to "ناموفق","open" to "باز","closed" to "بسته‌شده")[v.toString()]?:v.toString();else->v.toString()})}}} } }

@Composable fun ChatScreen(s:AppUiState,vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEditImage:()->Unit) {
 val list=rememberLazyListState()
 LaunchedEffect(s.messages.size,s.messages.lastOrNull()?.text,s.posting){if(s.messages.isNotEmpty())list.animateScrollToItem((list.layoutInfo.totalItemsCount-1).coerceAtLeast(0))}
 Column(Modifier.fillMaxSize().imePadding(),horizontalAlignment=Alignment.CenterHorizontally) {
 LazyColumn(state=list,modifier=Modifier.weight(1f).widthIn(max=1000.dp).fillMaxWidth(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  if(s.messages.isEmpty())item {HomeContent(s,vm)}
  itemsIndexed(s.messages){_,m->MessageCard(m,s.posting,vm)}
  if(s.posting)item{Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp);Text(s.stage.ifBlank{"در حال بررسی و آماده‌سازی پاسخ…"},style=MaterialTheme.typography.bodySmall)}}
  if(s.failedChat!=null)item {ErrorNotice(s);OutlinedButton(onClick={vm.sendChat(true)},enabled=!s.posting){Text("ارسال دوباره همان پیام")};Text("در خطای ارتباط، قبل از ارسال مجدد تاریخچه را بررسی کنید.",style=MaterialTheme.typography.bodySmall)}
 }
 Surface(modifier=Modifier.widthIn(max=1000.dp).fillMaxWidth(),color=MaterialTheme.colorScheme.background) { Column(Modifier.padding(horizontal=13.dp,vertical=9.dp)) {
  if(s.image!=null)ImageAttachment(s.image,onEditImage,{vm.setImage(null)})
  Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface,RoundedCornerShape(24.dp)).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   var attachMenu by remember{mutableStateOf(false)}
   Box{IconButton(onClick={attachMenu=true}){Icon(NativeIcons.AttachFile,"افزودن تصویر")};DropdownMenu(expanded=attachMenu,onDismissRequest={attachMenu=false}){DropdownMenuItem(text={Text("انتخاب تصویر")},onClick={attachMenu=false;onImage()});DropdownMenuItem(text={Text("دوربین")},onClick={attachMenu=false;onCamera()})}}
   OutlinedTextField(value=s.draft,onValueChange=vm::setDraft,placeholder={Text("ماجرا را ساده بنویسید…",style=MaterialTheme.typography.bodySmall)},modifier=Modifier.weight(1f),maxLines=5,shape=RoundedCornerShape(18.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Transparent,focusedBorderColor=Color.Transparent))
   FilledIconButton(onClick={vm.sendChat()},enabled=!s.posting&&(s.draft.isNotBlank()||s.image!=null),modifier=Modifier.size(46.dp)){Icon(NativeIcons.Send,"ارسال پیام")}
  }
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("پاسخ مستند؛ جایگزین وکالت در جلسه دادگاه نیست.",modifier=Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Row(verticalAlignment=Alignment.CenterVertically){Text("جریانی",style=MaterialTheme.typography.labelSmall);Switch(checked=s.streaming,onCheckedChange=vm::setStream,enabled=!s.posting,modifier=Modifier.height(38.dp))}}
 } }
 }
}
@Composable fun HomeContent(s:AppUiState,vm:AppViewModel) {
 Column(verticalArrangement=Arrangement.spacedBy(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
  Surface(shape=RoundedCornerShape(18.dp),color=SiteNavy){Icon(NativeIcons.Balance,null,Modifier.padding(15.dp).size(28.dp),tint=SiteGold)}
  Text("نیازی نیست اصطلاحات حقوقی بلد باشید؛ راحت بگویید چه اتفاقی افتاده؟",style=MaterialTheme.typography.titleLarge.copy(lineHeight=30.sp),fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
  Text("ماجرا را دقیقاً مثل تعریف کردن برای یک دوست بنویسید تا قوانین ایران، هزینه دقیق و بهترین راه عملی فردا صبح را به زبان ساده برایتان روشن کنیم.",color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center,style=MaterialTheme.typography.bodyMedium)
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
  val topics=listOf("پول، چک و بدهی" to "طلبم را نمی‌دهند یا چکم برگشت خورده","خانه، مستأجر و صاحب‌خانه" to "مشکل پول پیش، تخلیه خانه یا قولنامه ملک","خانواده و ازدواج" to "مهریه، نفقه، طلاق یا حضانت فرزند","محل کار و بیمه" to "کارفرما حقوق، بیمه یا سنواتم را نداده","اینترنت، کلاهبرداری و جرم" to "کلاه سرم رفته، فیشینگ شده یا مزاحمم شده‌اند","ارث و میراث خانوادگی" to "انحصار وراثت و تقسیم ارث بعد از فوت")
  AdaptiveCards(topics,minWidth=260.dp) { (title,description) ->
   TextButton(onClick={vm.chooseTopic(description)},contentPadding=PaddingValues(0.dp),modifier=Modifier.fillMaxWidth()) {
    Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(title,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
     Text(description,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}
@Composable fun MessageCard(m:ChatMessage,busy:Boolean,vm:AppViewModel) {
 GlassCard(Modifier.padding(start=if(m.role=="user")25.dp else 0.dp)) {
  Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){Icon(if(m.role=="user")NativeIcons.PersonOutline else NativeIcons.Balance,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Text(if(m.role=="user")"شما"else "حق‌سنج • دستیار حقوقی",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}
  if(m.image!=null)NativeImage(m.image)
  SelectionContainer {NativeMarkdown(m.text.ifBlank{"در حال دریافت پاسخ…"})}
  if(m.citations.isNotEmpty()){var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Icon(NativeIcons.MenuBook,null,Modifier.size(17.dp));Text("${fa(m.citations.size)} استناد قانونی")};if(expanded)m.citations.forEach { c->Text(c.str("law_name")+" — ماده "+c.str("article_number"),fontWeight=FontWeight.Medium);Text(c.str("official_text"),style=MaterialTheme.typography.bodySmall) }}
  if(m.document!=null)OutlinedButton(onClick={vm.navigate(Screen.DOCUMENTS)}){Text("مشاهده برگه قضایی آماده")}
  if(m.role=="assistant"&&m.text.isNotBlank())FlowRow(horizontalArrangement=Arrangement.spacedBy(3.dp)){TextButton(onClick={vm.copy(m.text)}){Icon(NativeIcons.ContentCopy,"کپی",Modifier.size(16.dp));Text("کپی")};if(m.id!=null){TextButton(onClick={vm.feedback(m.id,"like")},enabled=!busy){Icon(if(m.feedback=="like")NativeIcons.Check else NativeIcons.ThumbUp,"مفید بود",Modifier.size(16.dp))};TextButton(onClick={vm.feedback(m.id,"dislike")},enabled=!busy){Icon(NativeIcons.ThumbDown,"مفید نبود",Modifier.size(16.dp))};TextButton(onClick={vm.simplify(m.id)},enabled=!busy){Text("ساده‌تر بگو")}}}
 }
}
@Composable fun NativeImage(data:String,modifier:Modifier=Modifier) {val bitmap=remember(data){runCatching{val bytes=Base64.decode(data.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(bytes,0,bytes.size)}.getOrNull()};if(bitmap!=null)Image(bitmap.asImageBitmap(),"تصویر انتخاب‌شده",modifier.fillMaxWidth().heightIn(max=180.dp))}
@Composable fun ImageAttachment(data:String,onEdit:()->Unit,onRemove:()->Unit) {Row(verticalAlignment=Alignment.CenterVertically){NativeImage(data,Modifier.width(58.dp).height(52.dp));TextButton(onClick=onEdit){Text("پوشاندن مشخصات")};IconButton(onClick=onRemove){Icon(NativeIcons.Close,"حذف تصویر")}}}
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
  var name by remember(s.user){mutableStateOf(s.user.str("full_name"))}
  var email by remember(s.user){mutableStateOf(s.user.str("email"))}
  var national by remember(s.user){mutableStateOf(s.user.str("national_id"))}
  AdaptiveSplit(first={GlassCard {
   Text("مشخصات هویتی (جهت درج در دادخواست‌ها)",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Field("نام و نام خانوادگی رسمی",name,{name=it})
   Field("کد ملی ۱۰ رقمی (اختیاری)",national,{national=it},numeric=true)
   Field("نشانی ایمیل (اختیاری)",email,{email=it})
   Action("ذخیره مشخصات هویتی",s.posting){vm.saveProfile(name,email,national)}
  }},second={GlassCard {
   Text("اشتراک شما",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(s.subscription.str("plan_name","اطلاعات اشتراک موجود نیست."))
   if(s.subscription.has("days_remaining"))Text("${fa(s.subscription.optInt("days_remaining"))} روز اعتبار باقی‌مانده")
   OutlinedButton(onClick={vm.navigate(Screen.PLANS)}){Text("اشتراک و پرداخت‌ها")}
   Text("اطلاعات حساب فقط از API رسمی دریافت می‌شود.",style=MaterialTheme.typography.bodySmall)
  }})
 } else GlassCard {
  Text("امنیت و رمز عبور",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
  Text("نشست با Android Keystore رمزگذاری می‌شود؛ رمز عبور ذخیره نمی‌شود.")
  OutlinedButton(onClick={vm.openDialog("password","تغییر رمز عبور")}){Text("تغییر رمز عبور")}
  if(s.user.str("role")=="admin")TextButton(onClick={vm.openDialog("external","پنل رسمی مدیریت",JSONObject().put("url","https://hsai.ir/app/admin"))}){Text("پنل رسمی مدیریت در مرورگر")}
 }
} }
@Composable fun HistoryScreen(s:AppUiState,vm:AppViewModel) {Page{PageHeader("گفتگوهای من","پرونده‌های قبلی خود را ادامه دهید.");OutlinedButton(onClick=vm::newChat){Icon(NativeIcons.Add,null);Text("گفتگوی جدید")};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.items.isEmpty())EmptyState("هنوز گفتگویی ندارید","اولین سؤال خود را در بخش گفتگو مطرح کنید.");AdaptiveCards(s.items) { c->Text(c.str("title","گفتگوی حقوقی"),fontWeight=FontWeight.Medium);Text(dateFa(c.str("updated_at",c.str("created_at"))),style=MaterialTheme.typography.bodySmall);FlowRow{Button(onClick={vm.openConversation(c.optLong("id"))},enabled=!s.posting){Text("ادامه گفتگو")};TextButton(onClick={vm.openDialog("delete","حذف گفتگو",JSONObject().put("kind","consultations").put("id",c.optLong("id")))}){Text("حذف")}}};Pager(s,vm)} }
@Composable fun DocumentScreen(s:AppUiState,vm:AppViewModel) {Page{PageHeader("اوراق قضایی شما","پیش‌نویس رسمی را قبل از چاپ و ثبت، بررسی و اصلاح کنید.");Button(onClick={vm.openDialog("documentCreate","تنظیم برگه قضایی")}){Icon(NativeIcons.Add,null);Text("تنظیم برگه جدید")};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.items.isEmpty())EmptyState("برگه‌ای ثبت نشده","شرح ماجرا را به پیش‌نویس دادخواست، شکواییه یا قرارداد تبدیل کنید.");AdaptiveCards(s.items) { d->Text(d.str("title",d.str("doc_type","سند قضایی")),fontWeight=FontWeight.Medium);Text(d.str("doc_type")+" · "+dateFa(d.str("created_at")),style=MaterialTheme.typography.bodySmall);FlowRow{TextButton(onClick={vm.viewDocument(d.optLong("id"))},enabled=!s.posting){Text("مشاهده")};TextButton(onClick={vm.viewDocument(d.optLong("id"),true)},enabled=!s.posting){Text("ویرایش")};TextButton(onClick={vm.download(d.optLong("id"))}){Text("Word")};TextButton(onClick={vm.openDialog("delete","حذف سند",JSONObject().put("kind","documents").put("id",d.optLong("id")))}){Text("حذف")}}};Pager(s,vm)} }
@Composable fun ScannerScreen(s:AppUiState,vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEdit:()->Unit) {Page{
 PageHeader("از متن پیچیده، به معنی روشن","ابلاغیه یا قرارداد را به زبان ساده بخوانید و مهلت‌ها را بررسی کنید.")
 var kind by rememberSaveable{mutableStateOf("notice")};var text by rememberSaveable{mutableStateOf("")};var consent by rememberSaveable{mutableStateOf(false)}
 GlassCard{Choice("نوع برگه",kind,listOf("notice" to "ابلاغیه دادگاه","contract" to "قرارداد","other" to "سایر برگه‌ها")){kind=it};FlowRow{OutlinedButton(onClick=onImage){Icon(NativeIcons.AddPhotoAlternate,null);Text("گالری")};Spacer(Modifier.width(10.dp));OutlinedButton(onClick=onCamera){Icon(NativeIcons.PhotoCamera,null);Text("دوربین")}};if(s.image!=null){NativeImage(s.image);ImageAttachment(s.image,onEdit,{vm.setImage(null)})};Field("یا متن برگه را وارد کنید",text,{text=it},multiline=true);Row(verticalAlignment=Alignment.CenterVertically){Checkbox(checked=consent,onCheckedChange={consent=it});Text("مشخصات حساس را بررسی کرده‌ام و با ارسال برگه برای تحلیل موافقم.",style=MaterialTheme.typography.bodySmall)};Action("تحلیل برگه",s.posting){vm.analyze(kind,text,s.image,consent)}}
 ErrorNotice(s);s.result?.let{GlassCard{Text("نتیجه تحلیل",style=MaterialTheme.typography.titleMedium);Text("این تحلیل و هر مهلت پیشنهادی، خروجی هوش مصنوعی است و ممکن است نامرتبط یا اشتباه باشد. نوع برگه، تاریخ ابلاغ و مهلت قانونی را با اصل سند و مرجع قضایی مستقلاً تأیید کنید؛ اپ یادآور خودکار ثبت نمی‌کند.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall);DataResult(it)}}
 Text("تحلیل‌های قبلی",style=MaterialTheme.typography.titleMedium);Loading(s);AdaptiveCards(s.items) { a->Text(a.str("title","تحلیل برگه"),fontWeight=FontWeight.Medium);var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Text(if(expanded)"بستن تحلیل"else "نمایش تحلیل")};if(expanded)DataResult(a.optJSONObject("analysis")?:JSONObject().put("text",a.str("raw_text")))};Pager(s,vm)
} }
@Composable fun LawScreen(s:AppUiState,vm:AppViewModel) {Page{
 PageHeader("جستجوی ساده در قوانین ایران و واژه‌نامه دادگاه","متن رسمی قانون را به همراه معنی ساده آن ببینید یا تفاوت قوانین قدیم و جدید را مقایسه کنید.")
 FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("search" to "جستجو","comparisons" to "قدیم و جدید","collections" to "مجموعه‌ها").forEach{(k,l)->FilterChip(selected=s.lawTab==k,onClick={vm.lawTab(k)},label={Text(l)})}}
 if(s.lawTab=="search"){var q by rememberSaveable{mutableStateOf(s.lawQuery)};GlassCard{Field("کلمه یا موضوع حقوقی",q,{q=it});Action("جستجو در قوانین",s.loading){vm.searchLaws(q)};FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("چک صیادی","مهریه","دادگاه صلح").forEach{t->SuggestionChip(onClick={q=t;vm.searchLaws(t)},label={Text(t)})}}}}
 Loading(s);ErrorNotice(s,{vm.refresh(s.page)})
 if(!s.loading&&s.items.isEmpty())EmptyState(if(s.lawQuery.isBlank()&&s.lawTab=="search")"دنبال چه قانونی می‌گردید؟"else "نتیجه‌ای یافت نشد","موضوع را کوتاه و دقیق وارد کنید یا دسته دیگری را ببینید.")
 AdaptiveCards(s.items) { a->if(s.lawTab=="search"){Text(a.str("law_name")+" — ماده "+a.str("article_number"),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary);SelectionContainer{Text(a.str("official_text"))};if(a.str("plain_explanation").isNotBlank()){var expanded by remember{mutableStateOf(false)};TextButton(onClick={expanded=!expanded}){Text("شرح ساده")};if(expanded)Text(a.str("plain_explanation"))}}else{Text(a.str("title",a.str("name",a.str("law_name","اطلاعات قانونی"))),fontWeight=FontWeight.Medium);DataResult(a)} };Pager(s,vm)
} }
@Composable fun CalculatorScreen(s:AppUiState,vm:AppViewModel) {Page {
 PageHeader("محاسبه آنلاین مهریه، هزینه دادگاه، ارث و سنوات","رایگان برای همه مردم • همه مبالغ را به تومان وارد کنید، نه ریال. تاریخ محاسبات شمسی است.")
 val calc=Domain.calculators.first {it.id==s.calculator}
 val values=remember(s.calculator){mutableStateMapOf<String,String>().apply{calc.fields.forEach { put(it.key,it.options.firstOrNull()?.first?:if(it.key.contains("count")||it.key.contains("will")||it.key.contains("debts")||it.key.contains("stocks")||it.key.contains("deposits"))"0"else "") }}}
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val wide=maxWidth>=800.dp
  val selection:@Composable ()->Unit={
   if(wide)Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
    Domain.calculators.forEach { c->OutlinedButton(onClick={vm.calculator(c.id)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),colors=ButtonDefaults.outlinedButtonColors(containerColor=if(c.id==s.calculator)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)) {Text(c.title,Modifier.fillMaxWidth(),textAlign=TextAlign.Start)} }
   } else Choice("انتخاب محاسبه‌گر",s.calculator,Domain.calculators.map{it.id to it.title},vm::calculator)
  }
  val form:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
   GlassCard {
    Text(calc.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    calc.fields.forEach { f->if(f.options.isNotEmpty())Choice(f.label,values[f.key].orEmpty(),f.options){values[f.key]=it}else Field(f.label,values[f.key].orEmpty(),{values[f.key]=it},numeric=f.numeric) }
    if(s.calculator=="inheritance")Text("برای تقسیم‌های پیچیده، طبقات دیگر و تعدد زوجات از گفتگو راهنمایی بگیرید.",style=MaterialTheme.typography.bodySmall)
    if(s.calculator=="inheritance_tax")Text("برای ملک، ارزش مالیاتی ملاک است؛ دارایی ناموجود را صفر بگذارید.",style=MaterialTheme.typography.bodySmall)
    Action("محاسبه و نمایش نتیجه",s.posting){vm.compute(values.toMap())}
   }
   ErrorNotice(s)
   s.result?.let { result->GlassCard {
    Text("کاربرگ نتیجه رسمی حق‌سنج",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
    DataResult(result)
    Text("نتیجه وابسته به ورودی‌ها و منطق سرور است؛ قبل از اقدام رسمی بررسی شود.",style=MaterialTheme.typography.bodySmall)
   } }
  }}
  if(wide)Row(horizontalArrangement=Arrangement.spacedBy(24.dp)){Column(Modifier.width(260.dp)){selection()};Column(Modifier.weight(1f)){form()}}
  else Column(verticalArrangement=Arrangement.spacedBy(20.dp)){selection();form()}
 }
} }
@Composable fun DeadlineScreen(s:AppUiState,vm:AppViewModel) {Page{
 PageHeader("فرصت اقدام را از دست ندهید","تاریخ پایان را با مرجع قضایی تطبیق دهید. یادآور جایگزین پیگیری شخصی نیست.")
 Button(onClick={vm.openDialog("deadline","ثبت مهلت جدید")}){Icon(NativeIcons.Add,null);Text("ثبت مهلت")};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.items.isEmpty())EmptyState("مهلتی ثبت نشده","عنوان و تاریخ دقیق پایان مهلت را وارد کنید.")
 AdaptiveCards(s.items) { d->val title=d.str("title",d.str("case_title","مهلت قانونی"));val date=d.str("due_date",d.str("deadline_date"));Text(title,fontWeight=FontWeight.Medium);Text(dateFa(date));Text(if(d.str("status")=="done")"انجام‌شده"else "باز",color=MaterialTheme.colorScheme.primary);if(d.str("notes").isNotBlank())Text(d.str("notes"),style=MaterialTheme.typography.bodySmall);FlowRow{if(d.str("status")!="done")TextButton(onClick={vm.completeDeadline(d.optLong("id"))},enabled=!s.posting){Text("انجام شد")};TextButton(onClick={vm.calendar(title,date)}){Text("فایل تقویم")};TextButton(onClick={vm.reminder(d.optLong("id"),title,date)}){Text("یادآور گوشی")};TextButton(onClick={vm.openDialog("delete","حذف مهلت",JSONObject().put("kind","deadlines").put("id",d.optLong("id")))}){Text("حذف")}}};Pager(s,vm)
} }
@Composable fun PlansScreen(s:AppUiState,vm:AppViewModel) {Page{
 PageHeader("تعرفه‌های شفاف و منصفانه حق‌سنج","طرح مناسب نیاز خود را انتخاب کنید. قیمت‌ها از سرور دریافت می‌شوند؛ پرداخت فقط با تأیید شما انجام می‌شود.")
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(selected=s.planCycle=="monthly",onClick={vm.cycle("monthly")},label={Text("ماهانه")});FilterChip(selected=s.planCycle=="yearly",onClick={vm.cycle("yearly")},label={Text("سالانه")});if(s.authenticated)TextButton(onClick=vm::paymentHistory){Text("تاریخچه پرداخت")}}
 if(vm.pendingPayment)OutlinedButton(onClick={vm.verifyPayment()}){Text("استعلام آخرین پرداخت")}
 Loading(s);ErrorNotice(s,{vm.refresh()});AdaptiveCards(s.items.filter{it.str("billing_cycle")==s.planCycle}) { p->Text(p.str("badge_text","اشتراک حق‌سنج"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary);Text(p.str("name"),style=MaterialTheme.typography.titleMedium);Text(p.str("description"),style=MaterialTheme.typography.bodySmall);Text("${fa(p.optLong("price_toman"))} تومان",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("${fa(p.optInt("duration_days"))} روز اعتبار",style=MaterialTheme.typography.bodySmall);val features=p.optJSONArray("features")?:runCatching{JSONArray(p.str("features_json","[]"))}.getOrDefault(JSONArray());(0 until features.length()).forEach { i->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(NativeIcons.Check,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.primary);Text(features.optString(i),Modifier.weight(1f))}};Action("انتخاب این اشتراک",s.posting){vm.selectPlan(p)}}
} }
@Composable fun TicketsScreen(s:AppUiState,vm:AppViewModel) {Page{
 PageHeader("کنارتان هستیم","مشکلات حساب یا خدمات را برای تیم پشتیبانی بفرستید.")
 Button(onClick={vm.openDialog("ticket","درخواست پشتیبانی جدید")}){Icon(NativeIcons.Add,null);Text("درخواست جدید")};Loading(s);ErrorNotice(s,{vm.refresh()});if(!s.loading&&s.items.isEmpty())EmptyState("درخواست پشتیبانی ندارید","رمز عبور و اطلاعات بانکی را در تیکت ننویسید.")
 AdaptiveCards(s.items) { t->Text(t.str("subject"),fontWeight=FontWeight.Medium);Text(mapOf("open" to "باز","answered" to "پاسخ داده‌شده","closed" to "بسته‌شده")[t.str("status")]?:t.str("status"),color=MaterialTheme.colorScheme.primary);if(t.str("message").isNotBlank())Text(t.str("message"));t.objects("replies").plus(t.objects("messages")).forEach { r->Text(r.str("message",r.str("content",r.str("body"))),style=MaterialTheme.typography.bodySmall) };if(t.str("status")!="closed")OutlinedButton(onClick={vm.openDialog("reply","ارسال توضیح بیشتر",t)}){Text("پاسخ / توضیح بیشتر")}};Pager(s,vm)
} }

@Composable fun AppDialogs(s:AppUiState,vm:AppViewModel) {
 val d=s.dialog?:return
 androidx.compose.ui.window.Dialog(onDismissRequest=vm::closeDialog,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,dismissOnBackPress=!s.posting,dismissOnClickOutside=!s.posting)) {
 Surface(Modifier.widthIn(max=700.dp).fillMaxWidth().padding(16.dp).imePadding().heightIn(max=720.dp),shape=RoundedCornerShape(24.dp),color=MaterialTheme.colorScheme.surface,tonalElevation=0.dp) {
 key(d.kind,d.data.optLong("id"),d.title){Column(Modifier.verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(15.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(d.title,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);IconButton(onClick=vm::closeDialog,enabled=!s.posting){Icon(NativeIcons.Close,"بستن پنجره")}}
  when(d.kind){
   "delete"->{Text("این مورد حذف می‌شود. این عمل قابل بازگشت نیست.");Action("تأیید حذف",s.posting){vm.delete(d.data.str("kind"),d.data.optLong("id"))}}
   "logout"->{Text("اطلاعات روی سرور باقی می‌مانند؛ نشست و پیش‌نویس این دستگاه پاک می‌شود.");Action("خروج از حساب",s.posting,vm::logout)}
   "external"->{Text("این بخش در مرورگر امن گوشی باز می‌شود؛ رابط اصلی اپ بومی است.");Action("باز کردن در مرورگر",s.posting){vm.openExternal(d.data.str("url"));vm.closeDialog()}}
   "otp"->{var code by remember{mutableStateOf("")};Text("کد ارسال‌شده به ${d.data.str("phone")} را وارد کنید.");Field("کد یک‌بارمصرف",code,{code=it},numeric=true);Action("تأیید کد",s.posting){vm.verifyOtp(d.data.str("phone"),code)}}
   "password"->{var old by remember{mutableStateOf("")};var new by remember{mutableStateOf("")};var confirm by remember{mutableStateOf("")};Field("رمز فعلی",old,{old=it},password=true);Field("رمز جدید (حداقل ۶ نویسه)",new,{new=it},password=true);Field("تکرار رمز جدید",confirm,{confirm=it},password=true);Action("تغییر رمز عبور",s.posting){vm.changePassword(old,new,confirm)}}
   "documentCreate"->{var title by rememberSaveable{mutableStateOf("")};var detail by rememberSaveable{mutableStateOf("")};var type by rememberSaveable{mutableStateOf("دادخواست")};Choice("نوع برگه",type,listOf("دادخواست","اظهارنامه","شکواییه","لایحه دفاعیه","قرارداد").map{it to it}){type=it};Field("عنوان برگه",title,{title=it});Field("شرح ماجرا و خواسته",detail,{detail=it},multiline=true);Text("اسامی، تاریخ‌ها و مبلغ را دقیق بنویسید و متن نهایی را قبل از ثبت بررسی کنید.",style=MaterialTheme.typography.bodySmall);Action("تنظیم هوشمند برگه",s.posting){vm.createDocument(type,title,detail)}}
   "documentView"->{SelectionContainer{Text(d.data.str("content","متن سند موجود نیست."))};Text("این متن پیش‌نویس است؛ قبل از ثبت رسمی اطلاعات را بررسی کنید.",style=MaterialTheme.typography.bodySmall);FlowRow{OutlinedButton(onClick={vm.download(d.data.optLong("id"))}){Text("دریافت Word")};TextButton(onClick={vm.copy(d.data.str("content"))}){Text("کپی متن")}}}
   "documentEdit"->{var title by rememberSaveable{mutableStateOf(d.data.str("title"))};var content by rememberSaveable{mutableStateOf(d.data.str("content"))};Field("عنوان",title,{title=it});Field("متن سند",content,{content=it},multiline=true);Action("ذخیره تغییرات",s.posting){vm.saveDocument(d.data.optLong("id"),title,content)}}
   "deadline"->{var title by rememberSaveable{mutableStateOf("")};var date by rememberSaveable{mutableStateOf("")};var notes by rememberSaveable{mutableStateOf("")};var picker by remember{mutableStateOf(false)};Field("عنوان مهلت",title,{title=it});OutlinedButton(onClick={picker=true}){Icon(NativeIcons.CalendarMonth,null);Text(if(date.isBlank())"انتخاب تاریخ پایان مهلت"else dateFa(date)+" / "+date)};if(picker){val dateState=rememberDatePickerState();DatePickerDialog(onDismissRequest={picker=false},confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{date=Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()};picker=false}){Text("انتخاب")}},dismissButton={TextButton(onClick={picker=false}){Text("انصراف")}}){DatePicker(state=dateState,title={Text("تاریخ پایان (تقویم میلادی)",Modifier.padding(20.dp))})}};Field("یادداشت (اختیاری)",notes,{notes=it},multiline=true);Text("این اپ مهلت قانونی را از روی نوع دعوا حدس نمی‌زند؛ تاریخ تأییدشده را وارد کنید.",style=MaterialTheme.typography.bodySmall);Action("ثبت مهلت",s.posting){vm.addDeadline(title,date,notes)}}
   "ticket"->{var subject by rememberSaveable{mutableStateOf("")};var message by rememberSaveable{mutableStateOf("")};Field("موضوع",subject,{subject=it});Field("شرح درخواست",message,{message=it},multiline=true);Text("رمز، کد ورود و اطلاعات بانکی را ننویسید.",style=MaterialTheme.typography.bodySmall);Action("ارسال درخواست",s.posting){vm.createTicket(subject,message)}}
   "reply"->{var message by rememberSaveable{mutableStateOf("")};Field("پاسخ شما",message,{message=it},multiline=true);Action("ارسال پاسخ",s.posting){vm.replyTicket(d.data.optLong("id"),message)}}
   "checkout"->{var code by rememberSaveable{mutableStateOf("")};Text(d.data.str("name"),fontWeight=FontWeight.Bold);Text("مبلغ اولیه: ${fa(d.data.optLong("price_toman"))} تومان");Field("کد تخفیف (اختیاری)",code,{code=it});OutlinedButton(onClick={vm.validateDiscount(d.data.optLong("id"),code)},enabled=!s.posting){Text("بررسی کد تخفیف")};Text("با تأیید، سفارش ایجاد می‌شود و درگاه در مرورگر امن باز خواهد شد. بازگشت به اپ به معنی پرداخت موفق نیست؛ وضعیت از سرور استعلام می‌شود.",style=MaterialTheme.typography.bodySmall);Action("تأیید و ادامه به درگاه",s.posting){vm.checkout(d.data.optLong("id"),code)}}
   "payments"->{val payments=d.data.objects("items");if(payments.isEmpty())Text("پرداختی ثبت نشده است.");payments.forEach { p->DataResult(p);if(ApiContract.paymentAuthority(p).isNotBlank())TextButton(onClick={vm.verifyPayment(ApiContract.paymentAuthority(p))}){Text("استعلام این پرداخت")}}}
   "result"->{DataResult(d.data);Text("وضعیت مستقیماً از سرویس دریافت شده است.",style=MaterialTheme.typography.bodySmall);TextButton(onClick={vm.verifyPayment()}){Text("استعلام دوباره")}}
  }
  ErrorNotice(s)
 }}
 }
 }
}

@Composable fun NativeMarkdown(text:String){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){text.lines().forEach { line->
 val heading=line.startsWith('#');val raw=if(heading)line.trimStart('#',' ')else if(line.trimStart().startsWith("- ")||line.trimStart().startsWith("* "))"• "+line.trimStart().drop(2)else line
 if(raw.isNotBlank()){
 val annotated=androidx.compose.ui.text.buildAnnotatedString{var end=0;Regex("\\*\\*(.+?)\\*\\*").findAll(raw).forEach { m->append(raw.substring(end,m.range.first));pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight=FontWeight.Bold));append(m.groupValues[1]);pop();end=m.range.last+1 };append(raw.substring(end))}
 Text(annotated,style=if(heading)MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,fontWeight=if(heading)FontWeight.Bold else FontWeight.Normal)
 }
}}}
