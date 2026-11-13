@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package ir.hsai.mobile

/**
 * بازبینی «امضای API» لایهٔ بصری.
 *
 * چرا این فایل؟ بازطراحی «UI مثل سایت» فقط رنگ و عدد نیست؛ چند API کامپوز را با پارامترهای
 * نام‌دار صدا می‌زند (border روی Button و Surface، پارامترهای OutlinedTextFieldDefaults.colors،
 * ساخت colorScheme/typography/shapes). این فایل همان فراخوانی‌ها را جداگانه بازتولید می‌کند تا
 * بدون نیاز به Android SDK کامل، صحت امضاها با نسخهٔ واقعی کتابخانه‌ها (Compose 1.7.6 /
 * Material3 1.3.1) قابل بررسی باشد. اجرا: bash typecheck.sh
 */
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun hsaiShapesLike()=Shapes(extraSmall=RoundedCornerShape(12.dp),small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(24.dp),extraLarge=RoundedCornerShape(32.dp))

fun hsaiTypographyLike()=Typography(
 displayLarge=TextStyle(fontWeight=FontWeight.Black,fontSize=32.sp,lineHeight=41.sp,letterSpacing=(-0.2).sp),
 headlineSmall=TextStyle(fontSize=20.sp),titleMedium=TextStyle(fontSize=16.sp),bodyMedium=TextStyle(fontSize=14.sp,lineHeight=26.sp),
 labelLarge=TextStyle(fontWeight=FontWeight.ExtraBold,fontSize=13.sp),labelSmall=TextStyle(fontSize=11.sp))

fun hsaiLightLike()=lightColorScheme(
 primary=Color.Black,onPrimary=Color.White,primaryContainer=Color.White,onPrimaryContainer=Color.Black,
 secondary=Color.Gray,onSecondary=Color.White,secondaryContainer=Color.White,onSecondaryContainer=Color.Black,
 tertiary=Color.Blue,onTertiary=Color.White,background=Color.White,onBackground=Color.Black,
 surface=Color.White,onSurface=Color.Black,surfaceVariant=Color.White,onSurfaceVariant=Color.Gray,
 outline=Color.Gray,outlineVariant=Color.Gray,error=Color.Red,onError=Color.White,
 errorContainer=Color.Red,onErrorContainer=Color.Red,inverseSurface=Color.Black,inverseOnSurface=Color.White)

fun hsaiDarkLike()=darkColorScheme(
 primary=Color.Black,onPrimary=Color.White,primaryContainer=Color.White,onPrimaryContainer=Color.Black,
 secondary=Color.Gray,onSecondary=Color.White,secondaryContainer=Color.White,onSecondaryContainer=Color.Black,
 tertiary=Color.Blue,onTertiary=Color.White,background=Color.Black,onBackground=Color.White,
 surface=Color.Black,onSurface=Color.White,surfaceVariant=Color.Black,onSurfaceVariant=Color.Gray,
 outline=Color.Gray,outlineVariant=Color.Gray,error=Color.Red,onError=Color.Black,
 errorContainer=Color.Red,onErrorContainer=Color.Red,inverseSurface=Color.White,inverseOnSurface=Color.Black)

@Composable fun softCardLike() {
 Surface(Modifier.fillMaxWidth(),shape=hsaiShapesLike().large,color=Color.White,border=BorderStroke(1.dp,Color.Gray),shadowElevation=3.dp) {
  Box {
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(0f to Color.White,0.42f to Color.Transparent)))
   Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color.Gray))))
   Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color.Transparent,Color.Gray,Color.Gray,Color.Transparent))))
   Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {Text("x")}
  }
 }
}

@Composable fun bubbleLike(user:Boolean) {
 if(user) Surface(Modifier.fillMaxWidth(.9f),shape=RoundedCornerShape(topStart=22.dp,topEnd=22.dp,bottomStart=9.dp,bottomEnd=22.dp),contentColor=Color.White,border=BorderStroke(1.dp,Color.Black),shadowElevation=6.dp) {
  Box(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF1B3852),Color(0xFF0C1F2E))))) {Column(Modifier.padding(16.dp)) {Text("u")}}
 }
}

@Composable fun clickableSurfaceLike() {
 Surface(onClick={},enabled=true,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp),color=Color.White,border=BorderStroke(1.dp,Color.Gray)) {
  Row(Modifier.padding(horizontal=14.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {Text("y")}
 }
}

@Composable fun primaryButtonLike() {
 Button(onClick={},enabled=true,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=RoundedCornerShape(12.dp),
  border=BorderStroke(1.dp,Color.White),
  colors=ButtonDefaults.buttonColors(containerColor=Color.Black,contentColor=Color.White,disabledContainerColor=Color.Gray,disabledContentColor=Color.DarkGray),
  contentPadding=PaddingValues(horizontal=18.dp,vertical=14.dp)) {
  CircularProgressIndicator(Modifier.size(17.dp),color=LocalContentColor.current,strokeWidth=2.dp)
  Text("b",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.ExtraBold)
 }
}

@Composable fun softInputLike() {
 OutlinedTextField(value="",onValueChange={},modifier=Modifier.fillMaxWidth(),isError=false,singleLine=true,
  shape=RoundedCornerShape(12.dp),
  keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),
  textStyle=MaterialTheme.typography.bodyMedium,
  colors=OutlinedTextFieldDefaults.colors(
   focusedContainerColor=Color.White,unfocusedContainerColor=Color.White,disabledContainerColor=Color.LightGray,errorContainerColor=Color.White,
   focusedBorderColor=Color.Black,unfocusedBorderColor=Color.Transparent,disabledBorderColor=Color.Transparent,errorBorderColor=Color.Red,
   cursorColor=Color.Black))
}

@Composable fun chipLike() {
 AssistChip(onClick={},label={Text("c")})
 SuggestionChip(onClick={},label={Text("s")})
}

@Composable fun heroLike() {
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val wide=maxWidth>=900.dp
  Column(Modifier.padding(20.dp)) {
   Surface(color=Color.Black,shape=RoundedCornerShape(50),border=BorderStroke(1.dp,Color.White)) {
    Row(Modifier.padding(horizontal=10.dp,vertical=5.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {Text("badge")}
   }
   Text(if(wide)"w" else "n")
  }
  Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(1.dp).background(Color.White))
 }
}

@Composable fun tabsLike() {
 FlowRow(Modifier.padding(5.dp),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
  listOf("a" to "A").forEach { (_,label)-> Surface(onClick={},shape=RoundedCornerShape(12.dp),color=Color.White,contentColor=Color.Black) {Text(label,Modifier.padding(horizontal=14.dp,vertical=8.dp))} }
 }
}

@Composable fun paperLike() {
 androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
  var y=34.dp.toPx()
  while(y<size.height) {drawLine(Color.Gray,androidx.compose.ui.geometry.Offset(0f,y),androidx.compose.ui.geometry.Offset(size.width,y),1.dp.toPx());y+=34.dp.toPx()}
 }
}
