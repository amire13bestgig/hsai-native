@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ir.hsai.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/** Width is the available window width, not the device model or orientation. */
enum class WorkspaceLayout { COMPACT, RAIL, SIDEBAR }
fun workspaceLayout(widthDp:Float):WorkspaceLayout = when {
 widthDp>=SiteDesign.DesktopBreakpoint -> WorkspaceLayout.SIDEBAR
 widthDp>=600f -> WorkspaceLayout.RAIL
 else -> WorkspaceLayout.COMPACT
}

@Composable fun NativeApp(vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEditImage:()->Unit,snackbar:SnackbarHostState) {
 val s by vm.state.collectAsStateWithLifecycle()
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
  HsaiTheme(if(s.entryComplete)s.dark else false) {
   key(vm.sessionVersion,s.authenticated,s.user.optLong("id")) {
   if(!s.initialized) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   } else if(!s.entryComplete) {
    BackHandler(enabled=!s.posting){vm.browseAsGuest()}
    Box(Modifier.fillMaxSize()) {
     LoginScreen(s,vm)
     SnackbarHost(snackbar,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding())
     AppDialogs(s,vm)
    }
   } else {
    BoxWithConstraints(Modifier.fillMaxSize()) {
     val layout=workspaceLayout(maxWidth.value)
     val drawer=rememberDrawerState(DrawerValue.Closed)
     val scope=rememberCoroutineScope()
     BackHandler(enabled=s.dialog!=null||s.pane!=null||drawer.isOpen||s.screen!=Screen.CHAT||s.backDepth>0) {
      if(drawer.isOpen)scope.launch{drawer.close()}else vm.goBack()
     }
     val select:(Screen)->Unit={if(vm.state.value.screen!=it)vm.navigate(it);scope.launch{drawer.close()}}
     val body:@Composable ()->Unit={
      Row(Modifier.fillMaxSize()) {
       if(layout!=WorkspaceLayout.COMPACT)WorkspaceMenu(s,vm,select,layout==WorkspaceLayout.RAIL)
       Scaffold(modifier=Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.background,
        snackbarHost={SnackbarHost(snackbar)},topBar={
         TopAppBar(title={Text(if(s.screen==Screen.CHAT&&s.conversationId==null)"گفتگوی حقوقی جدید"else s.screen.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)},
          navigationIcon={if(layout==WorkspaceLayout.COMPACT)IconButton(onClick={scope.launch{drawer.open()}}){Icon(NativeIcons.Menu,"منوی اصلی")}},
          actions={
           IconButton(onClick={vm.navigate(Screen.ACCOUNT)}){Icon(NativeIcons.PersonOutline,if(s.authenticated)"حساب کاربری"else "ورود یا عضویت")}
           IconButton(onClick=vm::toggleTheme){Icon(if(s.dark)NativeIcons.LightMode else NativeIcons.DarkMode,"تغییر حالت روشن و تیره")}
          },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
         if(s.reminderNotice)Surface(color=MaterialTheme.colorScheme.secondaryContainer) {
          Column(Modifier.fillMaxWidth().padding(12.dp)) {
           Text("تنظیم یادآورها به‌روز شد؛ یادآورهای قبلی را بررسی و در صورت نیاز دوباره ثبت کنید.",style=MaterialTheme.typography.bodySmall)
           Row {TextButton(onClick={vm.navigate(Screen.DEADLINES)}){Text("بررسی مهلت‌ها")};TextButton(onClick=vm::dismissReminderNotice){Text("متوجه شدم")}}
          }
         }
         if(!s.online)Surface(color=MaterialTheme.colorScheme.errorContainer){Column(Modifier.fillMaxWidth().padding(12.dp)){Text("اتصال برقرار نشد؛ اینترنت را بررسی و دوباره تلاش کنید.");TextButton(onClick={vm.checkConnection()},enabled=!s.checkingConnection){Text(if(s.checkingConnection)"در حال بررسی اتصال…"else "بررسی اتصال")}}}
         if(s.posting&&s.screen!=Screen.CHAT)LinearProgressIndicator(Modifier.fillMaxWidth())
         when(s.screen) {
          Screen.CHAT->ChatScreen(s,vm,onImage,onCamera,onEditImage)
          Screen.ACCOUNT->AccountScreen(s,vm)
          Screen.HISTORY->HistoryScreen(s,vm)
          Screen.DOCUMENTS->DocumentScreen(s,vm)
          Screen.SCANNER->ScannerScreen(s,vm,onImage,onCamera,onEditImage)
          Screen.LAWS->LawScreen(s,vm)
          Screen.CALCULATORS->CalculatorScreen(s,vm)
          Screen.DEADLINES->DeadlineScreen(s,vm)
          Screen.PLANS->PlansScreen(s,vm)
          Screen.TICKETS->TicketsScreen(s,vm)
         }
        }
       }
      }
     }
     if(layout==WorkspaceLayout.COMPACT)ModalNavigationDrawer(drawerState=drawer,drawerContent={WorkspaceMenu(s,vm,select,false)}){body()}else body()
     AppDialogs(s,vm)
    }
   }
  }
 }
}

}

@Composable private fun WorkspaceMenu(s:AppUiState,vm:AppViewModel,select:(Screen)->Unit,rail:Boolean) {
 Surface(Modifier.width(if(rail)88.dp else 288.dp).fillMaxHeight(),color=SiteNavy,contentColor=Color.White) {
  if(rail)Column(Modifier.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
   BrandEmblem(Modifier.padding(vertical=12.dp))
   IconButton(onClick=vm::newChat){Icon(NativeIcons.Add,"گفتگوی جدید",tint=SiteGold)}
   Screen.entries.forEach { screen->Surface(onClick={select(screen)},shape=RoundedCornerShape(12.dp),color=if(s.screen==screen)SiteGold.copy(alpha=.18f)else Color.Transparent) {
    Icon(screen.symbol(),screen.title,Modifier.padding(16.dp).size(22.dp),tint=if(s.screen==screen)SiteGold else Color(0xFFCFD8E1))
   }}
  } else Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
     BrandEmblem(Modifier.size(32.dp));Column{Text("حق‌سنج",fontWeight=FontWeight.Black);Text("مشاهده وب‌سایت اصلی",style=MaterialTheme.typography.labelSmall,color=SiteDesign.Brass300,modifier=Modifier.clickable{vm.openExternal("https://hsai.ir")})}
    }
    Button(onClick={vm.newChat();select(Screen.CHAT)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=SiteGold,contentColor=SiteNavy)) {Icon(NativeIcons.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("گفتگوی جدید",fontWeight=FontWeight.Bold)}
    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
     listOf(Screen.LAWS,Screen.CALCULATORS).forEach { screen->SidebarLink(screen.title,screen.symbol(),Modifier.weight(1f)){select(screen)} }
    }
   }
   HorizontalDivider(color=Color.White.copy(alpha=.1f))
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    val tools=listOf(Screen.DOCUMENTS,Screen.SCANNER,Screen.DEADLINES,Screen.TICKETS)
    tools.chunked(2).forEach { row->Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){row.forEach { screen->SidebarLink(screen.title,screen.symbol(),Modifier.weight(1f)){select(screen)} }} }
    SidebarLink("گفتگوهای من",NativeIcons.History,Modifier.fillMaxWidth()){select(Screen.HISTORY)}
    var query by rememberSaveable{mutableStateOf("")}
    if(s.sidebarHistory.size>3)OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("جستجو در گفتگوهای اخیر…",style=MaterialTheme.typography.labelSmall)},textStyle=MaterialTheme.typography.bodySmall,shape=RoundedCornerShape(10.dp),colors=OutlinedTextFieldDefaults.colors(unfocusedTextColor=Color.White,focusedTextColor=Color.White,unfocusedBorderColor=Color.White.copy(alpha=.12f),focusedBorderColor=SiteGold,unfocusedContainerColor=Color.White.copy(alpha=.05f)))
    val history=s.sidebarHistory.filter{query.isBlank()||it.str("title").contains(query,ignoreCase=true)}
    if(history.isEmpty())Text(if(query.isBlank())"گفتگوهای شما در این بخش نمایش داده می‌شوند."else "گفتگویی با این عبارت یافت نشد.",Modifier.padding(vertical=18.dp),style=MaterialTheme.typography.bodySmall,color=Color(0xFF94A3B8))
    history.forEach { conversation->
     Surface(onClick={vm.openConversation(conversation.optLong("id"));select(Screen.CHAT)},enabled=!s.posting,shape=RoundedCornerShape(10.dp),color=if(s.conversationId==conversation.optLong("id"))Color.White.copy(alpha=.1f)else Color.Transparent,contentColor=Color(0xFFCBD5E1)) {
      Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       Icon(NativeIcons.ChatBubbleOutline,null,Modifier.size(16.dp));Text(conversation.str("title","گفتگوی حقوقی"),Modifier.weight(1f),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
      }
     }
    }
    if(s.sidebarHistory.size>=30)TextButton(onClick={select(Screen.HISTORY)}){Text("مشاهده همه گفتگوها",color=SiteDesign.Brass300)}
   }
   HorizontalDivider(color=Color.White.copy(alpha=.1f))
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    SidebarLink(s.subscription.str("plan_name","اشتراک و پرداخت‌ها"),NativeIcons.WorkspacePremium,Modifier.fillMaxWidth()){select(Screen.PLANS)}
    SidebarLink(if(s.authenticated)"حساب کاربری، رمز عبور و فاکتورها"else "ورود / عضویت",NativeIcons.PersonOutline,Modifier.fillMaxWidth()){select(Screen.ACCOUNT)}
    Text(s.user.str("full_name",if(s.authenticated)"حساب حق‌سنج"else "کاربر مهمان"),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
   }
  }
 }
}
@Composable private fun SidebarLink(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit) {
 Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(10.dp),color=Color.White.copy(alpha=.05f),contentColor=Color(0xFFDFE7EF),border=BorderStroke(1.dp,Color.White.copy(alpha=.1f))) {
  Row(Modifier.heightIn(min=48.dp).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   Icon(icon,null,Modifier.size(16.dp),tint=SiteDesign.Brass300);Text(title,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
  }
 }
}

/** Non-lazy grid inside the page's one vertical scroller; stable at large font scales. */
@Composable fun <T> AdaptiveCards(items:List<T>,minWidth:androidx.compose.ui.unit.Dp=300.dp,featured:(T)->Boolean={false},content:@Composable ColumnScope.(T)->Unit) {
 val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val columns=(maxWidth.value/(minWidth.value*fontScale.coerceAtLeast(1f))).toInt().coerceIn(1,3)
  Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
   items.chunked(columns).forEach { row ->
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
     row.forEach { item -> if(featured(item))FeaturedPlan(Modifier.weight(1f).fillMaxHeight()){content(item)}else GlassCard(Modifier.weight(1f).fillMaxHeight()){content(item)} }
     repeat(columns-row.size){Spacer(Modifier.weight(1f))}
    }
   }
  }
 }
}

@Composable fun AdaptiveSplit(first:@Composable ()->Unit,second:@Composable ()->Unit) {
 val scale=androidx.compose.ui.platform.LocalDensity.current.fontScale
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  if(maxWidth>=660.dp*scale.coerceAtLeast(1f))Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
   Column(Modifier.weight(1f)){first()};Column(Modifier.weight(1f)){second()}
  } else Column(verticalArrangement=Arrangement.spacedBy(20.dp)){first();second()}
 }
}
