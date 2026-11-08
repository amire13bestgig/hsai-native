package ir.hsai.mobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.*
import java.util.concurrent.TimeUnit

fun calendarText(title:String,date:String):String {
 val d=LocalDate.parse(date)
 val clean=title.replace("\\","\\\\").replace("\n"," ").replace("\r"," ").replace(",","\\,").replace(";","\\;")
 return listOf("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//HSAI Native//Deadline//FA","BEGIN:VEVENT","UID:${java.util.UUID.randomUUID()}@hsai.ir","DTSTAMP:"+java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now()),"DTSTART;VALUE=DATE:"+d.toString().replace("-",""),"DTEND;VALUE=DATE:"+d.plusDays(1).toString().replace("-",""),"SUMMARY:$clean","BEGIN:VALARM","TRIGGER:-P1D","ACTION:DISPLAY","DESCRIPTION:یادآوری مهلت ثبت‌شده توسط کاربر","END:VALARM","END:VEVENT","END:VCALENDAR","").flatMap(::foldCalendarLine).joinToString("\r\n")
}
object DeadlineReminder {
 fun schedule(context:Context,id:Long,title:String,date:String,owner:String){
  val current=SecureSession(context).read()?.optJSONObject("user")?.str("id")
  require(ReminderPrivacy.sameOwner(owner,current)){"برای یادآور، نشست ذخیره‌شده لازم است؛ دوباره وارد شوید یا فایل تقویم بگیرید."}
  val time=LocalDate.parse(date).minusDays(1).atTime(9,0).atZone(ZoneId.systemDefault()).toInstant()
  val delay=Duration.between(Instant.now(),time).toMillis()
  require(delay>0){"زمان یادآور یک روز قبل گذشته است؛ تاریخ را بررسی کنید."}
  val work=OneTimeWorkRequestBuilder<DeadlineWorker>().setInitialDelay(delay,TimeUnit.MILLISECONDS)
   .addTag("hsai-deadline").addTag(ReminderMigration.TAG).setInputData(workDataOf("id" to id,"title" to title,"date" to date,"owner" to owner)).build()
  WorkManager.getInstance(context).enqueueUniqueWork("deadline-$id",ExistingWorkPolicy.REPLACE,work)
 }
 fun cancel(context:Context,id:Long){WorkManager.getInstance(context).cancelUniqueWork("deadline-$id")}
}
class DeadlineWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result {
  val context=applicationContext
  val manager=context.getSystemService(NotificationManager::class.java)
  if(!manager.areNotificationsEnabled())return Result.success()
  val channel="legal_deadlines";manager.createNotificationChannel(NotificationChannel(channel,"مهلت‌های حقوقی",NotificationManager.IMPORTANCE_DEFAULT))
  val id=inputData.getLong("id",0).toInt()
  val intent=PendingIntent.getActivity(context,id,Intent(context,MainActivity::class.java).setAction("ir.hsai.mobile.OPEN_DEADLINES"),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val publicNotice=NotificationCompat.Builder(context,channel).setSmallIcon(R.drawable.ic_notification).setContentTitle("یادآور حق‌سنج").setContentText("برای مشاهده وارد برنامه شوید.").build()
  val notification=NotificationCompat.Builder(context,channel).addExtras(android.os.Bundle().apply{putBoolean(ReminderMigration.MARKER,true)}).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setPublicVersion(publicNotice).setSmallIcon(R.drawable.ic_notification).setContentTitle("یادآوری مهلت حق‌سنج").setContentText(inputData.getString("title")).setStyle(NotificationCompat.BigTextStyle().bigText("${inputData.getString("title")}\nتاریخ ثبت‌شده: ${inputData.getString("date")}\nبرای اقدام قانونی تاریخ را با مرجع مربوط تطبیق دهید.")).setContentIntent(intent).setAutoCancel(true).build()
  return try{synchronized(ReminderPrivacy){
   val current=SecureSession(context).read()?.optJSONObject("user")?.str("id")
   if(ReminderPrivacy.sameOwner(inputData.getString("owner"),current))manager.notify(id,notification)
   Result.success()
  }}catch(_:SecurityException){Result.success()}
 }
}

/** RFC 5545 folding counts UTF-8 octets, not Persian characters. */
fun foldCalendarLine(value:String):List<String> {
 if(value.isEmpty())return listOf("")
 val lines=mutableListOf<String>();var line=StringBuilder();var bytes=0
 value.codePoints().toArray().forEach { point->
  val text=String(Character.toChars(point));val length=text.toByteArray(Charsets.UTF_8).size
  if(bytes+length>75){lines.add(line.toString());line=StringBuilder(" ");bytes=1}
  line.append(text);bytes+=length
 }
 lines.add(line.toString());return lines
}

/** Missing owner also rejects reminders made by old app versions (fail closed). */
object ReminderPrivacy {
 fun sameOwner(scheduled:String?,current:String?):Boolean = !scheduled.isNullOrBlank()&&!current.isNullOrBlank()&&scheduled==current
}
