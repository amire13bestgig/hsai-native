@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable fun RedactionDialog(source:String,onDismiss:()->Unit,onSave:(String)->Unit){
 val bitmap=remember(source){val b=Base64.decode(source.substringAfter(','),Base64.DEFAULT);BitmapFactory.decodeByteArray(b,0,b.size)}
 if(bitmap==null){onDismiss();return}
 val masks=remember{mutableStateListOf<Rect>()};var preview by remember{mutableStateOf<Rect?>(null)};var canvasSize by remember{mutableStateOf(IntSize(1,1))}
 Dialog(onDismissRequest=onDismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
 Surface(Modifier.widthIn(max=760.dp).fillMaxWidth().padding(14.dp).imePadding(),shape=RoundedCornerShape(22.dp),color=MaterialTheme.colorScheme.surface){Column(Modifier.heightIn(max=740.dp).verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
 Text("پوشاندن مشخصات حساس",style=MaterialTheme.typography.titleMedium)
 Text("روی کد ملی، شماره حساب یا اطلاعات حساس بکشید تا با رنگ مشکی پوشانده شود. فقط نسخه نهایی ارسال می‌شود.",style=MaterialTheme.typography.bodySmall)
 BoxWithConstraints(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){
  val ratio=bitmap.width.toFloat()/bitmap.height;val w=minOf(maxWidth,460.dp*ratio)
  Canvas(Modifier.width(w).aspectRatio(ratio).pointerInput(source){var start=Offset.Zero;detectDragGestures(onDragStart={start=it},onDragEnd={preview?.let{masks.add(it)};preview=null},onDragCancel={preview=null}){change,_->change.consume();val p=change.position;preview=Rect(min(start.x,p.x),min(start.y,p.y),max(start.x,p.x),max(start.y,p.y))}}){
   canvasSize=IntSize(size.width.toInt().coerceAtLeast(1),size.height.toInt().coerceAtLeast(1));drawImage(bitmap.asImageBitmap(),dstSize=canvasSize)
   (masks+listOfNotNull(preview)).forEach{r->drawRect(Color.Black,Offset(r.left,r.top),r.size)}
  }
 }
 FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
  TextButton(onClick={if(masks.isNotEmpty())masks.removeAt(masks.lastIndex)}){Text("برگشت")}
  TextButton(onClick={masks.clear()}){Text("پاک کردن پوشش‌ها")}
  Button(shape=SiteShape.Xl,onClick={
   val out=bitmap.copy(Bitmap.Config.ARGB_8888,true);val canvas=android.graphics.Canvas(out);val paint=android.graphics.Paint().apply{color=android.graphics.Color.BLACK;style=android.graphics.Paint.Style.FILL}
   val sx=bitmap.width.toFloat()/canvasSize.width;val sy=bitmap.height.toFloat()/canvasSize.height
   masks.forEach{r->canvas.drawRect(r.left*sx,r.top*sy,r.right*sx,r.bottom*sy,paint)}
   val bytes=ByteArrayOutputStream();out.compress(Bitmap.CompressFormat.JPEG,94,bytes);out.recycle();onSave("data:image/jpeg;base64,"+Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP))
  }){Text("استفاده از تصویر نهایی")}
 }
 TextButton(onClick=onDismiss){Text("انصراف")}
 }}
 }
}
