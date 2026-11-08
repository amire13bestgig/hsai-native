@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

/** Tokens transcribed from website v53 tailwind.config.js and src/index.css. */
object SiteDesign {
 val Brass300=Color(0xFFDCBE86)
 val Brass100=Color(0xFFF5EEDC)
 val Navy900=Color(0xFF0D2130)
 val DarkCanvas=Color(0xFF0B1B28)
 val CardRadius=16.dp
 const val DesktopBreakpoint=1024f
}

@Composable fun BrandEmblem(modifier:Modifier=Modifier) {
 Image(painterResource(R.drawable.brand_mark),"نشان حق‌سنج",modifier.size(40.dp))
}

/** Translucent native surfaces; never blur child text to imitate CSS backdrop-filter. */
@Composable fun SiteGlass(modifier:Modifier=Modifier,strong:Boolean=false,onDark:Boolean=false,content:@Composable ()->Unit) {
 val dark=LocalSiteDark.current||onDark
 val top=when{onDark->Color.White.copy(alpha=.065f);dark&&strong->Color(0xFF172A3B).copy(alpha=.96f);dark->Color.White.copy(alpha=.075f);strong->Color.White.copy(alpha=.95f);else->Color.White.copy(alpha=.82f)}
 val bottom=when{onDark->top;dark&&strong->Color(0xFF0B1824).copy(alpha=.94f);dark->Color.White.copy(alpha=.035f);strong->Color.White.copy(alpha=.82f);else->Color.White.copy(alpha=.64f)}
 val line=if(dark)Color.White.copy(alpha=.12f)else Color(0xFFE6E2D9).copy(alpha=.9f)
 val shape=RoundedCornerShape(if(strong)22.dp else SiteDesign.CardRadius)
 Surface(modifier=modifier.fillMaxWidth(),color=if(dark)Color(0xFF102330)else Color.White.copy(alpha=.94f),shape=shape,tonalElevation=0.dp,shadowElevation=0.dp,border=BorderStroke(1.dp,line)) {
  Box(Modifier.background(Brush.verticalGradient(listOf(top,bottom)))) {content()}
 }
}

@Composable fun LegalGrid(modifier:Modifier=Modifier,aurora:Boolean=true) {
 Canvas(modifier.clipToBounds()) {
  if(aurora) {
   drawCircle(Brush.radialGradient(listOf(SiteGold.copy(alpha=.17f),Color.Transparent),Offset(size.width*.65f,size.height*.1f),size.width*.75f),size.width*.75f,Offset(size.width*.65f,size.height*.1f))
   drawCircle(Brush.radialGradient(listOf(Color(0xFF10B981).copy(alpha=.065f),Color.Transparent),Offset(0f,size.height*.95f),size.width*.8f),size.width*.8f,Offset(0f,size.height*.95f))
  }
  val step=38.dp.toPx();var x=0f;var y=0f
  while(x<size.width){drawLine(Color.White.copy(alpha=.04f),Offset(x,0f),Offset(x,size.height));x+=step}
  while(y<size.height){drawLine(Color.White.copy(alpha=.04f),Offset(0f,y),Offset(size.width,y));y+=step}
 }
}

@Composable fun SiteHero(title:String,description:String,badge:String,icon:ImageVector=NativeIcons.Balance,footer:(@Composable ()->Unit)?=null) {
 BoxWithConstraints(Modifier.fillMaxWidth().background(SiteNavy)) {
  val wide=maxWidth>=900.dp
  LegalGrid(Modifier.matchParentSize())
  Column(Modifier.padding(horizontal=20.dp,vertical=if(wide)24.dp else 20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   if(wide&&badge.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    Icon(icon,null,Modifier.size(16.dp),tint=SiteDesign.Brass300);Text(badge,color=SiteDesign.Brass300,style=MaterialTheme.typography.labelMedium)
   }
   Text(title,color=Color.White,style=if(wide)MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
   Text(description,color=Color(0xFFCBD5E1),style=MaterialTheme.typography.bodySmall)
   if(footer!=null)footer()
  }
 }
}

@Composable fun AuthShowcase() {
 Column(verticalArrangement=Arrangement.spacedBy(20.dp)) {
  Text("میز کار اختصاصی حقوقی شما،",color=Color.White,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Black)
  Text("همراه با بانک قوانین و آرای رسمی ایران",color=SiteDesign.Brass300,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  listOf(NativeIcons.MenuBook to "مطالعه قوانین و استنادهای پاسخ",NativeIcons.Description to "تنظیم پیش‌نویس سند و دریافت Word").forEach { (icon,text)->
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Icon(icon,null,tint=SiteDesign.Brass300,modifier=Modifier.size(22.dp));Text(text,color=Color(0xFFE2E8F0),style=MaterialTheme.typography.bodyMedium)
   }
  }
 }
}

/** The source website's decorative A4 paper treatment, without altering document content. */
@Composable fun DocumentPaper(text:String) {
 val ink=if(LocalSiteDark.current)Color(0xFFE8EEF5)else SiteNavy
 val paper=if(LocalSiteDark.current)Color(0xFF10263A)else Color(0xFFFFFEF9)
 Surface(color=paper,shape=RoundedCornerShape(4.dp),border=BorderStroke(1.dp,SiteGold.copy(alpha=.3f))) {
  androidx.compose.foundation.text.selection.SelectionContainer {
   Text(text,Modifier.fillMaxWidth().padding(22.dp),color=ink,style=MaterialTheme.typography.bodyMedium.copy(lineHeight=30.sp))
  }
 }
}

@Composable fun FeaturedPlan(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 HsaiTheme(true) {
  Surface(modifier.fillMaxWidth(),color=SiteNavy,shape=RoundedCornerShape(24.dp),border=BorderStroke(1.dp,SiteGold),shadowElevation=0.dp) {
   Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content)
  }
 }
}

@Composable fun LegalGlossary() {
 val context=androidx.compose.ui.platform.LocalContext.current
 val entries=remember { val array=org.json.JSONArray(context.resources.openRawResource(R.raw.legal_glossary_v53).bufferedReader().use{it.readText()});(0 until array.length()).map{array.getJSONObject(it)} }
 var query by androidx.compose.runtime.saveable.rememberSaveable{mutableStateOf("")}
 Column(verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Text("فرهنگ لغات خودمانی دادگاه و ابلاغیه ثنا",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Black)
  Text("توضیح‌های کوتاه برگرفته از وب‌سایت؛ استثناها و شرایط قانونی هر پرونده را جداگانه بررسی کنید.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
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
 if(item.str("badge").isNotBlank())Text(item.str("badge"),color=MaterialTheme.colorScheme.secondary,style=MaterialTheme.typography.labelLarge)
 item.objects("rows").forEach { row->
  HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
  Text(row.str("aspect"),fontWeight=FontWeight.Bold)
  AdaptiveSplit(first={
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
    Text("قبلاً چطور بود؟",color=if(LocalSiteDark.current)Color(0xFFFDA4AF)else Color(0xFF9F1239),fontWeight=FontWeight.Bold)
    Text(item.str("oldLawTitle"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(row.str("oldRule"),style=MaterialTheme.typography.bodyMedium)
   }
  },second={
   Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
    Text("الان طبق قانون جدید چطور است؟",color=if(LocalSiteDark.current)Color(0xFF6EE7B7)else Color(0xFF065F46),fontWeight=FontWeight.Bold)
    Text(item.str("newLawTitle"),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(row.str("newRule"),style=MaterialTheme.typography.bodyMedium)
   }
  })
 }
}

@Composable fun SiteTabs(options:List<Pair<String,String>>,selected:String,onSelect:(String)->Unit) {
 Surface(color=Color.White.copy(alpha=.08f),shape=RoundedCornerShape(16.dp),border=BorderStroke(1.dp,Color.White.copy(alpha=.15f))) {
  FlowRow(Modifier.padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
   options.forEach { (key,label)->
    TextButton(onClick={onSelect(key)},shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(selected==key)SiteDesign.Brass300 else Color.Transparent,contentColor=if(selected==key)SiteNavy else Color(0xFFE2E8F0))) {
     Text(label,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
    }
   }
  }
 }
}
