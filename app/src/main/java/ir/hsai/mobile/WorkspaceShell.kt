@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ir.hsai.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

@Composable fun NativeApp(vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onDictate:()->Unit,onEditImage:()->Unit,snackbar:SnackbarHostState) {
 val s by vm.state.collectAsStateWithLifecycle()
 // P2-4: اندازهٔ متن انتخابی کاربر همان تایپوگرافی سایت را بزرگ/کوچک می‌کند
 // (فقط ضریب fontScale؛ تراکم پiksلی و همهٔ رنگ‌ها و فاصله‌های dp دست‌نخورده می‌مانند).
 val baseDensity=androidx.compose.ui.platform.LocalDensity.current
 val scaledDensity=remember(baseDensity,s.textScale){androidx.compose.ui.unit.Density(baseDensity.density,baseDensity.fontScale*s.textScale)}
 LaunchedEffect(Unit){vm.updateSessionClock()}
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,androidx.compose.ui.platform.LocalDensity provides scaledDensity) {
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
         // UI-parity: نوار بالای سایت یک «کلای‌بار» با هایرلاین پایین است، نه سرفیس تخت بدون مرز.
         // UI-parity: نوار بالای سایت `.soft-clay-bar` است (گرادیان + حاشیهٔ سفید + هایلایت ۱٫۵px)،
         // نه سرفیس یک‌رنگ M3؛ حالا همان لایه با همان گرادیان ساخته می‌شود.
         Column(Modifier.background(clayBarBrush())) {
         Box(Modifier.fillMaxWidth().height(1.5.dp).background(clayBarEdge()))
         TopAppBar(title={Text(if(s.screen==Screen.CHAT&&s.conversationId==null)"گفتگوی حقوقی جدید"else s.screen.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.semantics{heading()})},
          // سایت: `hsai-tap` = حداقل ۲٫۷۵rem (۴۴dp) — دکمه‌های ۴۰dp قبلی زیر حد لمسی بودند.
          // رنگ هم عیناً از سایت: bg-slate-100 dark:bg-white/10 (نه Soft.well).
          navigationIcon={if(layout==WorkspaceLayout.COMPACT)IconButton(onClick={scope.launch{drawer.open()}},modifier=Modifier.padding(start=6.dp).size(44.dp).background(if(s.dark)Color(0x1AFFFFFF) else Palette.Slate100,SiteShape.Xl)){Icon(NativeIcons.Menu,"منوی اصلی",Modifier.size(20.dp))}},
          actions={
           IconButton(onClick={vm.navigate(Screen.ACCOUNT)},modifier=Modifier.size(44.dp).background(if(s.dark)Color(0x1AFFFFFF) else Palette.Slate100,SiteShape.Xl)){Icon(NativeIcons.PersonOutline,if(s.authenticated)"حساب کاربری"else "ورود یا عضویت",Modifier.size(20.dp))}
           // ThemeToggle سایت: border-slate-200 bg-white text-slate-700 / dark: border-white/15 bg-white/10،
           // آیکون Moon در روشن (navy-700) و Sun در تیره (brass-300).
           IconButton(onClick=vm::toggleTheme,modifier=Modifier.padding(horizontal=4.dp).size(44.dp).border(1.dp,if(s.dark)Color(0x26FFFFFF) else Palette.Slate200,SiteShape.Xl).background(if(s.dark)Color(0x1AFFFFFF) else Color.White,SiteShape.Xl)){Icon(if(s.dark)NativeIcons.LightMode else NativeIcons.DarkMode,if(s.dark)"روشن‌کردن پوسته"else "تاریک‌کردن پوسته",Modifier.size(18.dp),tint=if(s.dark)Palette.Brass300 else Palette.Navy700)}
          },colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent,titleContentColor=Soft.ink(),actionIconContentColor=Soft.ink()))
         Box(Modifier.fillMaxWidth().height(1.dp).background(Soft.hairline()))
         }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
         if(s.reminderNotice)Surface(color=MaterialTheme.colorScheme.secondaryContainer) {
          Column(Modifier.fillMaxWidth().padding(12.dp)) {
           Text("تنظیم یادآورها به‌روز شد؛ یادآورهای قبلی را بررسی و در صورت نیاز دوباره ثبت کنید.",style=MaterialTheme.typography.bodySmall)
           Row {TextButton(onClick={vm.navigate(Screen.DEADLINES)}){Text("بررسی مهلت‌ها")};TextButton(onClick=vm::dismissReminderNotice){Text("متوجه شدم")}}
          }
         }
         if(!s.online)Surface(color=MaterialTheme.colorScheme.errorContainer){Column(Modifier.fillMaxWidth().padding(12.dp)){Text("اتصال برقرار نشد؛ اینترنت را بررسی و دوباره تلاش کنید.");TextButton(onClick={vm.checkConnection()},enabled=!s.checkingConnection){Text(if(s.checkingConnection)"در حال بررسی اتصال…"else "بررسی اتصال")}}}
         // دور نهم: بنر نشست (پیش از خطای ۴۰۱) و بنر دادهٔ ذخیره‌شده (حالت بی‌اتصال)
         SessionBanner(s,vm)
         StaleBanner(s,vm)
         if(s.posting&&s.screen!=Screen.CHAT)LinearProgressIndicator(Modifier.fillMaxWidth())
         when(s.screen) {
          Screen.CHAT->ChatScreen(s,vm,onImage,onCamera,onDictate,onEditImage)
          Screen.ACCOUNT->AccountScreen(s,vm)
          Screen.HISTORY->HistoryScreen(s,vm)
          Screen.DOCUMENTS->DocumentScreen(s,vm)
          Screen.SCANNER->ScannerScreen(s,vm,onImage,onCamera,onEditImage)
          Screen.LAWS->LawScreen(s,vm)
          Screen.CALCULATORS->CalculatorScreen(s,vm)
          Screen.DEADLINES->DeadlineScreen(s,vm)
          Screen.PLANS->PlansScreen(s,vm)
          Screen.TICKETS->TicketsScreen(s,vm)
          Screen.SETTINGS->SettingsScreen(s,vm)
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
   BrandEmblem(44.dp,modifier=Modifier.padding(vertical=12.dp))
   IconButton(onClick=vm::newChat){Icon(NativeIcons.Add,"گفتگوی جدید",tint=SiteGold)}
   // FIX(nav): حساب کاربری در ریل کنارِ آیکونِ پروفایلِ نوار بالا تکرار می‌شد و دو دکمهٔ هم‌نام
   // یک مقصد واحد داشتند؛ نسخهٔ ریل فقط مقصدهای انحصاری خودش را نشان می‌دهد.
   Screen.entries.filterNot{it==Screen.ACCOUNT}.forEach { screen->Surface(onClick={select(screen)},shape=SiteShape.Xl,color=if(s.screen==screen)SiteGold.copy(alpha=.18f)else Color.Transparent) {
    Icon(screen.symbol(),screen.title,Modifier.padding(16.dp).size(22.dp),tint=if(s.screen==screen)SiteGold else Palette.Slate300)
   }}
  } else Column(Modifier.statusBarsPadding().navigationBarsPadding()) {
   Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
     BrandEmblem(32.dp);Column{Text("حق‌سنج",fontWeight=FontWeight.Black);Text("مشاهده وب‌سایت اصلی",style=MaterialTheme.typography.labelSmall,color=SiteDesign.Brass300,modifier=Modifier.heightIn(min=44.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role=Role.Button){vm.openExternal("https://hsai.ir")})}
    }
    Button(onClick={vm.newChat();select(Screen.CHAT)},modifier=Modifier.fillMaxWidth(),shape=SiteShape.Xl,colors=ButtonDefaults.buttonColors(containerColor=SiteGold,contentColor=SiteNavy)) {Icon(NativeIcons.Add,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text("گفتگوی جدید",fontWeight=FontWeight.Bold)}
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
    if(s.sidebarHistory.size>3)OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text("جستجو در گفتگوهای اخیر…",style=MaterialTheme.typography.labelSmall)},textStyle=MaterialTheme.typography.bodySmall,shape=SiteShape.Xl,colors=OutlinedTextFieldDefaults.colors(unfocusedTextColor=Color.White,focusedTextColor=Color.White,unfocusedBorderColor=Color.White.copy(alpha=.12f),focusedBorderColor=SiteGold,unfocusedContainerColor=Color.White.copy(alpha=.05f)))
    val history=s.sidebarHistory.filter{query.isBlank()||it.str("title").contains(query,ignoreCase=true)}
    if(history.isEmpty())Text(if(query.isBlank())"گفتگوهای شما در این بخش نمایش داده می‌شوند."else "گفتگویی با این عبارت یافت نشد.",Modifier.padding(vertical=18.dp),style=MaterialTheme.typography.bodySmall,color=Palette.Slate400)
    history.forEach { conversation->
     Surface(onClick={vm.openConversation(conversation.optLong("id"));select(Screen.CHAT)},enabled=!s.posting,shape=SiteShape.Xl,color=if(s.conversationId==conversation.optLong("id"))Color.White.copy(alpha=.1f)else Color.Transparent,contentColor=Palette.Slate300) {
      Row(Modifier.fillMaxWidth().heightIn(min=48.dp).padding(8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
       Icon(NativeIcons.ChatBubbleOutline,null,Modifier.size(16.dp));Text(conversation.str("title","گفتگوی حقوقی"),Modifier.weight(1f),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall)
      }
     }
    }
    if(s.sidebarHistory.size>=30)TextButton(onClick={select(Screen.HISTORY)}){Text("مشاهده همه گفتگوها",color=SiteDesign.Brass300)}
   }
   HorizontalDivider(color=Color.White.copy(alpha=.1f))
   Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    // UI-parity: پایین سایدبار سایت سه چیز دارد که در اپ نبود: پلن با «روز باقی‌مانده»،
    // سهمیهٔ رایگان ماهانه، و ماندهٔ «گفتگو / پیام / سند». همه از همان پاسخ‌های سرور.
    Surface(onClick={select(Screen.PLANS)},modifier=Modifier.fillMaxWidth(),shape=SiteShape.Xl,color=SiteGold.copy(alpha=.15f),border=BorderStroke(1.dp,SiteGold.copy(alpha=.3f))) {
     Row(Modifier.padding(horizontal=12.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Icon(NativeIcons.Crown,null,Modifier.size(17.dp),tint=SiteGold)
      Text(s.subscription.str("plan_name",if(s.authenticated)"تهیه یا تمدید اشتراک"else "ورود و تهیهٔ اشتراک"),Modifier.weight(1f),style=MaterialTheme.typography.labelLarge,color=SiteGold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
      if(s.subscription.has("days_remaining"))Text(fa(s.subscription.optInt("days_remaining"))+" روز",style=MaterialTheme.typography.labelSmall,color=Palette.OkBright)
     }
    }
    val freeQuota=s.freeQuota
    if(s.subscription.length()==0&&freeQuota.optBoolean("enabled",false)&&freeQuota.has("remaining")) {
     Row(Modifier.fillMaxWidth().padding(horizontal=2.dp),horizontalArrangement=Arrangement.SpaceBetween) {
      Text("سهمیهٔ رایگان این ماه:",style=MaterialTheme.typography.labelSmall,color=Palette.Slate400)
      Text(fa(freeQuota.optInt("remaining"))+" از "+fa(freeQuota.optInt("limit"))+" پیام",style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=Palette.OkBright)
     }
    }
    if(s.subscription.has("messages_remaining")) {
     Row(Modifier.fillMaxWidth().padding(horizontal=2.dp),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
      Text("مانده:",style=MaterialTheme.typography.labelSmall,color=Palette.Slate400)
      listOf("consultations_remaining" to "گفتگو","messages_remaining" to "پیام","documents_remaining" to "سند").forEachIndexed { index,(key,label)->
       val value=s.subscription.optInt(key,-1)
       if(index>0)Text("•",style=MaterialTheme.typography.labelSmall,color=Palette.Slate500)
       Text((if(value<0)"∞" else fa(value))+" "+label,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold,color=Palette.Slate200)
      }
     }
    }
    SidebarLink(if(s.authenticated)"حساب کاربری، رمز عبور و فاکتورها"else "ورود / عضویت",NativeIcons.PersonOutline,Modifier.fillMaxWidth()){select(Screen.ACCOUNT)}
    SidebarLink("تنظیمات و داده‌های گوشی",NativeIcons.Settings,Modifier.fillMaxWidth()){select(Screen.SETTINGS)}
    Text(s.user.str("full_name",if(s.authenticated)"حساب حق‌سنج"else "کاربر مهمان"),style=MaterialTheme.typography.labelMedium,fontWeight=FontWeight.Bold)
   }
  }
 }
}
@Composable private fun SidebarLink(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier,onClick:()->Unit) {
 Surface(onClick=onClick,modifier=modifier,shape=SiteShape.Xl,color=Color.White.copy(alpha=.05f),contentColor=Palette.Slate200,border=BorderStroke(1.dp,Color.White.copy(alpha=.1f))) {
  Row(Modifier.heightIn(min=48.dp).padding(horizontal=8.dp,vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   Icon(icon,null,Modifier.size(16.dp),tint=SiteDesign.Brass300);Text(title,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.SemiBold)
  }
 }
}

/** Non-lazy grid inside the page's one vertical scroller; stable at large font scales. */
@Composable fun <T> AdaptiveCards(items:List<T>,minWidth:androidx.compose.ui.unit.Dp=300.dp,featured:(T)->Boolean={false},onDelete:((T)->Unit)?=null,deleteLabel:(T)->String={"حذف"},content:@Composable ColumnScope.(T)->Unit) {
 val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val columns=(maxWidth.value/(minWidth.value*fontScale.coerceAtLeast(1f))).toInt().coerceIn(1,3)
  // FIX(ui-دور۱۱): ردیف آخرِ ناقص، یک کارت + فضای خالی می‌ساخت و کارت «یتیم» تنها می‌ماند.
  // حالا کارت تنها تمام‌عرض می‌شود و ارتفاع با IntrinsicSize.Min تثبیت می‌گردد.
  Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
   items.chunked(columns).forEach { row ->
    val render:@Composable (T, Modifier)->Unit = { item, m ->
     // P2-1: اگر onDelete داده شود، هر کارت با کشیدن (به‌سمت پایان خط) درخواست حذف می‌دهد؛
     // خودِ حذف فقط با دیالوگ تأیید انجام می‌شود.
     val body:@Composable (Modifier)->Unit = { mm->if(featured(item))FeaturedPlan(mm.fillMaxHeight()){content(item)}else GlassCard(mm.fillMaxHeight()){content(item)} }
     if(onDelete==null)body(m)
     else SwipeToDelete(label=deleteLabel(item),enabled=true,onDelete={onDelete(item)},modifier=m.fillMaxHeight()){body(Modifier.fillMaxSize())}
    }
    if(row.size==1&&columns>1) render(row[0],Modifier.fillMaxWidth())
    else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
     row.forEach { item -> render(item,Modifier.weight(1f)) }
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
