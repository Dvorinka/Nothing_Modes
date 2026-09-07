import { ensureSharedTable, getSql, isAdmin } from './lib/db';

export const config = { runtime: 'edge' };

/**
 * Admin review queue — also the AI-agent contract.
 *
 * GET /api/submissions?status=pending|approved|rejected|all  (default pending)
 * Authorization: Bearer <ADMIN_TOKEN>  (CRASH_ADMIN_TOKEN also accepted)
 *
 * An agent can pull this list, run its own analysis on `payload`, and post its
 * verdict to /api/moderate. `analysis` contains our static-analyzer findings;
 * agents should treat them as advisory and do their own pass.
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'GET') return new Response('method not allowed', { status: 405 });
  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const status = new URL(req.url).searchParams.get('status') ?? 'pending';
  const rows = await sql`
    SELECT id, created_at, updated_at, type, status, title, description,
           handle, email, github, content_hash, summary, capabilities,
           analysis, moderated_by, moderated_at, moderation_reason,
           downloads, payload
    FROM shared_items
    WHERE (${status === 'all' ? '' : status} = '' OR status = ${status === 'all' ? '' : status})
    ORDER BY created_at DESC
    LIMIT 200
  `;

  return Response.json({ submissions: rows });
}
