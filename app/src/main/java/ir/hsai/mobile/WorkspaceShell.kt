@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package ir.hsai.mobile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 widthDp>=1000f -> WorkspaceLayout.SIDEBAR
 widthDp>=600f -> WorkspaceLayout.RAIL
 else -> WorkspaceLayout.COMPACT
}

@Composable fun NativeApp(vm:AppViewModel,onImage:()->Unit,onCamera:()->Unit,onEditImage:()->Unit,snackbar:SnackbarHostState) {
 val s by vm.state.collectAsStateWithLifecycle()
 CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
  HsaiTheme(if(s.entryComplete)s.dark else false) {
   if(!s.entryComplete) {
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
     BackHandler(enabled=s.dialog!=null||drawer.isOpen||s.screen!=Screen.CHAT||s.backDepth>0) {
      if(drawer.isOpen)scope.launch{drawer.close()}else vm.goBack()
     }
     val select:(Screen)->Unit={vm.navigate(it);scope.launch{drawer.close()}}
     val body:@Composable ()->Unit={
      Row(Modifier.fillMaxSize()) {
       if(layout!=WorkspaceLayout.COMPACT)WorkspaceMenu(s,vm,select,layout==WorkspaceLayout.RAIL)
       Scaffold(modifier=Modifier.weight(1f),containerColor=MaterialTheme.colorScheme.background,
        snackbarHost={SnackbarHost(snackbar)},topBar={
         TopAppBar(title={Text(s.screen.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=1)},
          navigationIcon={if(layout==WorkspaceLayout.COMPACT)IconButton(onClick={scope.launch{drawer.open()}}){Icon(NativeIcons.Menu,"منوی اصلی")}},
          actions={
           if(layout!=WorkspaceLayout.COMPACT)TextButton(onClick={vm.navigate(Screen.CALCULATORS)}){Text("محاسبه مهریه و هزینه‌ها")}
           IconButton(onClick={vm.navigate(Screen.ACCOUNT)}){Icon(NativeIcons.PersonOutline,"حساب کاربری")}
           IconButton(onClick=vm::toggleTheme){Icon(if(s.dark)NativeIcons.LightMode else NativeIcons.DarkMode,"تغییر حالت روشن و تیره")}
          },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.surface))
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
         if(!s.online)Surface(color=MaterialTheme.colorScheme.errorContainer){Text("اتصال برقرار نشد؛ اینترنت را بررسی و دوباره تلاش کنید.",Modifier.fillMaxWidth().padding(12.dp))}
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

@Composable private fun WorkspaceMenu(s:AppUiState,vm:AppViewModel,select:(Screen)->Unit,rail:Boolean) {
 Surface(Modifier.width(if(rail)88.dp else 272.dp).fillMaxHeight(),color=SiteNavy,contentColor=Color.White) {
  Column(Modifier.statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(if(rail)8.dp else 16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(Modifier.fillMaxWidth().padding(vertical=16.dp),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
    Icon(NativeIcons.Balance,null,Modifier.size(30.dp),tint=SiteGold)
    if(!rail){Spacer(Modifier.width(12.dp));Column{Text("حق‌سنج",fontWeight=FontWeight.Bold);Text("اول حقت را بسنج، بعد قدم بردار",style=MaterialTheme.typography.labelSmall,color=SiteGold)}}
   }
   Button(onClick=vm::newChat,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(10.dp),colors=ButtonDefaults.buttonColors(containerColor=SiteGold,contentColor=SiteNavy)) {
    Icon(NativeIcons.Add,if(rail)"گفتگوی جدید"else null)
    if(!rail){Spacer(Modifier.width(6.dp));Text("گفتگوی جدید")}
   }
   HorizontalDivider(Modifier.padding(vertical=8.dp),color=Color.White.copy(alpha=.12f))
   Screen.entries.forEach { screen ->
    val selected=s.screen==screen
    Surface(onClick={select(screen)},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=if(selected)SiteGold.copy(alpha=.18f)else Color.Transparent,contentColor=if(selected)SiteGold else Color(0xFFCFD8E1)) {
     Column(Modifier.padding(vertical=if(rail)10.dp else 0.dp),horizontalAlignment=Alignment.CenterHorizontally) {
      Row(Modifier.heightIn(min=48.dp).padding(horizontal=if(rail)0.dp else 12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
       Icon(screen.symbol(),if(rail)screen.title else null,Modifier.size(22.dp))
       if(!rail)Text(screen.title,style=MaterialTheme.typography.bodyMedium,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal)
      }
     }
    }
   }
   if(!rail){HorizontalDivider(color=Color.White.copy(alpha=.12f));Text(s.user.str("full_name","حساب کاربری"),Modifier.padding(top=12.dp),fontWeight=FontWeight.Bold);Text("پاسخ‌ها مستند به قوانین رسمی ایران است؛ جایگزین وکالت در جلسه دادگاه نیست.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF9AABBA))}
  }
 }
}

/** Non-lazy grid inside the page's one vertical scroller; stable at large font scales. */
@Composable fun <T> AdaptiveCards(items:List<T>,minWidth:androidx.compose.ui.unit.Dp=300.dp,content:@Composable ColumnScope.(T)->Unit) {
 val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val columns=(maxWidth.value/(minWidth.value*fontScale.coerceAtLeast(1f))).toInt().coerceIn(1,3)
  Column(verticalArrangement=Arrangement.spacedBy(14.dp)) {
   items.chunked(columns).forEach { row ->
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
     row.forEach { item -> GlassCard(Modifier.weight(1f).fillMaxHeight()){content(item)} }
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
