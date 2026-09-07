import { readFile } from 'node:fs/promises';
import { ensureSharedTable, getSql, isAdmin } from './lib/db';
import { seedDemos } from './lib/seed';

export const config = { runtime: 'nodejs' };

/**
 * Admin-only: force seed the bundled demo templates and Glyph Museum presets.
 * Useful after the first deploy or after a database wipe.
 *
 * POST /api/seed
 * Authorization: Bearer <ADMIN_TOKEN>
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') return new Response('method not allowed', { status: 405 });
  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });
  await ensureSharedTable(sql);

  try {
    const raw = await readFile(new URL('../seed.json', import.meta.url), 'utf8');
    const { items } = JSON.parse(raw) as { items: { type: 'template' | 'glyph'; title: string; description: string; payload: unknown }[] };
    await seedDemos(sql, items);
    const [{ count }] = await sql`SELECT count(*)::int AS count FROM shared_items`;
    return Response.json({ ok: true, items: Number(count) });
  } catch (e) {
    return Response.json({ ok: false, error: String(e) }, { status: 500 });
  }
}
