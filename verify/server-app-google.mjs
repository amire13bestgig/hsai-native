/**
 * آزمون ماشینی HTTP دو جریان ورود با گوگل (بدون دیتابیس و بدون شبکه).
 *
 *   • `/api/auth/google/start` و `/api/auth/google/callback`  → رفتار وب‌سایت (ریدایرکت داخلی /auth…)
 *   • `/api/auth/google/app/start` و `/api/auth/google/app/callback` → جریان اپ (همیشه hsai:// یا accounts.google.com)
 *
 * اجرا: node verify/server-app-google.mjs   (SITE_ROOT اختیاری)
 */
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
const SITE = CANDIDATES.find((p) => fs.existsSync(path.join(p, 'server/routes/appGoogleAuth.js')));
if (!SITE) {
  console.error('سورس سایت پیدا نشد؛ SITE_ROOT را تنظیم کنید.');
  process.exit(2);
}
const load = (rel) => import(pathToFileURL(path.join(SITE, rel)).href);

const express = (await load('node_modules/express/index.js')).default;
const webAuth = (await load('server/routes/auth.js')).default;
const appAuth = (await load('server/routes/appGoogleAuth.js')).default;

const app = express();
app.use(express.json({ limit: '1mb' }));
// همان ترتیب server/app.js: روتر اپ پیش از روتر وب.
app.use('/api/auth', appAuth);
app.use('/api/auth', webAuth);

const server = app.listen(0);
await new Promise((r) => server.once('listening', r));
const base = `http://127.0.0.1:${server.address().port}`;

let pass = 0;
const fail = [];
async function check(name, fn) {
  try {
    await fn();
    pass += 1;
  } catch (e) {
    fail.push(`${name} → ${e.message}`);
  }
}
function assert(cond, msg) {
  if (!cond) throw new Error(msg);
}
async function get(path_) {
  const res = await fetch(base + path_, { redirect: 'manual', headers: { Accept: 'text/html' } });
  return { status: res.status, location: res.headers.get('location') || '', text: await res.text() };
}

const NONCE = 'A1b2C3d4E5f6G7h8I9j0K1l2';

// ── جریان اپ ───────────────────────────────────────────────────────────────
await check('اپ: بدون nonce پاسخ ۴۰۰ روشن می‌دهد (نه ریدایرکت به صفحهٔ وب)', async () => {
  const r = await get('/api/auth/google/app/start');
  assert(r.status === 400, `status=${r.status}`);
  const body = JSON.parse(r.text);
  assert(body.ok === false && /nonce/i.test(body.error), r.text.slice(0, 120));
});

await check('اپ: nonce کوتاه رد می‌شود', async () => {
  const r = await get('/api/auth/google/app/start?nonce=short');
  assert(r.status === 400, `status=${r.status}`);
});

await check('اپ: شروع ورود هرگز به صفحهٔ وب نمی‌رود (فقط گوگل یا deep link)', async () => {
  const r = await get('/api/auth/google/app/start?nonce=' + NONCE);
  assert(r.status === 302, `status=${r.status}`);
  const okTarget =
    r.location.startsWith('https://accounts.google.com/') || r.location.startsWith('hsai://auth/google');
  assert(okTarget, `location=${r.location}`);
  assert(!r.location.startsWith('/auth'), `به صفحهٔ وب ریدایرکت شد: ${r.location}`);
});

await check('اپ: redirect_uri گوگل روی مسیر API اپ است', async () => {
  const r = await get('/api/auth/google/app/start?nonce=' + NONCE);
  if (!r.location.startsWith('https://accounts.google.com/')) return; // بدون تنظیمات گوگل
  const target = new URL(r.location);
  assert(
    target.searchParams.get('redirect_uri')?.endsWith('/api/auth/google/app/callback'),
    target.searchParams.get('redirect_uri')
  );
  assert(target.searchParams.get('state')?.includes('.'), 'state امضاشده نیست');
});

await check('اپ: بازگشت بدون state به deep link خطا می‌رود', async () => {
  const r = await get('/api/auth/google/app/callback');
  assert(r.status === 302, `status=${r.status}`);
  assert(r.location.startsWith('hsai://auth/google?error='), r.location);
});

await check('اپ: state نامعتبر هم deep link می‌گیرد (نه صفحهٔ وب)', async () => {
  const r = await get('/api/auth/google/app/callback?code=abc&state=forged.signature');
  assert(r.status === 302, `status=${r.status}`);
  assert(r.location.startsWith('hsai://auth/google?'), r.location);
  assert(r.location.includes('error='), r.location);
});

await check('اپ: لغو توسط کاربر در گوگل هم به اپ برمی‌گردد', async () => {
  const r = await get('/api/auth/google/app/callback?error=access_denied&state=forged.signature');
  assert(r.status === 302 && r.location.startsWith('hsai://auth/google?error='), r.location);
});

// ── جریان وب (باید مثل اصل باشد) ──────────────────────────────────────────
/** state امضاشده را بدون اعتبارسنجی امضا باز می‌کند (فقط برای بازرسی محتوا). */
function statePayload(location) {
  const state = new URL(location).searchParams.get('state') || '';
  const payload = state.split('.')[0];
  return JSON.parse(Buffer.from(payload, 'base64url').toString('utf8'));
}

await check('وب: /google/start به گوگل می‌رود و callback وب را می‌خواهد', async () => {
  const r = await get('/api/auth/google/start');
  assert(r.status === 302, `status=${r.status}`);
  assert(r.location.startsWith('https://accounts.google.com/'), r.location);
  const target = new URL(r.location);
  assert(
    target.searchParams.get('redirect_uri')?.endsWith('/api/auth/google/callback'),
    target.searchParams.get('redirect_uri')
  );
  assert(!r.location.includes('hsai://'), 'مسیر وب به deep link اپ آلوده شده است');
});

await check('وب: state فقط next/redirectUri/nonce/ts دارد (بدون پرچم اپ)', async () => {
  const r = await get('/api/auth/google/start');
  const payload = statePayload(r.location);
  assert(JSON.stringify(Object.keys(payload).sort()) === JSON.stringify(['next', 'nonce', 'redirectUri', 'ts']),
    Object.keys(payload).join(','));
  assert(!('app' in payload) && !('appNonce' in payload), 'پرچم اپ در state وب مانده است');
});

await check('وب: پرچم app_return/app_nonce روی مسیر وب هیچ اثری ندارد', async () => {
  const plain = statePayload((await get('/api/auth/google/start')).location);
  const flagged = statePayload(
    (await get('/api/auth/google/start?app_return=1&app_nonce=' + NONCE)).location
  );
  assert(JSON.stringify(Object.keys(flagged).sort()) === JSON.stringify(Object.keys(plain).sort()),
    Object.keys(flagged).join(','));
  assert(!('app' in flagged) && !('appNonce' in flagged), 'پرچم اپ روی جریان وب اثر گذاشت');
  assert(flagged.next === plain.next && flagged.redirectUri === plain.redirectUri,
    'رفتار وب با پرچم اپ تغییر کرد');
});

await check('وب: /google/callback بدون state به /auth برمی‌گردد', async () => {
  const r = await get('/api/auth/google/callback');
  assert(r.status === 302, `status=${r.status}`);
  assert(r.location.startsWith('/auth?google_error='), r.location);
  assert(!r.location.startsWith('hsai://'), 'مسیر وب به deep link اپ آلوده شده است');
});

server.close();
console.log(`server-app-google: pass=${pass} fail=${fail.length}`);
fail.forEach((f) => console.log('FAIL', f));
if (fail.length) process.exit(1);
