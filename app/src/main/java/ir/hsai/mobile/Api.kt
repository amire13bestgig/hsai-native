package ir.hsai.mobile

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiException(message: String, val status: Int = 0, val code: String = "") : Exception(message)
data class StreamEvent(val name: String, val data: JSONObject)

/** Only the official API is used by the app. The alternate constructor base is for JVM tests. */
class ApiClient(
    private val base: String = "https://hsai.ir/api/v1",
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
) {
    @Volatile var token: String = ""
        set(value){require(value.isEmpty()||SessionTokens.headerSafe(value)){"نشست دریافتی معتبر نیست؛ دوباره وارد شوید."};field=value}
    private fun build(path: String, method: String, body: JSONObject?, authenticated: Boolean): Request {
        require(ApiRoutes.allowed(path, method)) { "مسیر درخواست مجاز نیست." }
        if (authenticated && token.isBlank()) throw ApiException("برای ادامه وارد حساب شوید.", 401, "LOGIN_REQUIRED")
        val builder = Request.Builder().url(base.trimEnd('/') + path)
            .header("Accept", "application/json")
        if (token.isNotBlank()) builder.header("Authorization", "Bearer $token")
        val requestBody = if (method in listOf("POST", "PATCH", "PUT"))
            (body ?: JSONObject()).toString().toRequestBody("application/json; charset=utf-8".toMediaType()) else null
        return builder.method(method, requestBody).build()
    }
    suspend fun request(path:String,method:String="GET",body:JSONObject?=null,authenticated:Boolean=true):JSONObject {
        val call=http.newCall(build(path,method,body,authenticated))
        return call.consume { response->
            val raw=response.body?.byteStream()?.use{String(TransportLimits.read(it,TransportLimits.JSON_BYTES),Charsets.UTF_8).removePrefix("\uFEFF")}.orEmpty()
            val envelope=try{JSONObject(raw)}catch(_:Exception){throw ApiException("پاسخ سرویس قابل خواندن نیست.",response.code,"INVALID_RESPONSE")}
            if(!response.isSuccessful||!envelope.optBoolean("ok",false)) {
                val error=envelope.optJSONObject("error")
                throw ApiException(error?.str("message")?.takeIf{it.isNotBlank()}?:"درخواست انجام نشد (${response.code}).",response.code,error?.str("code").orEmpty())
            }
            envelope.optJSONObject("data")?:JSONObject().put("value",envelope.opt("data"))
        }
    }
    suspend fun documentBytes(id:Long,maxBytes:Int=20*1024*1024):ByteArray {
        require(id>0&&maxBytes in 4..20*1024*1024)
        val call=http.newCall(build("/documents/$id/docx","GET",null,true).newBuilder().header("Accept","application/vnd.openxmlformats-officedocument.wordprocessingml.document").build())
        return call.consume { response->
            if(!response.isSuccessful)throw ApiException("دریافت سند انجام نشد (${response.code}).",response.code,"DOCUMENT_DOWNLOAD")
            val body=response.body?:throw ApiException("فایل سند موجود نیست.",0,"INVALID_DOCUMENT")
            if(body.contentLength()>maxBytes)throw ApiException("حجم سند بیش از حد مجاز است.",0,"DOCUMENT_TOO_LARGE")
            val bytes=try{body.byteStream().use{TransportLimits.read(it,maxBytes)}}catch(e:ApiException){throw ApiException("حجم سند بیش از حد مجاز است.",0,"DOCUMENT_TOO_LARGE")}
            if(bytes.size<4||bytes[0]!=80.toByte()||bytes[1]!=75.toByte()||bytes[2]!=3.toByte()||bytes[3]!=4.toByte())throw ApiException("خروجی دریافتی فایل Word معتبر به نظر نمی‌رسد.",0,"INVALID_DOCUMENT")
            bytes
        }
    }

    fun stream(body: JSONObject): Flow<StreamEvent> = callbackFlow {
        val call = http.newCall(build("/chat/stream", "POST", body, true).newBuilder().header("Accept", "text/event-stream").build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { close(ApiException("ارتباط با سرویس قطع شد. دوباره تلاش کنید.")) }
            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) {
                        val error = runCatching { JSONObject(r.body?.byteStream()?.use{String(TransportLimits.read(it,64*1024),Charsets.UTF_8)}.orEmpty()).optJSONObject("error") }.getOrNull()
                        close(ApiException(error?.optString("message") ?: "خطای سرویس", r.code,error?.str("code").orEmpty())); return
                    }
                    try {
                        val source = r.body?.source() ?: throw IOException("Empty body")
                        val parser = SseParser();val lines=SseLines(source)
                        while (!call.isCanceled()) {
                            val line = lines.read() ?: break
                            parser.line(line)?.let { event -> trySendBlocking(event) }
                        }
                        parser.line("")?.let { trySendBlocking(it) }
                        close()
                    } catch (e: Exception) { close(if(e is ApiException)e else ApiException("دریافت پاسخ جریانی کامل نشد. اتصال را بررسی کنید.")) }
                }
            }
        })
        awaitClose { call.cancel() }
    }
    /** Cancellation covers reading the body as well as waiting for response headers. */
    private suspend fun <T> Call.consume(block:(Response)->T):T=suspendCancellableCoroutine { continuation->
        continuation.invokeOnCancellation{cancel()}
        enqueue(object:Callback {
            override fun onFailure(call:Call,e:IOException){if(continuation.isActive)continuation.resumeWithException(ApiException("اتصال برقرار نشد؛ اینترنت را بررسی کنید."))}
            override fun onResponse(call:Call,response:Response){
                try {val result=response.use{block(it)};if(continuation.isActive)continuation.resume(result)}
                catch(e:Exception){if(continuation.isActive)continuation.resumeWithException(if(e is ApiException)e else ApiException("دریافت پاسخ کامل نشد؛ اتصال را بررسی کنید."))}
            }
        })
    }
}

/** Pure parser, also covered by JVM tests. No HTML or browser engine is involved. */
class SseParser(private val maxBytes:Int=TransportLimits.SSE_EVENT_BYTES) {
    init { require(maxBytes>0) }
    private var bytes=0
    private var first=true
    private var event = "message"
    private val data = StringBuilder()
    fun line(raw: String): StreamEvent? {
        val line=if(first)raw.removePrefix("\uFEFF")else raw;first=false
        if (line.isEmpty()) {
            if (data.isEmpty()) { event = "message"; return null }
            val text = data.toString().removeSuffix("\n"); data.clear();bytes=0
            val name = event.ifEmpty{"message"}; event = "message"
            return StreamEvent(name,try{JSONObject(text)}catch(_:Exception){if(name=="done"&&text.isNotEmpty()&&text!="[DONE]")throw ApiException("پاسخ نهایی جریانی قابل خواندن نیست؛ تاریخچه را بررسی کنید.",0,"INVALID_RESPONSE");JSONObject().put("chunk",text)})
        }
        if(line.startsWith(":"))return null
        val colon=line.indexOf(':');val field=if(colon<0)line else line.substring(0,colon)
        val value=if(colon<0)""else line.substring(colon+1).removePrefix(" ")
        when(field) {
            "event"->event=value
            "data"->{bytes+=value.toByteArray(Charsets.UTF_8).size+1;if(bytes>maxBytes)throw TransportLimits.tooLarge();data.append(value).append('\n')}
        }
        return null
    }
}
