@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

@Composable fun WorkspaceListDetail(s:AppUiState,vm:AppViewModel,kinds:Set<String>,list:@Composable (Boolean)->Unit,detail:@Composable (AppDialog)->Unit) {
 val scale=LocalDensity.current.fontScale.coerceAtLeast(1f)
 BoxWithConstraints(Modifier.fillMaxSize()) {
  val wide=maxWidth>=720.dp*scale&&kinds.isNotEmpty()
  LaunchedEffect(wide,s.pane,s.dialog){vm.adaptDetail(wide,kinds)}
  if(wide)Row(Modifier.fillMaxSize()) {
   Box(Modifier.weight(.42f).fillMaxHeight()){list(true)}
   VerticalDivider()
   Surface(Modifier.weight(.58f).fillMaxHeight().padding(12.dp),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,MaterialTheme.colorScheme.outlineVariant)) {
    val current=s.pane?.takeIf{it.kind in kinds}
    if(current==null)Column(Modifier.padding(24.dp)){Text("پیش‌نمایش",style=MaterialTheme.typography.titleMedium);if(s.posting){CircularProgressIndicator();Text("در حال دریافت پیش‌نمایش…")}else if(s.error.isNotBlank()&&s.dialog==null)ErrorNotice(s)else Text("یک مورد را از فهرست انتخاب کنید.",style=MaterialTheme.typography.bodyMedium)}
    else Column(Modifier.fillMaxSize().padding(16.dp)) {
     Row(Modifier.fillMaxWidth()) {Text(current.title,Modifier.weight(1f),maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold);IconButton(onClick=vm::closePane,enabled=!s.posting){Icon(NativeIcons.Close,"بستن پیش‌نمایش")}}
     Box(Modifier.weight(1f).fillMaxWidth()){key(current.kind,current.data.optLong("id"),current.title){detail(current)}}
    }
   }
  } else list(false)
 }
}
@Composable fun ScrollableDetail(d:AppDialog,s:AppUiState,vm:AppViewModel,article:Boolean) {
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)) {
  if(article)ArticleDetailContent(d,vm)else DocumentDetailContent(d,s,vm)
  ErrorNotice(s)
 }
}
@Composable fun HistoryDetail(d:AppDialog,s:AppUiState,vm:AppViewModel,modifier:Modifier) {
 val messages=remember(d.data){d.data.objects("messages").filter{it.str("role") in setOf("user","assistant")}.map{if(it.str("role")=="assistant")ApiContract.chatMessage(it)else ChatMessage("user",it.str("content"))}}
 Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Button(onClick={vm.openConversation(d.data.optLong("id"))},enabled=!s.posting){Text("ادامه گفتگو")}
  Text("پیش‌نمایش فقط‌خواندنی است؛ برای پاسخ، ادامه گفتگو را انتخاب کنید.",style=MaterialTheme.typography.bodySmall)
  LazyColumn(Modifier.weight(1f).fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)){itemsIndexed(messages){_,message->MessageCard(message,true,vm)}}
 }
}
@Composable fun ColumnScope.DocumentDetailContent(d:AppDialog,s:AppUiState,vm:AppViewModel) {
 if(d.kind=="documentEdit"){
  val id=d.data.optLong("id")
  val draft=s.documentDrafts[id]
  val title=draft?.title?:d.data.str("title");val content=draft?.content?:d.data.str("content")
  Field("عنوان",title,{vm.updateDocumentDraft(id,it,content)},error=s.fieldErrors["title"],enabled=!s.posting,onEdited={vm.inputEdited("title")},firstError=s.firstField("title"),attempt=s.validationAttempt)
  Field("متن سند",content,{vm.updateDocumentDraft(id,title,it)},multiline=true,error=s.fieldErrors["content"],enabled=!s.posting,onEdited={vm.inputEdited("content")},firstError=s.firstField("content"),attempt=s.validationAttempt)
  if(title!=d.data.str("title")||content!=d.data.str("content"))Text("تغییرات هنوز ذخیره نشده‌اند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  if(draft!=null)TextButton(onClick={vm.discardDocumentDraft(id)},enabled=!s.posting){Text("بازگشت به متن ذخیره‌شده")}
  Text("پیش‌نویس ویرایش با بستن فرایند اپ باقی نمی‌ماند؛ تغییرات مهم را ذخیره کنید.",style=MaterialTheme.typography.bodySmall)
  Action("ذخیره تغییرات",s.posting){vm.saveDocument(id,title,content)}
 }else{
DocumentPaper(d.data.str("content","متن سند موجود نیست."));Text("این متن پیش‌نویس است؛ قبل از ثبت رسمی اطلاعات را بررسی کنید.",style=MaterialTheme.typography.bodySmall);FlowRow{if(d.data.optLong("id")>0)OutlinedButton(onClick={vm.download(d.data.optLong("id"))}){Text("دریافت Word")};TextButton(onClick={vm.copy(d.data.str("content"))}){Text("کپی متن")}}
 }
}
@Composable fun ColumnScope.ArticleDetailContent(d:AppDialog,vm:AppViewModel) {
SelectionContainer{Text(d.data.str("official_text"))};if(d.data.str("plain_explanation").isNotBlank())Text(d.data.str("plain_explanation"));if(d.data.has("practical_tips"))DataResult(JSONObject().put("practical_tips",d.data.opt("practical_tips")));TextButton(onClick={vm.copy(d.data.str("official_text"))}){Text("کپی متن قانون")};OutlinedButton(onClick={vm.askAboutArticle(d.data)}){Text("پرسش درباره این ماده")}
}
