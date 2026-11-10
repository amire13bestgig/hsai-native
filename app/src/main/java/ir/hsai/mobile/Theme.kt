package ir.hsai.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ============================================================================
   پالت رنگ — عیناً از tailwind.config.js سایت حق‌سنج
   navy 50..950 و brass 50..900؛ هیچ رنگ سرانگشتی یا تقریبی در این فایل نیست.
   ========================================================================= */
object Palette {
 val Navy50 =Color(0xFFF0F5F8); val Navy100=Color(0xFFD9E6EE); val Navy200=Color(0xFFB6CFDF)
 val Navy300=Color(0xFF85AFC8); val Navy400=Color(0xFF5088AB); val Navy500=Color(0xFF336C8F)
 val Navy600=Color(0xFF275676); val Navy700=Color(0xFF214661); val Navy800=Color(0xFF163144)
 val Navy900=Color(0xFF0D2130); val Navy950=Color(0xFF07141E)
 val Brass50=Color(0xFFFBF8F1); val Brass100=Color(0xFFF5EEDC); val Brass200=Color(0xFFEAD9B5)
 val Brass300=Color(0xFFDCBE86); val Brass400=Color(0xFFCE9F58); val Brass500=Color(0xFFC38839)
 val Brass600=Color(0xFFB36F2E); val Brass700=Color(0xFF955628); val Brass800=Color(0xFF794526)
 val Brass900=Color(0xFF633A22)
 val Amber50=Color(0xFFFFFBEB); val Amber100=Color(0xFFFEF3C7)
 val Slate300=Color(0xFFCBD5E1); val Slate400=Color(0xFF94A3B8); val Slate500=Color(0xFF64748B)
 val Slate50=Color(0xFFF8FAFC); val Slate100=Color(0xFFF1F5F9); val Slate200=Color(0xFFE2E8F0)
 val Slate700=Color(0xFF334155); val Slate800=Color(0xFF1E293B); val Slate900=Color(0xFF0F172A)
 val Ok=Color(0xFF15803D); val OkLo=Color(0xFF116336); val OkBright=Color(0xFF1A9C4D)
 val Danger=Color(0xFFB91C1C); val DangerLo=Color(0xFF991B1B); val DangerBright=Color(0xFFD92D2D)
 val DangerSoftLight=Color(0xFFFEE2E2); val DangerSoftDark=Color(0xFF3B1416)
 val OkSoftLight=Color(0xFFDCFCE7); val OkSoftDark=Color(0xFF14301F)
 val WarnSoftLight=Color(0xFFFEF3C7); val WarnSoftDark=Color(0xFF3A2E12)
 val InfoSoftLight=Color(0xFFE0F2FE); val InfoSoftDark=Color(0xFF12293A)
 val White=Color(0xFFFFFFFF); val Black=Color(0xFF000000)
}

/**
 * توکن‌های سطح `--soft-*` از src/index.css سایت.
 * روشن: بوم #f4f2ec، کارت #fbf9f5، کارت۲ #f7f4ee، چاه #efece4
 * تیره: بوم #0b1b28، کارت #12293a، کارت۲ #0f2334، چاه #0a1825
 */
object Soft {
 val BgLight=Color(0xFFF4F2EC); val CardLight=Color(0xFFFBF9F5); val Card2Light=Color(0xFFF7F4EE)
 val WellLight=Color(0xFFEFECE4); val BorderLight=Color(0xFFE4E0D6); val HairlineLight=Color(0xFFEDEAE2)
 val BgDark=Color(0xFF0B1B28); val CardDark=Color(0xFF12293A); val Card2Dark=Color(0xFF0F2334)
 val WellDark=Color(0xFF0A1825); val BorderDark=Color(0x17FFFFFF); val HairlineDark=Color(0x1AFFFFFF)

 @Composable fun bg()=if(LocalSiteDark.current)BgDark else BgLight
 @Composable fun card()=if(LocalSiteDark.current)CardDark else CardLight
 @Composable fun card2()=if(LocalSiteDark.current)Card2Dark else Card2Light
 @Composable fun well()=if(LocalSiteDark.current)WellDark else WellLight
 @Composable fun border()=if(LocalSiteDark.current)BorderDark else BorderLight
 @Composable fun hairline()=if(LocalSiteDark.current)HairlineDark else HairlineLight
 /** هایلایت بالای کارت (linear-gradient 168deg در CSS) */
 @Composable fun sheen()=if(LocalSiteDark.current)Color(0x0FFFFFFF) else Color(0x8CFFFFFF)
 @Composable fun ink()=if(LocalSiteDark.current)Palette.Slate50 else Palette.Slate900
 @Composable fun inkMuted()=if(LocalSiteDark.current)Palette.Slate400 else Palette.Slate500
}

/* نام‌های قبلی برای سازگاری همهٔ کامپوزبل‌های موجود */
val SiteNavy = Palette.Navy950
val SiteGold = Palette.Brass400
val Copper = Palette.Brass700

val Vazir=FontFamily(
 Font(R.font.vazirmatn_regular,FontWeight.Normal),Font(R.font.vazirmatn_medium,FontWeight.Medium),
 Font(R.font.vazirmatn_semibold,FontWeight.SemiBold),Font(R.font.vazirmatn_bold,FontWeight.Bold),
 Font(R.font.vazirmatn_black,FontWeight.Black))
val LocalSiteDark=staticCompositionLocalOf{false}

/** گردی‌ها: rounded-xl=12dp، rounded-2xl=16dp، rounded-3xl=24dp، soft-card=1.5rem، clay=1.6rem */
object SiteShape {
 val Xl = RoundedCornerShape(12.dp)
 val Xxl = RoundedCornerShape(16.dp)
 val Xxxl = RoundedCornerShape(24.dp)
 val Card = RoundedCornerShape(24.dp)
 val Clay = RoundedCornerShape(26.dp)
 val Chip = RoundedCornerShape(50)
 /** حباب پیام: 1.4rem و گوشهٔ 0.55rem در سمت شروع/پایان گفتگو */
 fun bubbleUser()=RoundedCornerShape(topStart=22.dp,topEnd=22.dp,bottomStart=9.dp,bottomEnd=22.dp)
 fun bubbleAi()=RoundedCornerShape(topStart=22.dp,topEnd=22.dp,bottomStart=22.dp,bottomEnd=9.dp)
}

/**
 * تایپوگرافی: اندازه‌های سیال سایت (clamp) به نزدیک‌ترین پلهٔ sp در موبایل نگاشت شده‌اند؛
 * نسبت خط‌ها همان 1.28/1.35/1.45/1.5/1.85 سایت است. وزن‌ها: 800 و 900 سایت (extrabold/black).
 */
private fun hsaiTypography()=Typography(
 displayLarge=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Black,fontSize=32.sp,lineHeight=41.sp,letterSpacing=(-0.2).sp),
 displayMedium=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Black,fontSize=28.sp,lineHeight=36.sp),
 displaySmall=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Black,fontSize=24.sp,lineHeight=32.sp),
 headlineLarge=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Black,fontSize=26.sp,lineHeight=35.sp),
 headlineMedium=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Black,fontSize=23.sp,lineHeight=31.sp),
 headlineSmall=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Bold,fontSize=20.sp,lineHeight=29.sp),
 titleLarge=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Bold,fontSize=18.sp,lineHeight=27.sp),
 titleMedium=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Bold,fontSize=16.sp,lineHeight=26.sp),
 titleSmall=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.SemiBold,fontSize=14.sp,lineHeight=23.sp),
 bodyLarge=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Normal,fontSize=15.sp,lineHeight=28.sp),
 bodyMedium=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Normal,fontSize=14.sp,lineHeight=26.sp),
 bodySmall=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Normal,fontSize=12.sp,lineHeight=22.sp),
 labelLarge=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.ExtraBold,fontSize=13.sp,lineHeight=20.sp),
 labelMedium=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=18.sp),
 labelSmall=TextStyle(fontFamily=Vazir,fontWeight=FontWeight.Bold,fontSize=11.sp,lineHeight=17.sp))

private fun hsaiShapes()=Shapes(
 extraSmall=SiteShape.Xl,small=SiteShape.Xl,medium=SiteShape.Xxl,large=SiteShape.Card,extraLarge=RoundedCornerShape(32.dp))

@Composable fun HsaiTheme(dark:Boolean,content:@Composable ()->Unit) {
 val scheme=if(dark) darkColorScheme(
  primary=Palette.Brass300,onPrimary=Palette.Navy950,
  primaryContainer=Palette.Brass900,onPrimaryContainer=Palette.Brass100,
  secondary=Palette.Brass400,onSecondary=Color(0xFF171004),
  secondaryContainer=Color(0xFF293838),onSecondaryContainer=Color(0xFFFDE68A),
  tertiary=Palette.Navy300,onTertiary=Palette.Navy950,
  background=Soft.BgDark,onBackground=Palette.Slate50,
  surface=Soft.CardDark,onSurface=Palette.Slate50,
  surfaceVariant=Soft.WellDark,onSurfaceVariant=Color(0xFFB6C4D2),
  outline=Color(0xFF2A3B4A),outlineVariant=Color(0xFF1B2F3E),
  error=Color(0xFFFCA5A5),onError=Color(0xFF3B1416),errorContainer=Palette.DangerSoftDark,onErrorContainer=Color(0xFFFCA5A5),
  inverseSurface=Palette.Slate100,inverseOnSurface=Palette.Navy950
 ) else lightColorScheme(
  primary=Palette.Navy950,onPrimary=Palette.Brass300,
  primaryContainer=Palette.Brass100,onPrimaryContainer=Palette.Brass900,
  secondary=Palette.Brass500,onSecondary=Color(0xFF2A1C07),
  secondaryContainer=Palette.Amber50,onSecondaryContainer=Palette.Brass900,
  tertiary=Palette.Navy600,onTertiary=Palette.White,
  background=Soft.BgLight,onBackground=Palette.Slate900,
  surface=Soft.CardLight,onSurface=Palette.Slate900,
  surfaceVariant=Soft.WellLight,onSurfaceVariant=Palette.Slate500,
  outline=Color(0xFFCFC8B8),outlineVariant=Soft.HairlineLight,
  error=Palette.Danger,onError=Palette.White,errorContainer=Palette.DangerSoftLight,onErrorContainer=Color(0xFF7F1D1D),
  inverseSurface=Palette.Navy950,inverseOnSurface=Palette.Slate50
 )
 MaterialTheme(colorScheme=scheme,typography=hsaiTypography(),shapes=hsaiShapes(),
  content={CompositionLocalProvider(LocalContentColor provides scheme.onBackground,LocalSiteDark provides dark){content()}})
}

/**
 * کارت خمیری سایت (`.soft-card`): سطح کارت + هایرلاین + هایلایت ۱۶۸درجه + گردی ۱.۵rem.
 * برای دکمه‌های برجسته (`.soft-raised`) از raised=true استفاده می‌شود.
 */
@Composable fun GlassCard(modifier:Modifier=Modifier,raised:Boolean=false,content:@Composable ColumnScope.()->Unit) {
 val card=Soft.card()
 Surface(modifier.fillMaxWidth(),shape=SiteShape.Card,color=card,border=BorderStroke(1.dp,Soft.border()),
  shadowElevation=if(LocalSiteDark.current)0.dp else if(raised)6.dp else 3.dp) {
  Box {
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Soft.sheen(),0.42f to Color.Transparent)))
   Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
  }
 }
}

/**
 * دکمهٔ ثانویهٔ سایت (`.soft-btn`): سطح کارت + هایرلاین + گردی ۱۲dp + وزن ۸۰۰.
 * جای OutlinedButton پیش‌فرض M3 (که «قرصی» است و با سایت نمی‌خواند) استفاده می‌شود.
 * onCard=true برای دکمه‌های داخل کارت است (سطح = چاه، مثل `.soft-well`).
 */
@Composable fun SoftButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,onCard:Boolean=false,content:@Composable RowScope.()->Unit) {
 val dark=LocalSiteDark.current
 Surface(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=44.dp),shape=SiteShape.Xl,
  color=if(onCard)Soft.well() else Soft.card(),
  contentColor=if(dark)Palette.Slate50 else Palette.Navy950,
  border=BorderStroke(1.dp,Soft.border()),
  shadowElevation=if(dark)0.dp else 2.dp) {
  CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
   Row(Modifier.padding(horizontal=16.dp,vertical=11.dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically,content=content)
  }
 }
}

/**
 * نوار «کلای» سایت (`.soft-clay-bar`): گرادیان عمودی + حاشیهٔ سفید نیمه‌شفاف.
 * روشن: rgba(255,255,255,.96) → rgba(247,244,238,.94) | تیره: #12293A → #0B1B28
 */
@Composable fun clayBarBrush(): Brush = if(LocalSiteDark.current)
 Brush.verticalGradient(listOf(Color(0xFA12293A),Color(0xF50B1B28)))
else Brush.verticalGradient(listOf(Color(0xF5FFFFFF),Color(0xF0F7F4EE)))

/** هایلایت بالای نوار کلای (inset 0 1.5px 0 rgba(255,255,255,1)). */
@Composable fun clayBarEdge(): Color = if(LocalSiteDark.current) Color(0x14FFFFFF) else Color(0xFFFFFFFF)

/**
 * دکمهٔ شبح سایت (`.ghost-btn`): بدون پس‌زمینه، متن برنجی/مایل، گردی ۱۰dp،
 * ارتفاع ۳۲dp — برای «کپی / مفید / ساده‌تر / خواندن» زیر هر پاسخ.
 */
@Composable fun GhostAction(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector?=null,onClick:()->Unit,enabled:Boolean=true) {
 val tint=if(LocalSiteDark.current)Palette.Brass300 else Palette.Brass700
 Surface(onClick=onClick,enabled=enabled,shape=SiteShape.Xl,color=Color.Transparent,
  contentColor=if(enabled)tint else tint.copy(alpha=.45f),border=null,tonalElevation=0.dp,shadowElevation=0.dp) {
  Row(Modifier.defaultMinSize(minHeight=32.dp).padding(horizontal=8.dp,vertical=4.dp),
   verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
   if(icon!=null)Icon(icon,null,Modifier.size(15.dp))
   Text(label,style=MaterialTheme.typography.labelSmall)
  }
 }
}

/** جداکنندهٔ گرادیانی سایت (`.soft-divider`). */
@Composable fun SoftDivider(modifier:Modifier=Modifier) {
 Box(modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent,Soft.border(),Soft.border(),Color.Transparent))))
}


