package ir.hsai.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import android.Manifest
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.*
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream
import java.io.File

class MainActivity:ComponentActivity() {
 private val vm:AppViewModel by viewModels()
 private val snacks=SnackbarHostState()
 private var cameraUri:Uri?=null
 private var cameraFile:File?=null
 private var cameraOwner:String?=null
 private var pickerOwner:String?=null
 private var calendarBytes:ByteArray?=null
 private var pendingDocument:AppEffect.Download?=null
 private var wordBusy=false
 private var awaitingPayment=false
 private var pendingReminder:AppEffect.Remind?=null
 private var editing by mutableStateOf(false)
 private val network by lazy { getSystemService(ConnectivityManager::class.java) }
 private val networkCallback=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(n:Network){vm.setOnline(true)};override fun onLost(n:Network){vm.setOnline(network.activeNetwork!=null)}}
 private val pickPhoto=registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->val owner=pickerOwner;pickerOwner=null;if(uri!=null&&owner!=null)readImage(uri,owner)else if(uri!=null)vm.notice("انتخاب تصویر بازیابی نشد؛ دوباره انتخاب کنید.")}
 private val takePhoto=registerForActivityResult(ActivityResultContracts.TakePicture()){ok->val uri=cameraUri;val owner=cameraOwner;if(ok&&uri!=null&&owner!=null)readImage(uri,owner)else {cameraFile?.delete();cameraFile=null;cameraUri=null;cameraOwner=null;if(ok)vm.notice("خروجی دوربین بازیابی نشد؛ دوباره عکس بگیرید.")}}
 private val saveCalendar=registerForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")){uri->val bytes=calendarBytes;calendarBytes=null;if(uri!=null&&bytes!=null)lifecycleScope.launch(Dispatchers.IO){runCatching{contentResolver.openOutputStream(uri)?.use{it.write(bytes)}}.onSuccess{vm.notice("فایل ذخیره شد؛ آن را در تقویم وارد کنید.")}.onFailure{vm.notice("ذخیره فایل انجام نشد.")}}}
 private val saveWord=registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document")){uri->
  val document=pendingDocument;pendingDocument=null
  if(uri!=null&&document!=null) {
   wordBusy=true
   lifecycleScope.launch {
    val requestToken=vm.api.token
    try {
     require(document.session==vm.sessionVersion&&vm.state.value.authenticated){"نشست تغییر کرده؛ دریافت سند را دوباره انتخاب کنید."}
     vm.notice("در حال دریافت سند…")
     val bytes=vm.api.documentBytes(document.id)
     require(document.session==vm.sessionVersion&&vm.state.value.authenticated){"نشست تغییر کرده؛ سند ذخیره نشد."}
     withContext(Dispatchers.IO){val output=contentResolver.openOutputStream(uri)?:error("محل ذخیره قابل نوشتن نیست.");output.use{it.write(bytes)}}
     vm.notice("سند در محل انتخاب‌شده ذخیره شد.")
    }catch(c:CancellationException){throw c}
    catch(e:Exception){if(e is ApiException&&e.status==401)vm.downloadUnauthorized(requestToken,document.session);vm.notice(e.message?:"ذخیره سند انجام نشد.")}
    finally{wordBusy=false}
   }
  } else if(uri!=null)vm.notice("درخواست دریافت بازیابی نشد؛ سند را دوباره انتخاب کنید.")
 }
 /**
  * تایپ صوتی: وب‌سایت از `webkitSpeechRecognition` استفاده می‌کند (toggleVoiceDictation).
  * در اندروید معادل بدون‌مجوزش `RecognizerIntent` است؛ موتور گفتارِ خودِ دستگاه کار را
  * می‌کند و نتیجه به پیش‌نویس اضافه می‌شود. اگر موتور نبود، پیام روشن داده می‌شود.
  */
 private val dictateSpeech=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
  if(result.resultCode==RESULT_OK) {
   val spoken=result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
   if(!spoken.isNullOrBlank()){val current=vm.state.value.draft.trim();vm.setDraft(if(current.isBlank())spoken else current+" "+spoken)}
   else vm.notice("گفتاری تشخیص داده نشد؛ دوباره تلاش کنید یا تایپ کنید.")
  } else vm.notice("تایپ صوتی لغو شد.")
 }
 private fun dictate(){
  val intent=Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE,"fa-IR")
   putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT,"ماجرا را بگویید")
  }
  runCatching{dictateSpeech.launch(intent)}.onFailure{vm.notice("تشخیص گفتار روی این دستگاه در دسترس نیست؛ می‌توانید تایپ کنید.")}
 }
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->val r=pendingReminder;pendingReminder=null;if(granted&&r!=null)schedule(r)else vm.notice("اجازه اعلان داده نشد؛ از فایل تقویم استفاده کنید.")}
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  savedInstanceState?.let { val id=it.getLong("pending_doc_id",0);val epoch=it.getInt("pending_doc_session",-1);if(id>0&&epoch>=0)pendingDocument=AppEffect.Download(id,epoch) }
  pickerOwner=savedInstanceState?.getString("picker_owner")
  val restored=PendingCamera.restore(File(cacheDir,"camera"),savedInstanceState?.getString("camera_name"))
  cameraOwner=savedInstanceState?.getString("camera_owner")
  if(restored!=null&&!cameraOwner.isNullOrBlank()){cameraFile=restored;cameraUri=FileProvider.getUriForFile(this,"$packageName.files",restored)}
  enableEdgeToEdge()
  network.registerDefaultNetworkCallback(networkCallback)
  setContent {
   // وضعیت پیش از استفاده در نوار وضعیت خوانده می‌شود (ترتیب اعلان مهم است).
   val state by vm.state.collectAsState()
   Box(Modifier.fillMaxSize()) {
    NativeApp(vm,::gallery,::camera,::dictate,{editing=true},snacks)
    Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars)
     // UI-parity: نوار وضعیت در میز کار به رنگ بوم سایت است؛ در صفحهٔ ورود (پس‌زمینهٔ سرمه‌ای) همان سرمه‌ای می‌ماند.
     .background(if(state.entryComplete)Soft.bg() else SiteNavy))
   }
   SideEffect { androidx.core.view.WindowCompat.getInsetsController(window,window.decorView).apply { isAppearanceLightStatusBars=state.entryComplete&&!state.dark;isAppearanceLightNavigationBars=state.entryComplete&&!state.dark } }
   if(editing&&state.entryComplete&&state.image!=null)HsaiTheme(state.dark){RedactionDialog(state.image!!,{editing=false}){data->vm.setImage(data);editing=false;vm.notice("نسخه پوشانده‌شده انتخاب شد؛ پیش از ارسال بازبینی کنید.")}}
  }
  lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED){vm.effects.collect{effect->when(effect){
   is AppEffect.Toast->launch{snacks.showSnackbar(effect.text)}
   is AppEffect.Copy->{(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("حق‌سنج",effect.text));if(Build.VERSION.SDK_INT<33)vm.notice("متن کپی شد.")}
   is AppEffect.OpenUrl->{val uri=Uri.parse(effect.url);if(uri.scheme=="https"&&uri.host!=null){awaitingPayment=vm.pendingPayment&&(uri.host!="hsai.ir"||uri.path.orEmpty().contains("payment"));runCatching{startActivity(Intent(Intent.ACTION_VIEW,uri))}.onFailure{vm.notice("مرورگر برای باز کردن لینک در دسترس نیست.")}}}
   is AppEffect.Download->download(effect)
   is AppEffect.Calendar->{calendarBytes=calendarText(effect.title,effect.date).toByteArray(Charsets.UTF_8);saveCalendar.launch("hsai-deadline.ics")}
   is AppEffect.Remind->{if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){pendingReminder=effect;notifications.launch(Manifest.permission.POST_NOTIFICATIONS)}else schedule(effect)}
  }}} }
  handleIntent(intent)
 }
 private fun currentImageOwner():String? {
  val state=vm.state.value
  return if(!state.initialized)null else if(!state.authenticated)"guest:${vm.sessionVersion}"else state.user.optLong("id").takeIf{it>0}?.let{"user:$it:${vm.sessionVersion}"}
 }
 private fun gallery(){
  pickerOwner=currentImageOwner();if(pickerOwner==null){vm.notice("برای آماده‌شدن حساب صبر کنید.");return}
  runCatching{pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))}.onFailure{pickerOwner=null;vm.notice("انتخاب تصویر در دسترس نیست.")}
 }
 private fun camera(){
  cameraOwner=currentImageOwner();if(cameraOwner==null){vm.notice("برای آماده‌شدن حساب صبر کنید.");return}
  runCatching {
   val folder=File(cacheDir,"camera").apply{mkdirs()}
   folder.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>3600000}?.forEach{it.delete()}
   cameraFile=File.createTempFile("legal-",".jpg",folder);cameraUri=FileProvider.getUriForFile(this,"$packageName.files",cameraFile!!);takePhoto.launch(cameraUri!!)
  }.onFailure{cameraFile?.delete();cameraUri=null;cameraFile=null;cameraOwner=null;vm.notice("دوربین در دسترس نیست؛ از گالری استفاده کنید.")}
 }

 private fun readImage(uri:Uri,owner:String) { val photo=if(uri==cameraUri)cameraFile else null;lifecycleScope.launch {
  try { vm.state.filter{it.initialized}.first();require(currentImageOwner()==owner){"نشست تغییر کرده؛ تصویر را دوباره انتخاب کنید."};val data=withContext(Dispatchers.IO){
   val bytes=contentResolver.openInputStream(uri)?.use{input->val out=ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0;while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=20*1024*1024){"حجم تصویر بیش از حد بزرگ است."};out.write(buffer,0,n)};out.toByteArray()}?:error("تصویر خوانده نشد.")
   require(bytes.size<=20*1024*1024){"حجم تصویر باید کمتر از ۲۰ مگابایت باشد."}
   val options=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
   require(options.outWidth>0&&options.outHeight>0){"فایل تصویر معتبر نیست."}
   var sample=1;while(maxOf(options.outWidth,options.outHeight)/sample>2400)sample*=2
   val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply{inSampleSize=sample})?:error("تصویر قابل خواندن نیست.")
   val orientation=runCatching{androidx.exifinterface.media.ExifInterface(java.io.ByteArrayInputStream(bytes)).getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,1)}.getOrDefault(1)
   val matrix=android.graphics.Matrix().apply{when(orientation){2->setScale(-1f,1f);3->setRotate(180f);4->setScale(1f,-1f);5->{setRotate(90f);postScale(-1f,1f)};6->setRotate(90f);7->{setRotate(90f);postScale(1f,-1f)};8->setRotate(270f)}}
   val oriented=if(orientation in 2..8)Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)else bitmap
   val out=ByteArrayOutputStream();oriented.compress(Bitmap.CompressFormat.JPEG,92,out);if(oriented!==bitmap)oriented.recycle();bitmap.recycle();"data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP)
  };if(owner==currentImageOwner()&&vm.state.value.entryComplete){vm.setImage(data);vm.notice("تصویر آماده است؛ مشخصات حساس را پیش از ارسال بپوشانید.")}else vm.notice("نشست یا مسیر تغییر کرده؛ تصویر را دوباره انتخاب کنید.")
  }catch(c:CancellationException){throw c}catch(e:Exception){vm.notice(e.message?:"خواندن تصویر ناموفق بود.")}finally{photo?.delete();if(uri==cameraUri){cameraUri=null;cameraFile=null;cameraOwner=null}}
 } }
 private fun download(d:AppEffect.Download){
  if(pendingDocument!=null||wordBusy){vm.notice("یک دریافت سند در حال انجام است.");return}
  pendingDocument=d
  runCatching{saveWord.launch("hsai-${d.id}.docx")}.onFailure{pendingDocument=null;vm.notice("انتخاب محل ذخیره در دسترس نیست.")}
 }
 override fun onSaveInstanceState(outState:Bundle) {
  outState.putString("camera_name",cameraFile?.name);outState.putString("camera_owner",cameraOwner);outState.putString("picker_owner",pickerOwner)
  pendingDocument?.let{outState.putLong("pending_doc_id",it.id);outState.putInt("pending_doc_session",it.session)}
  super.onSaveInstanceState(outState)
 }

 private fun schedule(r:AppEffect.Remind){runCatching{DeadlineReminder.schedule(this,r.id,r.title,r.date,r.owner)}.onSuccess{vm.notice("یادآور یک روز قبل ثبت شد؛ ممکن است به علت محدودیت باتری با تأخیر اجرا شود.")}.onFailure{vm.notice(it.message?:"ثبت یادآور ناموفق بود.")}}
 private fun handleIntent(i:Intent?){
  if(i?.action=="ir.hsai.mobile.OPEN_DEADLINES")lifecycleScope.launch {
   vm.state.filter{it.initialized}.first()
   if(vm.state.value.authenticated&&vm.state.value.entryComplete)vm.navigate(Screen.DEADLINES)else vm.loginRequired(Screen.DEADLINES)
  }
  if(i?.data?.scheme=="hsai"&&i.data?.host=="payment")lifecycleScope.launch{vm.state.filter{it.initialized}.first();if(vm.state.value.authenticated&&vm.state.value.entryComplete)vm.verifyPayment()else vm.loginRequired(Screen.PLANS)}}
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent)}
 override fun onResume(){super.onResume();if(awaitingPayment){awaitingPayment=false;if(vm.state.value.entryComplete)vm.verifyPayment()else vm.loginRequired(Screen.PLANS)}}
 override fun onDestroy(){runCatching{network.unregisterNetworkCallback(networkCallback)};Speaker.shutdown();super.onDestroy()}
}
