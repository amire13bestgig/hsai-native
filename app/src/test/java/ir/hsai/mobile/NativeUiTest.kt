package ir.hsai.mobile

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

/** Native Compose rendering only; fixtures never authenticate or write to production. No APK task required. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[29],qualifiers="w390dp-h844dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NativeUiTest {
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val vm get()=ViewModelProvider(compose.activity)[AppViewModel::class.java]
 private fun ready(){
  compose.waitUntil(15000){vm.state.value.initialized}
  // Every subsequent UI action uses a local in-process HTTP response, never private production data.
  val http=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("UI fixture")
    .body("{\"ok\":true,\"data\":{\"items\":[]}}".toResponseBody("application/json".toMediaType())).build()
  }.build()
  val field=ApiClient::class.java.getDeclaredField("http");field.isAccessible=true;field.set(vm.api,http)
  vm.api.token="test-only-not-a-real-session"
  compose.waitForIdle()
 }
 @Suppress("UNCHECKED_CAST") private fun state(value:AppUiState) {
  compose.runOnIdle {
   val field=AppViewModel::class.java.getDeclaredField("mutable");field.isAccessible=true
   (field.get(vm) as MutableStateFlow<AppUiState>).value=value
  };compose.waitForIdle()
 }
 private fun fixture(screen:Screen,dark:Boolean=false):AppUiState {
  val item=when(screen) {
   Screen.HISTORY->JSONObject().put("id",1).put("title","گفتگوی آزمایشی درباره اجاره").put("created_at","2026-11-05")
   Screen.DOCUMENTS->JSONObject().put("id",1).put("title","پیش‌نویس آزمایشی اظهارنامه").put("doc_type","اظهارنامه")
   Screen.SCANNER->JSONObject().put("title","تحلیل آزمایشی")
   Screen.LAWS->JSONObject().put("law_name","قانون نمونه").put("article_number","۱").put("official_text","این متن صرفاً داده آزمایشی رابط است و استناد واقعی نیست.")
   Screen.DEADLINES->JSONObject().put("id",1).put("title","بررسی مهلت آزمایشی").put("due_date","2026-11-25").put("status","open")
   Screen.PLANS->JSONObject().put("id",1).put("name","طرح آزمایشی").put("billing_cycle","monthly").put("price_toman",195000).put("duration_days",30)
   Screen.TICKETS->JSONObject().put("id",1).put("subject","درخواست آزمایشی").put("message","شرح کوتاه آزمایشی").put("status","open")
   else->null
  }
  return AppUiState(screen=screen,entryComplete=true,authenticated=true,initialized=true,dark=dark,
   user=JSONObject().put("full_name","کاربر آزمایشی").put("email","sample@example.com"),
   subscription=JSONObject().put("plan_name","اشتراک آزمایشی").put("days_remaining",30),items=if(item==null)emptyList()else listOf(item,item))
 }
 private fun screenshot(name:String) {
  val file=File("build/native-screenshots/review/$name.png");file.parentFile?.mkdirs()
  compose.runOnIdle {
   val globalClass=Class.forName("android.view.WindowManagerGlobal")
   val global=globalClass.getMethod("getInstance").invoke(null)
   val viewsField=globalClass.getDeclaredField("mViews");viewsField.isAccessible=true
   @Suppress("UNCHECKED_CAST") val views=viewsField.get(global) as List<android.view.View>
   val view=views.lastOrNull{it.width>0&&it.height>0}?:compose.activity.window.decorView
   val bitmap=android.graphics.Bitmap.createBitmap(view.width,view.height,android.graphics.Bitmap.Config.ARGB_8888)
   view.draw(android.graphics.Canvas(bitmap));file.outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
  }
 }
 private fun auditScreens(prefix:String) {
  ready();screenshot("$prefix-login")
  Screen.entries.forEach { screen->state(fixture(screen));compose.onRoot().assertExists();screenshot("$prefix-${screen.name.lowercase()}") }
 }
 @Test fun loginIsFirstAndWorkspaceHidden() {
  ready();compose.onNodeWithText("ورود به میز کار حق‌سنج").assertIsDisplayed()
  compose.onNodeWithContentDescription("منوی اصلی").assertDoesNotExist()
  compose.onNodeWithContentDescription("آدرس ایمیل یا شماره موبایل").performTextInput("sample@example.com")
  compose.onNodeWithContentDescription("رمز عبور").performTextInput("test-only-password")
  compose.onNodeWithContentDescription("نمایش رمز").performClick()
  compose.onNodeWithText("test-only-password").assertExists()
 }
 @Test fun loginValidatesBeforeNetwork() {
  ready();compose.onNodeWithText("ورود به حساب کاربری").performScrollTo().performClick()
  compose.onAllNodesWithText("رمز عبور را وارد کنید.").onFirst().assertExists()
 }
 @Test fun registerUsesRealNativeFields() {
  ready();compose.onNodeWithText("عضویت جدید",substring=false).performClick()
  compose.onNodeWithContentDescription("نام و نام خانوادگی رسمی").assertExists()
  compose.onNodeWithContentDescription("شماره موبایل").assertExists()
 }
 @Test fun restoredSessionStillShowsEntryBeforeWorkspace() {
  ready();state(fixture(Screen.ACCOUNT).copy(entryComplete=false))
  compose.onNodeWithText("ورود به میز کار حق‌سنج").assertExists()
  compose.onNodeWithText("ادامه به میز کار").performScrollTo().performClick()
  compose.onNodeWithContentDescription("منوی اصلی").assertExists()
 }
 @Test fun mobileMenuNavigatesToAllSections() {
  ready();state(fixture(Screen.CHAT))
  compose.onNodeWithContentDescription("منوی اصلی").performClick()
  compose.onNodeWithText("اوراق قضایی").performScrollTo().performClick()
  compose.onNodeWithText("اوراق قضایی شما").assertExists()
 }
 @Test fun composerPreservesDraftAcrossNavigation() {
  ready();state(fixture(Screen.CHAT));compose.onNode(hasSetTextAction()).performTextInput("سؤال آزمایشی")
  compose.onNodeWithContentDescription("حساب کاربری").performClick()
  compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()};compose.waitForIdle()
  compose.onNodeWithText("سؤال آزمایشی").assertExists()
 }
 @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun allScreensSmallMobile(){auditScreens("mobile320")}
 @Test fun allScreensMobile(){auditScreens("mobile390")}
 @Test @Config(qualifiers="w768dp-h1024dp-mdpi") fun allScreensPortraitTablet(){auditScreens("tablet768")}
 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun allScreensLargeTablet(){auditScreens("tablet1024")}
 @Test @Config(qualifiers="w1280dp-h800dp-land-mdpi") fun allScreensLandscapeTablet(){auditScreens("tablet1280")}
 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun allScreensDarkTablet(){
  ready();Screen.entries.forEach{screen->state(fixture(screen,true));screenshot("dark1024-${screen.name.lowercase()}")}
 }
 @Test fun breakpointsFollowWindowNotDeviceName(){
  assertEquals(WorkspaceLayout.COMPACT,workspaceLayout(599f))
  assertEquals(WorkspaceLayout.RAIL,workspaceLayout(600f))
  assertEquals(WorkspaceLayout.RAIL,workspaceLayout(999f))
  assertEquals(WorkspaceLayout.SIDEBAR,workspaceLayout(1000f))
 }
 private fun auditDialogs(prefix:String) {
  ready()
  listOf("delete","logout","external","otp","password","documentCreate","documentView","documentEdit","deadline","ticket","reply","checkout","payments","result").forEach { kind ->
   val data=JSONObject().put("id",1).put("kind","documents").put("title","سند آزمایشی").put("content","متن آزمایشی سند برای بررسی رابط").put("phone","09121234567").put("name","طرح آزمایشی").put("price_toman",195000)
   state(fixture(Screen.ACCOUNT).copy(dialog=AppDialog(kind,"بررسی پنجره $kind",data)))
   compose.onNodeWithText("بررسی پنجره $kind").assertIsDisplayed();screenshot("$prefix-dialog-$kind")
  }
 }
 @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun dialogsOnSmallMobile(){auditDialogs("mobile320")}
 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun dialogsOnTablet(){auditDialogs("tablet1024")}
 private fun auditCalculators(prefix:String) {
  ready();Domain.calculators.forEach { calculator ->
   state(fixture(Screen.CALCULATORS).copy(calculator=calculator.id))
   compose.onNodeWithText("محاسبه و نمایش نتیجه").performScrollTo().assertIsDisplayed()
   screenshot("$prefix-calculator-${calculator.id}")
  }
 }
 @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun everyCalculatorOnSmallMobile(){auditCalculators("mobile320")}
 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun everyCalculatorOnTablet(){auditCalculators("tablet1024")}
 @Test @Config(qualifiers="w320dp-h640dp-mdpi") fun largeFontLoginAndCalculatorStayReachable() {
  ready()
  compose.runOnIdle {
   val resources=compose.activity.resources
   val config=android.content.res.Configuration(resources.configuration).apply{fontScale=1.6f}
   @Suppress("DEPRECATION") resources.updateConfiguration(config,resources.displayMetrics)
   compose.activity.window.decorView.dispatchConfigurationChanged(config)
  };compose.waitForIdle()
  compose.onNodeWithText("ورود به حساب کاربری").performScrollTo().assertIsDisplayed();screenshot("largefont320-login")
  state(fixture(Screen.CALCULATORS).copy(calculator="inheritance"))
  compose.onNodeWithText("محاسبه و نمایش نتیجه").performScrollTo().assertIsDisplayed();screenshot("largefont320-calculator")
 }

 private fun stub(body:String,stream:Boolean=false,gate:java.util.concurrent.CountDownLatch?=null) {
  val http=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
   if(chain.request().method=="POST")gate?.await(10,java.util.concurrent.TimeUnit.SECONDS)
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("fixture")
    .header("Content-Type",if(stream)"text/event-stream"else "application/json")
    .body(body.toResponseBody((if(stream)"text/event-stream"else "application/json").toMediaType())).build()
  }.build()
  val f=ApiClient::class.java.getDeclaredField("http");f.isAccessible=true;f.set(vm.api,http)
 }
 @Test fun historicalConversationResponseIsNotDiscardedByNavigation() {
  ready();state(fixture(Screen.HISTORY))
  stub("""{"ok":true,"data":{"conversation":{"id":42},"messages":[{"id":77,"role":"assistant","content":"پاسخ ذخیره‌شده آزمایشی"}]}}""")
  compose.runOnIdle{vm.openConversation(42)}
  compose.waitUntil(10000){vm.state.value.messages.any{it.text=="پاسخ ذخیره‌شده آزمایشی"}}
  assertEquals(Screen.CHAT,vm.state.value.screen);assertEquals(42L,vm.state.value.conversationId)
 }
 @Test fun streamingKeepsArticlesAcrossDeltasAndUpdatesSubscription() {
  ready();state(fixture(Screen.CHAT))
  stub("""event: articles
data: {"articles":[{"law_name":"قانون آزمایشی","article_number":"۱"}]}

event: delta
data: {"chunk":"متن "}

event: delta
data: {"chunk":"پاسخ"}

event: done
data: {"answer":"متن پاسخ","conversation_id":42,"message_id":77,"subscription":{"days_remaining":7}}

""",true)
  compose.runOnIdle{vm.setStream(true);vm.setDraft("پرسش آزمایشی");vm.sendChat()}
  compose.waitUntil(10000){vm.state.value.messages.lastOrNull()?.id==77L}
  assertEquals(1,vm.state.value.messages.last().citations.size)
  assertEquals("متن پاسخ",vm.state.value.messages.last().text)
  assertEquals(7,vm.state.value.subscription.optInt("days_remaining"))
 }
 @Test fun delayedCalculatorCannotOverwriteAnotherPageResult() {
  ready();state(fixture(Screen.CALCULATORS))
  val gate=java.util.concurrent.CountDownLatch(1)
  stub("""{"ok":true,"data":{"type":"court_fee","first_stage_fee_toman":1}}""",gate=gate)
  compose.runOnIdle{vm.compute(mapOf("claim_amount_toman" to "1000000","claim_category" to "financial"));vm.navigate(Screen.LAWS)}
  gate.countDown();compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals(Screen.LAWS,vm.state.value.screen);assertNull(vm.state.value.result)
 }

}
