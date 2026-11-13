/**
 * آزمون ماشینی «دروازهٔ مسیرهای قدیمی» در /api/v1.
 *
 * اجرا:  node verify/server-gateway.mjs
 * نیازمند سورس سایت: متغیر SITE_ROOT (پیش‌فرض: /home/user/src/hsai یا ../site).
 *
 * چرا این آزمون؟ چون ادعای «اپ حالا به بازیابی رمز/تأیید ایمیل/فرم تماس/
 * لغو اشتراک دسترسی دارد» باید با یک درخواست واقعی HTTP روی همان روتر v1
 * تأیید شود، نه با خواندن کد.
 *
 * این آزمون به دیتابیس نیاز ندارد: نبود DATABASE_URL باعث می‌شود مسیرها
 * خطای ۵۰۰ بدهند؛ همان ۵۰۰ هم کافی است تا ثابت شود مسیر «mount شده» و
 * پاسخ با پوشش v1 برگشته است (پیش‌تر همین درخواست‌ها ENDPOINT_NOT_FOUND
 * می‌گرفتند).
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
const SITE = CANDIDATES.find((p) => fs.existsSync(path.join(p, 'server/routes/v1.js')));
if (!SITE) {
  console.error('سورس سایت پیدا نشد؛ SITE_ROOT را تنظیم کنید.');
  process.exit(2);
}
const load = (rel) => import(pathToFileURL(path.join(SITE, rel)).href);

const express = (await load('node_modules/express/index.js')).default;
const v1Routes = (await load('server/routes/v1.js')).default;

const app = express();
app.use(express.json({ limit: '1mb' }));
app.use('/api/v1', v1Routes);
app.use((req, res) => {
  if (req.originalUrl?.startsWith('/api/v1')) {
    return res.status(404).json({ ok: false, error: { code: 'ENDPOINT_NOT_FOUND', message: 'مسیر یافت نشد.' } });
  }
  res.status(404).json({ error: 'not found' });
});

const server = app.listen(0);
await new Promise((r) => server.once('listening', r));
const base = `http://127.0.0.1:${server.address().port}/api/v1`;

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
async function call(path_, { method = 'GET', body, token } = {}) {
  const res = await fetch(base + path_, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });
  const text = await res.text();
  let json = null;
  try {
    json = JSON.parse(text);
  } catch {
    json = { raw: text.slice(0, 200) };
  }
  return { status: res.status, json };
}
function assertEnvelope(r, { notStatus } = {}) {
  if (notStatus && r.status === notStatus) {
    throw new Error(`مسیر mount نشده است (status=${r.status}, code=${r.json?.error?.code})`);
  }
  if (r.status >= 400) {
    if (r.json.ok !== false) throw new Error('پاسخ خطا پوشش v1 ندارد: ' + JSON.stringify(r.json).slice(0, 160));
    if (!r.json.error || typeof r.json.error.message !== 'string' || !r.json.error.message)
      throw new Error('پیام خطای فارسی در پوشش نیست: ' + JSON.stringify(r.json).slice(0, 160));
    if (typeof r.json.error.code !== 'string' || !r.json.error.code)
      throw new Error('کد خطا در پوشش نیست: ' + JSON.stringify(r.json).slice(0, 160));
  } else if (r.json.ok !== true || !('data' in r.json)) {
    throw new Error('پاسخ موفق پوشش v1 ندارد: ' + JSON.stringify(r.json).slice(0, 160));
  }
}

await check('بازیابی رمز (password/forgot) از مسیر قدیمی زیر v1 سرو می‌شود', async () => {
  const r = await call('/auth/password/forgot', { method: 'POST', body: {} });
  assertEnvelope(r, { notStatus: 404 });
});

await check('تأیید ایمیل (email/send-verification) سرو می‌شود', async () => {
  const r = await call('/auth/email/send-verification', { method: 'POST', body: {} });
  assertEnvelope(r, { notStatus: 404 });
});

await check('فرم تماس عمومی (auth/contact) سرو می‌شود', async () => {
  const r = await call('/auth/contact', { method: 'POST', body: {} });
  assertEnvelope(r, { notStatus: 404 });
});

await check('صورت‌حساب کاربر (billing/my-billing) نیازمند ورود است و ۴۰۱ پوشش‌دار می‌دهد', async () => {
  const r = await call('/billing/my-billing');
  assertEnvelope(r, { notStatus: 404 });
  if (r.status !== 401) throw new Error('بدون توکن باید ۴۰۱ باشد، دریافت شد ' + r.status);
});

await check('لغو تمدید خودکار (billing/cancel) بدون ورود رد می‌شود', async () => {
  const r = await call('/billing/cancel', { method: 'POST', body: {} });
  assertEnvelope(r, { notStatus: 404 });
  if (r.status !== 401) throw new Error('بدون توکن باید ۴۰۱ باشد، دریافت شد ' + r.status);
});

await check('بازگرداندن تمدید خودکار (billing/resume) بدون ورود رد می‌شود', async () => {
  const r = await call('/billing/resume', { method: 'POST', body: {} });
  assertEnvelope(r, { notStatus: 404 });
  if (r.status !== 401) throw new Error('بدون توکن باید ۴۰۱ باشد، دریافت شد ' + r.status);
});

await check('روابط مواد قانونی (/laws/:id/relations) مسیر مستقل v1 دارد', async () => {
  // بدون دیتابیس، هندلر همین مسیر پاسخ فارسی «ماده قانونی یافت نشد» می‌دهد؛
  // نکتهٔ مهم این است که پاسخ از ENDPOINT_NOT_FOUND نیامده باشد.
  const r = await call('/laws/12/relations');
  assertEnvelope(r);
  if (r.json.error.code === 'ENDPOINT_NOT_FOUND')
    throw new Error('مسیر relations در v1 تعریف نشده است.');
  if (!/ماده قانونی/.test(r.json.error.message))
    throw new Error('پیام از هندلر relations نیامده است: ' + r.json.error.message);
});

await check('شناسهٔ نامعتبر ماده در relations باز هم ۴۰۴ پوشش‌دار است', async () => {
  const r = await call('/laws/0/relations');
  if (r.status !== 404) throw new Error('باید ۴۰۴ باشد، دریافت شد ' + r.status);
  assertEnvelope(r);
});

await check('دروازهٔ دست‌دهی گوگل وجود دارد و مسیر نیست (نه ۴۰۴)', async () => {
  const r = await call('/auth/google/handoff', {
    method: 'POST',
    body: { handoff: 'x.y', nonce: 'A1b2C3d4E5f6G7h8I9j0K1l2' }
  });
  assertEnvelope(r, { notStatus: 404 });
});

await check('مسیرهای صریح v1 دست‌نخورده مانده‌اند (auth/login همان رفتار قبلی)', async () => {
  const r = await call('/auth/login', { method: 'POST', body: {} });
  if (r.status !== 400) throw new Error('باید ۴۰۰ باشد، دریافت شد ' + r.status);
  assertEnvelope(r);
});

await check('مسیرهای صریح v1 دست‌نخورده مانده‌اند (billing/checkout نیازمند ورود)', async () => {
  const r = await call('/billing/checkout', { method: 'POST', body: {} });
  if (r.status !== 401) throw new Error('باید ۴۰۱ باشد، دریافت شد ' + r.status);
  assertEnvelope(r);
});

await check('همهٔ مسیرهای تازه در OpenAPI v1 مستند شده‌اند', async () => {
  const { buildOpenApiSpec } = await load('server/openapiSpec.js');
  const keys = Object.keys(buildOpenApiSpec().paths);
  const required = [
    '/laws/{id}/relations',
    '/billing/my-billing',
    '/billing/cancel',
    '/billing/resume',
    '/auth/password/forgot',
    '/auth/password/reset',
    '/auth/email/send-verification',
    '/auth/email/verify',
    '/auth/contact',
    '/auth/google/handoff'
  ];
  const missing = required.filter((k) => !keys.includes(k));
  if (missing.length) throw new Error('مستند نشده: ' + missing.join(', '));
});

server.close();
if (fail.length) {
  console.error(`server-gateway: pass=${pass} fail=${fail.length}`);
  fail.forEach((f) => console.error('  ✗ ' + f));
  process.exit(1);
}
console.log(`server-gateway: pass=${pass} fail=0`);
process.exit(0);
