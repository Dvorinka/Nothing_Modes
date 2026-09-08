import { ensureSharedTable, getSql, isAdmin } from './lib/db';
import { envCheck } from './lib/env';

export const config = { runtime: 'edge' };

/**
 * Admin delete endpoint.
 *
 * POST /api/delete
 * Authorization: Bearer <ADMIN_TOKEN>
 * Body: { type: 'submission' | 'report', id: string }
 */
export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') return new Response('method not allowed', { status: 405 });

  const env = envCheck();
  if (!env.ok) return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });

  if (!isAdmin(req)) return new Response('unauthorized', { status: 401 });

  let body: { type?: string; id?: string };
  try {
    body = await req.json();
  } catch {
    return new Response('invalid json', { status: 400 });
  }

  const id = body.id ?? '';
  const type = body.type ?? '';
  if (!/^[0-9a-f-]{36}$/i.test(id) || !['submission', 'report'].includes(type)) {
    return new Response('bad request: type submission|report + id (uuid) required', { status: 400 });
  }

  const sql = getSql();
  if (!sql) return new Response('server misconfigured', { status: 500 });

  if (type === 'report') {
    await sql`DELETE FROM crash_reports WHERE id = ${id}`;
  } else {
    await ensureSharedTable(sql);
    await sql`DELETE FROM shared_items WHERE id = ${id}`;
  }

  return Response.json({ ok: true, id, type });
}
