package ir.hsai.mobile

object Validation {
 fun digits(value:String):String = buildString { value.forEach { c -> when {
  c in '۰'..'۹' -> append('0' + (c-'۰'))
  c in '٠'..'٩' -> append('0' + (c-'٠'))
  c !in listOf('٬', ',', ' ', '\u200c') -> append(c)
 } } }
 fun phone(value:String):String {
  val p=digits(value.trim()).replace(Regex("^(\\+98|0098)"),"0")
  require(Regex("09[0-9]{9}").matches(p)) { "شماره موبایل معتبر ۱۱ رقمی وارد کنید." }; return p
 }
 fun number(key:String,value:String):Number {
  val raw=digits(value.trim())
  require(Regex("[0-9]+(\\.[0-9]+)?").matches(raw)) { "عدد مثبت یا صفر وارد کنید." }
  val n=raw.toDouble()
  require(n.isFinite() && n<=9_007_199_254_740_991.0) { "عدد واردشده بیش از حد بزرگ است." }
  if(key.endsWith("_year")) require(n in 1300.0..1600.0) { "سال شمسی معتبر وارد کنید." }
  if(key.endsWith("_month")) require(n in 1.0..12.0) { "ماه باید بین ۱ تا ۱۲ باشد." }
  if(key.contains("percent")) require(n<=100.0) { "درصد بین صفر و صد است." }
  if(listOf("count","months","days","year","month").any { key.contains(it) }) require(n%1.0==0.0) { "تعداد و تاریخ باید عدد صحیح باشند." }
  return if(n%1.0==0.0) n.toLong() else n
 }
 fun nationalId(value:String):Boolean {
  val id=digits(value)
  if(!Regex("[0-9]{10}").matches(id)||id.toSet().size==1) return false
  val sum=(0..8).sumOf { (id[it]-'0')*(10-it) }; val r=sum%11
  return id[9]-'0'==if(r<2) r else 11-r
 }
}

object PendingCamera {
 fun restore(folder:java.io.File,name:String?):java.io.File? {
  if(name==null||!Regex("legal-[A-Za-z0-9_-]+\\.jpg").matches(name))return null
  return runCatching{java.io.File(folder,name).canonicalFile.takeIf{it.parentFile==folder.canonicalFile&&it.isFile}}.getOrNull()
 }
}
