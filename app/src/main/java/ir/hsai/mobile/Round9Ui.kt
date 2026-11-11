@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

/* ============================================================================
   دور نهم — کامپوزبل‌های تازه‌ای که پیشنهادهای UI لازم داشتند.
   همه با توکن‌های موجود ساخته می‌شوند؛ هیچ رنگ یا شعاع تازه‌ای اینجا تعریف نمی‌شود.
   ========================================================================= */

/**
 * P2-1 — حذف با کشیدن.
 * کشیدن کامل، همان دیالوگ تأیید حذف را باز می‌کند (حذف فوری و بی‌تأیید انجام نمی‌شود)
 * و کارت به جای خود برمی‌گردد، چون confirmValueChange مقدار false برمی‌گرداند.
 */
@Composable fun SwipeToDelete(label:String,enabled:Boolean,onDelete:()->Unit,modifier:Modifier=Modifier,content:@Composable ()->Unit) {
 val state=rememberSwipeToDismissBoxState(confirmValueChange={ value->
  if(value!=SwipeToDismissBoxValue.Settled&&enabled)onDelete()
  false
 })
 SwipeToDismissBox(state=state,modifier=modifier,enableDismissFromStartToEnd=false,enableDismissFromEndToStart=enabled,
  backgroundContent={
   Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer,SiteShape.Card).padding(horizontal=18.dp),contentAlignment=Alignment.CenterEnd) {
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
     Icon(NativeIcons.Close,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.error)
     Text(label,style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.error)
    }
   }
  }) { content() }
}

/** P1-6 — بنر «نشست در شرف پایان» (پیش از خطای ۴۰۱، نه بعد از آن). */
@Composable fun SessionBanner(s:AppUiState,vm:AppViewModel) {
 val warning=SessionClock.warning(s.sessionLeft)
 if(warning==SessionWarning.NONE)return
 val expired=warning==SessionWarning.EXPIRED
 Surface(color=if(expired)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
  Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Text(SessionClock.warningText(s.sessionLeft),style=MaterialTheme.typography.bodySmall)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    TextButton(onClick={vm.navigate(Screen.ACCOUNT)}){Text(if(expired)"ورود دوباره" else "مدیریت حساب")}
    if(!expired)TextButton(onClick=vm::updateSessionClock){Text("بررسی دوباره")}
   }
  }
 }
}

/** P3-2 — بنر «دادهٔ ذخیره‌شده روی گوشی» وقتی شبکه در دسترس نیست. */
@Composable fun StaleBanner(s:AppUiState,vm:AppViewModel) {
 if(!s.stale)return
 Surface(color=MaterialTheme.colorScheme.secondaryContainer) {
  Column(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   Text("آخرین فهرست ذخیره‌شده روی گوشی نمایش داده می‌شود ("+s.staleAge+")؛ ممکن است تازه نباشد.",style=MaterialTheme.typography.bodySmall)
   TextButton(onClick={vm.refresh()}){Text("تلاش دوباره برای تازه‌سازی")}
  }
 }
}

/** P1-1 — دکمهٔ «پرش به آخرین پیام» در گفتگو. */
@Composable fun JumpToLatestFab(visible:Boolean,onClick:()->Unit) {
 if(!visible)return
 FloatingActionButton(onClick=onClick,shape=SiteShape.Chip,containerColor=Palette.Navy950,contentColor=Palette.Brass300,modifier=Modifier.semantics{contentDescription="پرش به آخرین پیام"}) {
  Icon(NativeIcons.ArrowDown,null,Modifier.size(20.dp))
 }
}

/** P3-4 — متن مادهٔ قانون در حالت مطالعه: بدون حاشیه‌های اضافه و با فاصلهٔ خط بازتر. */
@Composable fun ColumnScope.ArticleReadMode(on:Boolean,onToggle:()->Unit,officialText:String,explanation:String) {
 Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
  SoftButton(onClick=onToggle,onCard=true){Icon(NativeIcons.MenuBook,null,Modifier.size(16.dp));Text(if(on)"بازگشت به نمای معمولی" else "حالت مطالعه")}
 }
 if(on) {
  GlassCard(spacing=14) {
   Text("متن رسمی",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
   androidx.compose.foundation.text.selection.SelectionContainer {
    Text(officialText,style=MaterialTheme.typography.bodyLarge.copy(lineHeight=MaterialTheme.typography.bodyLarge.lineHeight*1.15f))
   }
   if(explanation.isNotBlank()) {
    Text("شرح ساده",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(explanation,style=MaterialTheme.typography.bodyMedium)
   }
  }
 }
}

/* ───────────────────────── P3-1: نمای تقویم ماهانهٔ مهلت‌ها ───────────────────────── */

private val JALALI_MONTHS = listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")

private fun persianMonth(offset:Int):Pair<Int,Int> {
 val cal=android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
 cal.add(android.icu.util.Calendar.MONTH,offset)
 return cal.get(android.icu.util.Calendar.YEAR) to (cal.get(android.icu.util.Calendar.MONTH)+1)
}

/**
 * تاریخ میلادیِ هر روزِ ماهِ شمسیِ نمایش‌داده‌شده (کلید تطبیق با `deadline_date` سرور).
 * محاسبهٔ تقویم با ICU انجام می‌شود، نه با فرمول دستی.
 */
private fun persianMonthDays(offset:Int):List<Pair<Int,String>> {
 val cal=android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian"))
 cal.add(android.icu.util.Calendar.MONTH,offset)
 cal.set(android.icu.util.Calendar.DAY_OF_MONTH,1)
 val days=cal.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH)
 val format=SimpleDateFormat("yyyy-MM-dd",Locale.US)
 return (1..days).map { day->
  cal.set(android.icu.util.Calendar.DAY_OF_MONTH,day)
  day to format.format(cal.time)
 }
}

@Composable fun DeadlineMonthView(s:AppUiState,vm:AppViewModel) {
 val offset=s.calendarOffset
 val (year,month)=remember(offset){persianMonth(offset)}
 val days=remember(offset){persianMonthDays(offset)}
 val byDate=remember(s.items){s.items.groupBy { it.str("deadline_date",it.str("due_date")).take(10) }}
 var selected by remember(offset){mutableStateOf<String?>(null)}
 val firstWeekday=remember(offset){android.icu.util.Calendar.getInstance(android.icu.util.ULocale("fa_IR@calendar=persian")).apply{add(android.icu.util.Calendar.MONTH,offset);set(android.icu.util.Calendar.DAY_OF_MONTH,1)}.get(android.icu.util.Calendar.DAY_OF_WEEK)%7}
 val cells=JalaliGrid.cells(firstWeekday,days.size)
 val isoByDay=days.toMap()

 GlassCard(spacing=10) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   SoftButton(onClick={vm.shiftCalendar(-1)},onCard=true){Text("ماه قبل")}
   val monthName=JALALI_MONTHS[(month-1).coerceIn(0,11)]
   Text(text="$monthName ${fa(year)}",modifier=Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,maxLines=1)
   SoftButton(onClick={vm.shiftCalendar(1)},onCard=true){Text("ماه بعد")}
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
   JalaliGrid.weekdayLabels.forEach { label->Text(label,Modifier.weight(1f),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=androidx.compose.ui.text.style.TextAlign.Center) }
  }
  JalaliGrid.weeks(cells).forEach { week->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
    week.forEach { day->
     val iso=if(day!=null)isoByDay[day].orEmpty() else ""
     val count=byDate[iso]?.size?:0
     val marked=count>0
     val isSelected=iso.isNotBlank()&&iso==selected
     Box(Modifier.weight(1f).height(44.dp).background(if(isSelected)MaterialTheme.colorScheme.secondaryContainer else if(marked)Soft.well() else Color.Transparent,SiteShape.Xl)
      .clickable(enabled=marked){selected=if(isSelected)null else iso},contentAlignment=Alignment.Center) {
      Column(horizontalAlignment=Alignment.CenterHorizontally) {
       Text(if(day==null)"" else fa(day),style=MaterialTheme.typography.labelMedium,fontWeight=if(marked)FontWeight.Bold else FontWeight.Normal,color=if(marked)Soft.ink() else MaterialTheme.colorScheme.onSurfaceVariant)
       if(marked)Text(fa(count),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
      }
     }
    }
    repeat(JalaliGrid.COLUMNS-week.size){Spacer(Modifier.weight(1f))}
   }
  }
  val dayItems=selected?.let{byDate[it]?:emptyList()}?:emptyList()
  if(selected!=null) {
   Text("مهلت‌های "+dateFa(selected!!),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
   if(dayItems.isEmpty())Text("در این روز مهلتی ثبت نشده است.",style=MaterialTheme.typography.bodySmall)
   dayItems.forEach { item->
    val daysLeft=if(item.has("days_left"))item.optInt("days_left") else null
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
     Icon(NativeIcons.CalendarClock,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.primary)
     Text(item.str("title",item.str("case_title","مهلت قانونی")),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
     if(DaysLeft.label(daysLeft).isNotBlank())Text(DaysLeft.label(daysLeft),style=MaterialTheme.typography.labelSmall,color=if(DaysLeft.urgent(daysLeft))MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
    }
   }
  } else Text("روزی که مهلت دارد با رنگ مشخص شده است؛ برای دیدن فهرست همان روز روی آن بزنید.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
 }
}

/* ───────────────────────── P2-4: صفحهٔ تنظیمات یک‌جا ───────────────────────── */

@Composable fun SettingsScreen(s:AppUiState,vm:AppViewModel) {
 Page {
  PageHeader("تنظیمات","ظاهر، اندازهٔ متن، خواندن پاسخ، داده‌های گوشی و پیوندهای حقوقی")
  GlassCard {
   Text("پوسته",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text(if(s.dark)"پوستهٔ تیره روشن است." else "پوستهٔ روشن روشن است.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   SoftButton(onClick=vm::toggleTheme,onCard=true){Icon(if(s.dark)NativeIcons.LightMode else NativeIcons.DarkMode,null,Modifier.size(16.dp));Text(if(s.dark)"تیره → روشن" else "روشن → تیره")}
  }
  GlassCard {
   Text("اندازهٔ متن",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("همان چیدمان سایت، با فونت بزرگ‌تر یا کوچک‌تر؛ پیش‌فرض «معمولی» است.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    listOf(0.9f to "کوچک",1f to "معمولی",1.15f to "بزرگ").forEach { (value,label)->
     val active=kotlin.math.abs(s.textScale-value)<0.01f
     SoftButton(onClick={vm.setTextScale(value)},onCard=!active){Text(if(active)"✓ "+label else label)}
    }
   }
  }
  GlassCard {
   Text("سرعت خواندن پاسخ",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("دکمهٔ «خواندن» زیر هر پاسخ، با موتور گفتار گوشی می‌خواند؛ اگر موتور فارسی نصب نباشد اپ صریح اعلام می‌کند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Slider(value=s.ttsRate,onValueChange=vm::setTtsRate,valueRange=0.7f..1.6f,steps=8)
   Text("سرعت: "+fa((s.ttsRate*100).toInt())+"٪ — در هر بار «خواندن» همین مقدار اعمال می‌شود.",style=MaterialTheme.typography.labelMedium)
  }
  GlassCard {
   Text("داده‌های گوشی",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   Text("آخرین فهرست‌های بارگذاری‌شده (گفتگوها، اوراق، مهلت‌ها، پشتیبانی، قوانین) برای حالت بی‌اتصال روی گوشی نگه داشته می‌شوند و هنگام بازگشت شبکه بازنویسی می‌شوند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   Text("متن پیام‌ها، تصاویر و پیش‌نویس‌ها در این کش ذخیره نمی‌شوند؛ پیش‌نویس گفتگو فقط تا پایان فرایند اپ می‌ماند.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   SoftButton(onClick=vm::clearLocalCache,onCard=true){Icon(NativeIcons.Close,null,Modifier.size(16.dp));Text("پاک‌کردن داده‌های ذخیره‌شده")}
  }
  GlassCard {
   Text("پیوندها و پشتیبانی",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    SoftButton(onClick={vm.openExternal("https://hsai.ir/privacy")},onCard=true){Text("حریم خصوصی")}
    SoftButton(onClick={vm.openExternal("https://hsai.ir/terms")},onCard=true){Text("شرایط استفاده")}
    SoftButton(onClick={vm.navigate(Screen.TICKETS)},onCard=true){Text("پشتیبانی")}
   }
   Text("هر بخشی که در اپ بومی نیست، در مرورگر امن گوشی باز می‌شود.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}
