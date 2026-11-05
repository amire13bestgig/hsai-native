package ir.hsai.mobile

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
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
        .writeTimeout(45, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
) {
    @Volatile var token: String = ""
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
    suspend fun request(path: String, method: String = "GET", body: JSONObject? = null, authenticated: Boolean = true): JSONObject {
        val call = http.newCall(build(path, method, body, authenticated))
        return withContext(Dispatchers.IO) {
            call.await().use { response ->
                val raw = response.body?.string().orEmpty()
                val envelope = try { JSONObject(raw) } catch (_: Exception) {
                    throw ApiException("پاسخ سرویس قابل خواندن نیست.", response.code, "INVALID_RESPONSE")
                }
                if (!response.isSuccessful || !envelope.optBoolean("ok", false)) {
                    val error = envelope.optJSONObject("error")
                    throw ApiException(error?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: "درخواست انجام نشد (${response.code}).", response.code, error?.optString("code").orEmpty())
                }
                envelope.optJSONObject("data") ?: JSONObject().put("value", envelope.opt("data"))
            }
        }
    }
    fun stream(body: JSONObject): Flow<StreamEvent> = callbackFlow {
        val call = http.newCall(build("/chat/stream", "POST", body, true).newBuilder().header("Accept", "text/event-stream").build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { close(ApiException("ارتباط با سرویس قطع شد. دوباره تلاش کنید.")) }
            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    if (!r.isSuccessful) {
                        val error = runCatching { JSONObject(r.body?.string().orEmpty()).optJSONObject("error") }.getOrNull()
                        close(ApiException(error?.optString("message") ?: "خطای سرویس", r.code)); return
                    }
                    try {
                        val source = r.body?.source() ?: throw IOException("Empty body")
                        val parser = SseParser()
                        while (!source.exhausted() && !call.isCanceled()) {
                            val line = source.readUtf8Line() ?: break
                            parser.line(line)?.let { event -> trySendBlocking(event) }
                        }
                        parser.line("")?.let { trySendBlocking(it) }
                        close()
                    } catch (e: Exception) { close(ApiException("دریافت پاسخ جریانی کامل نشد. اتصال را بررسی کنید.")) }
                }
            }
        })
        awaitClose { call.cancel() }
    }
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(ApiException("اتصال برقرار نشد؛ اینترنت را بررسی کنید."))
            }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        })
    }
}

/** Pure parser, also covered by JVM tests. No HTML or browser engine is involved. */
class SseParser {
    private var event = "message"
    private val data = StringBuilder()
    fun line(line: String): StreamEvent? {
        if (line.isBlank()) {
            if (data.isEmpty()) { event = "message"; return null }
            val text = data.toString().trimEnd('\n'); data.clear()
            val name = event; event = "message"
            return StreamEvent(name, try { JSONObject(text) } catch (_: Exception) { JSONObject().put("chunk", text) })
        }
        when {
            line.startsWith(":") -> Unit
            line.startsWith("event:") -> event = line.substringAfter(':').trim()
            line.startsWith("data:") -> data.append(line.substringAfter(':').trimStart()).append('\n')
        }
        return null
    }
}
