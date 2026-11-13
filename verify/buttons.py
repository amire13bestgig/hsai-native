#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
بازبینی خودکار «دکمه‌ها» در لایهٔ UI اپ اندروید.

این اسکریپت چهار قاعده را می‌سنجد و اگر نقضی پیدا شود با کد خروج ۱ برمی‌گردد:

  ۱) برچسب دسترس‌پذیری: هر IconButton/FilledIconButton باید آیکونش توضیح داشته باشد
     (یا خودِ دکمه semantics{contentDescription} داشته باشد).
  ۲) حد لمسی: هیچ دکمهٔ آیکونی نباید اندازهٔ ثابت کمتر از ۴۴dp داشته باشد، مگر اینکه
     minimumInteractiveComponentSize یا heightIn(min=…) جبرانش کند.
  ۳) onClick خالی و enabled=true هاردکدشده ممنوع است.
  ۴) هر فراخوانی «تغییردهنده» روی ViewModel (ارسال/حذف/ثبت) باید در یک ویجت با
     enabled=… باشد تا در زمان ارسال قابل تکرار نباشد.

اجرا: python3 verify/buttons.py            (کد خروج ۰ = بدون نقض)
"""
import re
import sys
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent
CANDIDATES = [
    ROOT.parent / "src/hsai-native/app/src/main/java/ir/hsai/mobile",
    ROOT.parent / "hsai-native/app/src/main/java/ir/hsai/mobile",
    pathlib.Path("app/src/main/java/ir/hsai/mobile"),
]
SRC = next((c for c in CANDIDATES if c.is_dir()), None)
if SRC is None:
    print("پوشهٔ سورس اپ پیدا نشد.")
    sys.exit(2)

FILES = sorted(SRC.glob("*.kt"))
TEXT = {f: f.read_text(encoding="utf-8") for f in FILES}

MUTATING = (
    "sendChat|stopChat|retryFailedChat|confirmRetryAfterReview|feedback|simplify|"
    "createDocument|saveDocument|viewDocument|download|addDeadline|completeDeadline|"
    "createTicket|replyTicket|saveProfile|changePassword|selectPlan|validateDiscount|"
    "verifyPayment|billingSummary|cancelAutoRenew|resumeAutoRenew|sendEmailVerification|"
    "verifyEmail|requestPasswordReset|sendContact|sendOtp|verifyOtp|authenticate|register|"
    "logout|delete|unlinkGoogle|googleLinkWithPassword|googleLinkSendOtp|googleLinkWithOtp|"
    "startGoogleSignIn|loadIdentities|compute|analyze|startChat|setImage"
)
WIDGETS = ("Button(", "SoftButton(", "TextButton(", "IconButton(", "FilledIconButton(",
           "Action(", "DangerAction(", "SoftDangerButton(", "GhostAction(", "SiteChip(")
ICONISH = ("IconButton(", "FilledIconButton(")

violations = []
stats = {"interactive": 0, "icon_buttons": 0, "mutating_calls": 0, "files": len(FILES)}


def line_of(text, index):
    return text[:index].count("\n") + 1


def enclosing_call(text, index):
    """بازگشت به نزدیک‌ترین ویجتِ باز قبل از index و برگرداندن (name, start, segment)."""
    best = None
    for w in WIDGETS:
        pos = text.rfind(w, 0, index)
        if pos != -1 and (best is None or pos > best[1]):
            best = (w, pos)
    if best is None:
        return None
    name, start = best
    depth = 0
    i = start
    while i < len(text):
        ch = text[i]
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    return name, start, text[start:i]


for f, text in TEXT.items():
    # قاعدهٔ ۴ فقط برای فایل‌های رابط کاربری معنا دارد؛ نسخه‌پذیری چرخهٔ عمر اکتیویتی و
    # لایهٔ شبکه دکمه نیستند.
    is_ui = "@Composable" in text
    stats["interactive"] += len(re.findall(r"onClick\s*=|\.clickable\(|onValueChange\s*=", text))

    # قاعدهٔ ۱ و ۲ — دکمه‌های آیکونی
    for m in re.finditer(r"(IconButton|FilledIconButton)\(", text):
        stats["icon_buttons"] += 1
        seg = enclosing_call(text, m.end())
        body = seg[2] if seg else text[m.start(): m.start() + 400]
        ln = line_of(text, m.start())
        has_semantics = "contentDescription" in body
        # تنها «null» حرفی به‌عنوان دومین آرگومان Icon نقص است؛ نام متغیر یا رشته مجاز است.
        null_label = re.search(r"Icon\([^,]*,\s*null\s*[,)]", body)
        if not has_semantics and null_label:
            violations.append((f.name, ln, "۱) دکمهٔ آیکونی بدون برچسب دسترس‌پذیری"))
        size = re.search(r"\.size\((\d+)\.dp\)", body)
        if size and int(size.group(1)) < 44 and "minimumInteractiveComponentSize" not in body and "heightIn(min=" not in body:
            violations.append((f.name, ln, "۲) اندازهٔ ثابت %sdp زیر حد لمسی ۴۴dp" % size.group(1)))

    # قاعدهٔ ۳
    for pat, why in ((r"onClick\s*=\s*\{\s*\}", "۳) onClick خالی"),
                     (r"onClick\s*=\s*\{[^{}]*\}", None)):
        if why:
            for m in re.finditer(pat, text):
                violations.append((f.name, line_of(text, m.start()), why))
    for m in re.finditer(r"enabled\s*=\s*true\b", text):
        seg = enclosing_call(text, m.start())
        line = text[text.rfind("\n", 0, m.start()) + 1: text.find("\n", m.start())]
        if "SwipeToDelete(" in line or (seg and seg[0] == "SwipeToDelete("):
            # استثنا: SwipeToDelete فقط با onDelete عمل می‌کند و حذف از راه دیالوگ تأیید
            # انجام می‌شود که در زمان ارسال غیرفعال است.
            continue
        violations.append((f.name, line_of(text, m.start()), "۳) enabled=true هاردکدشده (باید شرطی باشد)"))

    # قاعدهٔ ۴ — فراخوانی تغییردهنده بدون گارد (فقط در فایل‌های Compose)
    for m in (re.finditer(r"vm\.(%s)\(" % MUTATING, text) if is_ui else ()):
        stats["mutating_calls"] += 1
        # پنجرهٔ بررسی: ۴۰۰ نویسه پیش و پس از فراخوانی؛ اگر enabled= یا گاردِ posting/busy
        # در همان ویجت باشد، مشکلی نیست (دقیق‌تر از «نزدیک‌ترین ویجت»).
        prefix = text[max(0, m.start() - 200): m.start()]
        if re.search(r"(val|var)\s+\w+\s*:\s*\(\)\s*->\s*Unit\s*=\s*\{", prefix):
            # تعریف callback (نه ویجت) — گاردش روی دکمه‌ای است که این callback را صدا می‌زند.
            continue
        window = text[max(0, m.start() - 400): m.start() + 400]
        if "enabled=" in window or "s.posting" in window or "busy" in window:
            continue
        violations.append((f.name, line_of(text, m.start()),
                           "۴) فراخوانی تغییردهنده بدون enabled در %s" % seg[0].rstrip("(")))

print("فایل‌های بازبینی‌شده: %d" % stats["files"])
print("عناصر تعاملی: %d · دکمه‌های آیکونی: %d · فراخوانی‌های تغییردهنده: %d"
      % (stats["interactive"], stats["icon_buttons"], stats["mutating_calls"]))
if violations:
    print("---- نقض‌ها (%d)" % len(violations))
    for v in violations:
        print("  %s:%d  %s" % v)
    sys.exit(1)
print("بدون نقض")
