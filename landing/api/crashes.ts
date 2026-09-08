import { neon } from '@neondatabase/serverless';
import { envCheck } from './lib/env';

export const config = { runtime: 'edge' };

export default async function handler(req: Request): Promise<Response> {
  const env = envCheck();
  if (!env.ok) return new Response(`server misconfigured: missing ${env.missing.join(', ')}`, { status: 500 });

  const token = process.env.CRASH_ADMIN_TOKEN;
  if (!token || req.headers.get('authorization') !== `Bearer ${token}`) {
    return new Response('unauthorized', { status: 401 });
  }

  const dbUrl = process.env.DATABASE_URL;
  if (!dbUrl) return new Response('server misconfigured', { status: 500 });
  const sql = neon(dbUrl);

  const reports = await sql`
    SELECT id, created_at, app_version, version_code, flavor, device,
           android_release, sdk_int, kind, exception_class, message,
           thread, stacktrace, context
    FROM crash_reports
    ORDER BY created_at DESC
    LIMIT 200
  `;

  return Response.json({ reports });
}
