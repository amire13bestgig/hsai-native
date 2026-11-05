package ir.hsai.mobile

import android.Manifest
import android.app.DownloadManager
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
import org.json.JSONObject

class MainActivity:ComponentActivity() {
 private val vm:AppViewModel by viewModels()
 private val snacks=SnackbarHostState()
 private var cameraUri:Uri?=null
 private var cameraFile:File?=null
 private var calendarBytes:ByteArray?=null
 private var awaitingPayment=false
 private var pendingReminder:AppEffect.Remind?=null
 private var editing by mutableStateOf(false)
 private val network by lazy { getSystemService(ConnectivityManager::class.java) }
 private val networkCallback=object:ConnectivityManager.NetworkCallback(){override fun onAvailable(n:Network){vm.setOnline(true)};override fun onLost(n:Network){vm.setOnline(network.activeNetwork!=null)}}
 private val pickPhoto=registerForActivityResult(ActivityResultContracts.PickVisualMedia()){uri->if(uri!=null)readImage(uri)}
 private val takePhoto=registerForActivityResult(ActivityResultContracts.TakePicture()){ok->if(ok)cameraUri?.let(::readImage)else cameraFile?.delete()}
 private val saveCalendar=registerForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")){uri->val bytes=calendarBytes;calendarBytes=null;if(uri!=null&&bytes!=null)lifecycleScope.launch(Dispatchers.IO){runCatching{contentResolver.openOutputStream(uri)?.use{it.write(bytes)}}.onSuccess{vm.notice("فایل ذخیره شد؛ آن را در تقویم وارد کنید.")}.onFailure{vm.notice("ذخیره فایل انجام نشد.")}}}
 private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()){granted->val r=pendingReminder;pendingReminder=null;if(granted&&r!=null)schedule(r)else vm.notice("اجازه اعلان داده نشد؛ از فایل تقویم استفاده کنید.")}
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState);enableEdgeToEdge()
  network.registerDefaultNetworkCallback(networkCallback)
  setContent {
   NativeApp(vm,{pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},::camera,{editing=true},snacks)
   val state by vm.state.collectAsState()
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
 private fun camera(){val folder=File(cacheDir,"camera").apply{mkdirs()};folder.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>3600000}?.forEach{it.delete()};cameraFile=File.createTempFile("legal-",".jpg",folder);cameraUri=FileProvider.getUriForFile(this,"$packageName.files",cameraFile!!);takePhoto.launch(cameraUri!!)}
 private fun readImage(uri:Uri) { val ownerToken=vm.api.token;lifecycleScope.launch {
  try { val data=withContext(Dispatchers.IO){
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
  };if(ownerToken==vm.api.token&&vm.state.value.entryComplete){vm.setImage(data);vm.notice("تصویر آماده است؛ مشخصات حساس را پیش از ارسال بپوشانید.")}
  }catch(e:Exception){vm.notice(e.message?:"خواندن تصویر ناموفق بود.")}finally{if(uri==cameraUri){cameraFile?.delete();cameraUri=null;cameraFile=null}}
 } }
 private fun download(d:AppEffect.Download){runCatching{val r=DownloadManager.Request(Uri.parse("https://hsai.ir/api/v1/documents/${d.id}/docx"));r.addRequestHeader("Authorization","Bearer ${d.token}");r.setTitle("برگه قضایی حق‌سنج");r.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,"hsai-${d.id}-${System.currentTimeMillis()}.docx");r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);getSystemService(DownloadManager::class.java).enqueue(r)}.onSuccess{vm.notice("درخواست دانلود ثبت شد؛ وضعیت را در دانلودهای گوشی ببینید.")}.onFailure{vm.notice("شروع دانلود ناموفق بود.")}}
 private fun schedule(r:AppEffect.Remind){runCatching{DeadlineReminder.schedule(this,r.id,r.title,r.date)}.onSuccess{vm.notice("یادآور یک روز قبل ثبت شد؛ ممکن است به علت محدودیت باتری با تأخیر اجرا شود.")}.onFailure{vm.notice(it.message?:"ثبت یادآور ناموفق بود.")}}
 private fun handleIntent(i:Intent?){if(i?.data?.scheme=="hsai"&&i.data?.host=="payment")lifecycleScope.launch{vm.state.filter{it.initialized}.first();if(vm.state.value.authenticated&&vm.state.value.entryComplete)vm.verifyPayment()else vm.loginRequired(Screen.PLANS)}}
 override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);handleIntent(intent)}
 override fun onResume(){super.onResume();if(awaitingPayment){awaitingPayment=false;if(vm.state.value.entryComplete)vm.verifyPayment()else vm.loginRequired(Screen.PLANS)}}
 override fun onDestroy(){runCatching{network.unregisterNetworkCallback(networkCallback)};super.onDestroy()}
}
