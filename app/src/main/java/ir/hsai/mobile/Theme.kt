package ir.hsai.mobile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val SiteNavy=Color(0xFF07141E)
val SiteGold=Color(0xFFCE9F58)
val Jade=SiteNavy // Kept as a source-compatible token for existing native components.
val Copper=Color(0xFF955628)
val Vazir=FontFamily(
 Font(R.font.vazirmatn_regular,FontWeight.Normal),Font(R.font.vazirmatn_medium,FontWeight.Medium),
 Font(R.font.vazirmatn_semibold,FontWeight.SemiBold),Font(R.font.vazirmatn_bold,FontWeight.Bold),Font(R.font.vazirmatn_black,FontWeight.Black))
val LocalSiteDark=staticCompositionLocalOf{false}
@Composable fun HsaiTheme(dark:Boolean,content:@Composable ()->Unit) {
 val scheme=if(dark) darkColorScheme(
  primary=SiteGold,onPrimary=SiteNavy,secondary=SiteGold,onSecondary=SiteNavy,
  secondaryContainer=Color(0xFF3C3222),onSecondaryContainer=Color(0xFFFFE1A8),
  background=Color(0xFF0B1B28),onBackground=Color(0xFFF2F5F8),surface=Color(0xFF0E2130),onSurface=Color(0xFFF2F5F8),
  surfaceVariant=Color(0xFF1A2C3A),onSurfaceVariant=Color(0xFFB6C4D2),outline=Color(0xFF647586),outlineVariant=Color(0xFF304353))
 else lightColorScheme(
  primary=SiteNavy,onPrimary=Color(0xFFDCBE86),secondary=Color(0xFF9C601F),onSecondary=Color.White,
  secondaryContainer=Color(0xFFFFF3D4),onSecondaryContainer=Color(0xFF754711),
  background=Color(0xFFF8F7F4),onBackground=SiteNavy,surface=Color.White,onSurface=SiteNavy,
  surfaceVariant=Color(0xFFF1F5F9),onSurfaceVariant=Color(0xFF475569),outline=Color(0xFF94A3B8),outlineVariant=Color(0xFFDDE4ED))
 val b=Typography()
 MaterialTheme(colorScheme=scheme,typography=Typography(
  displayLarge=b.displayLarge.copy(fontFamily=Vazir),displayMedium=b.displayMedium.copy(fontFamily=Vazir),displaySmall=b.displaySmall.copy(fontFamily=Vazir),
  headlineLarge=b.headlineLarge.copy(fontFamily=Vazir),headlineMedium=b.headlineMedium.copy(fontFamily=Vazir),headlineSmall=b.headlineSmall.copy(fontFamily=Vazir),
  titleLarge=b.titleLarge.copy(fontFamily=Vazir,fontSize=21.sp),titleMedium=b.titleMedium.copy(fontFamily=Vazir,fontSize=16.sp),titleSmall=b.titleSmall.copy(fontFamily=Vazir),
  bodyLarge=b.bodyLarge.copy(fontFamily=Vazir,fontSize=15.sp,lineHeight=27.sp),bodyMedium=b.bodyMedium.copy(fontFamily=Vazir,fontSize=14.sp,lineHeight=26.sp),bodySmall=b.bodySmall.copy(fontFamily=Vazir,fontSize=12.sp,lineHeight=22.sp),
  labelLarge=b.labelLarge.copy(fontFamily=Vazir),labelMedium=b.labelMedium.copy(fontFamily=Vazir),labelSmall=b.labelSmall.copy(fontFamily=Vazir)
 ),content={CompositionLocalProvider(LocalContentColor provides scheme.onBackground,LocalSiteDark provides dark){content()}})
}
@Composable fun AmbientBackground() {
 val colors=MaterialTheme.colorScheme
 Canvas(Modifier.fillMaxSize().background(colors.background)) {
  drawCircle(Brush.radialGradient(listOf(colors.primary.copy(alpha=.12f),Color.Transparent),center=Offset(size.width*.8f,size.height*.12f),radius=size.width*.9f),radius=size.width,center=Offset(size.width*.8f,size.height*.12f))
  drawCircle(Brush.radialGradient(listOf(Color(0xFFBE926E).copy(alpha=.12f),Color.Transparent),center=Offset(0f,size.height*.5f),radius=size.width*.8f),radius=size.width*.9f,center=Offset(0f,size.height*.5f))
 }
}
@Composable fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 SiteGlass(modifier) {Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)}
}
