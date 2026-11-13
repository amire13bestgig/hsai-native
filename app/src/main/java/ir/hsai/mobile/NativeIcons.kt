package ir.hsai.mobile
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp

object NativeIcons {
private fun icon(name:String,data:String)=ImageVector.Builder(name,24.dp,24.dp,24f,24f).addPath(pathData=PathParser().parsePathString(data).toNodes(),fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.65f,strokeLineCap=StrokeCap.Round,strokeLineJoin=StrokeJoin.Round).build()
val ChatBubbleOutline=icon("ChatBubbleOutline","M21 11.5a8.4 8.4 0 0 1-.9 3.8A8.5 8.5 0 0 1 12.5 20a8.4 8.4 0 0 1-3.8-.9L3 21l1.9-5.7a8.4 8.4 0 0 1-.9-3.8A8.5 8.5 0 0 1 8.7 3.9a8.4 8.4 0 0 1 3.8-.9h.5a8.5 8.5 0 0 1 8 8v.5z")
val History=icon("History","M3 11a9 9 0 1 1 2.7 7M3 4v7h7M12 7v5l3 2")
val Description=icon("Description","M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zm0 0v6h6M8 13h8M8 17h6")
val DocumentScanner=icon("DocumentScanner","M4 8V5a1 1 0 0 1 1-1h3m8 0h3a1 1 0 0 1 1 1v3m0 8v3a1 1 0 0 1-1 1h-3m-8 0H5a1 1 0 0 1-1-1v-3M3 12h18M8 8h8M8 16h8")
val MenuBook=icon("MenuBook","M12 7v14M3 3h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5v16h-5a4 4 0 0 0-4 2 4 4 0 0 0-4-2H3V3z")
val Calculate=icon("Calculate","M6 3h12a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zm2 3h8v4H8V6zm0 8h1m3 0h1m3 0h.1M8 18h1m3 0h1m3 0h.1")
val EventAvailable=icon("EventAvailable","M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm3-2v5m8-5v5M3 10h18M8 14h2m4 0h2m-8 4h2")
val WorkspacePremium=icon("WorkspacePremium","M3 6l4 4 5-7 5 7 4-4-2 13H5L3 6zM5 22h14")
val SupportAgent=icon("SupportAgent","M4 13v-1a8 8 0 0 1 16 0v1M4 12H3v6h4v-6H4zm16 0h1v6h-4v-6h3zm0 6v2h-6")
val PersonOutline=icon("PersonOutline","M20 21v-2a6 6 0 0 0-6-6h-4a6 6 0 0 0-6 6v2M16 6a4 4 0 1 1-8 0 4 4 0 0 1 8 0z")
val Balance=icon("Balance","M12 3v18M7 21h10M4 7h16M6 7l-4 8h8L6 7zm12 0-4 8h8l-4-8z")
val Add=icon("Add","M12 5v14M5 12h14")
val MoreVert=icon("MoreVert","M12 5h0.01M12 12h0.01M12 19h0.01")
val Menu=icon("Menu","M4 6h16M4 12h16M4 18h16")
val CloudDone=icon("CloudDone","m5 12 4 4L19 6")
val CloudOff=icon("CloudOff","m6 6 12 12M6 18 18 6")
val LightMode=icon("LightMode","M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0z")
val DarkMode=icon("DarkMode","M21 12.7A9 9 0 0 1 11.3 3a9 9 0 1 0 9.7 9.7z")
val FolderOpen=icon("FolderOpen","M3 8V5a2 2 0 0 1 2-2h5l2 3h7a2 2 0 0 1 2 2v2M3 8h17a1 1 0 0 1 1 1l-2 11H5L3 8z")
val VisibilityOff=icon("VisibilityOff","m3 3 18 18M10 5a10 10 0 0 1 12 7 16 16 0 0 1-3 4M6 6a16 16 0 0 0-4 6s4 7 10 7a12 12 0 0 0 5-1M10 10a3 3 0 0 0 4 4")
val Visibility=icon("Visibility","M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12zm13 0a3 3 0 1 1-6 0 3 3 0 0 1 6 0z")
val ExpandMore=icon("ExpandMore","m6 9 6 6 6-6")
val AttachFile=icon("AttachFile","m21 11-8 8a6 6 0 0 1-8.5-8.5L13 2a4 4 0 0 1 5.7 5.7l-8.5 8.5a2 2 0 0 1-2.8-2.8L15 6")
val Send=icon("Send","M22 2 9 15M22 2l-7 20-6-7-7-6z")
val AutoAwesome=icon("AutoAwesome","M12 3l2.6 6.4L21 12l-6.4 2.6L12 21l-2.6-6.4L3 12l6.4-2.6L12 3z")
val NorthWest=icon("NorthWest","M19 5 5 19M5 5h14v14")
val ContentCopy=icon("ContentCopy","M9 9h12v12H9V9zM5 15H3V3h12v2")
val Check=icon("Check","m5 12 4 4L19 6")
// دور ۱۴: آیکون «بازتولید» — هم‌سبک با آیکون‌های خطی موجود
val Refresh=icon("Refresh","M21 12a9 9 0 1 1-2.6-6.4M21 4v6h-6")
val ThumbUp=icon("ThumbUp","M7 10H3v11h4V10zm0 0 5-7h2v7h7l-2 11H7V10z")
val ThumbDown=icon("ThumbDown","M7 14H3V3h4v11zm0 0 5 7h2v-7h7L19 3H7v11z")
val Mic=icon("Mic","M12 3a3 3 0 0 0-3 3v6a3 3 0 0 0 6 0V6a3 3 0 0 0-3-3zM5 11a7 7 0 0 0 14 0M12 18v3M9 21h6")
val VolumeUp=icon("VolumeUp","M11 5 6 9H3v6h3l5 4V5zM15.5 8.5a5 5 0 0 1 0 7M18.5 5.5a9 9 0 0 1 0 13")
val Close=icon("Close","m6 6 12 12M6 18 18 6")
val AddPhotoAlternate=icon("AddPhotoAlternate","M16 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7M16 5h6m-3-3v6M3 17l5-5 5 5 3-3 5 5M9 7h.01")
val PhotoCamera=icon("PhotoCamera","M14 4h-4L8 7H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-4l-2-3zM16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0z")
val CalendarMonth=icon("CalendarMonth","M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zm3-2v5m8-5v5M3 10h18M8 14h2m4 0h2m-8 4h2")
val Lock=icon("Lock","M6 10h12v11H6V10zM8 10V6a4 4 0 0 1 8 0v4M12 14v3")
val Mail=icon("Mail","M3 5h18v14H3V5zM3 5l9 7 9-7")
val Phone=icon("Phone","M7 3H4a1 1 0 0 0-1 1 17 17 0 0 0 17 17 1 1 0 0 0 1-1v-3l-5-2-2 3a15 15 0 0 1-8-8l3-2-2-5z")

// ── دور ششم: آیکون‌های lucide که سایت در صفحات خودش استفاده می‌کند و اپ نداشت ──
val Search=icon("Search","M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16M21 21 16.7 16.7")
val GitCompare=icon("GitCompare","M8 3 4 7l4 4M4 7h12a4 4 0 0 1 4 4v1M16 21l4-4-4-4M20 17H8a4 4 0 0 1-4-4v-1")
val HelpCircle=icon("HelpCircle","M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M9.1 9a3 3 0 0 1 5.8 1c0 2-3 2.5-3 3.5M12 17h.01")
val CalendarClock=icon("CalendarClock","M21 11V6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h6M16 2v4M8 2v4M3 10h18M16 22a4 4 0 1 0 0-8 4 4 0 0 0 0 8M16 16v2l1.5 1")
val Crown=icon("Crown","M3 17h18l-1.4-8.4-4.6 3.6L12 6 9 12.2 4.4 8.6 3 17M3 21h18")
val LifeBuoy=icon("LifeBuoy","M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6M5.6 5.6 8.5 8.5m7 7 2.9 2.9m0-12.8-2.9 2.9m-7 7-2.9 2.9")
val Wallet=icon("Wallet","M3 8a2 2 0 0 1 2-2h14a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H5a2 2 0 0 1-2-2V8zm17 4h-4a2 2 0 0 0 0 4h4")
val Home=icon("Home","M3 11 12 3l9 8M5 10v10h14V10M10 20v-6h4v6")
val Heart=icon("Heart","M12 20S4 15.3 4 10.5A3.5 3.5 0 0 1 12 8a3.5 3.5 0 0 1 8 2.5C20 15.3 12 20 12 20z")
val Briefcase=icon("Briefcase","M4 7h16a1 1 0 0 1 1 1v11a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8a1 1 0 0 1 1-1zm5 0V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2M3 12h18")
val AlertTriangle=icon("AlertTriangle","M12 3 2 20h20L12 3zm0 6v5m0 3h.01")
val Users=icon("Users","M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M14 4a4 4 0 1 1-8 0 4 4 0 0 1 8 0zM22 21v-2a4 4 0 0 0-3-3.9M16 3.1a4 4 0 0 1 0 7.8")
val Clock=icon("Clock","M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M12 7v5l3 2")
val Scale=icon("Scale","M12 3v18M7 21h10M5 7h14M12 7 5 7l-2 6a4 4 0 0 0 8 0L9 7m3 0 7 0 2 6a4 4 0 0 1-8 0l2-6")
val AlertCircle=icon("AlertCircle","M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M12 8v5m0 3h.01")
val HeartHandshake=icon("HeartHandshake","M19 14c1.5-1.5 3-3.2 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.8 0-3 .5-4.5 2-1.5-1.5-2.7-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4 3 5.5l7 7zM12 5 9 8a2.2 2.2 0 0 0 3 3l2-2a2.2 2.2 0 0 1 3 3l-2 2a2.2 2.2 0 0 1-3 0")
val ShieldCheck=icon("ShieldCheck","M12 3 4 6v6c0 4.5 3.4 7.6 8 9 4.6-1.4 8-4.5 8-9V6l-8-3zM9 12l2 2 4-4")
/** نشان چهاررنگ گوگل — همان SVG صفحهٔ ورود سایت (AuthPage.GoogleIcon)، نه آیکون خطی. */
private fun brand(name:String,vararg parts:Pair<String,Int>) =
 ImageVector.Builder(name,24.dp,24.dp,24f,24f).apply{parts.forEach{(d,c)->addPath(pathData=PathParser().parsePathString(d).toNodes(),fill=SolidColor(Color(c)))};}.build()
val GoogleMark=brand("GoogleMark",
 "M12 10.2v3.9h5.4c-.2 1.2-.9 2.3-2 3l3.2 2.5c1.9-1.7 3-4.3 3-7.3 0-.7-.1-1.4-.2-2.1H12z" to 0xFFEA4335.toInt(),
 "M12 22c2.7 0 5-.9 6.6-2.4l-3.2-2.5c-.9.6-2 1-3.4 1-2.6 0-4.8-1.8-5.6-4.2H3.1v2.6C4.7 19.8 8.1 22 12 22z" to 0xFF34A853.toInt(),
 "M6.4 13.9c-.2-.6-.3-1.2-.3-1.9s.1-1.3.3-1.9V7.5H3.1C2.4 8.9 2 10.4 2 12s.4 3.1 1.1 4.5l3.3-2.6z" to 0xFFFBBC05.toInt(),
 "M12 5.9c1.5 0 2.8.5 3.8 1.5l2.8-2.8C16.9 3 14.7 2 12 2 8.1 2 4.7 4.2 3.1 7.5l3.3 2.6c.8-2.4 3-4.2 5.6-4.2z" to 0xFF4285F4.toInt())
val ScanSearch=icon("ScanSearch","M4 8V5a1 1 0 0 1 1-1h3m8 0h3a1 1 0 0 1 1 1v3m0 8v3a1 1 0 0 1-1 1h-3m-8 0H5a1 1 0 0 1-1-1v-3M11 14a3 3 0 1 0 0-6 3 3 0 0 0 0 6M13.5 13.5 16 16")
val FileSignature=icon("FileSignature","M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h5M14 2v6h6M8 13h5M8 17h3M14 20c1.2-3 2.4 1.2 3.6-.8s1.2 1 2.4-.2")
val Download=icon("Download","M12 3v12m-5-5 5 5 5-5M5 21h14")
val CreditCard=icon("CreditCard","M3 7h18v12H3V7zm0 4h18M7 15h3")
val Library=icon("Library","M4 4h4v16H4V4zm6 0h4v16h-4V4zm7 .5 3 15.5")
val CheckCircle=icon("CheckCircle","M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18M8.5 12l2.5 2.5 4.5-5")
val ShieldAlert=icon("ShieldAlert","M12 3 4 6v6c0 4.5 3.4 7.6 8 9 4.6-1.4 8-4.5 8-9V6l-8-3zm0 5v5m0 3h.01")
// ── دور نهم: آیکون‌هایی که پیشنهادهای UI لازم داشتند ──
val Settings=icon("Settings","M12 15.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1A1.7 1.7 0 0 0 8.9 19a1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1A1.7 1.7 0 0 0 5 8.9a1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z")
val ArrowDown=icon("ArrowDown","M12 4v16M5 13l7 7 7-7")
val FilterList=icon("FilterList","M3 6h18M6 12h12M10 18h4")
}

