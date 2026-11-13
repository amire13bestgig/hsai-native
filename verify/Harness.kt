// Harness اجراییِ قراردادهای کلاینت — بیرون از درخت سورس اپ و بدون وابستگی اندروید.
// اجرا: bash run.sh  (نیازمند JDK 17، kotlinc و vendor/json-20240303.jar)
package ir.hsai.mobile

import org.json.JSONObject

// افزونه‌های JSON از فایل واقعیِ اپ (JsonExt.kt) کامپایل می‌شوند، نه از نسخهٔ بدل.
// فقط ApiException (Api.kt) و ChatMessage (AppViewModel.kt) که به اندروید وابسته‌اند اینجا آینه شده‌اند.
class ApiException(message:String, val status:Int = 0, val code:String = "") : Exception(message)
// آینهٔ دقیقِ data class ChatMessage در AppViewModel.kt (فقط برای کامپایل این هارنس).
data class ChatMessage(val role:String,val text:String,val id:Long?=null,val citations:List<JSONObject> = emptyList(),val image:String?=null,val document:JSONObject?=null,val feedback:String="",val verification:JSONObject?=null,val questions:List<JSONObject> = emptyList(),val suggestions:List<String> = emptyList(),val simplified:String?=null)

var pass = 0
var fail = 0
fun ok(name:String, cond:Boolean, detail:Any? = null) { if (cond) pass++ else { fail++; println("FAIL  $name  ${detail ?: ""}") } }

fun main() {
    // ۱) سند: عنوان و شرح نباید در هم ادغام شوند
    val doc = ApiContract.documentBody("petition", "  دادخواست مطالبه وجه  ", "متن ماجرا")
    ok("doc_title_clean", doc.str("title") == "دادخواست مطالبه وجه", doc.str("title"))
    ok("doc_subject_is_title_not_body", doc.str("subject") == doc.str("title"), doc.str("subject"))
    ok("doc_details_separate", doc.str("details") == "متن ماجرا" && doc.str("facts_summary") == "متن ماجرا")
    ok("doc_type_set", doc.str("doc_type") == "petition")

    // ۲) اسکنر: تاریخ ابلاغ واقعی باید ارسال شود، وگرنه هیچ سررسیدی ساخته نمی‌شود
    val noDate = ApiContract.scannerBody("notice", "متن ابلاغ", null)
    ok("scan_no_empty_notice_date", !noDate.has("notice_date"), noDate)
    val dated = ApiContract.scannerBody("notice", "متن ابلاغ", null, "2026-10-01")
    ok("scan_notice_date_sent", dated.str("notice_date") == "2026-10-01", dated)
    ok("scan_mode_notice", dated.str("scan_mode") == "sana_notice")
    val contractScan = ApiContract.scannerBody("contract", "متن قرارداد", "data:image/jpeg;base64,AAA")
    ok("scan_mode_contract", contractScan.str("scan_mode") == "contract_risk")
    ok("scan_image_passthrough", contractScan.str("image_base64").startsWith("data:image/jpeg;base64,"))

    // ۳) برگهٔ خوانده‌نشده: وضعیت واقعی سرور باید تشخیص داده شود
    ok("unreadable_true", ApiContract.scanUnreadable(JSONObject("{\"read_ok\":false}")))
    ok("unreadable_false", !ApiContract.scanUnreadable(JSONObject("{\"read_ok\":true}")))
    ok("unreadable_absent_ok", !ApiContract.scanUnreadable(JSONObject("{}")))
    ok("unreadable_null_ok", !ApiContract.scanUnreadable(null))
    ok("notice_date_missing", ApiContract.scanNoticeDateMissing(JSONObject("{\"deadline\":{\"status\":\"missing_notice_date\"}}")))
    ok("notice_date_present", !ApiContract.scanNoticeDateMissing(JSONObject("{\"deadline\":{\"status\":\"computed\"}}")))

    // ۴) راستی‌آزمایی: پاسخ تأییدنشده نباید مثل پاسخ سالم نمایش داده شود
    ok("verify_failed_flag", ApiContract.verificationFailed(JSONObject("{\"verified\":false}")))
    ok("verify_failed_status", ApiContract.verificationFailed(JSONObject("{\"verification_status\":\"failed\"}")))
    ok("verify_ok", !ApiContract.verificationFailed(JSONObject("{\"verified\":true,\"verification_status\":\"passed\"}")))
    ok("verify_null_ok", !ApiContract.verificationFailed(null))
    val issues = ApiContract.citationIssues(JSONObject("{\"citation_issues\":[{\"reference\":\"ماده ۱۹۰\"},{\"citation\":\"ماده ۱۰\"}]}"))
    ok("citation_issues_count", issues.size == 2, issues.size)
    ok("citation_issues_ref", issues[0].str("reference") == "ماده ۱۹۰")

    // ۵) ندارد بودنِ کلید آرایه نباید null بدهد (باگ NPE در نسخهٔ قبلی)
    ok("objects_missing_key_is_empty", JSONObject("{}").objects("items").isEmpty())
    ok("objects_null_array_is_empty", JSONObject("{\"items\":null}").objects("items").isEmpty())
    ok("objects_reads_array", JSONObject("{\"items\":[{\"id\":1},{\"id\":2}]}").objects("items").size == 2)
    ok("page_without_items_is_empty", ApiContract.page(JSONObject("{}"),1,20).items.isEmpty())
    ok("page_without_items_has_no_next", !ApiContract.page(JSONObject("{}"),1,20).hasNext)

    // ۶) هوشمندی‌های پاسخ: کارت‌هایی که سایت رندر می‌کند باید از همان JSON پاسخ بیرون بیایند
    val rich = JSONObject(
        "{\"mode\":\"calculator_and_rag\",\"hide_badge\":false," +
        "\"calc_card\":{\"title\":\"هزینه دادرسی\",\"items\":[{\"label\":\"خواسته\",\"value\":\"۱٬۰۰۰٬۰۰۰\"}]}," +
        "\"sana_reassurance\":{\"arrest_risk\":{\"safe\":true,\"badge\":\"خیر\"},\"deadline_summary\":\"۲۰ روز\"}," +
        "\"citizen_guide\":{\"quick_summary\":{\"bottom_line\":\"حق دارید\",\"best_move_now\":\"اظهارنامه\"}," +
        "\"deadly_mistakes\":[\"اصل چک را ندهید\"],\"action_plan\":{\"where_to_go\":\"دفتر خدمات\"}}," +
        "\"quick_questions\":[{\"question\":\"چک در چه تاریخ سررسید بود؟\",\"options\":[\"گذشته\",\"آینده\"]}]," +
        "\"stages\":[{\"name\":\"صلاحیت\",\"detail\":\"دادگاه صلح\"}],\"competent_authority\":\"دادگاه صلح\"," +
        "\"total_checked_corpus\":28360}"
    )
    ok("mode_read", ApiContract.mode(rich) == "calculator_and_rag", ApiContract.mode(rich))
    ok("hide_badge_false", !ApiContract.hideBadge(rich))
    ok("calc_card_items", ApiContract.calcCard(rich)?.objects("items")?.size == 1)
    ok("calc_card_empty_hidden", ApiContract.calcCard(JSONObject("{\"calc_card\":{\"title\":\"x\",\"items\":[]}}")) == null)
    ok("sana_card", ApiContract.sanaReassurance(rich)?.optJSONObject("arrest_risk")?.optBoolean("safe") == true)
    ok("citizen_guide", ApiContract.citizenGuide(rich) != null)
    ok("quick_summary", ApiContract.quickSummary(rich)?.str("bottom_line") == "حق دارید", ApiContract.quickSummary(rich))
    ok("quick_questions_from_verification", ApiContract.quickQuestions(JSONObject().put("verification", rich)).size == 1)
    ok("verification_not_failed", !ApiContract.verificationFailed(rich))
    // برچسب حالت: پاسخ گفتگویی و پاسخ rag بدون استناد برچسب نمی‌گیرند (منطق hideModeBadge سایت)
    ok("mode_hidden_conversational", ApiContract.modeBadgeHidden(JSONObject("{\"mode\":\"conversational\"}"), 0, false))
    ok("mode_hidden_rag_no_citation", ApiContract.modeBadgeHidden(JSONObject("{\"mode\":\"rag\"}"), 0, false))
    ok("mode_shown_rag_with_citation", !ApiContract.modeBadgeHidden(JSONObject("{\"mode\":\"rag\"}"), 2, false))
    ok("mode_shown_document", !ApiContract.modeBadgeHidden(JSONObject("{\"mode\":\"document\"}"), 0, true))
    ok("risk_flags_read", ApiContract.redFlags(JSONObject("{\"red_flags\":[{\"title\":\"جریمه سنگین\"}],\"risk_score\":72}")).size == 1)
    ok("risk_score_read", ApiContract.riskScore(JSONObject("{\"risk_score\":72}")) == 72)

    // ۷) رویداد خطای جویبار: کد سهمیه باید به ۴۰۲ نگاشت شود (پنجرهٔ ارتقا مثل سایت)
    ok("quota_code_subscription", ApiContract.quotaStatus("NO_SUBSCRIPTION") == 402)
    ok("quota_code_message_limit", ApiContract.quotaStatus("MESSAGE_LIMIT_REACHED") == 402)
    ok("quota_code_free_limit", ApiContract.quotaStatus("FREE_LIMIT_REACHED") == 402)
    ok("quota_code_validation", ApiContract.quotaStatus("VALIDATION_ERROR") == 400)
    ok("quota_code_unknown", ApiContract.quotaStatus("SERVER_ERROR") == 0)
    ok("quota_event_payload", ApiContract.quotaEvent(JSONObject("{\"free_quota\":{\"enabled\":true,\"remaining\":2,\"limit\":3}}"))?.optInt("remaining") == 2)
    ok("conversation_id_flat", ApiContract.conversationId(JSONObject("{\"conversation_id\":77}")) == 77L)
    ok("conversation_id_nested", ApiContract.conversationId(JSONObject("{\"conversation\":{\"id\":88}}")) == 88L)

    // ۸) دور پنجم — شکاف‌هایی که اپ نسبت به وب‌سایت داشت
    // روابط مواد قانونی (`/laws/:id/relations`)
    val rels = ApiContract.relations(JSONObject("{\"relations\":[{\"id\":1,\"source_article_id\":10,\"target_article_id\":20,\"target_title\":\"ماده ۲۲ قانون روابط موجر و مستأجر\",\"target_law_name\":\"روابط موجر و مستأجر\",\"relation_type\":\"نسخ\",\"note\":\"با تصویب قانون ۱۳۷۶ نسخ شد.\"}]}"))
    ok("relations_count", rels.size == 1, rels.size)
    ok("relation_outgoing", ApiContract.relationOutgoing(rels[0], 10L))
    ok("relation_not_outgoing", !ApiContract.relationOutgoing(rels[0], 20L))
    ok("relation_title_prefers_joined_title", ApiContract.relationTitle(rels[0], 10L) == "ماده ۲۲ قانون روابط موجر و مستأجر", ApiContract.relationTitle(rels[0], 10L))
    ok("relation_type_read", ApiContract.relationType(rels[0]) == "نسخ")
    ok("relation_note_read", ApiContract.relationNote(rels[0]).startsWith("با تصویب"))
    val incoming = JSONObject("{\"source_article_id\":33,\"source_law_name\":\"قانون ثبت\",\"source_article_number\":\"۴۸\"}")
    ok("relation_title_fallback_composed", ApiContract.relationTitle(incoming, 99L).contains("قانون ثبت") && ApiContract.relationTitle(incoming, 99L).contains("۴۸"), ApiContract.relationTitle(incoming, 99L))
    ok("relations_absent_is_empty", ApiContract.relations(JSONObject("{}")).isEmpty())

    // صورت‌حساب: کلیدها همیشه هستند (وگرنه UI روی پاسخ کوتاه NPE می‌دهد)
    val bill = ApiContract.billing(JSONObject("{}"))
    ok("billing_keys_present", bill.has("active_subscription") && bill.has("quota") && bill.has("subscription_history") && bill.has("payments"))
    ok("auto_renew_default_active", !ApiContract.autoRenewCancelled(JSONObject("{\"cancel_at_period_end\":0}")))
    ok("auto_renew_bool", ApiContract.autoRenewCancelled(JSONObject("{\"cancel_at_period_end\":true}")))
    ok("auto_renew_int", ApiContract.autoRenewCancelled(JSONObject("{\"cancel_at_period_end\":1}")))
    ok("auto_renew_null_safe", !ApiContract.autoRenewCancelled(null))
    ok("auto_renew_reason", ApiContract.autoRenewReason(JSONObject("{\"notes\":\"گران بود\"}")) == "گران بود")

    // تأیید ایمیل
    ok("email_verified_true", ApiContract.emailVerified(JSONObject("{\"email_verified\":true}")))
    ok("email_verified_int", ApiContract.emailVerified(JSONObject("{\"email_verified\":1}")))
    ok("email_verified_false", !ApiContract.emailVerified(JSONObject("{\"email_verified\":0}")))
    ok("email_verified_null_safe", !ApiContract.emailVerified(null))

    // ورود با گوگل: nonce دستگاه + برگهٔ دست‌دهی + تحلیل deep link
    val nonce1 = ApiContract.newHandoffNonce(); val nonce2 = ApiContract.newHandoffNonce()
    ok("nonce_length", nonce1.length == 36, nonce1.length)
    ok("nonce_hex", Regex("^[0-9a-f]{36}$").matches(nonce1), nonce1)
    ok("nonce_not_reused", nonce1 != nonce2)
    ok("nonce_server_rule", ApiContract.handoffNonceValid(nonce1))
    ok("nonce_rejects_short", !ApiContract.handoffNonceValid("abc"))
    ok("handoff_shape_ok", ApiContract.handoffTokenShapeOk("eyJ1Ijo0fQ.c2lnbmF0dXJl"))
    ok("handoff_shape_rejects_spaces", !ApiContract.handoffTokenShapeOk("a b c d e f g h i j k l m n o p"))
    val startUrl = ApiContract.googleStartUrl("https://hsai.ir",nonce1)
    ok("google_start_url_api_app", startUrl == "https://hsai.ir/api/auth/google/app/start?nonce=" + nonce1, startUrl)
    ok("google_start_url_not_web_route", !startUrl.contains("/api/auth/google/start"), startUrl)
    ok("google_start_url_no_web_flags", !startUrl.contains("app_return") && !startUrl.contains("app_nonce"), startUrl)
    ok("google_start_url_origin_trim", ApiContract.googleStartUrl("https://hsai.ir/",nonce1) == startUrl)

    // گیت نمایش دکمهٔ گوگل: جریان مرورگری باید روی سرور کامل باشد (client_id + secret)
    ok("google_gate_redirect", ApiContract.googleAppSignInEnabled(JSONObject("{\"google_redirect_enabled\":true}")))
    ok("google_gate_fallback", ApiContract.googleAppSignInEnabled(JSONObject("{\"google_sign_in_enabled\":true}")))
    ok("google_gate_off", !ApiContract.googleAppSignInEnabled(JSONObject("{}")))
    ok("google_gate_prefers_redirect", !ApiContract.googleAppSignInEnabled(JSONObject("{\"google_sign_in_enabled\":true,\"google_redirect_enabled\":false}")))
    ok("google_path_default", ApiContract.googleAppSignInPath(JSONObject("{}")) == "/api/auth/google/app/start")
    ok("google_path_from_server", ApiContract.googleAppSignInPath(JSONObject("{\"google_app_signin_path\":\"/api/auth/google/app/start\"}")) == "/api/auth/google/app/start")
    ok("google_path_blank_falls_back", ApiContract.googleAppSignInPath(JSONObject("{\"google_app_signin_path\":\"  \"}")) == "/api/auth/google/app/start")
    ok("google_url_with_path", ApiContract.googleStartUrl("https://hsai.ir",nonce1,"/api/auth/google/app/start") == startUrl)
    ok("google_url_never_web_flow", !startUrl.contains("/api/auth/google/start?") && !startUrl.contains("/api/auth/google/callback"))

    // nonce ورود گوگل باید از کشته‌شدن فرایند در حین ورود در مرورگر جان سالم ببرد
    val now = 1_700_000_000_000L
    ok("oauth_nonce_roundtrip", ApiContract.decodeOauthNonce(ApiContract.encodeOauthNonce(nonce1,now),now) == nonce1)
    ok("oauth_nonce_fresh_window", ApiContract.decodeOauthNonce(ApiContract.encodeOauthNonce(nonce1,now - 599_000L),now) == nonce1)
    ok("oauth_nonce_expired", ApiContract.decodeOauthNonce(ApiContract.encodeOauthNonce(nonce1,now - 601_000L),now).isEmpty())
    ok("oauth_nonce_skew_rejected", ApiContract.decodeOauthNonce(ApiContract.encodeOauthNonce(nonce1,now + 60_000L),now).isEmpty())
    ok("oauth_nonce_ttl_seconds", ApiContract.OAUTH_NONCE_TTL_SECONDS == 600L)
    ok("oauth_nonce_malformed", ApiContract.decodeOauthNonce("garbage",now).isEmpty() && ApiContract.decodeOauthNonce("",now).isEmpty())
    ok("oauth_nonce_rejects_bad_nonce", ApiContract.decodeOauthNonce(ApiContract.encodeOauthNonce("abc",now),now).isEmpty())
    val link = ApiContract.parseHandoffLink("hsai://auth/google?handoff=abc.def&nonce=" + nonce1)
    ok("handoff_link_parsed", link != null && link.handoff == "abc.def" && link.nonce == nonce1, link)
    ok("handoff_link_error", ApiContract.parseHandoffLink("hsai://auth/google?error=%D9%84%D8%BA%D9%88%20%D8%B4%D8%AF")?.error == "لغو شد", ApiContract.parseHandoffLink("hsai://auth/google?error=%D9%84%D8%BA%D9%88%20%D8%B4%D8%AF"))
    val linkReq = ApiContract.parseHandoffLink("hsai://auth/google?error=link_required&link_token=tk_9&email=a***z%40example.com")
    ok("handoff_link_required", linkReq != null && linkReq.error == "link_required" && linkReq.linkToken == "tk_9" && linkReq.email == "a***z@example.com", linkReq)
    ok("handoff_rejects_other_scheme", ApiContract.parseHandoffLink("https://evil.example/auth/google?handoff=x") == null)
    ok("handoff_rejects_other_host", ApiContract.parseHandoffLink("hsai://payment/callback?handoff=x") == null)

    // فرم تماس و روش‌های ورود
    ok("contact_tracking", ApiContract.contactTracking(JSONObject("{\"tracking_code\":\"HS-1042\"}")) == "HS-1042")
    val ids = JSONObject("{\"providers\":[{\"provider\":\"google\",\"email\":\"user@example.com\"}]}")
    ok("identities_read", ApiContract.identities(ids).size == 1)
    ok("identities_google", ApiContract.hasGoogleIdentity(ids))
    ok("identities_none", !ApiContract.hasGoogleIdentity(JSONObject("{}")))

    // ۹) allowlist مسیرها — قابلیت تازه بدون مجوز مسیر، در عمل کار نمی‌کند
    listOf(
      "/laws/12/relations" to "GET",
      "/billing/my-billing" to "GET",
      "/billing/cancel" to "POST",
      "/billing/resume" to "POST",
      "/auth/password/forgot" to "POST",
      "/auth/password/reset" to "POST",
      "/auth/email/send-verification" to "POST",
      "/auth/email/verify" to "POST",
      "/auth/contact" to "POST",
      "/auth/google/handoff" to "POST",
      "/auth/google/link" to "POST",
      "/auth/google/link/send-otp" to "POST",
      "/auth/identities" to "GET",
      "/auth/identities/google/unlink" to "POST"
    ).forEach { (path, method) ->
      ok("route_allowed:" + path, ApiRoutes.allowed(path, method), path)
    }
    ok("route_denies_wrong_method", !ApiRoutes.allowed("/auth/password/forgot", "GET"))
    ok("route_denies_delete_billing", !ApiRoutes.allowed("/billing/cancel", "DELETE"))
    ok("route_denies_unknown", !ApiRoutes.allowed("/laws/12/relations/../../admin", "GET"))
    ok("route_denies_admin", !ApiRoutes.allowed("/admin/users", "GET"))
    ok("route_keeps_payment_guard", ApiRoutes.allowed("/billing/checkout", "POST") && !ApiRoutes.allowed("/billing/checkout", "GET"))

    // ۱۰) دور هشتم — باگ‌های رفع‌شده باید رفتار تازه را قفل کنند
    // ۱۰.۱ کارت موضوع دیگر پیش‌نویس کاربر را پاک نمی‌کند
    ok("topic_keeps_user_draft", UiPresentation.topicDraft("چک برگشتی دارم", "مهلت اعتراض") == "چک برگشتی دارم مهلت اعتراض؛ ", UiPresentation.topicDraft("چک برگشتی دارم", "مهلت اعتراض"))
    ok("topic_from_empty", UiPresentation.topicDraft("   ", "مهریه") == "مهریه؛ ")
    ok("topic_no_double_semicolon", UiPresentation.topicDraft("", "مهریه؛") == "مهریه؛ ")
    ok("topic_draft_limit_respected", UiPresentation.topicDraft("x".repeat(UiPresentation.DRAFT_LIMIT), "مهریه").length > UiPresentation.DRAFT_LIMIT)
    // ۱۰.۲ سقف پیش‌نویس دیگر خاموش نیست
    val underLimit = UiPresentation.clampDraft("سلام")
    ok("draft_under_limit", underLimit.value == "سلام" && !underLimit.truncated)
    val overLimit = UiPresentation.clampDraft("y".repeat(UiPresentation.DRAFT_LIMIT + 5))
    ok("draft_over_limit_clamped", overLimit.value.length == UiPresentation.DRAFT_LIMIT && overLimit.truncated)
    ok("draft_limit_is_12000", UiPresentation.DRAFT_LIMIT == 12000)
    // ۱۰.۳ ماشین‌حساب ناشناخته دیگر استثنا پرتاب نمی‌کند
    ok("calc_known", Domain.calculatorOrNull("mehrieh")?.id == "mehrieh")
    ok("calc_unknown_is_null", Domain.calculatorOrNull("does_not_exist") == null)
    ok("calc_fallback_is_first", Domain.calculatorOrFirst("does_not_exist").id == Domain.calculators.first().id)
    ok("calc_fallback_keeps_known", Domain.calculatorOrFirst("labor").id == "labor")
    // ۱۰.۴ شناسهٔ پرداخت معلق فقط با تسویهٔ قطعی پاک می‌شود
    ok("payment_settled_paid", ApiContract.paymentSettled(JSONObject("""{"status":"paid"}""")))
    ok("payment_settled_nested", ApiContract.paymentSettled(JSONObject("""{"payment":{"status":"verified"}}""")))
    ok("payment_settled_flag", ApiContract.paymentSettled(JSONObject("""{"paid":true}""")))
    ok("payment_pending_not_settled", !ApiContract.paymentSettled(JSONObject("""{"status":"pending"}""")))
    ok("payment_unknown_not_settled", !ApiContract.paymentSettled(JSONObject("{}")))
    ok("payment_failed_not_settled", !ApiContract.paymentSettled(JSONObject("""{"status":"failed"}""")))

    // ۱۱) دور نهم — توابع خالصِ تازه
    // ۱۱.۱ ساعت نشست از توکن JWT
    fun jwt(exp:Long):String {
      fun b64(t:String)=java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(t.toByteArray())
      return b64("""{"alg":"HS256"}""")+"."+b64("""{"exp":$exp}""")+".sig"
    }
    val tok = jwt(1_800_000_000L)
    ok("session_expiry_read", SessionClock.expiryEpochSeconds(tok) == 1_800_000_000L, SessionClock.expiryEpochSeconds(tok))
    ok("session_left_positive", SessionClock.secondsLeft(tok, 1_799_999_700L) == 300L)
    ok("session_soon_boundary", SessionClock.warning(300L) == SessionWarning.SOON && SessionClock.warning(301L) == SessionWarning.NONE)
    ok("session_expired", SessionClock.warning(0L) == SessionWarning.EXPIRED && SessionClock.warning(-5L) == SessionWarning.EXPIRED)
    ok("session_unknown_none", SessionClock.warning(null) == SessionWarning.NONE)
    ok("session_non_jwt_null", SessionClock.expiryEpochSeconds("opaque-token") == null)
    ok("session_bad_exp_null", SessionClock.expiryEpochSeconds(jwt(5L)) == null)
    ok("session_text_expired", SessionClock.warningText(-1L).contains("پایان"))
    ok("session_text_blank_when_unknown", SessionClock.warningText(null) == "")
    ok("session_text_soon_mentions_minutes", SessionClock.warningText(120L).contains("۲"))
    // ۱۱.۲ برچسب روزهای مانده
    ok("days_today", DaysLeft.label(0) == "امروز آخرین روز")
    ok("days_tomorrow", DaysLeft.label(1) == "فردا آخرین روز")
    ok("days_future_fa_digits", DaysLeft.label(5) == "۵ روز مانده", DaysLeft.label(5))
    ok("days_past", DaysLeft.label(-1) == "۱ روز گذشته" && DaysLeft.label(-4) == "۴ روز گذشته")
    ok("days_null_blank", DaysLeft.label(null) == "")
    ok("days_urgent", DaysLeft.urgent(3) && DaysLeft.urgent(-2) && !DaysLeft.urgent(4) && !DaysLeft.urgent(null))
    ok("days_overdue", DaysLeft.overdue(-1) && !DaysLeft.overdue(0) && !DaysLeft.overdue(null))
    // ۱۱.۳ شبکهٔ ماه تقویم
    val g1 = JalaliGrid.cells(0, 31)
    ok("grid_full_month", g1.size == 35 && g1.first() == 1 && g1.last() == null, g1.size)
    val g2 = JalaliGrid.cells(6, 30)
    ok("grid_lead_nulls", g2.size == 42 && g2.take(6).all { it == null } && g2[6] == 1, g2.size)
    ok("grid_cells_multiple_of_seven", (0..6).all { JalaliGrid.cells(it, 31).size % 7 == 0 })
    ok("grid_weeks_shape", JalaliGrid.weeks(g1).size == 5 && JalaliGrid.weeks(g1).all { it.size == 7 })
    ok("grid_empty_month", JalaliGrid.cells(3, 0).isEmpty())
    ok("grid_labels", JalaliGrid.weekdayLabels.size == 7 && JalaliGrid.weekdayLabels.first() == "ش")
    // ۱۱.۴ کش فهرست برای حالت بی‌اتصال
    val cacheItems = listOf(JSONObject("""{"id":1}"""), JSONObject("""{"id":2}"""))
    val raw = ListCache.encode(cacheItems)
    ok("cache_round_trip", ListCache.decode(raw).size == 2 && ListCache.decode(raw)[1].optInt("id") == 2)
    ok("cache_garbage_empty", ListCache.decode("not-json").isEmpty() && ListCache.decode(null).isEmpty() && ListCache.decode("").isEmpty())
    ok("cache_key_plain", ListCache.key("DOCUMENTS", 2) == "cache:DOCUMENTS:2")
    ok("cache_key_extra", ListCache.key("LAWS", 1, "چک") == "cache:LAWS:1:چک")
    ok("cache_age_label", ListCache.ageLabel(0) == "همین حالا" && ListCache.ageLabel(7) == "۷ دقیقه پیش" && ListCache.ageLabel(120) == "۲ ساعت پیش" && ListCache.ageLabel(3000) == "۲ روز پیش")
    ok("fa_digits", faDigits(1404) == "۱۴۰۴" && faDigits(-3) == "-۳")
    println("----")
    println("pass=$pass fail=$fail")
    if (fail > 0) System.exit(1)
}
