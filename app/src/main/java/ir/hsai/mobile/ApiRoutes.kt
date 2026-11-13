package ir.hsai.mobile

object ApiRoutes {
 fun allowed(path:String, method:String):Boolean {
 val p=path.substringBefore("?")
 if (".." in p || !p.startsWith("/") || Regex("(?i)%2e|%2f|%5c|%25").containsMatchIn(p)) return false
 return when {
 Regex("/health").matches(p) -> method in setOf("GET")
 Regex("/config").matches(p) -> method in setOf("GET")
 Regex("/auth/register").matches(p) -> method in setOf("POST")
 Regex("/auth/login").matches(p) -> method in setOf("POST")
 Regex("/auth/otp/send").matches(p) -> method in setOf("POST")
 Regex("/auth/otp/verify").matches(p) -> method in setOf("POST")
 Regex("/auth/refresh").matches(p) -> method in setOf("POST")
 Regex("/me").matches(p) -> method in setOf("GET", "PATCH")
 Regex("/chat").matches(p) -> method in setOf("POST")
 Regex("/chat/stream").matches(p) -> method in setOf("POST")
 Regex("/ask").matches(p) -> method in setOf("POST")
 Regex("/consultations").matches(p) -> method in setOf("GET", "POST")
 Regex("/consultations/[0-9]+").matches(p) -> method in setOf("GET", "DELETE")
 Regex("/consultations/[0-9]+/messages").matches(p) -> method in setOf("POST")
 Regex("/messages/[0-9]+/feedback").matches(p) -> method in setOf("POST")
 Regex("/messages/[0-9]+/simplify").matches(p) -> method in setOf("POST")
 // دور ۱۴/۱۵: بازتولید پاسخ — اکنون در API v1 هم وجود دارد
 Regex("/messages/[0-9]+/regenerate").matches(p) -> method in setOf("POST")
 Regex("/documents").matches(p) -> method in setOf("GET", "POST")
 Regex("/documents/[0-9]+").matches(p) -> method in setOf("GET", "PUT", "DELETE")
 Regex("/documents/[0-9]+/docx").matches(p) -> method in setOf("GET")
 Regex("/scanner").matches(p) -> method in setOf("GET")
 Regex("/scanner/[0-9]+").matches(p) -> method in setOf("DELETE")
 Regex("/scanner/analyze").matches(p) -> method in setOf("POST")
 Regex("/laws").matches(p) -> method in setOf("GET")
 Regex("/laws/collections").matches(p) -> method in setOf("GET")
 Regex("/laws/comparisons").matches(p) -> method in setOf("GET")
 Regex("/laws/articles/[0-9]+").matches(p) -> method in setOf("GET")
 Regex("/calculators/compute").matches(p) -> method in setOf("POST")
 Regex("/deadlines").matches(p) -> method in setOf("GET", "POST")
 Regex("/deadlines/[0-9]+").matches(p) -> method in setOf("PATCH", "DELETE")
 Regex("/billing/plans").matches(p) -> method in setOf("GET")
 Regex("/billing/discount/validate").matches(p) -> method in setOf("POST")
 Regex("/billing/checkout").matches(p) -> method in setOf("POST")
 Regex("/billing/payments").matches(p) -> method in setOf("GET")
 Regex("/billing/payments/[A-Za-z0-9_%.-]+").matches(p) -> method in setOf("GET")
 Regex("/tickets").matches(p) -> method in setOf("GET", "POST")
 Regex("/tickets/[0-9]+/reply").matches(p) -> method in setOf("POST")
 // ── دور پنجم: همان قابلیت‌هایی که وب‌سایت داشت و اپ راهی به آن‌ها نداشت ──
 Regex("/laws/[0-9]+/relations").matches(p) -> method in setOf("GET")
 Regex("/billing/my-billing").matches(p) -> method in setOf("GET")
 Regex("/billing/cancel").matches(p) -> method in setOf("POST")
 Regex("/billing/resume").matches(p) -> method in setOf("POST")
 Regex("/auth/password/forgot").matches(p) -> method in setOf("POST")
 Regex("/auth/password/reset").matches(p) -> method in setOf("POST")
 Regex("/auth/email/send-verification").matches(p) -> method in setOf("POST")
 Regex("/auth/email/verify").matches(p) -> method in setOf("POST")
 Regex("/auth/contact").matches(p) -> method in setOf("POST")
 Regex("/auth/google/handoff").matches(p) -> method in setOf("POST")
 Regex("/auth/google/link").matches(p) -> method in setOf("POST")
 Regex("/auth/google/link/send-otp").matches(p) -> method in setOf("POST")
 Regex("/auth/identities").matches(p) -> method in setOf("GET")
 Regex("/auth/identities/[a-z]+/unlink").matches(p) -> method in setOf("POST")
else -> false
}
}
}
