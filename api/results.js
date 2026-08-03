// Vercel Serverless Function — 보상 MBTI 결과 기록 (Neon Postgres)
//
// 환경변수: DATABASE_URL (Neon 연결 문자열)
//   Vercel의 Neon 통합을 붙이면 보통 DATABASE_URL 이 자동 주입됩니다.
//   (없으면 POSTGRES_URL 도 함께 시도합니다.)
//
//  POST /api/results  { name, code, typeName, answers }  → 결과 1건 저장
//  GET  /api/results                                     → 최근 기록 + 유형 분포

const { neon } = require('@neondatabase/serverless');

const CONN =
  process.env.DATABASE_URL ||
  process.env.POSTGRES_URL ||
  process.env.DATABASE_URL_UNPOOLED ||
  process.env.POSTGRES_PRISMA_URL;

const VALID_CODE = /^[EI][SN][TF][JP]$/;

let schemaReady = false;
async function ensureSchema(sql) {
  if (schemaReady) return;
  await sql`
    CREATE TABLE IF NOT EXISTS results (
      id          BIGSERIAL PRIMARY KEY,
      name        TEXT        NOT NULL,
      code        TEXT        NOT NULL,
      type_name   TEXT        NOT NULL,
      answers     TEXT,
      created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
    )
  `;
  schemaReady = true;
}

module.exports = async function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store');

  if (!CONN) {
    res.status(500).json({ ok: false, error: 'DATABASE_URL is not configured' });
    return;
  }

  const sql = neon(CONN);

  try {
    await ensureSchema(sql);

    if (req.method === 'POST') {
      const body = typeof req.body === 'string' ? safeParse(req.body) : (req.body || {});
      const name = String(body.name ?? '').trim().slice(0, 40);
      const code = String(body.code ?? '').trim().toUpperCase();
      const typeName = String(body.typeName ?? '').trim().slice(0, 60);
      const answers = String(body.answers ?? '').trim().slice(0, 40);

      if (!name) { res.status(400).json({ ok: false, error: 'name is required' }); return; }
      if (!VALID_CODE.test(code)) { res.status(400).json({ ok: false, error: 'invalid code' }); return; }

      const rows = await sql`
        INSERT INTO results (name, code, type_name, answers)
        VALUES (${name}, ${code}, ${typeName}, ${answers})
        RETURNING id, created_at
      `;
      res.status(201).json({ ok: true, id: rows[0].id, created_at: rows[0].created_at });
      return;
    }

    if (req.method === 'GET') {
      const limit = Math.min(parseInt(req.query?.limit, 10) || 500, 1000);
      const results = await sql`
        SELECT id, name, code, type_name, created_at
        FROM results
        ORDER BY created_at DESC
        LIMIT ${limit}
      `;
      const counts = await sql`
        SELECT code, COUNT(*)::int AS count
        FROM results
        GROUP BY code
        ORDER BY count DESC
      `;
      const total = results.length;
      res.status(200).json({ ok: true, total, results, counts });
      return;
    }

    res.setHeader('Allow', 'GET, POST');
    res.status(405).json({ ok: false, error: 'method not allowed' });
  } catch (err) {
    res.status(500).json({ ok: false, error: 'server error', detail: String(err && err.message || err) });
  }
};

function safeParse(s) { try { return JSON.parse(s); } catch { return {}; } }
