/**
 * آزمون ماشینی ورود با گوگل — دو جریان جدا:
 *
 *  ۱) وب‌سایت: باید **دقیقاً** به حالت اصلی برگشته باشد
 *     (`routes/auth.js` هیچ اشاره‌ای به اپ، `app_return`، deep link یا مترجم ریدایرکت نداشته باشد).
 *  ۲) اپ بومی: مرورگر → `/api/auth/google/app/start` → گوگل → `/api/auth/google/app/callback`
 *     → `hsai://auth/google?handoff=…` و سپس `POST /auth/google/handoff` در API.
 *
 * اجرا: node verify/server-handoff.mjs
 * نیازمند سورس سایت: SITE_ROOT (پیش‌فرض /home/user/src/hsai). بدون شبکه و بدون دیتابیس.
 */
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const HERE = path.dirname(new URL(import.meta.url).pathname);
const CANDIDATES = [
  process.env.SITE_ROOT,
  '/home/user/src/hsai',
  path.resolve(HERE, '../site'),
  path.resolve(HERE, '../hsai')
].filter(Boolean);
const SITE = CANDIDATES.find((p) => fs.existsSync(path.join(p, 'server/services/appGoogleFlow.js')));
if (!SITE) {
  console.error('سورس سایت پیدا نشد؛ SITE_ROOT را تنظیم کنید.');
  process.exit(2);
}
const load = (rel) => import(pathToFileURL(path.join(SITE, rel)).href);

const flow = await load('server/services/appGoogleFlow.js');
const handoffSvc = await load('server/services/appAuthHandoff.js');
const { signUserToken } = await load('server/middleware/auth.js');

let pass = 0;
const fail = [];
function check(name, fn) {
  try {
    fn();
    pass += 1;
  } catch (e) {
    fail.push(`${name} → ${e.message}`);
  }
}

const NONCE = 'A1b2C3d4E5f6G7h8I9j0K1l2';
const OTHER_NONCE = 'Z9y8X7w6V5u4T3s2R1q0P9o8';

// ── ۱) وب‌سایت در حالت اصلی ───────────────────────────────────────────────
const authSrc = fs.readFileSync(path.join(SITE, 'server/routes/auth.js'), 'utf8');
check('وب: هیچ پرچم اپ در start نیست', () => {
  assert.ok(!authSrc.includes('app_return'), 'app_return در auth.js مانده');
  assert.ok(!authSrc.includes('app_nonce'), 'app_nonce در auth.js مانده');
});
check('وب: مترجم deep link حذف شده است', () => {
  assert.ok(!authSrc.includes('installAppRedirect'), 'installAppRedirect مانده');
  assert.ok(!authSrc.includes('translateWebAuthRedirect'), 'translateWebAuthRedirect مانده');
  assert.ok(!authSrc.includes('hsai://'), 'deep link اپ در مسیر وب مانده');
});
check('وب: state فقط next/redirectUri/nonce/ts دارد', () => {
  assert.ok(/signOAuthState\(\{\s*next: nextPath,\s*redirectUri,\s*nonce:/.test(authSrc), 'شکل state وب تغییر کرده');
  assert.ok(!/app: appFlow/.test(authSrc), 'پرچم app در state وب مانده');
});
check('وب: callback طبق اصل، state را داخل هندلر می‌خواند', () => {
  const callback = authSrc.slice(authSrc.indexOf("router.get('/google/callback'"));
  const head = callback.slice(0, callback.indexOf('const code'));
  assert.ok(!head.includes('verifyOAuthState'), 'خواندن state به بالای هندلر منتقل شده');
  assert.ok(callback.includes('const stateData = verifyOAuthState(String(req.query.state'),
    'خواندن state در callback نیست');
});
check('وب: بازگشت به #google_token و google_error دست‌نخورده است', () => {
  assert.ok(authSrc.includes('google_token: signUserToken(resolved.user)'), 'مسیر موفق وب تغییر کرده');
  assert.ok(authSrc.includes("google_link_token: resolved.linkTicket"), 'مسیر اتصال حساب وب تغییر کرده');
});
check('وب: هیچ ایمپورت اپ در auth.js نیست', () => {
  assert.ok(!authSrc.includes('appAuthHandoff'), 'ایمپورت appAuthHandoff مانده');
  assert.ok(!authSrc.includes('readUserToken'), 'ایمپورت readUserToken مانده');
});

// ── ۲) سرویس جریان اپ ─────────────────────────────────────────────────────
check('اپ: nonce معتبر پذیرفته می‌شود', () => {
  assert.equal(flow.isValidAppNonce(NONCE), true);
  assert.equal(flow.normalizeAppNonce('  ' + NONCE + '  '), NONCE);
});
check('اپ: nonce کوتاه/نامعتبر رد می‌شود', () => {
  assert.equal(flow.isValidAppNonce('short'), false);
  assert.equal(flow.isValidAppNonce(''), false);
  assert.equal(flow.isValidAppNonce('a'.repeat(65)), false);
  assert.equal(flow.normalizeAppNonce('has space 1234567890123456'), '');
});
check('اپ: redirect_uri روی API است نه صفحهٔ وب', () => {
  assert.equal(flow.appRedirectUri('https://hsai.ir'), 'https://hsai.ir/api/auth/google/app/callback');
  assert.equal(flow.appRedirectUri('https://hsai.ir/'), 'https://hsai.ir/api/auth/google/app/callback');
  assert.ok(!flow.appRedirectUri('https://hsai.ir').startsWith('https://hsai.ir/auth'));
});
check('اپ: deep link موفق handoff و nonce را می‌آورد', () => {
  const link = flow.appSuccessLink({ handoff: 'aa.bb', nonce: NONCE });
  assert.equal(link, `hsai://auth/google?handoff=aa.bb&nonce=${NONCE}`);
});
check('اپ: deep link خطا پارامترهای خالی را حذف می‌کند', () => {
  const link = flow.appErrorLink({ error: 'لغو شد' });
  // همان کدگذاری URLSearchParams (فاصله → +) که مسیر وب هم استفاده می‌کرد؛
  // اپ با URLDecoder آن را به فاصله برمی‌گرداند.
  assert.equal(link, 'hsai://auth/google?' + new URLSearchParams({ error: 'لغو شد' }).toString());
});
check('اپ: حالت «نیاز به اتصال حساب» با link_token برمی‌گردد', () => {
  const link = flow.appErrorLink({ error: 'link_required', linkToken: 'tk_1', email: 'a***z@example.com', hasPassword: true });
  const qs = new URLSearchParams(link.slice('hsai://auth/google?'.length));
  assert.equal(qs.get('error'), 'link_required');
  assert.equal(qs.get('link_token'), 'tk_1');
  assert.equal(qs.get('email'), 'a***z@example.com');
  assert.equal(qs.get('has_password'), '1');
});
check('اپ: پیش‌فرض deep link همان اسکیم ثبت‌شده در مانیفست است', () => {
  assert.equal(flow.APP_DEEPLINK, 'hsai://auth/google');
});
check('اپ: مسیرهای شروع/بازگشت مطابق توافق مستندشده‌اند', () => {
  assert.equal(flow.APP_START_PATH, '/api/auth/google/app/start');
  assert.equal(flow.APP_CALLBACK_PATH, '/api/auth/google/app/callback');
});

// ── ۳) برگهٔ دست‌دهی (همان مکانیزم قبلی، دست‌نخورده) ──────────────────────
const { createAppHandoff, verifyAppHandoff, APP_HANDOFF_TTL_SECONDS } = handoffSvc;
check('برگهٔ دست‌دهی برای nonce خودش پذیرفته می‌شود', () => {
  const t = createAppHandoff({ userId: 4242, nonce: NONCE });
  const v = verifyAppHandoff(t, NONCE);
  assert.equal(v.ok, true);
  assert.equal(v.userId, 4242);
});
check('nonce دیگر (اپ مهاجم) رد می‌شود', () => {
  const t = createAppHandoff({ userId: 4242, nonce: NONCE });
  const v = verifyAppHandoff(t, OTHER_NONCE);
  assert.equal(v.ok, false);
  assert.match(v.error, /صادر نشده/);
});
check('برگهٔ دست‌کاری‌شده رد می‌شود', () => {
  const t = createAppHandoff({ userId: 4242, nonce: NONCE });
  const [payload, sig] = t.split('.');
  const tampered = Buffer.from(
    JSON.stringify({ ...JSON.parse(Buffer.from(payload, 'base64url').toString()), u: 1 }),
    'utf8'
  ).toString('base64url');
  const v = verifyAppHandoff(`${tampered}.${sig}`, NONCE);
  assert.equal(v.ok, false);
  assert.match(v.error, /امضا/);
});
check('nonce کوتاه/نامعتبر در ساخت برگه رد می‌شود', () => {
  assert.throws(() => createAppHandoff({ userId: 4242, nonce: 'short' }), /nonce/);
});
check('شناسهٔ کاربر نامعتبر در ساخت برگه رد می‌شود', () => {
  assert.throws(() => createAppHandoff({ userId: 0, nonce: NONCE }), /شناسه/);
});
check('عمر برگهٔ دست‌دهی کوتاه است (۱۸۰ ثانیه)', () => {
  assert.equal(APP_HANDOFF_TTL_SECONDS, 180);
});
check('برگه به nonce دیگری قابل‌استفاده نیست (مقید به دستگاه)', () => {
  const t = createAppHandoff({ userId: 9, nonce: NONCE });
  assert.equal(verifyAppHandoff(t, '').ok, false);
});
check('توکن امضاشدهٔ کاربر معتبر است', () => {
  const token = signUserToken({ id: 4242, role: 'user', phone: '09120000000' });
  assert.equal(typeof token, 'string');
  assert.ok(token.split('.').length === 3 || token.length > 20);
});

console.log(`server-handoff: pass=${pass} fail=${fail.length}`);
fail.forEach((f) => console.log('FAIL', f));
if (fail.length) process.exit(1);
