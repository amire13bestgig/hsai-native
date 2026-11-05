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
 return listOf("BEGIN:VCALENDAR","VERSION:2.0","PRODID:-//HSAI Native//Deadline//FA","BEGIN:VEVENT","UID:${java.util.UUID.randomUUID()}@hsai.ir","DTSTAMP:"+java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(Instant.now()),"DTSTART;VALUE=DATE:"+d.toString().replace("-",""),"SUMMARY:$clean","BEGIN:VALARM","TRIGGER:-P1D","ACTION:DISPLAY","DESCRIPTION:یادآوری مهلت ثبت‌شده توسط کاربر","END:VALARM","END:VEVENT","END:VCALENDAR","").joinToString("\r\n")
}
object DeadlineReminder {
 fun schedule(context:Context,id:Long,title:String,date:String){
  val time=LocalDate.parse(date).minusDays(1).atTime(9,0).atZone(ZoneId.systemDefault()).toInstant()
  val delay=Duration.between(Instant.now(),time).toMillis()
  require(delay>0){"زمان یادآور یک روز قبل گذشته است؛ تاریخ را بررسی کنید."}
  val work=OneTimeWorkRequestBuilder<DeadlineWorker>().setInitialDelay(delay,TimeUnit.MILLISECONDS)
   .addTag("hsai-deadline").setInputData(workDataOf("id" to id,"title" to title,"date" to date)).build()
  WorkManager.getInstance(context).enqueueUniqueWork("deadline-$id",ExistingWorkPolicy.REPLACE,work)
 }
 fun cancel(context:Context,id:Long){WorkManager.getInstance(context).cancelUniqueWork("deadline-$id")}
}
class DeadlineWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
 override suspend fun doWork():Result {
  val context=applicationContext;val manager=context.getSystemService(NotificationManager::class.java)
  if(!manager.areNotificationsEnabled())return Result.success()
  val channel="legal_deadlines";manager.createNotificationChannel(NotificationChannel(channel,"مهلت‌های حقوقی",NotificationManager.IMPORTANCE_DEFAULT))
  val id=inputData.getLong("id",0).toInt()
  val intent=PendingIntent.getActivity(context,id,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val notification=NotificationCompat.Builder(context,channel).setSmallIcon(R.drawable.ic_launcher).setContentTitle("یادآوری مهلت حق‌سنج").setContentText(inputData.getString("title")).setStyle(NotificationCompat.BigTextStyle().bigText("${inputData.getString("title")}\nتاریخ ثبت‌شده: ${inputData.getString("date")}\nبرای اقدام قانونی تاریخ را با مرجع مربوط تطبیق دهید.")).setContentIntent(intent).setAutoCancel(true).build()
  return try{manager.notify(id,notification);Result.success()}catch(_:SecurityException){Result.success()}
 }
}
