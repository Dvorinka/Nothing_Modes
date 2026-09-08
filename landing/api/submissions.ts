import { ensureSharedTable, getSql, isAdmin } from './lib/db';
import { envCheck } from './lib/env';

export const config = { runtime: 'edge' };

/**
 * Admin review queue — also the AI-agent contract.
 *
 * GET /api/submissions?status=pending|approved|rejected|all&type=template|glyph|all
 * Authorization: Bearer <ADMIN_TOKEN>  (CRASH_ADMIN_TOKEN also accepted)
 *
 * An agent can pull this list, run its own analysis on `payload`, and post its
 * verdict to /api/moderate. `analysis` contains our static-analyzer findings;
 * agents should treat them as advisory and do their own pass.
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'GET') return new Response('method not allowed', { status: 405 });

  const env = envCheck();
  if (!env.ok) return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });

  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const url = new URL(req.url);
  const status = (url.searchParams.get('status') ?? 'pending').toLowerCase();
  const type = (url.searchParams.get('type') ?? 'all').toLowerCase();

  const statusFilter = status === 'all' || status === '' ? sql`true` : sql`status = ${status}`;
  const typeFilter = type === 'all' || type === '' ? sql`true` : sql`type = ${type}`;

  const rows = await sql`
    SELECT id, created_at, updated_at, type, status, title, description,
           handle, email, github, content_hash, summary, capabilities,
           analysis, moderated_by, moderated_at, moderation_reason,
           downloads, payload
    FROM shared_items
    WHERE ${statusFilter} AND ${typeFilter}
    ORDER BY created_at DESC
    LIMIT 200
  `;

  return Response.json({ submissions: rows });
}
