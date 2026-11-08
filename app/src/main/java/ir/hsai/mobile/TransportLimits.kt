package ir.hsai.mobile

import java.io.InputStream
import java.io.ByteArrayOutputStream
import okio.BufferedSource
import okio.ByteString.Companion.toByteString

/** Limits are checked on actual bytes, including chunked responses; no silent truncation. */
object TransportLimits {
 const val JSON_BYTES=16*1024*1024
 const val SSE_LINE_BYTES=512*1024
 const val SSE_EVENT_BYTES=2*1024*1024
 const val SSE_TOTAL_BYTES=16*1024*1024
 fun read(input:InputStream,limit:Int):ByteArray {
  require(limit>0)
  val out=ByteArrayOutputStream();val buffer=ByteArray(8192);var total=0
  while(true){val n=input.read(buffer);if(n<0)break;total+=n;if(total>limit)throw tooLarge();out.write(buffer,0,n)}
  return out.toByteArray()
 }
 fun tooLarge()=ApiException("حجم پاسخ بیش از حد مجاز است و کامل دریافت نشد؛ ارسال دوباره خودکار انجام نمی‌شود.",0,"RESPONSE_TOO_LARGE")
}

/** Handles LF, CRLF and CR without waiting for a byte after a complete CR-delimited line. */
class SseLines(private val source:BufferedSource,private val limit:Int=TransportLimits.SSE_LINE_BYTES,private val totalLimit:Long=TransportLimits.SSE_TOTAL_BYTES.toLong()) {
 private val endings=byteArrayOf(10,13).toByteString()
 private var afterCr=false
 private var consumed=0L
 init{require(limit>0&&totalLimit>0)}
 private fun count(bytes:Long){consumed+=bytes;if(consumed>totalLimit)throw TransportLimits.tooLarge()}
 fun read():String? {
  if(afterCr){afterCr=false;if(source.request(1)&&source.buffer[0]==10.toByte()){source.skip(1);count(1)}}
  var scanned=0L
  while(true) {
   val end=source.buffer.indexOfElement(endings,scanned)
   if(end>=0){if(end>limit)throw TransportLimits.tooLarge();val line=source.readUtf8(end);afterCr=source.readByte()==13.toByte();count(end+1);return line}
   if(source.buffer.size>limit)throw TransportLimits.tooLarge()
   scanned=source.buffer.size
   if(!source.request(scanned+1)){if(source.buffer.size==0L)return null;count(source.buffer.size);return source.readUtf8()}
  }
 }
}
