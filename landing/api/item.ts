import { ensureSharedTable, getSql } from './lib/db';

export const config = { runtime: 'edge' };

/**
 * Public download — one approved item including its (sanitized) payload.
 * The payload was already passed through static analysis + PII redaction at
 * submission time; content_hash lets the client verify integrity.
 *
 * GET /api/item?id=<uuid>
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'GET') return new Response('method not allowed', { status: 405 });

  const id = new URL(req.url).searchParams.get('id') ?? '';
  if (!/^[0-9a-f-]{36}$/i.test(id)) return new Response('bad id', { status: 400 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const rows = await sql`
    UPDATE shared_items SET downloads = downloads + 1
    WHERE id = ${id} AND status = 'approved'
    RETURNING id, created_at, type, title, description, handle, github,
              content_hash, summary, capabilities, payload
  `;
  if (rows.length === 0) return new Response('not found', { status: 404 });

  return Response.json({ item: rows[0] });
}
