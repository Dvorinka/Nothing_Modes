import { ensureSharedTable, getSql, sha256Hex } from './lib/db';
import { analyzeGlyph, analyzeTemplate } from './lib/analyze';

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
  const description = clean(sub.description, 1000);
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
  const contentHash = await sha256Hex(JSON.stringify(analysis.sanitized));

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

  const emailStatus = await notifyAdmin(row.id, { type, title, handle, description }, analysis).catch(
    (e) => `error:${String(e).slice(0, 80)}`,
  );

  return Response.json(
    { ok: true, id: row.id, verdict: analysis.verdict, findings: analysis.findings, email: emailStatus },
    { status: 201 },
  );
}

async function notifyAdmin(
  id: string,
  sub: { type: string; title: string; handle: string; description: string },
  analysis: { verdict: string; findings: { severity: string; code: string; detail: string }[]; summary: string },
): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  const to = process.env.CRASH_NOTIFY_EMAIL;
  if (!key || !to) return 'skipped:no-key-or-email';

  const esc = (s: unknown) =>
    String(s ?? '').replace(/[&<>"']/g, (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] ?? c,
    );

  const findingsHtml = analysis.findings.length
    ? `<ul style="margin:8px 0;padding-left:18px">${analysis.findings
        .map(
          (f) =>
            `<li style="color:${f.severity === 'block' ? '#D71921' : f.severity === 'warn' ? '#b8860b' : '#555'};font-size:12px;margin:4px 0">
              <b>${esc(f.severity.toUpperCase())}</b> ${esc(f.code)} — ${esc(f.detail)}</li>`,
        )
        .join('')}</ul>`
    : '<p style="color:#555;font-size:12px">No findings.</p>';

  const html = `<!DOCTYPE html><html><body style="margin:0;padding:0;background:#f4f4f4;font-family:ui-monospace,Menlo,Consolas,monospace">
  <div style="max-width:640px;margin:0 auto;padding:24px">
    <div style="background:#0a0a0a;border-radius:8px 8px 0 0;padding:20px 24px">
      <span style="color:#fff;font-size:14px;letter-spacing:.25em">N O T H I N G &nbsp; M O D E S</span>
      <span style="float:right;color:#D71921;font-size:11px;letter-spacing:.1em;font-weight:700">NEW SUBMISSION</span>
    </div>
    <div style="background:#fff;border:1px solid #e5e5e5;border-top:0;padding:16px 24px">
      <table style="width:100%;border-collapse:collapse;font-size:13px">
        <tr><td style="padding:6px 0;color:#888;font-size:11px">TYPE</td><td>${esc(sub.type)}</td></tr>
        <tr><td style="padding:6px 0;color:#888;font-size:11px">TITLE</td><td><b>${esc(sub.title)}</b></td></tr>
        <tr><td style="padding:6px 0;color:#888;font-size:11px">AUTHOR</td><td>@${esc(sub.handle)}</td></tr>
        <tr><td style="padding:6px 0;color:#888;font-size:11px">SUMMARY</td><td>${esc(analysis.summary)}</td></tr>
        <tr><td style="padding:6px 0;color:#888;font-size:11px">VERDICT</td><td><b>${esc(analysis.verdict.toUpperCase())}</b></td></tr>
      </table>
      ${sub.description ? `<p style="font-size:13px;color:#333;border-top:1px solid #eee;padding-top:12px;margin-top:12px">${esc(sub.description)}</p>` : ''}
      ${findingsHtml}
      <p style="margin-top:16px"><a href="https://nothing-modes.vercel.app/admin" style="color:#D71921">Review at nothing-modes.vercel.app/admin</a></p>
      <p style="color:#aaa;font-size:10px">id: ${esc(id)}</p>
    </div>
  </div></body></html>`;

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      from: 'Nothing Modes <nothing-modes@tdvorak.dev>',
      to: [to],
      subject: `[Nothing Modes] New ${sub.type} submission: ${sub.title} (@${sub.handle})`,
      text: `New ${sub.type} submission pending review.\nTitle: ${sub.title}\nAuthor: @${sub.handle}\nSummary: ${analysis.summary}\nVerdict: ${analysis.verdict}\nFindings: ${analysis.findings.map((f) => `${f.severity} ${f.code} ${f.detail}`).join('; ') || 'none'}\nReview: https://nothing-modes.vercel.app/admin\nid: ${id}`,
      html,
    }),
  });
  return res.ok ? 'sent' : `failed:${res.status}`;
}
