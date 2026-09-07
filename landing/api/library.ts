import { ensureSharedTable, getSql } from './lib/db';

export const config = { runtime: 'edge' };

/**
 * Public library index — approved items only.
 * Never exposes payload, email, ip, or moderation internals.
 *
 * GET /api/library            → all approved
 * GET /api/library?type=glyph → filter by type
 * GET /api/library?q=sleep    → search title/description/handle
 * GET /api/library?sort=download → newest | download | alpha
 * GET /api/library?caps=glyph,shizuku_required → must have all listed capabilities
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'GET') return new Response('method not allowed', { status: 405 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  const url = new URL(req.url);
  const type = url.searchParams.get('type') ?? '';
  const q = (url.searchParams.get('q') ?? '').trim().slice(0, 120);
  const sort = url.searchParams.get('sort') ?? 'newest';
  const caps = url.searchParams.get('caps') ?? '';
  const capList = caps.split(',').map((c) => c.trim()).filter(Boolean);
  const withPreview = url.searchParams.get('preview') === '1';

  const capFilter =
    capList.length > 0
      ? sql`AND capabilities @> ${capList}`
      : sql``;

  const rows = await sql`
    SELECT id, created_at, type, title, description, handle, github,
           content_hash, summary, capabilities, downloads
           ${withPreview ? sql`, payload` : sql``}
    FROM shared_items
    WHERE status = 'approved'
      AND (${type} = '' OR type = ${type})
      AND (${q} = '' OR title ILIKE ${'%' + q + '%'}
                      OR description ILIKE ${'%' + q + '%'}
                      OR handle ILIKE ${'%' + q + '%'})
      ${capFilter}
    ORDER BY ${
      sort === 'download' ? sql`downloads DESC, created_at DESC` :
      sort === 'alpha' ? sql`title ASC` :
      sql`created_at DESC`
    }
    LIMIT 200
  `;

  const items = withPreview ? rows.map((r) => ({ ...r, preview: buildPreview(r) })) : rows;
  return Response.json(
    { items },
    { headers: { 'cache-control': 'public, max-age=60' } },
  );
}

function buildPreview(r: Record<string, unknown>) {
  const payload = r.payload as Record<string, unknown> | undefined;
  if (!payload) return null;
  if (r.type === 'glyph') {
    const frames = Array.isArray(payload.frames) ? payload.frames : [];
    const first = frames[0] as Record<string, unknown> | undefined;
    if (!first) return null;
    return {
      size: payload.v === 4 ? 13 : 25,
      frameCount: frames.length,
      firstFrame: first.p,
      duration: first.d,
    };
  }
  const automations = Array.isArray(payload.automations) ? payload.automations : [];
  const first = automations[0] as Record<string, unknown> | undefined;
  const actionCount = automations.reduce(
    (n: number, a: Record<string, unknown>) => n + (Array.isArray(a.actions) ? a.actions.length : 0),
    0,
  );
  return {
    icon: first?.icon || 'routine',
    iconBackground: first?.iconBackground || 'transparent',
    automationCount: automations.length,
    actionCount,
  };
}
