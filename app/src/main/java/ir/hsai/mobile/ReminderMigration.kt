package ir.hsai.mobile

import android.app.NotificationManager
import android.content.Context
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

object ReminderMigration {
 const val TAG="hsai-deadline-v2"
 const val MARKER="hsai_reminder_v2"
 fun legacy(tags:Set<String>,finished:Boolean)=!finished&&TAG !in tags
 suspend fun run(context:Context):Boolean=withContext(Dispatchers.IO) {
  val vault=SecureSession(context)
  if(!vault.reminderMigrationDone) {
   val work=WorkManager.getInstance(context)
   val old=work.getWorkInfosByTag("hsai-deadline").get(10,TimeUnit.SECONDS).filter{legacy(it.tags,it.state.isFinished)}
   // No legal dates are inferred or rescheduled. Only old local jobs are cancelled.
   if(old.isNotEmpty())vault.reminderNotice=true
   old.forEach{work.cancelWorkById(it.id).result.get(10,TimeUnit.SECONDS)}
   synchronized(ReminderPrivacy) {
    val manager=context.getSystemService(NotificationManager::class.java)
    val notices=manager.activeNotifications.filter{it.notification.channelId=="legal_deadlines"&&!it.notification.extras.getBoolean(MARKER,false)}
    if(notices.isNotEmpty())vault.reminderNotice=true
    notices.forEach{manager.cancel(it.tag,it.id)}
   }
   vault.reminderMigrationDone=true
  }
  vault.reminderNotice
 }
}
