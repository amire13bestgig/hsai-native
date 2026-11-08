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
 @org.junit.After fun releaseNativeRenderCaches(){System.gc();System.runFinalization()}
 @get:Rule val compose=createAndroidComposeRule<MainActivity>()
 private val vm get()=ViewModelProvider(compose.activity)[AppViewModel::class.java]
 private fun ready(login:Boolean=true){
  compose.waitUntil(15000){vm.state.value.initialized}
  // Every subsequent UI action uses a local in-process HTTP response, never private production data.
  val http=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("UI fixture")
    .body("{\"ok\":true,\"data\":{\"items\":[]}}".toResponseBody("application/json".toMediaType())).build()
  }.build()
  val field=ApiClient::class.java.getDeclaredField("http");field.isAccessible=true;field.set(vm.api,http)
  vm.api.token="test-only-not-a-real-session"
  if(login)compose.runOnIdle{vm.loginRequired(Screen.CHAT)}
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
  if(System.getenv("HSAI_SKIP_SCREENSHOTS")=="1")return
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
 @Test fun explicitLoginShowsFormAndHidesWorkspace() {
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
 @Test fun explicitEntryGateStillRequiresContinueWhenRequested() {
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
  assertEquals(WorkspaceLayout.RAIL,workspaceLayout(1000f))
  assertEquals(WorkspaceLayout.SIDEBAR,workspaceLayout(1024f))
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

 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun v53LoginHasDesktopShowcase() {
  ready();compose.onNodeWithText("میز کار اختصاصی حقوقی شما،").assertIsDisplayed();screenshot("v53-login-desktop")
 }
 @Test fun v53MobileLoginHidesDesktopShowcase() {
  ready();compose.onNodeWithText("میز کار اختصاصی حقوقی شما،").assertDoesNotExist()
 }
 @Test fun v53AuthMethodSelectorRespectsServerConfig() {
  ready();state(AppUiState(initialized=true,config=JSONObject().put("sms_otp_enabled",true)))
  compose.onNodeWithText("ورود پیامکی").performScrollTo().performClick()
  compose.onNodeWithContentDescription("شماره موبایل").assertExists();screenshot("v53-login-sms")
 }
 @Test fun v53GlossaryUsesWebsiteContentAndLocalSearch() {
  ready();state(fixture(Screen.LAWS))
  compose.onNodeWithText("واژه‌نامه").performScrollTo().performClick()
  compose.onNodeWithText("فرهنگ لغات خودمانی دادگاه و ابلاغیه ثنا").assertExists()
  compose.onNode(hasSetTextAction()).performTextInput("اظهارنامه")
  compose.onNodeWithText("اظهارنامه قضایی").assertExists();compose.onNodeWithText("خواهان و خوانده").assertDoesNotExist();screenshot("v53-glossary")
 }
 @Test fun v53SidebarConversationDoesNotInvalidateItsResponse() {
  ready();state(fixture(Screen.CHAT).copy(sidebarHistory=listOf(JSONObject().put("id",42).put("title","گفتگوی آزمایشی سایدبار"))))
  stub("""{"ok":true,"data":{"messages":[{"role":"assistant","content":"پاسخ انتخاب‌شده از سایدبار","id":77}]}}""")
  compose.onNodeWithContentDescription("منوی اصلی").performClick()
  compose.onNodeWithText("گفتگوی آزمایشی سایدبار").performScrollTo().performClick()
  compose.waitUntil(10000){vm.state.value.messages.any{it.text=="پاسخ انتخاب‌شده از سایدبار"}}
  compose.onNodeWithText("پاسخ انتخاب‌شده از سایدبار").assertExists()
 }
 @Test fun v53UserAndAssistantBubblesRemainNative() {
  ready();state(fixture(Screen.CHAT).copy(messages=listOf(ChatMessage("user","متن پیام کاربر برای بررسی ظاهر"),ChatMessage("assistant","**پاسخ مستند آزمایشی**\nاین متن فقط برای بررسی رابط است."))))
  screenshot("v53-chat-bubbles")
 }
 @Test @Config(qualifiers="w1024dp-h1366dp-mdpi") fun v53PopularPlanUsesFeaturedSurface() {
  ready();val fixture=fixture(Screen.PLANS);fixture.items.first().put("is_popular",true)
  state(fixture.copy(items=fixture.items.take(1)));screenshot("v53-popular-plan")
 }
 @Test fun v53PasswordStrengthMatchesWebsiteScoring() {
  assertEquals(0,passwordStrengthScore("abc"));assertEquals(1,passwordStrengthScore("abcdefgh"));assertEquals(4,passwordStrengthScore("Abcdefghijk1!"))
 }


 @Test fun auditLawBrowseDoesNotRequireSearchText() {
  ready();state(fixture(Screen.LAWS).copy(items=emptyList()))
  stub("""{"ok":true,"data":{"items":[{"id":9,"law_name":"قانون مرور آزمایشی","article_number":"۱","official_text":"متن مرور"}],"pagination":{"page":1,"limit":15,"total":1}}}""")
  compose.runOnIdle{vm.refresh()};compose.waitUntil(10000){vm.state.value.items.any{it.optLong("id")==9L}}
  assertEquals("",vm.state.value.lawQuery)
 }
 @Test fun auditCollectionOpensItsArticles() {
  ready();state(fixture(Screen.LAWS).copy(lawTab="collections"))
  stub("""{"ok":true,"data":{"items":[{"id":9,"law_slug":"sample-law","official_text":"متن مجموعه"}]}}""")
  compose.runOnIdle{vm.openLawCollection("sample-law")}
  compose.waitUntil(10000){!vm.state.value.loading};assertEquals("search",vm.state.value.lawTab);assertEquals("sample-law",vm.state.value.lawSlug)
  assertEquals(9L,vm.state.value.items.first().optLong("id"))
 }
 @Test fun auditGeneratedDocumentOpensActualContentNotList() {
  ready();state(fixture(Screen.CHAT).copy(messages=listOf(ChatMessage("assistant","پیش‌نویس آماده",document=JSONObject().put("title","سند همین گفتگو").put("content","متن اختصاصی همین سند")))))
  compose.onNodeWithText("مشاهده برگه قضایی آماده").performClick()
  compose.onNodeWithText("متن اختصاصی همین سند").assertExists()
  compose.onNodeWithText("دریافت Word").assertDoesNotExist()
  assertEquals(Screen.CHAT,vm.state.value.screen)
 }
 @Test fun auditDeletingConversationRemovesSidebarEntry() {
  ready();val item=JSONObject().put("id",42).put("title","گفتگوی قابل حذف")
  state(fixture(Screen.CHAT).copy(conversationId=42,sidebarHistory=listOf(item)))
  stub("""{"ok":true,"data":{"deleted":true}}""")
  compose.runOnIdle{vm.delete("consultations",42)};compose.waitUntil(10000){!vm.state.value.posting}
  assertTrue(vm.state.value.sidebarHistory.isEmpty());assertNull(vm.state.value.conversationId)
 }
 @Test fun auditSwitchingCalculatorAwayAndBackDiscardsOldResult() {
  ready();state(fixture(Screen.CALCULATORS));val gate=java.util.concurrent.CountDownLatch(1)
  stub("""{"ok":true,"data":{"first_stage_fee_toman":999}}""",gate=gate)
  compose.runOnIdle{vm.compute(mapOf("claim_amount_toman" to "1000000","claim_category" to "financial"));vm.calculator("mehrieh");vm.calculator("court_fee")}
  gate.countDown();compose.waitUntil(10000){!vm.state.value.posting};assertNull(vm.state.value.result)
 }
 @Test fun auditArticleDetailCanSeedQuestionWithoutSendingIt() {
  ready();state(fixture(Screen.LAWS))
  compose.runOnIdle{vm.askAboutArticle(JSONObject().put("article_number","۱۲").put("law_name","قانون نمونه"))}
  assertEquals(Screen.CHAT,vm.state.value.screen);assertTrue(vm.state.value.draft.contains("۱۲ قانون نمونه"));assertTrue(vm.state.value.messages.isEmpty())
 }
 @Test fun auditStopStreamUnlocksComposerAndDoesNotEnableBlindRetry() {
  ready();state(fixture(Screen.CHAT).copy(streaming=true));val gate=java.util.concurrent.CountDownLatch(1)
  stub("event: done\ndata: {\"answer\":\"نباید دریافت شود\"}\n\n",stream=true,gate=gate)
  compose.runOnIdle{vm.setDraft("پرسش ساختگی");vm.sendChat()}
  compose.waitUntil(10000){vm.state.value.stage.isNotBlank()}
  compose.runOnIdle{vm.stopChat()};gate.countDown();compose.waitUntil(10000){!vm.state.value.posting}
  assertNull(vm.state.value.failedChat);assertEquals("",vm.state.value.stage)
 }

 @Test fun auditLatestAssistantQuestionsAreNativeAndSelectable() {
  ready();val question=JSONObject().put("question","آیا قرارداد کتبی دارید؟").put("options",org.json.JSONArray().put("بله دارم").put("خیر ندارم"))
  state(fixture(Screen.CHAT).copy(messages=listOf(ChatMessage("assistant","برای ادامه پاسخ بدهید",questions=listOf(question)))))
  compose.onNodeWithText("آیا قرارداد کتبی دارید؟").assertExists()
  compose.onNodeWithText("بله دارم").performScrollTo().performClick()
  compose.onNodeWithText("ارسال پاسخ پرسش‌ها").assertIsEnabled()
  assertEquals(1,vm.state.value.messages.size)
 }

 private fun recordRequests(body:String):java.util.concurrent.CopyOnWriteArrayList<Pair<String,JSONObject>> {
  val requests=java.util.concurrent.CopyOnWriteArrayList<Pair<String,JSONObject>>()
  val http=okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
   val r=chain.request();val buffer=okio.Buffer();r.body?.writeTo(buffer)
   requests.add((r.method+" "+r.url.encodedPath) to runCatching{JSONObject(buffer.readUtf8())}.getOrDefault(JSONObject()))
   okhttp3.Response.Builder().request(r).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("local audit fixture")
    .body(body.toResponseBody("application/json".toMediaType())).build()
  }.build()
  val f=ApiClient::class.java.getDeclaredField("http");f.isAccessible=true;f.set(vm.api,http);return requests
 }
 @Test fun auditTicketCreateAndReplyUseOfficialBodies() {
  ready();state(fixture(Screen.TICKETS));val requests=recordRequests("""{"ok":true,"data":{"items":[]}}""")
  compose.runOnIdle{vm.createTicket("موضوع آزمایشی","پیام آزمایشی")};compose.waitUntil(10000){!vm.state.value.posting}
  val create=requests.first{it.first=="POST /api/v1/tickets"}.second
  assertEquals("موضوع آزمایشی",create.getString("subject"));assertEquals("پیام آزمایشی",create.getString("message"))
  compose.runOnIdle{vm.replyTicket(42,"پاسخ ساختگی")};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals("پاسخ ساختگی",requests.first{it.first=="POST /api/v1/tickets/42/reply"}.second.getString("message"))
 }
 @Test fun auditEmptyTicketIsRejectedBeforeNetwork() {
  ready();state(fixture(Screen.TICKETS));val requests=recordRequests("""{"ok":true,"data":{}}""")
  compose.runOnIdle{vm.createTicket("","")};compose.waitUntil(10000){!vm.state.value.posting}
  assertTrue(requests.isEmpty());assertTrue(vm.state.value.error.isNotBlank())
 }
 @Test fun auditProfilePayloadAndReadbackAreConsistent() {
  ready();state(fixture(Screen.ACCOUNT));val requests=recordRequests("""{"ok":true,"data":{"user":{"full_name":"نام ساختگی جدید","email":"mock@example.com"},"subscription":{}}}""")
  compose.runOnIdle{vm.saveProfile("نام ساختگی جدید","mock@example.com","")}
  compose.waitUntil(10000){!vm.state.value.posting&&!vm.state.value.loading}
  val body=requests.first{it.first=="PATCH /api/v1/me"}.second
  assertEquals("mock@example.com",body.getString("email"));assertEquals("نام ساختگی جدید",vm.state.value.user.getString("full_name"))
 }
 @Test fun auditPasswordMismatchDoesNotTouchServer() {
  ready();state(fixture(Screen.ACCOUNT));val requests=recordRequests("""{"ok":true,"data":{}}""")
  compose.runOnIdle{vm.changePassword("mock-old","mock-new-password","different")};compose.waitUntil(10000){!vm.state.value.posting}
  assertTrue(requests.isEmpty());assertTrue(vm.state.value.error.isNotBlank())
 }
 @Test fun auditPasswordChangeChecksCurrentPasswordBeforePatchLocally() {
  ready();state(fixture(Screen.ACCOUNT));val requests=recordRequests("""{"ok":true,"data":{}}""")
  compose.runOnIdle{vm.changePassword("mock-old","mock-new-password","mock-new-password")};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals("POST /api/v1/auth/login",requests[0].first);assertEquals("PATCH /api/v1/me",requests[1].first)
  assertEquals("mock-new-password",requests[1].second.getString("new_password"))
 }
 @Test fun auditCheckoutRejectsInsecureGatewayWithoutPendingPayment() {
  ready();state(fixture(Screen.PLANS));recordRequests("""{"ok":true,"data":{"gateway_url":"http://insecure.example.test/pay","authority":"mock-authority"}}""")
  compose.runOnIdle{vm.checkout(1,"")};compose.waitUntil(10000){!vm.state.value.posting}
  assertFalse(vm.pendingPayment);assertTrue(vm.state.value.error.contains("امن"))
 }
 @Test fun auditPaymentLookupDisplaysServerStateNotAssumedSuccess() {
  ready();state(fixture(Screen.PLANS));val requests=recordRequests("""{"ok":true,"data":{"status":"pending"}}""")
  compose.runOnIdle{vm.verifyPayment("mock-authority")};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals("pending",vm.state.value.dialog!!.data.getString("status"));assertTrue(requests.any{it.first=="GET /api/v1/billing/payments/mock-authority"})
 }

 @Test fun guestSeesChatBeforeAnyLoginPrompt() {
  ready(login=false)
  assertFalse(vm.state.value.authenticated);assertTrue(vm.state.value.entryComplete);assertEquals(Screen.CHAT,vm.state.value.screen)
  compose.onNodeWithContentDescription("ارسال پیام").assertExists()
  compose.onNodeWithText("ورود به میز کار حق‌سنج").assertDoesNotExist();screenshot("guest-chat-first")
 }
 @Test fun guestSendOpensLoginWithoutSendingOrLosingDraft() {
  ready(login=false);val requests=recordRequests("""{"ok":true,"data":{}}""")
  compose.onNode(hasSetTextAction()).performTextInput("پرسش مهمان برای ادامه پس از ورود")
  compose.onNodeWithContentDescription("ارسال پیام").performClick()
  compose.onNodeWithText("ورود به میز کار حق‌سنج").assertExists()
  assertEquals("پرسش مهمان برای ادامه پس از ورود",vm.state.value.draft);assertTrue(vm.state.value.messages.isEmpty());assertTrue(requests.isEmpty())
  screenshot("guest-send-login")
 }
 @Test fun guestCanReturnFromLoginAndKeepMessage() {
  ready(login=false);compose.runOnIdle{vm.setDraft("پیش‌نویس محفوظ");vm.sendChat()}
  compose.onNodeWithText("بازگشت به چت").performClick()
  compose.onNodeWithText("پیش‌نویس محفوظ").assertExists();assertTrue(vm.state.value.entryComplete);assertFalse(vm.state.value.authenticated)
 }
 @Test fun guestLoginReturnsToChatButDoesNotAutoSend() {
  ready(login=false);val requests=recordRequests("""{"ok":true,"data":{"token":"local-fixture-token","user":{"full_name":"کاربر ساختگی"},"items":[]}}""")
  compose.runOnIdle{vm.setDraft("پیام آماده ارسال");vm.sendChat();vm.authenticate(false,mapOf("identifier" to "mock@example.com","password" to "mock-only-password"))}
  compose.waitUntil(10000){!vm.state.value.posting&&vm.state.value.authenticated}
  assertTrue(vm.state.value.entryComplete);assertEquals(Screen.CHAT,vm.state.value.screen);assertEquals("پیام آماده ارسال",vm.state.value.draft)
  assertTrue(requests.none{it.first=="POST /api/v1/chat"});assertTrue(vm.state.value.messages.isEmpty())
 }
 @Test fun guestPrivateNavigationRequiresLoginButPublicToolsDoNot() {
  ready(login=false);recordRequests("""{"ok":true,"data":{"items":[]}}""")
  compose.runOnIdle{vm.navigate(Screen.CALCULATORS)};assertTrue(vm.state.value.entryComplete)
  compose.runOnIdle{vm.navigate(Screen.DOCUMENTS)};assertFalse(vm.state.value.entryComplete)
  compose.onNodeWithText("ورود به میز کار حق‌سنج").assertExists()
 }
 @Test fun guestBackFromLoginRestoresChatAndEmptySendDoesNotPrompt() {
  ready(login=false);compose.runOnIdle{vm.sendChat()};assertTrue(vm.state.value.entryComplete)
  compose.runOnIdle{vm.loginRequired(Screen.CHAT)}
  compose.runOnIdle{compose.activity.onBackPressedDispatcher.onBackPressed()}
  assertEquals(Screen.CHAT,vm.state.value.screen);assertTrue(vm.state.value.entryComplete)
 }

 @Test fun brandingGuestPromptExplainsThatDraftIsKept() {
  ready(login=false);compose.runOnIdle{vm.setDraft("پیام محفوظ");vm.sendChat()}
  compose.onNodeWithText("پیامت محفوظ است؛ برای دریافت پاسخ وارد شو.").assertExists()
 }
 @Test fun brandingSuccessfulGuestLoginShowsExplicitReadyMessageAction() {
  ready(login=false);recordRequests("""{"ok":true,"data":{"token":"local-brand-fixture","user":{"full_name":"ساختگی"},"items":[]}}""")
  compose.runOnIdle{vm.setDraft("متن آماده");vm.sendChat();vm.authenticate(false,mapOf("identifier" to "mock@example.com","password" to "mock-password"))}
  compose.waitUntil(10000){!vm.state.value.posting&&vm.state.value.authenticated}
  compose.onNodeWithText("ارسال پیام آماده‌شده").assertExists();assertTrue(vm.state.value.draftReady);assertTrue(vm.state.value.messages.isEmpty());screenshot("brand-draft-ready")
 }
 @Test fun brandingUnknownNetworkResultCannotBeRetriedBlindly() {
  ready();state(fixture(Screen.CHAT).copy(failedChat=JSONObject().put("message","پیام نامشخص"),chatStatus=ChatStatus.UNKNOWN))
  val requests=recordRequests("""{"ok":true,"data":{"answer":"ساختگی"}}""")
  compose.runOnIdle{vm.sendChat(true)}
  assertEquals("retryChat",vm.state.value.dialog!!.kind);assertTrue(requests.isEmpty())
  compose.onNodeWithText("ارسال مجدد با تأیید من").assertIsNotEnabled()
 }
 @Test fun brandingReopenedHistoryPreservesReturnedRichData() {
  ready();state(fixture(Screen.HISTORY))
  stub("""{"ok":true,"data":{"messages":[{"id":77,"role":"assistant","content":"پاسخ ساختگی","verification":{"printable_document":{"title":"سند محفوظ","content":"متن سند محفوظ"},"quick_questions":[{"question":"پرسش محفوظ","options":["الف","ب"]}]}}]}}""")
  compose.runOnIdle{vm.openConversation(42)};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals("متن سند محفوظ",vm.state.value.messages.last().document!!.getString("content"));assertEquals(1,vm.state.value.messages.last().questions.size)
 }
 @Test fun brandingPersianDateUsesPersianCalendarWithoutChangingSourceDate() {
  ready();assertTrue(Validation.digits(dateFa("2026-03-21")).contains("1405"))
 }
 @Test @Config(qualifiers="w1280dp-h800dp-land-mdpi") fun brandingLandscapeChatHasFlatCardsAndUserLogo() {
  ready(login=false);compose.onAllNodesWithContentDescription("نشان حق‌سنج").onFirst().assertExists();screenshot("brand-chat-landscape")
 }

 @Test fun cleanupDocumentKeepsSecondaryActionsInAccessibleMenu() {
  ready();val item=JSONObject().put("id",42).put("title","سند آزمایشی منو").put("doc_type","اظهارنامه")
  state(fixture(Screen.DOCUMENTS).copy(items=listOf(item)))
  compose.onNodeWithText("ویرایش",substring=false).assertDoesNotExist()
  compose.onNodeWithContentDescription("گزینه‌های سند سند آزمایشی منو").performScrollTo().performClick()
  compose.onNodeWithText("ویرایش",substring=false).assertExists();compose.onNodeWithText("دریافت Word").assertExists()
  compose.onNodeWithText("حذف سند",substring=false).performClick()
  assertEquals("delete",vm.state.value.dialog!!.kind);assertEquals(42L,vm.state.value.dialog!!.data.optLong("id"))
 }
 @Test fun cleanupUnknownResultFieldsAreOptionalButValuesRemainAccessible() {
  ready();state(fixture(Screen.CALCULATORS).copy(result=JSONObject().put("total_toman",0).put("future_extra_field","اطلاعات محفوظ")))
  compose.onNodeWithText("اطلاعات محفوظ").assertDoesNotExist()
  compose.onNodeWithText("جزئیات تکمیلی (۱)").performScrollTo().performClick()
  compose.onNodeWithText("اطلاعات محفوظ").assertExists()

 }

 @Test fun cleanupAllTopicsRemainAvailableBehindDisclosure() {
  ready(login=false)
  compose.onNodeWithText("ارث و میراث خانوادگی").assertDoesNotExist()
  compose.onNodeWithText("موضوع‌های بیشتر").performScrollTo().performClick()
  compose.onNodeWithText("ارث و میراث خانوادگی").assertExists()
 }
 @Test fun cleanupAccountDoesNotExposeTechnicalApiOrAdminPromotion() {
  ready();state(fixture(Screen.ACCOUNT).copy(user=JSONObject().put("full_name","نمونه").put("role","admin")))
  compose.onNodeWithText("اطلاعات حساب فقط از API رسمی دریافت می‌شود.").assertDoesNotExist()
  compose.onNodeWithText("پنل رسمی مدیریت در مرورگر").assertDoesNotExist()
 }
 @Test fun cleanupIsoDateKeepsLtrIsolationInRtlApp() {
  ready();val f=fixture(Screen.DEADLINES);state(f.copy(items=f.items.take(1)))
  compose.onNodeWithText("میلادی: \u20662026-11-25\u2069").assertExists()
 }

 @Test fun auditGoogleLoginIsAbsentInLoginAndRegistration(){
  ready();compose.onNodeWithText("ورود با حساب گوگل در وب‌سایت").assertDoesNotExist();compose.onNodeWithText("ورود گوگل در مرورگر انجام می‌شود و به اپ منتقل نمی‌شود.").assertDoesNotExist()
  compose.onNodeWithText("عضویت جدید",substring=false).performClick();compose.onNodeWithText("ورود با حساب گوگل در وب‌سایت").assertDoesNotExist()
 }
 @Test fun auditRichOnlyResponseNeverPretendsToStillBeLoading(){
  ready();val message=ChatMessage("assistant","",document=JSONObject().put("title","برگه بدون متن چت").put("content","متن پیش‌نویس"))
  state(fixture(Screen.CHAT).copy(messages=listOf(message)))
  compose.onNodeWithText("در حال دریافت پاسخ…").assertDoesNotExist();compose.onNodeWithText("مشاهده برگه قضایی آماده").assertExists()
 }
 @Test fun auditSuggestionOnlyPrefillsDraftAndDoesNotAutoSend(){
  ready();state(fixture(Screen.CHAT).copy(messages=listOf(ChatMessage("assistant","پاسخ ساختگی",suggestions=listOf("پیشنهاد ساختگی قابل ویرایش")))))
  compose.onNodeWithText("پیشنهاد ساختگی قابل ویرایش").performScrollTo().performClick()
  assertEquals("پیشنهاد ساختگی قابل ویرایش",vm.state.value.draft);assertEquals(1,vm.state.value.messages.size);assertFalse(vm.state.value.posting)
 }
 @Test fun auditDoneIsTerminalAndPreservesOuterStreamMetadata(){
  ready();state(fixture(Screen.CHAT).copy(draft="تست محلی",streaming=true))
  stub("event: delta\ndata: {\"chunk\":\"پاسخ نهایی\"}\n\nevent: done\ndata: {\"message_id\":91,\"conversation_id\":31,\"verification\":{\"printable_document\":{\"content\":\"سند محفوظ\"}},\"assistant_message\":{\"content\":\"پاسخ نهایی\"}}\n\nevent: error\ndata: {\"message\":\"رویداد دیررس نباید پاسخ نهایی را خراب کند\"}\n\n",stream=true)
  compose.runOnIdle{vm.sendChat()};compose.waitUntil(15000){!vm.state.value.posting}
  assertEquals(ChatStatus.COMPLETED,vm.state.value.chatStatus);assertNull(vm.state.value.failedChat);assertEquals("سند محفوظ",vm.state.value.messages.last().document!!.getString("content"))
 }
 @Test fun auditNewDialogDoesNotInheritOldPageError(){
  ready();state(fixture(Screen.DOCUMENTS).copy(error="خطای درخواست قبلی"))
  compose.runOnIdle{vm.openDialog("documentCreate","سند تازه")};assertTrue(vm.state.value.error.isBlank())
 }

 @Test fun auditWordExportAsksUserForDestination(){
  ready();state(fixture(Screen.DOCUMENTS));compose.runOnIdle{vm.download(42)};compose.waitForIdle()
  val started=org.robolectric.Shadows.shadowOf(compose.activity).nextStartedActivityForResult
  assertNotNull(started);assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT,started.intent.action)
  assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",started.intent.type)
 }

 @Test fun internalVaultClearsOrphanPaymentWhenSessionIsMissingOrCorrupt(){
  ready();val prefs=compose.activity.getSharedPreferences("native_vault",android.content.Context.MODE_PRIVATE);val vault=SecureSession(compose.activity)
  prefs.edit().remove("session").putString("payment","old-test-payment").commit();assertNull(vault.read());assertTrue(vault.authority.isBlank())
  prefs.edit().putString("session","broken").putString("payment","old-test-payment").commit();assertNull(vault.read());assertTrue(vault.authority.isBlank());assertFalse(prefs.contains("session"))
 }
 @Test fun internalDocumentFormShowsAndFocusesFirstInvalidField(){
  ready();state(fixture(Screen.DOCUMENTS).copy(dialog=AppDialog("documentCreate","سند تازه")))
  compose.onNodeWithText("تنظیم هوشمند برگه").performScrollTo().performClick();compose.waitForIdle()
  assertEquals(listOf("title","details"),vm.state.value.fieldErrors.keys.toList())
  compose.onNodeWithContentDescription("عنوان برگه").assertIsFocused()
  compose.onAllNodesWithText("شرح ماجرا را بنویسید.").onFirst().assertExists()
  compose.onNodeWithContentDescription("عنوان برگه").performTextInput("عنوان آزمایشی");compose.waitForIdle()
  compose.onNodeWithContentDescription("عنوان برگه").assertIsFocused();assertFalse(vm.state.value.fieldErrors.containsKey("title"))
  compose.onNodeWithText("تنظیم هوشمند برگه").performScrollTo().performClick();compose.waitForIdle();compose.onNodeWithContentDescription("شرح ماجرا و خواسته").assertIsFocused()
 }
 @Test @Config(qualifiers="w1280dp-h800dp-land-mdpi") fun internalTabletDocumentDetailsStayAlongsideList(){
  ready();val f=fixture(Screen.DOCUMENTS);state(f.copy(items=f.items.take(1)))
  stub("""{"ok":true,"data":{"id":1,"title":"سند پنلی","content":"متن محفوظ در پنل"}}""")
  compose.onNodeWithText("مشاهده",substring=false).performClick();compose.waitUntil(10000){!vm.state.value.posting}
  assertNull(vm.state.value.dialog);assertEquals("documentView",vm.state.value.pane!!.kind)
  compose.onNodeWithText("متن محفوظ در پنل").assertExists();compose.onNodeWithText("تنظیم برگه جدید").assertExists()
 }
 @Test fun internalDocumentEditSurvivesMovingBetweenPaneAndDialog(){
  ready();val item=JSONObject().put("id",12).put("title","عنوان قدیم").put("content","متن قدیم")
  state(fixture(Screen.DOCUMENTS).copy(pane=AppDialog("documentEdit","ویرایش",item),documentDrafts=mapOf(12L to DocumentDraft(12,"عنوان جدید","متن جدید"))))
  compose.runOnIdle{vm.adaptDetail(false,setOf("documentEdit"))}
  compose.onNodeWithText("متن جدید",substring=false).assertExists();assertEquals("متن جدید",vm.state.value.documentDrafts[12L]!!.content)
 }
 @Test @Config(qualifiers="w1280dp-h800dp-land-mdpi") fun internalHistoryPreviewDoesNotOverwriteActiveChatOrDraft(){
  ready();state(fixture(Screen.HISTORY).copy(messages=listOf(ChatMessage("assistant","گفتگوی فعال")),conversationId=99,draft="پیش‌نویس محفوظ"))
  stub("""{"ok":true,"data":{"conversation":{"title":"پیش‌نمایش"},"messages":[{"role":"assistant","content":"متن تاریخچه"}]}}""")
  compose.runOnIdle{vm.previewConversation(1)};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals(99L,vm.state.value.conversationId);assertEquals("پیش‌نویس محفوظ",vm.state.value.draft);assertEquals("گفتگوی فعال",vm.state.value.messages.single().text);assertEquals("historyPreview",vm.state.value.pane!!.kind)
 }
 @Test fun internalRestoredValidSessionStartsInChatWithoutLoginGate(){
  ready()
  val user=JSONObject().put("id",42).put("full_name","کاربر ساختگی")
  val store=object:SessionStore {
   var value=JSONObject().put("token","mock-session").put("user",user)
   override fun read()=value
   override fun save(value:JSONObject){this.value=value}
   override fun clear(){value=JSONObject();authority=""}
   override var authority="";override var dark=false;override var reminderMigrationDone=true;override var reminderNotice=false
  }
  val http=okhttp3.OkHttpClient.Builder().addInterceptor{chain->
   val data=if(chain.request().url.encodedPath.endsWith("/auth/refresh"))JSONObject().put("token","mock-refreshed").put("user",user)else JSONObject().put("items",org.json.JSONArray())
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("fixture").body(JSONObject().put("ok",true).put("data",data).toString().toResponseBody("application/json".toMediaType())).build()
  }.build()
  val model=compose.runOnIdle{AppViewModel(compose.activity.application,androidx.lifecycle.SavedStateHandle(mapOf("session_epoch" to 3)),ApiClient(http=http),store)}
  val holder=androidx.lifecycle.ViewModelStore();val provider=ViewModelProvider(holder,object:ViewModelProvider.Factory {@Suppress("UNCHECKED_CAST") override fun <T:androidx.lifecycle.ViewModel> create(modelClass:Class<T>):T=model as T});provider[AppViewModel::class.java]
  try{compose.waitUntil(10000){org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();model.state.value.initialized&&model.api.token=="mock-refreshed"};assertEquals(Screen.CHAT,model.state.value.screen);assertTrue(model.state.value.entryComplete);assertTrue(model.state.value.authenticated);assertEquals(3,model.sessionVersion)}finally{compose.runOnIdle{holder.clear()}}
 }
 @Test fun internalMigrationNoticeIsAcknowledgedPersistently(){
  ready();SecureSession(compose.activity).reminderMigrationDone=true;SecureSession(compose.activity).reminderNotice=true
  state(fixture(Screen.DEADLINES).copy(reminderNotice=true));compose.onNodeWithText("متوجه شدم").performClick();assertFalse(vm.state.value.reminderNotice);compose.waitUntil(10000){!SecureSession(compose.activity).reminderNotice}
 }

 @Test fun internalEmptySuccessfulResponseKeepsRetryPayloadForReview(){
  ready();state(fixture(Screen.CHAT).copy(messages=emptyList(),draft="پیام آزمایشی",streaming=false));stub("""{"ok":true,"data":{}}""")
  compose.runOnIdle{vm.sendChat()};compose.waitUntil(10000){!vm.state.value.posting}
  assertEquals(ChatStatus.UNKNOWN,vm.state.value.chatStatus);assertNotNull(vm.state.value.failedChat);assertEquals(1,vm.state.value.messages.size)
 }
 @Test fun internalDifferentDocumentDraftsDoNotOverwriteEachOther(){ready();compose.runOnIdle{vm.updateDocumentDraft(11,"یک","متن یک");vm.updateDocumentDraft(12,"دو","متن دو");vm.discardDocumentDraft(12)};assertEquals("متن یک",vm.state.value.documentDrafts[11L]!!.content);assertFalse(vm.state.value.documentDrafts.containsKey(12L))}

 @Test fun internalChangingCalculatorInputInvalidatesOldResult(){
  ready();state(fixture(Screen.CALCULATORS).copy(result=JSONObject().put("total_initial_cost_toman",0)))
  val field=Domain.calculators.first{it.id==vm.state.value.calculator}.fields.first{it.numeric&&it.options.isEmpty()}
  compose.onNodeWithContentDescription(field.label).performScrollTo().performTextReplacement("1000000");assertNull(vm.state.value.result)
 }
 @Test fun internalDeadlineDateErrorReceivesFocus(){
  ready();state(fixture(Screen.DEADLINES).copy(dialog=AppDialog("deadline","ثبت مهلت جدید")))
  compose.onNodeWithContentDescription("عنوان مهلت").performTextInput("مهلت آزمایشی")
  compose.onAllNodesWithText("ثبت مهلت",substring=false).onLast().performScrollTo().performClick();compose.waitForIdle()
  assertEquals(listOf("date"),vm.state.value.fieldErrors.keys.toList());compose.onNodeWithText("انتخاب تاریخ پایان مهلت").assertIsFocused()
 }
 @Test fun internalScannerInputAndConsentHaveSeparateAccessibleErrors(){
  ready();state(fixture(Screen.SCANNER));compose.onNodeWithText("تحلیل برگه",substring=false).performScrollTo().performClick();compose.waitForIdle()
  assertEquals(listOf("text","consent"),vm.state.value.fieldErrors.keys.toList());compose.onNodeWithContentDescription("یا متن برگه را وارد کنید").assertIsFocused()
  compose.onNodeWithContentDescription("یا متن برگه را وارد کنید").performTextInput("متن آزمایشی سند");compose.onNodeWithText("تحلیل برگه",substring=false).performScrollTo().performClick();compose.waitForIdle()
  assertEquals(listOf("consent"),vm.state.value.fieldErrors.keys.toList());compose.onNodeWithText("مشخصات حساس را بررسی کرده‌ام و با ارسال برگه برای تحلیل موافقم.").assertIsFocused()
 }

 @Test fun internalProfileRefreshDoesNotEraseUnsavedTyping(){
  ready();val f=fixture(Screen.ACCOUNT);state(f)
  compose.onNodeWithContentDescription("نام و نام خانوادگی رسمی").performScrollTo().performTextReplacement("ویرایش محفوظ")
  state(f.copy(user=JSONObject(f.user.toString()).put("full_name","متن سرور")))
  compose.onNodeWithContentDescription("نام و نام خانوادگی رسمی").assertTextContains("ویرایش محفوظ")
 }
 @Test fun internalLateCheckoutResponseCannotRestorePaymentAfterExpiry(){
  ready();state(fixture(Screen.PLANS));val entered=java.util.concurrent.CountDownLatch(1);val release=java.util.concurrent.CountDownLatch(1)
  val client=okhttp3.OkHttpClient.Builder().addInterceptor{chain->
   if(chain.request().url.encodedPath.endsWith("/billing/checkout")){entered.countDown();release.await(10,java.util.concurrent.TimeUnit.SECONDS)}
   okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("fixture").body("""{"ok":true,"data":{"authority":"mock-only-authority","gateway_url":"https://example.invalid/mock-only"}}""".toResponseBody("application/json".toMediaType())).build()
  }.build()
  ApiClient::class.java.getDeclaredField("http").apply{isAccessible=true}.set(vm.api,client)
  try {
   compose.runOnIdle{vm.checkout(1,"")};compose.waitUntil(10000){entered.count==0L}
   compose.runOnIdle{vm.downloadUnauthorized(vm.api.token,vm.sessionVersion)};compose.waitUntil(10000){!vm.state.value.authenticated}
  }finally{release.countDown()}
  compose.waitUntil(10000){!vm.state.value.posting};assertFalse(vm.pendingPayment);assertFalse(vm.state.value.authenticated)
 }

 @Test fun finalFourAddedCalculatorsRenderAndAgreementIsConditional(){
  ready();listOf("expert_fee","arbitration_fee","notary_tariff","office_services_fee").forEach{id->state(fixture(Screen.CALCULATORS).copy(calculator=id));compose.onAllNodesWithText(Domain.calculators.first{it.id==id}.title,substring=false).onFirst().assertExists();val first=Domain.calculators.first{it.id==id}.fields.first();if(first.multiple)compose.onNodeWithText(first.label).assertExists()else compose.onNodeWithContentDescription(first.label).assertExists()}
  state(fixture(Screen.CALCULATORS).copy(calculator="arbitration_fee"));compose.onNodeWithContentDescription("حق‌الزحمه توافقی داور (تومان)").assertDoesNotExist()
  compose.onNodeWithContentDescription("مبنای حق‌الزحمه").performScrollTo().performClick();compose.onNodeWithText("مبلغ توافقی طرفین").performClick();compose.onNodeWithContentDescription("حق‌الزحمه توافقی داور (تومان)").assertExists()
 }
 @Test fun finalOfficeAllowsMultipleSelectionsAndRejectsNoSelection(){
  ready();state(fixture(Screen.CALCULATORS).copy(calculator="office_services_fee"))
  compose.onNodeWithText("ثبت و ارسال اظهارنامه",substring=false).performScrollTo().performClick()
  compose.onNodeWithText("محاسبه و نمایش نتیجه").performScrollTo().performClick();compose.waitForIdle();assertTrue(vm.state.value.fieldErrors.containsKey("services"))
  compose.onNodeWithText("گواهی عدم سوءپیشینه",substring=false).performScrollTo().performClick();compose.onNodeWithText("ثبت دادخواست و شکواییه",substring=false).performClick();assertFalse(vm.state.value.fieldErrors.containsKey("services"))
 }
}
