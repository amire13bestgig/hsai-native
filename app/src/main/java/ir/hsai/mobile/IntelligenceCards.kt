@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * «هوشمندی‌های پاسخ» وب‌سایت، به‌صورت کارت‌های بومی.
 *
 * وب‌سایت هنگام پاسخ‌دادن چند کارت ساختاریافته نشان می‌دهد که همه از میدان `verification`
 * پاسخ سرور می‌آیند (سورس: `unifiedChatOrchestrator.js` → `verification`، و رندر در
 * `components/CitizenLegalCards.jsx` + `VerificationBadge.jsx`):
 *
 *   • برچسب حالت پاسخ         (mode / hide_badge)            ← MODE_LABELS
 *   • کارت محاسبهٔ قانونی      (calc_card)                     ← LegalCalcSummaryCard
 *   • کارت آرام‌سازی ابلاغیه   (sana_reassurance)              ← SanaCalmingCard
 *   • خلاصهٔ خودمانی سه‌خطی    (citizen_guide.quick_summary)   ← CitizenQuickSummaryCard
 *   • جعبه‌ابزار عملی          (citizen_guide)                  ← CitizenPracticalToolkit
 *   • کارت ریسک قرارداد        (scan_mode + red_flags)          ← ContractRiskReportCard
 *
 * نسخهٔ قبلی اپ هیچ‌یک را رندر نمی‌کرد و در عوض کل شیء `verification` را به‌صورت
 * «کلید: مقدار» خام چاپ می‌کرد — یعنی همان هوشمندی در API بود ولی در اپ دیده نمی‌شد.
 */

/** برچسب‌های حالت پاسخ — عیناً از `MODE_LABELS` در ChatAgentPage.jsx. */
object AnswerMode {
    private val labels = mapOf(
        "rag" to "مستند به قوانین ایران",
        "calculator_and_rag" to "محاسبه دقیق قانونی",
        "document" to "سند رسمی آماده چاپ",
        "scanner" to "تحلیل ابلاغیه و قرارداد"
    )

    fun label(mode: String): String = labels[mode] ?: labels.getValue("rag")

    /**
     * همان منطق `hideModeBadge` وب: حالت‌های گفتگویی/پرسشی برچسب نمی‌گیرند و
     * پاسخ «rag» بدون استناد و بدون برگه هم برچسب نمی‌گیرد.
     */
    fun hidden(v: JSONObject?, cited: Int, hasDocument: Boolean): Boolean {
        if (v == null) return true
        if (ApiContract.hideBadge(v)) return true
        val mode = ApiContract.mode(v)
        if (mode == "conversational" || mode == "quick_question") return true
        return (mode.isBlank() || mode == "rag") && cited == 0 && !hasDocument
    }
}

@Composable private fun ModePill(text: String, container: Color, content: Color) {
    Surface(color = container, shape = SiteShape.Chip) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall, color = content, fontWeight = FontWeight.Bold)
    }
}

/** برچسب کوچک حالت پاسخ (گوشهٔ سربرگ پاسخ). */
@Composable fun ModeBadge(mode: String) {
    val dark = LocalSiteDark.current
    val pair: Pair<Color, Color> = when (mode) {
        "calculator_and_rag" -> Pair(if (dark) Palette.OkSoftDark else Palette.OkSoftLight, Palette.Ok)
        "document" -> Pair(if (dark) Palette.WarnSoftDark else Palette.WarnSoftLight, if (dark) Palette.Brass300 else Palette.Brass800)
        "scanner" -> Pair(if (dark) Palette.InfoSoftDark else Palette.InfoSoftLight, if (dark) Palette.Navy300 else Palette.Navy600)
        else -> Pair(if (dark) Palette.Slate800 else Palette.Slate100, if (dark) Palette.Slate200 else Palette.Slate700)
    }
    val container = pair.first
    val content = pair.second
    ModePill(AnswerMode.label(mode), container, content)
}

/** سربرگ پاسخ دستیار: نشان در قاب سرمه‌ای + «دستیار هوشمند حق‌سنج» + برچسب حالت. */
@Composable fun AssistantHeader(mode: String?, showMode: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Surface(color = Palette.Navy950, shape = SiteShape.Xl) {
                BrandEmblem(19.dp, modifier = Modifier.padding(3.dp))
            }
            Text("دستیار هوشمند حق‌سنج", style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold, color = Soft.ink())
        }
        if (showMode && !mode.isNullOrBlank()) ModeBadge(mode)
    }
}

/**
 * کارت محاسبهٔ قانونی — معادل `LegalCalcSummaryCard` سایت:
 * سطح navy-900، عنوان برنجی، ردیف‌های label/value.
 */
@Composable fun CalcSummaryCard(card: JSONObject) {
    val items = card.objects("items")
    if (items.isEmpty()) return
    Surface(shape = SiteShape.Xxl, color = Palette.Navy900) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(NativeIcons.Balance, null, Modifier.size(15.dp), tint = Palette.Brass300)
                    Text(card.str("title", "محاسبهٔ قانونی"), style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black, color = Palette.Brass300)
                }
                Surface(color = Color(0x1AFFFFFF), shape = SiteShape.Chip) {
                    Text("نرخ‌های رسمی دادگستری", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall, color = Palette.Slate200)
                }
            }
            items.forEach { item ->
                Surface(color = Color(0x0DFFFFFF), shape = SiteShape.Xl, border = BorderStroke(1.dp, Color(0x1AFFFFFF))) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(item.str("label"), style = MaterialTheme.typography.labelSmall, color = Palette.Slate300)
                        Text(item.str("value"), style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * کارت آرام‌سازی ابلاغیهٔ ثنا — معادل `SanaCalmingCard`: سه ستون
 * «حکم جلب؟ / حضور دادگاه؟ / چقدر وقت دارید؟».
 */
@Composable fun SanaCalmingCard(r: JSONObject) {
    val arrest = r.optJSONObject("arrest_risk")
    val attendance = r.optJSONObject("attendance")
    val deadline = r.str("deadline_summary")
    if (arrest == null && attendance == null && deadline.isBlank()) return
    Surface(shape = SiteShape.Xxl, color = if (LocalSiteDark.current) Color(0xFF10312A) else Color(0xFFEAF8F1),
        border = BorderStroke(2.dp, Palette.Ok.copy(alpha = .35f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(NativeIcons.Check, null, Modifier.size(16.dp), tint = if (LocalSiteDark.current) Palette.OkBright else Palette.Ok)
                Text("در یک نگاه ساده (بدون استرس بخوانید):", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black, color = Soft.ink())
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val cellModifier = Modifier.weight(1f)
                if (arrest != null) TriCell(cellModifier, "۱. آیا حکم جلب فوری دارد؟",
                    arrest.str("badge"), arrest.str("detail"),
                    if (arrest.optBoolean("safe", true)) Palette.Ok else Palette.Brass700)
                if (attendance != null) TriCell(cellModifier, "۲. لازم است حضوری دادگاه بروید؟",
                    attendance.str("badge"), attendance.str("detail"), Palette.Navy700)
                if (deadline.isNotBlank()) TriCell(cellModifier, "۳. چقدر وقت دارید؟", deadline,
                    "شمارش مهلت از «فردای روز ابلاغ در ثنا» آغاز می‌شود.", Palette.Brass700)
            }
        }
    }
}

@Composable private fun TriCell(modifier: Modifier, title: String, badge: String, detail: String, tone: Color) {
    Surface(modifier = modifier.widthIn(min = 150.dp), shape = SiteShape.Xl, color = Soft.card(),
        border = BorderStroke(1.dp, Soft.border())) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Soft.inkMuted(), fontWeight = FontWeight.Bold)
            Text(badge, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold, color = tone)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.labelSmall,
                color = Soft.inkMuted(), lineHeight = MaterialTheme.typography.labelSmall.lineHeight)
        }
    }
}

/** خلاصهٔ خودمانی سه‌خطی — معادل `CitizenQuickSummaryCard`. */
@Composable fun CitizenQuickSummaryCard(qs: JSONObject) {
    val bottom = qs.str("bottom_line"); val move = qs.str("best_move_now"); val cost = qs.str("cost_and_time")
    if (bottom.isBlank() && move.isBlank() && cost.isBlank()) return
    Surface(shape = SiteShape.Xxl, color = if (LocalSiteDark.current) Color(0xFF2A2317) else Palette.Brass50.copy(alpha = .9f),
        border = BorderStroke(1.dp, Palette.Brass300.copy(alpha = .6f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(NativeIcons.AutoAwesome, null, Modifier.size(15.dp), tint = Palette.Brass600)
                Text("خلاصه خودمانی در یک نگاه:", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black, color = Soft.ink())
            }
            SummaryLine("حرف حساب قانون:", bottom, Palette.Brass700)
            SummaryLine("بهترین کار الان:", move, Palette.Ok)
            SummaryLine("هزینه و زمان تقریبی:", cost, Palette.Navy600)
        }
    }
}

@Composable private fun SummaryLine(label: String, value: String, tone: Color) {
    if (value.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = tone)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold, color = Soft.ink())
    }
}

/**
 * جعبه‌ابزار عملی شهروند — معادل `CitizenPracticalToolkit`:
 * هشدار اشتباه‌های مرگبار + تب‌های «اقدام فردا صبح / سنجش مدرک / مقایسهٔ مسیر».
 */
@Composable fun CitizenToolkit(guide: JSONObject) {
    val mistakes = guide.array("deadly_mistakes").mapNotNull { it as? String }.filter { it.isNotBlank() }
    val plan = guide.optJSONObject("action_plan")
    val evidence = guide.optJSONObject("evidence_meter")
    val paths = guide.optJSONObject("path_comparison")
    if (mistakes.isEmpty() && plan == null && evidence == null && paths == null) return
    var tab by remember(guide) { mutableStateOf("action") }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (mistakes.isNotEmpty()) {
            Surface(shape = SiteShape.Xxl, color = if (LocalSiteDark.current) Palette.DangerSoftDark else Color(0xFFFFF1F2),
                border = BorderStroke(1.dp, Palette.Danger.copy(alpha = .3f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("هشدار مهم — مبادا این اشتباه را بکنید:", style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black, color = if (LocalSiteDark.current) Color(0xFFFCA5A5) else Palette.DangerLo)
                    mistakes.forEach { m ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("!", fontWeight = FontWeight.Black, color = Palette.Danger)
                            Text(m, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                                color = if (LocalSiteDark.current) Palette.Slate200 else Palette.Slate800)
                        }
                    }
                }
            }
        }
        Surface(shape = SiteShape.Xxl, color = Soft.card(), border = BorderStroke(1.dp, Soft.border())) {
            Column(Modifier.fillMaxWidth()) {
                FlowRow(Modifier.fillMaxWidth().padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("action" to "اقدام فردا صبح", "evidence" to "سنجش مدرک", "compare" to "مقایسهٔ مسیر")
                        .filter { (key, _) -> when (key) { "action" -> plan != null; "evidence" -> evidence != null; else -> paths != null } }
                        .forEach { (key, label) ->
                            val on = tab == key
                            Surface(onClick = { tab = key }, shape = SiteShape.Xl,
                                color = if (on) Palette.Navy950 else Color.Transparent,
                                contentColor = if (on) Palette.Brass300 else Soft.inkMuted()) {
                                Text(label, Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold,
                                    color = if (on) Palette.Brass300 else Soft.inkMuted())
                            }
                        }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(Soft.hairline()))
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (tab) {
                        "action" -> ActionPlanTab(plan)
                        "evidence" -> EvidenceTab(evidence)
                        else -> PathsTab(paths)
                    }
                }
            }
        }
    }
}

@Composable private fun ActionPlanTab(plan: JSONObject?) {
    if (plan == null) return
    val where = plan.str("where_to_go")
    val budget = plan.str("estimated_budget")
    val checklist = plan.array("folder_checklist").mapNotNull { it as? String }
    if (where.isNotBlank()) LabelledRow("کجا برویم؟", where)
    if (checklist.isNotEmpty()) {
        Text("چه چیزی با خود ببریم؟", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Soft.ink())
        checklist.forEach { item -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(NativeIcons.Check, null, Modifier.size(14.dp), tint = Palette.Ok)
            Text(item, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = Soft.ink())
        } }
    }
    if (budget.isNotBlank()) LabelledRow("هزینهٔ تقریبی", budget)
}

@Composable private fun EvidenceTab(meter: JSONObject?) {
    if (meter == null) return
    val level = meter.str("level")
    val tone = when (level) { "strong" -> Palette.Ok; "weak" -> Palette.Danger; else -> Palette.Brass600 }
    val levelText = when (level) { "strong" -> "مدارک قوی"; "weak" -> "مدارک ضعیف"; else -> "متوسط" }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(color = tone.copy(alpha = .15f), shape = SiteShape.Chip) {
            Text(levelText, Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = tone)
        }
        Text(meter.str("badge"), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold, color = Soft.ink())
    }
    val proofs = meter.array("detected_proofs").mapNotNull { it as? String }
    if (proofs.isNotEmpty()) LabelledRow("مدارکی که دارید", proofs.joinToString("، "))
    if (meter.str("summary").isNotBlank()) Text(meter.str("summary"), style = MaterialTheme.typography.bodySmall, color = Soft.inkMuted())
    if (meter.str("how_to_strengthen").isNotBlank()) LabelledRow("چطور قوی‌ترش کنیم", meter.str("how_to_strengthen"))
}

@Composable private fun PathsTab(paths: JSONObject?) {
    if (paths == null) return
    var shown = false
    paths.keys().forEach { key ->
        val option = paths.optJSONObject(key) ?: return@forEach
        shown = true
        Surface(shape = SiteShape.Xl, color = Soft.well(), border = BorderStroke(1.dp, Soft.border())) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(option.str("title"), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold, color = Soft.ink())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(option.str("cost"), style = MaterialTheme.typography.labelSmall, color = Palette.Brass700)
                    Text(option.str("duration"), style = MaterialTheme.typography.labelSmall, color = Palette.Navy600)
                }
                if (option.str("note").isNotBlank()) Text(option.str("note"),
                    style = MaterialTheme.typography.bodySmall, color = Soft.inkMuted())
            }
        }
    }
    if (!shown) Text("مقایسهٔ مسیری برای این پرونده ثبت نشده است.", style = MaterialTheme.typography.bodySmall, color = Soft.inkMuted())
}

@Composable private fun LabelledRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = Palette.Brass700)
        Text(value, style = MaterialTheme.typography.bodySmall, color = Soft.ink())
    }
}

/** کارت ریسک قرارداد (حالت اسکنر) — معادل `ContractRiskReportCard`. */
@Composable fun ContractRiskCard(riskScore: Int, flags: List<JSONObject>) {
    if (flags.isEmpty()) return
    val high = riskScore >= 60
    val tone = if (high) Palette.Danger else Palette.Brass700
    Surface(shape = SiteShape.Xxl, color = if (high) (if (LocalSiteDark.current) Palette.DangerSoftDark else Color(0xFFFFF1F2))
        else (if (LocalSiteDark.current) Palette.WarnSoftDark else Palette.Amber50),
        border = BorderStroke(1.dp, tone.copy(alpha = .35f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(NativeIcons.DocumentScanner, null, Modifier.size(16.dp), tint = tone)
                Text("بازبینی بندهای پرخطر قرارداد", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black, color = tone)
                Text("ریسک ${fa(riskScore)} از ۱۰۰", style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold, color = tone)
            }
            flags.forEach { flag ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("•", color = tone, fontWeight = FontWeight.Black)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(flag.str("title", flag.str("clause", "بند پرخطر")),
                            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Soft.ink())
                        val detail = flag.str("explanation", flag.str("detail", flag.str("risk")))
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = Soft.inkMuted())
                    }
                }
            }
        }
    }
}

/**
 * راستی‌آزمایی پاسخ — جانشین «چاپ خام JSON».
 * معادل `VerificationBadge` سایت: اگر ممیزی شکست خورده باشد هشدار صریح، وگرنه
 * یک ردیف جمع‌وجور با امکان بازکردن جزئیات مراحل.
 */
@Composable fun VerificationSection(v: JSONObject) {
    if (ApiContract.verificationFailed(v)) {
        val issues = ApiContract.citationIssues(v)
        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = SiteShape.Xxl,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .35f))) {
            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("این پاسخ تأیید نشد", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(if (issues.isEmpty())
                    "استنادهای این پاسخ با متن مواد قانونی تطبیق داده نشد؛ پیش از هر اقدام، متن ماده را از منبع رسمی ببینید."
                else "استنادهای تأییدنشده: " + issues.joinToString("، ") { it.str("reference", it.str("cited_phrase", it.str("citation", "نامشخص"))) },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        return
    }
    if (ApiContract.hideBadge(v)) return
    val stages = v.array("stages").mapNotNull { it as? JSONObject }
    val authority = v.str("competent_authority", v.str("authority"))
    if (stages.isEmpty() && authority.isBlank()) return

    var open by remember(v) { mutableStateOf(false) }
    val corpus = v.optInt("total_checked_corpus", 0)
    Surface(shape = SiteShape.Xxl, color = if (LocalSiteDark.current) Color(0xFF10281F) else Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Palette.Ok.copy(alpha = .3f))) {
        Column(Modifier.fillMaxWidth()) {
            Surface(onClick = { open = !open }, color = Color.Transparent, shape = SiteShape.Xxl) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(NativeIcons.Check, null, Modifier.size(15.dp), tint = Palette.Ok)
                    Text(
                        if (corpus > 0) "راستی‌آزمایی با بانک ${fa(corpus)} سندی قوانین" else "راستی‌آزمایی منطقی و تطبیق با بانک قوانین",
                        Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold, color = if (LocalSiteDark.current) Palette.OkBright else Palette.OkLo)
                    if (v.optInt("corrections_applied", 0) > 0) Text("اصلاح‌شده",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(if (open) NativeIcons.ExpandMore else NativeIcons.ExpandMore, null, Modifier.size(16.dp),
                        tint = if (LocalSiteDark.current) Palette.OkBright else Palette.OkLo)
                }
            }
            if (open) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (authority.isNotBlank()) LabelledRow("مرجع صالح", authority)
                    stages.forEach { stage ->
                        LabelledRow(stage.str("name", stage.str("title", "مرحله")),
                            stage.str("detail", stage.str("result", stage.str("status"))))
                    }
                }
            }
        }
    }
}

/** جعبهٔ «بازنویسی به زبان کاملاً ساده» — معادل `simplified_content` در سایت. */
@Composable fun SimplifiedBox(text: String) {
    Surface(shape = SiteShape.Xxl, color = if (LocalSiteDark.current) Color(0xFF11301F) else Color(0xFFECFDF5),
        border = BorderStroke(2.dp, Palette.Ok.copy(alpha = .4f))) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(NativeIcons.AutoAwesome, null, Modifier.size(15.dp), tint = Palette.Ok)
                Text("بازنویسی به زبان کاملاً ساده و خودمانی:", style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black, color = if (LocalSiteDark.current) Palette.OkBright else Palette.OkLo)
            }
            androidx.compose.foundation.text.selection.SelectionContainer { NativeMarkdown(text) }
        }
    }
}
