import { ensureSharedTable, getSql } from './lib/db';

export const config = { runtime: 'edge' };

/**
 * Public library index — approved items only.
 * Never exposes payload, email, ip, or moderation internals.
 *
 * GET /api/library            → all approved
 * GET /api/library?type=glyph → filter by type
 * GET /api/library?q=sleep    → search title/description/handle
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'GET') return new Response('method not allowed', { status: 405 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const url = new URL(req.url);
  const type = url.searchParams.get('type');
  const q = (url.searchParams.get('q') ?? '').trim().slice(0, 120);

  const rows = await sql`
    SELECT id, created_at, type, title, description, handle, github,
           content_hash, summary, capabilities, downloads
    FROM shared_items
    WHERE status = 'approved'
      AND (${type ?? ''} = '' OR type = ${type ?? ''})
      AND (${q} = '' OR title ILIKE ${'%' + q + '%'}
                      OR description ILIKE ${'%' + q + '%'}
                      OR handle ILIKE ${'%' + q + '%'})
    ORDER BY created_at DESC
    LIMIT 200
  `;

  return Response.json(
    { items: rows },
    { headers: { 'cache-control': 'public, max-age=60' } },
  );
}
