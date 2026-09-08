import { ensureSharedTable, getSql, sha256Hex, canonicalJson } from './lib/db';
import { analyzeGlyph, analyzeTemplate } from './lib/analyze';
import { notifyAdmin } from './lib/email';
import { envCheck } from './lib/env';

export const config = { runtime: 'edge' };

const MAX_BODY = 64 * 1024;
const MAX_PER_IP_DAY = 10;
const MAX_PER_HANDLE_DAY = 10;

interface Submission {
  type?: string;
  title?: string;
  description?: string;
  handle?: string;
  email?: string;
  github?: string;
  payload?: unknown;
}

function clean(s: unknown, max: number): string {
  return typeof s === 'string' ? s.trim().slice(0, max) : '';
}

export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') return new Response('method not allowed', { status: 405 });

  const env = envCheck();
  if (!env.ok) {
    return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });
  }

  const body = await req.text();
  if (body.length > MAX_BODY) return new Response('payload too large', { status: 413 });

  let sub: Submission;
  try {
    sub = JSON.parse(body);
  } catch {
    return new Response('invalid json', { status: 400 });
  }

  const type = sub.type === 'glyph' ? 'glyph' : sub.type === 'template' ? 'template' : null;
  const title = clean(sub.title, 120);
  const submittedDescription = clean(sub.description, 1000);
  const handle = clean(sub.handle, 120).replace(/^@/, '');
  const email = clean(sub.email, 320);
  const github = clean(sub.github, 200);

  if (!type || !title || !handle || sub.payload === undefined) {
    return new Response('missing fields: type, title, handle, payload are required', { status: 400 });
  }
  if (email && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    return new Response('invalid email', { status: 400 });
  }
  if (github && !/^https:\/\/(www\.)?github\.com\/[\w.-]+/i.test(github)) {
    return new Response('github must be a github.com URL or username', { status: 400 });
  }

  // ── static analysis — runs before anything touches the DB ────────────────
  const analysis = type === 'template' ? analyzeTemplate(sub.payload) : analyzeGlyph(sub.payload);
  const description = submittedDescription || analysis.description;
  if (analysis.verdict === 'reject') {
    return Response.json(
      { ok: false, verdict: 'reject', findings: analysis.findings },
      { status: 422 },
    );
  }

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const ip = req.headers.get('x-forwarded-for')?.split(',')[0]?.trim() ?? '';
  const ipHash = ip ? await sha256Hex(ip) : '';
  const contentHash = await sha256Hex(canonicalJson(analysis.sanitized));

  // ── rate limits ──────────────────────────────────────────────────────────
  const [recent] = await sql`
    SELECT
      count(*) FILTER (WHERE ip_hash = ${ipHash}) AS per_ip,
      count(*) FILTER (WHERE handle = ${handle}) AS per_handle
    FROM shared_items
    WHERE created_at > now() - interval '1 day'
  `;
  if (Number(recent?.per_ip ?? 0) >= MAX_PER_IP_DAY || Number(recent?.per_handle ?? 0) >= MAX_PER_HANDLE_DAY) {
    return new Response('rate limited — try again tomorrow', { status: 429 });
  }

  // Exact-duplicate detection.
  const dup = await sql`
    SELECT id, status FROM shared_items
    WHERE content_hash = ${contentHash} AND status != 'rejected'
    LIMIT 1
  `;
  if (dup.length > 0) {
    return Response.json(
      { ok: false, verdict: 'duplicate', detail: 'identical content already submitted', id: dup[0].id },
      { status: 409 },
    );
  }

  const [row] = await sql`
    INSERT INTO shared_items
      (type, title, description, handle, email, github, payload, analysis,
       content_hash, summary, capabilities, ip_hash)
    VALUES (
      ${type}, ${title}, ${description}, ${handle}, ${email}, ${github},
      ${JSON.stringify(analysis.sanitized)}, ${JSON.stringify(analysis)},
      ${contentHash}, ${analysis.summary}, ${analysis.capabilities}, ${ipHash}
    )
    RETURNING id
  `;

  const itemUrl = `https://nothing-modes.vercel.app/admin`;
  const emailStatus = await notifyAdmin(
    process.env.CRASH_NOTIFY_EMAIL || '',
    { id: row.id, type, title, handle, description },
    { verdict: analysis.verdict, findings: analysis.findings, summary: analysis.summary },
    itemUrl,
  ).catch((e) => `error:${String(e).slice(0, 80)}`);

  return Response.json(
    { ok: true, id: row.id, verdict: analysis.verdict, findings: analysis.findings, email: emailStatus },
    { status: 201 },
  );
}
