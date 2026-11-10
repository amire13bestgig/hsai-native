@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

/** توکن‌های بصری سایت (tailwind.config.js + src/index.css). */
object SiteDesign {
 val Brass300=Palette.Brass300
 val Brass400=Palette.Brass400
 val Navy900=Palette.Navy900
 val Navy950=Palette.Navy950
 /** rounded-2xl سایت (۱rem) — شعاع پیش‌فرض سطوح کوچک */
 val CardRadius=16.dp
 /** .soft-card = ۱.۵rem */
 val SoftCardRadius=24.dp
 /** چیدمان سایت در ۱۰۲۴px به دو ستون تبدیل می‌شود */
 const val DesktopBreakpoint=1024f
}

/**
 * نشان رسمی حق‌سنج — همان تصویر `public/brand/logo-mark.png` سایت.
 *
 * FIX(logo): نسخهٔ قبل `modifier.size(40.dp)` را همیشه اعمال می‌کرد و محتوای
 * غیرمربع (۶۲۰×۶۴۰) را در قاب مربع می‌فشرد؛ اندازهٔ درخواستی فراخواننده هم
 * بی‌اثر می‌شد. حالا اندازه پارامتر است، نسبت تصویر حفظ می‌شود (Fit) و
 * دارایی‌ها در `tools/make-brand-assets.py` از تک‌منبع سایت و مربع ساخته می‌شوند.
 *
 * contentDescription پیش‌فرض null است چون نشان در بیشتر جاها تزئینی است؛
 * گذاشتن «نشان حق‌سنج» روی هر پیام، TalkBack را پرحرف می‌کرد.
 */
@Composable fun BrandEmblem(size:Dp=40.dp,contentDescription:String?=null,modifier:Modifier=Modifier) {
 Image(painterResource(R.drawable.brand_mark),contentDescription,modifier.size(size),contentScale=ContentScale.Fit)
}

/**
 * «قفل‌نشانه»ٔ سایت (BrandMark.jsx): نشان + واژهٔ «حق‌سنج» + شعار.
 * سایز lg = نشان ۴۸dp و عنوان headline، sm = نشان ۳۲dp و عنوان labelLarge.
 */
@Composable fun BrandLockup(size:String="md",darkText:Boolean=false,subtitle:String="اول حقت را بسنج، بعد قدم بردار",modifier:Modifier=Modifier) {
 val emblem=when(size){"lg"->48.dp;"sm"->32.dp;else->40.dp}
 val title=when(size){"lg"->MaterialTheme.typography.headlineSmall;"sm"->MaterialTheme.typography.labelLarge;else->MaterialTheme.typography.titleMedium}
 Row(modifier,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
  BrandEmblem(emblem)
  Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
   Text("حق‌سنج",style=title,fontWeight=FontWeight.Black,color=if(darkText)Palette.Navy950 else Color.White)
   if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.labelSmall,color=if(darkText)Palette.Slate500 else Palette.Brass300.copy(alpha=.9f))
  }
 }
}

/**
 * لایهٔ شیشهٔ سایت (`.hsai-glass`, `.hsai-glass-strong`, `.hsai-glass-on-dark`).
 * مقدار شفافیت و رنگ‌ها عیناً از index.css گرفته شده‌اند؛ متن فرزند تار نمی‌شود
 * (backdrop-filter مرورگر در بومی معادل ندارد و شبیه‌سازی آن خوانایی را کم می‌کرد).
 */
@Composable fun SiteGlass(modifier:Modifier=Modifier,strong:Boolean=false,onDark:Boolean=false,content:@Composable ()->Unit) {
 val dark=LocalSiteDark.current
 val top=when{onDark->Color(0x0FFFFFFF);strong&&dark->Color(0xF5172A3B);strong->Color(0xF2FFFFFF);dark->Color(0x13FFFFFF);else->Color(0xD1FFFFFF)}
 val bottom=when{onDark->top;strong&&dark->Color(0xF00B1824);strong->Color(0xD1FFFFFF);dark->Color(0x09FFFFFF);else->Color(0xA3FFFFFF)}
 val line=when{onDark->Color(0x24FFFFFF);dark->Color(0x1FFFFFFF);strong->Color(0xE6FFFFFF);else->Color(0xBFFFFFFF)}
 val shape=RoundedCornerShape(if(strong)SiteDesign.SoftCardRadius else SiteDesign.CardRadius)
 Surface(modifier=modifier.fillMaxWidth(),color=Color.Transparent,shape=shape,tonalElevation=0.dp,
  shadowElevation=if(dark)0.dp else 4.dp,border=BorderStroke(1.dp,line)) {
  Box(Modifier.background(Brush.verticalGradient(listOf(top,bottom)))) {content()}
 }
}

/** شبکهٔ حقوقی معماری سایت (۳۸px) + هالهٔ طلایی. */
@Composable fun LegalGrid(modifier:Modifier=Modifier,aurora:Boolean=true) {
 Canvas(modifier.clipToBounds()) {
  if(aurora) {
   drawCircle(Brush.radialGradient(listOf(Palette.Brass400.copy(alpha=.20f),Color.Transparent),Offset(size.width*.72f,size.height*.08f),size.width*.8f),size.width*.8f,Offset(size.width*.72f,size.height*.08f))
   drawCircle(Brush.radialGradient(listOf(Palette.Navy400.copy(alpha=.10f),Color.Transparent),Offset(0f,size.height*.95f),size.width*.85f),size.width*.85f,Offset(0f,size.height*.95f))
  }
  val step=38.dp.toPx();var x=0f;var y=0f
  while(x<size.width){drawLine(Color.White.copy(alpha=.04f),Offset(x,0f),Offset(x,size.height));x+=step}
  while(y<size.height){drawLine(Color.White.copy(alpha=.04f),Offset(0f,y),Offset(size.width,y));y+=step}
 }
}

/**
 * سربرگ سایت: گرادیان از navy-950 به navy-900 + شبکهٔ حقوقی + تیتر با وزن Black.
 * (در سایت: `bg-gradient-to-l from-navy-950 to-navy-900` با حاشیهٔ brass-400/40)
 */
@Composable fun SiteHero(title:String,description:String,badge:String,icon:ImageVector=NativeIcons.Balance,footer:(@Composable ()->Unit)?=null) {
 BoxWithConstraints(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Palette.Navy900,Palette.Navy950)))) {
  val wide=maxWidth>=900.dp
  LegalGrid(Modifier.matchParentSize())
  Column(Modifier.padding(horizontal=20.dp,vertical=if(wide)26.dp else 20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
   if(badge.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)) {
    Surface(color=Palette.Brass400.copy(alpha=.16f),shape=SiteShape.Chip,border=BorderStroke(1.dp,Palette.Brass400.copy(alpha=.35f))) {
     Row(Modifier.padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
      Icon(icon,null,Modifier.size(14.dp),tint=Palette.Brass300)
      Text(badge,color=Palette.Brass300,style=MaterialTheme.typography.labelSmall)
     }
    }
   }
   Text(title,color=Color.White,style=if(wide)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
   Text(description,color=Palette.Slate300,style=MaterialTheme.typography.bodyMedium)
   if(footer!=null)footer()
  }
  Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(Palette.Brass400.copy(alpha=.40f)))
 }
}

@Composable fun AuthShowcase() {
 Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Text("میز کار اختصاصی حقوقی شما،",color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
  Text("همراه با بانک قوانین و آرای رسمی ایران",color=Palette.Brass300,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Black)
  listOf(NativeIcons.MenuBook to "مطالعه قوانین و استنادهای پاسخ",NativeIcons.Description to "تنظیم پیش‌نویس سند و دریافت Word").forEach { (icon,text)->
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Surface(color=Palette.Brass400.copy(alpha=.14f),shape=SiteShape.Xl){Icon(icon,null,tint=Palette.Brass300,modifier=Modifier.padding(9.dp).size(20.dp))}
    Text(text,color=Palette.Slate200,style=MaterialTheme.typography.bodyMedium)
   }
  }
 }
}

/** برگهٔ کاغذی قضایی سایت (`.judicial-paper`): #fffef9 با خط‌های برنجی هر ۲.۱rem. */
@Composable fun DocumentPaper(text:String) {
 val dark=LocalSiteDark.current
 val ink=if(dark)Color(0xFFE8EEF5)else Palette.Navy950
 val paper=if(dark)Soft.CardDark else Color(0xFFFFFEF9)
 val rule=if(dark)Color(0x14FFFFFF) else Palette.Brass400.copy(alpha=.16f)
 Surface(color=paper,shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Palette.Brass400.copy(alpha=.35f))) {
  Box {
   Canvas(Modifier.matchParentSize()) {
    var y=34.dp.toPx()
    while(y<size.height){drawLine(rule,Offset(0f,y),Offset(size.width,y),1.dp.toPx());y+=34.dp.toPx()}
   }
   androidx.compose.foundation.text.selection.SelectionContainer {
    Text(text,Modifier.fillMaxWidth().padding(horizontal=22.dp,vertical=20.dp),color=ink,style=MaterialTheme.typography.bodyMedium.copy(lineHeight=30.sp))
   }
  }
 }
}

/** کارت طرح ویژه: گرادیان navy سایت + حاشیهٔ برنجی (border-brass-400/40). */
@Composable fun FeaturedPlan(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 HsaiTheme(true) {
  Surface(modifier.fillMaxWidth(),color=Color.Transparent,shape=RoundedCornerShape(SiteDesign.SoftCardRadius),
   border=BorderStroke(1.dp,Palette.Brass400.copy(alpha=.45f)),shadowElevation=0.dp) {
   Box(Modifier.background(Brush.horizontalGradient(listOf(Palette.Navy900,Palette.Navy950)))) {
    LegalGrid(Modifier.matchParentSize(),aurora=false)
    Column(Modifier.padding(22.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
   }
  }
 }
}

@Composable fun LegalGlossary() {
 val context=androidx.compose.ui.platform.LocalContext.current
 val entries=remember { val array=org.json.JSONArray(context.resources.openRawResource(R.raw.legal_glossary_v53).bufferedReader().use{it.readText()});(0 until array.length()).map{array.getJSONObject(it)} }
 var query by androidx.compose.runtime.saveable.rememberSaveable{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(16.dp)) {
  Text("فرهنگ لغات خودمانی دادگاه و ابلاغیه ثنا",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
  Text("توضیح‌های کوتاه برگرفته از وب‌سایت؛ استثناها و شرایط قانونی هر پرونده را جداگانه بررسی کنید.",style=MaterialTheme.typography.bodySmall,color=Soft.inkMuted())
  Field("جستجو در معنی کلمات دادگاه",query,{query=it})
  val visible=entries.filter{query.isBlank()||it.str("term").contains(query)||it.str("simple").contains(query)}
  if(visible.isEmpty())EmptyState("واژه‌ای یافت نشد","عبارت کوتاه‌تری را امتحان کنید.")
  AdaptiveCards(visible) { item->
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Text(item.str("term"),Modifier.weight(1f),fontWeight=FontWeight.Black)
   }
   Text(item.str("simple"),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }
 }
}

@Composable fun LawComparison(item:org.json.JSONObject) {
 Text(item.str("title"),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
 if(item.str("badge").isNotBlank())Surface(color=Palette.Amber50,shape=SiteShape.Chip){Text(item.str("badge"),Modifier.padding(horizontal=9.dp,vertical=3.dp),color=Palette.Brass900,style=MaterialTheme.typography.labelSmall)}
 item.objects("rows").forEach { row->
  SoftDivider()
  Text(row.str("aspect"),fontWeight=FontWeight.Bold)
  AdaptiveSplit(first={
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
    Text("قبلاً چطور بود؟",color=if(LocalSiteDark.current)Color(0xFFFDA4AF)else Color(0xFF9F1239),fontWeight=FontWeight.ExtraBold)
    Text(item.str("oldLawTitle"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(row.str("oldRule"),style=MaterialTheme.typography.bodyMedium)
   }
  },second={
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
    Text("الان طبق قانون جدید چطور است؟",color=if(LocalSiteDark.current)Color(0xFF6EE7B7)else Color(0xFF065F46),fontWeight=FontWeight.ExtraBold)
    Text(item.str("newLawTitle"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(row.str("newRule"),style=MaterialTheme.typography.bodyMedium)
   }
  })
 }
}

/**
 * تب‌های سایت: ظرف شیشه‌ای `bg-white/10` روی سربرگ تیره، گزینهٔ فعال Brass300 با متن navy-950.
 */
@Composable fun SiteTabs(options:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) {
 Surface(color=Color(0x1AFFFFFF),shape=SiteShape.Xxl,border=BorderStroke(1.dp,Color(0x26FFFFFF))) {
  FlowRow(Modifier.padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   options.forEach { (key,label)->
    val on=selected==key
    Surface(onClick={onSelect(key)},shape=SiteShape.Xl,color=if(on)Palette.Brass300 else Color.Transparent,
     contentColor=if(on)Palette.Navy950 else Palette.Slate200) {
     Text(label,Modifier.padding(horizontal=14.dp,vertical=8.dp),style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.ExtraBold,
      color=if(on)Palette.Navy950 else Palette.Slate200)
    }
   }
  }
 }
}
