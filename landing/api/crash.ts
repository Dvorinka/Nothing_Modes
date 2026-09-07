import { neon } from '@neondatabase/serverless';

export const config = { runtime: 'edge' };

const MAX_BODY = 128 * 1024;
const EMAIL_DEDUP_WINDOW = "1 hour";

interface CrashReport {
  kind?: string;
  app?: string;
  app_version?: string;
  version_code?: number;
  flavor?: string;
  device?: string;
  android_release?: string;
  sdk_int?: number;
  exception_class?: string;
  message?: string;
  thread?: string;
  stacktrace?: string;
  context?: string;
  client_time?: number;
}

export default async function handler(req: Request): Promise<Response> {
  if (req.method !== 'POST') {
    return new Response('method not allowed', { status: 405 });
  }

  const body = await req.text();
  if (body.length > MAX_BODY) {
    return new Response('payload too large', { status: 413 });
  }

  let report: CrashReport;
  try {
    report = JSON.parse(body);
  } catch {
    return new Response('invalid json', { status: 400 });
  }
  if (report?.app !== 'nothing-modes' || typeof report.stacktrace !== 'string') {
    return new Response('invalid report', { status: 400 });
  }

  const dbUrl = process.env.DATABASE_URL;
  if (!dbUrl) return new Response('server misconfigured', { status: 500 });
  const sql = neon(dbUrl);

  const exceptionClass = String(report.exception_class ?? '');
  const message = String(report.message ?? '');

  // Dedup check before insert so a crash loop doesn't spam email.
  const seen = await sql`
    SELECT 1 FROM crash_reports
    WHERE exception_class = ${exceptionClass} AND message = ${message}
      AND created_at > now() - ${EMAIL_DEDUP_WINDOW}::interval
    LIMIT 1
  `;

  await sql`
    INSERT INTO crash_reports
      (app_version, version_code, flavor, device, android_release, sdk_int,
       kind, exception_class, message, thread, stacktrace, context, raw)
    VALUES (
      ${String(report.app_version ?? '')}, ${Number(report.version_code ?? 0)},
      ${String(report.flavor ?? '')}, ${String(report.device ?? '')},
      ${String(report.android_release ?? '')}, ${Number(report.sdk_int ?? 0)},
      ${String(report.kind ?? 'crash')}, ${exceptionClass}, ${message},
      ${String(report.thread ?? '')}, ${report.stacktrace},
      ${String(report.context ?? '')}, ${JSON.stringify(report)}
    )
  `;

  let email = 'deduped';
  if (seen.length === 0) {
    // Email is best-effort; a mail failure must not reject the report.
    email = await notify(report).catch((e) => `error:${String(e).slice(0, 100)}`);
  }

  return Response.json({ ok: true, email }, { status: 200 });
}

async function notify(report: CrashReport): Promise<string> {
  const key = process.env.RESEND_API_KEY;
  const to = process.env.CRASH_NOTIFY_EMAIL;
  if (!key || !to) return 'skipped:no-key-or-email';

  const subject =
    `[Nothing Modes ${report.kind ?? 'crash'}] ${report.exception_class ?? 'Error'}` +
    (report.message ? `: ${String(report.message).slice(0, 80)}` : '');

  const text = [
    `Kind:      ${report.kind ?? 'crash'}`,
    `App:       ${report.app_version ?? '?'} (${report.version_code ?? '?'}) / ${report.flavor ?? '?'}`,
    `Device:    ${report.device ?? '?'} — Android ${report.android_release ?? '?'} (SDK ${report.sdk_int ?? '?'})`,
    `Thread:    ${report.thread ?? '?'}`,
    `Context:   ${report.context || '-'}`,
    `Time:      ${report.client_time ? new Date(report.client_time).toISOString() : '-'}`,
    '',
    String(report.stacktrace ?? '').slice(0, 8000),
  ].join('\n');

  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${key}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: 'Nothing Modes <onboarding@resend.dev>',
      to: [to],
      subject,
      text,
    }),
  });
  if (!res.ok) {
    const err = await res.text();
    return `failed:${res.status}:${err.slice(0, 150)}`;
  }
  return 'sent';
}
