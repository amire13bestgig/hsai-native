#!/usr/bin/env python3
"""بازبینی برابری توکن‌های بصری اپ با سایت (tailwind.config.js + src/index.css).

مقادیر مرجع از سورس وب حق‌سنج استخراج شده‌اند و همین‌جا ثابت شده‌اند تا بررسی
روی هر ماشینی و بدون دسترسی به سورس وب هم قابل تکرار باشد.
اجرا: python3 tokens.py
"""
import re, sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
CANDIDATES = [
    os.path.join(HERE, '..', 'src', 'hsai-native', 'app', 'src', 'main', 'java', 'ir', 'hsai', 'mobile'),
    os.path.join(HERE, '..', 'hsai-native', 'app', 'src', 'main', 'java', 'ir', 'hsai', 'mobile'),
]

# --- مرجع: tailwind.config.js سایت
NAVY = {50:'F0F5F8',100:'D9E6EE',200:'B6CFDF',300:'85AFC8',400:'5088AB',500:'336C8F',
        600:'275676',700:'214661',800:'163144',900:'0D2130',950:'07141E'}
BRASS = {50:'FBF8F1',100:'F5EEDC',200:'EAD9B5',300:'DCBE86',400:'CE9F58',500:'C38839',
         600:'B36F2E',700:'955628',800:'794526',900:'633A22'}

# --- مرجع: src/index.css (--soft-*)
SOFT_LIGHT = {'BgLight':'F4F2EC','CardLight':'FBF9F5','Card2Light':'F7F4EE','WellLight':'EFECE4'}
SOFT_DARK  = {'BgDark':'0B1B28','CardDark':'12293A','Card2Dark':'0F2334','WellDark':'0A1825'}

# --- مرجع: کلاس‌های کامپوننت سایت
BUBBLE_USER = [('1B3852'), ('0C1F2E')]      # .soft-bubble-user
RADII = {'Xl':'12','Xxl':'16','Xxxl':'24','Card':'24'}  # rounded-xl/2xl/3xl, .soft-card
HERO = ('0D2130', '07141E')                  # from-navy-900 to-navy-950

fails = []
checks = 0
def ok(name, cond, detail=''):
    global checks
    checks += 1
    if not cond:
        fails.append(f'{name} {detail}')

def find_src():
    for c in CANDIDATES:
        if os.path.isdir(c):
            return c
    return None

src = find_src()
if src is None:
    print('FAIL: پوشهٔ سورس اپ پیدا نشد'); sys.exit(2)

theme = open(os.path.join(src, 'Theme.kt'), encoding='utf-8').read()
design = open(os.path.join(src, 'SiteDesign.kt'), encoding='utf-8').read()
screens = open(os.path.join(src, 'Screens.kt'), encoding='utf-8').read()

# ۱) پالت navy/brass
for scale, ref, prefix in ((NAVY, NAVY, 'Navy'), (BRASS, BRASS, 'Brass')):
    for k, hexv in ref.items():
        pattern = re.compile(prefix + str(k) + r'\s*=\s*Color\(0xFF' + hexv + r'\)')
        ok(f'palette.{prefix}{k}', bool(pattern.search(theme)), f'(expected 0xFF{hexv})')

# ۲) توکن‌های سطح روشن/تیره
for name, hexv in SOFT_LIGHT.items():
    ok(f'soft.{name}', f'val {name}=Color(0xFF{hexv})' in theme, f'(expected 0xFF{hexv})')
for name, hexv in SOFT_DARK.items():
    ok(f'soft.{name}', f'val {name}=Color(0xFF{hexv})' in theme, f'(expected 0xFF{hexv})')

# ۳) گردی‌ها
for name, dp in RADII.items():
    ok(f'radius.{name}', f'val {name} = RoundedCornerShape({dp}.dp)' in theme or f'val {name}=RoundedCornerShape({dp}.dp)' in theme,
       f'(expected {dp}dp)')

# ۴) حباب‌ها: شعاع ۲۲/۹ و گرادیان سرمه‌ای سایت
ok('bubble.user_radius', 'topStart=22.dp,topEnd=22.dp,bottomStart=9.dp,bottomEnd=22.dp' in theme, '(user 22/9)')
ok('bubble.ai_radius', 'topStart=22.dp,topEnd=22.dp,bottomStart=22.dp,bottomEnd=9.dp' in theme, '(ai 22/9)')
for hexv in BUBBLE_USER:
    ok(f'bubble.user_color_{hexv}', f'Color(0xFF{hexv})' in screens, f'(expected 0xFF{hexv})')

# ۵) گرادیان سربرگ
ok('hero.gradient', f'listOf(Palette.Navy900,Palette.Navy950)' in design, '(navy-900 → navy-950)')

# ۶) تایپوگرافی: نسبت خط بدنه ۱.۸۵ سایت
m = re.search(r'bodyMedium=TextStyle\([^)]*fontSize=(\d+)\.sp,lineHeight=(\d+)\.sp', theme)
if m:
    size, lh = int(m.group(1)), int(m.group(2))
    ok('type.body_medium_ratio', abs(lh / size - 1.85) < 0.02, f'(got {lh}/{size})')
else:
    ok('type.body_medium_ratio', False, '(bodyMedium not found)')


# ─────────────────────────────────────────────────────────────────────────────
# ۷) ممیزی UI دور ششم — آیکون‌های سایت، پریمیتیوهای تازه، حد لمسی، دسترس‌پذیری
# ─────────────────────────────────────────────────────────────────────────────
icons = open(os.path.join(src, 'NativeIcons.kt'), encoding='utf-8').read()
shell = open(os.path.join(src, 'WorkspaceShell.kt'), encoding='utf-8').read()
login = open(os.path.join(src, 'LoginScreen.kt'), encoding='utf-8').read()
cards = open(os.path.join(src, 'IntelligenceCards.kt'), encoding='utf-8').read()

# ۷-۱ آیکون‌هایی که سایت دارد و اپ باید داشته باشد (هم‌نام lucide)
SITE_ICONS = ['Wallet','Home','Heart','Briefcase','AlertTriangle','Users','Search','GitCompare',
              'HelpCircle','CalendarClock','Crown','LifeBuoy','ScanSearch','FileSignature',
              'Download','CreditCard','Library','CheckCircle','ShieldAlert','Clock','Scale',
              'AlertCircle','HeartHandshake','ShieldCheck']
for name in SITE_ICONS:
    ok('icon.' + name, re.search(r'val ' + name + r'=icon\("' + name + '"', icons) is not None, '(missing)')
# آیکون‌ها همه خطی و هم‌ضخامت با بقیهٔ اپ‌اند
ok('icon.stroke_only', 'fill=null,stroke=SolidColor(Color.Black),strokeLineWidth=1.65f' in icons)

# ۷-۲ نشان چهاررنگ گوگل (همان SVG صفحهٔ ورود سایت)
for hexv in ('EA4335', '34A853', 'FBBC05', '4285F4'):
    ok('google.mark_' + hexv, f'0xFF{hexv}.toInt()' in icons, '(missing color)')
ok('google.mark_used', 'Image(NativeIcons.GoogleMark' in login)

# ۷-۳ `.hsai-glass-soft` سایت به‌صورت پریمیتیو
ok('glass.soft_light', 'val SoftGlassLight=Color(0xB8F8F7F4)' in design)
ok('glass.soft_dark', 'val SoftGlassDark=Color(0x0DFFFFFF)' in design)
ok('glass.soft_line_light', 'val SoftGlassLineLight=Color(0x120F172A)' in design)
ok('glass.soft_line_dark', 'val SoftGlassLineDark=Color(0x1AFFFFFF)' in design)
ok('glass.soft_param', 'soft:Boolean=false,radiusDp:Int=' in design)
ok('chip.site', 'fun SiteChip(' in design and 'SiteChip(suggestion' in screens)
ok('chip.no_m3_suggestion', 'SuggestionChip(onClick={vm.setDraft' not in screens)

# ۷-۴ کارت‌ها: soft-card و hsai-glass هر دو
ok('card.glass_variant', 'glass:Boolean=false,pad:Int=18,radiusDp:Int=24' in theme)
# دور یازدهم: چیدمان کارت موضوعی به لامبدای مشترک (topicCard) منتقل شد تا ردیف ناقص
# تمام‌عرض شود و height(IntrinsicSize.Min)+fillMaxHeight حذف گردد. قاعده همان است:
# کارت موضوع = hsai-glass با pad=14/radius=16 و برچسب کلیک = عنوان موضوع.
ok('card.topic_is_hsai_glass', 'clickable(onClickLabel=title)' in screens
   and 'glass=true,pad=14,radiusDp=16' in screens
   and 'heightIn(min=88.dp)' in screens)

# دور یازدهم: ردیف ناقص نباید کارت «یتیم» بسازد (هم در کارت‌های موضوعی، هم در AdaptiveCards)
ok('cards.no_orphan_row', 'if(row.size==1&&columns>1) render(row[0],Modifier.fillMaxWidth())' in shell
   and 'if(row.size==1) topicCard(row[0],Modifier.fillMaxWidth())' in screens)
# دور یازدهم: صفحهٔ پشتیبانی فقط یک حالت خالی دارد (قبلاً دو تای روی هم می‌آمد)
ok('tickets.single_empty_state', screens.count('EmptyState("هنوز درخواستی ثبت نکرده‌اید"')==1
   and 'EmptyState("درخواستی ثبت نشده"' not in screens)
# دور یازدهم: سرتیتر واقعی طرح‌ها و دکمهٔ «تاریخچهٔ پرداخت‌های من» به‌جای TextButton لخت
ok('plans.heading_and_history', 'Text(if(s.planCycle=="yearly")"طرح‌های سالانه"else "طرح‌های ماهانه"' in screens
   and 'Text("تاریخچهٔ پرداخت‌های من")' in screens and 'Text("تاریخچه پرداخت")' not in screens)

# ۷-۵ آیکون موضوعی هر کارت زندگی، به ترتیب سایت
TOPICS = ['Wallet','Home','Heart','Briefcase','AlertTriangle','Users']
idx = [screens.find('NativeIcons.' + n) for n in TOPICS]
ok('home.topic_icons', all(i > 0 for i in idx) and idx == sorted(idx), idx)

# ۷-۶ آیکون صفحه‌ها = آیتم متناظر فهرست سایت
for pair in ('Screen.PLANS->NativeIcons.Crown', 'Screen.DEADLINES->NativeIcons.CalendarClock',
             'Screen.TICKETS->NativeIcons.LifeBuoy', 'Screen.SCANNER->NativeIcons.ScanSearch',
             'Screen.DOCUMENTS->NativeIcons.FileSignature'):
    ok('symbol.' + pair.split('->')[0], pair in screens, pair)

# ۷-۷ رنگ‌های دکمهٔ خطر/خروج، عیناً از سایت
ok('danger.bg', 'Color(0xFFE11D48)' in screens, '(rose-600)')
for name, hexv in (('border', 'FECDD3'), ('bg', 'FFF1F2'), ('text', 'BE123C'), ('dark_text', 'FDA4AF')):
    ok('soft_danger.' + name, f'0xFF{hexv}' in screens, f'(rose {hexv})')

# ۷-۸ حد لمسی ۴۴dp در نوار بالا (`.hsai-tap`) و لینک سایت
ok('touch.no_40dp', '.size(40.dp)' not in shell)
ok('touch.topbar_44', shell.count('.size(44.dp)') >= 2)
ok('touch.site_link', 'heightIn(min=44.dp).wrapContentHeight(Alignment.CenterVertically).clickable(role=Role.Button)' in shell)
ok('touch.forgot_password', 'if(mode=="login")TextButton(onClick={vm.openDialog("forgot","بازیابی رمز عبور")},modifier=Modifier.heightIn(min=44.dp)' in login)

# ۷-۹ دیالوگ‌ها: سربرگ navy-900 + شعاع ۳٫xl سایت
ok('dialog.header_band', 'background(Palette.Navy900)' in screens)
ok('dialog.radius', 'heightIn(max=720.dp),shape=RoundedCornerShape(24.dp)' in screens)

# ۷-۱۰ بارگذاری = اسکلت تپنده (animate-pulse سایت) نه اسپینر
ok('loading.skeleton', 'var alpha by remember{mutableStateOf(.35f)}' in screens)
ok('loading.no_spinner', 'CircularProgressIndicator(Modifier.size(27.dp)' not in screens)

# ۷-۱۱ دسترس‌پذیری
ok('a11y.heading_page', 'headlineSmall,fontWeight=FontWeight.Bold,modifier=Modifier.semantics{heading()}' in screens)
ok('a11y.heading_hero', 'modifier=Modifier.semantics{heading()}' in design)
ok('a11y.field_error', 'stateDescription=error' in screens)
ok('a11y.choice_role', 'role=Role.DropdownList' in screens)
ok('a11y.dialog_title', 'titleSmall,fontWeight=FontWeight.Black,color=Color.White' in screens)

# ۷-۱۲ تیتر برنجی درخشان ورود
ok('shimmer.brush', 'val GoldShimmerBrush=Brush.linearGradient' in design)
ok('shimmer.auth', 'GoldShimmerText("همراه با بانک قوانین' in design)

# ۷-۱۳ ایمیل/تلفن به‌صورت LTR و monospace (مثل سایت)
ok('account.ltr_contacts', 'TextStyle(textDirection=TextDirection.Ltr,fontFamily=FontFamily.Monospace)' in screens)

# ۷-۱۴ آیکون کارت‌های هوشمند مطابق سایت
ok('cards.heart_handshake', 'NativeIcons.HeartHandshake' in cards)
ok('cards.scale', 'Icon(NativeIcons.Scale' in cards)
# ۷-۱۵ حالت خالی هر صفحه آیکون موضوعی همان صفحه را دارد
ok('empty_state.icons', sum(1 for m in re.finditer(r'EmptyState\("[^"]*","[^"]*",NativeIcons\.', screens)) >= 6)
ok('empty_state.fallback', 'icon:ImageVector=NativeIcons.FolderOpen' in screens)
# ۷-۱۶ ورودی‌ها عیناً مثل سایت: bg-white / dark:#10263a، حاشیهٔ slate-300 / white10، فوکوس brass-400
ok('input.light_bg', 'unfocusedContainerColor=if(LocalSiteDark.current)Color(0xFF10263A) else Color.White' in screens)
ok('input.dark_bg', '0xFF10263A' in screens)
ok('input.border_light', 'else Palette.Slate300' in screens)
ok('input.border_dark', 'Color(0x1AFFFFFF)' in screens)
ok('input.focus_brass', 'focusedBorderColor=Palette.Brass400' in screens)
ok('input.weight_rule', 'fontWeight=if(multiline)FontWeight.Normal else FontWeight.Bold' in screens)
ok('input.login_white', 'unfocusedContainerColor=Color.White' in login)
ok('input.no_false_well_claim', '«چاه» فرورفته (.soft-well)' not in screens)

print(f'{checks - len(fails)}/{checks} parity checks passed')
for f in fails:
    print('FAIL', f)
sys.exit(1 if fails else 0)

