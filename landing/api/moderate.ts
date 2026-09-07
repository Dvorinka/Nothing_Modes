import { ensureSharedTable, getSql, isAdmin } from './lib/db';

export const config = { runtime: 'edge' };

/**
 * Moderation endpoint — used by the admin page AND by AI agents.
 *
 * POST /api/moderate
 * Authorization: Bearer <ADMIN_TOKEN>
 * Body: {
 *   id: string,                    // submission id
 *   decision: "approve" | "reject",
 *   reason?: string,               // shown to the author
 *   moderatedBy?: string           // e.g. "admin" or "agent:claude"
 * }
 *
 * Human approval is final. An agent may approve/reject directly (it holds the
 * same token) — every decision is recorded with moderated_by so audit is clear.
 * On approve/reject the author is emailed if they left an address.
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') return new Response('method not allowed', { status: 405 });
  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  let body: { id?: string; decision?: string; reason?: string; moderatedBy?: string };
  try {
    body = await req.json();
  } catch {
    return new Response('invalid json', { status: 400 });
  }

  const id = body.id ?? '';
  const decision = body.decision === 'approve' ? 'approved' : body.decision === 'reject' ? 'rejected' : null;
  if (!/^[0-9a-f-]{36}$/i.test(id) || !decision) {
    return new Response('bad request: id (uuid) + decision approve|reject required', { status: 400 });
  }

  const reason = String(body.reason ?? '').slice(0, 1000);
  const moderatedBy = String(body.moderatedBy ?? 'admin').slice(0, 120);

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const rows = await sql`
    UPDATE shared_items
    SET status = ${decision}, moderated_by = ${moderatedBy},
        moderated_at = now(), moderation_reason = ${reason}, updated_at = now()
    WHERE id = ${id} AND status = 'pending'
    RETURNING id, type, title, handle, email
  `;
  if (rows.length === 0) return new Response('not found or already moderated', { status: 404 });

  const item = rows[0];
  const authorMail = item.email
    ? await notifyAuthor(item.email, item.title, decision, reason).catch(
        (e) => `error:${String(e).slice(0, 80)}`,
      )
    : 'no-email';

  return Response.json({ ok: true, id: item.id, status: decision, authorMail });
}

async function notifyAuthor(to: string, title: string, decision: string, reason: string): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  if (!key) return 'skipped:no-key';
  const esc = (s: string) =>
    s.replace(/[&<>"']/g, (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c] ?? c,
    );
  const approved = decision === 'approved';
  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: { Authorization: `Bearer ${key}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({
      from: 'Nothing Modes <nothing-modes@tdvorak.dev>',
      to: [to],
      subject: approved
        ? `Your Nothing Modes submission "${title}" was published`
        : `Your Nothing Modes submission "${title}" was not published`,
      text: approved
        ? `"${title}" is now live in the Nothing Modes library: https://nothing-modes.vercel.app/library`
        : `"${title}" was not published.${reason ? `\nReason: ${reason}` : ''}`,
      html: `<div style="font-family:ui-monospace,Menlo,monospace;max-width:560px;padding:24px">
        <h2 style="margin:0 0 12px">${approved ? 'Published' : 'Not published'}</h2>
        <p>"${esc(title)}" ${approved ? 'is now live in the <a href="https://nothing-modes.vercel.app/library">Nothing Modes library</a>.' : 'was not published.'}</p>
        ${reason ? `<p style="color:#555">${esc(reason)}</p>` : ''}
      </div>`,
    }),
  });
  return res.ok ? 'sent' : `failed:${res.status}`;
}
