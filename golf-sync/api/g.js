// 골프 기록 동행자 공유 서버 — 친구 코드 · 예정 공유(받은함) · 기록/사진 백업
// 저장소: Vercel Blob (private). 인증: 기기마다 받는 토큰 = HMAC(GOLF_KEY, uid)
import { put, get } from '@vercel/blob';
import { createHmac, randomBytes, timingSafeEqual } from 'node:crypto';

const KEY = process.env.GOLF_KEY || '';
const CORS = { 'access-control-allow-origin': '*', 'access-control-allow-methods': 'GET,POST,PUT,OPTIONS', 'access-control-allow-headers': 'content-type,x-golf-token', 'access-control-max-age': '86400' };
const json = (o, st = 200) => new Response(JSON.stringify(o), { status: st, headers: { 'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', ...CORS } });
const bad = (m, st = 400) => json({ error: m }, st);
const sign = uid => createHmac('sha256', KEY).update('golf:' + uid).digest('base64url').slice(0, 32);
function auth(req) {
  const t = req.headers.get('x-golf-token') || ''; const [uid, sig] = t.split('.');
  if (!uid || !sig || !/^[a-z0-9]{10,20}$/.test(uid)) return null;
  const a = Buffer.from(sign(uid)), b = Buffer.from(sig);
  return a.length === b.length && timingSafeEqual(a, b) ? uid : null;
}
const CODE_ABC = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
const newCode = () => [...randomBytes(6)].map(x => CODE_ABC[x % CODE_ABC.length]).join('');
const clean = (s, n) => String(s == null ? '' : s).replace(/[\u0000-\u001f]/g, '').trim().slice(0, n);

async function readJ(path, fresh) {
  const r = await get(path, { access: 'private', useCache: !fresh });
  if (!r || r.statusCode !== 200) return { data: null, etag: null };
  const txt = await new Response(r.stream).text();
  try { return { data: JSON.parse(txt), etag: r.blob.etag }; } catch { return { data: null, etag: r.blob.etag }; }
}
const writeJ = (path, data, etag) => put(path, JSON.stringify(data), { access: 'private', contentType: 'application/json', addRandomSuffix: false, allowOverwrite: true, cacheControlMaxAge: 60, ...(etag ? { ifMatch: etag } : {}) });
// 동시에 여러 명이 같은 받은함을 고쳐도 안 덮어쓰게: 읽고-고치고-조건부 쓰기, 실패하면 다시
async function update(path, fn, init) {
  for (let i = 0; i < 4; i++) {
    const { data, etag } = await readJ(path, true);
    const next = fn(data || init());
    if (next === undefined) return data;
    try { await writeJ(path, next, etag); return next; }
    catch (e) { if (!/precondition|etag|412/i.test(String(e && (e.name + e.message)))) throw e; }
  }
  throw new Error('busy');
}
const profilePath = uid => `u/${uid}.json`, inboxPath = uid => `ib/${uid}.json`;
const pub = p => ({ uid: p.uid, name: p.name, code: p.code });

const PLAN_KEYS = { id: 40, date: 10, tee: 5, course: 40, region: 20, country: 20, memo: 300 };
function cleanPlan(p) {
  const o = {}; for (const [k, n] of Object.entries(PLAN_KEYS)) o[k] = clean(p[k], n);
  if (!/^\d{4}-\d\d-\d\d$/.test(o.date) || !o.id) return null;
  o.abroad = !!p.abroad; o.caddie = !!p.caddie; o.holes = Math.max(9, Math.min(72, +p.holes || 18));
  o.mates = (Array.isArray(p.mates) ? p.mates : []).slice(0, 12).map(m => clean(m, 12)).filter(Boolean);
  return o;
}

async function handle(req) {
  if (!KEY) return bad('server key missing', 500);
  const url = new URL(req.url); const op = url.searchParams.get('op') || '';
  const body = req.method === 'POST' && op !== 'photo' ? await req.json().catch(() => ({})) : {};

  if (op === 'join' && req.method === 'POST') {
    const name = clean(body.name, 12); if (!name) return bad('name');
    const uid = randomBytes(8).toString('hex').replace(/[^a-z0-9]/g, '').slice(0, 14);
    let code = newCode();
    for (let i = 0; i < 4; i++) { const { data } = await readJ(`code/${code}.json`, true); if (!data) break; code = newCode(); }
    const p = { uid, name, code, friends: [], created: Date.now() };
    await writeJ(profilePath(uid), p); await writeJ(`code/${code}.json`, { uid });
    return json({ token: uid + '.' + sign(uid), me: pub(p), friends: [] });
  }

  if (op === 'ping') {
    // 연결 확인 · 관리용 점검(t=토큰일 때 저장소 읽기/쓰기와 동시 수정 막기까지 확인)
    const out = { ok: true, t: Date.now() };
    const t = url.searchParams.get('t'); if (t) { const r = new Request(req.url, { headers: { 'x-golf-token': t } }); const who = auth(r);
      if (who) { const path = `ping/${who}.json`; const w = await writeJ(path, { at: out.t }); const back = await readJ(path, true); out.rw = back.data && back.data.at === out.t;
        try { await writeJ(path, { at: 1 }, '"nope"'); out.conflict = 'not detected'; } catch (e) { out.conflict = String(e && (e.name + ': ' + e.message)).slice(0, 160); out.conflictOk = /precondition|etag|412/i.test(out.conflict); } out.etag = !!w; } }
    return json(out);
  }
  const uid = auth(req); if (!uid) return bad('auth', 401);

  if (op === 'me') {
    if (req.method === 'POST' && body.name) {
      const name = clean(body.name, 12);
      const p = await update(profilePath(uid), p => { p.name = name; return p; }, () => null);
      if (!p) return bad('gone', 404);
    }
    const { data: p } = await readJ(profilePath(uid), true); if (!p) return bad('gone', 404);
    // 친구 이름은 각자 프로필에서 새로 읽음 (이름 바꾼 것 반영)
    const fr = await Promise.all((p.friends || []).map(async f => { const { data } = await readJ(profilePath(f)); return data ? pub(data) : null; }));
    return json({ me: pub(p), friends: fr.filter(Boolean) });
  }

  if (op === 'friend' && req.method === 'POST') {
    const code = clean(body.code, 8).toUpperCase().replace(/[^A-Z0-9]/g, '');
    const { data: c } = await readJ(`code/${code}.json`, true); if (!c) return bad('no such code', 404);
    if (c.uid === uid) return bad('self');
    const add = other => p => { if (!p) return undefined; p.friends = [...new Set([...(p.friends || []), other])].slice(0, 100); return p; };
    const me = await update(profilePath(uid), add(c.uid), () => null); const them = await update(profilePath(c.uid), add(uid), () => null);
    if (!me || !them) return bad('gone', 404);
    return json({ friend: pub(them) });
  }

  if (op === 'unfriend' && req.method === 'POST') {
    const other = clean(body.uid, 20); const rm = x => p => { if (!p) return undefined; p.friends = (p.friends || []).filter(f => f !== x); return p; };
    await update(profilePath(uid), rm(other), () => null); await update(profilePath(other), rm(uid), () => null);
    return json({ ok: true });
  }

  if (op === 'share' && req.method === 'POST') {
    // items: [{to:[uid], plan:{...}} | {to:[uid], cancel:planId}]
    const { data: p } = await readJ(profilePath(uid), true); if (!p) return bad('gone', 404);
    const friends = new Set(p.friends || []); const per = new Map(); const now = Date.now();
    for (const it of (Array.isArray(body.items) ? body.items : []).slice(0, 30)) {
      const plan = it.plan ? cleanPlan(it.plan) : null; const cid = clean(it.cancel, 40);
      if (!plan && !cid) continue;
      for (const to of (Array.isArray(it.to) ? it.to : [])) {
        if (!friends.has(to)) continue;
        if (!per.has(to)) per.set(to, []);
        per.get(to).push(plan ? { k: uid + ':' + plan.id, from: uid, fromName: p.name, plan, u: now } : { k: uid + ':' + cid, from: uid, fromName: p.name, cancel: true, u: now });
      }
    }
    const cutoff = new Date(now - 60 * 86400000).toISOString().slice(0, 10);
    await Promise.all([...per.entries()].map(([to, ups]) => update(inboxPath(to), ib => {
      ib.items = ib.items || {};
      for (const x of ups) ib.items[x.k] = x;
      for (const [k, x] of Object.entries(ib.items)) if ((x.plan && x.plan.date < cutoff) || (x.cancel && now - x.u > 30 * 86400000)) delete ib.items[k];
      ib.v = now; return ib;
    }, () => ({ v: 0, items: {} }))));
    return json({ ok: true, sent: per.size });
  }

  if (op === 'inbox') {
    const { data } = await readJ(inboxPath(uid), url.searchParams.get('fresh') === '1');
    return json(data || { v: 0, items: {} });
  }

  if (op === 'backup') {
    const path = `bk/${uid}/data.json`;
    if (req.method === 'POST') {
      const s = JSON.stringify(body); if (s.length > 3_000_000) return bad('too big', 413);
      // 기록이 절반 넘게 줄어드는 자동 백업은 막음 (새 폰 빈 상태로 덮어쓰기 방지) · 직접 누른 백업(force)만 통과
      if (!body.force) { const { data: old } = await readJ(path, true); const was = old && Array.isArray(old.records) ? old.records.length : 0, now = Array.isArray(body.records) ? body.records.length : 0;
        if (was >= 10 && now < was * 0.5) return json({ error: 'shrink', was, now }, 409); }
      await writeJ(path, { ...body, at: Date.now() }); return json({ ok: true, at: Date.now() });
    }
    const { data } = await readJ(path, true); return json(data || {});
  }

  if (op === 'photo') {
    const id = clean(url.searchParams.get('id'), 40); if (!/^p[a-z0-9]{6,30}$/.test(id)) return bad('id');
    const path = `bk/${uid}/ph/${id}.jpg`;
    if (req.method === 'POST' || req.method === 'PUT') {
      const buf = Buffer.from(await req.arrayBuffer()); if (!buf.length || buf.length > 4_000_000) return bad('size', 413);
      await put(path, buf, { access: 'private', contentType: 'image/jpeg', addRandomSuffix: false, allowOverwrite: true });
      return json({ ok: true });
    }
    const r = await get(path, { access: 'private' }); if (!r || r.statusCode !== 200) return bad('none', 404);
    return new Response(r.stream, { headers: { 'content-type': 'image/jpeg', 'cache-control': 'private, max-age=86400', ...CORS } });
  }

  return bad('op', 404);
}

const run = async req => { try { return await handle(req); } catch (e) { console.error(e); return bad(String(e && e.message || e).slice(0, 200), 500); } };
export const GET = run, POST = run, PUT = run;
export const OPTIONS = () => new Response(null, { status: 204, headers: CORS });
